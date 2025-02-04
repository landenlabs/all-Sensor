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

package com.landenlabs.all_sensor.logger;

import android.database.CursorWrapper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Creates a cursor wrapper around a log file. 
 */
public class ALogCusor extends CursorWrapper {

    // Example log row
    //      Thu 24 17:50 | D | Scheduler-WIFI
    public static final String COL_DATE = "milli";
    public static final String COL_TYPE = "type";
    public static final String COL_MSG = "msg";
    public static final String[] COL_NAMES = { COL_DATE, COL_TYPE, COL_MSG};
    private static final String[] EMPTY_ROW_PARTS = new String[] {"", "", ""};

    private static final int COL_COUNT = 3;
    private static final int MAX_ROWS = 100;

    private RandomAccessFile logReader;
    private final long[] rowPos = new long[MAX_ROWS];
    private long lastRowPos = 0;
    private int rowFirstIdx;
    private int row = -1;
    private int ROW_CNT = 0;

    public ALogCusor(ALogFileWriter logFile) {
        super(null);
        String logPath = logFile.getFile().getAbsolutePath();
        try {
            logReader = new RandomAccessFile(logPath, "r");
            String str;
            try {
                long fileLen = logFile.getFile().length();
                long filePos = 0;
                do {
                    filePos = logReader.getFilePointer();
                    rowPos[(ROW_CNT++) % MAX_ROWS] = filePos;
                    str = logReader.readLine();
                } while (filePos < fileLen);
                rowFirstIdx = (ROW_CNT <= MAX_ROWS) ? 0 : ROW_CNT % MAX_ROWS;
                ROW_CNT = Math.min(ROW_CNT, MAX_ROWS);
                logReader.seek(0);
            } catch (Exception ex) {
                logReader = new RandomAccessFile(logPath, "r");
            }
            lastRowPos = getRowPos(ROW_CNT-1);
        } catch (FileNotFoundException ex) {
            ALog.e.tagMsg(this, ex);
        }
    }

    private long getRowPos(int pos) {
        return rowPos[(pos + rowFirstIdx) % MAX_ROWS];
    }

    @Override
    public boolean isClosed() {
        return logReader == null;
    }

    @Override
    public int getCount() {
        return ROW_CNT;
    }

    @Override
    public boolean moveToFirst() {
        try {
             logReader.seek(0);
             row=0;
             return lastRowPos > 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public int getColumnCount() {
        return COL_COUNT;
    }

    @Override
    public int getColumnIndex(String columnName) {
        for (int col = 0; col < COL_NAMES.length; col++) {
            if (COL_NAMES[col].equals(columnName))
                return col;
        }
        return -1;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return (columnIndex >=0 && columnIndex < COL_NAMES.length) ? COL_NAMES[columnIndex] : null;
    }

    @Override
    public String[] getColumnNames() {
        return COL_NAMES;
    }

    @Override
    public long getLong(int col) {
        throw new NumberFormatException("numbers not available");
    }

    @Override
    public String getString(int col) {
        String[] parts = getRowParts();
        return parts[col];
    }

    String rowStr;
    String[] rowParts;
    int rowStrPos = -1;
    private String[] getRowParts()   {
        if (rowStrPos != row) {
            try {
                rowStr = this.logReader.readLine();
                rowParts = rowStr.split("[|]");
            } catch (IOException e) {
                rowStr = null;
                rowParts = EMPTY_ROW_PARTS;
                rowStrPos = row;
            }
            rowStrPos = row;
        }
        return rowParts;
    }

    @Override
    public boolean isAfterLast() {
        return row >= ROW_CNT;
    }

    @Override
    public boolean isBeforeFirst() {
        return row == -1;
    }

    @Override
    public boolean isFirst() {
        return row == 0;
    }

    @Override
    public boolean isLast() {
        return row == ROW_CNT-1;
    }

    @Override
    public int getType(int columnIndex) {
        return super.getType(columnIndex);
    }

    @Override
    public boolean isNull(int columnIndex) {
        return super.isNull(columnIndex);
    }

    @Override
    public boolean moveToLast() {
        try {
            logReader.seek(logReader.length());
            return true;
        } catch (IOException ignore) {
        }
        return false;
    }

    @Override
    public boolean move(int newRow) {
        try {
            logReader.seek(getRowPos(newRow));
            row = newRow;
            return true;
        } catch (IOException ignore) {
        }
        row = Math.min(ROW_CNT, Math.max(0, newRow));
        return false;
    }

    @Override
    public boolean moveToPosition(int position) {
        return move(position);
    }

    @Override
    public boolean moveToNext() {
        return move(row+1);
    }

    @Override
    public boolean moveToPrevious() {
        return move(row-1);
    }
}
