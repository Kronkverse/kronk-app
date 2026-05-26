package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.utils.UiUtils;

import me.grishka.appkit.utils.V;

public class QuestionCardStatusDisplayItem extends StatusDisplayItem {
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

	public static class Holder extends StatusDisplayItem.Holder<QuestionCardStatusDisplayItem> {
		private final ImageView badgeIcon;
		private final TextView badgeLabel;
		private final TextView answersText;
		private final View inner;

		public Holder(Activity activity, ViewGroup parent) {
			super(activity, R.layout.display_item_question_card, parent);
			badgeIcon = findViewById(R.id.badge_icon);
			badgeLabel = findViewById(R.id.badge_label);
			answersText = findViewById(R.id.answers_text);
			inner = findViewById(R.id.inner);
		}

		@Override
		public void onBind(QuestionCardStatusDisplayItem item) {
			String pt = item.status.postType;
			boolean isQuestion = "question".equals(pt);
			boolean isProposal = "proposal".equals(pt);

			if (isQuestion) {
				badgeLabel.setText(R.string.question_label);
				badgeIcon.setImageResource(R.drawable.ic_help_24px);
			} else if (isProposal) {
				badgeLabel.setText(R.string.seed_label);
				badgeIcon.setImageResource(R.drawable.ic_gavel_24px);
			} else {
				badgeLabel.setText(R.string.answer_label);
				badgeIcon.setImageResource(R.drawable.ic_reply_24px);
			}
			badgeIcon.setImageTintList(ColorStateList.valueOf(
					UiUtils.getThemeColor(badgeIcon.getContext(), R.attr.colorM3Primary)));

			if (isQuestion && item.status.answersCount > 0) {
				answersText.setVisibility(View.VISIBLE);
				answersText.setText(answersText.getContext().getResources().getQuantityString(
						R.plurals.answers_count, item.status.answersCount, item.status.answersCount));
			} else {
				answersText.setVisibility(View.GONE);
			}

			inner.setOnClickListener(v -> item.callbacks.onItemClick(item.parentID));

			itemView.setPaddingRelative(V.dp(item.fullWidth ? 16 : 64), 0, itemView.getPaddingEnd(), itemView.getPaddingBottom());
		}
	}
}
