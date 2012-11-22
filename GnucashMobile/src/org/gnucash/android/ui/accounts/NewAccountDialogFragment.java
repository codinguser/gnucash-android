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

import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.data.Money;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.ui.transactions.TransactionsListFragment;
import org.gnucash.android.ui.widget.WidgetConfigurationActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockDialogFragment;

/**
 * Dialog fragment used for creating and editing accounts
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class NewAccountDialogFragment extends SherlockDialogFragment {
	/**
	 * Really? You want documentation for this too?
	 */
	private Button mSaveButton;
	
	/**
	 * Come on! It's a button, what else should I say?
	 */
	private Button mCancelButton;
	
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
	private AccountsDbAdapter mDbAdapter;
	
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
	
	/**
	 * Default constructor
	 * Required, else the app crashes on screen rotation
	 */
	public NewAccountDialogFragment() {
		//nothing to see here, move along
	}
	
	/**
	 * Construct a new instance of the dialog
	 * @param dbAdapter {@link AccountsDbAdapter} for saving the account
	 * @return New instance of the dialog fragment
	 */
	static public NewAccountDialogFragment newInstance(AccountsDbAdapter dbAdapter){
		NewAccountDialogFragment f = new NewAccountDialogFragment();
		f.mDbAdapter = dbAdapter;
		return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSelectedAccountId = getArguments().getLong(TransactionsListFragment.SELECTED_ACCOUNT_ID);				
	}
	
	/**
	 * Inflates the dialog view and retrieves references to the dialog elements
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.dialog_new_account, container, false);
		getDialog().setTitle(R.string.title_add_account);	
		setStyle(STYLE_NORMAL, R.style.Sherlock___Theme_Dialog);
		mSaveButton = (Button) v.findViewById(R.id.btn_save);
		mCancelButton = (Button) v.findViewById(R.id.btn_cancel);
		mCurrencySpinner = (Spinner) v.findViewById(R.id.input_currency_spinner);
		mNameEditText = (EditText) v.findViewById(R.id.edit_text_account_name);
		mNameEditText.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        
        mNameEditText.addTextChangedListener(new NameFieldWatcher());
		       
        if (mSelectedAccountId != 0) {
        	mAccount = mDbAdapter.getAccount(mSelectedAccountId);
        	mNameEditText.setText(mAccount.getName());  
        	getDialog().setTitle(R.string.title_edit_account);	
        }
		
		mSaveButton.setOnClickListener(new View.OnClickListener() {
			

			@Override
			public void onClick(View v) {
				if (mAccount == null)
					mAccount = new Account(getEnteredName());
				else
					mAccount.setName(getEnteredName());
								
				String curCode = mCurrencyCodes.get(mCurrencySpinner.getSelectedItemPosition());
				mAccount.setCurrency(Currency.getInstance(curCode));
				
				if (mDbAdapter == null)
					mDbAdapter = new AccountsDbAdapter(getActivity());
				mDbAdapter.addAccount(mAccount);
				
				((AccountsListFragment)getTargetFragment()).refreshList();
				
				WidgetConfigurationActivity.updateAllWidgets(getActivity().getApplicationContext());
				dismiss();				
			}
		});
		
		mCancelButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				NewAccountDialogFragment.this.dismiss();			
			}
		});
		return v;
	}
	
	/**
	 * Initializes the values of the views in the dialog
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.currency_names));
		
		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mCurrencySpinner.setAdapter(arrayAdapter);
		
		String currencyCode = Money.DEFAULT_CURRENCY_CODE;
		
		if (mSelectedAccountId != 0){
			//if we are editing an account instead of creating one
			currencyCode = mAccount.getCurrency().getCurrencyCode();
		}
		mCurrencyCodes = Arrays.asList(getResources().getStringArray(R.array.currency_codes));
		
		if (mCurrencyCodes.contains(currencyCode)){
			mCurrencySpinner.setSelection(mCurrencyCodes.indexOf(currencyCode));
		}		
	}
	
	/**
	 * Retrieves the name of the account which has been entered in the EditText
	 * @return
	 */
	public String getEnteredName(){
		return mNameEditText.getText().toString();
	}
	
	/**
	 * Validation text field watcher which enables the save button only when an account
	 * name has been provided
	 * @author Ngewi Fet <ngewif@gmail.com>
	 *
	 */
	private class NameFieldWatcher implements TextWatcher {

		/**
		 * Enable text if an account name has been entered, disable it otherwise
		 */
		@Override
		public void afterTextChanged(Editable s) {
			if (s.length() > 0)
				NewAccountDialogFragment.this.mSaveButton.setEnabled(true);
			else
				NewAccountDialogFragment.this.mSaveButton.setEnabled(false);
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			//nothing to see here, move along
			
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			// nothing to see here, move along
			
		}
		
	}

}
