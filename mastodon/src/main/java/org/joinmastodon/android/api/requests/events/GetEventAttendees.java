package org.joinmastodon.android.api.requests.events;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Account;

import java.util.List;

public class GetEventAttendees extends MastodonAPIRequest<List<Account>>{
	public GetEventAttendees(String eventId, String status){
		super(HttpMethod.GET, "/events/"+eventId+"/attendees", new TypeToken<>(){});
		addQueryParameter("status", status);
	}
}
