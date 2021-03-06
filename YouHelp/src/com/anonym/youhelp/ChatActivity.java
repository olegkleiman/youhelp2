package com.anonym.youhelp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.anonym.youhelp.database.YHDataSource;
import com.anonym.youhelp.media.YHMediaPlayer;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

public class ChatActivity extends Activity {

	private static final String LOG_TAG = "com.anonym.youhelp.chatactivity";
	String toUserid = "", provider = "";
	private String myUserID;
	private YHDataSource datasource;
	private ChatAdapter chatAdapter;
	
	private MediaRecorder mRecorder;
	private YHMediaPlayer yhPlayer;

	String voiceFileName;
	boolean mStartRecording = true;
	ImageView profilePictureView = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		myUserID = sharedPrefs.getString("userid", "");
		
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
				
				ImageView hazardPictureView = (ImageView)findViewById(R.id.imageViewHazardPic);
				GetBlobTask blobTask = new GetBlobTask(this, hazardPictureView);
				blobTask.execute("pictures", "1475946282659875_20140905_215401-888053217.jpg");
		}
		
		mRecorder = new MediaRecorder();
		yhPlayer = new YHMediaPlayer();
		
		ImageButton btnRecordMessage = (ImageButton)findViewById(R.id.btnRecordMessage);
		
		btnRecordMessage.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {

					onRecord(mStartRecording, myUserID);
	                mStartRecording = !mStartRecording;

			}
		});
		
		final ImageButton btnPlay = (ImageButton)findViewById(R.id.btnPlay);
		btnPlay.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				yhPlayer.onPlay(voiceFileName, btnPlay);
				
			};
		});
		
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
			Log.e(LOG_TAG, ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	private String createVoiceFile(String userName) throws IOException {
	    
		File outputDir = this.getCacheDir();
		String fileName = getTempFileName(userName);
		File file = File.createTempFile(fileName, ".3gp", outputDir);
		return file.getAbsolutePath();

	}
	
	// Generates random file name 
	@SuppressLint("SimpleDateFormat") 
	private String getTempFileName(String userName) {
		
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return userName + timeStamp;
	}
    
    private void startRecording(String fileName) {

        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(fileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
    }
	
    private void onRecord(boolean start, String userID) {
      
    	try {
	    	if (start) {
	    		
					voiceFileName = createVoiceFile(userID);
		            startRecording(voiceFileName);
		            
		            ImageButton btnRecordMessage = (ImageButton)findViewById(R.id.btnRecordMessage);
		            btnRecordMessage.setImageResource(R.drawable.record_stop);
	
	        } else {
	        	
	            ImageButton btnRecordMessage = (ImageButton)findViewById(R.id.btnRecordMessage);
	            btnRecordMessage.setImageResource(R.drawable.record_start);
	        	
	            stopRecording();
	            
	            ImageButton btnPlay = (ImageButton)findViewById(R.id.btnPlay);
	            btnPlay.setVisibility(View.VISIBLE);
	        }
        
		}
		catch(Exception ex) {
			Log.e(LOG_TAG, ex.getMessage());
		}
    }
    
    private void stopRecording() {
        mRecorder.stop();
        //mRecorder.release();
        //mRecorder = null;
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
		if( strMessage.isEmpty() ) {
			if( voiceFileName.isEmpty() ) {
				Toast.makeText(this, "No input was provided", Toast.LENGTH_LONG).show();
				return;
			}
			else 
				strMessage = "Voice message";
		}

		try {
			
			//persistMessage(strMessage);
			txtMessage.setText("");
			
			YHMessage message = new YHMessage(0, strMessage);
			
			message.setUserID(myUserID);
			addMessageToAdapte(message);
		
			String serviceURL = getString(R.string.send_chatmessage_url);
			// Should be something like http://youhelp.cloudapp.net/YouHelpService.svc/sendchatmessage?content=;
			
			StringBuilder sb = new StringBuilder(serviceURL); 
			sb.append("?content=");
			sb.append(URLEncoder.encode(strMessage, "utf-8")); 
	  	 	
			sb.append("&fromuserid=");
			sb.append(myUserID);
			
			sb.append("&touserid=");
			sb.append(toUserid);
			
			String uri = sb.toString();
			
			SendChatMessageAsyncTask sendTask = new SendChatMessageAsyncTask(this);
			sendTask.execute(uri);
			
			UploadParams params = new UploadParams("voicemessages", message);
			
            UploadBlobTask uploadTask = new UploadBlobTask(this);
            uploadTask.execute(params);
			
		}catch(Exception ex){
			
			Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	private void addMessageToAdapte(YHMessage message){
		
		if( chatAdapter != null && message != null) 
			chatAdapter.add(message);
	}
	
	public void persistMessage(YHMessage message) {

		try {
			String content = message.getContent();

			if( datasource == null)
				datasource = new YHDataSource(this);
			
			datasource.open();

			datasource.createYHMessage(content, 
										this.myUserID, 
										new Date(), 
										this.toUserid, 
										message.getBlobURL());
			datasource.close();
			
			addMessageToAdapte(message);
		} catch (SQLException error) {
			 Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
		} catch (Exception error) {
			 Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
		}
	
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
	
	public class UploadParams {
		
		public UploadParams(String containerName, YHMessage message) {
			this.containerName = containerName;
			this.message = message;
		}
		
		private String containerName;
		public String getContainerName() {
			return containerName;
		}
		
		private YHMessage message;
		public YHMessage getYHMessage() {
			return message;
		}

	}
	
	private class UploadBlobTask extends AsyncTask<UploadParams, String, Boolean> {

		Exception error;
		YHMessage yhMessage;
		ChatActivity activity;
		
		public static final String storageConnectionString = 
			    "DefaultEndpointsProtocol=http;" + 
			    "AccountName=youhelpstorage;" + 
			    "AccountKey=dtpTqukoGje8FSnSvUBc/of+6Y3FQZRi7eS2+PTanCnAglBBExnsvXjxTjZQxiROUWJbZZijlZ97WR7/l6MDMA==";
		
		public UploadBlobTask(ChatActivity ctx){
			activity = ctx;
		}
		
		@Override
	    protected void onPostExecute(Boolean result) {
		
			if( !result  && error != null ) {
				String strMessage = error.getMessage();
				Toast.makeText(activity, strMessage, Toast.LENGTH_LONG).show(); 
			}
			else {
			
				if( voiceFileName != null ) {
					String blogURL = "https://youhelpstorage.blob.core.windows.net/voicemessages/";
					String fileName = voiceFileName.substring(voiceFileName.lastIndexOf('/') + 1);
					blogURL += fileName;
					yhMessage.setBlobURL(blogURL);
				}
				activity.persistMessage(yhMessage);
			}
		}
		
		@Override
		protected Boolean doInBackground(UploadParams... params) {
			
			try{
				
				 if( voiceFileName != null && !voiceFileName.isEmpty() ) {

					String containerName = params[0].getContainerName();
					this.yhMessage = params[0].getYHMessage();

					// Retrieve storage account from connection-string.
				    CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
				    
				    // Create the blob client.
				    CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
				    
				    // Retrieve reference to a previously created container.
				    CloudBlobContainer container = blobClient.getContainerReference(containerName);

				    String fileName = voiceFileName.substring(voiceFileName.lastIndexOf('/') + 1);
				    CloudBlockBlob blob = container.getBlockBlobReference(fileName);

				    File file = new File(voiceFileName);
			    	blob.upload(new FileInputStream(voiceFileName), file.length());

			    }
			    
		    } catch(Exception e) {
		    	error = e;
		    	return false;
			}
			
			return true;

		}
	}

	
	private class GetBlobTask extends AsyncTask<String, String, Bitmap>
	{
		Context context;
		Exception error;
		private final WeakReference<ImageView> imageViewReference;
		
		public static final String storageConnectionString = 
			    "DefaultEndpointsProtocol=http;" + 
			    "AccountName=youhelpstorage;" + 
			    "AccountKey=dtpTqukoGje8FSnSvUBc/of+6Y3FQZRi7eS2+PTanCnAglBBExnsvXjxTjZQxiROUWJbZZijlZ97WR7/l6MDMA==";

		
		public GetBlobTask(Context ctx, ImageView imageView){
			context = ctx;
			 
			// Use a WeakReference to ensure the ImageView can be garbage collected
			imageViewReference = new WeakReference<ImageView>(imageView);
		}
		
		// BitmapFactory.decodeXXX() functions should not be executed on the main UI 
		// due to unpredictable time the data takes to load into memory
		@Override
	    protected void onPostExecute(Bitmap bitmap) {
			try{
				if( error != null ) {
	            	Toast.makeText(context, error.getMessage(), 
	    						Toast.LENGTH_LONG).show();
				}else if (imageViewReference != null && bitmap != null) {
					final ImageView imageView = imageViewReference.get();
					if (imageView != null) {
		                imageView.setImageBitmap(bitmap);
					}
				}

			} catch(OutOfMemoryError e){
				Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
			} catch (Exception ex) {
				Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
		
		@Override
		protected Bitmap doInBackground(String... params) {
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
			    
			    InputStream decodedInput = new ByteArrayInputStream(baos.toByteArray());
			    
			    return decodeSampledBitmapFromResource(decodedInput, 100, 100);
			    
			    //return baos;
			    
			} catch(Exception e) {
		    	error = e;
		    	return null;
			}

		}
		
		public Bitmap decodeSampledBitmapFromResource(InputStream stream, 
		        										int reqWidth, int reqHeight) {
//		    BitmapFactory.Options options = new BitmapFactory.Options();
//		    options.inJustDecodeBounds = true;
//		    BitmapFactory.decodeStream(stream, null, options);
//		    
//		    // Calculate inSampleSize
//		    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		    
//		    options.inJustDecodeBounds = false;
		    try {
		    	//Bitmap bitmap = BitmapFactory.decodeStream(stream, null, options);
		    	//Bitmap bitmap = BitmapFactory.decodeStream(stream);
		    	return null; //bitmap;
		    }catch(Exception ex) {
		    	ex.printStackTrace();
		    }
		    
		    return null;
		}
		
		public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		    
			// Raw height and width of image
		    final int height = options.outHeight;
		    final int width = options.outWidth;
		    int inSampleSize = 1;
	
		    if (height > reqHeight || width > reqWidth) {
	
		        final int halfHeight = height / 2;
		        final int halfWidth = width / 2;
	
		        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
		        // height and width larger than the requested height and width.
		        while ((halfHeight / inSampleSize) > reqHeight
		                && (halfWidth / inSampleSize) > reqWidth) {
		            inSampleSize *= 2;
		        }
		    }

	    return inSampleSize;
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
