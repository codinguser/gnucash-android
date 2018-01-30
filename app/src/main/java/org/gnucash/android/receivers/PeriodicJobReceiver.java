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

package org.gnucash.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.gnucash.android.util.BackupJob;

/**
 * Receiver to run periodic jobs.
 *
 * @author Àlex Magaz Graça <alexandre.magaz@gmail.com>
 */
public class PeriodicJobReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "PeriodicJobReceiver";

    public static final String ACTION_BACKUP = "org.gnucash.android.action_backup";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            Log.w(LOG_TAG, "No action was set in the intent. Ignoring...");
            return;
        }

        if (intent.getAction().equals(ACTION_BACKUP)) {
            BackupJob.enqueueWork(context);
        }
    }
}
