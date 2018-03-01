/*
 * Copyright (c) 2012 - 2015 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.db.adapter;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.util.TimestampHelper;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.gnucash.android.db.DatabaseSchema.AccountEntry;
import static org.gnucash.android.db.DatabaseSchema.ScheduledExportEntry;
import static org.gnucash.android.db.DatabaseSchema.SplitEntry;
import static org.gnucash.android.db.DatabaseSchema.TransactionEntry;

/**
 * Manages persistence of {@link Transaction}s in the database
 * Handles adding, modifying and deleting of transaction records.
 * @author Ngewi Fet <ngewif@gmail.com> 
 * @author Yongxin Wang <fefe.wyx@gmail.com>
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class TransactionsDbAdapter extends DatabaseAdapter<Transaction> {

    private final SplitsDbAdapter mSplitsDbAdapter;

    private final CommoditiesDbAdapter mCommoditiesDbAdapter;

    /**
     * Overloaded constructor. Creates adapter for already open db
     * @param db SQlite db instance
     */
    public TransactionsDbAdapter(SQLiteDatabase db, SplitsDbAdapter splitsDbAdapter) {
        super(db, TransactionEntry.TABLE_NAME, new String[]{
                TransactionEntry.COLUMN_DESCRIPTION,
                TransactionEntry.COLUMN_NOTES,
                TransactionEntry.COLUMN_POST_DATE,
                DatabaseSchema.SlotEntry.Transaction.COLUMN_EXPORTED,
                TransactionEntry.COLUMN_CURRENCY,
                TransactionEntry.COLUMN_COMMODITY_GUID,
                TransactionEntry.COLUMN_CREATED_AT,
                DatabaseSchema.SlotEntry.Transaction.COLUMN_SCHEDX_ACTION_UID,
                DatabaseSchema.TransactionView.COLUMN_TEMPLATE
        });
        mSplitsDbAdapter = splitsDbAdapter;
        mCommoditiesDbAdapter = new CommoditiesDbAdapter(db);
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
	 */
    @Override
	public void addRecord(@NonNull Transaction transaction, UpdateMethod updateMethod){
        Log.d(LOG_TAG, "Adding transaction to the db via " + updateMethod.name());
        mDb.beginTransaction();
        try {
            Split imbalanceSplit = transaction.createAutoBalanceSplit();
            if (imbalanceSplit != null){
                String imbalanceAccountUID = new AccountsDbAdapter(mDb, this)
                        .getOrCreateImbalanceAccountUID(transaction.getCommodity());
                imbalanceSplit.setAccountUID(imbalanceAccountUID);
            }
            super.addRecord(transaction, updateMethod);

            Log.d(LOG_TAG, "Adding splits for transaction");
            ArrayList<String> splitUIDs = new ArrayList<>(transaction.getSplits().size());
            for (Split split : transaction.getSplits()) {
                Log.d(LOG_TAG, "Replace transaction split in db");
                if (imbalanceSplit == split) {
                    mSplitsDbAdapter.addRecord(split, UpdateMethod.insert);
                } else {
                    mSplitsDbAdapter.addRecord(split, updateMethod);
                }
                splitUIDs.add(split.getUID());
            }
            Log.d(LOG_TAG, transaction.getSplits().size() + " splits added");

            long deleted = mDb.delete(SplitEntry.TABLE_NAME,
                    SplitEntry.COLUMN_TRANSACTION_GUID + " = ? AND "
                            + SplitEntry.COLUMN_GUID + " NOT IN ('" + TextUtils.join("' , '", splitUIDs) + "')",
                    new String[]{transaction.getUID()});
            Log.d(LOG_TAG, deleted + " splits deleted");

            mDb.setTransactionSuccessful();
        } catch (SQLException sqlEx) {
            Log.e(LOG_TAG, sqlEx.getMessage());
            Crashlytics.logException(sqlEx);
        } finally {
            mDb.endTransaction();
        }
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
    @Override
    public long bulkAddRecords(@NonNull List<Transaction> transactionList, UpdateMethod updateMethod){
        long start = System.nanoTime();
        long rowInserted = super.bulkAddRecords(transactionList, updateMethod);
        long end = System.nanoTime();
        Log.d(getClass().getSimpleName(), String.format("bulk add transaction time %d ", end - start));
        List<Split> splitList = new ArrayList<>(transactionList.size()*3);
        for (Transaction transaction : transactionList) {
            splitList.addAll(transaction.getSplits());
        }
        if (rowInserted != 0 && !splitList.isEmpty()) {
            try {
                start = System.nanoTime();
                long nSplits = mSplitsDbAdapter.bulkAddRecords(splitList, updateMethod);
                Log.d(LOG_TAG, String.format("%d splits inserted in %d ns", nSplits, System.nanoTime()-start));
            }
            finally {
                SQLiteStatement deleteEmptyTransaction = mDb.compileStatement("DELETE FROM " +
                        TransactionEntry.TABLE_NAME + " WHERE NOT EXISTS ( SELECT * FROM " +
                        SplitEntry.TABLE_NAME +
                        " WHERE " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_GUID +
                        " = " + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_GUID + " ) ");
                deleteEmptyTransaction.execute();
            }
        }
        return rowInserted;
    }

    @Override
    protected @NonNull SQLiteStatement setBindings(@NonNull SQLiteStatement stmt, @NonNull Transaction transaction) {
        stmt.clearBindings();
        stmt.bindString(1, transaction.getDescription());
        stmt.bindString(2, transaction.getNote());
        stmt.bindLong(3, transaction.getTimeMillis());
        stmt.bindLong(4, transaction.isExported() ? 1 : 0);
        stmt.bindString(5, transaction.getCurrencyCode());
        stmt.bindString(6, transaction.getCommodity().getUID());
        stmt.bindString(7, TimestampHelper.getUtcStringFromTimestamp(transaction.getCreatedTimestamp()));

        if (transaction.getScheduledActionUID() == null)
            stmt.bindNull(8);
        else
            stmt.bindString(8, transaction.getScheduledActionUID());
        stmt.bindLong(9, transaction.isTemplate() ? 1 : 0);
        stmt.bindString(10, transaction.getUID());

        return stmt;
    }

    /**
	 * Returns a cursor to a set of all transactions which have a split belonging to the accound with unique ID
	 * <code>accountUID</code>.
	 * @param accountUID UID of the account whose transactions are to be retrieved
	 * @return Cursor holding set of transactions for particular account
     * @throws java.lang.IllegalArgumentException if the accountUID is null
	 */
	public Cursor fetchAllTransactionsForAccount(String accountUID){
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TransactionEntry.TABLE_NAME
                + " INNER JOIN " + SplitEntry.TABLE_NAME + " ON "
                + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_GUID + " = "
                + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_GUID);
        queryBuilder.setDistinct(true);
        String[] projectionIn = new String[]{TransactionEntry.TABLE_NAME + ".*"};
        String selection = SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_ACCOUNT_GUID + " = ?"
                + " AND " + TransactionEntry.TABLE_NAME + "." + DatabaseSchema.TransactionView.COLUMN_TEMPLATE + " = 0";
        String[] selectionArgs = new String[]{accountUID};
        String sortOrder = TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_POST_DATE + " DESC";

        return queryBuilder.query(mDb, projectionIn, selection, selectionArgs, null, null, sortOrder);
    }

    /**
     * Returns a cursor to all scheduled transactions which have at least one split in the account
     * <p>This is basically a set of all template transactions for this account</p>
     * @param accountUID GUID of account
     * @return Cursor with set of transactions
     */
    public Cursor fetchScheduledTransactionsForAccount(String accountUID){
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TransactionEntry.TABLE_NAME
                + " INNER JOIN " + SplitEntry.TABLE_NAME + " ON "
                + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_GUID + " = "
                + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_GUID);
        queryBuilder.setDistinct(true);
        String[] projectionIn = new String[]{TransactionEntry.TABLE_NAME + ".*"};
        String selection = SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_ACCOUNT_GUID + " = ?"
                + " AND " + TransactionEntry.TABLE_NAME + "." + DatabaseSchema.TransactionView.COLUMN_TEMPLATE + " = 1";
        String[] selectionArgs = new String[]{accountUID};
        String sortOrder = TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_POST_DATE + " DESC";

        return queryBuilder.query(mDb, projectionIn, selection, selectionArgs, null, null, sortOrder);
    }

    /**
     * Deletes all transactions which contain a split in the account.
     * <p><b>Note:</b>As long as the transaction has one split which belongs to the account {@code accountUID},
     * it will be deleted. The other splits belonging to the transaction will also go away</p>
     * @param accountUID GUID of the account
     */
    public void deleteTransactionsForAccount(String accountUID){
        String rawDeleteQuery = "DELETE FROM " + TransactionEntry.TABLE_NAME + " WHERE " + TransactionEntry.COLUMN_GUID + " IN "
                + " (SELECT " + SplitEntry.COLUMN_TRANSACTION_GUID + " FROM " + SplitEntry.TABLE_NAME + " WHERE "
                + SplitEntry.COLUMN_ACCOUNT_GUID + " = ?)";
        mDb.execSQL(rawDeleteQuery, new String[]{accountUID});
    }

    /**
     * Deletes all transactions which have no splits associated with them
     * @return Number of records deleted
     */
    public int deleteTransactionsWithNoSplits(){
        return mDb.delete(
                TransactionEntry.TABLE_NAME,
                "NOT EXISTS ( SELECT * FROM " + SplitEntry.TABLE_NAME +
                        " WHERE " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_GUID +
                        " = " + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_GUID + " ) ",
                null
        );
    }

    /**
     * Fetches all recurring transactions from the database.
     * <p>Recurring transactions are the transaction templates which have an entry in the scheduled events table</p>
     * @return Cursor holding set of all recurring transactions
     */
    public Cursor fetchAllScheduledTransactions(){
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TransactionEntry.TABLE_NAME + " INNER JOIN " + ScheduledExportEntry.TABLE_NAME + " ON "
                + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_GUID + " = "
                + ScheduledExportEntry.TABLE_NAME + "." + ScheduledExportEntry.COLUMN_ACTION_UID);

        String[] projectionIn = new String[]{TransactionEntry.TABLE_NAME + ".*",
                ScheduledExportEntry.TABLE_NAME+"."+ ScheduledExportEntry.COLUMN_GUID + " AS " + "origin_scheduled_action_uid"};
        String sortOrder = TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_DESCRIPTION + " ASC";
