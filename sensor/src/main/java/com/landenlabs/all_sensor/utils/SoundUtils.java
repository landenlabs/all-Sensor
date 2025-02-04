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
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import com.landenlabs.all_sensor.logger.ALog;

import java.io.IOException;

public class SoundUtils {

    private SoundUtils() {
    }

    public static void playNotificationSound(@NonNull Context context) {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone tone = RingtoneManager.getRingtone(context, notification);
            tone.play();
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    private static MediaPlayer soundPlayer;
    public static void playSound(@NonNull Context context, @RawRes int soundId) {
        startPlayer(context, soundId, soundPlayer);
    }

    public static MediaPlayer startPlayer(@NonNull Context context, @RawRes int soundId, @Nullable MediaPlayer soundPlayer) {
        if (false) {
            soundPlayer = MediaPlayer.create(context, soundId); // R.raw.click
        } else {
            if (soundPlayer == null) {
                soundPlayer = new MediaPlayer();
                soundPlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build());
            } else {
                // soundClick.release();
                soundPlayer.reset();
            }
            try {
                AssetFileDescriptor afd = context.getResources().openRawResourceFd(soundId);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    soundPlayer.setDataSource(afd);
                } else {
                    soundPlayer.setDataSource(afd.getFileDescriptor());
                }
                soundPlayer.prepare();
            } catch (IOException ex) {
                ALog.e.tagMsg("Sound", ex);
            }
        }
        soundPlayer.start();
        return  soundPlayer;
    }
}
