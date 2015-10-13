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
import android.content.ContentValues;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.ScheduledActionDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.export.ExportAsyncTask;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.model.Transaction;

import java.sql.Timestamp;
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
        Log.i(LOG_TAG, "Starting scheduled action service");
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                LOG_TAG);
        wakeLock.acquire();

        ScheduledActionDbAdapter scheduledActionDbAdapter = GnuCashApplication.getScheduledEventDbAdapter();
        List<ScheduledAction> scheduledActions = scheduledActionDbAdapter.getAllEnabledScheduledActions();

        for (ScheduledAction scheduledAction : scheduledActions) {
            long endTime    = scheduledAction.getEndTime();
            long now        = System.currentTimeMillis();
            long nextRunTime;
            do { //loop so that we can add transactions which were missed while device was off
                nextRunTime = scheduledAction.computeNextRunTime();
                if (((endTime > 0 && now < endTime) //if and endTime is set and we did not reach it yet
                        || (scheduledAction.getExecutionCount() < scheduledAction.getTotalFrequency()) //or the number of scheduled runs
                        || (endTime == 0 && scheduledAction.getTotalFrequency() == 0)) //or the action is to run forever
                        && (nextRunTime <= now)  //one period has passed since last execution
                        && scheduledAction.getStartTime() <= now
                        && scheduledAction.isEnabled()) { //the start time has arrived
                    executeScheduledEvent(scheduledAction);
                }
            } while (nextRunTime <= now && scheduledAction.getActionType() == ScheduledAction.ActionType.TRANSACTION);
        }

        Log.i(LOG_TAG, "Completed service @ " + SystemClock.elapsedRealtime());

        wakeLock.release();
    }

    /**
     * Executes a scheduled event according to the specified parameters
     * @param scheduledAction ScheduledEvent to be executed
     */
    private void executeScheduledEvent(ScheduledAction scheduledAction){
        Log.i(LOG_TAG, "Executing scheduled action: " + scheduledAction.toString());
        switch (scheduledAction.getActionType()){
            case TRANSACTION:
                String eventUID = scheduledAction.getActionUID();
                TransactionsDbAdapter transactionsDbAdapter = TransactionsDbAdapter.getInstance();
                Transaction trxnTemplate = transactionsDbAdapter.getRecord(eventUID);
                Transaction recurringTrxn = new Transaction(trxnTemplate, true);

                //we may be executing scheduled action significantly after scheduled time (depending on when Android fires the alarm)
                //so compute the actual transaction time from pre-known values
                long transactionTime = scheduledAction.computeNextRunTime(); //default
                recurringTrxn.setTime(transactionTime);
                recurringTrxn.setCreatedTimestamp(new Timestamp(transactionTime));
                transactionsDbAdapter.addRecord(recurringTrxn);
                break;

            case BACKUP:
                ExportParams params = ExportParams.parseCsv(scheduledAction.getTag());
                try {
                    //wait for async task to finish before we proceed (we are holding a wake lock)
                    new ExportAsyncTask(GnuCashApplication.getAppContext()).execute(params).get();
                } catch (InterruptedException | ExecutionException e) {
                    //TODO: Create special log for scheduler service
                    Crashlytics.logException(e);
                    Log.e(LOG_TAG, e.getMessage());
                    return; //return immediately, do not update last run time of event
                }
                break;
        }

        long lastRun = scheduledAction.computeNextRunTime();
        int executionCount = scheduledAction.getExecutionCount() + 1;
        //update the last run time and execution count
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseSchema.ScheduledActionEntry.COLUMN_LAST_RUN, lastRun);
        contentValues.put(DatabaseSchema.ScheduledActionEntry.COLUMN_EXECUTION_COUNT, executionCount);
        ScheduledActionDbAdapter.getInstance().updateRecord(scheduledAction.getUID(), contentValues);

        scheduledAction.setLastRun(lastRun);
        scheduledAction.setExecutionCount(executionCount);
    }
}
