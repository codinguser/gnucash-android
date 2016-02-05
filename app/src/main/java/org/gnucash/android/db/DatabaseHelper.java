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

import javax.xml.parsers.ParserConfigurationException;

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
	public static final String LOG_TAG = DatabaseHelper.class.getName();
	
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
            + AccountEntry.COLUMN_COMMODITY_UID     + " varchar(255) not null, "
			+ AccountEntry.COLUMN_DESCRIPTION       + " varchar(255), "
            + AccountEntry.COLUMN_COLOR_CODE        + " varchar(255), "
            + AccountEntry.COLUMN_FAVORITE 		    + " tinyint default 0, "
            + AccountEntry.COLUMN_HIDDEN 		    + " tinyint default 0, "
            + AccountEntry.COLUMN_FULL_NAME 	    + " varchar(255), "
            + AccountEntry.COLUMN_PLACEHOLDER           + " tinyint default 0, "
            + AccountEntry.COLUMN_PARENT_ACCOUNT_UID    + " varchar(255), "
            + AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID   + " varchar(255), "
            + AccountEntry.COLUMN_CREATED_AT       + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + AccountEntry.COLUMN_MODIFIED_AT      + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
//            + "FOREIGN KEY (" 	+ AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID + ") REFERENCES " + AccountEntry.TABLE_NAME + " (" + AccountEntry.COLUMN_UID + ") ON DELETE SET NULL, "
            + "FOREIGN KEY (" 	+ AccountEntry.COLUMN_COMMODITY_UID + ") REFERENCES " + CommodityEntry.TABLE_NAME + " (" + CommodityEntry.COLUMN_UID + ") "
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
            + TransactionEntry.COLUMN_COMMODITY_UID + " varchar(255) not null, "
            + TransactionEntry.COLUMN_SCHEDX_ACTION_UID + " varchar(255), "
            + TransactionEntry.COLUMN_CREATED_AT    + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + TransactionEntry.COLUMN_MODIFIED_AT   + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY (" 	+ TransactionEntry.COLUMN_SCHEDX_ACTION_UID + ") REFERENCES " + ScheduledActionEntry.TABLE_NAME + " (" + ScheduledActionEntry.COLUMN_UID + ") ON DELETE SET NULL, "
            + "FOREIGN KEY (" 	+ TransactionEntry.COLUMN_COMMODITY_UID + ") REFERENCES " + CommodityEntry.TABLE_NAME + " (" + CommodityEntry.COLUMN_UID + ") "
			+ ");" + createUpdatedAtTrigger(TransactionEntry.TABLE_NAME);

    /**
     * SQL statement to create the transaction splits table
     */
    private static final String SPLITS_TABLE_CREATE = "CREATE TABLE " + SplitEntry.TABLE_NAME + " ("
            + SplitEntry._ID                    + " integer primary key autoincrement, "
            + SplitEntry.COLUMN_UID             + " varchar(255) not null UNIQUE, "
            + SplitEntry.COLUMN_MEMO 	        + " text, "
            + SplitEntry.COLUMN_TYPE            + " varchar(255) not null, "
            + SplitEntry.COLUMN_VALUE_NUM       + " integer not null, "
            + SplitEntry.COLUMN_VALUE_DENOM     + " integer not null, "
            + SplitEntry.COLUMN_QUANTITY_NUM    + " integer not null, "
            + SplitEntry.COLUMN_QUANTITY_DENOM  + " integer not null, "
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
            + ScheduledActionEntry.COLUMN_ACTION_UID     + " varchar(255) not null, "
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

    public static final String COMMODITIES_TABLE_CREATE = "CREATE TABLE " + DatabaseSchema.CommodityEntry.TABLE_NAME + " ("
            + CommodityEntry._ID                + " integer primary key autoincrement, "
            + CommodityEntry.COLUMN_UID         + " varchar(255) not null UNIQUE, "
            + CommodityEntry.COLUMN_NAMESPACE   + " varchar(255) not null default " + Commodity.Namespace.ISO4217.name() + ", "
            + CommodityEntry.COLUMN_FULLNAME    + " varchar(255) not null, "
            + CommodityEntry.COLUMN_MNEMONIC    + " varchar(255) not null, "
            + CommodityEntry.COLUMN_LOCAL_SYMBOL+ " varchar(255) not null default '', "
            + CommodityEntry.COLUMN_CUSIP       + " varchar(255), "
            + CommodityEntry.COLUMN_SMALLEST_FRACTION + " integer not null, "
            + CommodityEntry.COLUMN_QUOTE_FLAG  + " integer not null, "
            + CommodityEntry.COLUMN_CREATED_AT  + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + CommodityEntry.COLUMN_MODIFIED_AT + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP "
            + ");" + createUpdatedAtTrigger(CommodityEntry.TABLE_NAME);

    /**
     * SQL statement to create the commodity prices table
     */
    private static final String PRICES_TABLE_CREATE = "CREATE TABLE " + PriceEntry.TABLE_NAME + " ("
            + PriceEntry._ID                    + " integer primary key autoincrement, "
            + PriceEntry.COLUMN_UID             + " varchar(255) not null UNIQUE, "
            + PriceEntry.COLUMN_COMMODITY_UID 	+ " varchar(255) not null, "
            + PriceEntry.COLUMN_CURRENCY_UID    + " varchar(255) not null, "
            + PriceEntry.COLUMN_TYPE            + " varchar(255), "
            + PriceEntry.COLUMN_DATE 	        + " TIMESTAMP not null, "
            + PriceEntry.COLUMN_SOURCE          + " text, "
            + PriceEntry.COLUMN_VALUE_NUM       + " integer not null, "
            + PriceEntry.COLUMN_VALUE_DENOM     + " integer not null, "
            + PriceEntry.COLUMN_CREATED_AT      + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + PriceEntry.COLUMN_MODIFIED_AT     + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + "UNIQUE (" + PriceEntry.COLUMN_COMMODITY_UID + ", " + PriceEntry.COLUMN_CURRENCY_UID + ") ON CONFLICT REPLACE, "
            + "FOREIGN KEY (" 	+ PriceEntry.COLUMN_COMMODITY_UID + ") REFERENCES " + CommodityEntry.TABLE_NAME + " (" + CommodityEntry.COLUMN_UID + ") ON DELETE CASCADE, "
            + "FOREIGN KEY (" 	+ PriceEntry.COLUMN_CURRENCY_UID + ") REFERENCES " + CommodityEntry.TABLE_NAME + " (" + CommodityEntry.COLUMN_UID + ") ON DELETE CASCADE "
            + ");" + createUpdatedAtTrigger(PriceEntry.TABLE_NAME);

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
                + "  SET " + CommonColumns.COLUMN_MODIFIED_AT + " = CURRENT_TIMESTAMP"
                + "  WHERE OLD." + CommonColumns.COLUMN_UID + " = NEW." + CommonColumns.COLUMN_UID + ";"
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
        * The upgrade method should return the upgraded database version as the return value.
        * Then all you need to do is increment the DatabaseSchema.DATABASE_VERSION to the appropriate number.
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
                String msg = String.format("Database upgrade method upgradeToVersion%d(SQLiteDatabase) definition not found ", newVersion);
                Log.e(LOG_TAG, msg, e);
                Crashlytics.log(msg);
                Crashlytics.logException(e);
                throw new RuntimeException(e);
            }  catch (IllegalAccessException e) {
                String msg = String.format("Database upgrade to version %d failed. The upgrade method is inaccessible ", newVersion);
                Log.e(LOG_TAG, msg, e);
                Crashlytics.log(msg);
                Crashlytics.logException(e);
                throw new RuntimeException(e);
            } catch (InvocationTargetException e){
                Crashlytics.logException(e);
                throw new RuntimeException(e);
            }
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
        db.execSQL(SCHEDULED_ACTIONS_TABLE_CREATE);
        db.execSQL(COMMODITIES_TABLE_CREATE);
        db.execSQL(PRICES_TABLE_CREATE);

        String createAccountUidIndex = "CREATE UNIQUE INDEX '" + AccountEntry.INDEX_UID + "' ON "
                + AccountEntry.TABLE_NAME + "(" + AccountEntry.COLUMN_UID + ")";

        String createTransactionUidIndex = "CREATE UNIQUE INDEX '" + TransactionEntry.INDEX_UID + "' ON "
                + TransactionEntry.TABLE_NAME + "(" + TransactionEntry.COLUMN_UID + ")";

        String createSplitUidIndex = "CREATE UNIQUE INDEX '" + SplitEntry.INDEX_UID + "' ON "
                + SplitEntry.TABLE_NAME + "(" + SplitEntry.COLUMN_UID + ")";

        String createScheduledEventUidIndex = "CREATE UNIQUE INDEX '" + ScheduledActionEntry.INDEX_UID
                + "' ON " + ScheduledActionEntry.TABLE_NAME + "(" + ScheduledActionEntry.COLUMN_UID + ")";

        String createCommodityUidIndex = "CREATE UNIQUE INDEX '" + CommodityEntry.INDEX_UID
                + "' ON " + CommodityEntry.TABLE_NAME + "(" + CommodityEntry.COLUMN_UID + ")";

        String createPriceUidIndex = "CREATE UNIQUE INDEX '" + PriceEntry.INDEX_UID
                + "' ON " + PriceEntry.TABLE_NAME + "(" + PriceEntry.COLUMN_UID + ")";

        db.execSQL(createAccountUidIndex);
        db.execSQL(createTransactionUidIndex);
        db.execSQL(createSplitUidIndex);
        db.execSQL(createScheduledEventUidIndex);
        db.execSQL(createCommodityUidIndex);
        db.execSQL(createPriceUidIndex);

        try {
            MigrationHelper.importCommodities(db);
        } catch (SAXException | ParserConfigurationException | IOException e) {
            Log.e(LOG_TAG, "Error loading currencies into the database");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
