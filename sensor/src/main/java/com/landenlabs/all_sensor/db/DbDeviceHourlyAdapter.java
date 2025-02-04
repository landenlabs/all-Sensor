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
import com.landenlabs.all_sensor.sensor.AbsCfg;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.TimeZone;

/**
 * Recycler adapter to access Hourly device database.
 */
public class DbDeviceHourlyAdapter
        extends RecyclerView.Adapter<DbDeviceHourlyAdapter.HourlyHolder>
        implements IAdapterHighlight {

    private final Context mContext;
    private final Cursor mCursor;
    private final AbsCfg cfg;

    private final DateTimeFormatter dateHdrFmt;
    private final DateTimeFormatter yearHdrFmt;
    private final DateTimeFormatter dateRowFmt;
    private final DateTimeZone zone;

    private final int colMilli;
    private final int colTempC100;
    private final int colHumP100;

    /*
    private final SparseIntArray minTempC100 = new SparseIntArray();
    private final SparseIntArray maxTempC100 = new SparseIntArray();
    private final SparseIntArray minHumP100 = new SparseIntArray();
    private final SparseIntArray maxHumP100 = new SparseIntArray();
    */

    // ---------------------------------------------------------------------------------------------
    public DbDeviceHourlyAdapter(Context context, Cursor cursor, AbsCfg cfg) {
        mContext = context;
        mCursor = cursor;
        this.cfg = cfg;
        TimeZone localZone = TimeZone.getDefault();
        zone = DateTimeZone.forTimeZone(localZone);
        dateHdrFmt = DateTimeFormat.forPattern("MMM d E").withZone(zone);
        yearHdrFmt = DateTimeFormat.forPattern("YYYY").withZone(zone);
        dateRowFmt = DateTimeFormat.forPattern("hh:mm a").withZone(zone);

        colMilli = mCursor.getColumnIndex(DbDeviceHourly.COL_MILLI);
        colTempC100 = mCursor.getColumnIndex(DbDeviceHourly.COL_TEMPC100);
        colHumP100 = mCursor.getColumnIndex(DbDeviceHourly.COL_HUM100);
    }

    @NonNull
    @Override
    public HourlyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.device_hourly_row, parent, false);
        return new HourlyHolder(view);
    }

    static final @ColorInt int TEXT_COLOR_INC = 0xff101080;
    static final @ColorInt int TEXT_COLOR_DEC = 0xff801010;

    private static void colorText(TextView txView, int prevVal, int thisVal, int minDelta) {
        int deltaVal = thisVal - prevVal;
        if (Math.abs(deltaVal) > minDelta) {
            txView.setTextColor(deltaVal > 0 ? TEXT_COLOR_INC : TEXT_COLOR_DEC);
        } else {
            txView.setTextColor(Color.BLACK);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull HourlyHolder hourlyHolder, int position) {

        // Position = [0=header] 1...n [n+1=footer]
        if (!mCursor.moveToPosition(position)) {
            return;
        }

        long milli = mCursor.getLong(colMilli);
        int tempC100 = mCursor.getInt(colTempC100);
        int humP100 = mCursor.getInt(colHumP100);
        String tempStr = mCursor.isNull(colTempC100) ? "--" : String.format("%,.2f",cfg.toDegreeN(tempC100));
        String humStr = mCursor.isNull(colHumP100) ? "--" : String.format("%,.2f", cfg.toHumPerN(humP100));
        DateTime itemTime = new DateTime(milli).withZone(zone);

        @ColorInt int color = ((itemTime.getDayOfYear() & 1) == 1) ? 0xffe0ffe0 : 0xffffe0e0;
        hourlyHolder.itemView.setBackgroundColor(color);
        hourlyHolder.itemView.setTag(R.id.table_row_color, color);

        hourlyHolder.row_numTv.setText(String.format("%,d", position));
        hourlyHolder.row_dateTv.setText(dateRowFmt.print(itemTime));
        hourlyHolder.row_temperatureTv.setText(tempStr);
        hourlyHolder.row_humidityTv.setText(humStr);

        boolean newDay = false;
        if (mCursor.moveToPosition(position - 1)) {
            long prevMilli = mCursor.getLong(colMilli);
            DateTime prevTime = new DateTime(prevMilli).withZone(zone);
            newDay = prevTime.getDayOfYear() != itemTime.getDayOfYear();

            try {
                int prevC100 = mCursor.getInt(colTempC100);
                colorText(hourlyHolder.row_temperatureTv, prevC100, tempC100, 100);
                int prevP100 = mCursor.getInt(colHumP100);
                colorText(hourlyHolder.row_humidityTv, prevP100, humP100, 100);
            } catch (IllegalThreadStateException ignore) {
            }
        } else {
            colorText(hourlyHolder.row_temperatureTv, 0, 0, 100);
            colorText(hourlyHolder.row_humidityTv, 0, 0, 100);
        }

        if (newDay || position == 0) {
            hourlyHolder.hdr_hourly.setVisibility(View.VISIBLE);
            hourlyHolder.hdr_dateTv.setText(dateHdrFmt.print(itemTime));
            hourlyHolder.hdr_temperatureTv.setText(yearHdrFmt.print(itemTime));
        } else {
            hourlyHolder.hdr_hourly.setVisibility(View.GONE);
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
            } else {
                view.setBackgroundColor((Integer) view.getTag((R.id.table_row_color)));
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
    public static class HourlyHolder extends RecyclerView.ViewHolder {
        public final ViewGroup hdr_hourly;
        public final TextView hdr_numTv;
        public final TextView hdr_dateTv;
        public final TextView hdr_temperatureTv;
        public final TextView hdr_humidityTv;

        public final TextView row_numTv;
        public final TextView row_dateTv;
        public final TextView row_temperatureTv;
        public final TextView row_humidityTv;

        public final ViewGroup bot_hourly;
        public final TextView bot_numTv;
        public final TextView bot_dateTv;
        public final TextView bot_temperatureTv;
        public final TextView bot_humidityTv;

        public HourlyHolder(View itemView) {
            super(itemView);
            hdr_hourly = itemView.findViewById(R.id.hdr_hourly);
            hdr_numTv = itemView.findViewById(R.id.hdr_hourly_num);
            hdr_dateTv = itemView.findViewById(R.id.hdr_hourly_date);
            hdr_temperatureTv = itemView.findViewById(R.id.hdr_hourly_temperature);
            hdr_humidityTv = itemView.findViewById(R.id.hdr_hourly_humidity);

            row_numTv = itemView.findViewById(R.id.row_hourly_num);
            row_dateTv = itemView.findViewById(R.id.row_hourly_date);
            row_temperatureTv = itemView.findViewById(R.id.row_hourly_temperature);
            row_humidityTv = itemView.findViewById(R.id.row_hourly_humidity);

            bot_hourly = itemView.findViewById(R.id.bot_hourly);
            bot_numTv = itemView.findViewById(R.id.bot_hourly_num);
            bot_dateTv = itemView.findViewById(R.id.bot_hourly_date);
            bot_temperatureTv = itemView.findViewById(R.id.bot_hourly_temperature);
            bot_humidityTv = itemView.findViewById(R.id.bot_hourly_humidity);
        }
    }
}