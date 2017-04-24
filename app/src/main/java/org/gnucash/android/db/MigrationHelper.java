/*
 * Copyright (c) 2014 - 2015 Ngewi Fet <ngewif@gmail.com>
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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.importer.CommoditiesXmlHandler;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.BaseModel;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Recurrence;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.service.ScheduledActionService;
import org.gnucash.android.util.PreferencesHelper;
import org.gnucash.android.util.TimestampHelper;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.channels.FileChannel;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import static org.gnucash.android.db.DatabaseSchema.AccountEntry;
import static org.gnucash.android.db.DatabaseSchema.BudgetAmountEntry;
import static org.gnucash.android.db.DatabaseSchema.BudgetEntry;
import static org.gnucash.android.db.DatabaseSchema.CommodityEntry;
import static org.gnucash.android.db.DatabaseSchema.CommonColumns;
import static org.gnucash.android.db.DatabaseSchema.PriceEntry;
import static org.gnucash.android.db.DatabaseSchema.RecurrenceEntry;
import static org.gnucash.android.db.DatabaseSchema.ScheduledActionEntry;
import static org.gnucash.android.db.DatabaseSchema.SplitEntry;
import static org.gnucash.android.db.DatabaseSchema.TransactionEntry;

/**
 * Collection of helper methods which are used during database migrations
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
@SuppressWarnings("unused")
public class MigrationHelper {
    public static final String LOG_TAG = "MigrationHelper";

    /**
     * Performs same function as {@link AccountsDbAdapter#getFullyQualifiedAccountName(String)}
     * <p>This method is only necessary because we cannot open the database again (by instantiating {@link AccountsDbAdapter}
     * while it is locked for upgrades. So we re-implement the method here.</p>
     * @param db SQLite database
     * @param accountUID Unique ID of account whose fully qualified name is to be determined
     * @return Fully qualified (colon-separated) account name
     * @see AccountsDbAdapter#getFullyQualifiedAccountName(String)
     */
    static String getFullyQualifiedAccountName(SQLiteDatabase db, String accountUID){
        //get the parent account UID of the account
        Cursor cursor = db.query(AccountEntry.TABLE_NAME,
                new String[] {AccountEntry.COLUMN_PARENT_ACCOUNT_UID},
                AccountEntry.COLUMN_UID + " = ?",
                new String[]{accountUID},
                null, null, null, null);

        String parentAccountUID = null;
        if (cursor != null && cursor.moveToFirst()){
            parentAccountUID = cursor.getString(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_PARENT_ACCOUNT_UID));
            cursor.close();
        }

        //get the name of the account
        cursor = db.query(AccountEntry.TABLE_NAME,
                new String[]{AccountEntry.COLUMN_NAME},
                AccountEntry.COLUMN_UID + " = ?",
                new String[]{accountUID}, null, null, null);

        String accountName = null;
        if (cursor != null && cursor.moveToFirst()){
            accountName = cursor.getString(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_NAME));
            cursor.close();
        }

        String gnucashRootAccountUID = getGnuCashRootAccountUID(db);
        if (parentAccountUID == null || accountName == null
            || parentAccountUID.equalsIgnoreCase(gnucashRootAccountUID)){
            return accountName;
        }

        String parentAccountName = getFullyQualifiedAccountName(db, parentAccountUID);

        return parentAccountName + AccountsDbAdapter.ACCOUNT_NAME_SEPARATOR + accountName;
    }

    /**
     * Returns the GnuCash ROOT account UID.
     * <p>In GnuCash desktop account structure, there is a root account (which is not visible in the UI) from which
     * other top level accounts derive. GnuCash Android does not have this ROOT account by default unless the account
     * structure was imported from GnuCash for desktop. Hence this method also returns <code>null</code> as an
     * acceptable result.</p>
     * <p><b>Note:</b> NULL is an acceptable response, be sure to check for it</p>
     * @return Unique ID of the GnuCash root account.
     */
    private static String getGnuCashRootAccountUID(SQLiteDatabase db){
        String condition = AccountEntry.COLUMN_TYPE + "= '" + AccountType.ROOT.name() + "'";
        Cursor cursor =  db.query(AccountEntry.TABLE_NAME,
                null, condition, null, null, null,
                AccountEntry.COLUMN_NAME + " ASC");
        String rootUID = null;
        if (cursor != null && cursor.moveToFirst()){
            rootUID = cursor.getString(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_UID));
            cursor.close();
        }
        return rootUID;
    }

    /**
     * Copies the contents of the file in {@code src} to {@code dst} and then deletes the {@code src} if copy was successful.
     * If the file copy was unsuccessful, the src file will not be deleted.
     * @param src Source file
     * @param dst Destination file
     * @throws IOException if an error occurred during the file copy
     */
    static void moveFile(File src, File dst) throws IOException {
        Log.d(LOG_TAG, String.format(Locale.US, "Moving %s from %s to %s",
                src.getName(), src.getParent(), dst.getParent()));
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
            long bytesCopied = inChannel.transferTo(0, inChannel.size(), outChannel);
            if(bytesCopied >= src.length()) {
                boolean result = src.delete();
                String msg = result ? "Deleted src file: " : "Could not delete src: ";
                Log.d(LOG_TAG, msg + src.getPath());
            }
        } finally {
            if (inChannel != null)
                inChannel.close();
            outChannel.close();
        }
    }

    /**
     * Runnable which moves all exported files (exports and backups) from the old SD card location which
     * was generic to the new folder structure which uses the application ID as folder name.
     * <p>The new folder structure also futher enables parallel installation of multiple flavours of
     * the program (like development and production) on the same device.</p>
     */
    static final Runnable moveExportedFilesToNewDefaultLocation = new Runnable() {
        @Override
        public void run() {
            File oldExportFolder = new File(Environment.getExternalStorageDirectory() + "/gnucash");
            if (oldExportFolder.exists()){
                for (File src : oldExportFolder.listFiles()) {
                    if (src.isDirectory())
                        continue;
                    File dst = new File(Exporter.LEGACY_BASE_FOLDER_PATH + "/exports/" + src.getName());
                    try {
                        MigrationHelper.moveFile(src, dst);
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error migrating " + src.getName());
                        Crashlytics.logException(e);
                    }
                }
            } else {
                //if the base folder does not exist, no point going one level deeper
                return;
            }

            File oldBackupFolder = new File(oldExportFolder, "backup");
            if (oldBackupFolder.exists()){
                for (File src : new File(oldExportFolder, "backup").listFiles()) {
                    File dst = new File(Exporter.LEGACY_BASE_FOLDER_PATH + "/backups/" + src.getName());
                    try {
                        MigrationHelper.moveFile(src, dst);
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error migrating backup: " + src.getName());
                        Crashlytics.logException(e);
                    }
                }
            }

            if (oldBackupFolder.delete())
                oldExportFolder.delete();
        }
    };

    /**
     * Imports commodities into the database from XML resource file
     */
    static void importCommodities(SQLiteDatabase db) throws SAXException, ParserConfigurationException, IOException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp = spf.newSAXParser();
        XMLReader xr = sp.getXMLReader();

        InputStream commoditiesInputStream = GnuCashApplication.getAppContext().getResources()
                .openRawResource(R.raw.iso_4217_currencies);
        BufferedInputStream bos = new BufferedInputStream(commoditiesInputStream);

        /** Create handler to handle XML Tags ( extends DefaultHandler ) */

        CommoditiesXmlHandler handler = new CommoditiesXmlHandler(db);

        xr.setContentHandler(handler);
        xr.parse(new InputSource(bos));
    }


    /**
     * Upgrades the database from version 1 to 2
     * @param db SQLiteDatabase
     * @return Version number: 2 if upgrade successful, 1 otherwise
     */
    public static int upgradeDbToVersion2(SQLiteDatabase db) {
        int oldVersion;
        String addColumnSql = "ALTER TABLE " + TransactionEntry.TABLE_NAME +
                            " ADD COLUMN double_account_uid varchar(255)";

        //introducing sub accounts
        Log.i(DatabaseHelper.LOG_TAG, "Adding column for parent accounts");
        String addParentAccountSql = "ALTER TABLE " + AccountEntry.TABLE_NAME +
                " ADD COLUMN " + AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " varchar(255)";

        db.execSQL(addColumnSql);
        db.execSQL(addParentAccountSql);

        //update account types to GnuCash account types
        //since all were previously CHECKING, now all will be CASH
        Log.i(DatabaseHelper.LOG_TAG, "Converting account types to GnuCash compatible types");
        ContentValues cv = new ContentValues();
        cv.put(SplitEntry.COLUMN_TYPE, AccountType.CASH.toString());
        db.update(AccountEntry.TABLE_NAME, cv, null, null);

        oldVersion = 2;
        return oldVersion;
    }

    /**
     * Upgrades the database from version 2 to 3
     * @param db SQLiteDatabase to upgrade
     * @return Version number: 3 if upgrade successful, 2 otherwise
     */
    static int upgradeDbToVersion3(SQLiteDatabase db) {
        int oldVersion;
        String addPlaceHolderAccountFlagSql = "ALTER TABLE " + AccountEntry.TABLE_NAME +
                " ADD COLUMN " + AccountEntry.COLUMN_PLACEHOLDER + " tinyint default 0";

        db.execSQL(addPlaceHolderAccountFlagSql);
        oldVersion = 3;
        return oldVersion;
    }

    /**
     * Upgrades the database from version 3 to 4
     * @param db SQLiteDatabase
     * @return Version number: 4 if upgrade successful, 3 otherwise
     */
    static int upgradeDbToVersion4(SQLiteDatabase db) {
        int oldVersion;
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
        return oldVersion;
    }

    /**
     * Upgrades the database from version 4 to 5
     * <p>Adds favorites column to accounts</p>
     * @param db SQLiteDatabase
     * @return Version number: 5 if upgrade successful, 4 otherwise
     */
    static int upgradeDbToVersion5(SQLiteDatabase db) {
        int oldVersion;
        String addAccountFavorite = " ALTER TABLE " + AccountEntry.TABLE_NAME
                + " ADD COLUMN " + AccountEntry.COLUMN_FAVORITE + " tinyint default 0";
        db.execSQL(addAccountFavorite);

        oldVersion = 5;
        return oldVersion;
    }

    /**
     * Upgrades the database from version 5 to version 6.<br>
     * This migration adds support for fully qualified account names and updates existing accounts.
     * @param db SQLite Database to be upgraded
     * @return New database version (6) if upgrade successful, old version (5) if unsuccessful
     */
    static int upgradeDbToVersion6(SQLiteDatabase db) {
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
            String fullName = getFullyQualifiedAccountName(db, uid);

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
     * Code for upgrading the database to version 7 from version 6.<br>
     * Tasks accomplished in migration:
     *  <ul>
     *      <li>Added new splits table for transaction splits</li>
     *      <li>Extract existing info from transactions table to populate split table</li>
     *  </ul>
     * @param db SQLite Database
     * @return The new database version if upgrade was successful, or the old db version if it failed
     */
    static int upgradeDbToVersion7(SQLiteDatabase db) {
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
                    + "amount" + " varchar(255) not null, "
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
                            + "amount" + " , "
                            + SplitEntry.COLUMN_ACCOUNT_UID + " , "
                            + SplitEntry.COLUMN_TRANSACTION_UID + " ) SELECT "
                            + "LOWER(HEX(RANDOMBLOB(16))) , "
                            + "CASE WHEN " + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_TYPE + " IN ( 'CASH' , 'BANK', 'ASSET', 'EXPENSE', 'RECEIVABLE', 'STOCK', 'MUTUAL' ) THEN CASE WHEN "
                            + "amount" + " < 0 THEN 'CREDIT' ELSE 'DEBIT' END ELSE CASE WHEN "
                            + "amount" + " < 0 THEN 'DEBIT' ELSE 'CREDIT' END END , "
                            + "ABS ( " + TransactionEntry.TABLE_NAME + "_bak.amount ) , "
                            + TransactionEntry.TABLE_NAME + "_bak.account_uid , "
                            + TransactionEntry.TABLE_NAME + "_bak." + TransactionEntry.COLUMN_UID
                            + " FROM " + TransactionEntry.TABLE_NAME + "_bak , " + AccountEntry.TABLE_NAME
                            + " ON " + TransactionEntry.TABLE_NAME + "_bak.account_uid = " + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_UID
                            + " UNION SELECT "
                            + "LOWER(HEX(RANDOMBLOB(16))) AS " + SplitEntry.COLUMN_UID + " , "
                            + "CASE WHEN " + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_TYPE + " IN ( 'CASH' , 'BANK', 'ASSET', 'EXPENSE', 'RECEIVABLE', 'STOCK', 'MUTUAL' ) THEN CASE WHEN "
                            + "amount" + " < 0 THEN 'DEBIT' ELSE 'CREDIT' END ELSE CASE WHEN "
                            + "amount" + " < 0 THEN 'CREDIT' ELSE 'DEBIT' END END , "
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
            oldVersion = 7;
        } finally {
            db.endTransaction();
        }
        return oldVersion;
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
    static int upgradeDbToVersion8(SQLiteDatabase db) {
        Log.i(DatabaseHelper.LOG_TAG, "Upgrading database to version 8");
        int oldVersion = 7;
        new File(Exporter.LEGACY_BASE_FOLDER_PATH + "/backups/").mkdirs();
        new File(Exporter.LEGACY_BASE_FOLDER_PATH + "/exports/").mkdirs();
        //start moving the files in background thread before we do the database stuff
        new Thread(moveExportedFilesToNewDefaultLocation).start();

        db.beginTransaction();
        try {

            Log.i(DatabaseHelper.LOG_TAG, "Creating scheduled actions table");
            db.execSQL("CREATE TABLE " + ScheduledActionEntry.TABLE_NAME + " ("
                    + ScheduledActionEntry._ID                   + " integer primary key autoincrement, "
                    + ScheduledActionEntry.COLUMN_UID            + " varchar(255) not null UNIQUE, "
                    + ScheduledActionEntry.COLUMN_ACTION_UID    + " varchar(255) not null, "
                    + ScheduledActionEntry.COLUMN_TYPE           + " varchar(255) not null, "
                    + "period "                                 + " integer not null, "
                    + ScheduledActionEntry.COLUMN_LAST_RUN       + " integer default 0, "
                    + ScheduledActionEntry.COLUMN_START_TIME     + " integer not null, "
                    + ScheduledActionEntry.COLUMN_END_TIME       + " integer default 0, "
                    + ScheduledActionEntry.COLUMN_TAG            + " text, "
                    + ScheduledActionEntry.COLUMN_ENABLED        + " tinyint default 1, " //enabled by default
                    + ScheduledActionEntry.COLUMN_TOTAL_FREQUENCY + " integer default 0, "
                    + ScheduledActionEntry.COLUMN_EXECUTION_COUNT+ " integer default 0, "
                    + ScheduledActionEntry.COLUMN_CREATED_AT     + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + ScheduledActionEntry.COLUMN_MODIFIED_AT    + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP "
                    + ");" + DatabaseHelper.createUpdatedAtTrigger(ScheduledActionEntry.TABLE_NAME));


            //==============================BEGIN TABLE MIGRATIONS ========================================
            Log.i(DatabaseHelper.LOG_TAG, "Migrating accounts table");
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
                    + ");" + DatabaseHelper.createUpdatedAtTrigger(AccountEntry.TABLE_NAME));

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

            Log.i(DatabaseHelper.LOG_TAG, "Migrating transactions table");
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
                    + ");" + DatabaseHelper.createUpdatedAtTrigger(TransactionEntry.TABLE_NAME));

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

            Log.i(DatabaseHelper.LOG_TAG, "Migrating splits table");
            // backup split table
            db.execSQL("ALTER TABLE " + SplitEntry.TABLE_NAME + " RENAME TO " + SplitEntry.TABLE_NAME + "_bak");
            // create new split table
            db.execSQL("CREATE TABLE " + SplitEntry.TABLE_NAME + " ("
                    + SplitEntry._ID + " integer primary key autoincrement, "
                    + SplitEntry.COLUMN_UID + " varchar(255) not null UNIQUE, "
                    + SplitEntry.COLUMN_MEMO + " text, "
                    + SplitEntry.COLUMN_TYPE + " varchar(255) not null, "
                    + "amount" + " varchar(255) not null, "
                    + SplitEntry.COLUMN_ACCOUNT_UID + " varchar(255) not null, "
                    + SplitEntry.COLUMN_TRANSACTION_UID + " varchar(255) not null, "
                    + SplitEntry.COLUMN_CREATED_AT + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + SplitEntry.COLUMN_MODIFIED_AT + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (" + SplitEntry.COLUMN_ACCOUNT_UID + ") REFERENCES " + AccountEntry.TABLE_NAME + " (" + AccountEntry.COLUMN_UID + ") ON DELETE CASCADE, "
                    + "FOREIGN KEY (" + SplitEntry.COLUMN_TRANSACTION_UID + ") REFERENCES " + TransactionEntry.TABLE_NAME + " (" + TransactionEntry.COLUMN_UID + ") ON DELETE CASCADE "
                    + ");" + DatabaseHelper.createUpdatedAtTrigger(SplitEntry.TABLE_NAME));

            // initialize new split table with data from old table
            db.execSQL("INSERT INTO " + SplitEntry.TABLE_NAME + " ( "
                            + SplitEntry._ID + " , "
                            + SplitEntry.COLUMN_UID + " , "
                            + SplitEntry.COLUMN_MEMO + " , "
                            + SplitEntry.COLUMN_TYPE + " , "
                            + "amount" + " , "
                            + SplitEntry.COLUMN_ACCOUNT_UID + " , "
                            + SplitEntry.COLUMN_TRANSACTION_UID
                            + ")  SELECT "
                            + SplitEntry.TABLE_NAME + "_bak." + SplitEntry._ID + " , "
                            + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_UID + " , "
                            + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_MEMO + " , "
                            + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_TYPE + " , "
                            + SplitEntry.TABLE_NAME + "_bak." + "amount" + " , "
                            + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_ACCOUNT_UID + " , "
                            + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_TRANSACTION_UID
                            + " FROM " + SplitEntry.TABLE_NAME + "_bak;"
            );



            //================================ END TABLE MIGRATIONS ================================

            // String timestamp to be used for all new created entities in migration
            String timestamp = TimestampHelper.getUtcStringFromTimestamp(TimestampHelper.getTimestampFromNow());

            //ScheduledActionDbAdapter scheduledActionDbAdapter = new ScheduledActionDbAdapter(db);
            //SplitsDbAdapter splitsDbAdapter = new SplitsDbAdapter(db);
            //TransactionsDbAdapter transactionsDbAdapter = new TransactionsDbAdapter(db, splitsDbAdapter);
            //AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(db,transactionsDbAdapter);

            Log.i(DatabaseHelper.LOG_TAG, "Creating default root account if none exists");
            ContentValues contentValues = new ContentValues();
            //assign a root account to all accounts which had null as parent except ROOT (top-level accounts)
            String rootAccountUID;
            Cursor cursor = db.query(AccountEntry.TABLE_NAME,
                    new String[]{AccountEntry.COLUMN_UID},
                    AccountEntry.COLUMN_TYPE + "= ?",
                    new String[]{AccountType.ROOT.name()}, null, null, null);
            try {
                if (cursor.moveToFirst()) {
                    rootAccountUID = cursor.getString(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_UID));
                }
                else
                {
                    rootAccountUID = BaseModel.generateUID();
                    contentValues.clear();
                    contentValues.put(CommonColumns.COLUMN_UID, rootAccountUID);
                    contentValues.put(CommonColumns.COLUMN_CREATED_AT, timestamp);
                    contentValues.put(AccountEntry.COLUMN_NAME,         "ROOT");
                    contentValues.put(AccountEntry.COLUMN_TYPE,         "ROOT");
                    contentValues.put(AccountEntry.COLUMN_CURRENCY,     Money.DEFAULT_CURRENCY_CODE);
                    contentValues.put(AccountEntry.COLUMN_PLACEHOLDER,  0);
                    contentValues.put(AccountEntry.COLUMN_HIDDEN,       1);
                    contentValues.putNull(AccountEntry.COLUMN_COLOR_CODE);
                    contentValues.put(AccountEntry.COLUMN_FAVORITE, 0);
                    contentValues.put(AccountEntry.COLUMN_FULL_NAME,    " ");
                    contentValues.putNull(AccountEntry.COLUMN_PARENT_ACCOUNT_UID);
                    contentValues.putNull(AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID);
                    db.insert(AccountEntry.TABLE_NAME, null, contentValues);
                }
            } finally {
                cursor.close();
            }
            //String rootAccountUID = accountsDbAdapter.getOrCreateGnuCashRootAccountUID();
            contentValues.clear();
            contentValues.put(AccountEntry.COLUMN_PARENT_ACCOUNT_UID, rootAccountUID);
            db.update(AccountEntry.TABLE_NAME, contentValues, AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " IS NULL AND " + AccountEntry.COLUMN_TYPE + " != ?", new String[]{"ROOT"});

            Log.i(DatabaseHelper.LOG_TAG, "Migrating existing recurring transactions");
            cursor = db.query(TransactionEntry.TABLE_NAME + "_bak", null, "recurrence_period > 0", null, null, null, null);
            long lastRun = System.currentTimeMillis();
            while (cursor.moveToNext()){
                contentValues.clear();
                Timestamp timestampT = new Timestamp(cursor.getLong(cursor.getColumnIndexOrThrow(TransactionEntry.COLUMN_TIMESTAMP)));
                contentValues.put(TransactionEntry.COLUMN_CREATED_AT, TimestampHelper.getUtcStringFromTimestamp(timestampT));
                long transactionId = cursor.getLong(cursor.getColumnIndexOrThrow(TransactionEntry._ID));
                db.update(TransactionEntry.TABLE_NAME, contentValues, TransactionEntry._ID + "=" + transactionId, null);

                //ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
                //scheduledAction.setActionUID(cursor.getString(cursor.getColumnIndexOrThrow(TransactionEntry.COLUMN_UID)));
                //long period = cursor.getLong(cursor.getColumnIndexOrThrow("recurrence_period"));
                //scheduledAction.setPeriod(period);
                //scheduledAction.setStartTime(timestampT.getTime()); //the start time is when the transaction was created
                //scheduledAction.setLastRun(System.currentTimeMillis()); //prevent this from being executed at the end of migration

                contentValues.clear();
                contentValues.put(CommonColumns.COLUMN_UID, BaseModel.generateUID());
                contentValues.put(CommonColumns.COLUMN_CREATED_AT, timestamp);
                contentValues.put(ScheduledActionEntry.COLUMN_ACTION_UID, cursor.getString(cursor.getColumnIndexOrThrow(TransactionEntry.COLUMN_UID)));
                contentValues.put("period", cursor.getLong(cursor.getColumnIndexOrThrow("recurrence_period")));
                contentValues.put(ScheduledActionEntry.COLUMN_START_TIME, timestampT.getTime());
                contentValues.put(ScheduledActionEntry.COLUMN_END_TIME, 0);
                contentValues.put(ScheduledActionEntry.COLUMN_LAST_RUN, lastRun);
                contentValues.put(ScheduledActionEntry.COLUMN_TYPE, "TRANSACTION");
                contentValues.put(ScheduledActionEntry.COLUMN_TAG, "");
                contentValues.put(ScheduledActionEntry.COLUMN_ENABLED, 1);
                contentValues.put(ScheduledActionEntry.COLUMN_TOTAL_FREQUENCY, 0);
                contentValues.put(ScheduledActionEntry.COLUMN_EXECUTION_COUNT, 0);
                //scheduledActionDbAdapter.addRecord(scheduledAction);
                db.insert(ScheduledActionEntry.TABLE_NAME, null, contentValues);

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
            Log.i(DatabaseHelper.LOG_TAG, "Auto-balancing existing transaction splits");
            cursor = db.query(
                    TransactionEntry.TABLE_NAME + " , " + SplitEntry.TABLE_NAME + " ON "
                            + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID + "=" + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_UID
                            + " , " + AccountEntry.TABLE_NAME + " ON "
                            + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_ACCOUNT_UID + "=" + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_UID,
                    new String[]{
                            TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID + " AS trans_uid",
                            TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_CURRENCY + " AS trans_currency",
                            "TOTAL ( CASE WHEN " +
                                    SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TYPE + " = 'DEBIT' THEN " +
                                    SplitEntry.TABLE_NAME + "." + "amount" + " ELSE - " +
                                    SplitEntry.TABLE_NAME + "." + "amount" + " END ) AS trans_acct_balance",
                            "COUNT ( DISTINCT " +
                                    AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_CURRENCY +
                                    " ) AS trans_currency_count"
                    },
                    TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TEMPLATE + " == 0",
                    null,
                    TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID,
                    "trans_acct_balance != 0 AND trans_currency_count = 1",
                    null);
            try {
                while (cursor.moveToNext()){
                    double imbalance = cursor.getDouble(cursor.getColumnIndexOrThrow("trans_acct_balance"));
                    BigDecimal decimalImbalance = BigDecimal.valueOf(imbalance).setScale(2, BigDecimal.ROUND_HALF_UP);
                    if (decimalImbalance.compareTo(BigDecimal.ZERO) != 0) {
                        String currencyCode = cursor.getString(cursor.getColumnIndexOrThrow("trans_currency"));
                        String imbalanceAccountName = GnuCashApplication.getAppContext().getString(R.string.imbalance_account_name) + "-" + currencyCode;
                        String imbalanceAccountUID;
                        Cursor c = db.query(AccountEntry.TABLE_NAME, new String[]{AccountEntry.COLUMN_UID},
                                AccountEntry.COLUMN_FULL_NAME + "= ?", new String[]{imbalanceAccountName},
                                null, null, null);
                        try {
                            if (c.moveToFirst()) {
                                imbalanceAccountUID = c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_UID));
                            }
                            else {
                                imbalanceAccountUID = BaseModel.generateUID();
                                contentValues.clear();
                                contentValues.put(CommonColumns.COLUMN_UID, imbalanceAccountUID);
                                contentValues.put(CommonColumns.COLUMN_CREATED_AT, timestamp);
                                contentValues.put(AccountEntry.COLUMN_NAME,         imbalanceAccountName);
                                contentValues.put(AccountEntry.COLUMN_TYPE,         "BANK");
                                contentValues.put(AccountEntry.COLUMN_CURRENCY,     currencyCode);
                                contentValues.put(AccountEntry.COLUMN_PLACEHOLDER,  0);
                                contentValues.put(AccountEntry.COLUMN_HIDDEN,       GnuCashApplication.isDoubleEntryEnabled() ? 0 : 1);
                                contentValues.putNull(AccountEntry.COLUMN_COLOR_CODE);
                                contentValues.put(AccountEntry.COLUMN_FAVORITE, 0);
                                contentValues.put(AccountEntry.COLUMN_FULL_NAME,    imbalanceAccountName);
                                contentValues.put(AccountEntry.COLUMN_PARENT_ACCOUNT_UID, rootAccountUID);
                                contentValues.putNull(AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID);
                                db.insert(AccountEntry.TABLE_NAME, null, contentValues);
                            }
                        } finally {
                            c.close();
                        }
                        String TransactionUID = cursor.getString(cursor.getColumnIndexOrThrow("trans_uid"));
                        contentValues.clear();
                        contentValues.put(CommonColumns.COLUMN_UID, BaseModel.generateUID());
                        contentValues.put(CommonColumns.COLUMN_CREATED_AT, timestamp);
                        contentValues.put("amount",     decimalImbalance.abs().toPlainString());
                        contentValues.put(SplitEntry.COLUMN_TYPE,       decimalImbalance.compareTo(BigDecimal.ZERO) < 0 ? "DEBIT" : "CREDIT");
                        contentValues.put(SplitEntry.COLUMN_MEMO,       "");
                        contentValues.put(SplitEntry.COLUMN_ACCOUNT_UID, imbalanceAccountUID);
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

            Log.i(DatabaseHelper.LOG_TAG, "Dropping temporary migration tables");
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
     * Upgrades the database from version 8 to version 9.
     * <p>This migration accomplishes the following:
     *  <ul>
     *      <li>Adds a commodities table to the database</li>
     *      <li>Adds prices table to the database</li>
     *      <li>Add separate columns for split value and quantity</li>
     *      <li>Migrate amounts to use the correct denominations for the currency</li>
     *  </ul>
     * </p>
     * @param db SQLite Database to be upgraded
     * @return New database version (9) if upgrade successful, old version (8) if unsuccessful
     * @throws RuntimeException if the default commodities could not be imported
     */
    static int upgradeDbToVersion9(SQLiteDatabase db){
        Log.i(DatabaseHelper.LOG_TAG, "Upgrading database to version 9");
        int oldVersion = 8;

        db.beginTransaction();
        try {
            db.execSQL("CREATE TABLE " + CommodityEntry.TABLE_NAME + " ("
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
                    + ");" + DatabaseHelper.createUpdatedAtTrigger(CommodityEntry.TABLE_NAME));
            db.execSQL("CREATE UNIQUE INDEX '" + CommodityEntry.INDEX_UID
                    + "' ON " + CommodityEntry.TABLE_NAME + "(" + CommodityEntry.COLUMN_UID + ")");

            try {
                importCommodities(db);
            } catch (SAXException | ParserConfigurationException | IOException e) {
                Log.e(DatabaseHelper.LOG_TAG, "Error loading currencies into the database", e);
                Crashlytics.logException(e);
                throw new RuntimeException(e);
            }

            db.execSQL(" ALTER TABLE " + AccountEntry.TABLE_NAME
                    + " ADD COLUMN " + AccountEntry.COLUMN_COMMODITY_UID + " varchar(255) "
                    + " REFERENCES " + CommodityEntry.TABLE_NAME + " (" + CommodityEntry.COLUMN_UID + ") ");

            db.execSQL(" ALTER TABLE " + TransactionEntry.TABLE_NAME
                    + " ADD COLUMN " + TransactionEntry.COLUMN_COMMODITY_UID + " varchar(255) "
                    + " REFERENCES " + CommodityEntry.TABLE_NAME + " (" + CommodityEntry.COLUMN_UID + ") ");

            db.execSQL("UPDATE " + AccountEntry.TABLE_NAME + " SET " + AccountEntry.COLUMN_COMMODITY_UID + " = "
                    + " (SELECT " + CommodityEntry.COLUMN_UID
                    + " FROM " + CommodityEntry.TABLE_NAME
                    + " WHERE " + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_COMMODITY_UID + " = " + CommodityEntry.TABLE_NAME + "." + CommodityEntry.COLUMN_UID
                    + ")");

            db.execSQL("UPDATE " + TransactionEntry.TABLE_NAME + " SET " + TransactionEntry.COLUMN_COMMODITY_UID + " = "
                    + " (SELECT " + CommodityEntry.COLUMN_UID
                    + " FROM " + CommodityEntry.TABLE_NAME
                    + " WHERE " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_COMMODITY_UID + " = " + CommodityEntry.TABLE_NAME + "." + CommodityEntry.COLUMN_UID
                    + ")");

            db.execSQL("CREATE TABLE " + PriceEntry.TABLE_NAME + " ("
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
                    + ");" + DatabaseHelper.createUpdatedAtTrigger(PriceEntry.TABLE_NAME));
            db.execSQL("CREATE UNIQUE INDEX '" + PriceEntry.INDEX_UID
                    + "' ON " + PriceEntry.TABLE_NAME + "(" + PriceEntry.COLUMN_UID + ")");


            //store split amounts as integer components numerator and denominator

            db.execSQL("ALTER TABLE " + SplitEntry.TABLE_NAME + " RENAME TO " + SplitEntry.TABLE_NAME + "_bak");
            // create new split table
            db.execSQL("CREATE TABLE " + SplitEntry.TABLE_NAME + " ("
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
                    + ");" + DatabaseHelper.createUpdatedAtTrigger(SplitEntry.TABLE_NAME));

            // initialize new split table with data from old table
            db.execSQL("INSERT INTO " + SplitEntry.TABLE_NAME + " ( "
                    + SplitEntry._ID                    + " , "
                    + SplitEntry.COLUMN_UID             + " , "
                    + SplitEntry.COLUMN_MEMO            + " , "
                    + SplitEntry.COLUMN_TYPE            + " , "
                    + SplitEntry.COLUMN_VALUE_NUM       + " , "
                    + SplitEntry.COLUMN_VALUE_DENOM     + " , "
                    + SplitEntry.COLUMN_QUANTITY_NUM    + " , "
                    + SplitEntry.COLUMN_QUANTITY_DENOM  + " , "
                    + SplitEntry.COLUMN_ACCOUNT_UID     + " , "
                    + SplitEntry.COLUMN_TRANSACTION_UID
                    + ")  SELECT "
                    + SplitEntry.TABLE_NAME + "_bak." + SplitEntry._ID + " , "
                    + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_UID + " , "
                    + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_MEMO + " , "
                    + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_TYPE + " , "
                    + SplitEntry.TABLE_NAME + "_bak.amount * 100, " //we will update this value in the next steps
                    + "100, "
                    + SplitEntry.TABLE_NAME + "_bak.amount * 100, " //default units of 2 decimal places were assumed until now
                    + "100, "
                    + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_ACCOUNT_UID + " , "
                    + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_TRANSACTION_UID
                    + " FROM " + SplitEntry.TABLE_NAME + "_bak;");


            //************** UPDATE SPLITS WHOSE CURRENCIES HAVE NO DECIMAL PLACES *****************
            //get all account UIDs which have currencies with fraction digits of 0
            String query = "SELECT " + "A." + AccountEntry.COLUMN_UID + " AS account_uid "
                    + " FROM " + AccountEntry.TABLE_NAME + " AS A, " + CommodityEntry.TABLE_NAME + " AS C "
                    + " WHERE A." + AccountEntry.COLUMN_CURRENCY + " = C." + CommodityEntry.COLUMN_MNEMONIC
                    + " AND C." + CommodityEntry.COLUMN_SMALLEST_FRACTION + "= 1";

            Cursor cursor = db.rawQuery(query, null);

            List<String> accountUIDs = new ArrayList<>();
            try {
                while (cursor.moveToNext()) {
                    String accountUID = cursor.getString(cursor.getColumnIndexOrThrow("account_uid"));
                    accountUIDs.add(accountUID);
                }
            } finally {
                cursor.close();
            }

            String accounts = TextUtils.join("' , '", accountUIDs);
            db.execSQL("REPLACE INTO " + SplitEntry.TABLE_NAME + " ( "
                    + SplitEntry.COLUMN_UID + " , "
                    + SplitEntry.COLUMN_MEMO + " , "
                    + SplitEntry.COLUMN_TYPE + " , "
                    + SplitEntry.COLUMN_ACCOUNT_UID + " , "
                    + SplitEntry.COLUMN_TRANSACTION_UID + " , "
                    + SplitEntry.COLUMN_CREATED_AT + " , "
                    + SplitEntry.COLUMN_MODIFIED_AT + " , "
                    + SplitEntry.COLUMN_VALUE_NUM + " , "
                    + SplitEntry.COLUMN_VALUE_DENOM + " , "
                    + SplitEntry.COLUMN_QUANTITY_NUM + " , "
                    + SplitEntry.COLUMN_QUANTITY_DENOM
                    + ")  SELECT "
                    + SplitEntry.COLUMN_UID + " , "
                    + SplitEntry.COLUMN_MEMO + " , "
                    + SplitEntry.COLUMN_TYPE + " , "
                    + SplitEntry.COLUMN_ACCOUNT_UID + " , "
                    + SplitEntry.COLUMN_TRANSACTION_UID + " , "
                    + SplitEntry.COLUMN_CREATED_AT + " , "
                    + SplitEntry.COLUMN_MODIFIED_AT + " , "
                    + " ROUND (" + SplitEntry.COLUMN_VALUE_NUM + "/ 100), "
                    + "1, "
                    + " ROUND (" + SplitEntry.COLUMN_QUANTITY_NUM + "/ 100), "
                    + "1 "
                    + " FROM " + SplitEntry.TABLE_NAME
                    + " WHERE " + SplitEntry.COLUMN_ACCOUNT_UID + " IN ('" + accounts + "')"
                    + ";");



            //************ UPDATE SPLITS WITH CURRENCIES HAVING 3 DECIMAL PLACES *******************
            query = "SELECT " + "A." + AccountEntry.COLUMN_UID + " AS account_uid "
                    + " FROM " + AccountEntry.TABLE_NAME + " AS A, " + CommodityEntry.TABLE_NAME + " AS C "
                    + " WHERE A." + AccountEntry.COLUMN_CURRENCY + " = C." + CommodityEntry.COLUMN_MNEMONIC
                    + " AND C." + CommodityEntry.COLUMN_SMALLEST_FRACTION + "= 1000";

            cursor = db.rawQuery(query, null);

            accountUIDs.clear();
            try {
                while (cursor.moveToNext()) {
                    String accountUID = cursor.getString(cursor.getColumnIndexOrThrow("account_uid"));
                    accountUIDs.add(accountUID);
                }
            } finally {
                cursor.close();
            }

            accounts = TextUtils.join("' , '", accountUIDs);
            db.execSQL("REPLACE INTO " + SplitEntry.TABLE_NAME + " ( "
                    + SplitEntry.COLUMN_UID             + " , "
                    + SplitEntry.COLUMN_MEMO            + " , "
                    + SplitEntry.COLUMN_TYPE            + " , "
                    + SplitEntry.COLUMN_ACCOUNT_UID     + " , "
                    + SplitEntry.COLUMN_TRANSACTION_UID + " , "
                    + SplitEntry.COLUMN_CREATED_AT      + " , "
                    + SplitEntry.COLUMN_MODIFIED_AT     + " , "
                    + SplitEntry.COLUMN_VALUE_NUM       + " , "
                    + SplitEntry.COLUMN_VALUE_DENOM     + " , "
                    + SplitEntry.COLUMN_QUANTITY_NUM    + " , "
                    + SplitEntry.COLUMN_QUANTITY_DENOM
                    + ")  SELECT "
                    + SplitEntry.COLUMN_UID + " , "
                    + SplitEntry.COLUMN_MEMO + " , "
                    + SplitEntry.COLUMN_TYPE + " , "
                    + SplitEntry.COLUMN_ACCOUNT_UID + " , "
                    + SplitEntry.COLUMN_TRANSACTION_UID + " , "
                    + SplitEntry.COLUMN_CREATED_AT  + " , "
                    + SplitEntry.COLUMN_MODIFIED_AT + " , "
                    + SplitEntry.COLUMN_VALUE_NUM + "* 10, " //add an extra zero because we used only 2 digits before
                    + "1000, "
                    + SplitEntry.COLUMN_QUANTITY_NUM + "* 10, "
                    + "1000 "
                    + " FROM " + SplitEntry.TABLE_NAME
                    + " WHERE " + SplitEntry.COLUMN_ACCOUNT_UID + " IN ('" + accounts + "')"
                    + ";");

            db.execSQL("DROP TABLE " + SplitEntry.TABLE_NAME + "_bak");

            db.setTransactionSuccessful();
            oldVersion = 9;
        } finally {
            db.endTransaction();
        }
        return oldVersion;
    }

    /**
     * Upgrades the database to version 10
     * <p>This method converts all saved scheduled export parameters to the new format using the
     * timestamp of last export</p>
     * @param db SQLite database
     * @return 10 if upgrade was successful, 9 otherwise
     */
    static int upgradeDbToVersion10(SQLiteDatabase db){
        Log.i(DatabaseHelper.LOG_TAG, "Upgrading database to version 9");
        int oldVersion = 9;

        db.beginTransaction();
        try {
            Cursor cursor = db.query(ScheduledActionEntry.TABLE_NAME,
                    new String[]{ScheduledActionEntry.COLUMN_UID, ScheduledActionEntry.COLUMN_TAG},
                    ScheduledActionEntry.COLUMN_TYPE + " = ?",
                    new String[]{ScheduledAction.ActionType.BACKUP.name()},
                    null, null, null);

            ContentValues contentValues = new ContentValues();
            while (cursor.moveToNext()){
                String paramString = cursor.getString(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_TAG));
                String[] tokens = paramString.split(";");
                ExportParams params = new ExportParams(ExportFormat.valueOf(tokens[0]));
                params.setExportTarget(ExportParams.ExportTarget.valueOf(tokens[1]));
                params.setDeleteTransactionsAfterExport(Boolean.parseBoolean(tokens[3]));

                boolean exportAll = Boolean.parseBoolean(tokens[2]);
                if (exportAll){
                    params.setExportStartTime(TimestampHelper.getTimestampFromEpochZero());
                } else {
                    Timestamp timestamp = PreferencesHelper.getLastExportTime();
                    params.setExportStartTime(timestamp);
                }

                String uid = cursor.getString(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_UID));
                contentValues.clear();
                contentValues.put(ScheduledActionEntry.COLUMN_UID, uid);
                contentValues.put(ScheduledActionEntry.COLUMN_TAG, params.toCsv());
                db.insert(ScheduledActionEntry.TABLE_NAME, null, contentValues);
            }

            cursor.close();

            db.setTransactionSuccessful();
            oldVersion = 10;
        } finally {
            db.endTransaction();
        }
        return oldVersion;
    }

    /**
     * Upgrade database to version 11
     * <p>
     *     Migrate scheduled backups and update export parameters to the new format
     * </p>
     * @param db SQLite database
     * @return 11 if upgrade was successful, 10 otherwise
     */
    static int upgradeDbToVersion11(SQLiteDatabase db){
        Log.i(DatabaseHelper.LOG_TAG, "Upgrading database to version 9");
        int oldVersion = 10;

        db.beginTransaction();
        try {
            Cursor cursor = db.query(ScheduledActionEntry.TABLE_NAME, null,
                    ScheduledActionEntry.COLUMN_TYPE + "= ?",
                    new String[]{ScheduledAction.ActionType.BACKUP.name()}, null, null, null);

            Map<String, String> uidToTagMap = new HashMap<>();
            while (cursor.moveToNext()) {
                String uid = cursor.getString(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_UID));
                String tag = cursor.getString(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_TAG));
                String[] tokens = tag.split(";");
                try {
                    Timestamp timestamp = TimestampHelper.getTimestampFromUtcString(tokens[2]);
                } catch (IllegalArgumentException ex) {
                    tokens[2] = TimestampHelper.getUtcStringFromTimestamp(PreferencesHelper.getLastExportTime());
                } finally {
                    tag = TextUtils.join(";", tokens);
                }
                uidToTagMap.put(uid, tag);
            }

            cursor.close();

            ContentValues contentValues = new ContentValues();
            for (Map.Entry<String, String> entry : uidToTagMap.entrySet()) {
                contentValues.clear();
                contentValues.put(ScheduledActionEntry.COLUMN_TAG, entry.getValue());
                db.update(ScheduledActionEntry.TABLE_NAME, contentValues,
                        ScheduledActionEntry.COLUMN_UID + " = ?", new String[]{entry.getKey()});
            }

            db.setTransactionSuccessful();
            oldVersion = 11;
        } finally {
            db.endTransaction();
        }
        return oldVersion;
    }

    public static Timestamp subtractTimeZoneOffset(Timestamp timestamp, TimeZone timeZone) {
        final long millisecondsToSubtract = Math.abs(timeZone.getOffset(timestamp.getTime()));
        return new Timestamp(timestamp.getTime() - millisecondsToSubtract);
    }

    /**
     * Upgrade database to version 12
     * <p>
     *     Change last_export_time Android preference to current value - N
     *     where N is the absolute timezone offset for current user time zone.
     *     For details see #467.
     * </p>
     * @param db SQLite database
     * @return 12 if upgrade was successful, 11 otherwise
     */
    static int upgradeDbToVersion12(SQLiteDatabase db){
        Log.i(MigrationHelper.LOG_TAG, "Upgrading database to version 12");

        int oldVersion = 11;

        try {

            final Timestamp currentLastExportTime = PreferencesHelper.getLastExportTime();

            final Timestamp updatedLastExportTime = subtractTimeZoneOffset(
                    currentLastExportTime, TimeZone.getDefault());
            PreferencesHelper.setLastExportTime(updatedLastExportTime);

            oldVersion = 12;

        } catch (Exception ignored){
            // Do nothing: here oldVersion = 11.
        }

        return oldVersion;
    }

    /**
     * Upgrades the database to version 13.
     * <p>This migration makes the following changes to the database:
     * <ul>
     *     <li>Adds support for multiple database for different books and one extra database for storing book info</li>
     *     <li>Adds a table for budgets</li>
     *     <li>Adds an extra table for recurrences</li>
     *     <li>Migrate scheduled transaction recurrences to own table</li>
     *     <li>Adds flags for reconciled status to split table</li>
     *     <li>Add flags for auto-/advance- create and notification to scheduled actions</li>
     *     <li>Migrate old shared preferences into new book-specific preferences</li>
     * </ul>
     * </p>
     * @param db SQlite database to be upgraded
     * @return New database version, 13 if migration succeeds, 11 otherwise
     */
    static int upgradeDbToVersion13(SQLiteDatabase db){
        Log.i(DatabaseHelper.LOG_TAG, "Upgrading database to version 13");
        int oldVersion = 12;

        db.beginTransaction();
        try {
            db.execSQL("CREATE TABLE " + RecurrenceEntry.TABLE_NAME + " ("
                    + RecurrenceEntry._ID                   + " integer primary key autoincrement, "
                    + RecurrenceEntry.COLUMN_UID            + " varchar(255) not null UNIQUE, "
                    + RecurrenceEntry.COLUMN_MULTIPLIER     + " integer not null default 1, "
                    + RecurrenceEntry.COLUMN_PERIOD_TYPE    + " varchar(255) not null, "
                    + RecurrenceEntry.COLUMN_BYDAY          + " varchar(255), "
                    + RecurrenceEntry.COLUMN_PERIOD_START   + " timestamp not null, "
                    + RecurrenceEntry.COLUMN_PERIOD_END   + " timestamp, "
                    + RecurrenceEntry.COLUMN_CREATED_AT     + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + RecurrenceEntry.COLUMN_MODIFIED_AT    + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP); "
                    + DatabaseHelper.createUpdatedAtTrigger(RecurrenceEntry.TABLE_NAME));

            db.execSQL("CREATE TABLE " + BudgetEntry.TABLE_NAME + " ("
                    + BudgetEntry._ID                   + " integer primary key autoincrement, "
                    + BudgetEntry.COLUMN_UID            + " varchar(255) not null UNIQUE, "
                    + BudgetEntry.COLUMN_NAME           + " varchar(255) not null, "
                    + BudgetEntry.COLUMN_DESCRIPTION    + " varchar(255), "
                    + BudgetEntry.COLUMN_RECURRENCE_UID + " varchar(255) not null, "
                    + BudgetEntry.COLUMN_NUM_PERIODS    + " integer, "
                    + BudgetEntry.COLUMN_CREATED_AT     + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + BudgetEntry.COLUMN_MODIFIED_AT    + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (" 	+ BudgetEntry.COLUMN_RECURRENCE_UID + ") REFERENCES " + RecurrenceEntry.TABLE_NAME + " (" + RecurrenceEntry.COLUMN_UID + ") "
                    + ");" + DatabaseHelper.createUpdatedAtTrigger(BudgetEntry.TABLE_NAME));

            db.execSQL("CREATE UNIQUE INDEX '" + BudgetEntry.INDEX_UID
                    + "' ON " + BudgetEntry.TABLE_NAME + "(" + BudgetEntry.COLUMN_UID + ")");

            db.execSQL("CREATE TABLE " + BudgetAmountEntry.TABLE_NAME + " ("
                    + BudgetAmountEntry._ID                   + " integer primary key autoincrement, "
                    + BudgetAmountEntry.COLUMN_UID            + " varchar(255) not null UNIQUE, "
                    + BudgetAmountEntry.COLUMN_BUDGET_UID     + " varchar(255) not null, "
                    + BudgetAmountEntry.COLUMN_ACCOUNT_UID    + " varchar(255) not null, "
                    + BudgetAmountEntry.COLUMN_AMOUNT_NUM     + " integer not null, "
                    + BudgetAmountEntry.COLUMN_AMOUNT_DENOM   + " integer not null, "
                    + BudgetAmountEntry.COLUMN_PERIOD_NUM     + " integer not null, "
                    + BudgetAmountEntry.COLUMN_CREATED_AT     + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + BudgetAmountEntry.COLUMN_MODIFIED_AT    + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (" 	+ BudgetAmountEntry.COLUMN_ACCOUNT_UID + ") REFERENCES " + AccountEntry.TABLE_NAME + " (" + AccountEntry.COLUMN_UID + ") ON DELETE CASCADE, "
                    + "FOREIGN KEY (" 	+ BudgetAmountEntry.COLUMN_BUDGET_UID + ") REFERENCES " + BudgetEntry.TABLE_NAME + " (" + BudgetEntry.COLUMN_UID + ") ON DELETE CASCADE "
                    + ");" + DatabaseHelper.createUpdatedAtTrigger(BudgetAmountEntry.TABLE_NAME));

            db.execSQL("CREATE UNIQUE INDEX '" + BudgetAmountEntry.INDEX_UID
                    + "' ON " + BudgetAmountEntry.TABLE_NAME + "(" + BudgetAmountEntry.COLUMN_UID + ")");


            //extract recurrences from scheduled actions table and put in the recurrence table
            db.execSQL("ALTER TABLE " + ScheduledActionEntry.TABLE_NAME + " RENAME TO " + ScheduledActionEntry.TABLE_NAME + "_bak");

            db.execSQL("CREATE TABLE " + ScheduledActionEntry.TABLE_NAME + " ("
                    + ScheduledActionEntry._ID                      + " integer primary key autoincrement, "
                    + ScheduledActionEntry.COLUMN_UID               + " varchar(255) not null UNIQUE, "
                    + ScheduledActionEntry.COLUMN_ACTION_UID        + " varchar(255) not null, "
                    + ScheduledActionEntry.COLUMN_TYPE              + " varchar(255) not null, "
                    + ScheduledActionEntry.COLUMN_RECURRENCE_UID    + " varchar(255) not null, "
                    + ScheduledActionEntry.COLUMN_TEMPLATE_ACCT_UID + " varchar(255) not null, "
                    + ScheduledActionEntry.COLUMN_LAST_RUN          + " integer default 0, "
                    + ScheduledActionEntry.COLUMN_START_TIME        + " integer not null, "
                    + ScheduledActionEntry.COLUMN_END_TIME          + " integer default 0, "
                    + ScheduledActionEntry.COLUMN_TAG               + " text, "
                    + ScheduledActionEntry.COLUMN_ENABLED           + " tinyint default 1, " //enabled by default
                    + ScheduledActionEntry.COLUMN_AUTO_CREATE       + " tinyint default 1, "
                    + ScheduledActionEntry.COLUMN_AUTO_NOTIFY       + " tinyint default 0, "
                    + ScheduledActionEntry.COLUMN_ADVANCE_CREATION  + " integer default 0, "
                    + ScheduledActionEntry.COLUMN_ADVANCE_NOTIFY    + " integer default 0, "
                    + ScheduledActionEntry.COLUMN_TOTAL_FREQUENCY   + " integer default 0, "
                    + ScheduledActionEntry.COLUMN_EXECUTION_COUNT   + " integer default 0, "
                    + ScheduledActionEntry.COLUMN_CREATED_AT        + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + ScheduledActionEntry.COLUMN_MODIFIED_AT       + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (" 	+ ScheduledActionEntry.COLUMN_RECURRENCE_UID + ") REFERENCES " + RecurrenceEntry.TABLE_NAME + " (" + RecurrenceEntry.COLUMN_UID + ") "
                    + ");" + DatabaseHelper.createUpdatedAtTrigger(ScheduledActionEntry.TABLE_NAME));


            // initialize new transaction table with data from old table
            db.execSQL("INSERT INTO " + ScheduledActionEntry.TABLE_NAME + " ( "
                            + ScheduledActionEntry._ID + " , "
                            + ScheduledActionEntry.COLUMN_UID + " , "
                            + ScheduledActionEntry.COLUMN_ACTION_UID + " , "
                            + ScheduledActionEntry.COLUMN_TYPE + " , "
                            + ScheduledActionEntry.COLUMN_LAST_RUN + " , "
                            + ScheduledActionEntry.COLUMN_START_TIME + " , "
                            + ScheduledActionEntry.COLUMN_END_TIME + " , "
                            + ScheduledActionEntry.COLUMN_ENABLED + " , "
                            + ScheduledActionEntry.COLUMN_TOTAL_FREQUENCY + " , "
                            + ScheduledActionEntry.COLUMN_EXECUTION_COUNT + " , "
                            + ScheduledActionEntry.COLUMN_CREATED_AT + " , "
                            + ScheduledActionEntry.COLUMN_MODIFIED_AT + " , "
                            + ScheduledActionEntry.COLUMN_RECURRENCE_UID + " , "
                            + ScheduledActionEntry.COLUMN_TEMPLATE_ACCT_UID + " , "
                            + ScheduledActionEntry.COLUMN_TAG
                            + ")  SELECT "
                            + ScheduledActionEntry.TABLE_NAME + "_bak." + ScheduledActionEntry._ID + " , "
                            + ScheduledActionEntry.TABLE_NAME + "_bak." + ScheduledActionEntry.COLUMN_UID + " , "
                            + ScheduledActionEntry.TABLE_NAME + "_bak." + ScheduledActionEntry.COLUMN_ACTION_UID + " , "
                            + ScheduledActionEntry.TABLE_NAME + "_bak." + ScheduledActionEntry.COLUMN_TYPE + " , "
                            + ScheduledActionEntry.TABLE_NAME + "_bak." + ScheduledActionEntry.COLUMN_LAST_RUN + " , "
                            + ScheduledActionEntry.TABLE_NAME + "_bak." + ScheduledActionEntry.COLUMN_START_TIME + " , "
                            + ScheduledActionEntry.TABLE_NAME + "_bak." + ScheduledActionEntry.COLUMN_END_TIME + " , "
                            + ScheduledActionEntry.TABLE_NAME + "_bak." + ScheduledActionEntry.COLUMN_ENABLED + " , "
                            + ScheduledActionEntry.TABLE_NAME + "_bak." + ScheduledActionEntry.COLUMN_TOTAL_FREQUENCY + " , "
                            + ScheduledActionEntry.TABLE_NAME + "_bak." + ScheduledActionEntry.COLUMN_EXECUTION_COUNT + " , "
                            + ScheduledActionEntry.TABLE_NAME + "_bak." + ScheduledActionEntry.COLUMN_CREATED_AT + " , "
                            + ScheduledActionEntry.TABLE_NAME + "_bak." + ScheduledActionEntry.COLUMN_MODIFIED_AT + " , "
                            + " 'dummy-string' ," //will be updated in next steps
                            + " 'dummy-string' ,"
                            + ScheduledActionEntry.TABLE_NAME + "_bak." + ScheduledActionEntry.COLUMN_TAG
                            + " FROM " + ScheduledActionEntry.TABLE_NAME + "_bak;");

            //update the template-account-guid and the recurrence guid for all scheduled actions
            Cursor cursor = db.query(ScheduledActionEntry.TABLE_NAME + "_bak",
                    new String[]{ScheduledActionEntry.COLUMN_UID,
                            "period",
                            ScheduledActionEntry.COLUMN_START_TIME
                    },
                    null, null, null, null, null);

            ContentValues contentValues = new ContentValues();
            while (cursor.moveToNext()){
                String uid = cursor.getString(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_UID));
                long period = cursor.getLong(cursor.getColumnIndexOrThrow("period"));
                long startTime = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_START_TIME));
                Recurrence recurrence = Recurrence.fromLegacyPeriod(period);
                recurrence.setPeriodStart(new Timestamp(startTime));

                contentValues.clear();
                contentValues.put(RecurrenceEntry.COLUMN_UID, recurrence.getUID());
                contentValues.put(RecurrenceEntry.COLUMN_MULTIPLIER, recurrence.getMultiplier());
                contentValues.put(RecurrenceEntry.COLUMN_PERIOD_TYPE, recurrence.getPeriodType().name());
                contentValues.put(RecurrenceEntry.COLUMN_PERIOD_START, recurrence.getPeriodStart().toString());
                db.insert(RecurrenceEntry.TABLE_NAME, null, contentValues);

                contentValues.clear();
                contentValues.put(ScheduledActionEntry.COLUMN_RECURRENCE_UID, recurrence.getUID());
                contentValues.put(ScheduledActionEntry.COLUMN_TEMPLATE_ACCT_UID, BaseModel.generateUID());
                db.update(ScheduledActionEntry.TABLE_NAME, contentValues,
                        ScheduledActionEntry.COLUMN_UID + " = ?", new String[]{uid});
            }
            cursor.close();

            db.execSQL("DROP TABLE " + ScheduledActionEntry.TABLE_NAME + "_bak");


            //==============  Add RECONCILE_STATE and RECONCILE_DATE to the splits table ==========
            //We migrate the whole table because we want those columns to have default values

            db.execSQL("ALTER TABLE " + SplitEntry.TABLE_NAME + " RENAME TO " + SplitEntry.TABLE_NAME + "_bak");
            db.execSQL("CREATE TABLE " + SplitEntry.TABLE_NAME + " ("
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
                    + SplitEntry.COLUMN_RECONCILE_STATE + " varchar(1) not null default 'n', "
                    + SplitEntry.COLUMN_RECONCILE_DATE  + " timestamp not null default current_timestamp, "
                    + SplitEntry.COLUMN_CREATED_AT      + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + SplitEntry.COLUMN_MODIFIED_AT     + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (" 	+ SplitEntry.COLUMN_ACCOUNT_UID + ") REFERENCES " + AccountEntry.TABLE_NAME + " (" + AccountEntry.COLUMN_UID + ") ON DELETE CASCADE, "
                    + "FOREIGN KEY (" 	+ SplitEntry.COLUMN_TRANSACTION_UID + ") REFERENCES " + TransactionEntry.TABLE_NAME + " (" + TransactionEntry.COLUMN_UID + ") ON DELETE CASCADE "
                    + ");" + DatabaseHelper.createUpdatedAtTrigger(SplitEntry.TABLE_NAME));

            db.execSQL("INSERT INTO " + SplitEntry.TABLE_NAME + " ( "
                    + SplitEntry._ID                    + " , "
                    + SplitEntry.COLUMN_UID             + " , "
                    + SplitEntry.COLUMN_MEMO            + " , "
                    + SplitEntry.COLUMN_TYPE            + " , "
                    + SplitEntry.COLUMN_VALUE_NUM       + " , "
                    + SplitEntry.COLUMN_VALUE_DENOM     + " , "
                    + SplitEntry.COLUMN_QUANTITY_NUM    + " , "
                    + SplitEntry.COLUMN_QUANTITY_DENOM  + " , "
                    + SplitEntry.COLUMN_ACCOUNT_UID     + " , "
                    + SplitEntry.COLUMN_TRANSACTION_UID
                    + ")  SELECT "
                    + SplitEntry.TABLE_NAME + "_bak." + SplitEntry._ID                  + " , "
                    + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_UID           + " , "
                    + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_MEMO          + " , "
                    + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_TYPE          + " , "
                    + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_VALUE_NUM     + " , "
                    + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_VALUE_DENOM   + " , "
                    + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_QUANTITY_NUM  + " , "
                    + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_QUANTITY_DENOM + " , "
                    + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_ACCOUNT_UID   + " , "
                    + SplitEntry.TABLE_NAME + "_bak." + SplitEntry.COLUMN_TRANSACTION_UID
                    + " FROM " + SplitEntry.TABLE_NAME + "_bak;");


            db.execSQL("DROP TABLE " + SplitEntry.TABLE_NAME + "_bak");

            db.setTransactionSuccessful();
            oldVersion = 13;
        } finally {
            db.endTransaction();
        }

        //Migrate book-specific preferences away from shared preferences
        Log.d(LOG_TAG, "Migrating shared preferences into book preferences");
        Context context = GnuCashApplication.getAppContext();
        String keyUseDoubleEntry = context.getString(R.string.key_use_double_entry);
        String keySaveOpeningBalance = context.getString(R.string.key_save_opening_balances);
        String keyLastExportTime = PreferencesHelper.PREFERENCE_LAST_EXPORT_TIME_KEY;
        String keyUseCompactView = context.getString(R.string.key_use_compact_list);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lastExportTime = sharedPrefs.getString(keyLastExportTime, TimestampHelper.getTimestampFromEpochZero().toString());
        boolean useDoubleEntry = sharedPrefs.getBoolean(keyUseDoubleEntry, true);
        boolean saveOpeningBalance = sharedPrefs.getBoolean(keySaveOpeningBalance, false);
        boolean useCompactTrnView = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.key_use_double_entry), !useDoubleEntry);

        String rootAccountUID = getGnuCashRootAccountUID(db);
        SharedPreferences bookPrefs = context.getSharedPreferences(rootAccountUID, Context.MODE_PRIVATE);

        bookPrefs.edit()
                .putString(keyLastExportTime, lastExportTime)
                .putBoolean(keyUseDoubleEntry, useDoubleEntry)
                .putBoolean(keySaveOpeningBalance, saveOpeningBalance)
                .putBoolean(keyUseCompactView, useCompactTrnView)
                .apply();

        rescheduleServiceAlarm();


        return oldVersion;
    }

    /**
     * Cancel the existing alarm for the scheduled service and restarts/reschedules the service
     */
    private static void rescheduleServiceAlarm() {
        Context context = GnuCashApplication.getAppContext();

        //cancel the existing pending intent so that the alarm can be rescheduled
        Intent alarmIntent = new Intent(context, ScheduledActionService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, alarmIntent, PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }

        GnuCashApplication.startScheduledActionExecutionService(context);
    }

    /**
     * Move files from {@code srcDir} to {@code dstDir}
     * Subdirectories will be created in the target as necessary
     * @param srcDir Source directory which should already exist
     * @param dstDir Destination directory which should already exist
     * @see #moveFile(File, File)
     * @throws IOException if the {@code srcDir} does not exist or {@code dstDir} could not be created
     * @throws IllegalArgumentException if {@code srcDir} is not a directory
     */
    private static void moveDirectory(File srcDir, File dstDir) throws IOException {
        if (!srcDir.isDirectory()){
            throw new IllegalArgumentException("Source is not a directory: " + srcDir.getPath());
        }

        if (!srcDir.exists()){
            String msg = String.format(Locale.US, "Source directory %s does not exist", srcDir.getPath());
            Log.e(LOG_TAG, msg);
            throw new IOException(msg);
        }

        if (!dstDir.exists() || !dstDir.isDirectory()){
            Log.w(LOG_TAG, "Target directory does not exist. Attempting to create..." + dstDir.getPath());
            if (!dstDir.mkdirs()){
                throw new IOException(String.format("Target directory %s does not exist and could not be created", dstDir.getPath()));
            }
        }

        if (srcDir.listFiles() == null) //nothing to see here, move along
            return;

        for (File src : srcDir.listFiles()){
            if (src.isDirectory()){
                File dst = new File(dstDir, src.getName());
                dst.mkdir();
                moveDirectory(src, dst);
                if (!src.delete())
                    Log.i(LOG_TAG, "Failed to delete directory: " + src.getPath());
                continue;
            }

            try {
                File dst = new File(dstDir, src.getName());
                MigrationHelper.moveFile(src, dst);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error moving file " + src.getPath());
                Crashlytics.logException(e);
            }
        }
    }

    /**
     * Upgrade the database to version 14
     * <p>
     *     This migration actually does not change anything in the database
     *     It moves the backup files to a new backup location which does not require SD CARD write permission
     * </p>
     * @param db SQLite database to be upgraded
     * @return New database version
     */
    public static int upgradeDbToVersion14(SQLiteDatabase db){
        Log.i(DatabaseHelper.LOG_TAG, "Upgrading database to version 14");
        int oldDbVersion = 13;
        File backupFolder = new File(Exporter.BASE_FOLDER_PATH);
        backupFolder.mkdir();

        new Thread(new Runnable() {
            @Override
            public void run() {
                File srcDir = new File(Exporter.LEGACY_BASE_FOLDER_PATH);
                File dstDir = new File(Exporter.BASE_FOLDER_PATH);
                try {
                    moveDirectory(srcDir, dstDir);
                    File readmeFile = new File(Exporter.LEGACY_BASE_FOLDER_PATH, "README.txt");
                    FileWriter writer = null;
                    writer = new FileWriter(readmeFile);
                    writer.write("Backup files have been moved to " + dstDir.getPath() +
                            "\nYou can now delete this folder");
                    writer.flush();
                } catch (IOException | IllegalArgumentException ex) {
                    ex.printStackTrace();
                    String msg = String.format("Error moving files from %s to %s", srcDir.getPath(), dstDir.getPath());
                    Log.e(LOG_TAG, msg);
                    Crashlytics.log(msg);
                    Crashlytics.logException(ex);
                }

            }
        }).start();

        return 14;
    }

    /**
     * Upgrades the database to version 14.
     * <p>This migration makes the following changes to the database:
     * <ul>
     *     <li>Fixes accounts referencing a default transfer account that no longer
     *         exists (see #654)</li>
     * </ul>
     * </p>
     * @param db SQLite database to be upgraded
     * @return New database version, 14 if migration succeeds, 13 otherwise
     */
    static int upgradeDbToVersion15(SQLiteDatabase db) {
        Log.i(DatabaseHelper.LOG_TAG, "Upgrading database to version 15");
        int dbVersion = 14;

        db.beginTransaction();
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.putNull(AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID);
            db.update(
                    AccountEntry.TABLE_NAME,
                    contentValues,
                    AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID
                            + " NOT IN (SELECT " + AccountEntry.COLUMN_UID
                            + "             FROM " + AccountEntry.TABLE_NAME + ")",
                    null);
            db.setTransactionSuccessful();
            dbVersion = 15;
        } finally {
            db.endTransaction();
        }

        //remove previously saved export destination index because the number of destinations has changed
        //an invalid value would lead to crash on start
        Context context = GnuCashApplication.getAppContext();
        android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .remove(context.getString(R.string.key_last_export_destination))
                .apply();

        //the default interval has been changed from daily to hourly with this release. So reschedule alarm
        rescheduleServiceAlarm();
        return dbVersion;
    }
}
