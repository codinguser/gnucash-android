package org.gnucash.android.test;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.gnucash.android.R;
import org.gnucash.android.ui.AccountsActivity;

import android.test.ActivityInstrumentationTestCase2;

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
		mSolo.clickOnText("Export");
		long timeMillis = System.currentTimeMillis();
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmm");
		String filename = formatter.format(new Date(timeMillis)) + "_gnucash_export.ofx";
		
		File file = new File(getActivity().getExternalFilesDir(null), filename);
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
