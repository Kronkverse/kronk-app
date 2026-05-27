package org.joinmastodon.android.api.requests.kuestions;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

import java.util.List;

public class GetQuestions extends MastodonAPIRequest<List<Status>> {
	public GetQuestions(String maxID, int limit) {
		super(HttpMethod.GET, "/questions", new TypeToken<>() {});
		if (maxID != null) addQueryParameter("max_id", maxID);
		if (limit > 0) addQueryParameter("limit", "" + limit);
		removeUnsupportedItems = true;
	}
}
