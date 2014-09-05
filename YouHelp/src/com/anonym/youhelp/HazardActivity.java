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

import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.blob.*;

public class HazardActivity extends Activity {


	
	private Location currentLocation;
	private String userid;
	
	static final int REQUEST_IMAGE_CAPTURE = 1;
	File photoFile = null;
	ImageView mImageView;
	String mCurrentPhotoPath;
	
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
					
					UploadBlobTask uploadTask = new UploadBlobTask();
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
			            photoFile = createImageFile();
			        } catch (IOException ex) {
			            // Error occurred while creating the File
			           
			        }
		            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
		                    Uri.fromFile(photoFile));
			    	
			        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
			    }
				
			}
			
		});
	}

	private File createImageFile() throws IOException {
	    // Create an image file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    String imageFileName = "JPEG_" + timeStamp + "_";
	    File storageDir = Environment.getExternalStoragePublicDirectory(
	            Environment.DIRECTORY_PICTURES);
	    File image = File.createTempFile(
	        imageFileName,  /* prefix */
	        ".jpg",         /* suffix */
	        storageDir      /* directory */
	    );

	    // Save a file: path for use with ACTION_VIEW intents
	    mCurrentPhotoPath = "file:" + image.getAbsolutePath();
	    return image;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
	        
	    	if( data != null) {
		    	Bundle extras = data.getExtras();
		        Bitmap imageBitmap = (Bitmap) extras.get("data");
		        
		        mImageView = (ImageView) findViewById(R.id.imageViewHazardPicture);
		        mImageView.setImageBitmap(imageBitmap);
	    	}

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
	
	class UploadBlobTask extends AsyncTask<String, String, Void> {

		public static final String storageConnectionString = 
			    "DefaultEndpointsProtocol=http;" + 
			    "AccountName=youhelpstorage;" + 
			    "AccountKey=dtpTqukoGje8FSnSvUBc/of+6Y3FQZRi7eS2+PTanCnAglBBExnsvXjxTjZQxiROUWJbZZijlZ97WR7/l6MDMA==";
		
		@Override
		protected Void doInBackground(String... params) {
			
			try{
				
				 if( photoFile != null ) {

					String containerName = params[0];
					// Retrieve storage account from connection-string.
				    CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
				    
				    // Create the blob client.
				    CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
				    
				    // Retrieve reference to a previously created container.
				    CloudBlobContainer container = blobClient.getContainerReference(containerName);
				    
		//		    // Loop over blobs within the container and output the URI to each of them.
		//		    StringBuilder sb = new StringBuilder();
		//		    for (ListBlobItem blobItem : container.listBlobs()) {
		//		    	sb.append( blobItem.getUri() );
		//		    }
		//		    String str = sb.toString();
				    
				    CloudBlockBlob blob = container.getBlockBlobReference("myblob.jpg");
			    
			   
			    	blob.upload(new FileInputStream(photoFile), photoFile.length());
			    }

			    
		    } catch(Exception e) {
		    	e.printStackTrace();
			}
			
			return null;

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
