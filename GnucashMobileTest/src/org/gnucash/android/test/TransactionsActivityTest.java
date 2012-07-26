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

import java.util.Date;
import java.util.List;

import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.data.Money;
import org.gnucash.android.data.Transaction;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.ui.transactions.NewTransactionFragment;
import org.gnucash.android.ui.transactions.TransactionsActivity;
import org.gnucash.android.ui.transactions.TransactionsListFragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Spinner;

import com.jayway.android.robotium.solo.Solo;

public class TransactionsActivityTest extends
		ActivityInstrumentationTestCase2<TransactionsActivity> {
	private static final String DUMMY_ACCOUNT_UID = "transactions-account";
	private static final String DUMMY_ACCOUNT_NAME = "Transactions Account";
	private Solo mSolo;
	private Transaction mTransaction;
	
	public TransactionsActivityTest() {
		super(TransactionsActivity.class);		
	}
	
	@Override
	protected void setUp() throws Exception {		
		Account account = new Account(DUMMY_ACCOUNT_NAME);
		account.setUID(DUMMY_ACCOUNT_UID);
		
		mTransaction = new Transaction(9.99, "Pizza");
		mTransaction.setAccountUID(DUMMY_ACCOUNT_UID);
		mTransaction.setDescription("What up?");
		mTransaction.setTime(System.currentTimeMillis());
		
		account.addTransaction(mTransaction);		
		
		Context context = getInstrumentation().getTargetContext();
		AccountsDbAdapter adapter = new AccountsDbAdapter(context);
		long id = adapter.addAccount(account);
		adapter.close();
		

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.putExtra(TransactionsListFragment.SELECTED_ACCOUNT_ID, id);
		setActivityIntent(intent);
		
		mSolo = new Solo(getInstrumentation(), getActivity());			
	}
	
	private void validateTransactionListDisplayed(){
		Fragment fragment = getActivity()
				.getSupportFragmentManager()
				.findFragmentByTag(TransactionsActivity.FRAGMENT_TRANSACTIONS_LIST);
		
		assertNotNull(fragment);
	}
	
	private int getTranscationCount(){
		TransactionsDbAdapter transactionsDb = new TransactionsDbAdapter(getActivity());
		int count = transactionsDb.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID).size();
		transactionsDb.close();
		return count;
	}
	
	private void validateNewTransactionFields(){
		long timeMillis = System.currentTimeMillis();
		String expectedValue = NewTransactionFragment.DATE_FORMATTER.format(new Date(timeMillis));
		String actualValue = mSolo.getText(6).getText().toString();
		assertEquals(expectedValue, actualValue);
		
		expectedValue = NewTransactionFragment.TIME_FORMATTER.format(new Date(timeMillis));
		actualValue = mSolo.getText(7).getText().toString();
		assertEquals(expectedValue, actualValue);
		Spinner spinner = mSolo.getCurrentSpinners().get(0);
		
		actualValue = ((Cursor)spinner.getSelectedItem()).getString(DatabaseAdapter.COLUMN_NAME);
		assertEquals(DUMMY_ACCOUNT_NAME, actualValue);
	}
	
	public void testAddTransaction(){	
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		validateTransactionListDisplayed();
		
//		mSolo.clickOnActionBarItem(R.id.menu_add_transaction);
		mSolo.clickOnImage(2);
		mSolo.waitForText("Description");
		
		validateNewTransactionFields();
		
		//validate creation of transaction
		mSolo.enterText(0, "Lunch");
		mSolo.enterText(1, "899");
		//check that the amount is correctly converted in the input field
		String value = mSolo.getEditText(1).getText().toString();
		double actualValue = Double.parseDouble(Money.parse(value));
		assertEquals(-8.99, actualValue);
		
		int transactionsCount = getTranscationCount();
		
//		mSolo.clickOnActionBarItem(R.id.menu_save);	
		mSolo.clickOnImage(3);
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		validateTransactionListDisplayed();
		
		assertEquals(getTranscationCount(), transactionsCount + 1);
	}
	
	private void validateEditTransactionFields(Transaction transaction){
		
		String name = mSolo.getEditText(0).getText().toString();
		assertEquals(transaction.getName(), name);
		
		String amountString = mSolo.getEditText(1).getText().toString();
		Money amount = new Money(amountString);
		assertEquals(transaction.getAmount(), amount);
		
		String description = mSolo.getEditText(2).getText().toString();
		assertEquals(transaction.getDescription(), description);
		
		String expectedValue = NewTransactionFragment.DATE_FORMATTER.format(transaction.getTimeMillis());
		String actualValue = mSolo.getText(6).getText().toString();
		assertEquals(expectedValue, actualValue);
		
		expectedValue = NewTransactionFragment.TIME_FORMATTER.format(transaction.getTimeMillis());
		actualValue = mSolo.getText(7).getText().toString();
		assertEquals(expectedValue, actualValue);
		Spinner spinner = mSolo.getCurrentSpinners().get(0);
		
		actualValue = ((Cursor)spinner.getSelectedItem()).getString(DatabaseAdapter.COLUMN_UID);		
		assertEquals(transaction.getAccountUID(), actualValue);
	}
	
	public void testEditTransaction(){		
		//open transactions
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		
		validateTransactionListDisplayed();
		
		mSolo.clickOnText("Pizza");
		mSolo.waitForText("Description");
		
		validateEditTransactionFields(mTransaction);
				
		mSolo.enterText(0, "Pasta");
		mSolo.clickOnActionBarItem(R.id.menu_save);
		
		//if we see the text, then it was successfully created
		mSolo.waitForText("Pasta");
	}
	
	public void testDeleteTransaction(){
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		
		mSolo.clickOnCheckBox(0);
		mSolo.clickOnImage(2);
		
		AccountsDbAdapter accAdapter = new AccountsDbAdapter(getActivity());
		long id = accAdapter.getId(DUMMY_ACCOUNT_UID);
		TransactionsDbAdapter adapter = new TransactionsDbAdapter(getActivity());
		assertEquals(0, adapter.getTransactionsCount(id));
		
		accAdapter.close();
		adapter.close();
		
	}
	
	public void testIntentTransactionRecording(){
		TransactionsDbAdapter trxnAdapter = new TransactionsDbAdapter(getActivity());
		int beforeCount = trxnAdapter.getTransactionsCount(trxnAdapter.getAccountID(DUMMY_ACCOUNT_UID));
		Intent transactionIntent = new Intent(Intent.ACTION_INSERT);
		transactionIntent.setType(Transaction.MIME_TYPE);
		transactionIntent.putExtra(Intent.EXTRA_TITLE, "Power intents");
		transactionIntent.putExtra(Intent.EXTRA_TEXT, "Intents for sale");
		transactionIntent.putExtra(Transaction.EXTRA_AMOUNT, 4.99);
		transactionIntent.putExtra(Transaction.EXTRA_ACCOUNT_UID, DUMMY_ACCOUNT_UID);
		
		getActivity().sendBroadcast(transactionIntent);
		
		synchronized (mSolo) {
			try {
				mSolo.wait(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		int afterCount = trxnAdapter.getTransactionsCount(trxnAdapter.getAccountID(DUMMY_ACCOUNT_UID));
		
		assertEquals(beforeCount + 1, afterCount);
		
		List<Transaction> transactions = trxnAdapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID);
		
		for (Transaction transaction : transactions) {
			if (transaction.getName().equals("Power intents")){
				assertEquals("Intents for sale", transaction.getDescription());
				assertEquals(4.99, transaction.getAmount().asDouble());
			}
		}
		
		trxnAdapter.close();
	}

	@Override
	protected void tearDown() throws Exception {	
		AccountsDbAdapter adapter = new AccountsDbAdapter(getActivity());
		adapter.deleteAllAccounts();
		adapter.close();
		
		mSolo.finishOpenedActivities();
		
		super.tearDown();
	}
}
