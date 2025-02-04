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

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.landenlabs.all_sensor.utils.DataUtils;
import com.landenlabs.all_sensor.utils.IOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.zip.GZIPOutputStream;

/**
 * Custom Log output saves to a private log file
 */
@SuppressWarnings({"unused", "RedundantSuppression"})
public class ALogFileWriter implements ALogOut.LogPrinter {
    public static final ALogFileWriter Default = new ALogFileWriter();

    private static final String TAG = "ALogFileWriter";
    public static final String TIMESTAMP_FORMAT = "EEE dd HH:mm";
    private static final char[] LEVELS = {'0', '1', 'V', 'D', 'I', 'W', 'E', 'A'};

    private final ArrayBlockingQueue<String> mWriteQueue = new ArrayBlockingQueue<>(20);
    private final String mFilename = "filelog.txt";
    private String mMsgFmt =  "%1$s | %2$c | %3$s  %4$s";  // timestamp, level, tag, message
    // "%1$s | %4$s";   // timestamp | message
    private String mLogDir;
    private String mLogFileName = mFilename;
    private long mFileSizeLimit;           // bytes
    private File mLogFile;
    private BufferedWriter mBufferedWriter;
    private Thread mWriterThread;

    @SuppressWarnings("UnusedReturnValue")
    public static boolean init(Context context) {
        boolean okay = true;
        try {
            // Default.setDir(context.getFilesDir().getAbsolutePath() + "/logs");
            Default.open(context);
        } catch (Exception ex) {
            okay = false;
            Log.e(TAG, ex.toString());
        }

        return okay;
    }

    /**
     * @return Current time formatted for logging.
     */
    public static String getCurrentTimeStamp() {
        String currentTimeStamp = null;

        try {
            SimpleDateFormat dateFmt = new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.getDefault());
            currentTimeStamp = dateFmt.format(new Date());
        } catch (Exception e) {
            // ALog.e.tagMsg(this, Log.getStackTraceString(e));
        }

