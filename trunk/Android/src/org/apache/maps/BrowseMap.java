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
import com.google.android.maps.Point;
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

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mMapView = new MyMapView(this);

        // Set up the handler for receiving events for service state etc.
        mPhoneStateReceiver = new PhoneStateIntentReceiver(this, new ServiceStateHandler());
        mPhoneStateReceiver.notifyServiceState(MY_NOTIFICATION_ID);
        mPhoneStateReceiver.notifyPhoneCallState(MY_NOTIFICATION_ID);
        mPhoneStateReceiver.notifySignalStrength(MY_NOTIFICATION_ID);
        mPhoneStateReceiver.registerIntent();
        
        MapController mc = mMapView.getController();
        mc.zoomTo(9);
        setContentView(mMapView);

        mMapView.createOverlayController().add(
                new MyOverlay(this,currentlocation),
                true);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_I) {
            // Zoom In
            int level = mMapView.getZoomLevel();
            mMapView.getController().zoomTo(level + 1);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_O) {
            // Zoom Out
            int level = mMapView.getZoomLevel();
            mMapView.getController().zoomTo(level - 1);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_S) {
            // Switch on the satellite images
            mMapView.toggleSatellite();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_T) {
            // Switch on traffic overlays
            mMapView.toggleTraffic();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_0) {
            MapController mc = mMapView.getController();
            mc.animateTo(currentlocation);
            mc.zoomTo(14);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_F) {
            Intent intent = new Intent(BrowseMap.this, org.apache.maps.Search.class);
            startSubActivity(intent, GET_SEARCH_TEXT);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_P) {
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
        }
        return false;
    }

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
