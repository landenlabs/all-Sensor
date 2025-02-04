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
import android.database.CursorWrapper;

import org.joda.time.Duration;

import java.util.concurrent.TimeUnit;

// Sorted reverse order, newest first (higher milli) to oldest (smaller milli)
public class CursorHourly extends CursorWrapper {

    private final Cursor cursor;
    private final int colMilli;
    private long missingHours = 0;
    private int realPos = -1;
    private int virtPos = -1;
    private int curVPos = -1;
    private long realMilli = -1;

    public CursorHourly(Cursor cursor) {
        super(cursor);
        this.cursor = cursor;
        colMilli = cursor.getColumnIndex(DbDeviceHourly.COL_MILLI);

        if (cursor.moveToFirst()) {
            long prevMilli = cursor.getLong(colMilli);
            while (cursor.moveToNext()) {
                long nextMilli = cursor.getLong(colMilli);
                // Sorted reverse order, newest first (higher milli) to oldest (smaller milli)
                long hours = new Duration(nextMilli, prevMilli).getStandardHours();
                if (hours > 1)
                    missingHours += hours - 1;
                prevMilli = nextMilli;
            }
        }
        cursor.moveToFirst();
    }

    @Override
    public int getCount() {
        return (int)(cursor.getCount() + missingHours);
    }

    @Override
    public boolean moveToPosition(int position) {
        curVPos = position;

        // Make sure position isn't past the end of the cursor
        final int count = getCount();
        if (position >= count) {
            realPos = cursor.getCount();
            virtPos = count;
            return false;
        }

        // Make sure position isn't before the beginning of the cursor
        if (position < 0) {
            realPos = virtPos = -1;
            return false;
        }

        boolean moved = true;

        // When moving to an empty position, just pretend we did it
        // boolean moved = realPosition == -1 ? true : super.moveToPosition(realPosition);
        if (position > virtPos) {
            // Move forward
            if (virtPos == -1 && (moved = moveToFirst()))  {
                realMilli = cursor.getLong(colMilli);
                realPos = virtPos = 0;
            }
            while (position > virtPos ) {
                moved = moveToNext();
                if (moved) {
                    realPos++;
                    long nextMilli = cursor.getLong(colMilli);
                    long hours = new Duration(nextMilli, realMilli).getStandardHours();
                    virtPos += hours;
                    realMilli = nextMilli;
                }
            }
        } else if (position < virtPos) {
            while (position < virtPos ) {
                moved = moveToPrevious();
                if (moved) {
                    realPos--;
                    long nextMilli = cursor.getLong(colMilli);
                    long hours = new Duration(nextMilli, realMilli).getStandardHours();
                    virtPos += hours;
                    realMilli = nextMilli;
                }
            }
        }
        return moved;
    }

    @Override
    public boolean isNull(int columnIndex) {
        return (virtPos != curVPos) || super.isNull(columnIndex);
    }

    @Override
    public long getLong(int columnIndex) {
        if (columnIndex == colMilli && isNull(columnIndex)) {
            // realMilli  at curVPos;
            return realMilli + TimeUnit.HOURS.toMillis(virtPos - curVPos);
        }
        return super.getLong(columnIndex);
    }
}
