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

import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;
import static android.database.sqlite.SQLiteDatabase.CREATE_IF_NECESSARY;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.sensor.DeviceGoveeHelper;
import com.landenlabs.all_sensor.utils.ArrayListEx;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Objects;

/**
 * Hourly device SQL database, stores hourly average (or max or min) per hour.
 */
public class DbDeviceHourly extends DbDevice {

    private static final int VERSION = 101;

    // Hourly database columns (public so friend adapter can access)
    public static final String DATA_TABLE_NAME1 = "HourTableD1";
    public static final String COL_MILLI = "DMilli";
    public static final int COL_MILLI_NUM = 0;
    public static final String COL_TEMPC100 = "DTempC100";
    public static final int COL_TEMPC_NUM = COL_MILLI_NUM + 1;
    public static final String COL_HUM100 = "DHUM100";
    public static final int COL_HUM_NUM = COL_TEMPC_NUM + 1;

    // Meta database columns
    public static final String META_TABLE_NAME1 = "HourTableM1";
    public static final String COL_M_SLOT = "MSlot";
    public static final String COL_M_START_MILLI = "MStartMilli";
    public static final String COL_M_START_INDEX = "MStartIndex";
    public static final String COL_M_FIRST_MILLI = "MFirstMilli";
    public static final String COL_M_FIRST_INDEX = "MFirstIndex";
    public static final String COL_M_LAST_MILLI = "MLastMilli";
    public static final String COL_M_LAST_INDEX = "MLastIndex";

    public long startMilli = 0;     // Start of Cloud data
    public long startIndex = 0;     // Start of Cloud data
    public long firstMilli = 0;     // First entry in database
    public long firstIndex = 0;
    public long lastMilli = 0;      // Last entry in database
    public long lastIndex = 0;
    private boolean metaChanged = false;

    // ---------------------------------------------------------------------------------------------
    public DbDeviceHourly(@NonNull String dbFile) {
        super(dbFile);
    }

    @Override
    public void create(boolean deleteDbFirst, boolean dropTableFirst) throws android.database.SQLException {
        if (deleteDbFirst) {
            SQLiteDatabase.deleteDatabase(new File(getFile()));
        }
        if (dropTableFirst) {
            if (openWrite(false) == null) {
                database.rawQuery("DROP TABLE IF EXISTS " + DATA_TABLE_NAME1, null).close();
                database.rawQuery("DROP TABLE IF EXISTS " + META_TABLE_NAME1, null).close();
                close();
            }
        }

        database = SQLiteDatabase.openDatabase(getFile(), null, CREATE_IF_NECESSARY, this);
        if (database.getVersion() != VERSION) {
            database.setVersion(VERSION);

            // String anyTable = "SELECT name FROM sqlite_master WHERE type='table' AND name=" + DATA_TABLE_NAME;

            String createDataTableSql = CREATE_TABLE + DATA_TABLE_NAME1 + " ("
                    + COL_MILLI + " LONG" + " PRIMARY KEY"
                    + ", " + COL_TEMPC100 + " INT"
                    + ", " + COL_HUM100 + " INT"
                    + ")";
            database.execSQL(createDataTableSql);

            String createMetaTableSql = CREATE_TABLE + META_TABLE_NAME1 + " ("
                    + COL_M_SLOT + " INT" + " PRIMARY KEY"
                    + ", " + COL_M_START_MILLI + " LONG"
                    + ", " + COL_M_START_INDEX + " LONG"
                    + ", " + COL_M_FIRST_MILLI + " LONG"
                    + ", " + COL_M_FIRST_INDEX + " LONG"
                    + ", " + COL_M_LAST_MILLI + " LONG"
                    + ", " + COL_M_LAST_INDEX + " LONG"
                    + ")";
            database.execSQL(createMetaTableSql);

            addMeta(true, 0, 0, 0, 0, 0, 0);
        } else {
            readMeta(); // throws
        }
        close();
    }

