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

package com.landenlabs.all_sensor.ui.statusA;

import static com.landenlabs.all_sensor.Units.Sensors.Humidity;
import static com.landenlabs.all_sensor.Units.Sensors.Temperature;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import androidx.annotation.NonNull;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.NormedXYSeries;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.Units;
import com.landenlabs.all_sensor.sensor.AppCfg;
import com.landenlabs.all_sensor.sensor.DeviceGoveeHelper;
import com.landenlabs.all_sensor.sensor.DeviceItem;
import com.landenlabs.all_sensor.sensor.DeviceListManager;
import com.landenlabs.all_sensor.sensor.WxManager;
import com.landenlabs.all_sensor.utils.ArrayListEx;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Graph Sensor database data using androidplot.
 * <p>
 * <p>
 * Release notes
 * http://halfhp.github.io/androidplot/docs/release_notes.html
 * <p>
 * Home page
 * http://halfhp.github.io/androidplot/docs/xyplot.html
 * https://github.com/halfhp/androidplot/blob/master/docs/quickstart.md
 * <p>
 * GitHub
 * https://github.com/halfhp/androidplot
 */
class GraphA {

    private final static boolean LINE_MODE = false;
    private final static boolean FILL_MODE = true;
    private final SimpleDateFormat yAxisFmt = new SimpleDateFormat("ha");
    private XYPlot aplot;
    private final ArrayListEx<SeriesHolder> seriesList = new ArrayListEx<>(2);
    private LineAndPointFormatter temperatureFormat;
    private LineAndPointFormatter humidityFormat;
    private DateTime startTime;

    private static class SeriesHolder {
        public final NormedXYSeries series;
        public final LineAndPointFormatter formatter;

        public SeriesHolder(NormedXYSeries series, LineAndPointFormatter formatter) {
            this.series = series;
            this.formatter = formatter;
        }
    }