//        queryBuilder.setDistinct(true);

        return queryBuilder.query(mDb, projectionIn, null, null, null, null, sortOrder);
    }

	/**
	 * Returns list of all transactions for account with UID <code>accountUID</code>
	 * @param accountUID UID of account whose transactions are to be retrieved
	 * @return List of {@link Transaction}s for account with UID <code>accountUID</code>
	 */
    public List<Transaction> getAllTransactionsForAccount(String accountUID){
		Cursor c = fetchAllTransactionsForAccount(accountUID);
		ArrayList<Transaction> transactionsList = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                transactionsList.add(buildModelInstance(c));
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
                transactions.add(buildModelInstance(cursor));
            }
        } finally {
            cursor.close();
        }
        return transactions;
    }

    public Cursor fetchTransactionsWithSplits(String [] columns, @Nullable String where, @Nullable String[] whereArgs, @Nullable String orderBy) {
        return mDb.query(TransactionEntry.TABLE_NAME + " , " + SplitEntry.TABLE_NAME +
                        " ON " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_GUID +
                        " = " + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_GUID +
                        " , trans_extra_info ON trans_extra_info.trans_acct_t_uid = " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_GUID,
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
                TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_GUID + " , " +
                AccountEntry.TABLE_NAME + " AS account1 ON account1." + AccountEntry.COLUMN_GUID +
                " = trans_extra_info.trans_acct_a_uid",
                columns, where, whereArgs, null, null , orderBy);
    }

    /**
     * Return number of transactions in the database (excluding templates)
     * @return Number of transactions
     */
    public long getRecordsCount() {
        String queryCount = "SELECT COUNT(*) FROM " + TransactionEntry.TABLE_NAME +
                " WHERE " + DatabaseSchema.TransactionView.COLUMN_TEMPLATE + " =0";
        Cursor cursor = mDb.rawQuery(queryCount, null);
        try {
            cursor.moveToFirst();
            return cursor.getLong(0);
        } finally {
            cursor.close();
        }
    }

    /**
     * Returns the number of transactions in the database which fulfill the conditions
     * @param where SQL WHERE clause without the "WHERE" itself
     * @param whereArgs Arguments to substitute question marks for
     * @return Number of records in the databases
     */
    public long getRecordsCount(@Nullable String where, @Nullable String[] whereArgs) {
        Cursor cursor = mDb.query(true, TransactionEntry.TABLE_NAME + " , trans_extra_info ON "
                        + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_GUID
                        + " = trans_extra_info.trans_acct_t_uid",
                new String[]{"COUNT(*)"},
                where,
                whereArgs,
                null,
                null,
                null,
                null);
        try{
            cursor.moveToFirst();
            return cursor.getLong(0);
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
    @Override
    public Transaction buildModelInstance(@NonNull final Cursor c){
		String name   = c.getString(c.getColumnIndexOrThrow(TransactionEntry.COLUMN_DESCRIPTION));
		Transaction transaction = new Transaction(name);
        populateBaseModelAttributes(c, transaction);

		transaction.setTime(c.getLong(c.getColumnIndexOrThrow(TransactionEntry.COLUMN_POST_DATE)));
		transaction.setNote(c.getString(c.getColumnIndexOrThrow(TransactionEntry.COLUMN_NOTES)));
		transaction.setExported(c.getInt(c.getColumnIndexOrThrow(DatabaseSchema.SlotEntry.Transaction.COLUMN_EXPORTED)) == 1);
		transaction.setTemplate(c.getInt(c.getColumnIndexOrThrow(DatabaseSchema.TransactionView.COLUMN_TEMPLATE)) == 1);
        String currencyCode = c.getString(c.getColumnIndexOrThrow(TransactionEntry.COLUMN_CURRENCY));
        transaction.setCommodity(mCommoditiesDbAdapter.getCommodity(currencyCode));
        transaction.setScheduledActionUID(c.getString(c.getColumnIndexOrThrow(DatabaseSchema.SlotEntry.Transaction.COLUMN_SCHEDX_ACTION_UID)));
        long transactionID = c.getLong(c.getColumnIndexOrThrow(TransactionEntry._ID));
        transaction.setSplits(mSplitsDbAdapter.getSplitsForTransaction(transactionID));

		return transaction;
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

    /**
	 * Assigns transaction with id <code>rowId</code> to account with id <code>accountId</code>
	 * @param transactionUID GUID of the transaction
     * @param srcAccountUID GUID of the account from which the transaction is to be moved
	 * @param dstAccountUID GUID of the account to which the transaction will be assigned
	 * @return Number of transactions splits affected
	 */
	public int moveTransaction(String transactionUID, String srcAccountUID, String dstAccountUID){
		Log.i(LOG_TAG, "Moving transaction ID " + transactionUID
                + " splits from " + srcAccountUID + " to account " + dstAccountUID);

		List<Split> splits = mSplitsDbAdapter.getSplitsForTransactionInAccount(transactionUID, srcAccountUID);
        for (Split split : splits) {
            split.setAccountUID(dstAccountUID);
        }
        mSplitsDbAdapter.bulkAddRecords(splits, UpdateMethod.update);
        return splits.size();
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
     * Returns the number of template transactions in the database
     * @return Number of template transactions
     */
    public long getTemplateTransactionsCount(){
        String sql = "SELECT COUNT(*) FROM " + TransactionEntry.TABLE_NAME
                + " WHERE " + DatabaseSchema.TransactionView.COLUMN_TEMPLATE + "=1";
        SQLiteStatement statement = mDb.compileStatement(sql);
        return statement.simpleQueryForLong();
    }

    /**
     * Returns a list of all scheduled transactions in the database
     * @return List of all scheduled transactions
     */
    public List<Transaction> getScheduledTransactionsForAccount(String accountUID){
        Cursor cursor = fetchScheduledTransactionsForAccount(accountUID);
        List<Transaction> scheduledTransactions = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                scheduledTransactions.add(buildModelInstance(cursor));
            }
            return scheduledTransactions;
        } finally {
            cursor.close();
        }
    }

    /**
     * Returns the number of splits for the transaction in the database
     * @param transactionUID GUID of the transaction
     * @return Number of splits belonging to the transaction
     */
    public long getSplitCount(@NonNull String transactionUID){
        if (transactionUID == null)
            return 0;
        String sql = "SELECT COUNT(*) FROM " + SplitEntry.TABLE_NAME
                + " WHERE " + SplitEntry.COLUMN_TRANSACTION_GUID + "= '" + transactionUID + "'";
        SQLiteStatement statement = mDb.compileStatement(sql);
        return statement.simpleQueryForLong();
    }

    /**
     * Returns a cursor to transactions whose name (UI: description) start with the <code>prefix</code>
     * <p>This method is used for autocomplete suggestions when creating new transactions. <br/>
     * The suggestions are either transactions which have at least one split with {@code accountUID} or templates.</p>
     * @param prefix Starting characters of the transaction name
     * @param accountUID GUID of account within which to search for transactions
     * @return Cursor to the data set containing all matching transactions
     */
    public Cursor fetchTransactionSuggestions(String prefix, String accountUID){
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TransactionEntry.TABLE_NAME
                + " INNER JOIN " + SplitEntry.TABLE_NAME + " ON "
                + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_GUID + " = "
                + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_GUID);
        queryBuilder.setDistinct(true);
        String[] projectionIn = new String[]{TransactionEntry.TABLE_NAME + ".*"};
        String selection = "(" + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_ACCOUNT_GUID + " = ?"
                + " OR " + TransactionEntry.TABLE_NAME + "." + DatabaseSchema.TransactionView.COLUMN_TEMPLATE + "=1 )"
                + " AND " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_DESCRIPTION + " LIKE '" + prefix + "%'";
        String[] selectionArgs = new String[]{accountUID};
        String sortOrder = TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_POST_DATE + " DESC";
        String groupBy = TransactionEntry.COLUMN_DESCRIPTION;
        String limit = Integer.toString(5);

        return queryBuilder.query(mDb, projectionIn, selection, selectionArgs, groupBy, null, sortOrder, limit);
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
     * Return the number of currencies used in the transaction.
     * For example if there are different splits with different currencies
     * @param transactionUID GUID of the transaction
     * @return Number of currencies within the transaction
     */
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
     * Deletes all transactions except those which are marked as templates.
     * <p>If you want to delete really all transaction records, use {@link #deleteAllRecords()}</p>
     * @return Number of records deleted
     */
    public int deleteAllNonTemplateTransactions(){
        String where = DatabaseSchema.TransactionView.COLUMN_TEMPLATE + "=0";
        return mDb.delete(mTableName, where, null);
    }

    /**
     * Returns a timestamp of the earliest transaction for a specified account type and currency
     * @param type the account type
     * @param currencyCode the currency code
     * @return the earliest transaction's timestamp. Returns 1970-01-01 00:00:00.000 if no transaction found
     */
    public long getTimestampOfEarliestTransaction(AccountType type, String currencyCode) {
        return getTimestamp("MIN", type, currencyCode);
    }

    /**
     * Returns a timestamp of the latest transaction for a specified account type and currency
     * @param type the account type
     * @param currencyCode the currency code
     * @return the latest transaction's timestamp. Returns 1970-01-01 00:00:00.000 if no transaction found
     */
    public long getTimestampOfLatestTransaction(AccountType type, String currencyCode) {
        return getTimestamp("MAX", type, currencyCode);
    }

    /**
     * Returns the most recent `modified_at` timestamp of non-template transactions in the database
     * @return Last moodified time in milliseconds or current time if there is none in the database
     */
    public Timestamp getTimestampOfLastModification(){
        Cursor cursor = mDb.query(TransactionEntry.TABLE_NAME,
                new String[]{"MAX(" + TransactionEntry.COLUMN_MODIFIED_AT + ")"},
                null, null, null, null, null);

        Timestamp timestamp = TimestampHelper.getTimestampFromNow();
        if (cursor.moveToFirst()){
            String timeString = cursor.getString(0);
            if (timeString != null){ //in case there were no transactions in the XML file (account structure only)
                timestamp = TimestampHelper.getTimestampFromUtcString(timeString);
            }
        }
        cursor.close();
        return timestamp;
    }

    /**
     * Returns the earliest or latest timestamp of transactions for a specific account type and currency
     * @param mod Mode (either MAX or MIN)
     * @param type AccountType
     * @param currencyCode the currency code
     * @return earliest or latest timestamp of transactions
     * @see #getTimestampOfLatestTransaction(AccountType, String)
     * @see #getTimestampOfEarliestTransaction(AccountType, String)
     */
    private long getTimestamp(String mod, AccountType type, String currencyCode) {
        String sql = "SELECT " + mod + "(" + TransactionEntry.COLUMN_POST_DATE + ")"
                + " FROM " + TransactionEntry.TABLE_NAME
                + " INNER JOIN " + SplitEntry.TABLE_NAME + " ON "
                + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_GUID + " = "
                + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_GUID
                + " INNER JOIN " + AccountEntry.TABLE_NAME + " ON "
                + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_GUID + " = "
                + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_ACCOUNT_GUID
                + " WHERE " + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_ACCOUNT_TYPE + " = ? AND "
                + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_CURRENCY + " = ? AND "
                + TransactionEntry.TABLE_NAME + "." + DatabaseSchema.TransactionView.COLUMN_TEMPLATE + " = 0";
        Cursor cursor = mDb.rawQuery(sql, new String[]{ type.name(), currencyCode });
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
