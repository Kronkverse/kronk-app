package org.joinmastodon.android.api.requests.proposals;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Proposal;

public class VoteOnProposal extends MastodonAPIRequest<Proposal>{
	public VoteOnProposal(String proposalId, String position){
		super(HttpMethod.POST, "/proposals/"+proposalId+"/vote", Proposal.class);
		setRequestBody(new Body(position));
	}

	private static class Body{
		public Vote vote;
		Body(String position){ this.vote=new Vote(position); }
	}

	private static class Vote{
		public String position;
		Vote(String position){ this.position=position; }
	}
}
