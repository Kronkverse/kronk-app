package org.joinmastodon.android.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.proposals.ArchiveProposal;
import org.joinmastodon.android.api.requests.proposals.CreateProposal;
import org.joinmastodon.android.api.requests.proposals.GetProposals;
import org.joinmastodon.android.api.requests.proposals.MarkProposalDelivered;
import org.joinmastodon.android.api.requests.proposals.UnarchiveProposal;
import org.joinmastodon.android.api.requests.proposals.UnvoteProposal;
import org.joinmastodon.android.api.requests.proposals.UpdateProposal;
import org.joinmastodon.android.api.requests.proposals.VoteOnProposal;
import org.joinmastodon.android.api.session.AccountSessionManager;
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
	private String selfAccountId;
	private List<Proposal> proposals=new ArrayList<>();
	private List<Proposal> forest=new ArrayList<>();
	private boolean loading;

	private View listContainer;
	private ViewGroup detailContainer;
	private ViewGroup forestContainer;
	private RecyclerView recyclerView;
	private SwipeRefreshLayout swipeRefresh;
	private LinearLayout emptyView;
	private TextView emptyTitle;
	private TextView emptySubtitle;
	private ProposalAdapter adapter;
	private ForestAdapter forestAdapter;
	private Proposal selectedProposal;
	private final Runnable detailBackCallback=this::showList;
	private final Runnable forestBackCallback=this::hideForest;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		View rootView=inflater.inflate(R.layout.fragment_kommons, container, false);
		listContainer=rootView.findViewById(R.id.list_container);
		detailContainer=rootView.findViewById(R.id.detail_container);
		forestContainer=rootView.findViewById(R.id.forest_container);

		swipeRefresh=rootView.findViewById(R.id.refresh_layout);
		swipeRefresh.setOnRefreshListener(()->loadProposals(true));

		emptyView=rootView.findViewById(R.id.empty_view);
		emptyTitle=rootView.findViewById(R.id.empty_title);
		emptySubtitle=rootView.findViewById(R.id.empty_subtitle);

		recyclerView=rootView.findViewById(R.id.proposal_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
		adapter=new ProposalAdapter();
		recyclerView.setAdapter(adapter);

		rootView.findViewById(R.id.btn_plant_seed).setOnClickListener(v->showPlantSeedDialog());
		rootView.findViewById(R.id.btn_forest).setOnClickListener(v->showForest());

		accountID=getArguments()!=null ? getArguments().getString("account") : null;
		selfAccountId=AccountSessionManager.get(accountID).self.id;

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
					// active proposals first, archived at bottom
					for(Proposal p:result) if(p.archivedAt==null) proposals.add(p);
					for(Proposal p:result) if(p.archivedAt!=null) proposals.add(p);
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
					setEmptyState(getString(R.string.error_loading), null);
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

	private void showForest(){
		View forestView=LayoutInflater.from(getActivity()).inflate(R.layout.fragment_kommons_forest, forestContainer, false);
		forestContainer.removeAllViews();
		forestContainer.addView(forestView);
		listContainer.setVisibility(View.GONE);
		forestContainer.setVisibility(View.VISIBLE);
		addBackCallback(forestBackCallback);
		forestView.findViewById(R.id.forest_btn_back).setOnClickListener(v->hideForest());

		RecyclerView forestRv=forestView.findViewById(R.id.forest_list);
		forestRv.setLayoutManager(new LinearLayoutManager(getActivity()));

		new GetProposals("delivered")
			.setCallback(new Callback<List<Proposal>>(){
				@Override
				public void onSuccess(List<Proposal> result){
					if(getActivity()==null) return;
					forest.clear();
					forest.addAll(result);
					forestAdapter=new ForestAdapter();
					forestRv.setAdapter(forestAdapter);
				}
				@Override
				public void onError(ErrorResponse error){}
			})
			.exec(accountID);
	}

	private void hideForest(){
		forestContainer.setVisibility(View.GONE);
		listContainer.setVisibility(View.VISIBLE);
		removeBackCallback(forestBackCallback);
	}

	private void bindDetail(View v, Proposal p){
		((TextView)v.findViewById(R.id.detail_header_title)).setText(p.title);
		v.findViewById(R.id.btn_back).setOnClickListener(bv->showList());

		TextView statusBadge=v.findViewById(R.id.detail_status);
		statusBadge.setText(statusLabel(p.status));
		applyStatusColor(statusBadge, p.status);

		TextView meta=v.findViewById(R.id.detail_meta);
		String author=p.createdByAccount!=null ? "@"+p.createdByAccount.username : "";
		if(p.createdAt!=null){
			String date=DATE_FMT.format(p.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate());
			meta.setText(getString(R.string.kommons_meta_by_date, author, date));
		}else{
			meta.setText(author);
		}

		((TextView)v.findViewById(R.id.detail_body)).setText(p.body!=null ? p.body : "");

		boolean isSeeder=p.createdByAccount!=null && p.createdByAccount.id.equals(selfAccountId);
		bindSeederActions(v, p, isSeeder);
		bindVoteSection(v, p);
		bindVoters(v, p);
		bindChallenges(v, p);
		bindMatureButton(v, p, isSeeder);
	}

	private void bindSeederActions(View v, Proposal p, boolean isSeeder){
		LinearLayout seederActions=v.findViewById(R.id.seeder_actions);
		Button btnEdit=v.findViewById(R.id.btn_edit);
		Button btnArchive=v.findViewById(R.id.btn_archive);
		Button btnUnarchive=v.findViewById(R.id.btn_unarchive);

		if(!isSeeder){
			seederActions.setVisibility(View.GONE);
			return;
		}
		seederActions.setVisibility(View.VISIBLE);

		if(p.archivedAt!=null){
			btnEdit.setVisibility(View.GONE);
			btnArchive.setVisibility(View.GONE);
			btnUnarchive.setVisibility(View.VISIBLE);
		}else{
			btnEdit.setVisibility(View.VISIBLE);
			btnArchive.setVisibility(View.VISIBLE);
			btnUnarchive.setVisibility(View.GONE);
		}

		btnEdit.setOnClickListener(bv->showEditDialog(p, v));
		btnArchive.setOnClickListener(bv->doArchive(p, v));
		btnUnarchive.setOnClickListener(bv->doUnarchive(p, v));
	}

	private void showEditDialog(Proposal p, View detailView){
		View dialogView=LayoutInflater.from(getActivity()).inflate(android.R.layout.simple_list_item_2, null, false);
		// Use a simple two-field AlertDialog
		LinearLayout fields=new LinearLayout(getActivity());
		fields.setOrientation(LinearLayout.VERTICAL);
		int pad=V.dp(16);
		fields.setPadding(pad, pad, pad, 0);

		EditText titleField=new EditText(getActivity());
		titleField.setHint(getString(R.string.kommons_title_hint));
		titleField.setText(p.title);
		titleField.setSingleLine(true);
		fields.addView(titleField);

		EditText bodyField=new EditText(getActivity());
		bodyField.setHint(getString(R.string.kommons_body_hint));
		bodyField.setText(p.body!=null ? p.body : "");
		bodyField.setMinLines(3);
		fields.addView(bodyField);

		new AlertDialog.Builder(getActivity())
			.setTitle(R.string.kommons_edit)
			.setView(fields)
			.setPositiveButton(android.R.string.ok, (d, w)->{
				String newTitle=titleField.getText().toString().trim();
				String newBody=bodyField.getText().toString().trim();
				if(TextUtils.isEmpty(newTitle)) return;
				new UpdateProposal(p.id, newTitle, newBody)
					.setCallback(new Callback<Proposal>(){
						@Override
						public void onSuccess(Proposal updated){
							if(getActivity()==null) return;
							updateProposalInList(updated);
							selectedProposal=updated;
							bindDetail(detailView, updated);
						}
						@Override
						public void onError(ErrorResponse error){}
					})
					.exec(accountID);
			})
			.setNegativeButton(android.R.string.cancel, null)
			.show();
	}

	private void showPlantSeedDialog(){
		LinearLayout fields=new LinearLayout(getActivity());
		fields.setOrientation(LinearLayout.VERTICAL);
		int pad=V.dp(16);
		fields.setPadding(pad, pad, pad, 0);

		EditText titleField=new EditText(getActivity());
		titleField.setHint(getString(R.string.kommons_title_hint));
		titleField.setSingleLine(true);
		fields.addView(titleField);

		EditText bodyField=new EditText(getActivity());
		bodyField.setHint(getString(R.string.kommons_body_hint));
		bodyField.setMinLines(3);
		fields.addView(bodyField);

		new AlertDialog.Builder(getActivity())
			.setTitle(R.string.kommons_new_seed_title)
			.setView(fields)
			.setPositiveButton(android.R.string.ok, (d, w)->{
				String title=titleField.getText().toString().trim();
				String body=bodyField.getText().toString().trim();
				if(TextUtils.isEmpty(title)) return;
				new CreateProposal(title, body)
					.setCallback(new Callback<Proposal>(){
						@Override
						public void onSuccess(Proposal created){
							if(getActivity()==null) return;
							proposals.add(0, created);
							adapter.notifyItemInserted(0);
							recyclerView.scrollToPosition(0);
							recyclerView.setVisibility(View.VISIBLE);
							emptyView.setVisibility(View.GONE);
						}
						@Override
						public void onError(ErrorResponse error){}
					})
					.exec(accountID);
			})
			.setNegativeButton(android.R.string.cancel, null)
			.show();
	}

	private void doArchive(Proposal p, View detailView){
		new ArchiveProposal(p.id)
			.setCallback(new Callback<Proposal>(){
				@Override
				public void onSuccess(Proposal updated){
					if(getActivity()==null) return;
					updateProposalInList(updated);
					showList();
				}
				@Override
				public void onError(ErrorResponse error){}
			})
			.exec(accountID);
	}

	private void doUnarchive(Proposal p, View detailView){
		new UnarchiveProposal(p.id)
			.setCallback(new Callback<Proposal>(){
				@Override
				public void onSuccess(Proposal updated){
					if(getActivity()==null) return;
					updateProposalInList(updated);
					selectedProposal=updated;
					bindDetail(detailView, updated);
				}
				@Override
				public void onError(ErrorResponse error){}
			})
			.exec(accountID);
	}

	private void bindMatureButton(View v, Proposal p, boolean isSeeder){
		Button btnMature=v.findViewById(R.id.btn_mature);
		if(isSeeder && !"delivered".equals(p.status) && p.archivedAt==null){
			btnMature.setVisibility(View.VISIBLE);
			btnMature.setOnClickListener(bv->{
				btnMature.setEnabled(false);
				new MarkProposalDelivered(p.id)
					.setCallback(new Callback<Proposal>(){
						@Override
						public void onSuccess(Proposal updated){
							if(getActivity()==null) return;
							updateProposalInList(updated);
							selectedProposal=updated;
							bindDetail(v, updated);
						}
						@Override
						public void onError(ErrorResponse error){
							if(getActivity()==null) return;
							btnMature.setEnabled(true);
						}
					})
					.exec(accountID);
			});
		}else{
			btnMature.setVisibility(View.GONE);
		}
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
					updateProposalInList(updated);
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
				stmt.setTextAppearance(getActivity(), android.R.style.TextAppearance_Material_Body1);
				stmt.setText(ch.statement);
				stmt.setPadding(0, 0, 0, V.dp(4));
				block.addView(stmt);
			}
			if(ch.conditions!=null){
				for(Proposal.Condition cond:ch.conditions){
					TextView condTv=new TextView(getActivity());
					condTv.setTextAppearance(getActivity(), android.R.style.TextAppearance_Material_Caption);
					condTv.setText((cond.met?"✓ ":"· ")+cond.text);
					condTv.setPadding(V.dp(8), V.dp(2), 0, 0);
					block.addView(condTv);
				}
			}
			list.addView(block);
		}
	}

	private void updateProposalInList(Proposal updated){
		for(int i=0;i<proposals.size();i++){
			if(proposals.get(i).id.equals(updated.id)){
				proposals.set(i, updated);
				adapter.notifyItemChanged(i);
				return;
			}
		}
	}

	private String statusLabel(String status){
		if(status==null) return "";
		switch(status){
			case "open": return getString(R.string.kommons_status_open);
			case "in_progress": return getString(R.string.kommons_status_in_progress);
			case "vetoed": return getString(R.string.kommons_status_vetoed);
			case "delivered": return getString(R.string.kommons_status_matured);
			default: return status.toUpperCase();
		}
	}

	private void applyStatusColor(TextView tv, String status){
		if(status==null) return;
		int colorRes;
		switch(status){
			case "vetoed":
				colorRes=android.R.color.holo_red_dark; break;
			case "delivered":
				colorRes=android.R.color.holo_green_dark; break;
			default:
				colorRes=android.R.color.darker_gray; break;
		}
		tv.setTextColor(getResources().getColor(colorRes, getActivity().getTheme()));
	}

	private void applyPositionColor(TextView tv, String pos){
		if(pos==null) return;
		int colorRes;
		switch(pos){
			case "block":
				colorRes=android.R.color.holo_red_dark; break;
			case "agree":
				colorRes=android.R.color.holo_green_dark; break;
			default:
				colorRes=android.R.color.darker_gray; break;
		}
		tv.setTextColor(getResources().getColor(colorRes, getActivity().getTheme()));
	}

	// ---- Main list adapter ----

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
		private final TextView fanCount, statusBadge, author, title, summary;
		private final ImageButton fanBtn;

		ProposalViewHolder(View v){
			super(v);
			fanCount=v.findViewById(R.id.fan_count);
			fanBtn=v.findViewById(R.id.fan_btn);
			statusBadge=v.findViewById(R.id.status_badge);
			author=v.findViewById(R.id.author);
			title=v.findViewById(R.id.title);
			summary=v.findViewById(R.id.summary);
			v.setOnClickListener(bv->{
				int pos=getAdapterPosition();
				if(pos!=RecyclerView.NO_POSITION) openDetail(proposals.get(pos));
			});
		}

		void bind(Proposal p){
			boolean archived=p.archivedAt!=null;
			itemView.setAlpha(archived ? 0.45f : 1f);

			int agreeCount=p.voteSummary!=null ? p.voteSummary.agree : 0;
			fanCount.setText(String.valueOf(agreeCount));

			boolean isFanned=p.currentVote!=null && "agree".equals(p.currentVote.position);
			fanBtn.setSelected(isFanned);
			int fannedColor=getResources().getColor(android.R.color.holo_purple, itemView.getContext().getTheme());
			int unfannedColor=getResources().getColor(android.R.color.darker_gray, itemView.getContext().getTheme());
			fanBtn.setColorFilter(isFanned ? fannedColor : unfannedColor);
			fanBtn.setOnClickListener(bv->{
				bv.setEnabled(false);
				int pos=getAdapterPosition();
				if(pos==RecyclerView.NO_POSITION){ bv.setEnabled(true); return; }
				Proposal current=proposals.get(pos);
				boolean wasFanned=current.currentVote!=null && "agree".equals(current.currentVote.position);
				if(wasFanned){
					new UnvoteProposal(current.id)
						.setCallback(new Callback<Proposal>(){
							@Override
							public void onSuccess(Proposal updated){
								if(getActivity()==null) return;
								bv.setEnabled(true);
								updateProposalInList(updated);
							}
							@Override
							public void onError(ErrorResponse error){
								if(getActivity()==null) return;
								bv.setEnabled(true);
							}
						})
						.exec(accountID);
				}else{
					new VoteOnProposal(current.id, "agree")
						.setCallback(new Callback<Proposal>(){
							@Override
							public void onSuccess(Proposal updated){
								if(getActivity()==null) return;
								bv.setEnabled(true);
								updateProposalInList(updated);
							}
							@Override
							public void onError(ErrorResponse error){
								if(getActivity()==null) return;
								bv.setEnabled(true);
							}
						})
						.exec(accountID);
				}
			});

			statusBadge.setText(statusLabel(p.status));
			applyStatusColor(statusBadge, p.status);
			title.setText(p.title);
			if(p.createdByAccount!=null) author.setText("@"+p.createdByAccount.username);
			if(!TextUtils.isEmpty(p.summary)){
				summary.setText(p.summary); summary.setVisibility(View.VISIBLE);
			}else{
				summary.setVisibility(View.GONE);
			}
		}
	}

	// ---- Forest adapter ----

	private class ForestAdapter extends RecyclerView.Adapter<ForestViewHolder>{
		@Override
		public ForestViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
			return new ForestViewHolder(
				LayoutInflater.from(getActivity()).inflate(R.layout.item_proposal_card, parent, false));
		}
		@Override
		public void onBindViewHolder(ForestViewHolder h, int pos){ h.bind(forest.get(pos)); }
		@Override
		public int getItemCount(){ return forest.size(); }
	}

	private class ForestViewHolder extends RecyclerView.ViewHolder{
		private final TextView fanCount, statusBadge, author, title, summary;
		private final ImageButton fanBtn;

		ForestViewHolder(View v){
			super(v);
			fanCount=v.findViewById(R.id.fan_count);
			fanBtn=v.findViewById(R.id.fan_btn);
			statusBadge=v.findViewById(R.id.status_badge);
			author=v.findViewById(R.id.author);
			title=v.findViewById(R.id.title);
			summary=v.findViewById(R.id.summary);
			// forest cards open detail (back goes back to forest, not list)
			v.setOnClickListener(bv->{
				int pos=getAdapterPosition();
				if(pos!=RecyclerView.NO_POSITION) openDetail(forest.get(pos));
			});
		}

		void bind(Proposal p){
			int agreeCount=p.voteSummary!=null ? p.voteSummary.agree : 0;
			fanCount.setText(String.valueOf(agreeCount));
			boolean isFanned=p.currentVote!=null && "agree".equals(p.currentVote.position);
			fanBtn.setSelected(isFanned);
			int fannedColor=getResources().getColor(android.R.color.holo_purple, itemView.getContext().getTheme());
			int unfannedColor=getResources().getColor(android.R.color.darker_gray, itemView.getContext().getTheme());
			fanBtn.setColorFilter(isFanned ? fannedColor : unfannedColor);
			fanBtn.setOnClickListener(bv->{
				bv.setEnabled(false);
				int pos=getAdapterPosition();
				if(pos==RecyclerView.NO_POSITION){ bv.setEnabled(true); return; }
				Proposal current=forest.get(pos);
				boolean wasFanned=current.currentVote!=null && "agree".equals(current.currentVote.position);
				if(wasFanned){
					new UnvoteProposal(current.id)
						.setCallback(new Callback<Proposal>(){
							@Override
							public void onSuccess(Proposal updated){
								if(getActivity()==null) return;
								bv.setEnabled(true);
								for(int i=0;i<forest.size();i++){
									if(forest.get(i).id.equals(updated.id)){
										forest.set(i, updated);
										forestAdapter.notifyItemChanged(i);
										break;
									}
								}
							}
							@Override
							public void onError(ErrorResponse error){
								if(getActivity()==null) return;
								bv.setEnabled(true);
							}
						})
						.exec(accountID);
				}else{
					new VoteOnProposal(current.id, "agree")
						.setCallback(new Callback<Proposal>(){
							@Override
							public void onSuccess(Proposal updated){
								if(getActivity()==null) return;
								bv.setEnabled(true);
								for(int i=0;i<forest.size();i++){
									if(forest.get(i).id.equals(updated.id)){
										forest.set(i, updated);
										forestAdapter.notifyItemChanged(i);
										break;
									}
								}
							}
							@Override
							public void onError(ErrorResponse error){
								if(getActivity()==null) return;
								bv.setEnabled(true);
							}
						})
						.exec(accountID);
				}
			});
			statusBadge.setText(statusLabel(p.status));
			applyStatusColor(statusBadge, p.status);
			title.setText(p.title);
			if(p.createdByAccount!=null) author.setText("@"+p.createdByAccount.username);
			if(!TextUtils.isEmpty(p.summary)){
				summary.setText(p.summary); summary.setVisibility(View.VISIBLE);
			}else{
				summary.setVisibility(View.GONE);
			}
		}
	}
}
