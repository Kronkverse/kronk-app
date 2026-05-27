package org.joinmastodon.android.api.requests.proposals;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Proposal;

public class UnvoteProposal extends MastodonAPIRequest<Proposal>{
	public UnvoteProposal(String proposalId){
		super(HttpMethod.DELETE, "/proposals/"+proposalId+"/unvote", Proposal.class);
	}
}
