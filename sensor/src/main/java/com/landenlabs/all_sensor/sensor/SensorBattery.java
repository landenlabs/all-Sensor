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

import static com.landenlabs.all_sensor.utils.FragUtils.getServiceSafe;

import android.content.Context;
import android.os.BatteryManager;
import android.text.SpannableString;

import androidx.annotation.NonNull;

import com.landenlabs.all_sensor.db.DbSensorBatteryHourly;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.utils.SpanUtil;

/**
 * Gather battery data.
 */
public class SensorBattery extends SensorItem  {

    public static final String NAME = "Battery";
    public static final SensorBattery EMPTY = new SensorBattery();

    private float lastValue;
    private static final String DB_NAME = "dbBattery.db";

    // ---------------------------------------------------------------------------------------------
    public SensorBattery( ) {
        super(null);
        this.name = NAME;
        this.dbFile = null;
    }

    // Required - used by xxxxManager to initalize
    public SensorBattery(@NonNull IwxManager wxManager) {
        super(wxManager);
        this.name = NAME;
        this.dbFile = wxManager.getContext().getDatabasePath(DB_NAME);
    }

    @Override
    protected void openDatabase(String sensorName) {
        if (!isDbOpen()) {
            try {
                dbSensorHourly = new DbSensorBatteryHourly(dbFile.getAbsolutePath());
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

        String strTemp = cfg.toWifi(lastValue);
        SpannableString ss1 = SpanUtil.SString(strTemp, SpanUtil.SS_SUPER + SpanUtil.SS_SMALLER, strTemp.length()-1, strTemp.length());
        summary.strValue = ss1;
        summary.numValue = lastValue;
        summary.numTime = lastMilli;

        return summary;
    }
    @Override
    public int iValue() {
        return Math.round(lastValue);
    }

    @Override
    public float fValue(int iValue) {
        return (float)iValue;
    }

    @Override
    public  CharSequence sValue(int iValue) {
        return String.format("%d%%", iValue);
    }

    public void start(IwxManager manager, ExecState startStatus) {
        // TODO - use WorkManager to schedule updates defined in Setting/Config
        BatteryManager bm = getServiceSafe(wxManager.getContext(), Context.BATTERY_SERVICE);
        lastValue =  bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        lastMilli = System.currentTimeMillis();

        SensorAndroid.SensorSample sample = new SensorAndroid.SensorSample(lastMilli, lastValue);
        add(sample);
    }

    public void stop(IwxManager manager) {
        // TODO - use WorkManager to schedule updates defined in Setting/Config
    }

}