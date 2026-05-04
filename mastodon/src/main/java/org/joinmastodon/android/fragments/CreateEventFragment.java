package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.events.CreateEvent;
import org.joinmastodon.android.model.Event;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Calendar;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.V;

public class CreateEventFragment extends MastodonToolbarFragment{
	private String accountID;

	private boolean isHuddle=false;
	private EditText titleInput, locationNameInput, locationUrlInput, descriptionInput;
	private TextView startDateBtn, startTimeBtn, endDateBtn, endTimeBtn;
	private LinearLayout locationNameRow;
	private Spinner visibilitySpinner, recurrenceSpinner;
	private CheckBox rsvpCheckbox;
	private View eventToggle, huddleToggle;
	private TextView eventToggleText, huddleToggleText;

	private LocalDate startDate, endDate;
	private LocalTime startTime, endTime;

	private static final DateTimeFormatter DATE_FMT=DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
	private static final DateTimeFormatter TIME_FMT=DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);

	private static final String[] VISIBILITY_OPTIONS={"Public", "Unlisted", "Followers only", "Mentioned only"};
	private static final String[] VISIBILITY_VALUES={"public", "unlisted", "private", "direct"};
	private static final String[] RECURRENCE_OPTIONS={"None", "Daily", "Weekly", "Fortnightly", "Monthly"};
	private static final String[] RECURRENCE_VALUES={null, "daily", "weekly", "fortnightly", "monthly"};

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setTitle("New Event");
		accountID=getArguments().getString("account");
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		ScrollView scrollView=new ScrollView(getActivity());
		scrollView.setFillViewport(true);

		LinearLayout content=new LinearLayout(getActivity());
		content.setOrientation(LinearLayout.VERTICAL);
		content.setPadding(V.dp(20), V.dp(16), V.dp(20), V.dp(32));

		int textPrimary=UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary);
		int textSecondary=UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary);
		int primaryColor=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary);
		int onPrimary=UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnPrimary);
		int surfaceVariant=UiUtils.getThemeColor(getActivity(), R.attr.colorM3SurfaceVariant);
		int outline=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Outline);

		// Top accent gradient bar
		View accentBar=new View(getActivity());
		GradientDrawable accentGrad=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{primaryColor, (primaryColor & 0x00FFFFFF) | 0xB3000000});
		accentGrad.setCornerRadii(new float[]{V.dp(2), V.dp(2), V.dp(2), V.dp(2), 0, 0, 0, 0});
		accentBar.setBackground(accentGrad);
		LinearLayout.LayoutParams accentLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(3));
		accentLp.bottomMargin=V.dp(20);
		content.addView(accentBar, accentLp);

		// Event Type Toggle
		content.addView(createSectionLabel("Event Type", textSecondary));

		LinearLayout toggleRow=new LinearLayout(getActivity());
		toggleRow.setOrientation(LinearLayout.HORIZONTAL);
		toggleRow.setGravity(Gravity.CENTER_VERTICAL);
		GradientDrawable toggleBg=new GradientDrawable();
		toggleBg.setCornerRadius(V.dp(20));
		toggleBg.setColor(surfaceVariant);
		toggleRow.setBackground(toggleBg);
		toggleRow.setPadding(V.dp(4), V.dp(4), V.dp(4), V.dp(4));

		eventToggle=createTogglePill("Event", true, primaryColor, onPrimary, surfaceVariant, textPrimary);
		eventToggleText=(TextView)((LinearLayout)eventToggle).getChildAt(0);
		huddleToggle=createTogglePill("Huddle", false, primaryColor, onPrimary, surfaceVariant, textPrimary);
		huddleToggleText=(TextView)((LinearLayout)huddleToggle).getChildAt(0);

		eventToggle.setOnClickListener(v->{
			if(!isHuddle) return;
			isHuddle=false;
			updateToggleState(primaryColor, onPrimary, surfaceVariant, textPrimary);
			locationNameRow.setVisibility(View.VISIBLE);
		});
		huddleToggle.setOnClickListener(v->{
			if(isHuddle) return;
			isHuddle=true;
			updateToggleState(primaryColor, onPrimary, surfaceVariant, textPrimary);
			locationNameRow.setVisibility(View.GONE);
		});

		LinearLayout.LayoutParams togglePillLp=new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		togglePillLp.rightMargin=V.dp(2);
		toggleRow.addView(eventToggle, togglePillLp);
		LinearLayout.LayoutParams togglePillLp2=new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		togglePillLp2.leftMargin=V.dp(2);
		toggleRow.addView(huddleToggle, togglePillLp2);

		LinearLayout.LayoutParams toggleRowLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		toggleRowLp.topMargin=V.dp(6);
		toggleRowLp.bottomMargin=V.dp(16);
		content.addView(toggleRow, toggleRowLp);

		// Title Input
		content.addView(createSectionLabel("Title *", textSecondary));
		titleInput=createEditText("Event title", InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
		titleInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});
		content.addView(titleInput, createInputLp());

		// Start Date/Time
		content.addView(createSectionLabel("Start *", textSecondary));
		LinearLayout startRow=new LinearLayout(getActivity());
		startRow.setOrientation(LinearLayout.HORIZONTAL);

		startDateBtn=createPickerButton("Select date", outline, textPrimary);
		startDateBtn.setOnClickListener(v->{
			Calendar cal=Calendar.getInstance();
			if(startDate!=null){
				cal.set(startDate.getYear(), startDate.getMonthValue()-1, startDate.getDayOfMonth());
			}
			new DatePickerDialog(getActivity(), (dp, y, m, d)->{
				startDate=LocalDate.of(y, m+1, d);
				startDateBtn.setText(DATE_FMT.format(startDate));
			}, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
		});
		LinearLayout.LayoutParams startDateLp=new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		startDateLp.rightMargin=V.dp(8);
		startRow.addView(startDateBtn, startDateLp);

		startTimeBtn=createPickerButton("Select time", outline, textPrimary);
		startTimeBtn.setOnClickListener(v->{
			int hour=startTime!=null ? startTime.getHour() : 12;
			int minute=startTime!=null ? startTime.getMinute() : 0;
			new TimePickerDialog(getActivity(), (tp, h, m)->{
				startTime=LocalTime.of(h, m);
				startTimeBtn.setText(TIME_FMT.format(startTime));
			}, hour, minute, false).show();
		});
		LinearLayout.LayoutParams startTimeLp=new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		startRow.addView(startTimeBtn, startTimeLp);

		content.addView(startRow, createInputLp());

		// End Date/Time
		content.addView(createSectionLabel("End", textSecondary));
		LinearLayout endRow=new LinearLayout(getActivity());
		endRow.setOrientation(LinearLayout.HORIZONTAL);

		endDateBtn=createPickerButton("Select date", outline, textPrimary);
		endDateBtn.setOnClickListener(v->{
			Calendar cal=Calendar.getInstance();
			if(endDate!=null){
				cal.set(endDate.getYear(), endDate.getMonthValue()-1, endDate.getDayOfMonth());
			}else if(startDate!=null){
				cal.set(startDate.getYear(), startDate.getMonthValue()-1, startDate.getDayOfMonth());
			}
			new DatePickerDialog(getActivity(), (dp, y, m, d)->{
				endDate=LocalDate.of(y, m+1, d);
				endDateBtn.setText(DATE_FMT.format(endDate));
			}, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
		});
		LinearLayout.LayoutParams endDateLp=new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		endDateLp.rightMargin=V.dp(8);
		endRow.addView(endDateBtn, endDateLp);

		endTimeBtn=createPickerButton("Select time", outline, textPrimary);
		endTimeBtn.setOnClickListener(v->{
			int hour=endTime!=null ? endTime.getHour() : 13;
			int minute=endTime!=null ? endTime.getMinute() : 0;
			new TimePickerDialog(getActivity(), (tp, h, m)->{
				endTime=LocalTime.of(h, m);
				endTimeBtn.setText(TIME_FMT.format(endTime));
			}, hour, minute, false).show();
		});
		LinearLayout.LayoutParams endTimeLp=new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		endRow.addView(endTimeBtn, endTimeLp);

		content.addView(endRow, createInputLp());

		// Location Name
		locationNameRow=new LinearLayout(getActivity());
		locationNameRow.setOrientation(LinearLayout.VERTICAL);
		locationNameRow.addView(createSectionLabel("Location", textSecondary));
		locationNameInput=createEditText("Location name", InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);
		locationNameRow.addView(locationNameInput, createInputLp());
		content.addView(locationNameRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		// Location URL
		content.addView(createSectionLabel("Link", textSecondary));
		locationUrlInput=createEditText("URL or meeting link", InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
		content.addView(locationUrlInput, createInputLp());

		// Description
		content.addView(createSectionLabel("Description", textSecondary));
		descriptionInput=createEditText("Describe your event...", InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
		descriptionInput.setMinLines(3);
		descriptionInput.setGravity(Gravity.TOP|Gravity.START);
		content.addView(descriptionInput, createInputLp());

		// Visibility
		content.addView(createSectionLabel("Visibility", textSecondary));
		visibilitySpinner=new Spinner(getActivity());
		ArrayAdapter<String> visAdapter=new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, VISIBILITY_OPTIONS);
		visAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		visibilitySpinner.setAdapter(visAdapter);
		content.addView(visibilitySpinner, createInputLp());

		// Recurrence
		content.addView(createSectionLabel("Recurrence", textSecondary));
		recurrenceSpinner=new Spinner(getActivity());
		ArrayAdapter<String> recAdapter=new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, RECURRENCE_OPTIONS);
		recAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		recurrenceSpinner.setAdapter(recAdapter);
		content.addView(recurrenceSpinner, createInputLp());

		// RSVP Checkbox
		rsvpCheckbox=new CheckBox(getActivity());
		rsvpCheckbox.setText("Enable RSVPs");
		rsvpCheckbox.setTextSize(15);
		rsvpCheckbox.setTextColor(textPrimary);
		rsvpCheckbox.setChecked(true);
		LinearLayout.LayoutParams rsvpLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		rsvpLp.topMargin=V.dp(16);
		content.addView(rsvpCheckbox, rsvpLp);

		// Divider
		View divider=new View(getActivity());
		divider.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant));
		LinearLayout.LayoutParams divLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(1));
		divLp.topMargin=V.dp(20);
		divLp.bottomMargin=V.dp(20);
		divLp.leftMargin=V.dp(20);
		divLp.rightMargin=V.dp(20);
		content.addView(divider, divLp);

		// Create Button
		TextView createBtn=new TextView(getActivity());
		createBtn.setText("Create Event");
		createBtn.setTextSize(16);
		createBtn.setTypeface(null, Typeface.BOLD);
		createBtn.setTextColor(onPrimary);
		createBtn.setGravity(Gravity.CENTER);
		createBtn.setPadding(V.dp(16), V.dp(14), V.dp(16), V.dp(14));
		GradientDrawable createBg=new GradientDrawable();
		createBg.setCornerRadius(V.dp(14));
		createBg.setColor(primaryColor);
		createBtn.setBackground(createBg);
		createBtn.setElevation(V.dp(2));
		createBtn.setLetterSpacing(0.02f);
		createBtn.setOnClickListener(v->onCreateClick());
		content.addView(createBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		scrollView.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		return scrollView;
	}

	private void onCreateClick(){
		String title=titleInput.getText().toString().trim();
		if(TextUtils.isEmpty(title)){
			titleInput.setError("Title is required");
			titleInput.requestFocus();
			return;
		}
		if(startDate==null || startTime==null){
			startDateBtn.setError("Required");
			return;
		}

		ZonedDateTime startZdt=ZonedDateTime.of(LocalDateTime.of(startDate, startTime), ZoneId.systemDefault());
		String startTimeStr=startZdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

		String endTimeStr=null;
		if(endDate!=null && endTime!=null){
			ZonedDateTime endZdt=ZonedDateTime.of(LocalDateTime.of(endDate, endTime), ZoneId.systemDefault());
			endTimeStr=endZdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		}

		String description=descriptionInput.getText().toString().trim();
		String locName=locationNameInput.getText().toString().trim();
		String locUrl=locationUrlInput.getText().toString().trim();
		String eventType=isHuddle ? "huddle" : "event";
		String visibility=VISIBILITY_VALUES[visibilitySpinner.getSelectedItemPosition()];
		String recurrence=RECURRENCE_VALUES[recurrenceSpinner.getSelectedItemPosition()];
		boolean rsvpEnabled=rsvpCheckbox.isChecked();

		new CreateEvent(
				title,
				description.isEmpty() ? null : description,
				startTimeStr,
				endTimeStr,
				locName.isEmpty() ? null : locName,
				locUrl.isEmpty() ? null : locUrl,
				eventType,
				rsvpEnabled,
				null,
				recurrence,
				null,
				visibility,
				true
		).setCallback(new Callback<>(){
			@Override
			public void onSuccess(Event result){
				if(getActivity()==null) return;
				Nav.finish(CreateEventFragment.this);
			}

			@Override
			public void onError(ErrorResponse error){
				if(getActivity()!=null) error.showToast(getActivity());
			}
		}).wrapProgress(getActivity(), R.string.loading, true)
		.exec(accountID);
	}

	private void updateToggleState(int primaryColor, int onPrimary, int surfaceVariant, int textPrimary){
		styleTogglePill(eventToggle, eventToggleText, !isHuddle, primaryColor, onPrimary, surfaceVariant, textPrimary);
		styleTogglePill(huddleToggle, huddleToggleText, isHuddle, primaryColor, onPrimary, surfaceVariant, textPrimary);
	}

	private View createTogglePill(String label, boolean active, int primaryColor, int onPrimary, int surfaceVariant, int textPrimary){
		LinearLayout pill=new LinearLayout(getActivity());
		pill.setOrientation(LinearLayout.HORIZONTAL);
		pill.setGravity(Gravity.CENTER);
		pill.setPadding(V.dp(12), V.dp(10), V.dp(12), V.dp(10));

		TextView text=new TextView(getActivity());
		text.setText(label);
		text.setTextSize(14);
		text.setTypeface(null, Typeface.BOLD);
		text.setGravity(Gravity.CENTER);
		pill.addView(text);

		styleTogglePill(pill, text, active, primaryColor, onPrimary, surfaceVariant, textPrimary);
		return pill;
	}

	private void styleTogglePill(View pill, TextView text, boolean active, int primaryColor, int onPrimary, int surfaceVariant, int textPrimary){
		GradientDrawable bg=new GradientDrawable();
		bg.setCornerRadius(V.dp(16));
		if(active){
			bg.setColor(primaryColor);
			text.setTextColor(onPrimary);
		}else{
			bg.setColor(0x00000000);
			text.setTextColor(textPrimary);
		}
		pill.setBackground(bg);
		pill.setElevation(active ? V.dp(2) : 0);
	}

	private TextView createSectionLabel(String text, int color){
		TextView label=new TextView(getActivity());
		label.setText(text);
		label.setTextSize(12);
		label.setTypeface(null, Typeface.BOLD);
		label.setTextColor(color);
		label.setLetterSpacing(0.04f);
		LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.topMargin=V.dp(12);
		label.setLayoutParams(lp);
		return label;
	}

	private EditText createEditText(String hint, int inputType){
		EditText edit=new EditText(getActivity());
		edit.setHint(hint);
		edit.setInputType(inputType);
		edit.setTextSize(15);
		edit.setPadding(V.dp(12), V.dp(10), V.dp(12), V.dp(10));
		GradientDrawable bg=new GradientDrawable();
		bg.setCornerRadius(V.dp(8));
		bg.setStroke(V.dp(1), UiUtils.getThemeColor(getActivity(), R.attr.colorM3Outline));
		bg.setColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Surface));
		edit.setBackground(bg);
		int focusPrimary=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary);
		edit.setOnFocusChangeListener((v, hasFocus)->{
			GradientDrawable fbg=new GradientDrawable();
			fbg.setCornerRadius(V.dp(8));
			fbg.setColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Surface));
			if(hasFocus){
				fbg.setStroke(V.dp(1), focusPrimary);
			}else{
				fbg.setStroke(V.dp(1), UiUtils.getThemeColor(getActivity(), R.attr.colorM3Outline));
			}
			v.setBackground(fbg);
		});
		return edit;
	}

	private TextView createPickerButton(String hint, int outlineColor, int textColor){
		TextView btn=new TextView(getActivity());
		btn.setText(hint);
		btn.setTextSize(14);
		btn.setTextColor(textColor);
		btn.setGravity(Gravity.CENTER);
		btn.setPadding(V.dp(12), V.dp(10), V.dp(12), V.dp(10));
		GradientDrawable bg=new GradientDrawable();
		bg.setCornerRadius(V.dp(8));
		bg.setStroke(V.dp(1), outlineColor);
		bg.setColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Surface));
		btn.setBackground(bg);
		return btn;
	}

	private LinearLayout.LayoutParams createInputLp(){
		LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.topMargin=V.dp(6);
		return lp;
	}
}
