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

import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.ui.accounts.AccountsActivity;
import org.gnucash.android.util.OnTransactionClickedListener;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

/**
 * Activing for displaying, creating and editing transactions
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class TransactionsActivity extends SherlockFragmentActivity implements 
	OnTransactionClickedListener{

	/**
	 * Logging tag
	 */
	protected static final String TAG = "AccountsActivity";
	
	/**
	 * Tag for {@link TransactionsListFragment}
	 * Can be used to check if the fragment is currently loaded
	 */
	public static final String FRAGMENT_TRANSACTIONS_LIST 	= "transactions_list";
	
	/**
	 * Tag for {@link NewTransactionFragment}
	 */
	public static final String FRAGMENT_NEW_TRANSACTION 	= "new_transaction";	
	
	/**
	 * Database ID of {@link Account} whose transactions are displayed 
	 */
	private long mAccountId 	= 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_transactions);

		final Intent intent = getIntent();
		mAccountId = intent.getLongExtra(
				TransactionsListFragment.SELECTED_ACCOUNT_ID, -1);	
		
		showTransactionsList();
		
		if (intent.getAction().equals(Intent.ACTION_INSERT_OR_EDIT)) {			
			long transactionId = intent.getLongExtra(
					NewTransactionFragment.SELECTED_TRANSACTION_ID, -1);
			if (transactionId <= 0) {
				createNewTransaction(mAccountId);
			} else {
				editTransaction(transactionId);
			}
		}
	}	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);
	        FragmentManager fm = getSupportFragmentManager();
	        if (fm.getBackStackEntryCount() > 0) {
	            fm.popBackStack();
	        } else {
	        	Intent accountsActivityIntent = new Intent(this, AccountsActivity.class);
	        	accountsActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        	accountsActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        	startActivity(accountsActivityIntent);
	        	overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
	        	finish();
	        }
	        return true;

		default:
			return false;
		}
	}
	
	/**
	 * Opens a fragment to create a new transaction. 
	 * Is called from the XML views
	 * @param v View which triggered this method
	 */
	public void onNewTransactionClick(View v){
		createNewTransaction(mAccountId);
	}
	
	/**
	 * Show list of transactions. Loads {@link TransactionsListFragment} 
	 */
	protected void showTransactionsList(){
		FragmentManager fragmentManager = getSupportFragmentManager();

		TransactionsListFragment transactionsListFragment = (TransactionsListFragment) fragmentManager
				.findFragmentByTag(FRAGMENT_TRANSACTIONS_LIST);

		if (transactionsListFragment == null) {
			FragmentTransaction fragmentTransaction = fragmentManager
					.beginTransaction();
			transactionsListFragment = new TransactionsListFragment();
			Bundle args = new Bundle();
			args.putLong(TransactionsListFragment.SELECTED_ACCOUNT_ID,
					mAccountId);
			transactionsListFragment.setArguments(args);
			Log.i(TAG, "Opening transactions for account id " +  mAccountId);

			fragmentTransaction.add(R.id.fragment_container,
					transactionsListFragment, FRAGMENT_TRANSACTIONS_LIST);
						
			fragmentTransaction.commit();
		}
	}
	
	@Override
	public void createNewTransaction(long accountRowId) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager
				.beginTransaction();
		NewTransactionFragment newTransactionFragment = new NewTransactionFragment();
		Bundle args = new Bundle();
		args.putLong(TransactionsListFragment.SELECTED_ACCOUNT_ID, accountRowId);		
		newTransactionFragment.setArguments(args);
		
		fragmentTransaction.replace(R.id.fragment_container,
				newTransactionFragment, TransactionsActivity.FRAGMENT_NEW_TRANSACTION);

		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}

	@Override
	public void editTransaction(long transactionId){
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager
				.beginTransaction();
		NewTransactionFragment newTransactionFragment = new NewTransactionFragment();
		Bundle args = new Bundle();
		args.putLong(NewTransactionFragment.SELECTED_TRANSACTION_ID, transactionId);		
		newTransactionFragment.setArguments(args);
		
		fragmentTransaction.replace(R.id.fragment_container,
				newTransactionFragment, TransactionsActivity.FRAGMENT_NEW_TRANSACTION);

		fragmentTransaction.addToBackStack(null);	
		fragmentTransaction.commit();
	}
}
