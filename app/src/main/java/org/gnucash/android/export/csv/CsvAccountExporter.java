/*
 * Copyright (c) 2018 Semyannikov Gleb <nightdevgame@gmail.com>
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

package org.gnucash.android.export.csv;

import android.database.sqlite.SQLiteDatabase;

import com.crashlytics.android.Crashlytics;

import org.gnucash.android.R;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.model.Account;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Creates a GnuCash CSV account representation of the accounts and transactions
 *
 * @author Semyannikov Gleb <nightdevgame@gmail.com>
 */
public class CsvAccountExporter extends Exporter{
    private char mCsvSeparator;

    /**
     * Construct a new exporter with export parameters
     * @param params Parameters for the export
     */
    public CsvAccountExporter(ExportParams params) {
        super(params, null);
        mCsvSeparator = params.getCsvSeparator();
        LOG_TAG = "GncXmlExporter";
    }

    /**
     * Overloaded constructor.
     * Creates an exporter with an already open database instance.
     * @param params Parameters for the export
     * @param db SQLite database
     */
    public CsvAccountExporter(ExportParams params, SQLiteDatabase db) {
        super(params, db);
        mCsvSeparator = params.getCsvSeparator();
        LOG_TAG = "GncXmlExporter";
    }

    @Override
    public List<String> generateExport() throws ExporterException {
        String outputFile = getExportCacheFilePath();
        try (CsvWriter writer = new CsvWriter(new FileWriter(outputFile), mCsvSeparator + "")) {
            generateExport(writer);
        } catch (IOException ex){
            Crashlytics.log("Error exporting CSV");
            Crashlytics.logException(ex);
            throw new ExporterException(mExportParams, ex);
        }

        return Arrays.asList(outputFile);
    }

    /**
     * Writes out all the accounts in the system as CSV to the provided writer
     * @param csvWriter Destination for the CSV export
     * @throws ExporterException if an error occurred while writing to the stream
     */
    public void generateExport(final CsvWriter csvWriter) throws ExporterException {
        try {
            List<String> names = Arrays.asList(mContext.getResources().getStringArray(R.array.csv_account_headers));
            List<Account> accounts = mAccountsDbAdapter.getAllRecords();

            for(int i = 0; i < names.size(); i++) {
                csvWriter.writeToken(names.get(i));
            }

            csvWriter.newLine();
            for (Account account : accounts) {
                csvWriter.writeToken(account.getAccountType().toString());
                csvWriter.writeToken(account.getFullName());
                csvWriter.writeToken(account.getName());

                csvWriter.writeToken(null); //Account code
                csvWriter.writeToken(account.getDescription());
                csvWriter.writeToken(account.getColorHexString());
                csvWriter.writeToken(null); //Account notes

                csvWriter.writeToken(account.getCommodity().getCurrencyCode());
                csvWriter.writeToken("CURRENCY");
                csvWriter.writeToken(account.isHidden() ? "T" : "F");

                csvWriter.writeToken("F"); //Tax
                csvWriter.writeEndToken(account.isPlaceholderAccount() ? "T": "F");
            }
        } catch (IOException e) {
            Crashlytics.logException(e);
            throw new ExporterException(mExportParams, e);
        }
    }
}
