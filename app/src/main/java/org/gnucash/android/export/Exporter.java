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
import android.support.annotation.NonNull;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.db.adapter.BudgetAmountsDbAdapter;
import org.gnucash.android.db.adapter.BudgetsDbAdapter;
import org.gnucash.android.db.adapter.CommoditiesDbAdapter;
import org.gnucash.android.db.adapter.PricesDbAdapter;
import org.gnucash.android.db.adapter.RecurrenceDbAdapter;
import org.gnucash.android.db.adapter.ScheduledActionDbAdapter;
import org.gnucash.android.db.adapter.SplitsDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Base class for the different exporters
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 * @author Yongxin Wang <fefe.wyx@gmail.com>
 */
public abstract class Exporter {

    /**
     * Tag for logging
     */
    protected static String LOG_TAG = "Exporter";

    /**
     * Application folder on external storage
     * @deprecated Use {@link #BASE_FOLDER_PATH} instead
     */
    @Deprecated
    public static final String LEGACY_BASE_FOLDER_PATH = Environment.getExternalStorageDirectory() + "/" + BuildConfig.APPLICATION_ID;

    /**
     * Application folder on external storage
     */
    public static final String BASE_FOLDER_PATH = GnuCashApplication.getAppContext().getExternalFilesDir(null).getAbsolutePath();

    /**
     * Export options
     */
    protected final ExportParams mExportParams;

    /**
     * Cache directory to which files will be first exported before moved to final destination.
     * <p>There is a different cache dir per export format, which has the name of the export format.<br/>
     *    The cache dir is cleared every time a new {@link Exporter} is instantiated.
     *    The files created here are only accessible within this application, and should be copied to SD card before they can be shared
     * </p>
     */
    private final File mCacheDir;

