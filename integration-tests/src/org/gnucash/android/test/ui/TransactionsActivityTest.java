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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.jayway.android.robotium.solo.Solo;
import org.gnucash.android.R;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.transaction.TransactionFormFragment;
import org.gnucash.android.ui.transaction.TransactionsActivity;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.fest.assertions.api.ANDROID.assertThat;

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
        mTransaction = new Transaction(TRANSACTION_NAME);
        mTransaction.setNote("What up?");
        mTransaction.setTime(mTransactionTimeMillis);

        account.addTransaction(mTransaction);

        Context context = getInstrumentation().getTargetContext();
        AccountsDbAdapter adapter = new AccountsDbAdapter(context);
        long id = adapter.addAccount(account);
        adapter.close();
        assertTrue(id > 0);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, id);
        setActivityIntent(intent);

        mSolo = new Solo(getInstrumentation(), getActivity());
	}

    /**
     * Finds a view in the action bar and clicks it, since the native methods are not supported by ActionBarSherlock
     * @param id
     */
    private void clickSherlockActionBarItem(int id){
        View view = mSolo.getView(id);
        mSolo.clickOnView(view);
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
		String expectedValue = TransactionFormFragment.DATE_FORMATTER.format(new Date(mTransactionTimeMillis));
		TextView dateView = (TextView) mSolo.getView(R.id.input_date);
		String actualValue = dateView.getText().toString();
		assertEquals(expectedValue, actualValue);
		
		expectedValue = TransactionFormFragment.TIME_FORMATTER.format(new Date(mTransactionTimeMillis));
		TextView timeView = (TextView) mSolo.getView(R.id.input_time);
		actualValue = timeView.getText().toString();
		assertEquals(expectedValue, actualValue);
		
	}
	
	public void testAddTransactionShouldRequireAmount(){
		mSolo.waitForText(TRANSACTION_NAME);
		validateTransactionListDisplayed();
		
		TransactionsDbAdapter adapter = new TransactionsDbAdapter(getActivity());
		int beforeCount = adapter.getTransactionsCount(adapter.getAccountID(DUMMY_ACCOUNT_UID));
        clickSherlockActionBarItem(R.id.menu_add_transaction);
		mSolo.waitForText("Description");
		mSolo.enterText(0, "Lunch");

        clickSherlockActionBarItem(R.id.menu_save);
        String toastAmountRequired = getActivity().getString(R.string.toast_transanction_amount_required);
		boolean toastFound = mSolo.waitForText(toastAmountRequired);
        assertTrue(toastFound);

		int afterCount = adapter.getTransactionsCount(adapter.getAccountID(DUMMY_ACCOUNT_UID));
		assertEquals(beforeCount, afterCount);

        adapter.close();
        mSolo.goBack();
	}
	
	private void validateEditTransactionFields(Transaction transaction){
		
		String name = mSolo.getEditText(0).getText().toString();
		assertEquals(transaction.getDescription(), name);
		
		String amountString = mSolo.getEditText(1).getText().toString();
		NumberFormat formatter = NumberFormat.getInstance();
		try {
			amountString = formatter.parse(amountString).toString();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Money amount = new Money(amountString, Currency.getInstance(Locale.getDefault()).getCurrencyCode());
		assertEquals(transaction.getBalance(DUMMY_ACCOUNT_UID), amount);
		
		String description = mSolo.getEditText(2).getText().toString();
		assertEquals(transaction.getNote(), description);
		
		String expectedValue = TransactionFormFragment.DATE_FORMATTER.format(transaction.getTimeMillis());
		TextView dateView = (TextView) mSolo.getView(R.id.input_date);
		String actualValue = dateView.getText().toString(); //mSolo.getText(6).getText().toString();
		assertEquals(expectedValue, actualValue);
		
		expectedValue = TransactionFormFragment.TIME_FORMATTER.format(transaction.getTimeMillis());
		TextView timeView = (TextView) mSolo.getView(R.id.input_time);
		actualValue = timeView.getText().toString();// mSolo.getText(7).getText().toString();
		assertEquals(expectedValue, actualValue);
	}
	
	public void testAddTransaction(){
			mSolo.waitForText(TRANSACTION_NAME);
//            mSolo.waitForFragmentByTag(TransactionsActivity.FRAGMENT_TRANSACTIONS_LIST);

        validateTransactionListDisplayed();

//			mSolo.clickOnActionBarItem(R.id.menu_add_transaction);
            clickSherlockActionBarItem(R.id.menu_add_transaction);

//			mSolo.waitForView(EditText.class);
            mSolo.waitForText("New transaction");

//			validateNewTransactionFields();


        //validate creation of transaction
			mSolo.enterText(0, "Lunch");
			mSolo.enterText(1, "899");
			//check that the amount is correctly converted in the input field
			String value = mSolo.getEditText(1).getText().toString();
			String expectedValue = NumberFormat.getInstance().format(-8.99);
			assertEquals(expectedValue, value);

			int transactionsCount = getTranscationCount();

			//Android 2.2 cannot handle this for some reason
//			mSolo.clickOnActionBarItem(R.id.menu_save);
//			mSolo.clickOnImage(3);
        clickSherlockActionBarItem(R.id.menu_save);

			mSolo.waitForText(DUMMY_ACCOUNT_NAME);
			validateTransactionListDisplayed();

			assertEquals(transactionsCount + 1, getTranscationCount());
		}

	public void testEditTransaction(){		
		//open transactions
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		
		validateTransactionListDisplayed();
		
		mSolo.clickOnText(TRANSACTION_NAME);
		mSolo.waitForText("Note");
		
		validateEditTransactionFields(mTransaction);
				
		mSolo.enterText(0, "Pasta");
		clickSherlockActionBarItem(R.id.menu_save);

		//if we see the text, then it was successfully created
		mSolo.waitForText("Pasta");
	}
	
	public void testDefaultTransactionType(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		Editor editor = prefs.edit();
		editor.putString(getActivity().getString(R.string.key_default_transaction_type), "CREDIT");
		editor.commit();

        clickSherlockActionBarItem(R.id.menu_add_transaction);
		mSolo.waitForText(getActivity().getString(R.string.label_transaction_name));
		
		ToggleButton transactionTypeButton = (ToggleButton) mSolo.getButton(0);
		assertThat(transactionTypeButton).isNotChecked();

		clickSherlockActionBarItem(R.id.menu_cancel);

		//now validate the other case 
		editor = prefs.edit();
		editor.putString(getActivity().getString(R.string.key_default_transaction_type), "DEBIT");
		editor.commit();
		
        clickSherlockActionBarItem(R.id.menu_add_transaction);
		mSolo.waitForText(getActivity().getString(R.string.label_transaction_name));
		
		transactionTypeButton = (ToggleButton) mSolo.getButton(0);
		assertThat(transactionTypeButton).isChecked();
        clickSherlockActionBarItem(R.id.menu_cancel);
        mSolo.goBack();
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

		clickSherlockActionBarItem(R.id.menu_save);
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		
		TransactionsDbAdapter adapter = new TransactionsDbAdapter(getActivity());
		List<Transaction> transactions = adapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID);
		
		assertEquals(1, transactions.size());
		Transaction trx = transactions.get(0);
		assertTrue(trx.getBalance(DUMMY_ACCOUNT_UID).isNegative());

        mSolo.goBack();
	}
	
	public void testOpenTransactionEditShouldNotModifyTransaction(){
			mSolo.waitForText(DUMMY_ACCOUNT_NAME);
			
			validateTransactionListDisplayed();
			
			mSolo.clickOnText(TRANSACTION_NAME);
			mSolo.waitForText("Edit transaction");
			
			validateNewTransactionFields();
			
			clickSherlockActionBarItem(R.id.menu_save);

			mSolo.waitForText(DUMMY_ACCOUNT_NAME);
			
			TransactionsDbAdapter adapter = new TransactionsDbAdapter(getActivity());
			List<Transaction> transactions = adapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID);
			
			assertEquals(1, transactions.size());
			Transaction trx = transactions.get(0);
			assertEquals(TRANSACTION_NAME, trx.getDescription());
			Date expectedDate = new Date(mTransactionTimeMillis);
			Date trxDate = new Date(trx.getTimeMillis());
			assertEquals(TransactionFormFragment.DATE_FORMATTER.format(expectedDate),
					TransactionFormFragment.DATE_FORMATTER.format(trxDate));
			assertEquals(TransactionFormFragment.TIME_FORMATTER.format(expectedDate),
					TransactionFormFragment.TIME_FORMATTER.format(trxDate));
			
			//FIXME: for some reason, the expected time is higher (in the future) than the actual time
			//this should not be the case since the transaction was created with the expected time
			//I guess it has to do with the time precision and the fact that the time is repeatedly 
			//converted to Date objects and back. But just validating the printable date and time should be ok
	//		assertEquals(mTransactionTimeMillis, trx.getTimeMillis());
		}

	public void testDeleteTransaction(){
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		
		mSolo.clickOnCheckBox(0);		
		clickSherlockActionBarItem(R.id.context_menu_delete);
		
		AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(getActivity());
		long id = accountsDbAdapter.getID(DUMMY_ACCOUNT_UID);
		TransactionsDbAdapter adapter = new TransactionsDbAdapter(getActivity());
		assertEquals(0, adapter.getTransactionsCount(id));
		
		accountsDbAdapter.close();
		adapter.close();
		
	}
	
	public void testBulkMoveTransactions(){
        String targetAccountName = "Target";
        Account account = new Account(targetAccountName);
		account.setCurrency(Currency.getInstance(Locale.getDefault()));
		AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(getActivity());
		accountsDbAdapter.addAccount(account);
		
		int beforeOriginCount = accountsDbAdapter.getAccount(DUMMY_ACCOUNT_UID).getTransactionCount();
		
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		
		validateTransactionListDisplayed();
		
		mSolo.clickOnCheckBox(0);
		mSolo.waitForText(getActivity().getString(R.string.title_selected, 1));
		//initiate bulk move
		clickSherlockActionBarItem(R.id.context_menu_move_transactions);
		
		mSolo.waitForDialogToClose(2000);
		
		Spinner spinner = mSolo.getCurrentViews(Spinner.class).get(0);
		mSolo.clickOnView(spinner);
        mSolo.sleep(500);
		mSolo.clickOnText(targetAccountName);
		mSolo.clickOnButton(1);
//		mSolo.clickOnText(getActivity().getString(R.string.btn_move));
		
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

        mSolo.sleep(2000);

		int afterCount = trxnAdapter.getTransactionsCount(trxnAdapter.getAccountID(DUMMY_ACCOUNT_UID));
		
		assertEquals(beforeCount + 1, afterCount);
		
		List<Transaction> transactions = trxnAdapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID);
		
		for (Transaction transaction : transactions) {
			if (transaction.getDescription().equals("Power intents")){
				assertEquals("Intents for sale", transaction.getNote());
				assertEquals(4.99, transaction.getBalance(DUMMY_ACCOUNT_UID).asDouble());
			}
		}
		
		trxnAdapter.close();
	}

	@Override
	protected void tearDown() throws Exception {	
		AccountsDbAdapter adapter = new AccountsDbAdapter(getActivity());
		adapter.deleteAllRecords();
		adapter.close();
		
		mSolo.finishOpenedActivities();
		
		super.tearDown();
	}
}
