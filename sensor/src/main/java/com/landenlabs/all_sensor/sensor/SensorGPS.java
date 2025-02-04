/*
 * Unpublished Work © 2020 Dennis Lang (LanDen Labs) landenlabs@gmail.com
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * @author Dennis Lang
 * @see http://LanDenLabs.com/
 */

package com.landenlabs.all_sensor.sensor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.landenlabs.all_sensor.db.DbSensorGpsHourly;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.utils.SpanUtil;
import com.landenlabs.all_sensor.utils.StrUtils;

import java.util.Locale;

/**
 * Gather gps data.
 */
public class SensorGPS extends SensorItem  {

    public static final String NAME = "GPS";
    public static final SensorGPS EMPTY = new SensorGPS();
    public static boolean useGpsPower = false;

    private static final String DB_NAME = "dbGpsLocation.db";
    // private static final String DB_NAME = "dbGpsElev.db";
    // private static final String DB_NAME = "dbGpsSpeed.db";

    private final LocationUpdate locationUpdate = new LocationUpdate();
    private final LocationUpdate locationUpdate2 = new LocationUpdate();    // Pixel bug
    private final LocationSuccess locationSuccess = new LocationSuccess();
    private final LocationFailure locationFailure = new LocationFailure();
    private long lastRequestMilli = 0L;
    private Location lastAndroidLocation = null;

    private Context context;

    // ---------------------------------------------------------------------------------------------
    public SensorGPS( ) {
        super(null);
        this.name = NAME;
        this.dbFile = null;
    }

    // Required - used by xxxxManager to initalize
    public SensorGPS(@NonNull IwxManager wxManager) {
        super(wxManager);
        this.name = NAME;
        this.dbFile = wxManager.getContext().getDatabasePath(DB_NAME);
    }

    @Override
    protected void openDatabase(String sensorName) {
        if (!isDbOpen()) {
            try {
                dbSensorHourly = new DbSensorGpsHourly(dbFile.getAbsolutePath());
                state.failIf(dbSensorHourly.openWrite(true));
            } catch (Exception ex) {
                ALog.e.tagMsg(this, "SQL database ", ex);
                state.fail(ex);
            }
        }
    }

    @Override
    public boolean hasSensor(@NonNull Context context) {
        return true;
    }

    @Override
    @NonNull
    public SensorSummary getSummary(@NonNull Context context, @NonNull AbsCfg cfg) {
        SensorSummary summary = super.getSummary(context, cfg);

        summary.strTime = cfg.timeFmt(lastMilli);
        summary.strDate = cfg.dateFmt(lastMilli);

        String strGps = (lastAndroidLocation == null)
                ? "No GPS"
                : String.format(Locale.US, "%.2f,%.2f", lastAndroidLocation.getLatitude(), lastAndroidLocation.getLongitude());
        SpannableString ss1 = SpanUtil.SString(strGps, SpanUtil.SS_GREEN, 0, strGps.length());
        summary.strValue = ss1;
        summary.numValue = 0f;
        summary.numTime = lastMilli;
        summary.data = lastAndroidLocation;
        return summary;
    }

    @Override
    public int iValue() {
        return 0;
    }

    @Override
    public float fValue(int iValue) {
        return Float.NaN;
    }

    @Override
    public CharSequence sValue(int iValue) {
        return "no GPS";
    }

    public void start(@NonNull IwxManager manager, @NonNull ExecState startStatus) {
        initGps(manager.getContext());
    }

    public void stop(IwxManager manager) {
        FusedLocationProviderClient locProvider = LocationServices.getFusedLocationProviderClient(context);
        locProvider.flushLocations();
        locProvider.removeLocationUpdates(locationUpdate);
        locProvider.removeLocationUpdates(locationUpdate2);
    }

    private void gotGPSLocation(@NonNull Location location) {
        this.lastAndroidLocation = location;
        // openDatabase(name);

        // lastValue = location.getSpeed();
        // lastValue = (float)location.getAltitude();
        // lastValue = (float)location.getLatitude();
        // lastValue = (float)location.getLongitude();

        lastMilli = System.currentTimeMillis();

        SensorAndroid.GpsLocation sample = new SensorAndroid.GpsLocation(lastMilli, location);
        add(sample);
    }

