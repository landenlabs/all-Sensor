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

import androidx.annotation.Nullable;

import com.landenlabs.all_sensor.logger.ALog;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

@SuppressWarnings({"CheckStyle", "WeakerAccess"})
public class IOUtils {

    private static final String TAG = IOUtils.class.getSimpleName();

    private IOUtils() {
    }

    /**
     * Alternate version then system method File.mkdirs() which returns false if directory exists.
     */
    public static boolean mkdirs(@Nullable File file) {
        try {
            if (file == null || file.exists()) {
                return true;
            }

            if (file.mkdir()) {
                return true;
            }

            File canonFile;
            try {
                canonFile = file.getCanonicalFile();
            } catch (IOException ex) {
                ALog.w.tagMsg(TAG, "mkdirs failed ", ex);
                return false;
            }

            File parent = canonFile.getParentFile();
            return (parent != null && mkdirs(parent) && canonFile.mkdir());
        } catch (Exception ex) {
            ALog.w.tagMsg(TAG, "mkdirs failed ", ex);
            return false;
        }
    }

    public static void delete(@Nullable File file) {
        try {
            if (file != null && !file.delete()) {
                ALog.e.tagMsg(TAG, "delete failed on=" + file);
            }
        } catch (Exception ex) {
            ALog.e.tagMsg(TAG, "delete failed on=" + file);
        }
    }


    public static int[] readArray(DataInputStream inDataStream) throws IOException {
        int len = inDataStream.readInt();
        if (len > 0) {
            int[] array = new int[len];
            for (int idx=0; idx < len; idx++) {
                array[idx] = inDataStream.readInt();
            }
            return array;
        } else {
            return new int[0];
        }
    }

    public static void writeArray(int[] array, DataOutputStream outputStream) throws IOException {
        if (array != null && array.length > 0) {
            outputStream.writeInt(array.length);
            for (int val : array) {
                outputStream.writeInt(val);
            }
        } else {
            outputStream.writeInt(0);
        }

    }

}
