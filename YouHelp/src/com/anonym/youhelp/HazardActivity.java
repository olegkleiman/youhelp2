package com.anonym.youhelp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class HazardActivity extends Activity {

	private Location currentLocation;
	private String userid;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_hazard);
		
		Bundle extras = getIntent().getExtras();
		userid = extras.getString("userid");
		
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		String locationProvider = LocationManager.NETWORK_PROVIDER;
		// Or use LocationManager.GPS_PROVIDER

		currentLocation = locationManager.getLastKnownLocation(locationProvider);
		
		ImageButton btnSend = (ImageButton) findViewById(R.id.btnSend);
		btnSend.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				
				if( isNetworkAvailable() ) {
					
					String serviceURL = getString(R.string.send_toast_service_url);
					// Should be something like http://youhelp.cloudapp.net/YouHelpService.svc/sendtoast?title=;
					
					StringBuilder sb = new StringBuilder(serviceURL); 
					sb.append("?title=");
					sb.append("Help");//strTilte);
			  	 	
					sb.append("&subtitle=");
					
					double lat = currentLocation.getLatitude();
			   	    double lon = currentLocation.getLongitude();
			   	 	String strCurrentLocation = String.format(Locale.US, "%.13f;%.13f", lat, lon);
					
					sb.append(strCurrentLocation);
					sb.append("&tags=null");
					
					sb.append("&userid=");
					sb.append(userid);
					
					String uri = sb.toString();
					SendMessageAsyncTask sendTask = new SendMessageAsyncTask();
					//sendTask.ParentActivity = this;
					sendTask.execute(uri);
					
					finish();
				}
				else{
					
					AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(HazardActivity.this);
					
			   	 	dlgAlert.setMessage("Yau are not connected to network");
			   	 	dlgAlert.setTitle("YouHelp");
			   	 	dlgAlert.setPositiveButton("OK", null);
			   	 	dlgAlert.setCancelable(true);
			   	 	dlgAlert.create().show();
				}
				
				
			}
			
		});
	}

    private boolean isNetworkAvailable() {
    	
    	ConnectivityManager connectivityManager 
    	      = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    	return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.hazard, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	class SendMessageAsyncTask extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... uri) {

			HttpClient httpclient = new DefaultHttpClient();
			String responseString = null;

	   	 	try{
	   	 		
	   	 		HttpPost httpGet = new HttpPost(uri[0]);
	   	 		HttpContext localContext = new BasicHttpContext();
	   	 		
	   	 		HttpResponse response = httpclient.execute(httpGet, localContext);
	   	 		StatusLine statusLine = response.getStatusLine();
	   	 		
	   	 		if(statusLine.getStatusCode() == HttpStatus.SC_OK){
	   	 			ByteArrayOutputStream out = new ByteArrayOutputStream();
	   	 			response.getEntity().writeTo(out);
	   	 			out.close();
	   	 			responseString = out.toString();
	   	 		}else{
	   	 			//Closes the connection.
	   	 			response.getEntity().getContent().close();
	   	 			throw new IOException(statusLine.getReasonPhrase());
	   	 		}
	   	 	}catch(Exception e){
   	 		
   	        	e.printStackTrace();
   	        }
	   	 	
	   	 	return responseString;
		}
		
	}
}
