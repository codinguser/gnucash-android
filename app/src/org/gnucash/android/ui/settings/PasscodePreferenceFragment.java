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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;

import org.gnucash.android.R;
import org.gnucash.android.ui.UxArgument;

/**
 * Fragment for configuring passcode to the application
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class PasscodePreferenceFragment extends PreferenceFragment {

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

        final SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(getActivity().getApplicationContext()).edit();

        final String keyEnablePasscode = getString(R.string.key_enable_passcode);
        findPreference(keyEnablePasscode)
                .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        editor.putBoolean(UxArgument.ENABLED_PASSCODE, (Boolean) newValue);
                        editor.commit();
                        return true;
                    }
                });
        findPreference(getString(R.string.key_change_passcode))
                .setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        editor.putString(UxArgument.PASSCODE, newValue.toString());
                        editor.commit();
                        return true;
                    }
                });

    }

}
