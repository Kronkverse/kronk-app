package org.joinmastodon.android.ui.sheets;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.SearchAccounts;
import org.joinmastodon.android.api.requests.statuses.AddMediaTag;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.MediaTag;

import java.util.List;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.BottomSheet;

public class TagPeopleSheet extends BottomSheet {
	private final String mediaId;
	private final String accountID;
	private final Runnable onTagAdded;

	private EditText searchField;
	private ProgressBar progress;
	private LinearLayout resultsContainer;
	private Runnable debouncer;

	private static final long DEBOUNCE_MS = 400;

	public TagPeopleSheet(Context context, String mediaId, String accountID, Runnable onTagAdded) {
		super(context);
		this.mediaId = mediaId;
		this.accountID = accountID;
		this.onTagAdded = onTagAdded;

		View content = LayoutInflater.from(context).inflate(R.layout.sheet_tag_people, null);
		setContentView(content);

		searchField = content.findViewById(R.id.search_field);
		progress = content.findViewById(R.id.search_progress);
		resultsContainer = content.findViewById(R.id.results_container);

		searchField.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
			@Override public void afterTextChanged(Editable s) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if(debouncer!=null) searchField.removeCallbacks(debouncer);
				String query=s.toString().trim();
				if(query.isEmpty()){
					resultsContainer.removeAllViews();
					progress.setVisibility(View.GONE);
					return;
				}
				debouncer=()->doSearch(query);
				searchField.postDelayed(debouncer, DEBOUNCE_MS);
			}
		});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE |
				WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
	}

	private void doSearch(String query){
		progress.setVisibility(View.VISIBLE);
		resultsContainer.removeAllViews();
		new SearchAccounts(query, 10, 0, false, false)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Account> result){
						progress.setVisibility(View.GONE);
						resultsContainer.removeAllViews();
						for(Account account : result) addResultRow(account);
					}

					@Override
					public void onError(ErrorResponse error){
						progress.setVisibility(View.GONE);
					}
				})
				.exec(accountID);
	}

	private void addResultRow(Account account){
		Context ctx=getContext();

		LinearLayout row=new LinearLayout(ctx);
		row.setOrientation(LinearLayout.VERTICAL);
		row.setPadding(V.dp(16), V.dp(10), V.dp(16), V.dp(10));

		TypedValue tv=new TypedValue();
		ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
		row.setBackgroundResource(tv.resourceId);

		String displayName=account.displayName!=null && !account.displayName.isEmpty()
				? account.displayName : account.username;

		TextView nameView=new TextView(ctx);
		nameView.setText(displayName);
		nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

		TextView acctView=new TextView(ctx);
		acctView.setText("@"+account.acct);
		acctView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
		acctView.setAlpha(0.6f);

		row.addView(nameView);
		row.addView(acctView);
		row.setOnClickListener(v->submitTag(account));

		resultsContainer.addView(row);
	}

	private void submitTag(Account account){
		dismiss();
		new AddMediaTag(mediaId, account.id, 0.5, 0.5)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(MediaTag result){
						if(onTagAdded!=null) onTagAdded.run();
					}

					@Override
					public void onError(ErrorResponse error){
						Toast.makeText(getContext(), "Could not add tag", Toast.LENGTH_SHORT).show();
					}
				})
				.exec(accountID);
	}
}
