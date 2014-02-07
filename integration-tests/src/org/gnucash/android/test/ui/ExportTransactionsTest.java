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

import java.io.File;

import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.data.Transaction;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.ui.accounts.AccountsActivity;
import org.gnucash.android.export.ExportDialogFragment;

import android.os.Environment;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.Spinner;

import com.jayway.android.robotium.solo.Solo;

public class ExportTransactionsTest extends
		ActivityInstrumentationTestCase2<AccountsActivity> {

	private Solo mSolo;
	
	public ExportTransactionsTest() {
		super(AccountsActivity.class);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mSolo = new Solo(getInstrumentation(), getActivity());	
		
		Account account = new Account("Exportable");		
		Transaction transaction = new Transaction("9.99", "Pizza");		
		transaction.setDescription("What up?");
		transaction.setTime(System.currentTimeMillis());
		
		account.addTransaction(transaction);
		
		AccountsDbAdapter adapter = new AccountsDbAdapter(getActivity());
		adapter.addAccount(account);
		adapter.close();	
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
		String filename = ExportDialogFragment.buildExportFilename(ExportFormat.OFX);
		
		mSolo.waitForDialogToClose(2000);
        mSolo.sleep(2000);
		
		
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
        String filename = ExportDialogFragment.buildExportFilename(ExportFormat.QIF);

        mSolo.waitForDialogToClose(10000);

        File file = new File(Environment.getExternalStorageDirectory() + "/gnucash/" + filename);
        assertNotNull(file);
        assertTrue(file.exists());

        //if this is not deleted, we cannot be certain that the next test will pass on its own merits
        boolean isDeleted = file.delete();
        assertTrue(isDeleted);
    }

	public void testDeleteTransactionsAfterExport(){
		TransactionsDbAdapter transAdapter = new TransactionsDbAdapter(getActivity());
		assertTrue(transAdapter.getAllTransactionsCount() != 0);
		
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

		assertEquals(0, transAdapter.getAllTransactionsCount());
		
		transAdapter.close();
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
		String filename = ExportDialogFragment.buildExportFilename(ExportFormat.OFX);
		
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
		String filename = ExportDialogFragment.buildExportFilename(ExportFormat.OFX);
		
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
		AccountsDbAdapter adapter = new AccountsDbAdapter(getActivity());
		adapter.deleteAllRecords();
		adapter.close();
		mSolo.finishOpenedActivities();
		super.tearDown();
	}
}
