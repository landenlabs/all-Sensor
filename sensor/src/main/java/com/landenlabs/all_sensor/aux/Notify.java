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

package com.landenlabs.all_sensor.aux;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static com.landenlabs.all_sensor.aux.SysInfo.getBatteryTempF;
import static com.landenlabs.all_sensor.widget.WidDataProvider.PREF_DATA_HUM;
import static com.landenlabs.all_sensor.widget.WidDataProvider.PREF_DATA_MILLI;
import static com.landenlabs.all_sensor.widget.WidDataProvider.PREF_DATA_SENSOR_NAME;
import static com.landenlabs.all_sensor.widget.WidDataProvider.PREF_DATA_TEMP;
import static com.landenlabs.all_sensor.widget.WidDataProvider.PREF_DATA_VALUE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.text.SpannableString;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.landenlabs.all_sensor.MainActivity;
import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.sensor.AppCfg;
import com.landenlabs.all_sensor.sensor.SensorItem;
import com.landenlabs.all_sensor.sensor.SensorListManager;
import com.landenlabs.all_sensor.utils.ArrayListEx;
import com.landenlabs.all_sensor.utils.DataUtils;
import com.landenlabs.all_sensor.utils.FragUtils;
import com.landenlabs.all_sensor.utils.SpanUtil;
import com.landenlabs.all_sensor.utils.StrUtils;
import com.landenlabs.all_sensor.widget.WidView;

/**
 * Manage Notify draw
 */
public class Notify {

    private static final int notificationId = 101;
    private static final String CHANNEL_ID = "ScheduleId";
    private static final String channel_name = "Schedule";
    static final ArrayListEx<CharSequence> prevMsgs = new ArrayListEx<>();
    private static String channel_description;

