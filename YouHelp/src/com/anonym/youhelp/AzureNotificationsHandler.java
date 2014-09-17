package com.anonym.youhelp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.anonym.youhelp.database.YHDataSource;
import com.microsoft.windowsazure.notifications.NotificationsHandler;

public class AzureNotificationsHandler extends NotificationsHandler {
	
	private YHDataSource datasource;
	
	public AzureNotificationsHandler()
	{
		Log.d(TAG, "AzureNotificationsHandler created");
	}
	
	private static final String TAG = "AzureNotificationsHandler";
	
	public static final int NOTIFICATION_ID = 1;
	private NotificationManager mNotificationManager;
	NotificationCompat.Builder builder;
	Context ctx;
	
	@Override
	public void onRegistered(Context context, 
							String gcmRegistrationId) 
	{
		String msg ="Azure Notification Handler was registered with GCM: " + gcmRegistrationId;
		
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show(); 
	}
	
	@Override
	public void onReceive(Context context, Bundle bundle) {
	    
		Log.d(TAG, "Message received");
		
		ctx = context;
	    String title = bundle.getString("msg");
	    String sentUserid = bundle.getString("userid");
	    String blobURL = "";
	    String coords = bundle.getString("coords");
		String[] tokens = coords.split(";");
		if( tokens.length != 2 ) 
			return;

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		String myUserid = sharedPrefs.getString("registrationProvider", "") 
				+ ":" 
				+ sharedPrefs.getString("userid", "") ;
		
		// Do not receive from yourself
//		if( myUserid.length() != 0
//			&& myUserid.equals(sentUserid) ) { // current user == sending user
//				return;
//		}
		
		persistMessage(context, title, sentUserid, blobURL);
	    
		startExternalActivity(context, tokens[0], tokens[1], title, sentUserid);
		
	    sendNotification(title);

	}
	
	private void persistMessage(Context context, String content, String userid, String blobURL){
		
		try{
			if( datasource == null)
				datasource = new YHDataSource(context);
				
			datasource.open();
			 
			Date date = new Date();
			datasource.createYHMessage(content, userid, date, "", blobURL);
			datasource.close();
		
		}catch(Exception ex){
			
			Log.e(TAG, ex.getMessage());
			//Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();

		}
	}
	
	private boolean isActivityRunning(Context context, String activityName)
	{
		ArrayList<String> runningactivities = new ArrayList<String>();
		ActivityManager activityManager = (ActivityManager)context.getSystemService (Context.ACTIVITY_SERVICE); 
		//activityManager.isUserAMonkey()
		
		List<RunningTaskInfo> services = activityManager.getRunningTasks(Integer.MAX_VALUE); 

		for (int i1 = 0; i1 < services.size(); i1++) { 
	        runningactivities.add(0,services.get(i1).topActivity.toString());  
	    }
		
		StringBuilder sb = new StringBuilder("ComponentInfo{");
		sb.append(activityName);
		sb.append("}");
		
		String componentName = sb.toString();
		
		if(runningactivities.contains(componentName)==true){
	        return true;

	    }
		
		return false;
	}
	
	private void startExternalActivity(Context context,
										String lat,
										String lon,
										String title,
										String userid)
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		// Assume Google Maps as default
		int mapCode = sharedPrefs.getInt("mapsApp", 2);
		
