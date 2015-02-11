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
import android.os.Environment;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.Spinner;
import com.robotium.solo.Solo;
import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.ui.account.AccountsActivity;

import java.io.File;

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
        transaction.addSplit(split);
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
        mSolo.clickOnActionBarItem(R.id.menu_export);
//		ActionBarUtils.clickSherlockActionBarItem(mSolo, R.id.menu_export);

		mSolo.waitForText(getActivity().getString(R.string.menu_export_transactions));
		Spinner spinner = mSolo.getCurrentViews(Spinner.class).get(0);
		mSolo.clickOnView(spinner);
		String[] options = getActivity().getResources().getStringArray(R.array.export_destinations);	
		mSolo.clickOnText(options[1]);
        mSolo.clickOnRadioButton(1);

        mSolo.clickOnView(mSolo.getView(R.id.btn_save));
		
		//the file name is time-based (down to the minute), so we cache it here, 
		//as close as possible to the export itself to minimize difference
		String filename = Exporter.buildExportFilename(ExportFormat.OFX);
		
		mSolo.waitForDialogToClose(2000);
        mSolo.sleep(5000);
		
		
		File file = new File(Environment.getExternalStorageDirectory() + "/gnucash/" + filename);
		assertNotNull(file);
		assertTrue(file.exists());
		
		//if this is not deleted, we cannot be certain that the next test will pass on its own merits
		boolean isDeleted = file.delete();
		assertTrue(isDeleted);
	}

    /**
     * Test the export of transactions in the QIF format
     */
    public void testQifExport(){
        mSolo.clickOnActionBarItem(R.id.menu_export);
//		ActionBarUtils.clickSherlockActionBarItem(mSolo, R.id.menu_export);

        mSolo.waitForText(getActivity().getString(R.string.menu_export_transactions));
        Spinner spinner = mSolo.getCurrentViews(Spinner.class).get(0);
        mSolo.clickOnView(spinner);
        String[] options = getActivity().getResources().getStringArray(R.array.export_destinations);
        mSolo.clickOnText(options[1]);
        mSolo.clickOnRadioButton(0);
        mSolo.clickOnView(mSolo.getView(R.id.btn_save));

        //the file name is time-based (down to the minute), so we cache it here,
        //as close as possible to the export itself to minimize difference
        String filename = Exporter.buildExportFilename(ExportFormat.QIF);

        mSolo.waitForDialogToClose(10000);

        File file = new File(Environment.getExternalStorageDirectory() + "/gnucash/" + filename);
        assertNotNull(file);
//        assertTrue(file.exists());

        //if this is not deleted, we cannot be certain that the next test will pass on its own merits
        boolean isDeleted = file.delete();
        mSolo.sleep(1000);
        assertTrue(isDeleted);
    }

	public void testDeleteTransactionsAfterExport(){
		assertTrue(mTransactionsDbAdapter.getAllTransactionsCount() != 0);
		
        mSolo.clickOnActionBarItem(R.id.menu_export);
//        ActionBarUtils.clickSherlockActionBarItem(mSolo, R.id.menu_export);

		mSolo.waitForText(getActivity().getString(R.string.menu_export_transactions));
		Spinner spinner = mSolo.getCurrentViews(Spinner.class).get(0);
		mSolo.clickOnView(spinner);
		String[] options = getActivity().getResources().getStringArray(R.array.export_destinations);	
		mSolo.clickOnText(options[1]);
        mSolo.clickOnRadioButton(1);

		//check to delete after export
		mSolo.clickOnCheckBox(1);

        mSolo.clickOnView(mSolo.getView(R.id.btn_save));
		mSolo.waitForDialogToClose(2000);

        String deleteConfirm = getActivity().getString(R.string.alert_dialog_ok_delete);
        mSolo.clickOnText(deleteConfirm);
		mSolo.waitForDialogToClose(1000);
        mSolo.sleep(1000);

		assertEquals(0, mTransactionsDbAdapter.getAllTransactionsCount());
		
        mSolo.goBack();
	}
	
	public void testShouldIgnoreExportedTransactions(){
		testOfxExport();
        mSolo.clickOnActionBarItem(R.id.menu_export);
//		ActionBarUtils.clickSherlockActionBarItem(mSolo, R.id.menu_export);

		mSolo.waitForText(getActivity().getString(R.string.menu_export_transactions));
		Spinner spinner = mSolo.getCurrentViews(Spinner.class).get(0);
		mSolo.clickOnView(spinner);
		String[] options = getActivity().getResources().getStringArray(R.array.export_destinations);	
		mSolo.clickOnText(options[1]);
        mSolo.clickOnRadioButton(1);

		mSolo.clickOnCheckBox(0);
		mSolo.clickOnView(mSolo.getView(R.id.btn_save));

		//the file name is time-based (down to the minute), so we cache it here, 
		//as close as possible to the export itself to minimize chance of a different name 
		//due to a different minute
		String filename = Exporter.buildExportFilename(ExportFormat.OFX);
		
		mSolo.waitForDialogToClose(10000);
				
		File file = new File(Environment.getExternalStorageDirectory() + "/gnucash/" + filename);
		assertNotNull(file);
		assertTrue(file.exists());
		//there should be something in the file (OFX headers, etc)
		assertTrue(file.length() > 0);
		
		//if this is not deleted, we cannot be certain that the next test will pass on its own merits
		boolean isDeleted = file.delete();
		assertTrue(isDeleted);		
	}
	
	public void testExportAlreadyExportedTransactions(){
		testOfxExport();
        mSolo.clickOnActionBarItem(R.id.menu_export);
//		ActionBarUtils.clickSherlockActionBarItem(mSolo, R.id.menu_export);

		mSolo.waitForText(getActivity().getString(R.string.menu_export_transactions));
		Spinner spinner = mSolo.getCurrentViews(Spinner.class).get(0);
		mSolo.clickOnView(spinner);
		String[] options = getActivity().getResources().getStringArray(R.array.export_destinations);	
		mSolo.clickOnText(options[1]);
        mSolo.clickOnRadioButton(1);

		mSolo.clickOnCheckBox(0);
        mSolo.clickOnView(mSolo.getView(R.id.btn_save));
		
		//the file name is time-based (down to the minute), so we cache it here, 
		//as close as possible to the export itself to minimize chance of a different name 
		//due to a different minute
		String filename = Exporter.buildExportFilename(ExportFormat.OFX);
		
		mSolo.waitForDialogToClose(10000);		
		
		File file = new File(Environment.getExternalStorageDirectory() + "/gnucash/" + filename);
		assertNotNull(file);
		//the file will exist but not contain any account information
		assertTrue(file.exists());

		//if this is not deleted, we cannot be certain that the next test will pass on its own merits
		boolean isDeleted = file.delete();
		assertTrue(isDeleted);		
	}
	
	public void testValidityOfExport(){
		//TODO: Validate exported file contents with an XML schema, if possible
	}
	
	@Override
	protected void tearDown() throws Exception {
		mAccountsDbAdapter.deleteAllRecords();
        mDbHelper.close();
        mDb.close();
		mSolo.finishOpenedActivities();
		super.tearDown();
	}
}
