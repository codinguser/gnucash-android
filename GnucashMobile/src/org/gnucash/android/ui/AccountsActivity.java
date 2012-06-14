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

import org.gnucash.android.R;
import org.gnucash.android.util.OnAccountSelectedListener;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

/**
 * Displays the list of accounts and summary of transactions
 * 
 * @author Ngewi Fet <ngewif@gmail.com>
 * 
 */
public class AccountsActivity extends SherlockFragmentActivity implements OnAccountSelectedListener {

	private static final String FRAGMENT_ACCOUNTS_LIST 		= "accounts_list";
	private static final String FRAGMENT_TRANSACTIONS_LIST 	= "transactions_list";
	private static final String FRAGMENT_NEW_TRANSACTION 	= "new_transaction";
	
	static final int DIALOG_ADD_ACCOUNT = 0x01;

	protected static final String TAG = "AccountsActivity";	


	// private AccountsDbAdapter mAccountsDbAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_accounts);

		FragmentManager fragmentManager = getSupportFragmentManager();

		AccountsListFragment accountsListFragment = (AccountsListFragment) fragmentManager
				.findFragmentByTag(FRAGMENT_ACCOUNTS_LIST);

		if (accountsListFragment == null) {
			FragmentTransaction fragmentTransaction = fragmentManager
					.beginTransaction();
			fragmentTransaction.add(R.id.fragment_container,
					new AccountsListFragment(), FRAGMENT_ACCOUNTS_LIST);

			fragmentTransaction.commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.action_bar, menu);
		return true;
	}

	public void onNewAccountClick(View v) {
		AccountsListFragment accountFragment = (AccountsListFragment) getSupportFragmentManager()
				.findFragmentByTag(FRAGMENT_ACCOUNTS_LIST);
		if (accountFragment != null)
			accountFragment.showAddAccountDialog(0);
	}

	public void onNewTransactionClick(View v){
		createNewTransaction(0);
	}
	
	@Override
	public void accountSelected(long accountRowId, String accountName) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager
				.beginTransaction();
		TransactionsListFragment transactionsFragment = new TransactionsListFragment();
		Bundle args = new Bundle();
		args.putLong(TransactionsListFragment.SELECTED_ACCOUNT_ID, accountRowId);		
		args.putString(TransactionsListFragment.SELECTED_ACCOUNT_NAME, accountName);
		transactionsFragment.setArguments(args);
		Log.i(TAG, "Opening transactions for account " + accountName);
		fragmentTransaction.replace(R.id.fragment_container,
				transactionsFragment, FRAGMENT_TRANSACTIONS_LIST);

		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
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
				newTransactionFragment, FRAGMENT_NEW_TRANSACTION);

		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}

	public void editTransaction(long transactionId){
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager
				.beginTransaction();
		NewTransactionFragment newTransactionFragment = new NewTransactionFragment();
		Bundle args = new Bundle();
		args.putLong(NewTransactionFragment.SELECTED_TRANSACTION_ID, transactionId);		
		newTransactionFragment.setArguments(args);
		
		fragmentTransaction.replace(R.id.fragment_container,
				newTransactionFragment, FRAGMENT_NEW_TRANSACTION);

		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}
}