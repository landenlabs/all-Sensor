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

package com.landenlabs.all_sensor.aux;

import static com.landenlabs.all_sensor.utils.FragUtils.getServiceSafe;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import androidx.annotation.NonNull;

import com.landenlabs.all_sensor.Units;
import com.landenlabs.all_sensor.utils.ArrayListEx;

import java.util.Locale;

/**
 * Device system information.
 */
public class SysInfo {

    public static final String BATTERY = "Battery";


    public static float getBatteryTempF(@NonNull Context context, Units.Temperature unit)
    {
        Intent intent2 = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        float  batteryTempC   =  intent2.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)  / 10f;
        if (unit == Units.Temperature.Fahrenheit) {
            return batteryTempC * 9 / 5f + 32;
        } else {
            return batteryTempC;
        }

        /*
        Process process;
        try {
            process = Runtime.getRuntime().exec("cat sys/class/thermal/thermal_zone0/temp");
            process.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            if (line != null) {
                float temp = Float.parseFloat(line);
                return temp / 1000.0f;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return -1.0f;
        */

        /*
        SensorManager sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        Sensor sensor  = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        if (sensor != null) {
            sensorManager.registerListener(
                    new SensorEventListener() {
                        @Override
                        public void onSensorChanged(SensorEvent event) {
                            lastCpuTemp = event.values[0];
                        }

                        @Override
                        public void onAccuracyChanged(Sensor sensor, int accuracy) {
                        }
                    },
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        return lastCpuTemp;
         */
    }

    public static String[] getBatteryInfo(Context context) {
        ArrayListEx<String> info = new ArrayListEx<>();
        // Map<String, String> listStr = new LinkedHashMap<>();

        BatteryManager mBatteryManager = getServiceSafe(context, Context.BATTERY_SERVICE);
        // int avgCurrent = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
        // int currentNow = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        // Integer capPer = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        // Battery remaining energy in nanowatt-hours, as a long integer.
        // Long nanowattHours = mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);


        // listStr.put("Current (avg)", String.format("%.3f mA", avgCurrent / 1e3));
        // listStr.put("Current (now)", String.format("%.3f mA", currentNow/1e3));
        // listStr.put("Percent", String.format("%d%%", capPer.intValue()));
        // listStr.put("Remain", String.format("%.4f Hours", nanowattHours/1e9));

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        if (batteryStatus != null) {
            // Are we charging / charged?
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
            boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
            String charging = isCharging ? "Yes" : "No";
            charging = (isCharging && usbCharge) ? "USB" : charging;
            charging = (isCharging && acCharge) ? "AC" : charging;
            // listStr.put("Charging", charging);
            if (isCharging)
                info.add("Charging=" + charging);

            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level / (float) scale;

            // int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            // listStr.put("Percent", String.format("%.1f%%", batteryPct*100));
            // listStr.put("Voltage", String.format("%d mV", voltage));
            info.add(String.format(Locale.getDefault(), "%s %.0f%%", BATTERY, batteryPct * 100));
        }

        return info.toArray(new String[0]);
    }
}
