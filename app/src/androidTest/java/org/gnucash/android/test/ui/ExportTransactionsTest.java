/*
 * Copyright (c) 2012 Ngewi Fet <ngewif@gmail.com>
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

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.robotium.solo.Solo;

import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.ScheduledActionDbAdapter;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.PeriodType;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.ui.account.AccountsActivity;

import java.io.File;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

public class ExportTransactionsTest extends
		ActivityInstrumentationTestCase2<AccountsActivity> {

	private Solo mSolo;
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private AccountsDbAdapter mAccountsDbAdapter;
    private TransactionsDbAdapter mTransactionsDbAdapter;
    private SplitsDbAdapter mSplitsDbAdapter;

    public ExportTransactionsTest() {
		super(AccountsActivity.class);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mSolo = new Solo(getInstrumentation(), getActivity());

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

		Account account = new Account("Exportable");		
		Transaction transaction = new Transaction("Pizza");
		transaction.setNote("What up?");
		transaction.setTime(System.currentTimeMillis());
        Split split = new Split(new Money("8.99", "USD"), account.getUID());
		split.setMemo("Hawaii is the best!");
        transaction.addSplit(split);
		transaction.addSplit(split.createPair(mAccountsDbAdapter.getOrCreateImbalanceAccountUID(Currency.getInstance("USD"))));
		account.addTransaction(transaction);
		
		mAccountsDbAdapter.addAccount(account);
	}
	
	/**
	 * Tests the export of an OFX file with the transactions from the application.
	 * The exported file name contains a timestamp with minute precision.
	 * If this test fails, it may be due to the file being created and tested in different minutes of the clock
	 * Just try rerunning it again.
	 */
	public void testOfxExport(){
        testExport(ExportFormat.OFX);
	}

	/**
	 * Test the export of transactions in the QIF format
	 */
	public void testQifExport(){
		testExport(ExportFormat.QIF);
	}

	public void testXmlExport(){
		testExport(ExportFormat.XML);
	}

	/**
	 * Generates export for the specified format and tests that the file actually is created
	 * @param format
	 */
    public void testExport(ExportFormat format){
		File folder = new File(Exporter.EXPORT_FOLDER_PATH);
		folder.mkdirs();
		for (File file : folder.listFiles()) {
			file.delete();
		}

		mSolo.setNavigationDrawer(Solo.OPENED);
		mSolo.clickOnText(mSolo.getString(R.string.nav_menu_export));

        mSolo.waitForText(getActivity().getString(R.string.menu_export_transactions));
		mSolo.waitForDialogToOpen();
		mSolo.clickOnText(format.name());
		mSolo.clickOnButton(mSolo.getString(R.string.btn_export));

        mSolo.waitForDialogToClose();
		mSolo.sleep(2000); //sleep so that emulators can save the file

		assertThat(folder.listFiles().length).isEqualTo(1);
		File exportFile = folder.listFiles()[0];
		assertThat(exportFile.getName()).endsWith(format.getExtension());
    }

	public void testDeleteTransactionsAfterExport(){
		assertThat(mTransactionsDbAdapter.getAllTransactionsCount()).isGreaterThan(0);

		PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
				.putBoolean(mSolo.getString(R.string.key_delete_transactions_after_export), true).commit();

		testExport(ExportFormat.QIF);

		assertThat(mTransactionsDbAdapter.getAllTransactionsCount()).isEqualTo(0);
		PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
				.putBoolean(mSolo.getString(R.string.key_delete_transactions_after_export), false).commit();
	}

	/**
	 * Test creating a scheduled export
	 */
	public void testCreateExportSchedule(){
		mSolo.setNavigationDrawer(Solo.OPENED);
		mSolo.clickOnText(mSolo.getString(R.string.nav_menu_export));
		mSolo.waitForText(getActivity().getString(R.string.menu_export_transactions));

		mSolo.clickOnText(ExportFormat.XML.name());
		mSolo.clickOnView(mSolo.getView(R.id.input_recurrence));

		mSolo.clickOnToggleButton("OFF");
		mSolo.clickOnText("Done");
		mSolo.waitForDialogToClose();

		mSolo.clickOnButton(mSolo.getString(R.string.btn_export));
		mSolo.waitForDialogToClose();

		mSolo.sleep(2000); //wait for database save

		ScheduledActionDbAdapter scheduledactionDbAdapter = new ScheduledActionDbAdapter(mDb);
		assertThat(scheduledactionDbAdapter.getAllEnabledScheduledActions())
				.hasSize(1)
				.extracting("mActionType").contains(ScheduledAction.ActionType.BACKUP);

		ScheduledAction action = scheduledactionDbAdapter.getAllScheduledActions().get(0);
		assertThat(action.getPeriodType()).isEqualTo(PeriodType.WEEK);
		assertThat(action.getEndTime()).isEqualTo(0);
	}

	//todo: add testing of export flag to unit test
	//todo: add test of ignore exported transactions to unit tests
	@Override
	protected void tearDown() throws Exception {
		mSolo.finishOpenedActivities();
		mSolo.waitForEmptyActivityStack(10000);
		mAccountsDbAdapter.deleteAllRecords();
        mDbHelper.close();
        mDb.close();
		super.tearDown();
	}
}
