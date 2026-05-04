package org.joinmastodon.android.fragments;

import android.app.Fragment;
import android.graphics.Outline;
import android.view.WindowInsets;
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
import me.grishka.appkit.utils.V;

public class EventsFragment extends Fragment implements ScrollableToTop {
	private LinearLayout content;
	private RecyclerView list;
	private SwipeRefreshLayout refreshLayout;
	private TextView emptyView;
	private String accountID;
	private List<Event> events = new ArrayList<>();
	private List<Event> allCalendarEvents = new ArrayList<>();
	private EventsAdapter adapter;
	public boolean loaded;
	public boolean dataLoading;

	private String currentFilter = "upcoming";
	private static final String[] FILTERS = {"upcoming", "past", "mine", "invited"};
	private static final String[] FILTER_LABELS = {"Upcoming", "Past", "Mine", "Invited"};
	private LinearLayout filterContainer;
	private View[] filterChips;

	private boolean calendarMode = false;
	private View listToggle;
	private View calendarToggle;
	private LinearLayout calendarContainer;
	private LinearLayout calendarGrid;
	private TextView calendarMonthLabel;
	private YearMonth displayedMonth;

	private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("d").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter WEEKDAY_SHORT = DateTimeFormatter.ofPattern("EEE").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter MONTH_YEAR_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy");

