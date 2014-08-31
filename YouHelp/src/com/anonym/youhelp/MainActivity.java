package com.anonym.youhelp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.facebook.AppEventsLogger;


public class MainActivity extends FragmentActivity implements AnimationListener {

	private static final String TAG = "YouHelp v 2";
    
	static final int REGISTER_USER_REQUEST = 1; 
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        
        // Set background for layout
        LinearLayout mainLayout = (LinearLayout)findViewById(R.id.main_layout);
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
		
		InitStuff();
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
				imageHelp.startAnimation(animationMove);
				
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

    }
  
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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

	@Override
	public void onAnimationEnd(Animation animation) {
        
        LinearLayout mainLayout = (LinearLayout)findViewById(R.id.main_layout);
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
