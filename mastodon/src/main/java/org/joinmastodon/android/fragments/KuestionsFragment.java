package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;

import org.joinmastodon.android.api.requests.timelines.GetQuestionsTimeline;
import org.joinmastodon.android.model.Status;

import java.util.List;

import me.grishka.appkit.api.SimpleCallback;

public class KuestionsFragment extends StatusListFragment {
	private String maxID;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		accountID = getArguments().getString("account");
	}

	@Override
	protected void doLoadData(int offset, int count) {
		currentRequest = new GetQuestionsTimeline(offset == 0 ? null : maxID, count)
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

}
