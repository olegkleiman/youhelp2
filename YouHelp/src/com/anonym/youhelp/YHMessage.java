package com.anonym.youhelp;

import java.util.Date;

public class YHMessage {

	public int icon;

	public YHMessage(){
		
	}
	
	public YHMessage(int icon, String title){
		super();
		
        this.icon = icon;
        this.Content = title;
	}
	
	private long id;
	public long getId(){
		return id;
	}
	public void setId(long id){
		this.id = id;
	}
	
	private String Content;
	public String getContent(){
		return Content;
	}
	public void setContent(String content){
		this.Content = content;
	}
	
	private String UserID;
	public String getUserId(){
		return UserID;
	}
	public void setUserID(String userID){
		this.UserID = userID;
	}
	
	private Date DateCreated;
	public Date getDateCreated(){
		return DateCreated;
	}
	public void setDateCreated(Date date){
		DateCreated = date;
	}
	
	private String toUserID;
	public String getToUserId() {
		return toUserID;
	}
	public void setToUserId(String userid){
		toUserID = userid;
	}

	public boolean hasVoiceAttachement() { 
		// TODO: Check type of the blob also
		return ( blobURL == null || blobURL.isEmpty() ) ? false : true; 
	} 
 
	
	private String blobURL;
	public String getBlobURL() {
		return blobURL;
	}
	public void setBlobURL(String url) {
		blobURL = url;
	}
	
	@Override
	public String toString(){
		return UserID + Content;
	}
	
	
	
	
}
