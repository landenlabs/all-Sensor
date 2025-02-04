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

package com.landenlabs.all_sensor.aux;

/*
@SuppressWarnings({"unused", "RedundantSuppression"})
class Scheduler {
    public static final int DEFAULT_SCH_MIN = 15;   // Schedule min interval 15min
    private static final String TAG = "Scheduler";
    private final Delegate delegate;
    private final WorkManager scheduler;

    // ---------------------------------------------------------------------------------------------

    public Scheduler(Context context, Delegate delegate) {
        scheduler = WorkManager.getInstance(context);
        this.delegate = delegate;
    }

    private String whatsActive() {
        scheduler.pruneWork();
        ListenableFuture<List<WorkInfo>> workers = scheduler.getWorkInfosByTag(TAG);

        try {
            if (workers.isCancelled()) {
                return "isCancelled";
            } else {
                List<WorkInfo> list = workers.get(100, TimeUnit.MILLISECONDS);
                if (list.size() > 0) {
                    WorkInfo workInfo = list.get(0);
                    WorkInfo.State state = workInfo.getState();
                    return " #active=" + list.size() + " " + state.toString();
                }
            }
        } catch (Exception ex) {
            ALog.e.tagMsg(this, ex.getMessage());
        }
        return "";
    }

    public void scheduleJob(String schdTag, int minutes) {
        // minutes must be greater than 15 minutes
        // see MIN_PERIODIC_INTERVAL_MILLIS
        PeriodicWorkRequest.Builder periodBuilder =
                new PeriodicWorkRequest.Builder(SchedulerService.class,
                        minutes, TimeUnit.MINUTES,
                        minutes/2, TimeUnit.MINUTES);

        Constraints myConstraints = new Constraints.Builder()
                // .setRequiresDeviceIdle(true)
                // .setRequiresCharging(false)
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        periodBuilder.setConstraints(myConstraints);
        periodBuilder.addTag(schdTag);
        periodBuilder.setBackoffCriteria(BackoffPolicy.EXPONENTIAL,
                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS);

        PeriodicWorkRequest workRequest = periodBuilder.build();
        // scheduler.enqueue(workRequest);
        scheduler.enqueueUniquePeriodicWork(schdTag,  ExistingPeriodicWorkPolicy.KEEP, workRequest);

        SimpleDateFormat dateFmt = new SimpleDateFormat("dd-MMM-yyyy ,", Locale.getDefault());
        Date buildDate = new Date(BuildConfig.BuildTimeMilli);

        delegate.update("Scheduled",
                "BuildDate",
                dateFmt.format(buildDate),
                "AppVersion",
                BuildConfig.VERSION_NAME,
                "Minutes=" + minutes,
                whatsActive());
    }

    public void cancelJob(String schdTag) {
        scheduler.cancelAllWorkByTag(schdTag);
        scheduler.pruneWork();
    }

    interface Delegate {
        void update(Object... msgs);
    }

}


 */