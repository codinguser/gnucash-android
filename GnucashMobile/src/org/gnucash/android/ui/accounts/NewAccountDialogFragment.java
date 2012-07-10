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

package org.gnucash.android.ui.accounts;

import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.ui.transactions.TransactionsListFragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class NewAccountDialogFragment extends SherlockDialogFragment {
	private Button mSaveButton;
	private Button mCancelButton;
	private EditText mNameEditText;
	private AccountsDbAdapter mDbAdapter;
	private long mSelectedId = 0;
	private Account mAccount = null;
	
	public NewAccountDialogFragment(AccountsDbAdapter dbAdapter) {		
		mDbAdapter = dbAdapter;
	}
	
	static public NewAccountDialogFragment newInstance(AccountsDbAdapter dbAdapter){
		NewAccountDialogFragment f = new NewAccountDialogFragment(dbAdapter);
		
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
				mDbAdapter.addAccount(mAccount);
				((AccountsListFragment)getTargetFragment()).refreshList();				
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
