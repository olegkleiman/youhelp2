package com.anonym.youhelp;

import java.util.Date;

public class YHMessage {

	public int icon;
	private long id;
	private String Content;
	private String UserID;
	private Date DateCreated;
	private String toUserID;
	
	public YHMessage(){
		
	}
	
	public YHMessage(int icon, String title){
		super();
		
        this.icon = icon;
        this.Content = title;
	}
	
	public long getId(){
		return id;
	}
	
	public void setId(long id){
		this.id = id;
	}
	
	public String getContent(){
		return Content;
	}
	
	public void setContent(String content){
		this.Content = content;
	}
	
	public String getUserId(){
		return UserID;
	}
	
	public void setUserID(String userID){
		this.UserID = userID;
	}
	
	public Date getDateCreated(){
		return DateCreated;
	}
	
	public void setDateCreated(Date date){
		DateCreated = date;
	}
	
	public String getToUserId() {
		return toUserID;
	}
	
	public void setToUserId(String userid){
		toUserID = userid;
	}
	
	@Override
	public String toString(){
		return UserID + Content;
	}
	
	
}
