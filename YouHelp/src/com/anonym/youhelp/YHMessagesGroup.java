package com.anonym.youhelp;

public class YHMessagesGroup {

	private long Count;
	private String UserId;
	
	public YHMessagesGroup() {

	}
	
	public long getCount(){
		return Count;
	}
	
	public void setCount(long count){
		this.Count = count;
	}
	
	public String getUserId() {
		return UserId;
	}
	
	public void setUserId(String userid){
		this.UserId = userid;
	}
}
