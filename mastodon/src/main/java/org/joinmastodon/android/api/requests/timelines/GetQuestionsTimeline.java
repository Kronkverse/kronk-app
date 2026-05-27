package org.joinmastodon.android.api.requests.timelines;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

import java.util.List;

public class GetQuestionsTimeline extends MastodonAPIRequest<List<Status>> {
	public GetQuestionsTimeline(String maxID, int limit) {
		super(HttpMethod.GET, "/timelines/home", new TypeToken<>() {});
		addQueryParameter("post_type", "question");
		if (maxID != null) addQueryParameter("max_id", maxID);
		if (limit > 0) addQueryParameter("limit", "" + limit);
		removeUnsupportedItems = true;
	}
}
