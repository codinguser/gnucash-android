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
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;

import static org.gnucash.android.db.DatabaseSchema.*;

/**
 * Helper class for managing the SQLite database.
 * Creates the database and handles upgrades
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    /**
	 * Tag for logging
	 */
	private static final String LOG_TAG = DatabaseHelper.class.getName();
	
	/**
	 * Name of the database
	 */
	private static final String DATABASE_NAME = "gnucash_db";

	/**
	 * Account which the origin account this transaction in double entry mode.
     * This is no longer used since the introduction of splits
	 */
    @Deprecated
	public static final String KEY_DOUBLE_ENTRY_ACCOUNT_UID 	= "double_account_uid";

	/**
	 * SQL statement to create the accounts table in the database
	 */
	private static final String ACCOUNTS_TABLE_CREATE = "create table " + AccountEntry.TABLE_NAME + " ("
			+ AccountEntry._ID                      + " integer primary key autoincrement, "
			+ AccountEntry.COLUMN_UID 	            + " varchar(255) not null, "
			+ AccountEntry.COLUMN_NAME 	            + " varchar(255) not null, "
			+ AccountEntry.COLUMN_TYPE              + " varchar(255) not null, "
			+ AccountEntry.COLUMN_CURRENCY          + " varchar(255) not null, "
            + AccountEntry.COLUMN_COLOR_CODE        + " varchar(255), "
            + AccountEntry.COLUMN_FAVORITE 		    + " tinyint default 0, "
            + AccountEntry.COLUMN_FULL_NAME 	    + " varchar(255), "
            + AccountEntry.COLUMN_PLACEHOLDER            + " tinyint default 0, "
            + AccountEntry.COLUMN_PARENT_ACCOUNT_UID     + " varchar(255), "
            + AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID   + " varchar(255), "
            + "UNIQUE (" + AccountEntry.COLUMN_UID       + ")"
			+ ");";
	
	/**
	 * SQL statement to create the transactions table in the database
	 */
	private static final String TRANSACTIONS_TABLE_CREATE = "create table " + TransactionEntry.TABLE_NAME + " ("
			+ TransactionEntry._ID 		            + " integer primary key autoincrement, "
			+ TransactionEntry.COLUMN_UID 		    + " varchar(255) not null, "
			+ TransactionEntry.COLUMN_NAME		    + " varchar(255), "
			+ TransactionEntry.COLUMN_DESCRIPTION 	+ " text, "
			+ TransactionEntry.COLUMN_TIMESTAMP     + " integer not null, "
			+ TransactionEntry.COLUMN_EXPORTED      + " tinyint default 0, "
            + TransactionEntry.COLUMN_CURRENCY      + " varchar(255) not null, "
            + TransactionEntry.COLUMN_RECURRENCE_PERIOD + " integer default 0, "
			+ "UNIQUE (" 		+ TransactionEntry.COLUMN_UID + ") "
			+ ");";

    /**
     * SQL statement to create the transaction splits table
     */
    private static final String SPLITS_TABLE_CREATE = "CREATE TABLE " + SplitEntry.TABLE_NAME + " ("
            + SplitEntry._ID                    + " integer primary key autoincrement, "
            + SplitEntry.COLUMN_UID             + " varchar(255) not null, "
            + SplitEntry.COLUMN_MEMO 	        + " text, "
            + SplitEntry.COLUMN_TYPE            + " varchar(255) not null, "
            + SplitEntry.COLUMN_AMOUNT          + " varchar(255) not null, "
            + SplitEntry.COLUMN_ACCOUNT_UID 	+ " varchar(255) not null, "
            + SplitEntry.COLUMN_TRANSACTION_UID + " varchar(255) not null, "
            + "FOREIGN KEY (" 	+ SplitEntry.COLUMN_ACCOUNT_UID + ") REFERENCES " + AccountEntry.TABLE_NAME + " (" + AccountEntry.COLUMN_UID + "), "
            + "FOREIGN KEY (" 	+ SplitEntry.COLUMN_TRANSACTION_UID + ") REFERENCES " + TransactionEntry.TABLE_NAME + " (" + TransactionEntry.COLUMN_UID + "), "
            + "UNIQUE (" 		+ SplitEntry.COLUMN_UID + ") "
            + ");";

    /**
	 * Constructor
	 * @param context Application context
	 */
	public DatabaseHelper(Context context){
		super(context, DATABASE_NAME, null, DatabaseSchema.DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		createDatabaseTables(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(LOG_TAG, "Upgrading database from version "
				+ oldVersion + " to " + newVersion);
		
		if (oldVersion < newVersion){
			//introducing double entry accounting
			Log.i(LOG_TAG, "Upgrading database to version " + newVersion);
			if (oldVersion == 1 && newVersion >= 2){
				Log.i(LOG_TAG, "Adding column for double-entry transactions");
				String addColumnSql = "ALTER TABLE " + TransactionEntry.TABLE_NAME +
									" ADD COLUMN " + KEY_DOUBLE_ENTRY_ACCOUNT_UID + " varchar(255)";
				
				//introducing sub accounts
				Log.i(LOG_TAG, "Adding column for parent accounts");
				String addParentAccountSql = "ALTER TABLE " + AccountEntry.TABLE_NAME +
						" ADD COLUMN " + AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " varchar(255)";
	
				db.execSQL(addColumnSql);
				db.execSQL(addParentAccountSql);

                //update account types to GnuCash account types
                //since all were previously CHECKING, now all will be CASH
                Log.i(LOG_TAG, "Converting account types to GnuCash compatible types");
                ContentValues cv = new ContentValues();
                cv.put(SplitEntry.COLUMN_TYPE, AccountType.CASH.toString());
                db.update(AccountEntry.TABLE_NAME, cv, null, null);

                oldVersion = 2;
            }
			

            if (oldVersion == 2 && newVersion >= 3){
                Log.i(LOG_TAG, "Adding flag for placeholder accounts");
                String addPlaceHolderAccountFlagSql = "ALTER TABLE " + AccountEntry.TABLE_NAME +
                        " ADD COLUMN " + AccountEntry.COLUMN_PLACEHOLDER + " tinyint default 0";

                db.execSQL(addPlaceHolderAccountFlagSql);
                oldVersion = 3;
            }

            if (oldVersion == 3 && newVersion >= 4){
                Log.i(LOG_TAG, "Updating database to version 4");
                String addRecurrencePeriod = "ALTER TABLE " + TransactionEntry.TABLE_NAME +
                        " ADD COLUMN " + TransactionEntry.COLUMN_RECURRENCE_PERIOD + " integer default 0";

                String addDefaultTransferAccount = "ALTER TABLE " + AccountEntry.TABLE_NAME
                        + " ADD COLUMN " + AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID + " varchar(255)";

                String addAccountColor = " ALTER TABLE " + AccountEntry.TABLE_NAME
                        + " ADD COLUMN " + AccountEntry.COLUMN_COLOR_CODE + " varchar(255)";

                db.execSQL(addRecurrencePeriod);
                db.execSQL(addDefaultTransferAccount);
                db.execSQL(addAccountColor);

                oldVersion = 4;
            }

            if (oldVersion == 4 && newVersion >= 5){
                Log.i(LOG_TAG, "Upgrading database to version 5");
                String addAccountFavorite = " ALTER TABLE " + AccountEntry.TABLE_NAME
                        + " ADD COLUMN " + AccountEntry.COLUMN_FAVORITE + " tinyint default 0";
                db.execSQL(addAccountFavorite);

                oldVersion = 5;
            }

            if (oldVersion == 5 && newVersion >= 6){
                Log.i(LOG_TAG, "Upgrading database to version 6");
                String addFullAccountNameQuery = " ALTER TABLE " + AccountEntry.TABLE_NAME
                        + " ADD COLUMN " + AccountEntry.COLUMN_FULL_NAME + " varchar(255) ";
                db.execSQL(addFullAccountNameQuery);

                //update all existing accounts with their fully qualified name
                Cursor cursor = db.query(AccountEntry.TABLE_NAME,
                        new String[]{AccountEntry._ID, AccountEntry.COLUMN_UID},
                        null, null, null, null, null);
                while(cursor != null && cursor.moveToNext()){
                    String uid = cursor.getString(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_UID));
                    String fullName = MigrationHelper.getFullyQualifiedAccountName(db, uid);

                    if (fullName == null)
                        continue;

                    ContentValues contentValues = new ContentValues();
                    contentValues.put(AccountEntry.COLUMN_FULL_NAME, fullName);

                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(AccountEntry._ID));
                    db.update(AccountEntry.TABLE_NAME, contentValues, AccountEntry._ID + " = " + id, null);
                }

                if (cursor != null) {
                    cursor.close();
                }

                oldVersion = 6;
            }

            if (oldVersion == 6 && newVersion >= DatabaseSchema.SPLITS_DB_VERSION){
                Log.i(LOG_TAG, "Upgrading database to version 7");

                //for users who do not have double-entry activated, we create imbalance accounts for their splits
                //TODO: Enable when we can hide imbalance accounts from user
//                List<Currency> currencies = MigrationHelper.getCurrencies(db);
//                AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(db);
//                for (Currency currency : currencies) {
//                    accountsDbAdapter.getOrCreateImbalanceAccountUID(currency);
//                }

                String filepath = MigrationHelper.exportDatabase(db, ExportFormat.GNC_XML);

                dropAllDatabaseTables(db);
                createDatabaseTables(db);

                MigrationHelper.importGnucashXML(db, filepath);

                oldVersion = DatabaseSchema.SPLITS_DB_VERSION;
            }
		}

        if (oldVersion != newVersion) {
            Log.w(LOG_TAG, "Upgrade for the database failed. The Database is currently at version " + oldVersion);
        }
	}

    /**
     * Creates the tables in the database
     * @param db Database instance
     */
    private void createDatabaseTables(SQLiteDatabase db) {
        Log.i(LOG_TAG, "Creating database tables");
        db.execSQL(ACCOUNTS_TABLE_CREATE);
        db.execSQL(TRANSACTIONS_TABLE_CREATE);
        db.execSQL(SPLITS_TABLE_CREATE);

        String createAccountUidIndex = "CREATE UNIQUE INDEX '" + AccountEntry.INDEX_UID + "' ON "
                + AccountEntry.TABLE_NAME + "(" + AccountEntry.COLUMN_UID + ")";

        String createTransactionUidIndex = "CREATE UNIQUE INDEX '"+ TransactionEntry.INDEX_UID +"' ON "
                + TransactionEntry.TABLE_NAME + "(" + TransactionEntry.COLUMN_UID + ")";

        String createSplitUidIndex = "CREATE UNIQUE INDEX '" + SplitEntry.INDEX_UID +"' ON "
                + SplitEntry.TABLE_NAME + "(" + SplitEntry.COLUMN_UID + ")";

        db.execSQL(createAccountUidIndex);
        db.execSQL(createTransactionUidIndex);
        db.execSQL(createSplitUidIndex);
    }

    /**
     * Drops all tables in the database
     * @param db Database instance
     */
    private void dropAllDatabaseTables(SQLiteDatabase db) {
        Log.i(LOG_TAG, "Dropping all database tables");
        db.execSQL("DROP TABLE IF EXISTS " + AccountEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TransactionEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + SplitEntry.TABLE_NAME);
    }


}
