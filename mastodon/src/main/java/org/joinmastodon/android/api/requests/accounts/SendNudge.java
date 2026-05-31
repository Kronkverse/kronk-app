package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.NudgeResult;

public class SendNudge extends MastodonAPIRequest<NudgeResult> {
	public SendNudge(String accountId) {
		this(accountId, null, null, null);
	}

	public SendNudge(String accountId, String text, String mediaId) {
		this(accountId, text, mediaId, null);
	}

	public SendNudge(String accountId, String text, String mediaId, String voiceId) {
		super(HttpMethod.POST, "/accounts/" + accountId + "/nudge", NudgeResult.class);
		Body body = new Body();
		body.text = text;
		body.media_id = mediaId;
		body.voice_id = voiceId;
		setRequestBody(body);
	}

	private static class Body {
		public String text;
		public String media_id;
		public String voice_id;
	}
}
