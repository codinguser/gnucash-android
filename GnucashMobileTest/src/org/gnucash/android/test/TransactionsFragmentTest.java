package org.gnucash.android.test;

import java.util.Date;

import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.data.Transaction;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.ui.AccountsActivity;
import org.gnucash.android.ui.AccountsListFragment;
import org.gnucash.android.ui.NewTransactionFragment;

import android.database.Cursor;
import android.support.v4.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Spinner;

import com.jayway.android.robotium.solo.Solo;

public class TransactionsFragmentTest extends
		ActivityInstrumentationTestCase2<AccountsActivity> {
	private static final String DUMMY_ACCOUNT_UID = "transactions-account";
	private static final String DUMMY_ACCOUNT_NAME = "Transactions Account";
	private Solo mSolo;
	private Transaction mTransaction;
	
	public TransactionsFragmentTest() {
		super(AccountsActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		mSolo = new Solo(getInstrumentation(), getActivity());	
		
		Account account = new Account(DUMMY_ACCOUNT_NAME);
		account.setUID(DUMMY_ACCOUNT_UID);
		
		AccountsDbAdapter adapter = new AccountsDbAdapter(getActivity());
		adapter.addAccount(account);
		adapter.close();
		
		mTransaction = new Transaction(9.99, "Pizza");
		mTransaction.setAccountUID(DUMMY_ACCOUNT_UID);
		mTransaction.setDescription("What up?");
		mTransaction.setTime(System.currentTimeMillis());
		
		TransactionsDbAdapter dbAdapter = new TransactionsDbAdapter(getActivity());
		dbAdapter.addTransaction(mTransaction);
		dbAdapter.close();
		
	}
	
	private void validateTransactionListDisplayed(){
		Fragment fragment = getActivity()
				.getSupportFragmentManager()
				.findFragmentByTag(AccountsActivity.FRAGMENT_TRANSACTIONS_LIST);
		
		assertNotNull(fragment);
	}
	
	private int getTranscationCount(){
		TransactionsDbAdapter transactionsDb = new TransactionsDbAdapter(getActivity());
		int count = transactionsDb.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID).size();
		transactionsDb.close();
		return count;
	}
	
	private void validateNewTransactionFields(){
		String expectedValue = NewTransactionFragment.DATE_FORMATTER.format(new Date(System.currentTimeMillis()));
		String actualValue = mSolo.getText(5).getText().toString();
		assertEquals(expectedValue, actualValue);
		
		expectedValue = NewTransactionFragment.TIME_FORMATTER.format(new Date(System.currentTimeMillis()));
		actualValue = mSolo.getText(6).getText().toString();
		assertEquals(expectedValue, actualValue);
		Spinner spinner = mSolo.getCurrentSpinners().get(0);
		
		actualValue = ((Cursor)spinner.getSelectedItem()).getString(DatabaseAdapter.COLUMN_NAME);
		assertEquals(DUMMY_ACCOUNT_NAME, actualValue);
	}
	
	public void testAddTransaction(){
		refreshAccountsList();
		
		//open transactions
		mSolo.clickOnText(DUMMY_ACCOUNT_NAME);
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);		
		validateTransactionListDisplayed();
		
		mSolo.clickOnActionBarItem(R.id.menu_add_transaction);
		mSolo.waitForText("Description");
		
		validateNewTransactionFields();
		
		//validate creation of transaction
				mSolo.enterText(0, "Lunch");
		mSolo.enterText(1, "899");
		//check that the amount is correctly converted in the input field
		String actualValue = mSolo.getEditText(1).getText().toString();
		assertEquals(" - $8.99", actualValue);
		
		int transactionsCount = getTranscationCount();
		
		mSolo.clickOnActionBarItem(R.id.menu_save);	
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		validateTransactionListDisplayed();
		
		assertEquals(getTranscationCount(), transactionsCount + 1);
	}
	
	private void validateEditTransactionFields(Transaction transaction){
		
		String name = mSolo.getEditText(0).getText().toString();
		assertEquals(transaction.getName(), name);
		
		String amountString = mSolo.getEditText(1).getText().toString();
		double amount = Double.parseDouble(NewTransactionFragment.stripCurrencyFormatting(amountString))/100;
		amount *= mSolo.getCurrentToggleButtons().get(0).isChecked() ? -1 : 1; //set negative for debit
		assertEquals(transaction.getAmount(), amount);
		
		String description = mSolo.getEditText(2).getText().toString();
		assertEquals(transaction.getDescription(), description);
		
		String expectedValue = NewTransactionFragment.DATE_FORMATTER.format(transaction.getTimeMillis());
		String actualValue = mSolo.getText(5).getText().toString();
		assertEquals(expectedValue, actualValue);
		
		expectedValue = NewTransactionFragment.TIME_FORMATTER.format(transaction.getTimeMillis());
		actualValue = mSolo.getText(6).getText().toString();
		assertEquals(expectedValue, actualValue);
		Spinner spinner = mSolo.getCurrentSpinners().get(0);
		
		actualValue = ((Cursor)spinner.getSelectedItem()).getString(DatabaseAdapter.COLUMN_UID);		
		assertEquals(transaction.getAccountUID(), actualValue);
	}
	
	public void testEditTransaction(){
		refreshAccountsList();
		
		//open transactions
		mSolo.clickOnText(DUMMY_ACCOUNT_NAME);
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
		refreshAccountsList();
		
		mSolo.clickOnText(DUMMY_ACCOUNT_NAME);
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
	
	private void refreshAccountsList(){
		Fragment fragment = getActivity()
				.getSupportFragmentManager()
				.findFragmentByTag(AccountsActivity.FRAGMENT_ACCOUNTS_LIST);
		assertNotNull(fragment);
		((AccountsListFragment) fragment).refreshList();		
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
