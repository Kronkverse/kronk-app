package org.joinmastodon.android.ui.sheets;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.KronkSpace;

import java.util.List;

import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.BottomSheet;

public class KosmosSheet extends BottomSheet {
	private final Activity activity;
	private OnSpaceSelectedListener listener;

	public interface OnSpaceSelectedListener {
		void onSpaceSelected(KronkSpace space);
	}

	public KosmosSheet(Activity activity) {
		super(activity);
		this.activity = activity;

		View view = LayoutInflater.from(activity).inflate(R.layout.sheet_kosmos, null);
		setContentView(view);

		LinearLayout mySpacesRow = view.findViewById(R.id.my_spaces_row);
		LinearLayout spacesRow1 = view.findViewById(R.id.spaces_row_1);
		LinearLayout spacesRow2 = view.findViewById(R.id.spaces_row_2);

		List<KronkSpace> all = KronkSpace.ALL;

		// My Spaces: first MY_SPACES_COUNT spaces pinned
		for (int i = 0; i < KronkSpace.MY_SPACES_COUNT && i < all.size(); i++) {
			mySpacesRow.addView(inflateTile(mySpacesRow, all.get(i)));
		}

		// The Kosmos: all spaces split into rows of 3
		int cols = 3;
		for (int i = 0; i < all.size(); i++) {
			LinearLayout row = (i < cols) ? spacesRow1 : spacesRow2;
			row.addView(inflateTile(row, all.get(i)));
		}

		// Pad out any incomplete rows so tiles stay equal-width
		int remainder1 = Math.min(all.size(), cols) % cols;
		if (remainder1 != 0) {
			for (int i = remainder1; i < cols; i++) addSpacer(spacesRow1);
		}
		int remainder2 = all.size() > cols ? all.size() % cols : 0;
		if (remainder2 != 0) {
			for (int i = remainder2; i < cols; i++) addSpacer(spacesRow2);
		}
	}

	public KosmosSheet setOnSpaceSelectedListener(OnSpaceSelectedListener l) {
		this.listener = l;
		return this;
	}

	private View inflateTile(ViewGroup parent, KronkSpace space) {
		View tile = LayoutInflater.from(activity).inflate(R.layout.item_space_tile, parent, false);

		View iconContainer = tile.findViewById(R.id.icon_container);
		ImageView icon = tile.findViewById(R.id.icon);
		TextView name = tile.findViewById(R.id.name);

		// Colored circle background
		GradientDrawable circle = new GradientDrawable();
		circle.setShape(GradientDrawable.OVAL);
		circle.setColor(space.color);
		circle.setStroke(V.dp(1), darken(space.color, 0.15f));
		iconContainer.setBackground(circle);

		icon.setImageResource(space.iconRes);
		icon.setColorFilter(0xFFFFFFFF);
		name.setText(space.nameRes);

		tile.setOnClickListener(v -> {
			dismiss();
			if (listener != null) listener.onSpaceSelected(space);
		});

		return tile;
	}

	private void addSpacer(LinearLayout row) {
		View spacer = new View(activity);
		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, 0, 1f);
		spacer.setLayoutParams(p);
		row.addView(spacer);
	}

	private static int darken(int color, float factor) {
		int a = (color >> 24) & 0xFF;
		int r = (int) (((color >> 16) & 0xFF) * (1f - factor));
		int g = (int) (((color >> 8) & 0xFF) * (1f - factor));
		int b = (int) ((color & 0xFF) * (1f - factor));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
}