	private static final int COLOR_BLURPLE_MID   = 0xFF6364FF;
	private static final int COLOR_BLURPLE_MUTED  = 0xFF858AFA;
	private static final int COLOR_BLURPLE_FAINT  = 0x146364FF;
	private static final int COLOR_CANCELLED      = 0xFFC75D6E;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		accountID = getArguments().getString("account");
		displayedMonth = YearMonth.now();
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		if (!hidden && !loaded && !dataLoading)
			loadData();
	}

	public void onApplyWindowInsets(WindowInsets insets) {
		if (content != null)
			content.setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
		FrameLayout root = new FrameLayout(getActivity());
		root.setBackgroundColor(UiUtils.getThemeColor(getActivity(), android.R.attr.colorBackground));

		content = new LinearLayout(getActivity());
		content.setOrientation(LinearLayout.VERTICAL);

		// Top bar
		LinearLayout topBar = new LinearLayout(getActivity());
		topBar.setOrientation(LinearLayout.HORIZONTAL);
		topBar.setGravity(Gravity.CENTER_VERTICAL);
		topBar.setPadding(V.dp(20), V.dp(10), V.dp(12), V.dp(10));

		// Filter tabs
		HorizontalScrollView filterScroll = new HorizontalScrollView(getActivity());
		filterScroll.setHorizontalScrollBarEnabled(false);
		filterScroll.setClipToPadding(false);

		filterContainer = new LinearLayout(getActivity());
		filterContainer.setOrientation(LinearLayout.HORIZONTAL);
		filterContainer.setGravity(Gravity.BOTTOM);

		filterChips = new View[FILTERS.length];
		for (int i = 0; i < FILTERS.length; i++) {
			filterChips[i] = createFilterChip(FILTER_LABELS[i], i);
			LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			if (i > 0) chipLp.leftMargin = V.dp(20);
			filterContainer.addView(filterChips[i], chipLp);
		}
		updateFilterChipStyles();

		filterScroll.addView(filterContainer, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		topBar.addView(filterScroll, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		// "+ Host" button
		TextView newEventBtn = new TextView(getActivity());
		newEventBtn.setText("+ Host");
		newEventBtn.setTextSize(13);
		newEventBtn.setTextColor(COLOR_BLURPLE_MUTED);
		newEventBtn.setLetterSpacing(0.02f);
		newEventBtn.setGravity(Gravity.CENTER);
		newEventBtn.setPadding(V.dp(12), V.dp(7), V.dp(12), V.dp(7));
		GradientDrawable newEventBg = new GradientDrawable();
		newEventBg.setCornerRadius(V.dp(8));
		newEventBg.setColor(0x00000000);
		newEventBg.setStroke(1, COLOR_BLURPLE_MUTED);
		newEventBtn.setBackground(newEventBg);
		newEventBtn.setOnClickListener(v -> {
			Bundle args = new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), CreateEventFragment.class, args);
		});
		LinearLayout.LayoutParams neLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		neLp.leftMargin = V.dp(8);
		topBar.addView(newEventBtn, neLp);

		// View toggle
		LinearLayout viewToggle = new LinearLayout(getActivity());
		viewToggle.setOrientation(LinearLayout.HORIZONTAL);
		viewToggle.setGravity(Gravity.CENTER);
		GradientDrawable toggleBg = new GradientDrawable();
		toggleBg.setCornerRadius(V.dp(8));
		toggleBg.setStroke(1, UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant));
		viewToggle.setBackground(toggleBg);
		viewToggle.setClipToOutline(true);
		viewToggle.setOutlineProvider(new ViewOutlineProvider() {
			@Override public void getOutline(View v, Outline o) {
				o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), V.dp(8));
			}
		});

		listToggle = createViewToggleButton("List");
		calendarToggle = createViewToggleButton("Month");

		listToggle.setOnClickListener(v -> {
			if (!calendarMode) return;
			calendarMode = false;
			updateViewToggleStyles();
			showListView();
		});
		calendarToggle.setOnClickListener(v -> {
			if (calendarMode) return;
			calendarMode = true;
			updateViewToggleStyles();
			showCalendarView();
		});

		viewToggle.addView(listToggle);
		viewToggle.addView(calendarToggle);
		updateViewToggleStyles();

		LinearLayout.LayoutParams vtLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		vtLp.leftMargin = V.dp(8);
		topBar.addView(viewToggle, vtLp);

		content.addView(topBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		View topDivider = new View(getActivity());
		topDivider.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant));
		content.addView(topDivider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

		// List
		refreshLayout = new SwipeRefreshLayout(getActivity());
		refreshLayout.setColorSchemeColors(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary));

		list = new RecyclerView(getActivity());
		list.setId(R.id.list);
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		adapter = new EventsAdapter(events);
		list.setAdapter(adapter);
		list.setClipToPadding(false);
		list.setPadding(0, 0, 0, V.dp(72));

		refreshLayout.addView(list, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		refreshLayout.setOnRefreshListener(this::loadData);

		// Calendar
		calendarContainer = new LinearLayout(getActivity());
		calendarContainer.setOrientation(LinearLayout.VERTICAL);
		calendarContainer.setVisibility(View.GONE);
		buildCalendarHeader();
		buildCalendarGridContainer();

		// Empty state
		emptyView = new TextView(getActivity());
		emptyView.setTextSize(14);
		emptyView.setTextColor(COLOR_BLURPLE_MUTED);
		emptyView.setTypeface(null, Typeface.ITALIC);
		emptyView.setGravity(Gravity.CENTER);
		emptyView.setPadding(V.dp(32), V.dp(60), V.dp(32), V.dp(60));
		emptyView.setVisibility(View.GONE);
		updateEmptyStateText();

		content.addView(refreshLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
		content.addView(calendarContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
		content.addView(emptyView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		root.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		return root;
	}

	// ==================== Filter Chips ====================

	private View createFilterChip(String label, int index) {
		FrameLayout tab = new FrameLayout(getActivity());

		TextView chip = new TextView(getActivity());
		chip.setText(label);
		chip.setTextSize(14);
		chip.setGravity(Gravity.CENTER);
		chip.setPadding(V.dp(2), V.dp(4), V.dp(2), V.dp(12));
		tab.addView(chip, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		View indicator = new View(getActivity());
		indicator.setTag("indicator");
		indicator.setBackgroundColor(COLOR_BLURPLE_MID);
		FrameLayout.LayoutParams indLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(2));
		indLp.gravity = Gravity.BOTTOM;
		tab.addView(indicator, indLp);

		tab.setOnClickListener(v -> {
			if (currentFilter.equals(FILTERS[index])) return;
			currentFilter = FILTERS[index];
			updateFilterChipStyles();
			updateEmptyStateText();
			if (calendarMode) loadCalendarEvents();
			else loadData();
		});
		return tab;
	}

	private void updateFilterChipStyles() {
		for (int i = 0; i < filterChips.length; i++) {
			FrameLayout tab = (FrameLayout) filterChips[i];
			TextView chip = (TextView) tab.getChildAt(0);
			View indicator = tab.findViewWithTag("indicator");
			boolean selected = currentFilter.equals(FILTERS[i]);
			if (selected) {
				chip.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
				chip.setTypeface(null, Typeface.BOLD);
				indicator.setVisibility(View.VISIBLE);
			} else {
				chip.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
				chip.setTypeface(null, Typeface.NORMAL);
				indicator.setVisibility(View.INVISIBLE);
			}
		}
	}

	private void updateEmptyStateText() {
		if (emptyView == null) return;
		switch (currentFilter) {
			case "past":    emptyView.setText("No past events"); break;
			case "mine":    emptyView.setText("No events hosted yet"); break;
			case "invited": emptyView.setText("No event invitations"); break;
			default:        emptyView.setText("No upcoming events");
		}
	}

	// ==================== View Toggle ====================

	private View createViewToggleButton(String label) {
		TextView btn = new TextView(getActivity());
		btn.setText(label);
		btn.setTextSize(12);
		btn.setGravity(Gravity.CENTER);
		btn.setPadding(V.dp(12), V.dp(6), V.dp(12), V.dp(6));
		return btn;
	}

	private void updateViewToggleStyles() {
		int surfaceColor = UiUtils.getThemeColor(getActivity(), R.attr.colorM3Surface);
		int secondaryText = UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary);
		int primaryText = UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary);

		TextView listBtn = (TextView) listToggle;
		GradientDrawable listBg = new GradientDrawable();
		listBg.setCornerRadii(new float[]{V.dp(7), V.dp(7), 0, 0, 0, 0, V.dp(7), V.dp(7)});
		if (!calendarMode) {
			listBg.setColor(surfaceColor);
			listBtn.setTextColor(primaryText);
			listBtn.setTypeface(null, Typeface.BOLD);
		} else {
			listBg.setColor(0x00000000);
			listBtn.setTextColor(secondaryText);
			listBtn.setTypeface(null, Typeface.NORMAL);
		}
		listToggle.setBackground(listBg);

		TextView calBtn = (TextView) calendarToggle;
		GradientDrawable calBg = new GradientDrawable();
		calBg.setCornerRadii(new float[]{0, 0, V.dp(7), V.dp(7), V.dp(7), V.dp(7), 0, 0});
		if (calendarMode) {
			calBg.setColor(surfaceColor);
			calBtn.setTextColor(primaryText);
			calBtn.setTypeface(null, Typeface.BOLD);
		} else {
			calBg.setColor(0x00000000);
			calBtn.setTextColor(secondaryText);
			calBtn.setTypeface(null, Typeface.NORMAL);
		}
		calendarToggle.setBackground(calBg);
	}

	private void showListView() {
		calendarContainer.setVisibility(View.GONE);
		boolean empty = events.isEmpty() && loaded;
		refreshLayout.setVisibility(empty ? View.GONE : View.VISIBLE);
		emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
		if (!loaded) loadData();
	}

	private void showCalendarView() {
		refreshLayout.setVisibility(View.GONE);
		emptyView.setVisibility(View.GONE);
		calendarContainer.setVisibility(View.VISIBLE);
		loadCalendarEvents();
	}

	// ==================== Calendar Header ====================

	private void buildCalendarHeader() {
		LinearLayout header = new LinearLayout(getActivity());
		header.setOrientation(LinearLayout.HORIZONTAL);
		header.setGravity(Gravity.CENTER_VERTICAL);
		header.setPadding(V.dp(16), V.dp(12), V.dp(16), V.dp(8));

		header.addView(buildNavButton("←", v -> {
			displayedMonth = displayedMonth.minusMonths(1);
			updateCalendarMonthLabel();
			rebuildCalendarGrid();
		}), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		calendarMonthLabel = new TextView(getActivity());
		calendarMonthLabel.setTextSize(16);
		calendarMonthLabel.setTypeface(null, Typeface.BOLD);
		calendarMonthLabel.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
		calendarMonthLabel.setGravity(Gravity.CENTER);
		updateCalendarMonthLabel();
		header.addView(calendarMonthLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		header.addView(buildNavButton("→", v -> {
			displayedMonth = displayedMonth.plusMonths(1);
			updateCalendarMonthLabel();
			rebuildCalendarGrid();
		}), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		calendarContainer.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	}

	private TextView buildNavButton(String arrow, View.OnClickListener listener) {
		TextView btn = new TextView(getActivity());
		btn.setText(arrow);
		btn.setTextSize(16);
		btn.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		btn.setGravity(Gravity.CENTER);
		btn.setPadding(V.dp(14), V.dp(7), V.dp(14), V.dp(7));
		GradientDrawable bg = new GradientDrawable();
		bg.setCornerRadius(V.dp(8));
		bg.setColor(0x00000000);
		bg.setStroke(V.dp(1), UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant));
		btn.setBackground(bg);
		btn.setOnClickListener(listener);
		return btn;
	}

	private void updateCalendarMonthLabel() {
		calendarMonthLabel.setText(displayedMonth.format(MONTH_YEAR_FORMAT));
	}

	// ==================== Calendar Grid ====================

	private void buildCalendarGridContainer() {
		LinearLayout weekdayRow = new LinearLayout(getActivity());
		weekdayRow.setOrientation(LinearLayout.HORIZONTAL);
		weekdayRow.setPadding(V.dp(8), 0, V.dp(8), V.dp(4));
		DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY};
		for (DayOfWeek dow : days) {
			TextView tv = new TextView(getActivity());
			tv.setText(dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()).toUpperCase(Locale.ROOT));
			tv.setTextSize(11);
			tv.setTypeface(null, Typeface.BOLD);
			tv.setTextColor(COLOR_BLURPLE_MUTED);
			tv.setGravity(Gravity.CENTER);
			tv.setLetterSpacing(0.06f);
			weekdayRow.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
		}
		calendarContainer.addView(weekdayRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		calendarGrid = new LinearLayout(getActivity());
		calendarGrid.setOrientation(LinearLayout.VERTICAL);
		calendarGrid.setPadding(V.dp(8), 0, V.dp(8), V.dp(8));
		calendarContainer.addView(calendarGrid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	}

	private void rebuildCalendarGrid() {
		calendarGrid.removeAllViews();
		LocalDate today = LocalDate.now();
		LocalDate first = displayedMonth.atDay(1);
		int startDow = first.getDayOfWeek().getValue();
		int daysInMonth = displayedMonth.lengthOfMonth();

		Map<Integer, List<Event>> eventsByDay = new HashMap<>();
		for (Event e : allCalendarEvents) {
			if (e.startTime == null) continue;
			LocalDate ed = e.startTime.atZone(ZoneId.systemDefault()).toLocalDate();
			if (ed.getYear() == displayedMonth.getYear() && ed.getMonthValue() == displayedMonth.getMonthValue()) {
				eventsByDay.computeIfAbsent(ed.getDayOfMonth(), k -> new ArrayList<>()).add(e);
			}
		}

		int dayCounter = 1;
		int rows = (int) Math.ceil(((startDow - 1) + daysInMonth) / 7.0);

		for (int row = 0; row < rows; row++) {
			LinearLayout rowLayout = new LinearLayout(getActivity());
			rowLayout.setOrientation(LinearLayout.HORIZONTAL);

			for (int col = 0; col < 7; col++) {
				int globalIndex = row * 7 + col;
				if (globalIndex < (startDow - 1) || dayCounter > daysInMonth) {
					View empty = new View(getActivity());
					GradientDrawable emptyBg = new GradientDrawable();
					emptyBg.setStroke(1, UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant));
					empty.setBackground(emptyBg);
					empty.setAlpha(0.3f);
					LinearLayout.LayoutParams emptyLp = new LinearLayout.LayoutParams(0, V.dp(80), 1f);
					emptyLp.setMargins(1, 1, 1, 1);
					rowLayout.addView(empty, emptyLp);
				} else {
					int day = dayCounter;
					LocalDate cellDate = displayedMonth.atDay(day);
					boolean isToday = cellDate.equals(today);
					List<Event> dayEvents = eventsByDay.get(day);

					LinearLayout cell = new LinearLayout(getActivity());
					cell.setOrientation(LinearLayout.VERTICAL);
					cell.setPadding(V.dp(4), V.dp(4), V.dp(4), V.dp(4));

					GradientDrawable cellBg = new GradientDrawable();
					if (isToday) {
						cellBg.setColor(COLOR_BLURPLE_FAINT);
						cellBg.setStroke(V.dp(2), COLOR_BLURPLE_MID);
					} else {
						cellBg.setColor(0x00000000);
						cellBg.setStroke(1, UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant));
					}
					cell.setBackground(cellBg);

					TextView dayTv = new TextView(getActivity());
					dayTv.setText(String.valueOf(day));
					dayTv.setTextSize(12);
					dayTv.setGravity(Gravity.CENTER);
					dayTv.setTextColor(isToday ? COLOR_BLURPLE_MID : UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
					if (isToday) dayTv.setTypeface(null, Typeface.BOLD);
					cell.addView(dayTv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

					if (dayEvents != null && !dayEvents.isEmpty()) {
						int pillCount = Math.min(dayEvents.size(), 3);
						for (int d = 0; d < pillCount; d++) {
							Event evt = dayEvents.get(d);
							TextView pill = new TextView(getActivity());
							String pillTitle = evt.title != null ? evt.title : "";
							if (pillTitle.length() > 10) pillTitle = pillTitle.substring(0, 10);
							pill.setText(pillTitle);
							pill.setTextSize(9);
							pill.setTypeface(null, Typeface.BOLD);
							pill.setSingleLine(true);
							pill.setTextColor(0xFFFFFFFF);
							pill.setPadding(V.dp(4), V.dp(1), V.dp(4), V.dp(1));
							GradientDrawable pillBg = new GradientDrawable();
							pillBg.setCornerRadius(V.dp(4));
							pillBg.setColor(COLOR_BLURPLE_MID);
							pill.setBackground(pillBg);
							LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
							pillLp.topMargin = V.dp(2);
							cell.addView(pill, pillLp);
						}
						if (dayEvents.size() > 3) {
							TextView more = new TextView(getActivity());
							more.setText("+" + (dayEvents.size() - 3));
							more.setTextSize(8);
							more.setTextColor(COLOR_BLURPLE_MUTED);
							LinearLayout.LayoutParams moreLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
							moreLp.topMargin = V.dp(1);
							cell.addView(more, moreLp);
						}
					}

					final List<Event> finalDayEvents = dayEvents;
					cell.setOnClickListener(v -> {
						if (finalDayEvents != null && !finalDayEvents.isEmpty())
							openEventDetail(finalDayEvents.get(0));
					});

					LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(0, V.dp(80), 1f);
					cellLp.setMargins(1, 1, 1, 1);
					rowLayout.addView(cell, cellLp);
					dayCounter++;
				}
			}
			calendarGrid.addView(rowLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		}
	}

	// ==================== Calendar Data ====================

	private void loadCalendarEvents() {
		allCalendarEvents.clear();
		new GetEvents(currentFilter.equals("upcoming") ? "upcoming" : currentFilter, null, 200)
				.setCallback(new Callback<>() {
					@Override
					public void onSuccess(List<Event> result) {
						if (getActivity() == null) return;
						allCalendarEvents.addAll(result);
						if ("upcoming".equals(currentFilter)) loadPastForCalendar();
						else rebuildCalendarGrid();
					}
					@Override
					public void onError(ErrorResponse error) {
						if (getActivity() != null) rebuildCalendarGrid();
					}
				})
				.exec(accountID);
	}

	private void loadPastForCalendar() {
		new GetEvents("past", null, 200)
				.setCallback(new Callback<>() {
					@Override
					public void onSuccess(List<Event> result) {
						if (getActivity() == null) return;
						for (Event e : result) {
							boolean dup = false;
							for (Event ex : allCalendarEvents) {
								if (ex.id.equals(e.id)) { dup = true; break; }
							}
							if (!dup) allCalendarEvents.add(e);
						}
						rebuildCalendarGrid();
					}
					@Override
					public void onError(ErrorResponse error) {
						if (getActivity() != null) rebuildCalendarGrid();
					}
				})
				.exec(accountID);
	}

	// ==================== Data Loading ====================

	public void loadData() {
		if (dataLoading) return;
		dataLoading = true;
		if (calendarMode) {
			dataLoading = false;
			loadCalendarEvents();
			return;
		}
		new GetEvents(currentFilter, null, 40)
				.setCallback(new Callback<>() {
					@Override
					public void onSuccess(List<Event> result) {
						if (getActivity() == null) return;
						dataLoading = false;
						loaded = true;
						refreshLayout.setRefreshing(false);
						events.clear();
						events.addAll(result);
						adapter.notifyDataSetChanged();
						boolean empty = events.isEmpty();
						emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
						refreshLayout.setVisibility(empty ? View.GONE : View.VISIBLE);
					}
					@Override
					public void onError(ErrorResponse error) {
						if (getActivity() == null) return;
						dataLoading = false;
						refreshLayout.setRefreshing(false);
						emptyView.setVisibility(View.VISIBLE);
						refreshLayout.setVisibility(View.GONE);
					}
				})
				.exec(accountID);
	}

	@Override
	public void scrollToTop() {
		if (list != null && !calendarMode) list.smoothScrollToPosition(0);
	}

	// ==================== Helpers ====================

	private String buildMetaLine(Event event) {
		StringBuilder sb = new StringBuilder();
		if (event.startTime != null) {
			sb.append(WEEKDAY_SHORT.format(event.startTime));
			sb.append(" ");
			sb.append(TIME_FORMAT.format(event.startTime));
			if (event.endTime != null) {
				sb.append("–").append(TIME_FORMAT.format(event.endTime));
			}
		}
		if (!TextUtils.isEmpty(event.locationName)) {
			if (sb.length() > 0) sb.append(" · ");
			sb.append(event.locationName);
		}
		if (event.goingCount > 0) {
			if (sb.length() > 0) sb.append(" · ");
			sb.append(event.goingCount).append(" going");
		}
		if (event.interestedCount > 0) {
			if (sb.length() > 0) sb.append(" · ");
			sb.append(event.interestedCount).append(" maybe");
		}
		if (event.account != null && !TextUtils.isEmpty(event.account.displayName)) {
			if (sb.length() > 0) sb.append(" · ");
			sb.append("by ").append(event.account.displayName);
		}
		return sb.toString();
	}

	private void openEventDetail(Event event) {
		Bundle args = new Bundle();
		args.putString("account", accountID);
		args.putParcelable("event", Parcels.wrap(event));
		Nav.go(getActivity(), EventDetailFragment.class, args);
	}

	private void doRsvp(Event event, String status) {
		new RsvpEvent(event.id, status)
				.setCallback(new Callback<>() {
					@Override
					public void onSuccess(Event result) {
						if (getActivity() == null) return;
						int idx = events.indexOf(event);
						if (idx >= 0) {
							events.set(idx, result);
							adapter.notifyItemChanged(idx);
						}
						for (int i = 0; i < allCalendarEvents.size(); i++) {
							if (allCalendarEvents.get(i).id.equals(event.id)) {
								allCalendarEvents.set(i, result);
								break;
							}
						}
						if (calendarMode) rebuildCalendarGrid();
					}
					@Override
					public void onError(ErrorResponse error) {
						if (getActivity() != null) error.showToast(getActivity());
					}
				})
				.exec(accountID);
	}

	// ==================== Adapter ====================

	private class EventsAdapter extends RecyclerView.Adapter<EventViewHolder> {
		private final List<Event> data;
		EventsAdapter(List<Event> data) { this.data = data; }

		@Override
		public EventViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			return new EventViewHolder();
		}
		@Override
		public void onBindViewHolder(EventViewHolder holder, int position) {
			holder.bind(data.get(position));
		}
		@Override
		public int getItemCount() { return data.size(); }
	}

	// ==================== ViewHolder ====================

	private class EventViewHolder extends RecyclerView.ViewHolder {
		private final TextView monthText, dayText, titleText, metaText, cancelledBadge;
		private final LinearLayout goingBtn, maybeBtn, skipBtn;
		private final TextView goingBtnText, maybeBtnText, skipBtnText;
		private final LinearLayout rsvpGroup;

		EventViewHolder() {
			super(createEventCardView());
			monthText = itemView.findViewWithTag("monthText");
			dayText = itemView.findViewWithTag("dayText");
			titleText = itemView.findViewWithTag("titleText");
			metaText = itemView.findViewWithTag("metaText");
			cancelledBadge = itemView.findViewWithTag("cancelledBadge");
			goingBtn = itemView.findViewWithTag("goingBtn");
			maybeBtn = itemView.findViewWithTag("maybeBtn");
			skipBtn = itemView.findViewWithTag("skipBtn");
			goingBtnText = itemView.findViewWithTag("goingBtnText");
			maybeBtnText = itemView.findViewWithTag("maybeBtnText");
			skipBtnText = itemView.findViewWithTag("skipBtnText");
			rsvpGroup = itemView.findViewWithTag("rsvpGroup");
		}

		void bind(Event event) {
			if (event.startTime != null) {
				monthText.setText(MONTH_FORMAT.format(event.startTime).toUpperCase(Locale.ROOT));
				dayText.setText(DAY_FORMAT.format(event.startTime));
			} else {
				monthText.setText("");
				dayText.setText("");
			}

			cancelledBadge.setVisibility(event.cancelled ? View.VISIBLE : View.GONE);
			titleText.setText(event.title);
			titleText.setAlpha(event.cancelled ? 0.5f : 1f);

			String meta = buildMetaLine(event);
			metaText.setText(meta);
			metaText.setVisibility(TextUtils.isEmpty(meta) ? View.GONE : View.VISIBLE);

			if (!event.cancelled) {
				rsvpGroup.setVisibility(View.VISIBLE);
				boolean isGoing = "going".equals(event.rsvp);
				boolean isMaybe = "interested".equals(event.rsvp);

				applyRsvpStyle(goingBtnText, isGoing);
				applyRsvpStyle(maybeBtnText, isMaybe);
				applyRsvpStyle(skipBtnText, false);

				goingBtn.setOnClickListener(v -> doRsvp(event, isGoing ? "remove" : "going"));
				maybeBtn.setOnClickListener(v -> doRsvp(event, isMaybe ? "remove" : "interested"));
				skipBtn.setOnClickListener(v -> doRsvp(event, "remove"));
			} else {
				rsvpGroup.setVisibility(View.GONE);
			}

			itemView.setOnClickListener(v -> openEventDetail(event));
		}

		private void applyRsvpStyle(TextView label, boolean active) {
			label.setTextColor(active
					? UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary)
					: UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
			label.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
		}
	}

	// ==================== Card View Builder ====================

	private View createEventCardView() {
		// Wrapper: row + bottom divider
		LinearLayout wrapper = new LinearLayout(getActivity());
		wrapper.setOrientation(LinearLayout.VERTICAL);

		// Row
		LinearLayout row = new LinearLayout(getActivity());
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding(V.dp(16), V.dp(14), V.dp(16), V.dp(14));

		// Date column
		LinearLayout dateCol = new LinearLayout(getActivity());
		dateCol.setOrientation(LinearLayout.VERTICAL);
		dateCol.setGravity(Gravity.CENTER_HORIZONTAL);
		LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(V.dp(44), ViewGroup.LayoutParams.WRAP_CONTENT);
		dateLp.gravity = Gravity.TOP;
		dateLp.topMargin = V.dp(2);

		TextView monthText = new TextView(getActivity());
		monthText.setTag("monthText");
		monthText.setTextSize(10);
		monthText.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		monthText.setLetterSpacing(0.10f);
		monthText.setGravity(Gravity.CENTER);
		dateCol.addView(monthText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		TextView dayText = new TextView(getActivity());
		dayText.setTag("dayText");
		dayText.setTextSize(28);
		dayText.setTypeface(Typeface.SERIF, Typeface.NORMAL);
		dayText.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
		dayText.setGravity(Gravity.CENTER);
		dayText.setIncludeFontPadding(false);
		dateCol.addView(dayText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		row.addView(dateCol, dateLp);

		// Content column
		LinearLayout contentCol = new LinearLayout(getActivity());
		contentCol.setOrientation(LinearLayout.VERTICAL);
		contentCol.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		contentLp.leftMargin = V.dp(14);

		TextView cancelledBadge = new TextView(getActivity());
		cancelledBadge.setTag("cancelledBadge");
		cancelledBadge.setText("CANCELLED");
		cancelledBadge.setTextSize(9);
		cancelledBadge.setTypeface(null, Typeface.BOLD);
		cancelledBadge.setTextColor(0xFFFFFFFF);
		cancelledBadge.setLetterSpacing(0.06f);
		cancelledBadge.setPadding(V.dp(5), V.dp(2), V.dp(5), V.dp(2));
		GradientDrawable cancelBg = new GradientDrawable();
		cancelBg.setCornerRadius(V.dp(4));
		cancelBg.setColor(COLOR_CANCELLED);
		cancelledBadge.setBackground(cancelBg);
		cancelledBadge.setVisibility(View.GONE);
		LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		cbLp.bottomMargin = V.dp(3);
		contentCol.addView(cancelledBadge, cbLp);

		TextView titleText = new TextView(getActivity());
		titleText.setTag("titleText");
		titleText.setTextSize(16);
		titleText.setTypeface(Typeface.SERIF, Typeface.NORMAL);
		titleText.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
		titleText.setSingleLine(true);
		titleText.setEllipsize(TextUtils.TruncateAt.END);
		contentCol.addView(titleText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		TextView metaText = new TextView(getActivity());
		metaText.setTag("metaText");
		metaText.setTextSize(12);
		metaText.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		metaText.setSingleLine(true);
		metaText.setEllipsize(TextUtils.TruncateAt.END);
		LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		metaLp.topMargin = V.dp(3);
		contentCol.addView(metaText, metaLp);

		row.addView(contentCol, contentLp);

		// RSVP segmented control
		LinearLayout rsvpGroup = new LinearLayout(getActivity());
		rsvpGroup.setTag("rsvpGroup");
		rsvpGroup.setOrientation(LinearLayout.HORIZONTAL);
		rsvpGroup.setGravity(Gravity.CENTER_VERTICAL);
		rsvpGroup.setClipToOutline(true);
		rsvpGroup.setOutlineProvider(new ViewOutlineProvider() {
			@Override public void getOutline(View v, Outline o) {
				o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), V.dp(8));
			}
		});
		GradientDrawable rsvpBg = new GradientDrawable();
		rsvpBg.setCornerRadius(V.dp(8));
		rsvpBg.setStroke(1, UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant));
		rsvpGroup.setBackground(rsvpBg);

		rsvpGroup.addView(buildRsvpSegment("Going", "goingBtn", "goingBtnText"));
		rsvpGroup.addView(buildRsvpSeparator(), new LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT));
		rsvpGroup.addView(buildRsvpSegment("Maybe", "maybeBtn", "maybeBtnText"));
		rsvpGroup.addView(buildRsvpSeparator(), new LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT));
		rsvpGroup.addView(buildRsvpSegment("Skip", "skipBtn", "skipBtnText"));

		LinearLayout.LayoutParams rsvpLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		rsvpLp.leftMargin = V.dp(10);
		rsvpLp.gravity = Gravity.CENTER_VERTICAL;
		row.addView(rsvpGroup, rsvpLp);

		wrapper.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		View divider = new View(getActivity());
		divider.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant));
		wrapper.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

		RecyclerView.LayoutParams wrapLp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		wrapper.setLayoutParams(wrapLp);

		return wrapper;
	}

	private LinearLayout buildRsvpSegment(String label, String btnTag, String textTag) {
		LinearLayout seg = new LinearLayout(getActivity());
		seg.setTag(btnTag);
		seg.setGravity(Gravity.CENTER);
		seg.setPadding(V.dp(11), V.dp(5), V.dp(11), V.dp(5));

		TextView text = new TextView(getActivity());
		text.setTag(textTag);
		text.setText(label);
		text.setTextSize(11);
		text.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		text.setSingleLine(true);
		seg.addView(text);
		return seg;
	}

	private View buildRsvpSeparator() {
		View sep = new View(getActivity());
		sep.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant));
		return sep;
	}
}
