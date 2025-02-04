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

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.landenlabs.all_sensor.BuildConfig;
import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.logger.ALogFileWriter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

/**
 * Share screen shot of screen.
 */
public class ShareUtil {
    private static final String TAG = "ShareUtil";
    /**
     * Present list of actions and execute intent associated with resolveInfo.
     */
    private static final String SHARE_DIALOG_TITLE = "Share";
    static Rect SCREEN_RECT = null;

    // ---------------------------------------------------------------------------------------------

    public static void shareScreen(
            @NonNull Activity activity,
            @NonNull View view,
            @NonNull String toEmail,
            @NonNull String subject,
            @Nullable ShareActionProvider shareActionProvider) {
        Bitmap screenBitmap = getBitmap(view);
        List<Bitmap> bitmapList = new ArrayListEx<>();
        bitmapList.add(screenBitmap);
        SparseArray<String> rowLiost = new SparseArray<>();
        getTextFromViews(view, rowLiost, 0);
        List<String> strList = ToList(rowLiost);
        shareList(activity, bitmapList, strList, toEmail, subject, "allThreadTest.jpg", shareActionProvider);
    }

    static Rect getScreenRect() {
        if (SCREEN_RECT == null) {
            int widthPx = Resources.getSystem().getDisplayMetrics().widthPixels;
            int heightPx = Resources.getSystem().getDisplayMetrics().heightPixels;
            SCREEN_RECT = new Rect(0, 0, widthPx, heightPx);
        }
        return SCREEN_RECT;
    }

    static void getTextFromViews(View view, SparseArray<String> strList, int level) {
        if (view instanceof TextView) {
            Rect rect = new Rect();
            Point off = new Point();
            boolean isPartiallyVisible = view.getGlobalVisibleRect(rect, off);
            if (getScreenRect().contains(off.x, 0) && view.getVisibility() == View.VISIBLE) {
                int y = rect.top + view.getBaseline();
                // String padX =  String.format("%" + x + "s", "");
                int idx = y / 20;
                String prev = strList.get(idx, "") + " ";
                strList.put(idx, prev + ((TextView) view).getText().toString());
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int idx = 0; idx < vg.getChildCount(); idx++) {
                getTextFromViews(vg.getChildAt(idx), strList, level + 1);
            }
        }
    }

    static ArrayListEx<String> ToList(SparseArray<String> sparseArray) {
        ArrayListEx<String> strList = new ArrayListEx<>(sparseArray.size());
        for (int idx = 0; idx < sparseArray.size(); idx++) {
            strList.add(sparseArray.valueAt(idx));
        }
        return strList;
    }

    /**
     * Helper to get screen shot of View object.
     */
    private static Bitmap getBitmap(@NonNull View view) {
        Bitmap screenBitmap =
                Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        screenBitmap.eraseColor(Color.WHITE);
        Canvas canvas = new Canvas(screenBitmap);
        view.draw(canvas);
        return screenBitmap;
    }

    private static boolean isBitmapValid(@Nullable Bitmap bitmap) {
        return bitmap != null && !bitmap.isRecycled() && bitmap.getHeight() * bitmap.getWidth() > 0;
    }

    @SuppressWarnings({"SameParameterValue"})
    private static void shareList(
            @NonNull Activity activity,
            @Nullable List<Bitmap> shareImages,
            @Nullable List<String> shareStrings,
            @NonNull String toEmail,
            @NonNull String subject,
            String imageName,
            @Nullable ShareActionProvider shareActionProvider) {

        final String IMAGE_TYPE = "image/jpg";
        final String TEXT_TYPE = "text/plain";
        Intent shareIntent;

        if (shareImages != null && shareImages.size() > 0) {
            int imgCnt = shareImages.size();
            shareIntent = new Intent(imgCnt == 1 ? Intent.ACTION_SEND : Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType(IMAGE_TYPE);
            if (imgCnt == 1) {
                Bitmap bitmap = shareImages.get(0);
                if (!isBitmapValid(bitmap))
                    return;

                /*
                    // String screenImgFilename = Images.Media.insertImage(getContentResolver(), bitmap, imageName, null);
                    String screenImgFilename = Utils.saveBitmap(context, bitmap, imageName);

                    Uri uri = Uri.fromFile(new File(screenImgFilename));
                */
                Uri uri = getUriForBitmap(activity, bitmap, imageName);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                bitmap.recycle();
            } else {
                ArrayListEx<Uri> uris = new ArrayListEx<>();
                for (int bmIdx = 0; bmIdx != shareImages.size(); bmIdx++) {
                    Bitmap bitmap = shareImages.get(bmIdx);
                    if (isBitmapValid(bitmap)) {
                        /*
                        String screenImgFilename = Utils.saveBitmap(context, bitmap, String.valueOf(bmIdx) + imageName);
                        Uri uri = Uri.fromFile(new File(screenImgFilename));
                        */
                        Uri uri = getUriForBitmap(activity, bitmap, bmIdx + imageName);
                        uris.add(uri);
                        bitmap.recycle();
                    } else {
                        ALog.e.tagMsg(TAG, "invalid bitmap");
                    }
                }
                shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            }
        } else {
            shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(TEXT_TYPE);
        }

        String appName = activity.getString(R.string.app_name);
        String what = Build.MANUFACTURER + " " + Build.DEVICE;
        String appVersion = BuildConfig.VERSION_NAME;
        String shareBody = String.format("%s v%s\n%s\n%s\n",
                appName,
                appVersion,
                subject,
                what);

        if (shareStrings != null && shareStrings.size() > 0) {
            shareBody += "--------\n\n";
            shareBody += TextUtils.join("\n", shareStrings);
        }
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, appName + " " + subject);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        shareIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{toEmail});


