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

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.landenlabs.all_sensor.db.DbDeviceDaily;
import com.landenlabs.all_sensor.db.DbDeviceHourly;
import com.landenlabs.all_sensor.utils.ArrayListEx;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Minutes;
import org.joda.time.Period;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * Abstract base class for remote devices, see DeviceListManager for historical storage.
 */
public abstract class DeviceItem extends DataItem implements ExecState.Exec {

    @Nullable   // null until opened
    public DbDeviceHourly dbDeviceHourly;
    @Nullable   // null until opened
    public DbDeviceDaily dbDeviceDaily;

    /**
     * Setup internal state, prior to any other calls.
     */
    abstract boolean init(@NonNull Context context, IwxManager manager, ExecState state);

    // Accessed via Reflection
    // abstract  ArrayListEx<DeviceItem> devices(ExecState state);


    // ------
    // Get information from device without any direct dependencies.

    @NonNull
    public abstract Map<String, String> getInfoMap(@NonNull Context context, AbsCfg cfg);

    @NonNull
    public abstract ArrayListEx<CharSequence> getInfoList(@NonNull Context context, AbsCfg cfg);

    /*
    public static final String STR_TEMP = "StrTemp";
    public static final String STR_HUM = "StrHum";
    public static final String STR_DATE = "StrDate";
    public static final String STR_TIME = "StrTime";
    public static final String NUM_TEMP = "NumTemp";
    public static final String NUM_TEMP_MIN = "NumTempMin";
    public static final String NUM_TEMP_MAX = "NumTempMax";
    public static final String NUM_HUM = "NumHum";
    public static final String NUM_HUM_MIN = "NumHumMin";
    public static final String NUM_HUM_MAX = "NumHumMax";
    public static final String NUM_TIME = "NumTim";
    public static final String NUM_BATTERY = "NumBat";
    public static final String NUM_WIFI = "NumWiFi";
     */

    @NonNull
    public abstract DeviceSummary getSummary(@NonNull Context context, @NonNull AbsCfg cfg);


    // ---------------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static <T> T get(Map<String, Object> map, String key) {
        Object obj = map.get(key);
        return (T) map.get(key);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Map<String, Object> map, String key, T def) {
        T val = (T) map.get(key);
        return (val == null) ? def : val;
    }

    public static class DeviceTrend {
        public int deltaTem;
        public int deltaHum;
        public DateTime lastTime;
    }

    @Nullable
    public DeviceTrend getTrend(Interval interval, Period period, DeviceSummary summary) {
        DeviceTrend deviceTrend = null;
        ArrayListEx<DeviceGoveeHelper.Sample> samples = DeviceListManager.getHourlyList(name, interval);
        if (samples.size() > 0) {
            DeviceGoveeHelper.Sample sample = samples.last(DeviceGoveeHelper.Sample.EMPTY);
            deviceTrend = new DeviceTrend();
            int tem = summary.numTemp;
            int hum = summary.numHum;
            deviceTrend.lastTime = new DateTime(summary.numTime.longValue());

            DateTime sampelDt = new DateTime(sample.milli);
            int deltaTem = tem - sample.tem;
            int deltaHum = hum - sample.hum;
            float hours = (float) Minutes.minutesBetween(sampelDt, deviceTrend.lastTime).getMinutes() / 60.0f;
            deviceTrend.deltaTem = Math.round(deltaTem / hours);
            deviceTrend.deltaHum = Math.round(deltaHum / hours);
        }

        return deviceTrend;
    }

    static WeakReference<SharedPreferences> PREF_REF = null;
    @NonNull
    synchronized
    public static SharedPreferences getPref(@Nullable Context context) {
        if (PREF_REF == null || PREF_REF.get() == null) {
            if (context != null) {
                SharedPreferences pref =
                        context.getSharedPreferences("sensorWidget", Context.MODE_PRIVATE);
                PREF_REF = new WeakReference<>(pref);
            }
        }
        return PREF_REF.get();
    }
}
