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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.model.Transaction;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Currency;
import java.util.UUID;

import static org.gnucash.android.db.DatabaseSchema.AccountEntry;
import static org.gnucash.android.db.DatabaseSchema.ScheduledActionEntry;
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
	 * SQL statement to create the accounts table in the database
	 */
	private static final String ACCOUNTS_TABLE_CREATE = "create table " + AccountEntry.TABLE_NAME + " ("
			+ AccountEntry._ID                      + " integer primary key autoincrement, "
			+ AccountEntry.COLUMN_UID 	            + " varchar(255) not null UNIQUE, "
			+ AccountEntry.COLUMN_NAME 	            + " varchar(255) not null, "
			+ AccountEntry.COLUMN_TYPE              + " varchar(255) not null, "
			+ AccountEntry.COLUMN_CURRENCY          + " varchar(255) not null, "
			+ AccountEntry.COLUMN_DESCRIPTION       + " varchar(255), "
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
			+ TransactionEntry.COLUMN_TEMPLATE      + " tinyint default 0, "
            + TransactionEntry.COLUMN_CURRENCY      + " varchar(255) not null, "
            + TransactionEntry.COLUMN_SCHEDX_ACTION_UID + " varchar(255), "
            + TransactionEntry.COLUMN_CREATED_AT    + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + TransactionEntry.COLUMN_MODIFIED_AT   + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY (" 	+ TransactionEntry.COLUMN_SCHEDX_ACTION_UID + ") REFERENCES " + ScheduledActionEntry.TABLE_NAME + " (" + ScheduledActionEntry.COLUMN_UID + ") ON DELETE SET NULL "
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


    public static final String SCHEDULED_ACTIONS_TABLE_CREATE = "CREATE TABLE " + ScheduledActionEntry.TABLE_NAME + " ("
            + ScheduledActionEntry._ID                   + " integer primary key autoincrement, "
            + ScheduledActionEntry.COLUMN_UID            + " varchar(255) not null UNIQUE, "
            + ScheduledActionEntry.COLUMN_ACTION_UID    + " varchar(255) not null, "
            + ScheduledActionEntry.COLUMN_TYPE           + " varchar(255) not null, "
            + ScheduledActionEntry.COLUMN_PERIOD         + " integer not null, "
            + ScheduledActionEntry.COLUMN_LAST_RUN       + " integer default 0, "
            + ScheduledActionEntry.COLUMN_START_TIME     + " integer not null, "
            + ScheduledActionEntry.COLUMN_END_TIME       + " integer default 0, "
            + ScheduledActionEntry.COLUMN_TAG            + " text, "
            + ScheduledActionEntry.COLUMN_ENABLED        + " tinyint default 1, " //enabled by default
            + ScheduledActionEntry.COLUMN_TOTAL_FREQUENCY + " integer default 0, "
            + ScheduledActionEntry.COLUMN_EXECUTION_COUNT+ " integer default 0, "
            + ScheduledActionEntry.COLUMN_CREATED_AT     + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + ScheduledActionEntry.COLUMN_MODIFIED_AT    + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP "
            + ");" + createUpdatedAtTrigger(ScheduledActionEntry.TABLE_NAME);


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
									" ADD COLUMN double_account_uid varchar(255)";
				
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
                        " ADD COLUMN recurrence_period integer default 0";

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
                oldVersion = upgradeDbToVersion6(db);
            }

            if (oldVersion == 6 && newVersion >= DatabaseSchema.SPLITS_DB_VERSION){
                Log.i(LOG_TAG, "Upgrading database to version 7");
                oldVersion = upgradeDbToVersion7(db);
            }

            if (oldVersion == 7 && newVersion >= 8){
                Log.i(LOG_TAG, "Upgrading database to version 8");
                oldVersion = upgradeDbToVersion8(db);
            }
		}

        if (oldVersion != newVersion) {
            Log.w(LOG_TAG, "Upgrade for the database failed. The Database is currently at version " + oldVersion);
        }
	}

    /**
     * Upgrades the database from version 7 to version 8.
     * <p>This migration accomplishes the following:
     *      <ul>
     *          <li>Added created_at and modified_at columns to all tables (including triggers for updating the columns).</li>
     *          <li>New table for scheduled actions and migrate all existing recurring transactions</li>
     *          <li>Auto-balancing of all existing splits</li>
     *          <li>Added "hidden" flag to accounts table</li>
     *          <li>Add flag for transaction templates</li>
     *      </ul>
     * </p>
     * @param db SQLite Database to be upgraded
     * @return New database version (8) if upgrade successful, old version (7) if unsuccessful
     */
    private int upgradeDbToVersion8(SQLiteDatabase db) {
        Log.i(LOG_TAG, "Upgrading database to version 8");
        int oldVersion = 7;
        new File(Exporter.BACKUP_FOLDER_PATH).mkdirs();
        new File(Exporter.EXPORT_FOLDER_PATH).mkdirs();
        //start moving the files in background thread before we do the database stuff
        new Thread(MigrationHelper.moveExportedFilesToNewDefaultLocation).start();

        db.beginTransaction();
        try {

            Log.i(LOG_TAG, "Creating scheduled actions table");
            db.execSQL("CREATE TABLE " + ScheduledActionEntry.TABLE_NAME + " ("
                    + ScheduledActionEntry._ID                   + " integer primary key autoincrement, "
                    + ScheduledActionEntry.COLUMN_UID            + " varchar(255) not null UNIQUE, "
                    + ScheduledActionEntry.COLUMN_ACTION_UID    + " varchar(255) not null, "
                    + ScheduledActionEntry.COLUMN_TYPE           + " varchar(255) not null, "
                    + ScheduledActionEntry.COLUMN_PERIOD         + " integer not null, "
                    + ScheduledActionEntry.COLUMN_LAST_RUN       + " integer default 0, "
                    + ScheduledActionEntry.COLUMN_START_TIME     + " integer not null, "
                    + ScheduledActionEntry.COLUMN_END_TIME       + " integer default 0, "
                    + ScheduledActionEntry.COLUMN_TAG            + " text, "
                    + ScheduledActionEntry.COLUMN_ENABLED        + " tinyint default 1, " //enabled by default
                    + ScheduledActionEntry.COLUMN_TOTAL_FREQUENCY + " integer default 0, "
                    + ScheduledActionEntry.COLUMN_EXECUTION_COUNT+ " integer default 0, "
                    + ScheduledActionEntry.COLUMN_CREATED_AT     + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + ScheduledActionEntry.COLUMN_MODIFIED_AT    + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP "
                    + ");" + createUpdatedAtTrigger(ScheduledActionEntry.TABLE_NAME));


            //==============================BEGIN TABLE MIGRATIONS ========================================
            Log.i(LOG_TAG, "Migrating accounts table");
            // backup transaction table
            db.execSQL("ALTER TABLE " + AccountEntry.TABLE_NAME + " RENAME TO " + AccountEntry.TABLE_NAME + "_bak");
            // create new transaction table
            db.execSQL("CREATE TABLE " + AccountEntry.TABLE_NAME + " ("
                    + AccountEntry._ID + " integer primary key autoincrement, "
                    + AccountEntry.COLUMN_UID + " varchar(255) not null UNIQUE, "
                    + AccountEntry.COLUMN_NAME + " varchar(255) not null, "
                    + AccountEntry.COLUMN_TYPE + " varchar(255) not null, "
                    + AccountEntry.COLUMN_CURRENCY + " varchar(255) not null, "
                    + AccountEntry.COLUMN_DESCRIPTION + " varchar(255), "
                    + AccountEntry.COLUMN_COLOR_CODE + " varchar(255), "
                    + AccountEntry.COLUMN_FAVORITE + " tinyint default 0, "
                    + AccountEntry.COLUMN_HIDDEN + " tinyint default 0, "
                    + AccountEntry.COLUMN_FULL_NAME + " varchar(255), "
                    + AccountEntry.COLUMN_PLACEHOLDER + " tinyint default 0, "
                    + AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " varchar(255), "
                    + AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID + " varchar(255), "
                    + AccountEntry.COLUMN_CREATED_AT + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + AccountEntry.COLUMN_MODIFIED_AT + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP "
                    + ");" + createUpdatedAtTrigger(AccountEntry.TABLE_NAME));

            // initialize new account table with data from old table
            db.execSQL("INSERT INTO " + AccountEntry.TABLE_NAME + " ( "
                            + AccountEntry._ID + ","
                            + AccountEntry.COLUMN_UID + " , "
                            + AccountEntry.COLUMN_NAME + " , "
                            + AccountEntry.COLUMN_TYPE + " , "
                            + AccountEntry.COLUMN_CURRENCY + " , "
                            + AccountEntry.COLUMN_COLOR_CODE + " , "
                            + AccountEntry.COLUMN_FAVORITE + " , "
                            + AccountEntry.COLUMN_FULL_NAME + " , "
                            + AccountEntry.COLUMN_PLACEHOLDER + " , "
                            + AccountEntry.COLUMN_HIDDEN + " , "
                            + AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " , "
                            + AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID
                            + ") SELECT "
                            + AccountEntry.TABLE_NAME + "_bak." + AccountEntry._ID + " , "
                            + AccountEntry.TABLE_NAME + "_bak." + AccountEntry.COLUMN_UID + " , "
                            + AccountEntry.TABLE_NAME + "_bak." + AccountEntry.COLUMN_NAME + " , "
                            + AccountEntry.TABLE_NAME + "_bak." + AccountEntry.COLUMN_TYPE + " , "
                            + AccountEntry.TABLE_NAME + "_bak." + AccountEntry.COLUMN_CURRENCY + " , "
                            + AccountEntry.TABLE_NAME + "_bak." + AccountEntry.COLUMN_COLOR_CODE + " , "
                            + AccountEntry.TABLE_NAME + "_bak." + AccountEntry.COLUMN_FAVORITE + " , "
                            + AccountEntry.TABLE_NAME + "_bak." + AccountEntry.COLUMN_FULL_NAME + " , "
                            + AccountEntry.TABLE_NAME + "_bak." + AccountEntry.COLUMN_PLACEHOLDER + " , "
                            + " CASE WHEN " + AccountEntry.TABLE_NAME + "_bak.type = 'ROOT' THEN 1 ELSE 0 END, "
                            + AccountEntry.TABLE_NAME + "_bak." + AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " , "
                            + AccountEntry.TABLE_NAME + "_bak." + AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID
                            + " FROM " + AccountEntry.TABLE_NAME + "_bak;"
            );

            Log.i(LOG_TAG, "Migrating transactions table");
            // backup transaction table
            db.execSQL("ALTER TABLE " + TransactionEntry.TABLE_NAME + " RENAME TO " + TransactionEntry.TABLE_NAME + "_bak");
            // create new transaction table
            db.execSQL("CREATE TABLE " + TransactionEntry.TABLE_NAME + " ("
                    + TransactionEntry._ID + " integer primary key autoincrement, "
                    + TransactionEntry.COLUMN_UID + " varchar(255) not null UNIQUE, "
                    + TransactionEntry.COLUMN_DESCRIPTION + " varchar(255), "
                    + TransactionEntry.COLUMN_NOTES + " text, "
                    + TransactionEntry.COLUMN_TIMESTAMP + " integer not null, "
                    + TransactionEntry.COLUMN_EXPORTED + " tinyint default 0, "
                    + TransactionEntry.COLUMN_TEMPLATE + " tinyint default 0, "
                    + TransactionEntry.COLUMN_CURRENCY + " varchar(255) not null, "
                    + TransactionEntry.COLUMN_SCHEDX_ACTION_UID + " varchar(255), "
                    + TransactionEntry.COLUMN_CREATED_AT + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + TransactionEntry.COLUMN_MODIFIED_AT + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (" + TransactionEntry.COLUMN_SCHEDX_ACTION_UID + ") REFERENCES " + ScheduledActionEntry.TABLE_NAME + " (" + ScheduledActionEntry.COLUMN_UID + ") ON DELETE SET NULL "
                    + ");" + createUpdatedAtTrigger(TransactionEntry.TABLE_NAME));

            // initialize new transaction table with data from old table
            db.execSQL("INSERT INTO " + TransactionEntry.TABLE_NAME + " ( "
                            + TransactionEntry._ID + " , "
                            + TransactionEntry.COLUMN_UID + " , "
                            + TransactionEntry.COLUMN_DESCRIPTION + " , "
                            + TransactionEntry.COLUMN_NOTES + " , "
                            + TransactionEntry.COLUMN_TIMESTAMP + " , "
                            + TransactionEntry.COLUMN_EXPORTED + " , "
                            + TransactionEntry.COLUMN_CURRENCY + " , "
                            + TransactionEntry.COLUMN_TEMPLATE
                            + ")  SELECT "
                            + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry._ID + " , "
                            + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry.COLUMN_UID + " , "
                            + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry.COLUMN_DESCRIPTION + " , "
                            + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry.COLUMN_NOTES + " , "
                            + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry.COLUMN_TIMESTAMP + " , "
                            + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry.COLUMN_EXPORTED + " , "
                            + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry.COLUMN_CURRENCY + " , "
                            + " CASE WHEN " + TransactionEntry.TABLE_NAME + "_bak.recurrence_period > 0 THEN 1 ELSE 0 END "
                            + " FROM " + TransactionEntry.TABLE_NAME + "_bak;"
            );

            Log.i(LOG_TAG, "Migrating splits table");
            // backup transaction table
            db.execSQL("ALTER TABLE " + SplitEntry.TABLE_NAME + " RENAME TO " + SplitEntry.TABLE_NAME + "_bak");
            // create new split table
            db.execSQL("CREATE TABLE " + SplitEntry.TABLE_NAME + " ("
                    + SplitEntry._ID + " integer primary key autoincrement, "
                    + SplitEntry.COLUMN_UID + " varchar(255) not null UNIQUE, "
                    + SplitEntry.COLUMN_MEMO + " text, "
                    + SplitEntry.COLUMN_TYPE + " varchar(255) not null, "
                    + SplitEntry.COLUMN_AMOUNT + " varchar(255) not null, "
                    + SplitEntry.COLUMN_ACCOUNT_UID + " varchar(255) not null, "
                    + SplitEntry.COLUMN_TRANSACTION_UID + " varchar(255) not null, "
                    + SplitEntry.COLUMN_CREATED_AT + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + SplitEntry.COLUMN_MODIFIED_AT + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (" + SplitEntry.COLUMN_ACCOUNT_UID + ") REFERENCES " + AccountEntry.TABLE_NAME + " (" + AccountEntry.COLUMN_UID + ") ON DELETE CASCADE, "
                    + "FOREIGN KEY (" + SplitEntry.COLUMN_TRANSACTION_UID + ") REFERENCES " + TransactionEntry.TABLE_NAME + " (" + TransactionEntry.COLUMN_UID + ") ON DELETE CASCADE "
                    + ");" + createUpdatedAtTrigger(SplitEntry.TABLE_NAME));

            // initialize new split table with data from old table
            db.execSQL("INSERT INTO " + SplitEntry.TABLE_NAME + " ( "
                            + SplitEntry._ID + " , "
                            + SplitEntry.COLUMN_UID + " , "
                            + SplitEntry.COLUMN_MEMO + " , "
                            + SplitEntry.COLUMN_TYPE + " , "
                            + SplitEntry.COLUMN_AMOUNT + " , "
                            + SplitEntry.COLUMN_ACCOUNT_UID + " , "
                            + SplitEntry.COLUMN_TRANSACTION_UID
                            + ")  SELECT "
                            + SplitEntry.TABLE_NAME + "_bak." + SplitEntry._ID + " , "
                            + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_UID + " , "
                            + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_MEMO + " , "
                            + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_TYPE + " , "
                            + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_AMOUNT + " , "
                            + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_ACCOUNT_UID + " , "
                            + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_TRANSACTION_UID
                            + " FROM " + SplitEntry.TABLE_NAME + "_bak;"
            );



            //================================ END TABLE MIGRATIONS ================================



            ScheduledActionDbAdapter scheduledActionDbAdapter = new ScheduledActionDbAdapter(db);
            SplitsDbAdapter splitsDbAdapter = new SplitsDbAdapter(db);
            TransactionsDbAdapter transactionsDbAdapter = new TransactionsDbAdapter(db, splitsDbAdapter);
            AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(db,transactionsDbAdapter);

            Log.i(LOG_TAG, "Creating default root account if none exists");
            ContentValues contentValues = new ContentValues();
            //assign a root account to all accounts which had null as parent (top-level accounts)
            String rootAccountUID = accountsDbAdapter.getOrCreateGnuCashRootAccountUID();
            contentValues.put(AccountEntry.COLUMN_PARENT_ACCOUNT_UID, rootAccountUID);
            db.update(AccountEntry.TABLE_NAME, contentValues, AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " IS NULL AND " + AccountEntry.COLUMN_TYPE + " != ?", new String[]{"ROOT"});

            Log.i(LOG_TAG, "Migrating existing recurring transactions");
            Cursor cursor = db.query(TransactionEntry.TABLE_NAME + "_bak", null, "recurrence_period > 0", null, null, null, null);
            while (cursor.moveToNext()){
                contentValues.clear();
                Timestamp timestamp = new Timestamp(cursor.getLong(cursor.getColumnIndexOrThrow(TransactionEntry.COLUMN_TIMESTAMP)));
                contentValues.put(TransactionEntry.COLUMN_CREATED_AT, timestamp.toString());
                long transactionId = cursor.getLong(cursor.getColumnIndexOrThrow(TransactionEntry._ID));
                db.update(TransactionEntry.TABLE_NAME, contentValues, TransactionEntry._ID + "=" + transactionId, null);

                ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
                scheduledAction.setActionUID(cursor.getString(cursor.getColumnIndexOrThrow(TransactionEntry.COLUMN_UID)));
                long period = cursor.getLong(cursor.getColumnIndexOrThrow("recurrence_period"));
                scheduledAction.setPeriod(period);
                scheduledAction.setStartTime(timestamp.getTime()); //the start time is when the transaction was created
                scheduledAction.setLastRun(System.currentTimeMillis()); //prevent this from being executed at the end of migration
                scheduledActionDbAdapter.addScheduledAction(scheduledAction);

                //build intent for recurring transactions in the database
                Intent intent = new Intent(Intent.ACTION_INSERT);
                intent.setType(Transaction.MIME_TYPE);

                //cancel existing pending intent
                Context context = GnuCashApplication.getAppContext();
                PendingIntent recurringPendingIntent = PendingIntent.getBroadcast(context,
                        (int)transactionId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                alarmManager.cancel(recurringPendingIntent);
            }
            cursor.close();

            //auto-balance existing splits
            Log.i(LOG_TAG, "Auto-balancing existing transaction splits");
//            cursor = transactionsDbAdapter.fetchAllRecords();
//            while (cursor.moveToNext()){
//                Transaction transaction = transactionsDbAdapter.buildTransactionInstance(cursor);
//                if (transaction.isTemplate())
//                    continue;
//                Money imbalance = transaction.getImbalance();
//                if (!imbalance.isAmountZero()){
//                    Split split = new Split(imbalance.negate(),
//                            accountsDbAdapter.getOrCreateImbalanceAccountUID(imbalance.getCurrency()));
//                    split.setTransactionUID(transaction.getUID());
//                    splitsDbAdapter.addSplit(split);
//                }
//            }
//            cursor.close();
            cursor = db.query(
                    "trans_extra_info , " + TransactionEntry.TABLE_NAME + " ON "
                            + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID +
                            " = trans_extra_info.trans_acct_t_uid",
                    new String[]{
                            TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID + " AS trans_uid",
                            TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_CURRENCY + " AS trans_currency",
                            "trans_extra_info.trans_acct_balance AS trans_acct_balance",
                            "trans_extra_info.trans_currency_count AS trans_currency_count",
                    }, "trans_acct_balance != 0 AND trans_currency_count = 1 AND " +
                    TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TEMPLATE + " == 0", null, null, null, null);
            try {
                String timestamp = (new Timestamp(System.currentTimeMillis())).toString();
                while (cursor.moveToNext()){
                    double imbalance = cursor.getDouble(cursor.getColumnIndexOrThrow("trans_acct_balance"));
                    BigDecimal decimalImbalance = BigDecimal.valueOf(imbalance).setScale(2, BigDecimal.ROUND_HALF_UP);
                    if (decimalImbalance.compareTo(BigDecimal.ZERO) != 0) {
                        String currencyCode = cursor.getString(cursor.getColumnIndexOrThrow("trans_currency"));
                        String TransactionUID = cursor.getString(cursor.getColumnIndexOrThrow("trans_uid"));
                        contentValues.clear();
                        contentValues.put(DatabaseSchema.CommonColumns.COLUMN_UID, UUID.randomUUID().toString().replaceAll("-", ""));
                        contentValues.put(DatabaseSchema.CommonColumns.COLUMN_CREATED_AT, timestamp);
                        contentValues.put(SplitEntry.COLUMN_AMOUNT,     decimalImbalance.abs().toPlainString());
                        contentValues.put(SplitEntry.COLUMN_TYPE,       decimalImbalance.compareTo(BigDecimal.ZERO) > 0 ? "DEBIT" : "CREDIT");
                        contentValues.put(SplitEntry.COLUMN_MEMO,       "");
                        contentValues.put(SplitEntry.COLUMN_ACCOUNT_UID, accountsDbAdapter.getOrCreateImbalanceAccountUID(Currency.getInstance(currencyCode)));
                        contentValues.put(SplitEntry.COLUMN_TRANSACTION_UID, TransactionUID);
                        db.insert(SplitEntry.TABLE_NAME, null, contentValues);
                        contentValues.clear();
                        contentValues.put(TransactionEntry.COLUMN_MODIFIED_AT, timestamp);
                        db.update(TransactionEntry.TABLE_NAME, contentValues, TransactionEntry.COLUMN_UID + " == ?",
                                new String[]{TransactionUID});
                    }
                }
            } finally {
                cursor.close();
            }

            Log.i(LOG_TAG, "Dropping temporary migration tables");
            db.execSQL("DROP TABLE " + SplitEntry.TABLE_NAME + "_bak");
            db.execSQL("DROP TABLE " + AccountEntry.TABLE_NAME + "_bak");
            db.execSQL("DROP TABLE " + TransactionEntry.TABLE_NAME + "_bak");

            db.setTransactionSuccessful();
            oldVersion = 8;
        } finally {
            db.endTransaction();
        }

        GnuCashApplication.startScheduledActionExecutionService(GnuCashApplication.getAppContext());

        return oldVersion;
    }

    /**
     * Code for upgrading the database to the {@link DatabaseSchema#SPLITS_DB_VERSION} from version 6.<br>
     * Tasks accomplished in migration:
     *  <ul>
     *      <li>Added new splits table for transaction splits</li>
     *      <li>Extract existing info from transactions table to populate split table</li>
     *  </ul>
     * @param db SQLite Database
     * @return The new database version if upgrade was successful, or the old db version if it failed
     */
    private int upgradeDbToVersion7(SQLiteDatabase db) {
        int oldVersion = 6;
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
                    + "recurrence_period integer default 0, "
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
                            + "recurrence_period )  SELECT "
                            + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry._ID + " , "
                            + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry.COLUMN_UID + " , "
                            + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry.COLUMN_DESCRIPTION + " , "
                            + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry.COLUMN_NOTES + " , "
                            + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry.COLUMN_TIMESTAMP + " , "
                            + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry.COLUMN_EXPORTED + " , "
                            + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_CURRENCY + " , "
                            + TransactionEntry.TABLE_NAME + "_bak.recurrence_period"
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
                    + TransactionEntry.TABLE_NAME + "_bak.double_account_uid , "
                    + TransactionEntry.TABLE_NAME + "_baK." + TransactionEntry.COLUMN_UID
                    + " FROM " + TransactionEntry.TABLE_NAME + "_bak , " + AccountEntry.TABLE_NAME
                    + " ON " + TransactionEntry.TABLE_NAME + "_bak.account_uid = " + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_UID
                    + " WHERE " + TransactionEntry.TABLE_NAME + "_bak.double_account_uid IS NOT NULL"
            );
            // drop backup transaction table
            db.execSQL("DROP TABLE " + TransactionEntry.TABLE_NAME + "_bak");
            db.setTransactionSuccessful();
            oldVersion = DatabaseSchema.SPLITS_DB_VERSION;
        } finally {
            db.endTransaction();
        }
        return oldVersion;
    }

    /**
     * Upgrades the database from version 5 to version 6.<br>
     * This migration adds support for fully qualified account names and updates existing accounts.
     * @param db SQLite Database to be upgraded
     * @return New database version (6) if upgrade successful, old version (5) if unsuccessful
     */
    private int upgradeDbToVersion6(SQLiteDatabase db) {
        int oldVersion = 5;
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
        return oldVersion;
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
        db.execSQL(SCHEDULED_ACTIONS_TABLE_CREATE);

        String createAccountUidIndex = "CREATE UNIQUE INDEX '" + AccountEntry.INDEX_UID + "' ON "
                + AccountEntry.TABLE_NAME + "(" + AccountEntry.COLUMN_UID + ")";

        String createTransactionUidIndex = "CREATE UNIQUE INDEX '"+ TransactionEntry.INDEX_UID +"' ON "
                + TransactionEntry.TABLE_NAME + "(" + TransactionEntry.COLUMN_UID + ")";

        String createSplitUidIndex = "CREATE UNIQUE INDEX '" + SplitEntry.INDEX_UID +"' ON "
                + SplitEntry.TABLE_NAME + "(" + SplitEntry.COLUMN_UID + ")";

        String createScheduledEventUidIndex = "CREATE UNIQUE INDEX '" + ScheduledActionEntry.INDEX_UID
                +"' ON " + ScheduledActionEntry.TABLE_NAME + "(" + ScheduledActionEntry.COLUMN_UID + ")";

        db.execSQL(createAccountUidIndex);
        db.execSQL(createTransactionUidIndex);
        db.execSQL(createSplitUidIndex);
        db.execSQL(createScheduledEventUidIndex);
    }


}
