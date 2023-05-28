/* Copyright (c) 2018 Àlex Magaz Graça <alexandre.magaz@gmail.com>
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

package org.gnucash.android.util;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import android.util.Log;


/**
 * Job to back up books periodically.
 *
 * <p>The backups are triggered by an alarm set in
 * {@link BackupManager#schedulePeriodicBackups(Context)}
 * (through {@link org.gnucash.android.receivers.PeriodicJobReceiver}).</p>
 */
public class BackupJob extends JobIntentService {
    private static final String LOG_TAG = "BackupJob";
    private static final int JOB_ID = 1000;

    public static void enqueueWork(Context context) {
        Intent intent = new Intent(context, BackupJob.class);
        enqueueWork(context, BackupJob.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.i(LOG_TAG, "Doing backup of all books.");
        BackupManager.backupAllBooks();
    }
}
