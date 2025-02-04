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

import android.location.Location;

import androidx.annotation.NonNull;

import com.landenlabs.all_sensor.utils.ArrayListEx;

import org.joda.time.DateTime;

public class SensorAndroid {

    public static class SensorSample {
        public final long milli;
        public final float fvalue;

        public SensorSample(long milli, float fvalue) {
            this.milli = milli;
            this.fvalue = fvalue;
        }

        public static SensorSample getNear( ArrayListEx<SensorSample> samples, long milli, SensorSample defVal) {
            SensorAndroid.SensorSample sample;
            long minMilli = Long.MAX_VALUE;
            int minIdx = 0;
            for (int idx = 0; idx < samples.size(); idx++) {
                sample = samples.get(idx, defVal);
                DateTime sampleDt = new DateTime(sample.milli);
                long deltaMilli = Math.abs(milli - sampleDt.getMillis());
                if (deltaMilli < minMilli) {
                    minMilli = deltaMilli;
                    minIdx = idx;
                } else if (deltaMilli > minMilli) {
                    break;
                }
            }

            return samples.get(minIdx, defVal);
        }
    }

    public static class Pressure extends SensorSample {
        public Pressure(long milli, float mb) {
            super(milli, mb);
        }
        public static final Pressure EMPTY = new Pressure(0L, 0);
    }

    public static class GpsLocation extends SensorSample {
        public final Location location;
        public GpsLocation() {
            super(0, Float.NaN);
            location = null;
        }
        public GpsLocation(long milli, @NonNull Location location) {
            super(milli, location.getAccuracy());
            this.location = location;
        }
        public static final GpsLocation EMPTY = new GpsLocation();
    }
}