        return currentTimeStamp;
    }

    /**
     * Set row format for four fields:
     * <ul>
     * <li>Date Time string
     * <li>Severity character
     * <li>Tag string
     * <li>Message string
     * </ul>
     * Examples:
     * <ul>
     * <li>"%s/%c %s - %s"
     * <li>"%s,%c,%s,%s"
     * <li>%1$s,%4$s
     * </ul>
     */
    void setFormat(String fmt) {
        mMsgFmt = fmt;
    }

    /**
     * Set file directory. Defaults to Cache directory.
     * <ul>
     *  <li>getCacheDir
     *  <li>getFilesDir()
     * </ul>
     */
    void setDir(String logDir) {
        mLogDir = logDir;
    }

    /**
     * Open default file with default file size.
     *
     * <pre>
     * File stored in Download directory:
     *     /storage/emulated/0/Download/package.name.filelog.txt
     * Example:
     *     /storage/emulated/0/Download/com.wsicarousel.android.weather.filelog.txt
     * </pre>
     */
    public void open(@NonNull Context context) {
        // AndroidManifest sets up sharable directory for logs
        setDir(context.getFilesDir().getAbsolutePath() + "/logs");
        final long FILE_SIZE_LIMIT = 1024 * 1024 * 10;
        open(context.getPackageName() + "." + mFilename, FILE_SIZE_LIMIT);
    }

    /**
     * Open new log file with maximum file size.
     * When logging exceeds maximum size it will be archived and a new file opened.
     * Only one archived file kept.
     */
    void open(String logFileName, long fileSizeLimit) {
        mFileSizeLimit = fileSizeLimit;

        File dir = new File(mLogDir);
        IOUtils.mkdirs(dir);

        mLogFile = new File(mLogDir, logFileName);
        mLogFileName = mLogFile.getName();
        setPermissions(mLogFile);

        if (!mLogFile.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                mLogFile.createNewFile();
                setPermissions(mLogFile);
            } catch (Exception ex) {
                Log.e(TAG, ex.toString());
            }
        }

        checkFileSize();

        if (!isOpen()) {
            try {
                mBufferedWriter = new BufferedWriter(new java.io.FileWriter(mLogFile, true));
            } catch (IOException ex) {
                Log.e(TAG, ex.toString());
            }
        }
    }

    public File getFile() {
        if (mBufferedWriter != null) {
            try {
                mBufferedWriter.flush();
            } catch (IOException e) {
                // ALog.e.tagMsg(this, Log.getStackTraceString(e));
            }
        }
        return mLogFile;
    }

    boolean isOpen() {
        return mBufferedWriter != null;
    }

    /**
     * Close current log file.
     * Subsequent logging will fail until re-opened.
     */
    public void close() {
        try {
            if (mBufferedWriter != null) {
                mBufferedWriter.write('\n');
                // mBufferedWriter.flush( );
                mBufferedWriter.close();
                mBufferedWriter = null;
            }
        } catch (IOException e) {
            // ALog.e.tagMsg(this, Log.getStackTraceString(e));
        }
    }

    /**
     * Close and Delete file. Log file is not re-opened, so subsequent logging will fail.
     */
    void delete() {
        close();
        if (mLogFile != null) {
            IOUtils.delete(mLogFile);
        }
    }

    /**
     * Clear current logging by closing and deleting current file, then re-open file.
     */
    public void clear() {
        close();
        if (mLogFile != null) {
            IOUtils.delete(mLogFile);
            open(mLogFileName, mFileSizeLimit);
        }
    }

    /**
     * Print log level, tag and message.
     */
    public void println(int level, String tag, Object... msgs) {
        initWriterThread();
        try {
            mWriteQueue.add(formatMsg(level, tag, DataUtils.join(",", msgs)) + "\n");
        } catch (IllegalStateException ignore) {
            // Is full - ignore it
        }
    }

    /**
     * Start worker thread to complete file i/o.
     */
    private void initWriterThread() {
        if (mWriterThread == null) {
            mWriterThread = new Thread("ALogFileWriter") {
                @Override
                public void run() {
                    try {
                        Looper.prepare();
                        while (!isInterrupted()) {
                            String msg = mWriteQueue.take();
                            writeln(msg);
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Writing log file ", ex);
                    }
                }
            };
            mWriterThread.start();
        }
    }

    /**
     * Wite log level, tag and message.
     */
    @WorkerThread
    private void writeln(String... msgs) {
        if (mBufferedWriter != null) {
            synchronized (this) {
                try {
                    if (checkFileSize()) {
                        mBufferedWriter.close();
                        mBufferedWriter = new BufferedWriter(new java.io.FileWriter(mLogFile, true));
                    }

                    mBufferedWriter.write(DataUtils.join(",", msgs).toString());
                    mBufferedWriter.flush();
                } catch (IOException ex) {
                    Log.e(TAG, ex.toString());
                }
            }
        }

        if (mBufferedWriter == null) {
            Log.e(TAG, "You have to call ALogFileWriter.open(...) before starting to log");
        }
    }

    public int maxTagLen() {
        return 0; // was 20, but lets force all user tag to moved into text message
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void setPermissions(File file) {
        file.setReadable(true, true);
        file.setWritable(true, true);
    }

    String formatMsg(int level, String tag, CharSequence... msgs) {
        return String.format(mMsgFmt, getCurrentTimeStamp(), LEVELS[level & 7], tag, DataUtils.join(",", msgs));
    }

    /**
     * Zip Archive full log file and open a new log file.
     */
    private void archiveByGzip() {

        byte[] buffer = new byte[1024];

        try {

            File dstFile = new File(mLogDir, mLogFileName + ".gz");
            if (dstFile.exists()) {
                IOUtils.delete(dstFile);
            }

            try (GZIPOutputStream gzout = new GZIPOutputStream(new FileOutputStream(dstFile))) {
                try (FileInputStream in = new FileInputStream(mLogFile)) {
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        gzout.write(buffer, 0, len);
                    }
                }
            }

            setPermissions(dstFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Archive full log file and open a new log file.
     */
    private void archiveLog() {
        archiveByGzip();
    }

    /**
     * If file size has been exceeded, archive current file and open new file.
     *
     * @return True if current file archived and new file created.
     */
    private boolean checkFileSize() {
        boolean createdNewLogFile = false;
        try {
            if (mLogFile.length() > mFileSizeLimit) {
                archiveLog();

                mLogFile = new File(mLogDir, mLogFileName);
                //noinspection ResultOfMethodCallIgnored
                mLogFile.createNewFile();
                setPermissions(mLogFile);
                createdNewLogFile = true;
            }
        } catch (Exception ignore) {
            // ALog.e.tagMsg(this, Log.getStackTraceString(e));
        }

        return createdNewLogFile;
    }
}
