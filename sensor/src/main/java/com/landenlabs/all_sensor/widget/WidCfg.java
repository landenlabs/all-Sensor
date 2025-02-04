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

import static com.landenlabs.all_sensor.sensor.DeviceListManager.DEFAULT_DEVICE;
import static com.landenlabs.all_sensor.widget.WidView.getPref;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.landenlabs.all_sensor.Units;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.sensor.AbsCfg;
import com.landenlabs.all_sensor.utils.SpanUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Widget Configuration
 */
public class WidCfg extends AbsCfg {
    public static final String TAG = "WidCfg";
    public static final String DEVICE_NAME = "deviceName";
    public static final int MIN_WIDTH_DP = 60;    // 60 or 79
    public static final int WIDTH_SMALL_DP = 120;
    public static final int WIDTH_TRNED_DP = 230;     // show trend
    public static final int MIN_HEIGHT_DP = 115;
    public static final int HEIGHT_SMALL_DP = MIN_HEIGHT_DP;   // Show humidity (TODO 3+ rows)
    public static final int HEIGHT_TALL_DP = MIN_HEIGHT_DP*2;   // show history

    private static final SimpleDateFormat dateFmtL = new SimpleDateFormat("MMM d E hh:mm a");
    private static final SimpleDateFormat dateFmtM = new SimpleDateFormat("MMM d E hh a");
    private static final SimpleDateFormat dateFmtS = new SimpleDateFormat("d E");

    private static final String PREF_WID_CFG = "WidCfg1";
    private static final String PREF_WID_WIDTH_DP = "WidWth1";
    private static final String PREF_WID_HEIGHT_DP = "WidHgt1";
    private static final String PREF_WID_DEV = "WidDev1";
    private static final int BOOL_ARRAY_LEN = 7;                        // 0..5, 6
    private static final int TITLE_ARRAY_IDX = BOOL_ARRAY_LEN;          // 7
    private static final int DEVICE_ARRAY_IDX = TITLE_ARRAY_IDX + 1;    // 8
    private static final int WIDGET_CLASS_IDX = DEVICE_ARRAY_IDX + 1;   // 9
    private static final String SEP = ";";

    public String title;
    public String deviceName;
    public boolean showName;
    public boolean showTime;
    public boolean showTemperature;
    public boolean showHumidity;
    public boolean showTrend;
    public boolean showHistory;
    public Class<? extends WidView> widClass;

    public int widthDp = WIDTH_TRNED_DP;
    public int heightDp = HEIGHT_SMALL_DP;

    // ---------------------------------------------------------------------------------------------
    public WidCfg(Class<? extends WidView> widClass) {
        this.title = "";
        this.deviceName = DEFAULT_DEVICE;
        showName = showTime = showTemperature = showHumidity = showTrend = showHistory = true;
        tunit = Units.Temperature.Fahrenheit;
        this.widClass = widClass;
    }

    @NonNull
    @Override
    public String toString() {
        return ALog.tagStr(this) + " device=" + deviceName + " id=" + id;
    }

    public Class<? extends WidView> getWidgetClass() {
        return widClass;
    }

    public  void save(@NonNull Context context,  @NonNull Bundle widgetInfo) {
        int orientation = context.getResources().getConfiguration().orientation;
        // LG and Galaxy don't report correct values.
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            widthDp = widgetInfo.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
            heightDp = widgetInfo.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        } else {
            widthDp = widgetInfo.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            heightDp = widgetInfo.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        }

        ALog.d.tagMsg(this, "Save WidgetID=", id, " Width=", widthDp, " Height=", heightDp, " devName=", deviceName);
        getPref(context).edit()
                .putInt(PREF_WID_WIDTH_DP + id, widthDp)
                .putInt(PREF_WID_HEIGHT_DP + id, heightDp)
                .apply();

