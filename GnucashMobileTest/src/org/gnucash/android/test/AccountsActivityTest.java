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

package org.gnucash.android.test;

import java.util.List;

import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.data.Transaction;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.ui.accounts.AccountsActivity;
import org.gnucash.android.ui.accounts.AccountsListFragment;
import org.gnucash.android.ui.transactions.TransactionsActivity;

import android.content.Intent;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListView;
import android.widget.TextView;

import com.jayway.android.robotium.solo.Solo;

public class AccountsActivityTest extends ActivityInstrumentationTestCase2<AccountsActivity> {
	private static final String DUMMY_ACCOUNT_NAME = "Test account";
	private Solo mSolo;
	
	public AccountsActivityTest() {		
		super(AccountsActivity.class);
	}

	protected void setUp() throws Exception {
		mSolo = new Solo(getInstrumentation(), getActivity());	
		
		AccountsDbAdapter adapter = new AccountsDbAdapter(getActivity());
		Account account = new Account(DUMMY_ACCOUNT_NAME);
		adapter.addAccount(account);
		adapter.close();
	}

	public void testDisplayAccountsList(){		
		//there should exist a listview of accounts
		Fragment fragment = getActivity()
				.getSupportFragmentManager()
				.findFragmentByTag(AccountsActivity.FRAGMENT_ACCOUNTS_LIST);
		assertNotNull(fragment);
		assertNotNull(mSolo.getCurrentListViews().get(0));		
	}
	
	public void testCreateAccount(){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			mSolo.clickOnActionBarItem(R.id.menu_add_account);
		else
			mSolo.clickOnImage(1);
		
		mSolo.waitForText("Create");
		mSolo.enterText(0, "New Account");
		
		mSolo.clickOnButton(1);
		
		mSolo.waitForDialogToClose(1000);
		ListView lv = mSolo.getCurrentListViews().get(0);
		assertNotNull(lv);
		TextView v = (TextView) lv.getChildAt(lv.getCount() - 1)
				.findViewById(R.id.account_name);
		
		assertEquals(v.getText().toString(), "New Account");
	}
	
	public void testEditAccount(){
		Fragment fragment = getActivity()
				.getSupportFragmentManager()
				.findFragmentByTag(AccountsActivity.FRAGMENT_ACCOUNTS_LIST);
		((AccountsListFragment) fragment).refreshList();
		
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		
		String editedAccountName = "Edited Account";
				
		mSolo.clickLongOnText(DUMMY_ACCOUNT_NAME);
		
		mSolo.clickOnImage(1);
		
		mSolo.clearEditText(0);
		mSolo.enterText(0, editedAccountName);
		
		mSolo.clickOnButton(1);
		
		mSolo.waitForDialogToClose(1000);
		
		ListView lv = mSolo.getCurrentListViews().get(0);
		TextView tv = (TextView) lv.getChildAt(lv.getCount() - 1)
				.findViewById(R.id.account_name);		
		assertEquals(editedAccountName, tv.getText().toString());
	}
	
	public void testDisplayTransactionsList(){	
		Fragment fragment = getActivity()
				.getSupportFragmentManager()
				.findFragmentByTag(AccountsActivity.FRAGMENT_ACCOUNTS_LIST);
		((AccountsListFragment) fragment).refreshList();
		
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		mSolo.clickOnText(DUMMY_ACCOUNT_NAME);
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		
		fragment = getActivity()
				.getSupportFragmentManager()
				.findFragmentByTag(TransactionsActivity.FRAGMENT_TRANSACTIONS_LIST);
		assertNotNull(fragment);
		
		assertNotNull(mSolo.getCurrentListViews());
		assertTrue(mSolo.getCurrentListViews().size() != 0);	
		
	}
		
	public void testDeleteAccount(){		
		Account acc = new Account("TO BE DELETED");
		acc.setUID("to-be-deleted");
		
		Transaction transaction = new Transaction(5.99, "hats");
		transaction.setAccountUID("to-be-deleted");
		acc.addTransaction(transaction);
		AccountsDbAdapter accDbAdapter = new AccountsDbAdapter(getActivity());
		accDbAdapter.addAccount(acc);		
		
		Fragment fragment = getActivity()
				.getSupportFragmentManager()
				.findFragmentByTag(AccountsActivity.FRAGMENT_ACCOUNTS_LIST);
		assertNotNull(fragment);
		
		((AccountsListFragment) fragment).refreshList();
		
		mSolo.clickLongOnText("TO BE DELETED");
		
		mSolo.waitForText(getActivity().getString(R.string.edit_accounts));
		
		mSolo.clickOnImage(2);
		
		mSolo.clickOnText("Delete");
		
		mSolo.waitForDialogToClose(1000);
		
		long id = accDbAdapter.fetchAccountWithUID("to-be-deleted");
		assertEquals(-1, id);
		
		TransactionsDbAdapter transDbAdapter = new TransactionsDbAdapter(getActivity());
		List<Transaction> transactions = transDbAdapter.getAllTransactionsForAccount("to-be-deleted");
		
		assertEquals(0, transactions.size());
		
		accDbAdapter.close();
		transDbAdapter.close();
	}
	
	public void testIntentAccountCreation(){
		Intent intent = new Intent(Intent.ACTION_INSERT);
		intent.putExtra(Intent.EXTRA_TITLE, "Intent Account");
		intent.putExtra(Intent.EXTRA_UID, "intent-account");
		intent.putExtra(Account.EXTRA_CURRENCY_CODE, "EUR");
		intent.setType(Account.MIME_TYPE);
		getActivity().sendBroadcast(intent);
		
		//give time for the account to be created
		synchronized (mSolo) {
			try {
				mSolo.wait(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
				
		AccountsDbAdapter dbAdapter = new AccountsDbAdapter(getActivity());
		Account account = dbAdapter.getAccount("intent-account");
		dbAdapter.close();
		assertNotNull(account);
		assertEquals("Intent Account", account.getName());
		assertEquals("intent-account", account.getUID());
		assertEquals("EUR", account.getCurrency().getCurrencyCode());
	}
	
	protected void tearDown() throws Exception {
		AccountsDbAdapter adapter = new AccountsDbAdapter(getActivity());
		adapter.deleteAllAccounts();
		adapter.close();
		
		mSolo.finishOpenedActivities();		
		super.tearDown();
	}

}
