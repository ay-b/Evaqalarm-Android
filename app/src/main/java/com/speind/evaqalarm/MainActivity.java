package com.speind.evaqalarm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Debug;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.speind.evaqalarm.EvaqalarmProgressBar.EvaqalarmProgressBarEventListener;
import com.yandex.metrica.YandexMetrica;

import me.taifuno.*;

public class MainActivity extends ActionBarActivity implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener, EvaqalarmProgressBarEventListener, OnMapReadyCallback {
	public static final String PREFS_NAME = "evaqalarmConfig";
	public static final String PARAM_UPDATE = "actualversion";
	public static final String PARKING_NAME = "evaqalarmParking";
	public static final String PARKING_COUNT = "evaqalarmParkingCount";
	public static final String PARKING_ALARM = "evaqalarmAlarm";
	public static final String PARKING_ALARM_SOURCES = "evaqalarmAlarmSources";
	public static final String PARKING_ALARM_SOURCES_COUNT = "evaqalarmAlarmSourcesCount";
	
	//private static final int PICK_IMAGE = 1;
	//public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    public static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    
	private BroadcastReceiver serviceInfoReceiver;
    
    String SENDER_ID = "346300744013";
    //String SENDER_ID = "538198777563";
    
    Context context;
    GoogleCloudMessaging gcm;
    String regid="";
    
    private static final int MILLISECONDS_PER_SECOND = 1000;
    private static final int UPDATE_INTERVAL_IN_SECONDS = 5;
    private static final long UPDATE_INTERVAL =  MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
    private static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
    
    private GoogleMap mMap=null;
    private GoogleApiClient mGoogleApiClient=null;
	private LocationRequest locationRequest=null; 
	private Location detectedLocation=null;
	
	private Marker parkingMarker=null;
	
	private boolean waitUserCorrect=false;
	
	boolean isGoogleservicesAvailable=false;
		
	private EvaqalarmProgressBar main_button=null;
	
	private int parkingcount=0;
	private boolean isNeedHideDialog=false;
	
	Handler handler=null;
	Runnable animateSplashRunnable=new Runnable(){
		@Override
		public void run() {
			animateSplash();
		}
	};
	Runnable animateHideSplashRunnable=new Runnable(){
		@Override
		public void run() {
			animateHideSplash();
		}
	};
	
	Runnable clearWaitUserCorrectRunnable=new Runnable(){
		@Override
		public void run() {
			waitUserCorrect=false;
			if (detectedLocation!=null) {
				setCamera(new LatLng(detectedLocation.getLatitude(), detectedLocation.getLongitude()));
			}
		}
	};
	
//	Runnable animateMainRunnable=new Runnable(){
//		@Override
//		public void run() {
//			animateMain();
//		}
//	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//ActionBar actionBar = getSupportActionBar();
    	//actionBar.hide();
		setContentView(R.layout.activity_splash);
		
		Taifuno tf = Taifuno.getInstance();
        tf.setContext(getApplicationContext());
        tf.setApikey("a3e50355255741c0ade1643fbecf4596");

		handler=new Handler();

		EvaqAlarmSupportMapFragment mapFragment = (EvaqAlarmSupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);
		

		mapFragment.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				if (event.getAction()==MotionEvent.ACTION_DOWN) {
					handler.removeCallbacks(clearWaitUserCorrectRunnable);
					waitUserCorrect=true;
					handler.postDelayed(clearWaitUserCorrectRunnable, 12000);
				}
						
