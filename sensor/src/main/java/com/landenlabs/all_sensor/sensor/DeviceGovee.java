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

import static com.landenlabs.all_sensor.sensor.WxViewModel.FAILED_LOGIN_MSG;
import static com.landenlabs.all_sensor.sensor.WxViewModel.FAILED_SUMMARY_MSG;
import static com.landenlabs.all_sensor.sensor.WxViewModel.LOAD_DATA_MSG;
import static com.landenlabs.all_sensor.sensor.WxViewModel.LOAD_START_MSG;
import static com.landenlabs.all_sensor.sensor.WxViewModel.LOAD_SUMMARY_MSG;
import static com.landenlabs.all_sensor.sensor.WxViewModel.SERIES_MSG;
import static com.landenlabs.all_sensor.sensor.WxViewModel.STATUS_MSG;
import static com.landenlabs.all_sensor.utils.RefUtils.extract;

import android.content.Context;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.db.DbDeviceDaily;
import com.landenlabs.all_sensor.db.DbDeviceHourly;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.utils.ArrayListEx;
import com.landenlabs.all_sensor.utils.NetInfo;
import com.landenlabs.all_sensor.utils.SpanUtil;
import com.landenlabs.all_sensor.widget.WidViewList;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Govee Weather device/sensor manager.
 */
public class DeviceGovee extends DeviceItem {

    private static final String TAG = WxManager.class.getSimpleName();
    public static final DeviceGovee EMPTY =
            new DeviceGovee( null, DeviceGoveeHelper.DEVICE_EMPTY,
            new DeviceAccount("?user", "?pwd"),
            new DeviceGoveeHelper());

    public final WeakReference<Context> refContext;
    public final DeviceGoveeHelper devHelper;
    public final DeviceAccount account;
    public final ExecState state = new ExecState("DG-state");
    public DateTime freshenDbStart = DateTime.now();

    public WxViewModel viewModel = null;
    private ThreadCloudDbLoader loaderThread;

    private static final String DB_HOUR_NAME = "dbHourly.db";
    private static final String DB_DAILY_RECORD_NAME = "dbDaily.db";
    private static final Pattern FRACT_PAT = Pattern.compile("[.][0-9]+");

    // ---------------------------------------------------------------------------------------------

    // Only used to make static empty instance
    public DeviceGovee(
            @Nullable Context context,
            @NonNull DeviceGoveeHelper.Device device,
            @NonNull DeviceAccount account,
            @NonNull DeviceGoveeHelper devHelper) {
        this.refContext = new WeakReference<>(context);
        this.name = device.deviceName;
        // this.device = device;
        this.account = account;
        this.devHelper = devHelper;
    }

    @Override
    boolean init(@NonNull Context context, IwxManager manager, ExecState state) {
        viewModel = manager.viewModel();
        return openDatabase(context);
    }

    @Override
    public long lastMilli() {
        return devHelper.lastMilli();
    }

    @Override
    public boolean isValid() {
        return (refContext.get() != null)
                && NetInfo.haveNetwork(refContext.get())
                && devHelper.isValid(this.getClass().getSimpleName());
    }

    @Nullable
    @Override
    public Exception getException() {
        return devHelper.loginException;
    }

    @Override
    public void setException(Exception ex) {
        devHelper.loginException = ex;
    }

    @NonNull
    @Override
    public String toString() {
        return ALog.tagStr(this) + name;
    }
/*
    private void makeValid() {
        if (device.deviceExt == null) {
            device.deviceExt = new DeviceGoveeHelper.DeviceExt();
        }
        if (device.deviceExt.lastDeviceData == null) {
            device.deviceExt.lastDeviceData = new DeviceGoveeHelper.LastDeviceData();
        }
    }
     */

