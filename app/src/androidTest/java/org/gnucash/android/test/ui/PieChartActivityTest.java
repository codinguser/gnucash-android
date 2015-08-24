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

import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.CoordinatesProvider;
import android.support.test.espresso.action.GeneralClickAction;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Tap;
import android.support.test.espresso.contrib.PickerActions;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.importer.GncXmlImporter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;
import org.gnucash.android.ui.chart.PieChartActivity;
import org.joda.time.LocalDateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.Currency;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class PieChartActivityTest extends ActivityInstrumentationTestCase2<PieChartActivity> {

    public static final String TAG = PieChartActivityTest.class.getName();

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

    public static final Currency CURRENCY = Currency.getInstance("USD");

    private AccountsDbAdapter mAccountsDbAdapter;
    private TransactionsDbAdapter mTransactionsDbAdapter;

    private PieChartActivity mPieChartActivity;

	public PieChartActivityTest() {
		super(PieChartActivity.class);
	}
	
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		injectInstrumentation(InstrumentationRegistry.getInstrumentation());

        SQLiteDatabase db;
        DatabaseHelper dbHelper = new DatabaseHelper(getInstrumentation().getTargetContext());
        try {
            db = dbHelper.getWritableDatabase();
        } catch (SQLException e) {
            Log.e(TAG, "Error getting database: " + e.getMessage());
            db = dbHelper.getReadableDatabase();
        }
        mTransactionsDbAdapter = new TransactionsDbAdapter(db, new SplitsDbAdapter(db));
        mAccountsDbAdapter = new AccountsDbAdapter(db, mTransactionsDbAdapter);
        mAccountsDbAdapter.deleteAllRecords();

        // creates default accounts
        GncXmlImporter.parse(GnuCashApplication.getAppContext().getResources().openRawResource(R.raw.default_accounts));
	}

    /**
     * Call this method in every tests after adding data
     */
    private void getTestActivity() {
        setActivityIntent(new Intent(Intent.ACTION_VIEW));
        mPieChartActivity = getActivity();
    }

    private void addTransactionForCurrentMonth() throws Exception {
        Transaction transaction = new Transaction(TRANSACTION_NAME);
        transaction.setTime(System.currentTimeMillis());

        Split split = new Split(new Money(BigDecimal.valueOf(TRANSACTION_AMOUNT), CURRENCY), DINING_EXPENSE_ACCOUNT_UID);
        split.setType(TransactionType.DEBIT);

        transaction.addSplit(split);
        transaction.addSplit(split.createPair(CASH_IN_WALLET_ASSET_ACCOUNT_UID));

        Account account = mAccountsDbAdapter.getRecord(DINING_EXPENSE_ACCOUNT_UID);
        account.addTransaction(transaction);
        mTransactionsDbAdapter.addRecord(transaction);
    }

    private void addTransactionForPreviousMonth(int minusMonths) {
        Transaction transaction = new Transaction(TRANSACTION2_NAME);
        transaction.setTime(new LocalDateTime().minusMonths(minusMonths).toDate().getTime());

        Split split = new Split(new Money(BigDecimal.valueOf(TRANSACTION2_AMOUNT), CURRENCY), BOOKS_EXPENSE_ACCOUNT_UID);
        split.setType(TransactionType.DEBIT);

        transaction.addSplit(split);
        transaction.addSplit(split.createPair(CASH_IN_WALLET_ASSET_ACCOUNT_UID));

        Account account = mAccountsDbAdapter.getRecord(BOOKS_EXPENSE_ACCOUNT_UID);
        account.addTransaction(transaction);
        mTransactionsDbAdapter.addRecord(transaction);
    }


    @Test
    public void testNoData() {
        getTestActivity();

        onView(withId(R.id.chart_date)).check(matches(withText("Overall")));
        onView(withId(R.id.chart_date)).check(matches(not(isEnabled())));

        onView(withId(R.id.previous_month_chart_button)).check(matches(not(isEnabled())));
        onView(withId(R.id.next_month_chart_button)).check(matches(not(isEnabled())));

        onView(withId(R.id.pie_chart)).perform(click());
        onView(withId(R.id.selected_chart_slice)).check(matches(withText("")));
    }

    @Test
    public void testSelectingValue() throws Exception {
        addTransactionForCurrentMonth();
        addTransactionForPreviousMonth(1);
        getTestActivity();

        onView(withId(R.id.pie_chart)).perform(clickXY(Position.BEGIN, Position.MIDDLE));
        float percent = (float) (TRANSACTION_AMOUNT / (TRANSACTION_AMOUNT + TRANSACTION2_AMOUNT) * 100);
        String selectedText = String.format(PieChartActivity.SELECTED_VALUE_PATTERN, DINING_EXPENSE_ACCOUNT_NAME, TRANSACTION_AMOUNT, percent);
        onView(withId(R.id.selected_chart_slice)).check(matches(withText(selectedText)));
    }

    @Test
    public void testDatePicker() throws Exception {
        addTransactionForCurrentMonth();
        addTransactionForPreviousMonth(1);
        addTransactionForPreviousMonth(2);
        getTestActivity();

        onView(withId(R.id.chart_date)).check(matches(isEnabled()));
        onView(withId(R.id.chart_date)).check(matches(withText("Overall")));
        onView(withId(R.id.chart_date)).perform(click());

        LocalDateTime date = new LocalDateTime().minusMonths(2);
        onView(withClassName(equalTo(DatePicker.class.getName()))).perform(PickerActions.setDate(date.getYear(), date.getMonthOfYear(), 1));
        onView(anyOf(withText(containsString("Done")), withText(containsString("Set")), withText(containsString("OK")))).perform(click());
        
        onView(withId(R.id.chart_date)).check(matches(withText(date.toString(PieChartActivity.DATE_PATTERN))));
    }

    @Test
    public void testPreviousAndNextMonthButtons() throws Exception {
        addTransactionForCurrentMonth();
        addTransactionForPreviousMonth(1);
        addTransactionForPreviousMonth(2);
        getTestActivity();

        onView(withId(R.id.previous_month_chart_button)).check(matches(isEnabled()));
        onView(withId(R.id.next_month_chart_button)).check(matches(not(isEnabled())));

        onView(withId(R.id.previous_month_chart_button)).perform(click());

        onView(withId(R.id.previous_month_chart_button)).check(matches(isEnabled()));
        onView(withId(R.id.next_month_chart_button)).check(matches(isEnabled()));

        onView(withId(R.id.previous_month_chart_button)).perform(click());

        onView(withId(R.id.previous_month_chart_button)).check(matches(not(isEnabled())));
        onView(withId(R.id.next_month_chart_button)).check(matches(isEnabled()));

        onView(withId(R.id.next_month_chart_button)).perform(click());

        onView(withId(R.id.previous_month_chart_button)).check(matches(isEnabled()));
        onView(withId(R.id.next_month_chart_button)).check(matches(isEnabled()));

        onView(withId(R.id.next_month_chart_button)).perform(click());

        onView(withId(R.id.previous_month_chart_button)).check(matches(isEnabled()));
        onView(withId(R.id.next_month_chart_button)).check(matches(not(isEnabled())));
    }

    @Test
    public void testSpinner() throws Exception {
        Split split = new Split(new Money(BigDecimal.valueOf(TRANSACTION3_AMOUNT), CURRENCY), GIFTS_RECEIVED_INCOME_ACCOUNT_UID);
        Transaction transaction = new Transaction(TRANSACTION3_NAME);
        transaction.addSplit(split);
        transaction.addSplit(split.createPair(CASH_IN_WALLET_ASSET_ACCOUNT_UID));

        mAccountsDbAdapter.getRecord(GIFTS_RECEIVED_INCOME_ACCOUNT_UID).addTransaction(transaction);
        mTransactionsDbAdapter.addRecord(transaction);

        getTestActivity();

        onView(withId(R.id.chart_data_spinner)).perform(click());
        onView(withText(containsString(AccountType.INCOME.name()))).perform(click());

        onView(withId(R.id.pie_chart)).perform(click());
        String selectedText = String.format(PieChartActivity.SELECTED_VALUE_PATTERN, GIFTS_RECEIVED_INCOME_ACCOUNT_NAME, TRANSACTION3_AMOUNT, 100f);
        onView(withId(R.id.selected_chart_slice)).check(matches(withText(selectedText)));

        onView(withId(R.id.chart_data_spinner)).perform(click());
        onView(withText(containsString(AccountType.EXPENSE.name()))).perform(click());

        onView(withId(R.id.pie_chart)).perform(click());
        onView(withId(R.id.selected_chart_slice)).check(matches(withText("")));
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

    @Override
	@After
	public void tearDown() throws Exception {
		mPieChartActivity.finish();
		super.tearDown();
	}

}
