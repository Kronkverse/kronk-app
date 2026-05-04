package org.joinmastodon.android.api.requests.events;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Event;

public class DeleteEvent extends MastodonAPIRequest<Event>{
	public DeleteEvent(String eventId){
		super(HttpMethod.DELETE, "/events/"+eventId, Event.class);
	}
}
