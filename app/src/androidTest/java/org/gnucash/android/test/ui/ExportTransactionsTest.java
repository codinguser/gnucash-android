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
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.contrib.DrawerActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.CompoundButton;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.ScheduledActionDbAdapter;
import org.gnucash.android.db.adapter.SplitsDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.PeriodType;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.ui.account.AccountsActivity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Currency;
import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
public class ExportTransactionsTest extends
		ActivityInstrumentationTestCase2<AccountsActivity> {

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private AccountsDbAdapter mAccountsDbAdapter;
    private TransactionsDbAdapter mTransactionsDbAdapter;
    private SplitsDbAdapter mSplitsDbAdapter;

	private AccountsActivity mAcccountsActivity;

    public ExportTransactionsTest() {
		super(AccountsActivity.class);
	}
	
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		injectInstrumentation(InstrumentationRegistry.getInstrumentation());
		AccountsActivityTest.preventFirstRunDialogs(getInstrumentation().getTargetContext());
		mAcccountsActivity = getActivity();

        mDbHelper = new DatabaseHelper(getActivity());
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

		Account account = new Account("Exportable");
		Transaction transaction = new Transaction("Pizza");
		transaction.setNote("What up?");
		transaction.setTime(System.currentTimeMillis());
		String currencyCode = GnuCashApplication.getDefaultCurrencyCode();
        Split split = new Split(new Money("8.99", currencyCode), account.getUID());
		split.setMemo("Hawaii is the best!");
		transaction.addSplit(split);
		transaction.addSplit(split.createPair(mAccountsDbAdapter.getOrCreateImbalanceAccountUID(Currency.getInstance(currencyCode))));
		account.addTransaction(transaction);

		mAccountsDbAdapter.addRecord(account);

	}
	
	/**
	 * Tests the export of an OFX file with the transactions from the application.
	 * The exported file name contains a timestamp with minute precision.
	 * If this test fails, it may be due to the file being created and tested in different minutes of the clock
	 * Just try rerunning it again.
	 */
	@Test
	public void testOfxExport(){
		PreferenceManager.getDefaultSharedPreferences(mAcccountsActivity)
				.edit().putBoolean(mAcccountsActivity.getString(R.string.key_use_double_entry), false)
				.commit();
        testExport(ExportFormat.OFX);
		PreferenceManager.getDefaultSharedPreferences(mAcccountsActivity)
				.edit().putBoolean(mAcccountsActivity.getString(R.string.key_use_double_entry), true)
				.commit();
	}

	@Test
	public void shouldNotOfferXmlExportInSingleEntryMode(){
		PreferenceManager.getDefaultSharedPreferences(mAcccountsActivity)
				.edit().putBoolean(mAcccountsActivity.getString(R.string.key_use_double_entry), false)
				.commit();

		DrawerActions.openDrawer(R.id.drawer_layout);
		onView(withText(R.string.nav_menu_export)).perform(click());
		onView(withId(R.id.radio_xml_format)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

		PreferenceManager.getDefaultSharedPreferences(mAcccountsActivity)
				.edit().putBoolean(mAcccountsActivity.getString(R.string.key_use_double_entry), true)
				.commit();
	}

	/**
	 * Test the export of transactions in the QIF format
	 */
	@Test
	public void testQifExport(){
		testExport(ExportFormat.QIF);
	}

	@Test
	public void testXmlExport(){
		testExport(ExportFormat.XML);
	}

	/**
	 * Generates export for the specified format and tests that the file actually is created
	 * @param format Export format to use
	 */
    public void testExport(ExportFormat format){
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (mAcccountsActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
					!= PackageManager.PERMISSION_GRANTED) {
				mAcccountsActivity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
						Manifest.permission.READ_EXTERNAL_STORAGE}, 0x23);

				onView(withId(android.R.id.button1)).perform(click());
			}
		}

		File folder = new File(Exporter.EXPORT_FOLDER_PATH);
		folder.mkdirs();
		assertThat(folder).exists();

		for (File file : folder.listFiles()) {
			file.delete();
		}

		onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
		onView(withText(R.string.nav_menu_export)).perform(click());
		onView(withText(format.name())).perform(click());

		onView(withId(R.id.menu_save)).perform(click());

		assertThat(folder.listFiles().length).isEqualTo(1);
		File exportFile = folder.listFiles()[0];
		assertThat(exportFile.getName()).endsWith(format.getExtension());
    }

	@Test
	public void testDeleteTransactionsAfterExport(){
		assertThat(mTransactionsDbAdapter.getRecordsCount()).isGreaterThan(0);

		PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
				.putBoolean(mAcccountsActivity.getString(R.string.key_delete_transactions_after_export), true).commit();

		testExport(ExportFormat.XML);

		assertThat(mTransactionsDbAdapter.getRecordsCount()).isEqualTo(0);
		PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
				.putBoolean(mAcccountsActivity.getString(R.string.key_delete_transactions_after_export), false).commit();
	}

	/**
	 * Test creating a scheduled export
	 * Does not work on Travis yet
	 */
	@Test
	public void shouldCreateExportSchedule(){
		onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
		onView(withText(R.string.nav_menu_export)).perform(click());

		onView(withText(ExportFormat.XML.name())).perform(click());
		onView(withId(R.id.input_recurrence)).perform(click());

		//switch on recurrence dialog
		onView(allOf(isAssignableFrom(CompoundButton.class), isDisplayed(), isEnabled())).perform(click());
		onView(withText("OK")).perform(click());

		onView(withId(R.id.menu_save)).perform(click());
		ScheduledActionDbAdapter scheduledactionDbAdapter = new ScheduledActionDbAdapter(mDb);
		List<ScheduledAction> scheduledActions = scheduledactionDbAdapter.getAllEnabledScheduledActions();
		assertThat(scheduledActions)
				.hasSize(1)
				.extracting("mActionType").contains(ScheduledAction.ActionType.BACKUP);

		ScheduledAction action = scheduledActions.get(0);
		assertThat(action.getRecurrence().getPeriodType()).isEqualTo(PeriodType.WEEK);
		assertThat(action.getEndTime()).isEqualTo(0);
	}

	//todo: add testing of export flag to unit test
	//todo: add test of ignore exported transactions to unit tests
	@Override
	@After public void tearDown() throws Exception {
        mDbHelper.close();
        mDb.close();
		super.tearDown();
	}
}
