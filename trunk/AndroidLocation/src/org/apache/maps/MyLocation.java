package org.apache.maps;

import java.util.List;
import android.telephony.PhoneStateIntentReceiver;
import com.google.android.maps.Point;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentReceiver;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Handler;
import android.os.Message;
import android.telephony.ServiceState;
import android.location.Location;

public class MyLocation {

	public boolean gps = false;
	public boolean virtualgps = false;
    public Location location = new Location();
	public Point point = null;

	protected final IntentFilter myIntentFilter = new IntentFilter(MY_LOCATION_CHANGED_ACTION);
	protected Context aContext = null;
	protected MyIntentReceiver myIntentReceiver = null; 
	protected PhoneStateIntentReceiver mPhoneStateReceiver;
	protected static final int MY_NOTIFICATION_ID = 0x100;
	protected LocationManager myLocationManager = null;
	protected static final String MY_LOCATION_CHANGED_ACTION = new String("android.intent.action.LOCATION_CHANGED");
	final long MINIMUM_DISTANCECHANGE_FOR_UPDATE = 25; // in Meters
	final long MINIMUM_TIME_BETWEEN_UPDATE = 5000; // in Milliseconds

	public MyLocation (Context cContext){
		aContext = cContext;
		myLocationManager = (LocationManager) aContext.getSystemService(Context.LOCATION_SERVICE);

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
			myIntentReceiver = new MyIntentReceiver();

			// Called automatically from OnResume()
			// aContext.registerReceiver(this.myIntentReceiver, this.myIntentFilter);

			}
		else {
			this.gps = false;
	        /*
	         * Set up the handler for receiving events for service state. Like Cell-Id and LAC to determine location
	         */
	        mPhoneStateReceiver = new PhoneStateIntentReceiver(aContext, new ServiceStateHandler());
	        mPhoneStateReceiver.notifyServiceState(MY_NOTIFICATION_ID);
	        mPhoneStateReceiver.notifyPhoneCallState(MY_NOTIFICATION_ID);
	        mPhoneStateReceiver.notifySignalStrength(MY_NOTIFICATION_ID);
	        mPhoneStateReceiver.registerIntent();
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
			MyLocation.this.location = myLocationManager.getCurrentLocation("gps");
			MyLocation.this.point = new Point((int) (location.getLatitude() * 1E6), (int) (location.getLongitude() * 1E6));
		}
	}

	
	/*
     * Detecta el Cell ID y el LAC y luego llama a la funcion convertCellID
     * para convertirlo a coordenadas
     * Se ejecuta asincronicamente luego de definir un objeto
     * de tipo ServiceStateHandler
     */
    class ServiceStateHandler extends Handler {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MY_NOTIFICATION_ID:
                    ServiceState state = mPhoneStateReceiver.getServiceState();
                    int notification_cid = state.getCid();
                    int notification_lac = state.getLac();
                    try {
                    	location = ConvertCellID(notification_cid , notification_lac);
            			point = new Point((int) (location.getLatitude() * 1E6), (int) (location.getLongitude() * 1E6));
                    } catch (Exception e) {
                    }
                    break;
            }
        }
    }
	
	
    private Location ConvertCellID(int cid,int lac){
    	//TODO: llamar a API de server que devuelta Location y setear virtualgps=1 si trajo correctamente
    	return null;
    }

//TODO: Hacer que refresque cada x tiempo    
    
}

