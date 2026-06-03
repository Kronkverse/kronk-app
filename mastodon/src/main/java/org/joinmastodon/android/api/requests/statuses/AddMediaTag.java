package org.joinmastodon.android.api.requests.statuses;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.MediaTag;

public class AddMediaTag extends MastodonAPIRequest<MediaTag> {
	public AddMediaTag(String mediaId, String accountId, double x, double y) {
		super(HttpMethod.POST, "/media/" + mediaId + "/tag", MediaTag.class);
		Body body = new Body();
		body.account_id = accountId;
		body.x = x;
		body.y = y;
		setRequestBody(body);
	}

	private static class Body {
		public String account_id;
		public double x;
		public double y;
	}
}
