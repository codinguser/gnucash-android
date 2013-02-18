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
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseAdapter;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.ui.accounts.AccountsActivity;
import org.gnucash.android.util.OnTransactionClickedListener;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.ResourceCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.SpinnerAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

/**
 * Activity for displaying, creating and editing transactions
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
		
	/**
	 * Flag which is used to determine if the activity is running or not. 
	 * Basically if onCreate has already been called or not. It is used
	 * to determine if to call addToBackStack() for fragments. When adding 
	 * the very first fragment, it should not be added to the backstack.
	 * @see #showTransactionEditFragment(Bundle)
	 */
	private boolean mActivityRunning = false;
	
	private OnNavigationListener mTransactionListNavigationListener = new OnNavigationListener() {

		  @Override
		  public boolean onNavigationItemSelected(int position, long itemId) {		    
			mAccountId = itemId;
		    FragmentManager fragmentManager = getSupportFragmentManager();
		    
		    //inform new accounts fragment that account was changed
		    NewTransactionFragment newTransactionsFragment = (NewTransactionFragment) fragmentManager
					.findFragmentByTag(FRAGMENT_NEW_TRANSACTION);	
		    if (newTransactionsFragment != null){
		    	newTransactionsFragment.onAccountChanged(itemId);
		    	//if we do not return, the transactions list fragment could also be found (although it's not visible)
		    	return true; 
		    }
		    
			TransactionsListFragment transactionsListFragment = (TransactionsListFragment) fragmentManager
					.findFragmentByTag(FRAGMENT_TRANSACTIONS_LIST);						
			if (transactionsListFragment != null) {
				transactionsListFragment.refreshList(itemId);
			}
				
		    return true;
		  }
	};

	private AccountsDbAdapter mAccountsDbAdapter;

	private SpinnerAdapter mSpinnerAdapter;
				
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_transactions);

		final Intent intent = getIntent();
		mAccountId = intent.getLongExtra(
				TransactionsListFragment.SELECTED_ACCOUNT_ID, -1);

		setupActionBarNavigation();
		
		if (intent.getAction().equals(Intent.ACTION_INSERT_OR_EDIT)) {
			long transactionId = intent.getLongExtra(
					NewTransactionFragment.SELECTED_TRANSACTION_ID, -1);
			if (transactionId <= 0) {
				createNewTransaction(mAccountId);
			} else {
				editTransaction(transactionId);
			}
		} else {	//load the transactions list					
			FragmentManager fragmentManager = getSupportFragmentManager();
			TransactionsListFragment transactionsListFragment = (TransactionsListFragment) fragmentManager
					.findFragmentByTag(FRAGMENT_TRANSACTIONS_LIST);
			
			FragmentTransaction fragmentTransaction = fragmentManager
					.beginTransaction();
			transactionsListFragment = new TransactionsListFragment();
			Bundle args = new Bundle();
			args.putLong(TransactionsListFragment.SELECTED_ACCOUNT_ID,
					mAccountId);
			transactionsListFragment.setArguments(args);
			Log.i(TAG, "Opening transactions for account id " +  mAccountId);

			fragmentTransaction.replace(R.id.fragment_container,
					transactionsListFragment, FRAGMENT_TRANSACTIONS_LIST);
						
			fragmentTransaction.commit();
		}

		// done creating, activity now running
		mActivityRunning = true;
	}

	/**
	 * Set up action bar navigation list and listener callbacks
	 */
	private void setupActionBarNavigation() {
		// set up spinner adapter for navigation list
		mAccountsDbAdapter = new AccountsDbAdapter(this);
		Cursor accountsCursor = mAccountsDbAdapter.fetchAllRecords();
		mSpinnerAdapter = new SimpleCursorAdapter(getSupportActionBar()
				.getThemedContext(), R.layout.sherlock_spinner_item,
				accountsCursor, new String[] { DatabaseHelper.KEY_NAME },
				new int[] { android.R.id.text1 }, 0);
		((ResourceCursorAdapter) mSpinnerAdapter)
				.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(mSpinnerAdapter,
				mTransactionListNavigationListener);
		
		updateNavigationSelection();
	}
	
	/**
	 * Updates the action bar navigation list selection to that of the current account
	 * whose transactions are being displayed/manipulated
	 */
	public void updateNavigationSelection() {
		// set the selected item in the spinner
		int i = 0;
		Cursor accountsCursor = mAccountsDbAdapter.fetchAllRecords();
		accountsCursor.moveToFirst();
		do {
			long id = accountsCursor.getLong(DatabaseAdapter.COLUMN_ROW_ID);			
			if (mAccountId == id) {
				getSupportActionBar().setSelectedNavigationItem(i);
				break;
			}
			++i;
		} while (accountsCursor.moveToNext());

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
	        	finish();
	        }
	        return true;

		default:
			return false;
		}
	}
		
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mAccountsDbAdapter.close();
	}
	
	/**
	 * Returns the database row ID of the current account
	 * @return Database row ID of the current account
	 */
	public long getCurrentAccountID(){
		return mAccountId;
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
	
	/**
	 * Loads the transaction insert/edit fragment and passes the arguments
	 * @param args Bundle arguments to be passed to the fragment
	 */
	private void showTransactionEditFragment(Bundle args){
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager
				.beginTransaction();
				
		NewTransactionFragment newTransactionFragment = new NewTransactionFragment();	
		newTransactionFragment.setArguments(args);
		
		fragmentTransaction.replace(R.id.fragment_container,
				newTransactionFragment, TransactionsActivity.FRAGMENT_NEW_TRANSACTION);
		
		if (mActivityRunning)
			fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}
	
	@Override
	public void createNewTransaction(long accountRowId) {
		Bundle args = new Bundle();
		args.putLong(TransactionsListFragment.SELECTED_ACCOUNT_ID, accountRowId);		
		showTransactionEditFragment(args);
	}

	@Override
	public void editTransaction(long transactionId){
		Bundle args = new Bundle();
		args.putLong(NewTransactionFragment.SELECTED_TRANSACTION_ID, transactionId);
		showTransactionEditFragment(args);
	}
}
