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
import android.os.Environment;
import android.util.Log;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.export.qif.QifExporter;
import org.gnucash.android.export.xml.GncXmlExporter;
import org.gnucash.android.importer.GncXmlImporter;
import org.gnucash.android.model.AccountType;

import java.io.*;

import static org.gnucash.android.db.DatabaseSchema.AccountEntry;

/**
 * Date: 23.03.2014
 *
 * @author Ngewi
 */
public class MigrationHelper {
    public static final String LOG_TAG = "MigrationHelper";

    /**
     * Performs same functtion as {@link AccountsDbAdapter#getFullyQualifiedAccountName(String)}
     * <p>This method is only necessary because we cannot open the database again (by instantiating {@link org.gnucash.android.db.AccountsDbAdapter}
     * while it is locked for upgrades. So we reimplement the method here.</p>
     * @param db SQLite database
     * @param accountUID Unique ID of account whose fully qualified name is to be determined
     * @return Fully qualified (colon-sepaated) account name
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
     * Exports the database to a GnuCash XML file and returns the path to the file
     * @return String with exported GnuCash XML
     */
    static String exportDatabase(SQLiteDatabase db, ExportFormat format) throws IOException {
        Log.i(LOG_TAG, "Exporting database to GnuCash XML");
        ExportParams exportParams = new ExportParams(format);
        exportParams.setExportAllTransactions(true);
        exportParams.setExportTarget(ExportParams.ExportTarget.SD_CARD);
        exportParams.setDeleteTransactionsAfterExport(false);

        new File(Environment.getExternalStorageDirectory() + "/gnucash/").mkdirs();
        exportParams.setTargetFilepath(Environment.getExternalStorageDirectory()
                + "/gnucash/" + Exporter.buildExportFilename(format));

        //we do not use the ExporterAsyncTask here because we want to use an already open db
        Exporter exporter = null;
        switch (format){
            case GNC_XML:
                exporter = new GncXmlExporter(exportParams, db);
                break;
            default:
                throw new IllegalArgumentException("Only Gnc XML is supported in Migration");
        }

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(exportParams.getTargetFilepath()), "UTF-8"));
        exporter.generateExport(writer);

        writer.flush();
        writer.close();
        return exportParams.getTargetFilepath();
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
}
