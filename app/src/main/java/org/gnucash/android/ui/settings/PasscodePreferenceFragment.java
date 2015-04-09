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

package org.gnucash.android.ui.settings;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;

import org.gnucash.android.R;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.passcode.PasscodePreferenceActivity;

/**
 * Fragment for configuring passcode to the application
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
@TargetApi(11)
public class PasscodePreferenceFragment extends PreferenceFragment {

    /**
     * * Request code for retrieving passcode to store
     */
    public static final int PASSCODE_REQUEST_CODE = 2;

    private SharedPreferences.Editor editor;
    private CheckBoxPreference checkBoxPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fragment_passcode_preferences);

        ActionBar actionBar = ((SherlockPreferenceActivity) getActivity()).getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.title_passcode_preferences);
    }

    @Override
    public void onResume() {
        super.onResume();

        editor = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit();
        final Intent intent = new Intent(getActivity(), PasscodePreferenceActivity.class);

        checkBoxPreference = (CheckBoxPreference) findPreference(getString(R.string.key_enable_passcode));
        final String passcodeEnabled = getString(R.string.title_passcode_enabled);
        final String passcodeDisabled = getString(R.string.title_passcode_disabled);
        checkBoxPreference.setTitle(checkBoxPreference.isChecked() ? passcodeEnabled : passcodeDisabled);
        checkBoxPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if ((Boolean) newValue) {
                            startActivityForResult(intent, PASSCODE_REQUEST_CODE);
                        } else {
                            checkBoxPreference.setTitle(passcodeDisabled);
                        }
                        editor.putBoolean(UxArgument.ENABLED_PASSCODE, (Boolean) newValue);
                        editor.commit();
                        return true;
                    }
                });
        findPreference(getString(R.string.key_change_passcode))
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        startActivityForResult(intent, PASSCODE_REQUEST_CODE);
                        return true;
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && requestCode == PASSCODE_REQUEST_CODE && data!= null) {
            editor.putString(UxArgument.PASSCODE, data.getStringExtra(UxArgument.PASSCODE));
            Toast.makeText(getActivity(), R.string.toast_passcode_set, Toast.LENGTH_SHORT).show();
            checkBoxPreference.setTitle(getString(R.string.title_passcode_enabled));
        } else {
            editor.putBoolean(UxArgument.ENABLED_PASSCODE, false);
            checkBoxPreference.setChecked(false);
        }
        editor.commit();
    }

}
