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

package com.landenlabs.all_sensor.ui;

import static com.landenlabs.all_sensor.sensor.DeviceListManager.DEFAULT_DEVICE;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.Units;
import com.landenlabs.all_sensor.db.CursorHourly;
import com.landenlabs.all_sensor.db.DbDeviceDaily;
import com.landenlabs.all_sensor.db.DbDeviceDailyAdapter;
import com.landenlabs.all_sensor.db.DbDeviceHourly;
import com.landenlabs.all_sensor.db.DbDeviceHourlyAdapter;
import com.landenlabs.all_sensor.db.DbSensorHourly;
import com.landenlabs.all_sensor.db.DbSensorHourlyAdapter;
import com.landenlabs.all_sensor.db.IAdapterHighlight;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.sensor.AbsCfg;
import com.landenlabs.all_sensor.sensor.AppCfg;
import com.landenlabs.all_sensor.sensor.DeviceListManager;
import com.landenlabs.all_sensor.sensor.SensorListManager;
import com.landenlabs.all_sensor.sensor.SensorPressure;
import com.landenlabs.all_sensor.sensor.SensorWiFi;
import com.landenlabs.all_sensor.sensor.WxManager;
import com.landenlabs.all_sensor.ui.view.BaseFragment;
import com.landenlabs.all_sensor.utils.UiUtils;


/**
 * Tabular presentation of Hourly Samples and Records.
 */