    // ---------------------------------------------------------------------------------------------
    private void initGraph(View root, DeviceItem device) {
        if (aplot == null) {
            aplot = root.findViewById(R.id.aplot);
            aplot.clear();
            aplot.setTitle(device.name);

            // Set the plot background (do once)
            XYGraphWidget graph = aplot.getGraph();
            RectF rectF = graph.getGridRect();
            float mpPlotWidth = (rectF != null) ? rectF.right : 250;
            float mpPlotHeight = (rectF != null) ? rectF.height() : 250;
            Shader colorShader = new LinearGradient(0, 0, mpPlotWidth, 0,
                    Color.BLACK, Color.rgb(0, 64, 0), Shader.TileMode.MIRROR);
            graph.getGridBackgroundPaint().setShader(colorShader);

            // mpPlot.setRangeBoundaries(-1, 2, BoundaryMode.FIXED);  // Normalized
            //        mpPlot.setRangeBoundaries(0, 100, AUTO);

            //        mpPlot.setRangeStep(StepMode.SUBDIVIDE, 10 + 1);
            //2 m_plot.setTicksPerRangeLabel(2);
            aplot.setDomainLabel("Hours");
            aplot.setRangeLabel("Temperature °F");

            /*
            mpPlot.setRangeTitle(new TextLabelWidget(
                    getLayoutManager(),
                    new Size(
                            PixelUtils.dpToPix(DEFAULT_RANGE_LABEL_WIDGET_H_DP),
                            SizeMode.ABSOLUTE,
                            PixelUtils.dpToPix(DEFAULT_RANGE_LABEL_WIDGET_W_DP),
                            SizeMode.ABSOLUTE),
                    TextOrientation.VERTICAL_ASCENDING));
            */

            aplot.getGraph().setLineLabelEdges(
                    XYGraphWidget.Edge.BOTTOM,
                    XYGraphWidget.Edge.LEFT,
                    XYGraphWidget.Edge.RIGHT);
            aplot.getGraph().setPaddingRight(80);
            aplot.getGraph().setPaddingLeft(40);

            // ------
            final int lineOnlyWidth = 6;
            final int lineFillWidth = 6;
            int temperatureColor = Color.GREEN;
            temperatureFormat = makeFormatter(lineFillWidth, temperatureColor, 0x4000ff00,
                    0, mpPlotHeight, FILL_MODE);
            temperatureFormat.setVertexPaint(null);
            temperatureFormat.setPointLabelFormatter(null);

            // int PINK = 0xffff55BB;
            int humidityColor = 0xff8080ff;
            humidityFormat = makeFormatter(lineOnlyWidth, humidityColor, humidityColor,
                    0, mpPlotHeight, LINE_MODE);
            humidityFormat.setVertexPaint(null);
            humidityFormat.setPointLabelFormatter(null);

            // ------
            aplot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).getPaint().setColor(temperatureColor);
            aplot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(new Format() {

                @Override
                public StringBuffer format(Object o, @NonNull StringBuffer stringBuffer, @NonNull FieldPosition fieldPosition) {
                    final NormedXYSeries temperatureSeries = seriesList.get(Temperature.ordinal()).series;
                    final Number value = temperatureSeries.denormalizeYVal((Number) o);
                    stringBuffer.append(String.format("%.0f", value.doubleValue()));
                    return stringBuffer;
                }

                @Override
                public Object parseObject(String s, @NonNull ParsePosition parsePosition) {
                    return null;
                }
            });

            aplot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.RIGHT).getPaint().setColor(humidityColor);
            aplot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.RIGHT).setFormat(new Format() {

                @Override
                public StringBuffer format(Object o, @NonNull StringBuffer stringBuffer, @NonNull FieldPosition fieldPosition) {
                    final NormedXYSeries humiditySeries = seriesList.get(Humidity.ordinal()).series;
                    Number minWage = humiditySeries.denormalizeYVal((Number) o);
                    stringBuffer.append(String.format("%.0f", minWage.doubleValue()));
                    return stringBuffer;
                }

                @Override
                public Object parseObject(String s, @NonNull ParsePosition parsePosition) {
                    return null;
                }
            });

            aplot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {

                @Override
                public StringBuffer format(Object obj, @NonNull StringBuffer toAppendTo, @NonNull FieldPosition pos) {
                    int hourOffset = ((Number) obj).intValue();
                    DateTime yAxisDT = startTime.plusHours(hourOffset);
                    String str = yAxisFmt.format(yAxisDT.toDate());
                    return toAppendTo.append(str);
                }

                @Override
                public Object parseObject(String source, @NonNull ParsePosition pos) {
                    return null;
                }
            });
        }
    }

    void updateGraph(View root, WxManager wxManager, DeviceItem device, Interval interval, AppCfg appCfg) {
        if (aplot == null) {
            initGraph(root, device);
            seriesList.clear();
        } else {
            /*
            for (SeriesHolder holder : seriesList) {
                aplot.removeSeries(holder.series);
            }
             */
            seriesList.clear();
            aplot.clear();
            // Bug - legends accumulate, get too many instancees.
            aplot.getLegend().setVisible(false);
        }

        ArrayListEx<DeviceGoveeHelper.Sample> hourlySamples = DeviceListManager.getHourlyList(device.name, interval);
        if (hourlySamples.size() == 0) {
            return;
        }

        int numSamples = hourlySamples.size();
        int numPlot = Math.min(24 * 14, numSamples);
        int startIdx = numSamples - numPlot;
        ArrayList<Float> temSeries = new ArrayList<>(numPlot);
        ArrayList<Float> humSeries = new ArrayList<>(numPlot);
        startTime = new DateTime(hourlySamples.get(startIdx).milli);
        for (int hourIdx = startIdx; hourIdx < numSamples; hourIdx++) {
            final DeviceGoveeHelper.Sample sample = hourlySamples.get(hourIdx);
            temSeries.add(appCfg.toDegreeN(sample.tem));
            humSeries.add(appCfg.toHumPerN(sample.hum));
        }

        final NormedXYSeries temperatureSeries = new NormedXYSeries(new SimpleXYSeries(
                temSeries, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, Temperature.name()));
        final NormedXYSeries humiditySeries = new NormedXYSeries(new SimpleXYSeries(
                humSeries, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, Humidity.name()));

        /*
        // add an "dash" effect to the series2 line:
        minWageFormat.getLinePaint().setPathEffect(new DashPathEffect(new float[] {

                // always use DP when specifying pixel sizes, to keep things consistent across devices:
                PixelUtils.dpToPix(20),
                PixelUtils.dpToPix(15)}, 0));

        // add some smoothing to the lines:
        // see: http://androidplot.com/smooth-curves-and-androidplot/
        temperatureSeries.setInterpolationParams(
                new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));

        humiditySeries.setInterpolationParams(
                new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));
        */

        /*
            // just for fun, add some smoothing to the lines:
            // see: http://androidplot.com/smooth-curves-and-androidplot/
            temperatureSeries.setInterpolationParams(
                    new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));

            humiditySeries.setInterpolationParams(
                    new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));
        */

        // add a new series' to the xyplot:
        seriesList.add(new SeriesHolder(temperatureSeries, temperatureFormat));
        aplot.addSeries(temperatureSeries, temperatureFormat);
        seriesList.add(new SeriesHolder(humiditySeries, humidityFormat));
        aplot.addSeries(humiditySeries, humidityFormat);

        // aplot.setRangeBoundaries(-1, 2, BoundaryMode.FIXED);


        // int xSpacing = (int)interval.toDuration().getStandardHours()/8;
        int xSpacing = numPlot / 8;
        xSpacing = Math.max(3, (xSpacing + 2) / 6 * 6);
        aplot.setDomainStep(StepMode.INCREMENT_BY_VAL, xSpacing);
        // aplot.setDomainStep(StepMode.SUBDIVIDE, 12);

        aplot.calculateMinMaxVals();
        // aplot.setUserRangeOrigin();
        // aplot.setUserDomainOrigin(aplot.getCalculatedMaxX());
        aplot.invalidate();
    }

    private static LineAndPointFormatter makeFormatter(int width, int lineColor, int fillColor, float x2, float y2, boolean fill) {
        //2 LineAndPointFormatter formatter = new FastLineAndPointRenderer.Formatter(color1, null, color2, null);
        LineAndPointFormatter formatter = new LineAndPointFormatter(lineColor, null, fillColor, null);
        Paint linePaint = formatter.getLinePaint();
        linePaint.setStrokeWidth(width);
        float shadowOff = width / 2f;
        linePaint.setShadowLayer(width, shadowOff, shadowOff, Color.argb(200, 128, 128, 128));

        if (fill) {
            Paint lineFill = new Paint();
            lineFill.setShader(new LinearGradient(0, 0, x2, y2, fillColor, lineColor, Shader.TileMode.MIRROR));
            lineFill.setAlpha(128 + 32);
            formatter.setFillPaint(lineFill);
        } else {
            formatter.setFillPaint(null);
            linePaint.setShader(new LinearGradient(0, 0, x2, y2, lineColor, fillColor, Shader.TileMode.MIRROR));
        }
        formatter.setLinePaint(linePaint);

        return formatter;
    }

    public void enableSeries(Units.Sensors sensor, boolean show) {
        SeriesHolder holder = seriesList.get(sensor.ordinal(), null);
        if (aplot != null && holder != null) {
            boolean didIt;
            if (show) {
                didIt = aplot.addSeries(holder.series, holder.formatter);
            } else {
                didIt = true;
                aplot.removeSeries(holder.series);
            }

            if (didIt) {
                aplot.invalidate();
            }
        }
    }
}
