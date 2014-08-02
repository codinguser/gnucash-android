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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.gnucash.android.R;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.account.AccountsActivity;

/**
 * Activity for displaying and managing the passcode lock screen.
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class PasscodeLockScreenActivity extends Activity {

    private static final String TAG = "PasscodeLockScreenActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passcode_lockscreen);

        final SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        if (!sharedPreferences.getBoolean(UxArgument.ENABLED_PASSCODE, false)) {
            startActivity(new Intent(this, AccountsActivity.class));
        }

        final EditText passcodeEditText = (EditText) findViewById(R.id.passcode);
        passcodeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                String pass = editable.toString();
                String passcode = sharedPreferences.getString(UxArgument.PASSCODE, "");
                Log.d(TAG, passcode);
                if (pass.length() == 4) {
                    if (pass.equals(passcode)) {
                        startActivity(new Intent(getApplicationContext(), AccountsActivity.class));
                    } else {
                        Toast.makeText(getApplicationContext(),
                                R.string.toast_wrong_passcode, Toast.LENGTH_SHORT).show();
                        passcodeEditText.getText().clear();
                    }
                }
            }
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        passcodeEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN)
                        && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    Toast.makeText(getApplicationContext(),
                            R.string.toast_enter_full_passcode, Toast.LENGTH_LONG).show();
                }
                return false;
            }
        });

    }

}
