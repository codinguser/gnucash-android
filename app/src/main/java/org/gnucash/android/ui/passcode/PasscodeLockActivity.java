/*
 * Copyright (c) 2014-2015 Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
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

package org.gnucash.android.ui.passcode;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager.LayoutParams;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.ui.common.UxArgument;

/**
 * This activity used as the parent class for enabling passcode lock
 *
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 * @see org.gnucash.android.ui.account.AccountsActivity
 * @see org.gnucash.android.ui.transaction.TransactionsActivity
 */
public class PasscodeLockActivity extends AppCompatActivity {

    private static final String TAG = "PasscodeLockActivity";

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean isPassEnabled = prefs.getBoolean(UxArgument.ENABLED_PASSCODE, false);
        if (isPassEnabled) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                getWindow().addFlags(LayoutParams.FLAG_SECURE);
            }
        } else {
            getWindow().clearFlags(LayoutParams.FLAG_SECURE);
        }

        // Only for Android Lollipop that brings a few changes to the recent apps feature
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            GnuCashApplication.PASSCODE_SESSION_INIT_TIME = 0;
        }

        // see ExportFormFragment.onPause()
        boolean skipPasscode = prefs.getBoolean(UxArgument.SKIP_PASSCODE_SCREEN, false);
        prefs.edit().remove(UxArgument.SKIP_PASSCODE_SCREEN).apply();
        String passCode = prefs.getString(UxArgument.PASSCODE, "");

        if (isPassEnabled && !isSessionActive() && !passCode.trim().isEmpty() && !skipPasscode) {
            Log.v(TAG, "Show passcode screen");
            startActivity(new Intent(this, PasscodeLockScreenActivity.class)
                            .setAction(getIntent().getAction())
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            .putExtra(UxArgument.PASSCODE_CLASS_CALLER, this.getClass().getName())
                            .putExtras(getIntent().getExtras())
            );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        GnuCashApplication.PASSCODE_SESSION_INIT_TIME = System.currentTimeMillis();
    }

    /**
     * @return {@code true} if passcode session is active, and {@code false} otherwise
     */
    private boolean isSessionActive() {
        return System.currentTimeMillis() - GnuCashApplication.PASSCODE_SESSION_INIT_TIME
                < GnuCashApplication.SESSION_TIMEOUT;
    }

}
