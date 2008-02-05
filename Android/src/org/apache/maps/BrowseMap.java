package org.apache.maps;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;

import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateIntentReceiver;
import android.telephony.ServiceState;
import android.util.Log;
import android.view.KeyEvent;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MyMapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Point;
import com.google.googlenav.Placemark;
import com.google.googlenav.Search;
import com.google.googlenav.map.MapPoint;

public class BrowseMap extends MapActivity {
    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private MyMapView mMapView;
    private Search mSearch;

    private String LOG_TAG = "BrowseMap";
    private int GET_SEARCH_TEXT = 0;

    int notification_cid = -1;
    int notification_lac = -1;
    double coordinate_lat = -1;
    double coordinate_long = -1;
    Point currentlocation = new Point((int) 0, (int) 0);

    private static final int MY_NOTIFICATION_ID = 0x100;
    /*
     * Constructors (or the Entry-Point of it all)
     * @see com.google.android.maps.MapActivity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle icicle) {
    	/*
    	 * Execute Parent OnCreate Method
    	 */
        super.onCreate(icicle);
        /*
         * Set up the handler for receiving events for service state. Like Cell-Id and LAC to determine location
         */
        mPhoneStateReceiver = new PhoneStateIntentReceiver(this, new ServiceStateHandler());
        mPhoneStateReceiver.notifyServiceState(MY_NOTIFICATION_ID);
        mPhoneStateReceiver.notifyPhoneCallState(MY_NOTIFICATION_ID);
        mPhoneStateReceiver.notifySignalStrength(MY_NOTIFICATION_ID);
        mPhoneStateReceiver.registerIntent();
        /*
         * Create a new MapView
         */
        mMapView = new MyMapView(this);
        /*
         * Show the Map
         */
        setContentView(mMapView);
		/* 
		 * MapController is capable of zooming 
		 * and animating and stuff like that 
		 */
        MapController mc = mMapView.getController();
        /*
         * Zoom not so far
         */
        mc.zoomTo(9);
        
		/* With these objects we are capable of 
		 * drawing graphical stuff on top of the map */
		mMapView.createOverlayController().add(new MyOverlay(),true);
		
    }
    
    /*
     * When the user hit a Key
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */

        public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_I) {
            // Zoom In
            int level = mMapView.getZoomLevel();
            mMapView.getController().zoomTo(level + 1);
        } else if (keyCode == KeyEvent.KEYCODE_O) {
            // Zoom Out
            int level = mMapView.getZoomLevel();
            mMapView.getController().zoomTo(level - 1);
        } else if (keyCode == KeyEvent.KEYCODE_S) {
            // Switch on the satellite images
            mMapView.toggleSatellite();
        } else if (keyCode == KeyEvent.KEYCODE_T) {
            // Switch on traffic overlays
            mMapView.toggleTraffic();
        } else if (keyCode == KeyEvent.KEYCODE_0) {
        	/*
        	 * Va hacia My Location
        	 */
        	MapController mc = mMapView.getController();
            mc.animateTo(currentlocation);
            mc.zoomTo(14);
        } else if (keyCode == KeyEvent.KEYCODE_F) {
            Intent intent = new Intent(BrowseMap.this, org.apache.maps.Search.class);
            startSubActivity(intent, GET_SEARCH_TEXT);
        } else if (keyCode == KeyEvent.KEYCODE_P) {
            startSearch("Pizza");
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
        }
        return true;
    }

    /*
     * Seach objects in the Map
     */
    private void startSearch(String text) {
        // W00t! Search for Pizza near the center of the map
        mSearch = new Search(text, mMapView.getMap(), 0);

        // add the request the dispatcher
        getDispatcher().addDataRequest(mSearch);

        Thread t = new Thread(new Runnable() {
            public void run() {
                while (!mSearch.isComplete()) {
                    Log.i(LOG_TAG, ".");
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

    private static HttpConnectionManager connectionManager = new SimpleHttpConnectionManager();

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
            if (currentlocation.getLatitudeE6()!=0 && currentlocation.getLongitudeE6()!=0) 
            {
                pixelCalculator.getPointXY(currentlocation, screenCoords);
                canvas.drawCircle(screenCoords[0], screenCoords[1], 49, paint3);
            }
            /*
             * Draw found objects
             */
            Search search = BrowseMap.this.getSearch();
            if (search != null) {
                for (int i = 0; i < search.numPlacemarks(); i++) {
                    Placemark placemark = search.getPlacemark(i);
                    Point point = new Point(placemark.getLocation().getLatitude(),
                            placemark.getLocation().getLongitude());
                    pixelCalculator.getPointXY(point, screenCoords);
                    canvas.drawCircle(screenCoords[0], screenCoords[1], 9, paint1);
                    canvas.drawText(Integer.toString(i + 1),
                            screenCoords[0] - 4,
                            screenCoords[1] + 4, paint2);
                }

            }
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
            Log.i("LocateMe", "In handleMessage : " + msg.what);
            switch (msg.what) {
                case MY_NOTIFICATION_ID:
                    ServiceState state = mPhoneStateReceiver.getServiceState();
                    notification_cid = state.getCid();
                    notification_lac = state.getLac();
                    try {
                        convertCellID(notification_cid , notification_lac);
                    } catch (Exception e) {
                        Log.e("LocateMe", e.toString(), e);
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
            double lat = (double) dis.readInt() / 1000000D;
            double lng = (double) dis.readInt() / 1000000D;
            dis.readInt();
            dis.readInt();
            dis.readUTF();
            Log.i("LocateMe", "Lat, Long: " + lat + "," + lng);

            /*
             * Guarda Lat y Long en variables publicas
             */
            coordinate_lat = lat;
            coordinate_long = lng;
        } else {
        	/* Si no encuentra ese CID/LAC lo manda a una coordenada cualquiera*/
            coordinate_lat =  -34.407929;
            coordinate_long = -58.72461;
        }
 		currentlocation = new Point((int) (coordinate_lat * 1000000), (int)
                (coordinate_long * 1000000));
        connection.close();
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

    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    String data, Bundle extras) {
        if (requestCode == GET_SEARCH_TEXT) {
            startSearch(data);
        }
    }
}
