package org.joinmastodon.android.api.requests.proposals;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Proposal;

public class UnarchiveProposal extends MastodonAPIRequest<Proposal>{
	public UnarchiveProposal(String proposalId){
		super(HttpMethod.POST, "/proposals/"+proposalId+"/unarchive", Proposal.class);
		setRequestBody(new Object());
	}
}
