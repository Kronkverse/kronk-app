package org.joinmastodon.android.api.requests.statuses;

import org.joinmastodon.android.api.MastodonAPIRequest;

public class DeleteMediaTag extends MastodonAPIRequest<Void> {
	public DeleteMediaTag(String mediaId, String accountId) {
		super(HttpMethod.DELETE, "/media/" + mediaId + "/tags/" + accountId, Void.class);
	}
}
