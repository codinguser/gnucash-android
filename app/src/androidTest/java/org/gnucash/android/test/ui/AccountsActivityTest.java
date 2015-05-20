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
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
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

import java.util.Currency;
import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isNotChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class AccountsActivityTest extends ActivityInstrumentationTestCase2<AccountsActivity> {
	private static final String DUMMY_ACCOUNT_CURRENCY_CODE = "USD";
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
		
		Account account = new Account(DUMMY_ACCOUNT_NAME);
        account.setUID(DUMMY_ACCOUNT_UID);
		account.setCurrency(Currency.getInstance(DUMMY_ACCOUNT_CURRENCY_CODE));
		mAccountsDbAdapter.addAccount(account);
        refreshAccountsList();
	}

    /**
     * Prevents the first-run dialogs (Whats new, Create accounts etc) from being displayed when testing
     * @param context Application context
     */
    public static void preventFirstRunDialogs(Context context) {
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

    /*
        public void testDisplayAccountsList(){
            final int NUMBER_OF_ACCOUNTS = 15;
            for (int i = 0; i < NUMBER_OF_ACCOUNTS; i++) {
                Account account = new Account("Acct " + i);
                mAccountsDbAdapter.addAccount(account);
            }

            //there should exist a listview of accounts
            refreshAccountsList();
            mSolo.waitForText("Acct");
            mSolo.scrollToBottom();

            ListView accountsListView = (ListView) mSolo.getView(android.R.id.list);
            assertNotNull(accountsListView);
            assertEquals(NUMBER_OF_ACCOUNTS + 1, accountsListView.getCount());
        }
    */
    @Test
    public void testSearchAccounts(){
        String SEARCH_ACCOUNT_NAME = "Search Account";

        Account account = new Account(SEARCH_ACCOUNT_NAME);
        account.setParentUID(DUMMY_ACCOUNT_UID);
        mAccountsDbAdapter.addAccount(account);

        //enter search query
//        ActionBarUtils.clickSherlockActionBarItem(mSolo, R.id.menu_search);
        onView(withId(R.id.menu_search)).perform(click());
        onView(withId(R.id.abs__search_src_text)).perform(typeText("Se"));
        onView(withText(SEARCH_ACCOUNT_NAME)).check(matches(isDisplayed()));

        onView(withId(R.id.abs__search_src_text)).perform(clearText());
        onView(withId(R.id.primary_text)).check(matches(not(withText(SEARCH_ACCOUNT_NAME))));
    }

    /**
     * Tests that an account can be created successfully and that the account list is sorted alphabetically.
     */
    @Test
	public void testCreateAccount(){
        onView(withId(R.id.menu_add_account)).check(matches(isDisplayed())).perform(click());

        onView(withId(R.id.checkbox_transaction))
//                .check(matches(allOf(isDisplayed(), isNotChecked())))
                .perform(click());

        String NEW_ACCOUNT_NAME = "A New Account";
        onView(withId(R.id.input_account_name)).perform(typeText(NEW_ACCOUNT_NAME));
        onView(withId(R.id.checkbox_placeholder_account))
                .check(matches(isNotChecked()))
                .perform(click());
        onView(withId(R.id.menu_save)).perform(click());

        //check displayed
//        onView(withId(android.R.id.list)).check(matches(hasDescendant(withText(NEW_ACCOUNT_NAME))));

		List<Account> accounts = mAccountsDbAdapter.getAllAccounts();
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
        mAccountsDbAdapter.addAccount(account);

        refreshAccountsList();
//        onView(withId(android.R.id.list))
//                .check(matches(allOf(isDisplayed(), hasDescendant(withText(accountName)))));

        onView(withText(accountName)).perform(longClick());
        onView(withId(R.id.context_menu_edit_accounts)).perform(click());
        onView(withId(R.id.fragment_account_form)).check(matches(isDisplayed()));
        onView(withId(R.id.checkbox_transaction))
                .check(matches(isNotChecked()))
                .perform(click());

        onView(withId(R.id.menu_save)).perform(click());

        Account editedAccount = mAccountsDbAdapter.getAccount(account.getUID());
        String parentUID = editedAccount.getParentUID();

        assertThat(parentUID).isNotNull();
        assertThat(DUMMY_ACCOUNT_UID).isEqualTo(parentUID);
    }

    @Test
	public void testEditAccount(){
		String editedAccountName = "Edited Account";
//		onView(withText(DUMMY_ACCOUNT_NAME)).perform(longClick());
		onView(withId(R.id.primary_text)).perform(longClick());
        onView(withId(R.id.context_menu_edit_accounts)).perform(click());

        onView(withId(R.id.fragment_account_form)).check(matches(isDisplayed()));

        onView(withId(R.id.input_account_name)).perform(clearText()).perform(typeText(editedAccountName));

        onView(withId(R.id.menu_save)).perform(click());

        //test refresh
//        onView(withId(android.R.id.empty))
//                .check(matches(not(isDisplayed())));

		List<Account> accounts = mAccountsDbAdapter.getAllAccounts();
		Account latest = accounts.get(0);  //will be the first due to alphabetical sorting

        assertThat(latest.getName()).isEqualTo(editedAccountName);
        assertThat(latest.getCurrency().getCurrencyCode()).isEqualTo(DUMMY_ACCOUNT_CURRENCY_CODE);
	}

    //TODO: Add test for moving content of accounts before deleting it
    @Test(expected = IllegalArgumentException.class)
	public void testDeleteAccount() {
        Transaction transaction = new Transaction("hats");
        transaction.addSplit(new Split(Money.getZeroInstance(), DUMMY_ACCOUNT_UID));
        mTransactionsDbAdapter.addTransaction(transaction);

        onView(withId(R.id.primary_text)).perform(longClick());
        onView(withId(R.id.context_menu_delete)).perform(click());
        onView(withText(R.string.label_delete_sub_accounts)).perform(click());
        onView(withId(R.id.btn_save)).perform(click());

        //should throw expected exception
        mAccountsDbAdapter.getID(DUMMY_ACCOUNT_UID);

        List<Transaction> transactions = mTransactionsDbAdapter.getAllTransactionsForAccount(DUMMY_ACCOUNT_UID);
        assertThat(transactions).hasSize(0);
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

        AccountCreator accountCreator = new AccountCreator();
        accountCreator.onReceive(mAcccountsActivity, intent);

		Account account = mAccountsDbAdapter.getAccount("intent-account");
		assertNotNull(account);
		assertEquals("Intent Account", account.getName());
		assertEquals("intent-account", account.getUID());
		assertEquals("EUR", account.getCurrency().getCurrencyCode());
	}
	
	@After
	public void tearDown() throws Exception {
        mAcccountsActivity.finish();
        Thread.sleep(1000);
        mAccountsDbAdapter.deleteAllRecords();
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
