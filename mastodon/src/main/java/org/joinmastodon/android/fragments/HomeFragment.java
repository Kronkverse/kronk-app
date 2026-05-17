package org.joinmastodon.android.fragments;

import android.app.NotificationManager;
import android.app.assist.AssistContent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.PushNotificationReceiver;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.notifications.GetNotificationsV1;
import org.joinmastodon.android.api.requests.notifications.GetUnreadNotificationsCount;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.NotificationsMarkerUpdatedEvent;
import org.joinmastodon.android.events.StatusDisplaySettingsChangedEvent;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.NotificationType;
import org.joinmastodon.android.ui.compose.KronkSpace;
import org.joinmastodon.android.ui.compose.SpaceUsageTracker;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.utils.ObjectIdComparator;
import org.parceler.Parcels;

import java.util.EnumSet;
import java.util.List;

import me.grishka.appkit.FragmentStackActivity;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.fragments.LoaderFragment;
import me.grishka.appkit.utils.V;

public class HomeFragment extends AppKitFragment implements AssistContentProviderFragment {

	public enum Space { FEED, EVENTS, HUDDLE, KOMMONS, NUDGES }

	private FrameLayout fragmentContainer;
	private FrameLayout sheetContainer;
	private BottomSheetBehavior<FrameLayout> sheetBehavior;

	private KronkHubFragment hubFragment;
	private HomeTimelineFragment feedFragment;
	private EventsFragment eventsFragment;
	private LiveFragment huddleFragment;
	private KommonsFragment kommonsFragment;
	private NotificationsListFragment notificationsFragment;

	private View bottomNavWrap;
	private View navProfile;
	private View navHub;
	private View navNotifications;

	private Space currentSpace = Space.FEED;
	private boolean showingNotifications;
	private String accountID;

	private final Runnable spaceBackCallback = () -> switchToSpace(Space.FEED);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		accountID = getArguments().getString("account");
		setTitle(R.string.app_name);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
			setRetainInstance(true);

		if (savedInstanceState == null) {
			Bundle args = new Bundle();
			args.putString("account", accountID);

			hubFragment = new KronkHubFragment();
			hubFragment.setArguments(new Bundle(args));

			feedFragment = new HomeTimelineFragment();
			feedFragment.setArguments(new Bundle(args));

			Bundle lazyArgs = new Bundle(args);
			lazyArgs.putBoolean("noAutoLoad", true);

			eventsFragment = new EventsFragment();
			eventsFragment.setArguments(new Bundle(lazyArgs));

			huddleFragment = new LiveFragment();
			huddleFragment.setArguments(new Bundle(lazyArgs));

			kommonsFragment = new KommonsFragment();
			kommonsFragment.setArguments(new Bundle(lazyArgs));

			notificationsFragment = new NotificationsListFragment();
			notificationsFragment.setArguments(new Bundle(lazyArgs));
		}

		E.register(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		E.unregister(this);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
		CoordinatorLayout root = new CoordinatorLayout(getActivity());

		fragmentContainer = new FrameLayout(getActivity());
		fragmentContainer.setId(me.grishka.appkit.R.id.fragment_wrap);
		CoordinatorLayout.LayoutParams fcParams = new CoordinatorLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		root.addView(fragmentContainer, fcParams);

		sheetContainer = new FrameLayout(getActivity());
		sheetContainer.setId(View.generateViewId());
		CoordinatorLayout.LayoutParams sheetParams = new CoordinatorLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		sheetParams.setBehavior(new BottomSheetBehavior<>());
		root.addView(sheetContainer, sheetParams);

		sheetBehavior = BottomSheetBehavior.from(sheetContainer);
		sheetBehavior.setPeekHeight(V.dp(80));
		sheetBehavior.setHalfExpandedRatio(0.35f);
		sheetBehavior.setFitToContents(false);
		sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
		sheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View bottomSheet, int newState) {}

