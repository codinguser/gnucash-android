package org.gnucash.android.test;

import org.gnucash.android.R;
import org.gnucash.android.ui.AccountsActivity;

import android.support.v4.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListView;
import android.widget.TextView;

import com.jayway.android.robotium.solo.Solo;

public class AccountsActivityTest extends ActivityInstrumentationTestCase2<AccountsActivity> {
	private Solo mSolo;
	
	public AccountsActivityTest() {		
		super(AccountsActivity.class);
	}

	protected void setUp() throws Exception {
		mSolo = new Solo(getInstrumentation(), getActivity());
	}

	public void testDisplayAccountsList(){		
		//there should exist a listview of accounts
		Fragment fragment = getActivity()
				.getSupportFragmentManager()
				.findFragmentByTag(AccountsActivity.FRAGMENT_ACCOUNTS_LIST);
		assertNotNull(fragment);
		assertNotNull(mSolo.getCurrentListViews().get(0));		
	}
	
	public void testCreateAccount(){
		mSolo.clickOnActionBarItem(R.id.menu_add_account);
		mSolo.enterText(0, "Test account");
		
		mSolo.clickOnButton(1);
		
		mSolo.waitForDialogToClose(1000);
		ListView lv = mSolo.getCurrentListViews().get(0);
		assertNotNull(lv);
		TextView v = (TextView) lv.getChildAt(lv.getCount() - 1)
				.findViewById(R.id.account_name);
		
		assertEquals(v.getText().toString(), "Test account");
	}
	
	public void testEditAccount(){
		String editedAccountName = "Edited Account";
		ListView lv = mSolo.getCurrentListViews().get(0);
		
		mSolo.clickLongOnView(lv.getChildAt(lv.getCount() - 1));
		
		mSolo.clickOnImage(1);
		
		mSolo.clearEditText(0);
		mSolo.enterText(0, editedAccountName);
		
		mSolo.clickOnButton(1);
		mSolo.waitForDialogToClose(1000);
		
		TextView tv = (TextView) lv.getChildAt(lv.getCount() - 1)
				.findViewById(R.id.account_name);		
		assertEquals(editedAccountName, tv.getText().toString());
	}
	
	public void testDisplayTransactionsList(){		
		mSolo.clickOnText("Test account");
		mSolo.waitForText("Test account");
		Fragment fragment = getActivity()
				.getSupportFragmentManager()
				.findFragmentByTag(AccountsActivity.FRAGMENT_TRANSACTIONS_LIST);
		assertNotNull(fragment);
		
		assertNotNull(mSolo.getCurrentListViews());
		assertTrue(mSolo.getCurrentListViews().size() != 0);	
		
	}
		
	protected void tearDown() throws Exception {
		mSolo.finishOpenedActivities();		
	}

}
