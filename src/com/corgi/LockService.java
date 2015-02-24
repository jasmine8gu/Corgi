package com.corgi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.util.Log;

public class LockService extends Service {
    private static final String TAG = "corgi LockService";

    SharedPreferences sharedPrefs = null;
    ActivityManager activityManager = null;
    private ServiceThread serviceThread;
    private boolean running = false;

    public static final int PWD_CANCEL = 0;
    public static final int PWD_SUCCESS = 1;
    public static final int PREF_CHANGED = 2;
    
	String corgiPassword = "123456";
	HashMap<String, Boolean> loginMap;
	
	void clearHistory() {
		@SuppressWarnings("unchecked")
		Map<String, Boolean> checkMap = (Map<String, Boolean>) sharedPrefs.getAll();
		SharedPreferences.Editor editor = sharedPrefs.edit();
		
        for (Map.Entry<String, Boolean> entry : checkMap.entrySet()) {
            try {
	        	if (entry.getValue() == true) {
	        		editor.putBoolean(entry.getKey() + "login", false);
	        		loginMap.put(entry.getKey(), false);
	        	}
            }
            catch (Exception e) {
            	System.out.println(e.getMessage());
            }
        }
        
		editor.commit();		
	}
	
    private BroadcastReceiver receiver = new BroadcastReceiver() {
  	  
        @Override  
        public void onReceive(Context context, Intent intent) {  
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            	clearHistory();
            }
        }
    };
    
    public void onCreate() {    
        super.onCreate();  
        Notification.Builder builder = new Notification.Builder(this);  
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,    
                new Intent(this, SettingsActivity.class), 0);    
        builder.setContentIntent(contentIntent);  
        builder.setSmallIcon(R.drawable.ic_launcher);  
        //builder.setTicker("Corgi Service Start");  
        builder.setContentTitle("Corgi Service");  
        //builder.setContentText("Protect your confidential information.");  
        Notification notification = builder.build();  
          
        startForeground(1, notification);
    }
    
	@Override
	public IBinder onBind(Intent intent) {
		return new Messenger(handler).getBinder();
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Lockservice onStartCommand");
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        corgiPassword = sharedPrefs.getString("setpassword", "123456");
        loginMap = new HashMap<String, Boolean>();
        
		@SuppressWarnings("unchecked")
		Map<String, Boolean> checkMap = (Map<String, Boolean>) sharedPrefs.getAll();
		
		Boolean clearhistory = intent.getBooleanExtra("clearhistory", false);
		if (clearhistory) {
			Log.d(TAG, "Lockservice clearhistory");
			clearHistory();
		}
		else {
			Log.d(TAG, "Lockservice no clearhistory");
	        for (Map.Entry<String, Boolean> entry : checkMap.entrySet()) {
	            try {
		        	if (entry.getValue()) {
		        		Boolean login = sharedPrefs.getBoolean(entry.getKey() + "login", false);
		        		loginMap.put(entry.getKey(), login);
		        	}
	            }
	            catch (Exception e) {
	            	System.out.println(e.getMessage());
	            }
	        }
		}
		
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(receiver, filter);  
        
        activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (serviceThread != null) {
        	running = false;
        	serviceThread.interrupt();
        	serviceThread = null;
        }
        
        serviceThread = new ServiceThread();
        running = true;
        serviceThread.start();
        
        return START_STICKY;
    }
    
    public void onDestroy () {
    	Log.i(TAG, "LockService onDestroy");
    	super.onDestroy();
    	
    	stopForeground(true); 

    	unregisterReceiver(receiver);
    	
    	running = false;
    	serviceThread.interrupt();
    	serviceThread = null;
    }
    
	private class ServiceThread extends Thread {
        public void run() {
            while (running) {
    	        try {
    	        	Log.i(TAG, "service thread start");
    	        	List<RunningTaskInfo> list = activityManager.getRunningTasks(1);
    	        	if (list != null && list.size() > 0) { 
    		        	RunningTaskInfo tasksInfo = list.get(0);
    		        	
    		        	ComponentName cn = tasksInfo.baseActivity;
		        		String packageName = cn.getPackageName();
		        		
		        		Boolean login = loginMap.get(packageName);
		        		if (login != null && login == false) {
		        			//activityManager.moveTaskToFront(1, 0);
		        			Intent homeIntent= new Intent(Intent.ACTION_MAIN);
		        			homeIntent.addCategory(Intent.CATEGORY_HOME);
		        			homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		        			startActivity(homeIntent);
		        			
		        			Intent intent = new Intent(getApplicationContext(), LockActivity.class);
		        			intent.putExtra("packagename", packageName);
		        			intent.putExtra("password", corgiPassword);
		        			intent.putExtra("taskid", tasksInfo.id);
		        			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		        			startActivity(intent);
		        		}
    		        }
    	        }
    	        catch (Exception e) {
    	        }
    	        
            	try {
            		Thread.sleep(600);
    			} 
            	catch (InterruptedException e) {
    				e.printStackTrace();
    			}
            }
            
            Log.i(TAG, "Thread stopped");
        }
	}

    private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case PWD_SUCCESS: {
					String packageName = (String) msg.obj;
					synchronized(loginMap) {
						loginMap.put(packageName, true);
					}
					activityManager.moveTaskToFront(msg.arg1, 0);
            		break;
				}
				case PWD_CANCEL: {
            		break;
				}
				case PREF_CHANGED: {
					String key = (String) msg.obj;
					if (key != null && key.equals("setpassword")) {
						corgiPassword = sharedPrefs.getString("setpassword", "123456");
					}
					else {
						synchronized(loginMap) {
					        boolean check = sharedPrefs.getBoolean(key, false);
					        if (check == false) {
					        	loginMap.remove(key);
					        }
					        else {
					        	loginMap.put(key, false);
					        }
						}
					}
				}
				default: {
					break;
				}
			}
		}
	};
}