    public static void init(@NonNull Context context) {
        channel_description = context.getPackageName();

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String description = channel_description;
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channel_name, importance);
            channel.setDescription(description);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = FragUtils.getServiceSafe(context, Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void updateNotification(@NonNull Context context, String notify, Object... msgs) {

        CharSequence newMsg = DataUtils.join(" ", msgs);
        ALog.d.tagMsg("updateNotification", newMsg);

        synchronized (prevMsgs) {
            for (String token : new String[]{"wifi", "battery"}) {
                if (newMsg.toString().toLowerCase().contains(token))
                    prevMsgs.removeIf(a -> {
                        return (a != null) && a.toString().toLowerCase().contains(token);
                    });
            }
            prevMsgs.add(newMsg);
            while (prevMsgs.size() > 8) {
                prevMsgs.remove(0);
            }
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, FLAG_IMMUTABLE);

        Bitmap largeBitmap =
                BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notification, null);

        Notification notification;
        AppCfg appCfg = AppCfg.getInstance(context);

        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(CHANNEL_ID, "Sensor", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("All-Sensor");
            channel.setImportance(NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);

            RemoteViews remoteBigViews = new RemoteViews(context.getPackageName(), R.layout.notification_big);
            RemoteViews remoteStdViews = new RemoteViews(context.getPackageName(), R.layout.notification_std);
            SharedPreferences pref = WidView.getPref(context);

            // String devName = pref.getString(PREF_DATA_DEVNAME, "unknown");
            {
                int numTemp = pref.getInt(PREF_DATA_TEMP, -1);
                String strTemp = String.format("%.0f°%s", appCfg.toDegreeN(numTemp), appCfg.tunit.id);
                SpannableString ssT = SpanUtil.SString(strTemp, SpanUtil.SS_SUPER + SpanUtil.SS_SMALLER, strTemp.length() - 2, strTemp.length());
                remoteStdViews.setTextViewText(R.id.sb_temperature, ssT);
                remoteBigViews.setTextViewText(R.id.sb_temperature, ssT);
            }
            {
                int numHum = pref.getInt(PREF_DATA_HUM, -1);
                String strHum = String.format("%.0f%%", appCfg.toHumPerN(numHum));
                SpannableString ssH = SpanUtil.SString(strHum, SpanUtil.SS_SUPER + SpanUtil.SS_SMALLER, strHum.length() - 1, strHum.length());
                remoteStdViews.setTextViewText(R.id.sb_humidity, ssH);
                remoteBigViews.setTextViewText(R.id.sb_humidity, ssH);
            }
            {
                long devMilli = pref.getLong(PREF_DATA_MILLI, System.currentTimeMillis());
                SpannableString timeStr = appCfg.timeFmt(devMilli);
                remoteStdViews.setTextViewText(R.id.sb_time, timeStr);
                remoteBigViews.setTextViewText(R.id.sb_time, timeStr);
            }

            {
                float batteryTempF = getBatteryTempF(context, appCfg.tunit);
                String strTemp = String.format("%.0f°%s", batteryTempF, appCfg.tunit.id);   // TODO - handle units
                remoteStdViews.setTextViewText(R.id.sb_batTemp, strTemp);
                remoteBigViews.setTextViewText(R.id.sb_batTemp, strTemp);
            }

            {
                int sensorId = 0;
                int NO_SENSOR_DATA = -1;
                CharSequence sensorValue = "";
                int iValue = pref.getInt(PREF_DATA_VALUE + sensorId, NO_SENSOR_DATA);
                if (iValue != NO_SENSOR_DATA) {
                    String sensorName = pref.getString(PREF_DATA_SENSOR_NAME + sensorId, "");
                    long sensorMilli = pref.getLong(PREF_DATA_MILLI + sensorId, 0);
                    SensorItem sensorItem = SensorListManager.get(sensorName);
                    sensorValue = (sensorItem != null) ? sensorItem.sValue(iValue) : "";
                }
                remoteStdViews.setTextViewText(R.id.sb_value, sensorValue);
                remoteBigViews.setTextViewText(R.id.sb_value, sensorValue);
            }

            remoteBigViews.removeAllViews(R.id.sb_history);
            for (CharSequence msg : prevMsgs) {
                if (StrUtils.hasText(msg)) {
                    RemoteViews textRow = new RemoteViews(context.getPackageName(), R.layout.notification_row);
                    remoteBigViews.addView(R.id.sb_history, textRow);
                    textRow.setTextViewText(R.id.sb_row_tx, msg);
                }
            }

            boolean canBeDismissed = false;
            Notification.Builder mBuilder;
            mBuilder = new Notification.Builder(context, CHANNEL_ID)
                    .setCustomContentView(remoteStdViews)
                    .setCustomBigContentView(remoteBigViews)
                    .setOngoing(!canBeDismissed)       // prevents swiping away
                    .setAutoCancel(canBeDismissed)
                    .setOnlyAlertOnce(true)
             //       .setStyle(new Notification.DecoratedCustomViewStyle())
             //       .setLargeIcon(largeBitmap)
             //       .setContentTitle(channel_description)
             //       .setContentText(newMsg)
             //       .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            ;

            if (appCfg.showNotification && StrUtils.hasText(notify)) {
                Bitmap statusBitmap = createBitmapFromString(notify.trim(), 48, 0);
                mBuilder.setSmallIcon(Icon.createWithBitmap(statusBitmap));
            } else {
                // TODO - how to hide status bar icon, crashes without smallIcon
                mBuilder.setSmallIcon(R.drawable.ic_launcher_foreground);
            }
            // Set the intent that will fire when the user taps the notification
            mBuilder.setContentIntent(pendingIntent);

            if (appCfg.soundOnUpdate) {
                channel.enableVibration(true);
                channel.enableLights(true);
            }

            notification = mBuilder.build();
        } else {
            CharSequence bigMsg = DataUtils.join("\n", prevMsgs.toArray(), 0);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    // TODO - how to hide status bar icon, crashes without smallIcon
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setLargeIcon(largeBitmap)
                    .setContentTitle(channel_description)
                    .setContentText(newMsg)
                    .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(bigMsg))
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            // Set the intent that will fire when the user taps the notification
            mBuilder.setContentIntent(pendingIntent);

            if (appCfg.soundOnUpdate) {
                mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
                mBuilder.setLights(Color.YELLOW, 1000, 300);
                // mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            }

            notification = mBuilder.build();
        }

        // TODO - enabled sounds if AppCfg.soundXXX enabled.
        // notification.sound = xxx;
        if (appCfg.soundOnUpdate) {
            Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (channel != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioAttributes att = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build();

                    channel.setSound(sound, att);
            } else {
                notification.sound = sound;
            }
        }

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(notificationId, notification);
    }

    private static Bitmap createBitmapFromString(String inputNumber, float textSizePx, int padding) {

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        paint.setTextSize(textSizePx);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);

        Rect textBounds = new Rect();
        paint.getTextBounds(inputNumber, 0, inputNumber.length(), textBounds);

        int pad2 = padding * 2;
        Bitmap bitmap = Bitmap.createBitmap(textBounds.width() + pad2, textBounds.height()+2, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        // canvas.drawColor(Color.WHITE);
        canvas.drawText(inputNumber, textBounds.width() / 2f + padding, textBounds.height(), paint);
        canvas.drawText(inputNumber, textBounds.width() / 2f + padding+2, textBounds.height()+2, paint);

        // paint.setStrokeWidth(2f);
        // canvas.drawLine(0, canvas.getHeight()-1, canvas.getWidth()-2, canvas.getHeight()-1, paint);

        return bitmap;
    }
}
