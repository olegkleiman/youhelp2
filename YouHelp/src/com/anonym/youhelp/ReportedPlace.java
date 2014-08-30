package com.anonym.youhelp;

import android.location.Location;

public class ReportedPlace extends Location {

	private String Title;
	
	public void setTitle(String title){
		Title = title;
	}
	public String getTitle(){
		return Title;
	}
	
	private String userid;
	
	public void setUserID(String userid){
		this.userid = userid;
	}
	
	public String getUserID(){
		return userid;
	}
	
	public ReportedPlace(Location location) {
		
		super(location);
	}

}