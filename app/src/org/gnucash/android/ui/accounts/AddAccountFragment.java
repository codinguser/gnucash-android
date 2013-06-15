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

package org.gnucash.android.ui.accounts;

import java.util.Arrays;
import java.util.Currency;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.data.Money;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.ui.transactions.TransactionsListFragment;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Fragment used for creating and editing accounts
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class AddAccountFragment extends SherlockFragment {

	/**
	 * EditText for the name of the account to be created/edited
	 */
	private EditText mNameEditText;
	
	/**
	 * Spinner for selecting the currency of the account
	 * Currencies listed are those specified by ISO 4217
	 */
	private Spinner mCurrencySpinner;
	
	/**
	 * Accounts database adapter
	 */
	private AccountsDbAdapter mAccountsDbAdapter;
	
	/**
	 * List of all currency codes (ISO 4217) supported by the app
	 */
	private List<String> mCurrencyCodes;
	
	/**
	 * Record ID of the account which was selected
	 * This is used if we are editing an account instead of creating one
	 */
	private long mSelectedAccountId = 0;
	
	/**
	 * Reference to account object which will be created at end of dialog
	 */
	private Account mAccount = null;

	private Cursor mCursor;
	
	private SimpleCursorAdapter mCursorAdapter;
	
	private Spinner mParentAccountSpinner;

	private CheckBox mParentCheckBox;

    private Spinner mAccountTypeSpinner;

	/**
	 * Default constructor
	 * Required, else the app crashes on screen rotation
	 */
	public AddAccountFragment() {
		//nothing to see here, move along
	}
	
	/**
	 * Construct a new instance of the dialog
	 * @param dbAdapter {@link AccountsDbAdapter} for saving the account
	 * @return New instance of the dialog fragment
	 */
	static public AddAccountFragment newInstance(AccountsDbAdapter dbAdapter){
		AddAccountFragment f = new AddAccountFragment();
		f.mAccountsDbAdapter = dbAdapter;
		return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
        if (mAccountsDbAdapter == null){
            mAccountsDbAdapter = new AccountsDbAdapter(getSherlockActivity());
        }
	}
	
	/**
	 * Inflates the dialog view and retrieves references to the dialog elements
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_new_account, container, false);
		getSherlockActivity().getSupportActionBar().setTitle(R.string.title_add_account);
		mCurrencySpinner = (Spinner) view.findViewById(R.id.input_currency_spinner);
		mNameEditText = (EditText) view.findViewById(R.id.edit_text_account_name);
		mNameEditText.requestFocus();

        mAccountTypeSpinner = (Spinner) view.findViewById(R.id.input_account_type_spinner);

		mParentAccountSpinner = (Spinner) view.findViewById(R.id.input_parent_account);
		mParentAccountSpinner.setEnabled(false);
		
		mParentCheckBox = (CheckBox) view.findViewById(R.id.checkbox);
		mParentCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mParentAccountSpinner.setEnabled(isChecked);
			}
		});

		return view;
	}
	

	/**
	 * Initializes the values of the views in the dialog
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
				getActivity(), 
				android.R.layout.simple_spinner_item, 
				getResources().getStringArray(R.array.currency_names));		
		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mCurrencySpinner.setAdapter(arrayAdapter);

        loadParentAccountList();
        loadAccountTypesList();

        mSelectedAccountId = getArguments().getLong(TransactionsListFragment.SELECTED_ACCOUNT_ID);
        if (mSelectedAccountId > 0) {
            mAccount = mAccountsDbAdapter.getAccount(mSelectedAccountId);
            getSherlockActivity().getSupportActionBar().setTitle(R.string.title_edit_account);
        }

        if (mAccount != null){
            initializeViewsWithAccount(mAccount);
        } else {
            initializeViews();
        }
	}

    /**
     * Initialize view with the properties of <code>account</code>.
     * This is applicable when editing an account
     * @param account Account whose fields are used to populate the form
     */
    private void initializeViewsWithAccount(Account account){
        if (account == null)
            throw new IllegalArgumentException("Account cannot be null");

        String currencyCode = account.getCurrency().getCurrencyCode();
        setSelectedCurrency(currencyCode);

        mNameEditText.setText(account.getName());
        long parentAccountId = mAccountsDbAdapter.getAccountID(account.getParentUID());
        setParentAccountSelection(parentAccountId);

        String[] accountTypeEntries = getResources().getStringArray(R.array.account_type_entries);
        int accountTypeIndex = Arrays.asList(accountTypeEntries).indexOf(account.getAccountType().name());
        mAccountTypeSpinner.setSelection(accountTypeIndex);
    }

    /**
     * Initialize views with defaults for new account
     */
    private void initializeViews(){
        setSelectedCurrency(Money.DEFAULT_CURRENCY_CODE);

        long parentAccountId = getArguments().getLong(AccountsListFragment.ARG_PARENT_ACCOUNT_ID);
        setParentAccountSelection(parentAccountId);

    }

    /**
     * Selects the currency with code <code>currencyCode</code> in the spinner
     * @param currencyCode ISO 4217 currency code to be selected
     */
    private void setSelectedCurrency(String currencyCode){
        mCurrencyCodes = Arrays.asList(getResources().getStringArray(R.array.currency_codes));
        if (mCurrencyCodes.contains(currencyCode)){
            mCurrencySpinner.setSelection(mCurrencyCodes.indexOf(currencyCode));
        }
    }

    /**
     * Selects the account with ID <code>parentAccountId</code> in the parent accounts spinner
     * @param parentAccountId Record ID of parent account to be selected
     */
    private void setParentAccountSelection(long parentAccountId){
        if (parentAccountId > 0){
            mParentCheckBox.setChecked(true);
            mParentAccountSpinner.setEnabled(true);
        } else
            return;

        for (int pos = 0; pos < mCursorAdapter.getCount(); pos++) {
            if (mCursorAdapter.getItemId(pos) == parentAccountId){
                mParentAccountSpinner.setSelection(pos);
                break;
            }
        }
    }

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {		
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.default_save_actions, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_save:
			saveAccount();
			return true;

		case R.id.menu_cancel:
			finishFragment();
			return true;
		}
		
		return false;
	}
	
	private void loadParentAccountList(){
		String condition = DatabaseHelper.KEY_ROW_ID + "!=" + mSelectedAccountId;
		mCursor = mAccountsDbAdapter.fetchAccounts(condition);
		if (mCursor.getCount() <= 0){
            final View view = getView();
            view.findViewById(R.id.layout_parent_account).setVisibility(View.GONE);
            view.findViewById(R.id.label_parent_account).setVisibility(View.GONE);
        }

		String[] from = new String[] {DatabaseHelper.KEY_NAME};
		int[] to = new int[] {android.R.id.text1};
		mCursorAdapter = new SimpleCursorAdapter(
				getActivity(), 
				android.R.layout.simple_spinner_item, 
				mCursor, from, to, 0);
		mCursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);		
		mParentAccountSpinner.setAdapter(mCursorAdapter);
	}

    private void loadAccountTypesList(){
        String[] accountTypes = getResources().getStringArray(R.array.account_type_entry_values);
        ArrayAdapter<String> accountTypesAdapter = new ArrayAdapter<String>(
                getActivity(), android.R.layout.simple_list_item_1, accountTypes);

        accountTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAccountTypeSpinner.setAdapter(accountTypesAdapter);

    }

	/**
	 * Finishes the fragment appropriately.
	 * Depends on how the fragment was loaded, it might have a backstack or not
	 */
	private void finishFragment() {
		InputMethodManager imm = (InputMethodManager) getSherlockActivity().getSystemService(
			      Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mNameEditText.getWindowToken(), 0);

        final String action = getActivity().getIntent().getAction();
        if (action != null && action.equals(Intent.ACTION_INSERT_OR_EDIT)){
            getActivity().setResult(Activity.RESULT_OK);
            getActivity().finish();
        } else {
		    getSherlockActivity().getSupportFragmentManager().popBackStack();
        }
	}
	
	@Override
	public void onDestroy() {
		super.onDestroyView();
		if (mCursor != null)
			mCursor.close();
		//do not close the database adapter. We got it from the activity, 
		//the activity will take care of it.
	}
	
	private void saveAccount() {
		if (mAccount == null){
			String name = getEnteredName();
			if (name == null || name.length() == 0){
				Toast.makeText(getSherlockActivity(), 
						R.string.toast_no_account_name_entered, 
						Toast.LENGTH_LONG).show();
				return;				
			}
			mAccount = new Account(getEnteredName());
		}
		else
			mAccount.setName(getEnteredName());
			
		String curCode = mCurrencyCodes.get(mCurrencySpinner
				.getSelectedItemPosition());
		mAccount.setCurrency(Currency.getInstance(curCode));

        int selectedAccountType = mAccountTypeSpinner.getSelectedItemPosition();
        String[] accountTypeEntries = getResources().getStringArray(R.array.account_type_entries);
        mAccount.setAccountType(Account.AccountType.valueOf(accountTypeEntries[selectedAccountType]));

		if (mParentCheckBox.isChecked()){
			long id = mParentAccountSpinner.getSelectedItemId();
			mAccount.setParentUID(mAccountsDbAdapter.getAccountUID(id));
		} else {
			mAccount.setParentUID(null);
		}
		
		if (mAccountsDbAdapter == null)
			mAccountsDbAdapter = new AccountsDbAdapter(getActivity());
		mAccountsDbAdapter.addAccount(mAccount);

		finishFragment();
	}
	
	/**
	 * Retrieves the name of the account which has been entered in the EditText
	 * @return
	 */
	public String getEnteredName(){
		return mNameEditText.getText().toString().trim();
	}

}
