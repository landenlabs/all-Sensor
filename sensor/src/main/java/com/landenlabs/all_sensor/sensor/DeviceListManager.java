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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.landenlabs.all_sensor.db.DbDeviceDaily;
import com.landenlabs.all_sensor.db.DbDeviceHourly;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.utils.ArrayListEx;

import org.joda.time.Interval;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Manage collection of Device Items.
 */
public class DeviceListManager {
    private static final String TAG = DeviceListManager.class.getSimpleName();

    static final Class<? extends DeviceItem>[] DEVICE_CLASSES = new Class[]{DeviceGovee.class};

    private static Map<String, DeviceItem> DEVICES_MAP;
    private static final ArrayListEx<DeviceItem> DEVICES_ARRAY = new ArrayListEx<>();

    private static final Object LOCKER = new Object();
    private static WeakReference<ThreadInit> refThreadInit = new WeakReference<>(null);

    public static final String DEFAULT_DEVICE = "";

    // ---------------------------------------------------------------------------------------------

    @Nullable
    public static DbDeviceHourly getDbHourly(@NonNull String devName) {
        return get(devName).dbDeviceHourly;
    }

    public static DbDeviceDaily getDbDaily(String devName) {
        return get(devName).dbDeviceDaily;
    }

    @NonNull
    public static DeviceItem get(String devName) {
        synchronized (DEVICES_ARRAY) {
            return DEFAULT_DEVICE.equals(devName)
                    ? DEVICES_ARRAY.first(DeviceGovee.EMPTY)
                    : DEVICES_MAP.get(devName);
        }
    }

    @NonNull
    public static ArrayListEx<DeviceGoveeHelper.Sample> getHourlyList(String devName, Interval interval) {
        return getDbHourly(devName).getRange(interval);
    }

    @NonNull
    public static ArrayListEx<DeviceGoveeHelper.Record> getDailyList(String devName, Interval interval) {
        return getDbDaily(devName).getRange(interval);
    }

    public static DeviceState init(IwxManager manager, Map<String, DeviceItem> devices) {
        synchronized (LOCKER) {
            ThreadInit threadInit = refThreadInit.get();
            ALog.d.tagMsg(TAG, "init devices=", DEVICES_ARRAY.size(), " thread=", threadInit);
            devices.clear();
            DEVICES_MAP = devices;

            if (threadInit != null) {
                if (threadInit.state.isActive()) {
                    return threadInit.state;
                }
                threadInit.state.fail(null);
                threadInit.interrupt();
            }
            threadInit = new ThreadInit(manager.getContext(), manager);
            refThreadInit = new WeakReference<>(threadInit);
            return threadInit.init();
        }
    }

    public static ArrayListEx<DeviceItem> getDevices() {
        synchronized (DEVICES_ARRAY) {
            // return new ArrayListEx<>(DEVICES_ARRAY);
            return DEVICES_ARRAY;
        }
    }

    /*
    public void start(IwxManager manager) {
        for (DeviceItem item : DEVICES_ARRAY) {
            item.start(manager);
        }
    }
    public void stop(IwxManager manager) {
        for (DeviceItem item : DEVICES_ARRAY) {
            item.stop(manager);
        }
    }

    @Override
    public ExecState state() {
        return null;
    }

     */

    // =============================================================================================
    static class ThreadInit extends Thread {
        private final Map<String, DeviceItem> devices_map = new HashMap<>();
        private final ArrayListEx<DeviceItem> devices_array = new ArrayListEx<>();
        private final DeviceState state = new DeviceState();
        private final Context context;
        private final IwxManager manager;

        ThreadInit(Context context, IwxManager manager) {
            this.context = context;
            this.manager = manager;
        }

        DeviceState init() {
            super.start();
            return state;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            ALog.d.tagMsg(TAG, "init devices running state=", state);
            ALog.d.tagMsg(TAG, "init devices running devices=", DEVICE_CLASSES);

            for (Class<? extends DeviceItem> deviceClass : DEVICE_CLASSES) {
                if (state.ok2Run()) {
                    try {
                        // ExecState state
                        Method method = deviceClass.getMethod("devices",
                                Context.class, DeviceAccount.class, ExecState.class);
                        Exception ex = null;
                        for (DeviceAccount account : manager.getAccounts()) {
                            ArrayListEx<DeviceItem> devList =
                                    (ArrayListEx<DeviceItem>) method.invoke(null, context, account, state);
                            ALog.d.tagMsg(TAG, "init devices list ", devList);
                            if (devList != null) {
                                for (DeviceItem item : devList) {
                                    if (state.ok2Run()) {
                                        if (item.isValid()) {
                                            if (item.init(context, manager, state)) {
                                                devices_map.put(item.name, item);
                                                devices_array.add(item);
                                            }
                                        } else {
                                            ex = ALog.chainError(ex, item.getException());
                                        }
                                    }
                                }
                            }
                        }
                        synchronized (DEVICES_ARRAY) {
                            DEVICES_MAP.clear();
                            DEVICES_MAP.putAll(devices_map);
                            DEVICES_ARRAY.clear();
                            DEVICES_ARRAY.addAll(devices_array);
                        }
                        ALog.i.tagMsg(TAG, "init devices done ", DEVICES_ARRAY.size());
                        DeviceGovee.EMPTY.setException(ex);
                        if (ex == null) {
                            state.done();
                        } else {
                            state.fail(ex);
                        }
                    } catch (Exception ex) {
                        ALog.e.tagMsg(TAG, "########## init devices failed ", ex);
                        state.fail(ex);
                    }
                } else {
                    ALog.e.tagMsg(TAG, "##########  init devices cancelled");
                }
            }
        }
    }
}
