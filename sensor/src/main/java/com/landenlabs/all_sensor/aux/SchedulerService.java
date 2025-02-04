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

package com.landenlabs.all_sensor.aux;

import static com.landenlabs.all_sensor.MainActivity.addAndLog;
import static com.landenlabs.all_sensor.aux.SysInfo.getBatteryInfo;
import static com.landenlabs.all_sensor.sensor.WxViewModel.STATUS_MSG;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.sensor.WxManager;
import com.landenlabs.all_sensor.utils.NetInfo;
import com.landenlabs.all_sensor.widget.WidCfg;
import com.landenlabs.all_sensor.widget.WidView;
import com.landenlabs.all_sensor.widget.WidViewList1;
import com.landenlabs.all_sensor.widget.WidViewList2;
import com.landenlabs.all_sensor.widget.WidViewPage;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class SchedulerService extends Worker {
    // private static final String TAG = "SchedulerService";

    public static final String INTENT_EXTRA_ACTION = "XAction";
    public static final String ACTION_SCHED_FIRED = "ScheduleFired";
    static final String INTENT_NAME = "LocalReceiver";

    // ---------------------------------------------------------------------------------------------

    //                                      Green       Orange      Red
    @ColorInt
    final int[] darkColors = new int[]{0xff309020, 0xffd08030, 0xffff4040};

    public SchedulerService(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        darkColors[0] = context.getColor(R.color.wifi0);
        darkColors[1] = context.getColor(R.color.wifi1);
        darkColors[2] = context.getColor(R.color.wifi2);
        doWork();
    }

    @NonNull
    @Override
    public Result doWork() {
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        ALog.d.tagMsg(this, currentDateTimeString);

        Context appContext = getApplicationContext();

        NetInfo.NetStatus netStatus = new NetInfo.NetStatus(appContext);
        addAndLog(appContext, null, NetInfo.getStatus(appContext, netStatus, "", darkColors)); // getNetworkInfo(context),
        addAndLog(appContext, null, (Object[])getBatteryInfo(appContext));

        updateWidgets(appContext);
        updateNotification(appContext);

        // TODO - if update work fails, return either
        //    Result.failure
        //    Result.retry
        return Result.success();
    }

    private void updateNotification(Context appContext) {
        WxManager wxManager = WxManager.getInstance(appContext);
        wxManager.viewModel.setStatus(STATUS_MSG);
    }

    static final Class<? extends WidView>[] WID_CLASSES = new Class[]{WidViewPage.class, WidViewList1.class, WidViewList2.class, WidView.class };
    private static void updateWidgets(Context appContext) {
        AppWidgetManager appWidMgr = AppWidgetManager.getInstance(appContext);

        Set<Integer> appWidIds = new HashSet<>();
        for (Class<? extends WidView> clazz : WID_CLASSES){
            int[] appWidgetIds = appWidMgr.getAppWidgetIds(new ComponentName(appContext, clazz));
            for (int id : appWidgetIds)
                appWidIds.add(id);
        }

        final String TAG = "SchUpdWid";
        ALog.d.tagMsg(TAG,"wid ids", appWidIds);

        if (appWidIds.size() > 0) {
            int[] appWidgetIds = toArray(appWidIds);

            for (int id : appWidIds) {
                WidCfg widCfg = new WidCfg(WidViewPage.class); // any class is overridden during restored on next line.
                widCfg.restore(appContext, id);

                // Include the widget ID to be updated as an intent extra.
                // Intent intentUpdate = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                Intent intentUpdate = new Intent(appContext, widCfg.widClass);
                intentUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widCfg.id);
                intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                intentUpdate.putExtra(WidCfg.DEVICE_NAME, widCfg.deviceName);
                intentUpdate.putExtra(WidCfg.TAG, widCfg.getArray());

                // ALog.d.tagMsg(this, "force update widget=", widCfg.getWidgetClass());
                ALog.d.tagMsg(TAG,"send widUpd for id=", widCfg.id);
                appContext.sendBroadcast(intentUpdate);
            }
        }
    }

    private static int[] toArray(Collection<Integer> integers)  {
        int[] ret = new int[integers.size()];
        int i = 0;
        for (int inum : integers)
            ret[i++] = inum;
        return ret;
    }
}