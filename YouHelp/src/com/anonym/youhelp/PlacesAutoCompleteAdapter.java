package com.anonym.youhelp;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
//import android.widget.Filter.FilterResults;


class PlacesAutoCompleteAdapter extends ArrayAdapter<FoundPlace> implements Filterable {
	
	private ArrayList<FoundPlace> resultList;

	private static final String LOG_TAG = "com.anonym.youhelp.placeautocompleteadapter";

	private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
	private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
	private static final String OUT_JSON = "/json";

	private static final String API_KEY = "AIzaSyBJryLCLoWeBUnSTabBxwDL4dWO4tExR1c";
	
	Context context;
	int layoutResourceId;  
	
    public PlacesAutoCompleteAdapter(Context context, int layoutResourceId) {
        super(context, layoutResourceId);
        
        this.layoutResourceId = layoutResourceId;
        this.context = context;
    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Override
    public FoundPlace getItem(int index) {
        return resultList.get(index);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
    	
    	View row = convertView;
    	PlaceHolder holder = null;
    	
    	try{
    	if(row == null){
	    	LayoutInflater inflater = ((Activity)context).getLayoutInflater();
	    	row = inflater.inflate(layoutResourceId, parent, false);
	    	
	    	holder = new PlaceHolder();
	    	holder.txtView = (TextView)row.findViewById(R.id.txtPlaceDesc);
	    	
	    	row.setTag(holder);
    	}
        else
        {
            holder = (PlaceHolder)row.getTag();
            holder.txtView.setGravity(Gravity.RIGHT);
        }
    	
    	FoundPlace place = this.getItem(position);
    	holder.txtView.setText(place.getDescription());
    	}
    	catch(Exception ex) {
    		String msg = ex.getMessage();
    		Log.e(LOG_TAG, msg);
    	}
    	
    	return row;
    }
    
    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null) {
                    
                	// Retrieve the autocomplete results.
                    resultList = autocomplete(constraint.toString());

                    // Assign the data to the FilterResults
                    ArrayList<String> placeNames = new ArrayList<String>();
                    for(FoundPlace place : resultList) {
                    	placeNames.add(place.getDescription());
                    }
                    
                    filterResults.values = placeNames;// resultList;
                    filterResults.count = resultList.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                }
                else {
                    notifyDataSetInvalidated();
                }
            }};
        return filter;
    }
    
    private ArrayList<FoundPlace> autocomplete(String input) {
        ArrayList<FoundPlace> resultList = null;

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?key=" + API_KEY);
            sb.append("&components=country:il");
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));

            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            // Extract the Place descriptions from the results
            resultList = new ArrayList<FoundPlace>(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {

            	String placeID = predsJsonArray.getJSONObject(i).getString("place_id");
            	String desc =  predsJsonArray.getJSONObject(i).getString("description");
                
                FoundPlace fPlace = new FoundPlace();
                fPlace.setDescription(desc);
                fPlace.setPlaceID(placeID);
                
                resultList.add(fPlace);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }

        return resultList;
    }
    
    static class PlaceHolder
    {
        TextView txtView;

    }
}

