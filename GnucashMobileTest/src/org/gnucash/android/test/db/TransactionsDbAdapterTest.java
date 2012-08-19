package org.gnucash.android.test.db;

import java.util.List;

import org.gnucash.android.data.Account;
import org.gnucash.android.data.Transaction;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;

import android.test.AndroidTestCase;

public class TransactionsDbAdapterTest extends AndroidTestCase {
	private static final String ALPHA_ACCOUNT_NAME = "Alpha";
	private static final String BRAVO_ACCOUNT_NAME = "Bravo";
	private static final String ALPHA_ACCOUNT_UID = "alpha-team";
	
	private TransactionsDbAdapter mAdapter;	
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mAdapter = new TransactionsDbAdapter(getContext());
		AccountsDbAdapter accountsAdapter = new AccountsDbAdapter(mContext);
		Account first = new Account(ALPHA_ACCOUNT_NAME);
		first.setUID(ALPHA_ACCOUNT_UID);
		Transaction t1 = new Transaction("2.99", "T800");
		t1.setTime(System.currentTimeMillis() - 10000);
		Transaction t2 = new Transaction("4.99", "T1000");
		t2.setTime(System.currentTimeMillis());
		first.addTransaction(t1);
		first.addTransaction(t2);
		
		Account second = new Account(BRAVO_ACCOUNT_NAME);
		Transaction t = new Transaction("9.99", "buyout");
		second.addTransaction(t);
		
		accountsAdapter.addAccount(second);
		accountsAdapter.addAccount(first);
	}
	
	public void testTransactionsAreTimeSorted(){
		List<Transaction> transactionsList = mAdapter.getAllTransactionsForAccount(ALPHA_ACCOUNT_UID);
		assertEquals("T1000", transactionsList.get(0).getName());
		assertEquals("T800", transactionsList.get(1).getName());
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		AccountsDbAdapter accAdapter = new AccountsDbAdapter(mContext);
		accAdapter.deleteAllAccounts();
		accAdapter.close();
	}
}
