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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.dropbox.core.android.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.export.xml.GncXmlExporter;
import org.gnucash.android.importer.ImportAsyncTask;
import org.gnucash.android.ui.settings.dialog.OwnCloudDialogFragment;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
 * Fragment for displaying general preferences
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class BackupPreferenceFragment extends PreferenceFragmentCompat implements
		Preference.OnPreferenceClickListener,
		Preference.OnPreferenceChangeListener,
		OnCloudStorageListener {

	/**
	 * Collects references to the UI elements and binds click listeners
	 */
	private static final int REQUEST_LINK_TO_DBX = 0x11;
	public static final int REQUEST_RESOLVE_CONNECTION = 0x12;

	/**
	 * Testing app key for DropBox API
	 */
	final static public String DROPBOX_APP_KEY      = "dhjh8ke9wf05948";

	/**
	 * Testing app secret for DropBox API
	 */
	final static public String DROPBOX_APP_SECRET   = "h2t9fphj3nr4wkw";

	/**
	 * String for tagging log statements
	 */
	public static final String LOG_TAG = "BackupPrefFragment";

	/**
	 * Client for Google Drive Sync
	 */
	private static GoogleApiClient mGoogleApiClient;

	private Preference mGoogleDrivePreference;

	@Override
	public void onCreatePreferences(Bundle bundle, String s) {
		addPreferencesFromResource(R.xml.fragment_backup_preferences);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.title_backup_prefs);

		mGoogleApiClient = getGoogleApiClient(getActivity(), this);
	}

	@Override
	public void onResume() {
		super.onResume();
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

		//if we are returning from DropBox authentication, save the key which was generated

		String keyDefaultEmail = getString(R.string.key_default_export_email);		
		Preference pref = findPreference(keyDefaultEmail);
		String defaultEmail = sharedPrefs.getString(keyDefaultEmail, null);
		if (defaultEmail != null && !defaultEmail.trim().isEmpty()){
			pref.setSummary(defaultEmail);			
		}
		pref.setOnPreferenceChangeListener(this);

        String keyDefaultExportFormat = getString(R.string.key_default_export_format);
        pref = findPreference(keyDefaultExportFormat);
        String defaultExportFormat = sharedPrefs.getString(keyDefaultExportFormat, null);
        if (defaultExportFormat != null && !defaultExportFormat.trim().isEmpty()){
            pref.setSummary(defaultExportFormat);
        }
        pref.setOnPreferenceChangeListener(this);

		pref = findPreference(getString(R.string.key_restore_backup));
		pref.setOnPreferenceClickListener(this);

		pref = findPreference(getString(R.string.key_create_backup));
		pref.setOnPreferenceClickListener(this);

		pref = findPreference(getString(R.string.key_dropbox_sync));
		pref.setOnPreferenceClickListener(this);
		toggleDropboxPreference(pref);

		pref = findPreference(getString(R.string.key_google_drive_sync));
		pref.setOnPreferenceClickListener(this);
		toggleGoogleDrivePreference(pref);
		mGoogleDrivePreference = pref;

		pref = findPreference(getString(R.string.key_owncloud_sync));
		pref.setOnPreferenceClickListener(this);
		toggleOwnCloudPreference(pref);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();

		if (key.equals(getString(R.string.key_restore_backup))){
			restoreBackup();
		}


		if (key.equals(getString(R.string.key_dropbox_sync))){
			toggleDropboxSync();
			toggleDropboxPreference(preference);
		}

		if (key.equals(getString(R.string.key_google_drive_sync))){
			toggleGoogleDriveSync();
			toggleGoogleDrivePreference(preference);
		}

		if (key.equals(getString(R.string.key_owncloud_sync))){
			toggleOwnCloudSync(preference);
			toggleOwnCloudPreference(preference);
		}

		if (key.equals(getString(R.string.key_create_backup))){
			boolean result = GncXmlExporter.createBackup();
			int msg = result ? R.string.toast_backup_successful : R.string.toast_backup_failed;
			Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
		}

		return false;
	}

	/**
     * Listens for changes to the preference and sets the preference summary to the new value
     * @param preference Preference which has been changed
     * @param newValue New value for the changed preference
     * @return <code>true</code> if handled, <code>false</code> otherwise
     */
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		preference.setSummary(newValue.toString());
		if (preference.getKey().equals(getString(R.string.key_default_currency))){
			GnuCashApplication.setDefaultCurrencyCode(newValue.toString());
		}
		
		if (preference.getKey().equals(getString(R.string.key_default_export_email))){
			String emailSetting = newValue.toString();
			if (emailSetting == null || emailSetting.trim().isEmpty()){
				preference.setSummary(R.string.summary_default_export_email);
			}					
		}

        if (preference.getKey().equals(getString(R.string.key_default_export_format))){
            String exportFormat = newValue.toString();
            if (exportFormat == null || exportFormat.trim().isEmpty()){
                preference.setSummary(R.string.summary_default_export_format);
            }
        }
		return true;
	}



	/**
	 * Toggles the checkbox of the DropBox Sync preference if a DropBox account is linked
	 * @param pref DropBox Sync preference
	 */
	public void toggleDropboxPreference(Preference pref) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		String accessToken = prefs.getString(getString(R.string.key_dropbox_access_token), null);
		((CheckBoxPreference)pref).setChecked(accessToken != null);
	}

	/**
	 * Toggles the checkbox of the ownCloud Sync preference if an ownCloud account is linked
	 * @param pref ownCloud Sync preference
	 */
	public void toggleOwnCloudPreference(Preference pref) {
		SharedPreferences mPrefs = getActivity().getSharedPreferences(getString(R.string.owncloud_pref), Context.MODE_PRIVATE);
		((CheckBoxPreference)pref).setChecked(mPrefs.getBoolean(getString(R.string.owncloud_sync), false));
	}

	/**
	 * Toggles the checkbox of the GoogleDrive Sync preference if a Google Drive account is linked
	 * @param pref Google Drive Sync preference
	 */
	public void toggleGoogleDrivePreference(Preference pref){
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		String appFolderId = sharedPreferences.getString(getString(R.string.key_google_drive_app_folder_id),null);
		((CheckBoxPreference)pref).setChecked(appFolderId != null);
	}

	/**
	 * Toggles the authorization state of a DropBox account.
	 * If a link exists, it is removed else DropBox authorization is started
	 */
	private void toggleDropboxSync() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		String accessToken = prefs.getString(getString(R.string.key_dropbox_access_token), null);
		if (accessToken == null){
			Auth.startOAuth2Authentication(getActivity(), getString(R.string.dropbox_app_key));
		} else {
			prefs.edit().remove(getString(R.string.key_dropbox_access_token)).apply();
		}
	}

	/**
	 * Toggles synchronization with Google Drive on or off
	 */
	private void toggleGoogleDriveSync(){
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		final String appFolderId = sharedPreferences.getString(getString(R.string.key_google_drive_app_folder_id), null);
		if (appFolderId != null){
			sharedPreferences.edit().remove(getString(R.string.key_google_drive_app_folder_id)).commit(); //commit (not apply) because we need it to be saved *now*
			mGoogleApiClient.disconnect();
			onGoogleDriveFolderForgot(appFolderId);
		} else {
			mGoogleApiClient.connect();
		}
	}

	/**
	 * Toggles synchronization with ownCloud on or off
	 */
	private void toggleOwnCloudSync(Preference pref){
		SharedPreferences mPrefs = getActivity().getSharedPreferences(getString(R.string.owncloud_pref), Context.MODE_PRIVATE);

		if (mPrefs.getBoolean(getString(R.string.owncloud_sync), false))
			mPrefs.edit().putBoolean(getString(R.string.owncloud_sync), false).apply();
		else {
			OwnCloudDialogFragment ocDialog = OwnCloudDialogFragment.newInstance(pref);
            ocDialog.show(getActivity().getSupportFragmentManager(), "owncloud_dialog");
		}
	}

	public static GoogleApiClient getGoogleApiClient(final Context context) {
		return getGoogleApiClient(context, null);
	}

	public static GoogleApiClient getGoogleApiClient(final Context context, OnCloudStorageListener listener) {
		final WeakReference<OnCloudStorageListener> weakListener = new WeakReference<>(listener);
		mGoogleApiClient = new GoogleApiClient.Builder(context)
				.addApi(Drive.API)
				.addScope(Drive.SCOPE_APPFOLDER)
				.addScope(Drive.SCOPE_FILE)
				.addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
					@Override
					public void onConnected(Bundle bundle) {
						final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
						String appFolderId = sharedPreferences.getString(context.getString(R.string.key_google_drive_app_folder_id), null);
						if (appFolderId == null) {
							MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
									.setTitle(context.getString(R.string.app_name)).build();
							// FIXME: check if "changeSet" folder already exists before just creating new folders every time.
							Drive.DriveApi.getRootFolder(mGoogleApiClient).createFolder(
									mGoogleApiClient, changeSet).setResultCallback(new ResultCallback<DriveFolder.DriveFolderResult>() {
								@Override
								public void onResult(DriveFolder.DriveFolderResult result) {
									if (!result.getStatus().isSuccess()) {
										Log.e(LOG_TAG, "Error creating the application folder: " + result.getStatus().getStatusMessage());
										return;
									}

									String folderId = result.getDriveFolder().getDriveId().toString();
									sharedPreferences
											.edit().putString(context.getString(R.string.key_google_drive_app_folder_id),
											folderId).commit(); //commit because we need it to be saved *now*
									OnCloudStorageListener listener = weakListener.get();
									if (listener != null) {
										listener.onGoogleDriveFolderCreated(folderId);
									}
								}
							});
						}
						Toast.makeText(context, R.string.toast_connected_to_google_drive, Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onConnectionSuspended(int i) {
						Toast.makeText(context, "Connection to Google Drive suspended!", Toast.LENGTH_LONG).show();//FIXME put text in strings.xml
					}
				})
				.addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
					@Override
					public void onConnectionFailed(ConnectionResult connectionResult) {
						Log.e(PreferenceActivity.class.getName(), "Connection to Google Drive failed");
						if (context instanceof Activity) {
							Activity activity = (Activity) context;
							if (connectionResult.hasResolution()) {
								try {
									Log.e(BackupPreferenceFragment.class.getName(), "Trying resolution of Google API connection failure");
									connectionResult.startResolutionForResult(activity, REQUEST_RESOLVE_CONNECTION);
								} catch (IntentSender.SendIntentException e) {
									Log.e(BackupPreferenceFragment.class.getName(), e.getMessage());
									Toast.makeText(context, R.string.toast_unable_to_connect_to_google_drive, Toast.LENGTH_LONG).show();
								}
							} else {
								GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), activity, 0).show();
							}
						}
					}
				})
				.build();
		return mGoogleApiClient;
	}

	/**
	 * Opens a dialog for a user to select a backup to restore and then restores the backup
	 */
	private void restoreBackup() {
		Log.i("Settings", "Opening GnuCash XML backups for restore");
		String bookUID = BooksDbAdapter.getInstance().getActiveBookUID();
		File[] backupFiles = new File(Exporter.getBackupFolderPath(bookUID)).listFiles();
		if (backupFiles == null || backupFiles.length == 0){
			android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActivity())
					.setTitle("No backups found") //FIXME put text in strings.xml
					.setMessage("There are no existing backup files to restore from") //FIXME put text in strings.xml
					.setNegativeButton(R.string.label_dismiss, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			builder.create().show();
			return;
		}

		Arrays.sort(backupFiles);
		List<File> backupFilesList = Arrays.asList(backupFiles);
		Collections.reverse(backupFilesList);
		final File[] sortedBackupFiles = (File[]) backupFilesList.toArray();

		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.select_dialog_singlechoice);
		final DateFormat dateFormatter = SimpleDateFormat.getDateTimeInstance();
		for (File backupFile : sortedBackupFiles) {
			long time = Exporter.getExportTime(backupFile.getName());
			if (time > 0)
				arrayAdapter.add(dateFormatter.format(new Date(time)));
			else //if no timestamp was found in the filename, just use the name
				arrayAdapter.add(backupFile.getName());
		}

		AlertDialog.Builder restoreDialogBuilder =  new AlertDialog.Builder(getActivity());
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
				new ImportAsyncTask(getActivity()).execute(Uri.fromFile(backupFile));
			}
		});

		restoreDialogBuilder.create().show();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode){

			case REQUEST_LINK_TO_DBX:
				Preference preference = findPreference(getString(R.string.key_dropbox_sync));
				if (preference == null) //if we are in a preference header fragment, this may return null
					break;
				toggleDropboxPreference(preference);
				break;

			case REQUEST_RESOLVE_CONNECTION:
				if (resultCode == Activity.RESULT_OK) {
					mGoogleApiClient.connect();
					Preference pref = findPreference(getString(R.string.key_google_drive_sync));
					if (pref == null) //if we are in a preference header fragment, this may return null
						break;
					toggleGoogleDrivePreference(pref);
				}
				break;
		}
	}

	@Override
	public void onGoogleDriveFolderCreated(String folderId) {
		if (mGoogleDrivePreference != null) {
			toggleGoogleDrivePreference(mGoogleDrivePreference);
		}
	}

	@Override
	public void onGoogleDriveFolderForgot(String folderId) {
		if (mGoogleDrivePreference != null) {
			toggleGoogleDrivePreference(mGoogleDrivePreference);
		}
	}

	public static void connectToGoogle() {
		if (mGoogleApiClient != null) {
			mGoogleApiClient.connect();
		}
	}
}
