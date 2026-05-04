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
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
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

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class EventsFragment extends Fragment implements ScrollableToTop{
	private RecyclerView list;
	private SwipeRefreshLayout refreshLayout;
	private LinearLayout emptyView;
	private TextView emptyTitle;
	private TextView emptySubtext;
	private String accountID;
	private List<Event> events=new ArrayList<>();
	private List<Event> allCalendarEvents=new ArrayList<>();
	private List<Event> selectedDayEvents=new ArrayList<>();
	private EventsAdapter adapter;
	private EventsAdapter selectedDayAdapter;
	public boolean loaded;
	public boolean dataLoading;

	// Filter state
	private String currentFilter="upcoming";
	private static final String[] FILTERS={"upcoming", "past", "mine", "invited"};
	private static final String[] FILTER_LABELS={"Upcoming", "Past", "Mine", "Invited"};
	private LinearLayout filterContainer;
	private View[] filterChips;

	// View mode
	private boolean calendarMode=false;
	private View listToggle;
	private View calendarToggle;
	private LinearLayout calendarContainer;
	private LinearLayout calendarGrid;
	private TextView calendarMonthLabel;
	private YearMonth displayedMonth;
	private LocalDate selectedDate;
	private RecyclerView selectedDayList;
	private TextView selectedDayLabel;
	private LinearLayout selectedDaySection;

	// FAB
	private View fab;

	private static final DateTimeFormatter MONTH_FORMAT=DateTimeFormatter.ofPattern("MMM").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter DAY_FORMAT=DateTimeFormatter.ofPattern("d").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter TIME_FORMAT=DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter WEEKDAY_FORMAT=DateTimeFormatter.ofPattern("EEE").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter FULL_DATE_FORMAT=DateTimeFormatter.ofPattern("EEE, MMM d").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter MONTH_YEAR_FORMAT=DateTimeFormatter.ofPattern("MMMM yyyy");

	// Colors matching the web frontend
	private static final int COLOR_GOING=0xFF6a9f8a;
	private static final int COLOR_INTERESTED=0xFFb8945f;
	private static final int COLOR_CANCELLED=0xFFD32F2F;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		displayedMonth=YearMonth.now();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		// Root is a FrameLayout to overlay the FAB
		FrameLayout root=new FrameLayout(getActivity());
		root.setBackgroundColor(UiUtils.getThemeColor(getActivity(), android.R.attr.colorBackground));

		LinearLayout content=new LinearLayout(getActivity());
		content.setOrientation(LinearLayout.VERTICAL);

		// === Top bar: filter chips + new event button + view mode toggle ===
		LinearLayout topBar=new LinearLayout(getActivity());
		topBar.setOrientation(LinearLayout.HORIZONTAL);
		topBar.setGravity(Gravity.CENTER_VERTICAL);
		topBar.setPadding(V.dp(12), V.dp(8), V.dp(12), V.dp(8));

		// Filter chips in a horizontal scroll view
		HorizontalScrollView filterScroll=new HorizontalScrollView(getActivity());
		filterScroll.setHorizontalScrollBarEnabled(false);
		filterScroll.setClipToPadding(false);

		// Wrap filterContainer in a faint purple pill background
		LinearLayout filterWrapper=new LinearLayout(getActivity());
		filterWrapper.setOrientation(LinearLayout.HORIZONTAL);
		filterWrapper.setGravity(Gravity.CENTER_VERTICAL);
		int primaryColor=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary);
		GradientDrawable filterWrapperBg=new GradientDrawable();
		filterWrapperBg.setShape(GradientDrawable.RECTANGLE);
		filterWrapperBg.setCornerRadius(V.dp(10));
		filterWrapperBg.setColor((primaryColor & 0x00FFFFFF) | 0x14000000); // 8% opacity primary
		filterWrapper.setBackground(filterWrapperBg);
		filterWrapper.setPadding(V.dp(3), V.dp(3), V.dp(3), V.dp(3));

		filterContainer=new LinearLayout(getActivity());
		filterContainer.setOrientation(LinearLayout.HORIZONTAL);
		filterContainer.setGravity(Gravity.CENTER_VERTICAL);

		filterChips=new View[FILTERS.length];
		for(int i=0; i<FILTERS.length; i++){
			filterChips[i]=createFilterChip(FILTER_LABELS[i], i);
			LinearLayout.LayoutParams chipLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			if(i>0) chipLp.leftMargin=V.dp(2);
			filterContainer.addView(filterChips[i], chipLp);
		}
		updateFilterChipStyles();

		filterWrapper.addView(filterContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		filterScroll.addView(filterWrapper, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		topBar.addView(filterScroll, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		// "New Event" button
		TextView newEventBtn=new TextView(getActivity());
		newEventBtn.setText("+ New Event");
		newEventBtn.setTextSize(13);
		newEventBtn.setTypeface(null, Typeface.BOLD);
		newEventBtn.setTextColor(0xFFFFFFFF);
		newEventBtn.setGravity(Gravity.CENTER);
		newEventBtn.setPadding(V.dp(14), V.dp(8), V.dp(14), V.dp(8));
		GradientDrawable newEventBg=new GradientDrawable();
		newEventBg.setShape(GradientDrawable.RECTANGLE);
		newEventBg.setCornerRadius(V.dp(10));
		newEventBg.setColor(primaryColor);
		newEventBtn.setBackground(newEventBg);
		newEventBtn.setOnClickListener(v->{
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), CreateEventFragment.class, args);
		});
		LinearLayout.LayoutParams neLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		neLp.leftMargin=V.dp(8);
		topBar.addView(newEventBtn, neLp);

		// View mode toggle
		LinearLayout viewToggle=new LinearLayout(getActivity());
		viewToggle.setOrientation(LinearLayout.HORIZONTAL);
		viewToggle.setGravity(Gravity.CENTER);
		GradientDrawable toggleBg=new GradientDrawable();
		toggleBg.setShape(GradientDrawable.RECTANGLE);
		toggleBg.setCornerRadius(V.dp(8));
		toggleBg.setStroke(V.dp(1), UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant));
		viewToggle.setBackground(toggleBg);

		listToggle=createViewToggleButton("List", true);
		calendarToggle=createViewToggleButton("Cal", false);

		listToggle.setOnClickListener(v->{
			if(!calendarMode) return;
			calendarMode=false;
			updateViewToggleStyles();
			showListView();
		});
		calendarToggle.setOnClickListener(v->{
			if(calendarMode) return;
			calendarMode=true;
			updateViewToggleStyles();
			showCalendarView();
		});

		viewToggle.addView(listToggle);
		viewToggle.addView(calendarToggle);
		LinearLayout.LayoutParams vtLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		vtLp.leftMargin=V.dp(8);
		topBar.addView(viewToggle, vtLp);

		content.addView(topBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		// === SwipeRefreshLayout wrapping the list ===
		refreshLayout=new SwipeRefreshLayout(getActivity());
		int accentColor=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary);
		refreshLayout.setColorSchemeColors(accentColor);

		list=new RecyclerView(getActivity());
		list.setId(R.id.list);
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		adapter=new EventsAdapter(events);
		list.setAdapter(adapter);
		list.setClipToPadding(false);
		list.setPadding(V.dp(16), V.dp(8), V.dp(16), V.dp(72));

		refreshLayout.addView(list, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		refreshLayout.setOnRefreshListener(this::loadData);

		// === Calendar container (hidden initially) ===
		calendarContainer=new LinearLayout(getActivity());
		calendarContainer.setOrientation(LinearLayout.VERTICAL);
		calendarContainer.setVisibility(View.GONE);

		buildCalendarHeader();
		buildCalendarGridContainer();
		buildSelectedDaySection();

		// === Empty state ===
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

		emptyTitle=new TextView(getActivity());
		emptyTitle.setText("No upcoming events");
		emptyTitle.setTextSize(18);
		emptyTitle.setTypeface(null, Typeface.BOLD);
		emptyTitle.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
		emptyTitle.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams etlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		etlp.topMargin=V.dp(16);
		emptyView.addView(emptyTitle, etlp);

		emptySubtext=new TextView(getActivity());
		emptySubtext.setText("₭alendar events will appear here when they are created");
		emptySubtext.setTextSize(14);
		emptySubtext.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		emptySubtext.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams eslp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		eslp.topMargin=V.dp(8);
		emptyView.addView(emptySubtext, eslp);

		content.addView(refreshLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
		content.addView(calendarContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
		content.addView(emptyView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

		root.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		// === FAB ===
		fab=createFab();
		FrameLayout.LayoutParams fabLp=new FrameLayout.LayoutParams(V.dp(56), V.dp(56));
		fabLp.gravity=Gravity.BOTTOM|Gravity.END;
		fabLp.rightMargin=V.dp(16);
		fabLp.bottomMargin=V.dp(16);
		root.addView(fab, fabLp);

		return root;
	}

	// ==================== Filter Chips ====================

	private View createFilterChip(String label, int index){
		TextView chip=new TextView(getActivity());
		chip.setText(label);
		chip.setTextSize(13);
		chip.setTypeface(null, Typeface.BOLD);
		chip.setGravity(Gravity.CENTER);
		chip.setPadding(V.dp(14), V.dp(7), V.dp(14), V.dp(7));
		chip.setOnClickListener(v->{
			if(currentFilter.equals(FILTERS[index])) return;
			currentFilter=FILTERS[index];
			updateFilterChipStyles();
			updateEmptyStateText();
			if(calendarMode){
				loadCalendarEvents();
			}else{
				loadData();
			}
		});
		return chip;
	}

	private void updateFilterChipStyles(){
		int primaryColor=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary);
		for(int i=0; i<filterChips.length; i++){
			TextView chip=(TextView)filterChips[i];
			boolean selected=currentFilter.equals(FILTERS[i]);
			GradientDrawable bg=new GradientDrawable();
			bg.setShape(GradientDrawable.RECTANGLE);
			bg.setCornerRadius(V.dp(20));
			if(selected){
				bg.setColor(primaryColor);
				chip.setTextColor(0xFFFFFFFF);
				chip.setElevation(V.dp(1));
			}else{
				bg.setColor(0x00000000);
				// No border stroke for inactive chips
				chip.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
				chip.setElevation(0);
			}
			chip.setBackground(bg);
		}
	}

	private void updateEmptyStateText(){
		switch(currentFilter){
			case "upcoming":
				emptyTitle.setText("No upcoming events");
				emptySubtext.setText("₭alendar events will appear here when they are created");
				break;
			case "past":
				emptyTitle.setText("No past events");
				emptySubtext.setText("Past ₭alendar events will appear here");
				break;
			case "mine":
				emptyTitle.setText("No events created");
				emptySubtext.setText("Events you host will appear here");
				break;
			case "invited":
				emptyTitle.setText("No invitations");
				emptySubtext.setText("Event invitations will appear here");
				break;
		}
	}

	// ==================== View Mode Toggle ====================

	private View createViewToggleButton(String label, boolean isActive){
		TextView btn=new TextView(getActivity());
		btn.setText(label);
		btn.setTextSize(12);
		btn.setTypeface(null, Typeface.BOLD);
		btn.setGravity(Gravity.CENTER);
		btn.setPadding(V.dp(12), V.dp(6), V.dp(12), V.dp(6));
		return btn;
	}

	private void updateViewToggleStyles(){
		int primaryColor=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary);
		// List button
		{
			TextView btn=(TextView)listToggle;
			GradientDrawable bg=new GradientDrawable();
			bg.setShape(GradientDrawable.RECTANGLE);
			float[] radii={V.dp(7), V.dp(7), 0, 0, 0, 0, V.dp(7), V.dp(7)};
			bg.setCornerRadii(radii);
			if(!calendarMode){
				bg.setColor(primaryColor);
				btn.setTextColor(0xFFFFFFFF);
			}else{
				bg.setColor(0x00000000);
				btn.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
			}
			btn.setBackground(bg);
		}
		// Calendar button
		{
			TextView btn=(TextView)calendarToggle;
			GradientDrawable bg=new GradientDrawable();
			bg.setShape(GradientDrawable.RECTANGLE);
			float[] radii={0, 0, V.dp(7), V.dp(7), V.dp(7), V.dp(7), 0, 0};
			bg.setCornerRadii(radii);
			if(calendarMode){
				bg.setColor(primaryColor);
				btn.setTextColor(0xFFFFFFFF);
			}else{
				bg.setColor(0x00000000);
				btn.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
			}
			btn.setBackground(bg);
		}
	}

	private void showListView(){
		calendarContainer.setVisibility(View.GONE);
		refreshLayout.setVisibility(events.isEmpty() && loaded ? View.GONE : View.VISIBLE);
		emptyView.setVisibility(events.isEmpty() && loaded ? View.VISIBLE : View.GONE);
		if(!loaded){
			loadData();
		}
	}

	private void showCalendarView(){
		refreshLayout.setVisibility(View.GONE);
		emptyView.setVisibility(View.GONE);
		calendarContainer.setVisibility(View.VISIBLE);
		loadCalendarEvents();
	}

	// ==================== Calendar Header ====================

	private void buildCalendarHeader(){
		LinearLayout header=new LinearLayout(getActivity());
		header.setOrientation(LinearLayout.HORIZONTAL);
		header.setGravity(Gravity.CENTER_VERTICAL);
		header.setPadding(V.dp(16), V.dp(12), V.dp(16), V.dp(8));

		// Previous month button
		TextView prevBtn=new TextView(getActivity());
		prevBtn.setText("<");
		prevBtn.setTextSize(20);
		prevBtn.setTypeface(null, Typeface.BOLD);
		prevBtn.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary));
		prevBtn.setGravity(Gravity.CENTER);
		prevBtn.setPadding(V.dp(12), V.dp(4), V.dp(12), V.dp(4));
		prevBtn.setOnClickListener(v->{
			displayedMonth=displayedMonth.minusMonths(1);
			updateCalendarMonthLabel();
			rebuildCalendarGrid();
			updateSelectedDayEvents();
		});
		header.addView(prevBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		// Month label
		calendarMonthLabel=new TextView(getActivity());
		calendarMonthLabel.setTextSize(18);
		calendarMonthLabel.setTypeface(null, Typeface.BOLD);
		calendarMonthLabel.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
		calendarMonthLabel.setGravity(Gravity.CENTER);
		updateCalendarMonthLabel();
		header.addView(calendarMonthLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		// Next month button
		TextView nextBtn=new TextView(getActivity());
		nextBtn.setText(">");
		nextBtn.setTextSize(20);
		nextBtn.setTypeface(null, Typeface.BOLD);
		nextBtn.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary));
		nextBtn.setGravity(Gravity.CENTER);
		nextBtn.setPadding(V.dp(12), V.dp(4), V.dp(12), V.dp(4));
		nextBtn.setOnClickListener(v->{
			displayedMonth=displayedMonth.plusMonths(1);
			updateCalendarMonthLabel();
			rebuildCalendarGrid();
			updateSelectedDayEvents();
		});
		header.addView(nextBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		calendarContainer.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	}

	private void updateCalendarMonthLabel(){
		calendarMonthLabel.setText(displayedMonth.format(MONTH_YEAR_FORMAT));
	}

	// ==================== Calendar Grid ====================

	private void buildCalendarGridContainer(){
		// Weekday header row
		LinearLayout weekdayRow=new LinearLayout(getActivity());
		weekdayRow.setOrientation(LinearLayout.HORIZONTAL);
		weekdayRow.setPadding(V.dp(8), 0, V.dp(8), V.dp(4));
		DayOfWeek[] days={DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY};
		for(DayOfWeek dow : days){
			TextView tv=new TextView(getActivity());
			tv.setText(dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()));
			tv.setTextSize(11);
			tv.setTypeface(null, Typeface.BOLD);
			tv.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
			tv.setGravity(Gravity.CENTER);
			weekdayRow.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		}
		calendarContainer.addView(weekdayRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		// Grid container
		calendarGrid=new LinearLayout(getActivity());
		calendarGrid.setOrientation(LinearLayout.VERTICAL);
		calendarGrid.setPadding(V.dp(8), 0, V.dp(8), V.dp(8));
		calendarContainer.addView(calendarGrid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	}

	private void rebuildCalendarGrid(){
		calendarGrid.removeAllViews();
		LocalDate today=LocalDate.now();
		LocalDate first=displayedMonth.atDay(1);
		int startDow=first.getDayOfWeek().getValue(); // 1=Mon, 7=Sun
		int daysInMonth=displayedMonth.lengthOfMonth();

		// Build event map for this month
		Map<Integer, List<Event>> eventsByDay=new HashMap<>();
		for(Event e : allCalendarEvents){
			if(e.startTime==null) continue;
			LocalDate ed=e.startTime.atZone(ZoneId.systemDefault()).toLocalDate();
			if(ed.getYear()==displayedMonth.getYear() && ed.getMonthValue()==displayedMonth.getMonthValue()){
				eventsByDay.computeIfAbsent(ed.getDayOfMonth(), k->new ArrayList<>()).add(e);
			}
		}

		int dayCounter=1;
		int cellIndex=0;
		int totalCells=((startDow-1)+daysInMonth);
		int rows=(int)Math.ceil(totalCells/7.0);

		for(int row=0; row<rows; row++){
			LinearLayout rowLayout=new LinearLayout(getActivity());
			rowLayout.setOrientation(LinearLayout.HORIZONTAL);

			for(int col=0; col<7; col++){
				int globalIndex=row*7+col;
				if(globalIndex<(startDow-1) || dayCounter>daysInMonth){
					// Empty cell
					View empty=new View(getActivity());
					rowLayout.addView(empty, new LinearLayout.LayoutParams(0, V.dp(64), 1f));
				}else{
					int day=dayCounter;
					LocalDate cellDate=displayedMonth.atDay(day);
					boolean isToday=cellDate.equals(today);
					boolean isSelected=cellDate.equals(selectedDate);
					List<Event> dayEvents=eventsByDay.get(day);

					LinearLayout cell=new LinearLayout(getActivity());
					cell.setOrientation(LinearLayout.VERTICAL);
					cell.setGravity(Gravity.CENTER_HORIZONTAL);
					cell.setPadding(V.dp(2), V.dp(4), V.dp(2), V.dp(2));

					// Background for today/selected
					GradientDrawable cellBg=new GradientDrawable();
					cellBg.setShape(GradientDrawable.RECTANGLE);
					cellBg.setCornerRadius(V.dp(8));
					int cellPrimaryColor=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary);
					if(isSelected){
						cellBg.setColor((cellPrimaryColor & 0x00FFFFFF) | 0x33000000);
					}else if(isToday){
						cellBg.setStroke(V.dp(2), cellPrimaryColor);
					}
					cell.setBackground(cellBg);

					// Day number
					TextView dayTv=new TextView(getActivity());
					dayTv.setText(String.valueOf(day));
					dayTv.setTextSize(13);
					dayTv.setGravity(Gravity.CENTER);
					if(isToday){
						dayTv.setTypeface(null, Typeface.BOLD);
						dayTv.setTextColor(cellPrimaryColor);
					}else{
						dayTv.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
					}
					cell.addView(dayTv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

					// Event title pills (instead of dots)
					if(dayEvents!=null && !dayEvents.isEmpty()){
						LinearLayout pillsContainer=new LinearLayout(getActivity());
						pillsContainer.setOrientation(LinearLayout.VERTICAL);
						pillsContainer.setGravity(Gravity.CENTER_HORIZONTAL);
						int pillCount=Math.min(dayEvents.size(), 2);
						for(int d=0; d<pillCount; d++){
							Event evt=dayEvents.get(d);
							TextView pill=new TextView(getActivity());
							String pillTitle=evt.title!=null ? evt.title : "";
							if(pillTitle.length()>6) pillTitle=pillTitle.substring(0, 6)+"\u2026";
							pill.setText(pillTitle);
							pill.setTextSize(8);
							pill.setTypeface(null, Typeface.BOLD);
							int dotColor=getEventDotColor(evt);
							pill.setTextColor(dotColor);
							pill.setGravity(Gravity.CENTER);
							pill.setPadding(V.dp(1), 0, V.dp(1), 0);
							pill.setSingleLine(true);
							GradientDrawable pillBg=new GradientDrawable();
							pillBg.setShape(GradientDrawable.RECTANGLE);
							pillBg.setCornerRadius(V.dp(3));
							pillBg.setColor((dotColor & 0x00FFFFFF) | 0x33000000); // 20% opacity
							pill.setBackground(pillBg);
							LinearLayout.LayoutParams pillLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
							if(d>0) pillLp.topMargin=V.dp(1);
							pillsContainer.addView(pill, pillLp);
						}
						if(dayEvents.size()>2){
							TextView moreText=new TextView(getActivity());
							moreText.setText("+"+(dayEvents.size()-2));
							moreText.setTextSize(7);
							moreText.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
							moreText.setGravity(Gravity.CENTER);
							LinearLayout.LayoutParams moreLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
							moreLp.topMargin=V.dp(1);
							pillsContainer.addView(moreText, moreLp);
						}
						LinearLayout.LayoutParams pillsLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
						pillsLp.topMargin=V.dp(2);
						cell.addView(pillsContainer, pillsLp);
					}

					cell.setOnClickListener(v->{
						selectedDate=cellDate;
						rebuildCalendarGrid();
						updateSelectedDayEvents();
					});

					rowLayout.addView(cell, new LinearLayout.LayoutParams(0, V.dp(64), 1f));
					dayCounter++;
				}
			}

			calendarGrid.addView(rowLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		}
	}

	private int getEventDotColor(Event event){
		if("going".equals(event.rsvp)) return COLOR_GOING;
		if("interested".equals(event.rsvp)) return COLOR_INTERESTED;
		return UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary);
	}

	// ==================== Selected Day Section ====================

	private void buildSelectedDaySection(){
		selectedDaySection=new LinearLayout(getActivity());
		selectedDaySection.setOrientation(LinearLayout.VERTICAL);

		// Divider
		View divider=new View(getActivity());
		divider.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant));
		selectedDaySection.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(1)));

		// Label
		selectedDayLabel=new TextView(getActivity());
		selectedDayLabel.setTextSize(15);
		selectedDayLabel.setTypeface(null, Typeface.BOLD);
		selectedDayLabel.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
		selectedDayLabel.setPadding(V.dp(16), V.dp(10), V.dp(16), V.dp(6));
		selectedDaySection.addView(selectedDayLabel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		// RecyclerView for selected day events
		selectedDayList=new RecyclerView(getActivity());
		selectedDayList.setLayoutManager(new LinearLayoutManager(getActivity()));
		selectedDayAdapter=new EventsAdapter(selectedDayEvents);
		selectedDayList.setAdapter(selectedDayAdapter);
		selectedDayList.setClipToPadding(false);
		selectedDayList.setPadding(V.dp(16), V.dp(4), V.dp(16), V.dp(72));

		selectedDaySection.addView(selectedDayList, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

		// Empty day message
		TextView noDayEvents=new TextView(getActivity());
		noDayEvents.setTag("noDayEvents");
		noDayEvents.setText("No events on this day");
		noDayEvents.setTextSize(13);
		noDayEvents.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		noDayEvents.setGravity(Gravity.CENTER);
		noDayEvents.setPadding(V.dp(16), V.dp(16), V.dp(16), V.dp(16));
		noDayEvents.setVisibility(View.GONE);
		selectedDaySection.addView(noDayEvents, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		calendarContainer.addView(selectedDaySection, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
	}

	private void updateSelectedDayEvents(){
		if(selectedDate==null){
			selectedDayLabel.setText("Tap a day to see events");
			selectedDayEvents.clear();
			selectedDayAdapter.notifyDataSetChanged();
			View noDayEvents=selectedDaySection.findViewWithTag("noDayEvents");
			if(noDayEvents!=null) noDayEvents.setVisibility(View.GONE);
			selectedDayList.setVisibility(View.GONE);
			return;
		}

		selectedDayLabel.setText(FULL_DATE_FORMAT.format(selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));

		selectedDayEvents.clear();
		for(Event e : allCalendarEvents){
			if(e.startTime==null) continue;
			LocalDate ed=e.startTime.atZone(ZoneId.systemDefault()).toLocalDate();
			if(ed.equals(selectedDate)){
				selectedDayEvents.add(e);
			}
		}
		selectedDayAdapter.notifyDataSetChanged();

		View noDayEvents=selectedDaySection.findViewWithTag("noDayEvents");
		if(selectedDayEvents.isEmpty()){
			selectedDayList.setVisibility(View.GONE);
			if(noDayEvents!=null) noDayEvents.setVisibility(View.VISIBLE);
		}else{
			selectedDayList.setVisibility(View.VISIBLE);
			if(noDayEvents!=null) noDayEvents.setVisibility(View.GONE);
		}
	}

	// ==================== Calendar Data Loading ====================

	private void loadCalendarEvents(){
		allCalendarEvents.clear();
		selectedDayEvents.clear();
		if(selectedDayAdapter!=null) selectedDayAdapter.notifyDataSetChanged();

		// Load upcoming events
		new GetEvents(currentFilter.equals("upcoming") ? "upcoming" : currentFilter, null, 200)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Event> result){
						if(getActivity()==null) return;
						allCalendarEvents.addAll(result);
						// If filter is upcoming, also load past to get full picture
						if("upcoming".equals(currentFilter)){
							loadPastForCalendar();
						}else{
							rebuildCalendarGrid();
							updateSelectedDayEvents();
						}
					}
					@Override
					public void onError(ErrorResponse error){
						if(getActivity()==null) return;
						rebuildCalendarGrid();
						updateSelectedDayEvents();
					}
				})
				.exec(accountID);
	}

	private void loadPastForCalendar(){
		new GetEvents("past", null, 200)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Event> result){
						if(getActivity()==null) return;
						// Add past events, avoiding duplicates
						for(Event e : result){
							boolean dup=false;
							for(Event existing : allCalendarEvents){
								if(existing.id.equals(e.id)){
									dup=true;
									break;
								}
							}
							if(!dup) allCalendarEvents.add(e);
						}
						rebuildCalendarGrid();
						updateSelectedDayEvents();
					}
					@Override
					public void onError(ErrorResponse error){
						if(getActivity()==null) return;
						rebuildCalendarGrid();
						updateSelectedDayEvents();
					}
				})
				.exec(accountID);
	}

	// ==================== FAB ====================

	private View createFab(){
		FrameLayout fabView=new FrameLayout(getActivity());
		int primaryColor=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary);

		GradientDrawable fabBg=new GradientDrawable();
		fabBg.setShape(GradientDrawable.OVAL);
		fabBg.setColor(primaryColor);
		fabView.setBackground(fabBg);
		fabView.setElevation(V.dp(6));

		// Plus icon as text
		TextView plusIcon=new TextView(getActivity());
		plusIcon.setText("+");
		plusIcon.setTextSize(24);
		plusIcon.setTypeface(null, Typeface.NORMAL);
		plusIcon.setTextColor(0xFFFFFFFF);
		plusIcon.setGravity(Gravity.CENTER);
		fabView.addView(plusIcon, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		fabView.setOnClickListener(v->{
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), CreateEventFragment.class, args);
		});

		// Outline for ripple/shadow
		fabView.setOutlineProvider(new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setOval(0, 0, view.getWidth(), view.getHeight());
			}
		});
		fabView.setClipToOutline(true);

		return fabView;
	}

	// ==================== Data Loading (List Mode) ====================

	public void loadData(){
		if(dataLoading)
			return;
		dataLoading=true;
		if(calendarMode){
			dataLoading=false;
			loadCalendarEvents();
			return;
		}
		new GetEvents(currentFilter, null, 40)
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
		if(list!=null && !calendarMode)
			list.smoothScrollToPosition(0);
	}

	// ==================== Formatting Helpers ====================

	private String formatTimeRange(Event event){
		if(event.startTime==null) return "";
		StringBuilder sb=new StringBuilder();
		sb.append(FULL_DATE_FORMAT.format(event.startTime));
		sb.append(" \u00b7 ");
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

	// ==================== RSVP ====================

	private void doRsvp(Event event, String status){
		new RsvpEvent(event.id, status)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Event result){
						if(getActivity()==null) return;
						// Update in main list
						int idx=events.indexOf(event);
						if(idx>=0){
							events.set(idx, result);
							adapter.notifyItemChanged(idx);
						}
						// Update in calendar events
						for(int i=0; i<allCalendarEvents.size(); i++){
							if(allCalendarEvents.get(i).id.equals(event.id)){
								allCalendarEvents.set(i, result);
								break;
							}
						}
						// Update in selected day events
						int sdIdx=selectedDayEvents.indexOf(event);
						if(sdIdx>=0){
							selectedDayEvents.set(sdIdx, result);
							selectedDayAdapter.notifyItemChanged(sdIdx);
						}
						if(calendarMode){
							rebuildCalendarGrid();
						}
					}
					@Override
					public void onError(ErrorResponse error){
						if(getActivity()!=null) error.showToast(getActivity());
					}
				})
				.exec(accountID);
	}

	// ==================== Chip Styling ====================

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

	// ==================== Adapter ====================

	private class EventsAdapter extends RecyclerView.Adapter<EventViewHolder>{
		private final List<Event> data;

		EventsAdapter(List<Event> data){
			this.data=data;
		}

		@Override
		public EventViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
			return new EventViewHolder();
		}

		@Override
		public void onBindViewHolder(EventViewHolder holder, int position){
			holder.bind(data.get(position));
		}

		@Override
		public int getItemCount(){
			return data.size();
		}
	}

	// ==================== ViewHolder ====================

	private class EventViewHolder extends RecyclerView.ViewHolder{
		private final TextView dateBadgeMonth, dateBadgeDay;
		private final TextView title, timeText, locationText, descriptionText, relativeTimeText;
		private final TextView goingCountText, interestedCountText, hostText;
		private final ImageView coverImage;
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
			relativeTimeText=itemView.findViewById(android.R.id.hint);
			goingChip=itemView.findViewWithTag("goingChip");
			interestedChip=itemView.findViewWithTag("interestedChip");
			goingText=(TextView)itemView.findViewWithTag("goingText");
			interestedText=(TextView)itemView.findViewWithTag("interestedText");
			cancelledBadge=itemView.findViewWithTag("cancelledBadge");
			coverImage=(ImageView)itemView.findViewWithTag("coverImage");
			goingCountText=(TextView)itemView.findViewWithTag("goingCount");
			interestedCountText=(TextView)itemView.findViewWithTag("interestedCount");
			hostText=(TextView)itemView.findViewWithTag("hostText");
		}

		void bind(Event event){
			// Cover image
			if(coverImage!=null){
				if(!TextUtils.isEmpty(event.imageUrl)){
					coverImage.setVisibility(View.VISIBLE);
					ViewImageLoader.load(coverImage, null, new UrlImageLoaderRequest(event.imageUrl, V.dp(400), V.dp(140)));
				}else{
					coverImage.setVisibility(View.GONE);
				}
			}

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
				String desc=event.description.length()>140 ? event.description.substring(0, 140)+"\u2026" : event.description;
				descriptionText.setText(desc);
				descriptionText.setVisibility(View.VISIBLE);
			}else{
				descriptionText.setVisibility(View.GONE);
			}

			// Going / Interested counts
			if(goingCountText!=null){
				if(event.goingCount>0){
					goingCountText.setText(event.goingCount+" going");
					goingCountText.setVisibility(View.VISIBLE);
				}else{
					goingCountText.setVisibility(View.GONE);
				}
			}
			if(interestedCountText!=null){
				if(event.interestedCount>0){
					interestedCountText.setText(event.interestedCount+" interested");
					interestedCountText.setVisibility(View.VISIBLE);
				}else{
					interestedCountText.setVisibility(View.GONE);
				}
			}

			// Host info
			if(hostText!=null){
				if(event.account!=null && !TextUtils.isEmpty(event.account.displayName)){
					hostText.setText("Hosted by "+event.account.displayName);
					hostText.setVisibility(View.VISIBLE);
				}else{
					hostText.setVisibility(View.GONE);
				}
			}

			// RSVP chips
			cancelledBadge.setVisibility(event.cancelled ? View.VISIBLE : View.GONE);

			if(!event.cancelled){
				// Going chip
				boolean isGoing="going".equals(event.rsvp);
				goingChip.setVisibility(View.VISIBLE);
				goingText.setText(isGoing ? "Going \u2713" : "Going");
				styleChip(goingChip, goingText, isGoing, COLOR_GOING);

				// Interested chip
				boolean isInterested="interested".equals(event.rsvp);
				interestedChip.setVisibility(View.VISIBLE);
				interestedText.setText(isInterested ? "Interested \u2713" : "Interested");
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

	// ==================== Card View Builder ====================

	private View createEventCardView(){
		// Card container
		LinearLayout card=new LinearLayout(getActivity());
		card.setOrientation(LinearLayout.VERTICAL);
		RecyclerView.LayoutParams cardLp=new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		cardLp.bottomMargin=V.dp(12);
		card.setLayoutParams(cardLp);

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

		// Cover image at the top of the card (before topRow)
		ImageView coverImage=new ImageView(getActivity());
		coverImage.setTag("coverImage");
		coverImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
		coverImage.setVisibility(View.GONE);
		card.addView(coverImage, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(140)));

		// Content area with padding (below cover image)
		LinearLayout contentArea=new LinearLayout(getActivity());
		contentArea.setOrientation(LinearLayout.VERTICAL);
		contentArea.setPadding(V.dp(16), V.dp(14), V.dp(16), V.dp(14));

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
		contentArea.addView(topRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		// Time row
		TextView timeText=new TextView(getActivity());
		timeText.setId(android.R.id.text1);
		timeText.setTextSize(13);
		timeText.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		timeText.setCompoundDrawablePadding(V.dp(6));
		LinearLayout.LayoutParams timeLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		timeLp.topMargin=V.dp(10);
		timeLp.leftMargin=V.dp(62); // align with title (48 badge + 14 margin)
		contentArea.addView(timeText, timeLp);

		// Location row
		TextView locationText=new TextView(getActivity());
		locationText.setId(android.R.id.text2);
		locationText.setTextSize(13);
		locationText.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		locationText.setCompoundDrawablePadding(V.dp(6));
		LinearLayout.LayoutParams locLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		locLp.topMargin=V.dp(4);
		locLp.leftMargin=V.dp(62);
		contentArea.addView(locationText, locLp);

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
		contentArea.addView(descriptionText, descLp);

		// Host info row
		TextView hostText=new TextView(getActivity());
		hostText.setTag("hostText");
		hostText.setTextSize(12);
		hostText.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		hostText.setVisibility(View.GONE);
		LinearLayout.LayoutParams hostLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		hostLp.topMargin=V.dp(6);
		hostLp.leftMargin=V.dp(62);
		contentArea.addView(hostText, hostLp);

		// Counts row: "X going · Y interested"
		LinearLayout countsRow=new LinearLayout(getActivity());
		countsRow.setOrientation(LinearLayout.HORIZONTAL);
		countsRow.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout.LayoutParams countsLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		countsLp.topMargin=V.dp(4);
		countsLp.leftMargin=V.dp(62);

		TextView goingCount=new TextView(getActivity());
		goingCount.setTag("goingCount");
		goingCount.setTextSize(12);
		goingCount.setTextColor(COLOR_GOING);
		goingCount.setTypeface(null, Typeface.BOLD);
		goingCount.setVisibility(View.GONE);
		countsRow.addView(goingCount, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		TextView countSeparator=new TextView(getActivity());
		countSeparator.setTag("countSeparator");
		countSeparator.setText(" \u00b7 ");
		countSeparator.setTextSize(12);
		countSeparator.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		countsRow.addView(countSeparator, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		TextView interestedCount=new TextView(getActivity());
		interestedCount.setTag("interestedCount");
		interestedCount.setTextSize(12);
		interestedCount.setTextColor(COLOR_INTERESTED);
		interestedCount.setTypeface(null, Typeface.BOLD);
		interestedCount.setVisibility(View.GONE);
		countsRow.addView(interestedCount, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		contentArea.addView(countsRow, countsLp);

		// Bottom row: RSVP chips
		LinearLayout bottomRow=new LinearLayout(getActivity());
		bottomRow.setOrientation(LinearLayout.HORIZONTAL);
		bottomRow.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout.LayoutParams brLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		brLp.topMargin=V.dp(12);
		brLp.leftMargin=V.dp(62);

		// Spacer to push chips right
		View spacer=new View(getActivity());
		bottomRow.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1f));

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

		contentArea.addView(bottomRow, brLp);

		card.addView(contentArea, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		return card;
	}
}
