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

package org.gnucash.android.ui.transaction.dialog;

import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.transaction.TransactionsActivity;
import org.gnucash.android.ui.util.Refreshable;
import org.gnucash.android.ui.widget.WidgetConfigurationActivity;

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
import android.widget.Toast;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

/**
 * Dialog fragment for moving transactions from one account to another
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class BulkMoveDialogFragment extends DialogFragment {

	/**
	 * Spinner for selecting the account to move the transactions to
	 */
	Spinner mDestinationAccountSpinner; 
	
	/**
	 * Dialog positive button. Ok to moving the transactions
	 */
	Button mOkButton; 
	
	/**
	 * Cancel button
	 */
	Button mCancelButton; 
	
	/**
	 * Record IDs of the transactions to be moved
	 */
	long[] mTransactionIds = null;
	
	/**
	 * Account from which to move the transactions
	 */
	long mOriginAccountId = -1;
	
	/**
	 * Accounts database adapter
	 */
	private AccountsDbAdapter mAccountsDbAdapter;
	
	/**
	 * Creates the view and retrieves references to the dialog elements
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {		
		View v = inflater.inflate(R.layout.dialog_bulk_move, container, false);
		
		mDestinationAccountSpinner = (Spinner) v.findViewById(R.id.accounts_list_spinner);
		mOkButton = (Button) v.findViewById(R.id.btn_save);
		mOkButton.setText(R.string.btn_move);
		
		mCancelButton = (Button) v.findViewById(R.id.btn_cancel);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getDialog().getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		
		Bundle args = getArguments();
		mTransactionIds = args.getLongArray(UxArgument.SELECTED_TRANSACTION_IDS);
		mOriginAccountId = args.getLong(UxArgument.ORIGIN_ACCOUNT_ID);
		
		String title = getActivity().getString(R.string.title_move_transactions, 
				mTransactionIds.length);
		getDialog().setTitle(title);
		
		mAccountsDbAdapter = new AccountsDbAdapter(getActivity());
        String conditions = "(" + DatabaseSchema.AccountEntry._ID           + " != " + mOriginAccountId + " AND "
                + DatabaseSchema.AccountEntry.COLUMN_CURRENCY               + " = '" + mAccountsDbAdapter.getCurrencyCode(mOriginAccountId)
                + "' AND " + DatabaseSchema.AccountEntry.COLUMN_UID         + " != '" + mAccountsDbAdapter.getGnuCashRootAccountUID()
                + "' AND " + DatabaseSchema.AccountEntry.COLUMN_PLACEHOLDER + " = 0"
                + ")";
		Cursor cursor = mAccountsDbAdapter.fetchAccountsOrderedByFullName(conditions);

		SimpleCursorAdapter mCursorAdapter = new QualifiedAccountNameCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_item, cursor);
		mCursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mDestinationAccountSpinner.setAdapter(mCursorAdapter);
		setListeners();
	}
	
	/**
	 * Binds click listeners for the dialog buttons
	 */
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
				
				long dstAccountId = mDestinationAccountSpinner.getSelectedItemId();
				TransactionsDbAdapter trxnAdapter = new TransactionsDbAdapter(getActivity());
				if (!trxnAdapter.getCurrencyCode(dstAccountId).equals(trxnAdapter.getCurrencyCode(mOriginAccountId))){
					Toast.makeText(getActivity(), R.string.toast_incompatible_currency, Toast.LENGTH_LONG).show();
					return;
				}
                long accountId      = ((TransactionsActivity)getActivity()).getCurrentAccountID();
				for (long trxnId : mTransactionIds) {
					trxnAdapter.moveTranscation(trxnId, accountId, dstAccountId);
				}
				trxnAdapter.close();

				WidgetConfigurationActivity.updateAllWidgets(getActivity());
				((Refreshable)getTargetFragment()).refresh();
				dismiss();
			}			
		});
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}
}
