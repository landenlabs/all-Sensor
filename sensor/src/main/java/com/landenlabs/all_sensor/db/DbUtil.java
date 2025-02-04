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

package com.landenlabs.all_sensor.db;

import static com.landenlabs.all_sensor.sensor.DeviceListManager.DEFAULT_DEVICE;

import android.content.Context;
import android.database.Cursor;

import com.landenlabs.all_sensor.sensor.DeviceGoveeHelper;
import com.landenlabs.all_sensor.sensor.DeviceListManager;
import com.landenlabs.all_sensor.sensor.WxViewModel;

import org.joda.time.DateTime;

public class DbUtil {

    public static  void rebuildDeviceDaily(Context context, String devName, WxViewModel progressVW ) {
        // String queryStr = "Select * FROM " + DbDeviceHourly.DATA_TABLE_NAME1 + " ORDER BY " + DbDeviceHourly.COL_MILLI + " ASC";
        String queryStr = "Select * FROM " + DbDeviceHourly.DATA_TABLE_NAME1 + " ORDER BY " + DbDeviceHourly.COL_MILLI + " DESC";
        Cursor dbHourly = DeviceListManager.getDbHourly(DEFAULT_DEVICE).query(queryStr, null);

        final int HR_COL_MILLI = dbHourly.getColumnIndex(DbDeviceHourly.COL_MILLI);
        final int HR_COL_TEMPC100 = dbHourly.getColumnIndex(DbDeviceHourly.COL_TEMPC100);
        final int HR_COL_HUM100 = dbHourly.getColumnIndex(DbDeviceHourly.COL_HUM100);
        DateTime prevTime = new DateTime(0);


        int dailyDay = -1;
        DeviceGoveeHelper.Sample maxTem = null;
        DeviceGoveeHelper.Sample minTem = null;
        DeviceGoveeHelper.Sample maxHum = null;
        DeviceGoveeHelper.Sample minHum = null;

        DbDeviceDaily dbDeviceDaily = DeviceListManager.getDbDaily(devName);
        try {
            // dbDeviceDaily = new DbDeviceDaily(dbDeviceDaily.getFile()+"v2");
            dbDeviceDaily.create(true, false);
            dbDeviceDaily.openWrite(false);
            int rowCnt = dbHourly.getCount();
            int rowPos = 0;

            while (!Thread.currentThread().isInterrupted() && dbHourly.moveToNext()) {
                rowPos++;
                long milli = dbHourly.getLong(HR_COL_MILLI);
                int tempC100 = dbHourly.getInt(HR_COL_TEMPC100);
                int humP100 = dbHourly.getInt(HR_COL_HUM100);
                DeviceGoveeHelper.Sample sample = new DeviceGoveeHelper.Sample(tempC100, humP100, milli);
                DateTime sampleTime = new DateTime(milli);

                if (dailyDay == -1) {
                    // Init once using first sample
                    maxTem = minTem = maxHum = minHum = sample;
                    dailyDay = sampleTime.getDayOfYear();
                }

                if (sampleTime.getDayOfYear() != dailyDay) {
                    // dayMaxTem.add(maxTem);
                    // dayMinTem.add(minTem);
                    dbDeviceDaily.add(minTem, maxTem, minHum, maxHum);
                    maxTem = minTem = maxHum = minHum = sample;
                    dailyDay = sampleTime.getDayOfYear();

                    if (progressVW != null) {
                        progressVW.setProgress(0, 0,  rowPos*100 / rowCnt);
                    }
                }

                if (tempC100 > maxTem.tem) {
                    maxTem = sample;
                }
                if (tempC100 < minTem.tem) {
                    minTem = sample;
                }
                if (humP100 > maxHum.hum) {
                    maxHum = sample;
                }
                if (humP100 < minHum.hum) {
                    minHum = sample;
                }
            }


        } finally {
            dbDeviceDaily.add(minTem, maxTem, minHum, maxHum);

            dbHourly.close();
            dbDeviceDaily.close();
        }
    }
}
