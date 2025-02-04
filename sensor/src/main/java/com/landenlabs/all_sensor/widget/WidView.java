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

import static com.landenlabs.all_sensor.widget.WidCfg.HEIGHT_SMALL_DP;
import static com.landenlabs.all_sensor.widget.WidCfg.HEIGHT_TALL_DP;
import static com.landenlabs.all_sensor.widget.WidCfg.WIDTH_TRNED_DP;
import static com.landenlabs.all_sensor.widget.WidDataProvider.CMD_DATA;
import static com.landenlabs.all_sensor.widget.WidDataProvider.CMD_INFO;
import static com.landenlabs.all_sensor.widget.WidDataProvider.PREF_CMD_KEY;
import static com.landenlabs.all_sensor.widget.WidDataProvider.PREF_DATA_HUM;
import static com.landenlabs.all_sensor.widget.WidDataProvider.PREF_DATA_MILLI;
import static com.landenlabs.all_sensor.widget.WidDataProvider.PREF_DATA_SENSOR_NAME;
import static com.landenlabs.all_sensor.widget.WidDataProvider.PREF_DATA_TEMP;
import static com.landenlabs.all_sensor.widget.WidDataProvider.PREF_DATA_VALUE;
import static com.landenlabs.all_sensor.widget.WidDataProvider.PREF_INFO_LIST_KEY;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.landenlabs.all_sensor.MainActivity;
import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.logger.ALogFileWriter;
import com.landenlabs.all_sensor.logger.AppLog;
import com.landenlabs.all_sensor.sensor.AbsCfg;
import com.landenlabs.all_sensor.sensor.DeviceItem;
import com.landenlabs.all_sensor.sensor.DeviceSummary;
import com.landenlabs.all_sensor.sensor.ExecState;
import com.landenlabs.all_sensor.sensor.SensorItem;
import com.landenlabs.all_sensor.sensor.WxManager;
import com.landenlabs.all_sensor.utils.ArrayListEx;
import com.landenlabs.all_sensor.utils.SoundUtils;

import java.lang.ref.WeakReference;
import java.util.Arrays;

public abstract class WidView extends AppWidgetProvider {

    protected static final String LIST_SEP = "\n";
    protected static final String CLICK_OPN = "opn";
    protected static final String CLICK_MOR = "mor";
    protected static final String CLICK_UPD = "upd";
    protected static final String CLICK_CLR = "clr";
    protected static final String CLICK_INF = "inf";
    private static final String TAG = "WidView";
    private static WeakReference<SharedPreferences> PREF_REF;
    protected final WidCfg widCfg;

    // ---------------------------------------------------------------------------------------------

    protected WidView(Class<? extends WidView> widClass) {
        super();
        widCfg = new WidCfg(widClass);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle widgetInfo) {
        if (widCfg.id == appWidgetId) {
            ALog.w.tagMsg(this, "good wid id, cfgId=", widCfg.id,  " appWidId=", appWidgetId);
            widCfg.save(context, widgetInfo);
            // Calls update automatically
        } else {
            ALog.w.tagMsg(this, "wrong wid id, cfgId=", widCfg.id,  " appWidId=", appWidgetId);
        }
    }

    abstract void updateAppWidget(
            @NonNull Context context,
            @Nullable AppWidgetManager appWidgetManager,
            int widgetId);

    protected void setVisibility(Context context, RemoteViews views) {

        int vis;
        vis = widCfg.showName ? View.VISIBLE : View.GONE;
        // views.setViewVisibility(R.id.wid_more, vis);
        views.setViewVisibility(R.id.wid_title, vis);

        vis = widCfg.showTime ? View.VISIBLE : View.GONE;
        views.setViewVisibility(R.id.wid_date, vis);

        vis = widCfg.showTemperature ? View.VISIBLE : View.GONE;
        views.setViewVisibility(R.id.wid_temp_lbl, vis);
        views.setViewVisibility(R.id.wid_temp_val, vis);

        vis = widCfg.showHumidity ? View.VISIBLE : View.GONE;
   //     vis = (this.widCfg.heightDp > HEIGHT_SMALL_DP) ? vis : View.GONE;  // 110px to 238px on Galaxy S9
        views.setViewVisibility(R.id.wid_hum_lbl, vis);
        views.setViewVisibility(R.id.wid_hum_val, vis);

        setTrendVisibility(views, widCfg.showTrend);

        vis = widCfg.showHistory ? View.VISIBLE : View.GONE;
        vis = (this.widCfg.heightDp >= HEIGHT_TALL_DP) ? vis : View.GONE;     // 386px
        views.setViewVisibility(R.id.wid_btn_holder, vis);
        views.setViewVisibility(R.id.widget_list, vis);
    }

