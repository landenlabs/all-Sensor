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

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.landenlabs.all_sensor.R;

/**
 * List widget configuration.
 * <p>
 * Add entry to AndroidManifest.xml
 * <p>
 * <activity android:name=".widget.WidConfigList1Activity"
 * android:label="@string/cfg_list_name"
 * android:theme="@style/Theme.MaterialComponents.Dialog">
 * <intent-filter>
 * <action android:name="android.appwidget.action.APPWIDGET_CONFIGURE"/>
 * </intent-filter>
 * </activity>
 */
public class WidConfigList1Activity extends WidConfigActivity {

    public WidConfigList1Activity() {
        super(WidViewList1.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.widget_config);
        // widCfg.setWidType(WidViewList1.class);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void setUi(@NonNull WidCfg widCfg) {
        if (widCfg.firstTime()) {
            widCfg.showHistory = false;
        }
        super.setUi(widCfg);
    }
}
