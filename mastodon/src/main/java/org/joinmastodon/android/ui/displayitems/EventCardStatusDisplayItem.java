package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.EventDetailFragment;
import org.joinmastodon.android.model.Event;
import org.joinmastodon.android.model.Status;
import org.parceler.Parcels;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import me.grishka.appkit.Nav;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class EventCardStatusDisplayItem extends StatusDisplayItem{
	private final Status status;
	private final String accountID;
	private final ImageLoaderRequest coverRequest;

	private static final DateTimeFormatter FULL_DATE=DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter TIME_FMT=DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());

	public EventCardStatusDisplayItem(String parentID, Callbacks callbacks, Context context, Status status, String accountID){
		super(parentID, callbacks, context);
		this.status=status;
		this.accountID=accountID;
		if(!TextUtils.isEmpty(status.event.imageUrl)){
			coverRequest=new UrlImageLoaderRequest(status.event.imageUrl, 1000, 1000);
		}else{
			coverRequest=null;
		}
	}

	@Override
	public Type getType(){
		return Type.EVENT_CARD;
	}

	@Override
	public int getImageCount(){
		return coverRequest!=null ? 1 : 0;
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return coverRequest;
	}

	public static class Holder extends StatusDisplayItem.Holder<EventCardStatusDisplayItem>{
		private final ImageView coverImage;
		private final TextView dateText, titleText, locationText, descriptionText;
		private final TextView goingCountText, interestedCountText;
		private final LinearLayout rsvpRow;
		private final View inner;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_event_card, parent);
			coverImage=findViewById(R.id.event_cover_image);
			dateText=findViewById(R.id.event_date);
			titleText=findViewById(R.id.event_title);
			locationText=findViewById(R.id.event_location);
			descriptionText=findViewById(R.id.event_description);
			goingCountText=findViewById(R.id.event_going_count);
			interestedCountText=findViewById(R.id.event_interested_count);
			rsvpRow=findViewById(R.id.event_rsvp_row);
			inner=findViewById(R.id.inner);
		}

		@Override
		public void onBind(EventCardStatusDisplayItem item){
			Event event=item.status.event;

			titleText.setText(event.title);

			// Date
			StringBuilder dateStr=new StringBuilder();
			if(event.startTime!=null){
				dateStr.append(FULL_DATE.format(event.startTime));
				dateStr.append(" \u00b7 ");
				dateStr.append(TIME_FMT.format(event.startTime));
			}
			dateText.setText(dateStr);

			// Cover image
			if(item.coverRequest!=null){
				coverImage.setVisibility(View.VISIBLE);
				ViewImageLoader.load(coverImage, null, item.coverRequest);
			}else{
				coverImage.setVisibility(View.GONE);
				ViewImageLoader.cancelRequest(coverImage);
			}

			// Location
			if(!TextUtils.isEmpty(event.locationName)){
				locationText.setVisibility(View.VISIBLE);
				locationText.setText(event.locationName);
			}else{
				locationText.setVisibility(View.GONE);
			}

			// Description
			if(!TextUtils.isEmpty(event.description)){
				descriptionText.setVisibility(View.VISIBLE);
				descriptionText.setText(event.description);
			}else{
				descriptionText.setVisibility(View.GONE);
			}

			// RSVP counts
			if(event.goingCount>0 || event.interestedCount>0){
				rsvpRow.setVisibility(View.VISIBLE);
				goingCountText.setText(event.goingCount+" going");
				interestedCountText.setText(event.interestedCount+" interested");
				goingCountText.setVisibility(event.goingCount>0 ? View.VISIBLE : View.GONE);
				interestedCountText.setVisibility(event.interestedCount>0 ? View.VISIBLE : View.GONE);
			}else{
				rsvpRow.setVisibility(View.GONE);
			}

			// Click to open event detail
			inner.setOnClickListener(v->{
				Activity activity=(Activity) v.getContext();
				Bundle args=new Bundle();
				args.putString("account", item.accountID);
				args.putParcelable("event", Parcels.wrap(event));
				Nav.go(activity, EventDetailFragment.class, args);
			});

			itemView.setPaddingRelative(V.dp(item.fullWidth ? 16 : 64), 0, itemView.getPaddingEnd(), itemView.getPaddingBottom());
		}
	}
}
