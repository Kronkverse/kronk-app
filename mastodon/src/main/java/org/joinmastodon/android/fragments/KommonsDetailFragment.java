package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.proposals.VoteOnProposal;
import org.joinmastodon.android.api.requests.proposals.UnvoteProposal;
import org.joinmastodon.android.model.Proposal;
import org.joinmastodon.android.ui.OutlineProviders;
import org.parceler.Parcels;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class KommonsDetailFragment extends MastodonToolbarFragment {
	private static final DateTimeFormatter DATE_FMT =
			DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault());

	private Proposal proposal;
	private String accountID;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		proposal = Parcels.unwrap(getArguments().getParcelable("proposal"));
		accountID = getArguments().getString("account");
		setTitle(proposal != null ? proposal.title : "");
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_kommons_detail, container, false);
		if (proposal != null) bind(view, proposal);
		return view;
	}

	private void bind(View v, Proposal p) {
		TextView statusBadge = v.findViewById(R.id.detail_status);
		statusBadge.setText(statusLabel(p.status));
		applyStatusColor(statusBadge, p.status);

		TextView meta = v.findViewById(R.id.detail_meta);
		String author = p.createdByAccount != null ? "@" + p.createdByAccount.username : "";
		if (p.createdAt != null) {
			meta.setText(getString(R.string.kommons_meta_by_date, author,
					DATE_FMT.format(p.createdAt)));
		} else {
			meta.setText(author);
		}

		((TextView) v.findViewById(R.id.detail_body)).setText(p.body != null ? p.body : "");

		bindVoteSection(v, p);
		bindVoters(v, p);
		bindChallenges(v, p);
	}

	private void bindVoteSection(View v, Proposal p) {
		Button btnAgree   = v.findViewById(R.id.btn_agree);
		Button btnAbstain = v.findViewById(R.id.btn_abstain);
		Button btnBlock   = v.findViewById(R.id.btn_block);

		String pos = p.currentVote != null ? p.currentVote.position : null;
		updateVoteButtons(btnAgree, btnAbstain, btnBlock, pos);
		updateTally(v, p);

		btnAgree.setOnClickListener(bv   -> castVote(v, p, "agree",   btnAgree, btnAbstain, btnBlock));
		btnAbstain.setOnClickListener(bv -> castVote(v, p, "abstain", btnAgree, btnAbstain, btnBlock));
		btnBlock.setOnClickListener(bv   -> castVote(v, p, "block",   btnAgree, btnAbstain, btnBlock));
	}

	private void castVote(View v, Proposal p, String position, Button a, Button ab, Button bl) {
		a.setEnabled(false); ab.setEnabled(false); bl.setEnabled(false);
		new VoteOnProposal(p.id, position)
				.setCallback(new Callback<Proposal>() {
					@Override public void onSuccess(Proposal updated) {
						if (getActivity() == null) return;
						proposal = updated;
						String newPos = updated.currentVote != null ? updated.currentVote.position : null;
						updateVoteButtons(a, ab, bl, newPos);
						a.setEnabled(true); ab.setEnabled(true); bl.setEnabled(true);
						updateTally(v, updated);
						bindVoters(v, updated);
					}
					@Override public void onError(ErrorResponse error) {
						if (getActivity() == null) return;
						a.setEnabled(true); ab.setEnabled(true); bl.setEnabled(true);
					}
				})
				.exec(accountID);
	}

	private void updateVoteButtons(Button agree, Button abstain, Button block, String pos) {
		agree.setSelected("agree".equals(pos));
		abstain.setSelected("abstain".equals(pos));
		block.setSelected("block".equals(pos));
	}

	private void updateTally(View v, Proposal p) {
		if (p.voteSummary == null) return;
		((TextView) v.findViewById(R.id.tally_agree)).setText(
				getResources().getQuantityString(R.plurals.kommons_agree_count, p.voteSummary.agree, p.voteSummary.agree));
		((TextView) v.findViewById(R.id.tally_abstain)).setText(
				getResources().getQuantityString(R.plurals.kommons_abstain_count, p.voteSummary.abstain, p.voteSummary.abstain));
		((TextView) v.findViewById(R.id.tally_block)).setText(
				getResources().getQuantityString(R.plurals.kommons_block_count, p.voteSummary.block, p.voteSummary.block));
	}

	private void bindVoters(View v, Proposal p) {
		LinearLayout list = v.findViewById(R.id.voters_list);
		TextView heading  = v.findViewById(R.id.voters_heading);
		list.removeAllViews();
		if (p.voters == null || p.voters.isEmpty()) { heading.setVisibility(View.GONE); return; }
		heading.setVisibility(View.VISIBLE);
		LayoutInflater inf = LayoutInflater.from(getActivity());
		for (Proposal.Voter voter : p.voters) {
			View row = inf.inflate(R.layout.item_kommons_voter, list, false);
			ImageView ava = row.findViewById(R.id.avatar);
			ava.setOutlineProvider(OutlineProviders.roundedRect(99));
			ava.setClipToOutline(true);
			if (voter.account != null && !TextUtils.isEmpty(voter.account.avatar)) {
				ViewImageLoader.loadWithoutAnimation(ava, null,
						new UrlImageLoaderRequest(voter.account.avatar, V.dp(32), V.dp(32)));
			}
			((TextView) row.findViewById(R.id.username)).setText(
					voter.account != null ? "@" + voter.account.username : "");
			TextView badge = row.findViewById(R.id.position_badge);
			badge.setText(voter.position != null ? voter.position.toUpperCase() : "");
			applyStatusColor(badge, voter.position);
			TextView stmt = row.findViewById(R.id.statement);
			if (!TextUtils.isEmpty(voter.statement)) {
				stmt.setText(voter.statement); stmt.setVisibility(View.VISIBLE);
			} else {
				stmt.setVisibility(View.GONE);
			}
			list.addView(row);
		}
	}

	private void bindChallenges(View v, Proposal p) {
		LinearLayout list = v.findViewById(R.id.challenges_list);
		TextView heading  = v.findViewById(R.id.challenges_heading);
		list.removeAllViews();
		if (p.challenges == null || p.challenges.isEmpty()) { heading.setVisibility(View.GONE); return; }
		heading.setVisibility(View.VISIBLE);
		for (Proposal.Challenge ch : p.challenges) {
			LinearLayout block = new LinearLayout(getActivity());
			block.setOrientation(LinearLayout.VERTICAL);
			block.setPadding(0, 0, 0, V.dp(12));
			if (!TextUtils.isEmpty(ch.statement)) {
				TextView stmt = new TextView(getActivity());
				stmt.setTextAppearance(getActivity(), android.R.style.TextAppearance_Material_Body1);
				stmt.setText(ch.statement);
				stmt.setPadding(0, 0, 0, V.dp(4));
				block.addView(stmt);
			}
			if (ch.conditions != null) {
				for (Proposal.Condition cond : ch.conditions) {
					TextView condTv = new TextView(getActivity());
					condTv.setTextAppearance(getActivity(), android.R.style.TextAppearance_Material_Caption);
					condTv.setText((cond.met ? "✓ " : "· ") + cond.text);
					condTv.setPadding(V.dp(8), V.dp(2), 0, 0);
					block.addView(condTv);
				}
			}
			list.addView(block);
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
		if ("vetoed".equals(status) || "block".equals(status)) color = 0xFFE53935;
		else if ("delivered".equals(status) || "agree".equals(status)) color = 0xFF43A047;
		else if ("in_progress".equals(status)) color = 0xFF1E88E5;
		else color = 0xFF36248C;
		tv.setTextColor(color);
	}
}
