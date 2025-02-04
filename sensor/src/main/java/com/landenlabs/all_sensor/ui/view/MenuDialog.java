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

package com.landenlabs.all_sensor.ui.view;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.landenlabs.all_sensor.Units.INVALID_RES_ID;
import static com.landenlabs.all_sensor.sensor.DeviceListManager.DEFAULT_DEVICE;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;
import com.landenlabs.all_sensor.BuildConfig;
import com.landenlabs.all_sensor.R;
import com.landenlabs.all_sensor.db.DbUtil;
import com.landenlabs.all_sensor.logger.ALogFileWriter;
import com.landenlabs.all_sensor.sensor.WxManager;
import com.landenlabs.all_sensor.sensor.WxViewModel;
import com.landenlabs.all_sensor.utils.StrUtils;

import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Custom floating menu dialog.
 */
public class MenuDialog extends FloatingDialog {

    private final ExpandList<MenuGroup> expandList = new ExpandList<>(MenuGroup::new);

    final int MENU_ABOUT_GROUP;
    final int MENU_MAINTENANCE_GROUP;
    final int MENU_LOG_GROUP;
    final int MENU_SYS_GROUP;

    // ---------------------------------------------------------------------------------------------

    public MenuDialog(Context context, ViewGroup parent) {
        super(parent, R.layout.settings_menu, R.layout.settings_shadow, R.id.menu_dialog_close);
        setAction(FloatingDialog.Factions.HorzFromEnd);


        // versionName generated at build time.
        MENU_ABOUT_GROUP = expandList.addGroup(
                StrUtils.joinStrings(context, R.string.menu_about, " ", R.string.versionName),
                // context.getString(R.string.menu_about),
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                ExpandList.GroupItemsInfo.SHOW_NONE).setIcon(R.drawable.ic_menu_status).index;

        MenuGroup maintenanceGroup = expandList.addGroup(context.getString(R.string.menu_maintenance),
                Color.TRANSPARENT, Color.TRANSPARENT, ExpandList.GroupItemsInfo.SHOW_NONE);
        maintenanceGroup.setIcon(R.drawable.ic_maintenance);
        maintenanceGroup.addChild(context.getString(R.string.menu_maint_rebuild_daily)).setIcon(R.drawable.ic_maintenance);
        MENU_MAINTENANCE_GROUP = maintenanceGroup.index;

        MenuGroup logGroup = expandList.addGroup(context.getString(R.string.menu_log),
                Color.TRANSPARENT, Color.TRANSPARENT, ExpandList.GroupItemsInfo.SHOW_NONE);
        logGroup.setIcon(R.drawable.ic_log_enable);
        logGroup.addChecked(context.getString(R.string.menu_enable_log_cb), true).setIcon(R.drawable.ic_log_enable);
        logGroup.addChild(context.getString(R.string.menu_show_log)).setIcon(R.drawable.ic_log_show);
        logGroup.addChild(context.getString(R.string.menu_delete_log)).setIcon(R.drawable.ic_log_delete);
        MENU_LOG_GROUP = logGroup.index;

        // Add Network settings
        // App settings
        // App notification
        MenuGroup sysGroup = expandList.addGroup(context.getString(R.string.menu_sys), Color.TRANSPARENT,
                Color.TRANSPARENT, ExpandList.GroupItemsInfo.SHOW_NONE).setIcon(R.drawable.ic_settings);
        sysGroup.addChild(context.getString(R.string.menu_sys_net)).setIcon(R.drawable.ic_settings_network);
        sysGroup.addChild(context.getString(R.string.menu_sys_app)).setIcon(R.drawable.ic_settings_app);
        MENU_SYS_GROUP = sysGroup.index;

        ExpandableListView expandableListView = holder.findViewById(R.id.menu_expand_list);
        expandList.build(expandableListView);

        expandableListView.setOnChildClickListener((parent12, childView, groupPosition, childPosition, id) -> {

            // ExpandList.ExpandListAdapter<MenuGroup> expandListAdapter = (ExpandList.ExpandListAdapter)parent.getAdapter();
            // int cp = (int) expandList.expandListAdapter.getChildId(groupPosition, childPosition);
            final MenuGroup gpInfo = expandList.expandListAdapter.groupList.get(groupPosition);
            Toast.makeText(context, gpInfo.name + " " + gpInfo.getChildren().get(childPosition).name
                    , Toast.LENGTH_SHORT).show();
            gpInfo.getChild(childPosition).onClick(childView);
            if (groupPosition == MENU_MAINTENANCE_GROUP) {
                switch (childPosition) {
                    case 0: // Rebuild Daily DB
                        // TODO - open status bar
                        maintenanceRebuildDailyDb(context, DEFAULT_DEVICE);
                        close();
                        break;
                }
                return true;
            } else if (groupPosition == MENU_LOG_GROUP) {
                switch (childPosition) {
                    case 0: // Enable log
                        break;
                    case 1: // Show log
                        showLog(context, parent);
                        break;
                    case 2: // Delete log
                        clearLog();
                        break;
                }
                return true;
            } else if (groupPosition == MENU_SYS_GROUP) {
                Intent intent = null;
                switch (childPosition) {
                    case 0:     // Network settings
                        intent = new Intent("android.settings.WIRELESS_SETTINGS");
                        break;
                    case 1:     // App settings
                        intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                        intent.setData(uri);
                        break;
                }
                if (intent != null) {
                    intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    getContext().startActivity(intent);
                }
            }
            return false;
        });
        expandableListView.setOnGroupClickListener((parent1, view, groupPosition, id) -> {
            if (groupPosition == MENU_ABOUT_GROUP) {
                showAbout();
                close();
                return true;
            }
            return false;
        });

        setDragger(holder.findViewById(R.id.menu_dialog_drag));
    }

