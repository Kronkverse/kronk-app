package org.joinmastodon.android.api.requests.events;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Event;

import java.util.List;

public class InviteToEvent extends MastodonAPIRequest<Event>{
	public InviteToEvent(String eventId, List<String> accountIds){
		super(HttpMethod.POST, "/events/"+eventId+"/invite", Event.class);
		setRequestBody(new Body(accountIds));
	}

	private static class Body{
		@SerializedName("account_ids")
		public List<String> accountIds;

		Body(List<String> accountIds){
			this.accountIds=accountIds;
		}
	}
}
