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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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

		// Request Android permissions before creating the WebView so the result
		// routes to this fragment (HomeFragment now forwards permission results to children).
		huddleButton.setOnClickListener(v->checkPermissionsAndJoin());
		leaveButton.setOnClickListener(v->leaveRoom());

		handler=new Handler(Looper.getMainLooper());
		pollRunnable=()->{ fetchParticipants(); handler.postDelayed(pollRunnable, POLL_INTERVAL_MS); };

		return rootView;
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

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
		if(requestCode!=PERMISSION_REQUEST_CODE) return;
		if(getActivity()==null) return;
		boolean hasMic=getActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;
		if(hasMic){
			joinRoom();
		}else{
			android.widget.Toast.makeText(getActivity(), "Microphone permission is required for Huddle", android.widget.Toast.LENGTH_LONG).show();
		}
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
		// Strip "Mobile" from UA so Jitsi treats this as a browser, not the native app
		String defaultUA=settings.getUserAgentString();
		settings.setUserAgentString(defaultUA.replaceAll("\\bMobile\\b", "").replaceAll("\\bAndroid[^;)]*", ""));

		CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

		webView.setWebViewClient(new WebViewClient(){
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request){
				String scheme=request.getUrl().getScheme();
				if("intent".equals(scheme) || "market".equals(scheme)) return true;
				return false;
			}
		});

		webView.setWebChromeClient(new WebChromeClient(){
			@Override
			public void onPermissionRequest(PermissionRequest request){
				// Android permissions were pre-verified in checkPermissionsAndJoin;
				// grant the WebView request immediately without any async dance.
				android.util.Log.i("KronkHuddle", "onPermissionRequest origin="+request.getOrigin()+" resources="+java.util.Arrays.toString(request.getResources()));
				if(getActivity()!=null){
					request.grant(request.getResources());
				}else{
					request.deny();
				}
			}

			@Override
			public boolean onConsoleMessage(android.webkit.ConsoleMessage cm){
				android.util.Log.i("KronkHuddle/JS", cm.messageLevel()+" "+cm.sourceId()+":"+cm.lineNumber()+" "+cm.message());
				String low=cm.message().toLowerCase();
				if(cm.messageLevel()==android.webkit.ConsoleMessage.MessageLevel.ERROR
						|| low.contains("microphone") || low.contains("camera")
						|| low.contains("getusermedia") || low.contains("notallowed")
						|| low.contains("notreadable") || low.contains("permission")
						|| low.contains("gum")){
					String msg=cm.message();
					if(msg.length()>250) msg=msg.substring(0,250);
					android.util.Log.e("KronkHuddle/JS", "MEDIA ERROR: "+msg);
				}
				return true;
			}
		});

		webviewContainer.addView(webView, new ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		String username=getUsername();
		String escapedUsername=username.replace("\\", "\\\\").replace("'", "\\'");

		String html="<!DOCTYPE html><html><head>"
			+"<meta name='viewport' content='width=device-width,initial-scale=1'>"
			+"<style>html,body,#meet{margin:0;padding:0;width:100%;height:100%;overflow:hidden;}</style>"
			+"<script>"
			+"(function(){var orig=document.createElement;document.createElement=function(tag){"
			+"var el=orig.apply(document,arguments);"
			+"if(tag&&tag.toLowerCase()==='iframe'){"
			+"el.allow='camera; microphone; display-capture; autoplay; clipboard-write';"
			+"el.setAttribute('allow','camera; microphone; display-capture; autoplay; clipboard-write');}"
			+"return el;};})();"
			+"</script>"
			+"<script src='https://"+JITSI_DOMAIN+"/external_api.js'></script>"
			+"</head><body><div id='meet'></div><script>"
			+"var api=new JitsiMeetExternalAPI('"+JITSI_DOMAIN+"',{"
			+"roomName:'"+ROOM_NAME+"',"
			+"parentNode:document.getElementById('meet'),"
			+"width:'100%',height:'100%',"
			+"userInfo:{displayName:'"+escapedUsername+"'},"
			+"configOverwrite:{"
			+"prejoinPageEnabled:false,"
			+"prejoinConfig:{enabled:false},"
			+"disableDeepLinking:true,"
			+"startWithAudioMuted:false,"
			+"startWithVideoMuted:true,"
			+"subject:'The Huddle',"
			+"hideConferenceTimer:true,"
			+"disableInviteFunctions:true,"
			+"enableClosePage:false"
			+"},"
			+"interfaceConfigOverwrite:{"
			+"SHOW_JITSI_WATERMARK:false,"
			+"SHOW_BRAND_WATERMARK:false,"
			+"SHOW_POWERED_BY:false,"
			+"HIDE_INVITE_MORE_HEADER:true,"
			+"DEFAULT_REMOTE_DISPLAY_NAME:'Kronker'"
			+"}"
			+"});"
			+"var isGuest=false;"
			+"api.addListener('passwordRequired',function(){"
			+"isGuest=true;"
			+"api.executeCommand('password','kronkfam2026');"
			+"});"
			+"api.addListener('videoConferenceJoined',function(){"
			+"api.executeCommand('displayName','"+escapedUsername+"');"
			+"if(!isGuest){api.executeCommand('password','kronkfam2026');}"
			+"});"
			+"api.addListener('readyToClose',function(){"
			+"Android.leave();"
			+"});"
			+"</script></body></html>";

		webView.addJavascriptInterface(new Object(){
			@android.webkit.JavascriptInterface
			public void leave(){
				handler.post(()->leaveRoom());
			}
		}, "Android");

		webView.loadDataWithBaseURL("https://"+JITSI_DOMAIN+"/", html, "text/html", "UTF-8", null);
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
