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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.landenlabs.all_sensor.logger.ALog;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import java9.util.concurrent.CompletableFuture;

/**
 * Wrapper class to manage asynchronous execution state.
 */
public class ExecState {
    public static final String DONE = "done";
    public static final String FAIL = "fail";

    public CompletableFuture<String> complete = new CompletableFuture<>();
    public Throwable exception = null;
    private final String name;

    // ---------------------------------------------------------------------------------------------

    public ExecState(String name) {
        this.name = name;
        newComplete();
    }

    @NonNull
    @Override
    public String toString() {
        return ALog.tagStr(this) + (isActive() ? " ACTIVE" : " inActive") + " complete=" + complete.toString();
    }

    public void reset() {
        if (!isActive()) {
            ALog.i.tagMsgStack(this, 6, "RESET ", name);
            // complete.cancel(true);
            newComplete();
            exception = null;
        }
    }

    private void newComplete() {
        complete = new CompletableFuture<String>().handle((msg, ex) -> {
            ALog.w.tagMsgStack(this, 6, "HANDLE ", ex);
            if (ex != null) {
                return "Error=" + ex.getMessage();
            } else {
                return msg;
            }
        });
    }

    public String get() {
        try {
            // retrieving result after it was canceled throws CancellationException.
            return complete.get();
        } catch (CancellationException | InterruptedException | ExecutionException ex) {
            return FAIL;
        }
    }

    public String getNow(String def) {
        try {
            // retrieving result after it was canceled throws CancellationException.
            return complete.getNow(def);
        } catch (CancellationException ex) {
            return FAIL;
        }
    }

    public void done() {
        complete.complete(DONE);
    }

    public void fail(@Nullable Throwable tr) {
        chainError(tr);
        if (isActive()) {
            if (!(tr instanceof CancellationException)) {
                ALog.w.tagMsgStack(this, 0, "fail ", tr);
            }
            complete.cancel(true);
        }
    }

    public void failIf(@Nullable Exception ex) {
        if (ex != null) {
            fail(ex);
        }
    }

    public void chainError(@Nullable Throwable tr) {
        if (tr != null) {
            if (this.exception != null) {
                tr.addSuppressed(this.exception);
            }
            this.exception = tr;
        }
    }

    public boolean ok2Run() {
        boolean isCancelled = complete.isCancelled();
        if (isCancelled) {
            ALog.i.tagMsg("Status", "ok2Run ", complete);
        }
        return !isCancelled;
    }

    public static String combine(String... results) {
        ALog.d.tagMsg("Status", "combined ", results);
        // return TextUtils.join(",", results);
        for (String str : results) {
            if (!str.equals(DONE)) {
                return FAIL;
            }
        }
        return DONE;
    }

    public boolean isActive() {
        // return TextUtils.isEmpty(complete.getNow(null))
        return !complete.isCancelled()
                && !complete.isCompletedExceptionally()
                && !complete.isDone();
    }

    public void cancelIfActive() {
        if (isActive()) {
            complete.cancel(true);
        }
    }


    // =============================================================================================
    public interface Exec {
        void start(IwxManager manager, ExecState startState);

        void stop(IwxManager manager);

        ExecState state();
    }

}