package org.apache.maps;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.location.Location;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MapController;
import com.google.android.maps.Overlay;
import com.google.android.maps.Point;

public class BrowseMap extends MapActivity {
	/*
	 * Define variables
	 */
    private MapView mMapView;
    private int GET_SEARCH_TEXT = 0;
    protected MyLocation myLocation = null;
	
	/*
	 *  Constants
	 */
	protected static final int NEARFRIEND_MAX_DISTANCE = 10000000;  // 10.000km
	protected static final int ZOOM_INITIAL_LEVEL = 11;
	public LocationObjects myObjects = null; // Array to store List of friends
    
    /*
     * Start
     */
    @Override
    public void onCreate(Bundle icicle) {
    	// Execute Parent OnCreate Method
        super.onCreate(icicle);
		setContentView(R.layout.browsemap);

		// Create friends Objetcs
		myObjects = new LocationObjects(NEARFRIEND_MAX_DISTANCE, BrowseMap.this); // Array to store List of friends
		// Detect My Current Location (GPS or Virtual GPS)
		myLocation = new MyLocation(BrowseMap.this);

		mMapView = (MapView)this.findViewById(R.id.myMapView);
		
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
		    	if (myLocation.gps || myLocation.virtualgps) {
		        	mMapView.getController().animateTo(myLocation.point);
		        	mMapView.getController().zoomTo(ZOOM_INITIAL_LEVEL);
		    	} else {
		    		Toast.makeText(BrowseMap.this, getString(R.string.couldnotdetectlocation), Toast.LENGTH_SHORT).show();
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
	    	if (myLocation.point!=null) {
	        	mMapView.getController().animateTo(myLocation.point);
	        	mMapView.getController().zoomTo(ZOOM_INITIAL_LEVEL);
	    	} else {
	    		Toast.makeText(BrowseMap.this, getString(R.string.couldnotdetectlocation), Toast.LENGTH_SHORT).show();
	    	}
	        return true;
        } else if (keyCode == KeyEvent.KEYCODE_F) {
        	/*
        	 * Open the Seach Screen
        	 */
            Intent intent = new Intent(BrowseMap.this, org.apache.maps.Search.class);
            startSubActivity(intent, GET_SEARCH_TEXT);
	        return true;
        } 
/*        else if (keyCode == KeyEvent.KEYCODE_P) {
        	// Search for Pizza Objects
            startSearch("Pizza");
	        return true;
        } else if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_9) {
            int item = keyCode - KeyEvent.KEYCODE_1;
            if (mSearch != null && mSearch.numPlacemarks() > item) {
	    		Toast.makeText(BrowseMap.this, mSearch.getPlacemark(item).getDetailsDescriptor(), Toast.LENGTH_SHORT).show();
                goTo(item);
            }
	        return true;
        } 
        */
        return false;
    }

    /*
     * Seach objects in the Map
     */
/*
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
                if (mSearch.numPlacemarks() > 0) {
		    		Toast.makeText(BrowseMap.this, "Found " + mSearch.numPlacemarks() + " locations", Toast.LENGTH_SHORT).show();
                    goTo(0);
                } else {
		    		Toast.makeText(BrowseMap.this, "Could not find any location", Toast.LENGTH_SHORT).show();
                }
            }
        });
        t.start();
    }
*/
    /*
     * Goto one of the 9 objects found
     */
/*
    private void goTo(int itemNo) {
        MapPoint location = mSearch.getPlacemark(itemNo).getLocation();
        Point p = new Point(location.getLatitude(),
                location.getLongitude());
        MapController mc = mMapView.getController();
        mc.animateTo(p);
    }
*/
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
//          if (myLocation.point.getLatitudeE6()!=0 && myLocation.point.getLongitudeE6()!=0) 
            if (myLocation.point!=null) 
            {
            	float radioPixels = getPixelPerMeter(pixelCalculator);
            	pixelCalculator.getPointXY(myLocation.point, screenCoords);
            	if (myLocation.gps) {
                    canvas.drawCircle(screenCoords[0], screenCoords[1], radioPixels * 30, paint1);
            	} 
            	canvas.drawCircle(screenCoords[0], screenCoords[1], radioPixels * 1700, paint3);
            }
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
        	for(Friend aFriend : BrowseMap.this.myObjects.nearFriends){
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
	        		canvas.drawText(aFriend.itsUsername, friendScreenCoords[0] +9,
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
/*            Search search = BrowseMap.this.getSearch();
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
*/
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
     * Method executed when Subactivity Search.java returns
     */
/*
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    String data, Bundle extras) {
        if (requestCode == GET_SEARCH_TEXT) {
            startSearch(data);
        }
    }
*/
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

}
