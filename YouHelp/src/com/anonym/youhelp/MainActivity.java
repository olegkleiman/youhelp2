package com.anonym.youhelp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.facebook.AppEventsLogger;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.microsoft.windowsazure.messaging.NotificationHub;
import com.microsoft.windowsazure.notifications.NotificationsManager;

public class MainActivity extends FragmentActivity implements AnimationListener {

	private static final String TAG = "YouHelp";
    
	static final int REGISTER_USER_REQUEST = 1; 
	
	private String GCM_SENDER_ID = "314835619656";
	private GoogleCloudMessaging gcm;
	private NotificationHub hub;
	
	LinearLayout mainLayout;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        
        // Set background for layout
        mainLayout = (LinearLayout)findViewById(R.id.main_layout);
        mainLayout.setBackgroundResource(R.drawable.background);
        
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String prefUserID = sharedPrefs.getString("prefUsername", "");

		if(prefUserID.length() == 0 ){
			
			try{
				Intent intent = new Intent(this, RegisterActivity.class);
				startActivityForResult(intent, REGISTER_USER_REQUEST);
			} catch(Exception ex){
				ex.printStackTrace();
			}
			
			return;
		}
		
		NotificationsManager.handleNotifications(this, 
												GCM_SENDER_ID, 
												AzureNotificationsHandler.class);
		gcm = GoogleCloudMessaging.getInstance(this);
		
		String connectionString = "Endpoint=sb://variant.servicebus.windows.net/;SharedAccessKeyName=DefaultFullSharedAccessSignature;SharedAccessKey=am4TM7Ajiot5kLoEelqQvU03eqlDUsdYrBt7UxJP22A=";
		hub = new NotificationHub("youhelphub", connectionString, this);
		
		registerWithNotificationHubs();
		
