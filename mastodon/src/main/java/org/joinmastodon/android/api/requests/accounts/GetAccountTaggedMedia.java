package org.joinmastodon.android.api.requests.accounts;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.TaggedAttachment;

import java.util.List;

public class GetAccountTaggedMedia extends MastodonAPIRequest<List<TaggedAttachment>> {
	public GetAccountTaggedMedia(String accountId, String maxId) {
		super(HttpMethod.GET, "/accounts/" + accountId + "/tagged_media", new TypeToken<>() {});
		if (maxId != null)
			addQueryParameter("max_id", maxId);
	}
}
