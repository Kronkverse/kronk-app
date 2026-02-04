package org.joinmastodon.android.fragments.onboarding;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetOwnAccount;
import org.joinmastodon.android.api.MastodonErrorResponse;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import org.joinmastodon.android.AccountApprovalCheckReceiver;
import android.app.Notification;
import org.joinmastodon.android.api.requests.accounts.ResendConfirmationEmail;
import org.joinmastodon.android.api.requests.accounts.UpdateAccountCredentials;
import org.joinmastodon.android.api.session.AccountActivationInfo;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.settings.SettingsMainFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.ui.sheets.AccountSwitcherSheet;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.io.File;
import java.util.Collections;

import androidx.annotation.Nullable;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.ToolbarFragment;

public class AccountActivationFragment extends ToolbarFragment{
	private String accountID;

	private Button openEmailBtn, resendBtn;
	private View contentView;
	private Handler uiHandler=new Handler(Looper.getMainLooper());
	private Runnable pollRunnable=this::tryGetAccount;
	private APIRequest currentRequest;
	private Runnable resendTimer=this::updateResendTimer;
	private boolean approvalPending=false;
	private static final String CHANNEL_ID = "account_approved";
	private long lastResendTime;
	private boolean visible;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		setTitle(R.string.confirm_email_title);
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
		lastResendTime=session.activationInfo!=null ? session.activationInfo.lastEmailConfirmationResend : 0;
	}

	@Nullable
	@Override
	public View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		View view=inflater.inflate(R.layout.fragment_onboarding_activation, container, false);

		openEmailBtn=view.findViewById(R.id.btn_next);
		openEmailBtn.setOnClickListener(this::onOpenEmailClick);
		openEmailBtn.setOnLongClickListener(v->{
			Bundle args=new Bundle();
			args.putString("account", accountID);
			Nav.go(getActivity(), SettingsMainFragment.class, args);
			return true;
		});
		resendBtn=view.findViewById(R.id.btn_resend);
		resendBtn.setOnClickListener(this::onResendClick);
		TextView text=view.findViewById(R.id.subtitle);
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
		text.setText(getString(R.string.confirm_email_subtitle, session.activationInfo!=null ? session.activationInfo.email : "?"));
		updateResendTimer();

		contentView=view;
		return view;
	}

	@Override
	public boolean wantsLightStatusBar(){
		return !UiUtils.isDarkTheme();
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		getToolbar().setBackground(null);
		getToolbar().setElevation(0);
	}

	@Override
	protected boolean canGoBack(){
		return true;
	}

	@Override
	public void onToolbarNavigationClick(){
		new AccountSwitcherSheet(getActivity(), null).show();
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		super.onApplyWindowInsets(UiUtils.applyBottomInsetToFixedView(contentView, insets));
	}

	@Override
	protected void onShown(){
		super.onShown();
		visible=true;
		tryGetAccount();
	}

	@Override
	protected void onHidden(){
		super.onHidden();
		visible=false;
		if(currentRequest!=null){
			currentRequest.cancel();
			currentRequest=null;
		}else{
			uiHandler.removeCallbacks(pollRunnable);
		}
	}

	private void onOpenEmailClick(View v){
		try{
			startActivity(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		}catch(ActivityNotFoundException|IllegalArgumentException x){
			Toast.makeText(getActivity(), R.string.no_app_to_handle_action, Toast.LENGTH_SHORT).show();
		}
	}

	private void onResendClick(View v){
		new ResendConfirmationEmail(null)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Object result){
						Toast.makeText(getActivity(), R.string.resent_email, Toast.LENGTH_SHORT).show();
						AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
						if(session.activationInfo==null){
							session.activationInfo=new AccountActivationInfo("?", System.currentTimeMillis());
						}else{
							session.activationInfo.lastEmailConfirmationResend=System.currentTimeMillis();
						}
						lastResendTime=session.activationInfo.lastEmailConfirmationResend;
						AccountSessionManager.getInstance().writeAccountActivationInfo(accountID);
						updateResendTimer();
					}

					@Override
					public void onError(ErrorResponse error){
						error.showToast(getActivity());
					}
				})
				.wrapProgress(getActivity(), R.string.loading, false)
				.exec(accountID);
	}

	private void tryGetAccount(){
		if(AccountSessionManager.getInstance().tryGetAccount(accountID)==null){
			uiHandler.removeCallbacks(pollRunnable);
			((MainActivity)getActivity()).restartHomeFragment();
			return;
		}
		currentRequest=new GetOwnAccount()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						currentRequest=null;
						AccountSessionManager mgr=AccountSessionManager.getInstance();
						AccountSession session=mgr.getAccount(accountID);
						Instance instance=mgr.getInstanceInfo(session.domain);
						mgr.removeAccount(accountID);
						mgr.addAccount(instance, session.token, result, session.app, null);
						String newID=mgr.getLastActiveAccountID();
						accountID=newID;

						// Only show notification if user was waiting for approval
						boolean wasWaitingForApproval=approvalPending;
						cancelApprovalCheckWorker();
						if(wasWaitingForApproval){
							showApprovalNotification();
						}

						if((session.self.avatar!=null || session.self.displayName!=null) && !getArguments().getBoolean("debug")){
							new UpdateAccountCredentials(session.self.displayName, "", (File)null, null, Collections.emptyList())
									.setCallback(new Callback<>(){
										@Override
										public void onSuccess(Account result){
											mgr.updateAccountInfo(newID, result);
											proceed();
										}

										@Override
										public void onError(ErrorResponse error){
											proceed();
										}
									})
									.exec(newID);
						}else{
							proceed();
						}
					}

					@Override
					public void onError(ErrorResponse error){
						currentRequest=null;
						// Check if this is a 403 error - means email confirmed but awaiting approval
						if(error instanceof MastodonErrorResponse && ((MastodonErrorResponse)error).httpStatus==403){
							if(!approvalPending){
								approvalPending=true;
								updateUIForApprovalPending();
							}
						}
						uiHandler.postDelayed(pollRunnable, 10_000L);
					}
				})
				.exec(accountID);
	}

	@SuppressLint("DefaultLocale")
	private void updateResendTimer(){
		long sinceResend=System.currentTimeMillis()-lastResendTime;
		if(sinceResend>59_000L){
			resendBtn.setText(R.string.resend);
			resendBtn.setEnabled(true);
			return;
		}
		int seconds=(int)((60_000L-sinceResend)/1000L);
		resendBtn.setText(String.format("%s (%d)", getString(R.string.resend), seconds));
		if(resendBtn.isEnabled())
			resendBtn.setEnabled(false);
		resendBtn.postDelayed(resendTimer, 500);
	}

	@Override
	public void onDestroyView(){
		super.onDestroyView();
		resendBtn.removeCallbacks(resendTimer);
	}

	private void updateUIForApprovalPending(){
		if(getActivity()==null) return;
		getActivity().runOnUiThread(()->{
			setTitle(R.string.approval_pending_title);
			TextView text=contentView.findViewById(R.id.subtitle);
			text.setText(R.string.approval_pending_subtitle);
			// Hide the entire "Didn't get it?" section (resend button's parent)
			if(resendBtn.getParent() instanceof android.view.View){
				((android.view.View)resendBtn.getParent()).setVisibility(android.view.View.GONE);
			}
			// Hide the open email button - not needed for approval pending
			openEmailBtn.setVisibility(android.view.View.GONE);
		});

		// Schedule background worker to check for approval
		scheduleApprovalCheckWorker();
	}

	private void scheduleApprovalCheckWorker(){
		if(getActivity() != null){
			AccountApprovalCheckReceiver.scheduleApprovalCheck(getActivity(), accountID);
		}
	}

	private void showApprovalNotification(){
		if(getActivity()==null) return;
		Context context = getActivity();
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		Notification.Builder builder;
		// Create notification channel for Android O+
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
			NotificationChannel channel = new NotificationChannel(
				CHANNEL_ID,
				"Account Approved",
				NotificationManager.IMPORTANCE_HIGH
			);
			channel.setDescription("Notifications when your account is approved");
			notificationManager.createNotificationChannel(channel);
			builder = new Notification.Builder(context, CHANNEL_ID);
		}else{
			builder = new Notification.Builder(context)
				.setPriority(Notification.PRIORITY_HIGH);
		}

		// Build the notification
		Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

		builder.setSmallIcon(R.drawable.ic_ntf_logo)
			.setContentTitle(context.getString(R.string.account_approved_title))
			.setContentText(context.getString(R.string.account_approved_body))
			.setContentIntent(pendingIntent)
			.setAutoCancel(true);

		notificationManager.notify(1001, builder.build());
	}

	private void cancelApprovalCheckWorker(){
		if(getActivity()!=null){
			AccountApprovalCheckReceiver.cancelApprovalCheck(getActivity(), accountID);
		}
	}

	private void proceed(){
		if(!visible)
			return;
		Bundle args=new Bundle();
		args.putString("account", accountID);
//		Nav.goClearingStack(getActivity(), HomeFragment.class, args);
		Nav.goClearingStack(getActivity(), OnboardingFollowSuggestionsFragment.class, args);
	}
}
