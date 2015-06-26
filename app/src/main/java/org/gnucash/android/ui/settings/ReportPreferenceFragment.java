/*
 * Copyright (c) 2015 Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
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
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;

import org.gnucash.android.R;

/**
 * Report settings fragment inside the Settings activity
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
@TargetApi(11)
public class ReportPreferenceFragment extends PreferenceFragment implements OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.fragment_report_preferences);
        ActionBar actionBar = ((SherlockPreferenceActivity) getActivity()).getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.title_report_prefs);

        findPreference(getString(R.string.key_use_account_color)).setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals(getString(R.string.key_use_account_color))) {
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putBoolean(getString(R.string.key_use_account_color), Boolean.valueOf(newValue.toString()))
                    .commit();
        }
        return true;
    }

}
