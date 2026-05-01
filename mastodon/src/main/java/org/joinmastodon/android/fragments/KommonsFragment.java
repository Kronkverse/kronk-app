package org.joinmastodon.android.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.proposals.GetProposals;
import org.joinmastodon.android.api.requests.proposals.VoteOnProposal;
import org.joinmastodon.android.model.Proposal;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class KommonsFragment extends AppKitFragment{
	private static final DateTimeFormatter DATE_FMT=DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

	private String accountID;
	private List<Proposal> proposals=new ArrayList<>();
	private boolean loading;

	private View listContainer;
	private ViewGroup detailContainer;
	private RecyclerView recyclerView;
	private SwipeRefreshLayout swipeRefresh;
	private LinearLayout emptyView;
	private TextView emptyTitle;
	private TextView emptySubtitle;
	private ProposalAdapter adapter;
	private Proposal selectedProposal;
	private final Runnable detailBackCallback=this::showList;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		View rootView=inflater.inflate(R.layout.fragment_kommons, container, false);
		listContainer=rootView.findViewById(R.id.list_container);
		detailContainer=rootView.findViewById(R.id.detail_container);

		swipeRefresh=rootView.findViewById(R.id.refresh_layout);
		swipeRefresh.setOnRefreshListener(()->loadProposals(true));

		emptyView=rootView.findViewById(R.id.empty_view);
		emptyTitle=rootView.findViewById(R.id.empty_title);
		emptySubtitle=rootView.findViewById(R.id.empty_subtitle);

		recyclerView=rootView.findViewById(R.id.proposal_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
		adapter=new ProposalAdapter();
		recyclerView.setAdapter(adapter);

		accountID=getArguments()!=null ? getArguments().getString("account") : null;

		if(getArguments()==null || !getArguments().getBoolean("noAutoLoad")){
			loadProposals(false);
		}
		return rootView;
	}

	public void loadData(){
		if(!loading && proposals.isEmpty()) loadProposals(false);
	}

	private void loadProposals(boolean refresh){
		loading=true;
		if(refresh) swipeRefresh.setRefreshing(true);
		else setEmptyState(null, null);

		new GetProposals(null)
			.setCallback(new Callback<List<Proposal>>(){
				@Override
				public void onSuccess(List<Proposal> result){
					if(getActivity()==null) return;
					loading=false;
					swipeRefresh.setRefreshing(false);
					proposals.clear();
					proposals.addAll(result);
					adapter.notifyDataSetChanged();
					if(proposals.isEmpty()){
						setEmptyState(getString(R.string.kommons_empty_title), getString(R.string.kommons_empty_subtitle));
					}else{
						recyclerView.setVisibility(View.VISIBLE);
						emptyView.setVisibility(View.GONE);
					}
				}
				@Override
				public void onError(ErrorResponse error){
					if(getActivity()==null) return;
					loading=false;
					swipeRefresh.setRefreshing(false);
					setEmptyState(getString(R.string.error_loading), error.getMessage());
				}
			})
			.exec(accountID);
	}

	private void setEmptyState(String title, String sub){
		if(title==null){
			emptyView.setVisibility(View.GONE);
		}else{
			recyclerView.setVisibility(View.GONE);
			emptyView.setVisibility(View.VISIBLE);
			emptyTitle.setText(title);
			emptySubtitle.setText(sub!=null ? sub : "");
		}
	}

	private void openDetail(Proposal proposal){
		selectedProposal=proposal;
		View detail=LayoutInflater.from(getActivity()).inflate(R.layout.fragment_kommons_detail, detailContainer, false);
		bindDetail(detail, proposal);
		detailContainer.removeAllViews();
		detailContainer.addView(detail);
		listContainer.setVisibility(View.GONE);
		detailContainer.setVisibility(View.VISIBLE);
		addBackCallback(detailBackCallback);
	}

	private void showList(){
		selectedProposal=null;
		detailContainer.setVisibility(View.GONE);
		listContainer.setVisibility(View.VISIBLE);
		removeBackCallback(detailBackCallback);
	}

	private void bindDetail(View v, Proposal p){
		((TextView)v.findViewById(R.id.detail_header_title)).setText(p.title);
		v.findViewById(R.id.btn_back).setOnClickListener(bv->showList());

		TextView statusBadge=v.findViewById(R.id.detail_status);
		statusBadge.setText(statusLabel(p.status));
		applyPositionColor(statusBadge, p.status);

		TextView meta=v.findViewById(R.id.detail_meta);
		String author=p.createdByAccount!=null ? "@"+p.createdByAccount.username : "";
		if(p.createdAt!=null){
			String date=DATE_FMT.format(p.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate());
			meta.setText(getString(R.string.kommons_meta_by_date, author, date));
		}else{
			meta.setText(author);
		}

		((TextView)v.findViewById(R.id.detail_body)).setText(p.body!=null ? p.body : "");

		bindVoteSection(v, p);
		bindVoters(v, p);
		bindChallenges(v, p);
	}

	private void bindVoteSection(View v, Proposal p){
		Button btnAgree=v.findViewById(R.id.btn_agree);
		Button btnAbstain=v.findViewById(R.id.btn_abstain);
		Button btnBlock=v.findViewById(R.id.btn_block);

		String pos=p.currentVote!=null ? p.currentVote.position : null;
		updateVoteButtons(btnAgree, btnAbstain, btnBlock, pos);

		btnAgree.setOnClickListener(bv->castVote(v, p, "agree", btnAgree, btnAbstain, btnBlock));
		btnAbstain.setOnClickListener(bv->castVote(v, p, "abstain", btnAgree, btnAbstain, btnBlock));
		btnBlock.setOnClickListener(bv->castVote(v, p, "block", btnAgree, btnAbstain, btnBlock));

		updateTally(v, p);
	}

	private void castVote(View v, Proposal p, String position, Button a, Button ab, Button bl){
		a.setEnabled(false); ab.setEnabled(false); bl.setEnabled(false);
		new VoteOnProposal(p.id, position)
			.setCallback(new Callback<Proposal>(){
				@Override
				public void onSuccess(Proposal updated){
					if(getActivity()==null) return;
					for(int i=0;i<proposals.size();i++){
						if(proposals.get(i).id.equals(updated.id)){
							proposals.set(i, updated);
							adapter.notifyItemChanged(i);
							break;
						}
					}
					selectedProposal=updated;
					String newPos=updated.currentVote!=null ? updated.currentVote.position : null;
					updateVoteButtons(a, ab, bl, newPos);
					a.setEnabled(true); ab.setEnabled(true); bl.setEnabled(true);
					updateTally(v, updated);
					bindVoters(v, updated);
					bindChallenges(v, updated);
				}
				@Override
				public void onError(ErrorResponse error){
					if(getActivity()==null) return;
					a.setEnabled(true); ab.setEnabled(true); bl.setEnabled(true);
				}
			})
			.exec(accountID);
	}

	private void updateVoteButtons(Button agree, Button abstain, Button block, String pos){
		agree.setSelected("agree".equals(pos));
		abstain.setSelected("abstain".equals(pos));
		block.setSelected("block".equals(pos));
	}

	private void updateTally(View v, Proposal p){
		if(p.voteSummary==null) return;
		((TextView)v.findViewById(R.id.tally_agree)).setText(
			getResources().getQuantityString(R.plurals.kommons_agree_count, p.voteSummary.agree, p.voteSummary.agree));
		((TextView)v.findViewById(R.id.tally_abstain)).setText(
			getResources().getQuantityString(R.plurals.kommons_abstain_count, p.voteSummary.abstain, p.voteSummary.abstain));
		((TextView)v.findViewById(R.id.tally_block)).setText(
			getResources().getQuantityString(R.plurals.kommons_block_count, p.voteSummary.block, p.voteSummary.block));
	}

	private void bindVoters(View v, Proposal p){
		LinearLayout list=v.findViewById(R.id.voters_list);
		TextView heading=v.findViewById(R.id.voters_heading);
		list.removeAllViews();
		if(p.voters==null||p.voters.isEmpty()){ heading.setVisibility(View.GONE); return; }
		heading.setVisibility(View.VISIBLE);
		LayoutInflater inf=LayoutInflater.from(getActivity());
		for(Proposal.Voter voter:p.voters){
			View row=inf.inflate(R.layout.item_kommons_voter, list, false);
			ImageView ava=row.findViewById(R.id.avatar);
			if(voter.account!=null&&!TextUtils.isEmpty(voter.account.avatar)){
				ViewImageLoader.loadWithoutAnimation(ava, null,
					new UrlImageLoaderRequest(voter.account.avatar, V.dp(32), V.dp(32)));
			}
			((TextView)row.findViewById(R.id.username)).setText(
				voter.account!=null ? "@"+voter.account.username : "");
			TextView badge=row.findViewById(R.id.position_badge);
			badge.setText(voter.position!=null ? voter.position.toUpperCase() : "");
			applyPositionColor(badge, voter.position);
			TextView stmt=row.findViewById(R.id.statement);
			if(!TextUtils.isEmpty(voter.statement)){
				stmt.setText(voter.statement); stmt.setVisibility(View.VISIBLE);
			}else{
				stmt.setVisibility(View.GONE);
			}
			list.addView(row);
		}
	}

	private void bindChallenges(View v, Proposal p){
		LinearLayout list=v.findViewById(R.id.challenges_list);
		TextView heading=v.findViewById(R.id.challenges_heading);
		list.removeAllViews();
		if(p.challenges==null||p.challenges.isEmpty()){ heading.setVisibility(View.GONE); return; }
		heading.setVisibility(View.VISIBLE);
		for(Proposal.Challenge ch:p.challenges){
			LinearLayout block=new LinearLayout(getActivity());
			block.setOrientation(LinearLayout.VERTICAL);
			block.setPadding(0, 0, 0, V.dp(12));
			if(!TextUtils.isEmpty(ch.statement)){
				TextView stmt=new TextView(getActivity());
				stmt.setTextAppearance(getActivity(), com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
				stmt.setText(ch.statement);
				stmt.setPadding(0, 0, 0, V.dp(4));
				block.addView(stmt);
			}
			if(ch.conditions!=null){
				for(Proposal.Condition cond:ch.conditions){
					TextView condTv=new TextView(getActivity());
					condTv.setTextAppearance(getActivity(), com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
					condTv.setText((cond.met?"✓ ":"· ")+cond.text);
					condTv.setPadding(V.dp(8), V.dp(2), 0, 0);
					block.addView(condTv);
				}
			}
			list.addView(block);
		}
	}

	private String statusLabel(String status){
		if(status==null) return "";
		switch(status){
			case "open": return getString(R.string.kommons_status_open);
			case "in_progress": return getString(R.string.kommons_status_in_progress);
			case "vetoed": return getString(R.string.kommons_status_vetoed);
			case "delivered": return getString(R.string.kommons_status_delivered);
			default: return status.toUpperCase();
		}
	}

	private void applyPositionColor(TextView tv, String pos){
		if(pos==null) return;
		int colorRes;
		switch(pos){
			case "block": case "vetoed":
				colorRes=android.R.color.holo_red_dark; break;
			case "agree": case "delivered":
				colorRes=android.R.color.holo_green_dark; break;
			default:
				colorRes=android.R.color.darker_gray; break;
		}
		tv.setTextColor(getResources().getColor(colorRes, getActivity().getTheme()));
	}

	// ---- Adapter ----

	private class ProposalAdapter extends RecyclerView.Adapter<ProposalViewHolder>{
		@Override
		public ProposalViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
			return new ProposalViewHolder(
				LayoutInflater.from(getActivity()).inflate(R.layout.item_proposal_card, parent, false));
		}
		@Override
		public void onBindViewHolder(ProposalViewHolder h, int pos){ h.bind(proposals.get(pos)); }
		@Override
		public int getItemCount(){ return proposals.size(); }
	}

	private class ProposalViewHolder extends RecyclerView.ViewHolder{
		private final TextView statusBadge, author, title, summary, voteAgree, voteAbstain, voteBlock;

		ProposalViewHolder(View v){
			super(v);
			statusBadge=v.findViewById(R.id.status_badge);
			author=v.findViewById(R.id.author);
			title=v.findViewById(R.id.title);
			summary=v.findViewById(R.id.summary);
			voteAgree=v.findViewById(R.id.vote_agree);
			voteAbstain=v.findViewById(R.id.vote_abstain);
			voteBlock=v.findViewById(R.id.vote_block);
			v.setOnClickListener(bv->openDetail(proposals.get(getAdapterPosition())));
		}

		void bind(Proposal p){
			statusBadge.setText(statusLabel(p.status));
			applyPositionColor(statusBadge, p.status);
			title.setText(p.title);
			if(p.createdByAccount!=null) author.setText("@"+p.createdByAccount.username);
			if(!TextUtils.isEmpty(p.summary)){
				summary.setText(p.summary); summary.setVisibility(View.VISIBLE);
			}else{
				summary.setVisibility(View.GONE);
			}
			if(p.voteSummary!=null){
				voteAgree.setText(p.voteSummary.agree+" agree");
				voteAbstain.setText(p.voteSummary.abstain+" abstain");
				voteBlock.setText(p.voteSummary.block+" block");
			}
		}
	}
}