		switch( mapCode )
		{
			case 1: // Waze
				startWaze(context,lat, lon, title, userid);
				break;
			
			case 2: // Google Maps
				startGoogleMaps(context,lat, lon, title, userid);
				break;
				
			case 3:
				startInternalMaps(context,lat, lon, title, userid);
				break;
		}
		
		
	}
	
	private void startInternalMaps(Context context,
							String lat,
							String lon,
							String title,
							String userid)
	{
		try{
			
			//boolean bIsRunning = isActivityRunning(context, "com.anonym/com.anonym.MapActivity");
	        Intent activityIntent =  new Intent(context, MapActivity.class);
	        activityIntent.putExtra("userid", userid);
	        activityIntent.putExtra("coords", lat + "," + lon);
	        activityIntent.putExtra("title", title);
		    PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
		    											activityIntent, 0);
		    contentIntent.send(10);
		
		} catch(Exception ex) {
			Toast.makeText(context, ex.getMessage(),
			         Toast.LENGTH_LONG).show(); 
		}
	}
	
	private void startWaze(Context context,
							String lat,
							String lon,
							String title,
							String userid)
	{
		try{
			StringBuilder sb = new StringBuilder("waze://?ll=");
			sb.append(lat);
			sb.append(",");
			sb.append(lon);
			sb.append("&z=6");
			
			// center waze map to lat / lon:
			String url = sb.toString(); // "waze://?ll=32.072072072072075,34.8716280366431456&z=6"; // "waze://?q=Jerusalem";
			
			Intent wazeIntent = new Intent( Intent.ACTION_VIEW, Uri.parse( url ) );
			wazeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(wazeIntent);
		
		} catch(Exception ex) {
			Toast.makeText(context, ex.getMessage(),
			         Toast.LENGTH_LONG).show(); 
		}
	
	}
	
	
	private static final String GOOGLE_MAPS_PACKAGE_NAME = "com.google.android.apps.maps";
	
	private int getGoogleMapsVersion(Context context) {
		
		try{
			PackageManager packageManger = context.getPackageManager();
			PackageInfo pInfo = packageManger.getPackageInfo(GOOGLE_MAPS_PACKAGE_NAME, 0);
			
			return pInfo.versionCode; 
		} catch(Exception ex){
			ex.printStackTrace();
		}
		
		return 0;
	}
	
	private void startGoogleMaps(Context context,
								String lat,
								String lon,
								String title,
								String userid)
	{
		try{
			StringBuilder sb = new StringBuilder("geo:");
			sb.append(lat);
			sb.append(",");
			sb.append(lon);
			sb.append("?q="); // at this point should be "0,0?q=
			sb.append(lat);
			sb.append(",");
			sb.append(lon);

			if( getGoogleMapsVersion(context) >= 800000000 ) 
				sb.append("(" + title + " from " + userid + ")");
			
			String url = sb.toString();
			
			Intent gmIntent = new Intent(android.content.Intent.ACTION_VIEW);
			gmIntent.setData(Uri.parse(url));
				    //Uri.parse("http://maps.google.com/maps?daddr=32.072072072072075,34.8716280366431456&mode=driving"));
					//Uri.parse("geo:0,0?q=32.072072072072075,34.8716280366431456(Reported Place)")
					//Uri.parse( url )
					//);
			gmIntent.setClassName(GOOGLE_MAPS_PACKAGE_NAME, "com.google.android.maps.MapsActivity");
			gmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			
			if( gmIntent.resolveActivity(context.getPackageManager()) != null) {
				context.startActivity(gmIntent);
			}
			
		} catch(Exception ex) {
			Toast.makeText(context, ex.getMessage(),
			         Toast.LENGTH_LONG).show(); 
		}
		
	}
	
	
	
	private void sendNotification(String msg) {
	    mNotificationManager = (NotificationManager)
	              ctx.getSystemService(Context.NOTIFICATION_SERVICE);

	    PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0,
	          new Intent(ctx, MainActivity.class), 0);
	    
		NotificationCompat.Builder mBuilder =
		          new NotificationCompat.Builder(ctx)
		          .setSmallIcon(R.drawable.ic_launcher)
		          .setVibrate(new long[] { 500, 500 })
		          .setContentTitle("You Help")
		          .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
		          .setStyle(new NotificationCompat.BigTextStyle()
		              			.bigText(msg))
		          .setContentText(msg);

	     mBuilder.setContentIntent(contentIntent);
	     mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}
}
