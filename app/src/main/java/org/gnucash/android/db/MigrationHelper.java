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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.gnucash.android.importer.GncXmlImporter;
import org.gnucash.android.model.AccountType;

import java.io.FileInputStream;

import static org.gnucash.android.db.DatabaseSchema.AccountEntry;

/**
 * Collection of helper methods which are used during database migrations
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class MigrationHelper {
    public static final String LOG_TAG = "MigrationHelper";

    /**
     * Performs same function as {@link AccountsDbAdapter#getFullyQualifiedAccountName(String)}
     * <p>This method is only necessary because we cannot open the database again (by instantiating {@link org.gnucash.android.db.AccountsDbAdapter}
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
        Cursor cursor =  db.query(DatabaseSchema.AccountEntry.TABLE_NAME,
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
     * Imports GnuCash XML into the database from file
     * @param filepath Path to GnuCash XML file
     */
    static void importGnucashXML(SQLiteDatabase db, String filepath) throws Exception {
        Log.i(LOG_TAG, "Importing GnuCash XML");
        FileInputStream inputStream = new FileInputStream(filepath);
        GncXmlImporter.parse(db, inputStream);
    }

    /**
     * Add created_at and modified_at columns to a table in the database and create a trigger
     * for updating the modified_at columns
     * @param db SQLite database
     * @param tableName Name of the table
     */
    static void createUpdatedAndModifiedColumns(SQLiteDatabase db, String tableName){
        String addCreatedColumn = "ALTER TABLE " + tableName
                + " ADD COLUMN " + DatabaseSchema.CommonColumns.COLUMN_CREATED_AT
                + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP";

        String addModifiedColumn = "ALTER TABLE " + tableName
                + " ADD COLUMN " + DatabaseSchema.CommonColumns.COLUMN_MODIFIED_AT
                + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP";

        db.execSQL(addCreatedColumn);
        db.execSQL(addModifiedColumn);
        db.execSQL(DatabaseHelper.createUpdatedAtTrigger(tableName));
    }
}
