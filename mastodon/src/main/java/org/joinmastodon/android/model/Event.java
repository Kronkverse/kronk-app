package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

import java.time.Instant;

@Parcel
public class Event extends BaseModel{
	@RequiredField
	public String id;
	@RequiredField
	public String title;
	public String description;
	@RequiredField
	public Instant startTime;
	public Instant endTime;
	public String locationName;
	public String locationUrl;
	public String eventType;
	public String huddleUrl;
	public boolean rsvpEnabled;
	public Integer maxAttendees;
	public String recurrenceRule;
	public boolean cancelled;
	public int goingCount;
	public int interestedCount;
	public String imageUrl;
	public Instant createdAt;
	public Instant updatedAt;
	public Account account;
	public String statusId;
	public String visibility;
	public String rsvp;
	public boolean invited;
	public boolean isOwner;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(account!=null)
			account.postprocess();
	}
}
