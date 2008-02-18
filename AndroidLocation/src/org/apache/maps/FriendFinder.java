package org.apache.maps;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.Toast;

public class FriendFinder extends ListActivity {
	// ===========================================================
	// Fields
	// ===========================================================
	
    protected MyLocation myLocation = null;
    protected Long refresh = new Long(1000);
	
    /* Thread that sends a message 
     * to the handler every second */ 
    Thread myRefreshThread = null; 

    /** Minimum distance in meters for a friend 
	 * to be recognize as a Friend to be drawn */
	protected static final int NEARFRIEND_MAX_DISTANCE = 10000000;  // 10.000km
	
	/** List of friends in */
	public LocationObjects myObjects = null; // Array to store List of friends

    /* The Handler that receives the messages 
     * sent out by myRefreshThread every second */ 
    Handler myListViewUpdateHandler = new Handler(){ 
         /** Gets called on every message that is received */ 
         // @Override 
         public void handleMessage(Message msg) {
        	 updateList();
             super.handleMessage(msg); 
         } 
    }; 
	
	// Entry-Point
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		// Array to store List of Objects
		myObjects = new LocationObjects(NEARFRIEND_MAX_DISTANCE, FriendFinder.this);

		// Detect My Current Location (GPS or Virtual GPS)
		myLocation = new MyLocation(FriendFinder.this);

		// Read Preferences
		SharedPreferences settings = getSharedPreferences(getString(R.string.app_name), 0);
		// convert minutes to milliseconds
		Float refreshminutes = Float.parseFloat(settings.getString("refresh", "10000"))*1000*60;
        refresh = refreshminutes.longValue();

        // Thread to update objects every xx seconds
		this.myRefreshThread = new Thread(new secondCountDownRunner()); 
        this.myRefreshThread.start(); 

	}
	
    class secondCountDownRunner implements Runnable{ 
        // @Override 
        public void run() { 
             while(!Thread.currentThread().isInterrupted()){ 
                 Message m = new Message(); 
                 FriendFinder.this.myListViewUpdateHandler.sendMessage(m); 
                  try { 
                       Thread.sleep(refresh); 
                  } catch (InterruptedException e) { 
                       Thread.currentThread().interrupt(); 
                  } 
             } 
        } 
    }
	
	/**
	 * Restart the receiving, when we are back on line.
	 */
	@Override
	public void onResume() {
		super.onResume();
		
		/* As we only want to react on the LOCATION_CHANGED
		 * intents we made the OS send out, we have to 
		 * register it along with a filter, that will only
		 * "pass through" on LOCATION_CHANGED-Intents.
		 */
		this.registerReceiver(this.myLocation.myIntentReceiver, this.myLocation.myIntentFilter);
	}
	
	/**
	 * Make sure to stop the animation when we're no longer on screen,
	 * failing to do so will cause a lot of unnecessary cpu-usage!
	 */
	@Override
	public void onFreeze(Bundle icicle) {
		this.unregisterReceiver(this.myLocation.myIntentReceiver);
		super.onFreeze(icicle);
	}

	/** Called only the first time the options menu is displayed.
	 * Create the menu entries.
	 *  Menus are added in the order they are hardcoded. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean supRetVal = super.onCreateOptionsMenu(menu);
		menu.add(0, 0, getString(R.string.main_menu_open_map));
		menu.add(0, 1, getString(R.string.menu_settings));
		menu.add(0, 2, getString(R.string.menu_refresh));
		return supRetVal;
	}
	@Override
	public boolean onOptionsItemSelected(Menu.Item item) {
		switch (item.getId()) {
			case 0:
				startSubActivity(new Intent(this, BrowseMap.class), 0);
				return true;
			case 1:
				startSubActivity(new Intent(this, Settings.class), 0);
				return true;
			case 2:
		    	// Goto My Location
		    	if (myLocation.point!=null) {
		        	updateList();
		    	} else {
		    		Toast.makeText(FriendFinder.this, getString(R.string.couldnotdetectlocation), Toast.LENGTH_SHORT).show();
		    	}
				return true;
		}
		return false;
	}
	
    /*
     * When the user hit a Key
     */
        public boolean onKeyDown(int keyCode, KeyEvent event) {        	
        if (keyCode == KeyEvent.KEYCODE_0) {
	    	// Goto My Location
	    	if (myLocation.point!=null) {
	        	updateList();
	    	} else {
	    		Toast.makeText(FriendFinder.this, getString(R.string.couldnotdetectlocation), Toast.LENGTH_SHORT).show();
	    	}
	        return true;
        } 
        return false;
    }
	
	
	// ===========================================================
	// Methods
	// ===========================================================
	
	protected void updateList() {
		ArrayList<String> listItems = new ArrayList<String>();
		
		// For each Friend
		for(Friend aNearFriend : this.myObjects.nearFriends){
			/* Load the row-entry-format defined as a String 
			 * and replace $name with the contact's name we 
			 * get from the cursor */
			String curLine = new String(getString(R.string.main_list_format));
			curLine = curLine.replace("$name", aNearFriend.itsUsername);
			
			if(aNearFriend.itsLocation != null){
				final DecimalFormat df = new DecimalFormat("####0.00");
				String formattedDistance = 
					df.format(this.myLocation.location.distanceTo(
									aNearFriend.itsLocation) / 1000);
				curLine = curLine.replace("$distance", formattedDistance);
			}else{
				curLine = curLine.replace("$distance", 
						getString(R.string.main_list_geo_not_set));
			}
			
			listItems.add(curLine);
		}

		ArrayAdapter<String> notes =  new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, listItems);

		
		long beforeIndex = 0;
		if(this.getListAdapter() != null)
			beforeIndex = this.getSelectedItemId();
			
		this.setListAdapter(notes);
		
		try{
			this.setSelection((int)beforeIndex);
		}catch (Exception e){}
	}
}