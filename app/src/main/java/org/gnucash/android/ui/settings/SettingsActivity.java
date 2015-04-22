/*
 * Copyright (c) 2012 - 2015 Ngewi Fet <ngewif@gmail.com>
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
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
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.dropbox.sync.android.DbxAccountManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseSchema;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
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

    public static final String LOG_TAG = "SettingsActivity";
    /**
     * Allowed delay between two consecutive taps of a setting for it to be considered a double tap
     * Used on Android v2.3.3 or lower devices where dialogs cannot be instantiated easily in settings
     */
    public static final int DOUBLE_TAP_DELAY = 2000;
    final static public String DROPBOX_APP_KEY      = "dhjh8ke9wf05948";
    final static public String DROPBOX_APP_SECRET   = "h2t9fphj3nr4wkw";
    /**
     * Collects references to the UI elements and binds click listeners
     */
    public static final int REQUEST_LINK_TO_DBX = 0x11;
    public static final int REQUEST_RESOLVE_CONNECTION = 0x12;

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
    private DbxAccountManager mDbxAccountManager;
    /**
     * Client for Google Drive Sync
     */
    static GoogleApiClient mGoogleApiClient;

    /**
	 * Constructs the headers to display in the header list when the Settings activity is first opened
	 * Only available on Honeycomb and above
	 */
    @TargetApi(11)
	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

        mDbxAccountManager = DbxAccountManager.getInstance(getApplicationContext(),
                DROPBOX_APP_KEY, DROPBOX_APP_SECRET);

        mGoogleApiClient = getGoogleApiClient(this);

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
            addPreferencesFromResource(R.xml.fragment_account_preferences);
			addPreferencesFromResource(R.xml.fragment_transaction_preferences);
            addPreferencesFromResource(R.xml.fragment_backup_preferences);
            addPreferencesFromResource(R.xml.fragment_passcode_preferences);
			addPreferencesFromResource(R.xml.fragment_about_preferences);
			setDefaultCurrencyListener();
			SharedPreferences manager = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			String versionName = manager.getString(getString(R.string.key_build_version), "");
			Preference pref = findPreference(getString(R.string.key_build_version));
			pref.setSummary(versionName);

            pref = findPreference(getString(R.string.key_import_accounts));
            pref.setOnPreferenceClickListener(this);

            pref = findPreference(getString(R.string.key_restore_backup));
            pref.setOnPreferenceClickListener(this);

            pref = findPreference(getString(R.string.key_use_double_entry));
            pref.setOnPreferenceChangeListener(this);

            pref = findPreference(getString(R.string.key_delete_all_transactions));
            pref.setOnPreferenceClickListener(this);

            pref = findPreference(getString(R.string.key_delete_all_accounts));
            pref.setOnPreferenceClickListener(this);

            pref = findPreference(getString(R.string.key_build_version));
            pref.setOnPreferenceClickListener(this);

            pref = findPreference(getString(R.string.key_change_passcode));
            pref.setOnPreferenceClickListener(this);

            pref = findPreference(getString(R.string.key_dropbox_sync));
            toggleDropboxPreference(pref);
            pref.setOnPreferenceClickListener(this);

            pref = findPreference(getString(R.string.key_google_drive_sync));
            pref.setOnPreferenceClickListener(this);
            toggleGoogleDrivePreference(pref);

            pref = findPreference(getString(R.string.key_create_backup));
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

    @TargetApi(11)
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
        } else if (preference.getKey().equals(getString(R.string.key_use_double_entry))){
            setImbalanceAccountsHidden((Boolean) newValue);
        }

		return true;
	}

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return BackupPreferenceFragment.class.getName().equals(fragmentName)
                || AccountPreferencesFragment.class.getName().equals(fragmentName)
                || PasscodePreferenceFragment.class.getName().equals(fragmentName)
                || TransactionsPreferenceFragment.class.getName().equals(fragmentName)
                || AboutPreferenceFragment.class.getName().equals(fragmentName);
    }

    public void setImbalanceAccountsHidden(boolean useDoubleEntry) {
        String isHidden = useDoubleEntry ? "0" : "1";
        AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();
        List<Currency> currencies = accountsDbAdapter.getCurrencies();
        for (Currency currency : currencies) {
            String uid = accountsDbAdapter.getImbalanceAccountUID(currency);
            if (uid != null){
                accountsDbAdapter.updateRecord(uid, DatabaseSchema.AccountEntry.COLUMN_HIDDEN, isHidden);
            }
        }
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

        if (key.equals(getString(R.string.key_restore_backup))){
            restoreBackup();
        }

        if (key.equals(getString(R.string.key_build_version))){
            AccountsActivity.showWhatsNewDialog(this);
            return true;
        }

        if (key.equals(getString(R.string.key_dropbox_sync))){
            toggleDropboxSync();
            toggleDropboxPreference(preference);
        }

        if (key.equals(getString(R.string.key_google_drive_sync))){
            toggleGoogleDriveSync();
            toggleGoogleDrivePreference(preference);
        }

        if (key.equals(getString(R.string.key_create_backup))){
            boolean result = GncXmlExporter.createBackup();
            int msg = result ? R.string.toast_backup_successful : R.string.toast_backup_failed;
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }

        //since we cannot get a support FragmentManager in the SettingsActivity pre H0NEYCOMB,
        //we will just use 2 taps within 2 seconds as confirmation
        if (key.equals(getString(R.string.key_delete_all_accounts))){
            mDeleteAccountsClickCount++;
            if (mDeleteAccountsClickCount < 2){
                Toast.makeText(this, R.string.toast_tap_again_to_confirm_delete, Toast.LENGTH_SHORT).show();
            } else {
                GncXmlExporter.createBackup(); //create backup before deleting everything
                AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();
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
                    AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();
                    openingBalances = accountsDbAdapter.getAllOpeningBalanceTransactions();
                }
                TransactionsDbAdapter transactionsDbAdapter = TransactionsDbAdapter.getInstance();
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
     * Toggles the authorization state of a DropBox account.
     * If a link exists, it is removed else DropBox authorization is started
     */
    private void toggleDropboxSync() {
        if (mDbxAccountManager.hasLinkedAccount()){
            mDbxAccountManager.unlink();
        } else {
            mDbxAccountManager.startLink(this, REQUEST_LINK_TO_DBX);
        }
    }

    /**
     * Toggles synchronization with Google Drive on or off
     */
    private void toggleGoogleDriveSync(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String appFolderId = sharedPreferences.getString(getString(R.string.key_google_drive_app_folder_id), null);
        if (appFolderId != null){
            sharedPreferences.edit().remove(getString(R.string.key_google_drive_app_folder_id)).commit(); //commit (not apply) because we need it to be saved *now*
        } else {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Toggles the checkbox of the DropBox Sync preference if a DropBox account is linked
     * @param pref DropBox Sync preference
     */
    public void toggleDropboxPreference(Preference pref) {
        ((CheckBoxPreference)pref).setChecked(mDbxAccountManager.hasLinkedAccount());
    }

    /**
     * Toggles the checkbox of the GoogleDrive Sync preference if a Google Drive account is linked
     * @param pref Google Drive Sync preference
     */
    public void toggleGoogleDrivePreference(Preference pref){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String appFolderId = sharedPreferences.getString(getString(R.string.key_google_drive_app_folder_id),null);
        ((CheckBoxPreference)pref).setChecked(appFolderId != null);
    }


    public static GoogleApiClient getGoogleApiClient(final Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_APPFOLDER)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                        String appFolderId = sharedPreferences.getString(context.getString(R.string.key_google_drive_app_folder_id), null);
                        if (appFolderId == null) {
                            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                    .setTitle(context.getString(R.string.app_name)).build();
                            Drive.DriveApi.getRootFolder(mGoogleApiClient).createFolder(
                                    mGoogleApiClient, changeSet).setResultCallback(new ResultCallback<DriveFolder.DriveFolderResult>() {
                                @Override
                                public void onResult(DriveFolder.DriveFolderResult result) {
                                    if (!result.getStatus().isSuccess()) {
                                        Log.e(LOG_TAG, "Error creating the application folder");
                                        return;
                                    }

                                    String folderId = result.getDriveFolder().getDriveId().toString();
                                    PreferenceManager.getDefaultSharedPreferences(context)
                                            .edit().putString(context.getString(R.string.key_google_drive_app_folder_id),
                                            folderId).commit(); //commit because we need it to be saved *now*
                                }
                            });

                        }
                        Toast.makeText(context, "Connected to Google Drive", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Toast.makeText(context, "Connection to Google Drive suspended!", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.e(SettingsActivity.class.getName(), "Connection to Google Drive failed");
                        if (connectionResult.hasResolution() && context instanceof Activity) {
                            try {
                                Log.e(SettingsActivity.class.getName(), "Trying resolution of Google API connection failure");
                                connectionResult.startResolutionForResult((Activity) context, REQUEST_RESOLVE_CONNECTION);
                            } catch (IntentSender.SendIntentException e) {
                                Log.e(SettingsActivity.class.getName(), e.getMessage());
                                Toast.makeText(context, "Unable to link to Google Drive", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            if (context instanceof Activity)
                                GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), (Activity) context, 0).show();
                        }
                    }
                })
                .build();
    }

    /**
     * Resets the tap counter for preferences which need to be double-tapped
     */
    private class ResetCounter extends TimerTask {

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
        Intent chooser = Intent.createChooser(pickIntent, getString(R.string.title_select_gnucash_xml_file));

        try {
            startActivityForResult(chooser, AccountsActivity.REQUEST_PICK_ACCOUNTS_FILE);
        } catch (ActivityNotFoundException ex){
            Toast.makeText(this, R.string.toast_install_file_manager, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Opens a dialog for a user to select a backup to restore and then restores the backup
     */
    public void restoreBackup() {
        Log.i("Settings", "Opening GnuCash XML backups for restore");
        File[] backupFiles = new File(Exporter.BACKUP_FOLDER_PATH).listFiles();
        Arrays.sort(backupFiles);
        List<File> backupFilesList = Arrays.asList(backupFiles);
        Collections.reverse(backupFilesList);
        final File[] sortedBackupFiles = (File[]) backupFilesList.toArray();

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_singlechoice);
        final DateFormat dateFormatter = SimpleDateFormat.getDateTimeInstance();
        for (File backupFile : sortedBackupFiles) {
            long time = Exporter.getExportTime(backupFile.getName());
            arrayAdapter.add(dateFormatter.format(new Date(time)));
        }

        AlertDialog.Builder restoreDialogBuilder =  new AlertDialog.Builder(this);
        restoreDialogBuilder.setTitle(R.string.title_select_backup_to_restore);
        restoreDialogBuilder.setNegativeButton(R.string.alert_dialog_cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        restoreDialogBuilder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                File backupFile = sortedBackupFiles[which];

                try {
                    FileInputStream inputStream = new FileInputStream(backupFile);
                    new ImportAsyncTask(SettingsActivity.this).execute(inputStream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.e(LOG_TAG, "Error restoring backup: " + backupFile.getName());
                    Toast.makeText(SettingsActivity.this, R.string.toast_error_importing_accounts, Toast.LENGTH_LONG).show();
                }
            }
        });

        restoreDialogBuilder.create().show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            if (requestCode == PasscodePreferenceFragment.PASSCODE_REQUEST_CODE) {
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                        .edit()
                        .putBoolean(UxArgument.ENABLED_PASSCODE, false)
                        .commit();
                ((CheckBoxPreference) findPreference(getString(R.string.key_enable_passcode))).setChecked(false);
            }
            return;
        }

        switch (requestCode) {
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
                if (data != null) {
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                            .edit()
                            .putString(UxArgument.PASSCODE, data.getStringExtra(UxArgument.PASSCODE))
                            .commit();
                    Toast.makeText(getApplicationContext(), R.string.toast_passcode_set, Toast.LENGTH_SHORT).show();
                    findPreference(getString(R.string.key_enable_passcode)).setTitle(getString(R.string.title_passcode_enabled));
                }
                break;

            case REQUEST_LINK_TO_DBX:
                Preference preference = findPreference(getString(R.string.key_dropbox_sync));
                if (preference == null) //if we are in a preference header fragment, this may return null
                    break;
                toggleDropboxPreference(preference);
                break;

            case REQUEST_RESOLVE_CONNECTION:
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                    Preference pref = findPreference(getString(R.string.key_dropbox_sync));
                    if (pref == null) //if we are in a preference header fragment, this may return null
                        break;
                    toggleDropboxPreference(pref);
                }
                break;
        }
    }
}
