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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.kobakei.ratethisapp.RateThisApp;

import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.receivers.AccountCreator;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.account.AccountsListFragment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isNotChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class AccountsActivityTest extends ActivityInstrumentationTestCase2<AccountsActivity> {
	private static final String DUMMY_ACCOUNT_CURRENCY_CODE = "USD";
    private static final Currency DUMMY_ACCOUNT_CURRENCY = Currency.getInstance(DUMMY_ACCOUNT_CURRENCY_CODE);
	private static final String DUMMY_ACCOUNT_NAME = "Dummy account";
    public static final String  DUMMY_ACCOUNT_UID   = "dummy-account";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private AccountsDbAdapter mAccountsDbAdapter;
    private TransactionsDbAdapter mTransactionsDbAdapter;
    private SplitsDbAdapter mSplitsDbAdapter;
    private AccountsActivity mAcccountsActivity;

    public AccountsActivityTest() {
		super(AccountsActivity.class);
	}

    @Before
	public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        preventFirstRunDialogs(getInstrumentation().getTargetContext());
        mAcccountsActivity = getActivity();

        mDbHelper = new DatabaseHelper(mAcccountsActivity);
        try {
            mDb = mDbHelper.getWritableDatabase();
        } catch (SQLException e) {
            Log.e(getClass().getName(), "Error getting database: " + e.getMessage());
            mDb = mDbHelper.getReadableDatabase();
        }
        mSplitsDbAdapter = new SplitsDbAdapter(mDb);
        mTransactionsDbAdapter = new TransactionsDbAdapter(mDb, mSplitsDbAdapter);
        mAccountsDbAdapter = new AccountsDbAdapter(mDb, mTransactionsDbAdapter);
        mAccountsDbAdapter.deleteAllRecords(); //clear the data

		Account account = new Account(DUMMY_ACCOUNT_NAME);
        account.setUID(DUMMY_ACCOUNT_UID);
		account.setCurrency(Currency.getInstance(DUMMY_ACCOUNT_CURRENCY_CODE));
		mAccountsDbAdapter.addRecord(account);
        refreshAccountsList();
	}

    /**
     * Prevents the first-run dialogs (Whats new, Create accounts etc) from being displayed when testing
     * @param context Application context
     */
    public static void preventFirstRunDialogs(Context context) {
        AccountsActivity.rateAppConfig = new RateThisApp.Config(10000, 10000);
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

        //do not show first run dialog
        editor.putBoolean(context.getString(R.string.key_first_run), false);
        editor.putInt(AccountsActivity.LAST_OPEN_TAB_INDEX, AccountsActivity.INDEX_TOP_LEVEL_ACCOUNTS_FRAGMENT);

        //do not show "What's new" dialog
        String minorVersion = context.getString(R.string.app_minor_version);
        int currentMinor = Integer.parseInt(minorVersion);
        editor.putInt(context.getString(R.string.key_previous_minor_version), currentMinor);
        editor.commit();
    }


    public void testDisplayAccountsList(){
        AccountsActivity.createDefaultAccounts("EUR", mAcccountsActivity);
        mAcccountsActivity.recreate();

        refreshAccountsList();
        onView(withText("Assets")).perform(scrollTo());
        onView(withText("Expenses")).perform(click());
        onView(withText("Books")).perform(scrollTo());
    }

    @Test
    public void testSearchAccounts(){
        String SEARCH_ACCOUNT_NAME = "Search Account";

        Account account = new Account(SEARCH_ACCOUNT_NAME);
        account.setParentUID(DUMMY_ACCOUNT_UID);
        mAccountsDbAdapter.addRecord(account);

        //enter search query
//        ActionBarUtils.clickSherlockActionBarItem(mSolo, R.id.menu_search);
        onView(withId(R.id.menu_search)).perform(click());
        onView(withId(R.id.search_src_text)).perform(typeText("Se"));
        onView(withText(SEARCH_ACCOUNT_NAME)).check(matches(isDisplayed()));

        onView(withId(R.id.search_src_text)).perform(clearText());
        onView(withId(R.id.primary_text)).check(matches(not(withText(SEARCH_ACCOUNT_NAME))));
    }

    /**
     * Tests that an account can be created successfully and that the account list is sorted alphabetically.
     */
    @Test
	public void testCreateAccount(){
        onView(allOf(isDisplayed(), withId(R.id.fab_create_account))).perform(click());

        String NEW_ACCOUNT_NAME = "A New Account";
        onView(withId(R.id.input_account_name)).perform(typeText(NEW_ACCOUNT_NAME), closeSoftKeyboard());
        sleep(1000);
        onView(withId(R.id.checkbox_placeholder_account))
                .check(matches(isNotChecked()))
                .perform(click());

        onView(withId(R.id.checkbox_parent_account)).perform(scrollTo())
                .check(matches(allOf(isDisplayed(), isNotChecked())))
                .perform(click());

        onView(withId(R.id.menu_save)).perform(click());

		List<Account> accounts = mAccountsDbAdapter.getAllRecords();
        assertThat(accounts).isNotNull();
        assertThat(accounts).hasSize(2);
		Account newestAccount = accounts.get(0); //because of alphabetical sorting

		assertThat(newestAccount.getName()).isEqualTo(NEW_ACCOUNT_NAME);
		assertThat(newestAccount.getCurrency().getCurrencyCode()).isEqualTo(Money.DEFAULT_CURRENCY_CODE);
        assertThat(newestAccount.isPlaceholderAccount()).isTrue();
	}

    @Test
    public void testChangeParentAccount() {
        final String accountName = "Euro Account";
        Account account = new Account(accountName, Currency.getInstance("EUR"));
        mAccountsDbAdapter.addRecord(account);

        refreshAccountsList();

        onView(withText(accountName)).perform(click());
        openActionBarOverflowOrOptionsMenu(mAcccountsActivity);
        onView(withText(R.string.title_edit_account)).perform(click());
        onView(withId(R.id.fragment_account_form)).check(matches(isDisplayed()));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.checkbox_parent_account)).perform(scrollTo())
                .check(matches(isNotChecked()))
                .perform(click());

        onView(withId(R.id.menu_save)).perform(click());

        Account editedAccount = mAccountsDbAdapter.getRecord(account.getUID());
        String parentUID = editedAccount.getParentUID();

        assertThat(parentUID).isNotNull();
        assertThat(DUMMY_ACCOUNT_UID).isEqualTo(parentUID);
    }

    /**
     * When creating a sub-account (starting from within another account), if we change the account
     * type to another type with no accounts of that type, then the parent account list should be hidden.
     * The account which is then created is not a sub-account, but rather a top-level account
     */
    @Test
    public void shouldHideParentAccountViewWhenNoParentsExist(){
        onView(allOf(withText(DUMMY_ACCOUNT_NAME), isDisplayed())).perform(click());
        onView(withId(R.id.fragment_transaction_list)).perform(swipeRight());
        onView(withId(R.id.fab_create_transaction)).check(matches(isDisplayed())).perform(click());
        sleep(1000);
        onView(withId(R.id.checkbox_parent_account)).check(matches(allOf(isChecked())));
        onView(withId(R.id.input_account_name)).perform(typeText("Trading account"));
        onView(withId(R.id.input_account_type_spinner)).perform(click());
        onData(allOf(is(instanceOf(String.class)), is(AccountType.TRADING.name()))).perform(click());

        onView(withId(R.id.layout_parent_account)).check(matches(not(isDisplayed())));
        onView(withId(R.id.menu_save)).perform(click());
        sleep(1000);
        //no sub-accounts
        assertThat(mAccountsDbAdapter.getSubAccountCount(DUMMY_ACCOUNT_UID)).isEqualTo(0);
        assertThat(mAccountsDbAdapter.getSubAccountCount(mAccountsDbAdapter.getOrCreateGnuCashRootAccountUID())).isEqualTo(2);
        assertThat(mAccountsDbAdapter.getSimpleAccountList()).extracting("mAccountType").contains(AccountType.TRADING);
    }

    @Test
	public void testEditAccount(){
		String editedAccountName = "Edited Account";
        sleep(2000);
        onView(withId(R.id.options_menu)).perform(click());
        onView(withText(R.string.title_edit_account)).perform(click());

        onView(withId(R.id.fragment_account_form)).check(matches(isDisplayed()));

        onView(withId(R.id.input_account_name)).perform(clearText()).perform(typeText(editedAccountName));

        onView(withId(R.id.menu_save)).perform(click());

		List<Account> accounts = mAccountsDbAdapter.getAllRecords();
		Account latest = accounts.get(0);  //will be the first due to alphabetical sorting

        assertThat(latest.getName()).isEqualTo(editedAccountName);
        assertThat(latest.getCurrency().getCurrencyCode()).isEqualTo(DUMMY_ACCOUNT_CURRENCY_CODE);
	}

    @Test
    public void editingAccountShouldNotDeleteTransactions(){
        onView(allOf(withId(R.id.options_menu), isDisplayed()))
                .perform(click());

        Account account = new Account("Transfer Account");
        account.setCurrency(DUMMY_ACCOUNT_CURRENCY);
        Transaction transaction = new Transaction("Simple trxn");
        transaction.setCurrencyCode(DUMMY_ACCOUNT_CURRENCY.getCurrencyCode());
        Split split = new Split(new Money(BigDecimal.TEN, DUMMY_ACCOUNT_CURRENCY), account.getUID());
        transaction.addSplit(split);
        transaction.addSplit(split.createPair(DUMMY_ACCOUNT_UID));
        account.addTransaction(transaction);
        mAccountsDbAdapter.addRecord(account);

        assertThat(mAccountsDbAdapter.getRecord(DUMMY_ACCOUNT_UID).getTransactionCount()).isEqualTo(1);
        assertThat(mSplitsDbAdapter.getSplitsForTransaction(transaction.getUID())).hasSize(2);

        onView(withText(R.string.title_edit_account)).perform(click());

        onView(withId(R.id.menu_save)).perform(click());
        assertThat(mAccountsDbAdapter.getRecord(DUMMY_ACCOUNT_UID).getTransactionCount()).isEqualTo(1);
        assertThat(mSplitsDbAdapter.fetchSplitsForAccount(DUMMY_ACCOUNT_UID).getCount()).isEqualTo(1);
        assertThat(mSplitsDbAdapter.getSplitsForTransaction(transaction.getUID())).hasSize(2);

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

    //TODO: Add test for moving content of accounts before deleting it
    @Test(expected = IllegalArgumentException.class)
	public void testDeleteSimpleAccount() {
        sleep(2000);
        onView(withId(R.id.options_menu)).perform(click());
        onView(withText(R.string.menu_delete)).perform(click());

        //the account has no sub-accounts
//        onView(withId(R.id.accounts_options)).check(matches(not(isDisplayed())));
//        onView(withId(R.id.transactions_options)).check(matches(isDisplayed()));

//        onView(withText(R.string.label_delete_transactions)).perform(click());
//        onView(withId(R.id.btn_save)).perform(click());

        //should throw expected exception
        mAccountsDbAdapter.getID(DUMMY_ACCOUNT_UID);;
    }

	//TODO: Test import of account file
    //TODO: test settings activity
    @Test
	public void testIntentAccountCreation(){
		Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.putExtra(Intent.EXTRA_TITLE, "Intent Account");
        intent.putExtra(Intent.EXTRA_UID, "intent-account");
        intent.putExtra(Account.EXTRA_CURRENCY_CODE, "EUR");
        intent.setType(Account.MIME_TYPE);

        new AccountCreator().onReceive(mAcccountsActivity, intent);

		Account account = mAccountsDbAdapter.getRecord("intent-account");
		assertThat(account).isNotNull();
        assertThat(account.getName()).isEqualTo("Intent Account");
        assertThat(account.getUID()).isEqualTo("intent-account");
        assertThat(account.getCurrency().getCurrencyCode()).isEqualTo("EUR");
	}

    /**
     * Tests that the setup wizard is displayed on first run
     */
    @Test
    public void shouldShowWizardOnFirstRun() throws Throwable {
        PreferenceManager.getDefaultSharedPreferences(mAcccountsActivity)
                .edit()
                .remove(mAcccountsActivity.getString(R.string.key_first_run))
                .commit();

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAcccountsActivity.recreate();
            }
        });

        //check that wizard is shown
        onView(withText(mAcccountsActivity.getString(R.string.title_setup_gnucash)))
                .check(matches(isDisplayed()));
    }

	@After
	public void tearDown() throws Exception {
        mAcccountsActivity.finish();
		super.tearDown();
	}

    /**
     * Refresh the account list fragment
     */
    private void refreshAccountsList(){
        try {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Fragment fragment = mAcccountsActivity.getCurrentAccountListFragment();
                    ((AccountsListFragment) fragment).refresh();
                }
            });
        } catch (Throwable throwable) {
            System.err.println("Failed to refresh fragment");
        }

    }
}
