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

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

/**
 * Activity for displaying settings and information about the application
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class SettingsActivity extends SherlockPreferenceActivity{

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
		
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
			addPreferencesFromResource(R.xml.fragment_general_preferences);
			addPreferencesFromResource(R.xml.fragment_about_preferences);
		}
	}
		
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
	        android.app.FragmentManager fm = getFragmentManager();
	        if (fm.getBackStackEntryCount() > 0) {
	            fm.popBackStack();
	        } else {
	        	finish();
	        }
	        return true;

		default:
			return false;
		}
	}
	
	/**
	 * Fragment for displaying general preferences
	 * @author Ngewi Fet <ngewif@gmail.com>
	 *
	 */
	public static class GeneralPreferenceFragment extends PreferenceFragment{
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.fragment_general_preferences);
			ActionBar actionBar = ((SherlockPreferenceActivity) getActivity()).getSupportActionBar();
			actionBar.setHomeButtonEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}		
	}
	
	/**
	 * Fragment for displaying information about the application
	 * @author Ngewi
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
						
		}		
	}
}
