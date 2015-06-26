/*
 * Copyright (c) 2014 - 2015 Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
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
import android.support.v7.app.ActionBarActivity;
import android.widget.TextView;
import android.widget.Toast;

import org.gnucash.android.R;
import org.gnucash.android.ui.UxArgument;

/**
 * Activity for entering and confirming passcode
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class PasscodePreferenceActivity extends ActionBarActivity
        implements KeyboardFragment.OnPasscodeEnteredListener {

    private boolean mIsPassEnabled;
    private boolean mReenter = false;
    private String mPasscode;

    private TextView mPassTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passcode_lockscreen);

        mPassTextView = (TextView) findViewById(R.id.passcode_label);

        mIsPassEnabled = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getBoolean(UxArgument.ENABLED_PASSCODE, false);

        if (mIsPassEnabled) {
            mPassTextView.setText(R.string.label_old_passcode);
        }
    }

    @Override
    public void onPasscodeEntered(String pass) {
        String passCode = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getString(UxArgument.PASSCODE, "");

        if (mIsPassEnabled) {
            if (pass.equals(passCode)) {
                mIsPassEnabled = false;
                mPassTextView.setText(R.string.label_new_passcode);
            } else {
                Toast.makeText(this, R.string.toast_wrong_passcode, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (mReenter) {
            if (mPasscode.equals(pass)) {
                setResult(RESULT_OK, new Intent().putExtra(UxArgument.PASSCODE, pass));
                finish();
            } else {
                Toast.makeText(this, R.string.toast_invalid_passcode_confirmation, Toast.LENGTH_LONG).show();
            }
        } else {
            mPasscode = pass;
            mReenter = true;
            mPassTextView.setText(R.string.label_confirm_passcode);
        }
    }

}
