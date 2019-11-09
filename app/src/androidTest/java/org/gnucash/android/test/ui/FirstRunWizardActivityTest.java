/*
 * Copyright (c) 2015 Ngewi Fet <ngewif@gmail.com>
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
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.SplitsDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.model.BaseModel;
import org.gnucash.android.ui.wizard.FirstRunWizardActivity;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the first run wizard
 * @author Ngewi Fet
 */
@RunWith(AndroidJUnit4.class)
public class FirstRunWizardActivityTest {

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private AccountsDbAdapter mAccountsDbAdapter;
    private TransactionsDbAdapter mTransactionsDbAdapter;
    private SplitsDbAdapter mSplitsDbAdapter;

    FirstRunWizardActivity mActivity;

    @Rule public GrantPermissionRule animationPermissionsRule = GrantPermissionRule.grant(Manifest.permission.SET_ANIMATION_SCALE);

    @Rule
    public ActivityTestRule<FirstRunWizardActivity> mActivityRule = new ActivityTestRule<>(FirstRunWizardActivity.class);

    @Before
    public void setUp() {
        mActivity = mActivityRule.getActivity();
        mDbHelper = new DatabaseHelper(mActivity, BaseModel.generateUID());
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
    }


    @Test
    public void shouldRunWizardToEnd(){
        assertThat(mAccountsDbAdapter.getRecordsCount()).isEqualTo(0);

        onView(withId(R.id.btn_save)).perform(click());

        onView(withText("EUR")).perform(click());
        onView(withText(R.string.btn_wizard_next)).perform(click());
        onView(withText(R.string.wizard_title_account_setup)).check(matches(isDisplayed()));

        onView(withText(R.string.wizard_option_create_default_accounts)).perform(click());

        onView(withText(R.string.btn_wizard_next)).perform(click());
        onView(withText(R.string.wizard_option_auto_send_crash_reports)).perform(click());
        onView(withText(R.string.btn_wizard_next)).perform(click());

        onView(withText(R.string.review)).check(matches(isDisplayed()));

        onView(withId(R.id.btn_save)).perform(click());

        //default accounts should be created
        long actualCount = GnuCashApplication.getAccountsDbAdapter().getRecordsCount();
        assertThat(actualCount).isGreaterThan(60L);

        boolean enableCrashlytics = GnuCashApplication.isCrashlyticsEnabled();
        assertThat(enableCrashlytics).isTrue();

        String defaultCurrencyCode = GnuCashApplication.getDefaultCurrencyCode();
        assertThat(defaultCurrencyCode).isEqualTo("EUR");
    }

    @Test
    public void shouldDisplayFullCurrencyList(){
        assertThat(mAccountsDbAdapter.getRecordsCount()).isEqualTo(0);

        onView(withId(R.id.btn_save)).perform(click());

        onView(withText(R.string.wizard_option_currency_other)).perform(click());
        onView(withText(R.string.btn_wizard_next)).perform(click());
        onView(withText(R.string.wizard_title_select_currency)).check(matches(isDisplayed()));

//        onData(allOf(is(instanceOf(String.class)), is("CHF")))
//                .inAdapterView(withTagValue(is((Object)"currency_list_view"))).perform(click());
        onView(withText("AFA - Afghani")).perform(click());
        onView(withId(R.id.btn_save)).perform(click());

        onView(withText(R.string.wizard_option_let_me_handle_it)).perform(click());

        onView(withText(R.string.btn_wizard_next)).perform(click());
        onView(withText(R.string.wizard_option_disable_crash_reports)).perform(click());
        onView(withText(R.string.btn_wizard_next)).perform(click());

        onView(withText(R.string.review)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_save)).perform(click());

        //default accounts should not be created
        assertThat(mAccountsDbAdapter.getRecordsCount()).isZero();

        boolean enableCrashlytics = GnuCashApplication.isCrashlyticsEnabled();
        assertThat(enableCrashlytics).isFalse();

        String defaultCurrencyCode = GnuCashApplication.getDefaultCurrencyCode();
        assertThat(defaultCurrencyCode).isEqualTo("AFA");
    }
}
