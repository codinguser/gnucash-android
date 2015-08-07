

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
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.importer.GncXmlImporter;
import org.gnucash.android.model.Account;
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
import java.util.Arrays;
import java.util.Currency;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class PieChartActivityTest extends ActivityInstrumentationTestCase2<PieChartActivity> {

    public static final String TAG = PieChartActivityTest.class.getName();

    private static final String TRANSACTION_NAME = "Pizza";
    private static final double TRANSACTION_AMOUNT = 9.99;

    private static final String TRANSACTION2_NAME = "1984";
    private static final double TRANSACTION2_AMOUNT = 34.49;

    private static final String CASH_IN_WALLET_ASSET_ACCOUNT_UID = "b687a487849470c25e0ff5aaad6a522b";

    private static final String DINING_EXPENSE_ACCOUNT_UID = "62922c5ccb31d6198259739d27d858fe";
    private static final String DINING_EXPENSE_ACCOUNT_NAME = "Dining";

    private static final String BOOKS_EXPENSE_ACCOUNT_UID = "a8b342435aceac7c3cac214f9385dd72";
    private static final String BOOKS_EXPENSE_ACCOUNT_NAME = "Books";

    public static final Currency CURRENCY = Currency.getInstance("USD");

    private AccountsDbAdapter mAccountsDbAdapter;
    private TransactionsDbAdapter mTransactionsDbAdapter;
    private SplitsDbAdapter mSplitsDbAdapter;

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
        mSplitsDbAdapter = new SplitsDbAdapter(db);
        mTransactionsDbAdapter = new TransactionsDbAdapter(db, mSplitsDbAdapter);
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

        Account account = mAccountsDbAdapter.getAccount(DINING_EXPENSE_ACCOUNT_UID);
        account.addTransaction(transaction);
        mTransactionsDbAdapter.addTransaction(transaction);
    }

    private void addTransactionForPreviousMonth(int minusMonths) {
        Transaction transaction = new Transaction(TRANSACTION2_NAME);
        transaction.setTime(new LocalDateTime().minusMonths(minusMonths).toDate().getTime());

        Split split = new Split(new Money(BigDecimal.valueOf(TRANSACTION2_AMOUNT), CURRENCY), BOOKS_EXPENSE_ACCOUNT_UID);
        split.setType(TransactionType.DEBIT);

        transaction.addSplit(split);
        transaction.addSplit(split.createPair(CASH_IN_WALLET_ASSET_ACCOUNT_UID));

        Account account = mAccountsDbAdapter.getAccount(BOOKS_EXPENSE_ACCOUNT_UID);
        account.addTransaction(transaction);
        mTransactionsDbAdapter.addTransaction(transaction);
    }


    @Test
    public void testNoData() {
        Log.w(TAG, "testWhenNoData()");
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
        Log.w(TAG, "testSelectingValue()");
        addTransactionForCurrentMonth();
        addTransactionForPreviousMonth(1);
        getTestActivity();

        onView(withId(R.id.pie_chart)).perform(click());
        float percent = (float) (TRANSACTION_AMOUNT / (TRANSACTION_AMOUNT + TRANSACTION2_AMOUNT) * 100);
        String selectedText = String.format(PieChartActivity.SELECTED_VALUE_PATTERN, DINING_EXPENSE_ACCOUNT_NAME, TRANSACTION_AMOUNT, percent);
        onView(withId(R.id.selected_chart_slice)).check(matches(withText(selectedText)));
    }

    @Test
    public void testDataForCurrentMonth() throws Exception {
        Log.w(TAG, "testDataForCurrentMonth()");
        addTransactionForCurrentMonth();
        getTestActivity();

        onView(withId(R.id.chart_date)).check(matches(withText("Overall")));
        onView(withId(R.id.previous_month_chart_button)).check(matches(not(isEnabled())));
        onView(withId(R.id.next_month_chart_button)).check(matches(not(isEnabled())));

        onView(withId(R.id.pie_chart)).perform(click());
        String selectedText = String.format(PieChartActivity.SELECTED_VALUE_PATTERN, DINING_EXPENSE_ACCOUNT_NAME, TRANSACTION_AMOUNT, 100f);
        onView(withId(R.id.selected_chart_slice)).check(matches(withText(selectedText)));

    }

    @Test
    public void testWhenDataForPreviousAndCurrentMonth() throws Exception {
        Log.w(TAG, "testWhenDataForPreviousAndCurrentMonth");
        addTransactionForCurrentMonth();
        addTransactionForPreviousMonth(1);
        getTestActivity();

        onView(withId(R.id.chart_date)).check(matches(withText("Overall")));
        onView(withId(R.id.previous_month_chart_button)).check(matches(isEnabled()));
        onView(withId(R.id.next_month_chart_button)).check(matches(not(isEnabled())));

        onView(withId(R.id.pie_chart)).perform(click());
//        clickXY(Position.END, Position.MIDDLE)

        float percent = (float) (TRANSACTION2_AMOUNT / (TRANSACTION_AMOUNT + TRANSACTION2_AMOUNT) * 100);
        String selectedText = String.format(PieChartActivity.SELECTED_VALUE_PATTERN, BOOKS_EXPENSE_ACCOUNT_NAME, TRANSACTION2_AMOUNT, percent);
        onView(withId(R.id.selected_chart_slice)).check(matches(withText(selectedText)));
    }

    public static ViewAction clickXY(final Position horizontal, final Position vertical){
        return new GeneralClickAction(
                Tap.SINGLE,
                new CoordinatesProvider() {
                    @Override
                    public float[] calculateCoordinates(View view) {
                        final int[] xy = new int[2];
                        view.getLocationOnScreen(xy);
                        Log.w("Test", Arrays.toString(xy));
                        Log.w("Test", view.getHeight() + ", " + view.getWidth());

                        final float x = horizontal.getPosition(xy[0], view.getWidth());
                        final float y = vertical.getPosition(xy[1], view.getHeight());

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

    @Test
    public void testWhenDataForTwoPreviousAndCurrentMonth() throws Exception {
        Log.w(TAG, "testWhenDataForTwoPreviousAndCurrentMonth");
        addTransactionForCurrentMonth();
        addTransactionForPreviousMonth(1);
        addTransactionForPreviousMonth(2);
        getTestActivity();

        onView(withId(R.id.chart_date)).check(matches(withText("Overall")));
        onView(withId(R.id.previous_month_chart_button)).check(matches(isEnabled()));
        onView(withId(R.id.next_month_chart_button)).check(matches(not(isEnabled())));

        onView(withId(R.id.pie_chart)).perform(click());
        float percent = (float) (TRANSACTION_AMOUNT / (TRANSACTION_AMOUNT + TRANSACTION2_AMOUNT * 2) * 100);
        String selectedText = String.format(PieChartActivity.SELECTED_VALUE_PATTERN, DINING_EXPENSE_ACCOUNT_NAME, TRANSACTION_AMOUNT, percent);
        onView(withId(R.id.selected_chart_slice)).check(matches(withText(selectedText)));

        onView(withId(R.id.previous_month_chart_button)).perform(click());
        onView(withId(R.id.previous_month_chart_button)).check(matches(isEnabled()));
        onView(withId(R.id.next_month_chart_button)).check(matches(isEnabled()));
    }


    @Override
	@After
	public void tearDown() throws Exception {
		mPieChartActivity.finish();
		super.tearDown();
	}

}
