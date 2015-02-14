/*
 * Copyright (c) 2014 Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.ui.UxArgument;

/**
 * Activity for displaying and managing the passcode lock screen.
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class PasscodeLockScreenActivity extends SherlockFragmentActivity
        implements KeyboardFragment.OnPasscodeEnteredListener {

    private static final String TAG = "PasscodeLockScreenActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passcode_lockscreen);
    }

    @Override
    public void onPasscodeEntered(String pass) {
        String passcode = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getString(UxArgument.PASSCODE, "");
        Log.d(TAG, "Passcode: " + passcode);

        if (passcode.equals(pass)) {
            GnuCashApplication.PASSCODE_SESSION_INIT_TIME = System.currentTimeMillis();
            startActivity(new Intent()
                    .setClassName(this, getIntent().getStringExtra(UxArgument.PASSCODE_CLASS_CALLER))
                    .setAction(getIntent().getAction())
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(UxArgument.SELECTED_ACCOUNT_UID, getIntent().getStringExtra(UxArgument.SELECTED_ACCOUNT_UID))
            );
        } else {
            Toast.makeText(this, R.string.toast_wrong_passcode, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        GnuCashApplication.PASSCODE_SESSION_INIT_TIME = System.currentTimeMillis() - GnuCashApplication.SESSION_TIMEOUT;
        startActivity(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

}
