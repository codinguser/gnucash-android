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

import static org.gnucash.android.app.IntentExtKt.takePersistableUriPermission;
import static org.gnucash.android.util.ContentExtKt.getDocumentName;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.codetroopers.betterpickers.recurrencepicker.EventRecurrence;
import com.codetroopers.betterpickers.recurrencepicker.EventRecurrenceFormatter;
import com.codetroopers.betterpickers.recurrencepicker.RecurrencePickerDialogFragment;
import com.dropbox.core.android.Auth;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.db.adapter.DatabaseAdapter;
import org.gnucash.android.db.adapter.ScheduledActionDbAdapter;
import org.gnucash.android.export.DropboxHelper;
import org.gnucash.android.export.ExportAsyncTask;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.model.BaseModel;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.settings.BackupPreferenceFragment;
import org.gnucash.android.ui.settings.dialog.OwnCloudDialogFragment;
import org.gnucash.android.ui.transaction.TransactionFormFragment;
import org.gnucash.android.ui.util.RecurrenceParser;
import org.gnucash.android.ui.util.RecurrenceViewClickListener;
import org.gnucash.android.util.PreferencesHelper;
import org.gnucash.android.util.TimestampHelper;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

/**
 * Dialog fragment for exporting accounts and transactions in various formats
 * <p>The dialog is used for collecting information on the export options and then passing them
 * to the {@link org.gnucash.android.export.Exporter} responsible for exporting</p>
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ExportFormFragment extends Fragment implements
    RecurrencePickerDialogFragment.OnRecurrenceSetListener,
    CalendarDatePickerDialogFragment.OnDateSetListener,
    RadialTimePickerDialogFragment.OnTimeSetListener {

    /**
     * Request code for intent to pick export file destination
     */
    private static final int REQUEST_EXPORT_FILE = 0x14;

    //Save As..
    private static final int TARGET_URI = 0;
    //DROPBOX
    private static final int TARGET_DROPBOX = 1;
    //OwnCloud
    private static final int TARGET_OWNCLOUD = 2;
    //Share File
    private static final int TARGET_SHARE = 3;

    /**
     * Spinner for selecting destination for the exported file.
     * The destination could either be SD card, or another application which
     * accepts files, like Google Drive.
     */
    @BindView(R.id.spinner_export_destination)
    Spinner mDestinationSpinner;

    /**
     * Checkbox for deleting all transactions after exporting them
     */
    @BindView(R.id.checkbox_post_export_delete)
    CheckBox mDeleteAllCheckBox;

    /**
     * Text view for showing warnings based on chosen export format
     */
    @BindView(R.id.export_warning)
    TextView mExportWarningTextView;

    @BindView(R.id.target_uri)
    TextView mTargetUriTextView;

    /**
     * Recurrence text view
     */
    @BindView(R.id.input_recurrence)
    TextView mRecurrenceTextView;

    /**
     * Text view displaying start date to export from
     */
    @BindView(R.id.export_start_date)
    TextView mExportStartDate;

    @BindView(R.id.export_start_time)
    TextView mExportStartTime;

    /**
     * Switch toggling whether to export all transactions or not
     */
    @BindView(R.id.switch_export_all)
    SwitchCompat mExportAllSwitch;

    @BindView(R.id.export_date_layout)
    LinearLayout mExportDateLayout;

    @BindView(R.id.radio_ofx_format)
    RadioButton mOfxRadioButton;
    @BindView(R.id.radio_qif_format)
    RadioButton mQifRadioButton;
    @BindView(R.id.radio_xml_format)
    RadioButton mXmlRadioButton;
    @BindView(R.id.radio_csv_transactions_format)
    RadioButton mCsvTransactionsRadioButton;

    @BindView(R.id.radio_separator_comma_format)
    RadioButton mSeparatorCommaButton;
    @BindView(R.id.radio_separator_colon_format)
    RadioButton mSeparatorColonButton;
    @BindView(R.id.radio_separator_semicolon_format)
    RadioButton mSeparatorSemicolonButton;
    @BindView(R.id.layout_csv_options)
    LinearLayout mCsvOptionsLayout;

    @BindView(R.id.recurrence_options)
    View mRecurrenceOptionsView;
    /**
     * Event recurrence options
     */
    private final EventRecurrence mEventRecurrence = new EventRecurrence();

    /**
     * Recurrence rule
     */
    private String mRecurrenceRule;

    private final Calendar mExportStartCalendar = Calendar.getInstance();

    private final ExportParams mExportParams = new ExportParams();
    private ScheduledAction mScheduledAction;

    /**
     * Flag to determine if export has been started.
     * Used to continue export after user has picked a destination file
     */
    private boolean mExportStarted = false;

    private void onRadioButtonClicked(View view) {
        switch (view.getId()) {
            case R.id.radio_ofx_format:
                mExportParams.setExportFormat(ExportFormat.OFX);
                if (GnuCashApplication.isDoubleEntryEnabled()) {
                    mExportWarningTextView.setText(getString(R.string.export_warning_ofx));
                    mExportWarningTextView.setVisibility(View.VISIBLE);
                } else {
                    mExportWarningTextView.setVisibility(View.GONE);
                }

                OptionsViewAnimationUtils.expand(mExportDateLayout);
                OptionsViewAnimationUtils.collapse(mCsvOptionsLayout);
                break;

            case R.id.radio_qif_format:
                mExportParams.setExportFormat(ExportFormat.QIF);
                //TODO: Also check that there exist transactions with multiple currencies before displaying warning
                if (GnuCashApplication.isDoubleEntryEnabled()) {
                    mExportWarningTextView.setText(getString(R.string.export_warning_qif));
                    mExportWarningTextView.setVisibility(View.VISIBLE);
                } else {
                    mExportWarningTextView.setVisibility(View.GONE);
                }

                OptionsViewAnimationUtils.expand(mExportDateLayout);
                OptionsViewAnimationUtils.collapse(mCsvOptionsLayout);
                break;

            case R.id.radio_xml_format:
                mExportParams.setExportFormat(ExportFormat.XML);
                mExportWarningTextView.setText(R.string.export_warning_xml);
                OptionsViewAnimationUtils.collapse(mExportDateLayout);
                OptionsViewAnimationUtils.collapse(mCsvOptionsLayout);
                break;

            case R.id.radio_csv_transactions_format:
                mExportParams.setExportFormat(ExportFormat.CSVT);
                mExportWarningTextView.setText(R.string.export_notice_csv);
                OptionsViewAnimationUtils.expand(mExportDateLayout);
                OptionsViewAnimationUtils.expand(mCsvOptionsLayout);
                break;

            case R.id.radio_separator_comma_format:
                mExportParams.setCsvSeparator(',');
                break;
            case R.id.radio_separator_colon_format:
                mExportParams.setCsvSeparator(':');
                break;
            case R.id.radio_separator_semicolon_format:
                mExportParams.setCsvSeparator(';');
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_export_form, container, false);

        ButterKnife.bind(this, view);

        bindViewListeners(view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if ((args == null) || args.isEmpty()) {
            return;
        }
        String scheduledUID = args.getString(UxArgument.SCHEDULED_UID);
        if (TextUtils.isEmpty(scheduledUID)) {
            return;
        }
        ScheduledActionDbAdapter scheduledActionDbAdapter = ScheduledActionDbAdapter.getInstance();
        ScheduledAction scheduledAction = scheduledActionDbAdapter.getRecord(scheduledUID);
        if (scheduledAction == null) {
            return;
        }
        bindForm(scheduledAction);
    }

    private void bindForm(@NonNull ScheduledAction scheduledAction) {
        mScheduledAction = scheduledAction;
        String tag = scheduledAction.getTag();
        if (TextUtils.isEmpty(tag)) {
            return;
        }
        ExportParams exportParams = ExportParams.parseCsv(tag);
        ExportFormat exportFormat = exportParams.getExportFormat();
        Uri uri = exportParams.getExportLocation();
        ExportParams.ExportTarget exportTarget = exportParams.getExportTarget();
        char csvSeparator = exportParams.getCsvSeparator();
        Timestamp startTime = exportParams.getExportStartTime();

        switch (exportTarget) {
            case DROPBOX, GOOGLE_DRIVE:
                mDestinationSpinner.setSelection(TARGET_DROPBOX);
                break;
            case OWNCLOUD:
                mDestinationSpinner.setSelection(TARGET_OWNCLOUD);
                break;
            case SD_CARD, URI:
                mDestinationSpinner.setSelection(TARGET_URI);
                break;
            case SHARING:
                mDestinationSpinner.setSelection(TARGET_SHARE);
                break;
        }

        setExportUri(uri);

        switch (exportFormat) {
            case CSVA:
            case CSVT:
                mCsvTransactionsRadioButton.setChecked(true);
                break;
            case OFX:
                mOfxRadioButton.setChecked(true);
                break;
            case QIF:
                mQifRadioButton.setChecked(true);
                break;
            case XML:
                mXmlRadioButton.setChecked(true);
                break;
        }

        switch (csvSeparator) {
            case ',':
                mSeparatorCommaButton.setChecked(true);
                break;
            case ':':
                mSeparatorColonButton.setChecked(true);
                break;
            case ';':
                mSeparatorSemicolonButton.setChecked(true);
                break;
        }

        long startTimeMills = startTime.getTime();
        if (startTimeMills > 0L) {
            mExportStartCalendar.setTimeInMillis(startTimeMills);
            mExportStartDate.setText(TransactionFormFragment.DATE_FORMATTER.print(startTimeMills));
            mExportStartTime.setText(TransactionFormFragment.TIME_FORMATTER.print(startTimeMills));
            mExportAllSwitch.setChecked(false);
        } else {
            mExportAllSwitch.setChecked(true);
        }

        mDeleteAllCheckBox.setChecked(exportParams.shouldDeleteTransactionsAfterExport());

        String rrule = scheduledAction.getRuleString();
        onRecurrenceSet(rrule);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.export_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                startExport();
                return true;

            case android.R.id.home:
                requireActivity().finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        ActionBar supportActionBar = activity.getSupportActionBar();
        assert supportActionBar != null;
        supportActionBar.setTitle(R.string.title_export_dialog);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        DropboxHelper.retrieveAndSaveToken();
    }

    @Override
    public void onPause() {
        super.onPause();
        // When the user try to export sharing to 3rd party service like DropBox
        // then pausing all activities. That cause passcode screen appearing happened.
        // We use a disposable flag to skip this unnecessary passcode screen.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        prefs.edit().putBoolean(UxArgument.SKIP_PASSCODE_SCREEN, true).apply();
    }

    /**
     * Starts the export of transactions with the specified parameters
     */
    private void startExport() {
        ExportParams exportParameters = mExportParams;

        if (exportParameters.getExportTarget() == ExportParams.ExportTarget.URI && exportParameters.getExportLocation() == null) {
            mExportStarted = true;
            selectExportFile();
            return;
        }

        if (mExportAllSwitch.isChecked()) {
            exportParameters.setExportStartTime(TimestampHelper.getTimestampFromEpochZero());
        } else {
            exportParameters.setExportStartTime(new Timestamp(mExportStartCalendar.getTimeInMillis()));
        }

        Timber.i("Commencing async export of transactions");
        new ExportAsyncTask(requireActivity(), GnuCashApplication.getActiveBookUID()).execute(exportParameters);

        if (mRecurrenceRule != null) {
            DatabaseAdapter.UpdateMethod updateMethod = DatabaseAdapter.UpdateMethod.replace;
            ScheduledAction scheduledAction = mScheduledAction;
            if (scheduledAction == null) {
                scheduledAction = new ScheduledAction(ScheduledAction.ActionType.BACKUP);
                scheduledAction.setActionUID(BaseModel.generateUID());
                updateMethod = DatabaseAdapter.UpdateMethod.insert;
            }
            scheduledAction.setRecurrence(RecurrenceParser.parse(mEventRecurrence));
            scheduledAction.setTag(exportParameters.toCsv());
            ScheduledActionDbAdapter.getInstance().addRecord(scheduledAction, updateMethod);
            mScheduledAction = scheduledAction;
        }

        int position = mDestinationSpinner.getSelectedItemPosition();
        PreferenceManager.getDefaultSharedPreferences(getActivity())
            .edit()
            .putInt(getString(R.string.key_last_export_destination), position)
            .apply();

        // finish the activity will cause the progress dialog to be leaked
        // which would throw an exception
        //getActivity().finish();
    }

    /**
     * Bind views to actions when initializing the export form
     */
    private void bindViewListeners(View view) {
        final Context context = view.getContext();
        // export destination bindings
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
            R.array.export_destinations, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDestinationSpinner.setAdapter(adapter);
        mDestinationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view == null) //the item selection is fired twice by the Android framework. Ignore the first one
                    return;
                switch (position) {
                    case TARGET_URI:
                        mExportParams.setExportTarget(ExportParams.ExportTarget.URI);
                        mRecurrenceOptionsView.setVisibility(View.VISIBLE);
                        Uri exportUri = mExportParams.getExportLocation();
                        setExportUri(exportUri);
                        break;
                    case TARGET_DROPBOX:
                        setExportUriText(getString(R.string.label_dropbox_export_destination));
                        mRecurrenceOptionsView.setVisibility(View.VISIBLE);
                        mExportParams.setExportTarget(ExportParams.ExportTarget.DROPBOX);
                        String dropboxAppKey = getString(R.string.dropbox_app_key, BackupPreferenceFragment.DROPBOX_APP_KEY);

                        if (!DropboxHelper.hasToken()) {
                            Auth.startOAuth2Authentication(context, dropboxAppKey);
                        }
                        break;
                    case TARGET_OWNCLOUD:
                        setExportUri(null);
                        mRecurrenceOptionsView.setVisibility(View.VISIBLE);
                        mExportParams.setExportTarget(ExportParams.ExportTarget.OWNCLOUD);
                        if (!(PreferenceManager.getDefaultSharedPreferences(getActivity())
                            .getBoolean(getString(R.string.key_owncloud_sync), false))) {
                            OwnCloudDialogFragment ocDialog = OwnCloudDialogFragment.newInstance(null);
                            ocDialog.show(getParentFragmentManager(), "ownCloud dialog");
                        }
                        break;
                    case TARGET_SHARE:
                        setExportUriText(getString(R.string.label_select_destination_after_export));
                        mExportParams.setExportTarget(ExportParams.ExportTarget.SHARING);
                        mRecurrenceOptionsView.setVisibility(View.GONE);
                        break;

                    default:
                        mExportParams.setExportTarget(ExportParams.ExportTarget.SD_CARD);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //nothing to see here, move along
            }
        });

        int position = PreferenceManager.getDefaultSharedPreferences(getActivity())
            .getInt(getString(R.string.key_last_export_destination), 0);
        mDestinationSpinner.setSelection(position);

        //**************** export start time bindings ******************
        Timestamp timestamp = PreferencesHelper.getLastExportTime();
        final long date = timestamp.getTime() - DateUtils.WEEK_IN_MILLIS;
        mExportStartCalendar.setTimeInMillis(date);
        mExportStartDate.setText(TransactionFormFragment.DATE_FORMATTER.print(date));
        mExportStartTime.setText(TransactionFormFragment.TIME_FORMATTER.print(date));

        mExportStartDate.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                long dateMillis = 0;
                try {
                    dateMillis = TransactionFormFragment.DATE_FORMATTER.parseMillis(mExportStartDate.getText().toString());
                } catch (IllegalArgumentException e) {
                    Timber.e(e, "Error converting input time to Date object");
                }
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(dateMillis);

                int year = calendar.get(Calendar.YEAR);
                int monthOfYear = calendar.get(Calendar.MONTH);
                int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
                CalendarDatePickerDialogFragment datePickerDialog = new CalendarDatePickerDialogFragment();
                datePickerDialog.setOnDateSetListener(ExportFormFragment.this);
                datePickerDialog.setPreselectedDate(year, monthOfYear, dayOfMonth);
                datePickerDialog.show(getFragmentManager(), "date_picker_fragment");
            }
        });

        mExportStartTime.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                long timeMillis = 0;
                try {
                    timeMillis = TransactionFormFragment.TIME_FORMATTER.parseMillis(mExportStartTime.getText().toString());
                } catch (IllegalArgumentException e) {
                    Timber.e(e, "Error converting input time to Date object");
                }
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(timeMillis);

                RadialTimePickerDialogFragment timePickerDialog = new RadialTimePickerDialogFragment();
                timePickerDialog.setOnTimeSetListener(ExportFormFragment.this);
                timePickerDialog.setStartTime(calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE));
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
                mExportStartDate.setTextColor(ContextCompat.getColor(getContext(), color));
                mExportStartTime.setTextColor(ContextCompat.getColor(getContext(), color));
            }
        });

        mExportAllSwitch.setChecked(sharedPrefs.getBoolean(getString(R.string.key_export_all_transactions), false));
        mDeleteAllCheckBox.setChecked(sharedPrefs.getBoolean(getString(R.string.key_delete_transactions_after_export), false));
        mDeleteAllCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mExportParams.setDeleteTransactionsAfterExport(isChecked);
            }
        });

        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        mRecurrenceTextView.setOnClickListener(new RecurrenceViewClickListener(activity, mRecurrenceRule, this));

        //this part (setting the export format) must come after the recurrence view bindings above
        String defaultExportFormat = sharedPrefs.getString(getString(R.string.key_default_export_format), ExportFormat.CSVT.value);
        mExportParams.setExportFormat(ExportFormat.of(defaultExportFormat));

        RadioButton.OnCheckedChangeListener radioClickListener = new RadioButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    onRadioButtonClicked(buttonView);
                }
            }
        };

        mOfxRadioButton.setOnCheckedChangeListener(radioClickListener);
        mQifRadioButton.setOnCheckedChangeListener(radioClickListener);
        mXmlRadioButton.setOnCheckedChangeListener(radioClickListener);
        mCsvTransactionsRadioButton.setOnCheckedChangeListener(radioClickListener);

        mSeparatorCommaButton.setOnCheckedChangeListener(radioClickListener);
        mSeparatorColonButton.setOnCheckedChangeListener(radioClickListener);
        mSeparatorSemicolonButton.setOnCheckedChangeListener(radioClickListener);

        ExportFormat defaultFormat = ExportFormat.of(defaultExportFormat.toUpperCase());
        switch (defaultFormat) {
            case QIF:
                mQifRadioButton.performClick();
                break;
            case OFX:
                mOfxRadioButton.performClick();
                break;
            case XML:
                mXmlRadioButton.performClick();
                break;
            case CSVT:
                mCsvTransactionsRadioButton.performClick();
                break;
        }

        if (GnuCashApplication.isDoubleEntryEnabled()) {
            mOfxRadioButton.setVisibility(View.GONE);
        } else {
            mXmlRadioButton.setVisibility(View.GONE);
        }

    }

    /**
     * Display the file path of the file where the export will be saved
     *
     * @param filepath Path to export file. If {@code null}, the view will be hidden and nothing displayed
     */
    private void setExportUriText(@Nullable String filepath) {
        if (TextUtils.isEmpty(filepath)) {
            mTargetUriTextView.setVisibility(View.GONE);
            mTargetUriTextView.setText("");
        } else {
            mTargetUriTextView.setText(filepath);
            mTargetUriTextView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Display the file path of the file where the export will be saved
     *
     * @param uri URI to export file. If {@code null}, the view will be hidden and nothing displayed
     */
    private void setExportUri(@Nullable Uri uri) {
        mExportParams.setExportLocation(uri);
        if (uri == null) {
            setExportUriText("");
        } else {
            setExportUriText(getDocumentName(uri, getContext()));
        }
    }

    /**
     * Open a chooser for user to pick a file to export to
     */
    private void selectExportFile() {
        String bookName = BooksDbAdapter.getInstance().getActiveBookDisplayName();
        String filename = Exporter.buildExportFilename(mExportParams.getExportFormat(), bookName);

        Intent createIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
            .setType("*/*")
            .addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_TITLE, filename);
        startActivityForResult(createIntent, REQUEST_EXPORT_FILE);
    }

    @Override
    public void onRecurrenceSet(String rrule) {
        mRecurrenceRule = rrule;
        String repeatString = getString(R.string.label_tap_to_create_schedule);

        if (!TextUtils.isEmpty(rrule)) {
            mEventRecurrence.parse(rrule);
            Context context = mRecurrenceTextView.getContext();
            repeatString = EventRecurrenceFormatter.getRepeatString(context, context.getResources(),
                mEventRecurrence, true);
        }
        mRecurrenceTextView.setText(repeatString);
    }

    /**
     * Callback for when the activity chooser dialog is completed
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case BackupPreferenceFragment.REQUEST_RESOLVE_CONNECTION:
                if (resultCode == Activity.RESULT_OK) {
                    BackupPreferenceFragment.mGoogleApiClient.connect();
                }
                break;

            case REQUEST_EXPORT_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        takePersistableUriPermission(requireContext(), data);
                        Uri location = data.getData();
                        setExportUri(location);
                    } else {
                        setExportUri(null);
                    }

                    if (mExportStarted)
                        startExport();
                }
                break;
        }
    }

    @Override
    public void onDateSet(CalendarDatePickerDialogFragment dialog, int year, int monthOfYear, int dayOfMonth) {
        Calendar cal = new GregorianCalendar(year, monthOfYear, dayOfMonth);
        mExportStartDate.setText(TransactionFormFragment.DATE_FORMATTER.print(cal.getTimeInMillis()));
        mExportStartCalendar.set(Calendar.YEAR, year);
        mExportStartCalendar.set(Calendar.MONTH, monthOfYear);
        mExportStartCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
    }

    @Override
    public void onTimeSet(RadialTimePickerDialogFragment dialog, int hourOfDay, int minute) {
        Calendar cal = new GregorianCalendar(0, 0, 0, hourOfDay, minute);
        mExportStartTime.setText(TransactionFormFragment.TIME_FORMATTER.print(cal.getTimeInMillis()));
        mExportStartCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        mExportStartCalendar.set(Calendar.MINUTE, minute);
    }
}

// Gotten from: https://stackoverflow.com/a/31720191
class OptionsViewAnimationUtils {

    public static void expand(final View v) {
        v.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        v.getLayoutParams().height = 0;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                    ? ViewGroup.LayoutParams.WRAP_CONTENT
                    : (int) (targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration((int) (3 * targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }

    public static void collapse(final View v) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration((int) (3 * initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }
}
