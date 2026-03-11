package org.joinmastodon.android.api.requests.events;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Event;

import java.util.List;

public class GetEvents extends MastodonAPIRequest<List<Event>>{
	public GetEvents(String filter, String maxID, int limit){
		super(HttpMethod.GET, "/events", new TypeToken<>(){});
		if(filter!=null)
			addQueryParameter("filter", filter);
		if(maxID!=null)
			addQueryParameter("max_id", maxID);
		if(limit>0)
			addQueryParameter("limit", ""+limit);
	}
}
