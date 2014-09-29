package com.anonym.youhelp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.anonym.youhelp.nfc.NFCUtils;
import com.anonym.youhelp.dataobjects.RideRequest;
import com.microsoft.windowsazure.mobileservices.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.TableOperationCallback;
import com.microsoft.windowsazure.mobileservices.UserAuthenticationCallback;

public class NfcActivity extends BasicActivity implements NfcAdapter.OnNdefPushCompleteCallback,
													 NfcAdapter.CreateNdefMessageCallback{

	private static final String TAG = "youhelp";
	private NfcAdapter nfcAdapter;
	private Handler mHandler;
	private PendingIntent mPendingIntent;
	final String lpnRegex = "\\(\\d{3}\\)\\d{3}\\-\\d{2}\\-\\d{2}";  
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_nfc);
		
		resolveIntent(getIntent());
		
		nfcAdapter =  NfcAdapter.getDefaultAdapter(this);
		if( nfcAdapter == null || !nfcAdapter.isEnabled() ) {
			Toast.makeText(this, "NFC is disabled", Toast.LENGTH_LONG).show();
			return;
		}else {
			
			mHandler = new Handler() {
		        @Override
		        public void handleMessage(Message msg) {
		            switch (msg.what) {
		                case 1:
		                    Toast.makeText(getApplicationContext(),"BEAM completed",
		                                   Toast.LENGTH_LONG).show();
		                    break;
		            } 
		        }
		    }; 
			
			nfcAdapter.setNdefPushMessageCallback(this, this);
			nfcAdapter.setOnNdefPushCompleteCallback(this, this);
			
			mPendingIntent = PendingIntent.getActivity(this, 0,
	                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		}
		
		final EditText txtLPN = (EditText)findViewById(R.id.txtLPN);
		
//		txtLPN.setFilters(  
//			    new InputFilter[] {  
//			        new PartialRegexInputFilter(regex)  
//			    }  
//			);  
		
		txtLPN.addTextChangedListener( new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				String value  = s.toString(); 
				
				if(value.matches(lpnRegex))  
					txtLPN.setTextColor(Color.BLACK);  
                else  
                	txtLPN.setTextColor(Color.RED);  
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start,
					int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {}
			
		});
	}

	@Override
	public NdefMessage createNdefMessage(NfcEvent event) {
		return NFCUtils.newNdefMessage("Message from passenger");
	}

	// NOT Called on main UI thread
	@Override
	public void onNdefPushComplete(NfcEvent event) {
		mHandler.obtainMessage(1).sendToTarget();
	}

	@Override
	public void onResume() {
		super.onResume();
		
		if (nfcAdapter != null) 
			nfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null); 

	}
	
	@Override
    protected void onPause() {
        super.onPause();
        
        if (nfcAdapter != null) 
        	nfcAdapter.disableForegroundDispatch(this);
       
    }
	
	@Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        resolveIntent(intent);

    }
	
	private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            } else {
                // Unknown tag type
                byte[] empty = new byte[0];
                byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
                Parcelable tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, id, empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[] { record });
                msgs = new NdefMessage[] { msg };
            }
            
            // Setup the views
            buildTagViews(msgs);
        }
	}
	
	private MobileServiceClient mClient;
	private MobileServiceTable<RideRequest> mRideRequestsTable;
	
	public void onLPNSend(View v){
		final EditText txtLPN = (EditText)findViewById(R.id.txtLPN);
		final String carNumber = txtLPN.getText().toString();
		if( carNumber.isEmpty() ) {
			String message = getResources().getString(R.string.emptyLPN);
			Toast.makeText(this, message, Toast.LENGTH_LONG).show();
			return;
		}
		
		final Location currentLocation = getMyLocation();
		
		try {
			
			mClient = new MobileServiceClient(
					getResources().getString(R.string.wams_url),
					getResources().getString(R.string.wams_app_key),
					this);
			
			mClient.login(MobileServiceAuthenticationProvider.Facebook, new UserAuthenticationCallback(){

				private void wamsExceptionHandle(Exception exception){
					String message = ( exception.getCause() != null ) ? 
							exception.getCause().getLocalizedMessage()
							: exception.getLocalizedMessage();
					Log.e(TAG, message);
					Toast.makeText(NfcActivity.this, message, Toast.LENGTH_LONG).show();
				}
				
				@Override
				public void onCompleted(MobileServiceUser user, 
						Exception exception,
						ServiceFilterResponse response) {
							if( exception == null ) {
								
								String msg = String.format("You are how logged in - %1$2s", user.getUserId());
								Log.i(TAG, msg);
								
								mRideRequestsTable = mClient.getTable(RideRequest.class);
								
								RideRequest item = new RideRequest();
								item.setCarNumber(carNumber);
								item.setLatitude(currentLocation.getLatitude());
								item.setLongitude(currentLocation.getLongitude());
								item.setComplete(false);
								
								mRideRequestsTable.insert(item, new TableOperationCallback<RideRequest>() {
									
									@Override
									public void onCompleted(RideRequest entity, 
															Exception exception, 
															ServiceFilterResponse response) {
										if (exception == null) {
											String message = getResources().getString(R.string.ride_request_accepted);
											Toast.makeText(NfcActivity.this, message, Toast.LENGTH_LONG).show();
											finish();
										}
										else {
											wamsExceptionHandle(exception);
										}
									}
								});
							}
							else {
								wamsExceptionHandle(exception);
							}
						}
			});
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}

		
//		String serviceURL = getString(R.string.passenger_request_uri);
//		StringBuilder sb = new StringBuilder(serviceURL); 
//		sb.append("?lpn=");
//		sb.append(lpn);
//		
//		sb.append("&c=");
//		double lat = currentLocation.getLatitude();
//   	    double lon = currentLocation.getLongitude();
//   	 	String strCurrentLocation = String.format(Locale.US, "%.13f;%.13f", lat, lon);
//		sb.append(strCurrentLocation);
//		
//		sb.append("&p=");
//		sb.append("33333"); // Provide real ID 
//		String uri = sb.toString();
//		
//		SendMessageAsyncTask sendTask = new SendMessageAsyncTask(this);
//		sendTask.execute(uri);
	}
	
    void buildTagViews(NdefMessage[] msgs) {
        
//    	try {
//			if (msgs == null || msgs.length == 0) {
//			    return;
//			}
//			
//			LayoutInflater inflater = LayoutInflater.from(this);
//			LinearLayout content = mTagContent;
//			
//			// Clear out any old views in the content area, for example if you scan
//			// two tags in a row.
//			content.removeAllViews();
//			
//			List<ParsedNdefRecord> records = NdefMessageParser.parse(msgs[0]);
//			final int size = records.size();
//			for (int i = 0; i < size; i++) {
//			    ParsedNdefRecord record = records.get(i);
//			    content.addView(record.getView(this, inflater, content, i));
//			    inflater.inflate(R.layout.tag_divider, content, true);
//			}
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			Log.i("montreux", e.getMessage());
//		}
    }

    class SendMessageAsyncTask extends AsyncTask<String, String, String> {

		Context context;
		Exception error;
    	
		public SendMessageAsyncTask(Context ctx){
			context = ctx;
		}
		
		@Override
	    protected void onPostExecute(String result) {
		
			if( error != null ) {
				String strMessage = error.getMessage();
				Toast.makeText(context, strMessage, Toast.LENGTH_LONG).show(); 
			}
		}
    	
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
