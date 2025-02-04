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

import android.content.Context;
import android.content.res.Resources;
import android.text.SpannableString;
import android.text.SpannedString;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.gms.tasks.Task;

import java.util.Arrays;
import java.util.List;

public class StrUtils {

    @StringRes
    public static final int NO_STRING_RES = 0;  // API 25+   Resources.ID_NULL;
    public static final String NULL_STR = "(null)";

    // ---------------------------------------------------------------------------------------------

    public static String getString(@Nullable Context context, @Nullable Object value) {
        if (value == null) {
            return NULL_STR;
        } else if (value instanceof String
                || value instanceof StringBuilder
                || value instanceof SpannedString
                || value instanceof SpannableString) {
            return value.toString();
        } else if (value instanceof Integer && context != null) {
            try {
                return ((Integer) value == NO_STRING_RES) ? "" : context.getString((Integer) value);
            } catch (Resources.NotFoundException ex) {
                return String.valueOf(value);
            }
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Object[]) {
            return joinArray(context, (Object[]) value);
        } else if (value.getClass().isArray()) {
            String typeName = value.getClass().getSimpleName();
            switch (typeName) {
                case "int[]":
                    return Arrays.toString((int[]) value);
                case "long[]":
                    return Arrays.toString((long[]) value);
                case "float[]":
                    return Arrays.toString((float[]) value);
                case "double[]":
                    return Arrays.toString((double[]) value);
            }
            return joinStrings(context, value.getClass().getSimpleName(), value.toString());
        } else if (value instanceof List) {
            return String.format("List[%d]", ((List<?>)value).size()) + joinStrings("\n", ((List<?>) value).toArray());
        } else {
            // throw new UnsupportedOperationException("unknown getString type " + value.getClass().getSimpleName() + value);
            return joinStrings(context, value.getClass().getSimpleName(), value.toString());
        }
    }

    public static String joinStrings(Context context, @NonNull Object... values) {
        StringBuilder sb = new StringBuilder();
        for (Object value : values) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(getString(context, value));
        }
        return sb.toString();
    }

    public static String joinArray(Context context, @NonNull Object[] values) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s[%d]", values.getClass().getSimpleName(), values.length));
        for (Object value : values) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(getString(context, value));
        }
        return sb.toString();
    }

    public static String joinStrings(String sep, @NonNull Object... values) {
        StringBuilder sb = new StringBuilder();
        for (Object value : values) {
            String valueStr = getString(null, value);
            if (StrUtils.hasText(valueStr) && !NULL_STR.equals(valueStr)) {
                if (sb.length() > 0)
                    sb.append(sep);
                sb.append(valueStr);
            }
        }
        return sb.toString();
    }

    public static boolean isEmpty(@Nullable CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static boolean hasText(@Nullable CharSequence str) {
        return str != null && str.length() > 0;
    }
    public static String asString(@Nullable Object obj, @Nullable String def) {
        return (obj != null) ? obj.toString() : def;
    }
    public static String asString(@Nullable Object obj) {
        return getString(null, obj);
    }

    public static String toString(Task<Void> task) {
        StringBuilder sb = new StringBuilder();
        if (task.isCanceled())
            sb.append("Canceled ");
        if (task.isComplete())
            sb.append("Complete ");
        if (task.isSuccessful())
            sb.append("Successful ");
        if (task.getException() != null)
            sb.append(task.getException().toString());
        return sb.toString();
    }
}

