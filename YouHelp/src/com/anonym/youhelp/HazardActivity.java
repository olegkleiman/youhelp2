package com.anonym.youhelp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

public class HazardActivity extends Activity {

	private Location currentLocation;
	private String userid;
	
	static final int REQUEST_IMAGE_CAPTURE = 1;
	File photoFile = null;
	ImageView mImageView;
	//String mCurrentPhotoPath;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_hazard);
		
		Bundle extras = getIntent().getExtras();
		userid = extras.getString("userid");
		
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		String locationProvider = LocationManager.NETWORK_PROVIDER;
		// Or use LocationManager.GPS_PROVIDER

		currentLocation = locationManager.getLastKnownLocation(locationProvider);

		ImageView btnSend = (ImageView) findViewById(R.id.btnSend);
		btnSend.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View view) {
				
				if( isNetworkAvailable() ) {
					
					UploadBlobTask uploadTask = new UploadBlobTask(HazardActivity.this);
					uploadTask.execute("pictures");
					
					String serviceURL = getString(R.string.send_toast_service_url);
					// Should be something like http://youhelp.cloudapp.net/YouHelpService.svc/sendtoast?title=;
					
					StringBuilder sb = new StringBuilder(serviceURL); 
					sb.append("?title=");
					sb.append("Help");//strTilte);
			  	 	
					sb.append("&subtitle=");
					
					double lat = currentLocation.getLatitude();
			   	    double lon = currentLocation.getLongitude();
			   	 	String strCurrentLocation = String.format(Locale.US, "%.13f;%.13f", lat, lon);
					
					sb.append(strCurrentLocation);
					sb.append("&tags=null");
					
					sb.append("&userid=");
					sb.append(userid);
					
					String uri = sb.toString();
					
					SendMessageAsyncTask sendTask = new SendMessageAsyncTask();
					//sendTask.ParentActivity = this;
					sendTask.execute(uri);
					
					finish();
				}
				else{
					
					AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(HazardActivity.this);
					
			   	 	dlgAlert.setMessage("Yau are not connected to network");
			   	 	dlgAlert.setTitle("YouHelp");
			   	 	dlgAlert.setPositiveButton("OK", null);
			   	 	dlgAlert.setCancelable(true);
			   	 	dlgAlert.create().show();
				}
				
				
			}
			
		});
		
		mImageView = (ImageView) findViewById(R.id.btnPhoto);
		mImageView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {

			    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
			    	
			        // Create the File where the photo should go
			        photoFile = null;
			        try {
			        	String[] tokens = userid.split(":");
			        	String username = ( tokens.length > 1 ) ? tokens[1] : tokens[0];
			            photoFile = createImageFile(username);
			        } catch (IOException ex) {
			            
			        	// Error occurred while creating the File
			        	Toast.makeText(getApplicationContext(), 
			        					ex.getMessage(), 
			        					Toast.LENGTH_LONG).show();
			        	
			           
			        }
		            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
		                    					Uri.fromFile(photoFile));
			    	
			        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
			    }
				
			}
			
		});
	}

	private File createImageFile(String userName) throws IOException {
	    // Create an image file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    String imageFileName = userName + "_" + timeStamp;
	    File storageDir = Environment.getExternalStoragePublicDirectory(
	            Environment.DIRECTORY_PICTURES);
	    File image = File.createTempFile(
	        imageFileName,  /* prefix */
	        ".jpg",         /* suffix */
	        storageDir      /* directory */
	    );

	    // Save a file: path for use with ACTION_VIEW intents
	    //mCurrentPhotoPath = "file:" + image.getAbsolutePath();
	    return image;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		try {
		    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
		        
		    	if( data != null) {
			    	Bundle extras = data.getExtras();
			        Bitmap imageBitmap = (Bitmap) extras.get("data");
			        
			        mImageView = (ImageView) findViewById(R.id.imageViewHazardPicture);
			        mImageView.setImageBitmap(imageBitmap);
		    	}
		    	else {
		    		try{

			    		if( photoFile != null && photoFile.exists() ) {

			    			String filePath = photoFile.getAbsolutePath();
			    			
			    			 // Get the dimensions of the View
			    		    int targetW = mImageView.getWidth();
			    		    int targetH = mImageView.getHeight();
			    		    
			    		    // Get the dimensions of the bitmap
			    		    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
			    		    bmOptions.inJustDecodeBounds = true;
			    		    BitmapFactory.decodeFile(filePath, bmOptions);
			    		    int photoW = bmOptions.outWidth;
			    		    int photoH = bmOptions.outHeight;
			    		    
			    		    // Determine how much to scale down the image
			    		    int scaleFactor = Math.min(photoW/targetW, photoH/targetH);
			    		    
			    		    // Decode the image file into a Bitmap sized to fill the View
			    		    bmOptions.inJustDecodeBounds = false;
			    		    bmOptions.inSampleSize = scaleFactor;
			    		    bmOptions.inPurgeable = true;

			    		    Bitmap bitmap = BitmapFactory.decodeFile(filePath, bmOptions);
			    		    mImageView.setImageBitmap(bitmap);

			    		}
		    		} catch(Exception ex) {
		            	Toast.makeText(getApplicationContext(), 
		    					ex.getMessage(), 
		    					Toast.LENGTH_LONG).show();
		    		}
		    	}
	
		    }
		} catch(Exception ex) {
        	Toast.makeText(getApplicationContext(), 
					ex.getMessage(), 
					Toast.LENGTH_LONG).show();
		}
	}
	
    private boolean isNetworkAvailable() {
    	
    	ConnectivityManager connectivityManager 
    	      = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    	return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.hazard, menu);
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
	
	class UploadBlobTask extends AsyncTask<String, String, Boolean> {

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
				
				 if( photoFile != null ) {

					String containerName = params[0];
					// Retrieve storage account from connection-string.
				    CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
				    
				    // Create the blob client.
				    CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
				    
				    // Retrieve reference to a previously created container.
				    CloudBlobContainer container = blobClient.getContainerReference(containerName);

				    String fileName = photoFile.getName();
				    CloudBlockBlob blob = container.getBlockBlobReference(fileName);
			    
			    	blob.upload(new FileInputStream(photoFile), photoFile.length());

			    }
			    
		    } catch(Exception e) {
		    	error = e;
		    	return false;
			}
			
			return true;

		}
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
