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
import static com.github.mikephil.charting.components.YAxis.AxisDependency.RIGHT;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.Units;
import com.landenlabs.all_sensor.sensor.AbsCfg;
import com.landenlabs.all_sensor.sensor.DeviceGoveeHelper;
import com.landenlabs.all_sensor.sensor.DeviceItem;
import com.landenlabs.all_sensor.sensor.DeviceListManager;
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
 * https://github.com/halfhp/androidplot/blob/master/docs/quickstart.md
 * <p>
 * GitHub
 * https://github.com/PhilJay/MPAndroidChart
 * <p>
 * Release notes
 * https://github.com/PhilJay/MPAndroidChart/releases
 */
@SuppressWarnings({"SameParameterValue", "SuspiciousNameCombination"})
class GraphTH extends GraphBase {

    private static final int TEMPERATURE_COLOR = Color.GREEN;
    private static final int HUMIDITY_COLOR = 0xff8080ff;
    private Drawable temperatureFillDrawable;
    private Drawable humidityFillDrawable;
    public Units.SampleBy sampleBy = Units.SampleBy.Hours;

    // ---------------------------------------------------------------------------------------------
    @Override
    protected boolean initGraph(
            @NonNull View root,
            @NonNull AbsCfg cfg,
            @NonNull Interval interval,
            @NonNull Units.SampleBy sampleBy) {
        if (super.initGraph(root, cfg, interval, sampleBy)) {
            mpPlot.getAxisLeft().setTextColor(TEMPERATURE_COLOR);
            mpPlot.getAxisLeft().setTextSize(AXIS_TEXT_SIZE);
            mpPlot.getAxisLeft().setValueFormatter(new ValueFormatter() {
                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    return String.format("%.0f", value);
                }
            });
            temperatureFillDrawable = ContextCompat.getDrawable(root.getContext(), R.drawable.graph_temperature_fill);

            mpPlot.getAxisRight().setEnabled(true);
            mpPlot.getAxisRight().setTextColor(HUMIDITY_COLOR);
            mpPlot.getAxisRight().setTextSize(AXIS_TEXT_SIZE);
            mpPlot.getAxisRight().setValueFormatter(new ValueFormatter() {
                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    return String.format("%.0f", value);
                }
            });
            humidityFillDrawable = ContextCompat.getDrawable(root.getContext(), R.drawable.graph_humidity_fill);

            // mpPlot.setDomainLabel("Hours");
            // mpPlot.setRangeLabel("Temperature °F");

            float freezeValue = (cfg.tunit == Units.Temperature.Fahrenheit) ? 32F : 0F;
            LimitLine freezeLine = new LimitLine(freezeValue, "Freezing");
            freezeLine.setLineColor(Color.RED);
            freezeLine.setLineWidth(lineOnlyWidth / 2f);
            //    ll1.enableDashedLine(10f, 10f, 0f);
            freezeLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
            freezeLine.setTextSize(LABEL_TEXT_SIZE);
            freezeLine.setTextColor(Color.RED);
            mpPlot.getAxisLeft().removeAllLimitLines();
            mpPlot.getAxisLeft().addLimitLine(freezeLine);