    protected void setTrendVisibility(RemoteViews views, boolean visible) {
        ALog.d.tagMsg(this, " Trend widSize ", widCfg.widthDp, " x ", widCfg.heightDp, " WIDTH_MED_DP=", WIDTH_TRNED_DP, " HEIGHT_SMALL_DP=", HEIGHT_SMALL_DP);
        int vis = visible ? View.VISIBLE : View.GONE;
        vis =  (widCfg.widthDp > WIDTH_TRNED_DP) ? vis : View.GONE;
        views.setViewVisibility(R.id.wid_temp_trend_img, vis);
        views.setViewVisibility(R.id.wid_temp_trend_val, vis);
    //    vis = (widCfg.heightDp > HEIGHT_SMALL_DP) ? vis : View.GONE;  // 110px to 238px on Galaxy S9
        views.setViewVisibility(R.id.wid_hum_trend_img, vis);
        views.setViewVisibility(R.id.wid_hum_trend_val, vis);
    }

    @Override
    public void onUpdate(
            @NonNull Context context,
            @Nullable AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        AppLog.init(context);
        ALogFileWriter.init(context);
        // JodaTimeAndroid.init(context);
        // onEnabled(context);

        ALog.i.tagMsg(TAG, "+ onUpdate");
        WxManager wxManager = WxManager.getInstance(context); // Initialize devices, etc.
        ExecState state = wxManager.start(context, false);
        ALog.d.tagMsg(this, "  ++ Waiting for wxManager state=", state.complete);

        state.complete.thenAccept(r -> {
            for (int appWidgetId : appWidgetIds) {
                ALog.d.tagMsg(TAG, "   +++ Update widget=", appWidgetId);
                updateAppWidget(context, appWidgetManager, appWidgetId /*, deviceName */);
            }
        });

        /*
        // Prevent infinite loop caused by
        // broadcast of ACTION_PACKAGE_CHANGED in the process of
        // idle resource cleanup by the work manager.
        if (isUpdateTooFast(context.getApplicationContext())) {
            ALog.w.tagMsg(TAG, "update too fast");
            return;
        }

        Schedule update via WorkManager
        WidgetScheduler.scheduleWidgetUpdate(context, appWidgetIds);
         */
    }

