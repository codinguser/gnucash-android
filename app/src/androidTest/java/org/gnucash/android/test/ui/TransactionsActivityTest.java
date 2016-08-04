/*
 * Copyright (c) 2012 - 2015 Ngewi Fet <ngewif@gmail.com>
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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.CommoditiesDbAdapter;
import org.gnucash.android.db.adapter.DatabaseAdapter;
import org.gnucash.android.db.adapter.SplitsDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;
import org.gnucash.android.receivers.TransactionRecorder;
import org.gnucash.android.test.ui.util.DisableAnimationsRule;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.settings.PreferenceActivity;
import org.gnucash.android.ui.transaction.TransactionFormFragment;
import org.gnucash.android.ui.transaction.TransactionsActivity;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class TransactionsActivityTest {
    private static final String TRANSACTION_AMOUNT = "9.99";
	private static final String TRANSACTION_NAME = "Pizza";
	private static final String DUMMY_ACCOUNT_UID = "transactions-account";
	private static final String DUMMY_ACCOUNT_NAME = "Transactions Account";

    private static final String TRANSFER_ACCOUNT_NAME   = "Transfer account";
    private static final String TRANSFER_ACCOUNT_UID    = "transfer_account";
    public static final String CURRENCY_CODE = "USD";
	public static Commodity COMMODITY = Commodity.DEFAULT_COMMODITY;

	private Transaction mTransaction;
	private long mTransactionTimeMillis;

    private static AccountsDbAdapter mAccountsDbAdapter;
    private static TransactionsDbAdapter mTransactionsDbAdapter;
    private static SplitsDbAdapter mSplitsDbAdapter;
	private TransactionsActivity mTransactionsActivity;

	@ClassRule
	public static DisableAnimationsRule disableAnimationsRule = new DisableAnimationsRule();

	@Rule
	public ActivityTestRule<TransactionsActivity> mActivityRule =
			new ActivityTestRule<>(TransactionsActivity.class, true, false);

	private Account mBaseAccount;
	private Account mTransferAccount;

	public TransactionsActivityTest() {
		mBaseAccount = new Account(DUMMY_ACCOUNT_NAME, COMMODITY);
		mBaseAccount.setUID(DUMMY_ACCOUNT_UID);

		mTransferAccount = new Account(TRANSFER_ACCOUNT_NAME, COMMODITY);
		mTransferAccount.setUID(TRANSFER_ACCOUNT_UID);

		mTransactionTimeMillis = System.currentTimeMillis();
		mTransaction = new Transaction(TRANSACTION_NAME);
		mTransaction.setCommodity(COMMODITY);
		mTransaction.setNote("What up?");
		mTransaction.setTime(mTransactionTimeMillis);
		Split split = new Split(new Money(TRANSACTION_AMOUNT, CURRENCY_CODE), DUMMY_ACCOUNT_UID);
		split.setType(TransactionType.DEBIT);

		mTransaction.addSplit(split);
		mTransaction.addSplit(split.createPair(TRANSFER_ACCOUNT_UID));

		mBaseAccount.addTransaction(mTransaction);
	}

	@BeforeClass
	public static void prepareTestCase(){
		Context context = GnuCashApplication.getAppContext();
		AccountsActivityTest.preventFirstRunDialogs(context);

		mSplitsDbAdapter = SplitsDbAdapter.getInstance();
		mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();
		mAccountsDbAdapter = AccountsDbAdapter.getInstance();
		COMMODITY = CommoditiesDbAdapter.getInstance().getCommodity(CURRENCY_CODE);

//		PreferenceActivity.getActiveBookSharedPreferences(context)
//				.edit().putBoolean(context.getString(R.string.key_use_compact_list), false)
//				.apply();
	}

	@Before
	public void setUp() throws Exception {
		mAccountsDbAdapter.deleteAllRecords();
        mAccountsDbAdapter.addRecord(mBaseAccount, DatabaseAdapter.UpdateMethod.insert);
        mAccountsDbAdapter.addRecord(mTransferAccount, DatabaseAdapter.UpdateMethod.insert);

        mTransactionsDbAdapter.addRecord(mTransaction, DatabaseAdapter.UpdateMethod.insert);

		assertThat(mAccountsDbAdapter.getRecordsCount()).isEqualTo(3); //including ROOT account
		assertThat(mTransactionsDbAdapter.getRecordsCount()).isEqualTo(1);

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, DUMMY_ACCOUNT_UID);
		mTransactionsActivity = mActivityRule.launchActivity(intent);

		refreshTransactionsList();
	}


	private void validateTransactionListDisplayed(){
		onView(withId(R.id.transaction_recycler_view)).check(matches(isDisplayed()));
	}
	
	private int getTransactionCount(){
        return mTransactionsDbAdapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID).size();
	}
	
	private void validateTimeInput(long timeMillis){
		String expectedValue = TransactionFormFragment.DATE_FORMATTER.format(new Date(timeMillis));
		onView(withId(R.id.input_date)).check(matches(withText(expectedValue)));
		
		expectedValue = TransactionFormFragment.TIME_FORMATTER.format(new Date(timeMillis));
		onView(withId(R.id.input_time)).check(matches(withText(expectedValue)));
	}

	@Test
	public void testAddTransactionShouldRequireAmount(){
		validateTransactionListDisplayed();
		
		int beforeCount = mTransactionsDbAdapter.getTransactionsCount(DUMMY_ACCOUNT_UID);
        onView(withId(R.id.fab_create_transaction)).perform(click());

		onView(withId(R.id.input_transaction_name))
				.check(matches(isDisplayed()))
				.perform(typeText("Lunch"));

		onView(withId(R.id.menu_save)).perform(click());
		onView(withText(R.string.title_add_transaction)).check(matches(isDisplayed()));

		Espresso.closeSoftKeyboard();
		sleep(1000);

		assertToastDisplayed(R.string.toast_transanction_amount_required);

		int afterCount = mTransactionsDbAdapter.getTransactionsCount(DUMMY_ACCOUNT_UID);
		assertThat(afterCount).isEqualTo(beforeCount);

	}

	/**
	 * Sleep the thread for a specified period
	 * @param millis Duration to sleep in milliseconds
	 */
	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Checks that a specific toast message is displayed
	 * @param toastString String that should be displayed
	 */
	private void assertToastDisplayed(int toastString) {
		onView(withText(toastString))
				.inRoot(withDecorView(not(mTransactionsActivity.getWindow().getDecorView())))
				.check(matches(isDisplayed()));
	}


	private void validateEditTransactionFields(Transaction transaction){

		onView(withId(R.id.input_transaction_name)).check(matches(withText(transaction.getDescription())));

		Money balance = transaction.getBalance(DUMMY_ACCOUNT_UID);
		NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
		formatter.setMinimumFractionDigits(2);
		formatter.setMaximumFractionDigits(2);
		onView(withId(R.id.input_transaction_amount)).check(matches(withText(formatter.format(balance.asDouble()))));
		onView(withId(R.id.input_date)).check(matches(withText(TransactionFormFragment.DATE_FORMATTER.format(transaction.getTimeMillis()))));
		onView(withId(R.id.input_time)).check(matches(withText(TransactionFormFragment.TIME_FORMATTER.format(transaction.getTimeMillis()))));
		onView(withId(R.id.input_description)).check(matches(withText(transaction.getNote())));

		validateTimeInput(transaction.getTimeMillis());
	}

    //TODO: Add test for only one account but with double-entry enabled
	@Test
	public void testAddTransaction(){
        setDoubleEntryEnabled(true);
		setDefaultTransactionType(TransactionType.DEBIT);
        validateTransactionListDisplayed();

		onView(withId(R.id.fab_create_transaction)).perform(click());

		onView(withId(R.id.input_transaction_name)).perform(typeText("Lunch"));
		onView(withId(R.id.input_transaction_amount)).perform(typeText("899"));
		onView(withId(R.id.input_transaction_type))
				.check(matches(allOf(isDisplayed(), withText(R.string.label_receive))))
				.perform(click())
				.check(matches(withText(R.string.label_spend)));

		String expectedValue = NumberFormat.getInstance().format(-899);
		onView(withId(R.id.input_transaction_amount)).check(matches(withText(expectedValue)));

        int transactionsCount = getTransactionCount();
		onView(withId(R.id.menu_save)).perform(click());

        validateTransactionListDisplayed();

        List<Transaction> transactions = mTransactionsDbAdapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID);
        assertThat(transactions).hasSize(2);
        Transaction transaction = transactions.get(0);
		assertThat(transaction.getSplits()).hasSize(2);

        assertThat(getTransactionCount()).isEqualTo(transactionsCount + 1);
    }

	@Test
	public void testEditTransaction(){
		validateTransactionListDisplayed();

		onView(withId(R.id.edit_transaction)).perform(click());
		
		validateEditTransactionFields(mTransaction);

		onView(withId(R.id.input_transaction_name)).perform(clearText(), typeText("Pasta"));
		onView(withId(R.id.menu_save)).perform(click());
	}

	/**
	 * Tests that transactions splits are automatically balanced and an imbalance account will be created
	 * This test case assumes that single entry is used
	 */
	//TODO: move this to the unit tests
	public void testAutoBalanceTransactions(){
		setDoubleEntryEnabled(false);
		mTransactionsDbAdapter.deleteAllRecords();

		assertThat(mTransactionsDbAdapter.getRecordsCount()).isEqualTo(0);
		String imbalanceAcctUID = mAccountsDbAdapter.getImbalanceAccountUID(Currency.getInstance(CURRENCY_CODE));
		assertThat(imbalanceAcctUID).isNull();

		validateTransactionListDisplayed();
		onView(withId(R.id.fab_create_transaction)).perform(click());
		onView(withId(R.id.fragment_transaction_form)).check(matches(isDisplayed()));

		onView(withId(R.id.input_transaction_name)).perform(typeText("Autobalance"));
		onView(withId(R.id.input_transaction_amount)).perform(typeText("499"));

		//no double entry so no split editor
		//TODO: check that the split drawable is not displayed
		onView(withId(R.id.menu_save)).perform(click());

		assertThat(mTransactionsDbAdapter.getRecordsCount()).isEqualTo(1);
		Transaction transaction = mTransactionsDbAdapter.getAllTransactions().get(0);
		assertThat(transaction.getSplits()).hasSize(2);
		imbalanceAcctUID = mAccountsDbAdapter.getImbalanceAccountUID(Currency.getInstance(CURRENCY_CODE));
		assertThat(imbalanceAcctUID).isNotNull();
		assertThat(imbalanceAcctUID).isNotEmpty();
		assertThat(mAccountsDbAdapter.isHiddenAccount(imbalanceAcctUID)).isTrue(); //imbalance account should be hidden in single entry mode

		assertThat(transaction.getSplits()).extracting("mAccountUID").contains(imbalanceAcctUID);

	}

	/**
	 * Tests input of transaction splits using the split editor.
	 * Also validates that the imbalance from the split editor will be automatically added as a split
	 * //FIXME: find a more reliable way to test opening of the split editor
	 */
	@Test
	public void testSplitEditor(){
		setDoubleEntryEnabled(true);
		setDefaultTransactionType(TransactionType.DEBIT);
		mTransactionsDbAdapter.deleteAllRecords();

		//when we start there should be no imbalance account in the system
		String imbalanceAcctUID = mAccountsDbAdapter.getImbalanceAccountUID(Currency.getInstance(CURRENCY_CODE));
		assertThat(imbalanceAcctUID).isNull();

		validateTransactionListDisplayed();
		onView(withId(R.id.fab_create_transaction)).perform(click());

		onView(withId(R.id.input_transaction_name)).perform(typeText("Autobalance"));
		onView(withId(R.id.input_transaction_amount)).perform(typeText("499"));

		onView(withId(R.id.btn_split_editor)).perform(click());

		onView(withId(R.id.split_list_layout)).check(matches(allOf(isDisplayed(), hasDescendant(withId(R.id.input_split_amount)))));

		onView(withId(R.id.menu_add_split)).perform(click());

		onView(allOf(withId(R.id.input_split_amount), withText(""))).perform(typeText("400"));

		onView(withId(R.id.menu_save)).perform(click());
		//after we use split editor, we should not be able to toggle the transaction type
		onView(withId(R.id.input_transaction_type)).check(matches(not(isDisplayed())));

		onView(withId(R.id.menu_save)).perform(click());

		List<Transaction> transactions = mTransactionsDbAdapter.getAllTransactions();
		assertThat(transactions).hasSize(1);

		Transaction transaction = transactions.get(0);

		assertThat(transaction.getSplits()).hasSize(3); //auto-balanced
		imbalanceAcctUID = mAccountsDbAdapter.getImbalanceAccountUID(Currency.getInstance(CURRENCY_CODE));
		assertThat(imbalanceAcctUID).isNotNull();
		assertThat(imbalanceAcctUID).isNotEmpty();
		assertThat(mAccountsDbAdapter.isHiddenAccount(imbalanceAcctUID)).isFalse();

		//at least one split will belong to the imbalance account
		assertThat(transaction.getSplits()).extracting("mAccountUID").contains(imbalanceAcctUID);

		List<Split> imbalanceSplits = mSplitsDbAdapter.getSplitsForTransactionInAccount(transaction.getUID(), imbalanceAcctUID);
		assertThat(imbalanceSplits).hasSize(1);

		Split split = imbalanceSplits.get(0);
		assertThat(split.getValue().asBigDecimal()).isEqualTo(new BigDecimal("99.00"));
		assertThat(split.getType()).isEqualTo(TransactionType.CREDIT);
	}


    private void setDoubleEntryEnabled(boolean enabled){
        SharedPreferences prefs = PreferenceActivity.getActiveBookSharedPreferences(mTransactionsActivity);
        Editor editor = prefs.edit();
        editor.putBoolean(mTransactionsActivity.getString(R.string.key_use_double_entry), enabled);
        editor.apply();
    }

	@Test
	public void testDefaultTransactionType(){
		setDefaultTransactionType(TransactionType.CREDIT);

		onView(withId(R.id.fab_create_transaction)).perform(click());
		onView(withId(R.id.input_transaction_type)).check(matches(allOf(isChecked(), withText(R.string.label_spend))));
	}

	private void setDefaultTransactionType(TransactionType type) {
		SharedPreferences prefs = PreferenceActivity.getActiveBookSharedPreferences(mTransactionsActivity);
		Editor editor = prefs.edit();
		editor.putString(mTransactionsActivity.getString(R.string.key_default_transaction_type), type.name());
		editor.commit();
	}

	//FIXME: Improve on this test
	public void childAccountsShouldUseParentTransferAccountSetting(){
		Account transferAccount = new Account("New Transfer Acct");
		mAccountsDbAdapter.addRecord(transferAccount, DatabaseAdapter.UpdateMethod.insert);
		mAccountsDbAdapter.addRecord(new Account("Higher account"), DatabaseAdapter.UpdateMethod.insert);

		Account childAccount = new Account("Child Account");
		childAccount.setParentUID(DUMMY_ACCOUNT_UID);
		mAccountsDbAdapter.addRecord(childAccount, DatabaseAdapter.UpdateMethod.insert);
		ContentValues contentValues = new ContentValues();
		contentValues.put(DatabaseSchema.AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID, transferAccount.getUID());
		mAccountsDbAdapter.updateRecord(DUMMY_ACCOUNT_UID, contentValues);

		Intent intent = new Intent(mTransactionsActivity, TransactionsActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
		intent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, childAccount.getUID());

		mTransactionsActivity.startActivity(intent);

		onView(withId(R.id.input_transaction_amount)).perform(typeText("1299"));
		clickOnView(R.id.menu_save);

		//if our transfer account has a transaction then the right transfer account was used
		List<Transaction> transactions = mTransactionsDbAdapter.getAllTransactionsForAccount(transferAccount.getUID());
		assertThat(transactions).hasSize(1);
	}

	@Test
	public void testToggleTransactionType(){
		validateTransactionListDisplayed();
		onView(withId(R.id.edit_transaction)).perform(click());

		validateEditTransactionFields(mTransaction);

		onView(withId(R.id.input_transaction_type)).check(matches(
				allOf(isDisplayed(), withText(R.string.label_receive))
		)).perform(click()).check(matches(withText(R.string.label_spend)));

		onView(withId(R.id.input_transaction_amount)).check(matches(withText("-9.99")));

		onView(withId(R.id.menu_save)).perform(click());
		
		List<Transaction> transactions = mTransactionsDbAdapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID);
		assertThat(transactions).hasSize(1);
		Transaction trx = transactions.get(0);
		assertThat(trx.getSplits()).hasSize(2); //auto-balancing of splits
		assertThat(trx.getBalance(DUMMY_ACCOUNT_UID).isNegative()).isTrue();
	}

	@Test
	public void testOpenTransactionEditShouldNotModifyTransaction(){
		validateTransactionListDisplayed();

		onView(withId(R.id.edit_transaction)).perform(click());
		validateTimeInput(mTransactionTimeMillis);

		clickOnView(R.id.menu_save);

		List<Transaction> transactions = mTransactionsDbAdapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID);

		assertThat(transactions).hasSize(1);
		Transaction trx = transactions.get(0);
		assertThat(TRANSACTION_NAME).isEqualTo(trx.getDescription());
		Date expectedDate = new Date(mTransactionTimeMillis);
		Date trxDate = new Date(trx.getTimeMillis());
		assertThat(TransactionFormFragment.DATE_FORMATTER.format(expectedDate))
				.isEqualTo(TransactionFormFragment.DATE_FORMATTER.format(trxDate));
		assertThat(TransactionFormFragment.TIME_FORMATTER.format(expectedDate))
				.isEqualTo(TransactionFormFragment.TIME_FORMATTER.format(trxDate));
	}

	@Test
	public void testDeleteTransaction(){
		onView(withId(R.id.options_menu)).perform(click());
		onView(withText(R.string.menu_delete)).perform(click());

		long id = mAccountsDbAdapter.getID(DUMMY_ACCOUNT_UID);
		assertThat(0).isEqualTo(mTransactionsDbAdapter.getTransactionsCount(id));
	}

	@Test
	public void testMoveTransaction(){
		Account account = new Account("Move account");
		account.setCommodity(Commodity.getInstance(CURRENCY_CODE));
		mAccountsDbAdapter.addRecord(account, DatabaseAdapter.UpdateMethod.insert);

		assertThat(mTransactionsDbAdapter.getAllTransactionsForAccount(account.getUID())).hasSize(0);

		onView(withId(R.id.options_menu)).perform(click());
		onView(withText(R.string.menu_move_transaction)).perform(click());

		onView(withId(R.id.btn_save)).perform(click());

		assertThat(mTransactionsDbAdapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID)).hasSize(0);

		assertThat(mTransactionsDbAdapter.getAllTransactionsForAccount(account.getUID())).hasSize(1);

	}

