package com.anonym.youhelp;

import android.os.Parcel;
import android.os.Parcelable;

public class RegisteredUser implements Parcelable{

	public RegisteredUser() {
	}
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flag) {
		
		out.writeString(Username);
		out.writeString(Id);
		
	}

	// this is used to regenerate your object. 
	// All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<RegisteredUser> CREATOR = 
    		new Parcelable.Creator<RegisteredUser>() {
        public RegisteredUser createFromParcel(Parcel in) {
            return new RegisteredUser(in);
        }

        public RegisteredUser[] newArray(int size) {
            return new RegisteredUser[size];
        }
    };
	
	private String Username;
	
	public void setUsername(String val) {
		Username = val;
	}
	public String getUsername(){
		return Username;
	}
    private RegisteredUser(Parcel in) {
	   Username = in.readString();
	   Id = in.readString();
    }
   
    private String Id;
    public String getId(){
	   return Id;
    }
    public void setId(String id){
	   Id = id;
    }
}
