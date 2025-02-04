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


import static android.R.id.text1;
import static com.landenlabs.all_sensor.utils.SpanUtil.SSBlue;
import static com.landenlabs.all_sensor.utils.SpanUtil.SSJoin;
import static com.landenlabs.all_sensor.utils.SpanUtil.SSRed;
import static com.landenlabs.all_sensor.widget.WidViewList.getStringList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.SpannableString;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import androidx.annotation.ColorInt;

import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.utils.ArrayListEx;

import java.util.List;

/**
 * https://android.googlesource.com/platform/development/+/master/samples/WeatherListWidget/src/com/example/android/weatherlistwidget/WeatherWidgetService.java
 */
public class WidDataProvider implements RemoteViewsService.RemoteViewsFactory {

    // private static final String TAG = "WidDataProvider";

    public static final String PREF_DATA_MILLI = "dataMilli";
    public static final String PREF_DATA_LIST_KEY = "dataList1";
    public static final String PREF_INFO_LIST_KEY = "infoList";
    public static final String PREF_CMD_KEY = "cmd";
    public static final String PREF_DATA_TEMP = "dataTemp";
    public static final String PREF_DATA_HUM = "dataHum";
    public static final String PREF_DATA_VALUE = "dataValue";
    public static final String PREF_DATA_SENSOR_NAME = "dataDevName";

    public static final String CMD_INFO = "info";
    public static final String CMD_DATA = "data";
    public static final String CMD_MORE = "more";


    private final List<CharSequence> listView = new ArrayListEx<>();

    private final Context context;
    private final WidCfg widCfg;
    private final WidHistory widHistory;
    @ColorInt
    private final int tempRed, humidtyBlue;

    // ---------------------------------------------------------------------------------------------

    public WidDataProvider(Context context, Intent intent) {
        this.context = context;
        widCfg = new WidCfg(WidViewList2.class);  // TODO - assumes WidViewList2
        widCfg.restore(context, intent);
        ALog.d.tagMsg(this, "history provider widCfg=", widCfg);
        widHistory = new WidHistory(WidHistory.FIELDS_TH);
        tempRed = context.getColor(R.color.temRed);
        humidtyBlue = context.getColor(R.color.humBlue);
    }

    @Override
    public void onCreate() {
        initData();
    }

    // Called by notifyAppWidgetViewDataChanged()
    @Override
    public void onDataSetChanged() {
        initData();
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public int getCount() {
        return listView.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.widget_list_row1);
        if (position >= 0 && position < listView.size()) {
            view.setTextViewText(text1, listView.get(position));
        } else {
            view.setTextViewText(text1, "missing data");
        }
        return view;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private void initData() {
        SharedPreferences sharedPref = WidViewList.getPref(context);
        ArrayListEx<CharSequence> upListSet;
        String cmd = sharedPref.getString(PREF_CMD_KEY, CMD_DATA);
        if (CMD_INFO.equals(cmd)) {
            upListSet = getStringList(sharedPref, PREF_INFO_LIST_KEY);
        } else {
            upListSet = getHistoryList();
        }

        listView.clear();
        listView.addAll(upListSet);
    }

    private  ArrayListEx<CharSequence> getHistoryList() {
        ArrayListEx<CharSequence> list = new ArrayListEx<>();
        widHistory.load(context, widCfg.deviceName);
        for (WidHistory.WidHistoryItem item : widHistory.list) {
            SpannableString ssDate = widCfg.dateFmt(item.milli);
            String temp = widCfg.toDegree(item.values[0]);
            String hum = widCfg.toHumPer(item.values[1]);
            /*
            SpannableString span = new SpannableString(String.format("%s %s %s ", date, temp, hum));
            int start = date.length()+1;
            span.setSpan(new ForegroundColorSpan(tempRed), start, start + temp.length(), 0);
            start += temp.length()+1;
            span.setSpan(new ForegroundColorSpan(humidtyBlue), start, start + hum.length(), 0);
            */
            CharSequence span = SSJoin(ssDate, " ", SSRed(temp), " ", SSBlue(hum));
            list.add(span);
        }
        ALog.d.tagMsg(this, "getHistory ", list);
        return list;
    }
}



