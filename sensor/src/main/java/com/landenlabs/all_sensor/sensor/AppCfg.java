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

import static com.landenlabs.all_sensor.widget.WidView.getPref;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

/**
 * Application presentation configuration.
 */
public class AppCfg extends AbsCfg {

    static AppCfg AppCFG;

    public boolean showNotification;
    public boolean showStatusBar;
    public boolean soundOnUpdate;
    public String soundFile;

    private static final String PREF_SHOW_NOTIFY = "AppCfgNotify";
    private static final String PREF_SHOW_SB = "AppCfgShowSb";
    private static final String PREF_DO_SND = "AppCfgDoSnd";
    private static final String PREF_SND_FILE = "AppCfgSndFile";

    // ---------------------------------------------------------------------------------------------

    synchronized
    public static AppCfg getInstance(Context context) {
        if (AppCFG == null) {
            AppCFG = new AppCfg();
            AppCFG.showNotification = true;
            AppCFG.showStatusBar = false;
            AppCFG.restore(context, AppCFG.id);
        }
        return AppCFG;
    }

    @CallSuper
    @Override
    public void save(@NonNull Context context) {
        SharedPreferences.Editor pref = getPref(context).edit();
        pref.putBoolean(PREF_SHOW_NOTIFY+id, showNotification);
        pref.putBoolean(PREF_SHOW_SB+id, showStatusBar);
        pref.putBoolean(PREF_DO_SND+id, soundOnUpdate);
        pref.putString(PREF_SND_FILE+id, soundFile);
        pref.apply();
    }

    @CallSuper
    @Override
    public void restore(@NonNull Context context, int id) {
        SharedPreferences pref = getPref(context);
        showNotification = pref.getBoolean(PREF_SHOW_NOTIFY+id, showNotification);
        showStatusBar = pref.getBoolean(PREF_SHOW_SB+id, showStatusBar);
        soundOnUpdate = pref.getBoolean(PREF_DO_SND+id, soundOnUpdate);
        soundFile = pref.getString(PREF_SND_FILE+id, soundFile);
    }
}
