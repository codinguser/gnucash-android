/*
 * Written By: Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2012 Ngewi Fet
 *
 * This file is part of Gnucash for Android
 * 
 * Gnucash for Android is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, contact:
 *
 * Free Software Foundation           Voice:  +1-617-542-5942
 * 51 Franklin Street, Fifth Floor    Fax:    +1-617-542-2652
 * Boston, MA  02110-1301,  USA       gnu@gnu.org
 */

package org.gnucash.android.ui;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.data.Transaction;
import org.gnucash.android.data.Transaction.TransactionType;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.TransactionsDbAdapter;

import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
	final static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd MMM yyyy");
	final static SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("HH:mm");
	
	private ToggleButton mTransactionTypeButton;
	private TextView mDateTextView;
	private TextView mTimeTextView;	
	private EditText mAmountEditText;
	private Calendar mDate;
	private Calendar mTime;
	private Spinner mAccountsSpinner;
	private AccountsDbAdapter mAccountsDbAdapter;
	private SimpleCursorAdapter mCursorAdapter; 
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_new_transaction, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		ActionBar actionBar = getSherlockActivity().getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setTitle(R.string.add_transaction);
		
		mTransactionsDbAdapter = new TransactionsDbAdapter(getActivity());
		View v = getView();
		
		mDateTextView = (TextView) v.findViewById(R.id.input_date);
		mTimeTextView = (TextView) v.findViewById(R.id.input_time);
		mAmountEditText = (EditText) v.findViewById(R.id.input_transaction_amount);
		mAccountsSpinner = (Spinner) v.findViewById(R.id.input_accounts_spinner);
		mTransactionTypeButton = (ToggleButton) v.findViewById(R.id.input_transaction_type);
		
		bindViews();
		setListeners();
	}

	/**
	 * Binds the various views to the appropriate text
	 */
	private void bindViews() {
		Date time = new Date(System.currentTimeMillis()); 
		mDateTextView.setText(DATE_FORMATTER.format(time));
		mTimeTextView.setText(TIME_FORMATTER.format(time));
		mTime = mDate = Calendar.getInstance();
		
		String[] from = new String[] {DatabaseHelper.KEY_NAME};
		int[] to = new int[] {android.R.id.text1};
		mAccountsDbAdapter = new AccountsDbAdapter(getActivity());
		Cursor cursor = mAccountsDbAdapter.fetchAllAccounts();
		
		mCursorAdapter = new SimpleCursorAdapter(getActivity(), 
				android.R.layout.simple_spinner_item, 
				cursor,
				from,
				to, 
				0);
		mCursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mAccountsSpinner.setAdapter(mCursorAdapter);
		
		final long accountId = getArguments().getLong(TransactionsListFragment.SELECTED_ACCOUNT_ID);
		final int count = mCursorAdapter.getCount();
		for (int pos = 0; pos < count; pos++) {
			if (mCursorAdapter.getItemId(pos) == accountId)
				mAccountsSpinner.setSelection(pos);
		}
	}
	
	/**
	 * Sets click listeners for the dismiss buttons
	 */
	private void setListeners() {
		mAmountEditText.addTextChangedListener(new AmountInputWatcher());
		
		mTransactionTypeButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked){
					int red = getResources().getColor(R.color.debit_red);
					mTransactionTypeButton.setTextColor(red);
					mAmountEditText.setTextColor(red);					
				}
				else {
					int green = getResources().getColor(R.color.credit_green);
					mTransactionTypeButton.setTextColor(green);
					mAmountEditText.setTextColor(green);
				}
				mAmountEditText.setText(mAmountEditText.getText().toString()); //trigger an edit to update the number sign
			}
		});

		mDateTextView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				DialogFragment newFragment = new DatePickerDialogFragment(NewTransactionFragment.this);
				newFragment.show(ft, "date_dialog");
			}
		});
		
		mTimeTextView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				DialogFragment fragment = new TimePickerDialogFragment(NewTransactionFragment.this);
				fragment.show(ft, "time_dialog");
			}
		});
	}	
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mAccountsDbAdapter.close();
		mTransactionsDbAdapter.close();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.new_transaction_actions, menu);
	}
	
	private void saveNewTransaction() {
		String name = ((TextView)getView().findViewById(R.id.input_transaction_name)).getText().toString();
		String amountString = ((TextView)getView().findViewById(R.id.input_transaction_amount)).getText().toString();
		double amount = Double.parseDouble(stripCurrencyFormatting(amountString))/100;
		Calendar cal = new GregorianCalendar(
				mDate.get(Calendar.YEAR), 
				mDate.get(Calendar.MONTH), 
				mDate.get(Calendar.DAY_OF_MONTH), 
				mTime.get(Calendar.HOUR_OF_DAY), 
				mTime.get(Calendar.MINUTE), 
				mTime.get(Calendar.SECOND));
		
		long accountID = mAccountsSpinner.getSelectedItemId();
		Account account = mAccountsDbAdapter.getAccount(accountID);
		String type = mTransactionTypeButton.getText().toString();
		Transaction transaction = new Transaction(amount, name, TransactionType.valueOf(type));
		transaction.setAccountUID(account.getUID());
		transaction.setTime(cal.getTimeInMillis());
		
		mTransactionsDbAdapter.addTransaction(transaction);
		mTransactionsDbAdapter.close();
		
		getSherlockActivity().onBackPressed();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_cancel:
			getSherlockActivity().onBackPressed();
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
	
	private static String stripCurrencyFormatting(String s){

		//TODO: Generalize the code. Works only for $
		String symbol = Currency.getInstance(Locale.getDefault()).getSymbol();
		String regex = "[" + symbol + ",.-]";
		return s.replaceAll(regex, "");
	}
	
	private class AmountInputWatcher implements TextWatcher {
		private String current = null;
		
		@Override
		public void afterTextChanged(Editable s) {
			String cleanString = stripCurrencyFormatting(s.toString());
			if (cleanString.isEmpty())
				return;

			double parsed = Double.parseDouble(cleanString);

			mAmountEditText.removeTextChangedListener(this);

			String formattedString = NumberFormat.getCurrencyInstance().format(
					(parsed / 100));

			String prefix = mTransactionTypeButton.isChecked() ? " - " : "";

			current = prefix + formattedString;
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
