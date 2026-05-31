package org.joinmastodon.android.model;

public class NudgeThreadMessage extends BaseModel {
	public String notification_id;
	public String direction; // sent, received
	public String created_at;
	public String body;
	public String media_url;
	public String media_content_type;
	public String voice_url;
}
