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

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.ScheduledEvent;

import static org.gnucash.android.db.DatabaseSchema.AccountEntry;
import static org.gnucash.android.db.DatabaseSchema.ScheduledEventEntry;
import static org.gnucash.android.db.DatabaseSchema.SplitEntry;
import static org.gnucash.android.db.DatabaseSchema.TransactionEntry;

/**
 * Helper class for managing the SQLite database.
 * Creates the database and handles upgrades
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
@SuppressWarnings("deprecation")
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
			+ AccountEntry.COLUMN_UID 	            + " varchar(255) not null UNIQUE, "
			+ AccountEntry.COLUMN_NAME 	            + " varchar(255) not null, "
			+ AccountEntry.COLUMN_TYPE              + " varchar(255) not null, "
			+ AccountEntry.COLUMN_CURRENCY          + " varchar(255) not null, "
            + AccountEntry.COLUMN_COLOR_CODE        + " varchar(255), "
            + AccountEntry.COLUMN_FAVORITE 		    + " tinyint default 0, "
            + AccountEntry.COLUMN_HIDDEN 		    + " tinyint default 0, "
            + AccountEntry.COLUMN_FULL_NAME 	    + " varchar(255), "
            + AccountEntry.COLUMN_PLACEHOLDER           + " tinyint default 0, "
            + AccountEntry.COLUMN_PARENT_ACCOUNT_UID    + " varchar(255), "
            + AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID   + " varchar(255), "
            + AccountEntry.COLUMN_CREATED_AT       + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + AccountEntry.COLUMN_MODIFIED_AT      + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP "
			+ ");" + createUpdatedAtTrigger(AccountEntry.TABLE_NAME);
	
	/**
	 * SQL statement to create the transactions table in the database
	 */
	private static final String TRANSACTIONS_TABLE_CREATE = "create table " + TransactionEntry.TABLE_NAME + " ("
			+ TransactionEntry._ID 		            + " integer primary key autoincrement, "
			+ TransactionEntry.COLUMN_UID 		    + " varchar(255) not null UNIQUE, "
			+ TransactionEntry.COLUMN_DESCRIPTION   + " varchar(255), "
			+ TransactionEntry.COLUMN_NOTES         + " text, "
			+ TransactionEntry.COLUMN_TIMESTAMP     + " integer not null, "
			+ TransactionEntry.COLUMN_EXPORTED      + " tinyint default 0, "
            + TransactionEntry.COLUMN_CURRENCY      + " varchar(255) not null, "
            + TransactionEntry.COLUMN_RECURRENCE_PERIOD + " integer default 0, "
            + TransactionEntry.COLUMN_CREATED_AT       + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + TransactionEntry.COLUMN_MODIFIED_AT      + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP "
			+ ");" + createUpdatedAtTrigger(TransactionEntry.TABLE_NAME);

    /**
     * SQL statement to create the transaction splits table
     */
    private static final String SPLITS_TABLE_CREATE = "CREATE TABLE " + SplitEntry.TABLE_NAME + " ("
            + SplitEntry._ID                    + " integer primary key autoincrement, "
            + SplitEntry.COLUMN_UID             + " varchar(255) not null UNIQUE, "
            + SplitEntry.COLUMN_MEMO 	        + " text, "
            + SplitEntry.COLUMN_TYPE            + " varchar(255) not null, "
            + SplitEntry.COLUMN_AMOUNT          + " varchar(255) not null, "
            + SplitEntry.COLUMN_ACCOUNT_UID 	+ " varchar(255) not null, "
            + SplitEntry.COLUMN_TRANSACTION_UID + " varchar(255) not null, "
            + SplitEntry.COLUMN_CREATED_AT       + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + SplitEntry.COLUMN_MODIFIED_AT      + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY (" 	+ SplitEntry.COLUMN_ACCOUNT_UID + ") REFERENCES " + AccountEntry.TABLE_NAME + " (" + AccountEntry.COLUMN_UID + ") ON DELETE CASCADE, "
            + "FOREIGN KEY (" 	+ SplitEntry.COLUMN_TRANSACTION_UID + ") REFERENCES " + TransactionEntry.TABLE_NAME + " (" + TransactionEntry.COLUMN_UID + ") ON DELETE CASCADE "
            + ");" + createUpdatedAtTrigger(SplitEntry.TABLE_NAME);


    public static final String SCHEDULED_EVENTS_TABLE_CREATE = "CREATE TABLE " + ScheduledEventEntry.TABLE_NAME + " ("
            + ScheduledEventEntry._ID                   + " integer primary key autoincrement, "
            + ScheduledEventEntry.COLUMN_UID            + " varchar(255) not null UNIQUE, "
            + ScheduledEventEntry.COLUMN_EVENT_UID      + " varchar(255) not null, "
            + ScheduledEventEntry.COLUMN_TYPE           + " varchar(255) not null, "
            + ScheduledEventEntry.COLUMN_PERIOD         + " integer not null, "
            + ScheduledEventEntry.COLUMN_LAST_RUN       + " integer default 0, "
            + ScheduledEventEntry.COLUMN_START_TIME     + " integer not null, "
            + ScheduledEventEntry.COLUMN_END_TIME       + " integer default 0, "
            + ScheduledEventEntry.COLUMN_TAG            + " text, "
            + ScheduledEventEntry.COLUMN_CREATED_AT     + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + ScheduledEventEntry.COLUMN_MODIFIED_AT    + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP "
            + ");" + createUpdatedAtTrigger(ScheduledEventEntry.TABLE_NAME);


    /**
	 * Constructor
	 * @param context Application context
	 */
	public DatabaseHelper(Context context){
		super(context, DATABASE_NAME, null, DatabaseSchema.DATABASE_VERSION);

	}

    /**
     * Creates an update trigger to update the updated_at column for all records in the database.
     * This has to be run per table, and is currently appended to the create table statement.
     * @param tableName Name of table on which to create trigger
     * @return SQL statement for creating trigger
     */
    static String createUpdatedAtTrigger(String tableName){
        return "CREATE TRIGGER update_time_trigger "
                + "  AFTER UPDATE ON " + tableName + " FOR EACH ROW"
                + "  BEGIN " + "UPDATE " + tableName
                + "  SET " + DatabaseSchema.CommonColumns.COLUMN_MODIFIED_AT + " = CURRENT_TIMESTAMP;"
                + "  END;";
    }

	@Override
	public void onCreate(SQLiteDatabase db) {
		createDatabaseTables(db);
	}

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL("PRAGMA foreign_keys=ON");
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
                db.beginTransaction();
                try {
                    // backup transaction table
                    db.execSQL("ALTER TABLE " + TransactionEntry.TABLE_NAME + " RENAME TO " + TransactionEntry.TABLE_NAME + "_bak");
                    // create new transaction table
                    db.execSQL("create table " + TransactionEntry.TABLE_NAME + " ("
                            + TransactionEntry._ID + " integer primary key autoincrement, "
                            + TransactionEntry.COLUMN_UID + " varchar(255) not null, "
                            + TransactionEntry.COLUMN_DESCRIPTION + " varchar(255), "
                            + TransactionEntry.COLUMN_NOTES + " text, "
                            + TransactionEntry.COLUMN_TIMESTAMP + " integer not null, "
                            + TransactionEntry.COLUMN_EXPORTED + " tinyint default 0, "
                            + TransactionEntry.COLUMN_CURRENCY + " varchar(255) not null, "
                            + TransactionEntry.COLUMN_RECURRENCE_PERIOD + " integer default 0, "
                            + "UNIQUE (" + TransactionEntry.COLUMN_UID + ") "
                            + ");");
                    // initialize new transaction table wiht data from old table
                    db.execSQL("INSERT INTO " + TransactionEntry.TABLE_NAME + " ( "
                                    + TransactionEntry._ID + " , "
                                    + TransactionEntry.COLUMN_UID + " , "
                                    + TransactionEntry.COLUMN_DESCRIPTION + " , "
                                    + TransactionEntry.COLUMN_NOTES + " , "
                                    + TransactionEntry.COLUMN_TIMESTAMP + " , "
                                    + TransactionEntry.COLUMN_EXPORTED + " , "
                                    + TransactionEntry.COLUMN_CURRENCY + " , "
                                    + TransactionEntry.COLUMN_RECURRENCE_PERIOD + " )  SELECT "
                                    + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry._ID + " , "
                                    + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry.COLUMN_UID + " , "
                                    + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry.COLUMN_DESCRIPTION + " , "
                                    + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry.COLUMN_NOTES + " , "
                                    + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry.COLUMN_TIMESTAMP + " , "
                                    + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry.COLUMN_EXPORTED + " , "
                                    + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_CURRENCY + " , "
                                    + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry.COLUMN_RECURRENCE_PERIOD
                                    + " FROM " + TransactionEntry.TABLE_NAME + "_bak , " + AccountEntry.TABLE_NAME
                                    + " ON " + TransactionEntry.TABLE_NAME + "_bak.account_uid == " + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_UID
                    );
                    // create split table
                    db.execSQL("CREATE TABLE " + SplitEntry.TABLE_NAME + " ("
                            + SplitEntry._ID + " integer primary key autoincrement, "
                            + SplitEntry.COLUMN_UID + " varchar(255) not null, "
                            + SplitEntry.COLUMN_MEMO + " text, "
                            + SplitEntry.COLUMN_TYPE + " varchar(255) not null, "
                            + SplitEntry.COLUMN_AMOUNT + " varchar(255) not null, "
                            + SplitEntry.COLUMN_ACCOUNT_UID + " varchar(255) not null, "
                            + SplitEntry.COLUMN_TRANSACTION_UID + " varchar(255) not null, "
                            + "FOREIGN KEY (" + SplitEntry.COLUMN_ACCOUNT_UID + ") REFERENCES " + AccountEntry.TABLE_NAME + " (" + AccountEntry.COLUMN_UID + "), "
                            + "FOREIGN KEY (" + SplitEntry.COLUMN_TRANSACTION_UID + ") REFERENCES " + TransactionEntry.TABLE_NAME + " (" + TransactionEntry.COLUMN_UID + "), "
                            + "UNIQUE (" + SplitEntry.COLUMN_UID + ") "
                            + ");");
                    // Initialize split table with data from backup transaction table
                    // New split table is initialized after the new transaction table as the
                    // foreign key constraint will stop any data from being inserted
                    // If new split table is created before the backup is made, the foreign key
                    // constraint will be rewritten to refer to the backup transaction table
                    db.execSQL("INSERT INTO " + SplitEntry.TABLE_NAME + " ( "
                            + SplitEntry.COLUMN_UID + " , "
                            + SplitEntry.COLUMN_TYPE + " , "
                            + SplitEntry.COLUMN_AMOUNT + " , "
                            + SplitEntry.COLUMN_ACCOUNT_UID + " , "
                            + SplitEntry.COLUMN_TRANSACTION_UID + " ) SELECT "
                            + "LOWER(HEX(RANDOMBLOB(16))) , "
                            + "CASE WHEN " + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_TYPE + " IN ( 'CASH' , 'BANK', 'ASSET', 'EXPENSE', 'RECEIVABLE', 'STOCK', 'MUTUAL' ) THEN CASE WHEN "
                                    + SplitEntry.COLUMN_AMOUNT + " < 0 THEN 'CREDIT' ELSE 'DEBIT' END ELSE CASE WHEN "
                                    + SplitEntry.COLUMN_AMOUNT + " < 0 THEN 'DEBIT' ELSE 'CREDIT' END END , "
                            + "ABS ( " + TransactionEntry.TABLE_NAME + "_bak.amount ) , "
                            + TransactionEntry.TABLE_NAME + "_bak.account_uid , "
                            + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry.COLUMN_UID
                            + " FROM " + TransactionEntry.TABLE_NAME + "_bak , " + AccountEntry.TABLE_NAME
                            + " ON " + TransactionEntry.TABLE_NAME + "_bak.account_uid = " + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_UID
                            + " UNION SELECT "
                            + "LOWER(HEX(RANDOMBLOB(16))) AS " + SplitEntry.COLUMN_UID + " , "
                            + "CASE WHEN " + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_TYPE + " IN ( 'CASH' , 'BANK', 'ASSET', 'EXPENSE', 'RECEIVABLE', 'STOCK', 'MUTUAL' ) THEN CASE WHEN "
                                    + SplitEntry.COLUMN_AMOUNT + " < 0 THEN 'DEBIT' ELSE 'CREDIT' END ELSE CASE WHEN "
                                    + SplitEntry.COLUMN_AMOUNT + " < 0 THEN 'CREDIT' ELSE 'DEBIT' END END , "
                            + "ABS ( " + TransactionEntry.TABLE_NAME + "_bak.amount ) , "
                            + TransactionEntry.TABLE_NAME + "_bak." + KEY_DOUBLE_ENTRY_ACCOUNT_UID + " , "
                            + TransactionEntry.TABLE_NAME + "_baK." + TransactionEntry.COLUMN_UID
                            + " FROM " + TransactionEntry.TABLE_NAME + "_bak , " + AccountEntry.TABLE_NAME
                            + " ON " + TransactionEntry.TABLE_NAME + "_bak.account_uid = " + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_UID
                            + " WHERE " + TransactionEntry.TABLE_NAME + "_bak." + KEY_DOUBLE_ENTRY_ACCOUNT_UID + " IS NOT NULL"
                    );
                    // drop backup transaction table
                    db.execSQL("DROP TABLE " + TransactionEntry.TABLE_NAME + "_bak");
                    db.setTransactionSuccessful();
                    oldVersion = DatabaseSchema.SPLITS_DB_VERSION;
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion == 7 && newVersion >= 8){
                Log.i(LOG_TAG, "Upgrading database to version 8");
                //TODO: consider just backing up, recreating database and reimporting
                //FIXME: We really need to do this because the ON DELETE CASCADE constraint does not exist on older db versions
                //TODO: Also, we need to go through db and add second split with imbalance account wherever only one split exists.

                Log.i(LOG_TAG, "Adding hidden flag to accounts table");
                String addHiddenFlagSql = "ALTER TABLE " + AccountEntry.TABLE_NAME +
                        " ADD COLUMN " + AccountEntry.COLUMN_HIDDEN + " tinyint default 0";
                db.execSQL(addHiddenFlagSql);

                Log.i(LOG_TAG, "Adding created_at and modified_at columns to database tables");
                MigrationHelper.createUpdatedAndModifiedColumns(db, AccountEntry.TABLE_NAME);
                MigrationHelper.createUpdatedAndModifiedColumns(db, TransactionEntry.TABLE_NAME);
                MigrationHelper.createUpdatedAndModifiedColumns(db, SplitEntry.TABLE_NAME);

                Log.i(LOG_TAG, "Creating scheduled events table");
                db.execSQL(SCHEDULED_EVENTS_TABLE_CREATE);
                //TODO: Migrate existing scheduled transactions (cancel pending intents)

                GnuCashApplication.startScheduledEventExecutionService(GnuCashApplication.getAppContext());


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
        db.execSQL(SCHEDULED_EVENTS_TABLE_CREATE);

        String createAccountUidIndex = "CREATE UNIQUE INDEX '" + AccountEntry.INDEX_UID + "' ON "
                + AccountEntry.TABLE_NAME + "(" + AccountEntry.COLUMN_UID + ")";

        String createTransactionUidIndex = "CREATE UNIQUE INDEX '"+ TransactionEntry.INDEX_UID +"' ON "
                + TransactionEntry.TABLE_NAME + "(" + TransactionEntry.COLUMN_UID + ")";

        String createSplitUidIndex = "CREATE UNIQUE INDEX '" + SplitEntry.INDEX_UID +"' ON "
                + SplitEntry.TABLE_NAME + "(" + SplitEntry.COLUMN_UID + ")";

        String createScheduledEventUidIndex = "CREATE UNIQUE INDEX '" + ScheduledEventEntry.INDEX_UID
                +"' ON " + ScheduledEventEntry.TABLE_NAME + "(" + ScheduledEventEntry.COLUMN_UID + ")";

        db.execSQL(createAccountUidIndex);
        db.execSQL(createTransactionUidIndex);
        db.execSQL(createSplitUidIndex);
        db.execSQL(createScheduledEventUidIndex);
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
        db.execSQL("DROP TABLE IF EXISTS " + ScheduledEventEntry.TABLE_NAME);
    }


}
