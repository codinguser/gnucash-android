/*
 * Copyright (c) 2014 - 2015 Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2014 Yongxin Wang <fefe.wyx@gmail.com>
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

package org.gnucash.android.export;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.ScheduledActionDbAdapter;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;

import java.io.File;
import java.io.FileFilter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Base class for the different exporters
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 * @author Yongxin Wang <fefe.wyx@gmail.com>
 */
public abstract class Exporter {

    /**
     * Application folder on external storage
     */
    public static final String BASE_FOLDER_PATH = Environment.getExternalStorageDirectory() + "/" + BuildConfig.APPLICATION_ID;

    /**
     * Folder where exports like QIF and OFX will be saved for access by external programs
     */
    public static final String EXPORT_FOLDER_PATH =  BASE_FOLDER_PATH + "/exports/";

    /**
     * Folder where GNC_XML backups will be saved
     */
    public static final String BACKUP_FOLDER_PATH = BASE_FOLDER_PATH + "/backups/";

    /**
     * Export options
     */
    protected ExportParams mParameters;

    /**
     * Adapter for retrieving accounts to export
     * Subclasses should close this object when they are done with exporting
     */
    protected AccountsDbAdapter mAccountsDbAdapter;
    protected TransactionsDbAdapter mTransactionsDbAdapter;
    protected SplitsDbAdapter mSplitsDbAdapter;
    protected ScheduledActionDbAdapter mScheduledActionDbAdapter;
    protected Context mContext;

    public Exporter(ExportParams params, SQLiteDatabase db) {
        this.mParameters = params;
        mContext = GnuCashApplication.getAppContext();
        if (db == null) {
            mAccountsDbAdapter = AccountsDbAdapter.getInstance();
            mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();
            mSplitsDbAdapter = SplitsDbAdapter.getInstance();
            mScheduledActionDbAdapter = ScheduledActionDbAdapter.getInstance();
        } else {
            mSplitsDbAdapter = new SplitsDbAdapter(db);
            mTransactionsDbAdapter = new TransactionsDbAdapter(db, mSplitsDbAdapter);
            mAccountsDbAdapter = new AccountsDbAdapter(db, mTransactionsDbAdapter);
            mScheduledActionDbAdapter = new ScheduledActionDbAdapter(db);
        }
    }

    /**
     * Builds a file name based on the current time stamp for the exported file
     * @return String containing the file name
     */
    public static String buildExportFilename(ExportFormat format) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        String filename = formatter.format(
                new Date(System.currentTimeMillis()))
                + "_gnucash_export" + format.getExtension();
        return filename;
    }

    /**
     * Builds a file (creating folders where necessary) for saving the exported data
     * @param format Export format which determines the file extension
     * @return File for export
     * @see #EXPORT_FOLDER_PATH
     */
    public static File createExportFile(ExportFormat format){
        new File(EXPORT_FOLDER_PATH).mkdirs();
        return new File(EXPORT_FOLDER_PATH + buildExportFilename(format));
    }

    /**
     * Builds a file for backups of the database (in GNC_XML) format
     * @return File for saving backups
     * @see #BACKUP_FOLDER_PATH
     */
    public static File createBackupFile(){
        new File(BACKUP_FOLDER_PATH).mkdirs();
        return new File(BACKUP_FOLDER_PATH + buildExportFilename(ExportFormat.GNC_XML));
    }

    /**
     * Returns the most recent backup file from the backup folder
     * @return Last modified file from backup folder
     * @see #BACKUP_FOLDER_PATH
     */
    public static File getMostRecentBackupFile(){
        File backupFolder = new File(BACKUP_FOLDER_PATH);
        if (!backupFolder.exists())
            return null;

        File[] files = backupFolder.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        long lastMod = Long.MIN_VALUE;
        File backupFile = null;
        for (File file : files) {
            if (file.lastModified() > lastMod) {
                backupFile = file;
                lastMod = file.lastModified();
            }
        }
        return backupFile;
    }

    /**
     * Generates the export output
     * @param writer A Writer to export result to
     * @throws ExporterException if an error occurs during export
     */
    public abstract void generateExport(Writer writer) throws ExporterException;

    public static class ExporterException extends RuntimeException{

        public ExporterException(ExportParams params){
            super("Failed to generate " + params.getExportFormat().toString());
        }

        public ExporterException(ExportParams params, Throwable throwable){
            super("Failed to generate " + params.getExportFormat().toString(), throwable);
        }
    }
}
