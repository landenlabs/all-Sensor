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

import static com.landenlabs.all_sensor.sensor.DeviceGoveeHelper.toDegreeF;
import static com.landenlabs.all_sensor.sensor.DeviceGoveeHelper.toDeltaDegreeF;
import static com.landenlabs.all_sensor.sensor.DeviceGoveeHelper.toPercent;
import static com.landenlabs.all_sensor.widget.WidView.getPref;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.SpannableString;

import androidx.annotation.NonNull;

import com.landenlabs.all_sensor.Units;
import com.landenlabs.all_sensor.utils.SpanUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Abstract presentation configuration, units(celsius or fahrenheit) and widget settings.
 */
public abstract class AbsCfg {

    public int id = -1;
    public Units.Temperature tunit = Units.Temperature.Fahrenheit;
    public Units.Pressure punit = Units.Pressure.mb;
    public Units.Percent bunit = Units.Percent.percent;

    private static SimpleDateFormat TIME_HHMMA;
    private static SimpleDateFormat DATE_MDEHHMMA;

    private static final String PREF_UNIT_TEMP  = "AbsCfgUtmp";
    private static final String PREF_UNIT_PRESS = "AbsCfgUprs";
    private static final String PREF_UNIT_PERC  = "AbsCfgUper";

    // ---------------------------------------------------------------------------------------------

    public void save(@NonNull Context context) {
        SharedPreferences.Editor pref = getPref(context).edit();
        pref.putString(PREF_UNIT_TEMP+id, tunit.toString());
        pref.putString(PREF_UNIT_PRESS+id, punit.toString());
        pref.putString(PREF_UNIT_PERC+id, bunit.toString());
        pref.apply();
    }

    public void restore(@NonNull Context context, int Id) {
        this.id = Id;
        SharedPreferences pref = getPref(context);
        tunit = Units.Temperature.valueOf( pref.getString(PREF_UNIT_TEMP+id, tunit.toString()));
        punit = Units.Pressure.valueOf( pref.getString(PREF_UNIT_PRESS+id, punit.toString()));
        bunit = Units.Percent.valueOf( pref.getString(PREF_UNIT_PERC+id, bunit.toString()));
    }

    // ---------------------------------------------------------------------------------------------
    // Default implementations

    public float toDegreeN(int tempC100) {
        return (tunit == Units.Temperature.Fahrenheit)
                ? toDegreeF(tempC100)
                : tempC100 / 100.0f;
    }

    public String tempFmt() {
        return "%.2f°%s";
    }

    public String toDegree(int tempC100) {
        return String.format(tempFmt(), toDegreeN(tempC100), tunit.id);
    }

    public String toDeltaDegree(int tempC100) {
        return String.format(tempFmt(), (tunit == Units.Temperature.Fahrenheit)
                        ? toDeltaDegreeF(tempC100)
                        : tempC100 / 100.0f,
                tunit.id);
    }

    public String toBattery(float deltaBatLevel) {
        return String.format("%.0f%s", deltaBatLevel, bunit.id);
    }
    public String toPress(float deltaPress) {
        return String.format("%.0f%s", deltaPress, punit.id);
    }
    public String toWifi(float wifi) {
        return String.format("%.0f%%", wifi);
    }

    public float toHumPerN(int humP100) {
        return toPercent(humP100);
    }

    public String humFmt() {
        return "%.2f%%";
    }

    public String toHumPer(int humP100) {
        return String.format(humFmt(), toHumPerN(humP100));
    }

    public String intervalUnit() {
        return "/hr";
    }


    public SpannableString timeFmt(long milli) {
        if (TIME_HHMMA == null)
            TIME_HHMMA = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        // return TIME_HHMMA;
        String strTm = TIME_HHMMA.format(new Date(milli));
        return SpanUtil.SString(strTm, SpanUtil.SS_SMALLER_50, strTm.length()-2, strTm.length());
    }

    public SpannableString dateFmt(long milli) {
        if (DATE_MDEHHMMA == null)
            DATE_MDEHHMMA = new SimpleDateFormat("MMM dd E hh:mma", Locale.getDefault());
        // return DATE_MDEHHMMA;
        String strTm = DATE_MDEHHMMA.format(new Date(milli));
        return SpanUtil.SString(strTm, SpanUtil.SS_SMALLER_50, strTm.length()-2, strTm.length());
    }
}

