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

import static com.landenlabs.all_sensor.sensor.DeviceListManager.DEFAULT_DEVICE;
import static com.landenlabs.all_sensor.ui.cards.CardShared.isOn;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.sensor.AppCfg;
import com.landenlabs.all_sensor.sensor.DeviceGoveeHelper;
import com.landenlabs.all_sensor.sensor.DeviceItem;
import com.landenlabs.all_sensor.sensor.DeviceListManager;
import com.landenlabs.all_sensor.sensor.DeviceSummary;
import com.landenlabs.all_sensor.utils.ArrayListEx;
import com.landenlabs.all_sensor.utils.NetInfo;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

public class CardTmpHum extends Card {

    CardShared shared;

    @Override
    public View inflate(@NonNull ViewGroup parent, int cardType) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.card_tmp_hum, parent, false);
    }

    @Override
    public int cardType() {
        return CardTypes.TmpHum.ordinal();
    }

    @Override
    public  CardPageAdapter.CardVH createViewHolder(@NonNull View viewCard, int viewType, @NonNull CardShared shared) {
        return new CardVH(viewCard, viewType, shared);
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
            showDeviceInfo();
        }

        @UiThread
        private boolean showDeviceInfo() {
            View root = itemView;
            Context context = itemView.getContext();

            String deviceName = DEFAULT_DEVICE;     // TODO - prompt for device
            AppCfg appCfg = AppCfg.getInstance(context);  // TODO - add cfg page
            DeviceItem deviceItem = DeviceListManager.get(deviceName);
            if (!deviceItem.isValid()) {
                TextView ttv = root.findViewById(R.id.card_battery);
                NetInfo.NetStatus netStatus = new NetInfo.NetStatus(context);
                if (netStatus.netInfo == null && ttv != null)
                    ttv.setText(R.string.no_network);
                else if (deviceItem.getException() != null) {
                    ttv.setText(ALog.getErrorMsg(deviceItem.getException()));
                }
                return false;
            }
            DeviceSummary summary = deviceItem.getSummary(context, appCfg);
            TextView ttv;
            ttv = root.findViewById(R.id.card_dev_name);
            ttv.setText(summary.devName);
    //        ttv = root.findViewById(R.id.start_title);
    //        ttv.setText(summary.devName);
            ttv = root.findViewById(R.id.card_tmp_value);
            ttv.setText(summary.strTemp);
            ttv = root.findViewById(R.id.card_hum_val);
            ttv.setText(summary.strHum);
            ttv = root.findViewById(R.id.card_lastTime);
            ttv.setVisibility( isOn(shared.showTimeRange) ? View.VISIBLE : View.GONE);
            ttv.setText(summary.strDate);

            /*
            (showTimeRange = root.findViewById(R.id.start_showTimeAge)).setOnClickListener(this);
            (showMaxMin = root.findViewById(R.id.start_showMaxMin)).setOnClickListener(this);
            (showAlarm = root.findViewById(R.id.start_showAlarm)).setOnClickListener(this);
            */

            Period period = Period.minutes(60);
            DateTime nowDt = DateTime.now();
            Interval interval = new Interval(nowDt.minusHours(4), nowDt.minusHours(1));
            DeviceItem.DeviceTrend deviceTrend = deviceItem.getTrend(interval, period, summary);
            if (deviceTrend != null) {
                ttv = root.findViewById(R.id.card_temp_trend);
                ttv.setVisibility(View.VISIBLE);
                String INTERVAL_UNIT = appCfg.intervalUnit();
                ttv.setText(appCfg.toDeltaDegree(deviceTrend.deltaTem) + INTERVAL_UNIT);
                ttv.setVisibility(View.VISIBLE);

                ttv = root.findViewById(R.id.card_hum_trend);
                ttv.setText(appCfg.toHumPer(deviceTrend.deltaHum) + INTERVAL_UNIT);
                ttv.setVisibility(View.VISIBLE);
                ttv = root.findViewById(R.id.card_lastTime);
                ttv.setVisibility( isOn(shared.showTimeRange) ? View.VISIBLE : View.GONE);
                ttv.setText(fmtTimeAgo(context, deviceTrend.lastTime));
            } else {
                ttv = root.findViewById(R.id.card_temp_trend);
                ttv.setText("");
                ttv.setVisibility(View.GONE);
                ttv = root.findViewById(R.id.card_hum_trend);
                ttv.setText("");
                ttv.setVisibility(View.GONE);
            }

            showAlaram(appCfg, summary);
            showMaxMin(appCfg, summary);

            ttv = root.findViewById(R.id.card_battery);
            if (summary.numBattery != 100 || summary.numWifi != 0) {
                ttv.setText(context.getString(R.string.start_battery,
                        summary.numBattery,
                        summary.numWifi
                ));
            } else {
                ttv.setText("");
            }

            return true;
        }

        private void showAlaram(AppCfg appCfg, DeviceSummary summary) {
            View root = itemView;
            Context context = itemView.getContext();

            TextView ttv;
            ttv = root.findViewById(R.id.card_temp_alarm);
            ttv.setVisibility(View.GONE);
            if (summary.numAlarmTempMin != null && isOn(shared.showAlarm)) {
                ttv.setVisibility(View.VISIBLE);
                ttv.setText(context.getString(R.string.start_tem_alarm,
                        appCfg.toDegree(summary.numAlarmTempMin),
                        appCfg.toDegree(summary.numAlarmTempMax)));
            }
            ttv = root.findViewById(R.id.card_hum_alarm);
            ttv.setVisibility(View.GONE);
            if (summary.numAlarmHumMin != null && isOn(shared.showAlarm)) {
                ttv.setVisibility(View.VISIBLE);
                ttv.setText(context.getString(R.string.start_hum_alarm,
                        appCfg.toHumPer(summary.numAlarmHumMin),
                        appCfg.toHumPer(summary.numAlarmHumMax)));
            }
        }

        private void showMaxMin(AppCfg appCfg, DeviceSummary summary) {
            View root = itemView;
            Context context = itemView.getContext();
            TextView tmpMinMax = root.findViewById(R.id.card_temp_min_max);
            TextView humMinMax =  root.findViewById(R.id.card_hum_min_max);
            tmpMinMax.setVisibility(View.GONE);
            humMinMax.setVisibility(View.GONE);

            if (isOn(shared.showMaxMin)) {
                DateTime nowDt = DateTime.now();
                Interval interval = new Interval(nowDt.minusHours(24), nowDt.minusHours(1));
                ArrayListEx<DeviceGoveeHelper.Sample> samples =
                        DeviceListManager.getHourlyList(summary.devName, interval);

                if (samples.size() > 2) {
                    int minTmp = Integer.MAX_VALUE;
                    int maxTmp = Integer.MIN_VALUE;
                    int minHum = Integer.MAX_VALUE;
                    int maxHum = Integer.MIN_VALUE;
                    for (DeviceGoveeHelper.Sample sample : samples) {
                        minTmp = Math.min(minTmp, sample.tem);
                        maxTmp = Math.max(maxTmp, sample.tem);
                        minHum = Math.min(minHum, sample.hum);
                        maxHum = Math.max(maxHum, sample.hum);
                    }

                    tmpMinMax.setVisibility(View.VISIBLE);
                    tmpMinMax.setText(context.getString(R.string.start_tem_minmax,
                            appCfg.toDegree(minTmp),
                            appCfg.toDegree(maxTmp)));

                    humMinMax.setVisibility(View.VISIBLE);
                    humMinMax.setText(context.getString(R.string.start_hum_minmax,
                            appCfg.toHumPer(minHum),
                            appCfg.toHumPer(maxHum)));
                }
            }
        }
    }
}
