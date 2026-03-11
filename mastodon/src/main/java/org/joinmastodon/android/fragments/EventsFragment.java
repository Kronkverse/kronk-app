package org.joinmastodon.android.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.events.GetEvents;
import org.joinmastodon.android.api.requests.events.RsvpEvent;
import org.joinmastodon.android.model.Event;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.utils.V;

public class EventsFragment extends Fragment implements ScrollableToTop{
	private RecyclerView list;
	private SwipeRefreshLayout refreshLayout;
	private LinearLayout emptyView;
	private String accountID;
	private List<Event> events=new ArrayList<>();
	private EventsAdapter adapter;
	public boolean loaded;
	public boolean dataLoading;
	private static final DateTimeFormatter DATE_FORMAT=DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withZone(ZoneId.systemDefault());

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		LinearLayout content=new LinearLayout(getActivity());
		content.setOrientation(LinearLayout.VERTICAL);
		content.setBackgroundColor(UiUtils.getThemeColor(getActivity(), android.R.attr.colorBackground));

		refreshLayout=new SwipeRefreshLayout(getActivity());

		list=new RecyclerView(getActivity());
		list.setId(R.id.list);
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		adapter=new EventsAdapter();
		list.setAdapter(adapter);
		list.setClipToPadding(false);

		refreshLayout.addView(list, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		refreshLayout.setOnRefreshListener(this::loadData);

		emptyView=new LinearLayout(getActivity());
		emptyView.setOrientation(LinearLayout.VERTICAL);
		emptyView.setGravity(android.view.Gravity.CENTER);
		emptyView.setVisibility(View.GONE);
		TextView emptyText=new TextView(getActivity());
		emptyText.setText("No events yet");
		emptyText.setTextSize(16);
		emptyText.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		emptyView.addView(emptyText);

		content.addView(refreshLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
		content.addView(emptyView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

		return content;
	}

	public void loadData(){
		if(dataLoading)
			return;
		dataLoading=true;
		new GetEvents("upcoming", null, 40)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Event> result){
						if(getActivity()==null)
							return;
						dataLoading=false;
						loaded=true;
						refreshLayout.setRefreshing(false);
						events.clear();
						events.addAll(result);
						adapter.notifyDataSetChanged();
						emptyView.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
						refreshLayout.setVisibility(events.isEmpty() ? View.GONE : View.VISIBLE);
					}

					@Override
					public void onError(ErrorResponse error){
						if(getActivity()==null)
							return;
						dataLoading=false;
						refreshLayout.setRefreshing(false);
						// Show empty state on error for now
						emptyView.setVisibility(View.VISIBLE);
						refreshLayout.setVisibility(View.GONE);
					}
				})
				.exec(accountID);
	}

	@Override
	public void scrollToTop(){
		if(list!=null)
			list.smoothScrollToPosition(0);
	}

	private class EventsAdapter extends RecyclerView.Adapter<EventViewHolder>{
		@Override
		public EventViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
			return new EventViewHolder();
		}

		@Override
		public void onBindViewHolder(EventViewHolder holder, int position){
			holder.bind(events.get(position));
		}

		@Override
		public int getItemCount(){
			return events.size();
		}
	}

	private class EventViewHolder extends RecyclerView.ViewHolder{
		private final TextView title, dateTime, location, attendees;
		private final Button rsvpButton;

		EventViewHolder(){
			super(createEventCardView());
			title=itemView.findViewById(android.R.id.title);
			dateTime=itemView.findViewById(android.R.id.text1);
			location=itemView.findViewById(android.R.id.text2);
			attendees=itemView.findViewById(android.R.id.summary);
			rsvpButton=itemView.findViewById(android.R.id.button1);
		}

		void bind(Event event){
			title.setText(event.title);
			if(event.startTime!=null){
				dateTime.setText(DATE_FORMAT.format(event.startTime));
				dateTime.setVisibility(View.VISIBLE);
			}else{
				dateTime.setVisibility(View.GONE);
			}
			if(!TextUtils.isEmpty(event.locationName)){
				location.setText(event.locationName);
				location.setVisibility(View.VISIBLE);
			}else{
				location.setVisibility(View.GONE);
			}
			int total=event.goingCount+event.interestedCount;
			attendees.setText(total+" attending");
			attendees.setVisibility(total>0 ? View.VISIBLE : View.GONE);

			if("going".equals(event.rsvp)){
				rsvpButton.setText("Going");
				rsvpButton.setAlpha(0.7f);
			}else if("interested".equals(event.rsvp)){
				rsvpButton.setText("Interested");
				rsvpButton.setAlpha(0.7f);
			}else{
				rsvpButton.setText("RSVP");
				rsvpButton.setAlpha(1f);
			}

			rsvpButton.setOnClickListener(v->{
				String newStatus="going".equals(event.rsvp) ? "remove" : "going";
				new RsvpEvent(event.id, newStatus)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(Event result){
								if(getActivity()==null) return;
								int idx=events.indexOf(event);
								if(idx>=0){
									events.set(idx, result);
									adapter.notifyItemChanged(idx);
								}
							}
							@Override
							public void onError(ErrorResponse error){
								if(getActivity()!=null) error.showToast(getActivity());
							}
						})
						.exec(accountID);
			});
		}
	}

	private View createEventCardView(){
		LinearLayout card=new LinearLayout(getActivity());
		card.setOrientation(LinearLayout.VERTICAL);
		card.setPadding(V.dp(16), V.dp(12), V.dp(16), V.dp(12));
		card.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		// No card background needed - uses default

		TextView title=new TextView(getActivity());
		title.setId(android.R.id.title);
		title.setTextSize(18);
		title.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
		title.setTypeface(null, android.graphics.Typeface.BOLD);
		card.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		TextView dateTime=new TextView(getActivity());
		dateTime.setId(android.R.id.text1);
		dateTime.setTextSize(14);
		dateTime.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		LinearLayout.LayoutParams dtlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		dtlp.topMargin=V.dp(4);
		card.addView(dateTime, dtlp);

		TextView location=new TextView(getActivity());
		location.setId(android.R.id.text2);
		location.setTextSize(14);
		location.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorSecondary));
		LinearLayout.LayoutParams loclp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		loclp.topMargin=V.dp(2);
		card.addView(location, loclp);

		TextView attendees=new TextView(getActivity());
		attendees.setId(android.R.id.summary);
		attendees.setTextSize(13);
		attendees.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary));
		LinearLayout.LayoutParams atlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		atlp.topMargin=V.dp(4);
		card.addView(attendees, atlp);

		Button rsvpButton=new Button(getActivity());
		rsvpButton.setId(android.R.id.button1);
		rsvpButton.setAllCaps(false);
		rsvpButton.setTextSize(14);
		LinearLayout.LayoutParams btnlp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, V.dp(36));
		btnlp.topMargin=V.dp(8);
		card.addView(rsvpButton, btnlp);

		// Add a divider line at bottom
		View divider=new View(getActivity());
		divider.setBackgroundColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OutlineVariant));
		card.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(1)));

		return card;
	}
}
