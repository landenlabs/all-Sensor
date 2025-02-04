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

package com.landenlabs.all_sensor.ui;

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
import com.landenlabs.all_sensor.db.IAdapterHighlight;
import com.landenlabs.all_sensor.logger.ALogCusor;
import com.landenlabs.all_sensor.logger.ALogFileWriter;
import com.landenlabs.all_sensor.sensor.AbsCfg;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.TimeZone;

@SuppressWarnings("FieldCanBeLocal")
public class LogViewAdapter
        extends RecyclerView.Adapter<com.landenlabs.all_sensor.ui.LogViewAdapter.LogViewHolder>
        implements IAdapterHighlight {

    private final Context mContext;
    private final Cursor mCursor;
    private final AbsCfg cfg;

    private final DateTimeFormatter dateHdrFmt;
    private final DateTimeFormatter yearHdrFmt;
    private final DateTimeFormatter dateRowFmt;
    private final DateTimeZone zone;

    private final int colDate;
    private final int colType;
    private final int colMsg;
    public final DateTimeFormatter dtFmt;
    // ---------------------------------------------------------------------------------------------
    public LogViewAdapter(Context context, Cursor cursor, AbsCfg cfg) {
        mContext = context;
        mCursor = cursor;
        this.cfg = cfg;
        TimeZone localZone = TimeZone.getDefault();
        zone = DateTimeZone.forTimeZone(localZone);
        dateHdrFmt = DateTimeFormat.forPattern("MMM d E").withZone(zone);
        yearHdrFmt = DateTimeFormat.forPattern("YYYY").withZone(zone);
        dateRowFmt = DateTimeFormat.forPattern("hh:mm a").withZone(zone);

        colDate = mCursor.getColumnIndex(ALogCusor.COL_DATE);
        colType = mCursor.getColumnIndex(ALogCusor.COL_TYPE);
        colMsg = mCursor.getColumnIndex(ALogCusor.COL_MSG);

        dtFmt = DateTimeFormat.forPattern(ALogFileWriter.TIMESTAMP_FORMAT);
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.logview_row, parent, false);
        return new LogViewHolder(view);
    }

    private long getMilli() {
        String dateStr = mCursor.getString(colDate).trim();
        DateTime dt = DateTime.parse(dateStr, dtFmt);
        long milli = dt.getMillis();
        return milli;
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder hourlyHolder, int position) {

        // Position = [0=header] 1...n [n+1=footer]
        if (!mCursor.moveToPosition(position)) {
            return;
        }

        // Example row
        //      Thu 24 17:50 | D | Scheduler-WIFI
        try {
            long milli = getMilli();
            String typeStr = mCursor.getString(colType);
            String msgStr = mCursor.getString(colMsg);
            DateTime itemTime  = new DateTime(milli).withZone(zone);

            @ColorInt int color = ((itemTime.getDayOfYear() & 1) == 1) ? 0xffe0ffe0 : 0xffffe0e0;
            hourlyHolder.itemView.setBackgroundColor(color);
            hourlyHolder.itemView.setTag(R.id.table_row_color, color);

            hourlyHolder.row_numTv.setText(String.format("%,d", position));
            hourlyHolder.row_dateTv.setText(dateRowFmt.print(itemTime));
            hourlyHolder.row_typeTv.setText(typeStr);
            hourlyHolder.row_msgTv.setText(msgStr);

            if (false) {
                boolean newDay = false;
                if (mCursor.moveToPosition(position - 1)) {
                    long prevMilli = getMilli();
                    DateTime prevTime = new DateTime(prevMilli).withZone(zone);
                    newDay = prevTime.getDayOfYear() != itemTime.getDayOfYear();
                }

                if (newDay || position == 0) {
                    hourlyHolder.hdr_hourly.setVisibility(View.VISIBLE);
                    hourlyHolder.hdr_dateTv.setText(dateHdrFmt.print(itemTime));
                    hourlyHolder.hdr_temperatureTv.setText(yearHdrFmt.print(itemTime));
                } else {
                    hourlyHolder.hdr_hourly.setVisibility(View.GONE);
                }
            }
        } catch (Exception ex) {
            @ColorInt int color = 0x80ff8080;
            hourlyHolder.itemView.setBackgroundColor(color);
            hourlyHolder.itemView.setTag(R.id.table_row_color, color);

            hourlyHolder.row_numTv.setText(String.format("%,d", position));
            hourlyHolder.row_dateTv.setText("-");
            hourlyHolder.row_typeTv.setText("-");
            hourlyHolder.row_msgTv.setText("-");
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

    // =============================================================================================
    public static class LogViewHolder extends RecyclerView.ViewHolder {
        public final ViewGroup hdr_hourly;
        public final TextView hdr_numTv;
        public final TextView hdr_dateTv;
        public final TextView hdr_temperatureTv;
        public final TextView hdr_humidityTv;

        public final TextView row_numTv;
        public final TextView row_dateTv;
        public final TextView row_typeTv;
        public final TextView row_msgTv;

        public final ViewGroup bot_hourly;
        public final TextView bot_numTv;
        public final TextView bot_dateTv;
        public final TextView bot_temperatureTv;
        public final TextView bot_humidityTv;

        public LogViewHolder(View itemView) {
            super(itemView);
            hdr_hourly = itemView.findViewById(R.id.hdr_hourly);
            hdr_numTv = itemView.findViewById(R.id.hdr_hourly_num);
            hdr_dateTv = itemView.findViewById(R.id.hdr_hourly_date);
            hdr_temperatureTv = itemView.findViewById(R.id.hdr_hourly_temperature);
            hdr_humidityTv = itemView.findViewById(R.id.hdr_hourly_humidity);

            row_numTv = itemView.findViewById(R.id.row_hourly_num);
            row_dateTv = itemView.findViewById(R.id.row_hourly_date);
            row_typeTv = itemView.findViewById(R.id.row_hourly_temperature);
            row_msgTv = itemView.findViewById(R.id.row_hourly_humidity);

            bot_hourly = itemView.findViewById(R.id.bot_hourly);
            bot_numTv = itemView.findViewById(R.id.bot_hourly_num);
            bot_dateTv = itemView.findViewById(R.id.bot_hourly_date);
            bot_temperatureTv = itemView.findViewById(R.id.bot_hourly_temperature);
            bot_humidityTv = itemView.findViewById(R.id.bot_hourly_humidity);
        }
    }
}