    @Override
    public void readMeta() {
        if (startMilli == 0) {
            String sqlQuery = "Select * FROM " + META_TABLE_NAME1 + " WHERE " + COL_M_SLOT + "=0";
            try {
                try (Cursor result = database.rawQuery(sqlQuery, null)) {
                    startMilli = firstMilli = lastMilli = 0;
                    startIndex = firstIndex = lastIndex = 0;

                    if (result.moveToFirst()) {
                        // String[] colNames = result.getColumnNames();
                        int col = 1;
                        startMilli = result.getLong(col++);    // COL_M_START_MILLI);
                        startIndex = result.getLong(col++);    // COL_M_START_INDEX);
                        firstMilli = result.getLong(col++);    // COL_M_FIRST_MILLI);
                        firstIndex = result.getLong(col++);     // COL_M_FIRST_INDEX);
                        lastMilli = result.getLong(col++);      // COL_M_LAST_MILLI);
                        lastIndex = result.getLong(col);      // COL_M_LAST_INDEX);

                        if (firstMilli < startMilli || firstMilli > lastMilli) {
                            SimpleDateFormat debugFmt = new SimpleDateFormat("MMM d yyyy hh:mm a z");
                            ALog.e.tagMsg(this, "Read Invalid Meta data "
                                    ," Start=", debugFmt.format(startMilli)
                                    ," First=", debugFmt.format(firstMilli)
                                    ," Last=", debugFmt.format(lastMilli)
                            );
                        }
                    } else {
                        ALog.e.tagMsg(this, "Read unable to read meta data");
                    }
                }
            } catch (Exception ex) {
                ALog.e.tagMsg(this, "readMeta", ex);
                chainError(ex);
                throw ex;
            }
        }
    }

    @Override
    public void writeMeta() {
        addMeta(false, startMilli, startIndex, firstMilli, firstIndex, lastMilli, lastIndex);
        metaChanged = false;
    }

    /*
    public int getRowCount() {
        // COUNT(*)
        // SELECT COUNT(*) FROM t1;
        int count = -1;
        String sqlQuery = "SELECT COUNT(*) FROM " + DATA_TABLE_NAME1;
        try {
            try (Cursor result = database.rawQuery(sqlQuery, null)) {
                if (result.moveToNext()) {
                    count = result.getInt(0);
                }
            }
        } catch (Exception ex) {
            chainError(ex);
        }
        return count;
    }
     */

    public void updateMeta(long startMilli, long startIndex, long firstMilli, long firstIndex, long lastMilli, long lastIndex) {
        int hashBefore = Objects.hash(startMilli, startIndex, firstMilli, firstIndex, lastMilli, lastIndex);
        if (this.startMilli == 0) {
            this.startMilli = startMilli;
            this.startIndex = startIndex;
            this.firstMilli = firstMilli;
            this.firstIndex = firstIndex;
            this.lastMilli = lastMilli;
            this.lastIndex = lastIndex;
        } else {
            // this.startMilli = Math.max(this.startMilli, startMilli);
            // this.startIndex = Math.max(this.startIndex, startIndex);
            this.firstIndex = Math.min(this.firstIndex, firstIndex);
            this.firstMilli = Math.min(this.firstMilli, firstMilli);
            this.lastIndex = Math.max(this.lastIndex, lastIndex);
            this.lastMilli = Math.max(this.lastMilli, lastMilli);
        }
        if (firstMilli < startMilli ) {
            ALog.e.tagMsg(this, "updateMeta - Invalid time; first before start.",
                    " first=", new DateTime(firstMilli).toString(),
                    " start=", new DateTime(startMilli).toString()
            );
        }
        if (firstMilli > lastMilli) {
            ALog.e.tagMsg(this, "updateMeta - Invalid time; first after last.",
                    " first=", new DateTime(firstMilli).toString(),
                    " last=", new DateTime(lastMilli).toString()
            );
        }
        int hashAfter = Objects.hash(startMilli, startIndex, firstMilli, firstIndex, lastMilli, lastIndex);

        this.metaChanged |= (hashAfter != hashBefore);
    }

