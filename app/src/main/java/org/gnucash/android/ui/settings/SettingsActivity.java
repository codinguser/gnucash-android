/*
 * Copyright (c) 2012 Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2014 Yongxin Wang <fefe.wyx@gmail.com>
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
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.export.xml.GncXmlExporter;
import org.gnucash.android.importer.ImportAsyncTask;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.passcode.PasscodePreferenceActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Activity for displaying settings and information about the application
 * @author Ngewi Fet <ngewif@gmail.com>
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 * @author Yongxin Wang <fefe.wyx@gmail.com>
 */
public class SettingsActivity extends SherlockPreferenceActivity implements OnPreferenceChangeListener, Preference.OnPreferenceClickListener{

    /**
     * Allowed delay between two consecutive taps of a setting for it to be considered a double tap
     * Used on Android v2.3.3 or lower devices where dialogs cannot be instantiated easily in settings
     */
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
            addPreferencesFromResource(R.xml.fragment_passcode_preferences);
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

            pref = findPreference(getString(R.string.key_restore_backup));
            pref.setOnPreferenceClickListener(this);

            pref = findPreference(getString(R.string.key_change_passcode));
            pref.setOnPreferenceClickListener(this);

            pref = findPreference(getString(R.string.key_enable_passcode));
            pref.setOnPreferenceChangeListener(this);
            pref.setTitle(((CheckBoxPreference) pref).isChecked() ?
                    getString(R.string.title_passcode_enabled) : getString(R.string.title_passcode_disabled));
        }
	}

    @Override
    protected void onResume() {
        super.onResume();
        mDeleteAccountsClickCount = 0;
        mDeleteTransactionsClickCount = 0;
    }

    @Override
    protected void onPause() {
        super.onPause();
        GnuCashApplication.PASSCODE_SESSION_INIT_TIME = System.currentTimeMillis();
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
		if (preference.getKey().equals(getString(R.string.key_default_currency))){
			Money.DEFAULT_CURRENCY_CODE = newValue.toString();
            preference.setSummary(newValue.toString());
		} else if (preference.getKey().equals(getString(R.string.key_enable_passcode))) {
            if ((Boolean) newValue) {
                startActivityForResult(new Intent(this, PasscodePreferenceActivity.class),
                        PasscodePreferenceFragment.PASSCODE_REQUEST_CODE);
            } else {
                preference.setTitle(getString(R.string.title_passcode_disabled));
            }
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                    .edit()
                    .putBoolean(UxArgument.ENABLED_PASSCODE, (Boolean) newValue)
                    .commit();
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

        if (key.equals(getString(R.string.key_restore_backup))){
            importMostRecentBackup();
        }

        //since we cannot get a support FragmentManager in the SettingsActivity pre H0NEYCOMB,
        //we will just use 2 taps within 2 seconds as confirmation
        if (key.equals(getString(R.string.key_delete_all_accounts))){
            mDeleteAccountsClickCount++;
            if (mDeleteAccountsClickCount < 2){
                Toast.makeText(this, R.string.toast_tap_again_to_confirm_delete, Toast.LENGTH_SHORT).show();
            } else {
                GncXmlExporter.createBackup(); //create backup before deleting everything
                AccountsDbAdapter accountsDbAdapter = GnuCashApplication.getAccountsDbAdapter();
                accountsDbAdapter.deleteAllRecords();
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
                GncXmlExporter.createBackup(); //create backup before deleting everything
                List<Transaction> openingBalances = new ArrayList<Transaction>();
                boolean preserveOpeningBalances = GnuCashApplication.shouldSaveOpeningBalances(false);
                if (preserveOpeningBalances) {
                    AccountsDbAdapter accountsDbAdapter = GnuCashApplication.getAccountsDbAdapter();
                    openingBalances = accountsDbAdapter.getAllOpeningBalanceTransactions();
                }
                TransactionsDbAdapter transactionsDbAdapter = GnuCashApplication.getTransactionDbAdapter();
                transactionsDbAdapter.deleteAllRecords();

                if (preserveOpeningBalances) {
                    transactionsDbAdapter.bulkAddTransactions(openingBalances);
                }
                Toast.makeText(this, R.string.toast_all_transactions_deleted, Toast.LENGTH_LONG).show();
            }
            Timer timer = new Timer();
            timer.schedule(new ResetCounter(), DOUBLE_TAP_DELAY);
            return true;
        }

        if (key.equals(getString(R.string.key_change_passcode))){
            startActivityForResult(new Intent(this, PasscodePreferenceActivity.class),
                    PasscodePreferenceFragment.PASSCODE_REQUEST_CODE);
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

        startActivityForResult(chooser, AccountsActivity.REQUEST_PICK_ACCOUNTS_FILE);

    }

    public void importMostRecentBackup(){
        Log.i("Settings", "Importing GnuCash XML");
        File backupFile = Exporter.getMostRecentBackupFile();

        if (backupFile == null){
            Toast.makeText(this, R.string.toast_no_recent_backup, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            FileInputStream inputStream = new FileInputStream(backupFile);
            new ImportAsyncTask(this).execute(inputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED){
            if (requestCode == PasscodePreferenceFragment.PASSCODE_REQUEST_CODE) {
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                        .edit()
                        .putBoolean(UxArgument.ENABLED_PASSCODE, false)
                        .commit();
                ((CheckBoxPreference) findPreference(getString(R.string.key_enable_passcode))).setChecked(false);
            }
            return;
        }

        switch (requestCode){
            case AccountsActivity.REQUEST_PICK_ACCOUNTS_FILE:
                try {
                    InputStream accountInputStream = getContentResolver().openInputStream(data.getData());
                    new ImportAsyncTask(this).execute(accountInputStream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(this, R.string.toast_error_importing_accounts, Toast.LENGTH_SHORT).show();
                }
                break;
            case PasscodePreferenceFragment.PASSCODE_REQUEST_CODE:
                if (data!= null) {
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                            .edit()
                            .putString(UxArgument.PASSCODE, data.getStringExtra(UxArgument.PASSCODE))
                            .commit();
                    Toast.makeText(getApplicationContext(), R.string.toast_passcode_set, Toast.LENGTH_SHORT).show();
                    findPreference(getString(R.string.key_enable_passcode)).setTitle(getString(R.string.title_passcode_enabled));
                }
                break;
        }
    }
}
