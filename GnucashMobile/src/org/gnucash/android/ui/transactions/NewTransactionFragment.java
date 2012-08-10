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

import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
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

public class NewTransactionFragment extends SherlockFragment implements 
	OnDateSetListener, OnTimeSetListener {
	
	private TransactionsDbAdapter mTransactionsDbAdapter;
	private long mTransactionId = 0;
	private Transaction mTransaction;
	
	public static final String SELECTED_TRANSACTION_ID = "selected_transaction_id";
	
	public final static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd MMM yyyy");
	public final static SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("HH:mm");
	
	private ToggleButton mTransactionTypeButton;
	private EditText mNameEditText;
	private EditText mAmountEditText;
	private TextView mCurrencyTextView;
	private EditText mDescriptionEditText;
	private TextView mDateTextView;
	private TextView mTimeTextView;		
	private Calendar mDate;
	private Calendar mTime;
	private Spinner mAccountsSpinner;
	private AccountsDbAdapter mAccountsDbAdapter;
	private SimpleCursorAdapter mCursorAdapter; 
	
	private MenuItem mSaveMenuItem;
	private Cursor mCursor;
	
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
		actionBar.setTitle(R.string.add_transaction);
		
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
		
		mTransactionId = getArguments().getLong(SELECTED_TRANSACTION_ID);
		mTransactionsDbAdapter = new TransactionsDbAdapter(getActivity());
		mTransaction = mTransactionsDbAdapter.getTransaction(mTransactionId);
		
		setListeners();
		if (mTransaction == null)
			initalizeViews();
		else
			initializeViewsWithTransaction();
		
	}

	private void initializeViewsWithTransaction(){
				
		mNameEditText.setText(mTransaction.getName());
		mTransactionTypeButton.setChecked(mTransaction.getTransactionType() == TransactionType.DEBIT);
		mAmountEditText.setText(mTransaction.getAmount().toPlainString());
		mCurrencyTextView.setText(mTransaction.getAmount().getCurrency().getSymbol());
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
		actionBar.setTitle(R.string.edit_transaction);
	}
	
	/**
	 * Binds the various views to the appropriate text
	 */
	private void initalizeViews() {
		Date time = new Date(System.currentTimeMillis()); 
		mDateTextView.setText(DATE_FORMATTER.format(time));
		mTimeTextView.setText(TIME_FORMATTER.format(time));
		mTime = mDate = Calendar.getInstance();
				
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
		mCurrencyTextView.setText(accountCurrency.getSymbol());
	}
	
	/**
	 * Sets click listeners for the dismiss buttons
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
					Money money = new Money(amountText).negate();
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
		String type 	= mTransactionTypeButton.getText().toString();
		
		if (mTransaction != null){
			mTransaction.setAmount(amount);
			mTransaction.setName(name);
			mTransaction.setTransactionType(TransactionType.valueOf(type));
		} else {
			mTransaction = new Transaction(amount, name, TransactionType.valueOf(type));
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
		imm.hideSoftInputFromWindow(mNameEditText.getWindowToken(), 0);
		
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

	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
		Calendar cal = new GregorianCalendar(year, monthOfYear, dayOfMonth);
		mDateTextView.setText(DATE_FORMATTER.format(cal.getTime()));
		mDate.set(Calendar.YEAR, year);
		mDate.set(Calendar.MONTH, monthOfYear);
		mDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
	}

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		Calendar cal = new GregorianCalendar(0, 0, 0, hourOfDay, minute);
		mTimeTextView.setText(TIME_FORMATTER.format(cal.getTime()));	
		mTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
		mTime.set(Calendar.MINUTE, minute);
	}
	
	public static String stripCurrencyFormatting(String s){
		//remove all currency formatting and anything else which is not a number
		return s.trim().replaceAll("\\D*", "");
	}
	
	public BigDecimal parseInputToDecimal(String amountString){
		String clean = stripCurrencyFormatting(amountString);
		BigDecimal amount = new BigDecimal(clean).setScale(2,
				RoundingMode.HALF_EVEN).divide(new BigDecimal(100), 2,
				RoundingMode.HALF_EVEN);
		if (mTransactionTypeButton.isChecked() && amount.doubleValue() > 0)
			amount = amount.negate();
		return amount;
	}

	private class ValidationsWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {
			boolean valid = (mNameEditText.getText().length() > 0) && 
					(mAmountEditText.getText().length() > 0);
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
