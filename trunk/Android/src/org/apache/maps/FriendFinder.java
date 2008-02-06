package org.apache.maps;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentReceiver;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Contacts.People;
import android.view.Menu;
import android.widget.ArrayAdapter;

public class FriendFinder extends ListActivity {
	// ===========================================================
	// Fields
	// ===========================================================
	
	protected static final String MY_LOCATION_CHANGED_ACTION = new String("android.intent.action.LOCATION_CHANGED");
	protected LocationManager myLocationManager = null;
	protected Location myLocation = null;
	
	protected boolean doUpdates = true;
	protected MyIntentReceiver myIntentReceiver = null; 
	protected final IntentFilter myIntentFilter =  new IntentFilter(MY_LOCATION_CHANGED_ACTION);
	
	protected final long MINIMUM_DISTANCECHANGE_FOR_UPDATE = 25; // in Meters
	protected final long MINIMUM_TIME_BETWEEN_UPDATE = 2500; // in Milliseconds
	
	/** Minimum distance in meters for a friend 
	 * to be recognize as a Friend to be drawn */
	protected static final int NEARFRIEND_MAX_DISTANCE = 10000000;  // 10.000km
	
	/** List of friends in */
	protected ArrayList<Friend> allFriends = new ArrayList<Friend>();
	
	// ===========================================================
	// Extra-Class
	// ===========================================================


	/**
	 * This tiny IntentReceiver updates
	 * our stuff as we receive the intents 
	 * (LOCATION_CHANGED_ACTION) we told the 
	 * myLocationManager to send to us. 
	 */
	class MyIntentReceiver extends IntentReceiver {
		@Override
		public void onReceiveIntent(Context context, Intent intent) {
			if(FriendFinder.this.doUpdates)
				FriendFinder.this.updateList(); // Will simply update our list, when receiving an intent
		}
	}

	// ===========================================================
	// """Constructors""" (or the Entry-Point of it all)
	// ===========================================================
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		/* The first thing we need to do is to setup our own 
		 * locationManager, that will support us with our own gps data */
		this.myLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

		/* Update the list of our friends once on the start,
		 * as they are not(yet) moving, no updates to them are necessary */
		this.refreshFriendsList();
		
		/* Initiate the update of the contactList
		 * manually for the first time */ 
		this.updateList();

