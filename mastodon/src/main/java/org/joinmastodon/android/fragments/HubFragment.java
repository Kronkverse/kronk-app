package org.joinmastodon.android.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.joinmastodon.android.R;

import androidx.annotation.Nullable;
import me.grishka.appkit.fragments.AppKitFragment;

public class HubFragment extends AppKitFragment{

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		View view=inflater.inflate(R.layout.fragment_hub, container, false);

		view.findViewById(R.id.tile_feed).setOnClickListener(v->open(HomeFragment.Space.FEED));
		view.findViewById(R.id.tile_events).setOnClickListener(v->open(HomeFragment.Space.EVENTS));
		view.findViewById(R.id.tile_huddle).setOnClickListener(v->open(HomeFragment.Space.HUDDLE));
		view.findViewById(R.id.tile_kommons).setOnClickListener(v->open(HomeFragment.Space.KOMMONS));

		return view;
	}

	private void open(HomeFragment.Space space){
		Fragment parent=getParentFragment();
		if(parent instanceof HomeFragment home){
			home.openSpace(space);
		}
	}
}
