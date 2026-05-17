package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.NudgeResult;

public class SendNudge extends MastodonAPIRequest<NudgeResult> {
	public SendNudge(String accountId) {
		super(HttpMethod.POST, "/accounts/" + accountId + "/nudge", NudgeResult.class);
		setRequestBody(new Object());
	}
}
