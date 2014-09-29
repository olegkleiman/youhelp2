package com.anonym.youhelp.dataobjects;

public class RideRequest {

	/**
	 * Item text
	 */
	@com.google.gson.annotations.SerializedName("text")
	private String mText;

	@com.google.gson.annotations.SerializedName("carnumber")
	private String mCarNumber;
	
	@com.google.gson.annotations.SerializedName("latitude")
	private double mLatitude;
	
	@com.google.gson.annotations.SerializedName("longitude")
	private double mLongitude;

	
	/**
	 * Item Id
	 */
	@com.google.gson.annotations.SerializedName("id")
	private String mId;

	/**
	 * Indicates if the item is completed
	 */
	@com.google.gson.annotations.SerializedName("complete")
	private boolean mComplete;

	/**
	 * ToDoItem constructor
	 */
	public RideRequest() {

	}

	@Override
	public String toString() {
		return getId();
	}

	/**
	 * Initializes a new ToDoItem
	 * 
	 * @param text
	 *            The item text
	 * @param id
	 *            The item id
	 */
	public RideRequest(String text, String id) {
		//this.setText(text);
		this.setId(id);
	}
	
	public double getLatitude() {
		return mLatitude;
	}
	
	public void setLatitude(double latitude) {
		mLatitude = latitude;
	}
	
	public double getLongitude() {
		return mLongitude;
	}
	
	public void setLongitude(double longitude) {
		mLongitude = longitude;
	}
	
	public String getCarNumber() {
		return mCarNumber;
	}
	
	public void setCarNumber(String carNumber) {
		mCarNumber = carNumber;
	}

	/**
	 * Returns the item id
	 */
	public String getId() {
		return mId;
	}

	/**
	 * Sets the item id
	 * 
	 * @param id
	 *            id to set
	 */
	public final void setId(String id) {
		mId = id;
	}

	/**
	 * Indicates if the item is marked as completed
	 */
	public boolean isComplete() {
		return mComplete;
	}

	/**
	 * Marks the item as completed or incompleted
	 */
	public void setComplete(boolean complete) {
		mComplete = complete;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof RideRequest && ((RideRequest) o).mId == mId;
	}


}