    @Override
    @NonNull
    public DeviceSummary getSummary(@NonNull Context context, @NonNull AbsCfg cfg) {
        DeviceSummary summary = new DeviceSummary();

        final DeviceGoveeHelper.Device device = devHelper.getDevice();
        if (device.isValid(devHelper)) {
            summary.devName = device.deviceName;
            summary.strTime = cfg.timeFmt(device.deviceExt.lastDeviceData.lastTime);
            summary.strDate = cfg.dateFmt(device.deviceExt.lastDeviceData.lastTime);

            String strTemp = cfg.toDegree(device.deviceExt.lastDeviceData.tem);
            
            SpannableString ss1 = SpanUtil.SString(strTemp, SpanUtil.SS_SUPER + SpanUtil.SS_SMALLER, strTemp.length()-2, strTemp.length());
            summary.strTemp = fraction(ss1);
            String strHum = cfg.toHumPer(device.deviceExt.lastDeviceData.hum);
            SpannableString ss2 = SpanUtil.SString(strHum, SpanUtil.SS_SUPER + SpanUtil.SS_SMALLER, strHum.length()-1, strHum.length());
            summary.strHum = fraction(ss2);

            summary.numTemp = device.deviceExt.lastDeviceData.tem;
            summary.numHum = device.deviceExt.lastDeviceData.hum;
            summary.numTime = device.deviceExt.lastDeviceData.lastTime;

            if (device.deviceExt.deviceSettings.temWarning) {
                summary.numAlarmTempMin = device.deviceExt.deviceSettings.temMin;
                summary.numAlarmTempMax = device.deviceExt.deviceSettings.temMax;
            }
            if (device.deviceExt.deviceSettings.humWarning) {
                summary.numAlarmHumMin = device.deviceExt.deviceSettings.humMin;
                summary.numAlarmHumMax = device.deviceExt.deviceSettings.humMax;
            }
            summary.numBattery = device.deviceExt.deviceSettings.battery;
            summary.numWifi = device.deviceExt.deviceSettings.wifiLevel;
        }

        ALog.d.tagMsg(this, "getSummary(id=", cfg.id, ") ",
                " Temp=",  summary.strTemp,
                " hum=",  summary.strHum,
                " date=",  summary.strDate);

        return summary;
    }

    private SpannableString fraction(SpannableString ss) {
        Matcher m = FRACT_PAT.matcher(ss);
        if (m.find()) {
            ss.setSpan(new RelativeSizeSpan(0.7f), m.start(), m.end(), 0);
        }
        return ss;
    }

    @NonNull
    @Override
    public ArrayListEx<CharSequence> getInfoList(@NonNull Context context, AbsCfg cfg) {
        ArrayListEx<CharSequence> list = new ArrayListEx<>();
        final DeviceGoveeHelper.Device device = devHelper.getDevice();
        if (device.isValid(devHelper)) {
            // TODO - use spannableString to highlight info.
            list.add("Device:" + device.deviceName);
            if (device.deviceExt.deviceSettings.temWarning) {
                list.add(String.format("┝─Alarm %s %s Temperature"
                        , cfg.toDegree(device.deviceExt.deviceSettings.temMin)
                        , cfg.toDegree(device.deviceExt.deviceSettings.temMax)));
            }
            if (device.deviceExt.deviceSettings.humWarning) {
                list.add(String.format("┝─Alarm %s %s Humidity"
                        , cfg.toHumPer(device.deviceExt.deviceSettings.humMin)
                        , cfg.toHumPer(device.deviceExt.deviceSettings.humMax)));
            }
            list.add(String.format("┝─Battery:%d%% Wifi:%d"
                    , device.deviceExt.deviceSettings.battery
                    , device.deviceExt.deviceSettings.wifiLevel));
            list.add(String.format("┝─Gateway v%s v%s"
                    , device.deviceExt.deviceSettings.gatewayVersionHard
                    , device.deviceExt.deviceSettings.gatewayVersionSoft));
            list.add(String.format("┝─Version v%s v%s"
                    , device.deviceExt.deviceSettings.versionHard
                    , device.deviceExt.deviceSettings.versionSoft));
            list.add(String.format("└─SKU %s"
                    , device.deviceExt.deviceSettings.sku));
        }
        return list;
    }

