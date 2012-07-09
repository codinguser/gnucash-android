/*
 * Written By: Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2012 Ngewi Fet
 *
 * This file is part of Gnucash for Android
 * 
 * Gnucash for Android is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, contact:
 *
 * Free Software Foundation           Voice:  +1-617-542-5942
 * 51 Franklin Street, Fifth Floor    Fax:    +1-617-542-2652
 * Boston, MA  02110-1301,  USA       gnu@gnu.org
 */

package org.gnucash.android.test;

import java.io.File;

import org.gnucash.android.R;
import org.gnucash.android.ui.AccountsActivity;
import org.gnucash.android.ui.ExportDialogFragment;

import android.os.Environment;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Spinner;

import com.jayway.android.robotium.solo.Solo;

public class OfxExportTest extends
		ActivityInstrumentationTestCase2<AccountsActivity> {

	private Solo mSolo;
	
	public OfxExportTest() {
		super(AccountsActivity.class);
	}
	
	@Override
	protected void setUp() throws Exception {
		mSolo = new Solo(getInstrumentation(), getActivity());
		super.setUp();
	}
	
	public void testOfxExport(){
		mSolo.clickOnActionBarItem(R.id.menu_export);
		
		mSolo.waitForText("Export OFX");
		Spinner spinner = mSolo.getCurrentSpinners().get(0);
		mSolo.clickOnView(spinner);
		mSolo.clickOnText("SD Card");
		mSolo.clickOnText("Export");
		
		String filename = ExportDialogFragment.buildExportFilename();
		
//		File file = new File(getActivity().getExternalFilesDir(null), filename);
		File file = new File(Environment.getExternalStorageDirectory() + "/" + filename);
		assertNotNull(file);
		assertTrue(file.exists());
	}	
	
	public void testValidityOfExport(){
		//TODO: Validate with an XML schema if possible
	}
	
	@Override
	protected void tearDown() throws Exception {
		// TODO Auto-generated method stub
		super.tearDown();
	}
}
