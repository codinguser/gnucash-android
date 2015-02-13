package org.gnucash.android.test.db;

import java.util.List;

import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.util.Log;

public class TransactionsDbAdapterTest extends AndroidTestCase {
	private static final String ALPHA_ACCOUNT_NAME  = "Alpha";
	private static final String BRAVO_ACCOUNT_NAME  = "Bravo";
	private static final String ALPHA_ACCOUNT_UID   = "alpha-team";
	private static final String BRAVO_ACCOUNT_UID   = "bravo-team";

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

        Account first = new Account(ALPHA_ACCOUNT_NAME);
		first.setUID(ALPHA_ACCOUNT_UID);
        Account second = new Account(BRAVO_ACCOUNT_NAME);
        second.setUID(BRAVO_ACCOUNT_UID);

		Transaction t1 = new Transaction("T800");
		t1.setTime(System.currentTimeMillis() - 10000);
        Split split = new Split(Money.getZeroInstance(), ALPHA_ACCOUNT_UID);
        t1.addSplit(split);
        t1.addSplit(split.createPair(BRAVO_ACCOUNT_UID));

		Transaction t2 = new Transaction( "T1000");
		t2.setTime(System.currentTimeMillis());
        Split split2 = new Split(new Money("23.50"), BRAVO_ACCOUNT_UID);
        t2.addSplit(split2);
        t2.addSplit(split2.createPair(ALPHA_ACCOUNT_UID));

		first.addTransaction(t1);
		first.addTransaction(t2);
		
		mAccountsDbAdapter.addAccount(second);
		mAccountsDbAdapter.addAccount(first);
	}
	
	public void testTransactionsAreTimeSorted(){
		List<Transaction> transactionsList = mTransactionsDbAdapter.getAllTransactionsForAccount(ALPHA_ACCOUNT_UID);
		assertEquals("T1000", transactionsList.get(0).getDescription());
		assertEquals("T800", transactionsList.get(1).getDescription());
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		mAccountsDbAdapter.deleteAllRecords();
        mDbHelper.close();
        mDb.close();
	}
}