			@Override
			public void onSlide(@NonNull View bottomSheet, float slideOffset) {
				if (hubFragment != null) hubFragment.updateSlideOffset(slideOffset);
			}
		});

		inflater.inflate(R.layout.bottom_nav_bar, root);
		bottomNavWrap = root.findViewById(R.id.bottom_nav_wrap);
		navProfile = root.findViewById(R.id.nav_profile);
		navHub = root.findViewById(R.id.nav_hub);
		navNotifications = root.findViewById(R.id.nav_notifications);

		navProfile.setOnClickListener(v -> onProfileTapped());
		navHub.setOnClickListener(v -> onHubTapped());
		navNotifications.setOnClickListener(v -> onNotificationsTapped());

		if (savedInstanceState == null) {
			getChildFragmentManager().beginTransaction()
					.add(sheetContainer.getId(), hubFragment)
					.commit();

			getChildFragmentManager().beginTransaction()
					.add(me.grishka.appkit.R.id.fragment_wrap, feedFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, eventsFragment).hide(eventsFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, huddleFragment).hide(huddleFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, kommonsFragment).hide(kommonsFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, notificationsFragment).hide(notificationsFragment)
					.commit();

			String defaultTab = getArguments().getString("tab");
			if ("notifications".equals(defaultTab)) {
				fragmentContainer.post(this::switchToNotifications);
			}
		}

		updateNavSelection();
		return root;
	}

	public void openSpace(Space space) {
		if (sheetBehavior != null) {
			sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
		}
		if (showingNotifications) {
			showingNotifications = false;
			getChildFragmentManager().beginTransaction()
					.hide(notificationsFragment)
					.show(fragmentForSpace(currentSpace))
					.commitNow();
		}
		if (space == currentSpace) return;
		switchToSpace(space);
	}

	private void switchToSpace(Space space) {
		android.app.Fragment outgoing = fragmentForSpace(currentSpace);
		android.app.Fragment incoming = fragmentForSpace(space);

		getChildFragmentManager().beginTransaction()
				.hide(outgoing)
				.show(incoming)
				.commit();

		currentSpace = space;
		SpaceUsageTracker.INSTANCE.increment(getActivity(), accountID, toKronkSpace(space));

		if (space != Space.FEED) {
			addBackCallback(spaceBackCallback);
			maybeTriggerLoading(incoming);
		} else {
			removeBackCallback(spaceBackCallback);
		}

		updateNavSelection();
		((FragmentStackActivity) getActivity()).invalidateSystemBarColors(this);
	}

	private void switchToNotifications() {
		showingNotifications = true;
		getChildFragmentManager().beginTransaction()
				.hide(fragmentForSpace(currentSpace))
				.show(notificationsFragment)
				.commit();
		maybeTriggerLoading(notificationsFragment);
		updateNavSelection();
		NotificationManager nm = getActivity().getSystemService(NotificationManager.class);
		nm.cancel(accountID, PushNotificationReceiver.NOTIFICATION_ID);
		((FragmentStackActivity) getActivity()).invalidateSystemBarColors(this);
	}

	private android.app.Fragment fragmentForSpace(Space space) {
		switch (space) {
			case EVENTS: return eventsFragment;
			case HUDDLE: return huddleFragment;
			case KOMMONS: return kommonsFragment;
			default: return feedFragment;
		}
	}

	private KronkSpace toKronkSpace(Space space) {
		switch (space) {
			case EVENTS: return KronkSpace.KALENDAR;
			case HUDDLE: return KronkSpace.HUDDLE;
			case KOMMONS: return KronkSpace.KOMMONS;
			default: return KronkSpace.MURMUR;
		}
	}

	private void onProfileTapped() {
		AccountSession session = AccountSessionManager.get(accountID);
		Bundle args = new Bundle();
		args.putString("account", accountID);
		args.putParcelable("profileAccount", Parcels.wrap(session.self));
		Nav.go(getActivity(), ProfileFragment.class, args);
	}

	private void onHubTapped() {
		if (showingNotifications) {
			showingNotifications = false;
			getChildFragmentManager().beginTransaction()
					.hide(notificationsFragment)
					.show(feedFragment)
					.commit();
			currentSpace = Space.FEED;
			updateNavSelection();
			return;
		}
		int state = sheetBehavior.getState();
		if (state == BottomSheetBehavior.STATE_COLLAPSED) {
			sheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
		} else if (state == BottomSheetBehavior.STATE_HALF_EXPANDED) {
			sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
		} else {
			sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
		}
	}

	private void onNotificationsTapped() {
		if (!showingNotifications) {
			switchToNotifications();
		}
	}

	private void updateNavSelection() {
		if (navHub == null) return;
		navHub.setSelected(!showingNotifications);
		navNotifications.setSelected(showingNotifications);
		navProfile.setSelected(false);
	}

	private void maybeTriggerLoading(android.app.Fragment fragment) {
		if (fragment instanceof LoaderFragment lf) {
			if (!lf.loaded && !lf.dataLoading)
				lf.loadData();
		} else if (fragment instanceof LiveFragment lf) {
			lf.loadData();
		} else if (fragment instanceof KommonsFragment kf) {
			kf.loadData();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		for (android.app.Fragment child : getChildFragmentManager().getFragments()) {
			if (child != null) {
				child.onRequestPermissionsResult(requestCode, permissions, grantResults);
			}
		}
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		android.app.Fragment visible = showingNotifications ? notificationsFragment : fragmentForSpace(currentSpace);
		visible.onHiddenChanged(hidden);
	}

	@Override
	public boolean wantsLightStatusBar() {
		return !UiUtils.isDarkTheme();
	}

	@Override
	public boolean wantsLightNavigationBar() {
		return !UiUtils.isDarkTheme();
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets) {
		if (Build.VERSION.SDK_INT >= 27) {
			int inset = insets.getSystemWindowInsetBottom();
			bottomNavWrap.setPadding(0, 0, 0, inset > 0 ? Math.max(inset, V.dp(24)) : 0);
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0));
		} else {
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()));
		}
		WindowInsets topOnly = insets.replaceSystemWindowInsets(0, insets.getSystemWindowInsetTop(), 0, 0);
		feedFragment.onApplyWindowInsets(topOnly);
		notificationsFragment.onApplyWindowInsets(topOnly);
	}

	public void addSpaceBackCallback(Runnable cb) { addBackCallback(cb); }
	public void removeSpaceBackCallback(Runnable cb) { removeBackCallback(cb); }

	public void showNotifications() {
		if (!showingNotifications) switchToNotifications();
	}

	public void hideNotifications() {
		if (showingNotifications) {
			showingNotifications = false;
			getChildFragmentManager().beginTransaction()
					.hide(notificationsFragment)
					.show(fragmentForSpace(currentSpace))
					.commit();
			updateNavSelection();
			((FragmentStackActivity) getActivity()).invalidateSystemBarColors(this);
		}
	}

	public boolean isShowingNotifications() {
		return showingNotifications;
	}

	public void setCurrentTab(@androidx.annotation.IdRes int tabId) {
		if (tabId == R.id.tab_live) {
			openSpace(Space.HUDDLE);
		} else {
			openSpace(Space.FEED);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("showingNotifications", showingNotifications);
		outState.putString("currentSpace", currentSpace.name());
		if (hubFragment != null) getChildFragmentManager().putFragment(outState, "hubFragment", hubFragment);
		if (feedFragment != null) getChildFragmentManager().putFragment(outState, "feedFragment", feedFragment);
		if (eventsFragment != null) getChildFragmentManager().putFragment(outState, "eventsFragment", eventsFragment);
		if (huddleFragment != null) getChildFragmentManager().putFragment(outState, "huddleFragment", huddleFragment);
		if (kommonsFragment != null) getChildFragmentManager().putFragment(outState, "kommonsFragment", kommonsFragment);
		if (notificationsFragment != null) getChildFragmentManager().putFragment(outState, "notificationsFragment", notificationsFragment);
	}

	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		if (savedInstanceState == null || hubFragment != null) return;
		hubFragment = (KronkHubFragment) getChildFragmentManager().getFragment(savedInstanceState, "hubFragment");
		feedFragment = (HomeTimelineFragment) getChildFragmentManager().getFragment(savedInstanceState, "feedFragment");
		eventsFragment = (EventsFragment) getChildFragmentManager().getFragment(savedInstanceState, "eventsFragment");
		huddleFragment = (LiveFragment) getChildFragmentManager().getFragment(savedInstanceState, "huddleFragment");
		kommonsFragment = (KommonsFragment) getChildFragmentManager().getFragment(savedInstanceState, "kommonsFragment");
		notificationsFragment = (NotificationsListFragment) getChildFragmentManager().getFragment(savedInstanceState, "notificationsFragment");
		showingNotifications = savedInstanceState.getBoolean("showingNotifications");
		currentSpace = Space.valueOf(savedInstanceState.getString("currentSpace", Space.FEED.name()));
		if (currentSpace != Space.FEED) addBackCallback(spaceBackCallback);
		updateNavSelection();
	}

	@Override
	protected void onShown() {
		super.onShown();
		reloadNotificationsForUnreadCount();
	}

	private void reloadNotificationsForUnreadCount() {
		Instance instance = AccountSessionManager.get(accountID).getInstanceInfo();
		if (instance == null) return;
		if (instance.getApiVersion() >= 2) {
			new GetUnreadNotificationsCount(EnumSet.allOf(NotificationType.class), NotificationType.getGroupableTypes())
					.setCallback(new Callback<>() {
						@Override
						public void onSuccess(GetUnreadNotificationsCount.Response result) {
							updateUnreadNotificationsBadge(result.count, false);
						}
						@Override
						public void onError(ErrorResponse error) {}
					})
					.exec(accountID);
		} else {
			List<Notification>[] notifications = new List[]{null};
			String[] marker = {null};
			AccountSessionManager.get(accountID).reloadNotificationsMarker(m -> {
				marker[0] = m;
				if (notifications[0] != null) updateUnreadCountV1(notifications[0], marker[0]);
			});
			new GetNotificationsV1(null, 40, EnumSet.allOf(NotificationType.class))
					.setCallback(new Callback<>() {
						@Override
						public void onSuccess(List<Notification> result) {
							notifications[0] = result;
							if (marker[0] != null) updateUnreadCountV1(notifications[0], marker[0]);
						}
						@Override
						public void onError(ErrorResponse error) {}
					}).exec(accountID);
		}
	}

	private void updateUnreadCountV1(List<Notification> notifications, String marker) {
		if (notifications.isEmpty() || ObjectIdComparator.INSTANCE.compare(notifications.get(0).id, marker) <= 0) {
			updateUnreadNotificationsBadge(0, false);
		} else {
			if (ObjectIdComparator.INSTANCE.compare(notifications.get(notifications.size() - 1).id, marker) > 0) {
				updateUnreadNotificationsBadge(notifications.size(), true);
			} else {
				int count = 0;
				for (Notification n : notifications) {
					if (n.id.equals(marker)) break;
					count++;
				}
				updateUnreadNotificationsBadge(count, false);
			}
		}
	}

	private void updateUnreadNotificationsBadge(int count, boolean more) {}

	@Subscribe
	public void onNotificationsMarkerUpdated(NotificationsMarkerUpdatedEvent ev) {
		if (!ev.accountID.equals(accountID)) return;
		if (ev.clearUnread) updateUnreadNotificationsBadge(0, false);
	}

	@Subscribe
	public void onStatusDisplaySettingsChanged(StatusDisplaySettingsChangedEvent ev) {
		if (!ev.accountID.equals(accountID)) return;
		if (feedFragment != null && feedFragment.loaded) feedFragment.rebuildAllDisplayItems();
		if (notificationsFragment != null && notificationsFragment.loaded) notificationsFragment.rebuildAllDisplayItems();
	}

	@Override
	public void onProvideAssistContent(AssistContent assistContent) {}
}
