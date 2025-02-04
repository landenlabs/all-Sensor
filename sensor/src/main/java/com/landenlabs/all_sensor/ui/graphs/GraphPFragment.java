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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.Units;
import com.landenlabs.all_sensor.sensor.AppCfg;
import com.landenlabs.all_sensor.sensor.SensorItem;
import com.landenlabs.all_sensor.sensor.SensorListManager;
import com.landenlabs.all_sensor.sensor.WxManager;
import com.landenlabs.all_sensor.ui.view.GraphFragment;
import com.landenlabs.all_sensor.utils.StrUtils;

/**
 * Graph Pressure Sensor database data using MPAndroidChart.
 */
public class GraphPFragment extends GraphFragment {

    private final GraphP graph = new GraphP();
    private boolean doUpdateStatus = true;
    private AppCfg appCfg;

    // TODO -  remove this, no longer used
    private final Runnable bgRunnable = () -> {
        while (doUpdateStatus) {
            // getStatus(requireContext());
            try {
                Thread.sleep(2000);
            } catch (Exception ex) {
                break;
            }
        }
    };
    private Thread bgStatus;

    // ---------------------------------------------------------------------------------------------
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, (ViewGroup) inflater.inflate(R.layout.fragment_graph_sn, container, false));

        inputName = StrUtils.asString(root.getTag(R.id.SensorName), inputName);
        if (getArguments() != null) {
            inputName = getArguments().getString("SensorName", inputName);
        }
        if (savedInstanceState != null) {
            inputName = savedInstanceState.getString("SensorName", inputName);
        }

        wxManager = WxManager.getInstance(requireContext());

        appCfg = AppCfg.getInstance(requireContext());
        initIntervalUI();
        plotValue1Tx.setText(inputName);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        doUpdateStatus = true;
        bgStatus = new Thread(bgRunnable);
        bgStatus.start();
        updateUi();
    }

    @Override
    public void onPause() {
        doUpdateStatus = false;
        bgStatus.interrupt();
        bgStatus = null;
        super.onPause();
    }

    @Override
    public String getName() {
        return "GraphP";
    }

    @Override
    public String results() {
        return "";
    }

    @Override
    protected void updateUi() {
        SensorItem sensorItem = SensorListManager.get(inputName);
        if (sensorItem != null) {
            updateStatus();
            graph.sampleBy = sampleBy;
            graph.updateGraph(root, wxManager, sensorItem, WxManager.getInterval(units, duration), appCfg);
            setShowSensor(showSensors, true);
        }
    }

    @Override
    protected void updateStatus() {
        SensorItem sensorItem = SensorListManager.get(inputName);
        if (toolbar != null) {
            toolbar.setTitle("Graph Sensor " + sensorItem.name);
        }

        /* TODO - implement InfoMap for pressure sensor
        Map<String, String> infoMap = sensorItem.getInfoMap(requireContext(), AppCfg.getInstance());
        for (Map.Entry<String, String> entry : infoMap.entrySet()) {
            addGridRow(entry.getKey(), entry.getValue());
        }
        */
    }
    
    protected void showGraphSensor(Units.Sensors sensor) {
        graph.enableSeries(sensor, showSensors.contains(sensor));
    }
}
