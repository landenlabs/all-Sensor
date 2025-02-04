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
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.landenlabs.all_sensor.BuildConfig;
import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.Units;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.sensor.AppCfg;
import com.landenlabs.all_sensor.sensor.DeviceAccount;
import com.landenlabs.all_sensor.sensor.DeviceAdapter;
import com.landenlabs.all_sensor.sensor.DeviceListManager;
import com.landenlabs.all_sensor.sensor.WxManager;
import com.landenlabs.all_sensor.sensor.WxViewModel;
import com.landenlabs.all_sensor.ui.view.BaseFragment;
import com.landenlabs.all_sensor.utils.ArrayListEx;

/**
 * Start page for Sensor information.
 */
public class CfgFragment extends BaseFragment
        implements View.OnClickListener {

    private TextView unitF;
    private TextView unitC;
    private AppCfg appCfg;
    private RecyclerView listView;
    private DeviceAdapter deviceAdapter;
    private CheckBox showNotificationCb;
    private CheckBox showStatusCb;
    private CheckBox playSoundCb;

    // ---------------------------------------------------------------------------------------------

    @SuppressLint("ClickableViewAccessibility")
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {

        super.onCreateView( (ViewGroup) inflater.inflate(R.layout.fragment_cfg, container, false));
        appCfg = AppCfg.getInstance(requireContext());

        EditText user = root.findViewById(R.id.app_cfg_user);
        EditText pwd = root.findViewById(R.id.app_cfg_pwd);
        listView = root.findViewById(R.id.app_cfg_user_list);
        initList();
        unitF = root.findViewById(R.id.app_cfg_temp_unit_f);
        unitC = root.findViewById(R.id.app_cfg_temp_unit_c);
        unitF.setOnClickListener(this);
        unitC.setOnClickListener(this);
        showNotificationCb = root.findViewById(R.id.app_cfg_show_notification);
        showStatusCb = root.findViewById(R.id.app_cfg_show_status);
        playSoundCb = root.findViewById(R.id.app_cfg_sound_on_update);

        setUi(appCfg);

        Spinner devList = root.findViewById(R.id.app_cfg_device_list);

        // Create an ArrayAdapter using the string array and a default spinner layout
        deviceAdapter = new DeviceAdapter(getContext(), android.R.layout.simple_spinner_item,
                DeviceListManager.getDevices());
        deviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        devList.setAdapter(deviceAdapter);

        ALog.d.tagMsg(this, "widCfg deviceState device=", DeviceListManager.getDevices().size(),
                " list=", TextUtils.join(",", DeviceAdapter.getDeviceNameList(DeviceListManager.getDevices()).toArray()));
        if (DeviceListManager.getDevices().isEmpty()) {
            WxManager wxManager = WxManager.getInstance(getContext());

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

        return root;
    }

    @Override
    public void onPause() {
        getUi(appCfg);
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.app_cfg_temp_unit_f) {
            setUnit(Units.Temperature.Fahrenheit);
        } else {
            setUnit(Units.Temperature.Celsius);
        }
    }

    @Override
    public String getName() {
        return "Cfg";
    }

    @Override
    public String results() {
        return "";
    }

    @Override
    public boolean validSwipeRegion(MotionEvent ev) {
        return true;
    }

    private void updateUi(WxViewModel.Progress progress) {
    }

    private void initList() {
        listView.setLayoutManager(new LinearLayoutManager(getContext()));
        ArrayListEx<DeviceAccount> accountList = DeviceAccount.getAccounts(getContext());
        accountList.add(new DeviceAccount(BuildConfig.GoveeUser1, BuildConfig.GoveePwd1));
        accountList.add(new DeviceAccount(BuildConfig.GoveeUser1, BuildConfig.GoveePwd1));
        accountList.add(new DeviceAccount(BuildConfig.GoveeUser1, BuildConfig.GoveePwd1));

        listView.setAdapter(new CfgAccountAdapter(accountList));
        listView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
    }

    private void setUi(AppCfg appCfg) {
        showNotificationCb.setChecked(appCfg.showNotification);
        showStatusCb.setChecked(appCfg.showStatusBar);
        playSoundCb.setChecked(appCfg.soundOnUpdate);
        setUnit(appCfg.tunit);
    }

    private void getUi(AppCfg appCfg) {
        appCfg.showNotification = showNotificationCb.isChecked();
        appCfg.showStatusBar = showStatusCb.isChecked();
        appCfg.soundOnUpdate = playSoundCb.isChecked();
        appCfg.tunit = unitF.isSelected() ? Units.Temperature.Fahrenheit : Units.Temperature.Celsius;
        appCfg.save(requireContext());
    }

    private void setUnit(Units.Temperature tunit) {
        unitF.setSelected(tunit == Units.Temperature.Fahrenheit);
        unitC.setSelected(tunit == Units.Temperature.Celsius);
    }
}
