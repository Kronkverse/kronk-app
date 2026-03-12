package org.joinmastodon.android.fragments;

import android.app.Activity;
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
import android.widget.ScrollView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.events.RsvpEvent;
import org.joinmastodon.android.model.Event;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.V;

public class EventDetailFragment extends MastodonToolbarFragment{
	private String accountID;
	private Event event;

	private TextView titleText, dateTimeText, locationText, descriptionText, hostText;
	private TextView goingCountText, interestedCountText;
	private View goingBtn, interestedBtn, cantGoBtn;
	private TextView goingBtnText, interestedBtnText, cantGoBtnText;
	private View cancelledBanner;

	private static final DateTimeFormatter FULL_DATE=DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter TIME_FMT=DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());

	private static final int COLOR_GOING=0xFF6a9f8a;
	private static final int COLOR_INTERESTED=0xFFb8945f;
	private static final int COLOR_CANT_GO=0xFF888888;

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setNavigationBarColor(UiUtils.getThemeColor(activity, R.attr.colorM3Surface));
		accountID=getArguments().getString("account");
		event=Parcels.unwrap(getArguments().getParcelable("event"));
		setTitle(event.title);
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

		// Cancelled banner
		cancelledBanner=createCancelledBanner();
		content.addView(cancelledBanner, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		// Title
		titleText=new TextView(getActivity());
		titleText.setTextSize(24);
		titleText.setTypeface(null, Typeface.BOLD);
		titleText.setTextColor(textPrimary);
		titleText.setLineSpacing(V.dp(2), 1f);
		LinearLayout.LayoutParams titleLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		titleLp.topMargin=V.dp(4);
		content.addView(titleText, titleLp);

		// Event type badge
		if(!TextUtils.isEmpty(event.eventType)){
			TextView typeBadge=new TextView(getActivity());
			String typeLabel=event.eventType.substring(0, 1).toUpperCase()+event.eventType.substring(1);
			typeBadge.setText(typeLabel);
			typeBadge.setTextSize(11);
			typeBadge.setTypeface(null, Typeface.BOLD);
			typeBadge.setLetterSpacing(0.04f);
			typeBadge.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSecondaryContainer));
			typeBadge.setPadding(V.dp(8), V.dp(3), V.dp(8), V.dp(3));
			GradientDrawable tbBg=new GradientDrawable();
			tbBg.setCornerRadius(V.dp(6));
			tbBg.setColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3SecondaryContainer));
			typeBadge.setBackground(tbBg);
			LinearLayout.LayoutParams tbLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			tbLp.topMargin=V.dp(10);
			content.addView(typeBadge, tbLp);
		}

		// Divider
		content.addView(createDivider(), createDividerLp(16));

		// Date & Time section
		LinearLayout dateRow=createInfoRow(R.drawable.ic_calendar_month_24px, primaryColor);
		dateTimeText=new TextView(getActivity());
		dateTimeText.setTextSize(15);
		dateTimeText.setTextColor(textPrimary);
		dateTimeText.setLineSpacing(V.dp(2), 1f);
		((LinearLayout)dateRow.getChildAt(1)).addView(dateTimeText);
		content.addView(dateRow, createInfoRowLp());

		// Location section
		LinearLayout locRow=createInfoRow(R.drawable.ic_group_24px, primaryColor); // using group icon as placeholder
		locationText=new TextView(getActivity());
		locationText.setTextSize(15);
		locationText.setTextColor(textPrimary);
		((LinearLayout)locRow.getChildAt(1)).addView(locationText);
		LinearLayout.LayoutParams locRowLp=createInfoRowLp();
		locRowLp.topMargin=V.dp(12);
		content.addView(locRow, locRowLp);

		// Divider
		content.addView(createDivider(), createDividerLp(16));

		// Host info
		LinearLayout hostRow=new LinearLayout(getActivity());
		hostRow.setOrientation(LinearLayout.HORIZONTAL);
		hostRow.setGravity(Gravity.CENTER_VERTICAL);
		TextView hostedBy=new TextView(getActivity());
		hostedBy.setText("Hosted by ");
		hostedBy.setTextSize(14);
		hostedBy.setTextColor(textSecondary);
		hostRow.addView(hostedBy);
		hostText=new TextView(getActivity());
		hostText.setTextSize(14);
		hostText.setTypeface(null, Typeface.BOLD);
		hostText.setTextColor(textPrimary);
		hostRow.addView(hostText);
		LinearLayout.LayoutParams hostLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		hostLp.topMargin=V.dp(4);
		content.addView(hostRow, hostLp);

		// RSVP section
		content.addView(createDivider(), createDividerLp(16));

		// Attendee counts
		LinearLayout countsRow=new LinearLayout(getActivity());
		countsRow.setOrientation(LinearLayout.HORIZONTAL);
		countsRow.setGravity(Gravity.CENTER_VERTICAL);

		goingCountText=new TextView(getActivity());
		goingCountText.setTextSize(14);
		goingCountText.setTextColor(textSecondary);
		countsRow.addView(goingCountText);

		View countSpacer=new View(getActivity());
		countsRow.addView(countSpacer, new LinearLayout.LayoutParams(V.dp(16), 0));

		interestedCountText=new TextView(getActivity());
		interestedCountText.setTextSize(14);
		interestedCountText.setTextColor(textSecondary);
		countsRow.addView(interestedCountText);

		content.addView(countsRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		// RSVP buttons
		LinearLayout rsvpRow=new LinearLayout(getActivity());
		rsvpRow.setOrientation(LinearLayout.HORIZONTAL);
		rsvpRow.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout.LayoutParams rsvpLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		rsvpLp.topMargin=V.dp(12);

		goingBtn=createRsvpButton("Going", COLOR_GOING);
		goingBtnText=(TextView)((LinearLayout)goingBtn).getChildAt(0);
		LinearLayout.LayoutParams gLp=new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		gLp.rightMargin=V.dp(8);
		rsvpRow.addView(goingBtn, gLp);

		interestedBtn=createRsvpButton("Interested", COLOR_INTERESTED);
		interestedBtnText=(TextView)((LinearLayout)interestedBtn).getChildAt(0);
		LinearLayout.LayoutParams iLp=new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		iLp.rightMargin=V.dp(8);
		rsvpRow.addView(interestedBtn, iLp);

		cantGoBtn=createRsvpButton("Can't go", COLOR_CANT_GO);
		cantGoBtnText=(TextView)((LinearLayout)cantGoBtn).getChildAt(0);
		LinearLayout.LayoutParams cLp=new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		rsvpRow.addView(cantGoBtn, cLp);

		content.addView(rsvpRow, rsvpLp);

		// Description section
		if(!TextUtils.isEmpty(event.description)){
			content.addView(createDivider(), createDividerLp(20));

			TextView descLabel=new TextView(getActivity());
			descLabel.setText("About");
			descLabel.setTextSize(16);
			descLabel.setTypeface(null, Typeface.BOLD);
			descLabel.setTextColor(textPrimary);
			content.addView(descLabel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

			descriptionText=new TextView(getActivity());
			descriptionText.setTextSize(15);
			descriptionText.setTextColor(textPrimary);
			descriptionText.setLineSpacing(V.dp(3), 1f);
			LinearLayout.LayoutParams descLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			descLp.topMargin=V.dp(8);
			content.addView(descriptionText, descLp);
		}

		scrollView.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		bindData();
		return scrollView;
	}

	private void bindData(){
		titleText.setText(event.title);
		titleText.setAlpha(event.cancelled ? 0.5f : 1f);

		// Date/time
		if(event.startTime!=null){
			StringBuilder dt=new StringBuilder();
			dt.append(FULL_DATE.format(event.startTime));
			dt.append("\n");
			dt.append(TIME_FMT.format(event.startTime));
			if(event.endTime!=null){
				dt.append(" – ").append(TIME_FMT.format(event.endTime));
			}
			dateTimeText.setText(dt.toString());
		}

		// Location
		if(!TextUtils.isEmpty(event.locationName)){
			locationText.setText(event.locationName);
			locationText.setVisibility(View.VISIBLE);
			((View)locationText.getParent().getParent()).setVisibility(View.VISIBLE);
		}else{
			((View)locationText.getParent().getParent()).setVisibility(View.GONE);
		}

		// Host
		if(event.account!=null){
			String displayName=event.account.displayName;
			if(TextUtils.isEmpty(displayName)) displayName=event.account.acct;
			hostText.setText(displayName);
		}

		// Counts
		goingCountText.setText(event.goingCount+" going");
		interestedCountText.setText(event.interestedCount+" interested");

		// Cancelled
		cancelledBanner.setVisibility(event.cancelled ? View.VISIBLE : View.GONE);
		goingBtn.setVisibility(event.cancelled ? View.GONE : View.VISIBLE);
		interestedBtn.setVisibility(event.cancelled ? View.GONE : View.VISIBLE);
		cantGoBtn.setVisibility(event.cancelled ? View.GONE : View.VISIBLE);

		// Description
		if(descriptionText!=null){
			descriptionText.setText(event.description);
		}

		updateRsvpButtons();
	}

	private void updateRsvpButtons(){
		boolean isGoing="going".equals(event.rsvp);
		boolean isInterested="interested".equals(event.rsvp);
		boolean isCantGo="not_going".equals(event.rsvp);

		styleRsvpButton(goingBtn, goingBtnText, isGoing, COLOR_GOING, "Going");
		styleRsvpButton(interestedBtn, interestedBtnText, isInterested, COLOR_INTERESTED, "Interested");
		styleRsvpButton(cantGoBtn, cantGoBtnText, isCantGo, COLOR_CANT_GO, "Can't go");

		goingBtn.setOnClickListener(v->doRsvp(isGoing ? "remove" : "going"));
		interestedBtn.setOnClickListener(v->doRsvp(isInterested ? "remove" : "interested"));
		cantGoBtn.setOnClickListener(v->doRsvp(isCantGo ? "remove" : "not_going"));
	}

	private void doRsvp(String status){
		new RsvpEvent(event.id, status)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Event result){
						if(getActivity()==null) return;
						event=result;
						goingCountText.setText(event.goingCount+" going");
						interestedCountText.setText(event.interestedCount+" interested");
						updateRsvpButtons();
					}
					@Override
					public void onError(ErrorResponse error){
						if(getActivity()!=null) error.showToast(getActivity());
					}
				})
				.exec(accountID);
	}

	private void styleRsvpButton(View btn, TextView text, boolean active, int color, String label){
		GradientDrawable bg=new GradientDrawable();
		bg.setCornerRadius(V.dp(10));
		if(active){
			bg.setColor(color);
			text.setTextColor(0xFFFFFFFF);
			text.setText(label+" ✓");
		}else{
			bg.setColor(0x00000000);
			bg.setStroke(V.dp(1), UiUtils.getThemeColor(getActivity(), R.attr.colorM3Outline));
			text.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
			text.setText(label);
		}
		btn.setBackground(bg);
	}

	private View createRsvpButton(String label, int color){
		LinearLayout btn=new LinearLayout(getActivity());
		btn.setOrientation(LinearLayout.HORIZONTAL);
		btn.setGravity(Gravity.CENTER);
		btn.setPadding(V.dp(12), V.dp(10), V.dp(12), V.dp(10));

		GradientDrawable bg=new GradientDrawable();
		bg.setCornerRadius(V.dp(10));
		bg.setStroke(V.dp(1), UiUtils.getThemeColor(getActivity(), R.attr.colorM3Outline));
		btn.setBackground(bg);

		TextView text=new TextView(getActivity());
		text.setText(label);
		text.setTextSize(13);
		text.setTypeface(null, Typeface.BOLD);
		text.setGravity(Gravity.CENTER);
		text.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
		btn.addView(text);

		return btn;
	}

	private LinearLayout createInfoRow(int iconRes, int iconColor){
		LinearLayout row=new LinearLayout(getActivity());
		row.setOrientation(LinearLayout.HORIZONTAL);

		ImageView icon=new ImageView(getActivity());
		icon.setImageResource(iconRes);
		icon.setColorFilter(iconColor);
		LinearLayout.LayoutParams iconLp=new LinearLayout.LayoutParams(V.dp(22), V.dp(22));
		iconLp.rightMargin=V.dp(12);
		iconLp.topMargin=V.dp(2);
		row.addView(icon, iconLp);

		LinearLayout textCol=new LinearLayout(getActivity());
		textCol.setOrientation(LinearLayout.VERTICAL);
		row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

		return row;
	}

	private LinearLayout.LayoutParams createInfoRowLp(){
		LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.topMargin=V.dp(4);
		return lp;
	}

	private View createDivider(){
		View divider=new View(getActivity());
		divider.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant));
		return divider;
	}

	private LinearLayout.LayoutParams createDividerLp(int verticalMarginDp){
		LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(1));
		lp.topMargin=V.dp(verticalMarginDp);
		lp.bottomMargin=V.dp(verticalMarginDp);
		return lp;
	}

	private View createCancelledBanner(){
		LinearLayout banner=new LinearLayout(getActivity());
		banner.setOrientation(LinearLayout.HORIZONTAL);
		banner.setGravity(Gravity.CENTER);
		banner.setPadding(V.dp(16), V.dp(10), V.dp(16), V.dp(10));
		GradientDrawable bg=new GradientDrawable();
		bg.setCornerRadius(V.dp(8));
		bg.setColor(0x1AD32F2F); // 10% red
		banner.setBackground(bg);
		banner.setVisibility(View.GONE);

		TextView text=new TextView(getActivity());
		text.setText("This event has been cancelled");
		text.setTextSize(14);
		text.setTypeface(null, Typeface.BOLD);
		text.setTextColor(0xFFD32F2F);
		banner.addView(text);

		return banner;
	}
}
