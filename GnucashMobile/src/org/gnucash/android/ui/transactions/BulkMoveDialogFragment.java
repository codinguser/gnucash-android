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

package org.gnucash.android.ui.transactions;

import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.ui.MainActivity;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.Spinner;

public class BulkMoveDialogFragment extends DialogFragment {

	Spinner mDestinationAccountSpinner; 
	Button mOkButton; 
	Button mCancelButton; 
	
	long[] mTransactionIds = null;
	private AccountsDbAdapter mAccountsDbAdapter;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {		
		View v = inflater.inflate(R.layout.dialog_bulk_move, container, false);
		
		mDestinationAccountSpinner = (Spinner) v.findViewById(R.id.accounts_list_spinner);
		mOkButton = (Button) v.findViewById(R.id.btn_move);
		mCancelButton = (Button) v.findViewById(R.id.btn_cancel);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getDialog().getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		
		mTransactionIds = getArguments().getLongArray(TransactionsListFragment.SELECTED_TRANSACTION_IDS);
		
		String title = getActivity().getString(R.string.title_move_transactions, 
				mTransactionIds.length);
		getDialog().setTitle(title);
	
		mAccountsDbAdapter = new AccountsDbAdapter(getActivity());
		Cursor cursor = mAccountsDbAdapter.fetchAllAccounts();
		
		String[] from = new String[] {DatabaseHelper.KEY_NAME};
		int[] to = new int[] {android.R.id.text1};
		SimpleCursorAdapter mCursorAdapter = new SimpleCursorAdapter(getActivity(), 
				android.R.layout.simple_spinner_item, 
				cursor,
				from,
				to, 
				0);
		mCursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mDestinationAccountSpinner.setAdapter(mCursorAdapter);
		setListeners();
	}
	
	protected void setListeners(){
		mCancelButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		
		mOkButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (mTransactionIds == null){
					dismiss();
				}
				
				long accountId = mDestinationAccountSpinner.getSelectedItemId();
				TransactionsDbAdapter trxnAdapter = new TransactionsDbAdapter(getActivity());
				for (long trxnId : mTransactionIds) {
					trxnAdapter.moveTranscation(trxnId, accountId);
				}
				trxnAdapter.close();
				
				Fragment f = getActivity()
						.getSupportFragmentManager()
						.findFragmentByTag(MainActivity.FRAGMENT_TRANSACTIONS_LIST);
					
				((TransactionsListFragment)f).refreshList();
				dismiss();
			}			
		});
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mAccountsDbAdapter.close();
	}
}
