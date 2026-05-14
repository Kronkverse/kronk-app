package org.joinmastodon.android.model;

import java.util.List;

public class NudgePartnersResponse extends BaseModel {
	public List<NudgePartner> partners;
	public int pending_count;
	public int grand_total;
}
