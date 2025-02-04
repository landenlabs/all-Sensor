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

import static com.landenlabs.all_sensor.utils.StrUtils.hasText;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.landenlabs.all_sensor.db.DbDeviceDaily;
import com.landenlabs.all_sensor.db.DbDeviceHourly;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.utils.ArrayListEx;
import com.landenlabs.all_sensor.utils.NetUtils;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;
import org.joda.time.Minutes;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DeviceGoveeHelper {

    private static final String TAG = "DeviceGoveeHelper";

    /*
    {
      "message": "Login successful",
      "status": 200,
      "client": {
            "A": "testiot.cert",
            "B": "testIot",
            "topic": "GA/33b0ce1f583d7260ae1a17803be4e8ea",
            "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkYXRhIjp7InNpZCI6Imt5VGoxTUQwSm44WFhVbWtWaTQycVIyWDJBZncycjFqIn0sImlhdCI6MTYwMjcyNzIxMCwiZXhwIjoxNjA3OTExMjEwfQ.BOgQjg58uv9wE7g2GmCNWugvIKYTYKvRAYE6nbTVKnY",
            "refreshToken": "",
            "tokenExpireCycle": 57600,
            "client": "e6e7d9e73f5058a5",
            "clientName": "Android Android SDK built for x8",
            "accountId": 1005658,
            "pushToken": "fgJdd74oQy2aGlGxdMEqS3:APA91bHoVeV4mXM-H4P-4hpSqZDkdnU-dRHClSBvfGDm4Rrzi1iBT2C9G3R_rPkU6-ZDsQx-4Ve_FbHHVFQtF5Bhvs9tJ32rofu4y6S_lP7wJ1vtNJgwaARniB-WJz2ubOuQ42hxU2Cr",
            "versionCode": "121.0",
            "versionName": "3.6.0",
            "sysVersion": "6.0"
       }
    }
     */
    public JSONObject loginJson;
    public Exception loginException;
    public int loginStatus;
    public String loginMessage;
    public Client client;

    public String devSummaryStr;
    public Exception devSummaryException;
    @Nullable
    public ArrayListEx<Device> devices = null;

    public Exception devDataException;

    // ---------------------------------------------------------------------------------------------
    public DeviceGoveeHelper() {
        ALog.d.tagMsg(this, "ctor");
    }

    public boolean parseLogin() {
        Client newClient = new Client();
        try {
            JSONObject clientJson = loginJson.getJSONObject("client");
            newClient.tokenExpireCycle = clientJson.optInt("tokenExpireCycle");
            newClient.token = clientJson.getString("token");
            newClient.refreshToken = clientJson.optString("refreshToken");
            newClient.pushToken = clientJson.getString("pushToken");

            newClient.client = clientJson.getString("client");
            newClient.accountId = clientJson.getString("accountId");

            newClient.versionCode = clientJson.optString("versionCode");
            newClient.versionName = clientJson.optString("versionName");
            newClient.sysVersion = clientJson.optString("sysVersion");
            client = newClient;
            return true;
        } catch (JSONException ex) {
            // {"status":400,"message":"Your account is abnormal, please contact customer service."}
            ALog.e.tagMsg("DeviceGoveeHelper", "parse login client ", ex);
        }
        return false;
    }

    public boolean parseDeviceSummary(@NonNull Context context, @NonNull DeviceAccount account) {
        /*
        {
          "devices": [
            {
              "device": "7ca6b00aa141002970",
              "sku": "H5053",
              "spec": "",
              "versionHard": "1.00.01",
              "versionSoft": "1.00.06",
              "deviceName": "temp 1",
              "deviceExt": {
                "deviceSettings": "{\"temMin\":0,\"temMax\":2666,\"temWarning\":true,\"fahOpen\":false,\"temCali\":0,\"humMin\":2000,\"humMax\":9700,\"humWarning\":true,\"humCali\":0,\"netWaring\":true,\"uploadRate\":10,\"battery\":60,\"wifiLevel\":0,\"header\":\"1\",\"gatewayVersionHard\":\"1.00.00\",\"gatewayVersionSoft\":\"1.00.27\",\"sku\":\"H5053\",\"device\":\"7ca6b00aa141002970\",\"deviceName\":\"temp 1\",\"versionHard\":\"1.00.01\",\"versionSoft\":\"1.00.06\"}",
                "lastDeviceData": "{\"online\":true,\"gwonline\":true,\"tem\":943,\"hum\":8365,\"lastTime\":1602729300000,\"avgDayTem\":943,\"avgDayHum\":8365}",
                "extResources": "{\"skuUrl\":\"\",\"headOnImg\":\"\",\"headOffImg\":\"\",\"ext\":\"\"}"
              },
              "goodsType": 0
            }
          ],
          "message": "",
          "status": 200
        }
         */

        SharedPreferences pref = getPref(context);
        String DEVLIST_KEY = devListKey(account);
        String DEVLIST_TIME = devListTimeKey(account);

        try {
            Gson gson = new GsonBuilder().create();
            JSONObject devListJson = new JSONObject(devSummaryStr);

            String str = devListJson.getJSONArray("devices").toString()
                    .replace("\\", "")
                    .replace("\"{", "{")
                    .replace("}\"", "}");

            devices = gson.fromJson(str, new TypeToken<ArrayListEx<Device>>() {
            }.getType());

            if (devices != null && devices.size() > 0) {
                long milli = getDevice().deviceExt.lastDeviceData.lastTime;

                SimpleDateFormat debugFmt = new SimpleDateFormat("MMM d E hh:mm a z");
                ALog.d.tagMsg(this, "devSummary save cached last=", debugFmt.format(milli));
                ALog.d.tagMsg(this, "devSummary save cached ", devSummaryStr);

                pref.edit().putString(DEVLIST_KEY, devSummaryStr)
                        .putLong(DEVLIST_TIME, milli)
                        .apply();
                return true;
            }
        } catch (Exception ex) {
            ALog.e.tagMsg(this, "devieList ", ex);
        }

        pref.edit().putString(DEVLIST_KEY, "")
                .putLong(DEVLIST_TIME, 0)
                .apply();
        return false;
    }

    /*
    @Nullable
    public SampleSeries parseDataSeries1(JSONObject firstSeries) {
        try {
            Gson gson = new GsonBuilder().create();
            String str = firstSeries.toString();
            return gson.fromJson(str, new TypeToken<SampleSeries>() {
            }.getType());
        } catch (Exception ex) {
            ALog.e.tagMsg(this, "devieList ", ex);
        }
        return null;
    }
     */

    @Nullable
    public SampleSeries parseDataSeries(JSONObject jSeries) {
        try {
            JSONArray jArray = jSeries.getJSONArray("datas");
            if (jArray.length() > 0) {
                SampleSeries sampleSeries = new SampleSeries();

                sampleSeries.index = jSeries.getLong("index");
                sampleSeries.status = jSeries.getInt("status");
                sampleSeries.message = jSeries.getString("message");

                sampleSeries.datas = new ArrayListEx<>(jArray.length());
                for (int idx = 0; idx < jArray.length(); idx++) {
                    JSONObject jItem = jArray.getJSONObject(idx);
                    int tem = jItem.getInt("tem");
                    int hum = jItem.getInt("hum");
                    long milli = jItem.getLong("time");
                    sampleSeries.datas.add(new Sample(tem, hum, milli));
                }
                return sampleSeries;
            }
        } catch (Exception ex) {
            ALog.e.tagMsg(this, "devieList ", ex);
        }
        return null;
    }


    public static float toDegreeF(int celsius100) {
        return celsius100 / 100.0f * 9 / 5.0f + 32;
    }

    public static String toDegreeFStr(int celsius100) {
        return String.format("%.2f °F", toDegreeF(celsius100));
    }

    public static float toDeltaDegreeF(int celsius100) {
        return celsius100 / 100.0f * 9 / 5.0f;
    }

    public static float toPercent(int percent100) {
        return percent100 / 100.0f;
    }

    public static String toHumidityStr(int percent100) {
        return String.format("%.2f%%", percent100 / 100.0);
    }


    // ---------------------------------------------------------------------------------------------

    private static boolean hasJson(JSONObject jsonObject) {
        return jsonObject != null && jsonObject.length() > 0;
    }

    private static SharedPreferences getPref(Context context) {
        return context.getSharedPreferences("Govee", Context.MODE_PRIVATE);
    }

    private static final String PREF_LOGIN = "login";
    private static final String PREF_DEVLIST = "devlist";
    private static final String PREF_DEVLIST_TIME = "devlistTime";

    /**
     * Load and cache login response until load failure.
     */
    public boolean login(@NonNull Context context, @NonNull DeviceAccount account) {
        if (loginException == null && hasJson(loginJson)) {
            ALog.d.tagMsg(this, "login return cached");
            return true;
        }

        loginException = null;
        SharedPreferences pref = getPref(context);
        String loginStr = pref.getString(loginKey(account), "");

        try {
            if (hasText(loginStr)) {
                loginJson = new JSONObject(loginStr);
                ALog.d.tagMsg(this, "login return loaded cached");
                if (parseJsonStatus(loginJson)) {
                    return true;    // Reuse old login.
                }
                loginJson = null;
            }
        } catch (Exception ex) {
            ALog.e.tagMsg(this, "Failed to reload cached govee login");
        }

        boolean okay = false;
        final int RETRY_MAX = 1;    // avoid spamming login which then disables account for 24 hours.
        for (int tryCnt = 0; tryCnt < RETRY_MAX && !okay && loginJson == null; tryCnt++) {
            okay = login(account);
            ALog.d.tagMsg(this, "login=", okay, " try=", tryCnt);
        }

        if (okay && loginJson != null) {
            okay = parseJsonStatus(loginJson);
            loginStr = loginJson.toString();
        }

        if (loginStr == null) {
            okay = false;
            loginStr = "";
        }

        if (okay) {
            ALog.d.tagMsg(this, "login save login= ", loginStr);
            pref.edit().putString(loginKey(account), loginStr).apply();
        }

        return okay;
    }

    private boolean parseJsonStatus(@NonNull JSONObject jobj) {
        loginStatus = jobj.optInt("status");
        loginMessage = jobj.optString("message");
        if (loginStatus != 200) {
            loginException = new LoginException(loginMessage + " Status=" + loginStatus);
        }
        return (loginStatus == 200);
    }

    private boolean login(@NonNull DeviceAccount account) {
        /*
        curl --location --request POST 'https://app2.govee.com/account/rest/account/v1/login' \
        --header 'Accept-Language: en' \
        --header 'User-Agent: okhttp/3.12.0' \
        --header 'Accept-Encoding: gzip' \
        --header 'Content-Type: application/json; charset=UTF-8' \
        --data-raw '{\
            "client": "e6e7d9e73f5058a5",\
            "email": "user",\
            "password": "pwd",\
        }'
         */
        HashMap<String, String> loginHeaders = null;

        String loginPostJson = "{\"client\":\"e6e7d9e73f5058a5\",\"email\":\""
                + account.user + "\",\"password\":\""
                + account.pwd + "\"}";

        try {
            ALog.d.tagMsg(TAG, "load/login started");
            // https://www.baeldung.com/okhttp-post
            Response login = postTo("https://app2.govee.com/account/rest/account/v1/login", loginHeaders, loginPostJson);

            if (login != null && login.code() == 200 && login.body() != null) {
                String jsonStr = login.body().string();
                loginJson = new JSONObject(jsonStr);
                loginException = null;
                ALog.d.tagMsg(TAG, "load/login done");
                return true;
            } else {
                ALog.e.tagMsg(TAG, "load/login failed login=", login);
            }
        } catch (IOException | JSONException ex) {
            ALog.e.tagMsg(TAG, "load/login ", ex);
            loginException = ex;
        }

        return false;
    }

    /**
     * Load and cache DeviceSummary for 2 minutes.
     * <p>
     * Updates:
     * devListException
     * devListStr
     */
    public boolean getDeviceSummary(@NonNull Context context, @NonNull DeviceAccount account) {
        SharedPreferences pref = getPref(context);
        String DEVLIST_KEY = devListKey(account);
        String DEVLIST_TIME = devListTimeKey(account);

        DateTime cacheDT = new DateTime(pref.getLong(DEVLIST_TIME, 0L));
        int cageAgeMin = Minutes.minutesBetween(cacheDT, DateTime.now()).getMinutes();
        if (cageAgeMin < 2) {
            if (devSummaryException == null && hasText(devSummaryStr)) {
                ALog.d.tagMsg(this, "devSummary use cache ageMin=", cageAgeMin);
                return true;
            }

            devSummaryException = null;
            devSummaryStr = pref.getString(DEVLIST_KEY, "");

            try {
                if (hasText(devSummaryStr)) {
                    ALog.d.tagMsg(this, "devSummary loaded cached ");
                    return true;
                }
            } catch (Exception ex) {
                ALog.e.tagMsg(this, "Failed to reload cached govee login");
            }
        }

        boolean okay = getDeviceSummary();
        if (!okay) {
            // Load failed - retry login and devSummary load one more time.
            devSummaryStr = "";

            if (hasText(pref.getString(loginKey(account), ""))) {
                // Hope this does not go into an infinite loop.
                pref.edit().putString(loginKey(account), "").apply();
                ALog.e.tagMsg(this, "retry dev summary");
                loginJson = null;
                return (login(context, account) && parseLogin() && getDeviceSummary());
            }
        }

        return okay;
    }

    private boolean getDeviceSummary() {
        /*
            curl --location --request POST 'https://app2.govee.com/device/rest/devices/v1/list' \
            --header 'clientId: e6e7d9e73f5058a5' \
            --header 'appVersion: 3.6.0' \
            --header 'User-Agent: okhttp/3.12.0' \
            --header 'Accept-Encoding: gzip' \
            --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkYXRhIjp7ImFjY291bnQiOiJ7XCJjbGllbnRcIjpcImU2ZTdkOWU3M2Y1MDU4YTVcIixcInNpZFwiOlwiUERUTlNPSTBPSDFmOU5xRVlYSDNCT01sWDlkRUFNbXJcIixcImFjY291bnRJZFwiOjEwMDU2NTgsXCJlbWFpbFwiOlwibGFuZGVubGFic0BnbWFpbC5jb21cIn0ifSwiaWF0IjoxNjAwODI2ODEyLCJleHAiOjE2MDYwMTA4MTJ9.jgYmGiCKEJ-q_rYMCoXa-3QHZjO_IlWUg1sqJ9O_SpI' \
            --data-raw ''
         */

        HashMap<String, String> devListHeaders = new HashMap<>();
        devListHeaders.put("clientId", client.client);
        devListHeaders.put("appVersion", client.versionName);
        devListHeaders.put("Authorization", "Bearer " + client.token);

        String devListPostJson = "";

        try {
            Response devList = postTo("https://app2.govee.com/device/rest/devices/v1/list", devListHeaders, devListPostJson);

            if (devList != null && devList.code() == 200 && devList.body() != null) {
                devSummaryStr = devList.body().string();
                devSummaryException =  (devSummaryStr.contains("200")) ? null : new LoginException(devSummaryStr);
                return (devSummaryException == null);
            }
        } catch (IOException ex) {
            ALog.e.tagMsg(TAG, "load/devList ", ex);
            devSummaryException = ex;
        }

        devSummaryStr = "";
        return false;
    }

    /**
     * Populates DeviceGoveeHelper.samples with raw data.
     * <p>
     * See avgSamplesToHourly() to convert to hourly average in
     */
    @Nullable
    public IntervalSeries getDeviceData(
            @NonNull Context context,
            @Nullable WxViewModel progressModel,
            @NonNull DeviceGoveeHelper.Device device,
            @NonNull Interval interval) {
        RefPosition refPos = new RefPosition(0, 0);
        SharedPreferences pref = getPref(context);
        if (RefPosition.load(pref, device.getNameSku(), interval, refPos)) {
            IntervalSeries series = getDeviceData(progressModel, device, interval, refPos);
            if (series != null && series.samples.size() > 0) {
                refPos.save(pref, device.getNameSku(), false);
                return series;
            }
        }

        DateTime fromDt = interval.getStart();
        DateTime now = DateTime.now();

        HashMap<String, String> devDataHeaders = new HashMap<>();
        devDataHeaders.put("clientId", client.client);
        devDataHeaders.put("appVersion", client.versionName);
        devDataHeaders.put("Authorization", "Bearer " + client.token);

        JSONObject devDataPostJson = new JSONObject();
        try {

            devDataPostJson.put("device", device.device);
            devDataPostJson.put("index", 0);
            devDataPostJson.put("limit", 20);   // Smallish sample set just to get started
            devDataPostJson.put("sku", device.sku);
            // String devDataPostJson = "{\"client\":\"e6e7d9e73f5058a5\",\"email\":\"user\",\"password\":\"pwd\"}";
            String devDataPostJsonStr = devDataPostJson.toString();

            Response devData = postTo("https://app2.govee.com/th/rest/devices/v1/data/load", devDataHeaders, devDataPostJsonStr);

            if (devData != null && devData.code() == 200 && devData.body() != null) {
                String jsonStr = devData.body().string();
                JSONObject firstSeries = new JSONObject(jsonStr);
                DeviceGoveeHelper.SampleSeries sampleSeries = parseDataSeries(firstSeries);

                if (sampleSeries != null && sampleSeries.status == 200) {
                    // First payload can have garbage, find time closest to now (largest milli).
                    long maxMilli = 0;
                    for (DeviceGoveeHelper.Sample sample : sampleSeries.datas) {
                        maxMilli = Math.max(maxMilli, sample.milli);
                    }
                    devDataException = null;
                    refPos = new RefPosition(sampleSeries.index, maxMilli);
                    IntervalSeries series = getDeviceData(progressModel, device, interval, refPos);
                    refPos.save(pref, device.getNameSku(), series == null || series.samples == null || series.samples.isEmpty());
                    return series;
                }
            }
        } catch (Exception ex) { // IOException | JSONException
            ALog.e.tagMsg(TAG, "load/data ", ex);
            devDataException = ex;
        }
        return null;
    }

    private static final int BACKUP_TRY = 3;
    @Nullable
    IntervalSeries getDeviceData(
            @Nullable WxViewModel progressModel,
            @NonNull DeviceGoveeHelper.Device device,
            Interval interval,
            RefPosition refPos) {
        HashMap<String, String> devDataHeaders = new HashMap<>();
        devDataHeaders.put("clientId", client.client);
        devDataHeaders.put("appVersion", client.versionName);
        devDataHeaders.put("Authorization", "Bearer " + client.token);

        int maxSampleLimit = 60 * 48; // 2 days of minute samples (2880)
        // final int IDX_PER_DAY = (6 * 24 + 12) * 60;  // +20 at 98 days
        final int IDX_PER_DAY = 24 * 60 * 6;    // sample every 10 seconds, 6 per minute
        final long daysToStart = Days.daysBetween(refPos.dt().toLocalDate(), interval.getStart().toLocalDate()).getDays();
        long index = refPos.index + IDX_PER_DAY * daysToStart;
        long lastGoodIndx = index;
        long firstIndex = Long.MAX_VALUE;
        int backupTry = BACKUP_TRY;
        int dropped;
        int rawSeriesSize = 0;
        DateTime rawLastDt = DateTime.now();


        try {
            Response devData;
            ArrayListEx<Sample> samples = new ArrayListEx<>();
            DeviceGoveeHelper.SampleSeries sampleSeries = null;

            JSONObject devDataPostJson = new JSONObject();
            devDataPostJson.put("device", device.device);
            devDataPostJson.put("sku", device.sku);

            do {
                devDataPostJson.put("index", index);
                devDataPostJson.put("limit", maxSampleLimit);
                String devDataPostJsonStr = devDataPostJson.toString();
                devData = postTo("https://app2.govee.com/th/rest/devices/v1/data/load", devDataHeaders, devDataPostJsonStr);

                if (devData != null && devData.code() == 200 && devData.body() != null) {
                    String jsonStr = devData.body().string();

                    ALog.d.tagMsg(this, "Fetch index=", index, " jsonSize=", jsonStr.length());
                    sampleSeries = parseDataSeries(new JSONObject(jsonStr));
                    if (sampleSeries != null && !ArrayListEx.isEmpty(sampleSeries.datas)) {
                        lastGoodIndx = index;
                        rawSeriesSize = sampleSeries.datas.size();
                        rawLastDt = new DateTime(sampleSeries.datas.last(Sample.EMPTY).milli);
                        DateTime sampleDT = new DateTime(sampleSeries.datas.first(Sample.EMPTY).milli);
                        dropped = trimStart(sampleSeries.datas, interval);

                        ALog.d.tagMsg(this, interval
                                , " index=", sampleSeries.index
                                , " first="
                                , new DateTime(sampleSeries.datas.first(Sample.EMPTY).milli)
                                , " last="
                                , new DateTime(sampleSeries.datas.last(Sample.EMPTY).milli)
                                , " Dropped="
                                , dropped
                        );

                        trimEnd(sampleSeries.datas, interval);
                        if (sampleSeries.datas.size() > 0) {
                            if (index + dropped < firstIndex) {
                                firstIndex = index + dropped;
                                long indexErr = (firstIndex - index);
                                ALog.d.tagMsg(this, "index error=", indexErr, " days=", daysToStart);
                            }
                            samples.addAll(sampleSeries.datas);
                            index = sampleSeries.index;
                        } else if (dropped > 0) {
                            long refDaysToStart = Days.daysBetween(refPos.dt().toLocalDate(), sampleDT.toLocalDate()).getDays();
                            long idxPerDay = (sampleSeries.index - refPos.index) / refDaysToStart;
                            index = refPos.index + idxPerDay * daysToStart;
                            ALog.i.tagMsg(this, " RefIdx=", refPos.index, " idxPerDay=", idxPerDay, " daysToStart=", daysToStart, " newIdx=", index);
                        } else {
                            index = sampleSeries.index;
                        }
                        backupTry = BACKUP_TRY;
                    } else {
                        if (backupTry-- > 0 && index > lastGoodIndx) {
                            index = (index + lastGoodIndx) / 2;
                            sampleSeries = new SampleSeries();
                            continue;
                        }
                        ALog.e.tagMsg(TAG, " post parser error ", devData);
                    }
                } else {
                    ALog.e.tagMsg(TAG, " post load error ", devData);
                }

                if (rawLastDt.isAfter(interval.getStart())) {
                    long percent = 100 - 100 * (rawLastDt.getMillis() - interval.getStartMillis()) / (interval.getEndMillis() - interval.getStartMillis());
                    progressModel.setProgress(rawLastDt.getMillis(), 0, (int) percent);
                }
            } while (devData != null
                    && devData.code() == 200
                    && sampleSeries != null
                    && rawLastDt.getMillis() <= interval.getEndMillis()
                    && rawSeriesSize == maxSampleLimit
            );

            if (sampleSeries != null && sampleSeries.datas.size() != 0) {
                refPos.index = sampleSeries.index;
                refPos.milli = sampleSeries.datas.first(Sample.EMPTY).milli;
                progressModel.setProgress(rawLastDt.getMillis(), 0, 100);
                return new IntervalSeries(
                        refPos.dt(), refPos.index, interval, samples, firstIndex, sampleSeries.index);
            }

        } catch (IOException | JSONException ex) {
            ALog.e.tagMsg(TAG, "load/data ", ex);
            devDataException = ex;
        }

        progressModel.setProgress(rawLastDt.getMillis(), 0, 100);
        return null;
    }

    public static int trimStart(ArrayListEx<Sample> samples, Interval interval) {
        int size = samples.size();
        while (!samples.isEmpty() && samples.get(0).milli < interval.getStartMillis()) {
            samples.remove(0);
        }
        return size - samples.size();
    }

    public static int trimEnd(ArrayListEx<Sample> samples, Interval interval) {
        int size = samples.size();
        int idx = 0;
        while (idx < samples.size()) {
            if (samples.get(idx).milli < interval.getEndMillis()) {
                idx++;
            } else {
                samples.remove(idx);
            }
        }
        return size - samples.size();
    }

    public static void avgSamplesToHourly(
            IntervalSeries intervalSeries, DbDeviceHourly dbDeviceHourly, DbDeviceDaily dbDailyRecords,
            WxViewModel viewModel) {

        if (intervalSeries.samples.size() == 0) {
            ALog.e.tagMsg(TAG, "No samples to average");
            return;
        }
        DateTime prevTime = new DateTime(0);

        int avgCount = 0;
        int temSum = 0;
        int humSum = 0;
        int avgHour = -1;
        DeviceGoveeHelper.Sample avgItem = new DeviceGoveeHelper.Sample(0, 0, 0);

        int dailyDay = -1;
        DeviceGoveeHelper.Sample maxTem = null;
        DeviceGoveeHelper.Sample minTem = null;
        DeviceGoveeHelper.Sample maxHum = null;
        DeviceGoveeHelper.Sample minHum = null;

        // final ArrayListEx<Sample> hourlySamples = new ArrayListEx<>();
        // final ArrayListEx<Sample> dayMaxTem = new ArrayListEx<>();
        // final ArrayListEx<Sample> dayMinTem = new ArrayListEx<>();
        // hourlySamples.clear();
        // dayMaxTem.clear();
        // dayMinTem.clear();

        /*
        DbDeviceDaily  dbSensorDailyRecords = new DbDeviceDaily();
        if (! dbSensorDailyRecords.openWrite()) {
            ALog.e.tagMsg(this, "hourly record db openWrite failed");
            return;
        }
         */

        for (DeviceGoveeHelper.Sample sample : intervalSeries.samples) {
            DateTime sampleTime = new DateTime(sample.milli);
            if (sampleTime.isAfter(prevTime)) {
                long millis = sample.milli;
                int tem = sample.tem;
                int hum = sample.hum;

                if (avgHour == -1) {
                    // Init once using first sample
                    avgHour = sampleTime.getHourOfDay();
                    avgItem = new DeviceGoveeHelper.Sample(tem, hum, millis);
                    maxTem = minTem = maxHum = minHum = sample;
                    dailyDay = sampleTime.getDayOfYear();
                }

                temSum += tem;
                humSum += hum;
                avgCount++;

                if (sampleTime.getHourOfDay() != avgHour) {
                    avgItem.tem = temSum / avgCount;
                    avgItem.hum = humSum / avgCount;
                    dbDeviceHourly.add(avgItem);

                    avgHour = sampleTime.getHourOfDay();
                    temSum = humSum = avgCount = 0;
                    avgItem = new Sample(tem, hum, millis);
                }

                if (sampleTime.getDayOfYear() != dailyDay) {
                    // dayMaxTem.add(maxTem);
                    // dayMinTem.add(minTem);
                    dbDailyRecords.add(minTem, maxTem, minHum, maxHum);
                    maxTem = minTem = maxHum = minHum = sample;
                    dailyDay = sampleTime.getDayOfYear();
                }

                if (tem > maxTem.tem) {
                    maxTem = sample;
                }
                if (tem < minTem.tem) {
                    minTem = sample;
                }
                if (hum > maxHum.hum) {
                    maxHum = sample;
                }
                if (hum < minHum.hum) {
                    minHum = sample;
                }

                prevTime = sampleTime;
            }

            // long percent = 100 - 100 * (dbDeviceHourly.firstMilli - dbDeviceHourly.startMilli) / (dbDeviceHourly.lastMilli - dbDeviceHourly.startMilli);
            // viewModel.setProgress(dbDeviceHourly.lastMilli, intervalSeries.samples.size(), (int) percent);
        }
        if (avgCount != 0) {
            avgItem.tem = temSum / avgCount;
            avgItem.hum = humSum / avgCount;
            dbDeviceHourly.add(avgItem);

            // hourlySamples.add(avgItem);
            // dayMaxTem.add( maxTem);
            // dayMinTem.add( minTem);
            dbDailyRecords.add(minTem, maxTem, minHum, maxHum);
        }

        if (intervalSeries.samples.size() != 0) {
            dbDeviceHourly.updateMeta(intervalSeries.startTime.getMillis(), intervalSeries.startIndex,
                    intervalSeries.samples.first(Sample.EMPTY).milli, intervalSeries.firstIndex,
                    intervalSeries.samples.last(Sample.EMPTY).milli, intervalSeries.lastIndex);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Utility functions

    static Exception postException = null;

    @Nullable
    static Response postTo(String urlStr, Map<String, String> headers, String postJson) {
        OkHttpClient okHttpClient = NetUtils.getOkHttpClient();
        final int timeoutMilli = 5000;
        if (okHttpClient.connectTimeoutMillis() != timeoutMilli) {
            okHttpClient = NetUtils.getOkHTTPBuilder()
                    .connectTimeout(timeoutMilli, TimeUnit.MILLISECONDS)
                    .readTimeout(timeoutMilli, TimeUnit.MILLISECONDS)
                    .build();
        }

        RequestBody body = RequestBody.create(
                postJson, MediaType.parse("application/json"));

        Request.Builder requestBuilder = new Request.Builder()
                .url(urlStr)
                .post(body);

        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                requestBuilder.addHeader(header.getKey(), header.getValue());
            }
        }
        Request request = requestBuilder.build();

        Response response = null;
        postException = null;

        for (int retry = 0; retry < 3; retry++) {
            try {
                Thread.sleep(retry * 1000);
                response = okHttpClient.newCall(request).execute();
            } catch (Exception ex) {
                postException = ex;
            }
            if (response != null && response.code() == 200) {
                break;
            }
        }

        return response;
    }

    public boolean loadSuccessful() {
        return (/* hourlySamples.size() > 0
                && */ loginException == null
                && devSummaryException == null
                && devDataException == null);
    }


    private static String loginKey(DeviceAccount account) {
        return PREF_LOGIN + account.user;
    }

    private static String devListTimeKey(DeviceAccount account) {
        return PREF_DEVLIST_TIME + account.user;
    }

    private static String devListKey(DeviceAccount account) {
        return PREF_DEVLIST + account.user;
    }

    public Device getDevice() {
        // TODO - how to pick when multiple items in list.
        // ALog.d.tagMsg(this, "getDevices ", devices);
        return (devices != null) ? devices.first(Device.EMPTY) : Device.EMPTY;
    }

    public long lastMilli() {
        final Device device = getDevice();
        return device.isValid(this) ?  device.deviceExt.lastDeviceData.lastTime : 0L;
    }

    public boolean isValid(@NonNull String devType) {
        return getDevice().isValid(this);
    }

    // =============================================================================================


    @SuppressWarnings("unused")
    public static class Client {
        public String A;                //"testiot.cert",
        public String B;                // "testIot",
        public String topic;            //"GA/33b0ce1f583d7260ae1a17803be4e8ea",

        public int tokenExpireCycle;    //  57600,
        public String token;
        public String refreshToken;
        public String pushToken;

        public String client;           // "e6e7d9e73f5058a5",
        public String clientName;       // "Android Android SDK built for x8",
        public String accountId;        // 1005658,

        public String versionCode;      // "121.0",
        public String versionName;      // "3.6.0",
        public String sysVersion;       // "6.0"
    }

    @SuppressWarnings("unused")
    public static class Device {
        public String device;           // "7ca6b00aa141002970",
        public String sku;              // "H5053",
        //    public String spec;             // ""
        public String versionHard;      // "1.00.01",
        public String versionSoft;      // "1.00.06",
        public String deviceName;       // "temp 1",
        @Nullable
        public DeviceExt deviceExt;

        public Device() {
            device = deviceName = "?";
            sku = versionHard = versionSoft = "?";
            // deviceExt = new DeviceExt();
            deviceExt = null;
        }

        public static Device EMPTY = new Device();  // Make sure Retrofit not impacted by this.

        public boolean isValid(DeviceGoveeHelper devHlper) {
            boolean okay = (deviceExt != null && deviceExt.lastDeviceData != null && devHlper.loginException == null);
            if (!okay) {
                ALog.fe.tagMsg(this, "Govee device NOT available ", devHlper.loginException);
            } else {
                // ALog.d.tagMsg(this, "Device VALID, name=", device);
            }
            return okay;
        }

        public String getNameSku() {
            return deviceName + sku;
        }

        // -----------------------------------------------------------------------------------------
        // Save and Restore
        private static final String COMMA = ",";
        private static final String PREF_DEV = "devices";
        private static final int PREF_DEV_PARTS = 5;
        public static void saveTo(SharedPreferences pref, ArrayListEx<Device> devices) {
            Set<String> saveSet = new HashSet<>(devices.size());
            for (Device dev : devices) {
                String devStr = toJson(dev);
                saveSet.add(devStr);
            }
            pref.edit().putStringSet(PREF_DEV, saveSet).apply();
        }

        public static String toJson(Device dev) {
            try {
                return new GsonBuilder().create().toJson(dev);
            } catch (Exception ex) {
                ALog.e.tagMsg("DevGovee", "toJson ", ex);
            }
            return null;
        }

        public static Device fromJson(String jsonStr) {
            try {
                Gson gson = new GsonBuilder().create();
                return gson.fromJson(jsonStr, new TypeToken<Device>() {  }.getType());
            } catch (Exception ex) {
                ALog.e.tagMsg("DevGovee", "fromJson ", ex);
            }
            return null;
        }

        public static ArrayListEx<Device> loadFrom(SharedPreferences pref) {
            Set<String> savedStrSet = pref.getStringSet(PREF_DEV, new HashSet<>());
            ArrayListEx<Device> list = new ArrayListEx<>(savedStrSet.size());
            for (String savedStr : savedStrSet) {
                Device dev = fromJson(savedStr);
                list.add(dev);
            }
            return list;
        }
    }

    public static final Device DEVICE_EMPTY = new Device();

    public static class DeviceExt {
        public DeviceSettings deviceSettings;
        public LastDeviceData lastDeviceData;
        public ExtResources extResources;
    }

    @SuppressWarnings("unused")
    public static class DeviceSettings {
        public int temMin;                  //  0,       alarm min temperature (Celsius * 100)
        public int temMax;                  //  2666,    alarm max temperature (Celsius * 100)
        public boolean temWarning;          //  true,
        public boolean fahOpen;             //  false,
        public int temCali;                 //  0,
        public int humMin;                  //  2000,    alarm min humidity (Humidity * 100)
        public int humMax;                  //  9700,    alarm max humidity (Humidity * 100)
        public boolean humWarning;          //  true,
        public int humCali;                 //  0,
        public boolean netWaring;           //  true,
        public int uploadRate;              //  10,
        public int battery;                 //  60,
        public int wifiLevel;               //  0,
        public int header;                  //  "1",
        public String gatewayVersionHard;   //  "1.00.00",
        public String gatewayVersionSoft;   //  "1.00.27",
        public String sku;                  //  "H5053",
        public String device;               //  "7ca6b00aa141002970",
        public String deviceName;           //  "temp 1",
        public String versionHard;          //  "1.00.01",
        public String versionSoft;          //  "1.00.06"
    }

    @SuppressWarnings("unused")
    public static class LastDeviceData {
        public boolean online;             //  true,
        public boolean gwonline;           //  true,
        public int tem;                    //  941,     Celsius * 100
        public int hum;                    //  8405,    Humidity * 100
        public long lastTime;              //  1602730320000, (epoch milli)
        public int avgDayTem;              //  941,
        public int avgDayHum;              //  8405
    }

    @SuppressWarnings("unused")
    public static class ExtResources {
        public String skuUrl;              //  "",
        public String headOnImg;           //  "",
        public String headOffImg;          //  "",
        public String ext;                 //  ""
    }

    // "index":3914106,"message":"","status":200
    @SuppressWarnings("unused")
    public static class SampleSeries {
        public ArrayListEx<Sample> datas;
        public long index;
        public String message;
        public int status;
    }

    public static class IntervalSeries {
        public final DateTime startTime;  // Cloud start time
        public final long startIndex;     // Cloud start index

        public final Interval interval;   // Sample series interval
        public final ArrayListEx<Sample> samples;
        public final long firstIndex;     // First index for sample series
        public final long lastIndex;      // Last index for sample series

        public IntervalSeries(DateTime startTime, long startIndex, Interval interval, ArrayListEx<Sample> samples, long firstIndex, long lastIndex) {
            this.startTime = startTime;
            this.startIndex = startIndex;

            this.interval = interval;
            this.samples = samples;
            this.firstIndex = firstIndex;
            this.lastIndex = lastIndex;
        }
    }

    // {"tem":1096,"hum":6031,"time":1600645080000}
    public static class Sample {
        public static final Sample EMPTY = new Sample(0, 0, 0L);
        public int tem;     // Celsius * 100
        public int hum;     // Humidity * 100
        @SerializedName("time")
        public final long milli;  // Epoch milli

        public Sample(int tem, int hum, long milli) {
            this.tem = tem;
            this.hum = hum;
            this.milli = milli;
        }
    }

    public static class D2 {
        public final int val;
        public final long milli;

        public D2(int val, long milli) {
            this.val = val;
            this.milli = milli;
        }
    }

    public static class Record {
        public final long dayMilli;
        public final D2 tempMin;
        public final D2 tempMax;
        public final D2 humMin;
        public final D2 humMax;

        public Record(long dayMilli, D2 minTem, D2 maxTem, D2 minHum, D2 maxHum) {
            this.dayMilli = dayMilli;
            this.tempMin = minTem;
            this.tempMax = maxTem;
            this.humMin = minHum;
            this.humMax = maxHum;
        }

        public boolean isValid() {
            DateTime minDt = new DateTime(tempMin.milli);
            DateTime maxDt = new DateTime(tempMax.milli);
            DateTime minHt = new DateTime(humMin.milli);
            DateTime maxHt = new DateTime(humMax.milli);
            int refDay = minDt.getDayOfYear();
            boolean error = refDay != maxDt.getDayOfYear()
                    || refDay != minHt.getDayOfYear()
                    || refDay != maxHt.getDayOfYear();
            error |= tempMin.val > tempMax.val;
            error |= humMin.val > humMax.val;

            ALog.d.tagFmt(this, "get RecXXX %s %s %s %s %4d %4d %4d %4d %s"
                    , minDt.toString("M/dd HH")
                    , maxDt.toString("M/dd HH")
                    , minHt.toString("M/dd HH")
                    , maxHt.toString("M/dd HH")
                    , tempMin.val
                    , tempMax.val
                    , humMin.val
                    , humMax.val
                    , error ? "**ERROR**" : "");

            return !error;
        }
    }
}
