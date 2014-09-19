package com.anonym.youhelp.media;

import java.io.IOException;

import com.anonym.youhelp.R;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageButton;

public class YHMediaPlayer {
	
    private MediaPlayer mediaPlayer; 
    boolean mStartPlaying = true;
    private static final String LOG_TAG = "youhelp";
    
    public YHMediaPlayer() {
    	mediaPlayer = new MediaPlayer();
    }
    
    public void onPlay(Context context, Uri blobUri, ImageButton btnPlay) {
        if (mStartPlaying) 
            startPlaying(context, blobUri, btnPlay);
        else 
        	stopPlaying(btnPlay);
        
        mStartPlaying = !mStartPlaying;
    }
    
    public void onPlay(String fileName, final ImageButton btnPlay) {
        if (mStartPlaying) 
            startPlaying(fileName, btnPlay);
        else 
        	stopPlaying(btnPlay);
        
        mStartPlaying = !mStartPlaying;
    }

    private void stopPlaying(final ImageButton btnPlay) {

        btnPlay.setImageResource(R.drawable.sys_play_button);
    	
        mediaPlayer.stop();
        mediaPlayer.reset();
        //mPlayer.release();
        //mPlayer = null;
    }
    
    private void startPlaying(String fileName, final ImageButton btnPlay) {

        try {
        	mediaPlayer.setDataSource(fileName);
        	mediaPlayer.prepare();
        	mediaPlayer.start();

            btnPlay.setImageResource(R.drawable.sys_pause);
            
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        } catch(Exception ex) {
        	Log.e(LOG_TAG, ex.getMessage());
        }
    
    }
    
    private void startPlaying(Context context, Uri blobUri, final ImageButton btnPlay) {

        try {
        	mediaPlayer.setDataSource(context, blobUri);
        	mediaPlayer.prepare();
        	mediaPlayer.start();

            btnPlay.setImageResource(R.drawable.sys_pause);
            
    	    mediaPlayer.setOnCompletionListener(new OnCompletionListener(){
			   		     
    	    			@Override
			   		     public void onCompletion(MediaPlayer mp) { 
			   		    	 		stopPlaying(btnPlay);
			   		    	 		mStartPlaying = true;
			   		             }
			   		});
            
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        } catch(Exception ex) {
        	Log.e(LOG_TAG, ex.getMessage());
        }
    
    }
}
