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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.gnucash.android.R;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;
import org.gnucash.android.util.PreferencesHelper;
import org.gnucash.android.util.TimestampHelper;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Creates a GnuCash CSV transactions representation of the accounts and transactions
 *
 * @author Semyannikov Gleb <nightdevgame@gmail.com>
 */
public class CsvTransactionsExporter extends Exporter{

    private char mCsvSeparator;

    private DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd", Locale.US);

    /**
     * Construct a new exporter with export parameters
     * @param params Parameters for the export
     */
    public CsvTransactionsExporter(ExportParams params) {
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
    public CsvTransactionsExporter(ExportParams params, SQLiteDatabase db) {
        super(params, db);
        mCsvSeparator = params.getCsvSeparator();
        LOG_TAG = "GncXmlExporter";
    }

    @Override
    public List<String> generateExport() throws ExporterException {
        String outputFile = getExportCacheFilePath();

        try (CsvWriter csvWriter = new CsvWriter(new FileWriter(outputFile), "" + mCsvSeparator)){
            generateExport(csvWriter);
        } catch (IOException ex){
            Crashlytics.log("Error exporting CSV");
            Crashlytics.logException(ex);
            throw new ExporterException(mExportParams, ex);
        }

        return Arrays.asList(outputFile);
    }

    /**
     * Write splits to CSV format
     * @param splits Splits to be written
     */
    private void writeSplitsToCsv(@NonNull List<Split> splits, @NonNull CsvWriter writer,
			Map<String, String> accountNames, Map<String, String> accountFullNames) throws IOException {
        int index = 0;

        for (Split split : splits) {
            if (index++ > 0){ // the first split is on the same line as the transactions. But after that, we
                writer.write("" + mCsvSeparator + mCsvSeparator + mCsvSeparator + mCsvSeparator
                        + mCsvSeparator + mCsvSeparator + mCsvSeparator + mCsvSeparator);
            }
            writer.writeToken(split.getMemo());

            String accountUID = split.getAccountUID();

            // Cache account names
            String fullName, name;
            if (accountNames.containsKey(accountUID)) {
                fullName = accountFullNames.get(accountUID);
                name = accountNames.get(accountUID);
            } else {
                fullName = mAccountsDbAdapter.getAccountFullName(accountUID);
                name = mAccountsDbAdapter.getAccountName(accountUID);
                accountFullNames.put(accountUID, fullName);
                accountNames.put(accountUID, name);
            }

            writer.writeToken(fullName);
            writer.writeToken(name);

            String sign = split.getType() == TransactionType.CREDIT ? "-" : "";
            writer.writeToken(sign + split.getQuantity().formattedString());
            writer.writeToken(sign + split.getQuantity().toLocaleString());
            writer.writeToken("" + split.getReconcileState());
            if (split.getReconcileState() == Split.FLAG_RECONCILED) {
                String recDateString = dateFormat.format(new Date(split.getReconcileDate().getTime()));
                writer.writeToken(recDateString);
            } else {
                writer.writeToken(null);
            }
            writer.writeEndToken(split.getQuantity().divide(split.getValue()).toLocaleString());
        }
    }

    private void generateExport(final CsvWriter csvWriter) throws ExporterException {
        try {
            List<String> names = Arrays.asList(mContext.getResources().getStringArray(R.array.csv_transaction_headers));
            for(int i = 0; i < names.size(); i++) {
                csvWriter.writeToken(names.get(i));
            }
            csvWriter.newLine();

            Map<String, String> nameCache = new HashMap<>();
            Map<String, String> fullNameCache = new HashMap<>();

            Cursor cursor = mTransactionsDbAdapter.fetchTransactionsModifiedSince(mExportParams.getExportStartTime());
            Log.d(LOG_TAG, String.format("Exporting %d transactions to CSV", cursor.getCount()));
            while (cursor.moveToNext()){
                Transaction transaction = mTransactionsDbAdapter.buildModelInstance(cursor);
                Date date = new Date(transaction.getTimeMillis());
                csvWriter.writeToken(dateFormat.format(date));
                csvWriter.writeToken(transaction.getUID());
                csvWriter.writeToken(null);  //Transaction number

                csvWriter.writeToken(transaction.getDescription());
                csvWriter.writeToken(transaction.getNote());

                csvWriter.writeToken("CURRENCY::" + transaction.getCurrencyCode());
                csvWriter.writeToken(null); // Void Reason
                csvWriter.writeToken(null); // Action
                writeSplitsToCsv(transaction.getSplits(), csvWriter, nameCache, fullNameCache);
            }

            PreferencesHelper.setLastExportTime(TimestampHelper.getTimestampFromNow());
        } catch (IOException e) {
            Crashlytics.logException(e);
            throw new ExporterException(mExportParams, e);
        }
    }
}
