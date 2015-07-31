

package org.gnucash.android.test.ui;

import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

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
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class PieChartActivityTest extends ActivityInstrumentationTestCase2<PieChartActivity> {

    public static final String TAG = PieChartActivityTest.class.getName();

    private static final String TRANSACTION_NAME = "Pizza";
    private static final double TRANSACTION_AMOUNT = 9.99;

    private static final String CASH_IN_WALLET_INCOME_ACCOUNT_UID = "b687a487849470c25e0ff5aaad6a522b";
    private static final String DINING_EXPENSE_ACCOUNT_UID = "62922c5ccb31d6198259739d27d858fe";
    private static final String DINING_EXPENSE_ACCOUNT_NAME = "Dining";

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
        transaction.setNote("What up?");
        transaction.setTime(System.currentTimeMillis());

        Split split = new Split(new Money(BigDecimal.valueOf(TRANSACTION_AMOUNT), CURRENCY), DINING_EXPENSE_ACCOUNT_UID);
        split.setType(TransactionType.DEBIT);

        transaction.addSplit(split);
        transaction.addSplit(split.createPair(CASH_IN_WALLET_INCOME_ACCOUNT_UID));

        Account account = mAccountsDbAdapter.getAccount(DINING_EXPENSE_ACCOUNT_UID);
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

    @Override
	@After
	public void tearDown() throws Exception {
		mPieChartActivity.finish();
		super.tearDown();
	}

}
