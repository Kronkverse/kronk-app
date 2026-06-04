package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

public class TaggedAttachment extends Attachment {
	@SerializedName("status_id")
	public String statusId;
	@SerializedName("status_account_acct")
	public String statusAccountAcct;
}
