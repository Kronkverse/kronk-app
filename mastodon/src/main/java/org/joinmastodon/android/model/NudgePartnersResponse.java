package org.joinmastodon.android.model;

import java.util.List;

public class NudgePartnersResponse extends BaseModel {
	public List<Account> accounts;
	public List<NudgePartner> partners;
	public List<NudgeSuggestion> suggestions;
	public int pending_count;
	public int grand_total;
	public int total_sent;
	public int total_received;
}