    @Override
    @NonNull
    public Map<String, String> getInfoMap(@NonNull Context context, @NonNull AbsCfg cfg) {

        Map<String, String> infoMap = new LinkedHashMap<>();
        final DeviceGoveeHelper.Device device = devHelper.getDevice();
        if (device.isValid(devHelper)) {
            SimpleDateFormat dateFmt = new SimpleDateFormat("MMM E h:mm a", Locale.getDefault());

            infoMap.put(
                    context.getString(R.string.status_version),
                    context.getString(R.string.status_version_value, device.versionSoft, device.versionHard));

            infoMap.put(
                    context.getString(R.string.status_battery),
                    String.format("%d%%", device.deviceExt.deviceSettings.battery)
                            + String.format(", WiFi: %d", device.deviceExt.deviceSettings.wifiLevel));
            infoMap.put(
                    context.getString(R.string.status_last),
                    dateFmt.format(new Date(device.deviceExt.lastDeviceData.lastTime)));

            infoMap.put(
                    context.getString(R.string.status_temperature),
                    context.getString(R.string.status_temp_fmt, cfg.toDegree(device.deviceExt.lastDeviceData.tem)));
            infoMap.put(
                    context.getString(R.string.status_humidity),
                    cfg.toDegree(device.deviceExt.lastDeviceData.hum));
        }
        return infoMap;
    }

    private boolean openDatabase(@NonNull Context context) {
        boolean allGood = false;
        try {
            File dbHourFile = context.getDatabasePath(name + DB_HOUR_NAME);
            dbDeviceHourly = new DbDeviceHourly(dbHourFile.getAbsolutePath());
            state.chainError(dbDeviceHourly.openWrite(true));

            File dbDailyRecordFile = context.getDatabasePath(name + DB_DAILY_RECORD_NAME);
            dbDeviceDaily = new DbDeviceDaily(dbDailyRecordFile.getAbsolutePath());
            state.chainError(dbDeviceDaily.openWrite(true));
            allGood = true;
        } catch (Exception ex) {
            ALog.e.tagMsg(this, "SQL database ", ex);
            state.chainError(ex);
        }

        return allGood;
    }

    /**
     * Populate database with Cloud data.
     */
    private void freshenDatabase(@NonNull Context context,  @NonNull DeviceGoveeHelper.Device device) {

        // Get data near 'now'
        DateTime now = DateTime.now();
        freshenDbStart = now;

        Interval interval = new Interval(now.withTimeAtStartOfDay().minusDays(60), now);    // was 2
        interval = getMissingInterval(interval, dbDeviceHourly, TimeUnit.HOURS.toMillis(1),  5);

        viewModel.setProgress(dbDeviceHourly.firstMilli, 0, 0);
        viewModel.setStatus(LOAD_DATA_MSG);
        ALog.d.tagMsg(TAG, "=== freshen === ", interval);

        DeviceGoveeHelper.IntervalSeries intervalSeries = devHelper.getDeviceData(context, viewModel, device, interval);
        if (intervalSeries != null) {
            dbDeviceHourly.beginAdd();
            DeviceGoveeHelper.avgSamplesToHourly(intervalSeries, dbDeviceHourly, dbDeviceDaily, viewModel);
            dbDeviceHourly.writeMeta();
            dbDeviceHourly.endAdd();
            viewModel.setStatus(SERIES_MSG);
            // long percent = 100 - Math.max(0, 100 * (dbDeviceHourly.firstMilli - dbDeviceHourly.startMilli) / (dbDeviceHourly.lastMilli - dbDeviceHourly.startMilli));
            long percent =  100 * (dbDeviceHourly.lastMilli - dbDeviceHourly.startMilli) / (now.getMillis() - dbDeviceHourly.startMilli);
            percent = Math.min(100, percent +5);    // round 95.. up to 100
            viewModel.setProgress(dbDeviceHourly.lastMilli, intervalSeries.samples.size(), (int) percent);

            // Back fill to get ALL cloud data.
            // DateTime endDt = new DateTime(intervalSeries.samples.first(DeviceGoveeHelper.Sample.EMPTY).milli);
            DateTime endDt = new DateTime(dbDeviceHourly.firstMilli);
            if (interval.getStart().isBefore(endDt)) {
                endDt = interval.getStart();
            }

            DateTime freshenBackTo = new DateTime(dbDeviceHourly.startMilli);
            // Debug
            // DateTime maxFreshnBackTo = now.withTimeAtStartOfDay().minusMonths(2);
            // freshenBackTo = maxFreshnBackTo.isAfter(freshenBackTo) ? maxFreshnBackTo : freshenBackTo;

    final boolean threadQueue = false;  // TODO - return back to true or better only set to true once DB populated once.
            final int MAX_THREADS = 2;
            ThreadProcessInterval[] threads;
            //2 final ArrayBlockingQueue<DeviceGoveeHelper.IntervalSeries> dataQueue;
            final ArrayBlockingQueue<Interval> dataQueue;
            if (threadQueue) {
                dataQueue = new ArrayBlockingQueue<>(MAX_THREADS * 4);
                threads = new ThreadProcessInterval[MAX_THREADS];
                for (int threadIdx = 0; threadIdx < MAX_THREADS; threadIdx++) {
                    threads[threadIdx] = new ThreadProcessInterval(threadIdx, device, dataQueue, this);
                    threads[threadIdx].start();
                }
            }

            while (endDt.getMillis() > freshenBackTo.getMillis()) {
                viewModel.setStatus(LOAD_DATA_MSG);
                interval = new Interval(endDt.minusDays(7), endDt);
                endDt = interval.getStart();
                ALog.i.tagMsg(this, "update hour range ", interval.toString());
                // intervalSeries = sensor.getDeviceData(device, interval, dbDeviceHourly.startIndex, new DateTime(dbDeviceHourly.startMilli));
                if (threadQueue) {
                    // Waits if queue is full
                    try {
                        //2 dataQueue.put(intervalSeries);
                        //2 ALog.d.tagMsg(this, "queued ", intervalSeries.interval);
                        dataQueue.put(interval);
                        ALog.d.tagMsg(this, "queued ", interval);
                    } catch (InterruptedException ex) {
                        state.chainError(ex);
                        ALog.e.tagMsg(this, " que error ", ex);
                    }
                } else {
                    processIntervalSeries(intervalSeries);
                }
            }

            if (threadQueue) {

                try {
                    while (!dataQueue.isEmpty()) {
                        ALog.d.tagMsg(this, "waiting for queue to empty ", dataQueue.size());
                        Thread.sleep(1000);  // wait for the queue to become empty
                    }
                    for (int threadIdx = 0; threadIdx < MAX_THREADS; threadIdx++) {
                        ALog.d.tagMsg(this, "interrupt thread ", threadIdx);
                        threads[threadIdx].interrupt();
                        // dataQueue.put(null); // Null is not valid
                    }
                } catch (Exception ex) {
                    ALog.e.tagMsg(this, "Got exception Draining Database queue: ", ex);
                }

                viewModel.setProgress(intervalSeries.interval.getEndMillis(), intervalSeries.samples.size(), 100);
            }
        }
        dbDeviceHourly.writeMeta();
    }

