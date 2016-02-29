/*
 * Copyright (c) 2016 Felipe Morato <me@fmorato.com>
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
import android.content.SharedPreferences;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.adapter.*;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.ui.settings.PreferenceActivity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Currency;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.*;


@RunWith(AndroidJUnit4.class)
public class OwnCloudExportTest extends ActivityInstrumentationTestCase2<PreferenceActivity> {

    private PreferenceActivity mPreferenceActivity;
    private SharedPreferences mPrefs;

    public OwnCloudExportTest() { super(PreferenceActivity.class); }

    @Override
	@Before
	public void setUp() throws Exception {
        super.setUp();
		injectInstrumentation(InstrumentationRegistry.getInstrumentation());
		AccountsActivityTest.preventFirstRunDialogs(getInstrumentation().getTargetContext());

        mPreferenceActivity = getActivity();

        // creates Account and transaction
        String activeBookUID = BooksDbAdapter.getInstance().getActiveBookUID();
        DatabaseHelper mDbHelper = new DatabaseHelper(getActivity(), activeBookUID);
        SQLiteDatabase mDb;
        try {
            mDb = mDbHelper.getWritableDatabase();
        } catch (SQLException e) {
            Log.e(getClass().getName(), "Error getting database: " + e.getMessage());
            mDb = mDbHelper.getReadableDatabase();
        }
        SplitsDbAdapter mSplitsDbAdapter;
        mSplitsDbAdapter = new SplitsDbAdapter(mDb);
        TransactionsDbAdapter mTransactionsDbAdapter = new TransactionsDbAdapter(mDb, mSplitsDbAdapter);
        AccountsDbAdapter mAccountsDbAdapter = new AccountsDbAdapter(mDb, mTransactionsDbAdapter);
        mAccountsDbAdapter.deleteAllRecords();

        String currencyCode = GnuCashApplication.getDefaultCurrencyCode();
        Account account = new Account("ownCloud", new CommoditiesDbAdapter(mDb).getCommodity(currencyCode));
        Transaction transaction = new Transaction("birds");
        transaction.setTime(System.currentTimeMillis());
        Split split = new Split(new Money("11.11", currencyCode), account.getUID());
        transaction.addSplit(split);
        transaction.addSplit(split.createPair(mAccountsDbAdapter.getOrCreateImbalanceAccountUID(Currency.getInstance(currencyCode))));
        account.addTransaction(transaction);

        mAccountsDbAdapter.addRecord(account, DatabaseAdapter.UpdateMethod.insert);

    }

    @Test
    public void OpenOwnCloudDialog() {
        pressBack(); // The activity automatically opens General Settings. . let's go back first
        onView(withText("Backup & export")).perform(click());
        onView(withText("ownCloud Sync")).perform(click());
        onView(withId(R.id.owncloud_hostname)).check(matches(isDisplayed()));
    }

    @Test
    public void SetOwnCloudCredentials() {
        pressBack(); // The activity automatically opens General Settings. . let's go back first
        onView(withText("Backup & export")).perform(click());
        onView(withText("ownCloud Sync")).perform(click());
        onView(withId(R.id.owncloud_hostname)).check(matches(isDisplayed()));

        String OC_SERVER = "https://demo.owncloud.org";
        String OC_USERNAME = "test";
        String OC_PASSWORD = "test";
        String OC_DIR = "gc_test";

        onView(withId(R.id.owncloud_hostname)).perform(clearText()).perform(typeText(OC_SERVER), closeSoftKeyboard());
        onView(withId(R.id.owncloud_username)).perform(clearText()).perform(typeText(OC_USERNAME), closeSoftKeyboard());
        onView(withId(R.id.owncloud_password)).perform(clearText()).perform(typeText(OC_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.owncloud_dir)).perform(clearText()).perform(typeText(OC_DIR), closeSoftKeyboard());
        onView(withId(R.id.btn_save)).perform(click());
        sleep(5000);
        onView(withId(R.id.btn_save)).perform(click());

        mPrefs = mPreferenceActivity.getSharedPreferences(mPreferenceActivity.getString(R.string.owncloud_pref), Context.MODE_PRIVATE);
        assertEquals(mPrefs.getString(mPreferenceActivity.getString(R.string.key_owncloud_server), null), OC_SERVER);
        assertEquals(mPrefs.getString(mPreferenceActivity.getString(R.string.key_owncloud_username), null), OC_USERNAME);
        assertEquals(mPrefs.getString(mPreferenceActivity.getString(R.string.key_owncloud_password), null), OC_PASSWORD);
        assertEquals(mPrefs.getString(mPreferenceActivity.getString(R.string.key_owncloud_dir), null), OC_DIR);

    }

    @After
    public void tearDown() throws Exception {
        mPreferenceActivity.finish();
        super.tearDown();
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
}

