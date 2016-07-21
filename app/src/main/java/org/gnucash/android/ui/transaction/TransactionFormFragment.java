/*
 * Copyright (c) 2012 - 2015 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.ui.transaction;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;
import com.codetroopers.betterpickers.recurrencepicker.EventRecurrence;
import com.codetroopers.betterpickers.recurrencepicker.EventRecurrenceFormatter;
import com.codetroopers.betterpickers.recurrencepicker.RecurrencePickerDialogFragment;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.DatabaseAdapter;
import org.gnucash.android.db.adapter.ScheduledActionDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Recurrence;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;
import org.gnucash.android.ui.common.FormActivity;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.homescreen.WidgetConfigurationActivity;
import org.gnucash.android.ui.settings.PreferenceActivity;
import org.gnucash.android.ui.transaction.dialog.TransferFundsDialogFragment;
import org.gnucash.android.ui.util.RecurrenceParser;
import org.gnucash.android.ui.util.RecurrenceViewClickListener;
import org.gnucash.android.ui.util.widget.CalculatorEditText;
import org.gnucash.android.ui.util.widget.TransactionTypeSwitch;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Fragment for creating or editing transactions
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class TransactionFormFragment extends Fragment implements
        CalendarDatePickerDialogFragment.OnDateSetListener, RadialTimePickerDialogFragment.OnTimeSetListener,
        RecurrencePickerDialogFragment.OnRecurrenceSetListener, OnTransferFundsListener {

    private static final int REQUEST_SPLIT_EDITOR = 0x11;

    /**
	 * Transactions database adapter
	 */
	private TransactionsDbAdapter mTransactionsDbAdapter;

	/**
	 * Accounts database adapter
	 */
	private AccountsDbAdapter mAccountsDbAdapter;

	/**
	 * Adapter for transfer account spinner
	 */
	private QualifiedAccountNameCursorAdapter mAccountCursorAdapter;

	/**
	 * Cursor for transfer account spinner
	 */
	private Cursor mCursor;

    /**
	 * Transaction to be created/updated
	 */
	private Transaction mTransaction;

    /**
	 * Formats a {@link Date} object into a date string of the format dd MMM yyyy e.g. 18 July 2012
	 */
	public final static DateFormat DATE_FORMATTER = DateFormat.getDateInstance();

	/**
	 * Formats a {@link Date} object to time string of format HH:mm e.g. 15:25
	 */
	public final static DateFormat TIME_FORMATTER = DateFormat.getTimeInstance();

	/**
	 * Button for setting the transaction type, either credit or debit
	 */
	@Bind(R.id.input_transaction_type) TransactionTypeSwitch mTransactionTypeSwitch;

	/**
	 * Input field for the transaction name (description)
	 */
	@Bind(R.id.input_transaction_name) AutoCompleteTextView mDescriptionEditText;

	/**
	 * Input field for the transaction amount
	 */
	@Bind(R.id.input_transaction_amount) CalculatorEditText mAmountEditText;

	/**
	 * Field for the transaction currency.
	 * The transaction uses the currency of the account
	 */
	@Bind(R.id.currency_symbol) TextView mCurrencyTextView;

	/**
	 * Input field for the transaction description (note)
	 */
	@Bind(R.id.input_description) EditText mNotesEditText;

	/**
	 * Input field for the transaction date
	 */
	@Bind(R.id.input_date) TextView mDateTextView;

	/**
	 * Input field for the transaction time
	 */
	@Bind(R.id.input_time) TextView mTimeTextView;

	/**
	 * Spinner for selecting the transfer account
	 */
	@Bind(R.id.input_transfer_account_spinner) Spinner mTransferAccountSpinner;

    /**
     * Checkbox indicating if this transaction should be saved as a template or not
     */
    @Bind(R.id.checkbox_save_template) CheckBox mSaveTemplateCheckbox;

    @Bind(R.id.input_recurrence) TextView mRecurrenceTextView;

    /**
     * View which displays the calculator keyboard
     */
    @Bind(R.id.calculator_keyboard) KeyboardView mKeyboardView;

    /**
     * Open the split editor
     */
    @Bind(R.id.btn_split_editor) ImageView mOpenSplitEditor;

    /**
     * Layout for transfer account and associated views
     */
    @Bind(R.id.layout_double_entry) View mDoubleEntryLayout;

    /**
     * Flag to note if double entry accounting is in use or not
     */
	private boolean mUseDoubleEntry;

    /**
     * {@link Calendar} for holding the set date
     */
    private Calendar mDate;

    /**
     * {@link Calendar} object holding the set time
     */
    private Calendar mTime;


    private String mRecurrenceRule;
    private EventRecurrence mEventRecurrence = new EventRecurrence();

    private Account mAccount;

    private List<Split> mSplitsList = new ArrayList<>();

    private boolean mEditMode = false;

    /**
     * Split quantity which will be set from the funds transfer dialog
     */
    private Money mSplitQuantity;

    /**
	 * Create the view and retrieve references to the UI elements
	 */
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_transaction_form, container, false);
        ButterKnife.bind(this, v);
        mAmountEditText.bindListeners(mKeyboardView);
        mOpenSplitEditor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSplitEditor();
            }
        });
        return v;
	}

    /**
     * Starts the transfer of funds from one currency to another
     */
    private void startTransferFunds() {
        Currency fromCurrency = Currency.getInstance(mAccount.getCommodity().getCurrencyCode());
        long id = mTransferAccountSpinner.getSelectedItemId();
        String targetCurrency = mAccountsDbAdapter.getCurrencyCode(mAccountsDbAdapter.getUID(id));

        if (fromCurrency.equals(Currency.getInstance(targetCurrency))
                || !mAmountEditText.isInputModified()
                || mSplitQuantity != null) //if both accounts have same currency
            return;

        BigDecimal amountBigd = mAmountEditText.getValue();
        if (amountBigd.equals(BigDecimal.ZERO))
            return;
        Money amount 	= new Money(amountBigd, Commodity.getInstance(fromCurrency.getCurrencyCode())).abs();

        TransferFundsDialogFragment fragment
                = TransferFundsDialogFragment.getInstance(amount, targetCurrency, this);
        fragment.show(getFragmentManager(), "transfer_funds_editor");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mAmountEditText.bindListeners(mKeyboardView);
    }

    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);

		SharedPreferences sharedPrefs = PreferenceActivity.getActiveBookSharedPreferences(getActivity());
		mUseDoubleEntry = sharedPrefs.getBoolean(getString(R.string.key_use_double_entry), false);
		if (!mUseDoubleEntry){
			mDoubleEntryLayout.setVisibility(View.GONE);
            mOpenSplitEditor.setVisibility(View.GONE);
		}

        mAccountsDbAdapter = AccountsDbAdapter.getInstance();
        String accountUID = getArguments().getString(UxArgument.SELECTED_ACCOUNT_UID);
        assert accountUID != null;
        mAccount = mAccountsDbAdapter.getRecord(accountUID);

        String transactionUID = getArguments().getString(UxArgument.SELECTED_TRANSACTION_UID);
		mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();
		if (transactionUID != null) {
            mTransaction = mTransactionsDbAdapter.getRecord(transactionUID);
            mEditMode = true;
        } else {
            mTransaction = new Transaction("");
            mTransaction.setCommodity(mAccount.getCommodity());
            mEditMode = false;
        }

        setListeners();
        //updateTransferAccountsList must only be called after initializing mAccountsDbAdapter
        updateTransferAccountsList();
        mTransferAccountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            /**
             * Flag for ignoring first call to this listener.
             * The first call is during layout, but we want it called only in response to user interaction
             */
            boolean userInteraction = false;

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                if (mSplitsList.size() == 2) { //when handling simple transfer to one account
                    for (Split split : mSplitsList) {
                        if (!splitBelongsToThisTransactionAccount(split)) {
                            split.setAccountUID(mAccountsDbAdapter.getUID(id));
                        }
                        // else case is handled when saving the transactions
                    }
                }
                if (!userInteraction) {
                    userInteraction = true;
                    return;
                }
                startTransferFunds();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //nothing to see here, move along
            }
        });

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        assert actionBar != null;
//        actionBar.setSubtitle(mAccount.getFullName());

        initializeViews();

        if (mEditMode) {
            actionBar.setTitle(R.string.title_edit_transaction);
		} else {
            actionBar.setTitle(R.string.title_add_transaction);
            initTransactionNameAutocomplete();
        }

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
	}

    private boolean splitBelongsToThisTransactionAccount(Split split) {
        return split.getAccountUID().equals(mAccount.getUID());
    }

    /**
     * Extension of SimpleCursorAdapter which is used to populate the fields for the list items
     * in the transactions suggestions (auto-complete transaction description).
     */
    private class DropDownCursorAdapter extends SimpleCursorAdapter{

        public DropDownCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
            super(context, layout, c, from, to, 0);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);
            String transactionUID = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.COLUMN_UID));
            Money balance = TransactionsDbAdapter.getInstance().getBalance(transactionUID,
                                                                           mAccount.getUID());

            long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.COLUMN_TIMESTAMP));
            String dateString = DateUtils.formatDateTime(getActivity(), timestamp,
                    DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR);

            TextView secondaryTextView = (TextView) view.findViewById(R.id.secondary_text);
            secondaryTextView.setText(balance.formattedString() + " on " + dateString); //TODO: Extract string
        }
    }

    /**
     * Initializes the transaction name field for autocompletion with existing transaction names in the database
     */
    private void initTransactionNameAutocomplete() {
        final int[] to = new int[]{R.id.primary_text};
        final String[] from = new String[]{DatabaseSchema.TransactionEntry.COLUMN_DESCRIPTION};

        SimpleCursorAdapter adapter = new DropDownCursorAdapter(
                getActivity(), R.layout.dropdown_item_2lines, null, from, to);

        adapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
            @Override
            public CharSequence convertToString(Cursor cursor) {
                final int colIndex = cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.COLUMN_DESCRIPTION);
                return cursor.getString(colIndex);
            }
        });

        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence name) {
                return mTransactionsDbAdapter.fetchTransactionSuggestions(
                        name == null ? "" : name.toString(), mAccount.getUID());
            }
        });

        mDescriptionEditText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                mTransaction = new Transaction(mTransactionsDbAdapter.getRecord(id), true);
                mTransaction.setTime(System.currentTimeMillis());
                //we check here because next method will modify it and we want to catch user-modification
                boolean amountEntered = mAmountEditText.isInputModified();
                initializeViews();
                if (hasTransactionOnlyDefaultSplits(mTransaction.getSplits())) {
                    mSplitsList.clear();
                    if (!amountEntered) { //if user already entered an amount
                        mAmountEditText.setValue(
                                mTransaction.getSplits().get(0).getValue().asBigDecimal());
                    }
                } else {
                    if (amountEntered){ //if user entered own amount, clear loaded splits and use the user value
                        mSplitsList.clear();
                        setDoubleEntryViewsVisibility(View.VISIBLE);
                    } else {
                        if (mUseDoubleEntry) { //don't hide the view in single entry mode
                            setDoubleEntryViewsVisibility(View.GONE);
                        }
                    }
                }
                mEditMode = false;
            }
        });

        mDescriptionEditText.setAdapter(adapter);
    }

    /**
	 * Initialize views in the fragment with information from a transaction.
	 */
	private void initializeViews() {
		mDescriptionEditText.setText(mTransaction.getDescription());
        mDescriptionEditText.setSelection(mDescriptionEditText.getText().length());

        mTransactionTypeSwitch.setAccountType(mAccount.getAccountType());
        if (mEditMode) {
            mTransactionTypeSwitch.setChecked(
                    mTransaction.getBalance(mAccount.getUID()).isNegative());
        } else {
            String typePref = PreferenceActivity.getActiveBookSharedPreferences(getActivity())
                    .getString(getString(R.string.key_default_transaction_type), "DEBIT");
            mTransactionTypeSwitch.setChecked(TransactionType.valueOf(typePref));
        }

		if (mEditMode && !mAmountEditText.isInputModified()){
            //when autocompleting, only change the amount if the user has not manually changed it already
            mAmountEditText.setValue(mTransaction.getBalance(mAccount.getUID()).asBigDecimal());
        }
		mCurrencyTextView.setText(mTransaction.getCurrency().getSymbol());
		mNotesEditText.setText(mTransaction.getNote());
		mDateTextView.setText(DATE_FORMATTER.format(mTransaction.getTimeMillis()));
		mTimeTextView.setText(TIME_FORMATTER.format(mTransaction.getTimeMillis()));
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTimeInMillis(mTransaction.getTimeMillis());
		mDate = mTime = cal;

        //TODO: deep copy the split list. We need a copy so we can modify with impunity
        mSplitsList = new ArrayList<>(mTransaction.getSplits());
        toggleAmountInputEntryMode(mSplitsList.size() <= 2);

        if (mSplitsList.size() == 2){
            for (Split split : mSplitsList) {
                if (splitBelongsToThisTransactionAccount(split)) {
                    if (!split.getQuantity().getCurrency().equals(mTransaction.getCurrency())){
                        mSplitQuantity = split.getQuantity();
                    }
                }
            }
        }

        if (mUseDoubleEntry){
            if (mEditMode) {
                if (hasTransactionOnlyDefaultSplits(mSplitsList)) {
                    setSelectedTransferAccount(getTransferAccountIDFromSplits());
                }
            } else {
                long transferAccountID =
                        mAccountsDbAdapter.getDefaultTransferAccountIDFromParents(mAccount.getUID());
                if (transferAccountID > 0)
                    setSelectedTransferAccount(transferAccountID);
            }
        }

        if (mEditMode && !hasTransactionOnlyDefaultSplits(mSplitsList)) {
            //if there are other splits than the default ones, then
            //disable editing of the transfer account. User should open editor
            setDoubleEntryViewsVisibility(View.GONE);
        }

        mCurrencyTextView.setText(mAccount.getCommodity().getSymbol());
        mAmountEditText.setCommodity(mAccount.getCommodity());

        mSaveTemplateCheckbox.setChecked(mTransaction.isTemplate());
        String scheduledActionUID = getArguments().getString(UxArgument.SCHEDULED_ACTION_UID);
        if (scheduledActionUID != null && !scheduledActionUID.isEmpty()) {
            ScheduledAction scheduledAction = ScheduledActionDbAdapter.getInstance().getRecord(scheduledActionUID);
            mRecurrenceRule = scheduledAction.getRuleString();
            mEventRecurrence.parse(mRecurrenceRule);
            mRecurrenceTextView.setText(scheduledAction.getRepeatString());
        }
    }

    /**
     * Returns the transfer account ID this transaction is using by looking it
     * up in its splits.
     *
     * @return ID of the transfer account for the current transaction or -1 if none found
     */
    private long getTransferAccountIDFromSplits() {
        for (Split split : mTransaction.getSplits()) {
            if (!splitBelongsToThisTransactionAccount(split)) {
                return mAccountsDbAdapter.getID(split.getAccountUID());
            }
        }
        return -1;
    }

    /**
     * Returns true if the transaction only contains default splits.
     *
     * <p>A transaction for which the user only has set the amount and hasn't
     * added splits manually, has always two splits. One for the account where
     * the transaction has been created and another for the transfer account.</p>
     *
     * @param splits list of splits of the transaction
     * @return true if the transaction only contains default splits.
     * @see Split#isPairOf(Split)
     */
    private boolean hasTransactionOnlyDefaultSplits(List<Split> splits) {
        return splits.size() == 2 && splits.get(0).isPairOf(splits.get(1));
    }

    private void setDoubleEntryViewsVisibility(int visibility) {
        mDoubleEntryLayout.setVisibility(visibility);
        mTransactionTypeSwitch.setVisibility(visibility);
    }

    private void toggleAmountInputEntryMode(boolean enabled){
        if (enabled){
            mAmountEditText.setFocusable(true);
            mAmountEditText.bindListeners(mKeyboardView);
        } else {
            mAmountEditText.setFocusable(false);
            mAmountEditText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openSplitEditor();
                }
            });
        }
    }

    /**
     * Updates the list of possible transfer accounts.
     * Only accounts with the same currency can be transferred to
     */
	private void updateTransferAccountsList(){
        if (mCursor != null) {
            mCursor.close();
        }

        mCursor = mAccountsDbAdapter.getPossibleTransferAccounts(mAccount.getUID());
        mAccountCursorAdapter = new QualifiedAccountNameCursorAdapter(getActivity(), mCursor);
		mTransferAccountSpinner.setAdapter(mAccountCursorAdapter);
	}

    /**
     * Opens the split editor dialog
     */
    private void openSplitEditor(){
        if (mAmountEditText.getValue() == null){
            Toast.makeText(getActivity(), "Please enter an amount to split", Toast.LENGTH_SHORT).show();
            return;
        }

        String baseAmountString;

        if (mEditMode) {
            baseAmountString = getBiggestSplitAmount().toPlainString();
        } else {
            baseAmountString = mAmountEditText.getValue().toPlainString();
        }

        Intent intent = new Intent(getActivity(), FormActivity.class);
        intent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.SPLIT_EDITOR.name());
        intent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, mAccount.getUID());
        intent.putExtra(UxArgument.AMOUNT_STRING, baseAmountString);
        if (mSplitsList != null) {
            intent.putStringArrayListExtra(UxArgument.SPLIT_LIST, getSplitsAsCsvList());
        }
        startActivityForResult(intent, REQUEST_SPLIT_EDITOR);
    }


    /**
     * Returns the list of splits as a new list with the splits converted to CSV.
     *
     * @return the list of splits as a new list with the splits converted to CSV.
     */
    private @NonNull ArrayList<String> getSplitsAsCsvList() {
        ArrayList<String> splitStrings = new ArrayList<>();
        for (Split split : mSplitsList) {
            splitStrings.add(split.toCsv());
        }
        return splitStrings;
    }

    /**
     * Returns a Money object with the biggest amount of the splits of the current transaction.
     *
     * @return biggest amount of the splits of the current transaction
     */
    private Money getBiggestSplitAmount() {
        Money biggestAmount = Money.createZeroInstance(mTransaction.getCurrencyCode());
        for (Split split : mTransaction.getSplits()) {
            if (split.getValue().asBigDecimal().compareTo(biggestAmount.asBigDecimal()) > 0)
                biggestAmount = split.getValue();
        }
        return biggestAmount;
    }

    /**
	 * Sets click listeners for the dialog buttons
	 */
	private void setListeners() {
		mTransactionTypeSwitch.setAmountFormattingListener(mAmountEditText, mCurrencyTextView);

		mDateTextView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                long dateMillis = 0;
                try {
                    Date date = DATE_FORMATTER.parse(mDateTextView.getText().toString());
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
                        TransactionFormFragment.this,
                        year, monthOfYear, dayOfMonth);
                datePickerDialog.show(getFragmentManager(), "date_picker_fragment");
            }
        });

		mTimeTextView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                long timeMillis = 0;
                try {
                    Date date = TIME_FORMATTER.parse(mTimeTextView.getText().toString());
                    timeMillis = date.getTime();
                } catch (ParseException e) {
                    Log.e(getTag(), "Error converting input time to Date object");
                }

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(timeMillis);

                RadialTimePickerDialogFragment timePickerDialog = RadialTimePickerDialogFragment.newInstance(
                        TransactionFormFragment.this, calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE), true);
                timePickerDialog.show(getFragmentManager(), "time_picker_dialog_fragment");
            }
        });

        mRecurrenceTextView.setOnClickListener(new RecurrenceViewClickListener((AppCompatActivity) getActivity(), mRecurrenceRule, this));
	}

    /**
     * Updates the spinner to the selected transfer account
     * @param accountId Database ID of the transfer account
     */
	private void setSelectedTransferAccount(long accountId){
        int position = mAccountCursorAdapter.getPosition(mAccountsDbAdapter.getUID(accountId));
        if (position >= 0)
            mTransferAccountSpinner.setSelection(position);
	}

	/**
	 * Collects information from the fragment views and uses it to create
	 * and save a transaction
	 */
	private void saveNewTransaction() {
        mAmountEditText.getCalculatorKeyboard().hideCustomKeyboard();

        if (mAmountEditText.getValue() == null){ //if for whatever reason we cannot process the amount
            Toast.makeText(getActivity(), R.string.toast_transanction_amount_required,
                    Toast.LENGTH_SHORT).show();
            return;
        }

		Money amount = new Money(mAmountEditText.getValue(), mAccount.getCommodity()).abs();

        if (mSplitsList.size() == 1){ //means split editor was opened but no split was added
            String transferAcctUID = getSelectedTransferAccountUID();
            mSplitsList.add(mSplitsList.get(0).createPair(transferAcctUID));
        }

        //capture any edits which were done directly (not using split editor)
        if (hasTransactionOnlyDefaultSplits(mSplitsList)) {
            //if it is a simple transfer where the editor was not used, then respect the button
            for (Split split : mSplitsList) {
                if (splitBelongsToThisTransactionAccount(split)){
                    split.setType(mTransactionTypeSwitch.getTransactionType());
                    split.setValue(amount);
                    split.setQuantity(amount);
                } else {
                    split.setType(mTransactionTypeSwitch.getTransactionType().invert());
                    if (mSplitQuantity != null && hasTransferAccountDifferentCommodity())
                        split.setQuantity(mSplitQuantity);
                    else
                        split.setQuantity(amount);
                    split.setValue(amount);
                }
            }
        }

        mAccountsDbAdapter.beginTransaction();
        try {
            mTransaction.setDescription(mDescriptionEditText.getText().toString());

            if (mEditMode) {
                mTransaction.setSplits(mSplitsList);
            } else {

                //****************** amount entered in the simple interface (not using splits Editor) ************************
                if (mSplitsList.isEmpty()) {
                    Split split = new Split(amount, mAccount.getUID());
                    split.setType(mTransactionTypeSwitch.getTransactionType());
                    mTransaction.addSplit(split);

                    String transferAcctUID;
                    long transferAcctId = mTransferAccountSpinner.getSelectedItemId();
                    if (mUseDoubleEntry && transferAcctId > 0) {
                        transferAcctUID = mAccountsDbAdapter.getUID(transferAcctId);
                    } else {
                        Currency currency = Currency.getInstance(
                                mAccount.getCommodity().getCurrencyCode());
                        transferAcctUID = mAccountsDbAdapter.getOrCreateImbalanceAccountUID(currency);
                    }
                    Split pair = split.createPair(transferAcctUID);
                    if (mSplitQuantity != null && hasTransferAccountDifferentCommodity())
                        pair.setQuantity(mSplitQuantity);
                    else {
                        if (hasTransferAccountDifferentCommodity()){
                            startTransferFunds();
                            return;
                        }
                    }
                    mTransaction.addSplit(pair);
                } else { //split editor was used to enter splits
                    mTransaction.setSplits(mSplitsList);
                }
            }

            mTransaction.setCurrencyCode(mAccount.getCommodity().getCurrencyCode());
            mTransaction.setCommodity(mAccount.getCommodity());
            Calendar cal = new GregorianCalendar(
                    mDate.get(Calendar.YEAR),
                    mDate.get(Calendar.MONTH),
                    mDate.get(Calendar.DAY_OF_MONTH),
                    mTime.get(Calendar.HOUR_OF_DAY),
                    mTime.get(Calendar.MINUTE),
                    mTime.get(Calendar.SECOND));
            mTransaction.setTime(cal.getTimeInMillis());
            mTransaction.setNote(mNotesEditText.getText().toString());

            // set as not exported because we have just edited it
            mTransaction.setExported(false);
            // 1) mTransactions may be existing or non-existing
            // 2) when mTransactions exists in the db, the splits may exist or not exist in the db
            // So replace is chosen.
            mTransactionsDbAdapter.addRecord(mTransaction, DatabaseAdapter.UpdateMethod.replace);

            if (mSaveTemplateCheckbox.isChecked()) {//template is automatically checked when a transaction is scheduled
                if (mEditMode) {
                    scheduleRecurringTransaction(mTransaction.getUID());
                } else { // it was new transaction, so a new template
                    Transaction templateTransaction = new Transaction(mTransaction, true);
                    templateTransaction.setTemplate(true);
                    mTransactionsDbAdapter.addRecord(templateTransaction, DatabaseAdapter.UpdateMethod.replace);
                    scheduleRecurringTransaction(templateTransaction.getUID());
                }
            } else {
                String scheduledActionUID = getArguments().getString(UxArgument.SCHEDULED_ACTION_UID);
                if (scheduledActionUID != null){ //we were editing a schedule and it was turned off
                    ScheduledActionDbAdapter.getInstance().deleteRecord(scheduledActionUID);
                }
            }

            mAccountsDbAdapter.setTransactionSuccessful();
        }
        finally {
            mAccountsDbAdapter.endTransaction();
        }

        //update widgets, if any
		WidgetConfigurationActivity.updateAllWidgets(getActivity().getApplicationContext());

		finish(Activity.RESULT_OK);
	}

    /**
     * Returns true if the selected transfer account has a different commodity
     * from the transaction's account.
     *
     * @return true if the selected transfer account has a different commodity
     * from the transaction's account.
     */
    private boolean hasTransferAccountDifferentCommodity() {
        String accountCurrencyCode = mAccount.getCommodity().getCurrencyCode();
        String transferAccountCurrencyCode =
                mAccountsDbAdapter.getCurrencyCode(getSelectedTransferAccountUID());
        return !accountCurrencyCode.equals(transferAccountCurrencyCode);
    }

    /**
     * Returns the UID of the transaction account selected by the user.
     *
     * <p>If not in double entry mode, the UID of the imbalance account is returned instead.</p>
     *
     * @return UID of the transaction account selected by the user or of the imbalance
     * account, if not in double entry mode.
     */
    private String getSelectedTransferAccountUID() {
        String transferAcctUID;
        if (mUseDoubleEntry) {
            long transferAcctId = mTransferAccountSpinner.getSelectedItemId();
            transferAcctUID = mAccountsDbAdapter.getUID(transferAcctId);
        } else {
            Currency currency = Currency.getInstance(mAccount.getCommodity().getCurrencyCode());
            transferAcctUID = mAccountsDbAdapter.getOrCreateImbalanceAccountUID(currency);
        }
        return transferAcctUID;
    }

    /**
     * Schedules a recurring transaction (if necessary) after the transaction has been saved
     * @see #saveNewTransaction()
     */
    private void scheduleRecurringTransaction(String transactionUID) {
        ScheduledActionDbAdapter scheduledActionDbAdapter = ScheduledActionDbAdapter.getInstance();

        Recurrence recurrence = RecurrenceParser.parse(mEventRecurrence);

        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
        scheduledAction.setRecurrence(recurrence);

        String scheduledActionUID = getArguments().getString(UxArgument.SCHEDULED_ACTION_UID);

        if (scheduledActionUID != null) { //if we are editing an existing schedule
            if (recurrence == null){
                scheduledActionDbAdapter.deleteRecord(scheduledActionUID);
            } else {
                scheduledAction.setUID(scheduledActionUID);
                scheduledActionDbAdapter.updateRecurrenceAttributes(scheduledAction);
                Toast.makeText(getActivity(), "Updated transaction schedule", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (recurrence != null) {
                scheduledAction.setActionUID(transactionUID);
                scheduledActionDbAdapter.addRecord(scheduledAction, DatabaseAdapter.UpdateMethod.replace);
                Toast.makeText(getActivity(), R.string.toast_scheduled_recurring_transaction, Toast.LENGTH_SHORT).show();
            }
        }

    }


    @Override
	public void onDestroyView() {
		super.onDestroyView();
		if (mCursor != null)
			mCursor.close();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.default_save_actions, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//hide the keyboard if it is visible
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mDescriptionEditText.getApplicationWindowToken(), 0);

		switch (item.getItemId()) {
            case android.R.id.home:
                finish(Activity.RESULT_CANCELED);
                return true;

		case R.id.menu_save:
            if (canSave()){
                saveNewTransaction();
            } else {
                if (mAmountEditText.getValue() == null) {
                    Toast.makeText(getActivity(), R.string.toast_transanction_amount_required, Toast.LENGTH_SHORT).show();
                }
                if (mUseDoubleEntry && mTransferAccountSpinner.getCount() == 0){
                    Toast.makeText(getActivity(),
                            R.string.toast_disable_double_entry_to_save_transaction,
                            Toast.LENGTH_LONG).show();
                }
            }
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

    /**
     * Checks if the pre-requisites for saving the transaction are fulfilled
     * <p>The conditions checked are that a valid amount is entered and that a transfer account is set (where applicable)</p>
     * @return {@code true} if the transaction can be saved, {@code false} otherwise
     */
    private boolean canSave(){
        return (mAmountEditText.isInputValid())
                || (mUseDoubleEntry && mTransferAccountSpinner.getCount() > 0);
    }

    /**
     * Called by the split editor fragment to notify of finished editing
     * @param splitList List of splits produced in the fragment
     */
    public void setSplitList(List<Split> splitList, List<String> removedSplitUIDs){
        mSplitsList = splitList;
        Money balance = Transaction.computeBalance(mAccount.getUID(), mSplitsList);

        mAmountEditText.setValue(balance.asBigDecimal());
        mTransactionTypeSwitch.setChecked(balance.isNegative());
        //once we set the split list, do not allow direct editing of the total
        if (mSplitsList.size() > 1){
            toggleAmountInputEntryMode(false);
            setDoubleEntryViewsVisibility(View.GONE);
            mOpenSplitEditor.setVisibility(View.VISIBLE);
        }
    }


	/**
	 * Finishes the fragment appropriately.
	 * Depends on how the fragment was loaded, it might have a backstack or not
	 */
	private void finish(int resultCode) {
		if (getActivity().getSupportFragmentManager().getBackStackEntryCount() == 0){
            getActivity().setResult(resultCode);
			//means we got here directly from the accounts list activity, need to finish
			getActivity().finish();
		} else {
			//go back to transactions list
			getActivity().getSupportFragmentManager().popBackStack();
		}
	}

    @Override
    public void onDateSet(CalendarDatePickerDialogFragment calendarDatePickerDialog, int year, int monthOfYear, int dayOfMonth) {
        Calendar cal = new GregorianCalendar(year, monthOfYear, dayOfMonth);
        mDateTextView.setText(DATE_FORMATTER.format(cal.getTime()));
        mDate.set(Calendar.YEAR, year);
        mDate.set(Calendar.MONTH, monthOfYear);
        mDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
    }

    @Override
    public void onTimeSet(RadialTimePickerDialogFragment radialTimePickerDialog, int hourOfDay, int minute) {
        Calendar cal = new GregorianCalendar(0, 0, 0, hourOfDay, minute);
        mTimeTextView.setText(TIME_FORMATTER.format(cal.getTime()));
        mTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
        mTime.set(Calendar.MINUTE, minute);
    }

	/**
	 * Strips formatting from a currency string.
	 * All non-digit information is removed, but the sign is preserved.
	 * @param s String to be stripped
	 * @return Stripped string with all non-digits removed
	 */
	public static String stripCurrencyFormatting(String s){
        if (s.length() == 0)
            return s;
		//remove all currency formatting and anything else which is not a number
        String sign = s.trim().substring(0,1);
        String stripped = s.trim().replaceAll("\\D*", "");
        if (stripped.length() == 0)
            return "";
        if (sign.equals("+") || sign.equals("-")){
            stripped = sign + stripped;
        }
		return stripped;
	}

    @Override
    public void transferComplete(Money amount) {
        mSplitQuantity = amount;
    }

    @Override
    public void onRecurrenceSet(String rrule) {
        mRecurrenceRule = rrule;
        String repeatString = getString(R.string.label_tap_to_create_schedule);
        if (mRecurrenceRule != null){
            mEventRecurrence.parse(mRecurrenceRule);
            repeatString = EventRecurrenceFormatter.getRepeatString(getActivity(), getResources(), mEventRecurrence, true);

            //when recurrence is set, we will definitely be saving a template
            mSaveTemplateCheckbox.setChecked(true);
            mSaveTemplateCheckbox.setEnabled(false);
        } else {
            mSaveTemplateCheckbox.setEnabled(true);
            mSaveTemplateCheckbox.setChecked(false);
        }

        mRecurrenceTextView.setText(repeatString);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK){
            List<String> splits = data.getStringArrayListExtra(UxArgument.SPLIT_LIST);
            List<Split> splitList = new ArrayList<>();
            for (String splitCsv : splits) {
                splitList.add(Split.parseSplit(splitCsv));
            }
            List<String> removedSplits = data.getStringArrayListExtra(UxArgument.REMOVED_SPLITS);
            setSplitList(splitList, removedSplits);
        }
    }
}
