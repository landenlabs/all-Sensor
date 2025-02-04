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

/**
 * Daily device SQL database, stores record high/low values.
 */
public class DbDeviceDaily extends DbDevice {

    private static final int VERSION = 101;

    // public so friend DbDeviceDailyAdapter can access
    public static final String DATA_TABLE_NAME1 = "DailyRecTable1";
    public static final String COL00_DAY = "RDayMilli";
    public static final String COL01_TEMPC100_MIN = "RTempC100Min";
    public static final String COL02_TEMP_MIN_MILLI = "RTempMinMilli";
    public static final String COL03_TEMPC100_MAX = "RTempC100Max";
    public static final String COL04_TEMP_MAX_MILLI = "RTempMaxMilli";
    public static final String COL05_HUM100_MIN = "RHum100Min";
    public static final String COL06_HUM_MIN_MILLI = "RHumMinMilli";
    public static final String COL07_HUM100_MAX = "RHum100Max";
    public static final String COL08_HUM_MAX_MILLI = "RHumMaxMilli";

    // ---------------------------------------------------------------------------------------------
    public DbDeviceDaily(@NonNull String dbFile) {
        super(dbFile);
    }

    @Override
    public void create(boolean deleteDbFirst, boolean dropTableFirst) throws android.database.SQLException {

        if (deleteDbFirst) {
            SQLiteDatabase.deleteDatabase(new File(getFile()));
        }
        if (dropTableFirst) {
            openWrite(false);
            String cmd = "DROP TABLE IF EXISTS " + DATA_TABLE_NAME1;
            database.rawQuery(cmd, null).close();
            close();
        }

        database = SQLiteDatabase.openDatabase(getFile(), null, CREATE_IF_NECESSARY, this);

        if (database.getVersion() != VERSION) {
            database.setVersion(VERSION);
            String createTableSql = CREATE_TABLE + DATA_TABLE_NAME1 + " ("
                    + COL00_DAY + " LONG" + " PRIMARY KEY"
                    + ", " + COL01_TEMPC100_MIN + " INT"
                    + ", " + COL02_TEMP_MIN_MILLI + " LONG"
                    + ", " + COL03_TEMPC100_MAX + " INT"
                    + ", " + COL04_TEMP_MAX_MILLI + " LONG"
                    + ", " + COL05_HUM100_MIN + " INT"
                    + ", " + COL06_HUM_MIN_MILLI + " LONG"
                    + ", " + COL07_HUM100_MAX + " INT"
                    + ", " + COL08_HUM_MAX_MILLI + " LONG"
                    + ")";
            database.execSQL(createTableSql);
        }
        close();
    }

    @Override
    public void readMeta() {
    }

    @Override
    public void writeMeta() {
    }

    public boolean add(DeviceGoveeHelper.Sample minTem, DeviceGoveeHelper.Sample maxTem, DeviceGoveeHelper.Sample minHum, DeviceGoveeHelper.Sample maxHum) {
        boolean okay = false;
        if (database.isOpen()) {
            DateTime day = new DateTime(minTem.milli);
            ContentValues cv = new ContentValues();
            cv.put(COL00_DAY, day.withTimeAtStartOfDay().getMillis());
            cv.put(COL01_TEMPC100_MIN, minTem.tem);
            cv.put(COL02_TEMP_MIN_MILLI, minTem.milli);
            cv.put(COL03_TEMPC100_MAX, maxTem.tem);
            cv.put(COL04_TEMP_MAX_MILLI, maxTem.milli);
            cv.put(COL05_HUM100_MIN, minHum.hum);
            cv.put(COL06_HUM_MIN_MILLI, minHum.milli);
            cv.put(COL07_HUM100_MAX, maxHum.hum);
            cv.put(COL08_HUM_MAX_MILLI, maxHum.milli);

            try {
                if (false) {
                    DateTime minDt = new DateTime(minTem.milli);
                    DateTime maxDt = new DateTime(maxTem.milli);
                    DateTime minHt = new DateTime(minHum.milli);
                    DateTime maxHt = new DateTime(maxHum.milli);
                    int refDay = minDt.getDayOfYear();
                    boolean error = refDay != maxDt.getDayOfYear()
                            || refDay != minHt.getDayOfYear()
                            || refDay != maxHt.getDayOfYear();
                    error |= minTem.tem > maxTem.tem;
                    error |= minHum.hum > maxHum.hum;

                    if (error) {
                        ALog.d.tagFmt(this, "RecXXX %s %s %s %s %4d %4d %4d %4d %s"
                                , minDt.toString("yy-M-dd HH z")
                                , maxDt.toString("yy-M-dd HH z")
                                , minHt.toString("yy-M-dd HH z")
                                , maxHt.toString("yy-M-dd HH z")
                                , minTem.tem
                                , maxTem.tem
                                , minHum.hum
                                , maxHum.hum
                                , error ? "**ERROR**" : "");
                    }
                }

                long result = database.insertWithOnConflict(DATA_TABLE_NAME1, null, cv, CONFLICT_REPLACE);

                okay = (result != -1);
                if (!okay) {
                    chainError(new SQLException("Failed to update data record"));
                    ALog.e.tagMsg(this, "Failed to add/update ", DATA_TABLE_NAME1);
                }
            } catch (Exception ex) {
                ALog.e.tagMsg(this, "Failed to update record item ", ex);
            }
        }
        return okay;
    }

    @NonNull
    public ArrayListEx<DeviceGoveeHelper.Record> getRange(Interval interval) {
        ArrayListEx<DeviceGoveeHelper.Record> resultList = new ArrayListEx<>();

        if (database.isOpen()) {
            String sqlQuery = "Select * FROM " + DATA_TABLE_NAME1;
            sqlQuery += " WHERE " + COL00_DAY + " BETWEEN " + interval.getStartMillis() + " AND " + interval.getEndMillis();
            try (Cursor result = database.rawQuery(sqlQuery, null)) {
                while (result.moveToNext()) {
                    // String[] colNames = result.getColumnNames();
                    int col = 0;
                    long dayMilli = result.getLong(col++);
                    int tempC100min = result.getInt(col++);
                    long tempMinMilli = result.getLong(col++);
                    int tempC100max = result.getInt(col++);
                    long tempMaxMilli = result.getLong(col++);
                    int hum100min = result.getInt(col++);
                    long humMinMilli = result.getLong(col++);
                    int hum100max = result.getInt(col++);
                    long humMaxMilli = result.getLong(col);

                    // TODO - this test should be redundant when WHERE is used in SQL.
                    if (dayMilli >= interval.getStartMillis() && dayMilli <= interval.getEndMillis()) {
                        DeviceGoveeHelper.Record record = new DeviceGoveeHelper.Record(dayMilli,
                                new DeviceGoveeHelper.D2(tempC100min, tempMinMilli),
                                new DeviceGoveeHelper.D2(tempC100max, tempMaxMilli),
                                new DeviceGoveeHelper.D2(hum100min, humMinMilli),
                                new DeviceGoveeHelper.D2(hum100max, humMaxMilli));
                        record.isValid();
                        resultList.add(record);
                    }
                }
            }
        }

        return resultList;
    }
}
