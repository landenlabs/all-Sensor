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

import static com.landenlabs.all_sensor.sensor.ExecState.combine;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.landenlabs.all_sensor.Units;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.utils.ArrayListEx;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Seconds;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Weather data/sensor manager.
 */
public class WxManager implements IwxManager {

    private static WxManager instance;

    public DateTime initStartDt;
    public final ExecState wxState = new ExecState("wxM-state");
    private DeviceState deviceState;
    private final ExecState sensorState = new ExecState("wxM-sensorState");
    public final WxViewModel viewModel;

    // List of built-in sensors and remote devices.
    // See SensorListManager and DeviceListManager for data access.
    // These lists are just used to initialize the data Managers.

    // Built-in sensors
    private final Map<String /* name */, SensorItem> internalSensors = new HashMap<>();
    // Remote devices
    private final Map<String /* name */, DeviceItem> externalDevices = new HashMap<>();

    private final WeakReference<Context> refContext;


    // ---------------------------------------------------------------------------------------------

    @NonNull
    synchronized
    public static WxManager getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new WxManager(context, new WxViewModel());
        }
        return instance;
    }

    // Used in unit test
    public WxManager(@NonNull Context context, @NonNull WxViewModel viewModel) {
        this.refContext = new WeakReference<>(context);
        this.viewModel = viewModel;
        init();
    }

    // Used by unit test
    public WxManager(@NonNull File ignore, @NonNull WxViewModel viewModel) {
        this.refContext = new WeakReference<>(null);
        this.viewModel = viewModel;
        init();
    }

    private ExecState init() {
        initAccounts();
        if (externalDevices.isEmpty()) {
            deviceState = DeviceListManager.init(this, externalDevices);
        }

        ALog.i.tagMsg(this, "sensor started list size=", internalSensors.size());
        if (internalSensors.isEmpty()) {
            SensorListManager.init(this, internalSensors);
        }
        return deviceState;
    }

    public ExecState start(Context ignore, boolean doFreshen) {
        ALog.i.tagMsg(this, "------- start freshen=", doFreshen);
        ALog.i.tagMsg(this, "state=", wxState);
        ALog.i.tagMsg(this, "deviceState=", deviceState);
        ALog.i.tagMsg(this,  "sensorStat=", sensorState);

        if (wxState.isActive() && initStartDt != null) {
            int stateElapsedSeconds = Seconds.secondsBetween(initStartDt, DateTime.now()).getSeconds();
            ALog.i.tagMsg(this, "------- start active seconds=", stateElapsedSeconds);
            if (stateElapsedSeconds > 10) {
                ALog.i.tagMsg(this, "------- start TIMED OUT - force complete");

                /* Changed 12-Mar-2022
                wxState.reset();
                deviceState.reset();
                sensorState.reset();
                 */
                deviceState.complete.complete("reset");
                sensorState.complete.complete("reset");
            } else {
                return wxState;
            }
        } else {
            wxState.reset();
        }

        initStartDt = DateTime.now();
        ALog.d.tagMsg(this, "Devices=", externalDevices.size(), " deviceState=", deviceState);
        if (externalDevices.isEmpty() && !deviceState.isActive()) {
            init();
        }
        if (!externalDevices.isEmpty() && !deviceState.isActive()) {
            ALog.d.tagMsg(this, "Force DevState done");
            deviceState.done();
        }

        Thread initThread = new Thread("WxMgrInit") {
            @Override
            public void run() {
                threadInit(ignore, doFreshen);
            }
        };
        initThread.start();

        // https://www.callicoder.com/java-8-completablefuture-tutorial/
        // https://levelup.gitconnected.com/completablefuture-a-new-era-of-asynchronous-programming-86c2fe23e246
        // http://blog.tremblay.pro/2017/08/supply-async.html
        // state.complete = deviceState.complete.thenCombine(sensorStatus.complete,
        //         (a,b) -> { return combine(a,b);});

        ALog.d.tagMsg(this, "join deviceState=", deviceState, " sensorState=", sensorState , " wxState=", wxState);
        deviceState.complete.thenCombine(sensorState.complete,
                (a, b) -> {
                    String result = combine(a, b);
                    wxState.complete.complete(result);
                    ALog.d.tagMsg(this, "State completed DONE");
                    return wxState.complete.join();
                }
        ).exceptionally(e -> {
            ALog.e.tagMsg(this, "State failed ", e);
            wxState.fail(e);
            return ExecState.FAIL;
        });
        return wxState;
    }


    @WorkerThread
    private void threadInit(Context ignore, boolean doFreshen) {
        // Assume at least one device will be added.
        deviceState.complete.thenApply(xx -> {
            ALog.d.tagMsg(this, "___device init ", xx, " get summary");
            deviceState.reset();
            deviceState.freshen = doFreshen;
            for (DeviceItem item : externalDevices.values()) {
                if (wxState.ok2Run()) {
                    item.start(this, deviceState);
                }
            }
            if (externalDevices.isEmpty())
                deviceState.done();

            return deviceState;
        }).exceptionally(e -> {
            ALog.d.tagMsg(this, "___device init failed ", e);
            deviceState.fail(e);
            deviceState.reset();
            return deviceState;
        });

        ALog.d.tagMsg(this, "___sensor started");
        sensorState.reset();
        if (internalSensors.isEmpty()) {
            ALog.d.tagMsg(this, "sensor list is empty");
            sensorState.done();
        } else {
            for (SensorItem item : internalSensors.values()) {
                if (wxState.ok2Run()) {
                    item.start(this, sensorState);
                    ALog.d.tagMsg(this, "sensor STARTED ", item.name);
                } else {
                    ALog.w.tagMsg(this, "sensor NOT started ", item.name);
                }
            }
            ALog.d.tagMsg(this, "sensor started DONE #sensors=", internalSensors.size());
            // sensorStatus.done();
        }
    }

    public void stop() {
        ALog.i.tagMsg(this, "stop");
        deviceState.cancelIfActive();
        for (DeviceItem item : externalDevices.values()) {
            item.stop(this);
        }
        for (SensorItem item : internalSensors.values()) {
            item.stop(this);
        }
    }

    public boolean hasSensor(@NonNull String sensorName) {
        return internalSensors.containsKey(sensorName);
    }

    // ---------------------------------------------------------------------------------------------
    // IwxManager

    @NonNull
    public Context getContext() {
        return refContext.get();
    }

    @NonNull
    @Override
    public WxViewModel viewModel() {
        return this.viewModel;
    }

    @Override
    public void update(@NonNull DeviceItem deviceItem, ExecState state) {
        scanStatus();
    }

    @Override
    public void update(@NonNull SensorItem sensorItem, ExecState state) {
        scanStatus();
    }

    @Override
    public ArrayListEx<DeviceAccount> getAccounts() {
        return DeviceAccount.getAccounts(getContext());
    }

    @Override
    public DeviceState getDeviceState() {
        return deviceState;
    }

    // ---------------------------------------------------------------------------------------------

    private void initAccounts() {
    }

    void scanStatus() {
        Boolean devDone = scan(externalDevices.values());
        Boolean sensorDone = scan(internalSensors.values());
        if (devDone != null && sensorDone != null) {
            if (devDone && sensorDone)
                wxState.done();
            else
                wxState.fail(null);
        }
    }

    Boolean scan(Collection<? extends ExecState.Exec> items) {
        int doneCnt = 0;
        int failCnt = 0;
        boolean result = true;
        for (ExecState.Exec item : items) {
            ExecState itemStatus = item.state();
            String done = itemStatus.getNow("");

            switch (done) {
                case ExecState.DONE:
                    doneCnt++;
                    break;
                case ExecState.FAIL:
                    result = false;
                    failCnt++;
                    wxState.chainError(itemStatus.exception);
                    break;
            }
        }
        return ((doneCnt + failCnt) == items.size()) ? result : null;
    }

    // ---------------------------------------------------------------------------------------------
    // Utility functions

    public static Interval getInterval(Units.Duration units, int duration) {
        duration = Math.max(1, duration);
        DateTime startDay = DateTime.now().withTimeAtStartOfDay();
        switch (units) {
            case Days:
                startDay = startDay.minusDays(duration);
                break;
            case Months:
                startDay = startDay.minusMonths(duration);
                break;
        }

        return new Interval(startDay, DateTime.now());
    }
}
