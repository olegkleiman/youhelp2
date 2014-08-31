package com.anonym.youhelp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.windowsazure.notifications.NotificationsHandler;

public class AzureNotificationsHandler extends NotificationsHandler {
	
	private static final String TAG = "AzureNotificationsHandler";
	
	public static final int NOTIFICATION_ID = 1;
	private NotificationManager mNotificationManager;
	NotificationCompat.Builder builder;
	Context ctx;
	
	@Override
	public void onReceive(Context context, Bundle bundle) {
	    
		Log.d(TAG, "Message received");
		
		ctx = context;
	    String title = bundle.getString("msg");
	    String sentUserid = bundle.getString("userid");
	    String coords = bundle.getString("coords");
		String[] tokens = coords.split(";");
		if( tokens.length != 2 ) 
			return;

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		String myUserid = sharedPrefs.getString("userid", "") 
				+ ";" 
				+ sharedPrefs.getString("registrationProvider", "");
		
		// Do not receive from yourself
		if( myUserid.length() != 0
			&& myUserid.equals(sentUserid) ) { // current user == sending user
				return;
		}
	    
		startExternalActivity(context, tokens[0], tokens[1], title, sentUserid);
		
	    sendNotification(title);

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
	
	private void startGoogleMaps(Context context,
								String lat,
								String lon,
								String title,
								String userid)
	{
		try{
			StringBuilder sb = new StringBuilder("geo:0,0?q=");
			sb.append(lat);
			sb.append(",");
			sb.append(lon);
			sb.append("(" + title + " from " + userid + ")");
			
			String url = sb.toString();
			
			Intent gmIntent = new Intent(android.content.Intent.ACTION_VIEW, 
				    //Uri.parse("http://maps.google.com/maps?daddr=32.072072072072075,34.8716280366431456&mode=driving"));
					//Uri.parse("geo:0,0?q=32.072072072072075,34.8716280366431456(Reported Place)")
					Uri.parse( url )
					);
			gmIntent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
			gmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(gmIntent);
			
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
