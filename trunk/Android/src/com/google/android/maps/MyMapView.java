package com.google.android.maps;

import android.content.Context;
import com.google.googlenav.map.Map;
import com.google.googlenav.map.TrafficService;

public class MyMapView extends MapView {
    public MyMapView(Context context) {
        super(context);
    }

    void setup(Map map, TrafficService traffic) {
        mMap = map;
        super.setup(map, traffic);
    }

    public Map getMap() {
        return mMap;
    }

    private Map mMap;
}