				if (main_button!=null) {
					int [] location={0,0};
					main_button.getLocationInWindow(location);
					if (event.getAction()!=MotionEvent.ACTION_DOWN||event.getRawY()>location[1]&&event.getY()<(location[1]+main_button.getHeight()))
						main_button.processOnTouch(event);
				}
				return false;
			}
		});

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		parkingcount=settings.getInt(PARKING_COUNT, 0);
		
		Typeface fontFamily = Typeface.createFromAsset(getAssets(), "fonts/fontawesome.ttf");
		TextView thumbs_down = (TextView) findViewById(R.id.b_thumbs_down);
		TextView thumbs_up = (TextView) findViewById(R.id.b_thumbs_up);
		thumbs_down.setTypeface(fontFamily);
		thumbs_up.setTypeface(fontFamily);
		thumbs_down.setText("\uf165");
		thumbs_up.setText("\uf164");
		
		context = getApplicationContext();
		
		clearNotification();
		
		serviceInfoReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int update=intent.getIntExtra(PARAM_UPDATE, 0);
				if (update==0) {
					setCamera(new LatLng(0,0));
					animateMain();
					final LinearLayout main_wrap=(LinearLayout) findViewById(R.id.main_wrap);		
					main_wrap.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View v) {
							clearNotification();
							main_wrap.setOnClickListener(null);
							main_wrap.setClickable(false);
						}
					});
				} else {
					showUpdateDialog();
				}
			}
		};
		IntentFilter ifilter = new IntentFilter(GcmIntentService.BROADCAST_ACTION);
		registerReceiver(serviceInfoReceiver, ifilter);

	}
	
	private void clearNotification() {
		ComponentName comp = new ComponentName(context.getPackageName(), GcmIntentService.class.getName());
		Intent intent=new Intent();
		intent.setComponent(comp);
		WakefulBroadcastReceiver.startWakefulService(context, intent);
	}
	@Override
    protected void onDestroy() {
    	unregisterReceiver(serviceInfoReceiver);		    	
        super.onDestroy();
    }    
	
	@Override
	protected void onResume() {
	    super.onResume();
	    YandexMetrica.onResumeActivity(this);
	    if (!isGoogleservicesAvailable) {
	    	if (checkPlayServices()) {
				handler.postDelayed(animateSplashRunnable, 1000);

				gcm = GoogleCloudMessaging.getInstance(this);
	            regid = getRegistrationId(context);
	            Log.e("[---!!!---]", ""+regid);
	            
	            if (regid.isEmpty()) {
	                registerInBackground();
	            } else {
                    Taifuno.getInstance().regDeviceId(regid);
                }
				
	            mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();	            
				locationRequest = LocationRequest.create();
				locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
				locationRequest.setInterval(UPDATE_INTERVAL);
				locationRequest.setFastestInterval(FASTEST_INTERVAL);

				mGoogleApiClient.connect();
	        } else {
	            Log.i("[---!!!---]", "No valid Google Play Services APK found.");
	        }
	    } else {
			LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
		}
	    
	}
	
	@Override
	protected void onPause() {
		YandexMetrica.onPauseActivity(this);
		if (isGoogleservicesAvailable) {
			if (mGoogleApiClient.isConnected()) {
				LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
	        }
		}
		if (main_button!=null) main_button.stop();
		super.onPause();
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private Intent getDefaultIntent() {
	    Intent intent = new Intent(Intent.ACTION_SEND);	    
	    intent.setType("text/plain");
	    intent.addFlags(0x00080000/*Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET*/);
	    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
	    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_url));
	    intent.putExtra(Intent.EXTRA_HTML_TEXT, getString(R.string.share_subject)+"<br>"+getString(R.string.share_url));
	    
	    return intent;
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
       if ( keyCode == KeyEvent.KEYCODE_MENU ) {
           return false;
       } if ( keyCode == KeyEvent.KEYCODE_BACK ) {
    	   if (isNeedHideDialog) {
    		   hide_dialog();
    		   return false;
    	   }
       }
       return super.onKeyDown(keyCode, event);
	}
       
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.main, menu);
		//MenuItem shareItem = menu.findItem(R.id.action_share);
	    //mShareActionProvider = (ShareActionProvider)  MenuItemCompat.getActionProvider(shareItem);
	    //mShareActionProvider.setShareIntent(getDefaultIntent());
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//int id = item.getItemId();
		//if (id == R.id.action_settings) {
		//	return true;
		//}
		return true;//super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onConnected(Bundle arg0) {
		//Toast.makeText(this, "Connected", Toast.LENGTH_LONG).show();
		LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		//Toast.makeText(this, "Connection Failed", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onLocationChanged(Location location) {
		//Toast.makeText(this, "LocationChanged", Toast.LENGTH_LONG).show();
		if (isBestLocation(location, detectedLocation)) {
			detectedLocation=location;
		}
		hideRedLine();
		RelativeLayout map_wrap=(RelativeLayout) findViewById(R.id.map_wrap);
		if (map_wrap!=null) {
			if (main_button!=null&&map_wrap.getVisibility()!=View.VISIBLE) map_wrap.setVisibility(View.VISIBLE);
			setCamera(new LatLng(detectedLocation.getLatitude(), detectedLocation.getLongitude()));
		}
	}
	
	protected boolean isBestLocation(Location location, Location currentBestLocation) {
	    if (currentBestLocation == null) {
	        return true;
	    }
	    if (location == null) return false;
	    // Check whether the new location fix is newer or older
	    long timeDelta = location.getTime() - currentBestLocation.getTime();
	    //boolean isSignificantlyNewer = timeDelta > 2*60*1000;
	    //boolean isSignificantlyOlder = timeDelta < -2*60*1000;
	    boolean isNewer = timeDelta > 0;//1*60*1000;
	    //if (isSignificantlyNewer) {
	    //    return true;
	    //} else if (isSignificantlyOlder) {
	    //    return false;
	    //}
	    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;
	    boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());
	    if (isMoreAccurate) {
	        return true;
	    } else if (isNewer && !isLessAccurate) {
	        return true;
	    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	        return true;
	    }
	    return false;
	}
	
	private boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	        return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}
