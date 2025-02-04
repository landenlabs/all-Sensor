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

package com.landenlabs.all_sensor;

import androidx.annotation.AnyRes;

/**
 * Collection of Units
 */
public class Units {
    public @AnyRes static final int INVALID_RES_ID = 0;  // API 25+   Resources.ID_NULL

    public enum Duration {Days, Months}

    public enum SampleBy {Hours, Days}

    public enum Sensors {Temperature, Humidity, Pressure, Light, Gyro, Sound }

    public enum Temperature {
        Fahrenheit("F"), Celsius("C");

        Temperature(String id) {
            this.id = id;
        }

        String id() {
            return this.id;
        }

        public final String id;
    }

    public enum Pressure {
        mb("mb");

        Pressure(String id) {
            this.id = id;
        }

        String id() {
            return this.id;
        }

        public final String id;
    }

    public enum Percent {
        percent("%");

        Percent(String id) {
            this.id = id;
        }

        String id() {
            return this.id;
        }

        public final String id;
    }
}
