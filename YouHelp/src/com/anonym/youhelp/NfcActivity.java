package com.anonym.youhelp;

import java.util.List;

import com.anonym.youhelp.nfc.NFCUtils;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.Toast;

public class NfcActivity extends Activity implements NfcAdapter.OnNdefPushCompleteCallback,
													 NfcAdapter.CreateNdefMessageCallback{

	private NfcAdapter nfcAdapter;
	private Handler mHandler;
	private PendingIntent mPendingIntent;
	
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
}