		/* Prepare the things, that will give 
		 * us the ability, to receive Information 
		 * about our GPS-Position. */
		this.setupForGPSAutoRefreshing();
	}
	
	/**
	 * Restart the receiving, when we are back on line.
	 */
	@Override
	public void onResume() {
		super.onResume();
		this.doUpdates = true;
		
		/* As we only want to react on the LOCATION_CHANGED
		 * intents we made the OS send out, we have to 
		 * register it along with a filter, that will only
		 * "pass through" on LOCATION_CHANGED-Intents.
		 */
		this.registerReceiver(this.myIntentReceiver, this.myIntentFilter);
	}
	
	/**
	 * Make sure to stop the animation when we're no longer on screen,
	 * failing to do so will cause a lot of unnecessary cpu-usage!
	 */
	@Override
	public void onFreeze(Bundle icicle) {
		this.doUpdates = false;
		this.unregisterReceiver(this.myIntentReceiver);
		super.onFreeze(icicle);
	}

	/** Register with our LocationManager to send us 
	 * an intent (who's Action-String we defined above)
	 * when  an intent to the location manager,
	 * that we want to get informed on changes to our own position.
	 * This is one of the hottest features in Android.
	 */
	private void setupForGPSAutoRefreshing() {

		// Get the first provider available
		List<LocationProvider> providers = this.myLocationManager.getProviders();
		LocationProvider provider = providers.get(0);
		
		this.myLocationManager.requestUpdates(provider, MINIMUM_TIME_BETWEEN_UPDATE,
												MINIMUM_DISTANCECHANGE_FOR_UPDATE, 
												new Intent(MY_LOCATION_CHANGED_ACTION));
		
		
		/* Create an IntentReceiver, that will react on the
		 * Intents we said to our LocationManager to send to us. */ 
		this.myIntentReceiver = new MyIntentReceiver();

		/* 
		 * In onResume() the following method will be called automatically!
		 * registerReceiver(this.myIntentReceiver, this.myIntentFilter); 
		 */
	}

	/** Called only the first time the options menu is displayed.
	 * Create the menu entries.
	 *  Menus are added in the order they are hardcoded. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean supRetVal = super.onCreateOptionsMenu(menu);
		menu.add(0, 0, getString(R.string.main_menu_open_map));
		return supRetVal;
	}
	@Override
	public boolean onOptionsItemSelected(Menu.Item item) {
		switch (item.getId()) {
			case 0:
				startSubActivity(new Intent(this, BrowseMap.class), 0);
				return true;
		}
		return false;
	}

	// ===========================================================
	// Methods
	// ===========================================================
	
	private void refreshFriendsList(){
		// TODO: Get Objects Web Service instead of Phone Contacts
		Cursor c = getContentResolver().query(People.CONTENT_URI, 
				null, null, null, People.NAME + " ASC");
		/* This method allows the activity to take
         * care of managing the given Cursor's lifecycle
         * for you based on the activity's lifecycle. */ 
		startManagingCursor(c);

		int notesColumn = c.getColumnIndex(People.NOTES);
		int nameColumn = c.getColumnIndex(People.NAME);
		
		// Moves the cursor to the first row
		// and returns true if there is sth. to get
		if (c.first()) {
			do {		
				String notesString = c.getString(notesColumn);
				
				Location friendLocation = null;
				if (notesString != null) {
					// Pattern for extracting geo-ContentURIs from the notes.
					final String geoPattern = "(geo:[\\-]?[0-9]{1,3}\\.[0-9]{1,6}\\,[\\-]?[0-9]{1,3}\\.[0-9]{1,6}\\#)";
					// Compile and use regular expression
					Pattern pattern = Pattern.compile(geoPattern);

					CharSequence inputStr = notesString;
					Matcher matcher = pattern.matcher(inputStr);

					boolean matchFound = matcher.find();
					if (matchFound) {
						// We take the first match available
						String groupStr = matcher.group(0);
						// And parse the Lat/Long-GeoPos-Values from it
						friendLocation = new Location();
						String latid = groupStr.substring(groupStr.indexOf(":") + 1,
								groupStr.indexOf(","));
						String longit = groupStr.substring(groupStr.indexOf(",") + 1,
								groupStr.indexOf("#"));
						friendLocation.setLongitude(Float.parseFloat(longit));
						friendLocation.setLatitude(Float.parseFloat(latid));
					}
				}
				String friendName = c.getString(nameColumn);
				allFriends.add(new Friend(friendLocation, friendName));
			} while (c.next());
		}
	}
	
	private void updateList() {
		// TODO: Create a Class for detecting location (GPS or GSM Cell) and use it in FrindFiender and BrowseMap
		// Refresh our location...
		this.myLocation = myLocationManager.getCurrentLocation("gps");
		
		ArrayList<String> listItems = new ArrayList<String>();
		
		// For each Friend
		for(Friend aNearFriend : this.allFriends){
			/* Load the row-entry-format defined as a String 
			 * and replace $name with the contact's name we 
			 * get from the cursor */
			String curLine = new String(getString(R.string.main_list_format));
			curLine = curLine.replace("$name", aNearFriend.itsName);
			
			if(aNearFriend.itsLocation != null){
				if( this.myLocation.distanceTo(aNearFriend.itsLocation) < 
									NEARFRIEND_MAX_DISTANCE){
					final DecimalFormat df = new DecimalFormat("####0.000");
					String formattedDistance = 
						df.format(this.myLocation.distanceTo(
										aNearFriend.itsLocation) / 1000);
					curLine = curLine.replace("$distance", formattedDistance);
				}
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
			beforeIndex = this.getSelectionRowID();
			
		this.setListAdapter(notes);
		
		try{
			this.setSelection((int)beforeIndex);
		}catch (Exception e){}
	}
}