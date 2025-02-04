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

import static com.landenlabs.all_sensor.logger.ALog.isUnitTest;
import static com.landenlabs.all_sensor.utils.StrUtils.joinStrings;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Interface which defines Log println and open methods.
 */

public class ALogOut {

    public Context context = null;
    public LogPrinter outPrn = new SysLog();

    public interface LogPrinter {
        int MAX_TAG_LEN = 0;    // was 100, but lets move user tag into message body
        int MAX_TAG_LEN_API24 = 0;  // was 23

        void println(int priority, String tag, Object... msgs);

        void open(@NonNull Context context);

        int maxTagLen();
    }

    // =============================================================================================
    public static class SysLog implements LogPrinter {

        // IllegalArgumentException	is thrown if the tag.length() > 23
        // for Nougat (7.0) releases (API <= 23) and prior, there is
        // no tag limit of concern after this API level.
        static final int LOG_TAG_LEN = (Build.VERSION.SDK_INT >= 24) ? MAX_TAG_LEN_API24 : MAX_TAG_LEN;

        public void println(int priority, String tag, Object... msgs) {
            Context context = null;
            if (isUnitTest()) {
                System.out.println(tag + joinStrings(context, msgs));
            } else {
                Log.println(priority, tag, joinStrings(context, msgs));
            }
        }

        public void open(@NonNull Context context) {
        }

        public int maxTagLen() {
            return LOG_TAG_LEN;
        }
    }
}
