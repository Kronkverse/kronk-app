package org.joinmastodon.android.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.flexbox.FlexboxLayout;

import org.joinmastodon.android.R;

import java.util.List;

import me.grishka.appkit.fragments.AppKitFragment;

public class HubFragment extends AppKitFragment {

	private record SpaceEntry(
		HomeFragment.Space space,
		@DrawableRes int icon,
		@StringRes int label
	) {}

	private static final List<SpaceEntry> SPACES = List.of(
		new SpaceEntry(HomeFragment.Space.KOMMONS,     R.drawable.ic_gavel_24px,   R.string.kommons_space),
		new SpaceEntry(HomeFragment.Space.EVENTS,      R.drawable.ic_tab_events,   R.string.tab_events),
		new SpaceEntry(HomeFragment.Space.NUDGES,      R.drawable.ic_tab_nudges,   R.string.tab_nudges)
	);

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_hub, container, false);
		FlexboxLayout spacesContainer = view.findViewById(R.id.spaces_container);

		for (SpaceEntry entry : SPACES) {
			View bubble = inflater.inflate(R.layout.item_space_bubble, spacesContainer, false);
			((ImageView) bubble.findViewById(R.id.icon)).setImageResource(entry.icon());
			((TextView) bubble.findViewById(R.id.label)).setText(entry.label());
			bubble.findViewById(R.id.bubble).setOnClickListener(v -> open(entry.space()));
			spacesContainer.addView(bubble);
		}

		return view;
	}

	private void open(HomeFragment.Space space) {
		Fragment parent = getParentFragment();
		if (parent instanceof HomeFragment home) {
			home.openSpace(space);
		}
	}
}
