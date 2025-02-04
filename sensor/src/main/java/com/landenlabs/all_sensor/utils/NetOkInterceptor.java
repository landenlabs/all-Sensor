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
import android.net.TrafficStats;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.landenlabs.all_sensor.logger.ALog;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.connection.Exchange;
import okhttp3.internal.connection.RealConnection;

/**
 * Monitor okHTTP network speed.
 */
public class NetOkInterceptor implements Interceptor {

    // private static final int KB = 1 << 10;
    private static final int MB = 1 << 20;
    // private static final int GB = 1 << 30;

    private static final long NEXT_WALL_NANO = TimeUnit.MINUTES.toNanos(30);
    private static final long NEXT_USEC = TimeUnit.MINUTES.toMicros(1);

    private static double prevMBps = 0;
    private final String filterUrl; // "TileServer"
    private long totalBytes = 0;
    private long totalUsec = 0;
    private long nextWallNano = 0;

    // ---------------------------------------------------------------------------------------------

    NetOkInterceptor(@Nullable Context context, @Nullable String filterUrl) {
        this.filterUrl = filterUrl;
    }

    public static double getMBps() {
        return prevMBps;
    }

    @NonNull
    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {

        Request request = chain.request();

        ALog.d.tagMsg("okHTTP", "Request ", request);

        long t1 = System.nanoTime();
        long rdBytes1 = TrafficStats.getUidRxBytes(android.os.Process.myUid());
        //  request.url(), chain.connection(), request.headers()));

        Response response = chain.proceed(request);

        if (false) {
            if (response.code() == 200) {
                long t2 = System.nanoTime();
                long rdBytes2 = TrafficStats.getUidRxBytes(android.os.Process.myUid());

                // long length = response.body().contentLength();
                long length = rdBytes2 - rdBytes1;

                if (length > 0 &&
                        (StrUtils.isEmpty(filterUrl) || request.url().toString().contains(filterUrl))) {

                    if (totalBytes + length < totalBytes || totalUsec > NEXT_USEC || t2 > nextWallNano) {
                        if (totalUsec > 0) {
                            double MBps = totalBytes / (totalUsec / 1e6) / MB;
                            prevMBps = (prevMBps == 0) ? MBps : (MBps + prevMBps) / 2;
                            // Log.d("TWC", " network speed Avg Mbps=" + (prevMBps *8));
                        }
                        nextWallNano = t2 + NEXT_WALL_NANO;
                        totalUsec = 0;
                        totalBytes = 0;
                    }

                    totalBytes += length;
                    totalUsec += (t2 - t1) / 1e3;   //  convert nano to micro seconds

                    // Log.d("TWC", " network speed  Mbps=" +((totalBytes / (totalUsec / 1e6) / MB) *8) +
                    //        " totalU=" + totalUsec + " totB=" + totalBytes);
                }
            }
        }

        long duration = response.receivedResponseAtMillis() - response.sentRequestAtMillis();

        if (response.isSuccessful()) {
            long length = -1;

            /*
            // BAD - this consumes the responsen (unzip's compressed body)
            if (response.body() != null) {
                try {
                    MediaType contentType = response.body().contentType();
                    String bodyString = response.body().string();
                    response.body().close();
                    length = bodyString.length();
                    response = response.newBuilder().body(ResponseBody.create(contentType, bodyString)).build();
                } catch (Exception ignore) {
                }
            }
             */

            // Extract Ip from okHTTP response.
            String ipStr = "";
            try {
                Field getExchange = Response.class.getDeclaredField("exchange");
                getExchange.setAccessible(true);
                Exchange exchange = (Exchange) getExchange.get(response);

                // ipStr = exchange.connection().socket().getInetAddress().getHostAddress();
                // Use reflection to call "connection()
                Method connectionMethod = exchange.getClass().getDeclaredMethod("connection");
                connectionMethod.setAccessible(true);
                ipStr = ((RealConnection)connectionMethod.invoke(exchange)).socket().getInetAddress().getHostAddress();
            } catch (Exception ex) {
                ALog.e.tagMsg(this, ex);
            }

            ALog.d.tagMsg("okHTTP", "Response code=", response.code()
                    , " ", response.protocol()
                    , " HS=", response.handshake()
                    , " IP=", ipStr
                    , " url=", request.url()
                    , ", Duration=" + duration);
        } else {
            ALog.w.tagMsg("okHTTP", "Error code=", response.code()
                    , " url=", request.url()
                    , ", Duration=" + duration);
        }

        return response;
    }
}