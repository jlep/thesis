package fi.hiit.complesense.core;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import fi.hiit.complesense.Constants;
import fi.hiit.complesense.connection.AsyncStreamClient;
import fi.hiit.complesense.json.JsonSSI;
import fi.hiit.complesense.util.SensorUtil;

/**
 * Created by hxguo on 11.11.2014.
 */
public class LocationDataCollectionThread extends AbsSystemThread
{

    public static final String TAG = LocationDataCollectionThread.class.getSimpleName();
    private static final int LATITUDE = 0;
    public static final int LONGITUDE = 1;
    private JSONObject jsonGeoCoords = new JSONObject();

    private final LocationManager locationManager;
    private final CountDownLatch startSignal;
    private final AsyncStreamClient asyncStreamClient;
    private LocationListener mLocationListener = null;
    private ByteBuffer buffer;
    private int isJSON = 1;


    private LocationDataCollectionThread(ServiceHandler serviceHandler,
                                           Context context,
                                           AsyncStreamClient asyncStreamClient,
                                           CountDownLatch latch) throws JSONException {
        super(TAG, serviceHandler);
        locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        startSignal = latch;
        this.asyncStreamClient = asyncStreamClient;
        initBuffer();
    }

    private void initBuffer() throws JSONException
    {
        jsonGeoCoords.put(JsonSSI.TIMESTAMP, System.currentTimeMillis());
        jsonGeoCoords.put(JsonSSI.SENSOR_TYPE, SensorUtil.SENSOR_GPS);
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(LATITUDE, 0d);
        jsonArray.put(LONGITUDE, 0d);
        jsonGeoCoords.put(JsonSSI.SENSOR_VALUES,jsonArray);
        buffer = ByteBuffer.allocate(Constants.BYTES_INT + jsonGeoCoords.toString().getBytes().length);
    }

    @Override
    public void run()
    {
        Log.i(TAG, " starts running at thread: " + Thread.currentThread().getId());
        try {
            startSignal.await();

            mLocationListener = new LocationListener()
            {
                @Override
                public void onLocationChanged(Location location) {
                    try
                    {
                        jsonGeoCoords.put(JsonSSI.TIMESTAMP, System.currentTimeMillis());
                        jsonGeoCoords.put(JsonSSI.SENSOR_TYPE, SensorUtil.SENSOR_GPS);
                        JSONArray jsonArray = new JSONArray();
                        jsonArray.put(LATITUDE, location.getLatitude());
                        jsonArray.put(LONGITUDE, location.getLongitude());
                        jsonGeoCoords.put(JsonSSI.SENSOR_VALUES,jsonArray);

                        buffer.clear();
                        buffer.putInt(isJSON);
                        buffer.put(jsonGeoCoords.toString().getBytes());

                        asyncStreamClient.send(buffer.array());
                    } catch (JSONException e) {
                        Log.i(TAG, e.toString());
                    }

                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {

                }

                @Override
                public void onProviderEnabled(String s) {

                }

                @Override
                public void onProviderDisabled(String s) {

                }
            };

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,0, mLocationListener);

        } catch (InterruptedException e) {

        }

    }

    @Override
    public void stopThread() {
        keepRunning = false;
        if(mLocationListener!=null)
            locationManager.removeUpdates(mLocationListener);
    }
}