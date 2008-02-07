package org.apache.maps;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentReceiver;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Contacts.People;
import android.telephony.PhoneStateIntentReceiver;
import android.telephony.ServiceState;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MyMapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Point;
import com.google.googlenav.Placemark;
import com.google.googlenav.Search;
import com.google.googlenav.map.MapPoint;

public class BrowseMap extends MapActivity {
	/*
	 * Define variables
	 */
	protected boolean doUpdates = true;
	protected boolean gps = false;
	protected boolean virtualgps = false;
	private PhoneStateIntentReceiver mPhoneStateReceiver;
    private MyMapView mMapView;
    private Search mSearch;
    private int GET_SEARCH_TEXT = 0;
    protected Location myLocation = new Location();
	protected LocationManager myLocationManager = null;
	protected MyIntentReceiver myIntentReceiver = null; 
    private static HttpConnectionManager connectionManager = new SimpleHttpConnectionManager();
	protected final IntentFilter myIntentFilter = new IntentFilter(MY_LOCATION_CHANGED_ACTION);
    Point myPoint = new Point((int) 0, (int) 0);
	protected ArrayList<Friend> nearFriends = new ArrayList<Friend>(); // Array to store List of friends
	/*
	 *  Constants
	 */
	protected static final int NEARFRIEND_MAX_DISTANCE = 10000000;  // 10.000km
	protected static final int ZOOM_INITIAL_LEVEL = 11;
    private static final int MY_NOTIFICATION_ID = 0x100;
	private static final String MY_LOCATION_CHANGED_ACTION = new String("android.intent.action.LOCATION_CHANGED");
    
    /*
     * Start
     */
    @Override
    public void onCreate(Bundle icicle) {
    	// Execute Parent OnCreate Method
        super.onCreate(icicle);
		// Initialize the LocationManager for GPS
		this.myLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		this.updateView();
		// If GPS is available set autorefresh else try to detect location from Cell-ID 
		this.setupForGPSAutoRefreshing();
        // Create a new MapView
        mMapView = new MyMapView(this);
        // Show the Map
        setContentView(mMapView);
		// MapController is capable of zooming and animating and stuff like that 
        MapController mc = mMapView.getController();
        // Zoom
        mc.zoomTo(ZOOM_INITIAL_LEVEL);
		// Drawing objects on top of the map
		mMapView.createOverlayController().add(new MyOverlay(),true);
    }
    
