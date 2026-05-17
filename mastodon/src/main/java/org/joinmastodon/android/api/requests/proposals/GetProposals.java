package org.joinmastodon.android.api.requests.proposals;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Proposal;

import java.util.List;

public class GetProposals extends MastodonAPIRequest<List<Proposal>>{
	public GetProposals(String filter){
		super(HttpMethod.GET, "/proposals", new TypeToken<>(){});
		if(filter!=null) addQueryParameter("filter", filter);
	}
}
