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
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;


@SuppressWarnings("unused")
public final class Util {
    private static final int Max = 64 * 1024;

    public static String httpGetString(String url) throws IOException {
        byte[] b = httpGet(url);
        if (b == null) {
            return null;
        }
        return new String(b);
    }

    static byte[] httpGet(String url) throws IOException {
        URL u = new URL(url);
        HttpURLConnection httpConn = (HttpURLConnection) u.openConnection();
        httpConn.setConnectTimeout(10000);
        httpConn.setReadTimeout(20000);
        int responseCode = httpConn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            return null;
        }

        int length = httpConn.getContentLength();
        if (length < 0) {
            length = Max;
        }
        if (length > Max) {
            return null;
        }
        InputStream is = httpConn.getInputStream();
        byte[] data = new byte[Max];
        int read = is.read(data);
        is.close();
        if (read <= 0) {
            return null;
        }
        if (read < data.length) {
            byte[] b = new byte[read];
            System.arraycopy(data, 0, b, 0, read);
            return b;
        }
        return data;
    }

    public static void runInMain(Runnable run) {
        Handler handle = new Handler(Looper.getMainLooper());
        handle.post(run);
    }

    public static void runInBack(Runnable run) {
        // AsyncTask.Status
        Executors.newSingleThreadExecutor().execute(run);
        // AsyncTask.THREAD_POOL_EXECUTOR.execute(run);
        // AsyncTask.execute(run);
    }

    public static void append(StringBuilder sb, int cnt, char c) {
        while (cnt-- > 0)
            sb.append(c);
    }

    public static String join(String sep, Object... values) {
        StringBuilder sb = new StringBuilder();
        for (Object value : values) {
            if (value != null) {
                if (sb.length() > 0)
                    sb.append(sep);
                if (value instanceof Object[]) {
                    sb.append(join(sep, value));
                } else {
                    sb.append(value);
                }
            }
        }
        return sb.toString();
    }

    public static String getMessage(@NonNull String separator, @Nullable Throwable ex) {
        if (ex == null)
            return "";
        return join(separator, ex.getClass().getSimpleName(), ex.getMessage(), getMessage(separator, ex.getCause()));
    }

    /*
    public static String niceString(@Nullable Object obj) {
        if (obj instanceof Throwable) {
            Throwable tr = (Throwable) obj;
            return tr.getClass().getSimpleName()
                    + " " + tr.getLocalizedMessage()
                    + (tr.getCause() != null ? " Cause=" + niceString(tr.getCause()) : "");
        } else if (obj != null) {
            return obj.toString();
        }
        return "";
    }
     */
    @NonNull
    public static CharSequence niceString(@Nullable Object obj) {
        if (obj instanceof Throwable) {
            Throwable tr = (Throwable) obj;

            // Get message parts.
            String name = tr.getClass().getSimpleName();
            String error = " " + tr.getLocalizedMessage();
            boolean hasCause = (tr.getCause() != null && (tr.getCause() != tr));
            String causeLbl = hasCause ? "\n Cause=" : "";

            // Colorize
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            SpannableString span = new SpannableString(name + error + causeLbl);
            span.setSpan(new StyleSpan(Typeface.BOLD), 0, name.length(), 0);
            if (hasCause) {
                int start = name.length() + error.length();
                span.setSpan(new ForegroundColorSpan(Color.RED), start, start + causeLbl.length(), 0);
                span.setSpan(new StyleSpan(Typeface.BOLD), start, start + causeLbl.length(), 0);
                ssb.append(span);
                ssb.append(niceString(tr.getCause()));
                return ssb;
            }
            return span;

        } else if (obj instanceof CharSequence) {
            return (CharSequence) obj;
        } else if (obj != null) {
            return obj.toString();
        }
        return "";
    }

}