    /*
     * Create menu items
     */
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean supRetVal = super.onCreateOptionsMenu(menu);
		menu.add(0, 0, getString(R.string.map_menu_zoom_in));
		menu.add(0, 1, getString(R.string.map_menu_zoom_out));
		menu.add(0, 2, getString(R.string.map_menu_toggle_street_satellite));
		menu.add(0, 3, getString(R.string.map_menu_toggle_traffic));
		menu.add(0, 4, getString(R.string.map_menu_gotocurrentlocation));
		menu.add(0, 5, getString(R.string.map_menu_back_to_list));
		return supRetVal;
	}
	
	@Override
	public boolean onOptionsItemSelected(Menu.Item item){
	    switch (item.getId()) {
		    case 0:
		    	// Zoom not closer than possible
		    	this.mMapView.getController().zoomTo(Math.min(21, this.mMapView.getZoomLevel() + 1));
		        return true;
		    case 1:
		    	// Zoom not farer than possible 
		    	this.mMapView.getController().zoomTo(Math.max(1, this.mMapView.getZoomLevel() - 1));
		        return true;
		    case 2:
	        	// Switch to satellite view
		    	mMapView.toggleSatellite();
		        return true;
		    case 3:
	        	// Switch to satellite view
		    	mMapView.toggleTraffic();
		        return true;
		    case 4:
		    	// Goto My Location
		    	if (gps || virtualgps) {
		        	mMapView.getController().animateTo(myPoint);
		        	mMapView.getController().zoomTo(ZOOM_INITIAL_LEVEL);
		    	} else {
	                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	                nm.notifyWithText(1000,"Sorry, could not detect your location",NotificationManager.LENGTH_SHORT, null);
		    	}
		        return true;
		    case 5:
		    	this.finish();
		        return true;
	    }
	    return false;
	}
	    
    /*
     * When the user hit a Key
     */
        public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_I) {
            // Zoom In
	    	this.mMapView.getController().zoomTo(Math.min(21, this.mMapView.getZoomLevel() + 1));
	        return true;
        } else if (keyCode == KeyEvent.KEYCODE_O) {
            // Zoom Out
	    	this.mMapView.getController().zoomTo(Math.max(1, this.mMapView.getZoomLevel() - 1));
	        return true;
        } else if (keyCode == KeyEvent.KEYCODE_S) {
            // Switch on the satellite view
	    	mMapView.toggleSatellite();
	        return true;
        } else if (keyCode == KeyEvent.KEYCODE_T) {
            // Switch on traffic overlays
            mMapView.toggleTraffic();
	        return true;
        } else if (keyCode == KeyEvent.KEYCODE_0) {
	    	// Goto My Location
	    	if (gps || virtualgps) {
	        	mMapView.getController().animateTo(myPoint);
	        	mMapView.getController().zoomTo(ZOOM_INITIAL_LEVEL);
	    	} else {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                nm.notifyWithText(1000,"Sorry, could not detect your location",NotificationManager.LENGTH_SHORT, null);
	    	}
	        return true;
        } else if (keyCode == KeyEvent.KEYCODE_F) {
        	/*
        	 * Open the Seach Screen
        	 */
            Intent intent = new Intent(BrowseMap.this, org.apache.maps.Search.class);
            startSubActivity(intent, GET_SEARCH_TEXT);
	        return true;
        } else if (keyCode == KeyEvent.KEYCODE_P) {
        	/*
        	 * Search for Pizza Objects
        	 */
            startSearch("Pizza");
	        return true;
        } else if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_9) {
            int item = keyCode - KeyEvent.KEYCODE_1;
            if (mSearch != null && mSearch.numPlacemarks() > item) {
                NotificationManager nm = (NotificationManager)
                        getSystemService(NOTIFICATION_SERVICE);
                nm.notifyWithText(999,
                        mSearch.getPlacemark(item).getDetailsDescriptor(),
                        NotificationManager.LENGTH_SHORT, null);
                goTo(item);
            }
	        return true;
        }
        return false;
    }

    /*
     * Seach objects in the Map
     */
    private void startSearch(String text) {
        // Serarh text in Google Engine
        mSearch = new Search(text, mMapView.getMap(), 0);

        // add the request the dispatcher
        getDispatcher().addDataRequest(mSearch);

        Thread t = new Thread(new Runnable() {
            public void run() {
                while (!mSearch.isComplete()) {
                    Log.i(getString(R.string.main_title), ".");
                }
                NotificationManager nm = (NotificationManager)
                        getSystemService(NOTIFICATION_SERVICE);

                if (mSearch.numPlacemarks() > 0) {
                    nm.notifyWithText(999,
                            "Found " + mSearch.numPlacemarks() + " locations",
                            NotificationManager.LENGTH_SHORT, null);
                    goTo(0);
                } else {
                    nm.notifyWithText(1000,
                            "Could not find any location",
                            NotificationManager.LENGTH_SHORT, null);
                }
            }
        });
        t.start();
    }

    /*
     * Goto one of the 9 objects found
     */
    private void goTo(int itemNo) {
        MapPoint location = mSearch.getPlacemark(itemNo).getLocation();
        Point p = new Point(location.getLatitude(),
                location.getLongitude());
        MapController mc = mMapView.getController();
        mc.animateTo(p);
    }

    public MyMapView getMMapView() {
        return mMapView;
    }


    public Search getSearch() {
        return mSearch;
    }

    /*
     * Draw objects in the Map 
     */
    public class MyOverlay extends Overlay {
        public void draw(Canvas canvas, PixelCalculator pixelCalculator, boolean b) {
            super.draw(canvas, pixelCalculator, b);
            Paint paint1 = new Paint();
            Paint paint2 = new Paint();
            Paint paint3 = new Paint();

            paint1.setARGB(255, 49, 25, 173);
            paint2.setARGB(255, 255, 255, 255);
            paint3.setARGB(50, 49, 25, 173);

            int[] screenCoords = new int[2];
            
            /*
             * Draw a circle in my current location
             */
            if (myPoint.getLatitudeE6()!=0 && myPoint.getLongitudeE6()!=0) 
            {
            	float radioPixels = getPixelPerMeter(pixelCalculator);
            	pixelCalculator.getPointXY(myPoint, screenCoords);
            	if (gps) {
                    canvas.drawCircle(screenCoords[0], screenCoords[1], radioPixels * 30, paint1);
            	} 
            	canvas.drawCircle(screenCoords[0], screenCoords[1], radioPixels * 1700, paint3);
            }
            /*
             * Draw distance from current position to my location
             */
            Point currentPoint = mMapView.getMapCenter();
            Location currentLocation = new Location();
            currentLocation.setLatitude(currentPoint.getLatitudeE6()/1E6);
            currentLocation.setLongitude(currentPoint.getLongitudeE6()/1E6);
        	String text = Integer.toString((int) myLocation.distanceTo(currentLocation));
        	canvas.drawText(text,mMapView.getWidth()/2,mMapView.getHeight() - 10, paint1);
            /*
             * Draw friends
             */
        	Paint paint = new Paint();
        	paint.setTextSize(14);
        	Double lat = null;
        	Double lng = null;
        	Point point = new Point(0, 0);
            int[] friendScreenCoords = new int[2];
        	//Draw each friend with a line pointing to our own location.
        	for(Friend aFriend : BrowseMap.this.nearFriends){
        		lat = aFriend.itsLocation.getLatitude() * 1E6;
        		lng = aFriend.itsLocation.getLongitude() * 1E6;
        		point = new Point(lat.intValue(), lng.intValue());

        		// Converts lat/lng-Point to coordinates on the screen.
        		pixelCalculator.getPointXY(point, friendScreenCoords);
        		if(Math.abs(friendScreenCoords[0]) < 2000 && Math.abs(friendScreenCoords[1]) < 2000){
	        		// Draw a circle for this friend and his name
        			RectF oval = new RectF(friendScreenCoords[0] - 7, friendScreenCoords[1] + 7, 
        							friendScreenCoords[0] + 7, friendScreenCoords[1] - 7);
	        		
	            	// Setup a color for all friends
	        		paint.setStyle(Style.FILL);
	        		paint.setARGB(255, 255, 0, 0); // Nice red      		
	        		canvas.drawText(aFriend.itsName, friendScreenCoords[0] +9,
	        							friendScreenCoords[1], paint);
	        		
	            	// draw an oval around our friends location
	        		paint.setARGB(80, 255, 0, 0); // Nice red, more look through...
	            	paint.setStrokeWidth(1);
	        		canvas.drawOval(oval, paint);
	        		
	        		 // With a black stroke around the oval we drew before.
	        		paint.setARGB(255,0,0,0);
	        		paint.setStyle(Style.STROKE);
	        		canvas.drawCircle(friendScreenCoords[0], friendScreenCoords[1], 7, paint);
        		}
        	}
            /*
             * Draw objects search in Google Engine
             */
            Search search = BrowseMap.this.getSearch();
            if (search != null) {
                for (int i = 0; i < search.numPlacemarks(); i++) {
                    Placemark placemark = search.getPlacemark(i);
                    point = new Point(placemark.getLocation().getLatitude(),
                            placemark.getLocation().getLongitude());
                    pixelCalculator.getPointXY(point, screenCoords);
                    canvas.drawCircle(screenCoords[0], screenCoords[1], 9, paint1);
                    canvas.drawText(Integer.toString(i + 1),
                            screenCoords[0] - 4,
                            screenCoords[1] + 4, paint2);
                }

            }
        }
        /*
         * Determine the amount of pixels per meter
         */
        private float getPixelPerMeter(PixelCalculator calculator) 
        { 
                //calculate the lat/long span of the screen 
                int latSpan = mMapView.getLatitudeSpan(); 
                Point mapCenter = mMapView.getMapCenter(); 
                int latitudeToEdge = mapCenter.getLatitudeE6() + (latSpan/2); 
                int longitudeToEdge = mapCenter.getLongitudeE6(); 
                Location edgeLocation = new Location(); 
                edgeLocation.setLatitude((float)latitudeToEdge/1E6); 
                edgeLocation.setLongitude((float)longitudeToEdge/1E6); 
                Location centerLocation = new Location(); 
                centerLocation.setLatitude((float)(mapCenter.getLatitudeE6())/1E6); 
                centerLocation.setLongitude((float)(mapCenter.getLongitudeE6())/1E6); 

                //calculate the screen coordinates 
                Point edgePoint = new Point(latitudeToEdge, longitudeToEdge); 
                float HalfScreenDistanceX = centerLocation.distanceTo(edgeLocation); 
                int[] edgeScreenCoords = {0,0}; 
                int[] centerScreenCoords = {0,0}; 
                calculator.getPointXY(edgePoint, edgeScreenCoords); 
                calculator.getPointXY(mapCenter, centerScreenCoords); 


                //derive the pixel size in meters assuming x and y are equal 
                int screenSpanX = Math.abs(edgeScreenCoords[1] - centerScreenCoords[1]); 
                float pixelsPerMeter = ((float)screenSpanX)/HalfScreenDistanceX; 
                return pixelsPerMeter; 
        } 
    }


    /*
     * Detecta el Cell ID y el LAC y luego llama a la funcion convertCellID
     * para convertirlo a coordenadas
     * Se ejecuta asincronicamente luego de definir un objeto
     * de tipo ServiceStateHandler
     */
    private class ServiceStateHandler extends Handler {
        public void handleMessage(Message msg) {
            Log.i(getString(R.string.main_title), "Trying to detect Cell ID and LAC" + msg.what);
            switch (msg.what) {
                case MY_NOTIFICATION_ID:
                    ServiceState state = mPhoneStateReceiver.getServiceState();
                    int notification_cid = state.getCid();
                    int notification_lac = state.getLac();
                    try {
                        convertCellID(notification_cid , notification_lac);
                    } catch (Exception e) {
                        Log.e(getString(R.string.main_title), e.toString(), e);
                    }
                    break;
            }
        }
    }


    /*
     * Hace el Post en Google y Traduce el Cell ID/LAC a corrdenadas 
     */
    public void convertCellID(int cellid, int lac) throws Exception {
        String url = "http://www.google.com/glm/mmap";
        HttpURL httpURL = new HttpURL(url);
        HostConfiguration host = new HostConfiguration();
        host.setHost(httpURL.getHost(), httpURL.getPort());
        HttpConnection connection = connectionManager.getConnection(host);
        connection.open();

        PostMethod postMethod = new PostMethod(url);
        postMethod.setRequestEntity(new MyRequestEntity(cellid, lac));
        postMethod.execute(new HttpState(), connection);
        InputStream response = postMethod.getResponseBodyAsStream();
        DataInputStream dis = new DataInputStream(response);
        dis.readShort();
        dis.readByte();
        int code = dis.readInt();
        if (code == 0) {
            double lat = (double) dis.readInt() / 1E6;
            double lng = (double) dis.readInt() / 1E6;
            dis.readInt();
            dis.readInt();
            dis.readUTF();
            Log.i(getString(R.string.main_title), "Lat, Long: " + lat + "," + lng);

            /*
             * Store Lat y Long in MyLocation Object
             */
            myLocation.setLatitude(lat);
            myLocation.setLongitude(lng);
			this.virtualgps = true;
        }
 		myPoint = new Point((int) (myLocation.getLatitude() * 1E6), (int)
                (myLocation.getLongitude() * 1E6));
 		/*
 		 * Refresh the Location Objects Lists
 		 */
        connection.close();
        connection.releaseConnection();
		this.refreshObjectsList(NEARFRIEND_MAX_DISTANCE);
    }


    /*
     * Arma el paquete con CellID y LAC
     * que luego será enviado por Post a Google para que lo
     * convierta a Coordenadas
     */
    private static class MyRequestEntity implements RequestEntity {
        int cellId, lac;

        public MyRequestEntity(int cellId, int lac) {
            this.cellId = cellId;
            this.lac = lac;
        }

        public boolean isRepeatable() {
            return true;
        }

        public void writeRequest(OutputStream outputStream) throws IOException {
            DataOutputStream os = new DataOutputStream(outputStream);
            os.writeShort(21);
            os.writeLong(0);
            os.writeUTF("fr");
            os.writeUTF("Sony_Ericsson-K750");
            os.writeUTF("1.3.1");
            os.writeUTF("Web");
            os.writeByte(27);

            os.writeInt(0);
            os.writeInt(0);
            os.writeInt(3);
            os.writeUTF("");
            os.writeInt(cellId);  // CELL-ID
            os.writeInt(lac);     // LAC
            os.writeInt(0);
            os.writeInt(0);
            os.writeInt(0);
            os.writeInt(0);
            os.flush();
        }

        public long getContentLength() {
            return -1;
        }

        public String getContentType() {
            return "application/binary";
        }
    }


    /*
     * Method executed when Subactivity Search.java returns
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    String data, Bundle extras) {
        if (requestCode == GET_SEARCH_TEXT) {
            startSearch(data);
        }
    }


    /*
     * Read friends from Phone Contacts and put them in a array
     */
    private void refreshObjectsList(long maxDistanceInMeter){
    	// TODO: Web objects from Web Service instead of Phone Contacts
    	// TODO: In the application setting specify how often this refresh will occurr
//    	java.util.Date currentTime = new java.util.Date();
		Cursor c = getContentResolver().query(People.CONTENT_URI, null, null, null, People.NAME + " ASC");
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
						String latid = groupStr.substring(groupStr.indexOf(":") + 1, groupStr.indexOf(","));
						String longit = groupStr.substring(groupStr.indexOf(",") + 1, groupStr.indexOf("#"));
						friendLocation.setLongitude(Float.parseFloat(longit));
						friendLocation.setLatitude(Float.parseFloat(latid));
					}
				}
				if(friendLocation != null 
						&& myLocation.distanceTo(friendLocation) < maxDistanceInMeter) 
				{
					String friendName = c.getString(nameColumn);
					nearFriends.add(new Friend(friendLocation, friendName));
				}
			} while (c.next());
		}
	}


	/* 
	 * Register with out LocationManager to send us 
	 * an intent (whos Action-String we defined above)
	 * when  an intent to the location manager,
	 * that we want to get informed on changes to our own position.
	 * This is one of the hottest features in Android.
	 */
	private void setupForGPSAutoRefreshing() {
		final long MINIMUM_DISTANCECHANGE_FOR_UPDATE = 25; // in Meters
		final long MINIMUM_TIME_BETWEEN_UPDATE = 5000; // in Milliseconds
		// Get the first provider available
		List<LocationProvider> providers = this.myLocationManager.getProviders();
		LocationProvider provider = providers.get(0);
		// If GPS is available setup refresh
		if (myLocationManager.getProviderStatus("gps")==LocationProvider.AVAILABLE){
			this.gps = true;
			this.myLocationManager.requestUpdates(provider, MINIMUM_TIME_BETWEEN_UPDATE,
					MINIMUM_DISTANCECHANGE_FOR_UPDATE, new Intent(MY_LOCATION_CHANGED_ACTION));
			
			/* Create an IntentReceiver, that will react on the
			 * Intents we made our LocationManager to send to us. 
			 */ 
			this.myIntentReceiver = new MyIntentReceiver();
			Log.i(getString(R.string.main_title), "GPS Detected and setup for autorefreshing");
			}
		else {
			this.gps = false;
	        /*
	         * Set up the handler for receiving events for service state. Like Cell-Id and LAC to determine location
	         */
	        mPhoneStateReceiver = new PhoneStateIntentReceiver(this, new ServiceStateHandler());
	        mPhoneStateReceiver.notifyServiceState(MY_NOTIFICATION_ID);
	        mPhoneStateReceiver.notifyPhoneCallState(MY_NOTIFICATION_ID);
	        mPhoneStateReceiver.notifySignalStrength(MY_NOTIFICATION_ID);
	        mPhoneStateReceiver.registerIntent();
			Log.i(getString(R.string.main_title), "Virtual GPS Detected and setup for autorefreshing");
		}
		/* 
		 * In onResume() the following method will be called automatically!
		 * registerReceiver(this.myIntentReceiver, this.myIntentFilter); 
		 */
	}    

	/**
	 * This tiny IntentReceiver updates
	 * our stuff as we receive the intents 
	 * (LOCATION_CHANGED_ACTION) we told the 
	 * myLocationManager to send to us. 
	 */
	class MyIntentReceiver extends IntentReceiver {
		@Override
		public void onReceiveIntent(Context context, Intent intent) {
			if(doUpdates)
				updateView();
		}
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
		registerReceiver(this.myIntentReceiver, this.myIntentFilter);
	}
	
	/**
	 * Make sure to stop the animation when we're no longer on screen,
	 * failing to do so will cause a lot of unnecessary cpu-usage!
	 */
	@Override
	public void onFreeze(Bundle icicle) {
		this.doUpdates = false;
		unregisterReceiver(this.myIntentReceiver);
		super.onFreeze(icicle);
	}

	private void updateView() {
		// Refresh our gps-location
		this.myLocation = myLocationManager.getCurrentLocation("gps");
		this.myPoint = new Point((int) (myLocation.getLatitude() * 1E6), (int) (myLocation.getLongitude() * 1E6));
		
		/* Redraws the mapViee, which also makes our 
		 * OverlayController redraw our Circles and Lines */
		// this.mMapView.invalidate();
		
		/* As the location of our Friends is static and 
		 * for performance-reasons, we do not call this */
		this.refreshObjectsList(NEARFRIEND_MAX_DISTANCE);
	}
	
}
