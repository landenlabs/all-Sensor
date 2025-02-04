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

package com.landenlabs.all_sensor.ui;

import static com.landenlabs.all_sensor.sensor.DeviceListManager.DEFAULT_DEVICE;
import static com.landenlabs.all_sensor.ui.cards.CardShared.isOn;
import static com.landenlabs.all_sensor.ui.cards.CardShared.setOn;
import static com.landenlabs.all_sensor.utils.FragUtils.setText;
import static com.landenlabs.all_sensor.utils.NetInfo.loadWifi;
import static com.landenlabs.all_sensor.utils.SpanUtil.SSBlue;
import static com.landenlabs.all_sensor.utils.SpanUtil.SSBold;
import static com.landenlabs.all_sensor.utils.SpanUtil.SSJoin;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.navigation.NavArgument;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.db.DbSensorHourly;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.sensor.DeviceListManager;
import com.landenlabs.all_sensor.sensor.SensorBattery;
import com.landenlabs.all_sensor.sensor.SensorItem;
import com.landenlabs.all_sensor.sensor.SensorListManager;
import com.landenlabs.all_sensor.sensor.SensorPressure;
import com.landenlabs.all_sensor.sensor.SensorWiFi;
import com.landenlabs.all_sensor.sensor.WxManager;
import com.landenlabs.all_sensor.sensor.WxViewModel;
import com.landenlabs.all_sensor.ui.cards.Card;
import com.landenlabs.all_sensor.ui.cards.CardBattery;
import com.landenlabs.all_sensor.ui.cards.CardCpu;
import com.landenlabs.all_sensor.ui.cards.CardPageAdapter;
import com.landenlabs.all_sensor.ui.cards.CardPress;
import com.landenlabs.all_sensor.ui.cards.CardShared;
import com.landenlabs.all_sensor.ui.cards.CardTmpHum;
import com.landenlabs.all_sensor.ui.view.BaseFragment;
import com.landenlabs.all_sensor.ui.view.FloatingDialog;
import com.landenlabs.all_sensor.utils.NetInfo;

import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

/**
 * Start page for Sensor information.
 */
