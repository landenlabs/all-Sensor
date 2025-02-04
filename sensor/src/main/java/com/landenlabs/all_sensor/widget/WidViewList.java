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


import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.sensor.DeviceGoveeHelper;
import com.landenlabs.all_sensor.sensor.DeviceItem;
import com.landenlabs.all_sensor.sensor.DeviceListManager;
import com.landenlabs.all_sensor.sensor.DeviceSummary;
import com.landenlabs.all_sensor.utils.ArrayListEx;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Minutes;

import java.util.Date;

//
// https://github.com/JessicaThornsby/CollectionWidget
// https://developer.android.com/guide/topics/appwidgets/index.html#Pinning
//
// https://www.zoftino.com/android-widget-example
//
// https://www.sitepoint.com/killer-way-to-show-a-list-of-items-in-android-collection-widget/
//
public abstract class WidViewList extends WidView {

    private static final String TAG = "WidViewList";

    protected WidViewList(Class<? extends WidView> widClass) {
        super(widClass);
    }

    // ---------------------------------------------------------------------------------------------

    void updateAppWidget(
            @NonNull Context context,
            @Nullable AppWidgetManager appWidgetManager,
            int widgetId) {

        /*
        if (appWidgetManager != null) {
            AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(appWidgetId);
        }
        */

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_list1);

        ALog.d.tagMsg(this, "DL99 RemoteView=", views);

        widCfg.restore(context, widgetId);
        ALog.d.tagMsg(this, "DL99 restore done");
        setVisibility(context, views);  // Call before using widthPx or heightPx

        // See AndroidManifest.xml to enable "Service"
        // TODO - move data loading into service.
        views.setRemoteAdapter(R.id.widget_list, addExtra(new Intent(context, WidgetService.class), widCfg));

        // VectorDrawable must be set programmatically
        views.setImageViewResource(R.id.wid_more, R.drawable.scr_more);

        Date now = new Date();
        views.setTextViewText(R.id.wid_date, widCfg.dateFmt(now.getTime()) + "?");
        views.setTextViewText(R.id.wid_temp_lbl, widCfg.tempLbl());

        // WxManager wxManager = WxManager.getInstance(context);
        try {

            // ExecState state =  wxManager.start(context, false);
            // ALog.d.tagMsg(this, " Waiting for data state=", state.complete);
            // state.complete.thenAccept( r -> {

            DeviceItem deviceItem = DeviceListManager.get(widCfg.deviceName);
            String devName = (deviceItem != null) ? deviceItem.name : "NoName";
            // views.setTextViewText(R.id.wid_title, String.format("(%d)%s", widgetId, deviceItem.name));
            views.setTextViewText(R.id.wid_title, devName);

            if (deviceItem != null) {
                DeviceSummary summary = deviceItem.getSummary(context, widCfg);
                SpannableString strDate = widCfg.dateFmt(summary.numTime);
                views.setTextViewText(R.id.wid_date, strDate);
                views.setTextViewText(R.id.wid_temp_val, summary.strTemp);
                views.setTextViewText(R.id.wid_hum_val, summary.strHum);

                ALog.d.tagMsg(this, "Update(", widgetId,
                        ") Temp=", summary.strTemp,
                        " hum=", summary.strHum,
                        " time=", summary.strDate);

                if (widCfg.showTrend) {
                    DateTime nowDt = DateTime.now();
                    try {
                        Interval interval = new Interval(nowDt.minusHours(4), nowDt.minusHours(1));
                        ArrayListEx<DeviceGoveeHelper.Sample> samples =
                                DeviceListManager.getHourlyList(devName, interval);
                        if (samples.size() > 0) {
                            setTrendVisibility(views, true);
                            DeviceGoveeHelper.Sample sample = samples.last(DeviceGoveeHelper.Sample.EMPTY);

                            int tem = summary.numTemp;
                            int hum = summary.numHum;
                            DateTime lastTime = new DateTime(summary.numTime.longValue());

                            DateTime sampelDt = new DateTime(sample.milli);
                            int deltaTem = tem - sample.tem;
                            int deltaHum = hum - sample.hum;
                            float hours = Minutes.minutesBetween(sampelDt, lastTime).getMinutes() / 60.0f;
                            deltaTem = Math.round(deltaTem / hours);
                            deltaHum = Math.round(deltaHum / hours);

                            views.setImageViewResource(R.id.wid_temp_trend_img,
                                    (deltaTem >= 0) ? R.drawable.scr_arrow_up : R.drawable.scr_arrow_dn);
                            views.setImageViewResource(R.id.wid_hum_trend_img,
                                    (deltaHum >= 0) ? R.drawable.scr_arrow_up : R.drawable.scr_arrow_dn);
                            String INTERVAL_UNIT = widCfg.intervalUnit();
                            views.setTextViewText(R.id.wid_temp_trend_val,
                                    widCfg.toDeltaDegree(deltaTem) + INTERVAL_UNIT);
                            views.setTextViewText(R.id.wid_hum_trend_val,
                                    widCfg.toHumPer(deltaHum) + INTERVAL_UNIT);
                            if (hours < 0.5f || hours > 4.0f) {
                                ALog.e.tagFmt(this, "trend - bad interval hours=%.2f", hours);
                                setTrendVisibility(views, false);
                            }
                        } else {
                            ALog.e.tagMsg(this, "trend - no samples for ", interval);
                            setTrendVisibility(views, false);
                        }
                    } catch (Exception ex) {
                        ALog.e.tagMsg(this, "trend", ex);
                    }
                }

                logIt(context, deviceItem, widCfg);
            }
            // });
        } catch (Exception ex) {
            views.setTextViewText(R.id.wid_title, ex.getMessage());
            ALog.e.tagMsg(this, "List widget", ex);
        }

        setupClickAction(context, views, widCfg);
        if (appWidgetManager != null) {
            appWidgetManager.updateAppWidget(widgetId, views);
        }
    }
}

