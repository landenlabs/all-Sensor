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

import androidx.annotation.IntRange;
import androidx.lifecycle.ViewModel;

import org.joda.time.DateTime;

/**
 * Live view model
 */
public class WxViewModel extends ViewModel {

    public static final int NONE_MSG = 0;
    public static final int STATUS_MSG = 1;
    public static final int SERIES_MSG = 2;
    public static final int LOAD_START_MSG = 3;
    public static final int LOAD_SUMMARY_MSG = 4;
    public static final int LOAD_DATA_MSG = 5;
    public static final int LOAD_DONE_MSG = 6;
    public static final int LOAD_STOP_MSG = 7;
    public static final int FAILED_SUMMARY_MSG = 8;
    public static final int FAILED_LOGIN_MSG = 9;

    /*
    private final MutableLiveData<Progress> progress;
    private final MutableLiveData<Integer> status;
     */
    private final LiveQueue<Progress> progress;
    private final LiveQueue<Integer> status;

    public WxViewModel() {
        status = new LiveQueue<>();
        progress = new LiveQueue<>();
    }

    // ---------------------------------------------------------------------------------------------


    public LiveQueue<Integer> getStatus() {
        return status;
    }

    public void setStatus(Integer value) {
        status.postValue(value);
    }

    // ---------------------------------------------------------------------------------------------
    public static class Progress {
        public final DateTime dt;
        public final long size;
        public final int progress;

        public Progress(long milli, long size, @IntRange(from = 0, to = 100) int progress) {
            this.dt = new DateTime(milli);
            this.size = size;
            this.progress = progress;
        }
    }

    public LiveQueue<Progress> getProgress() {
        return progress;
    }

    public void setProgress(long milli, int size, @IntRange(from = 0, to = 100) int percent) {
        Progress progress = new Progress(milli, size, percent);
        this.progress.postValue(progress);
    }
}
