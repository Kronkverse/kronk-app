package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.kuestions.GetQuestions;
import org.joinmastodon.android.model.Status;
import org.parceler.Parcels;

import java.util.List;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.SimpleCallback;

public class KuestionsFragment extends StatusListFragment {
	private String maxID;

	public KuestionsFragment() {
		setListLayoutId(R.layout.recycler_fragment_with_fab);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		accountID = getArguments().getString("account");
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ImageButton fab = view.findViewById(R.id.fab);
		fab.setImageResource(R.drawable.ic_help_24px);
		fab.setContentDescription(getString(R.string.ask_a_question));
		fab.setOnClickListener(v -> {
			Bundle args = new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), AskQuestionFragment.class, args);
		});
	}

	@Override
	protected void doLoadData(int offset, int count) {
		currentRequest = new GetQuestions(offset == 0 ? null : maxID, count)
				.setCallback(new SimpleCallback<>(this) {
					@Override
					public void onSuccess(List<Status> result) {
						if (!result.isEmpty())
							maxID = result.get(result.size() - 1).id;
						onDataLoaded(result, !result.isEmpty());
					}
				})
				.exec(accountID);
	}

	@Override
	protected void onShown() {
		super.onShown();
		if (!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
			loadData();
	}

	@Override
	public void onItemClick(String id) {
		Status status = getContentStatusByID(id);
		if (status == null) return;
		Bundle args = new Bundle();
		args.putString("account", accountID);
		args.putParcelable("question", Parcels.wrap(status));
		Nav.go(getActivity(), QuestionDetailFragment.class, args);
	}
}
