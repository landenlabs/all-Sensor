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

package com.landenlabs.all_sensor.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.landenlabs.all_sensor.BuildConfig;
import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.Units;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.sensor.DeviceAdapter;
import com.landenlabs.all_sensor.sensor.DeviceListManager;
import com.landenlabs.all_sensor.sensor.WxManager;
import com.landenlabs.all_sensor.utils.StrUtils;

/**
 * Abstract Widget Configuration base class.
 */
public abstract class WidConfigActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "WidConfigActivity";
    protected final WidCfg widCfg;
    protected SharedPreferences pref;

    private Spinner devList;
    protected TextView widTitle;
    protected CheckBox showHisCb;
    protected CheckBox showHumCb;
    protected CheckBox showTemCb;
    protected CheckBox showTimCb;
    protected CheckBox showTrnCb;
    protected CheckBox showNamCb;
    protected TextView unitF;
    protected TextView unitC;

    DeviceAdapter deviceAdapter;

    // ---------------------------------------------------------------------------------------------
    public WidConfigActivity(Class<? extends WidView> widClass) {
        super();
        widCfg = new WidCfg(widClass);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = WidView.getPref(getBaseContext());

        // setContentView(R.layout.widget_config);
        setResult(RESULT_CANCELED);

        restoreCfg();

        TextView showWid = findViewById(R.id.wid_cfg_show);
        showWid.setOnClickListener(v -> handleWidCreate());
        TextView saveWidSettings = findViewById(R.id.wid_cfg_save_settings);
        saveWidSettings.setOnClickListener(v -> handleSaveWidSettings());

        devList = findViewById(R.id.wid_cfg_device_list);
        widTitle = findViewById(R.id.wid_cfg_wid_title);
        showNamCb = findViewById(R.id.wid_cfg_show_name);
        showTimCb = findViewById(R.id.wid_cfg_show_time);
        showTemCb = findViewById(R.id.wid_cfg_show_temp);
        showHumCb = findViewById(R.id.wid_cfg_show_hum);
        showTrnCb = findViewById(R.id.wid_cfg_show_trend);
        showHisCb = findViewById(R.id.wid_cfg_show_his);

        unitF = findViewById(R.id.wid_cfg_temp_unit_f);
        unitC = findViewById(R.id.wid_cfg_temp_unit_c);
        unitF.setOnClickListener(this);
        unitC.setOnClickListener(this);

        // Create an ArrayAdapter using the string array and a default spinner layout
        deviceAdapter = new DeviceAdapter(this, android.R.layout.simple_spinner_item,
                DeviceListManager.getDevices());
        deviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        devList.setAdapter(deviceAdapter);
        // devList.setSelection(0);   // TODO - save/restore selected item.

        ALog.d.tagMsg(this, "widCfg deviceState device=", DeviceListManager.getDevices().size(),
                " list=", TextUtils.join(",", DeviceAdapter.getDeviceNameList(DeviceListManager.getDevices()).toArray()));
        if (DeviceListManager.getDevices().isEmpty()) {
            WxManager wxManager = WxManager.getInstance(this);

            wxManager.getDeviceState().complete.thenAccept(xx -> {
                ALog.d.tagMsg(this, "widCfg deviceState done device=", DeviceListManager.getDevices().size(),
                        " list=", TextUtils.join(",", DeviceAdapter.getDeviceNameList(DeviceListManager.getDevices()).toArray()));
                deviceAdapter.clear();
                deviceAdapter.addAll(DeviceAdapter.getDeviceNameList(DeviceListManager.getDevices()));
            }).exceptionally(xx -> {
                ALog.w.tagMsg(this, "widCfg deviceState exception=", xx);
                wxManager.getDeviceState().fail(xx);
                return null;
            });
        }

        if (devList.getSelectedItem() == null) {
            Toast.makeText(getApplicationContext(), "No Device specified", Toast.LENGTH_LONG).show();
            // widCfg.deviceName = devList.getItemAtPosition(0).toString();
        } else {
            widCfg.deviceName = devList.getSelectedItem().toString();
        }
        setUi(widCfg);
    }

    @Override
    public void onClick(@NonNull View view) {
        int id = view.getId();
        if (id == R.id.wid_cfg_temp_unit_f) {
            widCfg.tunit = Units.Temperature.Fahrenheit;
        } else {
            widCfg.tunit = Units.Temperature.Celsius;
        }
        setUnit(widCfg.tunit);
    }

    protected void setUi(@NonNull WidCfg widCfg) {
        String title = widCfg.title;
        if (TextUtils.isEmpty(title)) {
            title = widCfg.deviceName;
        }
        if (TextUtils.isEmpty(title)) {
            title = "Sensor #" + widCfg.id;
        } else if (BuildConfig.DEBUG) {
            title = title.replace(" #" + widCfg.id, "");
            title += " #" + widCfg.id;  // See add ID to title
        }

        // widTitle.setText(String.format("(%d)%s", widCfg.widgetId, title));
        widTitle.setText(title);
        showNamCb.setChecked(widCfg.showName);
        showTimCb.setChecked(widCfg.showTime);
        showTemCb.setChecked(widCfg.showTemperature);
        showHumCb.setChecked(widCfg.showHumidity);
        showTrnCb.setChecked(widCfg.showTrend);
        showHisCb.setChecked(widCfg.showHistory);
        setUnit(widCfg.tunit);
    }

    protected void getUi(@NonNull WidCfg widCfg) {
        widCfg.title = widTitle.getText().toString();
        if (BuildConfig.DEBUG) {    // See add ID to title
            widCfg.title = widCfg.title.replace(" #" + widCfg.id, "");
        }
        widCfg.showName = showNamCb.isChecked();
        widCfg.showTime = showTimCb.isChecked();
        widCfg.showTemperature = showTemCb.isChecked();
        widCfg.showHumidity = showHumCb.isChecked();
        widCfg.showTrend = showTrnCb.isChecked();
        widCfg.showHistory = showHisCb.isChecked();
        widCfg.tunit = unitF.isSelected() ? Units.Temperature.Fahrenheit : Units.Temperature.Celsius;
        widCfg.deviceName = StrUtils.asString(devList.getSelectedItem(), DeviceListManager.DEFAULT_DEVICE);
    }

    protected void setUnit(@NonNull Units.Temperature tunit) {
        unitF.setSelected(tunit == Units.Temperature.Fahrenheit);
        unitC.setSelected(tunit == Units.Temperature.Celsius);
    }

    protected void handleWidCreate() {
        getUi(widCfg);
        widCfg.save(getApplicationContext());
        createWidget();
        updateWidget();
        finish();
    }

    protected void restoreCfg() {
        widCfg.restore(this, getIntent());   // Get widget's intent
    }

    protected void handleSaveWidSettings() {
        Toast.makeText(WidConfigActivity.this,
                "Config Sensor", Toast.LENGTH_LONG).show();
        getUi(widCfg);
        widCfg.save(getApplicationContext());
        updateWidget();
        finish();
    }

    protected void createWidget() {
        Intent resultValue = new Intent();
        WidView.addExtra(resultValue, widCfg);
        setResult(RESULT_OK, resultValue);
    }

    private void updateWidget() {
        // AppWidgetManager manager = AppWidgetManager.getInstance(context);

        Intent intentUpdate = new Intent(getApplicationContext(), widCfg.getWidgetClass());
        intentUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        // Include the widget ID to be updated as an intent extra.
        WidView.addExtra(intentUpdate, widCfg);

        ALog.d.tagMsg(this, "force update widget=", widCfg.getWidgetClass());
        getApplicationContext().sendBroadcast(intentUpdate);

        // PendingIntent pendingUpdate = PendingIntent.getBroadcast(getApplicationContext(),
        //        widCfg.widgetId, intentUpdate, PendingIntent.FLAG_UPDATE_CURRENT);

       /*

        Context context = getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, WidDataProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);

        */
    }


}