package com.anonym.youhelp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class YHDataSource 
{

	private SQLiteDatabase database;
	private YHSQLiteHelper dbHelper;
	
	private String[] allColumns = { YHSQLiteHelper.COLUMN_ID, 
			YHSQLiteHelper.COLUMN_CONTENT,
			YHSQLiteHelper.COLUMN_USERID,
			YHSQLiteHelper.COLUMN_DATECREATED,
			YHSQLiteHelper.COLUMN_TOUSERID};
	
	public YHDataSource(Context context){
		dbHelper = new YHSQLiteHelper(context);
	}
	
	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}
	
	public void close() {
		dbHelper.close();
	}
	
	public YHMessage createYHMessage(String content, 
			String userid, 
			Date dateCreated,
			String toUserid){

		ContentValues values = new ContentValues();
		values.put(YHSQLiteHelper.COLUMN_CONTENT, content);
		values.put(YHSQLiteHelper.COLUMN_USERID, userid);
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		String strDate = dateFormat.format(dateCreated);
		values.put(YHSQLiteHelper.COLUMN_DATECREATED, strDate);
		
		if( !toUserid.isEmpty() )
		values.put(YHSQLiteHelper.COLUMN_TOUSERID, toUserid);
		
		long insertID = database.insert(YHSQLiteHelper.TABLE_MESSAGES, null, values);
		
		Cursor cursor = database.query(YHSQLiteHelper.TABLE_MESSAGES, allColumns,
		YHSQLiteHelper.COLUMN_ID + " = " + insertID, null,
		null, null, null);
		cursor.moveToFirst();
		YHMessage newMessage = cursorToYHMessage(cursor);
		
		cursor.close();
		return newMessage;
	}
	
	public void deleteYHMessage(YHMessage message){
		long id = message.getId();
		database.delete(YHSQLiteHelper.TABLE_MESSAGES, 
				YHSQLiteHelper.COLUMN_ID + " = " + id, null);
	}
	
	public void deleteAllMessagesOfUser(String userid){
		
		List<YHMessage> messages = getMessagesOfUser(userid);
		if( messages.size() == 0)
			return;
		
		Iterator<YHMessage> iterator = messages.iterator();
		
		while(iterator.hasNext()){
			YHMessage message = iterator.next();
			deleteYHMessage(message);
			
			iterator.remove();
		}
	}
	
	public List<YHMessagesGroup> getAllMessagesGroupedByUsers() {
		List<YHMessagesGroup> messages = new ArrayList<YHMessagesGroup>();
		
		try{
			Cursor cursor = database.rawQuery("select userid, count(*) from yhMessages group by userid", 
											  null);
			cursor.moveToFirst();
			while( !cursor.isAfterLast()){
				YHMessagesGroup mGroup = cursorToYHMessageGroup(cursor);
				messages.add(mGroup);
				cursor.moveToNext();
			}
			
			cursor.close();
			
		}catch(Exception ex){
			
			ex.printStackTrace();
		}
		
		return messages;
	}
	
	public List<YHMessage> getAllMessages() {
		List<YHMessage> messages = new ArrayList<YHMessage>();
		
		Cursor cursor = database.query(YHSQLiteHelper.TABLE_MESSAGES,
										allColumns, null, null, null, null, null);
		cursor.moveToFirst();
		while( !cursor.isAfterLast()){
			YHMessage message = cursorToYHMessage(cursor);
			messages.add(message);
			cursor.moveToNext();
		}
		
		// make sure to close the cursor
		cursor.close();
		return messages;
	}
	
	public List<YHMessage> getMessagesOfUser(String userid){
		
		List<YHMessage> messages = new ArrayList<YHMessage>();
		
		try{
		Cursor cursor = database.query(YHSQLiteHelper.TABLE_MESSAGES,
				allColumns,
				YHSQLiteHelper.COLUMN_USERID + " = '" + userid + "' or touserid = '" + userid + "'" , 
				null, null, null, 
				// last parameter for order by clause
				"date_created ASC");
		
		cursor.moveToFirst();
		while( !cursor.isAfterLast()){
			YHMessage message = cursorToYHMessage(cursor);
	
			messages.add(message);
			cursor.moveToNext();
		}
		
		cursor.close();
		} catch(Exception ex){
			
			ex.printStackTrace();
		}
		return messages;
	}
	
	private YHMessagesGroup cursorToYHMessageGroup(Cursor cursor){
		
		YHMessagesGroup mGroup = new YHMessagesGroup();
		mGroup.setUserId(cursor.getString(0));
		mGroup.setCount(cursor.getLong(1));
		
		
		return mGroup;
	}
	
	private YHMessage cursorToYHMessage(Cursor cursor){
		
		YHMessage message = new YHMessage();
		message.setId(cursor.getLong(0));
		message.setContent(cursor.getString(1));
		message.setUserID(cursor.getString(2));
		
		String strDate = cursor.getString(3); //cursor.getLong(3)*1000);
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		
		Date date = new Date();
		try {
			date = dateFormat.parse(strDate); 
		} catch (ParseException e) {
		
		}
		message.setDateCreated(date);
		message.setToUserId(cursor.getString(4));
		
		return message;
	}
}