    protected void setupClickAction(Context context, RemoteViews views, WidCfg widCfg) {
        // Setup update button to send an update request as a pending intent.
        Intent intentUpdate = new Intent(context, getClass());

        // The intent action must be an app widget update.
        intentUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        // Include the widget ID to be updated as an intent extra.
        addExtra(intentUpdate, widCfg);

        // Wrap it all in a pending intent to send a broadcast.
        // Use the app widget ID as the request code (2nd argument) so that
        // each intent is unique.
        PendingIntent pendingUpdate = PendingIntent.getBroadcast(context,
                widCfg.id, intentUpdate, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Assign the pending intent to the button onClick handler
   //     views.setOnClickPendingIntent(R.id.wid_top_holder, pendingUpdate);

        views.setOnClickPendingIntent(R.id.wid_icon, getPendingSelfIntent(context, CLICK_OPN, widCfg));
        views.setOnClickPendingIntent(R.id.wid_more, getPendingSelfIntent(context, CLICK_MOR, widCfg));
        views.setOnClickPendingIntent(R.id.widUpdBtn, getPendingSelfIntent(context, CLICK_UPD, widCfg));
        views.setOnClickPendingIntent(R.id.widClrBtn, getPendingSelfIntent(context, CLICK_CLR, widCfg));
        views.setOnClickPendingIntent(R.id.widInfoBtn, getPendingSelfIntent(context, CLICK_INF, widCfg));

        views.setOnClickPendingIntent(R.id.wid_value_holder, getPendingSelfIntent(context, CLICK_UPD, widCfg));
        views.setOnClickPendingIntent(R.id.wid_temp_holder, getPendingSelfIntent(context, CLICK_UPD, widCfg));
        views.setOnClickPendingIntent(R.id.wid_hum_holder, getPendingSelfIntent(context, CLICK_UPD, widCfg));
    }

    abstract Class<? extends WidConfigActivity> getConfigActivityClass();

    public static Intent addExtra(Intent intent, WidCfg widCfg) {
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widCfg.id);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{widCfg.id});
        intent.putExtra(WidCfg.DEVICE_NAME, widCfg.deviceName);
        intent.putExtra(WidCfg.TAG, widCfg.getArray());
        return intent;
    }

    protected PendingIntent getPendingSelfIntent(Context context, String action, WidCfg widCfg) {
        if (CLICK_OPN.equals(action)) {
            Intent intent = new Intent(context, MainActivity.class);
            addExtra(intent, widCfg);
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else if (CLICK_MOR.equals(action)) {
            Intent intent = new Intent(context, getConfigActivityClass());
            addExtra(intent, widCfg);
            return PendingIntent.getActivity(context, widCfg.id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }

        Intent intent = new Intent(context, getClass());
        addExtra(intent, widCfg);
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();
        switch (action) {
            case AppWidgetManager.ACTION_APPWIDGET_DISABLED:
            case AppWidgetManager.ACTION_APPWIDGET_DELETED:
                // TODO - remove widCfg files for old widgets
                return;
        }

        // DEBUG
        AppWidgetManager appWidMgr = AppWidgetManager.getInstance(context);
        ComponentName cn = new ComponentName(context, this.getClass()); // WidDataProvider.class);
        int[] appWidgetIds = appWidMgr.getAppWidgetIds(cn);
        ALog.d.tagMsg(this, "onReceiver ids=", Arrays.toString(appWidgetIds), " action=", action);

    //    if (appWidgetIds == null || appWidgetIds.length == 0) {
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            ALog.d.tagMsg(this, "onReceiver 3 ids=", appWidgetId, " action=", action);
            if (appWidgetId != -1) {
                appWidgetIds = new int[] { appWidgetId };
            }
    //    }

        // action.APPWIDGET_DELETED has no list of ids
        if (appWidgetIds != null && appWidgetIds.length > 0) {
            widCfg.restore(context, appWidgetIds[0]);
        }
        SharedPreferences.Editor prefEdit = getPref(context).edit();
        boolean dataChanged = false;

        if (action != null)
            switch (action) {
                case CLICK_CLR:
                    prefEdit.putString(PREF_CMD_KEY, CMD_DATA); // TODO - set start to display time to now to emulate clear.
                    dataChanged = true;
                    break;
                case CLICK_INF:
                    prefEdit.putString(PREF_CMD_KEY, CMD_INFO);
                    dataChanged = true;
                    break;
                case CLICK_OPN:
                    break;
                case CLICK_UPD:
                    prefEdit.putString(PREF_CMD_KEY, CMD_DATA);
                    dataChanged = true;
                    break;
                case AppWidgetManager.ACTION_APPWIDGET_ENABLED:
                    break;
                case AppWidgetManager.ACTION_APPWIDGET_UPDATE:
                    break;
            }

        prefEdit.apply();
        if (dataChanged) {
            // notifyAppWidgetViewDataChanged calls onDataSetChanged()
            appWidMgr.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);
        }

        // Call onUpdate once.
        super.onReceive(context, intent);   // calls onUpdate  if action is ACTION_APPWIDGET_UPDATE
        if (!AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            onUpdate(context, appWidMgr, appWidgetIds);
            // SoundUtils.playNotificationSound(context);
            SoundUtils.playSound(context, R.raw.beep2);
        }
    }

    /*
    // =============================================================================================
    public static class WidgetScheduler {

        public static void scheduleWidgetUpdate(Context context, int[] appWidgetId) {
            Data source = new Data.Builder()
                    .putIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetId)
                    .build();

            Constraints myConstraints = new Constraints.Builder()
                    // .setRequiresDeviceIdle(true)
                    // .setRequiresCharging(false)
                    .setRequiresBatteryNotLow(true)
                    // .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            int min = 20;
            OneTimeWorkRequest.Builder periodBuilder =
                    new OneTimeWorkRequest.Builder(WidgetWork.class)
                            .setInitialDelay(min, TimeUnit.MINUTES);

            periodBuilder.setInputData(source);
            periodBuilder.setConstraints(myConstraints);
            periodBuilder.addTag("Sensor");
            OneTimeWorkRequest workRequest = periodBuilder.build();

            WorkManager.getInstance(context).enqueue(workRequest);
        }
    }

    // =============================================================================================
    public static class WidgetWork extends Worker {

        private final Context ctx;
        // private WidDataProvider dataProvider;
        private final int[] appWidgetId;

        public WidgetWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
            ctx = context;
            appWidgetId = workerParams.getInputData().getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS);
        }

        @NonNull
        @Override
        public Worker.Result doWork() {
            WidDataProvider dataProvider = null; // new WidDataProvider(ctx, null);
            ALog.d.tagMsg(TAG, "widget-scheduled-doWork");
            logFileLn(Log.DEBUG, TAG, "widget-scheduled-doWork");

            if (appWidgetId != null && appWidgetId.length > 0) {
                freshenWidget(dataProvider, appWidgetId);
                return Worker.Result.success();
            }
            return Worker.Result.failure();
        }

        private void freshenWidget(@Nullable WidDataProvider dataProvider, int[] appWidgetId) {
            // Optionally - Do async work to get extra data, pass to widget
            sendWidgetUpdate("", appWidgetId);
        }

        private void sendWidgetUpdate(String extra, int[] appWidgetId) {

            ALog.d.tagMsg(TAG, "widget-scheduled-sendWidgetUpdate");
            logFileLn(Log.DEBUG, TAG, "widget-scheduled-sendWidgetUpdate");

            // Broadcast message to widget provider to update widget
            Intent widgetIntent = new Intent();

            widgetIntent.setAction(WidViewList.DISPLAY_PLACE);
            widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,  appWidgetId);
            // widgetIntent.putExtra(WidViewList.PLACE_NAME, extra);

            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(widgetIntent);
        }
    }
     */

    // ---------------------------------------------------------------------------------------------
    public static ArrayListEx<CharSequence> getStringList(SharedPreferences pref, String key) {
        String listStr = pref.getString(key, null);
        ArrayListEx<CharSequence> list = new ArrayListEx<>();
        if (listStr != null) list.addAll(listStr.split(LIST_SEP));
        // ALog.d.tagMsg(TAG, "getStringList ", key, " list=", listStr);
        return list;
    }

    public static void putStringList(SharedPreferences pref, String key, ArrayListEx<CharSequence> list) {
        // TODO - handle SpannableString
        String listStr = TextUtils.join(LIST_SEP, list);
        // ALog.d.tagMsg(TAG, "putStringList ", key, " list=", listStr);
        pref.edit().putString(key, listStr).apply();
    }

    public static void logIt(Context context, DeviceItem deviceItem, AbsCfg cfg) {
        if (deviceItem.isValid()) {
            WidHistory widHistory = new WidHistory(WidHistory.FIELDS_TH);
            widHistory.load(context, deviceItem.name);

            SharedPreferences sharedPref = getPref(context);
            saveInfo(deviceItem, cfg, sharedPref);

            DeviceSummary summary = deviceItem.getSummary(context, cfg);
            WidHistory.WidHistoryItem histItem = new WidHistory.WidHistoryItem();
            histItem.milli = deviceItem.lastMilli();
            histItem.values = new int[] {summary.numTemp, summary.numHum};
            widHistory.append(histItem);
            widHistory.save(context, deviceItem.name);

            sharedPref.edit()
                    .putInt(PREF_DATA_TEMP, summary.numTemp)
                    .putInt(PREF_DATA_HUM, summary.numHum)
                    .putString(PREF_DATA_SENSOR_NAME, deviceItem.name)
                    .putLong(PREF_DATA_MILLI, deviceItem.lastMilli())
                    .apply();
        }
    }

    // private static final String SENOR_NAME = "_sensor";

    public static void logIt(Context context, SensorItem sensorItem, AbsCfg cfg) {
        if (sensorItem.isValid()) {
            WidHistory widHistory = new WidHistory(WidHistory.FIELDS_V);
            widHistory.load(context, sensorItem.name);

            SharedPreferences sharedPref = getPref(context);
            // saveInfo(sensorItem, cfg, sharedPref);

            // DeviceSummary summary = deviceItem.getSummary(context, cfg);
            WidHistory.WidHistoryItem histItem = new WidHistory.WidHistoryItem();
            histItem.milli = sensorItem.lastMilli();
            histItem.values = new int[] {sensorItem.iValue()};
            widHistory.append(histItem);
            widHistory.save(context, sensorItem.name);

            new Thread( () ->
                sharedPref.edit()
                        .putInt(PREF_DATA_VALUE+sensorItem.id, sensorItem.iValue())
                        .putString(PREF_DATA_SENSOR_NAME +sensorItem.id, sensorItem.name)
                        .putLong(PREF_DATA_MILLI+sensorItem.id, sensorItem.lastMilli())
                        .apply()).start();;
        }
    }

    protected static void saveInfo(@NonNull DeviceItem deviceItem, AbsCfg cfg, @NonNull SharedPreferences pref) {
        if (deviceItem.isValid()) {
            ArrayListEx<CharSequence> list = deviceItem.getInfoList(null, cfg);
            putStringList(pref, PREF_INFO_LIST_KEY, list);
            ALog.d.tagMsg(TAG, "saved info size=", list.size());
        }
    }

    /*
    private static boolean doRegister = true;
    @Override
    public void onEnabled(Context context) {
        // Toast.makeText(context, "onEnabled called", Toast.LENGTH_LONG).show();

        if (doRegister) {
            doRegister = false;
            ALog.i.tagMsg(TAG, "registerReceiver");
            LocalBroadcastManager.getInstance(context)
                    .registerReceiver(this, new IntentFilter(DISPLAY_PLACE));
            LocalBroadcastManager.getInstance(context)
                    .registerReceiver(this, new IntentFilter(WIDGET_CLICK));
        }

        / *
        // Location access permission is required
        if (!(ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)) {
            Intent mainActivity = new Intent(context, MainActivity.class);
            context.startActivity(mainActivity);
        }
         * /
    }

    @Override
    public void onDisabled(Context context) {
        // Toast.makeText(context, "onDisabled called", Toast.LENGTH_LONG).show();
        if (!doRegister) {
            ALog.i.tagMsg(TAG, "unregisterReceiver");
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
            doRegister = true;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        ALogFileWriter.init(context);
        JodaTimeAndroid.init(context);

        ALog.d.tagMsg(TAG, "widget-scheduled-onReceive ", intent.getAction());
        logFileLn(Log.DEBUG, TAG, "widget-scheduled-onReceive ", intent.getAction());

        if (DISPLAY_PLACE.equals(intent.getAction())) {
            // String placeName = intent.getStringExtra(PLACE_NAME);
            // updateAppWidget(context.getApplicationContext(), placeName);
            int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            if (appWidgetIds != null) {
                onUpdate(context, null, appWidgetIds);
            }
        } else if (WIDGET_CLICK.equals(intent.getAction())) {
            // Start settings activity
            Intent settings = new Intent(context, WidConfigActivity.class);
            settings.setAction(WIDGET_CLICK);
            context.startActivity(settings);
        }
    }

    // Prevent infinite loop by throttling updates.
    // See - Work manager, ACTION_PACKAGE_CHANGED  and onUpdate.
    private static boolean isUpdateTooFast(Context context){
        SharedPreferences sharedPref = getPref(context);

        final String LAST_UPDATE_MILLI = "lastUpdateMilli";
        long lastUpdateMilli = sharedPref.getLong(LAST_UPDATE_MILLI, 0);

        long currentMilli = System.currentTimeMillis();
        long deltaMilli = (currentMilli - lastUpdateMilli);
        if (deltaMilli > TimeUnit.MINUTES.toMillis(1)) {
            sharedPref.edit().putLong(LAST_UPDATE_MILLI, currentMilli).apply();
            return false;
        }
        return true;
    }
     */

    @NonNull
    synchronized
    public static SharedPreferences getPref(@Nullable Context context) {
        if (PREF_REF == null || PREF_REF.get() == null) {
            if (context != null) {
                SharedPreferences pref =
                        context.getSharedPreferences("sensorWidget", Context.MODE_PRIVATE);
                PREF_REF = new WeakReference<>(pref);
            }
        }
        return PREF_REF.get();
    }
}
