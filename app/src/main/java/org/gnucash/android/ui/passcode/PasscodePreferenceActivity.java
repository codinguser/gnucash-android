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
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.gnucash.android.R;
import org.gnucash.android.ui.UxArgument;

/**
 * Activity for entering and confirming passcode
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class PasscodePreferenceActivity extends SherlockFragmentActivity
        implements KeyboardFragment.OnPasscodeEnteredListener {

    private boolean reenter = false;
    private String passcode;

    private boolean checkOldPassCode;

    private TextView passCodeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passcode_lockscreen);

        passCodeTextView= (TextView) findViewById(R.id.passcode_label);

        checkOldPassCode = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getBoolean(UxArgument.ENABLED_PASSCODE, false);

        if (checkOldPassCode) {
            passCodeTextView.setText("Enter your old passcode");
        }
    }

    @Override
    public void onPasscodeEntered(String pass) {
        String passCode = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getString(UxArgument.PASSCODE, "");

        if (checkOldPassCode) {
            if (pass.equals(passCode)) {
                checkOldPassCode = false;
                passCodeTextView.setText("Enter your new passcode");
            } else {
                Toast.makeText(this, R.string.toast_wrong_passcode, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (reenter) {
            if (passcode.equals(pass)) {
                setResult(RESULT_OK, new Intent().putExtra(UxArgument.PASSCODE, pass));
                finish();
            } else {
                Toast.makeText(this, R.string.toast_invalid_passcode_confirmation, Toast.LENGTH_LONG).show();
            }
        } else {
            passcode = pass;
            reenter = true;
            ((TextView) findViewById(R.id.passcode_label)).setText(R.string.toast_confirm_passcode);
            Toast.makeText(this, R.string.toast_confirm_passcode, Toast.LENGTH_SHORT).show();
        }
    }

}
