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

import android.Manifest;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.util.Log;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.db.adapter.CommoditiesDbAdapter;
import org.gnucash.android.db.adapter.DatabaseAdapter;
import org.gnucash.android.db.adapter.SplitsDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.test.ui.util.GnucashAndroidTestRunner;
import org.gnucash.android.ui.account.AccountsActivity;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExportTransactionsTest  {

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private AccountsDbAdapter mAccountsDbAdapter;
    private TransactionsDbAdapter mTransactionsDbAdapter;
    private SplitsDbAdapter mSplitsDbAdapter;

	private AccountsActivity mAcccountsActivity;

	@Rule public GrantPermissionRule animationPermissionsRule = GrantPermissionRule.grant(Manifest.permission.SET_ANIMATION_SCALE);

	@Rule
	public ActivityTestRule<AccountsActivity> mActivityRule = new ActivityTestRule<>(AccountsActivity.class);

	public ExportTransactionsTest() {
	}

	@BeforeClass
	public static void setupClass(){
		GnucashAndroidTestRunner.preventFirstRunDialogs(GnuCashApplication.getAppContext());
	}

	@Before
	public void setUp() {
		mAcccountsActivity = mActivityRule.getActivity();

		String activeBookUID = BooksDbAdapter.getInstance().getActiveBookUID();
        mDbHelper = new DatabaseHelper(mAcccountsActivity.getApplicationContext(), activeBookUID);
        try {
            mDb = mDbHelper.getWritableDatabase();
        } catch (SQLException e) {
            Log.e(getClass().getName(), "Error getting database: " + e.getMessage());
            mDb = mDbHelper.getReadableDatabase();
        }

		mSplitsDbAdapter        = SplitsDbAdapter.getInstance();
		mTransactionsDbAdapter  = TransactionsDbAdapter.getInstance();
		mAccountsDbAdapter      = AccountsDbAdapter.getInstance();

		//this call initializes the static variables like DEFAULT_COMMODITY which are used implicitly by accounts/transactions
		@SuppressWarnings("unused")
		CommoditiesDbAdapter commoditiesDbAdapter = new CommoditiesDbAdapter(mDb);
		String currencyCode = GnuCashApplication.getDefaultCurrencyCode();
		Commodity.DEFAULT_COMMODITY = CommoditiesDbAdapter.getInstance().getCommodity(currencyCode);

		mAccountsDbAdapter.deleteAllRecords();

		Account account = new Account("Exportable");
		Transaction transaction = new Transaction("Pizza");
		transaction.setNote("What up?");
		transaction.setTime(System.currentTimeMillis());
        Split split = new Split(new Money("8.99", currencyCode), account.getUID());
		split.setMemo("Hawaii is the best!");
		transaction.addSplit(split);
		transaction.addSplit(split.createPair(
				mAccountsDbAdapter.getOrCreateImbalanceAccountUID(Commodity.DEFAULT_COMMODITY)));
		account.addTransaction(transaction);

		mAccountsDbAdapter.addRecord(account, DatabaseAdapter.UpdateMethod.insert);

	}

	@Test
	public void testCreateBackup(){
		onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
		onView(withId(R.id.nav_view)).perform(swipeUp());
		onView(withText(R.string.title_settings)).perform(click());
		onView(withText(R.string.header_backup_and_export_settings)).perform(click());

		onView(withText(R.string.title_create_backup_pref)).perform(click());
		assertToastDisplayed(R.string.toast_backup_successful);
	}

	/**
	 * Checks that a specific toast message is displayed
	 * @param toastString String that should be displayed
	 */
	private void assertToastDisplayed(int toastString) {
		onView(withText(toastString))
				.inRoot(withDecorView(not(is(mAcccountsActivity.getWindow().getDecorView()))))
				.check(matches(isDisplayed()));
	}

	//todo: add testing of export flag to unit test
	//todo: add test of ignore exported transactions to unit tests
	@After public void tearDown(){
        mDbHelper.close();
        mDb.close();
	}
}
