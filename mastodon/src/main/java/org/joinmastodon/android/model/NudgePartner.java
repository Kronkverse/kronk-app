package org.joinmastodon.android.model;

public class NudgePartner extends BaseModel {
	public String account_id;
	public Account account;
	public int sent_count;
	public int received_count;
	public int streak;
	public String last_nudge_at;
	public boolean can_nudge_back;
	public LastMessage last_message;

	public static class LastMessage {
		public String type; // plain, text, image, video, voice
		public String body;
		public String direction; // sent, received
		public String created_at;
	}
}
