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

package org.gnucash.android.test.ui;

import java.util.Currency;
import java.util.List;

import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.data.Money;
import org.gnucash.android.data.Transaction;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.ui.accounts.AccountsActivity;
import org.gnucash.android.ui.accounts.AccountsListFragment;
import org.gnucash.android.ui.transactions.TransactionsActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListView;
import android.widget.TextView;

import com.jayway.android.robotium.solo.Solo;

public class AccountsActivityTest extends ActivityInstrumentationTestCase2<AccountsActivity> {
	private static final String DUMMY_ACCOUNT_CURRENCY_CODE = "USD";
	private static final String DUMMY_ACCOUNT_NAME = "Test account";
	private Solo mSolo;
	
	public AccountsActivityTest() {		
		super(AccountsActivity.class);
	}

	protected void setUp() throws Exception {
		Context context = getInstrumentation().getTargetContext();
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(context.getString(R.string.key_first_run), false);
		editor.commit();
		
		mSolo = new Solo(getInstrumentation(), getActivity());	
		
		AccountsDbAdapter adapter = new AccountsDbAdapter(getActivity());
		Account account = new Account(DUMMY_ACCOUNT_NAME);
		account.setCurrency(Currency.getInstance(DUMMY_ACCOUNT_CURRENCY_CODE));
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
		
		mSolo.clickOnText(getActivity().getString(R.string.btn_save));
		
		mSolo.waitForDialogToClose(1000);
		ListView lv = mSolo.getCurrentListViews().get(0);
		assertNotNull(lv);
		TextView v = (TextView) lv.getChildAt(0) //accounts are sorted alphabetically
				.findViewById(R.id.account_name);
		
		assertEquals("New Account", v.getText().toString());
		AccountsDbAdapter accAdapter = new AccountsDbAdapter(getActivity());
		
		List<Account> accounts = accAdapter.getAllAccounts();
		Account newestAccount = accounts.get(0);
		
		assertEquals("New Account", newestAccount.getName());		
		assertEquals(Money.DEFAULT_CURRENCY_CODE, newestAccount.getCurrency().getCurrencyCode());	
		
		accAdapter.close();		
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
				
		mSolo.clickOnText(getActivity().getString(R.string.btn_save));
		mSolo.waitForDialogToClose(2000);
		
		ListView lv = mSolo.getCurrentListViews().get(0);
		TextView tv = (TextView) lv.getChildAt(0)
				.findViewById(R.id.account_name);		
		assertEquals(editedAccountName, tv.getText().toString());
		
		AccountsDbAdapter accAdapter = new AccountsDbAdapter(getActivity());
		
		List<Account> accounts = accAdapter.getAllAccounts();
		Account latest = accounts.get(0);  //will be the first due to alphabetical sorting
		
		assertEquals(latest.getName(), "Edited Account");
		assertEquals(DUMMY_ACCOUNT_CURRENCY_CODE, latest.getCurrency().getCurrencyCode());	
		accAdapter.close();
	}
	
	public void testDisplayTransactionsList(){	
		Fragment fragment = getActivity()
				.getSupportFragmentManager()
				.findFragmentByTag(AccountsActivity.FRAGMENT_ACCOUNTS_LIST);
		((AccountsListFragment) fragment).refreshList();
		
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		mSolo.clickOnText(DUMMY_ACCOUNT_NAME);
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		
		String classname = mSolo.getAllOpenedActivities().get(1).getComponentName().getClassName();
		assertEquals(TransactionsActivity.class.getName(), classname);
		
		fragment = ((TransactionsActivity)mSolo.getAllOpenedActivities().get(1))
				.getSupportFragmentManager()
				.findFragmentByTag(TransactionsActivity.FRAGMENT_TRANSACTIONS_LIST);
		assertNotNull(fragment);
		
		assertNotNull(mSolo.getCurrentListViews());
		assertTrue(mSolo.getCurrentListViews().size() != 0);	
		
	}
		
	public void testDeleteAccount(){		
		Account acc = new Account("TO BE DELETED");
		acc.setUID("to-be-deleted");
		
		Transaction transaction = new Transaction("5.99", "hats");
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
		
		mSolo.waitForText(getActivity().getString(R.string.title_edit_account));
		
		mSolo.clickOnImage(2);
		
		mSolo.clickOnText(getActivity().getString(R.string.alert_dialog_ok_delete));
		//FIXME: deletion fails often because the confirmation dialog cannot be confirmed
		//we could also click on the button position, but it is different pre and post 4.0
		
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
