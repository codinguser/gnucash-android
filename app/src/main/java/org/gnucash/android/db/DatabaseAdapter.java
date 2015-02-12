/*
 * Copyright (c) 2012 - 2014 Ngewi Fet <ngewif@gmail.com>
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

import android.util.Log;
import org.gnucash.android.db.DatabaseSchema.*;
import org.gnucash.android.model.AccountType;

/**
 * Adapter to be used for creating and opening the database for read/write operations.
 * The adapter abstracts several methods for database access and should be subclassed
 * by any other adapters to database-backed data models.
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public abstract class DatabaseAdapter {
	/**
	 * Tag for logging
	 */
	protected static final String TAG = DatabaseAdapter.class.getName();

	/**
	 * SQLite database
	 */
    protected final SQLiteDatabase mDb;

    /**
     * Opens the database adapter with an existing database
     * @param db SQLiteDatabase object
     */
    public DatabaseAdapter(SQLiteDatabase db) {
        this.mDb = db;
        if (!db.isOpen() || db.isReadOnly())
            throw new IllegalArgumentException("Database not open or is read-only. Require writeable database");

        if (mDb.getVersion() >= DatabaseSchema.SPLITS_DB_VERSION) {
            createTempView();
        }
    }

    private void createTempView() {
        // Create some temporary views. Temporary views only exists in one DB session, and will not
        // be saved in the DB
        //
        // TODO: Useful views should be add to the DB
        //
        // create a temporary view, combining accounts, transactions and splits, as this is often used
        // in the queries
        mDb.execSQL("CREATE TEMP VIEW IF NOT EXISTS trans_split_acct AS SELECT "
                        + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID + " AS "
                        + TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_UID + " , "
                        + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_DESCRIPTION + " AS "
                        + TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_DESCRIPTION + " , "
                        + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_NOTES + " AS "
                        + TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_NOTES + " , "
                        + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_CURRENCY + " AS "
                        + TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_CURRENCY + " , "
                        + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TIMESTAMP + " AS "
                        + TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_TIMESTAMP + " , "
                        + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_EXPORTED + " AS "
                        + TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_EXPORTED + " , "
                        + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_RECURRENCE_PERIOD + " AS "
                        + TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_RECURRENCE_PERIOD + " , "
                        + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_UID + " AS "
                        + SplitEntry.TABLE_NAME + "_" + SplitEntry.COLUMN_UID + " , "
                        + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TYPE + " AS "
                        + SplitEntry.TABLE_NAME + "_" + SplitEntry.COLUMN_TYPE + " , "
                        + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_AMOUNT + " AS "
                        + SplitEntry.TABLE_NAME + "_" + SplitEntry.COLUMN_AMOUNT + " , "
                        + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_MEMO + " AS "
                        + SplitEntry.TABLE_NAME + "_" + SplitEntry.COLUMN_MEMO + " , "
                        + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_UID + " AS "
                        + AccountEntry.TABLE_NAME + "_" + AccountEntry.COLUMN_UID + " , "
                        + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_NAME + " AS "
                        + AccountEntry.TABLE_NAME + "_" + AccountEntry.COLUMN_NAME + " , "
                        + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_CURRENCY + " AS "
                        + AccountEntry.TABLE_NAME + "_" + AccountEntry.COLUMN_CURRENCY + " , "
                        + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " AS "
                        + AccountEntry.TABLE_NAME + "_" + AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " , "
                        + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_PLACEHOLDER + " AS "
                        + AccountEntry.TABLE_NAME + "_" + AccountEntry.COLUMN_PLACEHOLDER + " , "
                        + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_COLOR_CODE + " AS "
                        + AccountEntry.TABLE_NAME + "_" + AccountEntry.COLUMN_COLOR_CODE + " , "
                        + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_FAVORITE + " AS "
                        + AccountEntry.TABLE_NAME + "_" + AccountEntry.COLUMN_FAVORITE + " , "
                        + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_FULL_NAME + " AS "
                        + AccountEntry.TABLE_NAME + "_" + AccountEntry.COLUMN_FULL_NAME + " , "
                        + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_TYPE + " AS "
                        + AccountEntry.TABLE_NAME + "_" + AccountEntry.COLUMN_TYPE + " , "
                        + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID + " AS "
                        + AccountEntry.TABLE_NAME + "_" + AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID
                        + " FROM " + TransactionEntry.TABLE_NAME + " , " + SplitEntry.TABLE_NAME + " ON "
                        + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID + "=" + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_UID
                        + " , " + AccountEntry.TABLE_NAME + " ON "
                        + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_ACCOUNT_UID + "=" + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_UID
        );

        // SELECT transactions_uid AS trans_acct_t_uid ,
        //      SUBSTR (
        //          MIN (
        //              ( CASE WHEN IFNULL ( splits_memo , '' ) == '' THEN 'a' ELSE 'b' END ) || accounts_uid
        //          ) ,
        //          2
        //      ) as trans_acct_a_uid ,
        //   TOTAL ( CASE WHEN splits_type = 'DEBIT' THEN splits_amount ELSE - splits_amount END ) AS trans_acct_balance,
        //   COUNT ( DISTINCT accounts_currency ) as trans_currency_count
        //   FROM trans_split_acct GROUP BY transactions_uid
        //
        // This temporary view would pick one Account_UID for each
        // Transaction, which can be used to order all transactions. If possible, account_uid of a split whose
        // memo is null is select.
        //
        // Transaction balance is also picked out by this view
        //
        // a split without split memo is chosen if possible, in the following manner:
        //   if the splits memo is null or empty string, attach an 'a' in front of the split account uid,
        //   if not, attach a 'b' to the split account uid
        //   pick the minimal value of the modified account uid (one of the ones begins with 'a', if exists)
        //   use substr to get account uid
        mDb.execSQL("CREATE TEMP VIEW IF NOT EXISTS trans_extra_info AS SELECT " + TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_UID +
                " AS trans_acct_t_uid , SUBSTR ( MIN ( ( CASE WHEN IFNULL ( " + SplitEntry.TABLE_NAME + "_" +
                SplitEntry.COLUMN_MEMO + " , '' ) == '' THEN 'a' ELSE 'b' END ) || " +
                AccountEntry.TABLE_NAME + "_" + AccountEntry.COLUMN_UID +
                " ) , 2 ) AS trans_acct_a_uid , TOTAL ( CASE WHEN " + SplitEntry.TABLE_NAME + "_" +
                SplitEntry.COLUMN_TYPE + " = 'DEBIT' THEN "+ SplitEntry.TABLE_NAME + "_" +
                SplitEntry.COLUMN_AMOUNT + " ELSE - " + SplitEntry.TABLE_NAME + "_" +
                SplitEntry.COLUMN_AMOUNT + " END ) AS trans_acct_balance , COUNT ( DISTINCT " +
                AccountEntry.TABLE_NAME + "_" + AccountEntry.COLUMN_CURRENCY +
                " ) AS trans_currency_count , COUNT (*) AS trans_split_count FROM trans_split_acct " +
                " GROUP BY " + TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_UID
        );
    }

    /**
     * Checks if the database is open
     * @return <code>true</code> if the database is open, <code>false</code> otherwise
     */
    public boolean isOpen(){
        return mDb.isOpen();
    }

	/**
	 * Retrieves record with id <code>rowId</code> from table <code>tableName</code>
	 * @param tableName Name of table where record is found
	 * @param rowId ID of record to be retrieved
	 * @return {@link Cursor} to record retrieved
	 */
	protected Cursor fetchRecord(String tableName, long rowId){
		return mDb.query(tableName, null, DatabaseSchema.CommonColumns._ID + "=" + rowId,
				null, null, null, null);
	}
	
	/**
	 * Retrieves all records from database table <code>tableName</code>
	 * @param tableName Name of table in database
	 * @return {@link Cursor} to all records in table <code>tableName</code>
	 */
	protected Cursor fetchAllRecords(String tableName){
		return mDb.query(tableName, 
        		null, null, null, null, null, null);
	}

	/**
	 * Deletes record with ID <code>rowID</code> from database table <code>tableName</code>
     * This does not delete the transactions and splits associated with the account
	 * @param tableName Name of table in database
	 * @param rowId ID of record to be deleted
	 * @return <code>true</code> if deletion was successful, <code>false</code> otherwise
	 */
	protected boolean deleteRecord(String tableName, long rowId){
		return mDb.delete(tableName, DatabaseSchema.CommonColumns._ID + "=" + rowId, null) > 0;
	}

    /**
     * Deletes all records in the database
     * @return Number of deleted records
     */
    protected int deleteAllRecords(String tableName){
        return mDb.delete(tableName, null, null);
    }

    /**
     * Returns the string unique ID (GUID) of a record in the database
     * @param tableName Name of table where record is found
     * @param uid GUID of the record
     * @return Long record ID
     */
    protected long getID(String tableName, String uid){
        if (uid == null)
            return 0;

        Cursor cursor = mDb.query(tableName,
                new String[] {DatabaseSchema.CommonColumns._ID},
                DatabaseSchema.CommonColumns.COLUMN_UID + " = ?",
                new String[]{uid},
                null, null, null);
        long result = -1;
        if (cursor != null){
            if (cursor.moveToFirst()) {
                Log.d(TAG, "Transaction already exists. Returning existing id");
                result = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.CommonColumns._ID));
            }
            cursor.close();
        }
        return result;
    }

    /**
     * Returns the string unique ID (GUID) of a record in the database
     * @param tableName Name of table where record is found
     * @param id long database record ID
     * @return GUID of the record
     */
    protected String getUID(String tableName, long id){
        Cursor cursor = mDb.query(tableName,
                new String[]{DatabaseSchema.CommonColumns.COLUMN_UID},
                DatabaseSchema.CommonColumns._ID + " = " + id,
                null, null, null, null);

        String uid = null;
        if (cursor != null){
            if (cursor.moveToFirst()) {
                uid = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.CommonColumns.COLUMN_UID));
            }
            cursor.close();
        }
        return uid;
    }

    /**
     * Retrieves record with id <code>rowId</code> from table
     * @param rowId ID of record to be retrieved
     * @return {@link Cursor} to record retrieved
     */
    public abstract Cursor fetchRecord(long rowId);

    /**
     * Retrieves all records from database table corresponding to this adapter
     * @return {@link Cursor} to all records in table
     */
    public abstract Cursor fetchAllRecords();

    /**
     * Deletes record with ID <code>rowID</code> from database table
     * @param rowId ID of record to be deleted
     * @return <code>true</code> if deletion was successful, <code>false</code> otherwise
     */
    public abstract boolean deleteRecord(long rowId);

    /**
     * Deletes all records in the database table
     * @return Count of database records which have been deleted
     */
    public abstract int deleteAllRecords();

    /**
     * Returns the currency code (according to the ISO 4217 standard) of the account
     * with unique Identifier <code>accountUID</code>
     * @param accountUID Unique Identifier of the account
     * @return Currency code of the account. "" if accountUID
     *      does not exist in DB
     */
    public String getAccountCurrencyCode(String accountUID) {
        Cursor cursor = mDb.query(DatabaseSchema.AccountEntry.TABLE_NAME,
                new String[] {DatabaseSchema.AccountEntry.COLUMN_CURRENCY},
                DatabaseSchema.AccountEntry.COLUMN_UID + "= ?",
                new String[]{accountUID}, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            } else {
                throw new IllegalArgumentException("account " + accountUID + " does not exist");
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Returns the {@link org.gnucash.android.model.AccountType} of the account with unique ID <code>uid</code>
     * @param accountUID Unique ID of the account
     * @return {@link org.gnucash.android.model.AccountType} of the account.
     * @throws java.lang.IllegalArgumentException if accountUID does not exist in DB,
     */
    public AccountType getAccountType(String accountUID){
        String type = "";
        Cursor c = mDb.query(DatabaseSchema.AccountEntry.TABLE_NAME,
                new String[]{DatabaseSchema.AccountEntry.COLUMN_TYPE},
                DatabaseSchema.AccountEntry.COLUMN_UID + "=?",
                new String[]{accountUID}, null, null, null);
        try {
            if (c.moveToFirst()) {
                type = c.getString(c.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_TYPE));
            } else {
                throw new IllegalArgumentException("account " + accountUID + " does not exist in DB");
            }
        } finally {
            c.close();
        }
        return AccountType.valueOf(type);
    }

    /**
     * Returns the database record ID of the entry
     * @param uid GUID of the record
     * @return Long database identifier of the record
     */
    public abstract long getID(String uid);

    /**
     * Returns the global unique identifier of the record
     * @param id Database record ID of the entry
     * @return String GUID of the record
     */
    public abstract String getUID(long id);

    /**
     * Updates a record in the table
     * @param recordId Database ID of the record to be updated
     * @param columnKey Name of column to be updated
     * @param newValue  New value to be assigned to the columnKey
     * @return Number of records affected
     */
    public int updateRecord(String tableName, long recordId, String columnKey, String newValue) {
        ContentValues contentValues = new ContentValues();
        if (newValue == null) {
            contentValues.putNull(columnKey);
        } else {
            contentValues.put(columnKey, newValue);
        }
        return mDb.update(tableName, contentValues,
                DatabaseSchema.CommonColumns._ID + "=" + recordId, null);
    }
}
