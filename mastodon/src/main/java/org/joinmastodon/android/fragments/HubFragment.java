package org.joinmastodon.android.fragments;

import android.app.Fragment;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.flexbox.FlexboxLayout;

import org.joinmastodon.android.R;

import java.util.List;

import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.utils.V;

public class HubFragment extends AppKitFragment {

	private record SpaceEntry(
		HomeFragment.Space space,
		@DrawableRes int icon,
		@StringRes int label,
		int color
	) {}

	// Planet colors matching KronkSpace.kt
	private static final int COLOR_KOMMONS   = Color.parseColor("#B97FFF");
	private static final int COLOR_EVENTS    = Color.parseColor("#4FD1A0");
	private static final int COLOR_NUDGES    = Color.parseColor("#FF6B9D");
	private static final int COLOR_KUESTIONS = Color.parseColor("#FFD166");

	private static final List<SpaceEntry> SPACES = List.of(
		new SpaceEntry(HomeFragment.Space.KOMMONS,   R.drawable.ic_gavel_24px,   R.string.kommons_space,    COLOR_KOMMONS),
		new SpaceEntry(HomeFragment.Space.EVENTS,    R.drawable.ic_tab_events,   R.string.tab_events,       COLOR_EVENTS),
		new SpaceEntry(HomeFragment.Space.NUDGES,    R.drawable.ic_tab_nudges,   R.string.tab_nudges,       COLOR_NUDGES),
		new SpaceEntry(HomeFragment.Space.QUESTIONS, R.drawable.ic_help_24px,    R.string.tab_kuestions,    COLOR_KUESTIONS)
	);

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_hub, container, false);
		FlexboxLayout spacesContainer = view.findViewById(R.id.spaces_container);

		for (SpaceEntry entry : SPACES) {
			View bubble = inflater.inflate(R.layout.item_space_bubble, spacesContainer, false);

			ImageView icon = bubble.findViewById(R.id.icon);
			icon.setImageResource(entry.icon());
			icon.setImageTintList(ColorStateList.valueOf(Color.WHITE));

			((TextView) bubble.findViewById(R.id.label)).setText(entry.label());

			FrameLayout bubbleView = bubble.findViewById(R.id.bubble);
			GradientDrawable shape = new GradientDrawable();
			shape.setShape(GradientDrawable.OVAL);
			shape.setColor(entry.color());
			bubbleView.setBackground(new RippleDrawable(
					ColorStateList.valueOf(withAlpha(Color.WHITE, 80)),
					shape, null));

			bubbleView.setOnClickListener(v -> open(entry.space()));
			spacesContainer.addView(bubble);
		}

		return view;
	}

	private static int withAlpha(int color, int alpha) {
		return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
	}

	private void open(HomeFragment.Space space) {
		Fragment parent = getParentFragment();
		if (parent instanceof HomeFragment home) {
			home.openSpace(space);
		}
	}
}
