package org.joinmastodon.android.api.requests.kuestions;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

import java.util.List;

public class GetQuestionAnswers extends MastodonAPIRequest<List<Status>> {
	public GetQuestionAnswers(String questionId) {
		super(HttpMethod.GET, "/questions/" + questionId + "/answers", new TypeToken<>() {});
		removeUnsupportedItems = true;
	}
}
