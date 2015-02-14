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
import android.os.SystemClock;
import android.util.Log;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.ScheduledEventDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.ExporterAsyncTask;
import org.gnucash.android.model.ScheduledEvent;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.receivers.SchedulerReceiver;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Service for running scheduled events
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class SchedulerService extends IntentService {

    public static final String LOG_TAG = "SchedulerService";

    /**
     * Creates an IntentService
     *
     */
    public SchedulerService() {
        super("SchedulerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ScheduledEventDbAdapter scheduledEventDbAdapter = GnuCashApplication.getScheduledEventDbAdapter();
        List<ScheduledEvent> scheduledEvents = scheduledEventDbAdapter.getAllScheduledEvents();

        for (ScheduledEvent scheduledEvent : scheduledEvents) {
            long lastRun    = scheduledEvent.getLastRun();
            long period     = scheduledEvent.getPeriod();
            long endTime    = scheduledEvent.getEndTime();

            long now = System.currentTimeMillis();
            //if we did not exceed the endtime, and one execution period has passed since last run
            if (endTime > 0 && now < endTime && (lastRun + period) < now ){
                executeScheduledEvent(scheduledEvent);
            }
        }

        Log.i(LOG_TAG, "Completed service @ " + SystemClock.elapsedRealtime());
        SchedulerReceiver.completeWakefulIntent(intent);
    }

    /**
     * Executes a scheduled event according to the specified parameters
     * @param scheduledEvent ScheduledEvent to be executed
     */
    private void executeScheduledEvent(ScheduledEvent scheduledEvent){
        switch (scheduledEvent.getEventType()){
            case TRANSACTION:
                String eventUID = scheduledEvent.getEventUID();
                TransactionsDbAdapter transactionsDbAdapter = GnuCashApplication.getTransactionDbAdapter();
                Transaction trxnTemplate = transactionsDbAdapter.getTransaction(eventUID);
                Transaction recurringTrxn = new Transaction(trxnTemplate, true);
                recurringTrxn.setTime(System.currentTimeMillis());

                transactionsDbAdapter.addTransaction(recurringTrxn);
                break;

            case EXPORT:
                ExportParams params = ExportParams.parseCsv(scheduledEvent.getTag());
                try {
                    new ExporterAsyncTask(GnuCashApplication.getAppContext()).execute(params).get();
                } catch (InterruptedException e) {
                    //TODO: Create special log for scheduler service
                    Log.e(LOG_TAG, e.getMessage());
                } catch (ExecutionException e) {
                    //TODO: Log to crashlytics
                    e.printStackTrace();
                    Log.e(LOG_TAG, e.getMessage());
                }
                break;
        }

    }
}
