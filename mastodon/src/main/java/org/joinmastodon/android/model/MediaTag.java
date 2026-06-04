package org.joinmastodon.android.model;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

@Parcel
public class MediaTag extends BaseModel {
	public String id;
	@SerializedName("account_id")
	public String accountId;
	public double x;
	public double y;
	public Account account;
}
