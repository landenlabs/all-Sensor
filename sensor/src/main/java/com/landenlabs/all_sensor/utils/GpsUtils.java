/*
 * Copyright (c) 2022 Dennis Lang (LanDen Labs) landenlabs@gmail.com
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

package com.landenlabs.all_sensor.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.landenlabs.all_sensor.logger.ALog;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

class GpsUtils {

   public static class GPSLocation {
       public GPSLocation() {
       }

       public GPSLocation(android.location.Location location, Address address) {
       }
    }

   // =============================================================================================
   private class GPSLocationResolver implements Runnable {

      private final Context context;
      private final android.location.Location mAndroidLocation;
      private Thread thread;
      public GPSLocation gpsLocation;

      @NonNull
      @Override
      public String toString() {
         return "GPSLocationResolver";
      }

      private GPSLocationResolver(@NonNull Context context, android.location.Location androidLocation) {
         super();
         this.context = context;
         this.mAndroidLocation = androidLocation;
      }

      public void cancel() {
         if (thread != null) {
            thread.interrupt();
            thread = null;
         }
      }

      public void execute() {
         thread = new Thread((Runnable) this);
         thread.start();
      }

      @Override
      public void run() {
         gpsLocation = getLocation(context, mAndroidLocation);
      }
   }

    /**
     * Get address from  {@link android.location.Location}.
     * <ul>
     *     <li>Use Android reverse Geocoder to get address from Latitude,Longitude
     *     <li>Have address:</li>
     *     <ul>
     *      <li>Try to convert postal code to WSI location
     *      <li>Else try to convert city to WSI location
     *     </ul>
     *     <li>No address, use WSI reverse geocoder</li>
     * </ul>
     */
    private GPSLocation getLocation(@NonNull Context context, android.location.Location location) {
        GPSLocation result = null;
        if (null != location) {
            try {
                Address address = getGeocoderAddress(
                        context, location.getLatitude(), location.getLongitude());
                if (null != address) {
                    String addressPart1 = address.getPostalCode();
                    result = new GPSLocation(location, address);
                }
            } catch (Exception ex) {
                ALog.e.tagMsg(this, "GetLocation failed ", ex);
            }
        }
        ALog.d.tagMsg(this, "GetLocation resolved to ", result);
        return result;
    }

    public static final int MAX_LOCATION_SEARCH_FAILED_ATTEMPTS = 3;

    /**
     * Get address by Geocoder using play services.
     */
    @Nullable
    public static Address getGeocoderAddress(@NonNull Context context, double latitude, double longitude) {
        final String TAG = "getGeocoder";
        Geocoder gc = new Geocoder(context, Locale.US);
        if (Geocoder.isPresent()) {
            int failedAttempts = 0;
            for (; ; ) {
                try {
                    Thread.sleep(200L * failedAttempts);
                    List<Address> res = gc.getFromLocation(latitude, longitude, 1);
                    return ((res != null) && !res.isEmpty()) ? res.get(0) : null;
                } catch (IOException ioe) {
                    ALog.w.tagMsg(TAG, "getGeocoderAddress for ", latitude, ", ", longitude, " retry ", failedAttempts);
                    if (++failedAttempts > MAX_LOCATION_SEARCH_FAILED_ATTEMPTS
                        /* || !isNetworkAvailable(context) */) {
                        ALog.e.tagMsg(TAG, "getGeocoderAddress failed ", ioe);
                        return null;
                    }
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }

        ALog.e.tagMsg(TAG, "getGeocoder service not available ");
        return null;
    }
}
