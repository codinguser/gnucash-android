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
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

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
		
		ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(R.string.title_settings);
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
			addPreferencesFromResource(R.xml.fragment_general_preferences);
			addPreferencesFromResource(R.xml.fragment_about_preferences);
			setDefaultCurrencyListener();
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
	
	/**
	 * Fragment for displaying general preferences
	 * @author Ngewi Fet <ngewif@gmail.com>
	 *
	 */
	public static class GeneralPreferenceFragment extends PreferenceFragment implements OnPreferenceChangeListener{
		
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
		}

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			preference.setSummary(newValue.toString());
			if (preference.getKey().equals(getString(R.string.key_default_currency))){
				Money.DEFAULT_CURRENCY_CODE = newValue.toString();
			}
			return true;
		}

	}
	
	/**
	 * Fragment for displaying information about the application
	 * @author Ngewi Fet <ngewif@gmail.com>
	 *
	 */
	public static class AboutPreferenceFragment extends PreferenceFragment{
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.fragment_about_preferences);
			ActionBar actionBar = ((SherlockPreferenceActivity) getActivity()).getSupportActionBar();
			actionBar.setHomeButtonEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(R.string.title_about_gnucash);
						
		}		
	}
}
