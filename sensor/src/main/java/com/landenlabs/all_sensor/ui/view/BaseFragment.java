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

package com.landenlabs.all_sensor.ui.view;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.CallSuper;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.AppBarLayout;
import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.ui.utils.GlobalModel;
import com.landenlabs.all_sensor.utils.ArrayListEx;
import com.landenlabs.all_sensor.utils.NetInfo;
import com.landenlabs.all_sensor.utils.SoundUtils;
import com.landenlabs.all_sensor.utils.StrUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Abstract baes fragment used for all fragments.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class BaseFragment extends Fragment implements Runnable {

    // Permissions
    protected static final int MY_PERMISSIONS_REQUEST = 27;
    private final static int STATUS_UPD_MILLI = 5000;

    // State
    private final static String RESULTS = "Results";
    protected ViewGroup root;
    protected TextView toolbarStatus;
    private MediaPlayer soundClick;

    protected androidx.appcompat.app.ActionBar actionBar;
    protected AppBarLayout barLayout;
    protected Toolbar toolbar;

    // Permission request
    private ActivityResultLauncher<String[]> multiplePermissionActivityResultLauncher;
    private interface PermissionsResultCallback {
        void onResult(Map<String,Boolean> activityResult);
    }
    private PermissionsResultCallback permissionsResultCallback;

    // ---------------------------------------------------------------------------------------------
    // Common methods
    @CallSuper
    public void onCreateView(ViewGroup root) {
        this.root = root;

        // View view = root.getRootView().findViewById(R.id.toolbar);
        if (getActivity() instanceof AppCompatActivity) {
            actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        }
    }

    abstract public String getName();

    abstract public String results();

    public abstract boolean validSwipeRegion(MotionEvent ev);

    int blink = 0;

    @Override
    public void onResume() {
        super.onResume();

        if (getView() != null) {
            barLayout = getView().getRootView().findViewById(R.id.bar_layout);
            toolbarStatus = getView().getRootView().findViewById(R.id.toolbarStatus);
            if (toolbarStatus != null) {
                updateToolbarStatusAndRepeat();
                toolbarStatus.setOnClickListener(v -> BaseFragment.this.startActivity(new Intent("android.settings.WIRELESS_SETTINGS")));
            }

            toolbar = requireView().getRootView().findViewById(R.id.toolbar);
        }
        restore();
    }

    @Override
    public void onPause() {
        toolbarStatus = null;
        save();
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == ORIENTATION_LANDSCAPE) {
            barLayout.setExpanded(false);
        }
    }

    protected SharedPreferences getPref() {
        return requireContext().getSharedPreferences(getName(), Context.MODE_PRIVATE);
    }

    protected void saveSettings() {
        /*
        getPref().edit()
                .putInt(STEP_MIN_KEY, seekStepSb.getProgress())
                .putBoolean(APPEND_KEY, appendMenuItem.isChecked())
                .putBoolean(AUTO_PERIOD_KEY, periodic.isChecked())
                .apply();
         */
    }

    protected void restoreSettings() {
                /*
        seekStepSb.setProgress(getPref().getInt(STEP_MIN_KEY, DEFAULT_SCH_MIN));
        periodic.setChecked(getPref().getBoolean(AUTO_PERIOD_KEY, true));
        if (getPref().getBoolean(APPEND_KEY, true)) {
            readLogIntoScroller();
        }
         */
    }

    @CallSuper
    protected boolean updateToolbarStatus() {
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd-MMM-yyyy ,", Locale.getDefault());

        NetInfo.NetStatus netStatus = new NetInfo.NetStatus(requireContext());
        SimpleDateFormat statusDateFmt = new SimpleDateFormat(" (hh:mm:ss a)", Locale.getDefault());
        String timeStr = statusDateFmt.format(new Date());

        // @ColorInt int[] brightColors = new int[]{Color.GREEN, Color.YELLOW, 0xffff60ff};
        @ColorInt int[] brightColors = new int[]{Color.BLACK, Color.BLACK, Color.BLACK};
        toolbarStatus.setText(NetInfo.getStatus(requireContext(), netStatus, timeStr, brightColors));
        toolbarStatus.setBackgroundColor( (netStatus.netInfo == null) ? 0x80ff8080 : Color.WHITE);
        return netStatus.netConn;
    }

    private void updateToolbarStatusAndRepeat() {
        if (updateToolbarStatus() || (blink++ % 2) == 1) {
            toolbarStatus.postDelayed(this, STATUS_UPD_MILLI);
        } else {
            toolbarStatus.setText("");
            toolbarStatus.postDelayed(this, STATUS_UPD_MILLI / 4);
        }
    }

    protected void setActionBarColor(@ColorInt int color) {
        if (barLayout != null) {
            barLayout.setBackgroundColor(color);
        }
    }

    protected void playSound(@RawRes int rawId) {
        soundClick = SoundUtils.startPlayer(requireContext(), rawId, soundClick);
    }

    protected void restore() {
        GlobalModel globalModel = new ViewModelProvider(requireActivity()).get(GlobalModel.class);
        String resultStr = globalModel.getValue(getName() + RESULTS, "");
        if (StrUtils.hasText(resultStr)) {
        }
    }

    protected void save() {
        GlobalModel globalModel = new ViewModelProvider(requireActivity()).get(GlobalModel.class);
        globalModel.setValue(getName() + RESULTS, results());
    }

    @NonNull
    protected Context getContextSafe() {
        return this.requireContext();
    }

    // ---------------------------------------------------------------------------------------------
    // Helper methods

    @NonNull
    public Activity getActivitySafe() {
        return requireActivity();
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public <T> T getServiceSafe(String service) {
        //noinspection unchecked
        return (T) Objects.requireNonNull(getActivitySafe().getSystemService(service));
    }

    public boolean checkPermissions(String... needPermissions) {
        boolean okay = true;
        List<String> requestPermissions = new ArrayListEx<>();
        for (String needPermission : needPermissions) {
            if (getContextSafe()
                    .checkSelfPermission(needPermission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions.add(needPermission);
            }
        }
        if (!requestPermissions.isEmpty()) {
            okay = false;
            if (multiplePermissionActivityResultLauncher == null) {
                multiplePermissionActivityResultLauncher = registerForActivityResult(
                        new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
                            if (permissionsResultCallback != null) {
                                permissionsResultCallback.onResult(isGranted);
                                permissionsResultCallback = null;
                            }
                        });
                this.permissionsResultCallback = new PermissionsResultCallback() {
                    @Override
                    public void onResult(Map<String, Boolean> activityResult) {
                        ALog.d.tagMsg(this, " requestPermissionResult for ", activityResult);
                    }
                };
            }
            multiplePermissionActivityResultLauncher.launch(requestPermissions.toArray(new String[0]));
        }

        return okay;
    }

    @Override
    public void run() {
        if (toolbarStatus != null) {
            updateToolbarStatusAndRepeat();
        }
    }

}
