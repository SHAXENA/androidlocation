package org.apache.maps;

import android.graphics.Canvas;
import android.graphics.Paint;
import com.google.android.maps.Overlay;
import com.google.android.maps.Point;
import com.google.googlenav.Placemark;
import com.google.googlenav.Search;

public class MyOverlay extends Overlay {
    BrowseMap mMap;
    Point mCurrentlocation;
    Paint paint1 = new Paint();
    Paint paint2 = new Paint();

    public MyOverlay(BrowseMap map, Point currentlocation) {
        mMap = map;
        mCurrentlocation = currentlocation;
        paint1.setARGB(255, 49, 25, 173);
        paint2.setARGB(255, 255, 255, 255);
    }

    public void draw(Canvas canvas, PixelCalculator pixelCalculator, boolean b) {
        super.draw(canvas, pixelCalculator, b);
        int[] screenCoords = new int[2];
        
        if (mCurrentlocation.getLatitudeE6()!=0) 
        {
            pixelCalculator.getPointXY(mCurrentlocation, screenCoords);
            canvas.drawCircle(screenCoords[0], screenCoords[1], 19, paint1);
        }
        Search search = mMap.getSearch();
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
