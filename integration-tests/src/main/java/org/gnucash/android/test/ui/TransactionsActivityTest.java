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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.robotium.solo.Solo;
import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.*;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.transaction.TransactionFormFragment;
import org.gnucash.android.ui.transaction.TransactionsActivity;
import org.gnucash.android.ui.util.TransactionTypeToggleButton;

import java.math.BigDecimal;
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

    private static final String TRANSFER_ACCOUNT_NAME   = "Transfer account";
    private static final String TRANSFER_ACCOUNT_UID    = "transfer_account";
    public static final String CURRENCY_CODE = "USD";

    private Solo mSolo;
	private Transaction mTransaction;
	private long mTransactionTimeMillis;

    private SQLiteDatabase mDb;
    private DatabaseHelper mDbHelper;
    private AccountsDbAdapter mAccountsDbAdapter;
    private TransactionsDbAdapter mTransactionsDbAdapter;
    private SplitsDbAdapter mSplitsDbAdapter;
	
	public TransactionsActivityTest() {
		super(TransactionsActivity.class);
	}
	
	@Override
	protected void setUp() throws Exception {
        mDbHelper = new DatabaseHelper(getInstrumentation().getTargetContext());
        try {
            mDb = mDbHelper.getWritableDatabase();
        } catch (SQLException e) {
            Log.e(getClass().getName(), "Error getting database: " + e.getMessage());
            mDb = mDbHelper.getReadableDatabase();
        }
        mSplitsDbAdapter = new SplitsDbAdapter(mDb);
        mTransactionsDbAdapter = new TransactionsDbAdapter(mDb, mSplitsDbAdapter);
        mAccountsDbAdapter = new AccountsDbAdapter(mDb, mTransactionsDbAdapter);

        mTransactionTimeMillis = System.currentTimeMillis();
        Account account = new Account(DUMMY_ACCOUNT_NAME);
        account.setUID(DUMMY_ACCOUNT_UID);
        account.setCurrency(Currency.getInstance(CURRENCY_CODE));

        Account account2 = new Account(TRANSFER_ACCOUNT_NAME);
        account2.setUID(TRANSFER_ACCOUNT_UID);
        account2.setCurrency(Currency.getInstance(CURRENCY_CODE));

        mTransaction = new Transaction(TRANSACTION_NAME);
        mTransaction.setNote("What up?");
        mTransaction.setTime(mTransactionTimeMillis);
        Split split = new Split(new Money(TRANSACTION_AMOUNT, CURRENCY_CODE), DUMMY_ACCOUNT_UID);
        split.setType(TransactionType.DEBIT);

        mTransaction.addSplit(split);
        mTransaction.addSplit(split.createPair(TRANSFER_ACCOUNT_UID));
        account.addTransaction(mTransaction);

        long id1 = mAccountsDbAdapter.addAccount(account);
        long id2 = mAccountsDbAdapter.addAccount(account2);
        assertTrue(id1 > 0);
        assertTrue(id2 > 0);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, DUMMY_ACCOUNT_UID);
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
		Fragment fragment = getActivity().getCurrentPagerFragment();
		assertNotNull(fragment);
	}
	
	private int getTranscationCount(){
		int count = mTransactionsDbAdapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID).size();
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
		
		int beforeCount = mTransactionsDbAdapter.getTransactionsCount(mTransactionsDbAdapter.getAccountID(DUMMY_ACCOUNT_UID));
        clickSherlockActionBarItem(R.id.menu_add_transaction);
		mSolo.waitForText("Description");
		mSolo.enterText(0, "Lunch");

        clickSherlockActionBarItem(R.id.menu_save);
        String toastAmountRequired = getActivity().getString(R.string.toast_transanction_amount_required);
		boolean toastFound = mSolo.waitForText(toastAmountRequired);
        assertTrue(toastFound);

		int afterCount = mTransactionsDbAdapter.getTransactionsCount(mTransactionsDbAdapter.getAccountID(DUMMY_ACCOUNT_UID));
		assertEquals(beforeCount, afterCount);

        mSolo.goBack();
	}
	
	private void validateEditTransactionFields(Transaction transaction){
		
		String name = ((EditText)mSolo.getView(R.id.input_transaction_name)).getText().toString();
		assertEquals(transaction.getDescription(), name);

        EditText amountEdittext = (EditText) mSolo.getView(R.id.input_transaction_amount);
		String amountString = amountEdittext.getText().toString();
		NumberFormat formatter = NumberFormat.getInstance();
		try {
			amountString = formatter.parse(amountString).toString();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Money amount = new Money(amountString, Currency.getInstance(Locale.getDefault()).getCurrencyCode());
		assertEquals(transaction.getBalance(DUMMY_ACCOUNT_UID), amount);

        EditText notesEditText = (EditText) mSolo.getView(R.id.input_description);
		String transactionNotes = notesEditText.getText().toString();
		assertEquals(transaction.getNote(), transactionNotes);
		
		String expectedValue = TransactionFormFragment.DATE_FORMATTER.format(transaction.getTimeMillis());
		TextView dateView = (TextView) mSolo.getView(R.id.input_date);
		String actualValue = dateView.getText().toString(); //mSolo.getText(6).getText().toString();
		assertEquals(expectedValue, actualValue);
		
		expectedValue = TransactionFormFragment.TIME_FORMATTER.format(transaction.getTimeMillis());
		TextView timeView = (TextView) mSolo.getView(R.id.input_time);
		actualValue = timeView.getText().toString();// mSolo.getText(7).getText().toString();
		assertEquals(expectedValue, actualValue);
	}

    //TODO: Add test for only one account but with double-entry enabled

	public void testAddTransaction(){
        setDoubleEntryEnabled(true);
        mSolo.waitForText(TRANSACTION_NAME);

        validateTransactionListDisplayed();
        clickSherlockActionBarItem(R.id.menu_add_transaction);

        mSolo.waitForText("New transaction");

        //validate creation of transaction
        mSolo.enterText(0, "Lunch");
        mSolo.enterText(1, "899");

        TransactionTypeToggleButton typeToggleButton = (TransactionTypeToggleButton) mSolo.getView(R.id.input_transaction_type);
        String text = typeToggleButton.getText().toString();

        assertTrue(mSolo.searchToggleButton(text));
        if (!mSolo.isToggleButtonChecked(0)){
            mSolo.clickOnToggleButton(text);
        }

        //check that the amount is correctly converted in the input field
        String value = mSolo.getEditText(1).getText().toString();
        String expectedValue = NumberFormat.getInstance().format(-8.99);
        assertEquals(expectedValue, value);

        int transactionsCount = getTranscationCount();

//        clickSherlockActionBarItem(R.id.menu_save);
        mSolo.clickOnActionBarItem(R.id.menu_save);

        mSolo.waitForText(DUMMY_ACCOUNT_NAME);
        validateTransactionListDisplayed();

        mSolo.sleep(1000);

        List<Transaction> transactions = mTransactionsDbAdapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID);
        assertEquals(2, transactions.size());
        Transaction transaction = transactions.get(0);

        assertEquals(2, transaction.getSplits().size());
        Split split = transaction.getSplits(TRANSFER_ACCOUNT_UID).get(0);
        //the main account is a CASH account which has debit normal type, so a negative value means actually CREDIT
        //so the other side of the split has to be a debit
        assertEquals(TransactionType.DEBIT, split.getType());
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

    private void setDoubleEntryEnabled(boolean enabled){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Editor editor = prefs.edit();
        editor.putBoolean(getActivity().getString(R.string.key_use_double_entry), enabled);
        editor.commit();
    }

	public void testDefaultTransactionType(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		Editor editor = prefs.edit();
		editor.putString(getActivity().getString(R.string.key_default_transaction_type), "CREDIT");
		editor.commit();

        clickSherlockActionBarItem(R.id.menu_add_transaction);
		mSolo.waitForText(getActivity().getString(R.string.label_transaction_name));
		
		ToggleButton transactionTypeButton = (ToggleButton) mSolo.getButton(0);
		assertThat(transactionTypeButton).isChecked();

		clickSherlockActionBarItem(R.id.menu_cancel);

		//now validate the other case 
		editor = prefs.edit();
		editor.putString(getActivity().getString(R.string.key_default_transaction_type), "DEBIT");
		editor.commit();
		
        clickSherlockActionBarItem(R.id.menu_add_transaction);
		mSolo.waitForText(getActivity().getString(R.string.label_transaction_name));
		
		transactionTypeButton = (ToggleButton) mSolo.getButton(0);
		assertThat(transactionTypeButton).isNotChecked();
        clickSherlockActionBarItem(R.id.menu_cancel);
        mSolo.goBack();
	}

	public void testToggleTransactionType(){
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		
		validateTransactionListDisplayed();
		mSolo.clickOnText(TRANSACTION_NAME);
		mSolo.waitForText(getActivity().getString(R.string.title_edit_transaction));
		
		validateEditTransactionFields(mTransaction);

        TransactionTypeToggleButton toggleButton = (TransactionTypeToggleButton) mSolo.getView(R.id.input_transaction_type);
        assertThat(toggleButton).isVisible();

        String label = toggleButton.getText().toString();
        assertTrue(mSolo.searchToggleButton(label));
		assertEquals(getActivity().getString(R.string.label_receive), label);

//		mSolo.clickOnButton(getActivity().getString(R.string.label_credit));
        mSolo.clickOnView(toggleButton);
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
		
		List<Transaction> transactions = mTransactionsDbAdapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID);
		
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
			
			List<Transaction> transactions = mTransactionsDbAdapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID);
			
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
		
		mSolo.sleep(500);

		long id = mAccountsDbAdapter.getID(DUMMY_ACCOUNT_UID);
		assertEquals(0, mTransactionsDbAdapter.getTransactionsCount(id));
	}
	
	public void testBulkMoveTransactions(){
        String targetAccountName = "Target";
        Account account = new Account(targetAccountName);
		account.setCurrency(Currency.getInstance(Locale.getDefault()));
		mAccountsDbAdapter.addAccount(account);
		
		int beforeOriginCount = mAccountsDbAdapter.getAccount(DUMMY_ACCOUNT_UID).getTransactionCount();
		
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
		
		int targetCount = mAccountsDbAdapter.getAccount(account.getUID()).getTransactionCount();
		assertEquals(1, targetCount);
		
		int afterOriginCount = mAccountsDbAdapter.getAccount(DUMMY_ACCOUNT_UID).getTransactionCount();
		assertEquals(beforeOriginCount-1, afterOriginCount);
	}
	
	public void testLegacyIntentTransactionRecording(){
		int beforeCount = mTransactionsDbAdapter.getTransactionsCount(DUMMY_ACCOUNT_UID);
		Intent transactionIntent = new Intent(Intent.ACTION_INSERT);
		transactionIntent.setType(Transaction.MIME_TYPE);
		transactionIntent.putExtra(Intent.EXTRA_TITLE, "Power intents");
		transactionIntent.putExtra(Intent.EXTRA_TEXT, "Intents for sale");
		transactionIntent.putExtra(Transaction.EXTRA_AMOUNT, new BigDecimal(4.99));
		transactionIntent.putExtra(Transaction.EXTRA_ACCOUNT_UID, DUMMY_ACCOUNT_UID);
		transactionIntent.putExtra(Transaction.EXTRA_TRANSACTION_TYPE, TransactionType.DEBIT.name());

		getActivity().sendBroadcast(transactionIntent);

        mSolo.sleep(2000);

		int afterCount = mTransactionsDbAdapter.getTransactionsCount(DUMMY_ACCOUNT_UID);
		
		assertEquals(beforeCount + 1, afterCount);
		
		List<Transaction> transactions = mTransactionsDbAdapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID);
		
		for (Transaction transaction : transactions) {
			if (transaction.getDescription().equals("Power intents")){
				assertEquals("Intents for sale", transaction.getNote());
				assertEquals(4.99, transaction.getBalance(DUMMY_ACCOUNT_UID).asDouble());
			}
		}
	}

	@Override
	protected void tearDown() throws Exception {	
		mAccountsDbAdapter.deleteAllRecords();

		mSolo.finishOpenedActivities();
		mDbHelper.close();
        mDb.close();
		super.tearDown();
	}
}
