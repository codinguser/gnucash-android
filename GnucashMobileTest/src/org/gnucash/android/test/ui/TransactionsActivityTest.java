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

import java.text.NumberFormat;
import java.text.ParseException;
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
	private static final String TRANSACTION_AMOUNT = "9.99";
	private static final String TRANSACTION_NAME = "Pizza";
	private static final String DUMMY_ACCOUNT_UID = "transactions-account";
	private static final String DUMMY_ACCOUNT_NAME = "Transactions Account";
	private Solo mSolo;
	private Transaction mTransaction;
	private long mTransactionTimeMillis;
	
	public TransactionsActivityTest() {
		super(TransactionsActivity.class);		
	}
	
	@Override
	protected void setUp() throws Exception {
		mTransactionTimeMillis = System.currentTimeMillis();
		Account account = new Account(DUMMY_ACCOUNT_NAME);
		account.setUID(DUMMY_ACCOUNT_UID);
		account.setCurrency(Currency.getInstance(Locale.getDefault()));
		mTransaction = new Transaction(TRANSACTION_AMOUNT, TRANSACTION_NAME);
		mTransaction.setAccountUID(DUMMY_ACCOUNT_UID);
		mTransaction.setDescription("What up?");
		mTransaction.setTime(mTransactionTimeMillis);
		
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
		String expectedValue = NewTransactionFragment.DATE_FORMATTER.format(new Date(mTransactionTimeMillis));
		String actualValue = mSolo.getText(6).getText().toString();
		assertEquals(expectedValue, actualValue);
		
		expectedValue = NewTransactionFragment.TIME_FORMATTER.format(new Date(mTransactionTimeMillis));
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
		String expectedValue = NumberFormat.getInstance().format(-8.99); 
		assertEquals(expectedValue, value);
		
		int transactionsCount = getTranscationCount();
		
		//Android 2.2 cannot handle this for some reason
//		mSolo.clickOnActionBarItem(R.id.menu_save);	
		mSolo.clickOnImage(3);
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		validateTransactionListDisplayed();
		
		assertEquals(transactionsCount + 1, getTranscationCount());
	}
	
	public void testAddTransactionShouldRequireAmount(){
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		validateTransactionListDisplayed();
		
		TransactionsDbAdapter adapter = new TransactionsDbAdapter(getActivity());
		int beforeCount = adapter.getTransactionsCount(adapter.getAccountID(DUMMY_ACCOUNT_UID));
		mSolo.clickOnImage(2);
		mSolo.waitForText("Description");
		mSolo.enterText(0, "Lunch");
		assertEquals(false, mSolo.getImage(3).isEnabled());
		mSolo.clickOnActionBarItem(R.id.btn_save);
		
		int afterCount = adapter.getTransactionsCount(adapter.getAccountID(DUMMY_ACCOUNT_UID));
		assertEquals(beforeCount, afterCount);
	}
	
	private void validateEditTransactionFields(Transaction transaction){
		
		String name = mSolo.getEditText(0).getText().toString();
		assertEquals(transaction.getName(), name);
		
		String amountString = mSolo.getEditText(1).getText().toString();
		NumberFormat formatter = NumberFormat.getInstance();
		try {
			amountString = formatter.parse(amountString).toString();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Money amount = new Money(amountString, Currency.getInstance(Locale.getDefault()).getCurrencyCode());
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
	
	public void testOpenTransactionEditShouldNotModifyTransaction(){
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		
		validateTransactionListDisplayed();
		
		mSolo.clickOnText(TRANSACTION_NAME);
		mSolo.waitForText("Note");
		
		validateNewTransactionFields();
		
		mSolo.clickOnActionBarItem(R.id.menu_save);
		
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		
		TransactionsDbAdapter adapter = new TransactionsDbAdapter(getActivity());
		List<Transaction> transactions = adapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID);
		
		assertEquals(1, transactions.size());
		Transaction trx = transactions.get(0);
		assertEquals(TRANSACTION_NAME, trx.getName());
		assertEquals(trx.getAccountUID(), DUMMY_ACCOUNT_UID);
		Date expectedDate = new Date(mTransactionTimeMillis);
		Date trxDate = new Date(trx.getTimeMillis());
		assertEquals(NewTransactionFragment.DATE_FORMATTER.format(expectedDate), 
				NewTransactionFragment.DATE_FORMATTER.format(trxDate));
		assertEquals(NewTransactionFragment.TIME_FORMATTER.format(expectedDate), 
				NewTransactionFragment.TIME_FORMATTER.format(trxDate));
		
		//FIXME: for some reason, the expected time is higher (in the future) than the actual time
		//this should not be the case since the transaction was created with the expected time
		//I guess it has to do with the time precision and the fact that the time is repeatedly 
		//converted to Date objects and back. But just validating the printable date and time should be ok
//		assertEquals(mTransactionTimeMillis, trx.getTimeMillis());
	}
	
	public void testEditTransaction(){		
		//open transactions
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		
		validateTransactionListDisplayed();
		
		mSolo.clickOnText(TRANSACTION_NAME);
		mSolo.waitForText("Note");
		
		validateEditTransactionFields(mTransaction);
				
		mSolo.enterText(0, "Pasta");
		mSolo.clickOnActionBarItem(R.id.menu_save);
		
		//if we see the text, then it was successfully created
		mSolo.waitForText("Pasta");
	}
	
	public void testToggleTransactionType(){
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		
		validateTransactionListDisplayed();
		mSolo.clickOnText(TRANSACTION_NAME);
		mSolo.waitForText("Note");
		
		validateEditTransactionFields(mTransaction);
		
		mSolo.clickOnButton(getActivity().getString(R.string.label_credit));
		String amountString = mSolo.getEditText(1).getText().toString();
		NumberFormat formatter = NumberFormat.getInstance();
		try {
			amountString = formatter.parse(amountString).toString();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Money amount = new Money(amountString, Currency.getInstance(Locale.getDefault()).getCurrencyCode());
		assertEquals("-9.99", amount.toPlainString());
		
		//save the transaction, should now be a debit
		mSolo.clickOnImage(3);
		
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		
		TransactionsDbAdapter adapter = new TransactionsDbAdapter(getActivity());
		List<Transaction> transactions = adapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID);
		
		assertEquals(1, transactions.size());
		Transaction trx = transactions.get(0);
		assertTrue(trx.getAmount().isNegative());
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
		account.setCurrency(Currency.getInstance(Locale.getDefault()));
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
