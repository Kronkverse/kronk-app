package org.joinmastodon.android.api.requests.proposals;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Proposal;

public class ArchiveProposal extends MastodonAPIRequest<Proposal>{
	public ArchiveProposal(String proposalId){
		super(HttpMethod.POST, "/proposals/"+proposalId+"/archive", Proposal.class);
		setRequestBody(new Object());
	}
}
