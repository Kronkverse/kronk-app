package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.OutlineProviders;

import java.util.List;

import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class QuestionCardStatusDisplayItem extends StatusDisplayItem {

	// Space colors (from planets.tsx: Saturn=#4844C0, Neptune=#343070, Jupiter=#36248C)
	static final int COLOR_QUESTIONS = Color.parseColor("#4844C0");
	static final int COLOR_EVENTS    = Color.parseColor("#343070");
	static final int COLOR_SEEDS     = Color.parseColor("#36248C");

	private final Status status;
	private final String accountID;

	public QuestionCardStatusDisplayItem(String parentID, Callbacks callbacks, Context context, Status status, String accountID) {
		super(parentID, callbacks, context);
		this.status = status;
		this.accountID = accountID;
	}

	@Override
	public Type getType() {
		return Type.QUESTION_CARD;
	}

	static int spaceColorForPostType(String postType) {
		if ("proposal".equals(postType)) return COLOR_SEEDS;
		return COLOR_QUESTIONS; // question + answer
	}

	private static int withAlpha(int color, int alpha) {
		return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
	}

	public static class Holder extends StatusDisplayItem.Holder<QuestionCardStatusDisplayItem> {
		private final View cardRoot;
		private final View badgeHeader;
		private final ImageView badgeIcon;
		private final TextView badgeLabel;
		private final TextView cardBody;
		private final View cardFooter;
		private final LinearLayout avatarsRow;
		private final ImageView[] avatarViews = new ImageView[4];
		private final TextView countText;
		private final TextView seeAnswersText;

		public Holder(Activity activity, ViewGroup parent) {
			super(activity, R.layout.display_item_question_card, parent);
			cardRoot = findViewById(R.id.card_root);
			badgeHeader = findViewById(R.id.badge_header);
			badgeIcon = findViewById(R.id.badge_icon);
			badgeLabel = findViewById(R.id.badge_label);
			cardBody = findViewById(R.id.card_body);
			cardFooter = findViewById(R.id.card_footer);
			avatarsRow = findViewById(R.id.avatars_row);
			countText = findViewById(R.id.count_text);
			seeAnswersText = findViewById(R.id.see_answers_text);

			avatarViews[0] = findViewById(R.id.avatar_0);
			avatarViews[1] = findViewById(R.id.avatar_1);
			avatarViews[2] = findViewById(R.id.avatar_2);
			avatarViews[3] = findViewById(R.id.avatar_3);

			for (ImageView av : avatarViews) {
				av.setOutlineProvider(OutlineProviders.roundedRect(99));
				av.setClipToOutline(true);
			}
		}

		@Override
		public void onBind(QuestionCardStatusDisplayItem item) {
			String pt = item.status.postType;
			boolean isQuestion = "question".equals(pt);
			boolean isProposal = "proposal".equals(pt);
			int spaceColor = spaceColorForPostType(pt);

			// Card border: 2dp stroke, space color at 80% opacity
			GradientDrawable border = new GradientDrawable();
			border.setCornerRadius(V.dp(10));
			border.setStroke(V.dp(2), withAlpha(spaceColor, 204));
			border.setColor(Color.TRANSPARENT);
			cardRoot.setBackground(border);
			cardRoot.setClipToOutline(true);
			cardRoot.setOutlineProvider(OutlineProviders.roundedRect(10));

			// Badge header: solid space color background
			GradientDrawable badgeBg = new GradientDrawable();
			badgeBg.setCornerRadii(new float[]{V.dp(8), V.dp(8), V.dp(8), V.dp(8), 0, 0, 0, 0});
			badgeBg.setColor(spaceColor);
			badgeHeader.setBackground(badgeBg);

			// Footer: tinted background + top border
			GradientDrawable footerBg = new GradientDrawable();
			footerBg.setCornerRadii(new float[]{0, 0, 0, 0, V.dp(8), V.dp(8), V.dp(8), V.dp(8)});
			footerBg.setColor(withAlpha(spaceColor, 25));
			footerBg.setStroke(1, withAlpha(spaceColor, 80));
			cardFooter.setBackground(footerBg);

			// Badge content
			if (isProposal) {
				badgeLabel.setText(R.string.seed_label);
				badgeIcon.setImageResource(R.drawable.ic_gavel_24px);
			} else if (isQuestion) {
				badgeLabel.setText(R.string.question_label);
				badgeIcon.setImageResource(R.drawable.ic_help_24px);
			} else {
				badgeLabel.setText(R.string.answer_label);
				badgeIcon.setImageResource(R.drawable.ic_reply_24px);
			}
			badgeIcon.setImageTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));

			// Body text (the question / answer / seed content)
			String rawContent = item.status.content;
			if (!TextUtils.isEmpty(rawContent)) {
				CharSequence parsed = android.text.Html.fromHtml(rawContent, android.text.Html.FROM_HTML_MODE_COMPACT);
				cardBody.setText(parsed.toString().trim());
				cardBody.setVisibility(View.VISIBLE);
			} else {
				cardBody.setVisibility(View.GONE);
			}

			// Answerer avatars
			List<Account> answerers = item.status.answerers;
			int avatarCount = (answerers != null && !isProposal) ? Math.min(answerers.size(), 4) : 0;
			if (avatarCount > 0) {
				avatarsRow.setVisibility(View.VISIBLE);
				for (int i = 0; i < 4; i++) {
					if (i < avatarCount) {
						avatarViews[i].setVisibility(View.VISIBLE);
						String url = answerers.get(i).avatarStatic;
						if (url == null) url = answerers.get(i).avatar;
						if (url != null) {
							ViewImageLoader.load(avatarViews[i], null, new UrlImageLoaderRequest(url, V.dp(22), V.dp(22)));
						}
					} else {
						avatarViews[i].setVisibility(View.GONE);
					}
				}
			} else {
				avatarsRow.setVisibility(View.GONE);
			}

			// Count / status text
			seeAnswersText.setTextColor(spaceColor);
			if (isProposal) {
				countText.setVisibility(View.GONE);
				seeAnswersText.setVisibility(View.GONE);
			} else if (isQuestion) {
				int count = item.status.answersCount;
				if (item.status.hasAnswered) {
					countText.setText(R.string.you_answered);
					countText.setVisibility(View.VISIBLE);
					seeAnswersText.setText(R.string.see_all_answers);
					seeAnswersText.setVisibility(View.VISIBLE);
				} else if (count > 0) {
					countText.setText(countText.getContext().getResources().getQuantityString(
							R.plurals.answers_count, count, count));
					countText.setVisibility(View.VISIBLE);
					seeAnswersText.setVisibility(View.GONE);
				} else {
					countText.setText(R.string.answer_to_unlock);
					countText.setVisibility(View.VISIBLE);
					seeAnswersText.setVisibility(View.GONE);
				}
			} else {
				// answer post type
				countText.setVisibility(View.GONE);
				seeAnswersText.setVisibility(View.GONE);
			}

			cardRoot.setOnClickListener(v -> item.callbacks.onItemClick(item.parentID));
			itemView.setPaddingRelative(V.dp(item.fullWidth ? 16 : 64), 0, V.dp(16), 0);
		}
	}
}