    	/*
        if (IMAGE_TYPE.equals(shareIntent.getMimeType())) {
        	shareActionProvider.setHistoryFileName(SHARE_IMAGE_HISTORY_FILE_NAME);
        } else if (TEXT_TYPE.equals(shareIntent.getMimeType())) {
        	shareActionProvider.setHistoryFileName(SHARE_TEXT_HISTORY_FILE_NAME);
        }
        */
        //	if (shareActionProvider != null) {
        //		shareActionProvider.setShareIntent(shareIntent);
        //	} else {
        activity.startActivity(Intent.createChooser(shareIntent, "Share"));
        //	}
    }

    /**
     * Save bitmap to local filesystem
     *
     * @param bitmap   Bitmap to save
     * @param baseName Base filename used to save image, ex: "screenshot.png"
     * @return full filename path
     */
    private static Uri getUriForBitmap(@NonNull Context context, @NonNull Bitmap bitmap, String baseName) {

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "allThreadPenalty.jpg");
            values.put(MediaStore.Images.Media.TITLE, "Thread Penalty");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            Uri uri = context.getContentResolver()
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            // context.grantUriPermission(mContext.getPackageName(), uri,
            //         Intent.FLAG_GRANT_WRITE_URI_PERMISSION + Intent.FLAG_GRANT_READ_URI_PERMISSION);
            OutputStream ostream = context.getContentResolver().openOutputStream(uri);

            // Jpeg format about 20x faster to export then PNG and smaller image.
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, ostream);
            ostream.close();
            return uri;
        } catch (Exception ex) {
            ALog.e.tagMsg(TAG, "Save bitmap failed ", ex);
        }
        return null;
    }

    public static  List<ResolveInfo> queryIntentActivities(Context context, Intent intent) {
        List<ResolveInfo> activities;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activities = context.getPackageManager().queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0));
        } else {
            activities = context.getPackageManager().queryIntentActivities(intent, 0);
        }
        return activities;
    }

    /**
     * Share files either Email or View intents.
     */
    public static void shareFiles(@NonNull Context context, String email, String subject, String mimeType, Uri... uriArray) {
        ArrayListEx<Uri> uris = new ArrayListEx<>(uriArray.length);
        Collections.addAll(uris, uriArray);

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        // shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        shareIntent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");

        shareIntent.setType(mimeType);
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        shareIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        shareIntent.putExtra(Intent.EXTRA_TEXT, "file attached");

        Intent mailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
        mailIntent.setData(Uri.parse("mailto:" + email));
        List<ResolveInfo> activities = queryIntentActivities(context, mailIntent);


        Intent viewIntent = new Intent(Intent.ACTION_VIEW).setDataAndType(uriArray[0], mimeType);
        viewIntent.putExtra(Intent.EXTRA_STREAM, uriArray[0]);
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        List<ResolveInfo> viewActivities = queryIntentActivities(context, viewIntent);

        List<Intent> intents = new ArrayListEx<>(activities.size() + viewActivities.size());
        for (int idx = 0; idx < activities.size(); idx++) {
            intents.add(shareIntent);
        }
        for (int idx = 0; idx < viewActivities.size(); idx++) {
            intents.add(viewIntent);
        }

        activities.addAll(viewActivities);
        createShareDialog(context, activities, intents).show();
    }

    private static AlertDialog createShareDialog(
            @NonNull final Context context,
            @NonNull final List<ResolveInfo> activities,
            @NonNull final List<Intent> intents) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context, 0); // R.style.AppCompatAlertDialogStyle);

        builder.setTitle(SHARE_DIALOG_TITLE);
        final ShareIntentAdapter adapter = new ShareIntentAdapter((Activity) context,
                R.layout.share_dialog_list_item, activities.toArray(new ResolveInfo[0]));
        builder.setAdapter(adapter, (dialog, which) -> {
            ResolveInfo info = adapter.getItem(which);
            if (info != null) {
                Intent shareIntent = intents.get(which);
                shareIntent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
                try {
                    context.startActivity(shareIntent);
                } catch (Throwable ignore) {
                    // Intent could fail.
                }
            }
        });

        return builder.create();
    }

    /**
     * Share system resource log file.
     */
    public static void shareFileLog(Activity activity) {

        String mimeType = "text/csv";
        String helpEmailBody = "";

        try {
            helpEmailBody = "\nApp Version " + BuildConfig.VERSION_NAME
                    + "\n\nApp Name " + BuildConfig.APPLICATION_ID
                    + "\n\nOS " + Build.VERSION.RELEASE + " " + Build.VERSION.SDK_INT
                    + "\n\nDevice " + Build.MANUFACTURER + " " + Build.MODEL
                    + "\n\nNet " + NetInfo.getActiveType(activity)
            ;
            helpEmailBody += "\n\n";
        } catch (Exception ignore) {
        }

        //
        //  Attach files
        //    #2  Optional private log file
        //    #3  Last 'n' entries of our system log file.
        //
        ArrayListEx<Uri> uriList = new ArrayListEx<>();
        String auth = activity.getPackageName() + ".fileprovider";   // match Provider in androidManifest.xml

        // #2 Any private log file.
        try {
            if (ALogFileWriter.Default.getFile().length() > 0) {
                uriList.add(FileProvider.getUriForFile(activity, auth, ALogFileWriter.Default.getFile()));
            }
        } catch (final Throwable ex) {
            ALog.e.tagMsg(TAG, ex);
        }


        //  #3  System log file.
        try {
            File logFile;
            logFile = getLog(activity, "logcat -d *:D", "TWC_", "appLog.txt", 200);
            uriList.add(FileProvider.getUriForFile(activity, auth, logFile));
            // Radio log does not work - log output limited to this package's output.
            // logFile = getLog(activity, "logcat -b radio -d *:D", "radioLog.txt", 50);
            // uriList.add(FileProvider.getUriForFile(mContext, auth, logFile));
        } catch (Exception ignore) {
        }

        Uri[] uriArray = new Uri[uriList.size()];
        uriList.toArray(uriArray);
        activity.grantUriPermission(activity.getPackageName(), uriArray[0],
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION + Intent.FLAG_GRANT_READ_URI_PERMISSION);

        shareFiles(activity, "wsimobile2@gmail.com",
                activity.getPackageName().replace("com.landenlabs", "")
                        + helpEmailBody,
                mimeType, uriArray);

        // mCsvFilelog.open(activity);
        // mCsvFilelog.println(ALog.DEBUG, "", "console-email-done");

    }

    // Get last 'maxLines' of log file.
    //   logCmd = "logcat -d *:D"  debug min level
    //   logCmd = "logcat -d *:E"  error min level
    //    getLog(activity, "logcat -d *:D", "appLog.txt", 100);
    @SuppressWarnings({"ResultOfMethodCallIgnored", "SameParameterValue"})
    private static File getLog(Activity activity, String logCmd, String mustContain, String filename, int maxLines) throws IOException {
        String eol = "\n";
        File logFile = new File(activity.getFilesDir().getAbsolutePath() + "/logs", filename);
        if (logFile.length() > 0) {
            logFile.delete();
            logFile.createNewFile();
        }
        logFile.setWritable(true);
        BufferedWriter logWriter = new BufferedWriter(new java.io.FileWriter(logFile, true));

        logWriter.write("\n-------" + filename + "-------\n");

        try {
            Process process = Runtime.getRuntime().exec(logCmd);   // Verbose and higher
            BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int idxEnd = 0;
            ArrayListEx<String> lines = new ArrayListEx<>(maxLines);
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(mustContain)) {
                    if (lines.size() < maxLines) {
                        lines.add(line);
                    } else {
                        lines.set(idxEnd, line);
                    }
                    idxEnd = (idxEnd + 1) % maxLines;
                }
            }

            StringBuilder log = new StringBuilder();
            int idx = (lines.size() == maxLines) ? idxEnd : 0;
            int len = lines.size();
            while (len-- > 0) {
                log.append(lines.get(idx));
                log.append(eol);
                idx = (idx + 1) % maxLines;
            }
            bufferedReader.close();
            logWriter.write(log.toString());
        } catch (Exception ex) {
            ALog.e.tagMsg(TAG, "Failed to capture sys log file ", ex);
        }

        logWriter.close();
        return logFile;
    }

    /**
     * Populates share dialog view.
     */
    private static class ShareIntentAdapter extends ArrayAdapter<ResolveInfo> {
        private final Activity mContext;
        private final ResolveInfo[] mItems;
        private final int mLayoutId;
        private final LayoutInflater mInflater;

        private ShareIntentAdapter(Activity context, int layoutId, ResolveInfo[] items) {
            super(context, layoutId, items);

            mInflater = context.getLayoutInflater();
            mContext = context;
            mItems = items;
            mLayoutId = layoutId;
        }

        @NonNull
        @Override
        public View getView(int pos, View convertView, @NonNull ViewGroup parent) {
            convertView = mInflater.inflate(mLayoutId, null);
            TextView shateItemTitle = convertView.findViewById(R.id.share_item_title);
            shateItemTitle.setText((mItems[pos]).activityInfo.applicationInfo
                    .loadLabel(mContext.getPackageManager()).toString());
            ImageView shareItemLogo = convertView.findViewById(R.id.share_item_logo);
            shareItemLogo.setImageDrawable((mItems[pos]).activityInfo.applicationInfo
                    .loadIcon(mContext.getPackageManager()));

            return convertView;
        }
    }


}
