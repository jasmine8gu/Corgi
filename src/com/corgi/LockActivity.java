package com.corgi;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class LockActivity extends Activity {
	
	Messenger serviceMessenger = null;
	boolean isBound;

	String packageName = null;
	String password = null;
	int taskId = -1;
	
	EditText etPassword = null;
	TextView tvInfo = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lock);
		
		etPassword = (EditText)findViewById(R.id.etPassword);
		tvInfo = (TextView)findViewById(R.id.tvInfo);
		
		Intent intent = getIntent();
		packageName = intent.getStringExtra("packagename");
		password = intent.getStringExtra("password");
		taskId = intent.getIntExtra("taskid", 1);
		
		Intent serviceIntent = new Intent(this, LockService.class);
	    bindService(serviceIntent, mConnection, 0);
	    isBound = true;
	}

	public void onResume() {
		super.onResume();
		etPassword.setText(null);
		tvInfo.setVisibility(View.GONE);
	}
	
	protected void onStop() {
		super.onStop();
		
        if (isBound) {
            unbindService(mConnection);
            isBound = false;
        }
        finish();
	}
	
	public void onDestroy() {
		super.onDestroy();
	}
	
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	serviceMessenger = new Messenger(service);
        }

        public void onServiceDisconnected(ComponentName className) {
        	serviceMessenger = null;
        }
    };

    private void sendMessageToService(int val) {
        if (isBound) {
            if (serviceMessenger != null) {
                try {
                    Message msg = Message.obtain(null, val, packageName);
                    msg.arg1 = taskId;
                    serviceMessenger.send(msg);
                }
                catch (RemoteException e) {
                }
            }
        }
    }

    public void onOK(View v) {
    	String pwd = etPassword.getText().toString();
    	
    	if (pwd != null) {
    		if (pwd.equals(password)) {
    			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
				sharedPrefs.edit().putBoolean(packageName + "login", true).apply();
				sharedPrefs.edit().commit();
				
    			sendMessageToService(LockService.PWD_SUCCESS);
    			finish();
    		}
    		else {
    			tvInfo.setVisibility(View.VISIBLE);
    		}
    	}
    }
    
    public void onCancel(View v) {
    	sendMessageToService(LockService.PWD_CANCEL);
    	finish();
    }	
}
