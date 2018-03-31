package com.speind.evaqalarm;

import java.util.ArrayList;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import me.taifuno.*;

public class GcmIntentService extends IntentService {
	public static String BROADCAST_ACTION = "com.speind.evaqalarm.servicebackbroadcast";
	
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        Log.e("[---!!!---]", "intent");
        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            messageType=(messageType==null ? "" : messageType);
            switch (messageType) {
                case GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR:
                    Log.e("[---!!!---]", "Send error: " + extras.toString());
                    break;
                case GoogleCloudMessaging.MESSAGE_TYPE_DELETED:
                    Log.e("[---!!!---]", "Deleted messages on server: " + extras.toString());
                    break;
                case GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE:
                    String regId = getRegistrationId(this);
                    if (!regId.equals("")) {
                        boolean isCommand = false;

                        if (extras.containsKey("sender_id")) {
                            Taifuno.getInstance().recievedNotification(extras, this, R.drawable.ic_launcher);
                        } else {
                            if (extras.getString("message") != null) {
                                try {
                                    byte[] decodedAr = Base64.decode(extras.getString("message"));
                                    byte[] regIdAr = regId.getBytes();
                                    Log.e("[---!!!---]", "[ " + regId.length() + " " + regIdAr.length);
                                    String message = new String(xorDecode(decodedAr, regIdAr));
                                    if (message.contains("Critical update: ")) {
                                        message = message.replaceAll(".Critical update: ([0-9]+).*", "$1");
                                        int update = Integer.valueOf(message);
                                        if (getAppVersion(this) < update) {
                                            Log.e("[---!!!---]", "! " + message);

                                            SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
                                            SharedPreferences.Editor editor = settings.edit();
                                            editor.putInt(MainActivity.PARAM_UPDATE, update);
                                            editor.apply();

                                            Intent broadcast = new Intent(BROADCAST_ACTION);
                                            broadcast.putExtra(MainActivity.PARAM_UPDATE, update);
                                            sendBroadcast(broadcast);
                                        }
                                        isCommand = true;
                                    }

                                } catch (Base64DecoderException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (!isCommand) {
                                //sendNotification(extras.getString("message"));
                                SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
                                boolean isAlarmState = settings.getBoolean(MainActivity.PARKING_ALARM, false);
                                boolean isParkingState = settings.getBoolean(MainActivity.PARKING_NAME, false);
                                if (isParkingState) {
                                    sendNotification(getString(R.string.alert_message));

                                    ArrayList<String> sources = new ArrayList<>();

                                    if (isAlarmState) {
                                        int savedCount = settings.getInt(MainActivity.PARKING_ALARM_SOURCES_COUNT, 0);
                                        for (int i = 0; i < savedCount; i++) {
                                            String resRegId = settings.getString(MainActivity.PARKING_ALARM_SOURCES + "_" + i, "");
                                            if (!regId.equals(""))
                                                sources.add(resRegId);
                                        }
                                    }

                                    String source = extras.getString("message");
                                    sources.add(source);

                                    SharedPreferences.Editor editor = settings.edit();
                                    editor.putBoolean(MainActivity.PARKING_ALARM, true);

                                    editor.putInt(MainActivity.PARKING_ALARM_SOURCES_COUNT, sources.size());
                                    for (int i = 0; i < sources.size(); i++) {
                                        editor.putString(MainActivity.PARKING_ALARM_SOURCES + "_" + i, sources.get(i));
                                    }
                                    editor.apply();

                                    if (!isAlarmState) {
                                        Intent broadcast = new Intent(BROADCAST_ACTION);
                                        sendBroadcast(broadcast);
                                    }
                                }
                            }
                        }
                    }
                    break;
                default:
                    Log.e("[---!!!---]", "intent 2");
                    mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.cancel(NOTIFICATION_ID);
                    break;
            }
        } else {
            Log.e("[---!!!---]", "intent 1");
            mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        	mNotificationManager.cancel(NOTIFICATION_ID);
        }
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void sendNotification(String msg) {
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        Uri soundUri = Uri.parse("android.resource://com.speind.evaqalarm/raw/alarm");

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        long[] pattern={0, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200, 200};
        
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle(getString(R.string.alert_title))
        .setStyle(new NotificationCompat.BigTextStyle()
        .bigText(msg))
        .setAutoCancel(true)
        .setLights(0xffff0000, 500, 500)
        .setSound(soundUri)
        .setVibrate(pattern)
        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
    
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGcmPreferences();
        String registrationId = prefs.getString(MainActivity.PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            return "";
        }
        int registeredVersion = prefs.getInt(MainActivity.PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            return "";
        }
        return registrationId;
    }
    
    private SharedPreferences getGcmPreferences() {
        return getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }
    
    public byte[] xorDecode(byte[] data, byte[] xordata) {
    	byte pv=0x00;
    	for (int i=0;i<data.length;i++) {
    		byte tpv=data[i];
    		data[i]=(byte) (data[i] ^ (xordata[i%(xordata.length)] + pv));
    		pv=tpv;
    	}
    	return data;
    }

 /*
    public byte[] xorEncode(byte[] data, byte[] xordata) {
    	byte pv=0x00;
    	for (int i=0;i<data.length;i++) {
    		data[i]=(byte) (data[i] ^ (xordata[i%(xordata.length)]+pv));
    		pv=data[i];
    	}
    	return data;
    }
*/

}