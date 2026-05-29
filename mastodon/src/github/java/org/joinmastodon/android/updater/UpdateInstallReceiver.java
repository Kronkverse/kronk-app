package org.joinmastodon.android.updater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

public class UpdateInstallReceiver extends BroadcastReceiver{
	@Override
	public void onReceive(Context context, Intent intent){
		GithubSelfUpdater updater=GithubSelfUpdater.getInstance();
		if(updater==null || updater.getState()!=GithubSelfUpdater.UpdateState.DOWNLOADED)
			return;
		Uri uri=new Uri.Builder()
				.scheme("content")
				.authority(context.getPackageName()+".self_update_provider")
				.path("update.apk")
				.build();
		Intent install=new Intent(Intent.ACTION_INSTALL_PACKAGE);
		install.setDataAndType(uri, "application/vnd.android.package-archive");
		install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(install);
	}
}