//	@Test //// FIXME: 03.11.2015 fix and re-enable this test
	public void editingSplit_shouldNotSetAmountToZero(){
		setDoubleEntryEnabled(true);
		mTransactionsDbAdapter.deleteAllRecords();

		Account account = new Account("Z Account", Commodity.getInstance(CURRENCY_CODE));
		mAccountsDbAdapter.addRecord(account, DatabaseAdapter.UpdateMethod.insert);

		onView(withId(R.id.fab_create_transaction)).perform(click());

		onView(withId(R.id.input_transaction_name)).perform(typeText("Test Split"));
		onView(withId(R.id.input_transaction_amount)).perform(typeText("1024"));

		onView(withId(R.id.menu_save)).perform(click());

		onView(withText("Test Split")).perform(click());
		onView(withId(R.id.fab_edit_transaction)).perform(click());

		onView(withId(R.id.btn_split_editor)).perform(click());

//		onView(withSpinnerText(DUMMY_ACCOUNT_NAME)).perform(click()); //// FIXME: 03.11.2015 properly select the spinner
		onData(withId(R.id.input_accounts_spinner))
				.inAdapterView(withId(R.id.split_list_layout))
				.atPosition(1)
				.perform(click());
		onData(allOf(is(instanceOf(String.class)), is(account.getFullName()))).perform(click());
//		onView(withText(account.getFullName())).perform(click());

		onView(withId(R.id.menu_save)).perform(click());
		onView(withId(R.id.menu_save)).perform(click());

		//split should have moved from account, it should now be empty
		onView(withId(R.id.empty_view)).check(matches(isDisplayed()));

		assertThat(mAccountsDbAdapter.getAccountBalance(DUMMY_ACCOUNT_UID)).isEqualTo(Money.createZeroInstance(CURRENCY_CODE));

		//split
		assertThat(mAccountsDbAdapter.getAccountBalance(account.getUID())).isEqualTo(new Money("1024", CURRENCY_CODE));
	}

	@Test
	public void testDuplicateTransaction(){
		assertThat(mTransactionsDbAdapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID)).hasSize(1);

		onView(withId(R.id.options_menu)).perform(click());
		onView(withText(R.string.menu_duplicate_transaction)).perform(click());

		List<Transaction> dummyAccountTrns = mTransactionsDbAdapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID);
		assertThat(dummyAccountTrns).hasSize(2);

		assertThat(dummyAccountTrns.get(0).getDescription()).isEqualTo(dummyAccountTrns.get(1).getDescription());
		assertThat(dummyAccountTrns.get(0).getTimeMillis()).isNotEqualTo(dummyAccountTrns.get(1).getTimeMillis());
	}

	//TODO: add normal transaction recording
	@Test
	public void testLegacyIntentTransactionRecording(){
		int beforeCount = mTransactionsDbAdapter.getTransactionsCount(DUMMY_ACCOUNT_UID);
		Intent transactionIntent = new Intent(Intent.ACTION_INSERT);
		transactionIntent.setType(Transaction.MIME_TYPE);
		transactionIntent.putExtra(Intent.EXTRA_TITLE, "Power intents");
		transactionIntent.putExtra(Intent.EXTRA_TEXT, "Intents for sale");
		transactionIntent.putExtra(Transaction.EXTRA_AMOUNT, new BigDecimal(4.99));
		transactionIntent.putExtra(Transaction.EXTRA_ACCOUNT_UID, DUMMY_ACCOUNT_UID);
		transactionIntent.putExtra(Transaction.EXTRA_TRANSACTION_TYPE, TransactionType.DEBIT.name());
		transactionIntent.putExtra(Account.EXTRA_CURRENCY_CODE, "USD");

		new TransactionRecorder().onReceive(mTransactionsActivity, transactionIntent);

		int afterCount = mTransactionsDbAdapter.getTransactionsCount(DUMMY_ACCOUNT_UID);
		
		assertThat(beforeCount + 1).isEqualTo(afterCount);
		
		List<Transaction> transactions = mTransactionsDbAdapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID);
		
		for (Transaction transaction : transactions) {
			if (transaction.getDescription().equals("Power intents")){
				assertThat("Intents for sale").isEqualTo(transaction.getNote());
				assertThat(4.99).isEqualTo(transaction.getBalance(DUMMY_ACCOUNT_UID).asDouble());
			}
		}
	}

	/**
	 * Simple wrapper for clicking on views with espresso
	 * @param viewId View resource ID
	 */
	private void clickOnView(int viewId){
		onView(withId(viewId)).perform(click());
	}

	/**
	 * Refresh the account list fragment
	 */
	private void refreshTransactionsList(){
		try {
			mActivityRule.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mTransactionsActivity.refresh();
				}
			});
		} catch (Throwable throwable) {
			System.err.println("Failed to refresh fragment");
		}
	}

	@After
	public void tearDown() throws Exception {
		if (mTransactionsActivity != null)
			mTransactionsActivity.finish();
	}

}
