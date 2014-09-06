package com.anonym.youhelp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

public class ChatActivity extends Activity {

	private static final String TAG = "com.anonym.youhelp.chatactivity";
	String toUserid = "", provider = "";
	private String myUserID;
	private YHDataSource datasource;
	private ChatAdapter chatAdapter;
	
	ImageView profilePictureView = null;
	ImageView hazardPictureView = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		
		Intent requestingIntent = getIntent();
		
		if( requestingIntent != null )
		{
				if( requestingIntent.hasExtra("userid") ) {
					toUserid = requestingIntent.getStringExtra("userid");
				}
				if( requestingIntent.hasExtra("provider") ) {
					provider = requestingIntent.getStringExtra("provider");
				}
				
				profilePictureView = (ImageView)findViewById(R.id.imageProfilePicture);
				GetProfileTask task = new GetProfileTask(); 
				task.execute(toUserid); 
				
				hazardPictureView = (ImageView)findViewById(R.id.imageViewHazardPic);
				GetBlobTask blobTask = new GetBlobTask(this);
				blobTask.execute("pictures", "1475946282659875_20140905_215401-888053217.jpg");
		}
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		myUserID = sharedPrefs.getString("prefUsername", "");
		

		
		ListView messagesList = (ListView)findViewById(R.id.lvChatRoom);
		
