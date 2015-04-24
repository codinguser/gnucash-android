/*
 * Copyright (c) 2012 - 2015 Ngewi Fet <ngewif@gmail.com>
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
import android.support.annotation.NonNull;
import android.util.Log;

import org.gnucash.android.db.DatabaseSchema.AccountEntry;
import org.gnucash.android.db.DatabaseSchema.CommonColumns;
import org.gnucash.android.db.DatabaseSchema.SplitEntry;
import org.gnucash.android.db.DatabaseSchema.TransactionEntry;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.BaseModel;

import java.sql.Timestamp;

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
	protected static String LOG_TAG = "DatabaseAdapter";

	/**
	 * SQLite database
	 */
    protected final SQLiteDatabase mDb;

    protected final String mTableName;

    /**
     * Opens the database adapter with an existing database
     * @param db SQLiteDatabase object
     */
    public DatabaseAdapter(SQLiteDatabase db, @NonNull String tableName) {
        this.mTableName = tableName;
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
                        + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TEMPLATE + " AS "
                        + TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_TEMPLATE + " , "
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
     * Returns a ContentValues object which has the data of the base model
     * @param model {@link org.gnucash.android.model.BaseModel} from which to extract values
     * @return {@link android.content.ContentValues} with the data to be inserted into the db
     */
    protected ContentValues getContentValues(BaseModel model){
        ContentValues contentValues = new ContentValues();
        contentValues.put(CommonColumns.COLUMN_UID, model.getUID());
        contentValues.put(CommonColumns.COLUMN_CREATED_AT, model.getCreatedTimestamp().toString());
        //there is a trigger in the database for updated the modified_at column
        /* Due to the use of SQL REPLACE syntax, we insert the created_at values each time
        * (maintain the original creation time and not the time of creation of the replacement)
        * The updated_at column has a trigger in the database which will update the column
         */
        return contentValues;
    }

    /**
     * Initializes the model with values from the database record common to all models (i.e. in the BaseModel)
     * @param cursor Cursor pointing to database record
     * @param model Model instance to be initialized
     */
    protected static void populateModel(Cursor cursor, BaseModel model){
        String uid = cursor.getString(cursor.getColumnIndexOrThrow(CommonColumns.COLUMN_UID));
        String created = cursor.getString(cursor.getColumnIndexOrThrow(CommonColumns.COLUMN_CREATED_AT));
        String modified= cursor.getString(cursor.getColumnIndexOrThrow(CommonColumns.COLUMN_MODIFIED_AT));

        model.setUID(uid);
        model.setCreatedTimestamp(Timestamp.valueOf(created));
        model.setModifiedTimestamp(Timestamp.valueOf(modified));
    }

	/**
	 * Retrieves record with id <code>rowId</code> from database table
	 * @param rowId ID of record to be retrieved
	 * @return {@link Cursor} to record retrieved
	 */
	public Cursor fetchRecord(long rowId){
		return mDb.query(mTableName, null, DatabaseSchema.CommonColumns._ID + "=" + rowId,
				null, null, null, null);
	}

    /**
     * Retrieves record with GUID {@code uid} from database table
     * @param uid GUID of record to be retrieved
     * @return {@link Cursor} to record retrieved
     */
    public Cursor fetchRecord(@NonNull String uid){
        return mDb.query(mTableName, null, CommonColumns.COLUMN_UID + "=?" ,
                new String[]{uid}, null, null, null);
    }

	/**
	 * Retrieves all records from database table
	 * @return {@link Cursor} to all records in table <code>tableName</code>
	 */
	public Cursor fetchAllRecords(){
		return mDb.query(mTableName,
        		null, null, null, null, null, null);
	}

	/**
	 * Deletes record with ID <code>rowID</code> from database table.
	 * @param rowId ID of record to be deleted
	 * @return <code>true</code> if deletion was successful, <code>false</code> otherwise
	 */
	public boolean deleteRecord(long rowId){
        Log.d(LOG_TAG, "Deleting record with id " + rowId + " from " + mTableName);
		return mDb.delete(mTableName, DatabaseSchema.CommonColumns._ID + "=" + rowId, null) > 0;
	}

    /**
     * Deletes all records in the database
     * @return Number of deleted records
     */
    public int deleteAllRecords(){
        return mDb.delete(mTableName, null, null);
    }

    /**
     * Returns the string unique ID (GUID) of a record in the database
     * @param uid GUID of the record
     * @return Long record ID
     */
    public long getID(@NonNull String uid){
        Cursor cursor = mDb.query(mTableName,
                new String[] {DatabaseSchema.CommonColumns._ID},
                DatabaseSchema.CommonColumns.COLUMN_UID + " = ?",
                new String[]{uid},
                null, null, null);
        long result = -1;
        try{
            if (cursor.moveToFirst()) {
                Log.d(LOG_TAG, "Transaction already exists. Returning existing id");
                result = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.CommonColumns._ID));
            } else {
                throw new IllegalArgumentException("Account UID " + uid + " does not exist in the db");
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    /**
     * Returns the string unique ID (GUID) of a record in the database
     * @param id long database record ID
     * @return GUID of the record
     */
    public String getUID(long id){
        Cursor cursor = mDb.query(mTableName,
                new String[]{DatabaseSchema.CommonColumns.COLUMN_UID},
                DatabaseSchema.CommonColumns._ID + " = " + id,
                null, null, null, null);

        String uid = null;
        try {
            if (cursor.moveToFirst()) {
                uid = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.CommonColumns.COLUMN_UID));
            } else {
                throw new IllegalArgumentException("Account record ID " + id + " does not exist in the db");
            }
        } finally {
            cursor.close();
        }
        return uid;
    }

    /**
     * Returns the currency code (according to the ISO 4217 standard) of the account
     * with unique Identifier <code>accountUID</code>
     * @param accountUID Unique Identifier of the account
     * @return Currency code of the account. "" if accountUID
     *      does not exist in DB
     */
    public String getAccountCurrencyCode(@NonNull String accountUID) {
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
    public AccountType getAccountType(@NonNull String accountUID){
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
     * Updates a record in the table
     * @param recordId Database ID of the record to be updated
     * @param columnKey Name of column to be updated
     * @param newValue  New value to be assigned to the columnKey
     * @return Number of records affected
     */
    protected int updateRecord(String tableName, long recordId, String columnKey, String newValue) {
        ContentValues contentValues = new ContentValues();
        if (newValue == null) {
            contentValues.putNull(columnKey);
        } else {
            contentValues.put(columnKey, newValue);
        }
        return mDb.update(tableName, contentValues,
                DatabaseSchema.CommonColumns._ID + "=" + recordId, null);
    }

    /**
     * Updates a record in the table
     * @param uid GUID of the record
     * @param columnKey Name of column to be updated
     * @param newValue  New value to be assigned to the columnKey
     * @return Number of records affected
     */
    public int updateRecord(@NonNull String uid, @NonNull String columnKey, String newValue) {
        return updateRecords(CommonColumns.COLUMN_UID + "= ?", new String[]{uid}, columnKey, newValue);
    }

    /**
     * Overloaded method. Updates the record with GUID {@code uid} with the content values
     * @param uid GUID of the record
     * @param contentValues Content values to update
     * @return Number of records updated
     */
    public int updateRecord(@NonNull String uid, @NonNull ContentValues contentValues){
        return mDb.update(mTableName, contentValues, CommonColumns.COLUMN_UID + "=?", new String[]{uid});
    }

    /**
     * Updates all records which match the {@code where} clause with the {@code newValue} for the column
     * @param where SQL where clause
     * @param whereArgs String arguments for where clause
     * @param columnKey Name of column to be updated
     * @param newValue New value to be assigned to the columnKey
     * @return Number of records affected
     */
    public int updateRecords(String where, String[] whereArgs, @NonNull String columnKey, String newValue){
        ContentValues contentValues = new ContentValues();
        if (newValue == null) {
            contentValues.putNull(columnKey);
        } else {
            contentValues.put(columnKey, newValue);
        }
        return mDb.update(mTableName, contentValues, where, whereArgs);
    }

    /**
     * Deletes a record from the database given its unique identifier.
     * <p>Overload of the method {@link #deleteRecord(long)}</p>
     * @param uid GUID of the record
     * @return <code>true</code> if deletion was successful, <code>false</code> otherwise
     * @see #deleteRecord(long)
     */
    public boolean deleteRecord(@NonNull String uid){
        return deleteRecord(getID(uid));
    }

    /**
     * Returns an attribute from a specific column in the database for a specific record.
     * <p>The attribute is returned as a string which can then be converted to another type if
     * the caller was expecting something other type </p>
     * @param recordUID GUID of the record
     * @param columnName Name of the column to be retrieved
     * @return String value of the column entry
     * @throws IllegalArgumentException if either the {@code recordUID} or {@code columnName} do not exist in the database
     */
    public String getAttribute(@NonNull String recordUID, @NonNull String columnName){
        Cursor cursor = mDb.query(mTableName,
                new String[]{columnName},
                AccountEntry.COLUMN_UID + " = ?",
                new String[]{recordUID}, null, null, null);

        try {
            if (cursor.moveToFirst())
                return cursor.getString(cursor.getColumnIndexOrThrow(columnName));
            else {
                throw new IllegalArgumentException(String.format("Record with GUID %s does not exist in the db", recordUID));
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Expose mDb.beginTransaction()
     */
    public void beginTransaction() {
        mDb.beginTransaction();
    }

    /**
     * Expose mDb.setTransactionSuccessful()
     */
    public void setTransactionSuccessful() {
        mDb.setTransactionSuccessful();
    }

    /**
     * Expose mDb.endTransaction()
     */
    public void endTransaction() {
        mDb.endTransaction();
    }
}
