package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.events.DeleteEvent;
import org.joinmastodon.android.api.requests.events.GetEventAttendees;
import org.joinmastodon.android.api.requests.events.RsvpEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Event;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class EventDetailFragment extends MastodonToolbarFragment{
	private String accountID;
	private Event event;

	private TextView titleText, dateTimeText, locationText, descriptionText, hostText;
	private TextView goingCountText, interestedCountText;
	private View goingBtn, interestedBtn, cantGoBtn;
	private TextView goingBtnText, interestedBtnText, cantGoBtnText;
	private View cancelledBanner;
	private ImageView coverImage;
	private LinearLayout goingAvatarsRow, interestedAvatarsRow;
	private LinearLayout contentLayout;

	private static final DateTimeFormatter FULL_DATE=DateTimeFormatter.ofPattern(EEEE, MMMM d, yyyy).withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter TIME_FMT=DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());

	private static final int COLOR_GOING=0xFF6a9f8a;
	private static final int COLOR_INTERESTED=0xFFb8945f;
	private static final int COLOR_CANT_GO=0xFF888888;

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setNavigationBarColor(UiUtils.getThemeColor(activity, R.attr.colorM3Surface));
		accountID=getArguments().getString(account);
		event=Parcels.unwrap(getArguments().getParcelable(event));
		setTitle(event.title);
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		ScrollView scrollView=new ScrollView(getActivity());
		scrollView.setFillViewport(true);

		contentLayout=new LinearLayout(getActivity());
		contentLayout.setOrientation(LinearLayout.VERTICAL);

		int textPrimary=UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary);
		int textSecondary=UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary);
		int primaryColor=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary);

		// Cover image
		if(!TextUtils.isEmpty(event.imageUrl)){
			coverImage=new ImageView(getActivity());
			coverImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
			coverImage.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3SurfaceVariant));
			contentLayout.addView(coverImage, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(200)));
			ViewImageLoader.load(coverImage, null, new UrlImageLoaderRequest(event.imageUrl, V.dp(400), V.dp(200)));
		}

		// Inner padding wrapper
		LinearLayout content=new LinearLayout(getActivity());
		content.setOrientation(LinearLayout.VERTICAL);
		content.setPadding(V.dp(20), V.dp(16), V.dp(20), V.dp(32));
		contentLayout.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		// Huddle live banner
		if(isHuddleLive()){
			LinearLayout huddleBanner=new LinearLayout(getActivity());
			huddleBanner.setOrientation(LinearLayout.HORIZONTAL);
			huddleBanner.setGravity(Gravity.CENTER_VERTICAL);
			huddleBanner.setPadding(V.dp(16), V.dp(10), V.dp(16), V.dp(10));
			GradientDrawable huddleBg=new GradientDrawable();
			huddleBg.setCornerRadius(V.dp(8));
			huddleBg.setColor(0x1AE53935);
			huddleBanner.setBackground(huddleBg);

			// Pulsing red dot
			View dot=new View(getActivity());
			GradientDrawable dotBg=new GradientDrawable();
			dotBg.setShape(GradientDrawable.OVAL);
			dotBg.setColor(0xFFE53935);
			dot.setBackground(dotBg);
			LinearLayout.LayoutParams dotLp=new LinearLayout.LayoutParams(V.dp(10), V.dp(10));
			dotLp.rightMargin=V.dp(8);
			huddleBanner.addView(dot, dotLp);

			TextView liveText=new TextView(getActivity());
			liveText.setText(LIVE NOW);
			liveText.setTextSize(13);
			liveText.setTypeface(null, Typeface.BOLD);
			liveText.setLetterSpacing(0.08f);
			liveText.setTextColor(0xFFE53935);
			huddleBanner.addView(liveText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

			// Join Huddle button
			TextView joinBtn=new TextView(getActivity());
			joinBtn.setText(Join Huddle);
			joinBtn.setTextSize(13);
			joinBtn.setTypeface(null, Typeface.BOLD);
			joinBtn.setTextColor(0xFFFFFFFF);
			joinBtn.setPadding(V.dp(14), V.dp(6), V.dp(14), V.dp(6));
			joinBtn.setGravity(Gravity.CENTER);
			GradientDrawable joinBg=new GradientDrawable();
			joinBg.setCornerRadius(V.dp(8));
			joinBg.setColor(0xFFE53935);
			joinBtn.setBackground(joinBg);
			joinBtn.setOnClickListener(v->{
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(event.huddleUrl)));
			});
			huddleBanner.addView(joinBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

			content.addView(huddleBanner, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

			// Add spacing after huddle banner
			View huddleSpacer=new View(getActivity());
			content.addView(huddleSpacer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(12)));
		}

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

		// Event type badge row (type badge + recurrence badge)
		boolean hasTypeBadge=!TextUtils.isEmpty(event.eventType);
		boolean hasRecurrence=!TextUtils.isEmpty(event.recurrenceRule);
		if(hasTypeBadge || hasRecurrence){
			LinearLayout badgeRow=new LinearLayout(getActivity());
			badgeRow.setOrientation(LinearLayout.HORIZONTAL);
			badgeRow.setGravity(Gravity.CENTER_VERTICAL);
			LinearLayout.LayoutParams badgeRowLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			badgeRowLp.topMargin=V.dp(10);

			if(hasTypeBadge){
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
				tbLp.rightMargin=V.dp(8);
				badgeRow.addView(typeBadge, tbLp);
			}

			if(hasRecurrence){
				TextView recBadge=new TextView(getActivity());
				recBadge.setText(parseRecurrenceLabel(event.recurrenceRule));
				recBadge.setTextSize(11);
				recBadge.setTypeface(null, Typeface.BOLD);
				recBadge.setLetterSpacing(0.04f);
				recBadge.setTextColor(primaryColor);
				recBadge.setPadding(V.dp(8), V.dp(3), V.dp(8), V.dp(3));
				recBadge.setCompoundDrawablePadding(V.dp(4));
				GradientDrawable recBg=new GradientDrawable();
				recBg.setCornerRadius(V.dp(6));
				recBg.setColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3PrimaryContainer));
				recBadge.setBackground(recBg);
				badgeRow.addView(recBadge, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			}

			content.addView(badgeRow, badgeRowLp);
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
		LinearLayout locRow=createInfoRow(R.drawable.ic_group_24px, primaryColor);
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
		hostedBy.setText(Hosted by );
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

		goingBtn=createRsvpButton(Going, COLOR_GOING);
		goingBtnText=(TextView)((LinearLayout)goingBtn).getChildAt(0);
		LinearLayout.LayoutParams gLp=new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		gLp.rightMargin=V.dp(8);
		rsvpRow.addView(goingBtn, gLp);

		interestedBtn=createRsvpButton(Interested, COLOR_INTERESTED);
		interestedBtnText=(TextView)((LinearLayout)interestedBtn).getChildAt(0);
		LinearLayout.LayoutParams iLp=new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		iLp.rightMargin=V.dp(8);
		rsvpRow.addView(interestedBtn, iLp);

		cantGoBtn=createRsvpButton(Cant go", COLOR_CANT_GO);
		cantGoBtnText=(TextView)((LinearLayout)cantGoBtn).getChildAt(0);
		LinearLayout.LayoutParams cLp=new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
		rsvpRow.addView(cantGoBtn, cLp);

		content.addView(rsvpRow, rsvpLp);

		// Attendees section - Going
		content.addView(createDivider(), createDividerLp(16));

		TextView goingHeader=new TextView(getActivity());
		goingHeader.setText("Going");
		goingHeader.setTextSize(15);
		goingHeader.setTypeface(null, Typeface.BOLD);
		goingHeader.setTextColor(textPrimary);
		content.addView(goingHeader, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		HorizontalScrollView goingScroll=new HorizontalScrollView(getActivity());
		goingScroll.setHorizontalScrollBarEnabled(false);
		goingScroll.setClipToPadding(false);
		goingAvatarsRow=new LinearLayout(getActivity());
		goingAvatarsRow.setOrientation(LinearLayout.HORIZONTAL);
		goingAvatarsRow.setGravity(Gravity.CENTER_VERTICAL);
		goingScroll.addView(goingAvatarsRow, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		LinearLayout.LayoutParams goingScrollLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		goingScrollLp.topMargin=V.dp(8);
		content.addView(goingScroll, goingScrollLp);

		// Attendees section - Interested
		TextView interestedHeader=new TextView(getActivity());
		interestedHeader.setText("Interested");
		interestedHeader.setTextSize(15);
		interestedHeader.setTypeface(null, Typeface.BOLD);
		interestedHeader.setTextColor(textPrimary);
		LinearLayout.LayoutParams intHeaderLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		intHeaderLp.topMargin=V.dp(16);
		content.addView(interestedHeader, intHeaderLp);

		HorizontalScrollView interestedScroll=new HorizontalScrollView(getActivity());
		interestedScroll.setHorizontalScrollBarEnabled(false);
		interestedScroll.setClipToPadding(false);
		interestedAvatarsRow=new LinearLayout(getActivity());
		interestedAvatarsRow.setOrientation(LinearLayout.HORIZONTAL);
		interestedAvatarsRow.setGravity(Gravity.CENTER_VERTICAL);
		interestedScroll.addView(interestedAvatarsRow, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		LinearLayout.LayoutParams intScrollLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		intScrollLp.topMargin=V.dp(8);
		content.addView(interestedScroll, intScrollLp);

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

		// Invite button
		content.addView(createDivider(), createDividerLp(20));

		TextView inviteBtn=new TextView(getActivity());
		inviteBtn.setText("Invite");
		inviteBtn.setTextSize(15);
		inviteBtn.setTypeface(null, Typeface.BOLD);
		inviteBtn.setTextColor(0xFFFFFFFF);
		inviteBtn.setGravity(Gravity.CENTER);
		inviteBtn.setPadding(V.dp(16), V.dp(12), V.dp(16), V.dp(12));
		GradientDrawable inviteBg=new GradientDrawable();
		inviteBg.setCornerRadius(V.dp(10));
		inviteBg.setColor(primaryColor);
		inviteBtn.setBackground(inviteBg);
		inviteBtn.setOnClickListener(v->{
			// Placeholder for future invite screen
		});
		content.addView(inviteBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		// Owner actions: Edit and Delete
		if(event.isOwner){
			content.addView(createDivider(), createDividerLp(20));

			LinearLayout ownerRow=new LinearLayout(getActivity());
			ownerRow.setOrientation(LinearLayout.HORIZONTAL);
			ownerRow.setGravity(Gravity.CENTER);

			// Edit button
			TextView editBtn=new TextView(getActivity());
			editBtn.setText("Edit");
			editBtn.setTextSize(14);
			editBtn.setTypeface(null, Typeface.BOLD);
			editBtn.setTextColor(primaryColor);
			editBtn.setGravity(Gravity.CENTER);
			editBtn.setPadding(V.dp(16), V.dp(10), V.dp(16), V.dp(10));
			GradientDrawable editBg=new GradientDrawable();
			editBg.setCornerRadius(V.dp(10));
			editBg.setStroke(V.dp(1), primaryColor);
			editBtn.setBackground(editBg);
			editBtn.setOnClickListener(v->{
				Bundle args=new Bundle();
				args.putString("account", accountID);
				args.putParcelable("event", Parcels.wrap(event));
				args.putBoolean("editing", true);
				Nav.go(getActivity(), CreateEventFragment.class, args);
			});
			LinearLayout.LayoutParams editLp=new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
			editLp.rightMargin=V.dp(8);
			ownerRow.addView(editBtn, editLp);

			// Delete button
			TextView deleteBtn=new TextView(getActivity());
			deleteBtn.setText("Delete");
			deleteBtn.setTextSize(14);
			deleteBtn.setTypeface(null, Typeface.BOLD);
			deleteBtn.setTextColor(0xFFD32F2F);
			deleteBtn.setGravity(Gravity.CENTER);
			deleteBtn.setPadding(V.dp(16), V.dp(10), V.dp(16), V.dp(10));
			GradientDrawable deleteBg=new GradientDrawable();
			deleteBg.setCornerRadius(V.dp(10));
			deleteBg.setStroke(V.dp(1), 0xFFD32F2F);
			deleteBtn.setBackground(deleteBg);
			deleteBtn.setOnClickListener(v->confirmDelete());
			LinearLayout.LayoutParams deleteLp=new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
			ownerRow.addView(deleteBtn, deleteLp);

			content.addView(ownerRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		}

		scrollView.addView(contentLayout, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		bindData();
		loadAttendees();
		return scrollView;
	}

	private boolean isHuddleLive(){
		if(!"huddle".equals(event.eventType) || TextUtils.isEmpty(event.huddleUrl))
			return false;
		Instant now=Instant.now();
		if(event.startTime==null || event.startTime.isAfter(now))
			return false;
		return event.endTime==null || event.endTime.isAfter(now);
	}

	private String parseRecurrenceLabel(String rule){
		if(rule==null) return "Repeats";
		String lower=rule.toLowerCase();
		if(lower.contains("daily")) return "Repeats daily";
		if(lower.contains("weekly")) return "Repeats weekly";
		if(lower.contains("monthly")) return "Repeats monthly";
		if(lower.contains("yearly")) return "Repeats yearly";
		return "Repeats";
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
			// Make location clickable if URL exists
			if(!TextUtils.isEmpty(event.locationUrl)){
				locationText.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary));
				locationText.setOnClickListener(v->{
					UiUtils.openURL(getActivity(), accountID, event.locationUrl);
				});
			}
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
		styleRsvpButton(cantGoBtn, cantGoBtnText, isCantGo, COLOR_CANT_GO, "Cant go);

		goingBtn.setOnClickListener(v->doRsvp(isGoing ? remove : going));
		interestedBtn.setOnClickListener(v->doRsvp(isInterested ? remove : interested));
		cantGoBtn.setOnClickListener(v->doRsvp(isCantGo ? remove : not_going));
	}

	private void doRsvp(String status){
		new RsvpEvent(event.id, status)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Event result){
						if(getActivity()==null) return;
						event=result;
						goingCountText.setText(event.goingCount+ going);
						interestedCountText.setText(event.interestedCount+ interested);
						updateRsvpButtons();
					}
					@Override
					public void onError(ErrorResponse error){
						if(getActivity()!=null) error.showToast(getActivity());
					}
				})
				.exec(accountID);
	}

	private void confirmDelete(){
		new M3AlertDialogBuilder(getActivity())
				.setTitle(Delete Event)
				.setMessage(Are you sure you want to delete this event?)
				.setPositiveButton(Delete, (dlg, which)->{
					new DeleteEvent(event.id)
							.setCallback(new Callback<>(){
								@Override
								public void onSuccess(Event result){
									if(getActivity()==null) return;
									Nav.finish(EventDetailFragment.this);
								}
								@Override
								public void onError(ErrorResponse error){
									if(getActivity()!=null) error.showToast(getActivity());
								}
							})
							.exec(accountID);
				})
				.setNegativeButton(Cancel, null)
				.show();
	}

	private void loadAttendees(){
		new GetEventAttendees(event.id, going)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Account> result){
						if(getActivity()==null) return;
						populateAvatarRow(goingAvatarsRow, result);
					}
					@Override
					public void onError(ErrorResponse error){
						// silently ignore
					}
				})
				.exec(accountID);

		new GetEventAttendees(event.id, interested)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Account> result){
						if(getActivity()==null) return;
						populateAvatarRow(interestedAvatarsRow, result);
					}
					@Override
					public void onError(ErrorResponse error){
						// silently ignore
					}
				})
				.exec(accountID);
	}

	private void populateAvatarRow(LinearLayout row, List<Account> accounts){
		row.removeAllViews();
		if(accounts==null || accounts.isEmpty()){
			TextView empty=new TextView(getActivity());
			empty.setText(None yet);
			empty.setTextSize(13);
			empty.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
			row.addView(empty);
			return;
		}
		for(Account acc : accounts){
			ImageView avatar=new ImageView(getActivity());
			avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
			avatar.setOutlineProvider(new ViewOutlineProvider(){
				@Override
				public void getOutline(View view, Outline outline){
					outline.setOval(0, 0, view.getWidth(), view.getHeight());
				}
			});
			avatar.setClipToOutline(true);
			LinearLayout.LayoutParams avatarLp=new LinearLayout.LayoutParams(V.dp(32), V.dp(32));
			avatarLp.rightMargin=V.dp(6);
			row.addView(avatar, avatarLp);
			if(!TextUtils.isEmpty(acc.avatarStatic)){
				ViewImageLoader.load(avatar, null, new UrlImageLoaderRequest(acc.avatarStatic, V.dp(32), V.dp(32)));
			}
		}
	}

	private void styleRsvpButton(View btn, TextView text, boolean active, int color, String label){
		GradientDrawable bg=new GradientDrawable();
		bg.setCornerRadius(V.dp(10));
		if(active){
			bg.setColor(color);
			text.setTextColor(0xFFFFFFFF);
			text.setText(label+ ✓);
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
		text.setText(This event has been cancelled);
		text.setTextSize(14);
		text.setTypeface(null, Typeface.BOLD);
		text.setTextColor(0xFFD32F2F);
		banner.addView(text);

		return banner;
	}
}
