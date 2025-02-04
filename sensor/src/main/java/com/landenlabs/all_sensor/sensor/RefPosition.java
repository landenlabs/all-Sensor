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

package com.landenlabs.all_sensor.sensor;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.concurrent.TimeUnit;

public class RefPosition {
    public long index;
    public long milli;

    public RefPosition(long index, long milli) {
        this.index = index;
        this.milli = milli;

    }
    public DateTime dt() {
        return new DateTime(milli);
    }

    private static final String KEY_INDEX = "index";
    private static final String KEY_MILLI = "milli";

    public  static boolean load(
            @NonNull SharedPreferences pref,
            @NonNull String tag,
            @NonNull Interval interval,
            RefPosition refPos) {
        long index = pref.getLong(tag+KEY_INDEX, -1);
        long milli = pref.getLong(tag+KEY_MILLI, -1);
        if (index != -1 && milli > 0) {
            long diffMilli = interval.getStartMillis() - milli;
            if (diffMilli < TimeUnit.DAYS.toMillis(30)) {
                refPos.index = index;
                refPos.milli = milli;
                return true;
            }
        }
        return false;
    }

    public void save(
            @NonNull SharedPreferences pref,
            @NonNull String tag,
            boolean reset) {
        pref.edit()
                .putLong(tag+KEY_INDEX, reset ? -1 : index)
                .putLong(tag+KEY_MILLI, reset ? -1 : milli)
                .apply();
    }
}
