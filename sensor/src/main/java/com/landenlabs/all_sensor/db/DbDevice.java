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

import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Abstract base class for SQL database classes
 * <p>
 * https://developer.android.com/reference/androidx/sqlite/db/package-summary
 */
public abstract class DbDevice implements DatabaseErrorHandler {

    public SQLiteDatabase database;
    public boolean isError = false;
    public Exception exception = null;
    public final String dbFile;

    // final String  CREATE_TABLE_IF_NOT_EXIST = "CREATE TABLE IF NOT EXISTS ";
    final String CREATE_TABLE = "CREATE TABLE ";

    // ---------------------------------------------------------------------------------------------
    public DbDevice(@NonNull String dbFile) {
        this.dbFile = dbFile;
    }

    public abstract void create(boolean deleteDbFirst, boolean dropTableFirst) throws android.database.SQLException;

    public abstract void readMeta();

    public abstract void writeMeta();

    public String getFile() {
        return dbFile;
    }

    public boolean isOpen() {
        return database != null && database.isOpen();
    }
    
    public void close() {
        if (database != null) {
            writeMeta();
            database.close();
            database = null;
            isError = false;
        }
    }

    public void beginAdd() {
        database.beginTransaction();
    }

    public void endAdd() {
        database.setTransactionSuccessful();
        database.endTransaction();
    }

    /*
    public boolean openReadOnly() throws android.database.SQLException {
        if (database == null) {
            isError = false;
            database = SQLiteDatabase.openDatabase(getFile(), null, SQLiteDatabase.OPEN_READONLY, this);
            readMeta();
        }
        return database != null && database.isReadOnly() && !isError;
    }
     */


    boolean deleteDbFirst() {
        return false;
    }
    boolean dropTableFirst() {
        return false;
    }
    public Exception openWrite(boolean create) throws android.database.SQLException {
        if (database == null) {
            isError = false;
            exception = null;
            try {
                if (create) {
                    create(deleteDbFirst(), dropTableFirst());
                }
                database = SQLiteDatabase.openDatabase(getFile(), null, SQLiteDatabase.OPEN_READWRITE, this);
                readMeta();
            } catch (Exception ex) {
                close();
                isError = true;
                this.exception = ex;
            }
            return this.exception;
        }
        return database.isOpen() && !database.isReadOnly() && !isError ? null : this.exception;
    }

    @Override
    public void onCorruption(SQLiteDatabase dbObj) {
        isError = true;
    }

    void chainError(@NonNull Exception ex) {
        if (this.exception != null) {
            ex.addSuppressed(this.exception);
        }
        this.exception = ex;
    }

    @NonNull
    public Cursor query(@NonNull String sqlQuery, @Nullable String[] selectionArgs) {
        // String sqlQuery = "Select * FROM " + DATA_TABLE_NAME1;
        return database.rawQuery(sqlQuery, selectionArgs);
    }
}