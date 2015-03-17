/*
 * Copyright (c) 2012 - 2014 Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2014 Yongxin Wang <fefe.wyx@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gnucash.android.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;
import android.util.Log;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.model.*;

import static org.gnucash.android.db.DatabaseSchema.*;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

/**
 * Manages persistence of {@link Transaction}s in the database
 * Handles adding, modifying and deleting of transaction records.
 * @author Ngewi Fet <ngewif@gmail.com> 
 * @author Yongxin Wang <fefe.wyx@gmail.com>
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class TransactionsDbAdapter extends DatabaseAdapter {

    private final SplitsDbAdapter mSplitsDbAdapter;

    /**
     * Overloaded constructor. Creates adapter for already open db
     * @param db SQlite db instance
     */
    public TransactionsDbAdapter(SQLiteDatabase db, SplitsDbAdapter splitsDbAdapter) {
        super(db, TransactionEntry.TABLE_NAME);
        mSplitsDbAdapter = splitsDbAdapter;
    }

    /**
     * Returns an application-wide instance of the database adapter
     * @return Transaction database adapter
     */
    public static TransactionsDbAdapter getInstance(){
        return GnuCashApplication.getTransactionDbAdapter();
    }

    public SplitsDbAdapter getSplitDbAdapter() {
        return mSplitsDbAdapter;
    }

    /**
	 * Adds an transaction to the database. 
	 * If a transaction already exists in the database with the same unique ID, 
	 * then the record will just be updated instead
	 * @param transaction {@link Transaction} to be inserted to database
	 * @return Database row ID of the inserted transaction
	 */
	public long addTransaction(Transaction transaction){
		ContentValues contentValues = getContentValues(transaction);
		contentValues.put(TransactionEntry.COLUMN_DESCRIPTION, transaction.getDescription());
		contentValues.put(TransactionEntry.COLUMN_TIMESTAMP,    transaction.getTimeMillis());
		contentValues.put(TransactionEntry.COLUMN_NOTES,        transaction.getNote());
		contentValues.put(TransactionEntry.COLUMN_EXPORTED,     transaction.isExported() ? 1 : 0);
		contentValues.put(TransactionEntry.COLUMN_TEMPLATE,     transaction.isTemplate() ? 1 : 0);
        contentValues.put(TransactionEntry.COLUMN_CURRENCY,     transaction.getCurrencyCode());

        Log.d(TAG, "Replacing transaction in db");
        long rowId = -1;
        mDb.beginTransaction();
        try {
            rowId = mDb.replaceOrThrow(TransactionEntry.TABLE_NAME, null, contentValues);

            Log.d(TAG, "Adding splits for transaction");
            ArrayList<String> splitUIDs = new ArrayList<String>(transaction.getSplits().size());
            for (Split split : transaction.getSplits()) {
                contentValues = getContentValues(split);
                contentValues.put(SplitEntry.COLUMN_AMOUNT,     split.getAmount().absolute().toPlainString());
                contentValues.put(SplitEntry.COLUMN_TYPE,       split.getType().name());
                contentValues.put(SplitEntry.COLUMN_MEMO,       split.getMemo());
                contentValues.put(SplitEntry.COLUMN_ACCOUNT_UID, split.getAccountUID());
                contentValues.put(SplitEntry.COLUMN_TRANSACTION_UID, split.getTransactionUID());
                splitUIDs.add(split.getUID());

                Log.d(TAG, "Replace transaction split in db");
                mDb.replaceOrThrow(SplitEntry.TABLE_NAME, null, contentValues);
            }
            Log.d(TAG, transaction.getSplits().size() + " splits added");

            long deleted = mDb.delete(SplitEntry.TABLE_NAME,
                    SplitEntry.COLUMN_TRANSACTION_UID + " = ? AND "
                            + SplitEntry.COLUMN_UID + " NOT IN ('" + TextUtils.join("' , '", splitUIDs) + "')",
                    new String[]{transaction.getUID()});
            Log.d(TAG, deleted + " splits deleted");
            mDb.setTransactionSuccessful();
        } catch (SQLException sqle) {
            Log.e(TAG, sqle.getMessage());
            sqle.printStackTrace();
        } finally {
            mDb.endTransaction();
        }
        return rowId;
	}

    /**
     * Adds an several transactions to the database.
     * If a transaction already exists in the database with the same unique ID,
     * then the record will just be updated instead. Recurrence Transactions will not
     * be inserted, instead schedule Transaction would be called. If an exception
     * occurs, no transaction would be inserted.
     * @param transactionList {@link Transaction} transactions to be inserted to database
     * @return Number of transactions inserted
     */
    public long bulkAddTransactions(List<Transaction> transactionList){
        List<Split> splitList = new ArrayList<Split>(transactionList.size()*3);
        long rowInserted = 0;
        try {
            mDb.beginTransaction();
            SQLiteStatement replaceStatement = mDb.compileStatement("REPLACE INTO " + TransactionEntry.TABLE_NAME + " ( "
                + TransactionEntry.COLUMN_UID 		    + " , "
                + TransactionEntry.COLUMN_DESCRIPTION   + " , "
                + TransactionEntry.COLUMN_NOTES         + " , "
                + TransactionEntry.COLUMN_TIMESTAMP     + " , "
                + TransactionEntry.COLUMN_EXPORTED      + " , "
                + TransactionEntry.COLUMN_CURRENCY      + " , "
                + TransactionEntry.COLUMN_CREATED_AT    + " , "
                + TransactionEntry.COLUMN_TEMPLATE + " ) VALUES ( ? , ? , ? , ?, ? , ? , ? , ?)");
            for (Transaction transaction : transactionList) {
                if (transaction.isTemplate()) { //TODO: Properly schedule transactions
                    scheduleTransaction(transaction);
                }
                //Log.d(TAG, "Replacing transaction in db");
                replaceStatement.clearBindings();
                replaceStatement.bindString(1,  transaction.getUID());
                replaceStatement.bindString(2,  transaction.getDescription());
                replaceStatement.bindString(3,  transaction.getNote());
                replaceStatement.bindLong(4,    transaction.getTimeMillis());
                replaceStatement.bindLong(5,    transaction.isExported() ? 1 : 0);
                replaceStatement.bindString(6,  transaction.getCurrencyCode());
                replaceStatement.bindString(7,  transaction.getCreatedTimestamp().toString());
                replaceStatement.bindLong(8,    transaction.isTemplate() ? 1 : 0);
                replaceStatement.execute();
                rowInserted ++;
                splitList.addAll(transaction.getSplits());
            }
            mDb.setTransactionSuccessful();
        }
        finally {
            mDb.endTransaction();
        }
        if (rowInserted != 0 && !splitList.isEmpty()) {
            try {
                long nSplits = mSplitsDbAdapter.bulkAddSplits(splitList);
                Log.d(TAG, String.format("%d splits inserted", nSplits));
            }
            finally {
                SQLiteStatement deleteEmptyTransaction = mDb.compileStatement("DELETE FROM " +
                        TransactionEntry.TABLE_NAME + " WHERE NOT EXISTS ( SELECT * FROM " +
                        SplitEntry.TABLE_NAME +
                        " WHERE " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID +
                        " = " + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_UID + " ) ");
                deleteEmptyTransaction.execute();
            }
        }
        return rowInserted;
    }

	/**
	 * Retrieves a transaction object from a database with database ID <code>rowId</code>
	 * @param rowId Identifier of the transaction record to be retrieved
	 * @return {@link Transaction} object corresponding to database record
	 */
    public Transaction getTransaction(long rowId) {
        Log.v(TAG, "Fetching transaction with id " + rowId);
        Cursor c = fetchRecord(rowId);
        try {
            if (c.moveToFirst()) {
                return buildTransactionInstance(c);
            } else {
                throw new IllegalArgumentException("row " + rowId + " does not exist");
            }
        } finally {
            c.close();
        }
    }
	
	/**
	 * Returns a cursor to a set of all transactions which have a split belonging to the accound with unique ID
	 * <code>accountUID</code>.
	 * @param accountUID UID of the account whose transactions are to be retrieved
	 * @return Cursor holding set of transactions for particular account
     * @throws java.lang.IllegalArgumentException if the accountUID is null
	 */
	public Cursor fetchAllTransactionsForAccount(String accountUID){
        //TODO: Remove this legacy code. Database has been upgraded
        if (mDb.getVersion() < DatabaseSchema.SPLITS_DB_VERSION){ //legacy from previous database format
            return mDb.query(TransactionEntry.TABLE_NAME, null,
                    "((" + SplitEntry.COLUMN_ACCOUNT_UID + " = '" + accountUID + "') "
                            + "OR (" + DatabaseHelper.KEY_DOUBLE_ENTRY_ACCOUNT_UID + " = '" + accountUID + "' ))"
                            + " AND " + TransactionEntry.COLUMN_TEMPLATE + " = 0",
                    null, null, null, TransactionEntry.COLUMN_TIMESTAMP + " DESC");
        } else {
            SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
            queryBuilder.setTables(TransactionEntry.TABLE_NAME
                    + " INNER JOIN " + SplitEntry.TABLE_NAME + " ON "
                    + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID + " = "
                    + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_UID);
            queryBuilder.setDistinct(true);
            String[] projectionIn = new String[]{TransactionEntry.TABLE_NAME + ".*"};
            String selection = SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_ACCOUNT_UID + " = ?"
                    + " AND " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TEMPLATE + " = 0";
            String[] selectionArgs = new String[]{accountUID};
            String sortOrder = TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TIMESTAMP + " DESC";

            return queryBuilder.query(mDb, projectionIn, selection, selectionArgs, null, null, sortOrder);
        }
	}

    /**
     * Fetches all recurring transactions from the database.
     * <p>Recurring transactions are the transaction templates which have an entry in the scheduled events table</p>
     * @return Cursor holding set of all recurring transactions
     */
    public Cursor fetchAllScheduledTransactions(){
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TransactionEntry.TABLE_NAME + " INNER JOIN " + ScheduledActionEntry.TABLE_NAME + " ON "
                + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID + " = "
                + ScheduledActionEntry.TABLE_NAME + "." + ScheduledActionEntry.COLUMN_EVENT_UID);

        String[] projectionIn = new String[]{TransactionEntry.TABLE_NAME + ".*"};
        String sortOrder = TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_DESCRIPTION + " ASC";
        queryBuilder.setDistinct(true);

        return queryBuilder.query(mDb, projectionIn, null, null, null, null, sortOrder);
    }

	/**
	 * Returns a cursor to a set of all transactions for the account with ID <code>accountID</code>
	 * or for which this account is the origin account in a double entry
	 * @param accountID ID of the account whose transactions are to be retrieved
	 * @return Cursor holding set of transactions for particular account
	 */
	public Cursor fetchAllTransactionsForAccount(long accountID){
        String accountUID = AccountsDbAdapter.getInstance().getUID(accountID);
		return fetchAllTransactionsForAccount(accountUID);
	}
	
	/**
	 * Returns list of all transactions for account with UID <code>accountUID</code>
	 * @param accountUID UID of account whose transactions are to be retrieved
	 * @return List of {@link Transaction}s for account with UID <code>accountUID</code>
	 */
    public List<Transaction> getAllTransactionsForAccount(String accountUID){
		Cursor c = fetchAllTransactionsForAccount(accountUID);
		ArrayList<Transaction> transactionsList = new ArrayList<Transaction>();
        try {
            while (c.moveToNext()) {
                transactionsList.add(buildTransactionInstance(c));
            }
        } finally {
            c.close();
        }
		return transactionsList;
	}

    /**
     * Returns all transaction instances in the database.
     * @return List of all transactions
     */
    public List<Transaction> getAllTransactions(){
        Cursor cursor = fetchAllRecords();
        List<Transaction> transactions = new ArrayList<Transaction>();
        try {
            while (cursor.moveToNext()) {
                transactions.add(buildTransactionInstance(cursor));
            }
        } finally {
            cursor.close();
        }
        return transactions;
    }

    public Cursor fetchTransactionsWithSplits(String [] columns, String where, String[] whereArgs, String orderBy) {
        return mDb.query(TransactionEntry.TABLE_NAME + " , " + SplitEntry.TABLE_NAME +
                        " ON " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID +
                        " = " + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_UID,
                columns, where, whereArgs, null, null,
                orderBy);
    }

    public Cursor fetchTransactionsWithSplitsWithTransactionAccount(String [] columns, String where, String[] whereArgs, String orderBy) {
        // table is :
        // trans_split_acct , trans_extra_info ON trans_extra_info.trans_acct_t_uid = transactions_uid ,
        // accounts AS account1 ON account1.uid = trans_extra_info.trans_acct_a_uid
        //
        // views effectively simplified this query
        //
        // account1 provides information for the grouped account. Splits from the grouped account
        // can be eliminated with a WHERE clause. Transactions in QIF can be auto balanced.
        //
        // Account, transaction and split Information can be retrieve in a single query.
        return mDb.query(
                "trans_split_acct , trans_extra_info ON trans_extra_info.trans_acct_t_uid = trans_split_acct." +
                TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_UID + " , " +
                AccountEntry.TABLE_NAME + " AS account1 ON account1." + AccountEntry.COLUMN_UID +
                " = trans_extra_info.trans_acct_a_uid",
                columns, where, whereArgs, null, null , orderBy);
    }

    /**
     * Return number of transactions in the database which are non recurring
     * @return Number of transactions
     */
    public int getTotalTransactionsCount() {
        String queryCount = "SELECT COUNT(*) FROM " + TransactionEntry.TABLE_NAME +
                " WHERE " + TransactionEntry.COLUMN_TEMPLATE + " =0";
        Cursor cursor = mDb.rawQuery(queryCount, null);
        try {
            cursor.moveToFirst();
            return cursor.getInt(0);
        } finally {
            cursor.close();
        }
    }

	/**
	 * Builds a transaction instance with the provided cursor.
	 * The cursor should already be pointing to the transaction record in the database
	 * @param c Cursor pointing to transaction record in database
	 * @return {@link Transaction} object constructed from database record
	 */
    public Transaction buildTransactionInstance(Cursor c){
		String name   = c.getString(c.getColumnIndexOrThrow(TransactionEntry.COLUMN_DESCRIPTION));
		Transaction transaction = new Transaction(name);
        populateModel(c, transaction);

		transaction.setTime(c.getLong(c.getColumnIndexOrThrow(TransactionEntry.COLUMN_TIMESTAMP)));
		transaction.setNote(c.getString(c.getColumnIndexOrThrow(TransactionEntry.COLUMN_NOTES)));
		transaction.setExported(c.getInt(c.getColumnIndexOrThrow(TransactionEntry.COLUMN_EXPORTED)) == 1);
		transaction.setTemplate(c.getInt(c.getColumnIndexOrThrow(TransactionEntry.COLUMN_TEMPLATE)) == 1);

        if (mDb.getVersion() < SPLITS_DB_VERSION){ //legacy, will be used once, when migrating the database
            String accountUID = c.getString(c.getColumnIndexOrThrow(SplitEntry.COLUMN_ACCOUNT_UID));
            String amountString = c.getString(c.getColumnIndexOrThrow(SplitEntry.COLUMN_AMOUNT));
            String currencyCode = getAccountCurrencyCode(accountUID);
            Money amount = new Money(amountString, currencyCode);

            Split split = new Split(amount.absolute(), accountUID);
            TransactionType type = Transaction.getTypeForBalance(getAccountType(accountUID), amount.isNegative());
            split.setType(type);
            transaction.addSplit(split);

            String transferAccountUID = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.KEY_DOUBLE_ENTRY_ACCOUNT_UID));
            if (transferAccountUID == null) {
                AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();
                transferAccountUID = accountsDbAdapter.getOrCreateImbalanceAccountUID(Currency.getInstance(currencyCode));
            }
            transaction.addSplit(split.createPair(transferAccountUID));
        } else {
            transaction.setCurrencyCode(c.getString(c.getColumnIndexOrThrow(TransactionEntry.COLUMN_CURRENCY)));
            long transactionID = c.getLong(c.getColumnIndexOrThrow(TransactionEntry._ID));
            transaction.setSplits(mSplitsDbAdapter.getSplitsForTransaction(transactionID));
        }
		return transaction;
	}

	/**
	 * Returns the currency code (ISO 4217) used by the account with id <code>accountId</code>
	 * If you do not have the database record Id, you can call {@link #getID(String)}  instead.
	 * @param accountId Database record id of the account 
	 * @return Currency code of the account with Id <code>accountId</code>
	 * @see #getAccountCurrencyCode(String)
	 */
	public String getAccountCurrencyCode(long accountId){
		String accountUID = AccountsDbAdapter.getInstance().getUID(accountId);
		return getAccountCurrencyCode(accountUID);
	}

    /**
     * Returns the transaction balance for the transaction for the specified account.
     * <p>We consider only those splits which belong to this account</p>
     * @param transactionUID GUID of the transaction
     * @param accountUID GUID of the account
     * @return {@link org.gnucash.android.model.Money} balance of the transaction for that account
     */
    public Money getBalance(String transactionUID, String accountUID){
        List<Split> splitList = mSplitsDbAdapter.getSplitsForTransactionInAccount(
                transactionUID, accountUID);

        return Transaction.computeBalance(accountUID, splitList);
    }

    //TODO: When "on delete cascade" is successfully migrated, we can remove this method and use the base implementation
	/**
	 * Deletes transaction record with id <code>rowId</code> and all it's splits
	 * @param rowId Long database record id
	 * @return <code>true</code> if deletion was successful, <code>false</code> otherwise
	 */
    @Override
	public boolean deleteRecord(long rowId){
		Log.d(TAG, "Delete transaction with record Id: " + rowId);
        //the splits db adapter handles deletion of the transaction
		mSplitsDbAdapter.deleteSplitsForTransaction(rowId);
        return mDb.delete(TransactionEntry.TABLE_NAME, TransactionEntry._ID + "=" + rowId, null) > 0;
	}

    /**
	 * Assigns transaction with id <code>rowId</code> to account with id <code>accountId</code>
	 * @param transactionUID GUID of the transaction
     * @param srcAccountUID GUID of the account from which the transaction is to be moved
	 * @param dstAccountUID GUID of the account to which the transaction will be assigned
	 * @return Number of transactions splits affected
	 */
	public int moveTransaction(String transactionUID, String srcAccountUID, String dstAccountUID){
		Log.i(TAG, "Moving transaction ID " + transactionUID
                + " splits from " + srcAccountUID + " to account " + dstAccountUID);

		List<Split> splits = mSplitsDbAdapter.getSplitsForTransactionInAccount(transactionUID, srcAccountUID);
        for (Split split : splits) {
            split.setAccountUID(dstAccountUID);
        }
        mSplitsDbAdapter.bulkAddSplits(splits);
        return splits.size();
	}
	
	/**
	 * Returns the number of transactions belonging to account with id <code>accountId</code>
	 * @param accountId Long ID of account
	 * @return Number of transactions assigned to account with id <code>accountId</code>
	 */
	public int getTransactionsCount(long accountId){
		Cursor cursor = fetchAllTransactionsForAccount(accountId);
        try {
            return cursor.getCount();
        } finally {
            cursor.close();
		}
	}

    /**
     * Returns the number of transactions belonging to an account
     * @param accountUID GUID of the account
     * @return Number of transactions with splits in the account
     */
    public int getTransactionsCount(String accountUID){
        Cursor cursor = fetchAllTransactionsForAccount(accountUID);
        int count = 0;
        if (cursor == null)
            return count;
        else {
            count = cursor.getCount();
            cursor.close();
        }
        return count;
    }

    /**
	 * Returns the total number of transactions in the database
	 * regardless of what account they belong to
	 * @return Number of transaction in the database
	 */
	public long getAllTransactionsCount() {
        String sql = "SELECT COUNT(*) FROM " + TransactionEntry.TABLE_NAME;
        SQLiteStatement statement = mDb.compileStatement(sql);
        return statement.simpleQueryForLong();
    }

    /**
     * Returns a cursor to transactions whose name (UI: description) start with the <code>prefix</code>
     * <p>This method is used for autocomplete suggestions when creating new transactions</p>
     * @param prefix Starting characters of the transaction name
     * @return Cursor to the data set containing all matching transactions
     */
    public Cursor fetchTemplatesStartingWith(String prefix){
        return mDb.query(TransactionEntry.TABLE_NAME,
                new String[]{TransactionEntry._ID, TransactionEntry.COLUMN_DESCRIPTION},
                TransactionEntry.COLUMN_TEMPLATE + "=1 AND "
                        + TransactionEntry.COLUMN_DESCRIPTION + " LIKE '" + prefix + "%'",
                null, null, null,
                TransactionEntry.COLUMN_DESCRIPTION + " ASC");
    }

    /**
     * Updates a specific entry of an transaction
     * @param contentValues Values with which to update the record
     * @param whereClause Conditions for updating formatted as SQL where statement
     * @param whereArgs Arguments for the SQL wehere statement
     * @return Number of records affected
     */
    public int updateTransaction(ContentValues contentValues, String whereClause, String[] whereArgs){
        return mDb.update(TransactionEntry.TABLE_NAME, contentValues, whereClause, whereArgs);
    }

    /**
     * Schedules <code>recurringTransaction</code> to be executed at specific intervals.
     * The interval period is packaged within the transaction
     * @param recurringTransaction Transaction which is to be recurring
     */
    public void scheduleTransaction(Transaction recurringTransaction) {
        long recurrencePeriodMillis = System.currentTimeMillis(); //recurringTransaction.getRecurrencePeriod();
        long firstRunMillis = System.currentTimeMillis() + recurrencePeriodMillis;
        long recurringTransactionId = addTransaction(recurringTransaction);


        //TODO: Properly create ScheduledEvent
    }

    /**
     * Returns a transaction for the given transaction GUID
     * @param transactionUID GUID of the transaction
     * @return Retrieves a transaction from the database
     */
    public Transaction getTransaction(String transactionUID) {
        return getTransaction(getID(transactionUID));
    }

    public int getNumCurrencies(String transactionUID) {
        Cursor cursor = mDb.query("trans_extra_info",
                new String[]{"trans_currency_count"},
                "trans_acct_t_uid=?",
                new String[]{transactionUID},
                null, null, null);
        int numCurrencies = 0;
        try {
            if (cursor.moveToFirst()) {
                numCurrencies = cursor.getInt(0);
            }
        }
        finally {
            cursor.close();
        }
        return numCurrencies;
    }

    /**
     * Returns a timestamp of the earliest transaction for the specified account type
     * @param type the account type
     * @return the earliest transaction's timestamp. Returns 1970-01-01 00:00:00.000 if no transaction found
     */
    public long getTimestampOfEarliestTransaction(AccountType type) {
        return getTimestamp("MIN", type);
    }

    /**
     * Returns a timestamp of the latest transaction for the specified account type
     * @param type the account type
     * @return the latest transaction's timestamp. Returns 1970-01-01 00:00:00.000 if no transaction found
     */
    public long getTimestampOfLatestTransaction(AccountType type) {
        return getTimestamp("MAX", type);
    }

    private long getTimestamp(String mod, AccountType type) {
        String sql = "SELECT " + mod + "(" + TransactionEntry.COLUMN_TIMESTAMP + ")" +
                " FROM " + TransactionEntry.TABLE_NAME +
                " INNER JOIN " + SplitEntry.TABLE_NAME + " ON "
                + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_UID + " = "
                + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID +
                " INNER JOIN " + AccountEntry.TABLE_NAME + " ON "
                + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_UID + " = "
                + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_ACCOUNT_UID +
                " WHERE " + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_TYPE + " = ?";
        Cursor cursor = mDb.rawQuery(sql, new String[]{type.toString()});
        long timestamp= 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                timestamp = cursor.getLong(0);
            }
            cursor.close();
        }
        return timestamp;
    }

}
