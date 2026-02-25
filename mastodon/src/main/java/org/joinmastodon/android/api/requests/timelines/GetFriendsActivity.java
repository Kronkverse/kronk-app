package org.joinmastodon.android.api.requests.timelines;

import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.FriendsActivityItem;

import java.util.List;

public class GetFriendsActivity extends MastodonAPIRequest<List<FriendsActivityItem>>{
	public GetFriendsActivity(String maxID, int limit){
		super(HttpMethod.GET, "/timelines/friends_activity", new TypeToken<>(){});
		if(!TextUtils.isEmpty(maxID))
			addQueryParameter("max_id", maxID);
		if(limit>0)
			addQueryParameter("limit", limit+"");
	}
}
