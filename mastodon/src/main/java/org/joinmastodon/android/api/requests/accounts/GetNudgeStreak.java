package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.NudgeResult;

public class GetNudgeStreak extends MastodonAPIRequest<NudgeResult> {
	public GetNudgeStreak(String accountId) {
		super(HttpMethod.GET, "/accounts/" + accountId + "/nudge_streak", NudgeResult.class);
	}
}
