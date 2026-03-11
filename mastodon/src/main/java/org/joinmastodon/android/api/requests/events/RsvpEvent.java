package org.joinmastodon.android.api.requests.events;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Event;

public class RsvpEvent extends MastodonAPIRequest<Event>{
	public RsvpEvent(String eventId, String status){
		super(HttpMethod.POST, "/events/"+eventId+"/rsvp", Event.class);
		setRequestBody(new Body(status));
	}

	private static class Body{
		public String status;
		Body(String status){
			this.status=status;
		}
	}
}
