package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.kuestions.GetQuestions;
import org.joinmastodon.android.api.requests.statuses.CreateStatus;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.ui.OutlineProviders;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class KuestionsFragment extends AppKitFragment {
	private static final int MAX_QUESTION_LENGTH = 140;
	private static final int COLOR_ACTIVE = Color.parseColor("#4844C0");
	private static final int COLOR_INACTIVE = Color.parseColor("#888888");

	private String accountID;

	private TextView tabAsk, tabBrowse;
	private View askSection, browseSection;
	private EditText questionInput;
	private TextView charCounter;
	private Button btnAsk;
	private RecyclerView questionsList;
	private ProgressBar questionsProgress;
	private TextView questionsEmpty;

	private final List<Status> questions = new ArrayList<>();
	private QuestionAdapter adapter;
	private boolean onAskTab = true;
	private boolean loaded = false;
	private boolean posting = false;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		accountID = getArguments().getString("account");
		setTitle(R.string.tab_kuestions);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_kuestions, container, false);

		tabAsk = view.findViewById(R.id.tab_ask);
		tabBrowse = view.findViewById(R.id.tab_browse);
		askSection = view.findViewById(R.id.ask_section);
		browseSection = view.findViewById(R.id.browse_section);
		questionInput = view.findViewById(R.id.question_input);
		charCounter = view.findViewById(R.id.char_counter);
		btnAsk = view.findViewById(R.id.btn_ask);
		questionsList = view.findViewById(R.id.questions_list);
		questionsProgress = view.findViewById(R.id.questions_progress);
		questionsEmpty = view.findViewById(R.id.questions_empty);

		tabAsk.setOnClickListener(v -> showTab(true));
		tabBrowse.setOnClickListener(v -> showTab(false));

		questionInput.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				int len = s.length();
				charCounter.setText(len + "/" + MAX_QUESTION_LENGTH);
				btnAsk.setEnabled(len > 0 && len <= MAX_QUESTION_LENGTH && !posting);
			}
		});
		btnAsk.setOnClickListener(v -> postQuestion());

		questionsList.setLayoutManager(new LinearLayoutManager(getActivity()));
		questionsList.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
		adapter = new QuestionAdapter();
		questionsList.setAdapter(adapter);

		updateTabVisuals();
		return view;
	}

	private void showTab(boolean askTab) {
		onAskTab = askTab;
		updateTabVisuals();
		if (!askTab && !loaded) {
			loadQuestions();
		}
	}

	private void updateTabVisuals() {
		askSection.setVisibility(onAskTab ? View.VISIBLE : View.GONE);
		browseSection.setVisibility(onAskTab ? View.GONE : View.VISIBLE);
		tabAsk.setTextColor(onAskTab ? COLOR_ACTIVE : COLOR_INACTIVE);
		tabBrowse.setTextColor(onAskTab ? COLOR_INACTIVE : COLOR_ACTIVE);
		tabAsk.setTypeface(null, onAskTab ? Typeface.BOLD : Typeface.NORMAL);
		tabBrowse.setTypeface(null, onAskTab ? Typeface.NORMAL : Typeface.BOLD);
	}

	private void postQuestion() {
		if (posting) return;
		String text = questionInput.getText().toString().trim();
		if (text.isEmpty()) return;

		posting = true;
		btnAsk.setEnabled(false);

		CreateStatus.Request req = new CreateStatus.Request();
		req.status = text;
		req.postType = "question";
		req.visibility = StatusPrivacy.PUBLIC;

		new CreateStatus(req, UUID.randomUUID().toString())
				.setCallback(new Callback<Status>() {
					@Override
					public void onSuccess(Status result) {
						if (getActivity() == null) return;
						posting = false;
						questionInput.setText("");
						questions.add(0, result);
						if (loaded) {
							adapter.notifyItemInserted(0);
							questionsList.scrollToPosition(0);
						}
						loaded = true;
						questionsEmpty.setVisibility(View.GONE);
						questionsList.setVisibility(View.VISIBLE);
						showTab(false);
					}

					@Override
					public void onError(ErrorResponse error) {
						if (getActivity() == null) return;
						posting = false;
						btnAsk.setEnabled(true);
						Toast.makeText(getActivity(), R.string.error_posting_question, Toast.LENGTH_SHORT).show();
					}
				})
				.exec(accountID);
	}

	private void loadQuestions() {
		questionsProgress.setVisibility(View.VISIBLE);
		questionsList.setVisibility(View.GONE);
		questionsEmpty.setVisibility(View.GONE);

		new GetQuestions(null, 40)
				.setCallback(new Callback<List<Status>>() {
					@Override
					public void onSuccess(List<Status> result) {
						if (getActivity() == null) return;
						loaded = true;
						questionsProgress.setVisibility(View.GONE);
						questions.clear();
						questions.addAll(result);
						adapter.notifyDataSetChanged();
						boolean empty = result.isEmpty();
						questionsList.setVisibility(empty ? View.GONE : View.VISIBLE);
						questionsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
					}

					@Override
					public void onError(ErrorResponse error) {
						if (getActivity() == null) return;
						questionsProgress.setVisibility(View.GONE);
						questionsEmpty.setVisibility(View.VISIBLE);
					}
				})
				.exec(accountID);
	}

	private class QuestionAdapter extends RecyclerView.Adapter<QuestionViewHolder> {
		@Override
		public QuestionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			return new QuestionViewHolder(
					LayoutInflater.from(getActivity()).inflate(R.layout.item_question, parent, false));
		}

		@Override
		public void onBindViewHolder(QuestionViewHolder h, int pos) {
			h.bind(questions.get(pos));
		}

		@Override
		public int getItemCount() { return questions.size(); }
	}

	private class QuestionViewHolder extends RecyclerView.ViewHolder {
		private final ImageView avatar;
		private final TextView displayName;
		private final TextView questionText;
		private final TextView answerCount;

		QuestionViewHolder(View v) {
			super(v);
			avatar = v.findViewById(R.id.avatar);
			displayName = v.findViewById(R.id.display_name);
			questionText = v.findViewById(R.id.question_text);
			answerCount = v.findViewById(R.id.answer_count);
			avatar.setOutlineProvider(OutlineProviders.roundedRect(99));
			avatar.setClipToOutline(true);
			v.setOnClickListener(view -> {
				int pos = getBindingAdapterPosition();
				if (pos == RecyclerView.NO_ID) return;
				Status q = questions.get(pos);
				Bundle args = new Bundle();
				args.putString("account", accountID);
				args.putParcelable("question", Parcels.wrap(q));
				Nav.go(getActivity(), QuestionDetailFragment.class, args);
			});
		}

		void bind(Status q) {
			Account acc = q.account;
			if (acc != null) {
				displayName.setText(TextUtils.isEmpty(acc.displayName) ? acc.username : acc.displayName);
				String url = acc.avatarStatic != null ? acc.avatarStatic : acc.avatar;
				if (url != null) {
					ViewImageLoader.load(avatar, null, new UrlImageLoaderRequest(url, V.dp(40), V.dp(40)));
				}
			}
			String raw = q.content;
			questionText.setText(!TextUtils.isEmpty(raw)
					? Html.fromHtml(raw, Html.FROM_HTML_MODE_COMPACT).toString().trim()
					: "");
			int count = q.answersCount;
			if (count > 0) {
				answerCount.setVisibility(View.VISIBLE);
				answerCount.setText(getResources().getQuantityString(R.plurals.answers_count, count, count));
			} else {
				answerCount.setVisibility(View.GONE);
			}
		}
	}
}
