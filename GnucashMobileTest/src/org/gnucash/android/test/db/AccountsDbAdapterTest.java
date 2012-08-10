package org.gnucash.android.test.db;

import java.util.Currency;
import java.util.List;

import org.gnucash.android.data.Account;
import org.gnucash.android.data.Transaction;
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
		Account first = new Account(ALPHA_ACCOUNT_NAME);
		Transaction t1 = new Transaction(2.99, "T800");
		t1.setAccountUID(first.getUID());
		Transaction t2 = new Transaction(4.99, "T1000");
		t2.setAccountUID(first.getUID());
		
		Account second = new Account(BRAVO_ACCOUNT_NAME);
		Transaction t = new Transaction(9.99, "buyout");
		t.setAccountUID(second.getUID());
		
		mAdapter.addAccount(second);
		mAdapter.addAccount(first);
	}
	
	public void testAlphabeticalSorting(){
		List<Account> accountsList = mAdapter.getAllAccounts();
		//bravo was saved first, but alpha should be first alphabetically
		assertEquals(ALPHA_ACCOUNT_NAME, accountsList.get(0).getName());
		assertEquals(BRAVO_ACCOUNT_NAME, accountsList.get(1).getName());
	}
	
	public void testTransactionsHaveSameCurrencyAsAccount(){
		Account acc1 = new Account("Japanese", Currency.getInstance("JPY"));
		acc1.setUID("simile");
		Transaction trx = new Transaction(2.50, "Underground");
		Transaction term = new Transaction("3.49", "Tube");
		acc1.addTransaction(trx);
		acc1.addTransaction(term);
		
		mAdapter.addAccount(acc1);
		
		Account account = mAdapter.getAccount("simile");
		for (Transaction t : account.getTransactions()) {
			assertEquals("JPY", t.getAmount().getCurrency().getCurrencyCode());
		}
	}
}
