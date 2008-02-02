package org.apache3;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.ContentURI;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateIntentReceiver;
import android.telephony.ServiceState;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LocateMe extends Activity implements View.OnClickListener {
    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private EditText mEditLac;
    private EditText mEditCid;
    private Button mLocateMe;
    private Button mUpdate;

    int notification_cid = -1;
    int notification_lac = -1;

    private static final int MY_NOTIFICATION_ID = 0x100;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);

        // Set up the handler for receiving events for service state etc.
        mPhoneStateReceiver = new PhoneStateIntentReceiver(this, new ServiceStateHandler());
        mPhoneStateReceiver.notifyServiceState(MY_NOTIFICATION_ID);
        mPhoneStateReceiver.notifyPhoneCallState(MY_NOTIFICATION_ID);
        mPhoneStateReceiver.notifySignalStrength(MY_NOTIFICATION_ID);
        mPhoneStateReceiver.registerIntent();

        // Round up the troops
        mEditLac = (EditText) findViewById(R.id.lac);
        mEditCid = (EditText) findViewById(R.id.cell_id);

        // Set the onclick handler
        mLocateMe = (Button) findViewById(R.id.LocateMe);
        mLocateMe.setOnClickListener(this);
        mUpdate = (Button) findViewById(R.id.update);
        mUpdate.setOnClickListener(this);
    }

    private static HttpConnectionManager connectionManager = new SimpleHttpConnectionManager();

    public void onClick(View view) {
        if (view == mLocateMe) {
            Log.i("LocateMe", "onClick - Locate Me!");
            try {
                int cellid = Integer.parseInt(mEditCid.getText().toString());
                int lac = Integer.parseInt(mEditLac.getText().toString());
                browseTo(cellid, lac);
            } catch (Exception e) {
                Log.e("LocateMe", e.toString(), e);
            }
        } else if (view == mUpdate) {
            updateTextFields();
        }
    }

    /**
     * Update the UI from the values stored in the notification handler
     */
    private void updateTextFields() {
        mEditCid.setText("" + notification_cid);
        mEditLac.setText("" + notification_lac);
    }

    /**
     * Notification Handler for Phone events
     */
    private class ServiceStateHandler extends Handler {
        private boolean updateOnce = false;

        public void handleMessage(Message msg) {
            Log.i("LocateMe", "In handleMessage : " + msg.what);
            switch (msg.what) {
                case MY_NOTIFICATION_ID:
                    ServiceState state = mPhoneStateReceiver.getServiceState();
                    notification_cid = state.getCid();
                    notification_lac = state.getLac();

                    if (updateOnce == false) {
                        updateOnce = true;
                        updateTextFields();
                    }
                    break;
            }
        }
    }

    public void browseTo(int cellid, int lac) throws Exception {
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

            /**
             * Start Google Maps and zoom into the lat/long
             */
            ContentURI uri = ContentURI.create("geo:" + lat
                    + "," + lng);
            Intent intent = new Intent("android.intent.action.VIEW", uri);
            startActivity(intent);

        } else {
            NotificationManager nm = (NotificationManager)
                    getSystemService(NOTIFICATION_SERVICE);
            nm.notifyWithText(100,
                    "Could not find lat/long information",
                    NotificationManager.LENGTH_SHORT, null);
        }
        connection.close();
    }

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
}
