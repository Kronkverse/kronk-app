package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.kuestions.GetQuestionAnswers;
import org.joinmastodon.android.api.requests.statuses.CreateStatus;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.displayitems.QuestionCardStatusDisplayItem;
import org.parceler.Parcels;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class QuestionDetailFragment extends MastodonToolbarFragment {
	private static final int MAX_ANSWER_LENGTH = 500;
	private static final DateTimeFormatter TIME_FMT =
			DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());

	private Status question;
	private String accountID;

	private ImageView authorAvatar;
	private TextView displayName;
	private TextView username;
	private TextView questionContent;
	private LinearLayout answerersRow;
	private TextView answerCountText;
	private ImageView[] smallAvatars;

	private TextView answersHeading;
	private LinearLayout composeSection;
	private EditText answerInput;
	private TextView charCounter;
	private Button btnPostAnswer;
	private ProgressBar answersProgress;
	private RecyclerView answersList;

	private List<Status> answers = new ArrayList<>();
	private AnswerAdapter adapter;
	private boolean posting = false;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		question = Parcels.unwrap(getArguments().getParcelable("question"));
		accountID = getArguments().getString("account");
		setTitle(R.string.tab_kuestions);
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_question_detail, container, false);

		authorAvatar = view.findViewById(R.id.author_avatar);
		displayName = view.findViewById(R.id.display_name);
		username = view.findViewById(R.id.username);
		questionContent = view.findViewById(R.id.question_content);
		answerersRow = view.findViewById(R.id.answerers_row);
		answerCountText = view.findViewById(R.id.answer_count_text);

		smallAvatars = new ImageView[]{
				view.findViewById(R.id.avatar_0),
				view.findViewById(R.id.avatar_1),
				view.findViewById(R.id.avatar_2),
		};
		for (ImageView av : smallAvatars) {
			av.setOutlineProvider(OutlineProviders.roundedRect(99));
			av.setClipToOutline(true);
		}

		answersHeading = view.findViewById(R.id.answers_heading);
		composeSection = view.findViewById(R.id.compose_section);
		answerInput = view.findViewById(R.id.answer_input);
		charCounter = view.findViewById(R.id.char_counter);
		btnPostAnswer = view.findViewById(R.id.btn_post_answer);
		answersProgress = view.findViewById(R.id.answers_progress);
		answersList = view.findViewById(R.id.answers_list);

		answersList.setLayoutManager(new LinearLayoutManager(getActivity()));
		answersList.setNestedScrollingEnabled(false);
		adapter = new AnswerAdapter();
		answersList.setAdapter(adapter);

		bindQuestion();

		// Clip author avatar to circle
		authorAvatar.setOutlineProvider(OutlineProviders.roundedRect(99));
		authorAvatar.setClipToOutline(true);

		if (question.hasAnswered) {
			composeSection.setVisibility(View.GONE);
			loadAnswers();
		} else {
			answersHeading.setText(R.string.answer_to_unlock);
			setupCompose();
		}

		return view;
	}

	private void bindQuestion() {
		Account account = question.account;
		if (account != null) {
			displayName.setText(TextUtils.isEmpty(account.displayName) ? account.username : account.displayName);
			username.setText("@" + account.acct);
			String avatarUrl = account.avatarStatic != null ? account.avatarStatic : account.avatar;
			if (avatarUrl != null) {
				ViewImageLoader.load(authorAvatar, null, new UrlImageLoaderRequest(avatarUrl, V.dp(40), V.dp(40)));
			}
		}

		String raw = question.content;
		if (!TextUtils.isEmpty(raw)) {
			questionContent.setText(Html.fromHtml(raw, Html.FROM_HTML_MODE_COMPACT).toString().trim());
		}

		// Answerer avatars + count
		int count = question.answersCount;
		List<Account> answerers = question.answerers;
		if (count > 0 || (answerers != null && !answerers.isEmpty())) {
			answerersRow.setVisibility(View.VISIBLE);
			int avatarCount = answerers != null ? Math.min(answerers.size(), 3) : 0;
			for (int i = 0; i < 3; i++) {
				if (i < avatarCount) {
					smallAvatars[i].setVisibility(View.VISIBLE);
					String url = answerers.get(i).avatarStatic != null ? answerers.get(i).avatarStatic : answerers.get(i).avatar;
					if (url != null) {
						ViewImageLoader.load(smallAvatars[i], null, new UrlImageLoaderRequest(url, V.dp(22), V.dp(22)));
					}
				} else {
					smallAvatars[i].setVisibility(View.GONE);
				}
			}
			if (count > 0) {
				answerCountText.setText(getResources().getQuantityString(R.plurals.answers_count, count, count));
			}
		}
	}

	private void setupCompose() {
		answerInput.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				int len = s.length();
				charCounter.setText(len + "/" + MAX_ANSWER_LENGTH);
				btnPostAnswer.setEnabled(len > 0 && len <= MAX_ANSWER_LENGTH && !posting);
			}
		});
		btnPostAnswer.setOnClickListener(v -> postAnswer());
	}

	private void postAnswer() {
		if (posting) return;
		String text = answerInput.getText().toString().trim();
		if (text.isEmpty()) return;

		posting = true;
		btnPostAnswer.setEnabled(false);

		CreateStatus.Request req = new CreateStatus.Request();
		req.status = text;
		req.postType = "answer";
		req.inReplyToId = question.id;
		req.visibility = StatusPrivacy.PUBLIC;

		new CreateStatus(req, UUID.randomUUID().toString())
				.setCallback(new Callback<Status>() {
					@Override
					public void onSuccess(Status result) {
						if (getActivity() == null) return;
						posting = false;
						question.hasAnswered = true;
						question.answersCount++;
						composeSection.setVisibility(View.GONE);
						loadAnswers();
					}
					@Override
					public void onError(ErrorResponse error) {
						if (getActivity() == null) return;
						posting = false;
						btnPostAnswer.setEnabled(true);
						Toast.makeText(getActivity(), R.string.error_posting_answer, Toast.LENGTH_SHORT).show();
					}
				})
				.exec(accountID);
	}

	private void loadAnswers() {
		answersHeading.setText(R.string.kuestions_answers_heading);
		answersProgress.setVisibility(View.VISIBLE);
		answersList.setVisibility(View.GONE);

		new GetQuestionAnswers(question.id)
				.setCallback(new Callback<List<Status>>() {
					@Override
					public void onSuccess(List<Status> result) {
						if (getActivity() == null) return;
						answersProgress.setVisibility(View.GONE);
						answers.clear();
						answers.addAll(result);
						adapter.notifyDataSetChanged();
						answersList.setVisibility(View.VISIBLE);
						int count = answers.size();
						answersHeading.setText(count > 0
								? getResources().getQuantityString(R.plurals.answers_count, count, count)
								: getString(R.string.kuestions_answers_heading));
					}
					@Override
					public void onError(ErrorResponse error) {
						if (getActivity() == null) return;
						answersProgress.setVisibility(View.GONE);
					}
				})
				.exec(accountID);
	}

	private class AnswerAdapter extends RecyclerView.Adapter<AnswerViewHolder> {
		@Override
		public AnswerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			return new AnswerViewHolder(
					LayoutInflater.from(getActivity()).inflate(R.layout.item_answer, parent, false));
		}
		@Override
		public void onBindViewHolder(AnswerViewHolder h, int pos) { h.bind(answers.get(pos)); }
		@Override
		public int getItemCount() { return answers.size(); }
	}

	private class AnswerViewHolder extends RecyclerView.ViewHolder {
		private final ImageView avatar;
		private final TextView displayNameView;
		private final TextView answerText;
		private final TextView timestamp;

		AnswerViewHolder(View v) {
			super(v);
			avatar = v.findViewById(R.id.avatar);
			displayNameView = v.findViewById(R.id.display_name);
			answerText = v.findViewById(R.id.answer_text);
			timestamp = v.findViewById(R.id.timestamp);
			avatar.setOutlineProvider(OutlineProviders.roundedRect(99));
			avatar.setClipToOutline(true);
		}

		void bind(Status answer) {
			Account acc = answer.account;
			if (acc != null) {
				displayNameView.setText(TextUtils.isEmpty(acc.displayName) ? acc.username : acc.displayName);
				String url = acc.avatarStatic != null ? acc.avatarStatic : acc.avatar;
				if (url != null) {
					ViewImageLoader.load(avatar, null, new UrlImageLoaderRequest(url, V.dp(38), V.dp(38)));
				}
			}
			String raw = answer.content;
			answerText.setText(!TextUtils.isEmpty(raw)
					? Html.fromHtml(raw, Html.FROM_HTML_MODE_COMPACT).toString().trim()
					: "");
			if (answer.createdAt != null) {
				timestamp.setText(TIME_FMT.format(answer.createdAt));
			}
		}
	}
}
