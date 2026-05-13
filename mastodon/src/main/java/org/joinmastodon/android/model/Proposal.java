package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

import java.time.Instant;
import java.util.List;

@Parcel
public class Proposal extends BaseModel{
	@RequiredField
	public String id;
	@RequiredField
	public String title;
	public String body;
	public String summary;
	@RequiredField
	public String status;
	public String proposalType;
	public List<String> categories;
	public String outcomeNotes;
	public Instant opensAt;
	public int supportCount;
	public int vetoCount;
	public int participationCount;
	public Instant createdAt;
	public CurrentVote currentVote;
	public VoteSummary voteSummary;
	public TaskSummary taskSummary;
	public float budgetTotal;
	public List<Voter> voters;
	public List<Challenge> challenges;
	public Account createdByAccount;
	public Instant archivedAt;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(createdByAccount!=null) createdByAccount.postprocess();
		if(voters!=null) for(Voter v:voters) if(v.account!=null) v.account.postprocess();
		if(challenges!=null) for(Challenge c:challenges) if(c.account!=null) c.account.postprocess();
	}

	@Parcel
	public static class CurrentVote{
		public String position;
		public String title;
		public String statement;
	}

	@Parcel
	public static class VoteSummary{
		public int agree;
		public int abstain;
		public int block;
	}

	@Parcel
	public static class TaskSummary{
		public int open;
		public int inProgress;
		public int done;
	}

	@Parcel
	public static class Voter{
		public String id;
		public String position;
		public String title;
		public String statement;
		public Instant createdAt;
		public Account account;
	}

	@Parcel
	public static class Challenge{
		public String id;
		public String title;
		public String statement;
		public Account account;
		public List<Condition> conditions;
	}

	@Parcel
	public static class Condition{
		public String id;
		public String text;
		public boolean met;
		public Instant metAt;
	}
}
