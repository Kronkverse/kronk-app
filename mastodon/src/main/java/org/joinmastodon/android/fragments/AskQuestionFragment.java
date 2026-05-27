package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.statuses.CreateStatus;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;

import java.util.UUID;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.AppKitFragment;

public class AskQuestionFragment extends AppKitFragment {
	private static final int MAX_LENGTH = 140;

	private String accountID;
	private EditText questionInput;
	private TextView charCounter;
	private Button btnAsk;
	private boolean posting = false;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		accountID = getArguments().getString("account");
		setTitle(R.string.ask_a_question);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_ask_question, container, false);

		questionInput = view.findViewById(R.id.question_input);
		charCounter = view.findViewById(R.id.char_counter);
		btnAsk = view.findViewById(R.id.btn_ask);

		questionInput.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				int len = s.length();
				charCounter.setText(len + "/" + MAX_LENGTH);
				btnAsk.setEnabled(len > 0 && len <= MAX_LENGTH && !posting);
			}
		});

		btnAsk.setOnClickListener(v -> postQuestion());

		questionInput.post(() -> {
			questionInput.requestFocus();
			InputMethodManager imm = getActivity().getSystemService(InputMethodManager.class);
			imm.showSoftInput(questionInput, InputMethodManager.SHOW_IMPLICIT);
		});

		return view;
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
						getActivity().onBackPressed();
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
}