		InitStuff();
    }

    private void registerWithNotificationHubs() {
    	new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try{
					
					String regid = gcm.register(GCM_SENDER_ID);
					hub.register(regid);
				} catch (Exception e) {
					String msg = e.getMessage();
					Log.e(TAG, msg);
				}
				
				return null;
			}
    		
    	}.execute();
    }
    
    private void InitStuff(){
        // Load all animations and set up their listeners
        // 1. Move animation
        final Animation animationMove = AnimationUtils.loadAnimation(getApplicationContext(), 
				R.anim.move);
        animationMove.setAnimationListener(this);
        
        // Load all images and set their listeners
        final ImageView imageHelp = (ImageView)findViewById(R.id.imageViewHelp);
        imageHelp.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				String tag = (String)mainLayout.getTag();
				if( tag == null || 
						tag.contains("1") )
				{
					float xOffset = getChangeFactorX();	
					ObjectAnimator animationX =
							createFloatObjectAnimator(imageHelp,"translationX", 
														0, // repeat count
														1000, // duration
														false, // no reverse
														0, // from
														xOffset); // to
					float yOffset = getChangeFactorY();
					ObjectAnimator animationY =
							createFloatObjectAnimator(imageHelp,"translationY", 0, 1000, false, 0, yOffset);
	
					AnimatorSet animSetXY = new AnimatorSet();
					animSetXY.playTogether(animationX, animationY);
					animSetXY.start();
					
					mainLayout.setTag("2");
					
					// Dummy animation
					imageHelp.startAnimation(animationMove);
					
				} else {
					float xOffset = getChangeFactorX();	
					ObjectAnimator animationX =
							createFloatObjectAnimator(imageHelp,"translationX", 
														0, // repeat count
														1000, // duration
														true, // no reverse
														0, // from
														xOffset); // to
					
					float yOffset = getChangeFactorY();
					ObjectAnimator animationY =
							createFloatObjectAnimator(imageHelp,"translationY", 0, 1000, true, 0, yOffset);
	
					AnimatorSet animSetXY = new AnimatorSet();
					animSetXY.playTogether(animationX, animationY);
					animSetXY.start();
					
					mainLayout.setTag("1");
					
					// Dummy animation
					imageHelp.startAnimation(animationMove);
				}
				
				
			}
		});
        
        final ImageView imageSettings = (ImageView)findViewById(R.id.imageViewSettings);
        imageSettings.setOnClickListener( new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
				startActivity(intent);
				
			}
		});
        
        final ImageView imageMap = (ImageView)findViewById(R.id.imageViewMap);
        imageMap.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent intent = new Intent(MainActivity.this, MapActivity.class);
				startActivity(intent);
				
			}
        	
        });
        
        final ImageView imageFaltTire = (ImageView)findViewById(R.id.imageViewFaltTire);
        imageFaltTire.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent intent = new Intent(MainActivity.this, HazardActivity.class);			
				intent.putExtra("userid", getUserID());
				
				startActivity(intent);
				
			}
        	
        });
        
        final ImageView imageHelpOther = (ImageView)findViewById(R.id.imageViewHelpOther);
        imageHelpOther.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				Intent intent = new Intent(MainActivity.this, HazardActivity.class);				
				intent.putExtra("userid", getUserID());
				
				startActivity(intent);
				
			}
        });
        
        
        final ImageView imageJamperCable = (ImageView)findViewById(R.id.imageViewJamperCable);
        imageJamperCable.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				Intent intent = new Intent(MainActivity.this, HazardActivity.class);			
				intent.putExtra("userid", getUserID());
				
				startActivity(intent);
				
			}
        });
        
        
        final ImageView imageSOS = (ImageView)findViewById(R.id.imageViewSOS);
        imageSOS.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				String smsNumber1 = sharedPrefs.getString("sos1", "");
				
				List<String> smsNumbers = new ArrayList<String>();
				
				if( !smsNumber1.isEmpty() )
					smsNumbers.add(smsNumber1);
				String smsNumber2 = sharedPrefs.getString("sos2", "");
				if( !smsNumber2.isEmpty() )
					smsNumbers.add(smsNumber2);
				
				if( smsNumbers.isEmpty() ) {
					msgBox("No emergency numbers", "Please provide the emergency numbers in settings.");
					return;
				}
				
				StringBuilder sb = new StringBuilder("I'm in an emergency. Please help!\n Map link: \n"); 
				sb.append("http://maps.google.com/?ie=UTF&z=138&hq=&ll="); // here.com/");
				
				LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
				String locationProvider = LocationManager.NETWORK_PROVIDER;
				// Or use LocationManager.GPS_PROVIDER

				Location currentLocation = locationManager.getLastKnownLocation(locationProvider);
				
				double lat = currentLocation.getLatitude();
		   	    double lon = currentLocation.getLongitude();
		   	    String strCurrentLocation = String.format(Locale.US, "%.13f;%.13f", lat, lon);
				sb.append(strCurrentLocation);
				
				String now = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
				sb.append(".\n Sent at ");
				sb.append(now);
				
				final String emergencyMessage = sb.toString();
				
				SendSmsWithIntent(smsNumbers, emergencyMessage);
				
				SendSmsWithManager(smsNumbers, emergencyMessage);

			}
		});
    }

    public class ReverseInterpolator implements Interpolator {
        @Override
        public float getInterpolation(float paramFloat) {
            return Math.abs(paramFloat -1f);
        }
    }
    
    private ObjectAnimator createFloatObjectAnimator(View view, 
			   String propertyName, 
			   int repeatCount,
			   int duration,
			   boolean isReverse,
			   float... values){

		ObjectAnimator animation = ObjectAnimator.ofFloat(view, 
							propertyName, 
							values[0], values[1]);
		
		animation.setDuration(duration);
		animation.setRepeatCount(repeatCount);
		
		Interpolator interpolator = isReverse ? 
				new ReverseInterpolator() :
				new 	AccelerateDecelerateInterpolator(); 
		
		animation.setInterpolator(interpolator);
		
		return animation;
    }

	// This factor depends on background image scratching
	private float getChangeFactorX() {
		// If not in Activity, use this:
		//final WindowManager wm = (WindowManager)MainActivity.this.getSystemService(Context.WINDOW_SERVICE);
		//wm.getDefaultDisplay().getSize(size);
		
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
		
		float aspectRatio = (float)height / width;
		
		int initialAspect = 28; // percents
		// Tested models : Galaxy Note II : 1.7
		//				   Nexus 7 (2012) : 1.52
		
		if( aspectRatio >= 1.7 ) 
		initialAspect = 36;
		
		return initialAspect * width / 100;
	}

	private float getChangeFactorY() {

		Display display = getWindowManager().getDefaultDisplay();		
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
		
		float aspectRatio = (float)height / width;
		
		int initialAspect = 11; // percents
		// Tested models : Galaxy Note II : 1.7
		//				   Nexus 7 (2012) : 1.52
		
		if( aspectRatio >= 1.7 ) 
		initialAspect = 14;
		
		return initialAspect * height / 100;
	}
    
    private void SendSmsWithManager(List<String> smsNumbers, String message){
		
    	try{
	    	String SENT = "sent";
			String DELIVERED = "delivered";
			
			// Register for SMS send action
			registerReceiver(sendReceiver, new IntentFilter(SENT));
			registerReceiver(deliveryReceiver, new IntentFilter(DELIVERED));
			
			final SmsManager smsManager = SmsManager.getDefault();
			Intent sentIntent = new Intent(SENT);
			final PendingIntent sentPI = PendingIntent.getBroadcast(getApplicationContext(), 0, 
									sentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			Intent deliveryIntent = new Intent(DELIVERED);
			final PendingIntent deliverPI = PendingIntent.getBroadcast(getApplicationContext(), 0,
									deliveryIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			
			
			for(final String smsNumber : smsNumbers){
				if( !smsNumber.isEmpty() ) {
					
					smsManager.sendTextMessage(smsNumber, null, message, 
							   sentPI, deliverPI);
				}
		}  	
    	}catch(Exception ex){
    		ex.printStackTrace();
    		Log.e(TAG, ex.getMessage());
    	}
    }
    
    private void SendSmsWithIntent(List<String> smsNumbers, String smsMesssage){
		StringBuilder sb = new StringBuilder();
		
		for(final String smsNumber : smsNumbers){
			sb.append(smsNumber);
			sb.append(";");
		}
		
		Uri uri = Uri.parse("smsto:" + sb.toString());
		Intent i = new Intent(Intent.ACTION_SENDTO, uri);
		i.putExtra("sms_body", smsMesssage);  
		//i.setPackage("com.whatsapp");  
		startActivity(i);		
	}
    
    private BroadcastReceiver deliveryReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			
			Toast.makeText(getApplicationContext(), "Deliverd",
			         Toast.LENGTH_LONG).show();
			
		}
		
	};

    
    private BroadcastReceiver sendReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			String result = "";
			
			switch( getResultCode() ){
				case Activity.RESULT_OK:
					result = "Transmission successful";
					break;
					
				case Activity.RESULT_CANCELED:
					result = "SMS not delivered";
	                break;                        
					
				case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					result = "Transmission failed";
					break;
					
				case SmsManager.RESULT_ERROR_RADIO_OFF:
					result = "Radio off";
					break;
					
			    case SmsManager.RESULT_ERROR_NULL_PDU:
			           result = "No PDU defined";
			           break;
			           
			    case SmsManager.RESULT_ERROR_NO_SERVICE:
			           result = "No service";
			           break;
			};
			
			Toast.makeText(getApplicationContext(),result,Toast.LENGTH_LONG).show();
		}
	};
    
    private String getUserID(){
    	
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String userid = sharedPrefs.getString("registrationProvider", "");
		return userid + ":" + sharedPrefs.getString("userid", "");
    }
    
	public void msgBox(String title,String message)
	{
	    AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);                      
	    dlgAlert.setMessage(message)
	    		.setTitle(title)           
	    		.setPositiveButton("OK", null)
	    		//.setCancelable(true)
	    		.create().show();

	}
    
    @Override
    protected void onResume() {
        super.onResume();

        // Call the 'activateApp' method to log an app event for use in analytics and advertising reporting.  Do so in
        // the onResume methods of the primary Activities that an app may be launched into.
        AppEventsLogger.activateApp(this);

        //updateUI();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        InitStuff();
        
        if (requestCode == REGISTER_USER_REQUEST) {
        
	        if(resultCode == RESULT_OK){
	        	
	        	try{
	        	
		        	RegisteredUser parcellUser = (RegisteredUser)data.getParcelableExtra("reg_user");
		        	String username = parcellUser.getUsername();
		        	username.trim();
		        	
	        	}catch(Exception ex){
	        		String str = ex.getMessage();
	        		Log.e(TAG, str);
	        	}
	        }
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();

        // Call the 'deactivateApp' method to log an app event for use in analytics and advertising
        // reporting.  Do so in the onPause methods of the primary Activities that an app may be launched into.
        AppEventsLogger.deactivateApp(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

		//NotificationsManager.stopHandlingNotifications(this);

    }
 
	@Override
	public void onAnimationEnd(Animation animation) {
        
        //LinearLayout mainLayout = (LinearLayout)findViewById(R.id.main_layout);
        mainLayout.setBackgroundResource(R.drawable.background2);

        final ImageView flatTireImageView = (ImageView)findViewById(R.id.imageViewFaltTire);
        flatTireImageView.setVisibility(View.VISIBLE);
        
        final ImageView cablesImageView = (ImageView)findViewById(R.id.imageViewJamperCable);
        cablesImageView.setVisibility(View.VISIBLE);
        
        final ImageView helpOtherImageView = (ImageView)findViewById(R.id.imageViewHelpOther);
        helpOtherImageView.setVisibility(View.VISIBLE);
	}

	@Override
	public void onAnimationRepeat(Animation animation) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAnimationStart(Animation animation) {
		
		final ImageView imageMap = (ImageView)findViewById(R.id.imageViewMap);
  		final ImageView imageTrip = (ImageView)findViewById(R.id.imageViewTrip);
  		final ImageView imageSettings = (ImageView)findViewById(R.id.imageViewSettings);
  		final ImageView imageCompass = (ImageView)findViewById(R.id.imageViewCompass);
 		final ImageView imageSOS = (ImageView)findViewById(R.id.imageViewSOS);
  		
		// Load and perform FadeOut animation
        final Animation animationFadeOut = AnimationUtils.loadAnimation(this, R.anim.fadeout);
      
        imageMap.startAnimation(animationFadeOut);
        imageTrip.startAnimation(animationFadeOut);
        imageSettings.startAnimation(animationFadeOut);
        imageCompass.startAnimation(animationFadeOut);
        imageSOS.startAnimation(animationFadeOut);
		
	}
}
