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

import android.os.Build;
import android.os.StrictMode;

public class StrictModeUtil {

    public static void setStrictMode(boolean enable) {
        if (enable) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    // .detectDiskReads()       // SharedPreferences does read
                    .detectDiskWrites()
                    .detectNetwork()
                    // .detectCustomSlowCalls()  // DoubleClick.loadad()  are slow spawning thread.
                    // .detectAll()
                    // .permitDiskWrites()        // policy #1
                    // .permitDiskReads()         // policy #2
                    // .permitCustomSlowCalls()   // policy #8
                    .penaltyLog()
                    // .penaltyDeath()
                    .build());

            StrictMode.VmPolicy.Builder vmBuilder =
                    new StrictMode.VmPolicy.Builder()
                            // .detectAll()       // See [Begin]...[End] code below
                            //.penaltyDeath()
                            .penaltyLog();

            // [BEGIN] - emulate .detectAll() but ignore Socket Tagging
            vmBuilder.detectLeakedSqlLiteObjects();


            final int targetSdk = Build.VERSION.SDK_INT;
            if (targetSdk >= Build.VERSION_CODES.HONEYCOMB) {
                vmBuilder.detectActivityLeaks();
                vmBuilder.detectLeakedClosableObjects();
            }
            if (targetSdk >= Build.VERSION_CODES.JELLY_BEAN) {
                vmBuilder.detectLeakedRegistrationObjects();
            }
            if (targetSdk >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                vmBuilder.detectFileUriExposure();
            }
            if (targetSdk >= Build.VERSION_CODES.M) {
                vmBuilder.detectCleartextNetwork();
            }
            if (targetSdk >= Build.VERSION_CODES.O) {
                vmBuilder.detectContentUriWithoutPermission();
                // vmBuilder.detectUntaggedSockets();   // okHTTP does not tag sockets, see interceptor
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                vmBuilder.detectNonSdkApiUsage();
            }

            // [END] - emulate .detectAll() but ignore Socket Tagging

            StrictMode.setVmPolicy(vmBuilder.build());
        }
    }
}
