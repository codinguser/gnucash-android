/*
 * Copyright (c) 2015 Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
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
import android.content.Context;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.CoordinatesProvider;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.view.View;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.db.adapter.CommoditiesDbAdapter;
import org.gnucash.android.db.adapter.DatabaseAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.importer.GncXmlImporter;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;
import org.gnucash.android.test.ui.util.DisableAnimationsRule;
import org.gnucash.android.ui.report.BaseReportFragment;
import org.gnucash.android.ui.report.ReportsActivity;
import org.gnucash.android.ui.report.piechart.PieChartFragment;
import org.gnucash.android.ui.settings.PreferenceActivity;
import org.gnucash.android.util.BookUtils;
import org.joda.time.LocalDateTime;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.Locale;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(AndroidJUnit4.class)
public class PieChartReportTest {

    public static final String TAG = PieChartReportTest.class.getName();

    private static final String TRANSACTION_NAME = "Pizza";
    private static final double TRANSACTION_AMOUNT = 9.99;

    private static final String TRANSACTION2_NAME = "1984";
    private static final double TRANSACTION2_AMOUNT = 12.49;

    private static final String TRANSACTION3_NAME = "Nice gift";
    private static final double TRANSACTION3_AMOUNT = 2000.00;

    private static final String CASH_IN_WALLET_ASSET_ACCOUNT_UID = "b687a487849470c25e0ff5aaad6a522b";

    private static final String DINING_EXPENSE_ACCOUNT_UID = "62922c5ccb31d6198259739d27d858fe";
    private static final String DINING_EXPENSE_ACCOUNT_NAME = "Dining";

    private static final String BOOKS_EXPENSE_ACCOUNT_UID = "a8b342435aceac7c3cac214f9385dd72";
    private static final String BOOKS_EXPENSE_ACCOUNT_NAME = "Books";

    private static final String GIFTS_RECEIVED_INCOME_ACCOUNT_UID = "b01950c0df0890b6543209d51c8e0b0f";
    private static final String GIFTS_RECEIVED_INCOME_ACCOUNT_NAME = "Gifts Received";

    public static Commodity CURRENCY;

    private static AccountsDbAdapter mAccountsDbAdapter;
    private static TransactionsDbAdapter mTransactionsDbAdapter;

    private ReportsActivity mReportsActivity;

    @Rule
    public ActivityTestRule<ReportsActivity> mActivityRule = new ActivityTestRule<>(ReportsActivity.class);

    @Rule public GrantPermissionRule animationPermissionsRule = GrantPermissionRule.grant(Manifest.permission.SET_ANIMATION_SCALE);

    @ClassRule
    public static DisableAnimationsRule disableAnimationsRule = new DisableAnimationsRule();

    private static String testBookUID;
    private static String oldActiveBookUID;


    public PieChartReportTest() {
        //nothing to se here, move along
        CURRENCY = new Commodity("US Dollars", "USD", 100);
	}

    @BeforeClass
    public static void prepareTestCase() throws Exception {
        Context context = GnuCashApplication.getAppContext();
        oldActiveBookUID = BooksDbAdapter.getInstance().getActiveBookUID();
        testBookUID = GncXmlImporter.parse(context.getResources().openRawResource(R.raw.default_accounts));

        BookUtils.loadBook(testBookUID);
        mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();
        mAccountsDbAdapter = AccountsDbAdapter.getInstance();

        CURRENCY = CommoditiesDbAdapter.getInstance().getCommodity("USD");

        PreferenceActivity.getActiveBookSharedPreferences().edit()
                .putString(context.getString(R.string.key_default_currency), CURRENCY.getCurrencyCode())
                .commit();
    }
	

	@Before
	public void setUp() throws Exception {
        mTransactionsDbAdapter.deleteAllRecords();
        mReportsActivity = mActivityRule.getActivity();
        assertThat(mAccountsDbAdapter.getRecordsCount()).isGreaterThan(20); //lots of accounts in the default
        onView(withId(R.id.btn_pie_chart)).perform(click());
	}

    /**
     * Add a transaction for the current month in order to test the report view
     * @throws Exception
     */
    private void addTransactionForCurrentMonth() throws Exception {
        Transaction transaction = new Transaction(TRANSACTION_NAME);
        transaction.setTime(System.currentTimeMillis());

        Split split = new Split(new Money(BigDecimal.valueOf(TRANSACTION_AMOUNT), CURRENCY), DINING_EXPENSE_ACCOUNT_UID);
        split.setType(TransactionType.DEBIT);

        transaction.addSplit(split);
        transaction.addSplit(split.createPair(CASH_IN_WALLET_ASSET_ACCOUNT_UID));

        mTransactionsDbAdapter.addRecord(transaction, DatabaseAdapter.UpdateMethod.insert);
    }

    /**
     * Add a transactions for the previous month for testing pie chart
     * @param minusMonths Number of months prior
     */
    private void addTransactionForPreviousMonth(int minusMonths) {
        Transaction transaction = new Transaction(TRANSACTION2_NAME);
        transaction.setTime(new LocalDateTime().minusMonths(minusMonths).toDate().getTime());

        Split split = new Split(new Money(BigDecimal.valueOf(TRANSACTION2_AMOUNT), CURRENCY), BOOKS_EXPENSE_ACCOUNT_UID);
        split.setType(TransactionType.DEBIT);

        transaction.addSplit(split);
        transaction.addSplit(split.createPair(CASH_IN_WALLET_ASSET_ACCOUNT_UID));

        mTransactionsDbAdapter.addRecord(transaction, DatabaseAdapter.UpdateMethod.insert);
    }


    @Test
    public void testNoData() {
        onView(withId(R.id.pie_chart)).perform(click());
        onView(withId(R.id.selected_chart_slice)).check(matches(withText(R.string.label_select_pie_slice_to_see_details)));
    }

    @Test
    public void testSelectingValue() throws Exception {
        addTransactionForCurrentMonth();
        addTransactionForPreviousMonth(1);
        refreshReport();

        onView(withId(R.id.pie_chart)).perform(clickXY(Position.BEGIN, Position.MIDDLE));
        float percent = (float) (TRANSACTION_AMOUNT / (TRANSACTION_AMOUNT + TRANSACTION2_AMOUNT) * 100);
        String selectedText = String.format(Locale.US, BaseReportFragment.SELECTED_VALUE_PATTERN, DINING_EXPENSE_ACCOUNT_NAME, TRANSACTION_AMOUNT, percent);
        onView(withId(R.id.selected_chart_slice)).check(matches(withText(selectedText)));
    }

    @Test
    public void testSpinner() throws Exception {
        Split split = new Split(new Money(BigDecimal.valueOf(TRANSACTION3_AMOUNT), CURRENCY), GIFTS_RECEIVED_INCOME_ACCOUNT_UID);
        Transaction transaction = new Transaction(TRANSACTION3_NAME);
        transaction.addSplit(split);
        transaction.addSplit(split.createPair(CASH_IN_WALLET_ASSET_ACCOUNT_UID));

        mTransactionsDbAdapter.addRecord(transaction, DatabaseAdapter.UpdateMethod.insert);

        refreshReport();

        Thread.sleep(1000);

        onView(withId(R.id.report_account_type_spinner)).perform(click());
        onView(withText(AccountType.INCOME.name())).perform(click());
        onView(withId(R.id.pie_chart)).perform(clickXY(Position.BEGIN, Position.MIDDLE));
        String selectedText = String.format(PieChartFragment.SELECTED_VALUE_PATTERN, GIFTS_RECEIVED_INCOME_ACCOUNT_NAME, TRANSACTION3_AMOUNT, 100f);
        onView(withId(R.id.selected_chart_slice)).check(matches(withText(selectedText)));

        onView(withId(R.id.report_account_type_spinner)).perform(click());
        onView(withText(AccountType.EXPENSE.name())).perform(click());

        onView(withId(R.id.pie_chart)).perform(click());
        onView(withId(R.id.selected_chart_slice)).check(matches(withText(R.string.label_select_pie_slice_to_see_details)));
    }

    public static ViewAction clickXY(final Position horizontal, final Position vertical){
        return new GeneralClickAction(
                Tap.SINGLE,
                new CoordinatesProvider() {
                    @Override
                    public float[] calculateCoordinates(View view) {
                        int[] xy = new int[2];
                        view.getLocationOnScreen(xy);

                        float x = horizontal.getPosition(xy[0], view.getWidth());
                        float y = vertical.getPosition(xy[1], view.getHeight());
                        return new float[]{x, y};
                    }
                },
                Press.FINGER);
    }

    private enum Position {
        BEGIN {
            @Override
            public float getPosition(int viewPos, int viewLength) {
                return viewPos + (viewLength * 0.15f);
            }
        },
        MIDDLE {
            @Override
            public float getPosition(int viewPos, int viewLength) {
                return viewPos + (viewLength * 0.5f);
            }
        },
        END {
            @Override
            public float getPosition(int viewPos, int viewLength) {
                return viewPos + (viewLength * 0.85f);
            }
        };

        abstract float getPosition(int widgetPos, int widgetLength);
    }

    /**
     * Refresh reports
     */
    private void refreshReport(){
        try {
            mActivityRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mReportsActivity.refresh();
                }
            });
        } catch (Throwable t){
            System.err.println("Faile to refresh reports");
        }
    }

	@After
	public void tearDown() throws Exception {
		mReportsActivity.finish();
	}

    @AfterClass
    public static void cleanup(){
        BooksDbAdapter booksDbAdapter = BooksDbAdapter.getInstance();
        booksDbAdapter.setActive(oldActiveBookUID);
        booksDbAdapter.deleteRecord(testBookUID);
    }
}
