package org.joinmastodon.android.api.requests.notifications;

import org.joinmastodon.android.api.MastodonAPIRequest;

public class NudgeUnreact extends MastodonAPIRequest<Object>{
	public NudgeUnreact(String notificationId){
		super(HttpMethod.POST, "/notifications/"+notificationId+"/nudge_unreact", Object.class);
		setRequestBody(new Object());
	}
}
