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
import android.preference.PreferenceManager;
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

public class NewAccountDialogFragment extends SherlockDialogFragment {
	private Button mSaveButton;
	private Button mCancelButton;
	private EditText mNameEditText;
	private Spinner mCurrencySpinner;
	
	private AccountsDbAdapter mDbAdapter;
	private List<String> mCurrencyCodes;
	
	private long mSelectedId = 0;
	private Account mAccount = null;
	
	public NewAccountDialogFragment() {
		
	}
	
	static public NewAccountDialogFragment newInstance(AccountsDbAdapter dbAdapter){
		NewAccountDialogFragment f = new NewAccountDialogFragment();
		f.mDbAdapter = dbAdapter;
		return f;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSelectedId = getArguments().getLong(TransactionsListFragment.SELECTED_ACCOUNT_ID);
				
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.dialog_new_account, container, false);
		getDialog().setTitle(R.string.add_account);	
		setStyle(STYLE_NORMAL, R.style.Sherlock___Theme_Dialog);
		mSaveButton = (Button) v.findViewById(R.id.btn_save);
		mCancelButton = (Button) v.findViewById(R.id.btn_cancel);
		mCurrencySpinner = (Spinner) v.findViewById(R.id.input_currency_spinner);
		mNameEditText = (EditText) v.findViewById(R.id.edit_text_account_name);
		mNameEditText.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        
        mNameEditText.addTextChangedListener(new NameFieldWatcher());
		       
        if (mSelectedId != 0) {
        	mAccount = mDbAdapter.getAccount(mSelectedId);
        	mNameEditText.setText(mAccount.getName());        	
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
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.currency_names));
		
		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mCurrencySpinner.setAdapter(arrayAdapter);
		
		String currencyCode = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.pref_default_currency), Money.DEFAULT_CURRENCY_CODE);
		if (mSelectedId != 0){
			//if we are editing an account instead of creating one
			currencyCode = mAccount.getCurrency().getCurrencyCode();
		}
		mCurrencyCodes = Arrays.asList(getResources().getStringArray(R.array.currency_codes));
		
		if (mCurrencyCodes.contains(currencyCode)){
			mCurrencySpinner.setSelection(mCurrencyCodes.indexOf(currencyCode));
		}		
	}
	
	public String getEnteredName(){
		return mNameEditText.getText().toString();
	}
	
	private class NameFieldWatcher implements TextWatcher {

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
