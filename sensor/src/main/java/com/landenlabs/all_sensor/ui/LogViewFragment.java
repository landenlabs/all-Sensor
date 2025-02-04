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

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.logger.ALogCusor;
import com.landenlabs.all_sensor.logger.ALogFileWriter;
import com.landenlabs.all_sensor.sensor.AbsCfg;
import com.landenlabs.all_sensor.sensor.AppCfg;
import com.landenlabs.all_sensor.sensor.DeviceListManager;
import com.landenlabs.all_sensor.sensor.SensorListManager;
import com.landenlabs.all_sensor.sensor.WxManager;
import com.landenlabs.all_sensor.ui.view.BaseFragment;
import com.landenlabs.all_sensor.utils.UiUtils;


/**
 * Tabular presentation of Log Records.
 */
@SuppressWarnings("FieldCanBeLocal")
public class LogViewFragment extends BaseFragment
        implements View.OnClickListener
        , RecyclerView.OnItemTouchListener {

    private WxManager wxManager;
    private View showBatteryBtn, showWifiBtn, showSensorBtn;
    private RecyclerView recyclerView;
    private ViewGroup tableHdr;
    private LayoutInflater inflater;
    private View prevTouch = null;

    private LogViewAdapter adapterHighlight;

    private final static int SHOW_BATTERY = 1;
    private final static int SHOW_WIFI = 2;
    private final static int SHOW_SENSOR = 4;

    private int showFields = SHOW_BATTERY + SHOW_WIFI + SHOW_SENSOR;
    private AbsCfg cfg;

    // ---------------------------------------------------------------------------------------------

    @SuppressLint("ClickableViewAccessibility")
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {

        cfg = AppCfg.getInstance(requireContext());
        this.inflater = inflater;
        super.onCreateView((ViewGroup) inflater.inflate(R.layout.fragment_logvwr, container, false));
        showBatteryBtn = root.findViewById(R.id.logvwr_battery_btn);
        showBatteryBtn.setOnClickListener(this);
        showWifiBtn = root.findViewById(R.id.logvwr_wifi_btn);
        showWifiBtn.setOnClickListener(this);
        showSensorBtn = root.findViewById(R.id.logvwr_sensor_btn);
        showSensorBtn.setOnClickListener(this);
        tableHdr = root.findViewById(R.id.table_header);

        if (DeviceListManager.getDevices().isEmpty()) {
            showBatteryBtn.setVisibility(View.INVISIBLE);
            showWifiBtn.setVisibility(View.INVISIBLE);
        }
        if (SensorListManager.getSensors().isEmpty()) {
            showSensorBtn.setVisibility(View.INVISIBLE);
        }

        wxManager = WxManager.getInstance(requireContext());
        recyclerView = root.findViewById(R.id.tableRV);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        adapterHighlight = new LogViewAdapter(requireContext(), getLogFile(), cfg);
        recyclerView.setAdapter(adapterHighlight);
        tableHdr.removeAllViews();

        wxManager.wxState.complete.thenAccept(xx -> {
            recyclerView.post(() -> {
                setTableType(showFields, cfg);
                adapterHighlight.notifyDataSetChanged();
                recyclerView.addOnItemTouchListener(LogViewFragment.this);
            });
        }).exceptionally(ex -> null);
    }

    @Override
    public String getName() {
        return "LogVwr";
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
        if (id == R.id.logvwr_battery_btn) {
            UiUtils.togleSelected(showBatteryBtn);
            setTableType(showFields ^ SHOW_BATTERY, cfg);
        } else if (id == R.id.logvwr_wifi_btn) {
            UiUtils.togleSelected(showWifiBtn);
            setTableType(showFields ^ SHOW_WIFI, cfg);
        } else if (id == R.id.logvwr_sensor_btn) {
            UiUtils.togleSelected(showSensorBtn);
            setTableType(showFields ^ SHOW_SENSOR, cfg);
        }
    }

    @UiThread
    private void setTableType(int showFields, AbsCfg cfg) {
        inflater.inflate(R.layout.logview_row_header, tableHdr, true);
        this.showFields = showFields;
        // switch (dbType) {
        //     case DeviceDaily:
    }

    @SuppressWarnings("SameParameterValue")
    Cursor getLogFile() {
        //  ALogFileWriter.Default.println(level, netTag, items);
        return new ALogCusor(ALogFileWriter.Default);
    }

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
