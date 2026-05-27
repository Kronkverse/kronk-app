package org.joinmastodon.android.api.requests.proposals;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Proposal;

public class CreateProposalTask extends MastodonAPIRequest<Proposal> {
	public CreateProposalTask(String proposalId, String title, String description) {
		super(HttpMethod.POST, "/proposals/" + proposalId + "/tasks", Proposal.class);
		setRequestBody(new Body(title, description));
	}

	private static class Body {
		public TaskFields task;
		Body(String title, String description) { this.task = new TaskFields(title, description); }
	}

	private static class TaskFields {
		public String title;
		public String description;
		TaskFields(String t, String d) { title = t; description = d; }
	}
}
