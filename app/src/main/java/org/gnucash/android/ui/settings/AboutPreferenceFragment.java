/*
 * Copyright (c) 2012 Ngewi Fet <ngewif@gmail.com>
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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.R;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.ui.account.AccountsActivity;


/**
 * Fragment for displaying information about the application
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class AboutPreferenceFragment extends PreferenceFragmentCompat{

	@Override
	public void onCreatePreferences(Bundle bundle, String s) {
		addPreferencesFromResource(R.xml.fragment_about_preferences);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.title_about_gnucash);
					
	}		
	
	@Override
	public void onResume() {
		super.onResume();
		Preference pref = findPreference(getString(R.string.key_about_gnucash));
		if (BuildConfig.FLAVOR.equals("development")){
			pref.setSummary(pref.getSummary() + " - Built: " + BuildConfig.BUILD_TIME);
		}
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AccountsActivity.showWhatsNewDialog(getActivity());
                return true;
            }
        });
	}
}