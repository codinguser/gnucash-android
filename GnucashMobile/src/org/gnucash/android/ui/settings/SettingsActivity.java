package org.gnucash.android.ui.settings;

import java.util.List;

import org.gnucash.android.R;

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

public class SettingsActivity extends SherlockPreferenceActivity{

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
