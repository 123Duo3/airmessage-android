package me.tagavari.airmessage.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import me.tagavari.airmessage.helper.ConnectionServiceLaunchHelper;
import me.tagavari.airmessage.service.ConnectionService;

public class StartBootReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		//Returning if the service is not a boot service
		if(!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
		
		//Starting the service
		ConnectionServiceLaunchHelper.launchPersistent(context);
	}
}