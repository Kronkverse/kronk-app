package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.model.BaseModel;

public class CheckInviteLink extends MastodonAPIRequest<CheckInviteLink.Response>{
	public CheckInviteLink(String path){
		super(HttpMethod.GET, path, Response.class);
		addHeader("Accept", "application/json");
	}

	@Override
	protected String getPathPrefix(){
		return "";
	}

	public static class Inviter extends BaseModel{
		public String id;
		public String username;
		public String acct;
		public String display_name;
		public String url;
	}

	public static class Response extends BaseModel{
		@RequiredField
		public String inviteCode;
		public Inviter inviter;
	}
}
