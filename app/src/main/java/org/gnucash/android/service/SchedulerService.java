/*
 * Copyright (c) 2015 Ngewi Fet <ngewif@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gnucash.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.ScheduledActionDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.ExporterAsyncTask;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.model.Transaction;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Service for running scheduled events.
 * <p>The service is started and goes through all scheduled event entries in the the database and executes them.
 * Then it is stopped until the next time it is run. <br>
 * Scheduled runs of the service should be achieved using an {@link android.app.AlarmManager}</p>
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class SchedulerService extends IntentService {

    public static final String LOG_TAG = "SchedulerService";

    /**
     * Creates an IntentService
     *
     */
    public SchedulerService() {
        super(LOG_TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                LOG_TAG);
        wakeLock.acquire();

        ScheduledActionDbAdapter scheduledActionDbAdapter = GnuCashApplication.getScheduledEventDbAdapter();
        List<ScheduledAction> scheduledActions = scheduledActionDbAdapter.getAllEnabledScheduledActions();

        for (ScheduledAction scheduledAction : scheduledActions) {
            long lastRun    = scheduledAction.getLastRun();
            long period     = scheduledAction.getPeriod();
            long endTime    = scheduledAction.getEndTime();

            long now = System.currentTimeMillis();
            //if we did not exceed the endtime (if there is one), and one execution period has passed since last run
            if (((endTime > 0 && now < endTime) || (scheduledAction.getExecutionCount() < scheduledAction.getNumberOfOccurences()) || endTime == 0)
                    && (lastRun + period) < now ){
                executeScheduledEvent(scheduledAction);
            }
        }

        Log.i(LOG_TAG, "Completed service @ " + SystemClock.elapsedRealtime());

        wakeLock.release();
    }

    /**
     * Executes a scheduled event according to the specified parameters
     * @param scheduledAction ScheduledEvent to be executed
     */
    private void executeScheduledEvent(ScheduledAction scheduledAction){
        switch (scheduledAction.getActionType()){
            case TRANSACTION:
                String eventUID = scheduledAction.getActionUID();
                TransactionsDbAdapter transactionsDbAdapter = TransactionsDbAdapter.getInstance();
                Transaction trxnTemplate = transactionsDbAdapter.getTransaction(eventUID);
                Transaction recurringTrxn = new Transaction(trxnTemplate, true);
                recurringTrxn.setTime(System.currentTimeMillis());

                transactionsDbAdapter.addTransaction(recurringTrxn);
                break;

            case EXPORT:
                ExportParams params = ExportParams.parseCsv(scheduledAction.getTag());
                try {
                    new ExporterAsyncTask(GnuCashApplication.getAppContext()).execute(params).get();
                } catch (InterruptedException e) {
                    //TODO: Create special log for scheduler service
                    Log.e(LOG_TAG, e.getMessage());
                    return; //return immediately, do not update last run time of event
                } catch (ExecutionException e) {
                    //TODO: Log to crashlytics
                    e.printStackTrace();
                    Log.e(LOG_TAG, e.getMessage());
                    return; //return immediately, do not update last run time of event
                }
                break;
        }

        //update last run time
        ScheduledActionDbAdapter.getInstance().updateRecord(
                scheduledAction.getUID(),
                DatabaseSchema.ScheduledActionEntry.COLUMN_LAST_RUN,
                Long.toString(System.currentTimeMillis()));

        //update the execution count
        ScheduledActionDbAdapter.getInstance().updateRecord(
                scheduledAction.getUID(),
                DatabaseSchema.ScheduledActionEntry.COLUMN_EXECUTION_COUNT,
                Integer.toString(scheduledAction.getExecutionCount()+1));
    }
}
