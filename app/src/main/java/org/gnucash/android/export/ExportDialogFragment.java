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

package org.gnucash.android.export;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.doomonafireball.betterpickers.recurrencepicker.EventRecurrence;
import com.doomonafireball.betterpickers.recurrencepicker.EventRecurrenceFormatter;
import com.doomonafireball.betterpickers.recurrencepicker.RecurrencePickerDialog;
import com.dropbox.sync.android.DbxAccountManager;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.ScheduledActionDbAdapter;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.ui.settings.SettingsActivity;
import org.gnucash.android.ui.util.RecurrenceParser;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * Dialog fragment for exporting account information as OFX files.
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ExportDialogFragment extends DialogFragment implements RecurrencePickerDialog.OnRecurrenceSetListener {
		
	/**
	 * Spinner for selecting destination for the exported file.
	 * The destination could either be SD card, or another application which
	 * accepts files, like Google Drive.
	 */
	Spinner mDestinationSpinner;
	
	/**
	 * Checkbox indicating that all transactions should be exported,
	 * regardless of whether they have been exported previously or not
	 */
	CheckBox mExportAllCheckBox;
	
	/**
	 * Checkbox for deleting all transactions after exporting them
	 */
	CheckBox mDeleteAllCheckBox;
	
	/**
	 * Save button for saving the exported files
	 */
	Button mSaveButton;
	
	/**
	 * Cancels the export dialog
	 */
	Button mCancelButton;

    /**
     * Text view for showing warnings based on chosen export format
     */
    TextView mExportWarningTextView;

	/**
	 * Recurrence text view
	 */
	TextView mRecurrenceTextView;

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
	private static final String TAG = "ExportDialogFragment";

	/**
	 * Export format
	 */
    private ExportFormat mExportFormat = ExportFormat.QIF;

	private ExportParams.ExportTarget mExportTarget = ExportParams.ExportTarget.SD_CARD;

	private GoogleApiClient mGoogleApiClient;


	/**
	 * Click listener for positive button in the dialog.
	 * @author Ngewi Fet <ngewif@gmail.com>
	 */
	protected class ExportClickListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
            ExportParams exportParameters = new ExportParams(mExportFormat);
            exportParameters.setExportAllTransactions(mExportAllCheckBox.isChecked());
			exportParameters.setExportTarget(mExportTarget);
			exportParameters.setDeleteTransactionsAfterExport(mDeleteAllCheckBox.isChecked());

			ScheduledActionDbAdapter scheduledActionDbAdapter = ScheduledActionDbAdapter.getInstance();
			scheduledActionDbAdapter.deleteScheduledBackupAction(mExportFormat);
			List<ScheduledAction> events = RecurrenceParser.parse(mEventRecurrence,
					ScheduledAction.ActionType.BACKUP);
			//this is done on purpose, we will add only one scheduled action per export type
			//FIXME: Prevent user from setting multiple days in dialog or update scheduled action parser to return only one recurrence
			if (!events.isEmpty()){
				ScheduledAction scheduledAction = events.get(0);
				scheduledAction.setTag(exportParameters.toCsv());
				scheduledAction.setActionUID(UUID.randomUUID().toString().replaceAll("-", ""));
				scheduledActionDbAdapter.addScheduledAction(scheduledAction);
			}
            dismiss();

            Log.i(TAG, "Commencing async export of transactions");
            new ExportAsyncTask(getActivity()).execute(exportParameters);
		}
		
	}

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
		refreshRecurrenceTextView(mExportFormat);
    }

	/**
	 * Refreshes the recurrence text view for the specified backup format
	 * This is meant to be called every time the backup format is changed in the dialog
	 * @param exportFormat ExportFormat
	 */
	private void refreshRecurrenceTextView(ExportFormat exportFormat){
		String repeatString	= getString(R.string.label_tap_to_create_schedule);
		mEventRecurrence = new EventRecurrence();
		ScheduledActionDbAdapter scheduledActionDbAdapter = ScheduledActionDbAdapter.getInstance();
		ScheduledAction scheduledBackup = scheduledActionDbAdapter.getScheduledBackupAction(exportFormat);
		if (scheduledBackup != null){
			repeatString = scheduledBackup.getRepeatString();
			mRecurrenceRule = scheduledBackup.getRuleString();
			mEventRecurrence.parse(mRecurrenceRule);
		}
		mRecurrenceTextView.setText(repeatString);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.dialog_export, container, false);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {		
		super.onActivityCreated(savedInstanceState);
        bindViews();
		getDialog().setTitle(R.string.title_export_dialog);
	}

	private void bindViews(){
		View v = getView();
        assert v != null;
        mDestinationSpinner = (Spinner) v.findViewById(R.id.spinner_export_destination);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
		        R.array.export_destinations, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);		
		mDestinationSpinner.setAdapter(adapter);
		mDestinationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				View recurrenceOptionsView = getView().findViewById(R.id.recurrence_options);
				switch (position){
					case 0:
						mExportTarget = ExportParams.ExportTarget.SD_CARD;
						recurrenceOptionsView.setVisibility(View.VISIBLE);
						break;
					case 1:
						recurrenceOptionsView.setVisibility(View.VISIBLE);
						mExportTarget = ExportParams.ExportTarget.DROPBOX;
						DbxAccountManager mDbxAccountManager = DbxAccountManager.getInstance(getActivity().getApplicationContext(),
								SettingsActivity.DROPBOX_APP_KEY, SettingsActivity.DROPBOX_APP_SECRET);
						if (!mDbxAccountManager.hasLinkedAccount()){
							mDbxAccountManager.startLink(getActivity(), 0);
						}
						break;
					case 2:
						recurrenceOptionsView.setVisibility(View.VISIBLE);
						mExportTarget = ExportParams.ExportTarget.GOOGLE_DRIVE;
						mGoogleApiClient = SettingsActivity.getGoogleApiClient(getActivity());
						mGoogleApiClient.connect();
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
		mExportAllCheckBox = (CheckBox) v.findViewById(R.id.checkbox_export_all);
		mExportAllCheckBox.setChecked(sharedPrefs.getBoolean(getString(R.string.key_export_all_transactions), false));
		
		mDeleteAllCheckBox = (CheckBox) v.findViewById(R.id.checkbox_post_export_delete);
		mDeleteAllCheckBox.setChecked(sharedPrefs.getBoolean(getString(R.string.key_delete_transactions_after_export), false));
		
		mSaveButton = (Button) v.findViewById(R.id.btn_save);
		mSaveButton.setText(R.string.btn_export);
		mCancelButton = (Button) v.findViewById(R.id.btn_cancel);
		
		mCancelButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		
		mSaveButton.setOnClickListener(new ExportClickListener());

		mRecurrenceTextView     = (TextView) v.findViewById(R.id.input_recurrence);
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
				rpd.setOnRecurrenceSetListener(ExportDialogFragment.this);
				rpd.show(fm, "recurrence_picker");
			}
		});

        mExportWarningTextView = (TextView) v.findViewById(R.id.export_warning);

		//this part (setting the export format) must come after the recurrence view bindings above
        String defaultExportFormat = sharedPrefs.getString(getString(R.string.key_default_export_format), ExportFormat.QIF.name());
        mExportFormat = ExportFormat.valueOf(defaultExportFormat);
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRadioButtonClicked(view);
            }
        };

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
			mGoogleApiClient.connect();
		}
	}

}

