package com.corgi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LockReceiver extends BroadcastReceiver{
    private static final String TAG = "corgi LockReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
    	Log.i(TAG, "LockReciever onReceive");

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
        	Log.i(TAG, "ACTION_BOOT_COMPLETED and start service");
            Intent j = new Intent(context, LockService.class);
            j.putExtra("clearhistory", true);
            context.startService(j);
        }        
        else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
        
        }
        else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
        	Log.i(TAG, "ACTION_SCREEN_ON and start service");
            Intent j = new Intent(context, LockService.class);
            j.putExtra("clearhistory", true);
            context.startService(j);
        }
	}
}
