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

package com.landenlabs.all_sensor.ui.graphs;

import static com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.Units;
import com.landenlabs.all_sensor.sensor.AbsCfg;
import com.landenlabs.all_sensor.sensor.SensorAndroid;
import com.landenlabs.all_sensor.sensor.SensorItem;
import com.landenlabs.all_sensor.sensor.SensorListManager;
import com.landenlabs.all_sensor.sensor.WxManager;
import com.landenlabs.all_sensor.utils.ArrayListEx;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Graph Sensor database data using MPAndroidChart.
 * <p>
 * <a href="https://github.com/halfhp/androidplot/blob/master/docs/quickstart.md">androidPlot</a>
 * <p>
 * GitHub<br>
 * <a href="https://github.com/PhilJay/MPAndroidChart">MPAndroidChart</a>
 * <p>
 * Release notes<br>
 * <a href="https://github.com/PhilJay/MPAndroidChart/releases">MPAndroidChart release notes</a>
 */
@SuppressWarnings({"SameParameterValue", "SuspiciousNameCombination"})
class GraphP extends GraphBase {

    private static final int SERIES_COLOR = Color.GREEN;
    private Drawable graphFillDrawable;
    public Units.SampleBy sampleBy = Units.SampleBy.Hours;

    // ---------------------------------------------------------------------------------------------
    @Override
    protected boolean initGraph(
            @NonNull View root,
            @NonNull AbsCfg cfg,
            @NonNull Interval interval,
            @NonNull Units.SampleBy sampleBy) {
        if (super.initGraph(root, cfg, interval, sampleBy)) {
            mpPlot.getAxisLeft().setTextColor(SERIES_COLOR);
            mpPlot.getAxisLeft().setTextSize(AXIS_TEXT_SIZE);
            mpPlot.getAxisLeft().setValueFormatter(new ValueFormatter() {
                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    return String.format("%.0f", value);
                }
            });
            graphFillDrawable = ContextCompat.getDrawable(root.getContext(), R.drawable.graph_temperature_fill);
            mpPlot.getAxisRight().setEnabled(false);
            return true;
        } else {
            mpPlot.clear();
            return false;
        }
    }

    Interval lastInterval = null;
    long lastSampleSize = 0;
    Units.SampleBy lastSampleBy = null;

    void updateGraph(
            @NonNull View root,
            @NonNull WxManager wxManager,
            @NonNull SensorItem device,
            @NonNull Interval interval,
            @NonNull AbsCfg cfg) {
        if (!sampleBy.equals(lastSampleBy)) {
            lastSampleBy = sampleBy;
            lastInterval = null;
            if (mpPlot != null && !mpPlot.isEmpty()) {
                mpPlot.clearValues();
            }
        }
        if (sampleBy == Units.SampleBy.Hours) {
            updateHourlyGraph(root, wxManager, device, interval, cfg);
        } else {
            updateDailyGraph(root, wxManager, device, interval, cfg);
        }
    }

    void updateHourlyGraph(
            @NonNull View root,
            @NonNull WxManager ignore,
            @NonNull SensorItem device,
            @NonNull Interval interval,
            @NonNull AbsCfg cfg) {
        mult = 1;
        ArrayListEx<SensorAndroid.SensorSample> hourlySamples = SensorListManager.getHourlyList(device.name, interval);
        if (hourlySamples == null || hourlySamples.size() == 0) {
            return;
        }
        if (interval.equals(lastInterval) && hourlySamples.size() == lastSampleSize) {
            return; // Nothing changed.
        }
        lastInterval = interval;
        lastSampleSize = hourlySamples.size();

        initGraph(root, cfg, interval, sampleBy);
        int numSamples = hourlySamples.size();
        Map<Integer, Long> startDayPosMap = new HashMap<>(numSamples / 24 + 1);

        // Trim data length to show at most "numPlot" values and start on requested Day.
        int startIdx = 0;
        while (startIdx < numSamples) {
            DateTime sampleDt = new DateTime(hourlySamples.get(startIdx).milli);
            if (sampleDt.isBefore(interval.getStart())) {
                startIdx++;
            } else {
                break;
            }
        }

        /*
        // Adjust start to fall on the 'start of a day'
        if (startIdx + 24 < numSamples) {
            startTime = new DateTime(hourlySamples.get(startIdx).milli).withSecondOfMinute(0).withMillisOfSecond(0);

            if (startTime.getHourOfDay() < 12) {
                startIdx = Math.max(0, startIdx - startTime.getHourOfDay());
            }
            while (startIdx < numSamples - 24) {
                final SensorAndroid.Pressure sample = hourlySamples.get(startIdx);
                DateTime dt = new DateTime(sample.milli);
                if (dt.getHourOfDay() == 0) {
                    break;
                }
                startIdx++;
            }
        }
        */

        // Populate Graph with uniform hourly offsets, handling missing data correctly.
        // Get "start of day" positions for later addition of Xaxis vertical lines.
        ArrayList<Entry> temSeries = new ArrayList<>(numSamples);
        startTime = new DateTime(hourlySamples.get(startIdx).milli).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
        DateTime nextStartOfDay = startTime.withTimeAtStartOfDay().plusDays(1);
        int startDayPos = startIdx;
        DateTime nextTm = startTime;
        long startDayMillis = startTime.withTimeAtStartOfDay().plusDays(1).getMillis();

        for (int dataIdx = startIdx; dataIdx < numSamples; dataIdx++) {
            final SensorAndroid.SensorSample sample = hourlySamples.get(dataIdx);
            DateTime dt = new DateTime(sample.milli).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
            int hourIdx = Hours.hoursBetween(startTime, dt).getHours();

            // Optionally add missing filler to always have a midnight entry.
            while (dt.isAfter(nextStartOfDay)) {
                int fillerIdx = Hours.hoursBetween(startTime, nextStartOfDay).getHours();
                if (fillerIdx + 1 < hourIdx) {
                    temSeries.add(new Entry(fillerIdx, sample.fvalue));
                }
                startDayPosMap.put(fillerIdx, nextStartOfDay.getMillis());
                nextStartOfDay = nextStartOfDay.plusDays(1);
                startDayMillis = nextStartOfDay.getMillis();
            }

            // Add data at hourly spacing
            temSeries.add(new Entry(hourIdx, sample.fvalue));

            // Save midnight positions for later use to add day markers.
            if (startDayMillis >= sample.milli) {
                startDayPos = hourIdx;
            } else {
                startDayPosMap.put(startDayPos, startDayMillis);
                startDayMillis += TimeUnit.DAYS.toMillis(1);
            }
        }

        addSet("Hr Press mb", temSeries, SERIES_COLOR, graphFillDrawable, LEFT, 0);

        /*
        // Add dummy series to force gap on right edge
        float freezeValue = (cfg.tunit == Units.Temperature.Fahrenheit) ? 32F : 0F;
        ArrayList<Entry> dummySeries = new ArrayList<>(2);
        dummySeries.add(new Entry(0, freezeValue));
        dummySeries.add(new Entry(temSeries.size()+2, freezeValue));
        addSet("", dummySeries, Color.TRANSPARENT, null, LEFT, 2);
        */

        mpPlot.getXAxis().removeAllLimitLines();
        for (Map.Entry<Integer, Long> dayPosAndMilli : startDayPosMap.entrySet()) {
            DateTime dt = new DateTime(dayPosAndMilli.getValue());
            boolean lblBottom = (dt.getDayOfYear() & 1) == 1;
            addDayLine(mpPlot.getXAxis(), dayPosAndMilli.getKey(), dayFmt.print(dt), lblBottom);
        }

        LineDataSet set0 = (LineDataSet) mpPlot.getData().getDataSetByIndex(0);

        final int NUM_LABELS = 6;
        normalizeAxis(mpPlot.getAxisLeft(), NUM_LABELS, 5, set0);

        mpPlot.getData().notifyDataChanged();
        mpPlot.notifyDataSetChanged();
        mpPlot.invalidate();
    }

    /**
     * Graph 1 line, splined max,min temperature and splines max,min humidity
     */
    void updateDailyGraph(
            @NonNull View root,
            @NonNull WxManager ignore,
            @NonNull SensorItem device,
            @NonNull Interval interval,
            @NonNull AbsCfg cfg) {
        mult = 1;
        // ArrayListEx<DeviceGoveeHelper.Record> recordSamples = DeviceListManager.getDailyList(device.name, interval);
        ArrayListEx<SensorAndroid.SensorSample> hourlySamples = SensorListManager.getHourlyList(device.name, interval);
        if (hourlySamples == null || hourlySamples.isEmpty()) {
            return;
        }
        ArrayListEx<SensorAndroid.SensorSample> recordSamples = new ArrayListEx<>(hourlySamples.size()/24 + 1);
        int lastDayIdx =  Days.daysBetween(startTime, new DateTime(hourlySamples.get(0).milli)).getDays();
        float value = 0;
        int count = 0;
        for (SensorAndroid.SensorSample sample : hourlySamples) {
            DateTime dt = new DateTime(sample.milli);
            int dayIdx = Days.daysBetween(startTime, dt).getDays();
            if (dayIdx == lastDayIdx) {
                value += sample.fvalue;
                count++;
            } else {
                SensorAndroid.SensorSample dailyAvg = new SensorAndroid.SensorSample(startTime.plusDays(lastDayIdx).getMillis(), value/count);
                recordSamples.add(dailyAvg);
                value = sample.fvalue;
                count = 1;
                lastDayIdx = dayIdx;
            }
        }

        if (interval.equals(lastInterval) && recordSamples.size() == lastSampleSize) {
            return; // Nothing changed.
        }
        lastInterval = interval;
        lastSampleSize = recordSamples.size();

        initGraph(root, cfg, interval, sampleBy);
        int numSamples = recordSamples.size();
        if (numSamples < 2) {
            return; // Nothing to show
        }
        yAxisDayFmt = numSamples < 10 ? yAxisDayFmt1 : yAxisDayFmt2;

        // Trim data length to show at most "numPlot" values and start on requested Day.
        int startIdx = 0;
        while (startIdx < numSamples) {
            DateTime sampleDt = new DateTime(recordSamples.get(startIdx).milli);
            if (sampleDt.isBefore(interval.getStart())) {
                startIdx++;
            } else {
                break;
            }
        }

        // Populate Graph with uniform daily offsets, handling missing data correctly.
        // Get "start of day" positions for later addition of Xaxis vertical lines.
        ArrayListEx<Entry> temSeries = new ArrayListEx<>(numSamples);
        startTime = new DateTime(recordSamples.get(startIdx).milli).withSecondOfMinute(0).withMillisOfSecond(0);
        DateTime nextStartOfDay = startTime.withTimeAtStartOfDay().plusDays(1);
        // int startDayPos = startIdx;
        long startDayMillis = nextStartOfDay.getMillis();

        for (int dataIdx = startIdx; dataIdx < numSamples; dataIdx++) {
            final SensorAndroid.SensorSample sample = recordSamples.get(dataIdx);
            DateTime dt = new DateTime(sample.milli).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
            int dayIdx = Days.daysBetween(startTime, dt).getDays();

            // Optionally add missing filler to always have a midnight entry.
            while (dt.isAfter(nextStartOfDay)) {
                int fillerIdx = Days.daysBetween(startTime, nextStartOfDay).getDays();
                if (fillerIdx + 1 < dayIdx) {
                    temSeries.add(new Entry(fillerIdx, sample.fvalue));
                }
                // startDayPosMap.put(fillerIdx, nextStartOfDay.getMillis());
                nextStartOfDay = nextStartOfDay.plusDays(1);
                startDayMillis = nextStartOfDay.getMillis();
            }

            // Add data at hourly spacing
            temSeries.add(new Entry(dayIdx, sample.fvalue));

            // Save midnight positions for later use to add day markers.
            if (startDayMillis >= sample.milli) {
                // startDayPos = dayIdx;
            } else {
                // startDayPosMap.put(startDayPos, startDayMillis);
                startDayMillis += TimeUnit.DAYS.toMillis(1);
            }
        }

        temSeries.sort(new EntryXComparator());

        int setIdx = 0;
        addSet("Day Press mb", temSeries, SERIES_COLOR, graphFillDrawable, LEFT, setIdx++);
        //xx addSet("Humidity", humSeries, HUMIDITY_COLOR, humidityFillDrawable, RIGHT, setIdx++);

        mpPlot.getXAxis().removeAllLimitLines();

        /*
        // Add dummy series to force gap on right edge
        float freezeValue = (cfg.tunit == Units.Temperature.Fahrenheit) ? 32F : 0F;
        ArrayList<Entry> dummySeries = new ArrayList<>(2);
        dummySeries.add(new Entry(0, freezeValue));
        dummySeries.add(new Entry(temSeries.size()+0.1f, freezeValue));
        addSet("", dummySeries, Color.TRANSPARENT, null, LEFT, 2);
        */

        if (true) {
            LineDataSet setTem = (LineDataSet) mpPlot.getData().getDataSetByIndex(0);
            // set0.calcMinMax();

            final int NUM_LABELS = 6;
            normalizeAxis(mpPlot.getAxisLeft(), NUM_LABELS, 5, setTem);

            if (false) {
                setTem.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                // Enable the cubic density : if 1 then it will be sharp curve
                setTem.setCubicIntensity(0.2f);
            }
        }

        mpPlot.getData().notifyDataChanged();
        mpPlot.notifyDataSetChanged();
        mpPlot.invalidate();
    }
}
