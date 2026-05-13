package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.NudgePartnersResponse;

public class GetNudgePartners extends MastodonAPIRequest<NudgePartnersResponse> {
	public GetNudgePartners() {
		super(HttpMethod.GET, "/accounts/nudge_partners", NudgePartnersResponse.class);
	}
}
