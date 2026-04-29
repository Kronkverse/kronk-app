package org.joinmastodon.android.fragments;

import android.Manifest;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class LiveFragment extends Fragment{
	private static final String JITSI_DOMAIN="meet.talitamoss.info";
	private static final String ROOM_NAME="huddle";
	private static final int PERMISSION_REQUEST_CODE=1001;
	private static final long POLL_INTERVAL_MS=10000;

	private View rootView;
	private View lobby;
	private View jitsiContainer;
	private ViewGroup webviewContainer;
	private WebView webView;
	private Button huddleButton;
	private Button leaveButton;
	private View participantBox;
	private TextView participantCount;
	private TextView participantNames;

	private boolean inRoom;
	private Handler handler;
	private Runnable pollRunnable;

	private String getUsername(){
		String accountID=getArguments()!=null ? getArguments().getString("account") : null;
		if(accountID!=null){
			try{
				return "@"+AccountSessionManager.getInstance().getAccount(accountID).self.username;
			}catch(Exception ignored){}
		}
		return "Kronker";
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		rootView=inflater.inflate(R.layout.fragment_huddle, container, false);
		lobby=rootView.findViewById(R.id.lobby);
		jitsiContainer=rootView.findViewById(R.id.jitsi_container);
		webviewContainer=rootView.findViewById(R.id.webview_container);
		huddleButton=rootView.findViewById(R.id.huddle_button);
		leaveButton=rootView.findViewById(R.id.leave_button);
		participantBox=rootView.findViewById(R.id.participant_box);
		participantCount=rootView.findViewById(R.id.participant_count);
		participantNames=rootView.findViewById(R.id.participant_names);

		huddleButton.setOnClickListener(v->checkPermissionsAndJoin());
		leaveButton.setOnClickListener(v->leaveRoom());

		handler=new Handler(Looper.getMainLooper());
		pollRunnable=()->{ fetchParticipants(); handler.postDelayed(pollRunnable, POLL_INTERVAL_MS); };

		return rootView;
	}

	@Override
	public void onHiddenChanged(boolean hidden){
		super.onHiddenChanged(hidden);
		if(!hidden){
			startPolling();
		}else{
			stopPolling();
		}
	}

	public void loadData(){
		startPolling();
	}

	private void startPolling(){
		if(!inRoom){
			handler.removeCallbacks(pollRunnable);
			fetchParticipants();
			handler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
		}
	}

	private void stopPolling(){
		handler.removeCallbacks(pollRunnable);
	}

	private void fetchParticipants(){
		Executors.newSingleThreadExecutor().execute(()->{
			try{
				URL url=new URL("https://"+JITSI_DOMAIN+"/room?room="+ROOM_NAME+"&domain=meet.jitsi");
				HttpURLConnection conn=(HttpURLConnection)url.openConnection();
				conn.setRequestMethod("GET");
				conn.setConnectTimeout(5000);
				conn.setReadTimeout(5000);

				if(conn.getResponseCode()!=200){
					updateParticipantUI(new ArrayList<>());
					return;
				}

				BufferedReader reader=new BufferedReader(new InputStreamReader(conn.getInputStream()));
				StringBuilder sb=new StringBuilder();
				String line;
				while((line=reader.readLine())!=null) sb.append(line);
				reader.close();

				JSONArray arr=new JSONArray(sb.toString());
				List<String> names=new ArrayList<>();
				for(int i=0; i<arr.length(); i++){
					JSONObject p=arr.getJSONObject(i);
					String name=p.optString("display_name", "");
					if(!TextUtils.isEmpty(name)) names.add(name);
				}
				updateParticipantUI(names);
			}catch(Exception e){
				updateParticipantUI(new ArrayList<>());
			}
		});
	}

	private void updateParticipantUI(List<String> names){
		if(getActivity()==null) return;
		handler.post(()->{
			if(participantBox==null) return;
			if(names.isEmpty()){
				participantBox.setVisibility(View.GONE);
			}else{
				participantBox.setVisibility(View.VISIBLE);
				int count=names.size();
				participantCount.setText(count==1
					? getString(R.string.huddle_people_in_room, count)
					: getString(R.string.huddle_people_in_room_plural, count));
				participantNames.setText(TextUtils.join("  ·  ", names));
			}
		});
	}

	private void checkPermissionsAndJoin(){
		if(getActivity()==null) return;
		boolean hasMic=getActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;
		boolean hasCam=getActivity().checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED;
		if(hasMic && hasCam){
			joinRoom();
		}else{
			ArrayList<String> needed=new ArrayList<>();
			if(!hasMic) needed.add(Manifest.permission.RECORD_AUDIO);
			if(!hasCam) needed.add(Manifest.permission.CAMERA);
			requestPermissions(needed.toArray(new String[0]), PERMISSION_REQUEST_CODE);
		}
	}

	private void joinRoom(){
		inRoom=true;
		stopPolling();
		lobby.setVisibility(View.GONE);
		jitsiContainer.setVisibility(View.VISIBLE);

		webView=new WebView(getActivity());
		WebSettings settings=webView.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setDomStorageEnabled(true);
		settings.setMediaPlaybackRequiresUserGesture(false);
		// No UA override — WebView's native UA signals mobile to Jitsi, which selects VP8 codec first

		CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

		webView.setWebViewClient(new WebViewClient(){
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request){
				String scheme=request.getUrl().getScheme();
				return "intent".equals(scheme) || "market".equals(scheme);
			}

			@Override
			public void doUpdateVisitedHistory(WebView view, String url, boolean isReload){
				// Jitsi navigates away from the room URL when the conference ends
				if(inRoom && !url.contains("/"+ROOM_NAME)){
					handler.post(()->leaveRoom());
				}
			}
		});

		webView.setWebChromeClient(new WebChromeClient(){
			@Override
			public void onPermissionRequest(PermissionRequest request){
				// Android permissions verified in checkPermissionsAndJoin; grant immediately.
				if(getActivity()!=null) request.grant(request.getResources());
				else request.deny();
			}
		});

		webviewContainer.addView(webView, new ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		String username=getUsername();
		String encodedUsername;
		try{
			encodedUsername=URLEncoder.encode(username, "UTF-8").replace("+", "%20");
		}catch(UnsupportedEncodingException e){
			encodedUsername="Kronker";
		}

		String url="https://"+JITSI_DOMAIN+"/"+ROOM_NAME
			+"#config.prejoinPageEnabled=false"
			+"&config.prejoinConfig.enabled=false"
			+"&config.disableDeepLinking=true"
			+"&config.startWithAudioMuted=true"
			+"&config.startWithVideoMuted=false"
			+"&config.hideConferenceTimer=true"
			+"&config.disableInviteFunctions=true"
			+"&config.enableClosePage=false"
			+"&userInfo.displayName="+encodedUsername;

		webView.loadUrl(url);
	}

	private void leaveRoom(){
		inRoom=false;
		if(webView!=null){
			webView.loadUrl("about:blank");
			webviewContainer.removeView(webView);
			webView.destroy();
			webView=null;
		}
		jitsiContainer.setVisibility(View.GONE);
		lobby.setVisibility(View.VISIBLE);
		startPolling();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
		if(requestCode!=PERMISSION_REQUEST_CODE) return;
		if(getActivity()==null) return;
		boolean hasMic=getActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;
		if(hasMic){
			joinRoom();
		}else{
			android.widget.Toast.makeText(getActivity(), R.string.huddle_mic_required, android.widget.Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onDestroyView(){
		stopPolling();
		if(webView!=null){
			webView.destroy();
			webView=null;
		}
		rootView=null;
		lobby=null;
		jitsiContainer=null;
		webviewContainer=null;
		huddleButton=null;
		leaveButton=null;
		participantBox=null;
		participantCount=null;
		participantNames=null;
		super.onDestroyView();
	}
}
