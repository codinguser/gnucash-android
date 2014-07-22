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

package org.gnucash.android.export;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Base class for the different exporters
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public abstract class Exporter {
    protected ExportParams mParameters;

    /**
     * Adapter for retrieving accounts to export
     * Subclasses should close this object when they are done with exporting
     */
    protected AccountsDbAdapter mAccountsDbAdapter;
    protected Context mContext;

    public Exporter(ExportParams params){
        this.mParameters = params;
        mContext = GnuCashApplication.getAppContext();
        mAccountsDbAdapter = new AccountsDbAdapter(mContext);
    }

    /**
     * Overloaded constructor, provided the database object to use
     * @param params Export parameters
     * @param db Database from which to export (should be initialized and open)
     */
    public Exporter(ExportParams params, SQLiteDatabase db){
        this.mParameters = params;
        mAccountsDbAdapter = new AccountsDbAdapter(db);
        mContext = GnuCashApplication.getAppContext();
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
     * Generates the export output
     * @return Export output as String
     * @throws ExporterException if an error occurs during export
     */
    public abstract String generateExport() throws ExporterException;

    public static class ExporterException extends RuntimeException{

        public ExporterException(ExportParams params){
            super("Failed to generate " + params.getExportFormat().toString());
        }

        public ExporterException(ExportParams params, Throwable throwable){
            super("Failed to generate " + params.getExportFormat().toString(), throwable);
        }
    }
}
