package org.joinmastodon.android.model;

import java.util.List;

public class NudgeThreadResponse extends BaseModel {
	public Account account;
	public List<NudgeThreadMessage> messages;
	public boolean can_nudge_back;
	public int streak;
}
