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

package com.landenlabs.all_sensor;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static com.landenlabs.all_sensor.aux.SchedulerService.ACTION_SCHED_FIRED;
import static com.landenlabs.all_sensor.aux.SysInfo.getBatteryInfo;
import static com.landenlabs.all_sensor.logger.AppLog.logFileLn;
import static com.landenlabs.all_sensor.sensor.WxViewModel.FAILED_LOGIN_MSG;
import static com.landenlabs.all_sensor.sensor.WxViewModel.FAILED_SUMMARY_MSG;
import static com.landenlabs.all_sensor.sensor.WxViewModel.LOAD_DATA_MSG;
import static com.landenlabs.all_sensor.sensor.WxViewModel.LOAD_DONE_MSG;
import static com.landenlabs.all_sensor.sensor.WxViewModel.LOAD_START_MSG;
import static com.landenlabs.all_sensor.sensor.WxViewModel.LOAD_SUMMARY_MSG;
import static com.landenlabs.all_sensor.sensor.WxViewModel.SERIES_MSG;
import static com.landenlabs.all_sensor.sensor.WxViewModel.STATUS_MSG;
import static com.landenlabs.all_sensor.utils.FragUtils.addOrExecuteShortcut;
import static com.landenlabs.all_sensor.utils.FragUtils.getCurrentFragmentName;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.graphics.Typeface;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavAction;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.landenlabs.all_sensor.aux.Notify;
import com.landenlabs.all_sensor.aux.SchedulerService;
import com.landenlabs.all_sensor.logger.ALog;
import com.landenlabs.all_sensor.logger.ALogFileWriter;
import com.landenlabs.all_sensor.logger.AppLog;
import com.landenlabs.all_sensor.logger.StrictModeUtil;
import com.landenlabs.all_sensor.sensor.AppCfg;
import com.landenlabs.all_sensor.sensor.DeviceItem;
import com.landenlabs.all_sensor.sensor.DeviceListManager;
import com.landenlabs.all_sensor.sensor.DeviceSummary;
import com.landenlabs.all_sensor.sensor.WxManager;
import com.landenlabs.all_sensor.sensor.WxViewModel;
import com.landenlabs.all_sensor.ui.utils.GlobalModel;
import com.landenlabs.all_sensor.ui.view.BaseFragment;
import com.landenlabs.all_sensor.ui.view.MenuDialog;
import com.landenlabs.all_sensor.utils.ArrayListEx;
import com.landenlabs.all_sensor.utils.DataUtils;
import com.landenlabs.all_sensor.utils.NetInfo;
import com.landenlabs.all_sensor.utils.ShareUtil;
import com.landenlabs.all_sensor.utils.StrUtils;
import com.landenlabs.all_sensor.utils.UncaughtExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@SuppressWarnings("FieldCanBeLocal")
public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private String intentAction = "";
    private ViewGroup rootView;
    private MenuDialog customMenu;
    private NavController navController;

    private final boolean enableSheet = false;
    private UncaughtExceptionHandler uncaughtExceptionHandler;
    private GlobalModel globalModel; //

    public static final String INTENT_NAME = "LocalReceiver";
    public static final String INTENT_EXTRA_ACTION = "XAction";
    private final LocalReceiver localReceiver = new LocalReceiver();
    private WorkManager scheduler;

    private WxManager wxManager;

    // ---------------------------------------------------------------------------------------------
    public MainActivity() {
        //this.globalModel = new ViewModelProvider(this).get(GlobalModel.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ALog.init(this);
        StrictModeUtil.setStrictMode(ALog.isDebugApp(this));

        // Initialize Joda before we use it.
        // JodaTimeAndroid.init(this);
        ALog.d.tagMsg(this, "onCreate");
        globalModel = new ViewModelProvider(this).get(GlobalModel.class);
        // initDatabase();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        rootView = (ViewGroup) toolbar.getRootView();
        customMenu = new MenuDialog(this, rootView);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_start) // This page Ids to show menu in uppper left, else arrow.
                .setOpenableLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.nav_start) {
                if (getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE) {
                    // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
            }
        });

        // Setup shortcuts for Bottom Nav items.
        intentAction = getIntent() != null ? getIntent().getAction() : null;
        navigationView.post(() -> {
            ViewGroup bottomNavigationView = findViewById(R.id.nav_host_fragment);
            if (bottomNavigationView != null) {
                addOrExecuteShortcut(MainActivity.this, bottomNavigationView, intentAction);
            }
        });

        drawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                ImageView viewAnim = drawerView.findViewById(R.id.nav_header_anim_pulse);
                if (viewAnim.getDrawable() instanceof AnimatedVectorDrawable) {
                    ((AnimatedVectorDrawable) viewAnim.getDrawable()).start();
                }
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });

        wxManager = WxManager.getInstance(this);
        wxManager.viewModel.getStatus().observeForever(
                state -> { updateStatus(state); wxManager.viewModel.getStatus().next(); });  // Forever to get inActive lifeCycle updates
        wxManager.viewModel.getProgress().observe(this,
                state -> { updateStatus(state); wxManager.viewModel.getProgress().next(); });

        setupScheduler();
    }

    private void setupScheduler() {
        final String WORK_TAG = "SenSchd";

        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver,
                new IntentFilter(INTENT_NAME));
        localReceiver.start();

        scheduler = WorkManager.getInstance(getApplicationContext());

        // min must be greater than 15 minutes
        // see MIN_PERIODIC_INTERVAL_MILLIS
        int min = 20;
        PeriodicWorkRequest.Builder periodBuilder =
                new PeriodicWorkRequest.Builder(SchedulerService.class,
                        min,  TimeUnit.MINUTES,
                        min/2, TimeUnit.MINUTES);

        Constraints myConstraints = new Constraints.Builder()
                // .setRequiresDeviceIdle(true)
                // .setRequiresCharging(false)
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        periodBuilder.setBackoffCriteria(BackoffPolicy.EXPONENTIAL,
                OneTimeWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS);
        periodBuilder.setConstraints(myConstraints);
        periodBuilder.addTag(WORK_TAG);

        PeriodicWorkRequest workRequest = periodBuilder.build();
        scheduler.enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.KEEP, workRequest);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Use adb to optionally override logging in release build.
        // adb shell setprop  log.tag.ALog DEBUG
        // adb shell setprop  log.tag.MLog DEBUG
        // adb shell setprop  log.tag.TLog DEBUG
        int releaseLogLevel = Log.isLoggable(ALog.PROPERTY_TAG, ALog.PROPERTY_LEVEL)
                ? ALog.DEBUG : ALog.NOLOGGING;       // restore using NOLOGGING for std release app.
        boolean isDebug = ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
        ALog.minLevel = isDebug ? ALog.DEBUG : releaseLogLevel;

        AppLog.init(this);
        ALogFileWriter.init(this);
        ALog.d.tagMsg(this, "MainActivity started");

        uncaughtExceptionHandler = new UncaughtExceptionHandler(this);

        getPermission();
        if (enableSheet) {
            // SheetUtil.initWriterThread(this);
        }

        initNavMap();

        Notify.init(this);
        restoreSettings();

        wxManager.start(this, true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        ALogFileWriter.Default.close();
        saveSettings();
        wxManager.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_menu) {
            customMenu.open();
        } else if (itemId == R.id.menu_share) {
            share();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    // ---------------------------------------------------------------------------------------------
    //  Swipe left/right page changer

    /*
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return gesture.onTouchEvent(ev) || super.dispatchTouchEvent(ev);
    }
    */

    protected GestureDetector gesture;
    protected final ArrayListEx<NavDestination> navArray = new ArrayListEx<>();

    private void initNavMap() {
        navArray.clear();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavDestination navDst = navController.getCurrentDestination();
        while (navDst != null) {
            navArray.add(navDst);
            NavAction act = navDst.getAction(R.id.action_id);
            navDst = (act == null) ? null : navController.getGraph().findNode(act.getDestinationId());
        }
        /*
        Iterator<NavDestination> navIT = navController.getGraph().iterator();
        while (navIT.hasNext()) {
            navArray.add(navIT.next());
        }
         */

        gesture = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    // MotionEvent downEv;
                    @Override
                    public boolean onDown(MotionEvent ev) {
                        // downEv = ev;
                        // return true;
                        return false;
                    }
                    @Override
                    public boolean onFling(
                            MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                        if (e1 != null && e2 != null) {
                            final int SWIPE_MIN_DISTANCE = 120;
                            float dY = e1.getY() - e2.getY();
                            float dX = e1.getX() - e2.getX();
                            if (Math.abs(dX) > 2 * Math.abs(dY)
                                    && Math.abs(dX) > SWIPE_MIN_DISTANCE
                                    && validSwipeRegion(e1)) {
                                NavDestination navDst = findNav((dX > 0) ? 1 : -1);
                                if (navDst.getId() != navController.getCurrentDestination().getId()) {
                                    Toast.makeText(MainActivity.this, "Change page to " + navDst.getLabel(), Toast.LENGTH_LONG).show();
                                    runOnUiThread(() -> {
                                        // navController.popBackStack();
                                        navController.navigate(navDst.getId());
                                    });

                                    return true;
                                }
                            }
                        }
                        super.onFling(e1, e2, velocityX, velocityY);
                        return false;
                    }
                });
    }

    NavDestination findNav(int direction) {
        int idx = 0;
        int currentId = navController.getCurrentDestination().getId();
        CharSequence lbl = navController.getCurrentDestination().getLabel();
        while (idx < navArray.size() && navArray.get(idx).getId() != currentId) {
            idx++;
        }
        return navArray.get((idx + direction + navArray.size()) % navArray.size());
    }

    boolean validSwipeRegion(MotionEvent ev) {
        BaseFragment baseFragment = getVisibleFragment();
        return baseFragment == null || baseFragment.validSwipeRegion(ev);
    }

    private BaseFragment getVisibleFragment() {
        NavHostFragment navHostFragment = (NavHostFragment)getSupportFragmentManager().getPrimaryNavigationFragment();
        FragmentManager fragmentManager = navHostFragment.getChildFragmentManager();
        Fragment fragment = fragmentManager.getPrimaryNavigationFragment();
        if (fragment instanceof BaseFragment){
            return (BaseFragment)fragment;
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /*
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == SheetUtil.MY_ACTIVITYS_AUTH_REQUEST_CODE) {
            try {
                // The Task returned from this call is always completed, no need to attach  a listener.
                Task<GoogleSignInAccount> tasks = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = tasks.getResult();
                if (account != null && account.getEmail() != null) {
                    ALog.d.tagMsg(this, account.getEmail());
                }
            } catch (Exception ex) {
                String msg = "Sheet signin FAILED " + ex.getMessage();
                ALog.e.tagMsg(this, msg);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        }
         */
    }

    private void getPermission() {

        /*
        if (enableSheet) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                ALog.w.tagMsg(this, "Don't have permission to access accounts");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        102);
            }

            SheetUtil.requestAccess(this);
        }
        */

        // checkPermissions(Manifest.permission.ACCESS_FINE_LOCATION);  for Wifi router SSID
    }

    /*
    private SharedPreferences getPref() {
        return getSharedPreferences("test", Context.MODE_PRIVATE);
    }
     */

    private void saveSettings() {
        /*
        getPref().edit()
                .putInt(STEP_MIN_KEY, seekStepSb.getProgress())
                .putBoolean(APPEND_KEY, appendMenuItem.isChecked())
                .putBoolean(AUTO_PERIOD_KEY, periodic.isChecked())
                .apply();
         */
    }

    private void restoreSettings() {
        /*
        seekStepSb.setProgress(getPref().getInt(STEP_MIN_KEY, DEFAULT_SCH_MIN));
        periodic.setChecked(getPref().getBoolean(AUTO_PERIOD_KEY, true));
        if (getPref().getBoolean(APPEND_KEY, true)) {
            readLogIntoScroller();
        }
         */
    }

    public void share() {
        final Activity activity = this;
        final String toEmail = "wsimobile2@gmail.com";

        String subject = getCurrentFragmentName(MainActivity.this);
        // Snackbar.make(rootView, "Sharing Log Files", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        // ShareUtil.shareFileLog(activity);
        Snackbar.make(rootView, "Sharing Screen", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        ShareUtil.shareScreen(activity, rootView, toEmail, subject, null);
    }

    // ---------------------------------------------------------------------------------------------

    private void updateStatus(WxViewModel.Progress progress) {
    }

    private void updateStatus(Integer status) {
        ALog.d.tagMsg(this, "updateStatus ", status);
        switch (status) {
            case SERIES_MSG:
                break;
            case STATUS_MSG:
            case LOAD_START_MSG:
            case LOAD_SUMMARY_MSG:
            case LOAD_DATA_MSG:
            case LOAD_DONE_MSG:
                updateStatus();
                break;

            case FAILED_LOGIN_MSG:
                ALog.e.tagMsg(this, "Device Login Failed");
                break;
            case FAILED_SUMMARY_MSG:
                ALog.e.tagMsg(this, "Device Summary Failed");
                break;
        }
    }

    final Map<String, Integer> lastUpdateHash = new HashMap<>();

    private void updateStatus() {
        AppCfg appCfg = AppCfg.getInstance(this);
        for (DeviceItem deviceItem : DeviceListManager.getDevices()) {
            DeviceSummary summary = deviceItem.getSummary(this, appCfg);
            int sumHC = summary.hashCode();
            Integer lastHC = lastUpdateHash.get(deviceItem.name);
            if (lastHC == null || lastHC != sumHC) {
                lastUpdateHash.put(deviceItem.name, sumHC);
                // TOOD - assumes all values are present.
                // SpannableString strTime = SpanUtil.SString(widCfg.timeFmt().format(new Date(summary.milli)), SpanUtil.SS_NONE);
                // SpannableString strDate = SpanUtil.SString(widCfg.dateFmt().format(new Date(summary.milli)), SpanUtil.SS_NONE);

                CharSequence time = summary.strTime;
                CharSequence tempF = summary.strTemp;
                CharSequence humP = summary.strHum;
                String msg = getString(R.string.sensor_summary, tempF, humP, time);
                SpannableString ssMsg = new SpannableString(msg);
                ssMsg.setSpan(new StyleSpan(Typeface.BOLD), 0, msg.length(), 0);
                int start = msg.indexOf(tempF.toString());
                @ColorInt int tempColor = getColor(R.color.temRed);
                ssMsg.setSpan(new ForegroundColorSpan(tempColor), start, start + tempF.length(), 0);
                start = msg.indexOf(humP.toString());
                @ColorInt int humidtyColor = getColor(R.color.humBlue);
                ssMsg.setSpan(new ForegroundColorSpan(humidtyColor), start, start + humP.length(), 0);
                // addAndLog(this, tempF.replaceAll("[.][0-9]",""), ssMsg);
                addAndLog(this, String.format("%.0f", appCfg.toDegreeN(summary.numTemp)), ssMsg);
            }
        }
    }

    public static void addAndLog(Context context, String notify,  Object... msgs) {
        // String timeStr = DateFormat.getDateTimeInstance().format(new Date());
        String timeStr = ALogFileWriter.getCurrentTimeStamp();
        CharSequence logMsg = DataUtils.join(" ", msgs);
        // SpannableString ssMsg = new SpannableString(logMsg);
        // ssMsg.setSpan(new StyleSpan(Typeface.BOLD), timeStr.length(), logMsg.length(), 0);
        ALog.d.tagMsg("main", logMsg);
        logFileLn(Log.DEBUG, "Sen", msgs);
        // SheetUtil.addRow(timeStr, msgs);
        if (AppCfg.getInstance(context).showNotification) {
            Notify.updateNotification(context, notify, timeStr, msgs);
        }
    }

    /**
     * BroadcastReceiver class to process Local intent receiver to update viewer.
     * <p>
     * Background thread is used to acquire Radar Status report. After it completes it fires
     * a local broadcast intent which is received by this class to allow the UI thread to update
     * the radar visual status indicators.
     * <p>
     * Update radar markers and select layer based on camera focus.
     */
    @SuppressWarnings("EmptyMethod")
    public static class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra(INTENT_EXTRA_ACTION);
            if (StrUtils.hasText(action)) {
                ALog.d.tagMsg(this, " Received Sensor Status action=" + action);
                //noinspection SwitchStatementWithTooFewBranches
                switch (action) {
                    case ACTION_SCHED_FIRED:
                        NetInfo.NetStatus netStatus = new NetInfo.NetStatus(context);
                        @ColorInt int[] darkColors = new int[]{0xff309020, 0xffd08030, 0xffffb0b0};
                        addAndLog(context, null, NetInfo.getStatus(context, netStatus, "", darkColors)); // getNetworkInfo(context),
                        addAndLog(context, null, (Object[])getBatteryInfo(context));
                        break;
                    default:
                        break;
                }
            }
        }

        void start() {
        }
    }
}

