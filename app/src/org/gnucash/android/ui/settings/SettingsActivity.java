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

import java.util.List;

import org.gnucash.android.R;
import org.gnucash.android.data.Money;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

/**
 * Activity for displaying settings and information about the application
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class SettingsActivity extends SherlockPreferenceActivity implements OnPreferenceChangeListener{

	/**
	 * Constructs the headers to display in the header list when the Settings activity is first opened
	 * Only available on Honeycomb and above
	 */
	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		
		//retrieve version from Manifest and set it
		String version = null;
		try {
			version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			Editor editor = sharedPrefs.edit();
			editor.putString(getString(R.string.key_build_version), version);
			editor.commit();
		} catch (NameNotFoundException e) {
			Log.e("SettingsActivity", "Could not set version preference");
			e.printStackTrace();
		}
				
		ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(R.string.title_settings);
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
			addPreferencesFromResource(R.xml.fragment_general_preferences);
			addPreferencesFromResource(R.xml.fragment_transaction_preferences);
			addPreferencesFromResource(R.xml.fragment_about_preferences);
			setDefaultCurrencyListener();
			SharedPreferences manager = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			String versionName = manager.getString(getString(R.string.key_build_version), "");
			Preference pref = findPreference(getString(R.string.key_build_version));
			pref.setSummary(versionName);
		}		
	}
		
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:		
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				android.app.FragmentManager fm = getFragmentManager();
				if (fm.getBackStackEntryCount() > 0) {
					fm.popBackStack();
				} else {
					finish();
				}
			} else {
				finish();
			}
	        return true;

		default:
			return false;
		}
	}
	
	@Override
 	public boolean onPreferenceChange(Preference preference, Object newValue) {
		preference.setSummary(newValue.toString());
		if (preference.getKey().equals(getString(R.string.key_default_currency))){
			Money.DEFAULT_CURRENCY_CODE = newValue.toString();
		}
		return true;
	}
	
	private void setDefaultCurrencyListener() {
		SharedPreferences manager = PreferenceManager.getDefaultSharedPreferences(this);
		String defaultCurrency = manager.getString(getString(R.string.key_default_currency), Money.DEFAULT_CURRENCY_CODE);
		@SuppressWarnings("deprecation")
		Preference pref = findPreference(getString(R.string.key_default_currency));
		pref.setSummary(defaultCurrency);
		pref.setOnPreferenceChangeListener(this);
	}

}