public class StartFragment extends BaseFragment
        implements View.OnClickListener {

    private WxManager wxManager;
    private TextView statusTv;
    private TextView statusTitleTv;
    private ProgressBar statusPbTop;
    private ProgressBar statusPbBot;
    private int lastProgress = -1;
    private ImageView statusScaleBtn;

    private final CardShared shared = new CardShared();
    private RecyclerView cardPager;
    private ArrayList<Card> cards = new ArrayList<>();

    // ---------------------------------------------------------------------------------------------

    @SuppressLint({"ClickableViewAccessibility", "FieldCanBeLocal"})
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {

        super.onCreateView((ViewGroup) inflater.inflate(R.layout.fragment_start, container, false));

        initUi();
        updateUi();
        return root;
    }

    @Override
    protected boolean updateToolbarStatus() {
        updateUi();
        return super.updateToolbarStatus();
    }

    private void addSensorGraphBtn(ViewGroup holder, String sensorName) {
        TextView tv = new TextView(new ContextThemeWrapper(holder.getContext(), R.style.textLink20));
        tv.setOnClickListener(this);
        tv.setTag(R.id.start_graph_sensor_page, sensorName);
        setText(tv, R.string.start_graph_sensor_page, R.string.start_graph_sensor_link, sensorName);
        holder.addView(tv);
    }

    private void initUi() {
        wxManager = WxManager.getInstance(requireContext());
        TextView tv;

        tv = root.findViewById(R.id.start_net_settings);
        tv.setOnClickListener(this);
        setText(tv, R.string.net_settings_page, R.string.net_settings_link);

        // TODO - provide list of sensor graphs
        ViewGroup holder = root.findViewById(R.id.start_graph_sensor_page);
        holder.removeAllViews();

        for (Class<? extends SensorItem> sensorClass :  SensorListManager.SENSOR_CLASSES) {
            // TODO - add sensor graph buttons.
            // TODO - add tabular buttons.
        }
        if (wxManager.hasSensor(SensorPressure.NAME)) {
            addSensorGraphBtn(holder,  SensorPressure.NAME);
        }
        addSensorGraphBtn(holder,  SensorWiFi.NAME);
        addSensorGraphBtn(holder,  SensorBattery.NAME);

        // TODO - provide list of device graphs
        tv = root.findViewById(R.id.start_graph_device_page);
        tv.setOnClickListener(this);
        setText(tv, R.string.start_graph_th_page, R.string.start_graph_th_link);

        tv = root.findViewById(R.id.start_table_page);
        tv.setOnClickListener(this);
        setText(tv, R.string.start_table_page, R.string.start_table_link);

        cardPager = root.findViewById(R.id.start_card_holder);
        LinearSnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(cardPager);
        cards = new ArrayList<>();
        cards.add(new CardTmpHum());
        // if (wxManager.hasSensor(SensorPressure.NAME) ) {
            cards.add(new CardPress());
        // }
        cards.add(new CardBattery());
        cards.add(new CardCpu());
        cardPager.setAdapter(new CardPageAdapter(cards, shared, this));

        // Status
        statusScaleBtn = root.findViewById(R.id.progress_scale_btn);
        statusScaleBtn.setOnClickListener(this);
        statusScaleBtn.setSelected(true);
        statusTitleTv = root.findViewById(R.id.start_progress_title);
        statusPbTop = root.findViewById(R.id.start_status_progress2);
        statusPbBot = root.findViewById(R.id.start_status_progress);
        statusPbBot.setOnTouchListener((view, motionEvent) -> true);   // Make seekbar readonly
        statusTv = root.findViewById(R.id.start_status);
        statusTv.setMovementMethod(new ScrollingMovementMethod());  // Enable scrolling

        (shared.showTimeRange = root.findViewById(R.id.start_showTimeAge)).setOnClickListener(this);
        (shared.showMaxMin = root.findViewById(R.id.start_showMaxMin)).setOnClickListener(this);
        (shared.showAlarm = root.findViewById(R.id.start_showAlarm)).setOnClickListener(this);
        root.findViewById(R.id.start_graph).setOnClickListener(this);
        root.findViewById(R.id.start_table).setOnClickListener(this);
        setOn(true, shared.showTimeRange, shared.showMaxMin);
        setOn(false, shared.showAlarm);

        // wxManager.getViewModel().getStatus().observe(getViewLifecycleOwner(), this::updateUi);
        wxManager.viewModel().getProgress().observe(getViewLifecycleOwner(),
                state-> { updateUi(state); wxManager.viewModel().getProgress().next(); });

        // root.findViewById(R.id.start_tmp_hum_card).setOnClickListener(this);
        cardPager.setOnClickListener(this);
    }

    private void updateUi() {
        loadWifi(this.requireContext(), this);

        ALog.d.tagMsg(this, "start complete=", wxManager.wxState);
        wxManager.wxState.complete.thenAccept(ss -> root.post(this::showDeviceInfo)).exceptionally(xx -> {
            ALog.d.tagMsg(this, "start complete exception=", xx);
            wxManager.wxState.fail(xx);
            return null;
        });

        showDeviceInfo(false);
    }

    long lastUpdateMilli = 0;
    private static final long UPDATE_MILLI = 1000;
    @UiThread
    private boolean showDeviceInfo() {
        return showDeviceInfo(false);
    }
    private boolean showDeviceInfo(boolean force) {
        long nowMilli = System.currentTimeMillis();
        if (!force && nowMilli - lastUpdateMilli < UPDATE_MILLI)
            return false;
        lastUpdateMilli = nowMilli;
        cardPager.getAdapter().notifyDataSetChanged();

        return true;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        // TODO - replace this with new start_graph and start_table button clicks.


        if (view.getTag(R.id.start_graph_sensor_page) instanceof String) {
            String sensorName = view.getTag(R.id.start_graph_sensor_page).toString();
            switch (sensorName) {
                case SensorWiFi.NAME:
                    gotoPage(R.id.nav_graph_wifi, true);
                    break;
                case SensorBattery.NAME:
                    gotoPage(R.id.nav_graph_battery, true);
                    break;
                case SensorPressure.NAME:
                default:
                    gotoPage(R.id.nav_graph_sn, true);
                    break;
            }

        } else if (id == R.id.start_graph_device_page) {
            gotoPage(R.id.nav_graph_dv, true);
        } else if (id == R.id.start_table_page) {
            gotoPage(R.id.nav_table, false);
        } else if (id == R.id.start_net_settings) {
            startActivity(new Intent("android.settings.WIRELESS_SETTINGS"));
        } else if (id == R.id.progress_scale_btn) {
            toggleStatus(statusScaleBtn, statusTv);
        } else if (id == R.id.card_tmp_hum_card || id == R.id.start_card_holder) {
            lastProgress = -1;
            statusPbBot.setVisibility(View.VISIBLE);
            statusPbBot.setProgress(0);
            statusPbTop.setVisibility(View.VISIBLE);
            wxManager.start(requireContext(), true);
            updateUi();
            TextView ttv = root.findViewById(R.id.card_battery);
            if (ttv != null)  {
                ttv.setText(R.string.status_updating);
            }
        } else if (id == R.id.start_showTimeAge) {
            setOn(!isOn(shared.showTimeRange), shared.showTimeRange);
            showDeviceInfo(true);
        } else if (id == R.id.start_showAlarm) {
            setOn(!isOn(shared.showAlarm), shared.showAlarm);
            showDeviceInfo(true);
        } else if (id == R.id.start_showMaxMin) {
            setOn(!isOn(shared.showMaxMin), shared.showMaxMin);
            showDeviceInfo(true);
        } else {
            ALog.w.tagMsg(this, "unknown click action");
        }
    }

    private void toggleStatus(ImageView view, TextView statusTv) {
        boolean doOpen = (statusTv.getVisibility() == View.GONE);
        // view.setSelected(selected);
        view.setImageResource(doOpen ? R.drawable.scr_open : R.drawable.scr_close);
        statusTv.setVisibility(doOpen ? View.VISIBLE : View.GONE);
    }

    @SuppressWarnings("SameParameterValue")
    private void gotoPage(int id, boolean requiresNetwork) {
        if (!requiresNetwork || NetInfo.haveNetwork(requireContext())) {
            NavController navBotController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

            // Propagate arguments to launched fragment.
            Bundle args = new Bundle();
            for (NavDestination navDestination : navBotController.getGraph()) {
                if (navDestination.getId() == id) {
                    for (Map.Entry<String, NavArgument> arg : navDestination.getArguments().entrySet()) {
                        Object argVal = arg.getValue().getDefaultValue();
                        if (argVal != null) {
                            if (argVal instanceof Integer) {
                                args.putInt(arg.getKey(), (Integer)argVal);
                            } else {
                                args.putString(arg.getKey(), argVal.toString());
                            }
                        }
                    }
                }
            }
            navBotController.navigate(id, args);
        } else {
            FloatingDialog errorDlg = new FloatingDialog(root, R.layout.error_dialog, R.layout.settings_shadow, R.id.errorDialogClose);
            errorDlg.<TextView>viewById(R.id.errorDialogText).setText("Please enable Wifi or Cellular network");
            errorDlg.open();
        }
    }

    @Override
    public String getName() {
        return "Start";
    }

    @Override
    public String results() {
        return "";
    }

    @Override
    public boolean validSwipeRegion(MotionEvent ev) {
        return true;
    }

    private void updateUi(WxViewModel.Progress progress) {
        if (progress.progress > lastProgress && progress.progress * lastProgress != -100) {
            boolean showSummary = (progress.progress == 100 && lastProgress != -1);
            statusPbBot.setIndeterminate(lastProgress == -1 && progress.progress == 0);

            lastProgress = progress.progress;

            if (progress.dt.getMillis() != 0) {
                DateTimeFormatter dateFmt = DateTimeFormat.forPattern("MMM d E hh:mm a");
                String msg =  dateFmt.print(progress.dt) + "  Progress=" + progress.progress + " Samples=" + progress.size;
                statusTv.setText((progress.progress == 0 ? "" : statusTv.getText()) + msg + "\n");
            }
            statusPbBot.setProgress(progress.progress);
            statusPbBot.setVisibility(progress.progress == 100 ? View.GONE : View.VISIBLE);
            statusPbTop.setVisibility(progress.progress == 100 ? View.GONE : View.VISIBLE);
            int seconds = Seconds.secondsBetween(wxManager.initStartDt, DateTime.now()).getSeconds();
            statusTitleTv.setText(getString(R.string.statusTitle, progress.progress, seconds));

            if (showSummary) {
                // TODO - show all files
                File dbDeviceHourly = new File(DeviceListManager.getDbHourly(DEFAULT_DEVICE).getFile());
                File dbDeviceDaily = new File(DeviceListManager.getDbDaily(DEFAULT_DEVICE).getFile());

                CharSequence dbSizeStr =
                        SSJoin(statusTv.getText()
                        , "Hourly Device Db size=", SSBold(String.format("%,d", dbDeviceHourly.length()))
                        , "\nDaily Device Db size=", SSBold(String.format("%,d", dbDeviceDaily.length()))
                        );
                if (SensorListManager.getSensors().size() > 0) {
                    DbSensorHourly dbSensorHourly =  SensorListManager.getDbHourly(DEFAULT_DEVICE);
                    if (dbSensorHourly != null) {
                        try {
                            File dbSensorHourlyFile = new File(dbSensorHourly.getFile());
                            dbSizeStr = SSJoin(dbSizeStr
                                    , "\nHourly Sensor Db size=", SSBold(String.format("%,d", dbSensorHourlyFile.length())));
                        } catch (Exception ex) {
                            ALog.e.tagMsg(this, "db file error ", ex);
                        }
                    }
                }
                statusTv.setText(SSJoin(dbSizeStr, SSBlue("\n[DONE]")));
                lastProgress = -1;
            }
        }
    }
}
