/*
 * Copyright (c) 2014 Ngewi Fet <ngewif@gmail.com>
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
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.TransactionType;

import java.util.ArrayList;
import java.util.List;

import static org.gnucash.android.db.DatabaseSchema.*;

/**
 * Database adapter for managing transaction splits in the database
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class SplitsDbAdapter extends DatabaseAdapter {

    protected static final String TAG = "SplitsDbAdapter";

    public SplitsDbAdapter(Context context){
        super(context);
    }

    public SplitsDbAdapter(SQLiteDatabase db) {
        super(db);
    }

    /**
     * Adds a split to the database.
     * If the split (with same unique ID) already exists, then it is simply updated
     * @param split {@link org.gnucash.android.model.Split} to be recorded in DB
     * @return Record ID of the newly saved split
     */
    public long addSplit(Split split){
        ContentValues contentValues = new ContentValues();
        contentValues.put(SplitEntry.COLUMN_UID,        split.getUID());
        contentValues.put(SplitEntry.COLUMN_AMOUNT,     split.getAmount().absolute().toPlainString());
        contentValues.put(SplitEntry.COLUMN_TYPE,       split.getType().name());
        contentValues.put(SplitEntry.COLUMN_MEMO,       split.getMemo());
        contentValues.put(SplitEntry.COLUMN_ACCOUNT_UID, split.getAccountUID());
        contentValues.put(SplitEntry.COLUMN_TRANSACTION_UID, split.getTransactionUID());

        long rowId = -1;
        if ((rowId = getID(split.getUID())) > 0){
            //if split already exists, then just update
            Log.d(TAG, "Updating existing transaction split");
            mDb.update(SplitEntry.TABLE_NAME, contentValues,
                    SplitEntry._ID + " = " + rowId, null);
        } else {
            Log.d(TAG, "Adding new transaction split to db");
            rowId = mDb.insert(SplitEntry.TABLE_NAME, null, contentValues);
        }

        //when a split is updated, we want mark the transaction as not exported
        updateRecord(TransactionEntry.TABLE_NAME, getTransactionID(split.getTransactionUID()),
                TransactionEntry.COLUMN_EXPORTED, String.valueOf(rowId > 0 ? 1 : 0));
        return rowId;
    }

    /**
     * Builds a split instance from the data pointed to by the cursor provided
     * <p>This method will not move the cursor in any way. So the cursor should already by pointing to the correct entry</p>
     * @param cursor Cursor pointing to transaction record in database
     * @return {@link org.gnucash.android.model.Split} instance
     */
    public Split buildSplitInstance(Cursor cursor){
        String uid          = cursor.getString(cursor.getColumnIndexOrThrow(SplitEntry.COLUMN_UID));
        String amountString = cursor.getString(cursor.getColumnIndexOrThrow(SplitEntry.COLUMN_AMOUNT));
        String typeName     = cursor.getString(cursor.getColumnIndexOrThrow(SplitEntry.COLUMN_TYPE));
        String accountUID   = cursor.getString(cursor.getColumnIndexOrThrow(SplitEntry.COLUMN_ACCOUNT_UID));
        String transxUID    = cursor.getString(cursor.getColumnIndexOrThrow(SplitEntry.COLUMN_TRANSACTION_UID));
        String memo         = cursor.getString(cursor.getColumnIndexOrThrow(SplitEntry.COLUMN_MEMO));

        String currencyCode = getCurrencyCode(accountUID);
        Money amount = new Money(amountString, currencyCode);

        Split split = new Split(amount, accountUID);
        split.setUID(uid);
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
        long id = getID(uid);
        Cursor cursor = fetchRecord(id);

        Split split = null;
        if (cursor != null && cursor.moveToFirst()){
            split = buildSplitInstance(cursor);
            cursor.close();
        }
        return split;
    }

    /**
     * Returns the sum of the splits for a given account.
     * This takes into account the kind of movement caused by the split in the account (which also depends on account type)
     * @param accountUID String unique ID of account
     * @return Balance of the splits for this account
     */
    public Money computeSplitBalance(String accountUID){
        Cursor cursor = fetchSplitsForAccount(accountUID);
        String currencyCode = getCurrencyCode(accountUID);
        Money splitSum = new Money("0", currencyCode);
        AccountType accountType = getAccountType(accountUID);

        if (cursor != null){
            while(cursor.moveToNext()){
                String amountString = cursor.getString(cursor.getColumnIndexOrThrow(SplitEntry.COLUMN_AMOUNT));
                String typeString = cursor.getString(cursor.getColumnIndexOrThrow(SplitEntry.COLUMN_TYPE));

                TransactionType transactionType = TransactionType.valueOf(typeString);
                Money amount = new Money(amountString, currencyCode);

                if (accountType.hasDebitNormalBalance()){
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
            cursor.close();
        }
        return splitSum;
    }

    /**
     * Returns the list of splits for a transaction
     * @param transactionUID String unique ID of transaction
     * @return List of {@link org.gnucash.android.model.Split}s
     */
    public List<Split> getSplitsForTransaction(String transactionUID){
        Cursor cursor = fetchSplitsForTransaction(transactionUID);
        List<Split> splitList = new ArrayList<Split>();
        while (cursor != null && cursor.moveToNext()){
            splitList.add(buildSplitInstance(cursor));
        }
        if (cursor != null)
            cursor.close();

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
        while (cursor != null && cursor.moveToNext()){
            splitList.add(buildSplitInstance(cursor));
        }
        if (cursor != null)
            cursor.close();

        return splitList;
    }

    /**
     * Fetches a collection of splits for a given condition and sorted by <code>sortOrder</code>
     * @param condition String condition, formatted as SQL WHERE clause
     * @param sortOrder Sort order for the returned records
     * @return Cursor to split records
     */
    public Cursor fetchSplits(String condition, String sortOrder){
        return mDb.query(SplitEntry.TABLE_NAME,
                null, condition, null, null, null, sortOrder);
    }

    /**
     * Returns the database record ID of the split with unique IDentifier <code>uid</code>
     * @param uid Unique Identifier String of the split transaction
     * @return Database record ID of split
     */
    public long getID(String uid){
        Cursor cursor = mDb.query(SplitEntry.TABLE_NAME,
                new String[] {SplitEntry._ID},
                SplitEntry.COLUMN_UID + " = ?", new String[]{uid}, null, null, null);
        long result = -1;
        if (cursor != null && cursor.moveToFirst()){
            Log.d(TAG, "Transaction already exists. Returning existing id");
            result = cursor.getLong(cursor.getColumnIndexOrThrow(SplitEntry._ID));

            cursor.close();
        }
        return result;
    }

    /**
     * Returns the unique identifier string of the split
     * @param id Database record ID of the split
     * @return String unique identifier of the split
     */
    public String getUID(long id){
        Cursor cursor = mDb.query(SplitEntry.TABLE_NAME,
                new String[]{SplitEntry.COLUMN_UID},
                SplitEntry._ID + " = " + id, null, null, null, null);

        String uid = null;
        if (cursor != null){
            if (cursor.moveToFirst()) {
                uid = cursor.getString(cursor.getColumnIndexOrThrow(SplitEntry.COLUMN_UID));
            }
            cursor.close();
        }
        return uid;
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
                + " AND " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_RECURRENCE_PERIOD + " = 0";
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

        String trxUID = null;
        if (cursor != null && cursor.moveToFirst()){
            trxUID = cursor.getString(cursor.getColumnIndexOrThrow(TransactionEntry.COLUMN_UID));
        }

        return trxUID;
    }

    @Override
    public Cursor fetchRecord(long rowId) {
        return fetchRecord(SplitEntry.TABLE_NAME, rowId);
    }

    @Override
    public Cursor fetchAllRecords() {
        return fetchAllRecords(SplitEntry.TABLE_NAME);
    }

    @Override
    public boolean deleteRecord(long rowId) {
        String transactionUID = getSplit(getUID(rowId)).getTransactionUID();
        boolean result = deleteRecord(SplitEntry.TABLE_NAME, rowId);

        //if we just deleted the last split, then remove the transaction from db
        Cursor cursor = fetchSplitsForTransaction(transactionUID);
        if (cursor != null){
            if (cursor.getCount() > 0){
                result &= deleteTransaction(getTransactionID(transactionUID));
            }
            cursor.close();
        }
        return result;
    }

    /**
     * Returns the database record ID for the specified transaction UID
     * @param transactionUID Unique idendtifier of the transaction
     * @return Database record ID for the transaction
     */
    public long getTransactionID(String transactionUID){
        long id = -1;
        Cursor c = mDb.query(TransactionEntry.TABLE_NAME,
                new String[]{TransactionEntry._ID},
                TransactionEntry.COLUMN_UID + "=?",
                new String[]{transactionUID}, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                id = c.getLong(0);
            }
            c.close();
        }
        return id;
    }

    /**
     * Deletes all splits for a particular transaction and the transaction itself
     * @param transactionId Database record ID of the transaction
     * @return <code>true</code> if at least one split was deleted, <code>false</code> otherwise.
     */
    public boolean deleteSplitsForTransaction(long transactionId){
        String trxUID = getTransactionUID(transactionId);
        boolean result = mDb.delete(SplitEntry.TABLE_NAME,
                SplitEntry.COLUMN_TRANSACTION_UID + "=?",
                new String[]{trxUID}) > 0;
        result &= deleteTransaction(transactionId);
        return result;
    }

    /**
     * Deletes splits for a specific transaction and account and the transaction itself
     * @param transactionId Database record ID of the transaction
     * @param accountId Database ID of the account
     * @return Number of records deleted
     */
    public int deleteSplitsForTransactionAndAccount(long transactionId, long accountId){
        String transactionUID  = getTransactionUID(transactionId);
        String accountUID      = getAccountUID(accountId);
        int deletedCount = mDb.delete(SplitEntry.TABLE_NAME,
                SplitEntry.COLUMN_TRANSACTION_UID + "= ? AND " + SplitEntry.COLUMN_ACCOUNT_UID + "= ?",
                new String[]{transactionUID, accountUID});
        deleteTransaction(transactionId);
        return deletedCount;
    }

    /**
     * Deletes the transaction from the the database
     * @param transactionId Database record ID of the transaction
     */
    private boolean deleteTransaction(long transactionId) {
        return deleteRecord(TransactionEntry.TABLE_NAME, transactionId);
    }

    @Override
    public int deleteAllRecords() {
        return deleteAllRecords(SplitEntry.TABLE_NAME);
    }
}
