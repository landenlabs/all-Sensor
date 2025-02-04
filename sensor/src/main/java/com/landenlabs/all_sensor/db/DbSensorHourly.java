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
import com.landenlabs.all_sensor.sensor.SensorAndroid;
import com.landenlabs.all_sensor.utils.ArrayListEx;

import org.joda.time.Interval;

import java.io.File;
import java.util.Objects;

/**
 * Store Pressure sensor data.
 */
public abstract class DbSensorHourly extends DbDevice {

    private static final int VERSION = 101;

    public  final String DATA_TABLE_NAME;
    public static final String COL_MILLI = "DMilli";
    public static final int COL_MILLI_NUM = 0;
    public static final String COL_FVALUE = "DPress";
    public static final int COL_FVALUE_NUM = COL_MILLI_NUM + 1;

    public  final String META_TABLE_NAME;
    public static final String COL_M_SLOT = "MSlot";
    public static final String COL_M_FIRST_MILLI = "MFirstMilli";
    public static final String COL_M_LAST_MILLI = "MLastMilli";

    public long firstMilli = 0;     // First entry in database
    public long lastMilli = 0;      // Last entry in database
    protected boolean metaChanged = false;

    // ---------------------------------------------------------------------------------------------
    public DbSensorHourly(@NonNull String dbFile, String dataName, String metaName) {
        super(dbFile);
        DATA_TABLE_NAME = dataName;
        META_TABLE_NAME = metaName;
    }

    @Override
    public void create(boolean deleteDbFirst, boolean dropTableFirst) throws SQLException {
        if (deleteDbFirst) {
            SQLiteDatabase.deleteDatabase(new File(getFile()));
        }
        if (dropTableFirst) {
            if (openWrite(false) == null) {
                database.rawQuery("DROP TABLE IF EXISTS " + DATA_TABLE_NAME, null).close();
                database.rawQuery("DROP TABLE IF EXISTS " + META_TABLE_NAME, null).close();
                close();
            }
        }

        database = SQLiteDatabase.openDatabase(getFile(), null, CREATE_IF_NECESSARY, this);
        if (database.getVersion() != VERSION) {
            database.setVersion(VERSION);

            String createDataTableSql = CREATE_TABLE + DATA_TABLE_NAME + " ("
                    + COL_MILLI + " LONG" + " PRIMARY KEY"
                    + ", " + COL_FVALUE + " FLOAT"
                    + ")";
            database.execSQL(createDataTableSql);

            String createMetaTableSql = CREATE_TABLE + META_TABLE_NAME + " ("
                    + COL_M_SLOT + " INT" + " PRIMARY KEY"
                    + ", " + COL_M_FIRST_MILLI + " LONG"
                    + ", " + COL_M_LAST_MILLI + " LONG"
                    + ")";
            database.execSQL(createMetaTableSql);

            addMeta(true, 0, 0);
        } else {
            readMeta(); // throws
        }
        close();
    }

    @Override
    public void readMeta() {
        if (firstMilli == 0) {
            String sqlQuery = "Select * FROM " + META_TABLE_NAME + " WHERE " + COL_M_SLOT + "=0";
            try {
                try (Cursor result = database.rawQuery(sqlQuery, null)) {
                    if (result.moveToFirst()) {
                        // String[] colNames = result.getColumnNames();
                        int col = 1;
                        firstMilli = result.getLong(col++);    // COL_M_FIRST_MILLI);
                        lastMilli = result.getLong(col++);      // COL_M_LAST_MILLI);
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
        addMeta(false, firstMilli, lastMilli);
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

    public void updateMeta(long firstMilli, long lastMilli) {
        int hashBefore = Objects.hash(firstMilli, lastMilli);
        if (this.firstMilli == 0) {
            this.firstMilli = firstMilli;
            this.lastMilli = lastMilli;
        } else {
            this.firstMilli = Math.min(this.firstMilli, firstMilli);
            this.lastMilli = Math.max(this.lastMilli, lastMilli);
        }
        int hashAfter = Objects.hash(firstMilli, lastMilli);

        this.metaChanged |= (hashAfter != hashBefore);
    }

    public void addMeta(boolean add, long firstMilli, long lastMilli) {
        boolean okay = false;
        if (database.isOpen()) {
            ContentValues cv = new ContentValues();
            cv.put(COL_M_SLOT, 0);
            cv.put(COL_M_FIRST_MILLI, firstMilli);
            cv.put(COL_M_LAST_MILLI, lastMilli);

            long result;
            if (add) {
                result = database.insert(META_TABLE_NAME, null, cv);
            } else {
                result = database.update(META_TABLE_NAME, cv, COL_M_SLOT + "=0", null);
            }
            okay = (result != -1);
            if (!okay) {
                chainError(new SQLException("Failed to update meta record"));
            }
        }

        // return okay;
    }

    public boolean add(SensorAndroid.SensorSample sample) {
        boolean okay;
        ContentValues cv = new ContentValues();
        cv.put(COL_MILLI, sample.milli);
        cv.put(COL_FVALUE, sample.fvalue); // sample millibars

        // ALog.i.tagMsg(this, "add hour insert ", new DateTime(sample.milli).toString("MMM d E hh:mm a"));
        long result = database.insertWithOnConflict(DATA_TABLE_NAME, null, cv, CONFLICT_REPLACE);

        okay = (result != -1);
        if (!okay) {
            chainError(new SQLException("Failed to update data record"));
            ALog.e.tagMsg(this, "Failed to add/update ", DATA_TABLE_NAME);
        } else {
            firstMilli = (firstMilli != 0) ? firstMilli : sample.milli;
            updateMeta(firstMilli, sample.milli);
        }
        return okay;
    }

    @NonNull
    public ArrayListEx<SensorAndroid.SensorSample> getRange(Interval interval) {
        ArrayListEx<SensorAndroid.SensorSample> resultList = new ArrayListEx<>();

        if (database.isOpen()) {
            String sqlQuery = "Select * FROM " + DATA_TABLE_NAME;
            sqlQuery += " WHERE " + COL_MILLI + " BETWEEN " + interval.getStartMillis() + " AND " + interval.getEndMillis();
            sqlQuery += " ORDER BY " + DbSensorHourly.COL_MILLI + " ASC";
            try (Cursor result = database.rawQuery(sqlQuery, null)) {
                while (result.moveToNext()) {
                    // String[] colNames = result.getColumnNames();
                    long milli = result.getLong(COL_MILLI_NUM);
                    float mb = result.getFloat(COL_FVALUE_NUM);

                    // TODO - this test should be redundant when WHERE is used in SQL.
                    if (milli >= interval.getStartMillis() && milli <= interval.getEndMillis()) {
                        SensorAndroid.SensorSample sample = new SensorAndroid.SensorSample(milli, mb);
                        resultList.add(sample);
                    }
                }
            }
        }

        return resultList;
    }

}
