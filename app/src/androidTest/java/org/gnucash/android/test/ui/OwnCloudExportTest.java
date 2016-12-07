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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.db.adapter.CommoditiesDbAdapter;
import org.gnucash.android.db.adapter.DatabaseAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.ui.account.AccountsActivity;
import org.junit.Assume;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.Currency;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.gnucash.android.test.ui.AccountsActivityTest.preventFirstRunDialogs;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;


@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OwnCloudExportTest {

    private static final String LOG_TAG = "OwnCloudExportTest";

    private AccountsActivity mAccountsActivity;
    private SharedPreferences mPrefs;

    private String OC_SERVER = "https://demo.owncloud.org";
    private String OC_USERNAME = "test";
    private String OC_PASSWORD = "test";
    private String OC_DIR = "gc_test";

    /**
     * A JUnit {@link Rule @Rule} to launch your activity under test. This is a replacement
     * for {@link ActivityInstrumentationTestCase2}.
     * <p>
     * Rules are interceptors which are executed for each test method and will run before
     * any of your setup code in the {@link Before @Before} method.
     * <p>
     * {@link ActivityTestRule} will create and launch of the activity for you and also expose
     * the activity under test. To get a reference to the activity you can use
     * the {@link ActivityTestRule#getActivity()} method.
     */
    @Rule
    public ActivityTestRule<AccountsActivity> mActivityRule = new ActivityTestRule<>(
            AccountsActivity.class);


    @Before
    public void setUp() throws Exception {

        mAccountsActivity = mActivityRule.getActivity();
        mPrefs = mAccountsActivity.getSharedPreferences(
                mAccountsActivity.getString(R.string.owncloud_pref), Context.MODE_PRIVATE);

        preventFirstRunDialogs(getInstrumentation().getTargetContext());

        // creates Account and transaction
        String activeBookUID = BooksDbAdapter.getInstance().getActiveBookUID();
        DatabaseHelper mDbHelper = new DatabaseHelper(mAccountsActivity, activeBookUID);
        SQLiteDatabase mDb;
        try {
            mDb = mDbHelper.getWritableDatabase();
        } catch (SQLException e) {
            Log.e(LOG_TAG, "Error getting database: " + e.getMessage(), e);
            mDb = mDbHelper.getReadableDatabase();
        }

        @SuppressWarnings("unused") //this call initializes constants in Commodity
        CommoditiesDbAdapter commoditiesDbAdapter = new CommoditiesDbAdapter(mDb);
        AccountsDbAdapter mAccountsDbAdapter = AccountsDbAdapter.getInstance();
        mAccountsDbAdapter.deleteAllRecords();

        String currencyCode = GnuCashApplication.getDefaultCurrencyCode();
        Commodity.DEFAULT_COMMODITY = CommoditiesDbAdapter.getInstance().getCommodity(currencyCode);

        Account account = new Account("ownCloud");
        Transaction transaction = new Transaction("birds");
        transaction.setTime(System.currentTimeMillis());
        Split split = new Split(new Money("11.11", currencyCode), account.getUID());
        transaction.addSplit(split);
        transaction.addSplit(split.createPair(
                mAccountsDbAdapter.getOrCreateImbalanceAccountUID(Currency.getInstance(currencyCode))));
        account.addTransaction(transaction);

        mAccountsDbAdapter.addRecord(account, DatabaseAdapter.UpdateMethod.insert);


        SharedPreferences.Editor editor = mPrefs.edit();

        editor.putBoolean(mAccountsActivity.getString(R.string.key_owncloud_sync), false).apply();
        editor.putInt(mAccountsActivity.getString(R.string.key_last_export_destination), 0);
        editor.apply();
    }

    /**
     * Test if there is an active internet connection on the device/emulator
     * @return {@code true} is an internet connection is available, {@code false} otherwise
     */
    public static boolean hasActiveInternetConnection(){
        ConnectivityManager connectivityManager
                = (ConnectivityManager) GnuCashApplication.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * It might fail if it takes too long to connect to the server or if there is no network
     */
    @Test
    public void OwnCloudCredentials() {
        Assume.assumeTrue(hasActiveInternetConnection());
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withId(R.id.nav_view)).perform(swipeUp());
        onView(withText(R.string.title_settings)).perform(click());
        onView(withText(R.string.header_backup_and_export_settings)).perform(click());
        onView(withText(R.string.title_owncloud_sync_preference)).perform(click());
        onView(withId(R.id.owncloud_hostname)).check(matches(isDisplayed()));

        onView(withId(R.id.owncloud_hostname)).perform(clearText()).perform(typeText(OC_SERVER), closeSoftKeyboard());
        onView(withId(R.id.owncloud_username)).perform(clearText()).perform(typeText(OC_USERNAME), closeSoftKeyboard());
        onView(withId(R.id.owncloud_password)).perform(clearText()).perform(typeText(OC_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.owncloud_dir)).perform(clearText()).perform(typeText(OC_DIR), closeSoftKeyboard());
        onView(withId(R.id.btn_save)).perform(click());
        sleep(5000);
        onView(withId(R.id.btn_save)).perform(click());

        assertEquals(mPrefs.getString(mAccountsActivity.getString(R.string.key_owncloud_server), null), OC_SERVER);
        assertEquals(mPrefs.getString(mAccountsActivity.getString(R.string.key_owncloud_username), null), OC_USERNAME);
        assertEquals(mPrefs.getString(mAccountsActivity.getString(R.string.key_owncloud_password), null), OC_PASSWORD);
        assertEquals(mPrefs.getString(mAccountsActivity.getString(R.string.key_owncloud_dir), null), OC_DIR);

        assertTrue(mPrefs.getBoolean(mAccountsActivity.getString(R.string.key_owncloud_sync), false));
    }

    @Test
    public void OwnCloudExport() {
        Assume.assumeTrue(hasActiveInternetConnection());
        mPrefs.edit().putBoolean(mAccountsActivity.getString(R.string.key_owncloud_sync), true).commit();

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withText(R.string.nav_menu_export)).perform(click());
        onView(withId(R.id.spinner_export_destination)).perform(click());
        String[] destinations = mAccountsActivity.getResources().getStringArray(R.array.export_destinations);
        onView(withText(destinations[3])).perform(click());
        onView(withId(R.id.menu_save)).perform(click());
//        onView(withSpinnerText(
//                mAccountsActivity.getResources().getStringArray(R.array.export_destinations)[3]))
//                .perform(click());
        assertToastDisplayed(String.format(mAccountsActivity.getString(R.string.toast_exported_to), "ownCloud -> " + OC_DIR));
    }

    /**
     * Checks that a specific toast message is displayed
     * @param toastString String that should be displayed
     */
    private void assertToastDisplayed(String toastString) {
        onView(withText(toastString))
                .inRoot(withDecorView(not(is(mActivityRule.getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));
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

