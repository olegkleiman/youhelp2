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
	
	private boolean ShowPlayer; 
	public boolean getShowPlayer() { 
		return ShowPlayer; 
	} 
	private void setShowPlayer(boolean show){ 
		ShowPlayer = show; 
	} 
	
	@Override
	public String toString(){
		return UserID + Content;
	}
	
	
	
	
}
