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

package com.landenlabs.all_sensor.utils;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;

import androidx.annotation.NonNull;

public class SpanUtil {
    public final static int SS_NONE = 0;
    public final static int SS_BOLD = 1 << 1;
    public final static int SS_BIG1 = 1 << 2;
    public final static int SS_RED = 1 << 3;
    public final static int SS_BLUE = 1 << 4;
    public final static int SS_SUPER = 1 << 5;
    public final static int SS_SMALLER = 1 << 6;
    public final static int SS_SMALLER_50 = 1 << 6;
    public final static int SS_SMALLER_80 = 1 << 7;
    public final static int SS_GREEN = 1 << 8;
    private final static int SS_LAST = SS_GREEN; // Last style

    private final static int COLOR_GPS_GREEN = 0xff008080; // R.color.gpsColor

    // ---------------------------------------------------------------------------------------------

    public static CharSequence SSJoin(CharSequence... csList) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        for (CharSequence cs : csList) {
            ssb.append(cs);
        }
        return ssb;
    }

    public static SpannableString SSBold(String str) {
        return SString(str, SS_BOLD);
    }

    public static SpannableString SSRed(String str) {
        return SString(str, SS_RED);
    }

    public static SpannableString SSBlue(String str) {
        return SString(str, SS_BLUE);
    }

    public static SpannableString SString(String str, int what) {
        return SString(str, what, 0, str.length());
    }

    public static SpannableString SString(String str, int what, int beg, int end) {
        SpannableString ss = new SpannableString(str);
        end = (end > 0) ? end : str.length();
        int style = 1;
        while (style <= SS_LAST) {
            if ((what & style) != 0) {
                ss.setSpan(getWhat(style), beg, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            style <<= 1;
        }

        // ss.setSpan(getWhat(SS_BIG1), beg, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ss;
    }

    @NonNull
    static Object getWhat(int what) {
        switch (what) {
            default:
            case SS_BOLD:
                return new StyleSpan(Typeface.BOLD);
            case SS_BIG1:
                return new RelativeSizeSpan(1.2f);
            case SS_RED:
                return new ForegroundColorSpan(Color.RED);
            case SS_BLUE:
                return new ForegroundColorSpan(Color.BLUE);
            case SS_GREEN:
                return new ForegroundColorSpan(COLOR_GPS_GREEN);
            case SS_SUPER:
                return new SuperscriptSpan();
            case SS_SMALLER_50: // SS_SMALLER
                return new RelativeSizeSpan(0.5f);
            case SS_SMALLER_80:
                return new RelativeSizeSpan(0.8f);
        }
    }

}
