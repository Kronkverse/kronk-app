package org.joinmastodon.android.api.requests.proposals;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Proposal;

public class MarkProposalDelivered extends MastodonAPIRequest<Proposal>{
	public MarkProposalDelivered(String proposalId){
		super(HttpMethod.POST, "/proposals/"+proposalId+"/mark_delivered", Proposal.class);
		setRequestBody(new Object());
	}
}
