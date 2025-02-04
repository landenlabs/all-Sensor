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
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.text.SpannableString;

import androidx.annotation.NonNull;

import com.landenlabs.all_sensor.db.DbSensorWiFiHourly;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.utils.NetInfo;
import com.landenlabs.all_sensor.utils.SpanUtil;

/**
 * Gather barometric pressure sensor data
 */
public class SensorWiFi extends SensorItem
        // implements SensorEventListener
    {

    public static final String NAME = "WiFi";
    public static final SensorWiFi EMPTY = new SensorWiFi();

    private float lastValue;
    private static final String DB_NAME = "dbWiFi.db";

    // ---------------------------------------------------------------------------------------------
    public SensorWiFi( ) {
        super(null);
        this.name = NAME;
        this.dbFile = null;
    }

    // Required - used by xxxxManager to initalize
    public SensorWiFi(@NonNull IwxManager wxManager) {
        super(wxManager);
        this.name = NAME;
        this.dbFile = wxManager.getContext().getDatabasePath(DB_NAME);
    }

    @Override
    protected void openDatabase(String sensorName) {
        if (!isDbOpen()) {
            try {
                dbSensorHourly = new DbSensorWiFiHourly(dbFile.getAbsolutePath());
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
        openDatabase(name);
        // TODO - use WorkManager to schedule updates defined in Setting/Config
        // sensorManager.registerListener(this, wifiSensor, SensorManager.SENSOR_DELAY_NORMAL);
        // final WifiManager wifiMgr = getServiceSafe(wxManager.getContext(), Context.WIFI_SERVICE);
        // wifiMgr.registerScanResultsCallback();
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        //        .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();
        final ConnectivityManager cm = getServiceSafe(wxManager.getContext(), Context.CONNECTIVITY_SERVICE);

        try {
            // WARNING - must unregisterNetworkCallback else exception after 100 calls.
            stop(manager);
            cm.requestNetwork(networkRequest, networkCallback);
        } catch (Exception ex) {
            ALog.e.tagMsg(this, "Failed to register to monitor network, ", ex);
        }

        startStatus.done();
        grabWifiStrength();
    }

    public void stop(IwxManager manager) {
        // TODO - use WorkManager to schedule updates defined in Setting/Config
        // sensorManager.unregisterListener(this);
        final ConnectivityManager cm = getServiceSafe(wxManager.getContext(), Context.CONNECTIVITY_SERVICE);
        try {
            cm.unregisterNetworkCallback(networkCallback);
        } catch (Exception ex) {
            // ALog.e.tagMsg(this, "Failed to unregister to monitor network, ", ex);
        }
    }

    public void grabWifiStrength() {
        lastMilli = System.currentTimeMillis();
        // Map<String, Object> netMap = NetInfo.loadNetInfo(wxManager.getContext());
        // Map<String, Object> wifiMap = NetInfo.loadWifi(wxManager.getContext(), null);

        WifiManager wifiManager = getServiceSafe(wxManager.getContext(), Context.WIFI_SERVICE);
        // WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int rssi = wifiManager.getConnectionInfo().getRssi();
        lastValue = NetInfo.getWifiLevelPercent(wifiManager, rssi);
        SensorAndroid.SensorSample sample = new SensorAndroid.SensorSample(lastMilli, lastValue);
        add(sample);

        // ALog.d.tagMsg(this, "wifi signal strength", lastValue);
    }

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            grabWifiStrength();
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            grabWifiStrength();
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            boolean hasCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            boolean hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            if (hasWifi) {
                grabWifiStrength();
            } else {
                lastValue = 0;
                lastMilli = System.currentTimeMillis();
            }
        }
    };

    /*
    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values;
        if (values != null && values.length > 0) {
            long nowMilli = System.currentTimeMillis();

            // Store in DB value if 20+ ninutes or value has changed after 1+ minutes.
            if ((nowMilli - lastMilli) > TimeUnit.MINUTES.toMillis(1)) {
                if ((nowMilli - lastMilli) > TimeUnit.MINUTES.toMillis(20)
                        || lastValue != values[0]) {
                    lastMilli = nowMilli;
                    lastValue = values[0];
                    viewModel.setStatus(Math.round(values[0]));
        // TODO - fix
        //            SensorAndroid.Pressure pressure = new SensorAndroid.Pressure(nowMilli, values[0]);
        //            dbSensorHourly.add(pressure);
                    WidView.logIt(wxManager.getContext(), this, AppCfg.getInstance());
                }
                state.complete.complete(ExecState.DONE);
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

     */
}