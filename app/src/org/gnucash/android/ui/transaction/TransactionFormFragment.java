/*
 * Copyright (c) 2012 - 2014 Ngewi Fet <ngewif@gmail.com>
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

import android.support.v4.app.FragmentManager;
import android.widget.*;
import org.gnucash.android.R;
import org.gnucash.android.db.*;
import org.gnucash.android.model.*;
import org.gnucash.android.ui.transaction.dialog.DatePickerDialogFragment;
import org.gnucash.android.ui.transaction.dialog.SplitEditorDialogFragment;
import org.gnucash.android.ui.transaction.dialog.TimePickerDialogFragment;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.util.AmountInputFormatter;
import org.gnucash.android.ui.util.TransactionTypeToggleButton;
import org.gnucash.android.ui.widget.WidgetConfigurationActivity;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

/**
 * Fragment for creating or editing transactions
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class TransactionFormFragment extends SherlockFragment implements
	OnDateSetListener, OnTimeSetListener {

    public static final String TAG_SPLITS_EDITOR_FRAGMENT = "splits_editor";
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
	private SimpleCursorAdapter mCursorAdapter;

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
	private TransactionTypeToggleButton mTransactionTypeButton;

	/**
	 * Input field for the transaction name (description)
	 */
	private AutoCompleteTextView mDescriptionEditText;

	/**
	 * Input field for the transaction amount
	 */
	private EditText mAmountEditText;

	/**
	 * Field for the transaction currency.
	 * The transaction uses the currency of the account
	 */
	private TextView mCurrencyTextView;

	/**
	 * Input field for the transaction description (note)
	 */
	private EditText mNotesEditText;

	/**
	 * Input field for the transaction date
	 */
	private TextView mDateTextView;

	/**
	 * Input field for the transaction time
	 */
	private TextView mTimeTextView;

	/**
	 * {@link Calendar} for holding the set date
	 */
	private Calendar mDate;

	/**
	 * {@link Calendar} object holding the set time
	 */
	private Calendar mTime;

	/**
	 * Spinner for selecting the transfer account
	 */
	private Spinner mDoubleAccountSpinner;

    /**
     * Flag to note if double entry accounting is in use or not
     */
	private boolean mUseDoubleEntry;

    /**
     * The AccountType of the account to which this transaction belongs.
     * Used for determining the accounting rules for credits and debits
     */
    AccountType mAccountType;

    /**
     * Spinner for marking the transaction as a recurring transaction
     */
    Spinner mRecurringTransactionSpinner;

    private AmountInputFormatter mAmountInputFormatter;

    private Button mOpenSplitsButton;
    private String mAccountUID;

    private List<Split> mSplitsList = new ArrayList<Split>();

    /**
	 * Create the view and retrieve references to the UI elements
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_new_transaction, container, false);

		mDescriptionEditText = (AutoCompleteTextView) v.findViewById(R.id.input_transaction_name);
		mNotesEditText = (EditText) v.findViewById(R.id.input_description);
		mDateTextView           = (TextView) v.findViewById(R.id.input_date);
		mTimeTextView           = (TextView) v.findViewById(R.id.input_time);
		mAmountEditText         = (EditText) v.findViewById(R.id.input_transaction_amount);
		mCurrencyTextView       = (TextView) v.findViewById(R.id.currency_symbol);
		mTransactionTypeButton  = (TransactionTypeToggleButton) v.findViewById(R.id.input_transaction_type);
		mDoubleAccountSpinner   = (Spinner) v.findViewById(R.id.input_double_entry_accounts_spinner);
        mOpenSplitsButton       = (Button) v.findViewById(R.id.btn_open_splits);

        mRecurringTransactionSpinner = (Spinner) v.findViewById(R.id.input_recurring_transaction_spinner);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		ActionBar actionBar = getSherlockActivity().getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowTitleEnabled(false);

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mUseDoubleEntry = sharedPrefs.getBoolean(getString(R.string.key_use_double_entry), false);
		if (!mUseDoubleEntry){
			getView().findViewById(R.id.layout_double_entry).setVisibility(View.GONE);
            mOpenSplitsButton.setVisibility(View.GONE);
		}

        mAccountUID = getArguments().getString(UxArgument.SELECTED_ACCOUNT_UID);
		mAccountsDbAdapter = new AccountsDbAdapter(getActivity());
        mAccountType = mAccountsDbAdapter.getAccountType(mAccountUID);

        //updateTransferAccountsList must only be called after initializing mAccountsDbAdapter
		updateTransferAccountsList();

        ArrayAdapter<CharSequence> recurrenceAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.recurrence_period_strings, android.R.layout.simple_spinner_item);
        recurrenceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRecurringTransactionSpinner.setAdapter(recurrenceAdapter);

        String transactionUID = getArguments().getString(UxArgument.SELECTED_TRANSACTION_UID);
		mTransactionsDbAdapter = new TransactionsDbAdapter(getActivity());
		mTransaction = mTransactionsDbAdapter.getTransaction(transactionUID);

        mDoubleAccountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                if (mSplitsList.size() == 2){ //when handling simple transfer to one account
                    for (Split split : mSplitsList) {
                        if (!split.getAccountUID().equals(mAccountUID)){
                            split.setAccountUID(mAccountsDbAdapter.getAccountUID(id));
                        }
                        // else case is handled when saving the transactions
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //nothing to see here, move along
            }
        });

        setListeners();
		if (mTransaction == null) {
            initalizeViews();
            initTransactionNameAutocomplete();
        } else {
			initializeViewsWithTransaction();
		}


	}

    /**
     * Initializes the transaction name field for autocompletion with existing transaction names in the database
     */
    private void initTransactionNameAutocomplete() {
        final int[] to = new int[]{android.R.id.text1};
        final String[] from = new String[]{DatabaseSchema.TransactionEntry.COLUMN_DESCRIPTION};

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                getActivity(), android.R.layout.simple_dropdown_item_1line,
                null, from, to, 0);

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
                return mTransactionsDbAdapter.fetchTransactionsStartingWith(name.toString());
            }
        });

        mDescriptionEditText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                mTransaction = new Transaction(mTransactionsDbAdapter.getTransaction(id), true);
                mTransaction.setTime(System.currentTimeMillis());
                //we check here because next method will modify it and we want to catch user-modification
                boolean amountEntered = mAmountInputFormatter.isInputModified();
                initializeViewsWithTransaction();
                List<Split> splitList = mTransaction.getSplits();
                boolean isSplitPair = splitList.size() == 2 && splitList.get(0).isPairOf(splitList.get(1));
                if (isSplitPair){
                    mSplitsList.clear();
                    if (!amountEntered) //if user already entered an amount
                        mAmountEditText.setText(splitList.get(0).getAmount().toPlainString());
                } else {
                    if (amountEntered){ //if user entered own amount, clear loaded splits and use the user value
                        mSplitsList.clear();
                        setAmountEditViewVisible(View.VISIBLE);
                    } else {
                        if (mUseDoubleEntry) { //don't hide the view in single entry mode
                            setAmountEditViewVisible(View.GONE);
                        }
                    }
                }
                mTransaction = null; //we are creating a new transaction after all
            }
        });

        mDescriptionEditText.setAdapter(adapter);
    }

    /**
	 * Initialize views in the fragment with information from a transaction.
	 * This method is called if the fragment is used for editing a transaction
	 */
	private void initializeViewsWithTransaction(){
		mDescriptionEditText.setText(mTransaction.getDescription());

        mTransactionTypeButton.setAccountType(mAccountType);
        mTransactionTypeButton.setChecked(mTransaction.getBalance(mAccountUID).isNegative());

		if (!mAmountInputFormatter.isInputModified()){
            //when autocompleting, only change the amount if the user has not manually changed it already
            mAmountEditText.setText(mTransaction.getBalance(mAccountUID).toPlainString());
        }
		mCurrencyTextView.setText(mTransaction.getCurrency().getSymbol(Locale.getDefault()));
		mNotesEditText.setText(mTransaction.getNote());
		mDateTextView.setText(DATE_FORMATTER.format(mTransaction.getTimeMillis()));
		mTimeTextView.setText(TIME_FORMATTER.format(mTransaction.getTimeMillis()));
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTimeInMillis(mTransaction.getTimeMillis());
		mDate = mTime = cal;

        //if there are more than two splits (which is the default for one entry), then
        //disable editing of the transfer account. User should open editor
        if (mTransaction.getSplits().size() > 2) {
            setAmountEditViewVisible(View.GONE);
        } else {
            for (Split split : mTransaction.getSplits()) {
                //two splits, one belongs to this account and the other to another account
                if (mUseDoubleEntry && !split.getAccountUID().equals(mAccountUID)) {
                    setSelectedTransferAccount(mAccountsDbAdapter.getAccountID(split.getAccountUID()));
                }
            }
        }
        mSplitsList = new ArrayList<Split>(mTransaction.getSplits()); //we need a copy so we can modify with impunity
        mAmountEditText.setEnabled(mSplitsList.size() <= 2);

		String currencyCode = mTransactionsDbAdapter.getCurrencyCode(mAccountUID);
		Currency accountCurrency = Currency.getInstance(currencyCode);
		mCurrencyTextView.setText(accountCurrency.getSymbol());

        setSelectedRecurrenceOption();
    }

    private void setAmountEditViewVisible(int visibility) {
        getView().findViewById(R.id.layout_double_entry).setVisibility(visibility);
        mTransactionTypeButton.setVisibility(visibility);
    }

    /**
	 * Initialize views with default data for new transactions
	 */
	private void initalizeViews() {
		Date time = new Date(System.currentTimeMillis());
		mDateTextView.setText(DATE_FORMATTER.format(time));
		mTimeTextView.setText(TIME_FORMATTER.format(time));
		mTime = mDate = Calendar.getInstance();

        mTransactionTypeButton.setAccountType(mAccountType);
		String typePref = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.key_default_transaction_type), "DEBIT");
        mTransactionTypeButton.setChecked(TransactionType.valueOf(typePref));

		final String accountUID = getArguments().getString(UxArgument.SELECTED_ACCOUNT_UID);
		String code = Money.DEFAULT_CURRENCY_CODE;
		if (accountUID != null){
			code = mTransactionsDbAdapter.getCurrencyCode(mAccountUID);
		}
		Currency accountCurrency = Currency.getInstance(code);
		mCurrencyTextView.setText(accountCurrency.getSymbol());

        if (mUseDoubleEntry){
            long accountId = mAccountsDbAdapter.getID(mAccountUID);
            long defaultTransferAccountID = mAccountsDbAdapter.getDefaultTransferAccountID(accountId);
            if (defaultTransferAccountID > 0){
                setSelectedTransferAccount(defaultTransferAccountID);
            }
        }
	}

    /**
     * Initializes the recurrence spinner to the appropriate value from the transaction.
     * This is only used when the transaction is a recurrence transaction
     */
    private void setSelectedRecurrenceOption() {
        //init recurrence options
        final long recurrencePeriod = mTransaction.getRecurrencePeriod();
        if (recurrencePeriod > 0){
            String[] recurrenceOptions = getResources().getStringArray(R.array.key_recurrence_period_millis);

            int selectionIndex = 0;
            for (String recurrenceOption : recurrenceOptions) {
                if (recurrencePeriod == Long.parseLong(recurrenceOption))
                    break;
                selectionIndex++;
            }
            mRecurringTransactionSpinner.setSelection(selectionIndex);
        }
    }

    /**
     * Updates the list of possible transfer accounts.
     * Only accounts with the same currency can be transferred to
     */
	private void updateTransferAccountsList(){
		String accountUID = ((TransactionsActivity)getActivity()).getCurrentAccountUID();

		String conditions = "(" + DatabaseSchema.AccountEntry.COLUMN_UID + " != '" + accountUID
                            + "' AND " + DatabaseSchema.AccountEntry.COLUMN_CURRENCY + " = '" + mAccountsDbAdapter.getCurrencyCode(accountUID)
                            + "' AND " + DatabaseSchema.AccountEntry.COLUMN_UID + " != '" + mAccountsDbAdapter.getGnuCashRootAccountUID()
                            + "' AND " + DatabaseSchema.AccountEntry.COLUMN_PLACEHOLDER + " = 0"
                            + ")";

        if (mCursor != null) {
            mCursor.close();
        }
		mCursor = mAccountsDbAdapter.fetchAccountsOrderedByFullName(conditions);

        mCursorAdapter = new QualifiedAccountNameCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_item, mCursor);
		mCursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mDoubleAccountSpinner.setAdapter(mCursorAdapter);
	}

    /**
     * Opens the split editor dialog
     */
    private void openSplitEditor(){
        if (mAmountEditText.getText().toString().length() == 0){
            Toast.makeText(getActivity(), "Please enter an amount to split", Toast.LENGTH_SHORT).show();
            return;
        }
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        String baseAmountString;

        if (mTransaction == null){ //if we are creating a new transaction (not editing an existing one)
            BigDecimal enteredAmount = parseInputToDecimal(mAmountEditText.getText().toString());
            baseAmountString = enteredAmount.toPlainString();
        } else {
            Money biggestAmount = Money.createZeroInstance(mTransaction.getCurrencyCode());
            for (Split split : mTransaction.getSplits()) {
                if (split.getAmount().compareTo(biggestAmount) > 0)
                    biggestAmount = split.getAmount();
            }
            baseAmountString = biggestAmount.toPlainString();
        }

        SplitEditorDialogFragment splitEditorDialogFragment =
                SplitEditorDialogFragment.newInstance(baseAmountString);
        splitEditorDialogFragment.setTargetFragment(TransactionFormFragment.this, 0);
        splitEditorDialogFragment.show(fragmentManager, TAG_SPLITS_EDITOR_FRAGMENT);
    }
	/**
	 * Sets click listeners for the dialog buttons
	 */
	private void setListeners() {
        mAmountInputFormatter = new AmountInputFormatter(mAmountEditText);
        mAmountEditText.addTextChangedListener(mAmountInputFormatter);

        mOpenSplitsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSplitEditor();
            }
        });

		mTransactionTypeButton.setAmountFormattingListener(mAmountEditText, mCurrencyTextView);

		mDateTextView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				FragmentTransaction ft = getFragmentManager().beginTransaction();

				long dateMillis = 0;
				try {
					Date date = DATE_FORMATTER.parse(mDateTextView.getText().toString());
					dateMillis = date.getTime();
				} catch (ParseException e) {
					Log.e(getTag(), "Error converting input time to Date object");
				}
				DialogFragment newFragment = new DatePickerDialogFragment(TransactionFormFragment.this, dateMillis);
				newFragment.show(ft, "date_dialog");
			}
		});

		mTimeTextView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                long timeMillis = 0;
                try {
                    Date date = TIME_FORMATTER.parse(mTimeTextView.getText().toString());
                    timeMillis = date.getTime();
                } catch (ParseException e) {
                    Log.e(getTag(), "Error converting input time to Date object");
                }
                DialogFragment fragment = new TimePickerDialogFragment(TransactionFormFragment.this, timeMillis);
                fragment.show(ft, "time_dialog");
            }
        });
	}

    /**
     * Updates the spinner to the selected transfer account
     * @param accountId Database ID of the transfer account
     */
	private void setSelectedTransferAccount(long accountId){
		for (int pos = 0; pos < mCursorAdapter.getCount(); pos++) {
			if (mCursorAdapter.getItemId(pos) == accountId){
                final int position = pos;
                mDoubleAccountSpinner.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mDoubleAccountSpinner.setSelection(position);
                    }
                }, 200);
				break;
			}
		}
	}

    /**
     * Callback when the account in the navigation bar is changed by the user
     * @param newAccountId Database record ID of the newly selected account
     */
	public void onAccountChanged(long newAccountId){
		AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(getActivity());
		String currencyCode = accountsDbAdapter.getCurrencyCode(newAccountId);
		Currency currency = Currency.getInstance(currencyCode);
		mCurrencyTextView.setText(currency.getSymbol(Locale.getDefault()));

        mAccountType = accountsDbAdapter.getAccountType(newAccountId);
        mTransactionTypeButton.setAccountType(mAccountType);

		updateTransferAccountsList();

        accountsDbAdapter.close();
	}

	/**
	 * Collects information from the fragment views and uses it to create
	 * and save a transaction
	 */
	private void saveNewTransaction() {
		Calendar cal = new GregorianCalendar(
				mDate.get(Calendar.YEAR),
				mDate.get(Calendar.MONTH),
				mDate.get(Calendar.DAY_OF_MONTH),
				mTime.get(Calendar.HOUR_OF_DAY),
				mTime.get(Calendar.MINUTE),
				mTime.get(Calendar.SECOND));
		String description = mDescriptionEditText.getText().toString();
		String notes = mNotesEditText.getText().toString();
		BigDecimal amountBigd = parseInputToDecimal(mAmountEditText.getText().toString());

		String accountUID 	= ((TransactionsActivity) getSherlockActivity()).getCurrentAccountUID();
		Currency currency = Currency.getInstance(mTransactionsDbAdapter.getCurrencyCode(accountUID));
		Money amount 	= new Money(amountBigd, currency).absolute();

        //capture any edits which were done directly (not using split editor)
        if (mSplitsList.size() == 2 && mSplitsList.get(0).isPairOf(mSplitsList.get(1))) {
            //if it is a simple transfer where the editor was not used, then respect the button
            for (Split split : mSplitsList) {
                if (split.getAccountUID().equals(accountUID)){
                    split.setType(mTransactionTypeButton.getTransactionType());
                    split.setAmount(amount);
                } else {
                    split.setType(mTransactionTypeButton.getTransactionType().invert());
                    split.setAmount(amount);
                }
            }
        }

		if (mTransaction != null){
            if (!mUseDoubleEntry){
                //first remove old splits for this transaction, since there is only one split
                SplitsDbAdapter splitsDbAdapter = new SplitsDbAdapter(getActivity());
                for (Split split : mTransaction.getSplits()) {
                    splitsDbAdapter.deleteSplit(split.getUID());
                }
                splitsDbAdapter.close();

                Split split = new Split(amount, accountUID);
                split.setType(mTransactionTypeButton.getTransactionType());
                mTransaction.getSplits().clear();
                mTransaction.addSplit(split);
            } else {
                mTransaction.setSplits(mSplitsList);
            }
			mTransaction.setDescription(description);
		} else {
			mTransaction = new Transaction(description);
            if (!mUseDoubleEntry){
                Split split = new Split(amount, accountUID);
                split.setType(mTransactionTypeButton.getTransactionType());
                mSplitsList.clear();
                mSplitsList.add(split);
            }

            if (mSplitsList.isEmpty()) { //amount entered in the simple interface (not using splits Editor)
                Split split = new Split(amount, accountUID);
                split.setType(mTransactionTypeButton.getTransactionType());
                mTransaction.addSplit(split);

                String transferAcctUID;
                if (mUseDoubleEntry) {
                    long transferAcctId = mDoubleAccountSpinner.getSelectedItemId();
                    transferAcctUID = mAccountsDbAdapter.getAccountUID(transferAcctId);
                    mTransaction.addSplit(split.createPair(transferAcctUID));
                } else {
                      //TODO: enable this when we can hide certain accounts from the user
//                    transferAcctUID = mAccountsDbAdapter.getOrCreateImbalanceAccountUID(currency);
                }
            } else { //split editor was used to enter splits
                mTransaction.setSplits(mSplitsList);
            }
		}
        mTransaction.setCurrencyCode(mAccountsDbAdapter.getCurrencyCode(mAccountUID));
		mTransaction.setTime(cal.getTimeInMillis());
		mTransaction.setNote(notes);

        //save the normal transaction first
        mTransactionsDbAdapter.addTransaction(mTransaction);
        scheduleRecurringTransaction();


        //update widgets, if any
		WidgetConfigurationActivity.updateAllWidgets(getActivity().getApplicationContext());

		finish();
	}

    /**
     * Schedules a recurring transaction (if necessary) after the transaction has been saved
     * @see #saveNewTransaction()
     */
    private void scheduleRecurringTransaction() {
        //set up recurring transaction if requested
        int recurrenceIndex = mRecurringTransactionSpinner.getSelectedItemPosition();
        if (recurrenceIndex != 0) {
            String[] recurrenceOptions = getResources().getStringArray(R.array.key_recurrence_period_millis);
            long recurrencePeriodMillis = Long.parseLong(recurrenceOptions[recurrenceIndex]);
            Transaction recurringTransaction;
            if (mTransaction.getRecurrencePeriod() > 0) //if we are editing the recurring transaction itself...
                recurringTransaction = mTransaction;
            else
                recurringTransaction = new Transaction(mTransaction, true);

            recurringTransaction.setRecurrencePeriod(recurrencePeriodMillis);
            mTransactionsDbAdapter.scheduleTransaction(recurringTransaction);
        }
    }


    @Override
	public void onDestroyView() {
		super.onDestroyView();
		if (mCursor != null)
			mCursor.close();
		mAccountsDbAdapter.close();
		mTransactionsDbAdapter.close();
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
		case R.id.menu_cancel:
			finish();
			return true;

		case R.id.menu_save:
            if (mAmountEditText.getText().length() == 0){
                Toast.makeText(getActivity(), R.string.toast_transanction_amount_required, Toast.LENGTH_SHORT).show();
            } else
			    saveNewTransaction();
			return true;

		default:
			return false;
		}
	}

    /**
     * Called by the split editor fragment to notify of finished editing
     * @param splitList List of splits produced in the fragment
     */
    public void setSplitList(List<Split> splitList, List<String> removedSplitUIDs){
        mSplitsList = splitList;
        Money balance = Transaction.computeBalance(mAccountUID, mSplitsList);

        mAmountEditText.setText(balance.toPlainString());
        //once we set the split list, do not allow direct editing of the total
        if (mSplitsList.size() > 1){
            mAmountEditText.setEnabled(false);
            setAmountEditViewVisible(View.GONE);
        }

        SplitsDbAdapter splitsDbAdapter = new SplitsDbAdapter(getActivity());
        for (String removedSplitUID : removedSplitUIDs) {
            splitsDbAdapter.deleteRecord(splitsDbAdapter.getID(removedSplitUID));
        }
        splitsDbAdapter.close();
    }

    /**
     * Returns the list of splits currently in editing
     * @return List of splits
     */
    public List<Split> getSplitList(){
        return mSplitsList;
    }

	/**
	 * Finishes the fragment appropriately.
	 * Depends on how the fragment was loaded, it might have a backstack or not
	 */
	private void finish() {
		if (getActivity().getSupportFragmentManager().getBackStackEntryCount() == 0){
			//means we got here directly from the accounts list activity, need to finish
			getActivity().finish();
		} else {
			//go back to transactions list
			getSherlockActivity().getSupportFragmentManager().popBackStack();
		}
	}

	/**
	 * Callback when the date is set in the {@link DatePickerDialog}
	 */
	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
		Calendar cal = new GregorianCalendar(year, monthOfYear, dayOfMonth);
		mDateTextView.setText(DATE_FORMATTER.format(cal.getTime()));
		mDate.set(Calendar.YEAR, year);
		mDate.set(Calendar.MONTH, monthOfYear);
		mDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
	}

	/**
	 * Callback when the time is set in the {@link TimePickerDialog}
	 */
	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
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
        if (sign.equals("+") || sign.equals("-")){
            stripped = sign + stripped;
        }
		return stripped;
	}

	/**
	 * Parse an input string into a {@link BigDecimal}
	 * This method expects the amount including the decimal part
	 * @param amountString String with amount information
	 * @return BigDecimal with the amount parsed from <code>amountString</code>
	 */
	public static BigDecimal parseInputToDecimal(String amountString){
		String clean = stripCurrencyFormatting(amountString);
        if (clean.length() == 0) //empty string
                return BigDecimal.ZERO;
		//all amounts are input to 2 decimal places, so after removing decimal separator, divide by 100
        //TODO: Handle currencies with different kinds of decimal places
		BigDecimal amount = new BigDecimal(clean).setScale(2,
				RoundingMode.HALF_EVEN).divide(new BigDecimal(100), 2,
				RoundingMode.HALF_EVEN);
		return amount;
	}


}
