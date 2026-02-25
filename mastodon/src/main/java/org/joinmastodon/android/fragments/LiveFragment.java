package org.joinmastodon.android.fragments;

import android.Manifest;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

import androidx.annotation.Nullable;

public class LiveFragment extends Fragment{
	private static final String LIVE_URL="https://mastodon.kronk.info/live";
	private static final int PERMISSION_REQUEST_CODE=1001;

	private WebView webView;
	private boolean loaded;
	private PermissionRequest pendingPermissionRequest;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		webView=new WebView(getActivity());
		WebSettings settings=webView.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setDomStorageEnabled(true);
		settings.setMediaPlaybackRequiresUserGesture(false);
		String defaultUA=settings.getUserAgentString();
		settings.setUserAgentString(defaultUA.replaceAll("\\bMobile\\b", "").replaceAll("\\bAndroid[^;)]*", ""));

		CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

		webView.setWebViewClient(new WebViewClient(){
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request){
				String scheme=request.getUrl().getScheme();
				if("intent".equals(scheme) || "market".equals(scheme)){
					return true;
				}
				return false;
			}

			@Override
			public void onPageFinished(WebView view, String url){
				view.evaluateJavascript(
					"(function(){var s=document.createElement('style');s.textContent='.sign-in-banner{display:none!important}';document.head.appendChild(s);})()",
					null);
			}
		});
		webView.setWebChromeClient(new WebChromeClient(){
			@Override
			public void onPermissionRequest(PermissionRequest request){
				if(getActivity()==null)
					return;
				boolean needsCamera=false;
				boolean needsMic=false;
				for(String res : request.getResources()){
					if(PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(res))
						needsCamera=true;
					if(PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(res))
						needsMic=true;
				}

				boolean hasCameraPerm=getActivity().checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED;
				boolean hasMicPerm=getActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;

				if((!needsCamera || hasCameraPerm) && (!needsMic || hasMicPerm)){
					request.grant(request.getResources());
				}else{
					pendingPermissionRequest=request;
					java.util.ArrayList<String> perms=new java.util.ArrayList<>();
					if(needsCamera && !hasCameraPerm)
						perms.add(Manifest.permission.CAMERA);
					if(needsMic && !hasMicPerm)
						perms.add(Manifest.permission.RECORD_AUDIO);
					requestPermissions(perms.toArray(new String[0]), PERMISSION_REQUEST_CODE);
				}
			}
		});

		return webView;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
		if(requestCode==PERMISSION_REQUEST_CODE && pendingPermissionRequest!=null){
			boolean allGranted=true;
			for(int result : grantResults){
				if(result!=PackageManager.PERMISSION_GRANTED){
					allGranted=false;
					break;
				}
			}
			if(allGranted){
				pendingPermissionRequest.grant(pendingPermissionRequest.getResources());
			}else{
				pendingPermissionRequest.deny();
			}
			pendingPermissionRequest=null;
		}
	}

	@Override
	public void onHiddenChanged(boolean hidden){
		super.onHiddenChanged(hidden);
		if(!hidden && !loaded){
			loadPage();
		}
	}

	public void loadData(){
		if(!loaded){
			loadPage();
		}
	}

	private void loadPage(){
		if(webView!=null){
			webView.loadUrl(LIVE_URL);
			loaded=true;
		}
	}

	@Override
	public void onDestroyView(){
		if(webView!=null){
			webView.destroy();
			webView=null;
		}
		super.onDestroyView();
	}
}
