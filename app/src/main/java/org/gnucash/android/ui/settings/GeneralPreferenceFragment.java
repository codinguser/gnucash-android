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

package org.gnucash.android.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.widget.Toast;

import org.gnucash.android.R;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.passcode.PasscodeLockScreenActivity;
import org.gnucash.android.ui.passcode.PasscodePreferenceActivity;

/**
 * Fragment for general preferences. Currently caters to the passcode and reporting preferences
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class GeneralPreferenceFragment extends PreferenceFragmentCompat implements
        android.support.v7.preference.Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    /**
     * Request code for retrieving passcode to store
     */
    public static final int PASSCODE_REQUEST_CODE = 0x2;
    /**
     * Request code for disabling passcode
     */
    public static final int REQUEST_DISABLE_PASSCODE = 0x3;
    /**
     * Request code for changing passcode
     */
    public static final int REQUEST_CHANGE_PASSCODE = 0x4;

    private SharedPreferences.Editor mEditor;
    private CheckBoxPreference mCheckBoxPreference;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.fragment_general_preferences);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.title_general_prefs);
    }

    @Override
    public void onResume() {
        super.onResume();

        final Intent intent = new Intent(getActivity(), PasscodePreferenceActivity.class);

        mCheckBoxPreference = (CheckBoxPreference) findPreference(getString(R.string.key_enable_passcode));
        mCheckBoxPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) {
                    startActivityForResult(intent, PASSCODE_REQUEST_CODE);
                } else {
                    Intent passIntent = new Intent(getActivity(), PasscodeLockScreenActivity.class);
                    passIntent.putExtra(UxArgument.DISABLE_PASSCODE, UxArgument.DISABLE_PASSCODE);
                    startActivityForResult(passIntent, REQUEST_DISABLE_PASSCODE);
                }
                return true;
            }
        });
        findPreference(getString(R.string.key_change_passcode)).setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (key.equals(getString(R.string.key_change_passcode))) {
            startActivityForResult(
                    new Intent(getActivity(), PasscodePreferenceActivity.class),
                    REQUEST_CHANGE_PASSCODE
            );
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference,
                                      Object newValue) {

        if (preference.getKey()
                      .equals(getString(R.string.key_enable_passcode))) {

            if ((Boolean) newValue) {

                startActivityForResult(new Intent(getActivity(),
                                                  PasscodePreferenceActivity.class),
                                       GeneralPreferenceFragment.PASSCODE_REQUEST_CODE);

            } else {

                Intent passIntent = new Intent(getActivity(),
                                               PasscodeLockScreenActivity.class);
                passIntent.putExtra(UxArgument.DISABLE_PASSCODE,
                                    UxArgument.DISABLE_PASSCODE);
                startActivityForResult(passIntent,
                                       GeneralPreferenceFragment.REQUEST_DISABLE_PASSCODE);
            }
        }

        //
        // Set Preference : use_color_in_reports
        //

        if (preference.getKey()
                      .equals(getString(R.string.key_use_account_color))) {

            getPreferenceManager().getSharedPreferences()
                                  .edit()
                                  .putBoolean(getString(R.string.key_use_account_color),
                                              Boolean.valueOf(newValue.toString()))
                                  .commit();
        }

        //
        // Preference : key_display_negative_signum_in_splits
        //

        if (preference.getKey()
                      .equals(getString(R.string.key_display_negative_signum_in_splits))) {

            // Store the new value of the Preference
            getPreferenceManager().getSharedPreferences()
                                  .edit()
                                  .putBoolean(getString(R.string.key_display_negative_signum_in_splits),
                                              Boolean.valueOf(newValue.toString()))
                                  .commit();
        }

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mEditor == null){
            mEditor = getPreferenceManager().getSharedPreferences().edit();
        }

        switch (requestCode) {
            case PASSCODE_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    mEditor.putString(UxArgument.PASSCODE, data.getStringExtra(UxArgument.PASSCODE));
                    mEditor.putBoolean(UxArgument.ENABLED_PASSCODE, true);
                    Toast.makeText(getActivity(), R.string.toast_passcode_set, Toast.LENGTH_SHORT).show();
                }
                if (resultCode == Activity.RESULT_CANCELED) {
                    mEditor.putBoolean(UxArgument.ENABLED_PASSCODE, false);
                    mCheckBoxPreference.setChecked(false);
                }
                break;
            case REQUEST_DISABLE_PASSCODE:
                boolean flag = resultCode != Activity.RESULT_OK;
                mEditor.putBoolean(UxArgument.ENABLED_PASSCODE, flag);
                mCheckBoxPreference.setChecked(flag);
                break;
            case REQUEST_CHANGE_PASSCODE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    mEditor.putString(UxArgument.PASSCODE, data.getStringExtra(UxArgument.PASSCODE));
                    mEditor.putBoolean(UxArgument.ENABLED_PASSCODE, true);
                    Toast.makeText(getActivity(), R.string.toast_passcode_set, Toast.LENGTH_SHORT).show();
                }
                break;
        }
        mEditor.commit();
    }

}
