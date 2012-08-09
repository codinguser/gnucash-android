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
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
		account.setCurrency(Currency.getInstance(Locale.getDefault()));
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
		
		//Android 2.2 cannot handle this for some reason, use image instead
//		mSolo.clickOnActionBarItem(R.id.menu_add_transaction);
		mSolo.clickOnImage(2);
		mSolo.waitForText("Description");
		
		validateNewTransactionFields();
		
		//validate creation of transaction
		mSolo.enterText(0, "Lunch");
		mSolo.enterText(1, "899");
		//check that the amount is correctly converted in the input field
		String value = mSolo.getEditText(1).getText().toString();
		double actualValue = Money.parseToDecimal(value).doubleValue();
		assertEquals(-8.99, actualValue);
		
		int transactionsCount = getTranscationCount();
		
		//Android 2.2 cannot handle this for some reason
//		mSolo.clickOnActionBarItem(R.id.menu_save);	
		mSolo.clickOnImage(3);
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		validateTransactionListDisplayed();
		
		assertEquals(transactionsCount + 1, getTranscationCount());
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
	
	public void testBulkMoveTransactions(){
		Account account = new Account("Target");
		AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(getActivity());
		accountsDbAdapter.addAccount(account);
		
		int beforeOriginCount = accountsDbAdapter.getAccount(DUMMY_ACCOUNT_UID).getTransactionCount();
		
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		
		validateTransactionListDisplayed();
		
		mSolo.clickOnCheckBox(0);
		mSolo.clickOnImage(1);
		
		mSolo.waitForDialogToClose(2000);
		
		Spinner spinner = mSolo.getCurrentSpinners().get(0);
		mSolo.clickOnView(spinner);
		mSolo.clickOnText("Target");
		mSolo.clickOnButton(1);
//		mSolo.clickOnText(getActivity().getString(R.string.menu_move));
		
		mSolo.waitForDialogToClose(2000);
		
		int targetCount = accountsDbAdapter.getAccount(account.getUID()).getTransactionCount();		
		assertEquals(1, targetCount);
		
		int afterOriginCount = accountsDbAdapter.getAccount(DUMMY_ACCOUNT_UID).getTransactionCount();
		assertEquals(beforeOriginCount-1, afterOriginCount);
		
		accountsDbAdapter.close();
		
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
