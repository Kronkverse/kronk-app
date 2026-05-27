package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.proposals.CreateProposal;
import org.joinmastodon.android.api.requests.proposals.CreateProposalTask;
import org.joinmastodon.android.model.Proposal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class CreateProposalFragment extends MastodonToolbarFragment {
	private static final int TITLE_MAX = 240;

	private String accountID;
	private EditText titleInput;
	private TextView titleCounter;
	private EditText bodyInput;
	private LinearLayout taskRowsContainer;
	private Button btnAddTask;
	private Button btnSubmit;
	private TextView errorText;
	private boolean submitting = false;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		accountID = getArguments().getString("account");
		setTitle(R.string.kommons_plant_seed);
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_create_proposal, container, false);

		titleInput = view.findViewById(R.id.title_input);
		titleCounter = view.findViewById(R.id.title_counter);
		bodyInput = view.findViewById(R.id.body_input);
		taskRowsContainer = view.findViewById(R.id.task_rows_container);
		btnAddTask = view.findViewById(R.id.btn_add_task);
		btnSubmit = view.findViewById(R.id.btn_submit);
		errorText = view.findViewById(R.id.error_text);

		titleInput.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				titleCounter.setText(s.length() + "/" + TITLE_MAX);
				updateSubmitEnabled();
			}
		});
		bodyInput.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override public void afterTextChanged(Editable s) { updateSubmitEnabled(); }
		});

		btnAddTask.setOnClickListener(v -> addTaskRow());
		btnSubmit.setOnClickListener(v -> submit());
		btnSubmit.setEnabled(false);

		return view;
	}

	private void updateSubmitEnabled() {
		int titleLen = titleInput.getText().length();
		int bodyLen = bodyInput.getText().length();
		btnSubmit.setEnabled(!submitting && titleLen > 0 && titleLen <= TITLE_MAX && bodyLen > 0);
	}

	private void addTaskRow() {
		View row = LayoutInflater.from(getActivity()).inflate(R.layout.item_task_row, taskRowsContainer, false);
		ImageButton btnRemove = row.findViewById(R.id.btn_remove_task);
		btnRemove.setOnClickListener(v -> taskRowsContainer.removeView(row));
		taskRowsContainer.addView(row);
	}

	private List<String[]> collectTasks() {
		List<String[]> tasks = new ArrayList<>();
		for (int i = 0; i < taskRowsContainer.getChildCount(); i++) {
			View row = taskRowsContainer.getChildAt(i);
			String title = ((EditText) row.findViewById(R.id.task_title)).getText().toString().trim();
			String desc = ((EditText) row.findViewById(R.id.task_description)).getText().toString().trim();
			if (!title.isEmpty()) tasks.add(new String[]{title, desc});
		}
		return tasks;
	}

	private void submit() {
		if (submitting) return;
		String title = titleInput.getText().toString().trim();
		String body = bodyInput.getText().toString().trim();
		if (title.isEmpty() || body.isEmpty()) return;

		submitting = true;
		btnSubmit.setEnabled(false);
		btnSubmit.setText(R.string.kommons_form_planting);
		errorText.setVisibility(View.GONE);

		new CreateProposal(title, body)
				.setCallback(new Callback<Proposal>() {
					@Override
					public void onSuccess(Proposal proposal) {
						if (getActivity() == null) return;
						List<String[]> tasks = collectTasks();
						if (tasks.isEmpty()) {
							KommonsFragment.sNeedsReload = true;
							getActivity().onBackPressed();
							return;
						}
						btnSubmit.setText(R.string.kommons_form_adding_tasks);
						AtomicInteger remaining = new AtomicInteger(tasks.size());
						for (String[] task : tasks) {
							new CreateProposalTask(proposal.id, task[0], task.length > 1 ? task[1] : null)
									.setCallback(new Callback<Proposal>() {
										@Override
										public void onSuccess(Proposal r) {
											if (remaining.decrementAndGet() == 0 && getActivity() != null) {
												KommonsFragment.sNeedsReload = true;
												getActivity().onBackPressed();
											}
										}
										@Override
										public void onError(ErrorResponse error) {
											// task creation failure is non-fatal — proposal was created
											if (remaining.decrementAndGet() == 0 && getActivity() != null) {
												KommonsFragment.sNeedsReload = true;
												getActivity().onBackPressed();
											}
										}
									})
									.exec(accountID);
						}
					}
					@Override
					public void onError(ErrorResponse error) {
						if (getActivity() == null) return;
						submitting = false;
						btnSubmit.setText(R.string.kommons_form_submit);
						updateSubmitEnabled();
						errorText.setText(R.string.kommons_form_error);
						errorText.setVisibility(View.VISIBLE);
					}
				})
				.exec(accountID);
	}
}
