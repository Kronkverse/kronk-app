package org.joinmastodon.android.api.requests.invites;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.PersonalInvite;

public class GetPersonalInvite extends MastodonAPIRequest<PersonalInvite>{
	public GetPersonalInvite(){
		super(HttpMethod.GET, "/invites/personal", PersonalInvite.class);
	}
}