    /*
    @Override
    public void onClick(View view) {
        super.onClick(view);
        int id = view.getId();
        switch (id) {
            case R.id.menu_dialog_drag:
                setDragger(view);
                break;
        }
    }
     */

    private void showLog(Context context, ViewGroup parent) {
        String logRows = "";
        String eol = "\n";
        try {
            Process process = Runtime.getRuntime().exec("logcat -d *:W");
            try (BufferedReader bufferedReader =
                         new BufferedReader(new InputStreamReader(process.getInputStream()))) {

                int maxLines = 30;
                StringBuilder log = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null && maxLines-- > 0) {
                    log.append(line);
                    log.append(eol);
                }
                logRows += log.toString();
            }

            // TODO - read ALogFile - merge into logRows

            FloatingDialog errorDlg = new FloatingDialog(parent, R.layout.log_dialog, R.layout.settings_shadow, R.id.logDialogClose);
            errorDlg.<TextView>viewById(R.id.logDialogText).setText(logRows);
            errorDlg.open();
        } catch (IOException ignore) {
        }
    }

    private void clearLog() {
        try {
            ALogFileWriter.Default.clear();
            Process process = Runtime.getRuntime().exec("logcat -c");
        } catch (Exception ignore) {
        }
    }

    // ---------------------------------------------------------------------------------------------
    public static void setTint(Drawable drawable, @ColorInt int color) {
        int[][] states = new int[][]{
                // new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        ColorStateList colorStateList = new ColorStateList(states, new int[]{color});

        drawable.setTintList(colorStateList);
        drawable.setTintMode(PorterDuff.Mode.MULTIPLY);
    }

    public void showAbout() {
        DateTime buildDate = new DateTime(BuildConfig.BuildTimeMilli);
        String appName = this.getString(R.string.app_name);
        String email = this.getString(R.string.email);
        String verStr = BuildConfig.VERSION_NAME;
        String msg = String.format("%s v%s\n%s", appName, verStr, email);

        FloatingDialog aboutDialog = new FloatingDialog(parent, R.layout.settings_about, INVALID_RES_ID, R.id.aboutSettingsClose);
        aboutDialog.open().startAnim();
        TextView appVerTx = aboutDialog.viewById(R.id.nav_version);
        appVerTx.setText(appVerTx.getText() + buildDate.toString("  (d-MMM-yy)"));

        // Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        Snackbar.make(getRootView(), msg, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }


    final Thread maintenanceThread = null;
    public void maintenanceRebuildDailyDb(@NonNull Context context, String devName) {
        if (maintenanceThread != null) {
            maintenanceThread.interrupt();
        }

        /*
        Activity activity = getActivity(context);
        if (activity instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity)activity;
            Fragment frag = FragUtils.getVisibleFragment(mainActivity);
            frag.statusPb;
        }
         */
        WxManager wxManager = WxManager.getInstance(context);
        WxViewModel progressVW = wxManager.viewModel();

        Thread worker = new Thread(() -> {
            DbUtil.rebuildDeviceDaily(context, devName, progressVW);
            // TODO - play sound when done and/or popup toast
            // MediaUtils.startPlayer(context, rawId, null);
        });
        worker.start();
    }

    // =============================================================================================
    static class MenuGroup extends ExpandList.GroupItemsInfo {

        int iconRes = R.drawable.ic_increase;

        public MenuGroup(int idx, String name, int selectionColor, int backgroundColor, int showChildIdx) {
            super(idx, name, selectionColor, backgroundColor, showChildIdx);
        }

        MenuGroup setIcon(int iconRes) {
            this.iconRes = iconRes;
            return this;
        }

        public MenuChild getChild(int childPos) {
            return (MenuChild) children.get(childPos);
        }

        @Override
        public MenuChild addChild(String childName, Object... values) {
            return (MenuChild) super.addChild(childName, values);
        }

        public MenuChild addChecked(String childName, boolean checked, Object... values) {
            return addChild(childName, values).setChecked(checked);
        }

        @Override
        public ExpandList.ChildItemsInfo createChild(String name, Object values) {
            return new MenuChild(name, values);
        }

        @Override
        public View getView(
                ExpandList.ExpandListAdapter<? extends ExpandList.GroupItemsInfo> expandListAdapter, int groupPosition,
                boolean isExpanded, View convertView, ViewGroup parent,
                int[] selected) {
            if (convertView == null) {
                Context context = parent.getContext();
                LayoutInflater inf = LayoutInflater.from(context);
                convertView = inf.inflate(R.layout.menu_list_group, parent, false);
            }

            ImageView groupIcon = convertView.findViewById(R.id.menu_group_icon);
            groupIcon.setImageResource(this.iconRes);
            TextView groupName = convertView.findViewById(R.id.menu_group_name);
            groupName.setText(this.name);
            // int selectedIdx = showChild(selected[groupPosition]);
            //groupName.setText(children.get(selectedIdx).name);
            convertView.setBackgroundColor(backgroundColor);
            return convertView;
        }
    }

    // =============================================================================================
    static class MenuChild extends ExpandList.ChildItemsInfo {
        int iconRes = R.drawable.ic_increase;
        Boolean checked = null;

        public MenuChild(String name, Object values) {
            super(name, values);
        }

        MenuChild setIcon(int iconRes) {
            this.iconRes = iconRes;
            return this;
        }

        MenuChild setChecked(boolean checked) {
            this.checked = checked;
            return this;
        }

        @Override
        public View getView(
                ExpandList.ExpandListAdapter<? extends ExpandList.GroupItemsInfo> expandListAdapter,
                int groupPosition,
                int childPosition,
                boolean isLastChild,
                View convertView,
                ViewGroup parent) {
            if (convertView == null) {
                Context context = parent.getContext();
                LayoutInflater infalInflater = LayoutInflater.from(context);
                convertView = infalInflater.inflate(R.layout.menu_list_child, parent, false);
            }

            ImageView groupIcon = convertView.findViewById(R.id.menu_child_icon);
            groupIcon.setImageResource(this.iconRes);

            CompoundButton childName = convertView.findViewById(R.id.menu_child_name);
            childName.setText(name);

            if (childName.getTag(R.id.check_drawables) == null) {
                childName.setTag(R.id.check_drawables, childName.getCompoundDrawablesRelative());
            }
            Drawable[] drawables = (Drawable[]) childName.getTag(R.id.check_drawables);
            if (checked != null) {
                childName.setChecked(checked);
                childName.setCompoundDrawablesRelative(drawables[0], drawables[1], drawables[2], drawables[3]);
            } else {
                childName.setCompoundDrawablesRelative(null, null, null, null);
            }

            return convertView;
        }

        public void onClick(View childView) {
            if (checked != null) {
                CompoundButton childName = childView.findViewById(R.id.menu_child_name);
                checked = !checked;
                childName.setChecked(checked);
            }
        }
    }
}
