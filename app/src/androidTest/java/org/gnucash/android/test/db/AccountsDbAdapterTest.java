package org.gnucash.android.test.db;

import java.util.Currency;
import java.util.List;

import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.db.AccountsDbAdapter;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.util.Log;

public class AccountsDbAdapterTest extends AndroidTestCase {

	private static final String BRAVO_ACCOUNT_NAME = "Bravo";
	private static final String ALPHA_ACCOUNT_NAME = "Alpha";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private AccountsDbAdapter mAccountsDbAdapter;
    private TransactionsDbAdapter mTransactionsDbAdapter;
    private SplitsDbAdapter mSplitsDbAdapter;
	
	@Override
	protected void setUp() throws Exception {		
		super.setUp();
        mDbHelper = new DatabaseHelper(getContext());
        try {
            mDb = mDbHelper.getWritableDatabase();
        } catch (SQLException e) {
            Log.e(getClass().getName(), "Error getting database: " + e.getMessage());
            mDb = mDbHelper.getReadableDatabase();
        }
        mSplitsDbAdapter = new SplitsDbAdapter(mDb);
        mTransactionsDbAdapter = new TransactionsDbAdapter(mDb, mSplitsDbAdapter);
        mAccountsDbAdapter = new AccountsDbAdapter(mDb, mTransactionsDbAdapter);
		mAccountsDbAdapter.deleteAllRecords();
		Account first = new Account(ALPHA_ACCOUNT_NAME);
        first.setUID(ALPHA_ACCOUNT_NAME);
		Account second = new Account(BRAVO_ACCOUNT_NAME);
        second.setUID(BRAVO_ACCOUNT_NAME);
		
		mAccountsDbAdapter.addAccount(second);
		mAccountsDbAdapter.addAccount(first);
	}
	
	public void testAlphabeticalSorting(){
		List<Account> accountsList = mAccountsDbAdapter.getAllAccounts();
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
		
		mAccountsDbAdapter.addAccount(acc1);
		
		Account account = mAccountsDbAdapter.getAccount("simile");
		for (Transaction t : account.getTransactions()) {
			assertEquals("JPY", t.getBalance(acc1.getUID()).getCurrency().getCurrencyCode());
		}
	}

    public void testAddAccountWithTransaction(){
        Account account1 = new Account("AlphaAccount");
        Account account2 = new Account("BetaAccount");
        Transaction transaction = new Transaction("MyTransaction");
        Split split = new Split(Money.getZeroInstance(), account1.getUID());
        transaction.addSplit(split);
        transaction.addSplit(split.createPair(account2.getUID()));

        long id1 = mAccountsDbAdapter.addAccount(account1);
        long id2 = mAccountsDbAdapter.addAccount(account2);

        assertTrue(id1 > 0);
        assertTrue(id2 > 0);
    }

    /**
     * Tests the foreign key constraint "ON DELETE CASCADE" between accounts and splits
     */
    public void testDeletingAccountShouldDeleteSplits(){
        Transaction transaction = new Transaction("TestTrn");
        Split split = new Split(Money.getZeroInstance(), ALPHA_ACCOUNT_NAME);
        transaction.addSplit(split);
        transaction.addSplit(split.createPair(BRAVO_ACCOUNT_NAME));

        long id = mTransactionsDbAdapter.addTransaction(transaction);
        assertTrue(id > 0);

        mAccountsDbAdapter.deleteRecord(ALPHA_ACCOUNT_NAME);

        Transaction trxn = mTransactionsDbAdapter.getTransaction(transaction.getUID());
        assertEquals(1, trxn.getSplits().size());
        assertEquals(BRAVO_ACCOUNT_NAME, trxn.getSplits().get(0).getAccountUID());
    }

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		mAccountsDbAdapter.deleteAllRecords();
        mDbHelper.close();
        mDb.close();
	}
}