    public void addMeta(boolean add, long startMilli, long startIndex, long firstMilli, long firstIndex, long lastMilli, long lastIndex) {
        if (firstMilli < startMilli) {
            SimpleDateFormat debugFmt = new SimpleDateFormat("MMM d yyyy hh:mm a z");
            ALog.e.tagMsg(this, getFile() + " Write Invalid Meta data "
                    ," Start=", debugFmt.format(startMilli)
                    ," First=", debugFmt.format(firstMilli)
                    ," StartIdx=", startIndex
                    ," FirstIdx=", firstIndex
            );
            startMilli = firstMilli;
            startIndex = firstIndex;
        }
        if (firstMilli > lastMilli) {
            SimpleDateFormat debugFmt = new SimpleDateFormat("MMM d yyyy hh:mm a z");
            ALog.e.tagMsg(this, getFile() + " Write Invalid Meta data "
                    ," First=", debugFmt.format(firstMilli)
                    ," Last=", debugFmt.format(lastMilli)
            );
        }
        boolean okay = false;
        if (database.isOpen()) {
            ContentValues cv = new ContentValues();
            cv.put(COL_M_SLOT, 0);
            cv.put(COL_M_START_MILLI, startMilli);
            cv.put(COL_M_START_INDEX, startIndex);
            cv.put(COL_M_FIRST_MILLI, firstMilli);
            cv.put(COL_M_FIRST_INDEX, firstIndex);
            cv.put(COL_M_LAST_MILLI, lastMilli);
            cv.put(COL_M_LAST_INDEX, lastIndex);

            long result;
            if (add) {
                result = database.insert(META_TABLE_NAME1, null, cv);
            } else {
                result = database.update(META_TABLE_NAME1, cv, COL_M_SLOT + "=0", null);
            }
            okay = (result != -1);
            if (!okay) {
                chainError(new SQLException("Failed to update meta record"));
            }
        }

        // return okay;
    }

    public boolean add(DeviceGoveeHelper.Sample sample) {
        boolean okay;
        ContentValues cv = new ContentValues();
        cv.put(COL_MILLI, sample.milli);
        cv.put(COL_TEMPC100, sample.tem);
        cv.put(COL_HUM100, sample.hum);

        // ALog.i.tagMsg(this, "add hour insert ", new DateTime(sample.milli).toString("MMM d E hh:mm a"));
        long result = database.insertWithOnConflict(DATA_TABLE_NAME1, null, cv, CONFLICT_REPLACE);

        okay = (result != -1);
        if (!okay) {
            chainError(new SQLException("Failed to update data record"));
            ALog.e.tagMsg(this, "Failed to add/update ", DATA_TABLE_NAME1);
        }
        return okay;
    }

    @NonNull
    public ArrayListEx<DeviceGoveeHelper.Sample> getRange(Interval interval) {
        ArrayListEx<DeviceGoveeHelper.Sample> resultList = new ArrayListEx<>();

        if (database.isOpen()) {
            String sqlQuery = "Select * FROM " + DATA_TABLE_NAME1;
            sqlQuery += " WHERE " + COL_MILLI + " BETWEEN " + interval.getStartMillis() + " AND " + interval.getEndMillis();
            sqlQuery += " ORDER BY " + DbDeviceHourly.COL_MILLI + " ASC";
            try (Cursor result = database.rawQuery(sqlQuery, null)) {
                while (result.moveToNext()) {
                    // String[] colNames = result.getColumnNames();
                    long milli = result.getLong(COL_MILLI_NUM);
                    int tempC100 = result.getInt(COL_TEMPC_NUM);
                    int hum100 = result.getInt(COL_HUM_NUM);

                    // TODO - this test should be redundant when WHERE is used in SQL.
                    if (milli >= interval.getStartMillis() && milli <= interval.getEndMillis()) {
                        DeviceGoveeHelper.Sample sample = new DeviceGoveeHelper.Sample(tempC100, hum100, milli);
                        resultList.add(sample);
                    }
                }
            }
        }

        return resultList;
    }

}
