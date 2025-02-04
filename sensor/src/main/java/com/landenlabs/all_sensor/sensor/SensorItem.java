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

import static android.content.Context.SENSOR_SERVICE;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.landenlabs.all_sensor.db.DbSensorHourly;
import com.landenlabs.all_sensor.utils.ArrayListEx;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Minutes;
import org.joda.time.Period;

import java.io.File;

public abstract class SensorItem extends DataItem implements  ExecState.Exec {

    public      DbSensorHourly dbSensorHourly;
    public      int id;

    protected final ExecState state;
    protected final IwxManager wxManager;
    protected final SensorManager sensorManager;
    protected   Sensor pressureSensor;
    protected   Sensor wifiSensor;
    protected final WxViewModel viewModel;
    protected   File dbFile;
    protected   long lastMilli;

    // ---------------------------------------------------------------------------------------------
    protected abstract void openDatabase(String sensorName);
    public abstract boolean hasSensor(@NonNull Context context);
    public abstract int iValue();
    public abstract float fValue(int iValue);
    public abstract CharSequence sValue(int iValue);

    // ---------------------------------------------------------------------------------------------

    @Override
    public boolean isValid() {
        return lastMilli != 0;
    }

    @Override
    public Exception getException() {
        return null;
    }
    @Override
    public void setException(Exception ex) {
    }

    @Override
    public long lastMilli() {
        return lastMilli;
    }

    @Override
    public ExecState state() {
        return state;
    }

    public SensorItem( @Nullable IwxManager wxManager) {
        this.state = new ExecState("SI-state");
        this.lastMilli = 0;
        this.wxManager = wxManager;
        if (wxManager != null) {
            this.viewModel = wxManager.viewModel();
            Context context = wxManager.getContext();
            this.sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        } else {
            this.viewModel = null;
            this.sensorManager = null;
        }
    }



    protected void closeDatabase() {
        if (dbSensorHourly != null) {
            dbSensorHourly.close();
            dbSensorHourly = null;
        }
    }

    public boolean isDbOpen() {
        return (dbSensorHourly != null && dbSensorHourly.isOpen());
    }

    synchronized
    public void add(SensorAndroid.SensorSample sample) {
        if (!isDbOpen()) {
            openDatabase(name);
        }
        dbSensorHourly.add(sample);
    }

    @NonNull
    public SensorSummary getSummary(@NonNull Context context, @NonNull AbsCfg cfg) {
        SensorSummary summary = new SensorSummary();
        return summary;
    }

    public static class SenorTrend {
        public float deltaVal;
        @Nullable
        public DateTime lastTime;
    }

    @Nullable
    synchronized
    public SensorItem.SenorTrend getTrend(@NonNull Interval interval, @NonNull Period period, @NonNull SensorSummary summary) {
        SensorItem.SenorTrend senorTrend = null;
        if (!isDbOpen()) {
            openDatabase(name);
        }

        ArrayListEx<SensorAndroid.SensorSample> samples = SensorListManager.getHourlyList(name, interval);
        if (samples != null && samples.size() > 0) {
            senorTrend = new SensorItem.SenorTrend();
            senorTrend.deltaVal = 0;
            senorTrend.lastTime = new DateTime(summary.numTime.longValue());

            SensorAndroid.SensorSample sample =
                    SensorAndroid.SensorSample.getNear(samples,
                    new DateTime(senorTrend.lastTime).minusHours(2).getMillis(),
                    SensorAndroid.Pressure.EMPTY);

            float value = summary.numValue;
            DateTime sampleDt = new DateTime(sample.milli);
            float deltaVal = value - sample.fvalue;
            float hours = (float) Minutes.minutesBetween(sampleDt, senorTrend.lastTime).getMinutes() / 60.0f;
            senorTrend.deltaVal = (hours != 0f) ? Math.round(deltaVal / hours) : 0;
        }

        return senorTrend;
    }
}
