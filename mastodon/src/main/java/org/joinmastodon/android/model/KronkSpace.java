package org.joinmastodon.android.model;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import org.joinmastodon.android.R;

import java.util.List;

public class KronkSpace {
	public final String id;
	@StringRes public final int nameRes;
	@DrawableRes public final int iconRes;
	@ColorInt public final int color;

	public KronkSpace(String id, @StringRes int nameRes, @DrawableRes int iconRes, @ColorInt int color) {
		this.id = id;
		this.nameRes = nameRes;
		this.iconRes = iconRes;
		this.color = color;
	}

	public static final List<KronkSpace> ALL = List.of(
		new KronkSpace("home",     R.string.space_home,     R.drawable.ic_home_fill1_24px,         0xFF5F4FE0),
		new KronkSpace("huddle",   R.string.space_huddle,   R.drawable.ic_diversity_2_24px,        0xFFC75D6E),
		new KronkSpace("kalendar", R.string.space_kalendar, R.drawable.ic_calendar_month_fill0_24px, 0xFFF0A500),
		new KronkSpace("kommons",  R.string.space_kommons,  R.drawable.ic_sp_kommons_24px,         0xFF2A9D5C),
		new KronkSpace("market",   R.string.space_market,   R.drawable.ic_sp_market_24px,          0xFF1A8F9C),
		new KronkSpace("nudges",   R.string.space_nudges,   R.drawable.ic_waving_hand_24px,        0xFFE8622A)
	);

	// How many spaces appear in the "My Spaces" pinned row
	public static final int MY_SPACES_COUNT = 3;
}
