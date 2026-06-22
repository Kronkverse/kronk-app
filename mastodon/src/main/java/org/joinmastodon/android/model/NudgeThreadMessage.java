package org.joinmastodon.android.model;

import java.util.Map;

public class NudgeThreadMessage extends BaseModel {
	public String notification_id;
	public String direction; // sent, received
	public String created_at;
	public String body;
	public String media_url;
	public String media_content_type;
	public String voice_url;
	public String read_at;
	public InReplyTo in_reply_to;
	public Map<String, Reaction> reactions;

	public static class InReplyTo extends BaseModel {
		public String notification_id;
		public String body;
		public boolean voice;
		public boolean image;
	}

	public static class Reaction extends BaseModel {
		public int count;
		public boolean me;
	}
}