/*
	private String formatLocation(Location location) {
		if (location == null) return "";
		//return String.format(location.getProvider()+" Coordinates: lat = %1$.6f, lon = %2$.6f, time = %3$tF %3$tT", location.getLatitude(), location.getLongitude(), new Date(location.getTime()));
		return String.format("Coordinates: lat = %1$.6f, lon = %2$.6f", location.getLatitude(), location.getLongitude());
	}
*/
	private void animateSplash() {
		Log.d("[---!!!---]", "animateSplash");
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		final boolean isParkingState=settings.getBoolean(PARKING_NAME, false);

		final TextView disclaimer=(TextView) this.findViewById(R.id.disclaimer);
		final TextView appname=(TextView) this.findViewById(R.id.appname);
		final LinearLayout main_wrap=(LinearLayout) this.findViewById(R.id.main_wrap);
		
		if (main_wrap!=null) {
			main_wrap.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					YandexMetrica.reportEvent("Skip splash clicked");
					if (appname!=null) {
						appname.clearAnimation();
						appname.setVisibility(View.INVISIBLE);
					}
					if (disclaimer!=null) {
						disclaimer.clearAnimation();
						disclaimer.setVisibility(View.INVISIBLE);
					}
					handler.removeCallbacks(animateHideSplashRunnable);
					handler.post(animateHideSplashRunnable);
					//animateMain();
				}});
		}
		if (appname!=null) {
			Animation animation = AnimationUtils.loadAnimation(this, R.anim.appname);
			appname.setVisibility(View.VISIBLE);
			appname.startAnimation(animation);			
		}
		if (disclaimer!=null) {
			Log.d("[---!!!---]", "animateSplash prepare");
			disclaimer.setText(Html.fromHtml(getString(R.string.disclaimer)));
			Animation animation = AnimationUtils.loadAnimation(this, R.anim.disclaimer);
			animation.setAnimationListener(new AnimationListener(){
				@Override
				public void onAnimationStart(Animation animation) {}
				@Override
				public void onAnimationEnd(Animation animation) {
					if (isParkingState) handler.postDelayed(animateHideSplashRunnable, 100);
					else handler.postDelayed(animateHideSplashRunnable, 10000);					
					Log.d("[---!!!---]", "animateSplash end");
				}
				@Override
				public void onAnimationRepeat(Animation animation) {}
			});
			disclaimer.setVisibility(View.VISIBLE);
			disclaimer.startAnimation(animation);			
		}
		
	}
	
	private void animateHideSplash() {
		handler.removeCallbacks(animateHideSplashRunnable);
		final TextView disclaimer=(TextView) this.findViewById(R.id.disclaimer);
		final TextView appname=(TextView) this.findViewById(R.id.appname);
		if (appname!=null) {
			if (appname.getVisibility()!=View.INVISIBLE) {
				Animation animation = AnimationUtils.loadAnimation(this, R.anim.appname_hide);
				animation.setAnimationListener(new AnimationListener(){
					@Override
					public void onAnimationStart(Animation animation) {}
					@Override
					public void onAnimationEnd(Animation animation) {
						appname.setVisibility(View.INVISIBLE);
					}
					@Override
					public void onAnimationRepeat(Animation animation) {}
				});
				appname.startAnimation(animation);
			}
		}
		if (disclaimer!=null) {
			if (disclaimer.getVisibility()!=View.INVISIBLE) {
				Animation animation = AnimationUtils.loadAnimation(this, R.anim.disclaimer_hide);
				animation.setAnimationListener(new AnimationListener(){
					@Override
					public void onAnimationStart(Animation animation) {}
					@Override
					public void onAnimationEnd(Animation animation) {
						disclaimer.setVisibility(View.INVISIBLE);	
						animateMain();
					}
					@Override
					public void onAnimationRepeat(Animation animation) {}
				});
				disclaimer.startAnimation(animation);
			} else {
				animateMain();
			}
		}	
	}

	private void animateMain() {
		//ActionBar actionBar = getSupportActionBar(); 
    	//actionBar.show();
		
		final LinearLayout main_wrap=(LinearLayout) this.findViewById(R.id.main_wrap);		
		main_wrap.setOnClickListener(null);
		main_wrap.setClickable(false);
		
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		final boolean isParkingState=settings.getBoolean(PARKING_NAME, false);
		final boolean isAlarmState=settings.getBoolean(PARKING_ALARM, false);
		

		
		final TextView disclaimer=(TextView) this.findViewById(R.id.disclaimer);
		final ImageView logo=(ImageView) this.findViewById(R.id.logo);
		final RelativeLayout main_button_wrap=(RelativeLayout) this.findViewById(R.id.button_wrap);
		final ImageView share_button=(ImageView) this.findViewById(R.id.share_button);
        final ImageView support_button=(ImageView) this.findViewById(R.id.support_button);
		final LinearLayout review_buttons_wrap=(LinearLayout) this.findViewById(R.id.review_buttons_wrap);
		final TextView thumbs_up=(TextView) this.findViewById(R.id.b_thumbs_up);
		final TextView thumbs_down=(TextView) this.findViewById(R.id.b_thumbs_down);
		
		if (main_button_wrap!=null) {
			if (main_button==null) {
				main_button=new EvaqalarmProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
			
				LayoutParams params=new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT); 	
	
		        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);//.setMargins((int)(20*dm.densityDpi/160f), (int)(8*dm.densityDpi/160f), 0, (int)(8*dm.densityDpi/160f));
				main_button.setLayoutParams(params);
				main_button.setIndeterminate(false);
				
				main_button.setMax(360*10);
				main_button.setEventListener(this);
				
				main_button.setChecked(isParkingState);
				main_button.setVisibility(View.INVISIBLE);
				main_button_wrap.addView(main_button);
				main_button.setViewTouchEvent(main_button_wrap);
			}
		}
		
		if (disclaimer!=null) {
			if (isAlarmState) disclaimer.setText(Html.fromHtml(getString(R.string.give_alarm_review)));
			else if (isParkingState) disclaimer.setText(Html.fromHtml(getString(R.string.press_to_cancell_parking)+"<br><br>"+getString(R.string.longpress_to_alert)));
			else  disclaimer.setText(Html.fromHtml(getString(R.string.press_to_parking)+"<br><br>"+getString(R.string.longpress_to_alert)));
			Animation animation = AnimationUtils.loadAnimation(this, R.anim.disclaimer);
			animation.setAnimationListener(new AnimationListener(){
				@Override
				public void onAnimationStart(Animation animation) {
					LayoutParams params=new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT); 	

					//params.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
			        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			        disclaimer.setLayoutParams(params);
				}
				@Override
				public void onAnimationEnd(Animation animation) {}
				@Override
				public void onAnimationRepeat(Animation animation) {}
			});
			disclaimer.setVisibility(View.VISIBLE);
			disclaimer.startAnimation(animation);			
		}
		
		if (logo!=null) {
			//logo.setVisibility(View.INVISIBLE);	
			Animation animation = AnimationUtils.loadAnimation(this, R.anim.logo_hide);
			animation.setAnimationListener(new AnimationListener(){
				@Override
				public void onAnimationStart(Animation animation) {}
				@Override
				public void onAnimationEnd(Animation animation) {
					logo.setVisibility(View.INVISIBLE);					
				}
				@Override
				public void onAnimationRepeat(Animation animation) {}
			});
			logo.startAnimation(animation);
		}
		if (review_buttons_wrap!=null) {
			if (isAlarmState) {
				Animation animation = AnimationUtils.loadAnimation(this, R.anim.disclaimer);
				animation.setAnimationListener(new AnimationListener(){
					@Override
					public void onAnimationStart(Animation animation) {
					}
					@Override
					public void onAnimationEnd(Animation animation) {}
					@Override
					public void onAnimationRepeat(Animation animation) {}
				});
				review_buttons_wrap.setVisibility(View.VISIBLE);
				review_buttons_wrap.startAnimation(animation);			
			}			
		}
		if (thumbs_up!=null) {
			thumbs_up.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					clearNotification();
					
					(new AsyncTask<Void, Void, Boolean>(){
						@Override
						protected Boolean doInBackground(Void... params) {
							
							SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
							int savedCount=settings.getInt(MainActivity.PARKING_ALARM_SOURCES_COUNT, 0);
		                	for (int i=0;i<savedCount;i++) {
		                		String regIdItem=settings.getString(MainActivity.PARKING_ALARM_SOURCES+"_"+i, "");
		                		if (!regIdItem.equals("")) {
		                			//Log.d("[---------]", ""+regIdItem.equals(regid)+" "+regIdItem);
		                			if (!regIdItem.equals(regid)) {
							            final List<NameValuePair> nameValuePairs = new ArrayList<>(2);
							            nameValuePairs.add(new BasicNameValuePair("auto[deviceId]", ""+regIdItem));					
										postData("/praise", nameValuePairs);
		                			}
		                		}		                			
		                	}
														
							return true;
						}
						@Override
						protected void onPostExecute(Boolean result) {}					
					}).execute();
					
					if (main_button!=null) {
						main_button.setAlarm(false);
					}
					SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
					boolean isParkingState=settings.getBoolean(PARKING_NAME, false);
					SharedPreferences.Editor editor = settings.edit();
					editor.putBoolean(MainActivity.PARKING_ALARM, false);
					editor.apply();
					
					if (review_buttons_wrap!=null) review_buttons_wrap.setVisibility(View.GONE); 

                    if (disclaimer!=null) {
                        if (isParkingState)
                            disclaimer.setText(Html.fromHtml(getString(R.string.press_to_cancell_parking) + "<br><br>" + getString(R.string.longpress_to_alert)));
                        else
                            disclaimer.setText(Html.fromHtml(getString(R.string.press_to_parking) + "<br><br>" + getString(R.string.longpress_to_alert)));
                    }
				}});
		}
		if (thumbs_down!=null) {
			thumbs_down.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					clearNotification();
					
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setTitle(getString(R.string.confirm))
							.setMessage(getString(R.string.bad_alarm_confirm))
							//.setIcon(R.drawable.ic_android_cat) 
							.setCancelable(false)
							.setPositiveButton(getString(R.string.not_sure),
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int id) {
											dialog.cancel();
										}
									})
							.setNegativeButton(getString(R.string.sure),
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int id) {
											dialog.cancel();

											(new AsyncTask<Void, Void, Boolean>(){
												@Override
												protected Boolean doInBackground(Void... params) {
													
													SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
													int savedCount=settings.getInt(MainActivity.PARKING_ALARM_SOURCES_COUNT, 0);
								                	for (int i=0;i<savedCount;i++) {
								                		String regIdItem=settings.getString(MainActivity.PARKING_ALARM_SOURCES+"_"+i, "");
								                		if (!regIdItem.equals("")) {
								                			if (!regIdItem.equals(regid)) {
													            final List<NameValuePair> nameValuePairs = new ArrayList<>(2);
													            nameValuePairs.add(new BasicNameValuePair("auto[deviceId]", ""+regIdItem));					
																postData("/petition", nameValuePairs);
								                			}
								                		}		                			
								                	}
													
													return false;
												}
												@Override
												protected void onPostExecute(Boolean result) {}					
											}).execute();
											
											if (main_button!=null) {
												main_button.setAlarm(false);
											}
											
											SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
											boolean isParkingState=settings.getBoolean(PARKING_NAME, false);
											SharedPreferences.Editor editor = settings.edit();
											editor.putBoolean(MainActivity.PARKING_ALARM, false);
											editor.apply();
											
											if (review_buttons_wrap!=null) review_buttons_wrap.setVisibility(View.GONE); 

                                            if (disclaimer!=null) {
                                                if (isParkingState)
                                                    disclaimer.setText(Html.fromHtml(getString(R.string.press_to_cancell_parking) + "<br><br>" + getString(R.string.longpress_to_alert)));
                                                else
                                                    disclaimer.setText(Html.fromHtml(getString(R.string.press_to_parking) + "<br><br>" + getString(R.string.longpress_to_alert)));
                                            }

										}
									});
					AlertDialog alert = builder.create();
					alert.show();
					
				}});
		}
		if (main_button!=null) {
			Animation animation = AnimationUtils.loadAnimation(this, R.anim.main_button);
			animation.setAnimationListener(new AnimationListener(){
				@Override
				public void onAnimationStart(Animation animation) {
				}
				@Override
				public void onAnimationEnd(Animation animation) {
					main_button.setAlarm(isAlarmState);
					
					SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
    				int update=settings.getInt(MainActivity.PARAM_UPDATE, 0);
    				if (update<=getAppVersion(MainActivity.this)) {
    					if (!(isAlarmState||isParkingState)) showRedLine(getString(R.string.gps_wait));
    				} else {
    					showUpdateDialog();
    				}
				}
				@Override
				public void onAnimationRepeat(Animation animation) {}
			});
			main_button.setVisibility(View.VISIBLE);					
			main_button.startAnimation(animation);
		}
		if (share_button!=null) {
			share_button.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					clearNotification();
					Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.share_button_click);
					share_button.startAnimation(animation);
					
					startActivity(Intent.createChooser(getDefaultIntent(), getResources().getText(R.string.send_to)));

					YandexMetrica.reportEvent("Share button clicked");
				}});
			Animation animation = AnimationUtils.loadAnimation(this, R.anim.share_button);
			animation.setAnimationListener(new AnimationListener(){
				@Override
				public void onAnimationStart(Animation animation) {
				}
				@Override
				public void onAnimationEnd(Animation animation) {}
				@Override
				public void onAnimationRepeat(Animation animation) {}
			});
			share_button.setVisibility(View.VISIBLE);					
			share_button.startAnimation(animation);
		}

        if (support_button!=null) {
            support_button.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    clearNotification();
                    Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.share_button_click);
                    support_button.startAnimation(animation);

                    // Taifuno.getInstance().showChat();
                    Taifuno.getInstance().showChat(getSystemInfo());

                    YandexMetrica.reportEvent("Support button clicked");
                }});
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.share_button);
            animation.setAnimationListener(new AnimationListener(){
                @Override
                public void onAnimationStart(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {}
                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            support_button.setVisibility(View.VISIBLE);
            support_button.startAnimation(animation);
        }
	}

	@Override
	public void onPressTimer() {
		if (detectedLocation!=null) {
			
			final LatLng latLng;
			if (mMap!=null) {
				latLng=mMap.getCameraPosition().target;
			} else {
				latLng=new LatLng(detectedLocation.getLatitude(), detectedLocation.getLongitude());
			}
			
			// Send alert on detected location
            final List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new BasicNameValuePair("auto[lat]", (String.format("%1$.6f", latLng.latitude)).replaceAll(",", ".")));
            nameValuePairs.add(new BasicNameValuePair("auto[lon]", (String.format("%1$.6f", latLng.longitude)).replaceAll(",", ".")));		            
            nameValuePairs.add(new BasicNameValuePair("auto[deviceId]", ""+regid));
            
            (new AsyncTask<Void, Void, Boolean>(){
				@Override
				protected Boolean doInBackground(Void... params) {
					return postData("/android/setAlarm", nameValuePairs);
				}
				@Override
				protected void onPostExecute(Boolean result) {
					super.onPostExecute(result);
					if (!result) {
						showMessage(getString(R.string.server_error));
						YandexMetrica.reportEvent("Server error on send alarm");
					} else {
						showMessage(getString(R.string.alert_success));
						YandexMetrica.reportEvent("Alarm success");
					}
				}
			}).execute();            			
		} else {
			showMessage(getString(R.string.gps_error));	
			YandexMetrica.reportEvent("GPS error on send alarm");		
		}
	}

	@Override
	public void onClick() {
		final TextView disclaimer=(TextView) this.findViewById(R.id.disclaimer);
		if (main_button!=null) {
			if (main_button.getChecked()) {
				if (detectedLocation!=null) {					
					// send parking info
					final LatLng latLng;
					if (mMap!=null) {
						latLng=mMap.getCameraPosition().target;
					} else {
						latLng=new LatLng(detectedLocation.getLatitude(), detectedLocation.getLongitude());
					}
					
					
		            final List<NameValuePair> nameValuePairs = new ArrayList<>(2);
		            nameValuePairs.add(new BasicNameValuePair("auto[lat]", (String.format("%1$.6f", latLng.latitude)).replaceAll(",", ".")));
		            nameValuePairs.add(new BasicNameValuePair("auto[lon]", (String.format("%1$.6f", latLng.longitude)).replaceAll(",", ".")));		            
		            nameValuePairs.add(new BasicNameValuePair("auto[deviceId]", ""+regid));					
		            Log.d("[---!!!---]", ""+regid);
		            
		            (new AsyncTask<Void, Void, Boolean>(){
						@Override
						protected Boolean doInBackground(Void... params) {
							return postData("/android/setParked", nameValuePairs);
						}
						@Override
						protected void onPostExecute(Boolean result) {
							super.onPostExecute(result);
							if (!result) {
								main_button.setChecked(false);
								SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
								SharedPreferences.Editor editor = settings.edit();
								editor.putBoolean(PARKING_NAME, false);
								editor.apply();
								if (disclaimer!=null) disclaimer.setText(Html.fromHtml(getString(R.string.press_to_parking)+"<br><br>"+getString(R.string.longpress_to_alert)));
								showMessage(getString(R.string.server_error));
								YandexMetrica.reportEvent("Server error on send parking");
							} else {
								SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
								SharedPreferences.Editor editor = settings.edit();
								editor.putBoolean(PARKING_NAME, true);
								parkingcount+=1;
								editor.putInt(PARKING_COUNT, parkingcount);
								editor.apply();
								if (disclaimer!=null) disclaimer.setText(Html.fromHtml(getString(R.string.press_to_cancell_parking)+"<br><br>"+getString(R.string.longpress_to_alert)));
								showMessage(getString(R.string.parking_success));
								YandexMetrica.reportEvent("Parking success");
								
								putParkingMarker(latLng);
								
								if (parkingcount>0&&(parkingcount%6==0)) {
									showShareDialog();
								} else if (parkingcount>0&&(parkingcount%10==0)) {
									showReviewDialog();
								}
							}
						}					
					}).execute();            			
										
				} else {
					main_button.setChecked(false);
					if (disclaimer!=null) disclaimer.setText(Html.fromHtml(getString(R.string.press_to_parking)+"<br><br>"+getString(R.string.longpress_to_alert)));
					showMessage(getString(R.string.gps_error));
					YandexMetrica.reportEvent("GPS error on send parking");
				}
			} else {			
	            final List<NameValuePair> nameValuePairs = new ArrayList<>(2);
	            nameValuePairs.add(new BasicNameValuePair("auto[deviceId]", ""+regid));				
	            Log.d("[---!!!---]", ""+regid);
	            
	            (new AsyncTask<Void, Void, Boolean>(){
					@Override
					protected Boolean doInBackground(Void... params) {
						return postData("/clearParking", nameValuePairs);
					}
					@Override
					protected void onPostExecute(Boolean result) {
						super.onPostExecute(result);
						SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
						SharedPreferences.Editor editor = settings.edit();
						editor.putBoolean(PARKING_NAME, false);
						editor.apply();
						if (disclaimer!=null) disclaimer.setText(Html.fromHtml(getString(R.string.press_to_parking)+"<br><br>"+getString(R.string.longpress_to_alert)));
						
						showMessage(getString(R.string.parking_cancell));
						
						if (result) YandexMetrica.reportEvent("Parking cancelled");
						else YandexMetrica.reportEvent("Server error on parking cancell");

						removeParkingMarker();
						
					}
				}).execute();            											
			}
		}
	}
	
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		isGoogleservicesAvailable=true;
        if (resultCode != ConnectionResult.SUCCESS) {
			isGoogleservicesAvailable=false;
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
            	YandexMetrica.reportEvent("Google Play service need update");
                Dialog ed=GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST);
                ed.setOnCancelListener(new OnCancelListener(){
					@Override
					public void onCancel(DialogInterface dialog) {
						finish();
					}
				});
                ed.show();
            } else {
            	Toast.makeText(this, "Google Play service is not available (status=" + resultCode + ")", Toast.LENGTH_LONG).show();
            	YandexMetrica.reportEvent("Google Play service is not available", "{\"status=\":\"" + resultCode + "\"}");
                finish();
            }
            return false;
        }
        return true;
    }
    
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences();
        int appVersion = getAppVersion(context);
        //Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }
    
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGcmPreferences();
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            //Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            //Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }
    
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg;
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    sendRegistrationIdToBackend();

                    // For this demo: we don't need to send it because the device will send
                    // upstream messages to a server that echo back the message using the
                    // 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                Log.d("[---!!!---]", msg + "\n");
            }
        }.execute(null, null, null);
    }
    
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }
    
    private SharedPreferences getGcmPreferences() {
        return getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
    }
    
    private void sendRegistrationIdToBackend() {
        if (regid != null) {
            handler.post(new Runnable(){
                @Override
                public void run() {
                    Taifuno.getInstance().regDeviceId(regid);
                }
            });
        }
    }
    
    public boolean postData(String cmd, List<NameValuePair> nameValuePairs) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://evaqalarm.speind.com"+cmd);
        try {
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            
            HttpEntity entity = response.getEntity();
            StringBuilder sb = new StringBuilder();
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()), 65728);
                String line;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } catch (Exception e) {
            	e.printStackTrace();
            }

            Log.d("[---!!!---]", sb.toString());
            return response.getStatusLine().getStatusCode() == 200;
        } catch (ClientProtocolException e) {
        	return false;
        } catch (IOException e) {
        	return false;
        }
    }
    
    private void showMessage(String msg) {
		Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 20);
		TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
		if( v != null) v.setGravity(Gravity.CENTER);
		toast.show();
    }
    
    private void show_dialog(int text_id, OnClickListener positive_button, int positive_text, final boolean closeable) {
    	isNeedHideDialog=closeable;
    	final RelativeLayout dialog_wrap=(RelativeLayout) findViewById(R.id.dialog_wrap);
    	if (dialog_wrap!=null) {
    		final ImageView close_button=(ImageView) findViewById(R.id.close_button);
    		final Button ok_button=(Button) findViewById(R.id.ok_botton);
    		final TextView message=(TextView) findViewById(R.id.message);
    		
    		if (message!=null) {
    			message.setText(Html.fromHtml(getString(text_id)));
    		}
    		if (ok_button!=null) {
    			ok_button.setText(positive_text);
    			ok_button.setOnClickListener(positive_button);
    		}
    		if (close_button!=null) {
    			close_button.setVisibility(View.GONE);
    			close_button.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.share_button_click);
						close_button.startAnimation(animation);
						hide_dialog();
					}  				
    			});
    		}
    		
    		Animation animation = AnimationUtils.loadAnimation(this, R.anim.dialog);
			animation.setAnimationListener(new AnimationListener(){
				@Override
				public void onAnimationStart(Animation animation) {
		    		dialog_wrap.setVisibility(View.VISIBLE);	
		    		if (close_button!=null) {
		    			if (closeable) {
		    				close_button.setVisibility(View.VISIBLE);
		    				Animation animation1 = AnimationUtils.loadAnimation(MainActivity.this, R.anim.close_button);
		    				close_button.startAnimation(animation1);
		    			}
		    		}
		    		if (message!=null) {
	    				Animation animation1 = AnimationUtils.loadAnimation(MainActivity.this, R.anim.message);
	    				message.startAnimation(animation1);
		    		}
		    		if (ok_button!=null) {
	    				Animation animation1 = AnimationUtils.loadAnimation(MainActivity.this, R.anim.ok_button);
	    				ok_button.startAnimation(animation1);
		    		}
				}
				@Override
				public void onAnimationEnd(Animation animation) {}
				@Override
				public void onAnimationRepeat(Animation animation) {}
			});
			dialog_wrap.startAnimation(animation);
			
    	}
    }
    
    private void hide_dialog() {
    	isNeedHideDialog=false;
    	RelativeLayout dialog_wrap=(RelativeLayout) findViewById(R.id.dialog_wrap);
    	if (dialog_wrap!=null) {
    		dialog_wrap.setVisibility(View.GONE);
    	}
    }
    
    private void showShareDialog() {
		show_dialog(R.string.share_text, new OnClickListener(){
			@Override
			public void onClick(View v) { 
				Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.share_button_click);
				v.startAnimation(animation);
				startActivity(Intent.createChooser(getDefaultIntent(), getResources().getText(R.string.send_to)));
				hide_dialog();
			}
		}, R.string.yes, true);
    }
    
    private void showReviewDialog() {
		show_dialog(R.string.feedback_text, new OnClickListener(){
			@Override
			public void onClick(View v) { 
				Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.share_button_click);
				v.startAnimation(animation);
				String appPackageName= getPackageName();
				Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+appPackageName));
				marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | 0x00080000/*Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET*/);
				startActivity(marketIntent);
				hide_dialog();
			}
		}, R.string.yes, true);
    }
    
    private void showUpdateDialog() {
		show_dialog(R.string.update_text, new OnClickListener(){
			@Override
			public void onClick(View v) { 
				Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.share_button_click);
				v.startAnimation(animation);
				String appPackageName= getPackageName();
				Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+appPackageName));
				marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | 0x00080000/*Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET*/);
				startActivity(marketIntent);
			}
		}, R.string.update, false);
    }
    
    private void showRedLine(String text) {
    	RelativeLayout red_line_wrap=(RelativeLayout) findViewById(R.id.red_line_wrap);
		final TextView wait_title=(TextView) findViewById(R.id.wait_title);
		if (wait_title!=null) {
			wait_title.setVisibility(View.INVISIBLE);
		}
    	if (red_line_wrap!=null) {
    		red_line_wrap.setVisibility(View.VISIBLE);
    	}    	
		if (wait_title!=null) {
			wait_title.setText(text);
    		Animation animation = AnimationUtils.loadAnimation(this, R.anim.red_line);
			wait_title.setVisibility(View.VISIBLE);
			wait_title.startAnimation(animation);	
			
		}
    }

    private void hideRedLine() {
    	final RelativeLayout red_line_wrap=(RelativeLayout) findViewById(R.id.red_line_wrap);
    	final TextView wait_title=(TextView) findViewById(R.id.wait_title);
    	if (wait_title!=null) {
    		Animation animation = AnimationUtils.loadAnimation(this, R.anim.red_line_hide);
			animation.setAnimationListener(new AnimationListener(){
				@Override
				public void onAnimationStart(Animation animation) {}
				@Override
				public void onAnimationEnd(Animation animation) {
			    	if (red_line_wrap!=null) {
			    		red_line_wrap.setVisibility(View.GONE);
			    	}    	
				}
				@Override
				public void onAnimationRepeat(Animation animation) {}
			});
			wait_title.startAnimation(animation);	
			
		}
    }

	@Override
	public void onMapReady(GoogleMap map) {
		mMap=map;
		mMap.setMyLocationEnabled(true);
		map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		
		UiSettings uiSettings=map.getUiSettings();
		
		uiSettings.setScrollGesturesEnabled(true);
		uiSettings.setZoomGesturesEnabled(false);
		uiSettings.setTiltGesturesEnabled(false);
		uiSettings.setRotateGesturesEnabled(false);
		
		uiSettings.setZoomControlsEnabled(false);		
		uiSettings.setCompassEnabled(false);
		uiSettings.setMyLocationButtonEnabled(false);
		uiSettings.setIndoorLevelPickerEnabled(false);
		uiSettings.setMapToolbarEnabled(false);		
		
		if (detectedLocation!=null) {
			setCamera(new LatLng(detectedLocation.getLatitude(), detectedLocation.getLongitude()));
		}
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		// TODO Auto-generated method stub
		
	}
    
	private void setCamera(LatLng latLng) {
		if (mMap!=null) {
	        RelativeLayout mw=(RelativeLayout) findViewById(R.id.map_wrap);
	        if (mw!=null) {
	        	RelativeLayout tw=(RelativeLayout) findViewById(R.id.top_wrap);
		        RelativeLayout mbw=(RelativeLayout) findViewById(R.id.button_wrap);
		        int mwh=mw.getHeight();
				int twh=0;
				int mbwh=0;
		        if (tw!=null) {
		        	twh=tw.getHeight();
		        }
		        if (mbw!=null) {
		        	mbwh=mbw.getHeight();
		        }
				mMap.setPadding(0, 0, 0, mwh-(mbwh+twh*2));				
	        }
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			final boolean isParkingState=settings.getBoolean(PARKING_NAME, false);
			final boolean isAlarmState=settings.getBoolean(PARKING_ALARM, false);
			if (isParkingState) {
				if (isAlarmState) {
					latLng=new LatLng(settings.getFloat("PLat", 0), settings.getFloat("PLng", 0));					
				}
				if (parkingMarker==null) {
					putParkingMarker(new LatLng(settings.getFloat("PLat", 0), settings.getFloat("PLng", 0)));
				} 
				if (!waitUserCorrect||isAlarmState) {
					CameraPosition cameraPosition = new CameraPosition.Builder()
				    .target(latLng)      		// Sets the center of the map
				    .zoom(17)                   // Sets the zoom
				    .bearing(0)               	 // Sets the orientation of the camera to north
				    //.tilt(90)                   // Sets the tilt of the camera to 30 degrees
				    .build();                   // Creates a CameraPosition from the builder
					mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
				}				
			} else {
				if (!waitUserCorrect) {
					CameraPosition cameraPosition = new CameraPosition.Builder()
				    .target(latLng)      		// Sets the center of the map
				    .zoom(17)                   // Sets the zoom
				    .bearing(0)               	 // Sets the orientation of the camera to north
				    //.tilt(90)                   // Sets the tilt of the camera to 30 degrees
				    .build();                   // Creates a CameraPosition from the builder
					mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
				}
			}
		}
	}
	
	private void putParkingMarker(LatLng latLng) {
		if (mMap!=null) {									
			parkingMarker=mMap.addMarker(new MarkerOptions()
	        .position(latLng)
	        .anchor(0.5f,0.5f)
	        .icon(BitmapDescriptorFactory.fromResource(R.drawable.parking))
	        .title("My car"));
		}
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putFloat("PLat", (float) latLng.latitude);
		editor.putFloat("PLng", (float) latLng.longitude);
		editor.apply();
	}

	private void removeParkingMarker() {
		if (detectedLocation!=null) {
			handler.removeCallbacks(clearWaitUserCorrectRunnable);
			waitUserCorrect=false;
			setCamera(new LatLng(detectedLocation.getLatitude(), detectedLocation.getLongitude()));
		}
		if (parkingMarker!=null) {
			parkingMarker.remove();
			parkingMarker=null;
		}
	}

    private static String getAppVersionStr(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private String getSystemInfo() {
        long max = Runtime.getRuntime().maxMemory(); //the maximum memory the app can use
        long heapSize = Runtime.getRuntime().totalMemory(); //current heap size
        long heapRemaining = Runtime.getRuntime().freeMemory(); //amount available in heap
        long nativeUsage = Debug.getNativeHeapAllocatedSize(); //is this right? I only want to account for native memory that my app is being "charged" for.  Is this the proper way to account for that?
        long remaining = max - (heapSize - heapRemaining + nativeUsage);
        String os="API Level "+android.os.Build.VERSION.SDK_INT;
        switch (android.os.Build.VERSION.SDK_INT) {
            case 14:
                os="Android 4.0 - 4.0.2";
                break;
            case 15:
                os="Android 4.0.3 - 4.0.4";
                break;
            case 16:
                os="Android 4.1 - 4.1.2";
                break;
            case 17:
                os="Android 4.2 - 4.2.2";
                break;
            case 18:
                os="Android 4.3 - 4.3.1";
                break;
            case 19:
                os="Android 4.4 - 4.4.2";
                break;
            case 20:
                os="Android 4.4W - 4.4W.2";
                break;
            case 21:
                os="Android 5.0 - 5.0.2";
                break;
            case 22:
                os="Android 5.1 - 5.1.1";
                break;
        }
        String info = "App: EvaqAlarm v"+getAppVersionStr(context)+";\n Device: "+android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL+" "+android.os.Build.PRODUCT+";\n OS: "+os+";\n Free memory: "+remaining/(1024*1024)+"Mb of "+max/((1024*1024))+"Mb available for app";
        return info;
    }

}