    private Interval getMissingInterval(
            Interval interval,
            DbDeviceHourly dbDeviceHourly,
            long milliStep,
            @IntRange(from=2, to=100) int minGapSize) {
        ArrayListEx<DeviceGoveeHelper.Sample> resultList = dbDeviceHourly.getRange(interval);
        if (resultList.size() < minGapSize)
            return interval;
        long tolerance = milliStep / 10;
        int gapStart = -1;
        int maxGapStart = -1;
        int maxGapLen = 0;

        for (int idx = 1; idx < resultList.size(); idx++) {
            long deltaMilli = resultList.get(idx).milli - resultList.get(idx-1).milli;
            long deltaDiff = Math.abs(milliStep - deltaMilli);
            if (deltaDiff > tolerance) {
                ALog.d.tagMsg(this, "Idx=", idx, " Gap=", deltaMilli / 1000, new DateTime(resultList.get(idx).milli).toString(" MM/dd/yyyy HH:mm"));
                if (gapStart == -1)
                    gapStart = idx-1;
            } else {
                if (gapStart != -1)  {
                    int gapLen = idx - gapStart;
                    if (gapLen >= minGapSize && gapLen > maxGapLen) {
                        maxGapLen = gapLen;
                        maxGapStart = gapStart;
                    }
                }
                gapStart = -1;
            }
        }

        if (maxGapLen != 0) {
            DateTime gapStartTm = new DateTime(resultList.get(maxGapStart).milli);
            ALog.d.tagMsg(this, "Max GapLen=", maxGapLen, " From=", maxGapStart, gapStartTm.toString(" MM/dd/yyyy HH:mm")  );
            interval = new Interval(gapStartTm, interval.getEnd());
        }
        return interval;
    }