		try {
			if( datasource == null )
				datasource = new YHDataSource(this);
		
			datasource.open();
			final List<YHMessage> messages = datasource.getMessagesOfUser(toUserid);
			chatAdapter = new ChatAdapter(this, 
										R.layout.chatroom_item_row,
										messages,
										myUserID);
			messagesList.setAdapter(chatAdapter);
			
		}catch(Exception ex){
			Log.e(TAG, ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	public void onCallMeChat(View view){
		
	}
	
	public void onDelteChat(View view){
		datasource.deleteAllMessagesOfUser(toUserid);
		chatAdapter.clear();
	}
	
	public void onSendChatMessage(View view) {
		
		EditText txtMessage = (EditText)findViewById(R.id.txtMessage);
		String strMessage = txtMessage.getText().toString();
		if( strMessage.isEmpty() )
			return;
		
		try {
			
			persistMessage(strMessage);
			txtMessage.setText("");
			
			YHMessage message = new YHMessage(0, strMessage);
			message.setUserID(myUserID);
			if( chatAdapter != null) 
				chatAdapter.add(message);
			
			String serviceURL = getString(R.string.send_chatmessage_url);
			// Should be something like http://youhelp.cloudapp.net/YouHelpService.svc/sendchatmessage?content=;
			
			StringBuilder sb = new StringBuilder(serviceURL); 
			sb.append("?content=");
			sb.append(strMessage);
	  	 	
			sb.append("&fromuserid=");
			sb.append(myUserID);
			
			sb.append("&touserid=");
			sb.append(toUserid);
			
			String uri = sb.toString();
			
			SendChatMessageAsyncTask sendTask = new SendChatMessageAsyncTask(this);
			sendTask.execute(uri);
			
		}catch(Exception ex){
			
			Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void persistMessage(String content){

		if( datasource == null)
			datasource = new YHDataSource(this);
		
		datasource.open();
		 
		Date date = new Date();
		datasource.createYHMessage(content, this.myUserID, date, toUserid);
		datasource.close();
	
	}
	
	public class SendChatMessageAsyncTask extends AsyncTask<String, String, Boolean> {
	
		Exception error;
		
		Context context;
	
		public SendChatMessageAsyncTask(Context ctx){
			context = ctx;
		}
		
		@Override
	    protected void onPostExecute(Boolean result) {
	       super.onPostExecute(result);
	       
	       if( error != null )
	    	   Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
	    }
		
		@Override
		protected Boolean doInBackground(String... uri) {

	   	 	HttpClient httpclient = new DefaultHttpClient();
	   	 	
	   	 	try{
	   	 		HttpPost httpGet = new HttpPost(uri[0]);
	   	 		HttpContext localContext = new BasicHttpContext();
	   	 		
	   	 		HttpResponse response = httpclient.execute(httpGet, localContext);
	   	 		StatusLine statusLine = response.getStatusLine();
	   	 		if(statusLine.getStatusCode() == HttpStatus.SC_OK){
	   	 			ByteArrayOutputStream out = new ByteArrayOutputStream();
	   	 			response.getEntity().writeTo(out);
	   	 			out.close();
	   		   	 	String responseString = out.toString();
	   		   	 	responseString.trim();
	   	 			
	   	 			return true;
	   	 		}else{
	   	 			//Closes the connection.
	   	 			response.getEntity().getContent().close();
	   	 			error = new IOException(statusLine.getReasonPhrase());
	   	 		}

	   	 	}
	   	 	catch(final Exception ex){
	   	 		error = ex;
			}
	   	 	
	   	 	return false;

		}
	}
	
	private class GetBlobTask extends AsyncTask<String, String, ByteArrayOutputStream>
	{
		Context context;
		Exception error;
		
		public static final String storageConnectionString = 
			    "DefaultEndpointsProtocol=http;" + 
			    "AccountName=youhelpstorage;" + 
			    "AccountKey=dtpTqukoGje8FSnSvUBc/of+6Y3FQZRi7eS2+PTanCnAglBBExnsvXjxTjZQxiROUWJbZZijlZ97WR7/l6MDMA==";

		
		public GetBlobTask(Context ctx){
			context = ctx;
		}
		
		@Override
	    protected void onPostExecute(ByteArrayOutputStream result) {
			try{
			if( error != null ) {
            	Toast.makeText(context, error.getMessage(), 
    						Toast.LENGTH_LONG).show();
			}else if( result != null ) {

				InputStream decodedInput = new ByteArrayInputStream(result.toByteArray());
				
//   			 	// Get the dimensions of the View
//    		    int targetW = hazardPictureView.getWidth();
//    		    int targetH = hazardPictureView.getHeight();
//    		    
//    		    // Get the dimensions of the bitmap
//    		    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
//    		    bmOptions.inJustDecodeBounds = true;
//    		    Bitmap bitmap = BitmapFactory.decodeStream(decodedInput, bmOptions);
    		    
				
				Bitmap bitmap = BitmapFactory.decodeStream(decodedInput);
				hazardPictureView.setImageBitmap(bitmap);
				
				//Drawable d = Drawable.createFromStream(decodedInput,"src");
				//hazardPictureView.setImageDrawable(d);

			}
			} catch(OutOfMemoryError e){
				Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
			} catch (Exception ex) {
				Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
		
		@Override
		protected ByteArrayOutputStream doInBackground(String... params) {
			try {
				
				String containerName = params[0];
				String blobName = params[1];
				
				// Retrieve storage account from connection-string.
			    CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
				
			    // Create the blob client.
			    CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
			    
			    // Retrieve reference to a previously created container.
			    CloudBlobContainer container = blobClient.getContainerReference(containerName);
			    
			    CloudBlockBlob blob = container.getBlockBlobReference(blobName);
			    
			    ByteArrayOutputStream baos = new ByteArrayOutputStream();
			    blob.download(baos);
			    
			    // Using pipelines seems to be more effective
//			    PipedOutputStream po = new PipedOutputStream();
//			    PipedInputStream pi = new PipedInputStream(po);
//			    pi.read(baos.toByteArray());
//			    return pi;
			    
			    return baos;
			    
			} catch(Exception e) {
		    	error = e;
		    	return null;
			}

		}
		
	}

	private class GetProfileTask extends AsyncTask<String, Object, Bitmap> 
	{ 
		// This method is called on main thread UI
		@Override 
		protected void onPostExecute(Bitmap result) { 
		
			if( result != null 
				&& profilePictureView != null )
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
	
	}
