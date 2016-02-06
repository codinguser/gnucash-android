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
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.CommoditiesDbAdapter;
import org.gnucash.android.db.PricesDbAdapter;
import org.gnucash.android.db.ScheduledActionDbAdapter;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;

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
     */
    private static final String BASE_FOLDER_PATH = Environment.getExternalStorageDirectory() + "/" + BuildConfig.APPLICATION_ID;

    /**
     * Folder where exports like QIF and OFX will be saved for access by external programs
     */
    public static final String EXPORT_FOLDER_PATH =  BASE_FOLDER_PATH + "/exports/";

    /**
     * Folder where XML backups will be saved
     */
    public static final String BACKUP_FOLDER_PATH = BASE_FOLDER_PATH + "/backups/";

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
    protected final Context mContext;
    private String mExportCacheFilePath;

    public Exporter(ExportParams params, SQLiteDatabase db) {
        this.mExportParams = params;
        mContext = GnuCashApplication.getAppContext();
        if (db == null) {
            mAccountsDbAdapter = AccountsDbAdapter.getInstance();
            mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();
            mSplitsDbAdapter = SplitsDbAdapter.getInstance();
            mScheduledActionDbAdapter = ScheduledActionDbAdapter.getInstance();
            mPricesDbAdapter = PricesDbAdapter.getInstance();
            mCommoditiesDbAdapter = CommoditiesDbAdapter.getInstance();
        } else {
            mSplitsDbAdapter = new SplitsDbAdapter(db);
            mTransactionsDbAdapter = new TransactionsDbAdapter(db, mSplitsDbAdapter);
            mAccountsDbAdapter = new AccountsDbAdapter(db, mTransactionsDbAdapter);
            mScheduledActionDbAdapter = new ScheduledActionDbAdapter(db);
            mPricesDbAdapter = new PricesDbAdapter(db);
            mCommoditiesDbAdapter = new CommoditiesDbAdapter(db);
        }

        mExportCacheFilePath = null;
        mCacheDir = new File(mContext.getCacheDir(), params.getExportFormat().name());
        mCacheDir.mkdir();
        purgeDirectory(mCacheDir);
    }

    /**
     * Builds a file name based on the current time stamp for the exported file
     * @return String containing the file name
     */
    public static String buildExportFilename(ExportFormat format) {
        return EXPORT_FILENAME_DATE_FORMAT.format(new Date(System.currentTimeMillis()))
                + "_gnucash_export" + format.getExtension();
    }

    /**
     * Parses the name of an export file and returns the date of export
     * @param filename Export file name generated by {@link #buildExportFilename(ExportFormat)}
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
            mExportCacheFilePath = cachePath + buildExportFilename(mExportParams.getExportFormat());
        }

        return mExportCacheFilePath;
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
