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

package com.landenlabs.all_sensor.ui.cards;

import static com.landenlabs.all_sensor.ui.cards.CardShared.isOn;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.sensor.AppCfg;
import com.landenlabs.all_sensor.sensor.DeviceSummary;
import com.landenlabs.all_sensor.sensor.SensorItem;
import com.landenlabs.all_sensor.sensor.SensorListManager;
import com.landenlabs.all_sensor.sensor.SensorPressure;
import com.landenlabs.all_sensor.sensor.SensorSummary;
import com.landenlabs.all_sensor.sensor.SensorWiFi;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import java.text.SimpleDateFormat;

public class CardPress extends Card {

    CardShared shared;

    @Override
    public View inflate(@NonNull ViewGroup parent, int cardType) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.card_press, parent, false);
    }

    @Override
    public int cardType() {
        return CardTypes.Pressure.ordinal();
    }

    @Override
    public  CardPageAdapter.CardVH createViewHolder(@NonNull View viewCard, int viewType, @NonNull CardShared shared) {
        return new CardPress.CardVH(viewCard, viewType, shared);
    }

    // =============================================================================================
    public static class CardVH extends CardPageAdapter.CardVH {

        final CardShared shared;

        public CardVH(@NonNull View viewCard, int viewType, @NonNull CardShared shared) {
            super(viewCard, viewType);
            this.shared = shared;
        }

        @Override
        public void fillView(Card card) {
            showSensorInfo();
        }

        @UiThread
        private boolean showSensorInfo() {
            View root = itemView;
            Context context = itemView.getContext();
            Period period = Period.minutes(60);
            DateTime nowDt = DateTime.now();
            Interval interval = new Interval(nowDt.minusDays(2), nowDt.minusHours(1));
            AppCfg appCfg = AppCfg.getInstance(context);
            TextView ttv;
            String INTERVAL_UNIT = appCfg.intervalUnit();

            ttv = root.findViewById(R.id.card_sensor_name);
            ttv.setText(SensorPressure.NAME);

            long lastMilli = System.currentTimeMillis();
            SensorSummary summary;
            SensorItem pressItem = SensorListManager.get(SensorPressure.NAME);
            if (pressItem == null || !pressItem.isValid()) {
                ttv = root.findViewById(R.id.card_press_value);
                ttv.setText("" );
                ttv = root.findViewById(R.id.card_press_trend);
                ttv.setText(R.string.no_data);
            } else {
                summary = pressItem.getSummary(context, appCfg);
                SensorItem.SenorTrend pressTrend = pressItem.getTrend(interval, period, summary);

                ttv = root.findViewById(R.id.card_press_value);
                ttv.setText(summary.strValue);

                ttv = root.findViewById(R.id.card_lastTime);
                ttv.setVisibility(isOn(shared.showTimeRange) ? View.VISIBLE : View.GONE);
                // ttv.setText(summary.strDate);
                ttv.setText(fmtTimeAgo(context, new DateTime(summary.numTime)));

                if (pressTrend != null) {
                    ttv = root.findViewById(R.id.card_press_trend);
                    ttv.setVisibility(View.VISIBLE);
                    ttv.setText(appCfg.toPress(pressTrend.deltaVal) + INTERVAL_UNIT);
                } else {
                    ttv = root.findViewById(R.id.card_press_trend);
                    ttv.setText("");
                    ttv.setVisibility(View.GONE);
                }
                lastMilli = pressItem.lastMilli();    // pressTrend.lastTime.getMillis()
            }

            SensorItem wifiItem = SensorListManager.get(SensorWiFi.NAME);
            if (wifiItem == null || !wifiItem.isValid()) {
                ttv = root.findViewById(R.id.card_wifi_val);
                ttv.setText("");
                ttv = root.findViewById(R.id.card_wifi_trend);
                ttv.setText(R.string.no_data);
            } else {
                summary = wifiItem.getSummary(context, appCfg);
                SensorItem.SenorTrend wifiTrend = wifiItem.getTrend(interval, period, summary);
                ttv = root.findViewById(R.id.card_wifi_val);
                ttv.setText(summary.strValue);
                if (wifiTrend != null) {
                    ttv = root.findViewById(R.id.card_wifi_trend);
                    ttv.setVisibility(View.VISIBLE);
                    ttv.setText(appCfg.toWifi(wifiTrend.deltaVal) + INTERVAL_UNIT);
                } else {
                    ttv = root.findViewById(R.id.card_wifi_trend);
                    ttv.setText("");
                    ttv.setVisibility(View.GONE);
                }
                lastMilli = wifiItem.lastMilli();    // wifiTrend.lastTime.getMillis()
            }
            ttv = root.findViewById(R.id.card_lastTime);
            ttv.setVisibility(isOn(shared.showTimeRange) ? View.VISIBLE : View.GONE);
            SimpleDateFormat dateFmt = new SimpleDateFormat("MMM d E hh:mm a z");
            ttv.setText(dateFmt.format(lastMilli));
            // showMaxMin(appCfg, summary);
            return true;
        }

        private void showMaxMin(AppCfg appCfg, DeviceSummary summary) {
            View root = itemView;
            Context context = itemView.getContext();

            TextView ttv;
            ttv = root.findViewById(R.id.card_temp_min_max);
            ttv.setVisibility(View.GONE);
            if ( isOn(shared.showMaxMin)) {
                ttv.setVisibility(View.VISIBLE);
                ttv.setText(context.getString(R.string.start_tem_minmax,
                        appCfg.toDegree(-1200),
                        appCfg.toDegree(12300)));
            }
            ttv = root.findViewById(R.id.card_hum_min_max);
            ttv.setVisibility(View.GONE);
            if ( isOn(shared.showMaxMin)) {
                ttv.setVisibility(View.VISIBLE);
                ttv.setText(context.getString(R.string.start_hum_minmax,
                        appCfg.toHumPer(1000),
                        appCfg.toHumPer(9900)));
            }
        }
    }
}
