package com.anonym.youhelp;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class AzureBroadcastReceiver extends BroadcastReceiver {
	
	private static final String TAG = "AzureBroadcastReceiver";
	public static final int NOTIFICATION_ID = 1;
	Context ctx;	
	String userId; 
	private NotificationManager mNotificationManager;
	
	public AzureBroadcastReceiver() {
	}

	public void setUserID(String userID){
		this.userId = userID;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {

		Log.d(TAG, intent.getAction());
		
		try {
			
			if( !intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE")) 
				return;
			
			Bundle extras = intent.getExtras();
			if( extras == null ) {
				Toast.makeText(context, "Empty Notification Received", Toast.LENGTH_SHORT).show();
				return;
			}
			
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
			if( sharedPrefs == null )
				return;
			
			String prefUserID = sharedPrefs.getString("prefUsername", "");
			// Do not receive from yourself
			if( prefUserID.length() != 0
				&& prefUserID.equals(userId) ) { // current user == sending user
					return;
			}
			
			GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
			ctx = context;
			
			String messageType = gcm.getMessageType(intent);
			
	//		if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
	//            sendNotification("Send error: " + intent.getExtras().toString());
	//		} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
	//            sendNotification("Deleted messages on server: " + 
	//                    intent.getExtras().toString());
	//		} else {
	//			sendNotification(title);
	//		}
	//		
	//		setResultCode(Activity.RESULT_OK);
			}
//			catch ( ActivityNotFoundException ex  )
//			{
//				Intent wazeIntent =
//						new Intent( Intent.ACTION_VIEW, Uri.parse( "market://details?id=com.waze" ) );
//				wazeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				context.startActivity(wazeIntent);
//			}	
			catch ( Exception ex  ) {
				Toast.makeText(context, ex.getMessage(), Toast.LENGTH_SHORT).show();
			}
	}
	
	private void persistMessage(Context context, String content, String userid){
	}
	
	private void sendNotification(String msg) {
		mNotificationManager = (NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
		
		NotificationCompat.Builder mBuilder =
		          new NotificationCompat.Builder(ctx)
		          .setSmallIcon(R.drawable.ic_launcher)
		          .setVibrate(new long[] { 500, 500 })
		          .setContentTitle("You Help")
		          .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
		          .setStyle(new NotificationCompat.BigTextStyle()
		              			.bigText(msg))
		          .setContentText(msg);
		
		//mBuilder.setContentIntent(contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}
}
