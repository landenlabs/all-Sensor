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

package com.landenlabs.all_sensor.ui.view;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class ProgressLiveModel {
    public final MutableLiveData<ProgessValue> liveProgress;

    public ProgressLiveModel() {
        liveProgress = new MutableLiveData<>();
    }

    public void postValue(ProgessValue progessValue) {
        liveProgress.postValue(progessValue);
    }

    public LiveData<ProgessValue> get() {
        return liveProgress;
    }

    public void update(int atPos, int maxPos, int channel) {
        postValue(new ProgessValue(atPos, maxPos, channel));
    }

    public static class ProgessValue {
        public final int atPos;
        public final int maxPos;
        public final int channel;

        public ProgessValue(int atPos, int maxPos, int channel) {
            this.atPos = atPos;
            this.maxPos = maxPos;
            this.channel = channel;
        }
    }
}
