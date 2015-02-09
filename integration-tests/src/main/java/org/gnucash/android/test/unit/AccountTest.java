package org.gnucash.android.test.unit;

import org.gnucash.android.model.Account;

import junit.framework.TestCase;

public class AccountTest extends TestCase {

	public AccountTest(String name) {
		super(name);
	}

	public void testUIDContainsName(){
        //TODO: modify this test when migrating to GnuCash file exports
		Account account = new Account("dummy");
		assertTrue(account.getUID().contains("dummy"));
				
		Account another = new Account("tele pathy x-men");
		String uid = another.getUID();
		
		//only first ten characters are used in uid
		assertTrue(uid.contains("telepathy"));
		
		//no spaces allowed
		assertFalse(uid.contains(" "));
		assertFalse(uid.contains("tele pathy x-men"));
	}
}
