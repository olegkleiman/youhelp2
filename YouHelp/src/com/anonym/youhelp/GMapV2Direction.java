package com.anonym.youhelp;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.graphics.Color;
import android.os.AsyncTask;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

public class GMapV2Direction { 
	 
	 
private class CallDirections extends AsyncTask<String, //the type of the parameters sent to the task upon execution.  
				Object, // the type of the progress units published during the background computation. 
				String> // the type of the result of the background computation. 
{ 
 
	private GoogleMap gMap; 
 
	public CallDirections setMap(GoogleMap map){ 
			gMap = map; 
			return this; 
	} 
 
	@Override 
    protected void onPostExecute(String result) { 
 
		 try { 
		  
		 	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 
		    DocumentBuilder builder = factory.newDocumentBuilder(); 
		    InputSource is = new InputSource(new StringReader(result)); 
		    Document doc =  builder.parse(is); 
		     
		    ArrayList<LatLng> directionPoint = this.getDirection(doc); 
		     
			PolylineOptions rectLine = new PolylineOptions().width(9).color(Color.BLUE); 
			for(int i = 0 ; i < directionPoint.size() ; i++) {           
			rectLine.add(directionPoint.get(i)); 
			} 
			     
			gMap.addPolyline(rectLine); 
		       
		} catch (ParserConfigurationException e) { 
		e.printStackTrace(); 
		} catch (SAXException e) { 
		e.printStackTrace(); 
		} catch (IOException e) { 
		e.printStackTrace(); 
		} 
  
	}	 
 
	@Override 
	protected String doInBackground(String... params) { 
	 
		HttpURLConnection conn = null; 
	 
		try { 
		 
			String strURL = params[0]; 
			URL url = new URL(strURL); 
			conn = (HttpURLConnection) url.openConnection(); 
			InputStreamReader in = new InputStreamReader(conn.getInputStream()); 
			 
			StringBuilder results = new StringBuilder(); 
			        // Load the results into a StringBuilder 
			        int read; 
			        char[] buff = new char[1024]; 
			        while ((read = in.read(buff)) != -1) { 
			        results.append(buff, 0, read);	 
			        } 
			         
			        return results.toString(); 
			 
			} catch(Exception ex) { 
			String strMessage = ex.getMessage(); 
			strMessage.trim(); 
			ex.printStackTrace(); 
			 
			return ""; 
		} 
 
	} 
 
	private ArrayList<LatLng> getDirection (Document doc) { 
 
		NodeList nl1, nl2, nl3; 
        ArrayList<LatLng> listGeopoints = new ArrayList<LatLng>(); 
        nl1 = doc.getElementsByTagName("step"); 
        if (nl1.getLength() > 0) { 
            for (int i = 0; i < nl1.getLength(); i++) { 
                Node node1 = nl1.item(i); 
                nl2 = node1.getChildNodes(); 
 
                Node locationNode = nl2.item(getNodeIndex(nl2, "start_location")); 
                nl3 = locationNode.getChildNodes(); 
                Node latNode = nl3.item(getNodeIndex(nl3, "lat")); 
                double lat = Double.parseDouble(latNode.getTextContent()); 
                Node lngNode = nl3.item(getNodeIndex(nl3, "lng")); 
                double lng = Double.parseDouble(lngNode.getTextContent()); 
                listGeopoints.add(new LatLng(lat, lng)); 
 
                locationNode = nl2.item(getNodeIndex(nl2, "polyline")); 
                nl3 = locationNode.getChildNodes(); 
                latNode = nl3.item(getNodeIndex(nl3, "points")); 
                ArrayList<LatLng> arr = decodePoly(latNode.getTextContent()); 
                for(int j = 0 ; j < arr.size() ; j++) { 
                    listGeopoints.add(new LatLng(arr.get(j).latitude, arr.get(j).longitude)); 
                } 
 
                locationNode = nl2.item(getNodeIndex(nl2, "end_location")); 
                nl3 = locationNode.getChildNodes(); 
                latNode = nl3.item(getNodeIndex(nl3, "lat")); 
                lat = Double.parseDouble(latNode.getTextContent()); 
                lngNode = nl3.item(getNodeIndex(nl3, "lng")); 
                lng = Double.parseDouble(lngNode.getTextContent()); 
                listGeopoints.add(new LatLng(lat, lng)); 
            } 
        } 
 
        return listGeopoints;	 
}	 
 
	}	 
 
	public final static String MODE_DRIVING = "driving"; 
	public final static String MODE_WALKING = "walking";	 
	 
	private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/directions"; 
	private static final String OUTPUT = "/xml";	 
	private static final String API_KEY = "AIzaSyBJryLCLoWeBUnSTabBxwDL4dWO4tExR1c"; 
	 
	public void drawDirectitions(GoogleMap map,  
								LatLng start, LatLng end,  
								String mode, 
								String language) 
	{ 
								     
		StringBuilder sb = new StringBuilder(PLACES_API_BASE + OUTPUT); 
		sb.append("?origin=" + start.latitude + "," + start.longitude); 
		sb.append("&destination=" + end.latitude + "," + end.longitude); 
		sb.append("&units=metric&mode=" + mode); 
		sb.append("&language=" + language);  
		sb.append("&key=" + API_KEY); 
	 
		CallDirections task = new CallDirections(); 
		task.setMap(map).execute(sb.toString()); 
	} 
 
    private int getNodeIndex(NodeList nl, String nodename) { 
        for(int i = 0 ; i < nl.getLength() ; i++) { 
            if(nl.item(i).getNodeName().equals(nodename)) 
                return i; 
        } 
        return -1; 
    } 
     
    private ArrayList<LatLng> decodePoly(String encoded) { 
        ArrayList<LatLng> poly = new ArrayList<LatLng>(); 
        int index = 0, len = encoded.length(); 
        int lat = 0, lng = 0; 
        while (index < len) { 
            int b, shift = 0, result = 0; 
            do { 
                b = encoded.charAt(index++) - 63; 
                result |= (b & 0x1f) << shift; 
                shift += 5; 
            } while (b >= 0x20); 
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1)); 
            lat += dlat; 
            shift = 0; 
            result = 0; 
            do { 
                b = encoded.charAt(index++) - 63; 
                result |= (b & 0x1f) << shift; 
                shift += 5; 
            } while (b >= 0x20); 
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1)); 
            lng += dlng; 
 
            LatLng position = new LatLng((double) lat / 1E5, (double) lng / 1E5); 
            poly.add(position); 
        } 
        return poly; 
    } 
} 

