package org.joinmastodon.android.fragments;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.NotificationManager;
import android.app.assist.AssistContent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.E;
import org.joinmastodon.android.PushNotificationReceiver;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.notifications.GetNotificationsV1;
import org.joinmastodon.android.api.requests.notifications.GetUnreadNotificationsCount;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.NotificationsMarkerUpdatedEvent;
import org.joinmastodon.android.events.StatusDisplaySettingsChangedEvent;
import org.joinmastodon.android.fragments.onboarding.OnboardingFollowSuggestionsFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.NotificationType;
import org.joinmastodon.android.ui.sheets.AccountSwitcherSheet;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.SwipeInterceptFrameLayout;
import org.joinmastodon.android.ui.views.TabBar;
import org.joinmastodon.android.utils.ObjectIdComparator;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import me.grishka.appkit.FragmentStackActivity;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.fragments.LoaderFragment;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public class HomeFragment extends AppKitFragment implements AssistContentProviderFragment{
	private FragmentRootLinearLayout content;
	private HomeTimelineFragment homeTimelineFragment;
	private NotificationsListFragment notificationsFragment;
	private LiveFragment liveFragment;
	private EventsFragment eventsFragment;
	private TabBar tabBar;
	private View tabBarWrap;
	@IdRes
	private int currentTab=R.id.tab_home;
	private int previousTab=R.id.tab_home;
	private boolean showingNotifications;

	private String accountID;

	// Tab order for swipe navigation
	private static final int[] TAB_ORDER={R.id.tab_live, R.id.tab_home, R.id.tab_events};

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		setTitle(R.string.app_name);

		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);

		if(savedInstanceState==null){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			homeTimelineFragment=new HomeTimelineFragment();
			homeTimelineFragment.setArguments(args);
			args=new Bundle(args);
			args.putBoolean("noAutoLoad", true);
			liveFragment=new LiveFragment();
			liveFragment.setArguments(args);
			notificationsFragment=new NotificationsListFragment();
			notificationsFragment.setArguments(args);
			eventsFragment=new EventsFragment();
			eventsFragment.setArguments(new Bundle(args));
		}

		E.register(this);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		E.unregister(this);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		content=new FragmentRootLinearLayout(getActivity());
		content.setOrientation(LinearLayout.VERTICAL);

		SwipeInterceptFrameLayout fragmentContainer=new SwipeInterceptFrameLayout(getActivity());
		fragmentContainer.setId(me.grishka.appkit.R.id.fragment_wrap);
		fragmentContainer.setOnSwipeListener(new SwipeInterceptFrameLayout.OnSwipeListener(){
			@Override
			public void onSwipeLeft(){
				swipeToNextTab();
			}
			@Override
			public void onSwipeRight(){
				swipeToPreviousTab();
			}
		});
		content.addView(fragmentContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

		inflater.inflate(R.layout.tab_bar, content);
		tabBar=content.findViewById(R.id.tabbar);
		tabBar.setListeners(this::onTabSelected, this::onTabLongClick);
		tabBarWrap=content.findViewById(R.id.tabbar_wrap);

		if(savedInstanceState==null){
			getChildFragmentManager().beginTransaction()
					.add(me.grishka.appkit.R.id.fragment_wrap, homeTimelineFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, liveFragment).hide(liveFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, notificationsFragment).hide(notificationsFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, eventsFragment).hide(eventsFragment)
					.commit();

			String defaultTab=getArguments().getString("tab");
			if("notifications".equals(defaultTab)){
				fragmentContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
					@Override
					public boolean onPreDraw(){
						fragmentContainer.getViewTreeObserver().removeOnPreDrawListener(this);
						showNotifications();
						return true;
					}
				});
			}
		}
		tabBar.selectTab(currentTab);

		return content;
	}

	private int currentTabIndex(){
		for(int i=0; i<TAB_ORDER.length; i++){
			if(TAB_ORDER[i]==currentTab) return i;
		}
		return 1; // default to home
	}

	private void swipeToNextTab(){
		if(showingNotifications){
			hideNotifications();
			return;
		}
		int idx=currentTabIndex();
		if(idx<TAB_ORDER.length-1){
			int nextTab=TAB_ORDER[idx+1];
			tabBar.selectTab(nextTab);
			onTabSelected(nextTab);
		}
	}

	private void swipeToPreviousTab(){
		if(showingNotifications){
			hideNotifications();
			return;
		}
		int idx=currentTabIndex();
		if(idx>0){
			int prevTab=TAB_ORDER[idx-1];
			tabBar.selectTab(prevTab);
			onTabSelected(prevTab);
		}
	}

	@Override
	public void onViewStateRestored(Bundle savedInstanceState){
		super.onViewStateRestored(savedInstanceState);
		if(savedInstanceState==null || homeTimelineFragment!=null)
			return;
		homeTimelineFragment=(HomeTimelineFragment) getChildFragmentManager().getFragment(savedInstanceState, "homeTimelineFragment");
		liveFragment=(LiveFragment) getChildFragmentManager().getFragment(savedInstanceState, "liveFragment");
		notificationsFragment=(NotificationsListFragment) getChildFragmentManager().getFragment(savedInstanceState, "notificationsFragment");
		eventsFragment=(EventsFragment) getChildFragmentManager().getFragment(savedInstanceState, "eventsFragment");
		currentTab=savedInstanceState.getInt("selectedTab");
		showingNotifications=savedInstanceState.getBoolean("showingNotifications");
		tabBar.selectTab(currentTab);
		Fragment current=fragmentForTab(currentTab);
		getChildFragmentManager().beginTransaction()
				.hide(homeTimelineFragment)
				.hide(liveFragment)
				.hide(notificationsFragment)
				.hide(eventsFragment)
				.show(showingNotifications ? notificationsFragment : current)
				.commit();
		if(showingNotifications)
			maybeTriggerLoading(notificationsFragment);
		else
			maybeTriggerLoading(current);
	}

	@Override
	public void onHiddenChanged(boolean hidden){
		super.onHiddenChanged(hidden);
		if(showingNotifications)
			notificationsFragment.onHiddenChanged(hidden);
		else
			fragmentForTab(currentTab).onHiddenChanged(hidden);
	}

	@Override
	public boolean wantsLightStatusBar(){
		return !UiUtils.isDarkTheme();
	}

	@Override
	public boolean wantsLightNavigationBar(){
		return !UiUtils.isDarkTheme();
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=27){
			int inset=insets.getSystemWindowInsetBottom();
			tabBarWrap.setPadding(0, 0, 0, inset>0 ? Math.max(inset, V.dp(24)) : 0);
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0));
		}else{
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()));
		}
		WindowInsets topOnlyInsets=insets.replaceSystemWindowInsets(0, insets.getSystemWindowInsetTop(), 0, 0);
		homeTimelineFragment.onApplyWindowInsets(topOnlyInsets);
		notificationsFragment.onApplyWindowInsets(topOnlyInsets);
	}

	private Fragment fragmentForTab(@IdRes int tab){
		if(tab==R.id.tab_live){
			return liveFragment;
		}else if(tab==R.id.tab_events){
			return eventsFragment;
		}
		return homeTimelineFragment;
	}

	public void setCurrentTab(@IdRes int tab){
		if(tab==currentTab)
			return;
		tabBar.selectTab(tab);
		onTabSelected(tab);
	}

	public void showNotifications(){
		if(showingNotifications)
			return;
		previousTab=currentTab;
		showingNotifications=true;
		getChildFragmentManager().beginTransaction().hide(fragmentForTab(currentTab)).show(notificationsFragment).commit();
		maybeTriggerLoading(notificationsFragment);
		((FragmentStackActivity)getActivity()).invalidateSystemBarColors(this);
	}

	public void hideNotifications(){
		if(!showingNotifications)
			return;
		showingNotifications=false;
		Fragment target=fragmentForTab(previousTab);
		getChildFragmentManager().beginTransaction().hide(notificationsFragment).show(target).commit();
		currentTab=previousTab;
		tabBar.selectTab(currentTab);
		((FragmentStackActivity)getActivity()).invalidateSystemBarColors(this);
	}

	public boolean isShowingNotifications(){
		return showingNotifications;
	}

	private void onTabSelected(@IdRes int tab){
		Fragment newFragment=fragmentForTab(tab);
		if(showingNotifications){
			getChildFragmentManager().beginTransaction().hide(notificationsFragment).show(newFragment).commit();
			showingNotifications=false;
			maybeTriggerLoading(newFragment);
			currentTab=tab;
			((FragmentStackActivity)getActivity()).invalidateSystemBarColors(this);
			return;
		}
		if(tab==currentTab){
			if(newFragment instanceof ScrollableToTop scrollable)
				scrollable.scrollToTop();
			return;
		}
		getChildFragmentManager().beginTransaction().hide(fragmentForTab(currentTab)).show(newFragment).commit();
		maybeTriggerLoading(newFragment);
		currentTab=tab;
		((FragmentStackActivity)getActivity()).invalidateSystemBarColors(this);
	}

	private void maybeTriggerLoading(Fragment newFragment){
		if(newFragment instanceof LoaderFragment lf){
			if(!lf.loaded && !lf.dataLoading)
				lf.loadData();
		}else if(newFragment instanceof LiveFragment){
			((LiveFragment) newFragment).loadData();
		}else if(newFragment instanceof EventsFragment ef){
			if(!ef.loaded && !ef.dataLoading)
				ef.loadData();
		}
		if(newFragment instanceof NotificationsListFragment){
			NotificationManager nm=getActivity().getSystemService(NotificationManager.class);
			nm.cancel(accountID, PushNotificationReceiver.NOTIFICATION_ID);
		}
	}

	private boolean onTabLongClick(@IdRes int tab){
		if(tab==R.id.tab_home && BuildConfig.DEBUG){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), OnboardingFollowSuggestionsFragment.class, args);
		}
		return false;
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putInt("selectedTab", currentTab);
		outState.putBoolean("showingNotifications", showingNotifications);
		getChildFragmentManager().putFragment(outState, "homeTimelineFragment", homeTimelineFragment);
		getChildFragmentManager().putFragment(outState, "liveFragment", liveFragment);
		getChildFragmentManager().putFragment(outState, "notificationsFragment", notificationsFragment);
		getChildFragmentManager().putFragment(outState, "eventsFragment", eventsFragment);
	}

	@Override
	protected void onShown(){
		super.onShown();
		reloadNotificationsForUnreadCount();
	}

	private void reloadNotificationsForUnreadCount(){
		Instance instance=AccountSessionManager.get(accountID).getInstanceInfo();
		if(instance==null)
			return;
		if(instance.getApiVersion()>=2){
			new GetUnreadNotificationsCount(EnumSet.allOf(NotificationType.class), NotificationType.getGroupableTypes())
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(GetUnreadNotificationsCount.Response result){
							updateUnreadNotificationsBadge(result.count, false);
						}

						@Override
						public void onError(ErrorResponse error){

						}
					})
					.exec(accountID);
		}else{
			List<Notification>[] notifications=new List[]{null};
			String[] marker={null};
			AccountSessionManager.get(accountID).reloadNotificationsMarker(m->{
				marker[0]=m;
				if(notifications[0]!=null){
					updateUnreadCountV1(notifications[0], marker[0]);
				}
			});

			new GetNotificationsV1(null, 40, EnumSet.allOf(NotificationType.class))
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(List<Notification> result){
							notifications[0]=result;
							if(marker[0]!=null)
								updateUnreadCountV1(notifications[0], marker[0]);
						}

						@Override
						public void onError(ErrorResponse error){}
					}).exec(accountID);
		}
	}

	@SuppressLint("DefaultLocale")
	private void updateUnreadCountV1(List<Notification> notifications, String marker){
		if(notifications.isEmpty() || ObjectIdComparator.INSTANCE.compare(notifications.get(0).id, marker)<=0){
			updateUnreadNotificationsBadge(0, false);
		}else{
			if(ObjectIdComparator.INSTANCE.compare(notifications.get(notifications.size()-1).id, marker)>0){
				updateUnreadNotificationsBadge(notifications.size(), true);
			}else{
				int count=0;
				for(Notification n:notifications){
					if(n.id.equals(marker))
						break;
					count++;
				}
				updateUnreadNotificationsBadge(count, false);
			}
		}
	}

	private void updateUnreadNotificationsBadge(int count, boolean more){
		if(homeTimelineFragment!=null){
			homeTimelineFragment.updateNotificationsBadge(count, more);
		}
	}

	@Subscribe
	public void onNotificationsMarkerUpdated(NotificationsMarkerUpdatedEvent ev){
		if(!ev.accountID.equals(accountID))
			return;
		if(ev.clearUnread)
			updateUnreadNotificationsBadge(0, false);
	}

	@Subscribe
	public void onStatusDisplaySettingsChanged(StatusDisplaySettingsChangedEvent ev){
		if(!ev.accountID.equals(accountID))
			return;
		if(homeTimelineFragment.loaded)
			homeTimelineFragment.rebuildAllDisplayItems();
		if(notificationsFragment.loaded)
			notificationsFragment.rebuildAllDisplayItems();
	}

	@Override
	public void onProvideAssistContent(AssistContent content){
		if(fragmentForTab(currentTab) instanceof AssistContentProviderFragment provider){
			provider.onProvideAssistContent(content);
		}
	}
}
