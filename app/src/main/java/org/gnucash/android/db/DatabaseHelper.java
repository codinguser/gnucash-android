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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.model.Commodity;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

import static org.gnucash.android.db.DatabaseSchema.AccountEntry;
import static org.gnucash.android.db.DatabaseSchema.BudgetAmountEntry;
import static org.gnucash.android.db.DatabaseSchema.BudgetEntry;
import static org.gnucash.android.db.DatabaseSchema.CommodityEntry;
import static org.gnucash.android.db.DatabaseSchema.CommonColumns;
import static org.gnucash.android.db.DatabaseSchema.LotEntry;
import static org.gnucash.android.db.DatabaseSchema.PriceEntry;
import static org.gnucash.android.db.DatabaseSchema.RecurrenceEntry;
import static org.gnucash.android.db.DatabaseSchema.ScheduledExportEntry;
import static org.gnucash.android.db.DatabaseSchema.ScheduledTransactionEntry;
import static org.gnucash.android.db.DatabaseSchema.SplitEntry;
import static org.gnucash.android.db.DatabaseSchema.TransactionEntry;
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
	public static final String LOG_TAG = DatabaseHelper.class.getName();

    /**
	 * SQL statement to create the accounts table in the database
	 */
	private static final String ACCOUNTS_TABLE_CREATE = "create table " + AccountEntry.TABLE_NAME + " ("
			+ AccountEntry._ID                      + " integer primary key autoincrement, "
			+ AccountEntry.COLUMN_GUID              + " varchar(255) not null UNIQUE, "
			+ AccountEntry.COLUMN_NAME              + " varchar(255) not null, "
            + AccountEntry.COLUMN_DESCRIPTION       + " varchar(255), "
            + AccountEntry.COLUMN_ACCOUNT_TYPE      + " varchar(255) not null, "
			+ AccountEntry.COLUMN_CURRENCY_CODE     + " varchar(255) not null, "
            + AccountEntry.COLUMN_COMMODITY_GUID    + " varchar(255) not null, "
            + AccountEntry.COLUMN_COMMODITY_SCU     + " integer not null, "
            + AccountEntry.COLUMN_NON_STD_SCU       + " integer not null, "
            + AccountEntry.COLUMN_PARENT_GUID       + " varchar(255), "
            + AccountEntry.COLUMN_CREATED_AT       + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + AccountEntry.COLUMN_MODIFIED_AT      + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY (" 	+ AccountEntry.COLUMN_COMMODITY_GUID + ") REFERENCES " + CommodityEntry.TABLE_NAME + " (" + CommodityEntry.COLUMN_GUID + ") "
			+ ");" + createUpdatedAtTrigger(AccountEntry.TABLE_NAME);
	
	/**
	 * SQL statement to create the transactions table in the database
	 */
	private static final String TRANSACTIONS_TABLE_CREATE = "create table " + TransactionEntry.TABLE_NAME + " ("
			+ TransactionEntry._ID 		            + " integer primary key autoincrement, "
			+ TransactionEntry.COLUMN_GUID          + " varchar(255) not null UNIQUE, "
			+ TransactionEntry.COLUMN_DESCRIPTION   + " varchar(255), "
			+ TransactionEntry.COLUMN_POST_DATE     + " timestamp not null, "
			+ TransactionEntry.COLUMN_ENTER_DATE     + " timestamp not null, "
			+ TransactionEntry.COLUMN_NUM           + " integer not null, "
            + TransactionEntry.COLUMN_COMMODITY_GUID + " varchar(255) not null, "
            + TransactionEntry.COLUMN_CREATED_AT    + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + TransactionEntry.COLUMN_MODIFIED_AT   + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY (" 	+ DatabaseSchema.SlotEntry.Transaction.COLUMN_SCHEDX_ACTION_UID + ") REFERENCES " + ScheduledExportEntry.TABLE_NAME + " (" + ScheduledExportEntry.COLUMN_GUID + ") ON DELETE SET NULL, "
            + "FOREIGN KEY (" 	+ TransactionEntry.COLUMN_COMMODITY_GUID + ") REFERENCES " + CommodityEntry.TABLE_NAME + " (" + CommodityEntry.COLUMN_GUID + ") "
			+ ");" + createUpdatedAtTrigger(TransactionEntry.TABLE_NAME);

    /**
     * SQL statement to create the transaction splits table
     */
    private static final String SPLITS_TABLE_CREATE = "CREATE TABLE " + SplitEntry.TABLE_NAME + " ("
            + SplitEntry._ID                    + " integer primary key autoincrement, "
            + SplitEntry.COLUMN_GUID            + " varchar(255) not null UNIQUE, "
            + SplitEntry.COLUMN_ACCOUNT_GUID    + " varchar(255) not null, "
            + SplitEntry.COLUMN_MEMO 	        + " text, "
            + SplitEntry.COLUMN_ACTION          + " varchar(255) not null, "
            + SplitEntry.COLUMN_VALUE_NUM       + " integer not null, "
            + SplitEntry.COLUMN_VALUE_DENOM     + " integer not null, "
            + SplitEntry.COLUMN_QUANTITY_NUM    + " integer not null, "
            + SplitEntry.COLUMN_QUANTITY_DENOM  + " integer not null, "
            + SplitEntry.COLUMN_TRANSACTION_GUID + " varchar(255) not null, "
            + SplitEntry.COLUMN_LOT_GUID        + " varchar(32), "
            + SplitEntry.COLUMN_RECONCILE_STATE + " varchar(1) not null default 'n', "
            + SplitEntry.COLUMN_RECONCILE_DATE  + " timestamp not null default current_timestamp, "
            + SplitEntry.COLUMN_CREATED_AT      + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + SplitEntry.COLUMN_MODIFIED_AT     + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY (" 	+ SplitEntry.COLUMN_ACCOUNT_GUID + ") REFERENCES " + AccountEntry.TABLE_NAME + " (" + AccountEntry.COLUMN_GUID + ") ON DELETE CASCADE, "
            + "FOREIGN KEY (" 	+ SplitEntry.COLUMN_TRANSACTION_GUID + ") REFERENCES " + TransactionEntry.TABLE_NAME + " (" + TransactionEntry.COLUMN_GUID + ") ON DELETE CASCADE "
            + ");" + createUpdatedAtTrigger(SplitEntry.TABLE_NAME);

    /**
     * Lots are used to tie buy and sell transactions together
     * This table is currently not used by the Android app, but is included for completeness of the DB schema
     */
    private static final String LOTS_TABLE_CREATE = "CREATE TABLE " + LotEntry.TABLE_NAME + " ("
            + LotEntry._ID                    + " integer primary key autoincrement, "
            + LotEntry.COLUMN_GUID            + " varchar(255) not null UNIQUE, "
            + LotEntry.COLUMN_ACCOUNT_GUID    + " varchar(255) not null, "
            + LotEntry.COLUMN_IS_CLOSED       + " tinyint not null, "
            + ");" + createUpdatedAtTrigger(LotEntry.TABLE_NAME);

    private static final String SCHEDULED_TRANSACTIONS_TABLE_CREATE = "CREATE TABLE " + ScheduledTransactionEntry.TABLE_NAME + " ("
            + ScheduledTransactionEntry._ID                             + " integer primary key autoincrement, "
            + ScheduledTransactionEntry.COLUMN_GUID                     + " varchar(255) not null UNIQUE, "
            + ScheduledTransactionEntry.COLUMN_NAME                     + " varchar(255) not null, "
            + ScheduledTransactionEntry.COLUMN_ENABLED                  + " tinyint default 1, " //enabled by default
            + ScheduledTransactionEntry.COLUMN_START_DATE               + " timestamp not null, "
            + ScheduledTransactionEntry.COLUMN_END_DATE                 + " timestamp default 0, "
            + ScheduledTransactionEntry.COLUMN_LAST_OCCURRENCE          + " timestamp, "
            + ScheduledTransactionEntry.COLUMN_NUM_OCCURRENCES          + " integer not null, "
            + ScheduledTransactionEntry.COLUMN_REMAINING_OCCURRENCES    + " integer not null, "
            + ScheduledTransactionEntry.COLUMN_AUTO_CREATE              + " tinyint default 1, "
            + ScheduledTransactionEntry.COLUMN_AUTO_NOTIFY              + " tinyint default 0, "
            + ScheduledTransactionEntry.COLUMN_ADVANCE_CREATION         + " tinyint default 0, "
            + ScheduledTransactionEntry.COLUMN_ADVANCE_NOTIFY           + " tinyint default 0, "
            + ScheduledTransactionEntry.COLUMN_INSTANCE_COUNT           + " integer not null, "
            + ScheduledTransactionEntry.COLUMN_TEMPLATE_ACCT_UID        + " varchar(255) not null, "
            + ScheduledTransactionEntry.COLUMN_CREATED_AT        + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + ScheduledTransactionEntry.COLUMN_MODIFIED_AT       + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + ");" + createUpdatedAtTrigger(ScheduledTransactionEntry.TABLE_NAME);

    private static final String SCHEDULED_EXPORTS_TABLE_CREATE = "CREATE TABLE " + ScheduledExportEntry.TABLE_NAME + " ("
            + ScheduledExportEntry._ID                      + " integer primary key autoincrement, "
            + ScheduledExportEntry.COLUMN_GUID              + " varchar(255) not null UNIQUE, "
            + ScheduledExportEntry.COLUMN_LAST_RUN_TIME     + " timestamp default 0, "
            + ScheduledExportEntry.COLUMN_START_TIME        + " timestamp not null, "
            + ScheduledExportEntry.COLUMN_END_TIME          + " timestamp default 0, "
            + ScheduledExportEntry.COLUMN_EXPORT_PARAMS     + " text, "
            + ScheduledExportEntry.COLUMN_ENABLED           + " tinyint default 1, " //enabled by default
            + ScheduledExportEntry.COLUMN_RECURRENCE_RULE   + " text not null, "
            + ScheduledExportEntry.COLUMN_EXECUTION_COUNT   + " integer default 0, "
            + ScheduledExportEntry.COLUMN_CREATED_AT        + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + ScheduledExportEntry.COLUMN_MODIFIED_AT       + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + ");" + createUpdatedAtTrigger(ScheduledExportEntry.TABLE_NAME);

    private static final String COMMODITIES_TABLE_CREATE = "CREATE TABLE " + DatabaseSchema.CommodityEntry.TABLE_NAME + " ("
            + CommodityEntry._ID                    + " integer primary key autoincrement, "
            + CommodityEntry.COLUMN_GUID            + " varchar(255) not null UNIQUE, "
            + CommodityEntry.COLUMN_NAMESPACE       + " varchar(255) not null default " + Commodity.Namespace.ISO4217.name() + ", "
            + CommodityEntry.COLUMN_FULLNAME        + " varchar(255) not null, "
            + CommodityEntry.COLUMN_MNEMONIC        + " varchar(255) not null, "
            + CommodityEntry.COLUMN_LOCAL_SYMBOL    + " varchar(255) not null default '', "
            + CommodityEntry.COLUMN_CUSIP           + " varchar(255), "
            + CommodityEntry.COLUMN_SMALLEST_FRACTION + " integer not null, "
            + CommodityEntry.COLUMN_QUOTE_FLAG      + " integer not null, "
            + CommodityEntry.COLUMN_QUOTE_SOURCE    + " text, "
            + CommodityEntry.COLUMN_QUOTE_TZ        + " text, "
            + CommodityEntry.COLUMN_CREATED_AT      + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + CommodityEntry.COLUMN_MODIFIED_AT     + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP "
            + ");" + createUpdatedAtTrigger(CommodityEntry.TABLE_NAME);

    /**
     * SQL statement to create the commodity prices table
     */
    private static final String PRICES_TABLE_CREATE = "CREATE TABLE " + PriceEntry.TABLE_NAME + " ("
            + PriceEntry._ID                    + " integer primary key autoincrement, "
            + PriceEntry.COLUMN_GUID + " varchar(255) not null UNIQUE, "
            + PriceEntry.COLUMN_COMMODITY_GUID  + " varchar(255) not null, "
            + PriceEntry.COLUMN_CURRENCY_GUID   + " varchar(255) not null, "
            + PriceEntry.COLUMN_TYPE            + " varchar(255), "
            + PriceEntry.COLUMN_DATE 	        + " TIMESTAMP not null, "
            + PriceEntry.COLUMN_SOURCE          + " text, "
            + PriceEntry.COLUMN_VALUE_NUM       + " integer not null, "
            + PriceEntry.COLUMN_VALUE_DENOM     + " integer not null, "
            + PriceEntry.COLUMN_CREATED_AT      + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + PriceEntry.COLUMN_MODIFIED_AT     + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + "UNIQUE (" + PriceEntry.COLUMN_COMMODITY_GUID + ", " + PriceEntry.COLUMN_CURRENCY_GUID + ") ON CONFLICT REPLACE, "
            + "FOREIGN KEY (" 	+ PriceEntry.COLUMN_COMMODITY_GUID + ") REFERENCES " + CommodityEntry.TABLE_NAME + " (" + CommodityEntry.COLUMN_GUID + ") ON DELETE CASCADE, "
            + "FOREIGN KEY (" 	+ PriceEntry.COLUMN_CURRENCY_GUID + ") REFERENCES " + CommodityEntry.TABLE_NAME + " (" + CommodityEntry.COLUMN_GUID + ") ON DELETE CASCADE "
            + ");" + createUpdatedAtTrigger(PriceEntry.TABLE_NAME);


    private static final String BUDGETS_TABLE_CREATE = "CREATE TABLE " + BudgetEntry.TABLE_NAME + " ("
            + BudgetEntry._ID                   + " integer primary key autoincrement, "
            + BudgetEntry.COLUMN_GUID           + " varchar(255) not null UNIQUE, "
            + BudgetEntry.COLUMN_NAME           + " varchar(255) not null, "
            + BudgetEntry.COLUMN_DESCRIPTION    + " varchar(255), "
            + BudgetEntry.COLUMN_NUM_PERIODS    + " integer, "
            + BudgetEntry.COLUMN_CREATED_AT     + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + BudgetEntry.COLUMN_MODIFIED_AT    + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + ");" + createUpdatedAtTrigger(BudgetEntry.TABLE_NAME);

    private static final String BUDGET_AMOUNTS_TABLE_CREATE = "CREATE TABLE " + BudgetAmountEntry.TABLE_NAME + " ("
            + BudgetAmountEntry._ID                     + " integer primary key autoincrement, "
            + BudgetAmountEntry.COLUMN_GUID             + " varchar(255) not null UNIQUE, "
            + BudgetAmountEntry.COLUMN_BUDGET_GUID      + " varchar(255) not null, "
            + BudgetAmountEntry.COLUMN_ACCOUNT_GUID     + " varchar(255) not null, "
            + BudgetAmountEntry.COLUMN_AMOUNT_NUM       + " integer not null, "
            + BudgetAmountEntry.COLUMN_AMOUNT_DENOM     + " integer not null, "
            + BudgetAmountEntry.COLUMN_PERIOD_NUM       + " integer not null, "
            + BudgetAmountEntry.COLUMN_CREATED_AT       + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + BudgetAmountEntry.COLUMN_MODIFIED_AT      + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY (" 	+ BudgetAmountEntry.COLUMN_ACCOUNT_GUID + ") REFERENCES " + AccountEntry.TABLE_NAME + " (" + AccountEntry.COLUMN_GUID + ") ON DELETE CASCADE, "
            + "FOREIGN KEY (" 	+ BudgetAmountEntry.COLUMN_BUDGET_GUID + ") REFERENCES " + BudgetEntry.TABLE_NAME + " (" + BudgetEntry.COLUMN_GUID + ") ON DELETE CASCADE "
            + ");" + createUpdatedAtTrigger(BudgetAmountEntry.TABLE_NAME);


    private static final String RECURRENCE_TABLE_CREATE = "CREATE TABLE " + RecurrenceEntry.TABLE_NAME + " ("
            + RecurrenceEntry._ID                   + " integer primary key autoincrement, "
            + RecurrenceEntry.COLUMN_GUID           + " varchar(255) not null UNIQUE, "
            + RecurrenceEntry.COLUMN_OBJECT_GUID    + " varchar(255) not null, "
            + RecurrenceEntry.COLUMN_MULTIPLIER     + " integer not null default 1, "
            + RecurrenceEntry.COLUMN_PERIOD_TYPE    + " varchar(255) not null, "
            + RecurrenceEntry.COLUMN_PERIOD_START   + " timestamp not null, "
            + RecurrenceEntry.COLUMN_CREATED_AT     + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + RecurrenceEntry.COLUMN_MODIFIED_AT    + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP); "
            + createUpdatedAtTrigger(RecurrenceEntry.TABLE_NAME);


    /**
	 * Constructor
	 * @param context Application context
     * @param databaseName Name of the database
	 */
	public DatabaseHelper(Context context, String databaseName){
		super(context, databaseName, null, DatabaseSchema.DATABASE_VERSION);

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
                + "  SET " + CommonColumns.COLUMN_MODIFIED_AT + " = CURRENT_TIMESTAMP"
                + "  WHERE OLD." + CommonColumns.COLUMN_GUID + " = NEW." + CommonColumns.COLUMN_GUID + ";"
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
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
		Log.i(LOG_TAG, "Upgrading database from version "
                + oldVersion + " to " + newVersion);

        Toast.makeText(GnuCashApplication.getAppContext(), "Upgrading GnuCash database", Toast.LENGTH_SHORT).show();
        /*
        * NOTE: In order to modify the database, create a new static method in the MigrationHelper class
        * called upgradeDbToVersion<#>, e.g. int upgradeDbToVersion10(SQLiteDatabase) in order to upgrade to version 10.
        * The upgrade method should return the new (upgraded) database version as the return value.
        * Then all you need to do is increment the DatabaseSchema.DATABASE_VERSION to the appropriate number to trigger an upgrade.
        */
		if (oldVersion > newVersion) {
            throw new IllegalArgumentException("Database downgrades are not supported at the moment");
        }

        while(oldVersion < newVersion){
            try {
                Method method = MigrationHelper.class.getDeclaredMethod("upgradeDbToVersion" + (oldVersion+1), SQLiteDatabase.class);
                Object result = method.invoke(null, db);
                oldVersion = Integer.parseInt(result.toString());

            } catch (NoSuchMethodException e) {
                String msg = String.format(Locale.US, "Database upgrade method upgradeToVersion%d(SQLiteDatabase) definition not found ", newVersion);
                Log.e(LOG_TAG, msg, e);
                Crashlytics.log(msg);
                Crashlytics.logException(e);
                throw new RuntimeException(e);
            }  catch (IllegalAccessException e) {
                String msg = String.format(Locale.US, "Database upgrade to version %d failed. The upgrade method is inaccessible ", newVersion);
                Log.e(LOG_TAG, msg, e);
                Crashlytics.log(msg);
                Crashlytics.logException(e);
                throw new RuntimeException(e);
            } catch (InvocationTargetException e){
                Crashlytics.logException(e.getTargetException());
                throw new RuntimeException(e.getTargetException());
            }
        }
	}


    /**
     * Creates the tables in the database and import default commodities into the database
     * @param db Database instance
     */
    private void createDatabaseTables(SQLiteDatabase db) {
        Log.i(LOG_TAG, "Creating database tables");
        db.execSQL(ACCOUNTS_TABLE_CREATE);
        db.execSQL(TRANSACTIONS_TABLE_CREATE);
        db.execSQL(SPLITS_TABLE_CREATE);
        db.execSQL(LOTS_TABLE_CREATE);
        db.execSQL(SCHEDULED_TRANSACTIONS_TABLE_CREATE);
        db.execSQL(SCHEDULED_EXPORTS_TABLE_CREATE);
        db.execSQL(COMMODITIES_TABLE_CREATE);
        db.execSQL(PRICES_TABLE_CREATE);
        db.execSQL(RECURRENCE_TABLE_CREATE);
        db.execSQL(BUDGETS_TABLE_CREATE);
        db.execSQL(BUDGET_AMOUNTS_TABLE_CREATE);


        String createAccountUidIndex = "CREATE UNIQUE INDEX '" + AccountEntry.INDEX_UID + "' ON "
                + AccountEntry.TABLE_NAME + "(" + AccountEntry.COLUMN_GUID + ")";

        String createTransactionUidIndex = "CREATE UNIQUE INDEX '" + TransactionEntry.INDEX_UID + "' ON "
                + TransactionEntry.TABLE_NAME + "(" + TransactionEntry.COLUMN_GUID + ")";

        String createSplitUidIndex = "CREATE UNIQUE INDEX '" + SplitEntry.INDEX_UID + "' ON "
                + SplitEntry.TABLE_NAME + "(" + SplitEntry.COLUMN_GUID + ")";

        String createScheduledEventUidIndex = "CREATE UNIQUE INDEX '" + ScheduledTransactionEntry.INDEX_UID
                + "' ON " + ScheduledTransactionEntry.TABLE_NAME + "(" + ScheduledTransactionEntry.COLUMN_GUID + ")";

        String createCommodityUidIndex = "CREATE UNIQUE INDEX '" + CommodityEntry.INDEX_UID
                + "' ON " + CommodityEntry.TABLE_NAME + "(" + CommodityEntry.COLUMN_GUID + ")";

        String createPriceUidIndex = "CREATE UNIQUE INDEX '" + PriceEntry.INDEX_UID
                + "' ON " + PriceEntry.TABLE_NAME + "(" + PriceEntry.COLUMN_GUID + ")";

        String createBudgetUidIndex = "CREATE UNIQUE INDEX '" + BudgetEntry.INDEX_UID
                + "' ON " + BudgetEntry.TABLE_NAME + "(" + BudgetEntry.COLUMN_GUID + ")";

        String createBudgetAmountUidIndex = "CREATE UNIQUE INDEX '" + BudgetAmountEntry.INDEX_UID
                + "' ON " + BudgetAmountEntry.TABLE_NAME + "(" + BudgetAmountEntry.COLUMN_GUID + ")";

        String createRecurrenceUidIndex = "CREATE UNIQUE INDEX '" + RecurrenceEntry.INDEX_UID
                + "' ON " + RecurrenceEntry.TABLE_NAME + "(" + RecurrenceEntry.COLUMN_GUID + ")";

        db.execSQL(createAccountUidIndex);
        db.execSQL(createTransactionUidIndex);
        db.execSQL(createSplitUidIndex);
        db.execSQL(createScheduledEventUidIndex);
        db.execSQL(createCommodityUidIndex);
        db.execSQL(createPriceUidIndex);
        db.execSQL(createBudgetUidIndex);
        db.execSQL(createRecurrenceUidIndex);
        db.execSQL(createBudgetAmountUidIndex);

        try {
            MigrationHelper.importCommodities(db);
        } catch (SAXException | ParserConfigurationException | IOException e) {
            Log.e(LOG_TAG, "Error loading currencies into the database");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
