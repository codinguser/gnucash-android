

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
import org.gnucash.android.ui.chart.PieChartActivity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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


        setActivityIntent(new Intent(Intent.ACTION_VIEW));
		mPieChartActivity = getActivity();
	}

    @Test
    public void testNoData() {
        Log.w(TAG, "testWhenNoData");

        onView(withId(R.id.chart_date)).check(matches(withText("Overall")));
        onView(withId(R.id.chart_date)).check(matches(not(isEnabled())));

        onView(withId(R.id.previous_month_chart_button)).check(matches(not(isEnabled())));
        onView(withId(R.id.next_month_chart_button)).check(matches(not(isEnabled())));

        onView(withId(R.id.pie_chart)).perform(click());
        onView(withId(R.id.selected_chart_slice)).check(matches(withText("")));
    }


    @Override
	@After
	public void tearDown() throws Exception {
		mPieChartActivity.finish();
		super.tearDown();
	}

}