    private static final SimpleDateFormat EXPORT_FILENAME_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);

    /**
     * Adapter for retrieving accounts to export
     * Subclasses should close this object when they are done with exporting
     */
    protected final AccountsDbAdapter mAccountsDbAdapter;
    protected final TransactionsDbAdapter mTransactionsDbAdapter;
    protected final SplitsDbAdapter mSplitsDbAdapter;
    protected final ScheduledActionDbAdapter mScheduledActionDbAdapter;
    protected final PricesDbAdapter mPricesDbAdapter;
    protected final CommoditiesDbAdapter mCommoditiesDbAdapter;
	protected final BudgetsDbAdapter mBudgetsDbAdapter;
    protected final Context mContext;
    private String mExportCacheFilePath;

    /**
     * Database being currently exported
     */
    protected final SQLiteDatabase mDb;

    /**
     * GUID of the book being exported
     */
    protected String mBookUID;

    public Exporter(ExportParams params, SQLiteDatabase db) {
        this.mExportParams = params;
        mContext = GnuCashApplication.getAppContext();
        if (db == null) {
            mAccountsDbAdapter      = AccountsDbAdapter.getInstance();
            mTransactionsDbAdapter  = TransactionsDbAdapter.getInstance();
            mSplitsDbAdapter        = SplitsDbAdapter.getInstance();
            mPricesDbAdapter        = PricesDbAdapter.getInstance();
            mCommoditiesDbAdapter   = CommoditiesDbAdapter.getInstance();
            mBudgetsDbAdapter       = BudgetsDbAdapter.getInstance();
            mScheduledActionDbAdapter = ScheduledActionDbAdapter.getInstance();
            mDb = GnuCashApplication.getActiveDb();
        } else {
            mDb = db;
            mSplitsDbAdapter        = new SplitsDbAdapter(db);
            mTransactionsDbAdapter  = new TransactionsDbAdapter(db, mSplitsDbAdapter);
            mAccountsDbAdapter      = new AccountsDbAdapter(db, mTransactionsDbAdapter);
            mPricesDbAdapter        = new PricesDbAdapter(db);
            mCommoditiesDbAdapter   = new CommoditiesDbAdapter(db);
            RecurrenceDbAdapter recurrenceDbAdapter = new RecurrenceDbAdapter(db);
            mBudgetsDbAdapter       = new BudgetsDbAdapter(db, new BudgetAmountsDbAdapter(db), recurrenceDbAdapter);
            mScheduledActionDbAdapter = new ScheduledActionDbAdapter(db, recurrenceDbAdapter);
        }

        mBookUID = new File(mDb.getPath()).getName(); //this depends on the database file always having the name of the book GUID
        mExportCacheFilePath = null;
        mCacheDir = new File(mContext.getCacheDir(), params.getExportFormat().name());
        mCacheDir.mkdir();
        purgeDirectory(mCacheDir);
    }

    /**
     * Strings a string of any characters not allowed in a file name.
     * All unallowed characters are replaced with an underscore
     * @param inputName Raw file name input
     * @return Sanitized file name
     */
    public static String sanitizeFilename(String inputName) {
        return inputName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }

    /**
     * Builds a file name based on the current time stamp for the exported file
     * @param format Format to use when exporting
     * @param bookName Name of the book being exported. This name will be included in the generated file name
     * @return String containing the file name
     */
    public static String buildExportFilename(ExportFormat format, String bookName) {
        return EXPORT_FILENAME_DATE_FORMAT.format(new Date(System.currentTimeMillis()))
                + "_gnucash_export_" + sanitizeFilename(bookName) + format.getExtension();
    }

    /**
     * Parses the name of an export file and returns the date of export
     * @param filename Export file name generated by {@link #buildExportFilename(ExportFormat,String)}
     * @return Date in milliseconds
     */
    public static long getExportTime(String filename){
        String[] tokens = filename.split("_");
        long timeMillis = 0;
        if (tokens.length < 2){
            return timeMillis;
        }
        try {
            Date date = EXPORT_FILENAME_DATE_FORMAT.parse(tokens[0] + "_" + tokens[1]);
            timeMillis = date.getTime();
        } catch (ParseException e) {
            Log.e("Exporter", "Error parsing time from file name: " + e.getMessage());
            Crashlytics.logException(e);
        }
        return timeMillis;
    }

    /**
     * Generates the export output
     * @throws ExporterException if an error occurs during export
     */
    public abstract List<String> generateExport() throws ExporterException;

    /**
     * Recursively delete all files in a directory
     * @param directory File descriptor for directory
     */
    private void purgeDirectory(File directory){
        for (File file : directory.listFiles()) {
            if (file.isDirectory())
                purgeDirectory(file);
            else
                file.delete();
        }
    }

    /**
     * Returns the path to the file where the exporter should save the export during generation
     * <p>This path is a temporary cache file whose file extension matches the export format.<br>
     *     This file is deleted every time a new export is started</p>
     * @return Absolute path to file
     */
    protected String getExportCacheFilePath(){
        // The file name contains a timestamp, so ensure it doesn't change with multiple calls to
        // avoid issues like #448
        if (mExportCacheFilePath == null) {
            String cachePath = mCacheDir.getAbsolutePath();
            if (!cachePath.endsWith("/"))
                cachePath += "/";
            String bookName = BooksDbAdapter.getInstance().getAttribute(mBookUID, DatabaseSchema.BookEntry.COLUMN_DISPLAY_NAME);
            mExportCacheFilePath = cachePath + buildExportFilename(mExportParams.getExportFormat(), bookName);
        }

        return mExportCacheFilePath;
    }

    /**
     * Returns that path to the export folder for the book with GUID {@code bookUID}.
     * This is the folder where exports like QIF and OFX will be saved for access by external programs
     * @param bookUID GUID of the book being exported. Each book has its own export path
     * @return Absolute path to export folder for active book
     */
    public static String getExportFolderPath(String bookUID){
        String path = BASE_FOLDER_PATH + "/" + bookUID + "/exports/";
        File file = new File(path);
        if (!file.exists())
            file.mkdirs();
        return path;
    }


    /**
     * Returns the MIME type for this exporter.
     * @return MIME type as string
     */
    public String getExportMimeType(){
        return "text/plain";
    }

    public static class ExporterException extends RuntimeException{

        public ExporterException(ExportParams params){
            super("Failed to generate export with parameters:  " + params.toString());
        }

        public ExporterException(@NonNull ExportParams params, @NonNull String msg) {
            super("Failed to generate export with parameters: " + params.toString() + " - " + msg);
        }

        public ExporterException(ExportParams params, Throwable throwable){
            super("Failed to generate " + params.getExportFormat().toString() +"-"+ throwable.getMessage(),
                    throwable);
        }
    }
}