        save(context);
    }

    /**
     * Save array of cfg values associated with Widget ID.
     */
    @Override
    public void save(@NonNull Context context) {
        super.save(context);
        ALog.d.tagMsg(this, "Save WidgetID=", id, " devName=", deviceName);
        String toSave = getArray();
        getPref(context).edit().putString(PREF_WID_CFG + id, toSave)
                // .putString(PREF_WID_DEV + id, deviceName)
                .apply();
    }

    /**
     * Restore from saved cfg values per Widget ID
     */
    @SuppressWarnings("unchecked")
    @Override
    public void restore(@NonNull Context context, int widgetId) {
        super.restore(context, widgetId);

        SharedPreferences pref = getPref(context);
        String restoreFrom = pref.getString(PREF_WID_CFG + widgetId, "");
        String[] parts = restoreFrom.split(SEP);
        if (parts.length >= BOOL_ARRAY_LEN) {
            boolean[] bArray = new boolean[BOOL_ARRAY_LEN];
            for (int idx = 0; idx < BOOL_ARRAY_LEN; idx++) {
                bArray[idx] = "t".equals(parts[idx]);
            }
            takeBoolArray(bArray);
            title = (parts.length > TITLE_ARRAY_IDX) ? parts[TITLE_ARRAY_IDX] : "";
            deviceName = (parts.length > DEVICE_ARRAY_IDX) ? parts[DEVICE_ARRAY_IDX] : deviceName;
            try {
                widClass = (Class<? extends WidView>) Class.forName(((parts.length > WIDGET_CLASS_IDX) ? parts[WIDGET_CLASS_IDX] : WidViewPage.class.getName()));
            } catch (Exception ignore) {
                widClass = WidViewPage.class;
            }
        }

        // deviceName = pref.getString(PREF_WID_DEV + widgetId, deviceName);
        widthDp = pref.getInt(PREF_WID_WIDTH_DP + widgetId, widthDp);
        heightDp = pref.getInt(PREF_WID_HEIGHT_DP + widgetId, heightDp);
        ALog.d.tagMsg(this, "Restore WidgetID=", id, " devName=", deviceName);
    }

    public void restore(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            restore(context, appWidgetId);
            // deviceName = extras.getString(DEVICE_NAME, deviceName);
        }
    }

    /*
    public static int getWidgetId(Intent intent) {
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                return extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            }
        }
        ALog.e.tagMsg(TAG, "Failed to extract widgetID from intent ", intent);
        return -1;
    }
     */

    @Override
    public String tempFmt() {
        String tempFmt = "%.2f°%s";
        if (widthDp < WIDTH_SMALL_DP) {    // 225px
            tempFmt = "%.0f°%s";
        } else if (widthDp < WIDTH_TRNED_DP) {      // 330px
            tempFmt = "%.1f°%s";
        }
        return tempFmt;
    }


    @Override
    public String humFmt() {
        String tempFmt = "%.2f%%";
        if (widthDp < WIDTH_SMALL_DP) {    // 225px
            tempFmt = "%.0f%%";
        } else if (widthDp < WIDTH_TRNED_DP) {      // 330px
            tempFmt = "%.1f%%";
        }
        return tempFmt;
    }

    public String tempLbl() {
        String tempLbl = "Temperature";
        if (widthDp < WIDTH_TRNED_DP) {      // 330px
            tempLbl = "Temp";
        }
        return tempLbl;
    }

    public SpannableString dateFmt(long milli) {
        SpannableString ssTm;
        int smaller = 2;
        SimpleDateFormat dateFmt = dateFmtL;
        if (widthDp < 250) { // WIDTH_SMALL_DP) {
            dateFmt = dateFmtS;
            smaller = 0;
        } else if (widthDp < 349) { // WIDTH_TRNED_DP) {
            dateFmt = dateFmtM;
        }
        String strTm = dateFmt.format(new Date(milli));
        return SpanUtil.SString(strTm, SpanUtil.SS_SMALLER_50, strTm.length()-smaller, strTm.length());
    }


    // ---------------------------------------------------------------------------------------------
    // Utility functions

    @NonNull
    public String getArray() {
        StringBuilder sb = new StringBuilder();
        boolean[] bArray = new boolean[]{
                showName, showTime, showTemperature, showHumidity, showTrend, showHistory
                , tunit == Units.Temperature.Fahrenheit
        };
        for (boolean b : bArray) {
            if (sb.length() != 0) sb.append(SEP);
            sb.append(b ? 't' : 'f');
        }
        sb.append(SEP).append(title.replace(SEP, "_"));         // TITLE_ARRAY_IDX
        sb.append(SEP).append(deviceName.replace(SEP, "_"));    // DEVICE_ARRAY_IDX
        sb.append(SEP).append(widClass.getName());                           // WIDGET_CLASS_IDX

        return sb.toString();
    }

    void takeBoolArray(@Nullable boolean[] cfg) {
        if (cfg != null && cfg.length == BOOL_ARRAY_LEN) {
            showName = cfg[0];
            showTime = cfg[1];
            showTemperature = cfg[2];
            showHumidity = cfg[3];
            showTrend = cfg[4];
            showHistory = cfg[5];
            tunit = cfg[6] ? Units.Temperature.Fahrenheit : Units.Temperature.Celsius;
        } else {
            showName = showTime = showTemperature = showHumidity = showTrend = showHistory = true;
        }
    }

    public boolean firstTime() {
        return TextUtils.isEmpty(title);
    }
}