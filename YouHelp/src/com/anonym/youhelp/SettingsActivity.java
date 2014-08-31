package com.anonym.youhelp;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;

	@SuppressLint("NewApi") 
	public class SettingsActivity extends Activity 
				implements OnItemSelectedListener
	{

	ActionBar.Tab tab1, tab2, tab3;
	Fragment fragmentTab1 = new FragmentTabUser();
	Fragment fragmentTab2 = new FragmentTabSOS();
	Fragment fragmentTab3 = new FragmentTabSystem();
	
	private UiLifecycleHelper uiHelper;
	
	EditText txtPhoneNumber;
	
	EditText txtSOS1;
	EditText txtSOS2;
	
    private Session.StatusCallback callback = new Session.StatusCallback() {
        
    	@Override
        public void call(Session session, SessionState state, Exception exception) {
            
        }
    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_layout);
		
		uiHelper = new UiLifecycleHelper(SettingsActivity.this, callback);
	    uiHelper.onCreate(savedInstanceState);
		
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		tab1 = actionBar.newTab().setText("Registration");
        tab2 = actionBar.newTab().setText("SOS");
        tab3 = actionBar.newTab().setText("General");
        
        tab1.setTabListener(new MyTabListener(fragmentTab1));
        tab2.setTabListener(new MyTabListener(fragmentTab2));
        tab3.setTabListener(new MyTabListener(fragmentTab3));
        
        actionBar.addTab(tab1);
        actionBar.addTab(tab2);
        actionBar.addTab(tab3);
	}

	@Override
	public void onStop(){
		super.onStop();

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		  
		SharedPreferences.Editor editor = sharedPrefs.edit();
		
		String strPhoneNumber = txtPhoneNumber.getText().toString();
		editor.putString("userPhoneNumber", strPhoneNumber);
		
		// txtSOS views can be null is 'SOS' tab was no activated
		if( txtSOS1 != null ){
			String sos1 = txtSOS1.getText().toString();
			//if( sos1.length() != 0 )
				editor.putString("sos1", sos1);
		}
		
		if( txtSOS2 != null ) {
			String sos2 = txtSOS2.getText().toString();
			//if( sos2.length() != 0 )
				editor.putString("sos2", sos2);
		}
		
		editor.commit();
	  
	}
	
	@Override
	public void onItemSelected(AdapterView<?> parent, 
			View view, 
            int pos, 
            long id) 
	{
		String str = (String) parent.getItemAtPosition(pos);
		int nAppCode = 2;
		if( str.contains("Waze"))
			nAppCode = 1;
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putInt("mapsApp", nAppCode);
		editor.commit();
		
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings, menu);
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
	
	public class FragmentTabUser extends Fragment {
		
		ImageView profilePictureView = null;
		
		private class GetProfileTask extends AsyncTask<String, //the type of the parameters sent to the task upon execution.  
													Object, // the type of the progress units published during the background computation. 
													Bitmap> // the type of the result of the background computation. 
		{ 
				// This method is called on main thread UI
				@Override 
		        protected void onPostExecute(Bitmap result) { 
					
					if( result != null )
						profilePictureView.setImageBitmap(result);
				}
			
				@Override
				protected Bitmap doInBackground(String... params) {
					  
					URL fbAvatarUrl = null;
					Bitmap fbAvatarBitmap = null;
					
					try{
						  
						String userid = params[0];
						  
						fbAvatarUrl = new URL("https://graph.facebook.com/"+userid+"/picture?type=normal");
						fbAvatarBitmap = BitmapFactory.decodeStream(fbAvatarUrl.openConnection().getInputStream());
	 
					}catch (MalformedURLException e) {
		                e.printStackTrace();
		            } catch (IOException e) {
		                e.printStackTrace();
		            }
						
					return fbAvatarBitmap;
				}
		}
		
		public View onCreateView(LayoutInflater inflater, ViewGroup container, 
		                           Bundle savedInstanceState)
		  {

			  SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			  String username = sharedPrefs.getString("prefUsername", "");

			  String lastUsername = sharedPrefs.getString("lastUsername", "");
			  
			  View view = inflater.inflate(R.layout.tab_user, container, false);
			  
			  TextView txtUsername = (TextView) view.findViewById(R.id.txtUserFirstName);
			  txtUsername.setText(username);
			  
			  TextView txtLastUsername = (TextView) view.findViewById(R.id.txtUserLastName);
			  txtLastUsername.setText(lastUsername);
			  
			  profilePictureView = (ImageView) view.findViewById(R.id.userProfilePicture);
			  
			  txtPhoneNumber = (EditText)view.findViewById(R.id.txtPhoneNumber);
			  String strPhoneNumber = sharedPrefs.getString("userPhoneNumber", "+972");
			  
			  txtPhoneNumber.setText(strPhoneNumber);

			  String userid = sharedPrefs.getString("userid", "");
			  GetProfileTask task = new GetProfileTask(); 
			  task.execute(userid); 

			  return view;
		  }
		}
	
	public class FragmentTabSOS extends Fragment {
		  public View onCreateView(LayoutInflater inflater, ViewGroup container, 
		                           Bundle savedInstanceState){
			View view = inflater.inflate(R.layout.tab_sos, container, false);

			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			String sos1 = sharedPrefs.getString("sos1", "");
			String sos2 = sharedPrefs.getString("sos2", "");
			
			txtSOS1 = (EditText)view.findViewById(R.id.txtSOSnumber1);
			txtSOS1.setText(sos1);
			
			txtSOS2 = (EditText)view.findViewById(R.id.txtSOSnumber2);
			txtSOS2.setText(sos2);

			return view;
		  }
		}
	
	public class FragmentTabSystem extends Fragment 
	{
		  public View onCreateView(LayoutInflater inflater, ViewGroup container, 
		                           Bundle savedInstanceState){
			View view = inflater.inflate(R.layout.tab_system, container, false);

			Spinner spinner = (Spinner) view.findViewById(R.id.mapsapp_spinner);

			// Create an ArrayAdapter using the string array and a default spinner layout
			ArrayAdapter<CharSequence> adapter = 
					ArrayAdapter.createFromResource(SettingsActivity.this,
											        R.array.mapsapps_array, 
											        R.layout.spinner_item);
											        //android.R.layout.simple_spinner_item);
			// Specify the layout to use when the list of choices appears
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			
			// Apply the adapter to the spinner
			spinner.setAdapter(adapter);
			
			spinner.setOnItemSelectedListener(SettingsActivity.this);
			
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			int nAppCode = sharedPrefs.getInt("mapsApp", 2);
			spinner.setSelection(nAppCode-1);

			return view;
		  }
	}
	
	@SuppressLint("NewApi") 
	public class MyTabListener implements ActionBar.TabListener {
		Fragment fragment;
		
		public MyTabListener(Fragment fragment) {
			this.fragment = fragment;
		}
		
	    public void onTabSelected(Tab tab, FragmentTransaction ft) {
			ft.replace(R.id.fragment_container, fragment);
		}
		
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			ft.remove(fragment);
		}
		
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// nothing done here
		}
	}
}
