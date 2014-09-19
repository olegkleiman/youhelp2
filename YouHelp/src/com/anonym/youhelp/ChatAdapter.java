package com.anonym.youhelp;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.anonym.youhelp.media.YHMediaPlayer;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ChatAdapter extends ArrayAdapter<YHMessage> {

	private static final String LOG_TAG = "youhelp";
	Context context; 
    int layoutResourceId;    
    List<YHMessage> data = new ArrayList<YHMessage>();
    String myUSerID;
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private YHMediaPlayer yhPlayer;
    
	public ChatAdapter(Context context, 
						int layoutResourceId, 
						List<YHMessage> data, 
						String userID) 
	{
        super(context, layoutResourceId, data);
        
	    this.layoutResourceId = layoutResourceId;
	    this.context = context;
	    this.data = data;
	    this.myUSerID = userID;
	    
	    yhPlayer = new YHMediaPlayer(); 

	}
	
	@Override
	public void add(YHMessage message) {
		//data.add(message);
		super.add(message); // this call triggers the re-binding of adapter (including getView() invocations )
	}
	
	public int getCount() {
		return this.data.size();
	}
	
	public YHMessage getItem(int index) {
		return this.data.get(index);
	}
	
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        
		View row = convertView;
        ChatHolder holder = null;
        final YHMessage replica = this.getItem(position);
        if( replica == null ) {
        	Log.e(LOG_TAG, "Unable to get yhMessage corresponding to position " + position);
        	return null;
        }
        
        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            
            holder = new ChatHolder();
            
            holder.imgIcon = (ImageView)row.findViewById(R.id.imgIcon);
            holder.txtView = (TextView)row.findViewById(R.id.txtTitle);
            holder.background = (LinearLayout)row.findViewById(R.id.chatItemRowBackground);
            holder.playButton = (ImageButton)row.findViewById(R.id.btnPlayMessage); 
        	holder.playButton.setOnClickListener(new View.OnClickListener() { 
        		   
        		@Override 
        		public void onClick(View view) { 
        		 
        			if( replica.hasVoiceAttachement() ){
        				
        				String blobURL = replica.getBlobURL();
        				Log.i(LOG_TAG, "Playing " + blobURL);
        				Uri blogUri = Uri.parse(blobURL);
        				
        				yhPlayer.onPlay(context, blogUri, (ImageButton)view);
        			}

        		}; 
        	}); 
            
            row.setTag(holder);
        }
        else
        {
            holder = (ChatHolder)row.getTag();
        }
        
        
        
    	if( replica.hasVoiceAttachement ()) { 
    		holder.playButton.setVisibility(View.VISIBLE); 
    	} 
    	else { 
    		holder.playButton.setVisibility(View.GONE); 
    	} 
        
        Date dateCreated = replica.getDateCreated();
        
		//String strDate = dateFormat.format(dateCreated);
        //String strText = replica.getContent() + "\n" + strDate;
        // !!!
        holder.txtView.setText(replica.getContent());
        holder.imgIcon.setImageResource(replica.icon);
        
        if( //!replica.getUserId().isEmpty()  &&
        		myUSerID.equals( replica.getUserId()) ) {
        	
        	holder.txtView.setGravity(Gravity.RIGHT);
        	holder.txtView.setBackgroundResource(R.drawable.bubble_green);
            holder.background.setGravity(Gravity.RIGHT);
            
        } else {
        
        	holder.txtView.setGravity(Gravity.LEFT);
        	holder.txtView.setBackgroundResource(R.drawable.bubble_yellow);
        	holder.background.setGravity(Gravity.LEFT);
        }
        
        return row;
    }
	
	static class ChatHolder
    {
        ImageView imgIcon;
        TextView txtView;
        LinearLayout background;
        ImageButton playButton; 
    }

}
