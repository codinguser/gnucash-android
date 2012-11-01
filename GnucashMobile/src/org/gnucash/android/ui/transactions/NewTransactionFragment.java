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

package org.gnucash.android.ui.transactions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.gnucash.android.R;
import org.gnucash.android.data.Money;
import org.gnucash.android.data.Transaction;
import org.gnucash.android.data.Transaction.TransactionType;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.ui.DatePickerDialogFragment;
import org.gnucash.android.ui.TimePickerDialogFragment;
import org.gnucash.android.ui.widget.WidgetConfigurationActivity;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Fragment for creating or editing transactions
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class NewTransactionFragment extends SherlockFragment implements 
	OnDateSetListener, OnTimeSetListener {
	
	/**
	 * Transactions database adapter
	 */
	private TransactionsDbAdapter mTransactionsDbAdapter;
	
	/**
	 * Holds database ID of transaction to be edited (if in edit mode)
	 */
	private long mTransactionId = 0;
	
	/**
	 * Transaction to be created/updated
	 */
	private Transaction mTransaction;
	
	/**
	 * Arguments key for database ID of transaction. 
	 * Is used to pass a transaction ID into a bundle or intent
	 */
	public static final String SELECTED_TRANSACTION_ID = "selected_transaction_id";
	
	/**
	 * Formats a {@link Date} object into a date string of the format dd MMM yyyy e.g. 18 July 2012
	 */
	public final static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd MMM yyyy");
	
	/**
	 * Formats a {@link Date} object to time string of format HH:mm e.g. 15:25
	 */
	public final static SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("HH:mm");
	
	/**
	 * Button for setting the transaction type, either credit or debit
	 */
	private ToggleButton mTransactionTypeButton;
	
	/**
	 * Input field for the transaction name (description)
	 */
	private EditText mNameEditText;
	
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
	 * Spinner for selecting the account for the transaction
	 */
	private Spinner mAccountsSpinner;
	
	/**
	 * Accounts database adapter. 
	 * Used for getting list of transactions to populate the {@link #mAccountsSpinner}
	 */
	private AccountsDbAdapter mAccountsDbAdapter;
	
	/**
	 * Cursor adapter for {@link #mAccountsSpinner}
	 */
	private SimpleCursorAdapter mCursorAdapter; 
	
	/**
	 * ActionBar Menu item for saving the transaction
	 * A transaction needs atleast a name and amount, only then is the save menu item enabled
	 */
	private MenuItem mSaveMenuItem;
	
	/**
	 * Cursor for accounts list spinner
	 */
	private Cursor mCursor;
	
	/**
	 * Create the view and retrieve references to the UI elements
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_new_transaction, container, false);
		
		mNameEditText = (EditText) v.findViewById(R.id.input_transaction_name);
		mDescriptionEditText = (EditText) v.findViewById(R.id.input_description);
		mDateTextView = (TextView) v.findViewById(R.id.input_date);
		mTimeTextView = (TextView) v.findViewById(R.id.input_time);
		mAmountEditText = (EditText) v.findViewById(R.id.input_transaction_amount);		
		mCurrencyTextView = (TextView) v.findViewById(R.id.currency_symbol);
		mAccountsSpinner = (Spinner) v.findViewById(R.id.input_accounts_spinner);
		mTransactionTypeButton = (ToggleButton) v.findViewById(R.id.input_transaction_type);
		
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		ActionBar actionBar = getSherlockActivity().getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.title_add_transaction);
		
		String[] from = new String[] {DatabaseHelper.KEY_NAME};
		int[] to = new int[] {android.R.id.text1};
		mAccountsDbAdapter = new AccountsDbAdapter(getActivity());
		mCursor = mAccountsDbAdapter.fetchAllAccounts();
		
		mCursorAdapter = new SimpleCursorAdapter(getActivity(), 
				android.R.layout.simple_spinner_item, 
				mCursor,
				from,
				to, 
				0);
		mCursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mAccountsSpinner.setAdapter(mCursorAdapter);
		mAccountsSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				String currencyCode = mAccountsDbAdapter.getCurrency(id);
				Currency currency = Currency.getInstance(currencyCode);
				mCurrencyTextView.setText(currency.getSymbol(Locale.getDefault()));
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// nothing to see here, move along				
			}
		});
		mTransactionId = getArguments().getLong(SELECTED_TRANSACTION_ID);
		mTransactionsDbAdapter = new TransactionsDbAdapter(getActivity());
		mTransaction = mTransactionsDbAdapter.getTransaction(mTransactionId);
		
		setListeners();
		if (mTransaction == null)
			initalizeViews();
		else
			initializeViewsWithTransaction();
		
	}

	/**
	 * Initialize views in the fragment with information from a transaction.
	 * This method is called if the fragment is used for editing a transaction
	 */
	private void initializeViewsWithTransaction(){
				
		mNameEditText.setText(mTransaction.getName());
		mTransactionTypeButton.setChecked(mTransaction.getTransactionType() == TransactionType.DEBIT);
		mAmountEditText.setText(mTransaction.getAmount().toPlainString());
		mCurrencyTextView.setText(mTransaction.getAmount().getCurrency().getSymbol(Locale.getDefault()));
		mDescriptionEditText.setText(mTransaction.getDescription());
		mDateTextView.setText(DATE_FORMATTER.format(mTransaction.getTimeMillis()));
		mTimeTextView.setText(TIME_FORMATTER.format(mTransaction.getTimeMillis()));
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTimeInMillis(mTransaction.getTimeMillis());
		mDate = mTime = cal;
				
		final long accountId = mAccountsDbAdapter.fetchAccountWithUID(mTransaction.getAccountUID());
		final int count = mCursorAdapter.getCount();
		for (int pos = 0; pos < count; pos++) {
			if (mCursorAdapter.getItemId(pos) == accountId)
				mAccountsSpinner.setSelection(pos);
		}
		
		String code = mTransactionsDbAdapter.getCurrencyCode(accountId);
		Currency accountCurrency = Currency.getInstance(code);
		mCurrencyTextView.setText(accountCurrency.getSymbol());
		
		ActionBar actionBar = getSherlockActivity().getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.title_edit_transaction);
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
			mTransactionTypeButton.setChecked(false);
		}
		
		final long accountId = getArguments().getLong(TransactionsListFragment.SELECTED_ACCOUNT_ID);
		final int count = mCursorAdapter.getCount();
		for (int pos = 0; pos < count; pos++) {
			if (mCursorAdapter.getItemId(pos) == accountId)
				mAccountsSpinner.setSelection(pos);
		}
		
		String code = Money.DEFAULT_CURRENCY_CODE;
		if (accountId != 0)
			code = mTransactionsDbAdapter.getCurrencyCode(accountId);
		
			
		Currency accountCurrency = Currency.getInstance(code);
		mCurrencyTextView.setText(accountCurrency.getSymbol(Locale.getDefault()));
	}
	
	/**
	 * Sets click listeners for the dialog buttons
	 */
	private void setListeners() {
		ValidationsWatcher validations = new ValidationsWatcher();
		mAmountEditText.addTextChangedListener(validations);
		mNameEditText.addTextChangedListener(validations);
		
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
				DialogFragment newFragment = new DatePickerDialogFragment(NewTransactionFragment.this, dateMillis);
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
				DialogFragment fragment = new TimePickerDialogFragment(NewTransactionFragment.this, timeMillis);
				fragment.show(ft, "time_dialog");
			}
		});
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
		
		long accountID 	= mAccountsSpinner.getSelectedItemId();
		Currency currency = Currency.getInstance(mTransactionsDbAdapter.getCurrencyCode(accountID));
		Money amount 	= new Money(amountBigd, currency);
		TransactionType type = mTransactionTypeButton.isChecked() ? TransactionType.DEBIT : TransactionType.CREDIT;
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
		
		mTransactionsDbAdapter.addTransaction(mTransaction);
		mTransactionsDbAdapter.close();
		
		//update widgets, if any
		WidgetConfigurationActivity.updateAllWidgets(getActivity().getApplicationContext());
		
		getSherlockActivity().getSupportFragmentManager().popBackStack();
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
		inflater.inflate(R.menu.new_transaction_actions, menu);
		mSaveMenuItem = menu.findItem(R.id.menu_save);
		//only initially enable if we are editing a transaction
		mSaveMenuItem.setEnabled(mTransactionId > 0);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//hide the keyboard if it is visible
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mNameEditText.getApplicationWindowToken(), 0);
		
		switch (item.getItemId()) {
		case R.id.menu_cancel:
			getSherlockActivity().getSupportFragmentManager().popBackStack();
			return true;
			
		case R.id.menu_save:
			saveNewTransaction();
			return true;

		default:
			return false;
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
	 * Validates that the name and amount of the transaction is provided
	 * before enabling the save button
	 * @author Ngewi Fet <ngewif@gmail.com>
	 *
	 */
	private class ValidationsWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {
			boolean valid = (mAmountEditText.getText().length() > 0);
			mSaveMenuItem.setEnabled(valid);
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			// TODO Auto-generated method stub
			
		}
		
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
						
//			String cleanString = stripCurrencyFormatting(s.toString());
//			if (cleanString.length() == 0)
//				return;
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
			
		}
		
	}
}
