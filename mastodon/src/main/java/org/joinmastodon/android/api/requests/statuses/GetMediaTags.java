package org.joinmastodon.android.api.requests.statuses;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.MediaTag;

import java.util.List;

public class GetMediaTags extends MastodonAPIRequest<List<MediaTag>> {
	public GetMediaTags(String mediaId) {
		super(HttpMethod.GET, "/media/" + mediaId + "/tags", new TypeToken<>() {});
	}
}
