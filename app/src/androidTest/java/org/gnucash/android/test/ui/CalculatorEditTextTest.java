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

import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.transaction.TransactionsActivity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

// TODO: Find out how to press the keys in the KeyboardView.
@RunWith(AndroidJUnit4.class)
public class CalculatorEditTextTest extends
		ActivityInstrumentationTestCase2<TransactionsActivity> {
	private static final String DUMMY_ACCOUNT_UID = "transactions-account";
	private static final String DUMMY_ACCOUNT_NAME = "Transactions Account";

    private static final String TRANSFER_ACCOUNT_NAME   = "Transfer account";
    private static final String TRANSFER_ACCOUNT_UID    = "transfer_account";
    public static final String CURRENCY_CODE = "USD";

    private SQLiteDatabase mDb;
    private DatabaseHelper mDbHelper;
    private AccountsDbAdapter mAccountsDbAdapter;
    private TransactionsDbAdapter mTransactionsDbAdapter;
    private SplitsDbAdapter mSplitsDbAdapter;
	private TransactionsActivity mTransactionsActivity;

	public CalculatorEditTextTest() {
		super(TransactionsActivity.class);
	}
	
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		injectInstrumentation(InstrumentationRegistry.getInstrumentation());
		AccountsActivityTest.preventFirstRunDialogs(getInstrumentation().getTargetContext());


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
		mAccountsDbAdapter.deleteAllRecords();

        Account account = new Account(DUMMY_ACCOUNT_NAME);
        account.setUID(DUMMY_ACCOUNT_UID);
        account.setCommodity(Commodity.getInstance(CURRENCY_CODE));

        Account account2 = new Account(TRANSFER_ACCOUNT_NAME);
        account2.setUID(TRANSFER_ACCOUNT_UID);
        account2.setCommodity(Commodity.getInstance(CURRENCY_CODE));

        mAccountsDbAdapter.addRecord(account);
        mAccountsDbAdapter.addRecord(account2);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, DUMMY_ACCOUNT_UID);
        setActivityIntent(intent);
		mTransactionsActivity = getActivity();
	}

    /**
     * Checks the calculator keyboard is showed/hided as expected.
     */
    @Test
    public void testShowingHidingOfCalculatorKeyboard() {
        clickOnView(R.id.fab_create_transaction);

        // Giving the focus to the amount field shows the keyboard
        onView(withId(R.id.input_transaction_amount)).perform(click());
        onView(withId(R.id.calculator_keyboard)).check(matches(isDisplayed()));

        // Pressing back hides the keyboard (still with focus)
        pressBack();
        onView(withId(R.id.calculator_keyboard)).check(matches(not(isDisplayed())));

        // Clicking the amount field already focused shows the keyboard again
        clickOnView(R.id.input_transaction_amount);
        onView(withId(R.id.calculator_keyboard)).check(matches(isDisplayed()));

        // Changing the focus to another field hides the keyboard
        clickOnView(R.id.input_transaction_name);
        onView(withId(R.id.calculator_keyboard)).check(matches(not(isDisplayed())));
    }

	/**
	 * Simple wrapper for clicking on views with espresso
	 * @param viewId View resource ID
	 */
	private void clickOnView(int viewId){
		onView(withId(viewId)).perform(click());
	}

	@Override
	@After
	public void tearDown() throws Exception {
		mTransactionsActivity.finish();
		super.tearDown();
	}
}