            /*
            CustomMarkerView mv = new CustomMarkerView(root.getContext(), R.layout.chart_live_label);
            mpPlot.setMarkerView(mv);
            */
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
            @NonNull DeviceItem device,
            @NonNull Interval interval,
            @NonNull AbsCfg cfg) {
        if (!sampleBy.equals(lastSampleBy)) {
            lastSampleBy = sampleBy;
            lastInterval = null;
            if (mpPlot != null) {
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
            @NonNull DeviceItem device,
            @NonNull Interval interval,
            @NonNull AbsCfg cfg) {
        mult = 1;
        ArrayListEx<DeviceGoveeHelper.Sample> hourlySamples = DeviceListManager.getHourlyList(device.name, interval);
        if (hourlySamples.size() == 0) {
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

        // Adjust start to fall on the 'start of a day'
        if (startIdx + 24 < numSamples) {
            startTime = new DateTime(hourlySamples.get(startIdx).milli).withSecondOfMinute(0).withMillisOfSecond(0);

            if (startTime.getHourOfDay() < 12) {
                startIdx = Math.max(0, startIdx - startTime.getHourOfDay());
            }
            while (startIdx < numSamples - 24) {
                final DeviceGoveeHelper.Sample sample = hourlySamples.get(startIdx);
                DateTime dt = new DateTime(sample.milli);
                if (dt.getHourOfDay() == 0) {
                    break;
                }
                startIdx++;
            }
        }

        // Populate Graph with uniform hourly offsets, handling missing data correctly.
        // Get "start of day" positions for later addition of Xaxis vertical lines.
        ArrayList<Entry> temSeries = new ArrayList<>(numSamples);
        ArrayList<Entry> humSeries = new ArrayList<>(numSamples);
        startTime = new DateTime(hourlySamples.get(startIdx).milli).withSecondOfMinute(0).withMillisOfSecond(0);
        DateTime nextStartOfDay = startTime.withTimeAtStartOfDay().plusDays(1);
        int startDayPos = startIdx;
        long startDayMillis = nextStartOfDay.getMillis();

        for (int dataIdx = startIdx; dataIdx < numSamples; dataIdx++) {
            final DeviceGoveeHelper.Sample sample = hourlySamples.get(dataIdx);
            DateTime dt = new DateTime(sample.milli).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
            int hourIdx = Hours.hoursBetween(startTime, dt).getHours();

            // Optionally add missing filler to always have a midnight entry.
            while (dt.isAfter(nextStartOfDay)) {
                int fillerIdx = Hours.hoursBetween(startTime, nextStartOfDay).getHours();
                if (fillerIdx + 1 < hourIdx) {
                    temSeries.add(new Entry(fillerIdx, cfg.toDegreeN(sample.tem)));
                    humSeries.add(new Entry(fillerIdx, cfg.toHumPerN(sample.hum)));
                }
                startDayPosMap.put(fillerIdx, nextStartOfDay.getMillis());
                nextStartOfDay = nextStartOfDay.plusDays(1);
                startDayMillis = nextStartOfDay.getMillis();
            }

            // Add data at hourly spacing
            temSeries.add(new Entry(hourIdx, cfg.toDegreeN(sample.tem)));
            humSeries.add(new Entry(hourIdx, cfg.toHumPerN(sample.hum)));

            // Save midnight positions for later use to add day markers.
            if (startDayMillis >= sample.milli) {
                startDayPos = hourIdx;
            } else {
                startDayPosMap.put(startDayPos, startDayMillis);
                startDayMillis += TimeUnit.DAYS.toMillis(1);
            }
        }

        addSet("Temperature °F", temSeries, TEMPERATURE_COLOR, temperatureFillDrawable, LEFT, 0);
        addSet("Humidity", humSeries, HUMIDITY_COLOR, humidityFillDrawable, RIGHT, 1);

        // Add dummy series to force gap on right edge
        float freezeValue = (cfg.tunit == Units.Temperature.Fahrenheit) ? 32F : 0F;
        ArrayList<Entry> dummySeries = new ArrayList<>(2);
        dummySeries.add(new Entry(0, freezeValue));
        dummySeries.add(new Entry(temSeries.size()+2, freezeValue));
        addSet("", dummySeries, Color.TRANSPARENT, null, LEFT, 2);

        mpPlot.getXAxis().removeAllLimitLines();
        for (Map.Entry<Integer, Long> dayPosAndMilli : startDayPosMap.entrySet()) {
            DateTime dt = new DateTime(dayPosAndMilli.getValue());
            boolean lblBottom = (dt.getDayOfYear() & 1) == 1;
            addDayLine(mpPlot.getXAxis(), dayPosAndMilli.getKey(), dayFmt.print(dt), lblBottom);
        }

        LineDataSet set0 = (LineDataSet) mpPlot.getData().getDataSetByIndex(0);
        LineDataSet set1 = (LineDataSet) mpPlot.getData().getDataSetByIndex(1);
        // set0.calcMinMax();

        final int NUM_LABELS = 6;
        normalizeAxis(mpPlot.getAxisLeft(), NUM_LABELS, 5, set0);
        normalizeAxis(mpPlot.getAxisRight(), NUM_LABELS, 10, set1);

        mpPlot.getData().notifyDataChanged();
        mpPlot.notifyDataSetChanged();
        mpPlot.invalidate();
    }


    /* *
     * Graph 4 lines, max & min temperature and max & min humidity
     */
    /*
    void updateDailyGraph4(
            @NonNull View root,
            @NonNull WxManager wxManager,
            @NonNull DeviceItem device,
            @NonNull Interval interval,
            @NonNull AbsCfg cfg) {
        mult = 2;
        ArrayListEx<DeviceGoveeHelper.Record> recordSamples = DeviceListManager.getDailyList(device.name, interval);
        if (recordSamples.size() == 0) {
            return;
        }
        if (interval.equals(lastInterval) && recordSamples.size() == lastSampleSize) {
            return; // Nothing changed.
        }
        lastInterval = interval;
        lastSampleSize = recordSamples.size();

        initGraph(root);
        int numSamples = recordSamples.size();
        yAxisDayFmt = numSamples < 10 ? yAxisDayFmt1 : yAxisDayFmt2;

        // Trim data length to show at most "numPlot" values and start on requested Day.
        int startIdx = 0;
        while (startIdx < numSamples) {
            DateTime sampleDt = new DateTime(recordSamples.get(startIdx).dayMilli);
            if (sampleDt.isBefore(interval.getStart())) {
                startIdx++;
            } else {
                break;
            }
        }

        // Populate Graph with uniform daily offsets, handling missing data correctly.
        // Get "start of day" positions for later addition of Xaxis vertical lines.
        ArrayList<Entry> temMinSeries = new ArrayList<>(numSamples);
        ArrayList<Entry> temMaxSeries = new ArrayList<>(numSamples);
        ArrayList<Entry> humMinSeries = new ArrayList<>(numSamples);
        ArrayList<Entry> humMaxSeries = new ArrayList<>(numSamples);
        startTime = new DateTime( recordSamples.get(startIdx).dayMilli).withSecondOfMinute(0).withMillisOfSecond(0);
        DateTime nextStartOfDay = startTime.withTimeAtStartOfDay().plusDays(1);
        int startDayPos = startIdx;
        long startDayMillis = nextStartOfDay.getMillis();

        for (int dataIdx = startIdx; dataIdx < numSamples; dataIdx++) {
            final DeviceGoveeHelper.Record sample = recordSamples.get(dataIdx);
            DateTime dt = new DateTime(sample.dayMilli).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
            int dayIdx = Days.daysBetween(startTime, dt).getDays();

            // Optionally add missing filler to always have a midnight entry.
            while (dt.isAfter(nextStartOfDay)) {
                int fillerIdx = Days.daysBetween(startTime, nextStartOfDay).getDays();
                if (fillerIdx +1 < dayIdx) {
                    temMinSeries.add(new Entry(fillerIdx, toDegreeF(sample.tempMin.val)));
                    temMaxSeries.add(new Entry(fillerIdx, toDegreeF(sample.tempMax.val)));
                    humMinSeries.add(new Entry(fillerIdx, toPercent(sample.humMin.val)));
                    humMaxSeries.add(new Entry(fillerIdx, toPercent(sample.humMax.val)));
                }
                // startDayPosMap.put(fillerIdx, nextStartOfDay.getMillis());
                nextStartOfDay = nextStartOfDay.plusDays(1);
                startDayMillis = nextStartOfDay.getMillis();
            }

            // Add data at hourly spacing
            if (true) {
                temMinSeries.add(DayEntry(dayIdx, startTime, sample.tempMin, rec -> toDegreeF(rec.val)));
                temMaxSeries.add(DayEntry(dayIdx, startTime, sample.tempMax, rec -> toDegreeF(rec.val)));
                humMinSeries.add(DayEntry(dayIdx, startTime, sample.humMin, rec -> toPercent(rec.val)));
                humMaxSeries.add(DayEntry(dayIdx, startTime, sample.humMax, rec -> toPercent(rec.val)));
            } else {
                temMinSeries.add(new Entry(dayIdx, toDegreeF(sample.tempMin.val)));
                temMaxSeries.add(new Entry(dayIdx, toDegreeF(sample.tempMax.val)));
                humMinSeries.add(new Entry(dayIdx, toPercent(sample.humMin.val)));
                humMaxSeries.add(new Entry(dayIdx, toPercent(sample.humMax.val)));
            }

            // Save midnight positions for later use to add day markers.
            if (startDayMillis >= sample.dayMilli) {
                startDayPos = dayIdx;
            } else {
                // startDayPosMap.put(startDayPos, startDayMillis);
                startDayMillis += TimeUnit.DAYS.toMillis(1);
            }
        }

        @ColorInt final int TEMP_MIN_COLOR = 0x8000ff00;    // green
        @ColorInt final int TEMP_MAX_COLOR = 0x80ff0000;    // red
        @ColorInt final int HUM_MIN_COLOR = 0xff88fbff;     // light blue
        @ColorInt final int HUM_MAX_COLOR = 0xff8080ff;     // dark blue

        int setIdx = 0;
        addSet("TMin °F", temMinSeries, TEMP_MIN_COLOR, LEFT, setIdx++);  // set #0
        addSet("TMax °F", temMaxSeries, TEMP_MAX_COLOR, LEFT, setIdx++);  // set #1
        addSet("HMin", humMinSeries, HUM_MIN_COLOR, RIGHT, setIdx++);      // set #2
        addSet("HMax", humMaxSeries, HUM_MAX_COLOR, RIGHT, setIdx);        // set #3

        / *
        mpPlot.getXAxis().removeAllLimitLines();
        for (Map.Entry<Integer, Long> dayPosAndMilli : startDayPosMap.entrySet()) {
            DateTime dt = new DateTime(dayPosAndMilli.getValue());
            boolean lblBottom = (dt.getDayOfYear() & 1) == 1;
            addDayLine(mpPlot.getXAxis(), dayPosAndMilli.getKey(), dayFmt.print(dt), lblBottom);
        }
         * /
        if (true) {
            LineDataSet setTemMin = (LineDataSet)mpPlot.getData().getDataSetByIndex(0);
            LineDataSet setTemMax = (LineDataSet)mpPlot.getData().getDataSetByIndex(1);
            LineDataSet setHumMin = (LineDataSet)mpPlot.getData().getDataSetByIndex(2);
            LineDataSet setHumMax = (LineDataSet)mpPlot.getData().getDataSetByIndex(3);
            // set0.calcMinMax();

            final int NUM_LABELS = 6;
            normalizeAxis( mpPlot.getAxisLeft(), NUM_LABELS, 5, setTemMin, setTemMax);
            normalizeAxis(mpPlot.getAxisRight(), NUM_LABELS, 10, setHumMin, setHumMax);
        }

        mpPlot.getData().notifyDataChanged();
        mpPlot.notifyDataSetChanged();
        mpPlot.invalidate();
    }
    */

    /**
     * Graph 2 lines, splined max,min temperature and splines max,min humidity
     */
    void updateDailyGraph(
            @NonNull View root,
            @NonNull WxManager ignore,
            @NonNull DeviceItem device,
            @NonNull Interval interval,
            @NonNull AbsCfg cfg) {
        mult = 1;
        ArrayListEx<DeviceGoveeHelper.Record> recordSamples = DeviceListManager.getDailyList(device.name, interval);
        if (recordSamples.size() == 0) {
            return;
        }
        if (interval.equals(lastInterval) && recordSamples.size() == lastSampleSize) {
            return; // Nothing changed.
        }
        lastInterval = interval;
        lastSampleSize = recordSamples.size();

        initGraph(root, cfg, interval, sampleBy);
        int numSamples = recordSamples.size();
        yAxisDayFmt = numSamples < 10 ? yAxisDayFmt1 : yAxisDayFmt2;

        // Trim data length to show at most "numPlot" values and start on requested Day.
        int startIdx = 0;
        while (startIdx < numSamples) {
            DateTime sampleDt = new DateTime(recordSamples.get(startIdx).dayMilli);
            if (sampleDt.isBefore(interval.getStart())) {
                startIdx++;
            } else {
                break;
            }
        }

        // Populate Graph with uniform daily offsets, handling missing data correctly.
        // Get "start of day" positions for later addition of Xaxis vertical lines.
        ArrayListEx<Entry> temSeries = new ArrayListEx<>(numSamples);
        ArrayListEx<Entry> humSeries = new ArrayListEx<>(numSamples);
        startTime = new DateTime(recordSamples.get(startIdx).dayMilli).withSecondOfMinute(0).withMillisOfSecond(0);
        DateTime nextStartOfDay = startTime.withTimeAtStartOfDay().plusDays(1);
        // int startDayPos = startIdx;
        long startDayMillis = nextStartOfDay.getMillis();

        for (int dataIdx = startIdx; dataIdx < numSamples; dataIdx++) {
            final DeviceGoveeHelper.Record sample = recordSamples.get(dataIdx);
            DateTime dt = new DateTime(sample.dayMilli).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
            int dayIdx = Days.daysBetween(startTime, dt).getDays();

            // Optionally add missing filler to always have a midnight entry.
            while (dt.isAfter(nextStartOfDay)) {
                int fillerIdx = Days.daysBetween(startTime, nextStartOfDay).getDays();
                if (fillerIdx + 1 < dayIdx) {
                    temSeries.add(new Entry(fillerIdx, cfg.toDegreeN(sample.tempMin.val)));
                    temSeries.add(new Entry(fillerIdx, cfg.toDegreeN(sample.tempMax.val)));
                    humSeries.add(new Entry(fillerIdx, cfg.toHumPerN(sample.humMin.val)));
                    humSeries.add(new Entry(fillerIdx, cfg.toHumPerN(sample.humMax.val)));
                }
                // startDayPosMap.put(fillerIdx, nextStartOfDay.getMillis());
                nextStartOfDay = nextStartOfDay.plusDays(1);
                startDayMillis = nextStartOfDay.getMillis();
            }

            // Add data at hourly spacing
            if (true) {
                temSeries.add(DayEntry(dayIdx, startTime, sample.tempMin, rec -> cfg.toDegreeN(rec.val)));
                temSeries.add(DayEntry(dayIdx, startTime, sample.tempMax, rec -> cfg.toDegreeN(rec.val)));
                humSeries.add(DayEntry(dayIdx, startTime, sample.humMin, rec -> cfg.toHumPerN(rec.val)));
                humSeries.add(DayEntry(dayIdx, startTime, sample.humMax, rec -> cfg.toHumPerN(rec.val)));
            } else {
                temSeries.add(new Entry(dayIdx, cfg.toDegreeN(sample.tempMin.val)));
                temSeries.add(new Entry(dayIdx, cfg.toDegreeN(sample.tempMax.val)));
                humSeries.add(new Entry(dayIdx, cfg.toHumPerN(sample.humMin.val)));
                humSeries.add(new Entry(dayIdx, cfg.toHumPerN(sample.humMax.val)));
            }

            // Save midnight positions for later use to add day markers.
            if (startDayMillis >= sample.dayMilli) {
                // startDayPos = dayIdx;
            } else {
                // startDayPosMap.put(startDayPos, startDayMillis);
                startDayMillis += TimeUnit.DAYS.toMillis(1);
            }
        }

        temSeries.sort(new EntryXComparator());
        humSeries.sort(new EntryXComparator());

        int setIdx = 0;
        addSet("Temp °F", temSeries, TEMPERATURE_COLOR, temperatureFillDrawable, LEFT, setIdx++);
        addSet("Humidity", humSeries, HUMIDITY_COLOR, humidityFillDrawable, RIGHT, setIdx++);

        mpPlot.getXAxis().removeAllLimitLines();

        // Add dummy series to force gap on right edge
        float freezeValue = (cfg.tunit == Units.Temperature.Fahrenheit) ? 32F : 0F;
        ArrayList<Entry> dummySeries = new ArrayList<>(2);
        dummySeries.add(new Entry(0, freezeValue));
        dummySeries.add(new Entry(temSeries.size()+0.1f, freezeValue));
        addSet("", dummySeries, Color.TRANSPARENT, null, LEFT, 2);

        /*
        mpPlot.getXAxis().removeAllLimitLines();
        for (Map.Entry<Integer, Long> dayPosAndMilli : startDayPosMap.entrySet()) {
            DateTime dt = new DateTime(dayPosAndMilli.getValue());
            boolean lblBottom = (dt.getDayOfYear() & 1) == 1;
            addDayLine(mpPlot.getXAxis(), dayPosAndMilli.getKey(), dayFmt.print(dt), lblBottom);
        }
         */
        if (true) {
            LineDataSet setTem = (LineDataSet) mpPlot.getData().getDataSetByIndex(0);
            LineDataSet setHum = (LineDataSet) mpPlot.getData().getDataSetByIndex(1);
            // set0.calcMinMax();

            final int NUM_LABELS = 6;
            normalizeAxis(mpPlot.getAxisLeft(), NUM_LABELS, 5, setTem);
            normalizeAxis(mpPlot.getAxisRight(), NUM_LABELS, 10, setHum);

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
