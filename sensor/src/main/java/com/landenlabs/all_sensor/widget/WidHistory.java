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

package com.landenlabs.all_sensor.widget;

import static com.landenlabs.all_sensor.utils.IOUtils.readArray;
import static com.landenlabs.all_sensor.utils.IOUtils.writeArray;

import android.content.Context;

import androidx.annotation.NonNull;

import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.utils.ArrayListEx;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Persistent management of historical Widget device and sensor values.
 */
public class WidHistory {

    public static class WidHistoryItem  {
        public long milli = 0L;
        public int[] values;  // temperature, humidity, pressure, battery, wifi, light, sound, gps, speed, ...

        public static final WidHistoryItem EMPTY = new WidHistoryItem();

        @NonNull
        @Override
        public String toString() {
            return  "WidHistory " + Arrays.toString(values);
        }
    }

    public static final String FIELDS_TH = "TH";    // Temperature, Humidity
    public static final String FIELDS_V = "V";      // value
    public static final int MAX_LIST_LEN = 10;
    private static final long MILLI_BETWEEN = TimeUnit.MINUTES.toMillis(30);
    private static final int VERSION = 100;

    public String fields;      // user code to manage fields in values array
    public final ArrayListEx<WidHistoryItem> list = new ArrayListEx<>(MAX_LIST_LEN);

    private static final String FNAME = "widHistory.txt";
    private static final boolean FMODE_APPEND = true;
    private static final boolean FMODE_TRUNCATE = false;

    // ---------------------------------------------------------------------------------------------

    public WidHistory(String fields) {
        this.fields = fields;
    }

    public void load(Context context, String name) {
        list.clear();
        File hstFile = new File(context.getCacheDir(), name+FNAME);

        try {
            if (hstFile.exists() && hstFile.length() > 0) {
                try (FileInputStream inStream = new FileInputStream(hstFile)) {
                    DataInputStream inDataStream = new DataInputStream(inStream);
                    if (VERSION == inDataStream.readInt()) {
                        fields = inDataStream.readUTF();
                        int size = inDataStream.readInt();
                        for (int idx = 0; idx < size; idx++) {
                            WidHistoryItem item = new WidHistoryItem();
                            item.milli = inDataStream.readLong();
                            item.values = readArray(inDataStream);
                            list.add(item);
                        }
                    } else {
                        ALog.e.tagMsg(this, name, FNAME, " Load wrong version ");
                    }
                }
            } else {
                hstFile.createNewFile();
            }
        } catch (Exception ex) {
            ALog.e.tagMsgStack(this, 10, name, FNAME, " Load failed ", ex);
        }

        ALog.d.tagMsg(this, name, FNAME, " Load history size=", list);
    }

    public void save(Context context, String name) {
        ALog.d.tagMsg(this,  name, FNAME, " Save history size=", list);

        // TODO - use a thread - violates Strict policy doing i/o on UI thread.
        new Thread ( () -> {
            File hstFile = new File(context.getCacheDir(), name+FNAME);
            try {
                try (FileOutputStream outStream = new FileOutputStream(hstFile, FMODE_TRUNCATE)) {
                    DataOutputStream outDataStream = new DataOutputStream(outStream);
                    outDataStream.writeInt(VERSION);
                    outDataStream.writeUTF(fields);
                    outDataStream.writeInt(list.size());
                    for (WidHistoryItem item : list) {
                        outDataStream.writeLong(item.milli);
                        writeArray(item.values, outDataStream);
                        ALog.d.tagMsg(this,  name, FNAME, " Save history row=", item.values);
                    }
                }
            } catch (Exception ex) {
                ALog.e.tagMsgStack(this, 10, name, FNAME, " Save failed ", ex);
            }
        }).start();;
    }

    public void append(WidHistoryItem item) {
        if (list.size() == 0 || item.milli > list.last(WidHistoryItem.EMPTY).milli) {
            list.add(item);
            ALog.d.tagMsg(this,  "Add history row=", item.values);
            while (list.size() > MAX_LIST_LEN  && removeWithIn(MILLI_BETWEEN)) {
            }
            while (list.size() > MAX_LIST_LEN) {
                list.remove(0);  // too many - remove first item.
            }
        }
        ALog.d.tagMsg(this,  "Add history size=", list);
    }

    public boolean removeWithIn(long deltaMilli) {
        if (list.size() < 3)
            return false;

        int minIdx = list.size()-2;
        long minMilli = list.get(minIdx+1).milli - list.get(minIdx).milli;

        // Keep first and last
        for (int idx = 1; idx < list.size(); idx++) {
            long gapMilli = list.get(idx).milli - list.get(idx-1).milli;
            if (gapMilli < minMilli) {
                minMilli = gapMilli;
                minIdx = idx;
            }
        }

        if (minMilli >= deltaMilli) {
            return false;
        }
        list.remove(minIdx);
        return true;
    }
}
