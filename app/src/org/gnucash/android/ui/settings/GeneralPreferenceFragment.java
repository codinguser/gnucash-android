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

import org.gnucash.android.R;
import org.gnucash.android.data.Money;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;


/**
 * Fragment for displaying general preferences
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class GeneralPreferenceFragment extends PreferenceFragment implements OnPreferenceChangeListener{
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.fragment_general_preferences);
		ActionBar actionBar = ((SherlockPreferenceActivity) getActivity()).getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.title_general_prefs);
		
	}	
	
	@Override
	public void onResume() {
		super.onResume();
		SharedPreferences manager = PreferenceManager.getDefaultSharedPreferences(getActivity());
		String defaultCurrency = manager.getString(getString(R.string.key_default_currency), Money.DEFAULT_CURRENCY_CODE);
		Preference pref = findPreference(getString(R.string.key_default_currency));
		pref.setSummary(defaultCurrency);
		pref.setOnPreferenceChangeListener(this);
		
		String keyDefaultEmail = getString(R.string.key_default_export_email);		
		pref = findPreference(keyDefaultEmail);
		String defaultEmail = manager.getString(keyDefaultEmail, null);
		if (defaultEmail != null && !defaultEmail.trim().isEmpty()){
			pref.setSummary(defaultEmail);			
		}
		pref.setOnPreferenceChangeListener(this);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		preference.setSummary(newValue.toString());
		if (preference.getKey().equals(getString(R.string.key_default_currency))){
			Money.DEFAULT_CURRENCY_CODE = newValue.toString();
		}
		
		if (preference.getKey().equals(getString(R.string.key_default_export_email))){
			String emailSetting = newValue.toString();
			if (emailSetting == null || emailSetting.trim().isEmpty()){
				preference.setSummary(R.string.summary_default_export_email);
			}					
		}
		return true;
	}

}
