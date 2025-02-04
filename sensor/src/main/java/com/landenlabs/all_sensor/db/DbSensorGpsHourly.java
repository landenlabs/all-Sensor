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

public class DbSensorGpsHourly extends DbSensorHourly {

    public static final String DATA_TABLE_NAME1 = "GpsTableD1";
    public static final String META_TABLE_NAME1 = "GpsTableM1";

    private static final int VERSION = 102;

    public static final String COL_LAT = "DLat";
    public static final String COL_LNG = "DLng";
    public static final String COL_ELV = "DElv";
    public static final String COL_SPD = "DSpd";
    // public static final int COL_FVALUE_NUM = COL_MILLI_NUM + 1;

    // ---------------------------------------------------------------------------------------------
    public DbSensorGpsHourly(@NonNull String dbFile) {
        super(dbFile, DATA_TABLE_NAME1, META_TABLE_NAME1);
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
                    + ", " + COL_LAT + " FLOAT"
                    + ", " + COL_LNG + " FLOAT"
                    + ", " + COL_ELV + " FLOAT"
                    + ", " + COL_SPD + " FLOAT"
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

    @Override
    public boolean add(SensorAndroid.SensorSample sample) {
        boolean okay = false;
        if (sample instanceof  SensorAndroid.GpsLocation) {
            SensorAndroid.GpsLocation gpsLocation = (SensorAndroid.GpsLocation)sample;
            ContentValues cv = new ContentValues();
            cv.put(COL_MILLI, sample.milli);
            cv.put(COL_LAT, (float)gpsLocation.location.getLatitude());
            cv.put(COL_LNG, (float)gpsLocation.location.getLongitude());
            cv.put(COL_ELV, (float)gpsLocation.location.getAltitude());
            cv.put(COL_SPD, (float)gpsLocation.location.getSpeed());

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
        }
        return okay;
    }

    @Override
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
