package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetNudgePartners;
import org.joinmastodon.android.api.requests.accounts.SendNudge;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.NudgePartner;
import org.joinmastodon.android.model.NudgePartnersResponse;
import org.joinmastodon.android.model.NudgeResult;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class NudgesFragment extends android.app.Fragment implements ScrollableToTop {

	private static final int TYPE_HEADER = 0;
	private static final int TYPE_SECTION = 1;
	private static final int TYPE_PARTNER = 2;

	private String accountID;
	private NudgePartnersResponse data;
	private boolean loaded, loading;

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

	private void loadData() {
		loading = true;
		new GetNudgePartners()
				.setCallback(new Callback<NudgePartnersResponse>() {
					@Override
					public void onSuccess(NudgePartnersResponse result) {
						if (getActivity() == null) return;
						data = result;
						loaded = true;
						loading = false;
						refreshLayout.setRefreshing(false);
						progress.setVisibility(View.GONE);
						rebuildItems();
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

		items.add(new ListItem(TYPE_HEADER, null, null));

		List<NudgePartner> waiting = new ArrayList<>();
		List<NudgePartner> active = new ArrayList<>();
		for (NudgePartner p : data.partners) {
			if (p.can_nudge_back) waiting.add(p);
			else active.add(p);
		}

		if (!waiting.isEmpty()) {
			items.add(new ListItem(TYPE_SECTION, getString(R.string.nudge_section_waiting), null));
			for (NudgePartner p : waiting) items.add(new ListItem(TYPE_PARTNER, null, p));
		}

		if (!active.isEmpty()) {
			items.add(new ListItem(TYPE_SECTION, getString(R.string.nudge_section_partners), null));
			for (NudgePartner p : active) items.add(new ListItem(TYPE_PARTNER, null, p));
		}

		adapter.notifyDataSetChanged();
	}

	private static class ListItem {
		final int type;
		final String label;
		final NudgePartner partner;
		ListItem(int type, String label, NudgePartner partner) {
			this.type = type;
			this.label = label;
			this.partner = partner;
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
				case TYPE_HEADER -> new HeaderViewHolder(inflater.inflate(R.layout.item_nudge_stats_header, parent, false));
				case TYPE_SECTION -> new SectionViewHolder(inflater.inflate(R.layout.item_nudge_section_label, parent, false));
				default -> new PartnerViewHolder(inflater.inflate(R.layout.item_nudge_partner, parent, false));
			};
		}

		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			ListItem item = items.get(position);
			if (holder instanceof HeaderViewHolder h) h.bind();
			else if (holder instanceof SectionViewHolder s) s.bind(item.label);
			else if (holder instanceof PartnerViewHolder p) p.bind(item.partner);
		}

		@Override
		public int getItemCount() {
			return items.size();
		}
	}

	private class HeaderViewHolder extends RecyclerView.ViewHolder {
		private final TextView grandTotal;
		private final TextView pendingHint;
		private final ImageView waveIcon;

		HeaderViewHolder(View view) {
			super(view);
			grandTotal = view.findViewById(R.id.grand_total);
			pendingHint = view.findViewById(R.id.pending_hint);
			waveIcon = view.findViewById(R.id.wave_icon);
			waveIcon.setImageTintList(ColorStateList.valueOf(
					UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary)));
		}

		void bind() {
			if (data == null) return;
			int total = data.grand_total;
			grandTotal.setText(getResources().getQuantityString(R.plurals.nudge_grand_total, total, total));
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
		void bind(String text) {
			label.setText(text);
		}
	}

	private class PartnerViewHolder extends RecyclerView.ViewHolder {
		private final ImageView avatar;
		private final ImageView waveIcon;
		private final TextView displayName;
		private final TextView username;
		private final TextView streakLabel;
		private final Button nudgeBtn;
		private NudgePartner currentPartner;
		private boolean nudgeSent;

		PartnerViewHolder(View view) {
			super(view);
			avatar = view.findViewById(R.id.avatar);
			waveIcon = view.findViewById(R.id.wave_icon);
			displayName = view.findViewById(R.id.display_name);
			username = view.findViewById(R.id.username);
			streakLabel = view.findViewById(R.id.streak_label);
			nudgeBtn = view.findViewById(R.id.nudge_btn);

			avatar.setOutlineProvider(OutlineProviders.roundedRect(8));
			avatar.setClipToOutline(true);

			waveIcon.setImageTintList(ColorStateList.valueOf(
					UiUtils.getThemeColor(view.getContext(), R.attr.colorM3Primary)));

			avatar.setOnClickListener(v -> openProfile());
			displayName.setOnClickListener(v -> openProfile());
			nudgeBtn.setOnClickListener(v -> onNudgeClick());
		}

		void bind(NudgePartner partner) {
			currentPartner = partner;
			nudgeSent = false;

			Account acc = partner.account;
			if (acc != null) {
				String name = acc.displayName.isEmpty() ? acc.username : acc.displayName;
				displayName.setText(name);
				username.setText("@" + acc.acct);
				String url = GlobalUserPreferences.playGifs ? acc.avatar : acc.avatarStatic;
				ViewImageLoader.load(avatar, null, new UrlImageLoaderRequest(url, V.dp(46), V.dp(46)));
			} else {
				displayName.setText(partner.account_id);
				username.setText("");
				avatar.setImageResource(R.drawable.image_placeholder);
			}

			int total = partner.sent_count + partner.received_count;
			streakLabel.setText(getResources().getString(R.string.nudge_streak, total));

			updateButton();
		}

		private void updateButton() {
			if (nudgeSent) {
				nudgeBtn.setText(R.string.nudged);
				nudgeBtn.setEnabled(false);
				nudgeBtn.setAlpha(0.5f);
			} else if (currentPartner.can_nudge_back) {
				nudgeBtn.setText(R.string.nudge_back);
				nudgeBtn.setEnabled(true);
				nudgeBtn.setAlpha(1f);
			} else {
				nudgeBtn.setText(R.string.nudge_waiting);
				nudgeBtn.setEnabled(false);
				nudgeBtn.setAlpha(0.45f);
			}
		}

		private void onNudgeClick() {
			if (currentPartner == null || nudgeSent) return;
			Account acc = currentPartner.account;
			if (acc == null) return;

			nudgeBtn.setEnabled(false);
			new SendNudge(acc.id)
					.setCallback(new Callback<NudgeResult>() {
						@Override
						public void onSuccess(NudgeResult result) {
							if (getActivity() == null) return;
							nudgeSent = true;
							currentPartner.can_nudge_back = false;
							if (result.streak > 0) currentPartner.streak = result.streak;
							currentPartner.sent_count++;
							updateButton();
							int total = currentPartner.sent_count + currentPartner.received_count;
							streakLabel.setText(getResources().getString(R.string.nudge_streak, total));
							if (data != null) {
								data.grand_total++;
								int hPos = -1;
								for (int i = 0; i < items.size(); i++) {
									if (items.get(i).type == TYPE_HEADER) { hPos = i; break; }
								}
								if (hPos >= 0) adapter.notifyItemChanged(hPos);
							}
						}

						@Override
						public void onError(ErrorResponse error) {
							if (getActivity() == null) return;
							if (error instanceof org.joinmastodon.android.api.MastodonErrorResponse mr && mr.httpStatus == 422) {
								nudgeSent = true;
								nudgeBtn.setText(R.string.nudge_waiting);
								nudgeBtn.setEnabled(false);
								nudgeBtn.setAlpha(0.45f);
							} else {
								nudgeBtn.setEnabled(true);
								error.showToast(getActivity());
							}
						}
					})
					.exec(accountID);
		}

		private void openProfile() {
			if (currentPartner == null || currentPartner.account == null) return;
			Bundle args = new Bundle();
			args.putString("account", accountID);
			args.putParcelable("profileAccount", org.parceler.Parcels.wrap(currentPartner.account));
			Nav.go(getActivity(), ProfileFragment.class, args);
		}
	}
}
