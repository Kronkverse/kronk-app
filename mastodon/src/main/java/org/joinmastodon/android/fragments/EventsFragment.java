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
import org.parceler.Parcels;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.grishka.appkit.Nav;
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

	private static final DateTimeFormatter MONTH_FORMAT=DateTimeFormatter.ofPattern("MMM").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter DAY_FORMAT=DateTimeFormatter.ofPattern("d").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter TIME_FORMAT=DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter WEEKDAY_FORMAT=DateTimeFormatter.ofPattern("EEE").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter FULL_DATE_FORMAT=DateTimeFormatter.ofPattern("EEE, MMM d").withZone(ZoneId.systemDefault());

	// Colors matching the web frontend
	private static final int COLOR_GOING=0xFF6a9f8a;
	private static final int COLOR_INTERESTED=0xFFb8945f;
	private static final int COLOR_CANCELLED=0xFFD32F2F;

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

		refreshLayout=new SwipeRefreshLayout(getActivity());
		int accentColor=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary);
		refreshLayout.setColorSchemeColors(accentColor);

		list=new RecyclerView(getActivity());
		list.setId(R.id.list);
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		adapter=new EventsAdapter();
		list.setAdapter(adapter);
		list.setClipToPadding(false);
		list.setPadding(V.dp(16), V.dp(8), V.dp(16), V.dp(16));

		refreshLayout.addView(list, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		refreshLayout.setOnRefreshListener(this::loadData);

		// Empty state
		emptyView=new LinearLayout(getActivity());
		emptyView.setOrientation(LinearLayout.VERTICAL);
		emptyView.setGravity(Gravity.CENTER);
		emptyView.setVisibility(View.GONE);
		emptyView.setPadding(V.dp(32), V.dp(64), V.dp(32), V.dp(64));

		ImageView emptyIcon=new ImageView(getActivity());
		emptyIcon.setImageResource(R.drawable.ic_tab_events);
		emptyIcon.setColorFilter(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		LinearLayout.LayoutParams iconLp=new LinearLayout.LayoutParams(V.dp(48), V.dp(48));
		emptyView.addView(emptyIcon, iconLp);

		TextView emptyTitle=new TextView(getActivity());
		emptyTitle.setText("No upcoming events");
		emptyTitle.setTextSize(18);
		emptyTitle.setTypeface(null, Typeface.BOLD);
		emptyTitle.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
		emptyTitle.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams etlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		etlp.topMargin=V.dp(16);
		emptyView.addView(emptyTitle, etlp);

		TextView emptySubtext=new TextView(getActivity());
		emptySubtext.setText("Events will appear here when they're created");
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

	private String formatTimeRange(Event event){
		if(event.startTime==null) return "";
		StringBuilder sb=new StringBuilder();
		sb.append(FULL_DATE_FORMAT.format(event.startTime));
		sb.append(" · ");
		sb.append(TIME_FORMAT.format(event.startTime));
		if(event.endTime!=null){
			sb.append(" – ");
			sb.append(TIME_FORMAT.format(event.endTime));
		}
		return sb.toString();
	}

	private String formatRelativeTime(Event event){
		if(event.startTime==null) return "";
		Instant now=Instant.now();
		if(event.startTime.isBefore(now)) return "Now";
		Duration d=Duration.between(now, event.startTime);
		long days=d.toDays();
		if(days==0){
			long hours=d.toHours();
			if(hours<=1) return "Soon";
			return "In "+hours+"h";
		}else if(days==1){
			return "Tomorrow";
		}else if(days<7){
			return "In "+days+"d";
		}
		return "";
	}

	private void openEventDetail(Event event){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("event", Parcels.wrap(event));
		Nav.go(getActivity(), EventDetailFragment.class, args);
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
		private final TextView dateBadgeMonth, dateBadgeDay;
		private final TextView title, timeText, locationText, descriptionText, attendeesText, relativeTimeText;
		private final View dateBadge, goingChip, interestedChip, cancelledBadge;
		private final TextView goingText, interestedText;

		EventViewHolder(){
			super(createEventCardView());
			dateBadge=itemView.findViewWithTag("dateBadge");
			dateBadgeMonth=itemView.findViewWithTag("dateBadgeMonth");
			dateBadgeDay=itemView.findViewWithTag("dateBadgeDay");
			title=itemView.findViewById(android.R.id.title);
			timeText=itemView.findViewById(android.R.id.text1);
			locationText=itemView.findViewById(android.R.id.text2);
			descriptionText=itemView.findViewById(android.R.id.message);
			attendeesText=itemView.findViewById(android.R.id.summary);
			relativeTimeText=itemView.findViewById(android.R.id.hint);
			goingChip=itemView.findViewWithTag("goingChip");
			interestedChip=itemView.findViewWithTag("interestedChip");
			goingText=(TextView)itemView.findViewWithTag("goingText");
			interestedText=(TextView)itemView.findViewWithTag("interestedText");
			cancelledBadge=itemView.findViewWithTag("cancelledBadge");
		}

		void bind(Event event){
			// Date badge
			if(event.startTime!=null){
				dateBadgeMonth.setText(MONTH_FORMAT.format(event.startTime).toUpperCase(Locale.ROOT));
				dateBadgeDay.setText(DAY_FORMAT.format(event.startTime));
				dateBadge.setVisibility(View.VISIBLE);
			}else{
				dateBadge.setVisibility(View.INVISIBLE);
			}

			// Title
			title.setText(event.title);
			title.setAlpha(event.cancelled ? 0.5f : 1f);

			// Relative time
			String relative=formatRelativeTime(event);
			if(!TextUtils.isEmpty(relative)){
				relativeTimeText.setText(relative);
				relativeTimeText.setVisibility(View.VISIBLE);
				if("Now".equals(relative)){
					relativeTimeText.setTextColor(UiUtils.getThemeColor(itemView.getContext(), R.attr.colorM3Primary));
				}else{
					relativeTimeText.setTextColor(UiUtils.getThemeColor(itemView.getContext(), android.R.attr.textColorSecondary));
				}
			}else{
				relativeTimeText.setVisibility(View.GONE);
			}

			// Time
			String time=formatTimeRange(event);
			if(!TextUtils.isEmpty(time)){
				timeText.setText(time);
				timeText.setVisibility(View.VISIBLE);
			}else{
				timeText.setVisibility(View.GONE);
			}

			// Location
			if(!TextUtils.isEmpty(event.locationName)){
				locationText.setText(event.locationName);
				locationText.setVisibility(View.VISIBLE);
			}else{
				locationText.setVisibility(View.GONE);
			}

			// Description preview
			if(!TextUtils.isEmpty(event.description)){
				String desc=event.description.length()>140 ? event.description.substring(0, 140)+"…" : event.description;
				descriptionText.setText(desc);
				descriptionText.setVisibility(View.VISIBLE);
			}else{
				descriptionText.setVisibility(View.GONE);
			}

			// Attendees
			int total=event.goingCount+event.interestedCount;
			if(total>0){
				attendeesText.setText(total+" attending");
				attendeesText.setVisibility(View.VISIBLE);
			}else{
				attendeesText.setVisibility(View.GONE);
			}

			// RSVP chips
			cancelledBadge.setVisibility(event.cancelled ? View.VISIBLE : View.GONE);

			if(!event.cancelled){
				// Going chip
				boolean isGoing="going".equals(event.rsvp);
				goingChip.setVisibility(View.VISIBLE);
				goingText.setText(isGoing ? "Going ✓" : "Going");
				styleChip(goingChip, goingText, isGoing, COLOR_GOING);

				// Interested chip
				boolean isInterested="interested".equals(event.rsvp);
				interestedChip.setVisibility(View.VISIBLE);
				interestedText.setText(isInterested ? "Interested ✓" : "Interested");
				styleChip(interestedChip, interestedText, isInterested, COLOR_INTERESTED);

				goingChip.setOnClickListener(v->{
					String newStatus=isGoing ? "remove" : "going";
					doRsvp(event, newStatus);
				});
				interestedChip.setOnClickListener(v->{
					String newStatus=isInterested ? "remove" : "interested";
					doRsvp(event, newStatus);
				});
			}else{
				goingChip.setVisibility(View.GONE);
				interestedChip.setVisibility(View.GONE);
			}

			// Click to open detail
			itemView.setOnClickListener(v->openEventDetail(event));
		}
	}

	private void doRsvp(Event event, String status){
		new RsvpEvent(event.id, status)
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
	}

	private void styleChip(View chip, TextView text, boolean active, int activeColor){
		GradientDrawable bg=new GradientDrawable();
		bg.setShape(GradientDrawable.RECTANGLE);
		bg.setCornerRadius(V.dp(8));
		if(active){
			bg.setColor(activeColor);
			text.setTextColor(0xFFFFFFFF);
		}else{
			bg.setColor(0x00000000);
			bg.setStroke(V.dp(1), UiUtils.getThemeColor(chip.getContext(), R.attr.colorM3Outline));
			text.setTextColor(UiUtils.getThemeColor(chip.getContext(), android.R.attr.textColorPrimary));
		}
		chip.setBackground(bg);
	}

	private View createEventCardView(){
		// Card container
		LinearLayout card=new LinearLayout(getActivity());
		card.setOrientation(LinearLayout.VERTICAL);
		RecyclerView.LayoutParams cardLp=new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		cardLp.bottomMargin=V.dp(12);
		card.setLayoutParams(cardLp);
		card.setPadding(V.dp(16), V.dp(14), V.dp(16), V.dp(14));

		GradientDrawable cardBg=new GradientDrawable();
		cardBg.setShape(GradientDrawable.RECTANGLE);
		cardBg.setCornerRadius(V.dp(12));
		cardBg.setColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Surface));
		cardBg.setStroke(V.dp(1), UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant));
		card.setBackground(cardBg);
		card.setElevation(V.dp(1));
		card.setClipToOutline(true);
		card.setOutlineProvider(new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), V.dp(12));
			}
		});

		// Top row: date badge + title/meta
		LinearLayout topRow=new LinearLayout(getActivity());
		topRow.setOrientation(LinearLayout.HORIZONTAL);

		// Date badge
		LinearLayout dateBadge=new LinearLayout(getActivity());
		dateBadge.setTag("dateBadge");
		dateBadge.setOrientation(LinearLayout.VERTICAL);
		dateBadge.setGravity(Gravity.CENTER);
		int primaryColor=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary);
		GradientDrawable dateBg=new GradientDrawable();
		dateBg.setShape(GradientDrawable.RECTANGLE);
		dateBg.setCornerRadius(V.dp(10));
		dateBg.setColor((primaryColor & 0x00FFFFFF) | 0x1A000000); // 10% opacity of primary
		dateBadge.setBackground(dateBg);
		dateBadge.setPadding(V.dp(2), V.dp(6), V.dp(2), V.dp(6));
		LinearLayout.LayoutParams dateLp=new LinearLayout.LayoutParams(V.dp(48), V.dp(48));
		dateLp.rightMargin=V.dp(14);

		TextView monthText=new TextView(getActivity());
		monthText.setTag("dateBadgeMonth");
		monthText.setTextSize(10);
		monthText.setTypeface(null, Typeface.BOLD);
		monthText.setTextColor(primaryColor);
		monthText.setGravity(Gravity.CENTER);
		monthText.setLetterSpacing(0.06f);
		dateBadge.addView(monthText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		TextView dayText=new TextView(getActivity());
		dayText.setTag("dateBadgeDay");
		dayText.setTextSize(18);
		dayText.setTypeface(null, Typeface.BOLD);
		dayText.setTextColor(primaryColor);
		dayText.setGravity(Gravity.CENTER);
		dayText.setIncludeFontPadding(false);
		dateBadge.addView(dayText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		topRow.addView(dateBadge, dateLp);

		// Right side: title + relative time
		LinearLayout titleCol=new LinearLayout(getActivity());
		titleCol.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams titleColLp=new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		titleCol.setGravity(Gravity.CENTER_VERTICAL);

		// Cancelled badge
		TextView cancelledBadge=new TextView(getActivity());
		cancelledBadge.setTag("cancelledBadge");
		cancelledBadge.setText("CANCELLED");
		cancelledBadge.setTextSize(10);
		cancelledBadge.setTypeface(null, Typeface.BOLD);
		cancelledBadge.setTextColor(0xFFFFFFFF);
		cancelledBadge.setLetterSpacing(0.04f);
		cancelledBadge.setPadding(V.dp(6), V.dp(2), V.dp(6), V.dp(2));
		GradientDrawable cancelBg=new GradientDrawable();
		cancelBg.setShape(GradientDrawable.RECTANGLE);
		cancelBg.setCornerRadius(V.dp(4));
		cancelBg.setColor(COLOR_CANCELLED);
		cancelledBadge.setBackground(cancelBg);
		cancelledBadge.setVisibility(View.GONE);
		LinearLayout.LayoutParams cbLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		cbLp.bottomMargin=V.dp(4);
		titleCol.addView(cancelledBadge, cbLp);

		TextView title=new TextView(getActivity());
		title.setId(android.R.id.title);
		title.setTextSize(16);
		title.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
		title.setTypeface(null, Typeface.BOLD);
		title.setMaxLines(2);
		title.setEllipsize(TextUtils.TruncateAt.END);
		titleCol.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		// Relative time label
		TextView relativeTime=new TextView(getActivity());
		relativeTime.setId(android.R.id.hint);
		relativeTime.setTextSize(12);
		relativeTime.setTypeface(null, Typeface.BOLD);
		relativeTime.setVisibility(View.GONE);
		LinearLayout.LayoutParams rtLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		rtLp.topMargin=V.dp(2);
		titleCol.addView(relativeTime, rtLp);

		topRow.addView(titleCol, titleColLp);
		card.addView(topRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		// Time row
		TextView timeText=new TextView(getActivity());
		timeText.setId(android.R.id.text1);
		timeText.setTextSize(13);
		timeText.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		timeText.setCompoundDrawablePadding(V.dp(6));
		LinearLayout.LayoutParams timeLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		timeLp.topMargin=V.dp(10);
		timeLp.leftMargin=V.dp(62); // align with title (48 badge + 14 margin)
		card.addView(timeText, timeLp);

		// Location row
		TextView locationText=new TextView(getActivity());
		locationText.setId(android.R.id.text2);
		locationText.setTextSize(13);
		locationText.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		locationText.setCompoundDrawablePadding(V.dp(6));
		LinearLayout.LayoutParams locLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		locLp.topMargin=V.dp(4);
		locLp.leftMargin=V.dp(62);
		card.addView(locationText, locLp);

		// Description
		TextView descriptionText=new TextView(getActivity());
		descriptionText.setId(android.R.id.message);
		descriptionText.setTextSize(13);
		descriptionText.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		descriptionText.setMaxLines(2);
		descriptionText.setEllipsize(TextUtils.TruncateAt.END);
		descriptionText.setLineSpacing(V.dp(1), 1f);
		descriptionText.setVisibility(View.GONE);
		LinearLayout.LayoutParams descLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		descLp.topMargin=V.dp(8);
		descLp.leftMargin=V.dp(62);
		card.addView(descriptionText, descLp);

		// Bottom row: attendees + RSVP chips
		LinearLayout bottomRow=new LinearLayout(getActivity());
		bottomRow.setOrientation(LinearLayout.HORIZONTAL);
		bottomRow.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout.LayoutParams brLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		brLp.topMargin=V.dp(12);
		brLp.leftMargin=V.dp(62);

		// Attendees text
		TextView attendees=new TextView(getActivity());
		attendees.setId(android.R.id.summary);
		attendees.setTextSize(12);
		attendees.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		attendees.setVisibility(View.GONE);
		bottomRow.addView(attendees, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		// Going chip
		LinearLayout goingChip=new LinearLayout(getActivity());
		goingChip.setTag("goingChip");
		goingChip.setGravity(Gravity.CENTER);
		goingChip.setPadding(V.dp(10), V.dp(5), V.dp(10), V.dp(5));
		TextView goingText=new TextView(getActivity());
		goingText.setTag("goingText");
		goingText.setTextSize(12);
		goingText.setText("Going");
		goingChip.addView(goingText);
		LinearLayout.LayoutParams gcLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		gcLp.rightMargin=V.dp(8);
		bottomRow.addView(goingChip, gcLp);

		// Interested chip
		LinearLayout interestedChip=new LinearLayout(getActivity());
		interestedChip.setTag("interestedChip");
		interestedChip.setGravity(Gravity.CENTER);
		interestedChip.setPadding(V.dp(10), V.dp(5), V.dp(10), V.dp(5));
		TextView interestedText=new TextView(getActivity());
		interestedText.setTag("interestedText");
		interestedText.setTextSize(12);
		interestedText.setText("Interested");
		interestedChip.addView(interestedText);
		bottomRow.addView(interestedChip, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		card.addView(bottomRow, brLp);

		return card;
	}
}
