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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;

import com.landenlabs.all_sensor.db.DbSensorPressureHourly;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.utils.SpanUtil;
import com.landenlabs.all_sensor.widget.WidView;

import java.util.concurrent.TimeUnit;

/**
 * Gather device barometric pressure.
 */
public class SensorPressure extends SensorItem
        implements SensorEventListener {

    public static final String NAME = "Pressure";
    public static final SensorPressure EMPTY = new SensorPressure();
    public static final int I_SCALE = 100;
    public static final float MIN_MB_DELTA = 0.5f;

    private float lastValue;
    private static final String DB_NAME = "dbPressure.db";

    // ---------------------------------------------------------------------------------------------
    public SensorPressure( ) {
        super(null);
        this.name = NAME;
        this.dbFile = null;
        this.pressureSensor = null;
    }

    // Required - used by xxxxManager to initalize
    public SensorPressure( @NonNull IwxManager wxManager) {
        super(wxManager);
        this.name = NAME;
        this.dbFile = wxManager.getContext().getDatabasePath(DB_NAME);
        if (sensorManager != null) {
            pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        } else {
            pressureSensor = null;
        }
    }

    @Override
    protected void openDatabase(String sensorName) {
        if (!isDbOpen()) {
            try {
                dbSensorHourly = new DbSensorPressureHourly(dbFile.getAbsolutePath());
                state.failIf(dbSensorHourly.openWrite(true));
            } catch (Exception ex) {
                ALog.e.tagMsg(this, "SQL database ", ex);
                state.fail(ex);
            }
        }
    }

    @Override
    public boolean hasSensor(@NonNull Context context) {
        SensorManager sensorManager = getServiceSafe(context, Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        return (sensor != null);
    }

    @Override
    @NonNull
    public SensorSummary getSummary(@NonNull Context context, @NonNull AbsCfg cfg) {
        SensorSummary summary = super.getSummary(context, cfg);

        summary.strTime = cfg.timeFmt(lastMilli);
        summary.strDate = cfg.dateFmt(lastMilli);

        String strTemp = cfg.toPress(lastValue);
        summary.strValue = SpanUtil.SString(strTemp, SpanUtil.SS_SUPER + SpanUtil.SS_SMALLER, strTemp.length()-2, strTemp.length());
        summary.numValue = lastValue;
        summary.numTime = lastMilli;

        return summary;
    }

    @Override
    public int iValue() {
        return Math.round(lastValue * I_SCALE);
    }

    @Override
    public float fValue(int iValue) {
        return (float)iValue / I_SCALE;
    }

    @Override
    public CharSequence sValue(int iValue) {
        String strValue = String.format("%,.1fmb", fValue(iValue));
        return SpanUtil.SString(strValue, SpanUtil.SS_SUPER + SpanUtil.SS_SMALLER, strValue.length() - 2, strValue.length());
    }

    public void start(IwxManager manager, ExecState startStatus) {
        if (!isDbOpen()) {
            openDatabase(name);
        }
        sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        startStatus.done();
    }

    public void stop(IwxManager manager) {
        sensorManager.unregisterListener(this);
        closeDatabase();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values;
        if (values != null && values.length > 0) {
            long nowMilli = System.currentTimeMillis();

            // Store in DB value if 20+ ninutes or value has changed after 1+ minutes.
            if ((nowMilli - lastMilli) > TimeUnit.MINUTES.toMillis(1)) {
                if ((nowMilli - lastMilli) > TimeUnit.MINUTES.toMillis(20)
                        || Math.abs(lastValue - values[0]) > MIN_MB_DELTA) {
                    lastMilli = nowMilli;
                    lastValue = values[0];
                    viewModel.setStatus(Math.round(values[0]));
                    // txt.setText(String.format("%.3f mbar", values[0]));
                    SensorAndroid.SensorSample sample = new SensorAndroid.Pressure(nowMilli, values[0]);
                    add(sample);
                    WidView.logIt(wxManager.getContext(), this, AppCfg.getInstance(wxManager.getContext()));
                }
                state.complete.complete(ExecState.DONE);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}