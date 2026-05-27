package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import org.joinmastodon.android.ui.OutlineProviders;
import org.parceler.Parcels;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.Locale;

import me.grishka.appkit.Nav;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class EventCardStatusDisplayItem extends StatusDisplayItem {
	private final Status status;
	private final String accountID;
	private final ImageLoaderRequest coverRequest;

	private static final DateTimeFormatter TIME_FMT =
			DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());

	public EventCardStatusDisplayItem(String parentID, Callbacks callbacks, Context context, Status status, String accountID) {
		super(parentID, callbacks, context);
		this.status = status;
		this.accountID = accountID;
		if (!TextUtils.isEmpty(status.event.imageUrl)) {
			coverRequest = new UrlImageLoaderRequest(status.event.imageUrl, 1000, 1000);
		} else {
			coverRequest = null;
		}
	}

	@Override
	public Type getType() {
		return Type.EVENT_CARD;
	}

	@Override
	public int getImageCount() {
		return coverRequest != null ? 1 : 0;
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index) {
		return coverRequest;
	}

	private static int withAlpha(int color, int alpha) {
		return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
	}

	public static class Holder extends StatusDisplayItem.Holder<EventCardStatusDisplayItem> {
		private final View cardRoot;
		private final ImageView coverImage;
		private final LinearLayout dateBadge;
		private final TextView dateDay;
		private final TextView dateMonth;
		private final TextView eventTitle;
		private final TextView eventMeta;

		public Holder(Activity activity, ViewGroup parent) {
			super(activity, R.layout.display_item_event_card, parent);
			cardRoot = findViewById(R.id.card_root);
			coverImage = findViewById(R.id.event_cover_image);
			dateBadge = findViewById(R.id.date_badge);
			dateDay = findViewById(R.id.date_day);
			dateMonth = findViewById(R.id.date_month);
			eventTitle = findViewById(R.id.event_title);
			eventMeta = findViewById(R.id.event_meta);
		}

		@Override
		public void onBind(EventCardStatusDisplayItem item) {
			Event event = item.status.event;
			int spaceColor = QuestionCardStatusDisplayItem.COLOR_EVENTS;

			// Card border: 2dp stroke at 80% opacity
			GradientDrawable border = new GradientDrawable();
			border.setCornerRadius(V.dp(12));
			border.setStroke(V.dp(2), withAlpha(spaceColor, 153)); // 60% opacity like webapp
			border.setColor(Color.TRANSPARENT);
			cardRoot.setBackground(border);
			cardRoot.setClipToOutline(true);
			cardRoot.setOutlineProvider(OutlineProviders.roundedRect(12));

			// Date badge background: space color at 8% opacity
			GradientDrawable badgeBg = new GradientDrawable();
			badgeBg.setCornerRadius(V.dp(10));
			badgeBg.setColor(withAlpha(spaceColor, 20));
			badgeBg.setStroke(1, withAlpha(spaceColor, 30));
			dateBadge.setBackground(badgeBg);

			// Date badge text
			if (event.startTime != null) {
				dateDay.setText(String.valueOf(event.startTime.atZone(ZoneId.systemDefault()).getDayOfMonth()));
				dateMonth.setText(event.startTime.atZone(ZoneId.systemDefault())
						.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault()).toUpperCase(Locale.getDefault()));
				dateMonth.setTextColor(withAlpha(spaceColor, 165)); // 65% opacity
			}

			// Title
			eventTitle.setText(event.title);

			// Meta: time · location · N going · N maybe
			StringBuilder meta = new StringBuilder();
			if (event.startTime != null) {
				meta.append(TIME_FMT.format(event.startTime));
			}
			if (!TextUtils.isEmpty(event.locationName)) {
				if (meta.length() > 0) meta.append(" · ");
				meta.append(event.locationName);
			}
			if (event.goingCount > 0) {
				if (meta.length() > 0) meta.append(" · ");
				meta.append(event.goingCount).append(" going");
			}
			if (event.interestedCount > 0) {
				if (meta.length() > 0) meta.append(" · ");
				meta.append(event.interestedCount).append(" maybe");
			}
			eventMeta.setText(meta);
			eventMeta.setVisibility(meta.length() > 0 ? View.VISIBLE : View.GONE);

			// Cover image
			if (item.coverRequest != null) {
				coverImage.setVisibility(View.VISIBLE);
				ViewImageLoader.load(coverImage, null, item.coverRequest);
			} else {
				coverImage.setVisibility(View.GONE);
				coverImage.setImageDrawable(null);
			}

			// Click opens event detail
			cardRoot.setOnClickListener(v -> {
				Activity activity = (Activity) v.getContext();
				Bundle args = new Bundle();
				args.putString("account", item.accountID);
				args.putParcelable("event", Parcels.wrap(event));
				Nav.go(activity, EventDetailFragment.class, args);
			});

			itemView.setPaddingRelative(V.dp(item.fullWidth ? 16 : 64), 0, V.dp(16), 0);
		}
	}
}
