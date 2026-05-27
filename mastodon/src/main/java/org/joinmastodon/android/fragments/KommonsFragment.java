package org.joinmastodon.android.fragments;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.proposals.GetProposals;
import org.joinmastodon.android.api.requests.proposals.UnvoteProposal;
import org.joinmastodon.android.api.requests.proposals.VoteOnProposal;
import org.joinmastodon.android.model.Proposal;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class KommonsFragment extends AppKitFragment {

	// Set by CreateProposalFragment on success so we reload when returning
	public static boolean sNeedsReload = false;

	private static final int COLOR_KOMMONS = Color.parseColor("#36248C");
	private static final int COLOR_FAN_ACTIVE = Color.parseColor("#36248C");

	private static final int VT_HEADER   = 0;
	private static final int VT_PROPOSAL = 1;
	private static final int VT_SECTION  = 2;

	private String accountID;
	private List<Object> items = new ArrayList<>(); // HeaderSentinel | Proposal | SectionSentinel
	private boolean loading = false;
	private boolean loaded = false;

	private View root;
	private RecyclerView recyclerView;
	private SwipeRefreshLayout swipeRefresh;
	private TextView emptyText;
	private KommonsAdapter adapter;

	// Sentinels for special list positions
	private static final Object HEADER = new Object();
	private static final Object ARCHIVED_SECTION = new Object();

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
		root = inflater.inflate(R.layout.fragment_kommons, container, false);
		recyclerView = root.findViewById(R.id.proposal_list);
		swipeRefresh = root.findViewById(R.id.refresh_layout);
		emptyText = root.findViewById(R.id.empty_subtitle);

		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
		adapter = new KommonsAdapter();
		recyclerView.setAdapter(adapter);
		// suppress default divider — cards provide their own spacing
		swipeRefresh.setOnRefreshListener(() -> loadProposals(true));

		accountID = getArguments() != null ? getArguments().getString("account") : null;

		rebuildItems(new ArrayList<>());
		adapter.notifyDataSetChanged();

		if (!loading && !loaded) loadProposals(false);
		return root;
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		if (!hidden && sNeedsReload) {
			sNeedsReload = false;
			loadProposals(true);
		}
	}

	public void loadData() {
		if (!loading && !loaded) loadProposals(false);
	}

	public void onApplyWindowInsets(WindowInsets insets) {
		if (root != null) {
			root.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
		}
	}

	private void loadProposals(boolean refresh) {
		loading = true;
		if (refresh) swipeRefresh.setRefreshing(true);

		new GetProposals(null)
				.setCallback(new Callback<List<Proposal>>() {
					@Override
					public void onSuccess(List<Proposal> result) {
						if (getActivity() == null) return;
						loading = false;
						loaded = true;
						swipeRefresh.setRefreshing(false);
						rebuildItems(result);
						adapter.notifyDataSetChanged();
						emptyText.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
					}
					@Override
					public void onError(ErrorResponse error) {
						if (getActivity() == null) return;
						loading = false;
						swipeRefresh.setRefreshing(false);
					}
				})
				.exec(accountID);
	}

	private void rebuildItems(List<Proposal> proposals) {
		items.clear();
		items.add(HEADER);

		List<Proposal> active   = new ArrayList<>();
		List<Proposal> archived = new ArrayList<>();
		for (Proposal p : proposals) {
			if (p.archivedAt != null) archived.add(p); else active.add(p);
		}
		items.addAll(active);
		if (!archived.isEmpty()) {
			items.add(ARCHIVED_SECTION);
			items.addAll(archived);
		}
	}

	private void updateProposal(Proposal updated) {
		for (int i = 0; i < items.size(); i++) {
			if (items.get(i) instanceof Proposal p && p.id.equals(updated.id)) {
				items.set(i, updated);
				adapter.notifyItemChanged(i);
				break;
			}
		}
	}

	private void openCreateProposal() {
		Bundle args = new Bundle();
		args.putString("account", accountID);
		Nav.go(getActivity(), CreateProposalFragment.class, args);
	}

	private void openDetail(Proposal proposal) {
		Bundle args = new Bundle();
		args.putString("account", accountID);
		args.putParcelable("proposal", Parcels.wrap(proposal));
		Nav.go(getActivity(), KommonsDetailFragment.class, args);
	}

	// ---- Adapter ----

	private class KommonsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		@Override
		public int getItemViewType(int pos) {
			Object item = items.get(pos);
			if (item == HEADER) return VT_HEADER;
			if (item == ARCHIVED_SECTION) return VT_SECTION;
			return VT_PROPOSAL;
		}

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inf = LayoutInflater.from(getActivity());
			if (viewType == VT_HEADER) {
				return new HeaderHolder(inf.inflate(R.layout.item_kommons_header, parent, false));
			} else if (viewType == VT_SECTION) {
				return new SectionHolder(inf.inflate(R.layout.item_kommons_section, parent, false));
			} else {
				return new ProposalHolder(inf.inflate(R.layout.item_proposal_card, parent, false));
			}
		}

		@Override
		public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
			if (h instanceof ProposalHolder ph) ph.bind((Proposal) items.get(pos));
			else if (h instanceof SectionHolder sh) sh.bind();
			// HeaderHolder binds itself in constructor
		}

		@Override
		public int getItemCount() { return items.size(); }
	}

	// ---- View Holders ----

	private class HeaderHolder extends RecyclerView.ViewHolder {
		HeaderHolder(View v) {
			super(v);
			v.findViewById(R.id.btn_plant_seed).setOnClickListener(bv -> openCreateProposal());
		}
	}

	private class SectionHolder extends RecyclerView.ViewHolder {
		private final TextView label;
		SectionHolder(View v) {
			super(v);
			label = v.findViewById(R.id.section_label);
		}
		void bind() { label.setText(R.string.kommons_archived_section); }
	}

	private class ProposalHolder extends RecyclerView.ViewHolder {
		private final TextView statusBadge;
		private final ImageView archivedIcon;
		private final TextView title;
		private final TextView bodyPreview;
		private final ImageView authorAvatar;
		private final TextView authorName;
		private final TextView timeAgo;
		private final TextView fanCount;
		private final ImageButton btnFan;
		private final View voteStrip;

		ProposalHolder(View v) {
			super(v);
			statusBadge  = v.findViewById(R.id.status_badge);
			archivedIcon = v.findViewById(R.id.archived_icon);
			title        = v.findViewById(R.id.title);
			bodyPreview  = v.findViewById(R.id.body_preview);
			authorAvatar = v.findViewById(R.id.author_avatar);
			authorName   = v.findViewById(R.id.author_name);
			timeAgo      = v.findViewById(R.id.time_ago);
			fanCount     = v.findViewById(R.id.fan_count);
			btnFan       = v.findViewById(R.id.btn_fan);
			voteStrip    = v.findViewById(R.id.vote_strip);

			authorAvatar.setOutlineProvider(
					org.joinmastodon.android.ui.OutlineProviders.roundedRect(99));
			authorAvatar.setClipToOutline(true);

			v.setOnClickListener(bv -> {
				int pos = getBindingAdapterPosition();
				if (pos == RecyclerView.NO_POSITION || !(items.get(pos) instanceof Proposal)) return;
				openDetail((Proposal) items.get(pos));
			});
		}

		void bind(Proposal p) {
			boolean isArchived = p.archivedAt != null;

			// Status badge
			statusBadge.setText(statusLabel(p.status));
			applyStatusColor(statusBadge, p.status);
			archivedIcon.setVisibility(isArchived ? View.VISIBLE : View.GONE);

			// Title
			title.setText(p.title);
			float alpha = isArchived ? 0.45f : 1f;
			title.setAlpha(alpha);

			// Body preview — strip [New Space] / meta prefix tags
			String rawBody = p.body != null ? p.body : "";
			String stripped = rawBody.replaceAll("^(\\[.*?]\\s*\\n?)+", "").trim();
			if (!TextUtils.isEmpty(stripped)) {
				String preview = stripped.length() > 160 ? stripped.substring(0, 157) + "…" : stripped;
				bodyPreview.setText(preview);
				bodyPreview.setVisibility(View.VISIBLE);
				bodyPreview.setAlpha(alpha);
			} else {
				bodyPreview.setVisibility(View.GONE);
			}

			// Author
			if (p.createdByAccount != null) {
				authorName.setText("@" + p.createdByAccount.username);
				String avatarUrl = p.createdByAccount.avatarStatic != null
						? p.createdByAccount.avatarStatic : p.createdByAccount.avatar;
				if (avatarUrl != null) {
					ViewImageLoader.load(authorAvatar, null,
							new UrlImageLoaderRequest(avatarUrl, V.dp(16), V.dp(16)));
				}
			}

			// Relative time
			if (p.createdAt != null) {
				long millis = p.createdAt.toEpochMilli();
				timeAgo.setText(DateUtils.getRelativeTimeSpanString(
						millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
						DateUtils.FORMAT_ABBREV_RELATIVE));
			}

			// Fan count + button state
			int agreeCount = p.voteSummary != null ? p.voteSummary.agree : 0;
			fanCount.setText(String.valueOf(agreeCount));
			boolean isFanned = p.currentVote != null && "agree".equals(p.currentVote.position);
			int fanTint = isFanned ? COLOR_FAN_ACTIVE : 0xFF888888;
			btnFan.setImageTintList(android.content.res.ColorStateList.valueOf(fanTint));
			btnFan.setOnClickListener(v -> toggleFan(p));

			// Vote strip: color bar proportional to agree
			updateVoteStrip(p);
		}

		private void updateVoteStrip(Proposal p) {
			if (p.voteSummary == null) {
				voteStrip.setBackgroundColor(COLOR_KOMMONS);
				return;
			}
			int agree   = p.voteSummary.agree;
			int abstain = p.voteSummary.abstain;
			int block   = p.voteSummary.block;
			int total   = agree + abstain + block;
			if (total == 0) {
				voteStrip.setBackgroundColor(COLOR_KOMMONS);
				return;
			}
			// Gradient: agree=green, abstain=gray, block=red (top→bottom)
			float agreeEnd   = (float) agree / total;
			float abstainEnd = agreeEnd + (float) abstain / total;
			GradientDrawable gd = new GradientDrawable(
					GradientDrawable.Orientation.TOP_BOTTOM,
					new int[]{0xFF4CAF50, 0xFF4CAF50, 0xFF9E9E9E, 0xFF9E9E9E, 0xFFF44336, 0xFFF44336});
			// Approximate proportional strip using a solid color weighted toward majority
			int color;
			if (agree >= abstain && agree >= block) color = 0xFF4CAF50;
			else if (block >= abstain) color = 0xFFF44336;
			else color = 0xFF9E9E9E;
			voteStrip.setBackgroundColor(color);
		}
	}

	private void toggleFan(Proposal p) {
		boolean isFanned = p.currentVote != null && "agree".equals(p.currentVote.position);
		if (isFanned) {
			new UnvoteProposal(p.id)
					.setCallback(new Callback<Proposal>() {
						@Override public void onSuccess(Proposal updated) {
							if (getActivity() != null) updateProposal(updated);
						}
						@Override public void onError(ErrorResponse error) {}
					})
					.exec(accountID);
		} else {
			new VoteOnProposal(p.id, "agree")
					.setCallback(new Callback<Proposal>() {
						@Override public void onSuccess(Proposal updated) {
							if (getActivity() != null) updateProposal(updated);
						}
						@Override public void onError(ErrorResponse error) {}
					})
					.exec(accountID);
		}
	}

	private String statusLabel(String status) {
		if (status == null) return "";
		switch (status) {
			case "open":        return getString(R.string.kommons_status_open);
			case "in_progress": return getString(R.string.kommons_status_in_progress);
			case "vetoed":      return getString(R.string.kommons_status_vetoed);
			case "delivered":   return getString(R.string.kommons_status_delivered);
			default:            return status.toUpperCase();
		}
	}

	private void applyStatusColor(TextView tv, String status) {
		int color;
		if ("vetoed".equals(status))    color = 0xFFE53935;
		else if ("delivered".equals(status)) color = 0xFF43A047;
		else if ("in_progress".equals(status)) color = 0xFF1E88E5;
		else color = COLOR_KOMMONS; // open
		tv.setTextColor(color);
	}
}
