package com.anonym.youhelp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

import com.anonym.youhelp.HazardActivity.SendMessageAsyncTask;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.support.v4.app.FragmentActivity;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;

public class MapActivity extends FragmentActivity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener,
		LocationListener,
		OnItemClickListener
{

	private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place/details"; 
	private static final String OUT_JSON = "/json";	 
	private static final String API_KEY = "AIzaSyBJryLCLoWeBUnSTabBxwDL4dWO4tExR1c"; 
	
	private GoogleMap gMap;
	private LocationClient locationClient;
	private ArrayList<ReportedPlace> reportedLocations = new ArrayList<ReportedPlace>();
	private static final String TAG = "com.anonym.youhelp.mapactivity";
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	
	private Location myLocation = new Location("");
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
		
		String userid, coords, title;
		
		Intent requestingIntent = getIntent();
		if( requestingIntent != null )
		{
			if( requestingIntent.hasExtra("userid") )
				userid = requestingIntent.getStringExtra("userid");
			if( requestingIntent.hasExtra("coords") )
				coords = requestingIntent.getStringExtra("coords");
			if( requestingIntent.hasExtra("title") )
				title = requestingIntent.getStringExtra("title");	
		}
		
		AutoCompleteTextView autoCompView = (AutoCompleteTextView) findViewById(R.id.autocomplete);
	    autoCompView.setAdapter(new PlacesAutoCompleteAdapter(this, R.layout.search_map_list_item));
	    autoCompView.setOnItemClickListener(this);
	   
	    ensureMap();
	    
		gMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		gMap.getUiSettings().setMyLocationButtonEnabled(true);
		gMap.setBuildingsEnabled(true);	
		gMap.setMyLocationEnabled(true);
		
		gMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener(){ 
			
			@Override 
			public void onInfoWindowClick(Marker marker) { 
			 
				LatLng destination = marker.getPosition(); 
				String snippet = marker.getSnippet(); 
				snippet.trim(); 
				 
				// demo primarily for emulator 
				if( myLocation.getLatitude() == 0  
					|| myLocation.getLongitude() == 0) {  
						myLocation.setLatitude(Double.parseDouble("32.0816110")); 
						myLocation.setLongitude(Double.parseDouble("34.7827041")); 
				} 
				 
				LatLng myLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude()); 
				 
				GMapV2Direction md = new GMapV2Direction(); 
				md.drawDirectitions(gMap, myLatLng, destination,  
				GMapV2Direction.MODE_DRIVING, 
				// TODO : detect used language 
				// List of supported languages : https://spreadsheets.google.com/pub?key=p9pdwsai2hDMsLkXsoM05KQ&gid=1); 
				"iw");  
				
				// Send Notification
				
				String serviceURL = getString(R.string.send_toast_service_url);
				StringBuilder sb = new StringBuilder(serviceURL); 
				sb.append("?title=");
				sb.append("Ride");
		  	 	
				sb.append("&subtitle=");
				
				double lat = myLocation.getLatitude();
		   	    double lon = myLocation.getLongitude();
		   	 	String strCurrentLocation = String.format(Locale.US, "%.13f;%.13f", lat, lon);
				
				sb.append(strCurrentLocation);
				sb.append("&tags=null");
				
				sb.append("&userid=");
				SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				String userid = sharedPrefs.getString("registrationProvider", "")
						+ ":"
						+ sharedPrefs.getString("userid", "");
				sb.append(userid);
				
				String uri = sb.toString();
			 
				SendMessageAsyncTask sendTask = new SendMessageAsyncTask();
				sendTask.execute(uri);
			} 
			
		});
		
		LocationManager locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
		
		// Bound to updates to at most 3 sec. and for 
		// geographical accuracies of more that 300 meters
		locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 
											  3 * 1000, 
											  300,
											  this);
		
		// Check device for Play Services APK
		if( checkPlayServices() ) {
			
			try{
				// Location client depends on GooglePlay services and used to get the first location fix as fast as possible.
				// Further updates will come from previously initialized LocationManager
				locationClient = new LocationClient(this, this, this);
				locationClient.connect();
			}
			catch(Exception ex){
				String msg = ex.getMessage();
				Log.e(TAG, msg);
			}
		}
	}

	private boolean checkPlayServices(){
		try
		{
			int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
			if( resultCode != ConnectionResult.SUCCESS ) {
				if( GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
					GooglePlayServicesUtil.getErrorDialog(resultCode, this, 
							PLAY_SERVICES_RESOLUTION_REQUEST).show();
				}
				else {
					Log.i(TAG, "This device is not supported");
					finish();
				}
				
				return false;
			}
		}
		catch(Exception ex){
			
			ex.getMessage();
		}

		return true;
	}
	
	@SuppressLint("NewApi")
	private void ensureMap() {
	    
		try{
		// Do a null check to confirm that we have not already instantiated the map.
	    if (gMap == null) {
	        gMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
	                            .getMap();
	        // Check if we were successful in obtaining the map.
	        if (gMap != null) {
	            // The Map is verified. It is now safe to manipulate the map.
	        	

	        }
	    }
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	public void setMyLocation(Location location){
		myLocation = location;
	}
	
	public void showLocations(){
		
		gMap.clear();
		
		if( myLocation != null )
			showME(myLocation);
		
		if( reportedLocations != null) {
			for(ReportedPlace place: reportedLocations){
				showReportedPlace(place, place.getTitle(), place.getUserID());
		}}
		

	}
	
	private void showReportedPlace(Location location, String title, String userid){
		showMarker(location, title, "Reported by " + userid, BitmapDescriptorFactory.HUE_ROSE);
	}
	
	private void showME(Location location){
		
//		Location tempLoc = new Location("");
//		tempLoc.setLatitude(32.072072072);
//		tempLoc.setLongitude(34.871628036);
		//showMarker(tempLoc, "She is there", "", BitmapDescriptorFactory.HUE_RED);

		//String currentLanguage = Locale.getDefault().getLanguage();
		
		String youAreHere =  getResources().getString(R.string.you_are_here);
		String saySomething = getResources().getString(R.string.say_something);
		showMarker(location, 
				youAreHere, 
				saySomething, BitmapDescriptorFactory.HUE_AZURE);
	}
	
	@Override 
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) { 
	 
		FoundPlace place = (FoundPlace) adapterView.getItemAtPosition(position); 
		
		StringBuilder sb = new StringBuilder(PLACES_API_BASE + OUT_JSON); 
		sb.append("?placeid=" + place.getPlaceID());
		sb.append("&key=" + API_KEY); 
		 
		CallPlaceDetails task = new CallPlaceDetails(); 
		task.setPlaceDescription(place.getDescription());
		task.execute(sb.toString()); 

		Toast.makeText(this, place.getDescription(), Toast.LENGTH_SHORT).show(); 
	} 
	
	private void showMarker(Location location, String title, String snippet,  float color){
		if( location == null ) {
			Log.i(TAG, "Location passed to showMarker() is invalid");
			return;
		}else if( gMap == null ) {
			Log.i(TAG, "Map is not ready when calling to showMarker()");
			return;
		}
	
		final LatLng ME = new LatLng(location.getLatitude(),
									location.getLongitude());
		
		final int zoomLevel = 16;
		// Move the camera instantly to the current location with a zoom.
		gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ME, zoomLevel));
		
		// Zoom in, animating the camera.
		gMap.animateCamera(CameraUpdateFactory.zoomIn());
		// Zoom out to specified zoom level, animating with a duration of 2 seconds.
		gMap.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel), 2000, null);
		
		gMap.setInfoWindowAdapter( new InfoWindowAdapter() {

			@Override
			public View getInfoContents(Marker marker) {
					
				View content = getLayoutInflater().inflate(R.layout.infoview, null);
				
				TextView info = ((TextView) content.findViewById(R.id.txtInfoWindowTitle));
				if( info != null) {

					String title = marker.getTitle();
					
					// Span-able string allows to edit the formatting of the text.
					SpannableString titleText = new SpannableString(title);
					titleText.setSpan(new ForegroundColorSpan(Color.BLUE), 0, 
										titleText.length(), 0);
					info.setText(titleText);
					
					TextView txtSnippet = ((TextView) content.findViewById(R.id.txtInfoWindowEventType));
					txtSnippet.setText(marker.getSnippet());
				}

				return content;
			}

			// If getInfoWindow() returns null, getInfoContents() is used
			// for the contents of the window
			@Override
			public View getInfoWindow(Marker marker) {
				
				return null;
			}
		});
		
		Marker meMarker = gMap.addMarker(new MarkerOptions()
        		.position(ME)
        		.title(title)
        		.snippet(snippet)
        		.icon(BitmapDescriptorFactory.defaultMarker(color)));
		meMarker.showInfoWindow();	
	}
	
	
	private class CallPlaceDetails extends AsyncTask<String, //the type of the parameters sent to the task upon execution.  
												Object, // the type of the progress units published during the background computation. 
												String> // the type of the result of the background computation. 
	{ 
		private String PlaceDescription;

		public void setPlaceDescription(String desc){
			PlaceDescription = desc;
		}


		// This method is called on main thread UI
		@Override 
		protected void onPostExecute(String result) { 

			try { 
				// Create a JSON object hierarchy from the results 
				JSONObject jsonObj = new JSONObject(result.toString()); 
				JSONObject jsonObjResult = jsonObj.getJSONObject("result");  
				JSONObject jsonObjGeometry = jsonObjResult.getJSONObject("geometry"); 
				JSONObject jsonObjLocation = jsonObjGeometry.getJSONObject("location"); 
				
				String strLat = jsonObjLocation.getString("lat"); 
				String strLon = jsonObjLocation.getString("lng"); 
				
				Location location = new Location("");
				location.setLatitude(Double.parseDouble(strLat));
				location.setLongitude(Double.parseDouble(strLon));
				showMarker(location, 
					PlaceDescription, 
					"", 
					BitmapDescriptorFactory.HUE_RED);

			}catch(Exception ex){ 

		String strMessage = ex.getMessage(); 
		strMessage.trim(); 
		ex.printStackTrace(); 
		} 

		} 

		@Override 
		protected String doInBackground(String... params) { 

			HttpURLConnection conn = null; 

			try { 
			
				String strURL = params[0]; 
				URL url = new URL(strURL); 
				conn = (HttpURLConnection) url.openConnection(); 
				InputStreamReader in = new InputStreamReader(conn.getInputStream()); 
				
				StringBuilder jsonResults = new StringBuilder(); 
				
				// Load the results into a StringBuilder 
				int read; 
				char[] buff = new char[1024]; 
				while ((read = in.read(buff)) != -1) { 
					jsonResults.append(buff, 0, read);	 
				} 

				return jsonResults.toString(); 

			} catch(Exception ex) { 
				String strMessage = ex.getMessage(); 
				strMessage.trim(); 
				ex.printStackTrace(); 
				
				return ""; 
			}	 

		} 

	}


	@Override
	public void onLocationChanged(Location location) {
		
		try{

			Log.i(TAG, "Location fix received");
			
			if( location != null ){

				String provider = location.getProvider();
				
				String s = "Provider: " + provider +  "\n";
				s += "\tLatitude:  " + location.getLatitude() + "¡\n";
		        s += "\tLongitude: " + location.getLongitude() + "¡\n";
		        
		        if( location.hasSpeed() )
		        	s += "\tSpeed: " + location.getSpeed() + "\n";
		        else
		        	s += "\tNo speed reported";
		        
		        Log.i(TAG, s);
		        
				Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
				
				setMyLocation(location);
				showLocations();
				
				/* ok
				String gcmRegID = this.getGCMRegistrationID(this);
				if( !gcmRegID.isEmpty() ) {
					registerWithNotificationHubs(location, gcmRegID);
					
				}
				 */
			}

		}catch(Exception ex){
			ex.printStackTrace();
		}		
	}

	@Override
	public void onStart() {
		super.onStart();
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnected(Bundle dataBundle) {
		//Toast.makeText(this, "Connected to GooglePlay", Toast.LENGTH_SHORT).show();
		Log.i(TAG, "Connected to GooglePlay");
		
		Location currentLocation = locationClient.getLastLocation();
		if( currentLocation != null){
/*			
			String gcmRegID = this.getGCMRegistrationID(this);
			if( !gcmRegID.isEmpty() ) {
				registerWithNotificationHubs(currentLocation, gcmRegID);
			}
*/			
			setMyLocation(currentLocation);
			
			showLocations();
		}
		
	}

	@Override
	public void onDisconnected() {
	     
		// Display the connection status
        Toast.makeText(this, "Disconnected from GooglePaly. Please re-connect.",
                Toast.LENGTH_SHORT).show();
		
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
