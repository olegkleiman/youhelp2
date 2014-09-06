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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

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
	private MediaPlayer   mPlayer = null;
	String voiceFileName;
	boolean mStartRecording = true;
	boolean mStartPlaying = true;
	
	ImageView profilePictureView = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		myUserID = sharedPrefs.getString("prefUsername", "");
		
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
		mPlayer = new MediaPlayer();
		
		ImageButton btnRecordMessage = (ImageButton)findViewById(R.id.btnRecordMessage);
		btnRecordMessage.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {

					onRecord(mStartRecording, myUserID);
	                mStartRecording = !mStartRecording;

			}
		});
		
		Button btnPlay = (Button)findViewById(R.id.btnPlay);
		btnPlay.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				onPlay(mStartPlaying, voiceFileName);
				mStartPlaying = !mStartPlaying;
				
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
	    // Create an image file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    String imageFileName = userName + "_" + timeStamp;
	    File storageDir = Environment.getExternalStoragePublicDirectory(
	            Environment.DIRECTORY_PICTURES);
	    File voiceFile = File.createTempFile(
	        imageFileName,  /* prefix */
	        ".3gp",         /* suffix */
	        storageDir      /* directory */
	    );

	    return voiceFile.getAbsolutePath();
	}
	
    private void onPlay(boolean start, String fileName) {
        if (start) {
            startPlaying(fileName);
        } else {
            stopPlaying();
        }
    }
	
    private void startPlaying(String fileName) {

        try {
            mPlayer.setDataSource(fileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        } catch(Exception ex) {
        	Log.e(LOG_TAG, ex.getMessage());
        }
    
    }
	
    private void stopPlaying() {
    	mPlayer.stop();
    	mPlayer.reset();
        //mPlayer.release();
        //mPlayer = null;
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
	
	        } else {
	            stopRecording();
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
			
            UploadBlobTask uploadTask = new UploadBlobTask(this);
            uploadTask.execute("voicemessages");
			
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
	
	private class UploadBlobTask extends AsyncTask<String, String, Boolean> {

		Context context;
		Exception error;
		
		public static final String storageConnectionString = 
			    "DefaultEndpointsProtocol=http;" + 
			    "AccountName=youhelpstorage;" + 
			    "AccountKey=dtpTqukoGje8FSnSvUBc/of+6Y3FQZRi7eS2+PTanCnAglBBExnsvXjxTjZQxiROUWJbZZijlZ97WR7/l6MDMA==";
		
		public UploadBlobTask(Context ctx){
			context = ctx;
		}
		
		@Override
	    protected void onPostExecute(Boolean result) {
		
			if( !result  && error != null ) {
				String strMessage = error.getMessage();
				Toast.makeText(context, strMessage, Toast.LENGTH_LONG).show(); 
			}
		}
		
		@Override
		protected Boolean doInBackground(String... params) {
			
			try{
				
				 if( voiceFileName != null && !voiceFileName.isEmpty() ) {

					String containerName = params[0];

					// Retrieve storage account from connection-string.
				    CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
				    
				    // Create the blob client.
				    CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
				    
				    // Retrieve reference to a previously created container.
				    CloudBlobContainer container = blobClient.getContainerReference(containerName);

				    //String fileName = photoFile.getName();
				    CloudBlockBlob blob = container.getBlockBlobReference(voiceFileName);
			    
				    File file = new File(voiceFileName);
			    	blob.upload(new FileInputStream(file), file.length());

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