    private static final long LOCATION_UPDATE_REQUEST_EXPIRATION_MILLI = 30000; // 30 seconds
    @SuppressLint("MissingPermission")
    void initGps(@NonNull Context context) {
        this.context = context;
        long delta = System.currentTimeMillis() - lastRequestMilli;
        if (isLocationPermissionsGranted(context)
                && delta > LOCATION_UPDATE_REQUEST_EXPIRATION_MILLI * 2) {
            // Force searching process, single GPS update with 30sec timeout.
            // Proceed with No Power request for 15sec and also High accuracy 30sec timeout.
            ALog.d.tagMsg(this, "start Fused Location update ");
            try {
                Looper looper = Looper.getMainLooper();
                FusedLocationProviderClient locProvider = LocationServices.getFusedLocationProviderClient(context);
                // locProvider.getLocationAvailability() ;
                locProvider.flushLocations();
                locProvider.removeLocationUpdates(locationUpdate);
                ALog.d.tagMsg(this, "Request GPS(fuse)");

                locProvider.getLastLocation()
                        .addOnSuccessListener(locationSuccess)
                        .addOnFailureListener(locationFailure);

                // Do a quick no power gps request for a quick 'less' accurate location
                LocationRequest requestNoPower = new LocationRequest.Builder(5000)
                    .setDurationMillis(LOCATION_UPDATE_REQUEST_EXPIRATION_MILLI /2)
                    .setPriority(LocationRequest.PRIORITY_NO_POWER)
                    .setIntervalMillis(5000)
                    .setMaxUpdates(2)
                    .build();
                Task<Void> noPowerTask = locProvider.requestLocationUpdates(requestNoPower, locationUpdate, looper);
                ALog.i.tagMsg(this, "GPS no power request=", StrUtils.toString(noPowerTask));

                if (useGpsPower) {
                    // Add a more accurate high accuracy request with 30 second timeout
                    LocationRequest requestBalance =  new LocationRequest.Builder(5000)
                            .setDurationMillis(LOCATION_UPDATE_REQUEST_EXPIRATION_MILLI)
                            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                            .setIntervalMillis(5000)
                            .setMaxUpdates(2)
                            .build();
                    // Pixel bug - can't set numUpdates to 1 or have multiple requests on same callback.
                    // Work around - use 2nd instance of callback.
                    Task<Void> highPowerTask = locProvider.requestLocationUpdates(requestBalance, locationUpdate2, looper);
                    ALog.i.tagMsg(this, "GPS high power request=", StrUtils.toString(highPowerTask));
                }

                lastRequestMilli = System.currentTimeMillis();
            } catch (IllegalStateException ex) {
                ALog.e.tagMsg(this, "Failed to request GPS(fuse) ", ex);
            }
        } else {
            ALog.e.tagMsg(this, "Ignore rapid GPS requests ");
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Static helper methods
    public static boolean isLocationPermissionsGranted(@NonNull Context context) {
        boolean locFine = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean locCoarse = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return locFine && locCoarse;
    }

    // =============================================================================================
    private class LocationSuccess implements OnSuccessListener<Location> {
        @Override
        public void onSuccess(Location location) {
            ALog.i.tagMsg(this, "GPS last location=", location);
            if (location != null) {
                gotGPSLocation(location);
            }
        }
    }
    private static class LocationFailure implements OnFailureListener {
        @Override
        public void onFailure(@NonNull Exception ex) {
            ALog.e.tagMsg(this, "GPS load location failure ", ex);
        }
    }

    // =============================================================================================
    private class LocationUpdate extends LocationCallback {
        @Override
        public void onLocationResult(@NonNull LocationResult result) {
            android.location.Location location = result.getLastLocation();
            ALog.i.tagMsg(this, "onLocationResult ", location);
            gotGPSLocation(location);
        }
        @Override
        public void onLocationAvailability(@NonNull LocationAvailability result) {
            if (!result.isLocationAvailable()) {
                if (context != null) {
                    LocationManager locMgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    ALog.i.tagMsg(this, "GPS NoPower not available, available=", locMgr.getAllProviders());
                }
            }
        }
    }
}