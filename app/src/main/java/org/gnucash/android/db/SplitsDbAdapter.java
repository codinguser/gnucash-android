/*
 * Copyright (c) 2014 Ngewi Fet <ngewif@gmail.com>
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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;
import android.util.Log;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.TransactionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static org.gnucash.android.db.DatabaseSchema.SplitEntry;
import static org.gnucash.android.db.DatabaseSchema.TransactionEntry;

/**
 * Database adapter for managing transaction splits in the database
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 * @author Yongxin Wang <fefe.wyx@gmail.com>
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class SplitsDbAdapter extends DatabaseAdapter {

    protected static final String TAG = "SplitsDbAdapter";

    public SplitsDbAdapter(SQLiteDatabase db) {
        super(db, SplitEntry.TABLE_NAME);
    }

    /**
     * Returns application-wide instance of the database adapter
     * @return SplitsDbAdapter instance
     */
    public static SplitsDbAdapter getInstance(){
        return GnuCashApplication.getSplitsDbAdapter();
    }

    /**
     * Adds a split to the database.
     * If the split (with same unique ID) already exists, then it is simply updated
     * @param split {@link org.gnucash.android.model.Split} to be recorded in DB
     * @return Record ID of the newly saved split
     */
    public long addSplit(Split split){
        ContentValues contentValues = getContentValues(split);
        contentValues.put(SplitEntry.COLUMN_AMOUNT,     split.getAmount().absolute().toPlainString());
        contentValues.put(SplitEntry.COLUMN_TYPE,       split.getType().name());
        contentValues.put(SplitEntry.COLUMN_MEMO,       split.getMemo());
        contentValues.put(SplitEntry.COLUMN_ACCOUNT_UID, split.getAccountUID());
        contentValues.put(SplitEntry.COLUMN_TRANSACTION_UID, split.getTransactionUID());

        Log.d(TAG, "Replace transaction split in db");
        long rowId = mDb.replace(SplitEntry.TABLE_NAME, null, contentValues);

        long transactionId = getTransactionID(split.getTransactionUID());
        //when a split is updated, we want mark the transaction as not exported
        updateRecord(TransactionEntry.TABLE_NAME, transactionId,
                TransactionEntry.COLUMN_EXPORTED, String.valueOf(rowId > 0 ? 0 : 1));

        //modifying a split means modifying the accompanying transaction as well
        updateRecord(TransactionEntry.TABLE_NAME, transactionId,
                TransactionEntry.COLUMN_MODIFIED_AT, Long.toString(System.currentTimeMillis()));
        return rowId;
    }

    /**
     * Adds some splits to the database.
     * If the split already exists, then it is simply updated.
     * This function will NOT update the exported status of corresponding transactions.
     * All or none of the splits will be inserted/updated into the database.
     * @param splitList {@link org.gnucash.android.model.Split} to be recorded in DB
     * @return Number of records of the newly saved split
     */
    public long bulkAddSplits(List<Split> splitList) {
        long nRow = 0;
        try {
            mDb.beginTransaction();
            SQLiteStatement replaceStatement = mDb.compileStatement("REPLACE INTO " + SplitEntry.TABLE_NAME + " ( "
                    + SplitEntry.COLUMN_UID             + " , "
                    + SplitEntry.COLUMN_MEMO 	        + " , "
                    + SplitEntry.COLUMN_TYPE            + " , "
                    + SplitEntry.COLUMN_AMOUNT          + " , "
                    + SplitEntry.COLUMN_CREATED_AT      + " , "
                    + SplitEntry.COLUMN_ACCOUNT_UID 	+ " , "
                    + SplitEntry.COLUMN_TRANSACTION_UID + " ) VALUES ( ? , ? , ? , ? , ? , ? , ? ) ");
            for (Split split : splitList) {
                replaceStatement.clearBindings();
                replaceStatement.bindString(1, split.getUID());
                if (split.getMemo() != null) {
                    replaceStatement.bindString(2, split.getMemo());
                }
                replaceStatement.bindString(3, split.getType().name());
                replaceStatement.bindString(4, split.getAmount().absolute().toPlainString());
                replaceStatement.bindString(5, split.getCreatedTimestamp().toString());
                replaceStatement.bindString(6, split.getAccountUID());
                replaceStatement.bindString(7, split.getTransactionUID());

                //Log.d(TAG, "Replacing transaction split in db");
                replaceStatement.execute();
                nRow++;
            }
            mDb.setTransactionSuccessful();
        }
        finally {
            mDb.endTransaction();
        }

        return nRow;
    }

    /**
     * Builds a split instance from the data pointed to by the cursor provided
     * <p>This method will not move the cursor in any way. So the cursor should already by pointing to the correct entry</p>
     * @param cursor Cursor pointing to transaction record in database
     * @return {@link org.gnucash.android.model.Split} instance
     */
    public Split buildSplitInstance(Cursor cursor){
        String amountString = cursor.getString(cursor.getColumnIndexOrThrow(SplitEntry.COLUMN_AMOUNT));
        String typeName     = cursor.getString(cursor.getColumnIndexOrThrow(SplitEntry.COLUMN_TYPE));
        String accountUID   = cursor.getString(cursor.getColumnIndexOrThrow(SplitEntry.COLUMN_ACCOUNT_UID));
        String transxUID    = cursor.getString(cursor.getColumnIndexOrThrow(SplitEntry.COLUMN_TRANSACTION_UID));
        String memo         = cursor.getString(cursor.getColumnIndexOrThrow(SplitEntry.COLUMN_MEMO));

        String currencyCode = getAccountCurrencyCode(accountUID);
        Money amount = new Money(amountString, currencyCode);

        Split split = new Split(amount, accountUID);
        populateModel(cursor, split);
        split.setTransactionUID(transxUID);
        split.setType(TransactionType.valueOf(typeName));
        split.setMemo(memo);

        return split;
    }


    /**
     * Retrieves a split from the database
     * @param uid Unique Identifier String of the split transaction
     * @return {@link org.gnucash.android.model.Split} instance
     */
    public Split getSplit(String uid){
        return getSplit(getID(uid));
    }

    /**
     * Returns the Split instance given the database id
     * @param id Database record ID of the split
     * @return {@link org.gnucash.android.model.Split} instance
     */
    public Split getSplit(long id){
        Cursor cursor = fetchRecord(id);
        try {
            if (cursor.moveToFirst()) {
                return buildSplitInstance(cursor);
            }
            else {
                throw new IllegalArgumentException("split " + id + " does not exist");
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Returns the sum of the splits for a given account.
     * This takes into account the kind of movement caused by the split in the account (which also depends on account type)
     * @param accountUID String unique ID of account
     * @return Balance of the splits for this account
     */
    public Money computeSplitBalance(String accountUID) {
        Cursor cursor = fetchSplitsForAccount(accountUID);
        String currencyCode = getAccountCurrencyCode(accountUID);
        Money splitSum = new Money("0", currencyCode);
        AccountType accountType = getAccountType(accountUID);

        try {
            while (cursor.moveToNext()) {
                String amountString = cursor.getString(cursor.getColumnIndexOrThrow(SplitEntry.COLUMN_AMOUNT));
                String typeString = cursor.getString(cursor.getColumnIndexOrThrow(SplitEntry.COLUMN_TYPE));

                TransactionType transactionType = TransactionType.valueOf(typeString);
                Money amount = new Money(amountString, currencyCode);

                if (accountType.hasDebitNormalBalance()) {
                    switch (transactionType) {
                        case DEBIT:
                            splitSum = splitSum.add(amount);
                            break;
                        case CREDIT:
                            splitSum = splitSum.subtract(amount);
                            break;
                    }
                } else {
                    switch (transactionType) {
                        case DEBIT:
                            splitSum = splitSum.subtract(amount);
                            break;
                        case CREDIT:
                            splitSum = splitSum.add(amount);
                            break;
                    }
                }
            }
        } finally {
            cursor.close();
        }
        return splitSum;
    }

    /**
     * Returns the sum of the splits for given set of accounts.
     * This takes into account the kind of movement caused by the split in the account (which also depends on account type)
     * The Caller must make sure all accounts have the currency, which is passed in as currencyCode
     * @param accountUIDList List of String unique IDs of given set of accounts
     * @param currencyCode currencyCode for all the accounts in the list
     * @param hasDebitNormalBalance Does the final balance has normal debit credit meaning
     * @return Balance of the splits for this account
     */
    public Money computeSplitBalance(List<String> accountUIDList, String currencyCode, boolean hasDebitNormalBalance){
        return calculateSplitBalance(accountUIDList, currencyCode, hasDebitNormalBalance, -1, -1);
    }

    /**
     * Returns the sum of the splits for given set of accounts within the specified time range.
     * This takes into account the kind of movement caused by the split in the account (which also depends on account type)
     * The Caller must make sure all accounts have the currency, which is passed in as currencyCode
     * @param accountUIDList List of String unique IDs of given set of accounts
     * @param currencyCode currencyCode for all the accounts in the list
     * @param hasDebitNormalBalance Does the final balance has normal debit credit meaning
     * @param startTimestamp the start timestamp of the time range
     * @param endTimestamp the end timestamp of the time range
     * @return Balance of the splits for this account within the specified time range
     */
    public Money computeSplitBalance(List<String> accountUIDList, String currencyCode, boolean hasDebitNormalBalance,
                                     long startTimestamp, long endTimestamp){
        return calculateSplitBalance(accountUIDList, currencyCode, hasDebitNormalBalance, startTimestamp, endTimestamp);
    }

    private Money calculateSplitBalance(List<String> accountUIDList, String currencyCode, boolean hasDebitNormalBalance,
                          long startTimestamp, long endTimestamp){
        if (accountUIDList.size() == 0){
            return new Money("0", currencyCode);
        }

        Cursor cursor;
        String[] selectionArgs = null;
        String selection = SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_ACCOUNT_UID + " in ( '" + TextUtils.join("' , '", accountUIDList) + "' ) AND " +
                SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_UID + " = " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID + " AND " +
                TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TEMPLATE + " = 0";

        if (startTimestamp != -1 && endTimestamp != -1) {
            selection += " AND " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TIMESTAMP + " BETWEEN ? AND ? ";
            selectionArgs = new String[]{String.valueOf(startTimestamp), String.valueOf(endTimestamp)};
        } else if (startTimestamp == -1 && endTimestamp != -1) {
            selection += " AND " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TIMESTAMP + " <= ?";
            selectionArgs = new String[]{String.valueOf(endTimestamp)};
        } else if (startTimestamp != -1 && endTimestamp == -1) {
            selection += " AND " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TIMESTAMP + " >= ?";
            selectionArgs = new String[]{String.valueOf(startTimestamp)};
        }

        cursor = mDb.query(SplitEntry.TABLE_NAME + " , " + TransactionEntry.TABLE_NAME,
                new String[]{"TOTAL ( CASE WHEN " + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TYPE + " = 'DEBIT' THEN " +
                        SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_AMOUNT + " ELSE - " + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_AMOUNT + " END )"},
                selection, selectionArgs, null, null, null);

        try {
            if (cursor.moveToFirst()) {
                double amount = cursor.getDouble(0);
                cursor.close();
                Log.d(TAG, "amount return " + amount);
                if (!hasDebitNormalBalance) {
                    amount = -amount;
                }
                return new Money(BigDecimal.valueOf(amount).setScale(2, BigDecimal.ROUND_HALF_UP), Currency.getInstance(currencyCode));
            }
        } finally {
            cursor.close();
        }
        return new Money("0", currencyCode);
    }

//        SELECT TOTAL ( CASE WHEN splits.type = 'DEBIT' THEN splits.amount ELSE - splits.amount END ) FROM splits , transactions WHERE splits.account_uid in ( '532ee7592d4efae7fe2418891d598e59' ) AND splits.transaction_uid = transactions.uid AND transactions.recurrence_period = 0 AND transactions.timestamp BETWEEN ?x AND ?
//        String query = "SELECT TOTAL ( CASE WHEN splits.type = 'DEBIT' THEN splits.amount ELSE - splits.amount END )" +
//                " FROM splits " +
//                " INNER JOIN transactions ON transactions.uid = splits.transaction_uid" +
//                " WHERE splits.account_uid in ( '" + TextUtils.join("' , '", accountUIDList)  + "' )" +
//                " AND transactions.recurrence_period = 0" +
//                " AND transactions.timestamp > 1413109811000";


    /**
     * Returns the list of splits for a transaction
     * @param transactionUID String unique ID of transaction
     * @return List of {@link org.gnucash.android.model.Split}s
     */
    public List<Split> getSplitsForTransaction(String transactionUID){
        Cursor cursor = fetchSplitsForTransaction(transactionUID);
        List<Split> splitList = new ArrayList<Split>();
        try {
            while (cursor.moveToNext()) {
                splitList.add(buildSplitInstance(cursor));
            }
        } finally {
            cursor.close();
        }
        return splitList;
    }

    /**
     * Returns the list of splits for a transaction
     * @param transactionID DB record ID of the transaction
     * @return List of {@link org.gnucash.android.model.Split}s
     * @see #getSplitsForTransaction(String)
     * @see #getTransactionUID(long)
     */
    public List<Split> getSplitsForTransaction(long transactionID){
        return getSplitsForTransaction(getTransactionUID(transactionID));
    }

    /**
     * Fetch splits for a given transaction within a specific account
     * @param transactionUID String unique ID of transaction
     * @param accountUID String unique ID of account
     * @return List of splits
     */
    public List<Split> getSplitsForTransactionInAccount(String transactionUID, String accountUID){
        Cursor cursor = fetchSplitsForTransactionAndAccount(transactionUID, accountUID);
        List<Split> splitList = new ArrayList<Split>();
        if (cursor != null){
            while (cursor.moveToNext()){
                splitList.add(buildSplitInstance(cursor));
            }
            cursor.close();
        }
        return splitList;
    }

    /**
     * Fetches a collection of splits for a given condition and sorted by <code>sortOrder</code>
     * @param where String condition, formatted as SQL WHERE clause
     * @param whereArgs where args
     * @param sortOrder Sort order for the returned records
     * @return Cursor to split records
     */
    public Cursor fetchSplits(String where, String[] whereArgs, String sortOrder){
        return mDb.query(SplitEntry.TABLE_NAME,
                null, where, whereArgs, null, null, sortOrder);
    }

    /**
     * Returns a Cursor to a dataset of splits belonging to a specific transaction
     * @param transactionUID Unique idendtifier of the transaction
     * @return Cursor to splits
     */
    public Cursor fetchSplitsForTransaction(String transactionUID){
        Log.v(TAG, "Fetching all splits for transaction UID " + transactionUID);
        return mDb.query(SplitEntry.TABLE_NAME,
                null, SplitEntry.COLUMN_TRANSACTION_UID + " = ?",
                new String[]{transactionUID},
                null, null, null);
    }

    /**
     * Fetches splits for a given account
     * @param accountUID String unique ID of account
     * @return Cursor containing splits dataset
     */
    public Cursor fetchSplitsForAccount(String accountUID){
        Log.d(TAG, "Fetching all splits for account UID " + accountUID);

        //This is more complicated than a simple "where account_uid=?" query because
        // we need to *not* return any splits which belong to recurring transactions
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TransactionEntry.TABLE_NAME
                + " INNER JOIN " +  SplitEntry.TABLE_NAME + " ON "
                + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID + " = "
                + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_UID);
        queryBuilder.setDistinct(true);
        String[] projectionIn = new String[]{SplitEntry.TABLE_NAME + ".*"};
        String selection = SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_ACCOUNT_UID + " = ?"
                + " AND " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TEMPLATE + " = 0";
        String[] selectionArgs = new String[]{accountUID};
        String sortOrder = TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TIMESTAMP + " DESC";

        return queryBuilder.query(mDb, projectionIn, selection, selectionArgs, null, null, sortOrder);

    }

    /**
     * Returns a cursor to splits for a given transaction and account
     * @param transactionUID Unique idendtifier of the transaction
     * @param accountUID String unique ID of account
     * @return Cursor to splits data set
     */
    public Cursor fetchSplitsForTransactionAndAccount(String transactionUID, String accountUID){
        if (transactionUID == null || accountUID == null)
            return null;

        Log.v(TAG, "Fetching all splits for transaction ID " + transactionUID
                + "and account ID " + accountUID);
        return mDb.query(SplitEntry.TABLE_NAME,
                null, SplitEntry.COLUMN_TRANSACTION_UID + " = ? AND "
                        + SplitEntry.COLUMN_ACCOUNT_UID + " = ?",
                new String[]{transactionUID, accountUID},
                null, null, SplitEntry.COLUMN_AMOUNT + " ASC");
    }

    /**
     * Returns the unique ID of a transaction given the database record ID of same
     * @param transactionId Database record ID of the transaction
     * @return String unique ID of the transaction or null if transaction with the ID cannot be found.
     */
    public String getTransactionUID(long transactionId){
        Cursor cursor = mDb.query(TransactionEntry.TABLE_NAME,
                new String[]{TransactionEntry.COLUMN_UID},
                TransactionEntry._ID + " = " + transactionId,
                null, null, null, null);

        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(TransactionEntry.COLUMN_UID));
            } else {
                throw new IllegalArgumentException("transaction " + transactionId + " does not exist");
            }
        } finally {
            cursor.close();
        }
    }

    @Override
    public boolean deleteRecord(long rowId) {
        Split split = getSplit(rowId);
        String transactionUID = split.getTransactionUID();
        boolean result = mDb.delete(SplitEntry.TABLE_NAME, SplitEntry._ID + "=" + rowId, null) > 0;

        if (!result) //we didn't delete for whatever reason, invalid rowId etc
            return false;

        //if we just deleted the last split, then remove the transaction from db
        Cursor cursor = fetchSplitsForTransaction(transactionUID);
        try {
            if (cursor.getCount() > 0) {
                long transactionID = getTransactionID(transactionUID);
                result = mDb.delete(TransactionEntry.TABLE_NAME,
                        TransactionEntry._ID + "=" + transactionID, null) > 0;
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    /**
     * Returns the database record ID for the specified transaction UID
     * @param transactionUID Unique idendtifier of the transaction
     * @return Database record ID for the transaction
     */
    public long getTransactionID(String transactionUID) {
        Cursor c = mDb.query(TransactionEntry.TABLE_NAME,
                new String[]{TransactionEntry._ID},
                TransactionEntry.COLUMN_UID + "=?",
                new String[]{transactionUID}, null, null, null);
        try {
            if (c.moveToFirst()) {
                return c.getLong(0);
            } else {
                throw new IllegalArgumentException("transaction " + transactionUID + " does not exist");
            }
        } finally {
            c.close();
        }
    }

}
