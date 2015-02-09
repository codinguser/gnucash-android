package org.gnucash.android.test.db;

import java.util.Currency;
import java.util.List;

import org.gnucash.android.model.Account;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.db.AccountsDbAdapter;

import android.test.AndroidTestCase;

public class AccountsDbAdapterTest extends AndroidTestCase {

	private static final String BRAVO_ACCOUNT_NAME = "Bravo";
	private static final String ALPHA_ACCOUNT_NAME = "Alpha";
	private AccountsDbAdapter mAdapter;
	
	@Override
	protected void setUp() throws Exception {		
		super.setUp();
		mAdapter = new AccountsDbAdapter(getContext());
		mAdapter.deleteAllRecords();
		Account first = new Account(ALPHA_ACCOUNT_NAME);
		Transaction t1 = new Transaction("T800");
		Transaction t2 = new Transaction("T1000");
		
		Account second = new Account(BRAVO_ACCOUNT_NAME);
		Transaction t = new Transaction("buyout");
		
		mAdapter.addAccount(second);
		mAdapter.addAccount(first);
	}
	
	public void testAlphabeticalSorting(){
		List<Account> accountsList = mAdapter.getAllAccounts();
		assertEquals(2, accountsList.size());
		//bravo was saved first, but alpha should be first alphabetically
		assertEquals(ALPHA_ACCOUNT_NAME, accountsList.get(0).getName());
		assertEquals(BRAVO_ACCOUNT_NAME, accountsList.get(1).getName());
	}
	
	public void testTransactionsHaveSameCurrencyAsAccount(){
		Account acc1 = new Account("Japanese", Currency.getInstance("JPY"));
		acc1.setUID("simile");
		Transaction trx = new Transaction("Underground");
		Transaction term = new Transaction( "Tube");
		acc1.addTransaction(trx);
		acc1.addTransaction(term);
		
		mAdapter.addAccount(acc1);
		
		Account account = mAdapter.getAccount("simile");
		for (Transaction t : account.getTransactions()) {
			assertEquals("JPY", t.getBalance(acc1.getUID()).getCurrency().getCurrencyCode());
		}
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		mAdapter.deleteAllRecords();
		mAdapter.close();
	}
}
