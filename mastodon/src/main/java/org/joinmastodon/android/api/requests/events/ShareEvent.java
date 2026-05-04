package org.joinmastodon.android.api.requests.events;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Event;

public class ShareEvent extends MastodonAPIRequest<Event> {
	public ShareEvent(String eventId, String comment) {
		super(HttpMethod.POST, "/events/" + eventId + "/share", new TypeToken<>() {});
		if (comment != null && !comment.isEmpty())
			addQueryParameter("comment", comment);
	}
}
