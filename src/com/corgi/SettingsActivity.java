package com.corgi;

import java.util.List;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.Menu;
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener{
	
	Messenger serviceMessenger = null;
	boolean isBound;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	serviceMessenger = new Messenger(service);
        }

        public void onServiceDisconnected(ComponentName className) {
        	serviceMessenger = null;
        }
    };

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
		
		PreferenceScreen preferenceScreen = getPreferenceScreen();
		PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("setapp");
		
		PackageManager packageManager = getPackageManager();
		List<PackageInfo> packageInfoList = packageManager.getInstalledPackages(0);
		
		for (PackageInfo info : packageInfoList) {
			CheckBoxPreference cbPref = new  CheckBoxPreference(this);
			cbPref.setKey(info.packageName);
			cbPref.setTitle(info.applicationInfo.loadLabel(packageManager).toString());
			cbPref.setIcon(info.applicationInfo.loadIcon(packageManager));
			preferenceCategory.addPreference(cbPref);
		}
		
        Intent j = new Intent(this, LockService.class);
        j.putExtra("clearhistory", true);
        startService(j);

        Intent serviceIntent = new Intent(this, LockService.class);
	    bindService(serviceIntent, mConnection, 0);
	    isBound = true;
	}

	protected void onDestroy() {
		super.onDestroy();
		
        if (isBound) {
            unbindService(mConnection);
            isBound = false;
        }
	}
	
	@Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (isBound) {
            if (serviceMessenger != null) {
                try {
                	if (!key.equals("setpassword")) {
	                	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
	                	Boolean check = sharedPrefs.getBoolean(key, false);
	                	if (check == false) {
	                		sharedPrefs.edit().remove(key + "login").apply();
	                		sharedPrefs.edit().commit();
	                	}
	                	else {
	                		sharedPrefs.edit().putBoolean(key + "login", false).apply();
	        				sharedPrefs.edit().commit();
	                	}
                	}
                	
                    Message msg = Message.obtain(null, LockService.PREF_CHANGED, key);
                    serviceMessenger.send(msg);
                }
                catch (RemoteException e) {
                }
            }
        }
    }
}
