package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.os.Bundle;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.timelines.GetFriendsActivity;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.FriendsActivityItem;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.ReblogOrReplyLineStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.grishka.appkit.api.SimpleCallback;

public class OrbitFragment extends StatusListFragment{
	private String maxID;
	private Map<String, List<FriendsActivityItem.Interaction>> interactionsByStatusID=new HashMap<>();

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setTitle(getString(R.string.orbit));
		loadData();
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetFriendsActivity(offset>0 ? maxID : null, count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<FriendsActivityItem> result){
						if(getActivity()==null)
							return;
						List<Status> statuses=new ArrayList<>();
						for(FriendsActivityItem item : result){
							if(item.status!=null){
								statuses.add(item.status);
								if(item.interactions!=null && !item.interactions.isEmpty()){
									interactionsByStatusID.put(item.status.id, item.interactions);
								}
							}
						}
						if(!result.isEmpty())
							maxID=result.get(result.size()-1).id;
						onDataLoaded(statuses, !result.isEmpty());
					}
				})
				.exec(accountID);
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(Status s){
		List<StatusDisplayItem> items=new ArrayList<>();
		List<FriendsActivityItem.Interaction> interactions=interactionsByStatusID.get(s.id);
		if(interactions!=null && !interactions.isEmpty()){
			FriendsActivityItem.Interaction first=interactions.get(0);
			String text;
			int icon;
			Account account=first.account;
			switch(first.type){
				case "favourite" -> {
					text=getString(R.string.user_favourited);
					icon=R.drawable.ic_star_wght700grad200fill1_20px;
				}
				case "reply" -> {
					text=getString(R.string.user_replied);
					icon=R.drawable.ic_reply_wght700_20px;
				}
				default -> {
					text=getString(R.string.user_boosted);
					icon=R.drawable.ic_repeat_wght700_20px;
				}
			}
			if(interactions.size()>1){
				text=text.replace("%s", "%s and "+(interactions.size()-1)+" others");
			}
			items.add(new ReblogOrReplyLineStatusDisplayItem(s.id, this, getActivity(), text, account, icon, accountID));
		}
		items.addAll(StatusDisplayItem.buildItems(this, s, accountID, s, knownAccounts, true));
		return items;
	}
}
