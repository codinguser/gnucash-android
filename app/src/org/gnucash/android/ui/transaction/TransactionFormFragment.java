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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.widget.*;
import org.gnucash.android.R;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.Transaction.TransactionType;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.ui.transaction.dialog.DatePickerDialogFragment;
import org.gnucash.android.ui.transaction.dialog.TimePickerDialogFragment;
import org.gnucash.android.ui.UxArgument;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton.OnCheckedChangeListener;

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
	private ToggleButton mTransactionTypeButton;
	
	/**
	 * Input field for the transaction name (description)
	 */
	private AutoCompleteTextView mNameEditText;
	
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
	private EditText mDescriptionEditText;
	
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
     * Flag to note if the user has manually edited the amount of the transaction
     */
    boolean mAmountManuallyEdited = false;

    /**
     * The AccountType of the account to which this transaction belongs.
     * Used for determining the accounting rules for credits and debits
     */
    Account.AccountType mAccountType;

    /**
     * Spinner for marking the transaction as a recurring transaction
     */
    Spinner mRecurringTransactionSpinner;

	/**
	 * Create the view and retrieve references to the UI elements
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_new_transaction, container, false);
		
		mNameEditText           = (AutoCompleteTextView) v.findViewById(R.id.input_transaction_name);
		mDescriptionEditText    = (EditText) v.findViewById(R.id.input_description);
		mDateTextView           = (TextView) v.findViewById(R.id.input_date);
		mTimeTextView           = (TextView) v.findViewById(R.id.input_time);
		mAmountEditText         = (EditText) v.findViewById(R.id.input_transaction_amount);
		mCurrencyTextView       = (TextView) v.findViewById(R.id.currency_symbol);
		mTransactionTypeButton  = (ToggleButton) v.findViewById(R.id.input_transaction_type);
		mDoubleAccountSpinner   = (Spinner) v.findViewById(R.id.input_double_entry_accounts_spinner);

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
		mUseDoubleEntry = sharedPrefs.getBoolean(getString(R.string.key_use_double_entry), true);
		if (!mUseDoubleEntry){
			getView().findViewById(R.id.layout_double_entry).setVisibility(View.GONE);
		}

		//updateTransferAccountsList must only be called after creating mAccountsDbAdapter
		mAccountsDbAdapter = new AccountsDbAdapter(getActivity());
		updateTransferAccountsList();

        ArrayAdapter<CharSequence> recurrenceAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.recurrence_period_strings, android.R.layout.simple_spinner_item);
        recurrenceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRecurringTransactionSpinner.setAdapter(recurrenceAdapter);

        long transactionId = getArguments().getLong(UxArgument.SELECTED_TRANSACTION_ID);
		mTransactionsDbAdapter = new TransactionsDbAdapter(getActivity());
		mTransaction = mTransactionsDbAdapter.getTransaction(transactionId);

        final long accountId = getArguments().getLong(UxArgument.SELECTED_ACCOUNT_ID);
        mAccountType = mAccountsDbAdapter.getAccountType(accountId);
        toggleTransactionTypeState();

        setListeners();
		if (mTransaction == null)
			initalizeViews();
		else {
			if (mUseDoubleEntry && isInDoubleAccount()){
				mTransaction.setAmount(mTransaction.getAmount().negate());
			}
			initializeViewsWithTransaction();
		}

        initTransactionNameAutocomplete();
	}

    /**
     * Toggles the state transaction type button in response to the type of account.
     * This just changes what label is shown to the user, but basically the button in checked state still
     * represents a negative amount, and unchecked is positive. The CREDIT/DEBIT label depends on the account.
     * Different types of accounts handle CREDITS/DEBITS differently
     */
    private void toggleTransactionTypeState() {
        if (mAccountType.hasDebitNormalBalance()){
            mTransactionTypeButton.setTextOff(getString(R.string.label_debit));
            mTransactionTypeButton.setTextOn(getString(R.string.label_credit));
        } else {
            mTransactionTypeButton.setTextOff(getString(R.string.label_credit));
            mTransactionTypeButton.setTextOn(getString(R.string.label_debit));
        }
        mTransactionTypeButton.invalidate();
    }

    /**
     * Initializes the transaction name field for autocompletion with existing transaction names in the database
     */
    private void initTransactionNameAutocomplete() {
        final int[] to = new int[]{android.R.id.text1};
        final String[] from = new String[]{DatabaseHelper.KEY_NAME};

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                getActivity(), android.R.layout.simple_dropdown_item_1line,
                null, from, to, 0);

        adapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
            @Override
            public CharSequence convertToString(Cursor cursor) {
                final int colIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_NAME);
                return cursor.getString(colIndex);
            }
        });

        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence name) {
                return mTransactionsDbAdapter.fetchTransactionsStartingWith(name.toString());
            }
        });

        mNameEditText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                mTransaction = mTransactionsDbAdapter.getTransaction(id);
                mTransaction.setUID(UUID.randomUUID().toString());
                mTransaction.setExported(false);
                mTransaction.setTime(System.currentTimeMillis());
                long accountId = ((TransactionsActivity)getSherlockActivity()).getCurrentAccountID();
                mTransaction.setAccountUID(mTransactionsDbAdapter.getAccountUID(accountId));
                initializeViewsWithTransaction();
            }
        });

        mNameEditText.setAdapter(adapter);
    }

    /**
	 * Initialize views in the fragment with information from a transaction.
	 * This method is called if the fragment is used for editing a transaction
	 */
	private void initializeViewsWithTransaction(){
		mNameEditText.setText(mTransaction.getName());

        //FIXME: You need to revisit me when splits are introduced
        //checking the type button means the amount will be shown as negative (in red) to user

        mTransactionTypeButton.setChecked(mTransaction.getAmount().isNegative());

		if (!mAmountManuallyEdited){
            //when autocompleting, only change the amount if the user has not manually changed it already
            mAmountEditText.setText(mTransaction.getAmount().toPlainString());
        }
		mCurrencyTextView.setText(mTransaction.getAmount().getCurrency().getSymbol(Locale.getDefault()));
		mDescriptionEditText.setText(mTransaction.getDescription());
		mDateTextView.setText(DATE_FORMATTER.format(mTransaction.getTimeMillis()));
		mTimeTextView.setText(TIME_FORMATTER.format(mTransaction.getTimeMillis()));
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTimeInMillis(mTransaction.getTimeMillis());
		mDate = mTime = cal;
				
		if (mUseDoubleEntry){			
			if (isInDoubleAccount()){
				long accountId = mTransactionsDbAdapter.getAccountID(mTransaction.getAccountUID());
				setSelectedTransferAccount(accountId);
			} else {
				long doubleAccountId = mTransactionsDbAdapter.getAccountID(mTransaction.getDoubleEntryAccountUID());
				setSelectedTransferAccount(doubleAccountId);
			}
		}
		
		final long accountId = mTransactionsDbAdapter.getAccountID(mTransaction.getAccountUID());
		String code = mTransactionsDbAdapter.getCurrencyCode(accountId);
		Currency accountCurrency = Currency.getInstance(code);
		mCurrencyTextView.setText(accountCurrency.getSymbol());

        setSelectedRecurrenceOption();
    }

    /**
	 * Initialize views with default data for new transactions
	 */
	private void initalizeViews() {
		Date time = new Date(System.currentTimeMillis()); 
		mDateTextView.setText(DATE_FORMATTER.format(time));
		mTimeTextView.setText(TIME_FORMATTER.format(time));
		mTime = mDate = Calendar.getInstance();

		String typePref = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.key_default_transaction_type), "DEBIT");
		if (typePref.equals("CREDIT")){
            if (mAccountType.hasDebitNormalBalance())
                mTransactionTypeButton.setChecked(false);
            else
                mTransactionTypeButton.setChecked(true);
		} else { //DEBIT
            if (mAccountType.hasDebitNormalBalance())
                mTransactionTypeButton.setChecked(true);
            else
                mTransactionTypeButton.setChecked(false);
        }
				
		final long accountId = getArguments().getLong(UxArgument.SELECTED_ACCOUNT_ID);
		String code = Money.DEFAULT_CURRENCY_CODE;
		if (accountId != 0){
			code = mTransactionsDbAdapter.getCurrencyCode(accountId);
		}
		Currency accountCurrency = Currency.getInstance(code);
		mCurrencyTextView.setText(accountCurrency.getSymbol(Locale.getDefault()));

        if (mUseDoubleEntry){
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
            String[] recurrenceOptions = getResources().getStringArray(R.array.recurrence_period_millis);

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
		long accountId = ((TransactionsActivity)getActivity()).getCurrentAccountID();

		String conditions = "(" + DatabaseHelper.KEY_ROW_ID + " != " + accountId + " AND "
							+ DatabaseHelper.KEY_CURRENCY_CODE + " = '" + mAccountsDbAdapter.getCurrencyCode(accountId)
                            + "' AND " + DatabaseHelper.KEY_UID + " != '" + mAccountsDbAdapter.getGnuCashRootAccountUID()
                            + "' AND " + DatabaseHelper.KEY_PLACEHOLDER + " = 0"
                            + ")";

		mCursor = mAccountsDbAdapter.fetchAccountsOrderedByFullName(conditions);

        mCursorAdapter = new QualifiedAccountNameCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_item, mCursor);
		mCursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);		
		mDoubleAccountSpinner.setAdapter(mCursorAdapter);
	}
	
	/**
	 * Sets click listeners for the dialog buttons
	 */
	private void setListeners() {
		mAmountEditText.addTextChangedListener(new AmountInputFormatter());
		
		mTransactionTypeButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked){
					int red = getResources().getColor(R.color.debit_red);
					mTransactionTypeButton.setTextColor(red);
					mAmountEditText.setTextColor(red);		
					mCurrencyTextView.setTextColor(red);
				}
				else {
					int green = getResources().getColor(R.color.credit_green);
					mTransactionTypeButton.setTextColor(green);
					mAmountEditText.setTextColor(green);
					mCurrencyTextView.setTextColor(green);
				}
				String amountText = mAmountEditText.getText().toString();
				if (amountText.length() > 0){
					Money money = new Money(stripCurrencyFormatting(amountText)).divide(100).negate();
					mAmountEditText.setText(money.toPlainString()); //trigger an edit to update the number sign
				} 
			}
		});

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
                }, 500);
				break;
			}
		}
	}

    /**
     * Returns true if we are editing the transaction from within it's transfer account,
     * rather than the account in which the transaction was created
     * @return <code>true</code> if in transfer account, <code>false</code> otherwise
     */
	private boolean isInDoubleAccount(){
		long accountId = mTransactionsDbAdapter.getAccountID(mTransaction.getAccountUID());
		return ((TransactionsActivity)getActivity()).getCurrentAccountID() != accountId;
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

        Account.AccountType previousAccountType = mAccountType;
        mAccountType = accountsDbAdapter.getAccountType(newAccountId);
        toggleTransactionTypeState();

        //if the new account has a different credit/debit philosophy as the previous one, then toggle the button
        if (mAccountType.hasDebitNormalBalance() != previousAccountType.hasDebitNormalBalance()){
            mTransactionTypeButton.toggle();
        }

		updateTransferAccountsList();
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
		String name = mNameEditText.getText().toString();
		String description = mDescriptionEditText.getText().toString();
		BigDecimal amountBigd = parseInputToDecimal(mAmountEditText.getText().toString());
		
		long accountID 	= ((TransactionsActivity) getSherlockActivity()).getCurrentAccountID(); 		
		Currency currency = Currency.getInstance(mTransactionsDbAdapter.getCurrencyCode(accountID));
		Money amount 	= new Money(amountBigd, currency);
		TransactionType type;
        if (mAccountType.hasDebitNormalBalance()){
            type = amount.isNegative() ? TransactionType.CREDIT : TransactionType.DEBIT;
        } else
            type = amount.isNegative() ? TransactionType.DEBIT : TransactionType.CREDIT;
		if (mTransaction != null){
			mTransaction.setAmount(amount);
			mTransaction.setName(name);
			mTransaction.setTransactionType(type);
		} else {
			mTransaction = new Transaction(amount, name, type);
		}
		
		mTransaction.setAccountUID(mTransactionsDbAdapter.getAccountUID(accountID));
		mTransaction.setTime(cal.getTimeInMillis());
		mTransaction.setDescription(description);
		
		//set the double account
		if (mUseDoubleEntry){
			long doubleAccountId = mDoubleAccountSpinner.getSelectedItemId();
			//negate the transaction before saving if we are in the double account
			if (isInDoubleAccount()){
				mTransaction.setAmount(amount.negate());
				mTransaction.setAccountUID(mTransactionsDbAdapter.getAccountUID(doubleAccountId));
				mTransaction.setDoubleEntryAccountUID(mTransactionsDbAdapter.getAccountUID(accountID));
			} else {
				mTransaction.setAccountUID(mTransactionsDbAdapter.getAccountUID(accountID));
				mTransaction.setDoubleEntryAccountUID(mTransactionsDbAdapter.getAccountUID(doubleAccountId));
			}
		}
        //save the normal transaction first
        mTransactionsDbAdapter.addTransaction(mTransaction);

        //set up recurring transaction if requested
        int recurrenceIndex = mRecurringTransactionSpinner.getSelectedItemPosition();
        if (recurrenceIndex != 0) {
            String[] recurrenceOptions = getResources().getStringArray(R.array.recurrence_period_millis);
            long recurrencePeriodMillis = Long.parseLong(recurrenceOptions[recurrenceIndex]);
            long firstRunMillis = System.currentTimeMillis() + recurrencePeriodMillis;

            Transaction recurringTransaction = new Transaction(mTransaction, true);
            recurringTransaction.setRecurrencePeriod(recurrencePeriodMillis);
            long recurringTransactionId = mTransactionsDbAdapter.addTransaction(recurringTransaction);

            PendingIntent recurringPendingIntent = PendingIntent.getBroadcast(getActivity().getApplicationContext(),
                    (int)recurringTransactionId, Transaction.createIntent(mTransaction), PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, firstRunMillis,
                    recurrencePeriodMillis, recurringPendingIntent);
        }

		//update widgets, if any
		WidgetConfigurationActivity.updateAllWidgets(getActivity().getApplicationContext());
		
		finish();
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
		imm.hideSoftInputFromWindow(mNameEditText.getApplicationWindowToken(), 0);
		
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
	 * All non-digit information is removed
	 * @param s String to be stripped
	 * @return Stripped string with all non-digits removed
	 */
	public static String stripCurrencyFormatting(String s){
		//remove all currency formatting and anything else which is not a number
		return s.trim().replaceAll("\\D*", "");
	}
	
	/**
	 * Parse an input string into a {@link BigDecimal}
	 * This method expects the amount including the decimal part
	 * @param amountString String with amount information
	 * @return BigDecimal with the amount parsed from <code>amountString</code>
	 */
	public BigDecimal parseInputToDecimal(String amountString){
		String clean = stripCurrencyFormatting(amountString);
		//all amounts are input to 2 decimal places, so after removing decimal separator, divide by 100
		BigDecimal amount = new BigDecimal(clean).setScale(2,
				RoundingMode.HALF_EVEN).divide(new BigDecimal(100), 2,
				RoundingMode.HALF_EVEN);
		if (mTransactionTypeButton.isChecked() && amount.doubleValue() > 0)
			amount = amount.negate();
		return amount;
	}


	/**
	 * Captures input string in the amount input field and parses it into a formatted amount
	 * The amount input field allows numbers to be input sequentially and they are parsed
	 * into a string with 2 decimal places. This means inputting 245 will result in the amount
	 * of 2.45
	 * @author Ngewi Fet <ngewif@gmail.com>
	 */
	private class AmountInputFormatter implements TextWatcher {
		private String current = "0";
		
		@Override
		public void afterTextChanged(Editable s) {
			if (s.length() == 0)
				return;
			
			BigDecimal amount = parseInputToDecimal(s.toString());
			DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.getDefault());
			formatter.setMinimumFractionDigits(2);
			formatter.setMaximumFractionDigits(2);
			current = formatter.format(amount.doubleValue());
			
			mAmountEditText.removeTextChangedListener(this);
			mAmountEditText.setText(current);
			mAmountEditText.setSelection(current.length());
			mAmountEditText.addTextChangedListener(this);
			
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// nothing to see here, move along
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			// nothing to see here, move along
			mAmountManuallyEdited = true;
		}
		
	}
}
