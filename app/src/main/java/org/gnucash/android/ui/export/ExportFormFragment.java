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
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
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
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.codetroopers.betterpickers.recurrencepicker.EventRecurrence;
import com.codetroopers.betterpickers.recurrencepicker.EventRecurrenceFormatter;
import com.codetroopers.betterpickers.recurrencepicker.RecurrencePickerDialogFragment;
import com.dropbox.sync.android.DbxAccountManager;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.DatabaseAdapter;
import org.gnucash.android.db.adapter.ScheduledActionDbAdapter;
import org.gnucash.android.export.ExportAsyncTask;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.model.BaseModel;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.settings.BackupPreferenceFragment;
import org.gnucash.android.ui.settings.PreferenceActivity;
import org.gnucash.android.ui.settings.dialog.OwnCloudDialogFragment;
import org.gnucash.android.ui.transaction.TransactionFormFragment;
import org.gnucash.android.ui.util.RecurrenceParser;
import org.gnucash.android.ui.util.RecurrenceViewClickListener;
import org.gnucash.android.util.PreferencesHelper;
import org.gnucash.android.util.TimestampHelper;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import butterknife.Bind;
import butterknife.ButterKnife;


/**
 * Dialog fragment for exporting accounts and transactions in various formats
 * <p>The dialog is used for collecting information on the export options and then passing them
 * to the {@link org.gnucash.android.export.Exporter} responsible for exporting</p>
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ExportFormFragment extends Fragment implements
		RecurrencePickerDialogFragment.OnRecurrenceSetListener,
		CalendarDatePickerDialogFragment.OnDateSetListener,
		RadialTimePickerDialogFragment.OnTimeSetListener {
		
	/**
	 * Spinner for selecting destination for the exported file.
	 * The destination could either be SD card, or another application which
	 * accepts files, like Google Drive.
	 */
	@Bind(R.id.spinner_export_destination) Spinner mDestinationSpinner;
	
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
	 * Text view displaying start date to export from
	 */
	@Bind(R.id.export_start_date) TextView mExportStartDate;

	@Bind(R.id.export_start_time) TextView mExportStartTime;

	/**
	 * Switch toggling whether to export all transactions or not
	 */
	@Bind(R.id.switch_export_all) SwitchCompat mExportAllSwitch;

	@Bind(R.id.export_date_layout) LinearLayout mExportDateLayout;

	@Bind(R.id.radio_ofx_format) RadioButton mOfxRadioButton;
	@Bind(R.id.radio_qif_format) RadioButton mQifRadioButton;
	@Bind(R.id.radio_xml_format) RadioButton mXmlRadioButton;

	/**
	 * Event recurrence options
	 */
	private EventRecurrence mEventRecurrence = new EventRecurrence();

	/**
	 * Recurrence rule
	 */
	private String mRecurrenceRule;

	private Calendar mExportStartCalendar = Calendar.getInstance();

	/**
	 * Tag for logging
	 */
	private static final String TAG = "ExportFormFragment";

	/**
	 * Export format
	 */
    private ExportFormat mExportFormat = ExportFormat.QIF;

	private ExportParams.ExportTarget mExportTarget = ExportParams.ExportTarget.SD_CARD;


	private void onRadioButtonClicked(View view){
        switch (view.getId()){
            case R.id.radio_ofx_format:
                mExportFormat = ExportFormat.OFX;
                if (GnuCashApplication.isDoubleEntryEnabled()){
                    mExportWarningTextView.setText(getActivity().getString(R.string.export_warning_ofx));
                    mExportWarningTextView.setVisibility(View.VISIBLE);
                } else {
                    mExportWarningTextView.setVisibility(View.GONE);
                }
				mExportDateLayout.setVisibility(View.VISIBLE);
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
				mExportDateLayout.setVisibility(View.VISIBLE);
				break;

			case R.id.radio_xml_format:
				mExportFormat = ExportFormat.XML;
				mExportWarningTextView.setText(R.string.export_warning_xml);
				mExportDateLayout.setVisibility(View.GONE);
				break;
        }
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_export_form, container, false);

		ButterKnife.bind(this, view);

		bindViewListeners();

		return view;
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

		ActionBar supportActionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
		assert supportActionBar != null;
		supportActionBar.setTitle(R.string.title_export_dialog);
		setHasOptionsMenu(true);

		getSDWritePermission();
	}

    @Override
    public void onPause() {
        super.onPause();
        // When the user try to export sharing to 3rd party service like DropBox
        // then pausing all activities. That cause passcode screen appearing happened.
        // We use a disposable flag to skip this unnecessary passcode screen.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().putBoolean(UxArgument.SKIP_PASSCODE_SCREEN, true).apply();
    }

	/**
	 * Get permission for WRITING SD card for Android Marshmallow and above
	 */
	private void getSDWritePermission(){
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
					!= PackageManager.PERMISSION_GRANTED) {
				getActivity().requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
						Manifest.permission.READ_EXTERNAL_STORAGE}, AccountsActivity.REQUEST_PERMISSION_WRITE_SD_CARD);
			}
		}
	}

	/**
	 * Starts the export of transactions with the specified parameters
	 */
	private void startExport(){
		ExportParams exportParameters = new ExportParams(mExportFormat);

		if (mExportAllSwitch.isChecked()){
			exportParameters.setExportStartTime(TimestampHelper.getTimestampFromEpochZero());
		} else {
			exportParameters.setExportStartTime(new Timestamp(mExportStartCalendar.getTimeInMillis()));
		}

		exportParameters.setExportTarget(mExportTarget);
		exportParameters.setDeleteTransactionsAfterExport(mDeleteAllCheckBox.isChecked());

		ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.BACKUP);
		scheduledAction.setRecurrence(RecurrenceParser.parse(mEventRecurrence));
		scheduledAction.setTag(exportParameters.toCsv());
		scheduledAction.setActionUID(BaseModel.generateUID());
		ScheduledActionDbAdapter.getInstance().addRecord(scheduledAction, DatabaseAdapter.UpdateMethod.insert);

		Log.i(TAG, "Commencing async export of transactions");
		new ExportAsyncTask(getActivity()).execute(exportParameters);

		int position = mDestinationSpinner.getSelectedItemPosition();
		PreferenceManager.getDefaultSharedPreferences(getActivity())
				.edit().putInt(getString(R.string.key_last_export_destination), position)
				.apply();

		// finish the activity will cause the progress dialog to be leaked
		// which would throw an exception
		//getActivity().finish();
	}

	private void bindViewListeners(){
		// export destination bindings
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
						String dropboxAppKey = getString(R.string.dropbox_app_key, BackupPreferenceFragment.DROPBOX_APP_KEY);
						String dropboxAppSecret = getString(R.string.dropbox_app_secret, BackupPreferenceFragment.DROPBOX_APP_SECRET);
						DbxAccountManager mDbxAccountManager = DbxAccountManager.getInstance(getActivity().getApplicationContext(),
								dropboxAppKey, dropboxAppSecret);
						if (!mDbxAccountManager.hasLinkedAccount()) {
							mDbxAccountManager.startLink(getActivity(), 0);
						}
						break;
					case 2:
						recurrenceOptionsView.setVisibility(View.VISIBLE);
						mExportTarget = ExportParams.ExportTarget.GOOGLE_DRIVE;
						BackupPreferenceFragment.mGoogleApiClient = BackupPreferenceFragment.getGoogleApiClient(getActivity());
						BackupPreferenceFragment.mGoogleApiClient.connect();
						break;
					case 3:
						recurrenceOptionsView.setVisibility(View.VISIBLE);
						mExportTarget = ExportParams.ExportTarget.OWNCLOUD;
						if(!(PreferenceManager.getDefaultSharedPreferences(getActivity())
								.getBoolean(getString(R.string.key_owncloud_sync), false))) {
							OwnCloudDialogFragment ocDialog = OwnCloudDialogFragment.newInstance(null);
							ocDialog.show(getActivity().getSupportFragmentManager(), "ownCloud dialog");
						}
						break;
					case 4:
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

		int position = PreferenceManager.getDefaultSharedPreferences(getActivity())
				.getInt(getString(R.string.key_last_export_destination), 0);
		mDestinationSpinner.setSelection(position);

		//**************** export start time bindings ******************
		Timestamp timestamp = PreferencesHelper.getLastExportTime();
		mExportStartCalendar.setTimeInMillis(timestamp.getTime());

		Date date = new Date(timestamp.getTime());
		mExportStartDate.setText(TransactionFormFragment.DATE_FORMATTER.format(date));
		mExportStartTime.setText(TransactionFormFragment.TIME_FORMATTER.format(date));

		mExportStartDate.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				long dateMillis = 0;
				try {
					Date date = TransactionFormFragment.DATE_FORMATTER.parse(mExportStartDate.getText().toString());
					dateMillis = date.getTime();
				} catch (ParseException e) {
					Log.e(getTag(), "Error converting input time to Date object");
				}
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(dateMillis);

				int year = calendar.get(Calendar.YEAR);
				int monthOfYear = calendar.get(Calendar.MONTH);
				int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
				CalendarDatePickerDialogFragment datePickerDialog = CalendarDatePickerDialogFragment.newInstance(
						ExportFormFragment.this,
						year, monthOfYear, dayOfMonth);
				datePickerDialog.show(getFragmentManager(), "date_picker_fragment");
			}
		});

		mExportStartTime.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				long timeMillis = 0;
				try {
					Date date = TransactionFormFragment.TIME_FORMATTER.parse(mExportStartTime.getText().toString());
					timeMillis = date.getTime();
				} catch (ParseException e) {
					Log.e(getTag(), "Error converting input time to Date object");
				}

				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(timeMillis);

				RadialTimePickerDialogFragment timePickerDialog = RadialTimePickerDialogFragment.newInstance(
						ExportFormFragment.this, calendar.get(Calendar.HOUR_OF_DAY),
						calendar.get(Calendar.MINUTE), true);
				timePickerDialog.show(getFragmentManager(), "time_picker_dialog_fragment");
			}
		});

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mExportAllSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mExportStartDate.setEnabled(!isChecked);
				mExportStartTime.setEnabled(!isChecked);
				int color = isChecked ? android.R.color.darker_gray : android.R.color.black;
				mExportStartDate.setTextColor(getResources().getColor(color));
				mExportStartTime.setTextColor(getResources().getColor(color));
			}
		});

		mExportAllSwitch.setChecked(sharedPrefs.getBoolean(getString(R.string.key_export_all_transactions), false));
		mDeleteAllCheckBox.setChecked(sharedPrefs.getBoolean(getString(R.string.key_delete_transactions_after_export), false));

		mRecurrenceTextView.setOnClickListener(new RecurrenceViewClickListener((AppCompatActivity) getActivity(), mRecurrenceRule, this));

		//this part (setting the export format) must come after the recurrence view bindings above
        String defaultExportFormat = sharedPrefs.getString(getString(R.string.key_default_export_format), ExportFormat.QIF.name());
        mExportFormat = ExportFormat.valueOf(defaultExportFormat);

        View.OnClickListener radioClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRadioButtonClicked(view);
            }
        };

		View v = getView();
		assert v != null;

		mOfxRadioButton.setOnClickListener(radioClickListener);
		mQifRadioButton.setOnClickListener(radioClickListener);
		mXmlRadioButton.setOnClickListener(radioClickListener);

		ExportFormat defaultFormat = ExportFormat.valueOf(defaultExportFormat.toUpperCase());
		switch (defaultFormat){
			case QIF: mQifRadioButton.performClick(); break;
			case OFX: mOfxRadioButton.performClick(); break;
			case XML: mXmlRadioButton.performClick(); break;
		}

		if (GnuCashApplication.isDoubleEntryEnabled()){
			mOfxRadioButton.setVisibility(View.GONE);
		} else {
			mXmlRadioButton.setVisibility(View.GONE);
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
		if (requestCode == BackupPreferenceFragment.REQUEST_RESOLVE_CONNECTION && resultCode == Activity.RESULT_OK) {
			BackupPreferenceFragment.mGoogleApiClient.connect();
		}
	}

	@Override
	public void onDateSet(CalendarDatePickerDialogFragment dialog, int year, int monthOfYear, int dayOfMonth) {
		Calendar cal = new GregorianCalendar(year, monthOfYear, dayOfMonth);
		mExportStartDate.setText(TransactionFormFragment.DATE_FORMATTER.format(cal.getTime()));
		mExportStartCalendar.set(Calendar.YEAR, year);
		mExportStartCalendar.set(Calendar.MONTH, monthOfYear);
		mExportStartCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
	}

	@Override
	public void onTimeSet(RadialTimePickerDialogFragment dialog, int hourOfDay, int minute) {
		Calendar cal = new GregorianCalendar(0, 0, 0, hourOfDay, minute);
		mExportStartTime.setText(TransactionFormFragment.TIME_FORMATTER.format(cal.getTime()));
		mExportStartCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
		mExportStartCalendar.set(Calendar.MINUTE, minute);
	}
}

