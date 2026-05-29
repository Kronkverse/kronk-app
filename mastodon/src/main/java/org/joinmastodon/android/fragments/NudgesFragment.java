package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountByID;
import org.joinmastodon.android.api.requests.accounts.GetNudgePartners;
import org.joinmastodon.android.api.requests.accounts.SendNudge;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.NudgePartner;
import org.joinmastodon.android.model.NudgePartnersResponse;
import org.joinmastodon.android.model.NudgeResult;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.BottomSheet;

public class NudgesFragment extends android.app.Fragment implements ScrollableToTop {

	private static final int TYPE_HEADER   = 0;
	private static final int TYPE_SECTION  = 1;
	private static final int TYPE_PARTNER  = 2;
	private static final int TYPE_NUDGE_ALL = 3;

	private static final int MILESTONE_THRESHOLD = 10;

	private String accountID;
	private NudgePartnersResponse data;
	private boolean loaded, loading;
	private int totalSent, totalReceived;
	private List<NudgePartner> pendingReceived = new ArrayList<>();

	private ProgressBar progress;
	private SwipeRefreshLayout refreshLayout;
	private RecyclerView list;
	private View emptyView;
	private NudgesAdapter adapter;
	private final List<ListItem> items = new ArrayList<>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		accountID = getArguments().getString("account");
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_nudges, container, false);
		progress = view.findViewById(R.id.progress);
		refreshLayout = view.findViewById(R.id.refresh);
		list = view.findViewById(R.id.list);
		emptyView = view.findViewById(R.id.empty_view);

		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		adapter = new NudgesAdapter();
		list.setAdapter(adapter);

		new ItemTouchHelper(new SwipeToNudgeCallback()).attachToRecyclerView(list);

		refreshLayout.setOnRefreshListener(() -> {
			loaded = false;
			loadData();
		});

		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (!loaded && !loading) {
			showLoading();
			loadData();
		}
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		if (!hidden && !loaded && !loading) {
			showLoading();
			loadData();
		}
	}

	public void onApplyWindowInsets(WindowInsets insets) {
		if (list != null)
			list.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
	}

	@Override
	public void scrollToTop() {
		if (list != null) list.smoothScrollToPosition(0);
	}

	void loadData() {
		loading = true;
		new GetNudgePartners()
				.setCallback(new Callback<NudgePartnersResponse>() {
					@Override
					public void onSuccess(NudgePartnersResponse result) {
						if (getActivity() == null) return;
						if (result.partners == null || result.partners.isEmpty()) {
							finishLoad(result);
							return;
						}
						AtomicInteger remaining = new AtomicInteger(result.partners.size());
						for (NudgePartner partner : result.partners) {
							if (partner.account != null) {
								try { partner.account.postprocess(); } catch (Exception ignored) {}
								if (remaining.decrementAndGet() == 0) finishLoad(result);
								continue;
							}
							new GetAccountByID(partner.account_id)
									.setCallback(new Callback<Account>() {
										@Override
										public void onSuccess(Account account) {
											partner.account = account;
											if (remaining.decrementAndGet() == 0 && getActivity() != null)
												finishLoad(result);
										}
										@Override
										public void onError(ErrorResponse error) {
											if (remaining.decrementAndGet() == 0 && getActivity() != null)
												finishLoad(result);
										}
									})
									.exec(accountID);
						}
					}

					@Override
					public void onError(ErrorResponse error) {
						if (getActivity() == null) return;
						loading = false;
						refreshLayout.setRefreshing(false);
						progress.setVisibility(View.GONE);
						refreshLayout.setVisibility(View.VISIBLE);
						error.showToast(getActivity());
					}
				})
				.exec(accountID);
	}

	private void finishLoad(NudgePartnersResponse result) {
		data = result;
		loaded = true;
		loading = false;
		refreshLayout.setRefreshing(false);
		progress.setVisibility(View.GONE);
		rebuildItems();
	}

	private void showLoading() {
		progress.setVisibility(View.VISIBLE);
		refreshLayout.setVisibility(View.GONE);
		emptyView.setVisibility(View.GONE);
	}

	private void rebuildItems() {
		items.clear();

		if (data == null || data.partners == null || data.partners.isEmpty()) {
			emptyView.setVisibility(View.VISIBLE);
			refreshLayout.setVisibility(View.GONE);
			adapter.notifyDataSetChanged();
			return;
		}

		emptyView.setVisibility(View.GONE);
		refreshLayout.setVisibility(View.VISIBLE);

		totalSent = 0;
		totalReceived = 0;
		for (NudgePartner p : data.partners) {
			totalSent += p.sent_count;
			totalReceived += p.received_count;
		}

		items.add(new ListItem(TYPE_HEADER, null, null, false));

		List<NudgePartner> received = new ArrayList<>();
		List<NudgePartner> sent = new ArrayList<>();
		for (NudgePartner p : data.partners) {
			if (p.can_nudge_back) received.add(p);
			else sent.add(p);
		}
		pendingReceived = received;

		if (!received.isEmpty()) {
			items.add(new ListItem(TYPE_SECTION, getString(R.string.nudge_section_received), null, true));
			if (received.size() >= 2)
				items.add(new ListItem(TYPE_NUDGE_ALL, null, null, true));
			for (NudgePartner p : received) items.add(new ListItem(TYPE_PARTNER, null, p, false));
		}

		if (!sent.isEmpty()) {
			items.add(new ListItem(TYPE_SECTION, getString(R.string.nudge_section_sent), null, false));
			for (NudgePartner p : sent) items.add(new ListItem(TYPE_PARTNER, null, p, false));
		}

		adapter.notifyDataSetChanged();
	}

	private void sendNudgeForPartner(NudgePartner partner) {
		if (partner.account == null || !partner.can_nudge_back) return;
		boolean wasReceived = partner.can_nudge_back;
		partner.can_nudge_back = false;
		partner.sent_count++;
		if (data != null) {
			data.grand_total++;
			if (wasReceived && data.pending_count > 0) data.pending_count--;
		}
		rebuildItems();

		new SendNudge(partner.account.id)
				.setCallback(new Callback<NudgeResult>() {
					@Override
					public void onSuccess(NudgeResult result) {
						if (result.streak > 0) partner.streak = result.streak;
						if (getActivity() != null) rebuildItems();
					}
					@Override
					public void onError(ErrorResponse error) {
						if (getActivity() == null) return;
						if (error instanceof org.joinmastodon.android.api.MastodonErrorResponse mr && mr.httpStatus == 422) {
							// server confirms already sent — keep optimistic state
							rebuildItems();
						} else {
							// revert
							partner.can_nudge_back = true;
							partner.sent_count--;
							if (data != null) {
								data.grand_total--;
								data.pending_count++;
							}
							rebuildItems();
							error.showToast(getActivity());
						}
					}
				})
				.exec(accountID);
	}

	private void nudgeAllBack() {
		for (NudgePartner p : new ArrayList<>(pendingReceived)) sendNudgeForPartner(p);
	}

	private void showQuickSheet(NudgePartner partner) {
		Activity activity = getActivity();
		if (activity == null || partner.account == null) return;
		Account acc = partner.account;

		BottomSheet sheet = new BottomSheet(activity);
		View v = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_nudge_quick, null);

		ImageView sheetAvatar = v.findViewById(R.id.sheet_avatar);
		TextView sheetName = v.findViewById(R.id.sheet_name);
		TextView sheetUsername = v.findViewById(R.id.sheet_username);
		TextView sheetStreakSent = v.findViewById(R.id.sheet_streak_sent);
		TextView sheetStreakReceived = v.findViewById(R.id.sheet_streak_received);
		Button sheetNudgeBtn = v.findViewById(R.id.sheet_nudge_btn);
		Button sheetProfileBtn = v.findViewById(R.id.sheet_profile_btn);

		String name = acc.displayName.isEmpty() ? acc.username : acc.displayName;
		sheetName.setText(name);
		sheetUsername.setText("@" + acc.acct);
		sheetStreakSent.setText(getString(R.string.nudge_streak_sent, partner.sent_count));
		sheetStreakReceived.setText(getString(R.string.nudge_streak_received, partner.received_count));

		String url = GlobalUserPreferences.playGifs ? acc.avatar : acc.avatarStatic;
		ViewImageLoader.load(sheetAvatar, null, new UrlImageLoaderRequest(url, V.dp(72), V.dp(72)));
		sheetAvatar.setOutlineProvider(OutlineProviders.roundedRect(12));
		sheetAvatar.setClipToOutline(true);

		if (partner.can_nudge_back) {
			sheetNudgeBtn.setText(R.string.nudge_back);
			sheetNudgeBtn.setEnabled(true);
		} else {
			sheetNudgeBtn.setText(R.string.nudge_waiting);
			sheetNudgeBtn.setEnabled(false);
			sheetNudgeBtn.setAlpha(0.5f);
		}

		sheetNudgeBtn.setOnClickListener(vv -> {
			sendNudgeForPartner(partner);
			sheet.dismiss();
		});

		sheetProfileBtn.setOnClickListener(vv -> {
			sheet.dismiss();
			Bundle args = new Bundle();
			args.putString("account", accountID);
			args.putParcelable("profileAccount", org.parceler.Parcels.wrap(acc));
			Nav.go(activity, ProfileFragment.class, args);
		});

		sheet.setContentView(v);
		sheet.show();
	}

	private static class ListItem {
		final int type;
		final String label;
		final NudgePartner partner;
		final boolean isReceivedSection;
		ListItem(int type, String label, NudgePartner partner, boolean isReceivedSection) {
			this.type = type;
			this.label = label;
			this.partner = partner;
			this.isReceivedSection = isReceivedSection;
		}
	}

	private class NudgesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

		@Override
		public int getItemViewType(int position) {
			return items.get(position).type;
		}

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(getActivity());
			return switch (viewType) {
				case TYPE_HEADER    -> new HeaderViewHolder(inflater.inflate(R.layout.item_nudge_stats_header, parent, false));
				case TYPE_SECTION   -> new SectionViewHolder(inflater.inflate(R.layout.item_nudge_section_label, parent, false));
				case TYPE_NUDGE_ALL -> new NudgeAllViewHolder(inflater.inflate(R.layout.item_nudge_all_back, parent, false));
				default             -> new PartnerViewHolder(inflater.inflate(R.layout.item_nudge_partner, parent, false));
			};
		}

		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			ListItem item = items.get(position);
			if (holder instanceof HeaderViewHolder h)    h.bind();
			else if (holder instanceof SectionViewHolder s)   s.bind(item.label, item.isReceivedSection);
			else if (holder instanceof NudgeAllViewHolder a)  a.bind(pendingReceived.size());
			else if (holder instanceof PartnerViewHolder p)   p.bind(item.partner);
		}

		@Override
		public int getItemCount() { return items.size(); }
	}

	private class HeaderViewHolder extends RecyclerView.ViewHolder {
		private final TextView grandTotalNumber;
		private final TextView sentNumber;
		private final TextView receivedNumber;
		private final TextView pendingHint;

		HeaderViewHolder(View view) {
			super(view);
			grandTotalNumber = view.findViewById(R.id.grand_total_number);
			sentNumber       = view.findViewById(R.id.total_sent_number);
			receivedNumber   = view.findViewById(R.id.total_received_number);
			pendingHint      = view.findViewById(R.id.pending_hint);
		}

		void bind() {
			if (data == null) return;
			grandTotalNumber.setText(String.valueOf(data.grand_total));
			sentNumber.setText(String.valueOf(totalSent));
			receivedNumber.setText(String.valueOf(totalReceived));
			if (data.pending_count > 0) {
				pendingHint.setText(getResources().getQuantityString(
						R.plurals.nudge_pending_hint, data.pending_count, data.pending_count));
				pendingHint.setVisibility(View.VISIBLE);
			} else {
				pendingHint.setVisibility(View.GONE);
			}
		}
	}

	private static class SectionViewHolder extends RecyclerView.ViewHolder {
		private final TextView label;
		SectionViewHolder(View view) {
			super(view);
			label = view.findViewById(R.id.section_label);
		}
		void bind(String text, boolean isReceived) {
			label.setText(text);
			int dotColor = isReceived
					? UiUtils.getThemeColor(label.getContext(), R.attr.colorM3Primary)
					: applyAlpha(UiUtils.getThemeColor(label.getContext(), R.attr.colorM3OnSurfaceVariant), 120);
			GradientDrawable dot = new GradientDrawable();
			dot.setShape(GradientDrawable.OVAL);
			dot.setBounds(0, 0, V.dp(7), V.dp(7));
			dot.setColor(dotColor);
			label.setCompoundDrawablePadding(V.dp(7));
			label.setCompoundDrawablesRelative(dot, null, null, null);
		}
	}

	private class NudgeAllViewHolder extends RecyclerView.ViewHolder {
		private final Button btn;
		NudgeAllViewHolder(View view) {
			super(view);
			btn = view.findViewById(R.id.nudge_all_btn);
			btn.setOnClickListener(v -> nudgeAllBack());
		}
		void bind(int count) {
			btn.setText(getResources().getQuantityString(R.plurals.nudge_all_back, count, count));
		}
	}

	private class PartnerViewHolder extends RecyclerView.ViewHolder {
		private final ImageView avatar;
		private final ImageView waveIcon;
		private final TextView displayName;
		private final TextView username;
		private final TextView streakSent;
		private final TextView streakReceived;
		private final TextView nudgeTimestamp;
		private final Button nudgeBtn;
		private NudgePartner currentPartner;

		PartnerViewHolder(View view) {
			super(view);
			avatar         = view.findViewById(R.id.avatar);
			waveIcon       = view.findViewById(R.id.wave_icon);
			displayName    = view.findViewById(R.id.display_name);
			username       = view.findViewById(R.id.username);
			streakSent     = view.findViewById(R.id.streak_sent);
			streakReceived = view.findViewById(R.id.streak_received);
			nudgeTimestamp = view.findViewById(R.id.nudge_timestamp);
			nudgeBtn       = view.findViewById(R.id.nudge_btn);

			avatar.setOutlineProvider(OutlineProviders.roundedRect(8));
			avatar.setClipToOutline(true);

			waveIcon.setImageTintList(ColorStateList.valueOf(
					UiUtils.getThemeColor(view.getContext(), R.attr.colorM3Primary)));

			avatar.setOnClickListener(v -> {
				if (currentPartner != null) showQuickSheet(currentPartner);
			});
			displayName.setOnClickListener(v -> {
				if (currentPartner != null) showQuickSheet(currentPartner);
			});
			nudgeBtn.setOnClickListener(v -> {
				if (currentPartner != null) sendNudgeForPartner(currentPartner);
			});
		}

		void bind(NudgePartner partner) {
			currentPartner = partner;

			Account acc = partner.account;
			if (acc != null) {
				username.setText("@" + acc.acct);
				String url = GlobalUserPreferences.playGifs ? acc.avatar : acc.avatarStatic;
				ViewImageLoader.load(avatar, null, new UrlImageLoaderRequest(url, V.dp(46), V.dp(46)));
			} else {
				username.setText("");
				avatar.setImageResource(R.drawable.image_placeholder);
			}

			streakSent.setText(getString(R.string.nudge_streak_sent, partner.sent_count));
			streakReceived.setText(getString(R.string.nudge_streak_received, partner.received_count));

			bindTimestamp(partner.last_nudge_at);
			bindMilestoneBadge(partner);
			updateRowBackground(partner.can_nudge_back);
			updateButton(partner.can_nudge_back);
		}

		private void bindTimestamp(String lastNudgeAt) {
			if (lastNudgeAt == null || lastNudgeAt.isEmpty()) {
				nudgeTimestamp.setVisibility(View.GONE);
				return;
			}
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
				sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
				Date date = sdf.parse(lastNudgeAt);
				CharSequence relative = DateUtils.getRelativeTimeSpanString(
						date.getTime(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
				nudgeTimestamp.setText(relative);
				nudgeTimestamp.setVisibility(View.VISIBLE);
			} catch (Exception e) {
				nudgeTimestamp.setVisibility(View.GONE);
			}
		}

		private void bindMilestoneBadge(NudgePartner partner) {
			int total = partner.sent_count + partner.received_count;
			Account acc = partner.account;
			String name = (acc != null)
					? (acc.displayName.isEmpty() ? acc.username : acc.displayName)
					: partner.account_id;

			if (total >= MILESTONE_THRESHOLD) {
				SpannableStringBuilder sb = new SpannableStringBuilder(name);
				sb.append("  ★");
				int starColor = UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3Tertiary);
				sb.setSpan(new ForegroundColorSpan(starColor),
						sb.length() - 1, sb.length(),
						android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				displayName.setText(sb);
			} else {
				displayName.setText(name);
			}
			waveIcon.setBackgroundResource(R.drawable.bg_nudge_wave_badge);
		}

		private void updateRowBackground(boolean isReceived) {
			if (isReceived) {
				int primary = UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3Primary);
				itemView.setBackgroundColor(applyAlpha(primary, 22));
			} else {
				itemView.setBackgroundColor(Color.TRANSPARENT);
			}
		}

		private void updateButton(boolean canNudgeBack) {
			if (canNudgeBack) {
				nudgeBtn.setText(R.string.nudge_back);
				nudgeBtn.setEnabled(true);
				nudgeBtn.setAlpha(1f);
				nudgeBtn.setBackgroundTintList(ColorStateList.valueOf(
						UiUtils.getThemeColor(nudgeBtn.getContext(), R.attr.colorM3Primary)));
				nudgeBtn.setTextColor(UiUtils.getThemeColor(nudgeBtn.getContext(), R.attr.colorM3OnPrimary));
			} else {
				nudgeBtn.setText(R.string.nudge_waiting);
				nudgeBtn.setEnabled(false);
				nudgeBtn.setAlpha(0.45f);
				nudgeBtn.setBackgroundTintList(ColorStateList.valueOf(
						UiUtils.getThemeColor(nudgeBtn.getContext(), R.attr.colorM3SecondaryContainer)));
				nudgeBtn.setTextColor(UiUtils.getThemeColor(nudgeBtn.getContext(), R.attr.colorM3OnSecondaryContainer));
			}
		}
	}

	private class SwipeToNudgeCallback extends ItemTouchHelper.SimpleCallback {
		private final Paint swipePaint = new Paint();

		SwipeToNudgeCallback() {
			super(0, ItemTouchHelper.RIGHT);
		}

		@Override
		public int getSwipeDirs(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
			int pos = vh.getAdapterPosition();
			if (pos < 0 || pos >= items.size()) return 0;
			ListItem item = items.get(pos);
			if (item.type == TYPE_PARTNER && item.partner != null && item.partner.can_nudge_back)
				return ItemTouchHelper.RIGHT;
			return 0;
		}

		@Override
		public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
			return false;
		}

		@Override
		public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
			int pos = vh.getAdapterPosition();
			if (pos >= 0 && pos < items.size() && items.get(pos).partner != null)
				sendNudgeForPartner(items.get(pos).partner);
		}

		@Override
		public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
				float dX, float dY, int actionState, boolean isCurrentlyActive) {
			if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX > 0) {
				View item = vh.itemView;
				int primary = UiUtils.getThemeColor(item.getContext(), R.attr.colorM3Primary);
				int alpha = Math.min(70, (int) (dX / item.getWidth() * 100));
				swipePaint.setColor(applyAlpha(primary, alpha));
				c.drawRect(item.getLeft(), item.getTop(), item.getLeft() + dX, item.getBottom(), swipePaint);
			}
			super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive);
		}
	}

	private static int applyAlpha(int color, int alpha) {
		return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
	}
}
