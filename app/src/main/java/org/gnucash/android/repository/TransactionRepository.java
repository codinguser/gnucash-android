package org.gnucash.android.repository;

import android.database.sqlite.SQLiteDatabase;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.DatabaseAdapter;
import org.gnucash.android.db.adapter.SplitsDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.model.Transaction;

import java.util.List;

public class TransactionRepository {

    final private SQLiteDatabase db;

    public TransactionRepository(SQLiteDatabase db) {
        this.db = db;
    }

    /**
     * Backups of the database, saves opening balances (if necessary)
     * and deletes all non-template transactions in the database.
     */
    public void deleteTransactions(){
        final List<Transaction> openingBalances;
        final boolean preserveOpeningBalances = GnuCashApplication.shouldSaveOpeningBalances(false);

        final TransactionsDbAdapter transactionsDbAdapter = new TransactionsDbAdapter(db, new SplitsDbAdapter(db));
        if (preserveOpeningBalances) {
            openingBalances = new AccountsDbAdapter(db, transactionsDbAdapter).getAllOpeningBalanceTransactions();

            transactionsDbAdapter.deleteAllNonTemplateTransactions();

            transactionsDbAdapter.bulkAddRecords(openingBalances, DatabaseAdapter.UpdateMethod.insert);
        } else {
            transactionsDbAdapter.deleteAllNonTemplateTransactions();
        }
    }
}
