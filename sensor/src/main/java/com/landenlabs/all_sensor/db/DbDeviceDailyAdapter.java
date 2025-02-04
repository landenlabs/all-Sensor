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

package com.landenlabs.all_sensor.db;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.sensor.AbsCfg;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.TimeZone;

/**
 * Recycler adapter to access Daily device database.
 */
public class DbDeviceDailyAdapter
        extends RecyclerView.Adapter<DbDeviceDailyAdapter.DailyHolder>
        implements IAdapterHighlight {

    private final Context mContext;
    private final Cursor mCursor;
    private final AbsCfg cfg;
    private final DateTimeFormatter yearHdrFmt = DateTimeFormat.forPattern("YYYY");
    private final DateTimeFormatter dateHdrFmt = DateTimeFormat.forPattern("MMM d E");
    private final DateTimeFormatter dateRowFmt = DateTimeFormat.forPattern("hh:mm a");
    private final DateTimeZone zone;

    // ---------------------------------------------------------------------------------------------
    public DbDeviceDailyAdapter(Context context, Cursor cursor, AbsCfg cfg) {
        mContext = context;
        mCursor = cursor;
        this.cfg = cfg;

        TimeZone localZone = TimeZone.getDefault();
        zone = DateTimeZone.forTimeZone(localZone);
    }

    @NonNull
    @Override
    public DailyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.device_daily_row, parent, false);
        return new DailyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DailyHolder dailyHolder, int position) {

        // Position = [0=header] 1...n [n+1=footer]
        if (!mCursor.moveToPosition(position)) {
            return;
        }

        long milli = mCursor.getLong(mCursor.getColumnIndex(DbDeviceDaily.COL00_DAY));
        int tempC100Min = mCursor.getInt(mCursor.getColumnIndex(DbDeviceDaily.COL01_TEMPC100_MIN));
        long tempMinMilli = mCursor.getLong(mCursor.getColumnIndex(DbDeviceDaily.COL02_TEMP_MIN_MILLI));
        int tempC100Max = mCursor.getInt(mCursor.getColumnIndex(DbDeviceDaily.COL03_TEMPC100_MAX));
        long tempMaxMilli = mCursor.getLong(mCursor.getColumnIndex(DbDeviceDaily.COL04_TEMP_MAX_MILLI));
        int humP100Min = mCursor.getInt(mCursor.getColumnIndex(DbDeviceDaily.COL05_HUM100_MIN));
        long humMinMilli = mCursor.getLong(mCursor.getColumnIndex(DbDeviceDaily.COL06_HUM_MIN_MILLI));
        int humP100Max = mCursor.getInt(mCursor.getColumnIndex(DbDeviceDaily.COL07_HUM100_MAX));
        long humMaxMilli = mCursor.getLong(mCursor.getColumnIndex(DbDeviceDaily.COL08_HUM_MAX_MILLI));
        DateTime itemTime = new DateTime(milli);

        DateTime minTDt = new DateTime(tempMinMilli);
        DateTime maxTDt = new DateTime(tempMaxMilli);
        DateTime minHDt = new DateTime(humMinMilli);
        DateTime maxHDt = new DateTime(humMaxMilli);

        if (false) {
            int refDay = minTDt.getDayOfYear();
            boolean error = refDay != maxTDt.getDayOfYear()
                    || refDay != minHDt.getDayOfYear()
                    || refDay != maxHDt.getDayOfYear();
            error |= tempC100Min > tempC100Max;
            error |= humP100Min > humP100Max;

            ALog.d.tagFmt(this, "get RecXXX %s %s %s %s %4d %4d %4d %4d %s"
                    , minTDt.toString("yy-M-dd HH")
                    , maxTDt.toString("yy-M-dd HH")
                    , minHDt.toString("yy-M-dd HH")
                    , maxHDt.toString("yy-M-dd HH")
                    , tempC100Min
                    , tempC100Max
                    , humP100Min
                    , humP100Max
                    , error ? "**ERROR**" : "");
        }

        ALog.d.tagMsg(this, "Row ", position, " Time=", itemTime.toDateTime(DateTimeZone.UTC). toString("YYYY-MM-dd hh:mm Z"));

        @ColorInt int color = ((itemTime.getDayOfYear() & 1) == 1) ? 0xffe0ffe0 : 0xffffe0e0;
        dailyHolder.itemView.setBackgroundColor(color);
        dailyHolder.itemView.setTag(R.id.table_row_color, color);
        dailyHolder.row_numTv.setText(String.format("%,d", position));
        dailyHolder.row_dayTv.setText(dateHdrFmt.print(milli));
        dailyHolder.row_temp_min_dateTv.setText(dateRowFmt.print(minTDt));
        dailyHolder.row_temp_minTv.setText(String.format("%,.2f", cfg.toDegreeN(tempC100Min)));
        dailyHolder.row_temp_max_dateTv.setText(dateRowFmt.print(maxTDt));
        dailyHolder.row_temp_maxTv.setText(String.format("%,.2f", cfg.toDegreeN(tempC100Max)));
        dailyHolder.row_hum_min_dateTv.setText(dateRowFmt.print(minHDt));
        dailyHolder.row_hum_minTv.setText(String.format("%,.2f", cfg.toHumPerN(humP100Min)));
        dailyHolder.row_hum_max_dateTv.setText(dateRowFmt.print(maxHDt));
        dailyHolder.row_hum_maxTv.setText(String.format("%,.2f", cfg.toHumPerN(humP100Max)));

        /*
        dailyHolder.bot_temp_min_dateTv.setText(dateHdrFmt.print(minTDt));
        dailyHolder.bot_temp_max_dateTv.setText(dateHdrFmt.print(maxTDt));
        dailyHolder.bot_hum_min_dateTv.setText(dateHdrFmt.print(minHDt));
        dailyHolder.bot_hum_max_dateTv.setText(dateHdrFmt.print(maxHDt));
         */

        boolean newYear = false;
        if (mCursor.moveToPosition(position - 1)) {
            long prevMilli = mCursor.getLong(mCursor.getColumnIndex(DbDeviceDaily.COL00_DAY));
            DateTime prevTime = new DateTime(prevMilli).withZone(zone);
            newYear = prevTime.getYear() != itemTime.getYear();
        }

        if (newYear || position == 0) {
            dailyHolder.hdr_rowLL.setVisibility(View.VISIBLE);
            dailyHolder.hdr_dayTv.setText(yearHdrFmt.print(itemTime));
        } else {
            dailyHolder.hdr_rowLL.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public void setHighlight(boolean highlight, @Nullable View view) {
        if (view != null) {
            if (highlight) {
                view.setBackgroundColor(Color.YELLOW);
                // View v = view.findViewById(R.id.bot_daily);
                // UiUtils.ifSetVisibility(View.VISIBLE, v);
            } else {
                view.setBackgroundColor((Integer) view.getTag((R.id.table_row_color)));
                // View v = view.findViewById(R.id.bot_daily);
                // UiUtils.ifSetVisibility(View.GONE, v);
            }
        }
    }

    /*
    public void swapCursor(Cursor newCursor) {
        if (mCursor != null) {
            mCursor.close();
        }

        mCursor = newCursor;

        if (newCursor != null) {
            notifyDataSetChanged();
        }
    }
     */

    // =============================================================================================
    public static class DailyHolder extends RecyclerView.ViewHolder {

        public final ViewGroup hdr_rowLL;
        public final TextView hdr_dayTv;

        public final TextView row_numTv;
        public final TextView row_dayTv;
        public final TextView row_temp_min_dateTv;
        public final TextView row_temp_minTv;
        public final TextView row_temp_max_dateTv;
        public final TextView row_temp_maxTv;
        public final TextView row_hum_min_dateTv;
        public final TextView row_hum_minTv;
        public final TextView row_hum_max_dateTv;
        public final TextView row_hum_maxTv;

        /*
        public final TextView bot_numTv;
        public final TextView bot_dayTv;
        public final TextView bot_temp_min_dateTv;
        public final TextView bot_temp_minTv;
        public final TextView bot_temp_max_dateTv;
        public final TextView bot_temp_maxTv;
        public final TextView bot_hum_min_dateTv;
        public final TextView bot_hum_minTv;
        public final TextView bot_hum_max_dateTv;
        public final TextView bot_hum_maxTv;
         */

        public DailyHolder(View itemView) {
            super(itemView);
            hdr_rowLL = itemView.findViewById(R.id.hdr_daily);
            hdr_dayTv = itemView.findViewById(R.id.hdr_daily_day);

            row_numTv = itemView.findViewById(R.id.row_daily_num);
            row_dayTv = itemView.findViewById(R.id.row_daily_day);
            row_temp_min_dateTv = itemView.findViewById(R.id.row_daily_temp_min_date);
            row_temp_minTv = itemView.findViewById(R.id.row_daily_temp_min_temp);
            row_temp_max_dateTv = itemView.findViewById(R.id.row_daily_temp_max_date);
            row_temp_maxTv = itemView.findViewById(R.id.row_daily_temp_max_temp);
            row_hum_min_dateTv = itemView.findViewById(R.id.row_daily_hum_min_date);
            row_hum_minTv = itemView.findViewById(R.id.row_daily_hum_min_hum);
            row_hum_max_dateTv = itemView.findViewById(R.id.row_daily_hum_max_date);
            row_hum_maxTv = itemView.findViewById(R.id.row_daily_hum_max_hum);

            /*
            bot_numTv = itemView.findViewById(R.id.bot_daily_num);
            bot_dayTv = itemView.findViewById(R.id.bot_daily_day);
            bot_temp_min_dateTv = itemView.findViewById(R.id.bot_daily_temp_min_date);
            bot_temp_minTv = itemView.findViewById(R.id.bot_daily_temp_min_temp);
            bot_temp_max_dateTv = itemView.findViewById(R.id.bot_daily_temp_max_date);
            bot_temp_maxTv = itemView.findViewById(R.id.bot_daily_temp_max_temp);
            bot_hum_min_dateTv = itemView.findViewById(R.id.bot_daily_hum_min_date);
            bot_hum_minTv = itemView.findViewById(R.id.bot_daily_hum_min_hum);
            bot_hum_max_dateTv = itemView.findViewById(R.id.bot_daily_hum_max_date);
            bot_hum_maxTv = itemView.findViewById(R.id.bot_daily_hum_max_hum);
             */
        }
    }
}