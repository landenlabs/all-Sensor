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


import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.landenlabs.all_sensor.R;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Save crash report and show it on next run of app.
 *
 * @author Dennis Lang (LanDen Labs)
 * @see <a href="https://landenlabs.com/android"> author's web-site </a>
 */
@SuppressWarnings({"Convert2Lambda", "StringBufferReplaceableByString"})
public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final String CRASH_REPORT = "CrashReport";
    private final Context context;
    private final SharedPreferences prefs;
    private final Thread.UncaughtExceptionHandler originalHandler;

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a reporter instance
     *
     * @throws NullPointerException if the parameter is null
     */
    public UncaughtExceptionHandler(Context context) throws NullPointerException {
        originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);

        this.context = context;

        StrictMode.ThreadPolicy policy = StrictMode.allowThreadDiskReads();
        StrictMode.allowThreadDiskWrites();
        prefs = context.getSharedPreferences(CRASH_REPORT, Context.MODE_PRIVATE);
        String msg = prefs.getString(CRASH_REPORT, "");

        if (StrUtils.hasText(msg)) {
            prefs.edit().remove(CRASH_REPORT).apply();
            showCrashDialog(msg);
        }
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {

        String stackTrace = Log.getStackTraceString(ex);
        Log.e("AllThreadPenalty", "Exception", ex);

        saveCrashReport(thread, ex);

        if (originalHandler != null) {
            originalHandler.uncaughtException(thread, ex);
        }
    }

    /**
     * Save stack trace in preferences to display on next app run.
     */
    private void saveCrashReport(Thread thread, Throwable ex) {
        try {
            StringBuilder report = new StringBuilder();
            report.append("Thread:")
                    .append(thread.getName())
                    .append("@")
                    .append(thread.getId())
                    .append("\n");
            report.append("Exception:\n")
                    .append(ex.getMessage())
                    .append("\nCause\n")
                    .append(ex.getCause())
                    .append("\n");
            report.append("Stack:\n");
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            ex.printStackTrace(printWriter);
            report.append(result);
            printWriter.close();
            report.append('\n');

            StrictMode.ThreadPolicy policy = StrictMode.allowThreadDiskWrites();
            prefs.edit().putString(CRASH_REPORT, report.toString()).apply();
            StrictMode.setThreadPolicy(policy);
        } catch (Throwable ignore) {
        }
    }

    /**
     * Show crash trace in Alert dialog, pause main_menu app from running.
     */
    private void showCrashDialog(final String msg) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setView(R.layout.crash_dlg);
        AlertDialog dialog = builder.show();

        ((TextView) dialog.findViewById(R.id.crash_text)).setText(msg);

        dialog.findViewById(R.id.crash_cont_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        System.exit(0);
                    }
                }
        );

        dialog.findViewById(R.id.crash_email_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent sendIntent = new Intent(Intent.ACTION_SEND);
                        StringBuilder body = new StringBuilder("Trace\n\n")
                                .append(msg);
                        sendIntent.setType("message/rfc822");
                        sendIntent.putExtra(Intent.EXTRA_TEXT, body.toString());
                        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Crash report");
                        sendIntent.setType("message/rfc822");
                        context.startActivity(sendIntent);
                        System.exit(0);
                    }
                }
        );

        Looper.loop();
    }
}
