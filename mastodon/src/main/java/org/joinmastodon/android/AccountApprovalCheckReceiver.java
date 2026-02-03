package org.joinmastodon.android;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.requests.accounts.GetOwnAccount;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;

public class AccountApprovalCheckReceiver extends BroadcastReceiver {
	private static final String TAG = "ApprovalCheckReceiver";
	private static final String CHANNEL_ID = "account_approved";
	private static final String ACTION_CHECK_APPROVAL = "org.joinmastodon.android.CHECK_APPROVAL";
	private static final String EXTRA_ACCOUNT_ID = "accountID";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (!ACTION_CHECK_APPROVAL.equals(intent.getAction())) return;

		String accountID = intent.getStringExtra(EXTRA_ACCOUNT_ID);
		if (accountID == null) {
			Log.w(TAG, "No accountID in intent");
			return;
		}

		Log.i(TAG, "Checking approval status for account: " + accountID);

		AccountSession session = AccountSessionManager.getInstance().tryGetAccount(accountID);
		if (session == null) {
			Log.w(TAG, "Account session not found, cancelling checks");
			cancelApprovalCheck(context, accountID);
			return;
		}

		if (session.activated) {
			Log.i(TAG, "Account already activated, cancelling checks");
			cancelApprovalCheck(context, accountID);
			return;
		}

		// Check account status
		MastodonAPIController.runInBackground(() -> {
			new GetOwnAccount()
				.setCallback(new Callback<>() {
					@Override
					public void onSuccess(Account result) {
						// Account is approved!
						Log.i(TAG, "Account approved! Activating account and showing notification.");

						// Mark the account as activated and save to database
						AccountSession sess = AccountSessionManager.getInstance().tryGetAccount(accountID);
						if (sess != null) {
							sess.activated = true;
							AccountSessionManager.getInstance().writeAccountActivationInfo(accountID);
						}

						showApprovalNotification(context);
						cancelApprovalCheck(context, accountID);
					}

					@Override
					public void onError(ErrorResponse error) {
						// Still not approved, schedule next check
						Log.i(TAG, "Account not yet approved, will check again");
						scheduleApprovalCheck(context, accountID);
					}
				})
				.exec(accountID);
		});
	}

	public static void scheduleApprovalCheck(Context context, String accountID) {
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AccountApprovalCheckReceiver.class);
		intent.setAction(ACTION_CHECK_APPROVAL);
		intent.putExtra(EXTRA_ACCOUNT_ID, accountID);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(
			context,
			accountID.hashCode(),
			intent,
			PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
		);

		// Check every 5 minutes
		long intervalMillis = 5 * 60 * 1000;
		long triggerAtMillis = SystemClock.elapsedRealtime() + intervalMillis;

		alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pendingIntent);
		Log.i(TAG, "Scheduled approval check in 5 minutes");
	}

	public static void cancelApprovalCheck(Context context, String accountID) {
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AccountApprovalCheckReceiver.class);
		intent.setAction(ACTION_CHECK_APPROVAL);
		intent.putExtra(EXTRA_ACCOUNT_ID, accountID);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(
			context,
			accountID.hashCode(),
			intent,
			PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
		);

		alarmManager.cancel(pendingIntent);
		Log.i(TAG, "Cancelled approval checks");
	}

	private void showApprovalNotification(Context context) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		Notification.Builder builder;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(
				CHANNEL_ID,
				"Account Approved",
				NotificationManager.IMPORTANCE_HIGH
			);
			channel.setDescription("Notifications when your account is approved");
			notificationManager.createNotificationChannel(channel);
			builder = new Notification.Builder(context, CHANNEL_ID);
		} else {
			builder = new Notification.Builder(context)
				.setPriority(Notification.PRIORITY_HIGH);
		}

		Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

		builder.setSmallIcon(R.drawable.ic_ntf_logo)
			.setContentTitle(context.getString(R.string.account_approved_title))
			.setContentText(context.getString(R.string.account_approved_body))
			.setContentIntent(pendingIntent)
			.setAutoCancel(true);

		notificationManager.notify(1002, builder.build());
	}
}
