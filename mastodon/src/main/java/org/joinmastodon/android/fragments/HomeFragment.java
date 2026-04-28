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
import android.widget.LinearLayout;

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
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.utils.ObjectIdComparator;
import org.parceler.Parcels;

import java.util.EnumSet;
import java.util.List;

import androidx.annotation.Nullable;
import me.grishka.appkit.FragmentStackActivity;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.fragments.LoaderFragment;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public class HomeFragment extends AppKitFragment implements AssistContentProviderFragment {

	public enum Space { HUB, FEED, EVENTS, HUDDLE }

	private FragmentRootLinearLayout content;
	private FrameLayout fragmentContainer;

	private HubFragment hubFragment;
	private HomeTimelineFragment feedFragment;
	private EventsFragment eventsFragment;
	private LiveFragment huddleFragment;
	private NotificationsListFragment notificationsFragment;

	private View bottomNavWrap;
	private View navProfile;
	private View navHub;
	private View navNotifications;

	private Space currentSpace = Space.HUB;
	private boolean showingNotifications;
	private String accountID;

	// Registered while any non-hub space is active so back press returns to hub.
	private final Runnable spaceBackCallback = this::switchToHub;

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

			hubFragment = new HubFragment();
			hubFragment.setArguments(new Bundle(args));

			feedFragment = new HomeTimelineFragment();
			feedFragment.setArguments(new Bundle(args));

			Bundle lazyArgs = new Bundle(args);
			lazyArgs.putBoolean("noAutoLoad", true);

			eventsFragment = new EventsFragment();
			eventsFragment.setArguments(new Bundle(lazyArgs));

			huddleFragment = new LiveFragment();
			huddleFragment.setArguments(new Bundle(lazyArgs));

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
		content = new FragmentRootLinearLayout(getActivity());
		content.setOrientation(LinearLayout.VERTICAL);

		fragmentContainer = new FrameLayout(getActivity());
		fragmentContainer.setId(me.grishka.appkit.R.id.fragment_wrap);
		content.addView(fragmentContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

		inflater.inflate(R.layout.bottom_nav_bar, content);
		bottomNavWrap = content.findViewById(R.id.bottom_nav_wrap);
		navProfile = content.findViewById(R.id.nav_profile);
		navHub = content.findViewById(R.id.nav_hub);
		navNotifications = content.findViewById(R.id.nav_notifications);

		navProfile.setOnClickListener(v -> onProfileTapped());
		navHub.setOnClickListener(v -> onHubTapped());
		navNotifications.setOnClickListener(v -> onNotificationsTapped());

		if (savedInstanceState == null) {
			getChildFragmentManager().beginTransaction()
					.add(me.grishka.appkit.R.id.fragment_wrap, hubFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, feedFragment).hide(feedFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, eventsFragment).hide(eventsFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, huddleFragment).hide(huddleFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, notificationsFragment).hide(notificationsFragment)
					.commit();

			String defaultTab = getArguments().getString("tab");
			if ("notifications".equals(defaultTab)) {
				fragmentContainer.post(this::switchToNotifications);
			}
		}

		updateNavSelection();
		return content;
	}

	// Called by HubFragment tiles
	public void openSpace(Space space) {
		if (showingNotifications) {
			showingNotifications = false;
			getChildFragmentManager().beginTransaction().hide(notificationsFragment).show(hubFragment).commitNow();
		}
		if (space == currentSpace && space != Space.HUB) return;
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

		if (space != Space.HUB) {
			addBackCallback(spaceBackCallback);
			maybeTriggerLoading(incoming);
		} else {
			removeBackCallback(spaceBackCallback);
		}

		updateNavSelection();
		((FragmentStackActivity) getActivity()).invalidateSystemBarColors(this);
	}

	private void switchToHub() {
		switchToSpace(Space.HUB);
	}

	private void switchToNotifications() {
		if (currentSpace != Space.HUB) {
			// Return to hub first so hub is restored when leaving notifications
			getChildFragmentManager().beginTransaction()
					.hide(fragmentForSpace(currentSpace))
					.show(hubFragment)
					.commitNow();
			currentSpace = Space.HUB;
			removeBackCallback(spaceBackCallback);
		}
		showingNotifications = true;
		getChildFragmentManager().beginTransaction()
				.hide(hubFragment)
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
			case FEED: return feedFragment;
			case EVENTS: return eventsFragment;
			case HUDDLE: return huddleFragment;
			default: return hubFragment;
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
					.show(fragmentForSpace(currentSpace))
					.commit();
			updateNavSelection();
			((FragmentStackActivity) getActivity()).invalidateSystemBarColors(this);
		} else if (currentSpace != Space.HUB) {
			switchToHub();
		}
		// Already on hub: no-op
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
		}
	}

	// Forward permission results to child fragments — the system only delivers
	// them to the parent fragment; children need explicit forwarding.
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

	// Public API used by HomeTimelineFragment and AccountSwitcherSheet
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
			switchToHub();
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
		if (notificationsFragment != null) getChildFragmentManager().putFragment(outState, "notificationsFragment", notificationsFragment);
	}

	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		if (savedInstanceState == null || hubFragment != null) return;
		hubFragment = (HubFragment) getChildFragmentManager().getFragment(savedInstanceState, "hubFragment");
		feedFragment = (HomeTimelineFragment) getChildFragmentManager().getFragment(savedInstanceState, "feedFragment");
		eventsFragment = (EventsFragment) getChildFragmentManager().getFragment(savedInstanceState, "eventsFragment");
		huddleFragment = (LiveFragment) getChildFragmentManager().getFragment(savedInstanceState, "huddleFragment");
		notificationsFragment = (NotificationsListFragment) getChildFragmentManager().getFragment(savedInstanceState, "notificationsFragment");
		showingNotifications = savedInstanceState.getBoolean("showingNotifications");
		currentSpace = Space.valueOf(savedInstanceState.getString("currentSpace", Space.HUB.name()));
		if (currentSpace != Space.HUB) addBackCallback(spaceBackCallback);
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