    private void processIntervalSeries(@NonNull DeviceGoveeHelper.IntervalSeries intervalSeries) {
        dbDeviceHourly.beginAdd();
        DeviceGoveeHelper.avgSamplesToHourly(intervalSeries, dbDeviceHourly, dbDeviceDaily, viewModel);
        dbDeviceHourly.writeMeta();
        dbDeviceHourly.endAdd();
        // ALog.i.tagMsg(TAG, "hourly rows=", dbDeviceHourly.getRowCount());
        viewModel.setStatus(SERIES_MSG);
        long percent = 100 - 100 * (dbDeviceHourly.firstMilli - dbDeviceHourly.startMilli) / (dbDeviceHourly.lastMilli - dbDeviceHourly.startMilli);
        viewModel.setProgress(intervalSeries.interval.getStartMillis(), intervalSeries.samples.size(), (int) percent);

        if (intervalSeries.interval.contains(dbDeviceHourly.startMilli) && intervalSeries != null) {
            dbDeviceHourly.startMilli = dbDeviceHourly.firstMilli;
            dbDeviceHourly.startIndex = dbDeviceHourly.firstIndex;
        }
    }


    @Override
    public void start(IwxManager manager, ExecState startStatus) {
        ALog.i.tagMsg(TAG, "start ThreadCloudDbLoader state=", startStatus);
        loaderThread = new ThreadCloudDbLoader(this, manager, startStatus);
        loaderThread.start();
    }

    @Override
    public void stop(IwxManager manager) {
        ALog.i.tagMsg(this, "stop");
        loaderThread = ThreadCloudDbLoader.clear(loaderThread);
    }

    @Override
    public ExecState state() {
        return state;
    }

    // Accessed via Reflection
    @SuppressWarnings("unused")
    @WorkerThread
    public static ArrayListEx<DeviceItem> devices(Context context, @NonNull DeviceAccount account, ExecState state) {
        DeviceGoveeHelper devHelper = new DeviceGoveeHelper();
        ArrayListEx<DeviceItem> deviceItems = new ArrayListEx<>();
        ALog.d.tagMsg(TAG, "init device govee state=", state);
        // state.reset();
        // viewModel.setStatus(LOAD_START_MSG);
        if (devHelper.login(context, account)) {
            ALog.d.tagMsg(TAG, "init device govee login ok2run=", state.ok2Run());
            if (state.ok2Run() && devHelper.parseLogin()) {
                // viewModel.setStatus(LOAD_SUMMARY_MSG);
                if (state.ok2Run()
                        && devHelper.getDeviceSummary(context, account)
                        && devHelper.parseDeviceSummary(context, account)) {
                    // viewModel.setStatus(STATUS_MSG);
                } else {
                    // viewModel.setStatus(FAILED_SUMMARY_MSG);
                    ALog.e.tagMsg(TAG, "init device govee summary failed");
                }

                if (devHelper.loadSuccessful()) {
                    for (DeviceGoveeHelper.Device device : devHelper.devices) {
                        DeviceItem deviceItem = new DeviceGovee(context, device, account, devHelper);
                        WidViewList.logIt(context, deviceItem, AppCfg.getInstance(context));
                        deviceItems.add(deviceItem);
                    }
                    DeviceGoveeHelper.Device.saveTo(getPref(context), devHelper.devices);
                } else {
                    ALog.e.tagMsg(TAG, "init device govee load failed ", devHelper.loginMessage);
                }
            } else {
                ALog.e.tagMsg(TAG, "init device govee login parse failed ");
            }
        } else {
            ALog.e.tagMsg(TAG, "init device govee login failed ", devHelper.loginMessage);
            devHelper.devices = DeviceGoveeHelper.Device.loadFrom(getPref(context));
            for (DeviceGoveeHelper.Device device : devHelper.devices) {
                DeviceItem deviceItem = new DeviceGovee(context, device, account, devHelper);
                WidViewList.logIt(context, deviceItem, AppCfg.getInstance(context));
                deviceItems.add(deviceItem);
            }
        }
        // state.done();
        return deviceItems;
    }

    // =============================================================================================

    /**
     * Load weather (temperature and humidity) data from cloud.
     * Govee Sensor.
     */
    static class ThreadCloudDbLoader extends Thread implements Runnable {
        private final ExecState state;
        private final boolean doFreshen;
        private final DeviceGovee deviceGovee;
        private final IwxManager manager;
        private final WxViewModel viewModel;

