package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.SendNudge;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.NudgeResult;
import org.joinmastodon.android.model.viewmodel.NotificationViewModel;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.text.LinkSpan;
import org.joinmastodon.android.ui.text.NonColoredLinkSpan;
import org.joinmastodon.android.api.MastodonErrorResponse;
import org.joinmastodon.android.ui.utils.UiUtils;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class NotificationNudgeDisplayItem extends StatusDisplayItem {
	public final NotificationViewModel notification;
	private final String accountID;
	private final CharSequence text;
	private final ImageLoaderRequest avaRequest;

	public NotificationNudgeDisplayItem(String parentID, Callbacks callbacks, Context context,
			NotificationViewModel notification, String accountID) {
		super(parentID, callbacks, context);
		this.notification = notification;
		this.accountID = accountID;

		Account account = notification.accounts.get(0);
		avaRequest = new UrlImageLoaderRequest(
				GlobalUserPreferences.playGifs ? account.avatar : account.avatarStatic,
				V.dp(46), V.dp(46));

		SpannableStringBuilder name = new SpannableStringBuilder(
				account.displayName.isEmpty() ? account.username : account.displayName);
		if (GlobalUserPreferences.customEmojiInNames)
			HtmlParser.parseCustomEmoji(name, account.emojis);

		String template = context.getString(R.string.user_nudged_you, "{{name}}");
		String[] parts = template.split("\\Q{{name}}\\E", 2);
		SpannableStringBuilder formatted = new SpannableStringBuilder();
		if (parts.length > 1 && !parts[0].isEmpty()) formatted.append(parts[0]);
		int nameStart = formatted.length();
		formatted.append(name, new TypefaceSpan("sans-serif-medium"), 0);
		formatted.setSpan(new NonColoredLinkSpan(null, s -> {
			Bundle args = new Bundle();
			args.putString("account", accountID);
			args.putParcelable("profileAccount", org.parceler.Parcels.wrap(account));
			Nav.go((Activity) context, ProfileFragment.class, args);
		}, LinkSpan.Type.CUSTOM, null, null, null), nameStart, formatted.length(), 0);
		if (parts.length == 1) {
			formatted.append(' ').append(parts[0]);
		} else if (!parts[1].isEmpty()) {
			formatted.append(parts[1]);
		}
		this.text = formatted;
	}

	@Override
	public Type getType() {
		return Type.NOTIFICATION_NUDGE;
	}

	@Override
	public int getImageCount() {
		return 1;
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index) {
		return avaRequest;
	}

	public static class Holder extends StatusDisplayItem.Holder<NotificationNudgeDisplayItem>
			implements ImageLoaderViewHolder {
		private final ImageView avatar;
		private final ImageView icon;
		private final TextView text;
		private final Button nudgeBackButton;
		private boolean nudgedBack = false;

		public Holder(Activity activity, ViewGroup parent) {
			super(activity, R.layout.display_item_notification_nudge, parent);
			avatar = findViewById(R.id.avatar);
			icon = findViewById(R.id.icon);
			text = findViewById(R.id.text);
			nudgeBackButton = findViewById(R.id.nudge_back_btn);

			avatar.setOutlineProvider(OutlineProviders.roundedRect(6));
			avatar.setClipToOutline(true);
			avatar.setOnClickListener(v -> {
				if (item == null) return;
				Bundle args = new Bundle();
				args.putString("account", item.accountID);
				args.putParcelable("profileAccount",
						org.parceler.Parcels.wrap(item.notification.accounts.get(0)));
				Nav.go(activity, ProfileFragment.class, args);
			});

			nudgeBackButton.setOnClickListener(this::onNudgeBackClick);
		}

		@Override
		public void onBind(NotificationNudgeDisplayItem item) {
			text.setText(item.text);
			nudgedBack = false;
			nudgeBackButton.setText(R.string.nudge_back);
			nudgeBackButton.setEnabled(true);
			nudgeBackButton.setVisibility(View.VISIBLE);
			icon.setImageResource(R.drawable.ic_waving_hand_24px);
			icon.setImageTintList(ColorStateList.valueOf(
					UiUtils.getThemeColor(item.context, R.attr.colorM3Primary)));
		}

		@Override
		public void setImage(int index, android.graphics.drawable.Drawable image) {
			avatar.setImageDrawable(image);
		}

		@Override
		public void clearImage(int index) {
			avatar.setImageResource(R.drawable.image_placeholder);
		}

		private void onNudgeBackClick(View v) {
			if (nudgedBack || item == null) return;
			String targetId = item.notification.accounts.get(0).id;
			BaseStatusListFragment<?> fragment = (BaseStatusListFragment<?>) item.callbacks;

			nudgeBackButton.setEnabled(false);
			new SendNudge(targetId)
					.setCallback(new Callback<NudgeResult>() {
						@Override
						public void onSuccess(NudgeResult result) {
							nudgedBack = true;
							nudgeBackButton.setText(R.string.nudged);
						}

						@Override
						public void onError(ErrorResponse error) {
							if (error instanceof MastodonErrorResponse mr && mr.httpStatus == 422) {
								nudgedBack = true;
								nudgeBackButton.setText(R.string.nudge_waiting);
							} else {
								nudgeBackButton.setEnabled(true);
								error.showToast(item.context);
							}
						}
					})
					.exec(fragment.getAccountID());
		}
	}
}
