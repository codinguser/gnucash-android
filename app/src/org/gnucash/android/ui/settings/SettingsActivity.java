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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import org.gnucash.android.R;
import org.gnucash.android.data.Money;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.ui.accounts.AccountsActivity;
import org.gnucash.android.ui.accounts.AccountsListFragment;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Activity for displaying settings and information about the application
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class SettingsActivity extends SherlockPreferenceActivity implements OnPreferenceChangeListener, Preference.OnPreferenceClickListener{

    public static final int DOUBLE_TAP_DELAY = 2000;
    /**
     * Counts the number of times the preference for deleting all accounts has been clicked.
     * It is reset every time the SettingsActivity is resumed.
     * Only useful on devices with API level < 11
     */
    private int mDeleteAccountsClickCount;

    /**
     * Counts the number of times the preference for deleting all transactions has been clicked.
     * It is reset every time the SettingsActivity is resumed.
     * Only useful on devices with API level < 11
     */
    private int mDeleteTransactionsClickCount;

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
            addPreferencesFromResource(R.xml.fragment_account_preferences);
			addPreferencesFromResource(R.xml.fragment_transaction_preferences);
			addPreferencesFromResource(R.xml.fragment_about_preferences);
			setDefaultCurrencyListener();
			SharedPreferences manager = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			String versionName = manager.getString(getString(R.string.key_build_version), "");
			Preference pref = findPreference(getString(R.string.key_build_version));
			pref.setSummary(versionName);

            pref = findPreference(getString(R.string.key_import_accounts));
            pref.setOnPreferenceClickListener(this);

            pref = findPreference(getString(R.string.key_delete_all_transactions));
            pref.setOnPreferenceClickListener(this);

            pref = findPreference(getString(R.string.key_delete_all_accounts));
            pref.setOnPreferenceClickListener(this);

            pref = findPreference(getString(R.string.key_build_version));
            pref.setOnPreferenceClickListener(this);

            pref = findPreference(getString(R.string.key_create_default_accounts));
            pref.setOnPreferenceClickListener(this);
		}
	}

    @Override
    protected void onResume() {
        super.onResume();
        mDeleteAccountsClickCount = 0;
        mDeleteTransactionsClickCount = 0;
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

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (key.equals(getString(R.string.key_import_accounts))){
            importAccounts();
            return true;
        }

        if (key.equals(getString(R.string.key_build_version))){
            AccountsActivity.showWhatsNewDialog(this);
            return true;
        }

        //since we cannot get a support FragmentManager in the SettingsActivity pre H0NEYCOMB,
        //we will just use 2 taps within 2 seconds as confirmation
        if (key.equals(getString(R.string.key_delete_all_accounts))){
            mDeleteAccountsClickCount++;
            if (mDeleteAccountsClickCount < 2){
                Toast.makeText(this, R.string.toast_tap_again_to_confirm_delete, Toast.LENGTH_SHORT).show();
            } else {
                AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(this);
                accountsDbAdapter.deleteAllRecords();
                accountsDbAdapter.close();
                Toast.makeText(this, R.string.toast_all_accounts_deleted, Toast.LENGTH_LONG).show();
            }
            Timer timer = new Timer();
            timer.schedule(new ResetCounter(), DOUBLE_TAP_DELAY);
            return true;
        }

        if (key.equals(getString(R.string.key_delete_all_transactions))){
            mDeleteTransactionsClickCount++;
            if (mDeleteTransactionsClickCount < 2){
                Toast.makeText(this, R.string.toast_tap_again_to_confirm_delete, Toast.LENGTH_SHORT).show();
            } else {
                TransactionsDbAdapter transactionsDbAdapter = new TransactionsDbAdapter(this);
                transactionsDbAdapter.deleteAllRecords();
                transactionsDbAdapter.close();
                Toast.makeText(this, R.string.toast_all_transactions_deleted, Toast.LENGTH_LONG).show();
            }
            Timer timer = new Timer();
            timer.schedule(new ResetCounter(), DOUBLE_TAP_DELAY);
            return true;
        }

        return false;
    }

    /**
     * Resets the tap counter for preferences which need to be double-tapped
     */
    private class ResetCounter extends TimerTask{

        @Override
        public void run() {
            mDeleteAccountsClickCount = 0;
            mDeleteTransactionsClickCount = 0;
        }
    }

    /**
     * Starts a request to pick a file to import into GnuCash
     */
    public void importAccounts() {
        Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT);
        pickIntent.setType("application/*");
        Intent chooser = Intent.createChooser(pickIntent, "Select GnuCash account file");

        startActivityForResult(chooser, AccountsListFragment.REQUEST_PICK_ACCOUNTS_FILE);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED){
            return;
        }

        switch (requestCode){
            case AccountsListFragment.REQUEST_PICK_ACCOUNTS_FILE:
                try {
                    InputStream accountInputStream = getContentResolver().openInputStream(data.getData());
                    new AccountsActivity.AccountImporterTask(this).execute(accountInputStream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(this, R.string.toast_error_importing_accounts, Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }
}