        public ThreadCloudDbLoader(
                @NonNull DeviceGovee deviceGovee, @NonNull IwxManager manager, @NonNull ExecState state) {
            this.state = state;
            this.deviceGovee = deviceGovee;
            this.manager = manager;
            this.viewModel = manager.viewModel();
            this.doFreshen = extract(state, "freshen", false);
        }

        public static ThreadCloudDbLoader clear(@Nullable ThreadCloudDbLoader loaderThread) {
            if (loaderThread != null) {
                loaderThread.state.fail(null);
            }
            return null;
        }

        public ExecState state() {
            return state;
        }

        @Override
        public void run() {
            ALog.d.tagMsg(this, "run #accounts=", manager.getAccounts().size());

            for (DeviceAccount account : manager.getAccounts()) {
                viewModel.setStatus(LOAD_START_MSG);
                if (deviceGovee.devHelper.login(manager.getContext(), account)) {
                    ALog.d.tagMsg(this, "login ", account.user);
                    if (state.ok2Run() && deviceGovee.devHelper.parseLogin()) {
                        viewModel.setStatus(LOAD_SUMMARY_MSG);
                        if (state.ok2Run()
                                && deviceGovee.devHelper.getDeviceSummary(manager.getContext(), account)
                                && deviceGovee.devHelper.parseDeviceSummary(manager.getContext(), account)) {
                            viewModel.setStatus(STATUS_MSG);
                        } else {
                            viewModel.setStatus(FAILED_SUMMARY_MSG);
                        }

                        if (deviceGovee.devHelper.loadSuccessful()) {
                            for (DeviceGoveeHelper.Device device : deviceGovee.devHelper.devices) {
                                WidViewList.logIt(manager.getContext(), deviceGovee, AppCfg.getInstance(manager.getContext()));
                                if (doFreshen) {
                                    deviceGovee.freshenDatabase(manager.getContext(), device);
                                }
                            }
                        }
                    } else {
                        ALog.e.tagMsg(this, "login FAILED ok2Run=", state.ok2Run());
                        viewModel.setStatus(FAILED_LOGIN_MSG);
                        state.fail(null);
                    }
                } else {
                    ALog.e.tagMsg(this, "login FAILED ", account.user);
                    viewModel.setStatus(FAILED_LOGIN_MSG);
                    state.fail(null);
                }
            }
            ALog.d.tagMsg(this, " dev run done ");
            state.done();
        }
    }

    // =============================================================================================

    private /* static */ class ThreadProcessInterval extends Thread implements Runnable {
        public final int idx;
        public final ArrayBlockingQueue<Interval> queue;
        //2 public ArrayBlockingQueue<DeviceGoveeHelper.IntervalSeries> queue;
        public final DeviceItem deviceManager;
        public final DeviceGoveeHelper.Device device;

        ThreadProcessInterval(int idx, DeviceGoveeHelper.Device device, ArrayBlockingQueue<Interval> queue, DeviceItem deviceManager) {
            //2 ThreadProcessInterval(int idx, DeviceGoveeHelper.Device device, ArrayBlockingQueue<DeviceGoveeHelper.IntervalSeries> queue, WxManager wxManager )  {
            this.idx = idx;
            this.device = device;
            this.queue = queue;
            this.deviceManager = deviceManager;
        }

        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    //2 DeviceGoveeHelper.IntervalSeries intervalSeries = queue.take();
                    Interval interval = queue.take();
                    ALog.d.tagMsg(this, idx + " Thread processing ", interval);
                    RefPosition refPos = new RefPosition(dbDeviceHourly.startIndex, dbDeviceHourly.startMilli);
                    DeviceGoveeHelper.IntervalSeries intervalSeries =
                            devHelper.getDeviceData(viewModel, device, interval, refPos);

                    if (intervalSeries != null && intervalSeries.samples.size() != 0) {
                        ALog.d.tagMsg(this, idx + " Thread processing ", intervalSeries.interval);
                        processIntervalSeries(intervalSeries);
                    } else {
                        ALog.d.tagMsg(this, idx + " Thread empty for ", interval);
                    }
                }
            } catch (InterruptedException ex) {
                // This is normal - after Database queue is drained, threads are interrupted.
                // ALog.e.tagMsg(this, idx + " Thread process ", ex);
            }

            ALog.d.tagMsg(this, idx + " Thread DONE");
        }
    }
}
