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

import static com.landenlabs.all_sensor.sensor.DeviceListManager.DEFAULT_DEVICE;

import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.Units;
import com.landenlabs.all_sensor.sensor.WxManager;
import com.landenlabs.all_sensor.ui.utils.SeekBarWithLabelHorizontal;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract Sensor Graphing fragment
 */
public abstract class GraphFragment extends BaseFragment
        implements View.OnClickListener, SeekBarWithLabelHorizontal.Apply {

    public static final String ARG_DEVICE = "device";
    public static final String ARG_DAYS = "days";
    public static final String ARG_MONTHS = "months";


    // Overridden by arguments
    protected String inputName = DEFAULT_DEVICE;
    protected Units.Duration units = Units.Duration.Days;

    protected int duration = 2;

    protected WxManager wxManager;

    protected TextView durationTitle, byDayTv, byHourTv;
    protected TextView plotValue1Tx, plotValue2Tx;
    protected SeekBarWithLabelHorizontal durationBar;
    protected TextView unitTg;
    protected final Map<String, TextView> gridViews = new HashMap<>();
    protected GridLayout sensorStatus;
    protected LayoutInflater inflater;

    protected Units.SampleBy sampleBy = Units.SampleBy.Hours;
    protected EnumSet<Units.Sensors> showSensors = EnumSet.allOf(Units.Sensors.class);

    protected boolean canUpdateUi = false;

    // ---------------------------------------------------------------------------------------------
    abstract protected void showGraphSensor(Units.Sensors sensor);

    abstract protected void updateUi();

    abstract protected void updateStatus();

    public void onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup root) {
        this.inflater = inflater;
        super.onCreateView(root);
        ScrollView vscroller = root.findViewById(R.id.vscroller);
        vscroller.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> barLayout.setExpanded(scrollY == 0, true));
    }

    protected void initIntervalUI() {
        showSensors.add(Units.Sensors.Temperature);
        showSensors.add(Units.Sensors.Humidity);
        if (getArguments() != null) {
            inputName = getArguments().getString(ARG_DEVICE, inputName);
            duration = getArguments().getInt(ARG_MONTHS, 0);
            if (duration > 0) {
                units = Units.Duration.Months;
            } else {
                units = Units.Duration.Days;
                duration = getArguments().getInt(ARG_DAYS, 2);
            }
        }

        sensorStatus = root.findViewById(R.id.sensor_grid1);
        durationTitle = root.findViewById(R.id.durationTitle);
        durationBar = new SeekBarWithLabelHorizontal(root, R.id.durationBar, R.id.durationBarLbl, this);
        byHourTv = root.findViewById(R.id.byHour);
        byDayTv = root.findViewById(R.id.byDay);
        plotValue1Tx = root.findViewById(R.id.plot_value1_toggle);
        plotValue2Tx = root.findViewById(R.id.plot_value2_toggle);
        unitTg = root.findViewById(R.id.unitTg);

        byHourTv.setOnClickListener(this);
        byDayTv.setOnClickListener(this);
        plotValue1Tx.setOnClickListener(this);
        plotValue2Tx.setOnClickListener(this);
        unitTg.setOnClickListener(this);

        setSampleBy(sampleBy, canUpdateUi);
        setDurationUnits(units);
        durationBar.seekBar.setProgress(duration);
        setShowSensor(showSensors, canUpdateUi);
        canUpdateUi = true;
    }

    @Override
    public boolean validSwipeRegion(MotionEvent ev) {
        Rect rect = new Rect();
        sensorStatus.getGlobalVisibleRect(rect);
        return (ev.getRawY() < rect.bottom);
    }

    @Override
    public void onResume() {
        super.onResume();
        restoreSettings();
    }

    @Override
    public void onPause() {
        saveSettings();
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.byHour) {
            setSampleBy(Units.SampleBy.Hours, true);
        } else if (id == R.id.byDay) {
            setSampleBy(Units.SampleBy.Days, true);
        } else if (id == R.id.plot_value1_toggle) {
            setShowSensor(toggle(showSensors, Units.Sensors.Temperature), true);
        } else if (id == R.id.plot_value2_toggle) {
            setShowSensor(toggle(showSensors, Units.Sensors.Humidity), true);
        } else if (id == R.id.unitTg) {
            setDurationUnits(!unitTg.isSelected() ? Units.Duration.Months : Units.Duration.Days);
        }
    }

    protected void updateUi(boolean doUpdate) {
        if (doUpdate) {
            updateUi();
        }
    }

    protected void addGridRow(String labelStr, String valueStr) {
        TextView value = gridViews.get(labelStr);
        if (value == null) {
            int rowCnt = sensorStatus.getRowCount();
            TextView label = (TextView) inflater.inflate(R.layout.grid_col0_label, sensorStatus, false);
            label.setText(labelStr);
            sensorStatus.addView(label,
                    new GridLayout.LayoutParams(GridLayout.spec(rowCnt), GridLayout.spec(0, GridLayout.END)));
            value = (TextView) inflater.inflate(R.layout.grid_col1_value, sensorStatus, false);
            sensorStatus.addView(value,
                    new GridLayout.LayoutParams(GridLayout.spec(rowCnt), GridLayout.spec(1, GridLayout.START)));
            gridViews.put(labelStr, value);
        }
        value.setText(valueStr);
    }

    // ----- Save/Restore
    private static final int PREF_VERSION = 100;
    private static final String KEY_VERSION = "version";
    private static final String KEY_UNIT_DURATION = "unitDuration";
    private static final String KEY_UNIT_SAMPLEBY = "unitSampleBy";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_SHOW_SENSORS = "showSensors";

    @Override
    protected void saveSettings() {
        getPref().edit()
                .putInt(KEY_VERSION, PREF_VERSION)
                .putString(KEY_UNIT_DURATION, units.name())
                .putString(KEY_UNIT_SAMPLEBY, sampleBy.name())
                .putInt(KEY_DURATION, duration)
                .putInt(KEY_SHOW_SENSORS, EnumSetBitmask.getMask(showSensors))
                .apply();
    }

    @Override
    protected void restoreSettings() {
        units = Units.Duration.valueOf(getPref().getString(KEY_UNIT_DURATION, Units.Duration.Days.name()));
        sampleBy = Units.SampleBy.valueOf(getPref().getString(KEY_UNIT_SAMPLEBY, Units.SampleBy.Hours.name()));
        duration = getPref().getInt(KEY_DURATION, 2);
        int showMask = getPref().getInt(KEY_SHOW_SENSORS, 3);
        showSensors = EnumSetBitmask.getSet(showMask);
        setDurationUnits(units);
        setSampleBy(sampleBy, false);
        durationBar.seekBar.setProgress(duration);
    }

    // ----- Interval UI
    @Override
    public void apply(SeekBar seekbar, int progress) {
        duration = progress;
        // durationBar.seekLbl.animate().alpha(0f).setDuration(3000).start();
        durationBar.seekLbl.setAlpha(0f);
        durationTitle.setText(durationTitle.getResources().getString(R.string.durationFmt, durationBar.seekBar.getProgress()));
        updateUi(canUpdateUi);
    }

    protected void setSampleBy(Units.SampleBy sampleBy, boolean doUpdate) {
        this.sampleBy = sampleBy;
        byHourTv.setSelected(sampleBy == Units.SampleBy.Hours);
        byDayTv.setSelected(sampleBy == Units.SampleBy.Days);
        if (byHourTv.isSelected()) {
            setDurationUnits(Units.Duration.Days);  // Force byHour duration uses Days, not months
        }
        updateUi(doUpdate);
    }

    protected void setDurationUnits(Units.Duration unit) {
        String unitStr = "Days";
        boolean selected = false;
        int maxProgress = 14;

        switch (unit) {
            default:
            case Days:
                break;
            case Months:
                setSampleBy(Units.SampleBy.Days, false);
                maxProgress = 12;
                unitStr = "Months";
                selected = true;
                break;
        }
        this.units = unit;
        unitTg.setText(unitStr);
        unitTg.setSelected(selected);
        durationBar.seekBar.setMax(maxProgress);
    }

    protected void setShowSensor(EnumSet<Units.Sensors> showSensors, boolean doUpdate) {
        plotValue1Tx.setSelected(showSensors.contains(Units.Sensors.Temperature));
        plotValue2Tx.setSelected(showSensors.contains(Units.Sensors.Humidity));

        if (doUpdate) {
            showGraphSensor(Units.Sensors.Temperature);
            showGraphSensor(Units.Sensors.Humidity);
        }
    }

    protected EnumSet<Units.Sensors> toggle(EnumSet<Units.Sensors> showSensors, Units.Sensors sensor) {
        if (showSensors.contains(sensor)) {
            showSensors.remove(sensor);
        } else {
            showSensors.add(sensor);
        }
        if (showSensors.isEmpty()) {
            showSensors.add(Units.Sensors.values()[(sensor.ordinal() + 1) % Units.Sensors.values().length]);
        }
        return showSensors;
    }

    // =============================================================================================
    // Convert EnumSet to/from bit mask value.
    protected static class EnumSetBitmask {

        public static <TT extends Enum<TT>> int getMask(EnumSet<TT> set) {
            int mask = 0;
            for (TT sensor : set) {
                mask |= 1 << sensor.ordinal();
            }
            return mask;
        }

        public static EnumSet<Units.Sensors> getSet(int mask) {
            EnumSet<Units.Sensors> set = EnumSet.noneOf(Units.Sensors.class);
            int item = 1;
            while (item <= mask) {
                if ((mask & item) != 0) {
                    set.add(Units.Sensors.values()[item / 2]);
                }
                item *= 2;
            }

            return set;
        }
    }
}

