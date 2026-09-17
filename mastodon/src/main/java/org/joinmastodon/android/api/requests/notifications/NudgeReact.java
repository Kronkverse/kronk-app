package org.joinmastodon.android.api.requests.notifications;

import org.joinmastodon.android.api.MastodonAPIRequest;

public class NudgeReact extends MastodonAPIRequest<Object>{
	public NudgeReact(String notificationId, String emoji){
		super(HttpMethod.POST, "/notifications/"+notificationId+"/nudge_react", Object.class);
		Body body = new Body();
		body.emoji = emoji;
		setRequestBody(body);
	}

	private static class Body{
		public String emoji;
	}
}
