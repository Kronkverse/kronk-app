package org.joinmastodon.android.fragments;

import android.app.Fragment;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.events.GetEvents;
import org.joinmastodon.android.api.requests.events.RsvpEvent;
import org.joinmastodon.android.model.Event;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.V;

public class EventsFragment extends Fragment implements ScrollableToTop{
	private RecyclerView list;
	private SwipeRefreshLayout refreshLayout;
	private LinearLayout emptyView;
	private String accountID;
	private List<Event> events=new ArrayList<>();
	private EventsAdapter adapter;
	public boolean loaded;
	public boolean dataLoading;

	private static final DateTimeFormatter DATE_FORMAT=DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter TIME_FORMAT=DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter DAY_OF_WEEK_FORMAT=DateTimeFormatter.ofPattern("EEEE").withZone(ZoneId.systemDefault());

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		LinearLayout content=new LinearLayout(getActivity());
		content.setOrientation(LinearLayout.VERTICAL);
		content.setBackgroundColor(UiUtils.getThemeColor(getActivity(), android.R.attr.colorBackground));

		// Header
		TextView header=new TextView(getActivity());
		header.setText("Events");
		header.setTextSize(24);
		header.setTypeface(null, Typeface.BOLD);
		header.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
		header.setPadding(V.dp(16), V.dp(16), V.dp(16), V.dp(8));
		content.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		refreshLayout=new SwipeRefreshLayout(getActivity());
		int accentColor=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary);
		refreshLayout.setColorSchemeColors(accentColor);

		list=new RecyclerView(getActivity());
		list.setId(R.id.list);
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		adapter=new EventsAdapter();
		list.setAdapter(adapter);
		list.setClipToPadding(false);
		list.setPadding(V.dp(12), 0, V.dp(12), V.dp(12));

		refreshLayout.addView(list, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		refreshLayout.setOnRefreshListener(this::loadData);

		// Empty state
		emptyView=new LinearLayout(getActivity());
		emptyView.setOrientation(LinearLayout.VERTICAL);
		emptyView.setGravity(Gravity.CENTER);
		emptyView.setVisibility(View.GONE);
		emptyView.setPadding(V.dp(32), V.dp(64), V.dp(32), V.dp(64));

		TextView emptyEmoji=new TextView(getActivity());
		emptyEmoji.setText("\uD83C\uDF89"); // party popper emoji
		emptyEmoji.setTextSize(48);
		emptyEmoji.setGravity(Gravity.CENTER);
		emptyView.addView(emptyEmoji);

		TextView emptyTitle=new TextView(getActivity());
		emptyTitle.setText("No upcoming events");
		emptyTitle.setTextSize(18);
		emptyTitle.setTypeface(null, Typeface.BOLD);
		emptyTitle.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
		emptyTitle.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams etlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		etlp.topMargin=V.dp(12);
		emptyView.addView(emptyTitle, etlp);

		TextView emptySubtext=new TextView(getActivity());
		emptySubtext.setText("Events created on this instance will appear here");
		emptySubtext.setTextSize(14);
		emptySubtext.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		emptySubtext.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams eslp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		eslp.topMargin=V.dp(8);
		emptyView.addView(emptySubtext, eslp);

		content.addView(refreshLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
		content.addView(emptyView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

		return content;
	}

	public void loadData(){
		if(dataLoading)
			return;
		dataLoading=true;
		new GetEvents("upcoming", null, 40)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Event> result){
						if(getActivity()==null)
							return;
						dataLoading=false;
						loaded=true;
						refreshLayout.setRefreshing(false);
						events.clear();
						events.addAll(result);
						adapter.notifyDataSetChanged();
						emptyView.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
						refreshLayout.setVisibility(events.isEmpty() ? View.GONE : View.VISIBLE);
					}

					@Override
					public void onError(ErrorResponse error){
						if(getActivity()==null)
							return;
						dataLoading=false;
						refreshLayout.setRefreshing(false);
						emptyView.setVisibility(View.VISIBLE);
						refreshLayout.setVisibility(View.GONE);
					}
				})
				.exec(accountID);
	}

	@Override
	public void scrollToTop(){
		if(list!=null)
			list.smoothScrollToPosition(0);
	}

	private String formatEventTime(Event event){
		if(event.startTime==null) return "";
		StringBuilder sb=new StringBuilder();
		sb.append(DAY_OF_WEEK_FORMAT.format(event.startTime));
		sb.append(", ");
		sb.append(DATE_FORMAT.format(event.startTime));
		sb.append(" \u00B7 ");
		sb.append(TIME_FORMAT.format(event.startTime));
		if(event.endTime!=null){
			sb.append(" \u2013 ");
			sb.append(TIME_FORMAT.format(event.endTime));
		}
		return sb.toString();
	}

	private String formatRelativeTime(Event event){
		if(event.startTime==null) return "";
		Instant now=Instant.now();
		if(event.startTime.isBefore(now)){
			return "Happening now";
		}
		Duration d=Duration.between(now, event.startTime);
		long days=d.toDays();
		if(days==0){
			long hours=d.toHours();
			if(hours==0){
				long minutes=d.toMinutes();
				return "In "+minutes+" min"+(minutes!=1?"s":"");
			}
			return "In "+hours+" hour"+(hours!=1?"s":"");
		}else if(days==1){
			return "Tomorrow";
		}else if(days<7){
			return "In "+days+" days";
		}
		return "";
	}

	private class EventsAdapter extends RecyclerView.Adapter<EventViewHolder>{
		@Override
		public EventViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
			return new EventViewHolder();
		}

		@Override
		public void onBindViewHolder(EventViewHolder holder, int position){
			holder.bind(events.get(position));
		}

		@Override
		public int getItemCount(){
			return events.size();
		}
	}

	private class EventViewHolder extends RecyclerView.ViewHolder{
		private final TextView title, dateTime, location, attendees, description, relativeTime, eventType;
		private final Button rsvpButton;
		private final View cancelledBadge;

		EventViewHolder(){
			super(createEventCardView());
			title=itemView.findViewById(android.R.id.title);
			dateTime=itemView.findViewById(android.R.id.text1);
			location=itemView.findViewById(android.R.id.text2);
			attendees=itemView.findViewById(android.R.id.summary);
			description=itemView.findViewById(android.R.id.message);
			relativeTime=itemView.findViewById(android.R.id.hint);
			eventType=itemView.findViewById(android.R.id.custom);
			rsvpButton=itemView.findViewById(android.R.id.button1);
			cancelledBadge=itemView.findViewById(android.R.id.icon);
		}

		void bind(Event event){
			title.setText(event.title);

			// Relative time badge
			String relative=formatRelativeTime(event);
			if(!TextUtils.isEmpty(relative)){
				relativeTime.setText(relative);
				relativeTime.setVisibility(View.VISIBLE);
				// Color "Happening now" differently
				if(relative.equals("Happening now")){
					relativeTime.setTextColor(UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3Primary));
				}else{
					relativeTime.setTextColor(UiUtils.getThemeColor(itemView.getContext(), android.R.attr.textColorSecondary));
				}
			}else{
				relativeTime.setVisibility(View.GONE);
			}

			// Date and time
			if(event.startTime!=null){
				dateTime.setText("\uD83D\uDCC5  "+formatEventTime(event));
				dateTime.setVisibility(View.VISIBLE);
			}else{
				dateTime.setVisibility(View.GONE);
			}

			// Location
			if(!TextUtils.isEmpty(event.locationName)){
				location.setText("\uD83D\uDCCD  "+event.locationName);
				location.setVisibility(View.VISIBLE);
			}else{
				location.setVisibility(View.GONE);
			}

			// Description
			if(!TextUtils.isEmpty(event.description)){
				String desc=event.description.length()>150 ? event.description.substring(0, 150)+"…" : event.description;
				description.setText(desc);
				description.setVisibility(View.VISIBLE);
			}else{
				description.setVisibility(View.GONE);
			}

			// Attendees
			int going=event.goingCount;
			int interested=event.interestedCount;
			if(going>0 || interested>0){
				StringBuilder atText=new StringBuilder();
				if(going>0) atText.append(going).append(" going");
				if(going>0 && interested>0) atText.append(" \u00B7 ");
				if(interested>0) atText.append(interested).append(" interested");
				attendees.setText(atText.toString());
				attendees.setVisibility(View.VISIBLE);
			}else{
				attendees.setVisibility(View.GONE);
			}

			// Event type badge
			if(!TextUtils.isEmpty(event.eventType)){
				eventType.setText(event.eventType.substring(0, 1).toUpperCase()+event.eventType.substring(1));
				eventType.setVisibility(View.VISIBLE);
			}else{
				eventType.setVisibility(View.GONE);
			}

			// Cancelled badge
			cancelledBadge.setVisibility(event.cancelled ? View.VISIBLE : View.GONE);
			if(event.cancelled){
				title.setAlpha(0.5f);
			}else{
				title.setAlpha(1f);
			}

			// RSVP button styling
			int primaryColor=UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3Primary);
			int onPrimaryColor=UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3OnPrimary);
			int surfaceVariantColor=UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3SurfaceVariant);
			int onSurfaceVariantColor=UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3OnSurfaceVariant);

			if("going".equals(event.rsvp)){
				rsvpButton.setText("\u2714  Going");
				setButtonStyle(rsvpButton, primaryColor, onPrimaryColor);
			}else if("interested".equals(event.rsvp)){
				rsvpButton.setText("\u2606  Interested");
				setButtonStyle(rsvpButton, surfaceVariantColor, onSurfaceVariantColor);
			}else{
				rsvpButton.setText("RSVP");
				setButtonOutlineStyle(rsvpButton, primaryColor);
			}

			if(event.cancelled){
				rsvpButton.setVisibility(View.GONE);
			}else{
				rsvpButton.setVisibility(View.VISIBLE);
			}

			rsvpButton.setOnClickListener(v->{
				String newStatus="going".equals(event.rsvp) ? "remove" : "going";
				new RsvpEvent(event.id, newStatus)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Event result){
								if(getActivity()==null) return;
								int idx=events.indexOf(event);
								if(idx>=0){
									events.set(idx, result);
									adapter.notifyItemChanged(idx);
								}
							}
							@Override
							public void onError(ErrorResponse error){
								if(getActivity()!=null) error.showToast(getActivity());
							}
						})
						.exec(accountID);
			});
		}
	}

	private void setButtonStyle(Button button, int bgColor, int textColor){
		GradientDrawable bg=new GradientDrawable();
		bg.setShape(GradientDrawable.RECTANGLE);
		bg.setCornerRadius(V.dp(20));
		bg.setColor(bgColor);
		button.setBackground(bg);
		button.setTextColor(textColor);
		button.setAlpha(1f);
	}

	private void setButtonOutlineStyle(Button button, int outlineColor){
		GradientDrawable bg=new GradientDrawable();
		bg.setShape(GradientDrawable.RECTANGLE);
		bg.setCornerRadius(V.dp(20));
		bg.setColor(0x00000000); // transparent
		bg.setStroke(V.dp(1), outlineColor);
		button.setBackground(bg);
		button.setTextColor(outlineColor);
		button.setAlpha(1f);
	}

	private View createEventCardView(){
		// Outer wrapper for card margins
		LinearLayout wrapper=new LinearLayout(getActivity());
		wrapper.setOrientation(LinearLayout.VERTICAL);
		RecyclerView.LayoutParams wrapperLp=new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		wrapperLp.topMargin=V.dp(8);
		wrapperLp.bottomMargin=V.dp(4);
		wrapper.setLayoutParams(wrapperLp);

		// Card with rounded corners and elevation
		LinearLayout card=new LinearLayout(getActivity());
		card.setOrientation(LinearLayout.VERTICAL);
		card.setPadding(V.dp(16), V.dp(14), V.dp(16), V.dp(14));

		GradientDrawable cardBg=new GradientDrawable();
		cardBg.setShape(GradientDrawable.RECTANGLE);
		cardBg.setCornerRadius(V.dp(16));
		cardBg.setColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Surface));
		card.setBackground(cardBg);
		card.setElevation(V.dp(2));
		card.setClipToOutline(true);
		card.setOutlineProvider(new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), V.dp(16));
			}
		});

		// Cancelled badge (hidden by default)
		TextView cancelledBadge=new TextView(getActivity());
		cancelledBadge.setId(android.R.id.icon);
		cancelledBadge.setText("CANCELLED");
		cancelledBadge.setTextSize(11);
		cancelledBadge.setTypeface(null, Typeface.BOLD);
		cancelledBadge.setTextColor(0xFFFFFFFF);
		cancelledBadge.setPadding(V.dp(8), V.dp(3), V.dp(8), V.dp(3));
		GradientDrawable cancelBg=new GradientDrawable();
		cancelBg.setShape(GradientDrawable.RECTANGLE);
		cancelBg.setCornerRadius(V.dp(4));
		cancelBg.setColor(0xFFD32F2F);
		cancelledBadge.setBackground(cancelBg);
		cancelledBadge.setVisibility(View.GONE);
		LinearLayout.LayoutParams cbLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		cbLp.bottomMargin=V.dp(8);
		card.addView(cancelledBadge, cbLp);

		// Event type badge row + relative time
		LinearLayout topRow=new LinearLayout(getActivity());
		topRow.setOrientation(LinearLayout.HORIZONTAL);
		topRow.setGravity(Gravity.CENTER_VERTICAL);

		TextView eventType=new TextView(getActivity());
		eventType.setId(android.R.id.custom);
		eventType.setTextSize(11);
		eventType.setTypeface(null, Typeface.BOLD);
		eventType.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSecondaryContainer));
		eventType.setPadding(V.dp(8), V.dp(3), V.dp(8), V.dp(3));
		GradientDrawable typeBg=new GradientDrawable();
		typeBg.setShape(GradientDrawable.RECTANGLE);
		typeBg.setCornerRadius(V.dp(4));
		typeBg.setColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3SecondaryContainer));
		eventType.setBackground(typeBg);
		eventType.setVisibility(View.GONE);
		topRow.addView(eventType, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		// Spacer
		View spacer=new View(getActivity());
		topRow.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1f));

		TextView relativeTime=new TextView(getActivity());
		relativeTime.setId(android.R.id.hint);
		relativeTime.setTextSize(12);
		relativeTime.setTypeface(null, Typeface.BOLD);
		relativeTime.setVisibility(View.GONE);
		topRow.addView(relativeTime, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		LinearLayout.LayoutParams trLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		trLp.bottomMargin=V.dp(8);
		card.addView(topRow, trLp);

		// Title
		TextView title=new TextView(getActivity());
		title.setId(android.R.id.title);
		title.setTextSize(18);
		title.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
		title.setTypeface(null, Typeface.BOLD);
		title.setMaxLines(2);
		title.setEllipsize(TextUtils.TruncateAt.END);
		card.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		// Date/time
		TextView dateTime=new TextView(getActivity());
		dateTime.setId(android.R.id.text1);
		dateTime.setTextSize(14);
		dateTime.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		LinearLayout.LayoutParams dtlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		dtlp.topMargin=V.dp(8);
		card.addView(dateTime, dtlp);

		// Location
		TextView location=new TextView(getActivity());
		location.setId(android.R.id.text2);
		location.setTextSize(14);
		location.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		LinearLayout.LayoutParams loclp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		loclp.topMargin=V.dp(4);
		card.addView(location, loclp);

		// Description
		TextView description=new TextView(getActivity());
		description.setId(android.R.id.message);
		description.setTextSize(14);
		description.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		description.setMaxLines(3);
		description.setEllipsize(TextUtils.TruncateAt.END);
		description.setLineSpacing(V.dp(2), 1f);
		description.setVisibility(View.GONE);
		LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		dlp.topMargin=V.dp(8);
		card.addView(description, dlp);

		// Bottom row: attendees + RSVP button
		LinearLayout bottomRow=new LinearLayout(getActivity());
		bottomRow.setOrientation(LinearLayout.HORIZONTAL);
		bottomRow.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout.LayoutParams brlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		brlp.topMargin=V.dp(12);

		TextView attendees=new TextView(getActivity());
		attendees.setId(android.R.id.summary);
		attendees.setTextSize(13);
		attendees.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary));
		attendees.setTypeface(null, Typeface.BOLD);
		bottomRow.addView(attendees, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		Button rsvpButton=new Button(getActivity());
		rsvpButton.setId(android.R.id.button1);
		rsvpButton.setAllCaps(false);
		rsvpButton.setTextSize(14);
		rsvpButton.setTypeface(null, Typeface.BOLD);
		rsvpButton.setMinimumWidth(V.dp(80));
		rsvpButton.setMinHeight(V.dp(36));
		rsvpButton.setPadding(V.dp(16), V.dp(6), V.dp(16), V.dp(6));
		LinearLayout.LayoutParams btnlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, V.dp(36));
		bottomRow.addView(rsvpButton, btnlp);

		card.addView(bottomRow, brlp);

		wrapper.addView(card, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		return wrapper;
	}
}
