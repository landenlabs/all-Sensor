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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;

@SuppressWarnings("FieldCanBeLocal")
public class NetUtils {

    private static OkHttpClient OK_HTTP_CLIENT;
    private static OkHttpClient.Builder OK_HTTP_BUILDER;
    private static final boolean ADD_INTERCEPTOR = false;

    // ---------------------------------------------------------------------------------------------

    synchronized
    public static OkHttpClient.Builder getOkHTTPBuilder() {
        if (OK_HTTP_BUILDER == null) {
            OK_HTTP_BUILDER = new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT))
            ;

            if (ADD_INTERCEPTOR) {
                // Measure network bandwidth from TileServer

                // Also logs all URL messages - good for debugging.
                OK_HTTP_BUILDER.addInterceptor(new NetOkInterceptor(null, null));
            }
        }
        return OK_HTTP_BUILDER;
    }

    synchronized
    public static OkHttpClient getOkHttpClient() {
        if (OK_HTTP_CLIENT == null) {
            OK_HTTP_CLIENT = getOkHTTPBuilder().build();
        }
        return OK_HTTP_CLIENT;
    }
}
