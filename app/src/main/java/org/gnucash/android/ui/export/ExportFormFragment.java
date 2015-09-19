/*
 * Copyright (c) 2012-2013 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.ui.export;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.doomonafireball.betterpickers.recurrencepicker.EventRecurrence;
import com.doomonafireball.betterpickers.recurrencepicker.EventRecurrenceFormatter;
import com.doomonafireball.betterpickers.recurrencepicker.RecurrencePickerDialog;
import com.dropbox.sync.android.DbxAccountManager;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.ScheduledActionDbAdapter;
import org.gnucash.android.export.ExportAsyncTask;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.settings.SettingsActivity;
import org.gnucash.android.ui.util.RecurrenceParser;

import java.util.List;
import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Dialog fragment for exporting account information as OFX files.
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ExportFormFragment extends Fragment implements RecurrencePickerDialog.OnRecurrenceSetListener {
		
	/**
	 * Spinner for selecting destination for the exported file.
	 * The destination could either be SD card, or another application which
	 * accepts files, like Google Drive.
	 */
	@Bind(R.id.spinner_export_destination) Spinner mDestinationSpinner;
	
	/**
	 * Checkbox indicating that all transactions should be exported,
	 * regardless of whether they have been exported previously or not
	 */
	@Bind(R.id.checkbox_export_all) CheckBox mExportAllCheckBox;
	
	/**
	 * Checkbox for deleting all transactions after exporting them
	 */
	@Bind(R.id.checkbox_post_export_delete) CheckBox mDeleteAllCheckBox;

    /**
     * Text view for showing warnings based on chosen export format
     */
    @Bind(R.id.export_warning) TextView mExportWarningTextView;

	/**
	 * Recurrence text view
	 */
	@Bind(R.id.input_recurrence) TextView mRecurrenceTextView;

	/**
	 * Event recurrence options
	 */
	EventRecurrence mEventRecurrence = new EventRecurrence();

	/**
	 * Recurrence rule
	 */
	String mRecurrenceRule;

	/**
	 * Tag for logging
	 */
	private static final String TAG = "ExportFormFragment";

	/**
	 * Export format
	 */
    private ExportFormat mExportFormat = ExportFormat.QIF;

	private ExportParams.ExportTarget mExportTarget = ExportParams.ExportTarget.SD_CARD;


    public void onRadioButtonClicked(View view){
        switch (view.getId()){
            case R.id.radio_ofx_format:
                mExportFormat = ExportFormat.OFX;
                if (GnuCashApplication.isDoubleEntryEnabled()){
                    mExportWarningTextView.setText(getActivity().getString(R.string.export_warning_ofx));
                    mExportWarningTextView.setVisibility(View.VISIBLE);
                } else {
                    mExportWarningTextView.setVisibility(View.GONE);
                }
                break;

            case R.id.radio_qif_format:
                mExportFormat = ExportFormat.QIF;
                //TODO: Also check that there exist transactions with multiple currencies before displaying warning
                if (GnuCashApplication.isDoubleEntryEnabled()) {
                    mExportWarningTextView.setText(getActivity().getString(R.string.export_warning_qif));
                    mExportWarningTextView.setVisibility(View.VISIBLE);
                } else {
                    mExportWarningTextView.setVisibility(View.GONE);
                }
				break;

			case R.id.radio_xml_format:
				mExportFormat = ExportFormat.XML;
				mExportWarningTextView.setText(R.string.export_warning_xml);
				break;
        }
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_export_form, container, false);
		ButterKnife.bind(this, view);
		return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.default_save_actions, menu);
		MenuItem menuItem = menu.findItem(R.id.menu_save);
		menuItem.setTitle(R.string.btn_export);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
			case R.id.menu_save:
				startExport();
				return true;

			case android.R.id.home:
				getActivity().finish();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {		
		super.onActivityCreated(savedInstanceState);
        bindViews();
		ActionBar supportActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
		assert supportActionBar != null;
		supportActionBar.setTitle(R.string.title_export_dialog);
		setHasOptionsMenu(true);

		getSDWritePermission();
	}

	/**
	 * Get permission for WRITING SD card for Android Marshmallow and above
	 */
	private void getSDWritePermission(){
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
					!= PackageManager.PERMISSION_GRANTED) {
				getActivity().requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
						Manifest.permission.READ_EXTERNAL_STORAGE}, AccountsActivity.PERMISSION_REQUEST_WRITE_SD_CARD);
			}
		}
	}

	/**
	 * Starts the export of transactions with the specified parameters
	 */
	private void startExport(){
		ExportParams exportParameters = new ExportParams(mExportFormat);
		exportParameters.setExportAllTransactions(mExportAllCheckBox.isChecked());
		exportParameters.setExportTarget(mExportTarget);
		exportParameters.setDeleteTransactionsAfterExport(mDeleteAllCheckBox.isChecked());

		List<ScheduledAction> scheduledActions = RecurrenceParser.parse(mEventRecurrence,
				ScheduledAction.ActionType.BACKUP);
		for (ScheduledAction scheduledAction : scheduledActions) {
			scheduledAction.setTag(exportParameters.toCsv());
			scheduledAction.setActionUID(UUID.randomUUID().toString().replaceAll("-", ""));
			ScheduledActionDbAdapter.getInstance().addRecord(scheduledAction);
		}

		Log.i(TAG, "Commencing async export of transactions");
		new ExportAsyncTask(getActivity()).execute(exportParameters);

		// finish the activity will cause the progress dialog to be leaked
		// which would throw an exception
		//getActivity().finish();
	}

	private void bindViews(){
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
		        R.array.export_destinations, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);		
		mDestinationSpinner.setAdapter(adapter);
		mDestinationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				View recurrenceOptionsView = getView().findViewById(R.id.recurrence_options);
				switch (position) {
					case 0:
						mExportTarget = ExportParams.ExportTarget.SD_CARD;
						recurrenceOptionsView.setVisibility(View.VISIBLE);
						break;
					case 1:
						recurrenceOptionsView.setVisibility(View.VISIBLE);
						mExportTarget = ExportParams.ExportTarget.DROPBOX;
						String dropboxAppKey = getString(R.string.dropbox_app_key, SettingsActivity.DROPBOX_APP_KEY);
						String dropboxAppSecret = getString(R.string.dropbox_app_secret, SettingsActivity.DROPBOX_APP_SECRET);
						DbxAccountManager mDbxAccountManager = DbxAccountManager.getInstance(getActivity().getApplicationContext(),
								dropboxAppKey, dropboxAppSecret);
						if (!mDbxAccountManager.hasLinkedAccount()) {
							mDbxAccountManager.startLink(getActivity(), 0);
						}
						break;
					case 2:
						recurrenceOptionsView.setVisibility(View.VISIBLE);
						mExportTarget = ExportParams.ExportTarget.GOOGLE_DRIVE;
						SettingsActivity.mGoogleApiClient = SettingsActivity.getGoogleApiClient(getActivity());
						SettingsActivity.mGoogleApiClient.connect();
						break;
					case 3:
						mExportTarget = ExportParams.ExportTarget.SHARING;
						recurrenceOptionsView.setVisibility(View.GONE);
						break;

					default:
						mExportTarget = ExportParams.ExportTarget.SD_CARD;
						break;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mExportAllCheckBox.setChecked(sharedPrefs.getBoolean(getString(R.string.key_export_all_transactions), false));
		
		mDeleteAllCheckBox.setChecked(sharedPrefs.getBoolean(getString(R.string.key_delete_transactions_after_export), false));

		mRecurrenceTextView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				FragmentManager fm = getActivity().getSupportFragmentManager();
				Bundle b = new Bundle();
				Time t = new Time();
				t.setToNow();
				b.putLong(RecurrencePickerDialog.BUNDLE_START_TIME_MILLIS, t.toMillis(false));
				b.putString(RecurrencePickerDialog.BUNDLE_TIME_ZONE, t.timezone);

				// may be more efficient to serialize and pass in EventRecurrence
				b.putString(RecurrencePickerDialog.BUNDLE_RRULE, mRecurrenceRule);

				RecurrencePickerDialog rpd = (RecurrencePickerDialog) fm.findFragmentByTag(
						"recurrence_picker");
				if (rpd != null) {
					rpd.dismiss();
				}
				rpd = new RecurrencePickerDialog();
				rpd.setArguments(b);
				rpd.setOnRecurrenceSetListener(ExportFormFragment.this);
				rpd.show(fm, "recurrence_picker");
			}
		});

		//this part (setting the export format) must come after the recurrence view bindings above
        String defaultExportFormat = sharedPrefs.getString(getString(R.string.key_default_export_format), ExportFormat.QIF.name());
        mExportFormat = ExportFormat.valueOf(defaultExportFormat);
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRadioButtonClicked(view);
            }
        };

		View v = getView();
		assert v != null;

        RadioButton ofxRadioButton = (RadioButton) v.findViewById(R.id.radio_ofx_format);
        ofxRadioButton.setOnClickListener(clickListener);
        if (defaultExportFormat.equalsIgnoreCase(ExportFormat.OFX.name())) {
            ofxRadioButton.performClick();
        }

        RadioButton qifRadioButton = (RadioButton) v.findViewById(R.id.radio_qif_format);
        qifRadioButton.setOnClickListener(clickListener);
        if (defaultExportFormat.equalsIgnoreCase(ExportFormat.QIF.name())){
            qifRadioButton.performClick();
        }

		RadioButton xmlRadioButton = (RadioButton) v.findViewById(R.id.radio_xml_format);
		xmlRadioButton.setOnClickListener(clickListener);
		if (defaultExportFormat.equalsIgnoreCase(ExportFormat.XML.name())){
			xmlRadioButton.performClick();
		}
	}

	@Override
	public void onRecurrenceSet(String rrule) {
		mRecurrenceRule = rrule;
		String repeatString = getString(R.string.label_tap_to_create_schedule);

		if (mRecurrenceRule != null){
			mEventRecurrence.parse(mRecurrenceRule);
			repeatString = EventRecurrenceFormatter.getRepeatString(getActivity(), getResources(),
					mEventRecurrence, true);
		}
		mRecurrenceTextView.setText(repeatString);
	}

	/**
	 * Callback for when the activity chooser dialog is completed
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SettingsActivity.REQUEST_RESOLVE_CONNECTION && resultCode == Activity.RESULT_OK) {
			SettingsActivity.mGoogleApiClient.connect();
		}
	}

}

