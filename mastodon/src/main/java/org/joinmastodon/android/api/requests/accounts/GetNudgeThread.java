package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.NudgeThreadResponse;

public class GetNudgeThread extends MastodonAPIRequest<NudgeThreadResponse> {
	public GetNudgeThread(String accountId) {
		super(HttpMethod.GET, "/accounts/" + accountId + "/nudge_thread", NudgeThreadResponse.class);
	}
}
