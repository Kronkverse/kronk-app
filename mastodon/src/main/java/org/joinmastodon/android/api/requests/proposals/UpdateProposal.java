package org.joinmastodon.android.api.requests.proposals;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Proposal;

public class UpdateProposal extends MastodonAPIRequest<Proposal>{
	public UpdateProposal(String proposalId, String title, String body){
		super(HttpMethod.PATCH, "/proposals/"+proposalId, Proposal.class);
		setRequestBody(new Body(title, body));
	}
	private static class Body{
		public ProposalFields proposal;
		Body(String title, String body){ this.proposal=new ProposalFields(title, body); }
	}
	private static class ProposalFields{
		public String title;
		public String body;
		ProposalFields(String t, String b){ title=t; body=b; }
	}
}
