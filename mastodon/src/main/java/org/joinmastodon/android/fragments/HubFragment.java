package org.joinmastodon.android.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.joinmastodon.android.R;

import androidx.annotation.Nullable;
import me.grishka.appkit.Nav;
import me.grishka.appkit.fragments.AppKitFragment;

public class HubFragment extends AppKitFragment {
	private String accountID;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		accountID = getArguments().getString("account");
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_hub, container, false);

		view.findViewById(R.id.tile_feed).setOnClickListener(v -> navigateToFeed());
		view.findViewById(R.id.tile_events).setOnClickListener(v -> navigateToEvents());
		view.findViewById(R.id.tile_huddle).setOnClickListener(v -> navigateToHuddle());
		view.findViewById(R.id.tile_kommons).setOnClickListener(v -> navigateToKommons());

		return view;
	}

	private void navigateToFeed() {
		Bundle args = new Bundle();
		args.putString("account", accountID);
		Nav.go(getActivity(), HomeTimelineFragment.class, args);
	}

	private void navigateToEvents() {
		Bundle args = new Bundle();
		args.putString("account", accountID);
		Nav.go(getActivity(), EventsFragment.class, args);
	}

	private void navigateToHuddle() {
		Bundle args = new Bundle();
		args.putString("account", accountID);
		Nav.go(getActivity(), LiveFragment.class, args);
	}

	private void navigateToKommons() {
		// Kommons native space — coming soon
	}
}
