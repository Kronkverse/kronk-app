package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;

import java.util.List;

public class FriendsActivityItem extends BaseModel{
	public String id;
	public Status status;
	public List<Interaction> interactions;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(status!=null)
			status.postprocess();
	}

	public static class Interaction{
		public String type;
		public Account account;
		public String created_at;
	}
}
