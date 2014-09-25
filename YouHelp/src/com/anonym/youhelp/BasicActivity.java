package com.anonym.youhelp;

import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class BasicActivity extends Activity 
							implements LocationListener{

	String LOG_TAG = "youhelp";
	
	private Location location;
	public Location getMyLocation() {
		return location;
	}
	
    private boolean isNetworkAvailable() {
    	
    	ConnectivityManager connectivityManager 
    	      = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    	return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_basic);
		
		LocationManager locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
		location = getLocation(locationManager);
		if( location == null ) {

			// Bound to updates to at most 3 sec. and for 
			// geographical accuracies of more that 300 meters
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 
												  3 * 1000, 
												  300,
												  this);
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		this.location = location;
	}

	@Override
	public void onProviderDisabled(String provider) {}

	@Override
	public void onProviderEnabled(String provider) {}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}
	
	private Location getLocation(LocationManager lm) {

		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		String bestProvider = lm.getBestProvider(criteria, true);
		Location location = lm.getLastKnownLocation(bestProvider);
		if( location != null) {
			Log.i(LOG_TAG, "Location provided by " + bestProvider + location.getLatitude() + ";" + location.getLongitude());
		} else {
			Log.e(LOG_TAG, "Unable get last known location from provider " + bestProvider);
		}
		return location;
	}
}
