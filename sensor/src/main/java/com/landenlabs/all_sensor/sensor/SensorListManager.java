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

import static com.landenlabs.all_sensor.utils.StrUtils.isEmpty;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.landenlabs.all_sensor.db.DbSensorHourly;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.utils.ArrayListEx;

import org.joda.time.Interval;

import java.lang.reflect.Constructor;
import java.util.Map;

public class SensorListManager {
    private static final String TAG = SensorListManager.class.getSimpleName();

    public static final Class<? extends SensorItem>[] SENSOR_CLASSES
            = new Class[]{
                    SensorPressure.class,
                    SensorWiFi.class,
                    SensorBattery.class,
                    SensorGPS.class
    };

    private static Map<String, SensorItem> SENSOR_MAP;
    private static final ArrayListEx<SensorItem> SENSOR_ARRAY = new ArrayListEx<>();

    private static final Object LOCKER = new Object();

    public static final String DEFAULT_SENSOR = "";
    public  static final SensorItem EMPTY_ITEM = new SensorPressure();

    // ---------------------------------------------------------------------------------------------

    @Nullable
    public static DbSensorHourly getDbHourly(String sensorName) {
        SensorItem sensorItem = get(sensorName);
        return (sensorItem != null) ? sensorItem.dbSensorHourly : null;
    }

    @Nullable
    public static SensorItem get(String sensorName) {
        synchronized (SENSOR_ARRAY) {
            if (SENSOR_MAP != null && SENSOR_MAP.containsKey(sensorName)) {
                return SENSOR_MAP.get(sensorName);
            } else if (isEmpty(sensorName) && SENSOR_MAP != null) {
                return SENSOR_MAP.values().iterator().next();
            } else {
                // throw new DataFormatException("no sensor data for " + devName);
                ALog.e.tagMsg(TAG, "No sensors available for=", sensorName);
                // ALog.w.tagMsgStack(TAG,  10);
                return null;
            }
        }
    }

    @Nullable
    public static ArrayListEx<SensorAndroid.SensorSample> getHourlyList(String devName, Interval interval) {
        DbSensorHourly dbSensorHourly = getDbHourly(devName);
        return  (dbSensorHourly != null) ? dbSensorHourly.getRange(interval) : null;
    }

    public static ExecState init(@NonNull IwxManager manager, @NonNull Map<String, SensorItem> sensors) {
        synchronized (LOCKER) {
            ALog.d.tagMsg(TAG, "init start sensors=", SENSOR_ARRAY.size());

            for (Class<? extends SensorItem> sensorClass :  SENSOR_CLASSES) {
                try {
                    Constructor<? extends SensorItem> ctor = sensorClass.getConstructor(IwxManager.class);
                    SensorItem item = ctor.newInstance(manager);
                    if (item.hasSensor(manager.getContext())) {
                        item.id = sensors.size();
                        sensors.put(item.name, item);
                    }
                } catch (Exception ex) {
                    ALog.e.tagMsg(TAG, "init sensor ERROR ", ex);
                }
            }

            // sensors.put(SensorPressure.NAME, new SensorPressure(manager));

            SENSOR_MAP = sensors;
            SENSOR_ARRAY.clear();
            SENSOR_ARRAY.addAll(SENSOR_MAP.values());
            ALog.d.tagMsg(TAG, "init end sensors=", SENSOR_ARRAY.size());
            ExecState state = new ExecState("SL-state");
            state.done();
            return state;
        }
    }

    public static ArrayListEx<SensorItem> getSensors() {
        synchronized (SENSOR_ARRAY) {
            return new ArrayListEx<>(SENSOR_ARRAY);
        }
    }
}
