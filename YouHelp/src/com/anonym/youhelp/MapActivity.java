package com.anonym.youhelp;

import java.util.ArrayList;

import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.GoogleMap;

import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AutoCompleteTextView;

public class MapActivity extends FragmentActivity {

	private GoogleMap gMap;
	private LocationClient locationClient;
	private ArrayList<ReportedPlace> reportedLocations = new ArrayList<ReportedPlace>();
	private static final String TAG = "com.anonym.youhelp.rideactivity";
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	
	private Location myLocation = new Location("");
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
		
		AutoCompleteTextView autoCompView = (AutoCompleteTextView) findViewById(R.id.autocomplete);
//	    autoCompView.setAdapter(new PlacesAutoCompleteAdapter(this, R.layout.list_item));
//	    autoCompView.setOnItemClickListener(this);
	   
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.map, menu);
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
}