@SuppressWarnings("FieldCanBeLocal")
public class TableFragment extends BaseFragment
        implements View.OnClickListener
        , RecyclerView.OnItemTouchListener {

    private WxManager wxManager;
    private View hourlyBtn, dailyBtn, pressBtn, wifiBtn;
    private RecyclerView recyclerView;
    private ViewGroup tableHdr;
    private LayoutInflater inflater;
    private View prevTouch = null;

    private IAdapterHighlight adapterHighlight;

    private enum DbTableType {DeviceHourly, DeviceDaily, SensorHourly }

    private DbTableType dbType = DbTableType.DeviceHourly;
    private AbsCfg cfg;

    // ---------------------------------------------------------------------------------------------

    @SuppressLint("ClickableViewAccessibility")
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {

        wxManager = WxManager.getInstance(requireContext());
        cfg = AppCfg.getInstance(requireContext());

        this.inflater = inflater;
        super.onCreateView((ViewGroup) inflater.inflate(R.layout.fragment_table, container, false));

        hourlyBtn = root.findViewById(R.id.table_hourly_btn);
        hourlyBtn.setOnClickListener(this);
        dailyBtn = root.findViewById(R.id.table_daily_btn);
        dailyBtn.setOnClickListener(this);

        pressBtn = root.findViewById(R.id.table_pressure_btn);
        if (wxManager.hasSensor(SensorPressure.NAME) ) {
            pressBtn.setOnClickListener(this);
        } else {
            pressBtn.setVisibility(View.GONE);
        }

        // TODO - add wifi, battery, etc
        wifiBtn = root.findViewById(R.id.table_wifi_btn);
        if (wxManager.hasSensor(SensorWiFi.NAME) ) {
            wifiBtn.setOnClickListener(this);
        } else {
            wifiBtn.setVisibility(View.GONE);
        }

        tableHdr = root.findViewById(R.id.table_header);

        if (DeviceListManager.getDevices().isEmpty()) {
            hourlyBtn.setVisibility(View.INVISIBLE);
            dailyBtn.setVisibility(View.INVISIBLE);
        }
        if (SensorListManager.getSensors().isEmpty()) {
            pressBtn.setVisibility(View.INVISIBLE);
        }


        // wxManager.getViewModel().getStatus().observe(getViewLifecycleOwner(), this::updateUi);
        // wxManager.getViewModel().getProgress().observe(getViewLifecycleOwner(), this::updateUi);

        recyclerView = root.findViewById(R.id.tableRV);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        ALog.d.tagMsg(this, "before setup tables");
        wxManager.wxState.complete.thenAccept(xx -> {
            ALog.d.tagMsg(this, "setup tables");
            recyclerView.post(() -> {
                setTableType(dbType, cfg, DEFAULT_DEVICE, hourlyBtn);  // Sets adapter
                recyclerView.getAdapter().notifyDataSetChanged();
                recyclerView.addOnItemTouchListener(TableFragment.this);
            });
        }).exceptionally(ex -> null);

        return root;
    }

    @Override
    public String getName() {
        return "Table";
    }

    @Override
    public String results() {
        return "";
    }

    @Override
    public boolean validSwipeRegion(MotionEvent ev) {
        return true;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.table_hourly_btn) {
            setTableType(DbTableType.DeviceHourly, cfg, DEFAULT_DEVICE, view);  // TODO - support multiple device names
        } else if (id == R.id.table_daily_btn) {
            setTableType(DbTableType.DeviceDaily, cfg, DEFAULT_DEVICE, view);   // TODO - support multiple device names
        } else if (id == R.id.table_pressure_btn) {
            setTableType(DbTableType.SensorHourly, cfg, SensorPressure.NAME, view);
        } else if (id == R.id.table_wifi_btn) {
            setTableType(DbTableType.SensorHourly, cfg, SensorWiFi.NAME, view);
        }
    }

    @UiThread
    private void setTableType(DbTableType dbType, AbsCfg cfg, String name, View btn) {
        this.dbType = dbType;
        UiUtils.setSelected(false, hourlyBtn, dailyBtn, pressBtn, wifiBtn);
        btn.setSelected(true);
        try {
            switch (dbType) {
                case DeviceDaily:
                    adapterHighlight = new DbDeviceDailyAdapter(requireContext(), getDeviceDaily(name), cfg);
                    recyclerView.setAdapter((DbDeviceDailyAdapter) adapterHighlight);
                    tableHdr.removeAllViews();
                    fixUnits(cfg, inflater.inflate(R.layout.device_daily_row_header, tableHdr, true));
                    break;
                default:
                case DeviceHourly:
                    adapterHighlight = new DbDeviceHourlyAdapter(requireContext(), getDeviceHourly(name, true), cfg);
                    recyclerView.setAdapter((DbDeviceHourlyAdapter) adapterHighlight);
                    tableHdr.removeAllViews();
                    fixUnits(cfg, inflater.inflate(R.layout.device_hourly_row_header, tableHdr, true));
                    break;
                case SensorHourly:
                    adapterHighlight = new DbSensorHourlyAdapter(requireContext(), getSensorHourly(name), cfg);
                    recyclerView.setAdapter((DbSensorHourlyAdapter) adapterHighlight);
                    tableHdr.removeAllViews();
                    inflater.inflate(R.layout.sensor_hourly_row_header, tableHdr, true);
                    break;
            }
        } catch (Exception ex) {
            ALog.e.tagMsg(this, ex);
        }
    }

    private static void  fixUnits(AbsCfg cfg, View view) {
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup)view;
            for (int idx = 0; idx < vg.getChildCount(); idx++) {
                fixUnits(cfg, vg.getChildAt(idx));
            }
        } else if (view instanceof TextView) {
            TextView tv = (TextView)view;
            String str = tv.getText().toString();
            if (cfg.tunit == Units.Temperature.Celsius && str.contains("°F")) {
                tv.setText(str.replace("°F", "°C"));
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    Cursor getDeviceHourly(String devName, boolean fillMissing) {
        // String queryStr = "Select * FROM " + DbDeviceHourly.DATA_TABLE_NAME1 + " ORDER BY " + DbDeviceHourly.COL_MILLI + " ASC";
        String queryStr = "Select * FROM " + DbDeviceHourly.DATA_TABLE_NAME1 + " ORDER BY " + DbDeviceHourly.COL_MILLI + " DESC";
        Cursor cursor = DeviceListManager.getDbHourly(devName).query(queryStr, null);
        return fillMissing ? new CursorHourly(cursor) : cursor;
    }

    @SuppressWarnings("SameParameterValue")
    Cursor getDeviceDaily(String devName) {
        // String queryStr = "Select * FROM " + DbDeviceDaily.DATA_TABLE_NAME1 + " ORDER BY " + DbDeviceDaily.COL00_DAY + " ASC";
        String queryStr = "Select * FROM " + DbDeviceDaily.DATA_TABLE_NAME1 + " ORDER BY " + DbDeviceDaily.COL00_DAY + " DESC";
        return DeviceListManager.getDbDaily(devName).query(queryStr, null);
    }

    @Nullable
    Cursor getSensorHourly(String sensorName) {
        DbSensorHourly dbSensorHourly = SensorListManager.getDbHourly(sensorName);
        if (dbSensorHourly != null) {
            // String queryStr = "Select * FROM " + DbDeviceDaily.DATA_TABLE_NAME1 + " ORDER BY " + DbDeviceDaily.COL00_DAY + " ASC";
            String queryStr = "Select * FROM " + dbSensorHourly.DATA_TABLE_NAME + " ORDER BY " + DbSensorHourly.COL_MILLI + " DESC";
            Cursor cursor = dbSensorHourly.query(queryStr, null);
            long cnt = cursor.getCount();
            return cursor;
        }
        return null;
    }

    /*
    Cursor getMinMaxAvgTempC100(String devName, Interval interval) {
        String queryStr = "Select min(?),max(?),avg(?) FROM " + DbDeviceHourly.DATA_TABLE_NAME1
                + " ORDER BY " + DbDeviceHourly.COL_MILLI + " ASC"
                + " WHERE " + DbDeviceHourly.COL_MILLI + " BETWEEN " + interval.getStartMillis() + " AND " + interval.getEndMillis();

        return DeviceListManager.getDbHourly(devName).query(queryStr,
                new String[]{
                        DbDeviceHourly.COL_TEMPC100,
                        DbDeviceHourly.COL_TEMPC100,
                        DbDeviceHourly.COL_TEMPC100});
    }
     */

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rview, @NonNull MotionEvent e) {
        View childView = rview.findChildViewUnder(e.getX(), e.getY());
        ALog.d.tagMsg(this, childView);
        adapterHighlight.setHighlight(false, prevTouch);
        prevTouch = childView;
        adapterHighlight.setHighlight(true, childView);
        return false;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rview, @NonNull MotionEvent e) {
        View childView = rview.findChildViewUnder(e.getX(), e.getY());
        ALog.d.tagMsg(this, childView);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }
}
