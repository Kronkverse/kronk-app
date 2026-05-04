package org.joinmastodon.android.api.requests.events;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Event;

public class UpdateEvent extends MastodonAPIRequest<Event>{
	public UpdateEvent(String eventId, String title, String description, String startTime,
			String endTime, String locationName, String locationUrl, String eventType,
			boolean rsvpEnabled, Integer maxAttendees, String recurrenceRule, String imageId,
			String visibility, boolean postToFeed){
		super(HttpMethod.PUT, "/events/"+eventId, Event.class);
		setRequestBody(new Body(title, description, startTime, endTime, locationName, locationUrl,
				eventType, rsvpEnabled, maxAttendees, recurrenceRule, imageId, visibility, postToFeed));
	}

	private static class Body{
		public String title;
		public String description;
		@SerializedName("start_time")
		public String startTime;
		@SerializedName("end_time")
		public String endTime;
		@SerializedName("location_name")
		public String locationName;
		@SerializedName("location_url")
		public String locationUrl;
		@SerializedName("event_type")
		public String eventType;
		@SerializedName("rsvp_enabled")
		public boolean rsvpEnabled;
		@SerializedName("max_attendees")
		public Integer maxAttendees;
		@SerializedName("recurrence_rule")
		public String recurrenceRule;
		@SerializedName("image_id")
		public String imageId;
		public String visibility;
		@SerializedName("post_to_feed")
		public boolean postToFeed;

		Body(String title, String description, String startTime, String endTime,
				String locationName, String locationUrl, String eventType, boolean rsvpEnabled,
				Integer maxAttendees, String recurrenceRule, String imageId, String visibility,
				boolean postToFeed){
			this.title=title;
			this.description=description;
			this.startTime=startTime;
			this.endTime=endTime;
			this.locationName=locationName;
			this.locationUrl=locationUrl;
			this.eventType=eventType;
			this.rsvpEnabled=rsvpEnabled;
			this.maxAttendees=maxAttendees;
			this.recurrenceRule=recurrenceRule;
			this.imageId=imageId;
			this.visibility=visibility;
			this.postToFeed=postToFeed;
		}
	}
}
