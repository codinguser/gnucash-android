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

import com.crashlytics.android.Crashlytics;

import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Creates a GnuCash CSV transactions representation of the accounts and transactions
 *
 * @author Semyannikov Gleb <nightdevgame@gmail.com>
 */
public class CsvTransactionsExporter extends Exporter{

    private char mCsvSeparator;

    private DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);

    private Comparator<Split> splitComparator = new Comparator<Split>() {
        @Override
        public int compare(Split o1, Split o2) {
            if(o1.getType() == TransactionType.DEBIT
                    && o2.getType() == TransactionType.CREDIT)
                return -1;
            if (o1.getType() == TransactionType.CREDIT
                    && o2.getType() == TransactionType.DEBIT)
                return 1;
            return 0;
        }
    };

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
        OutputStreamWriter writerStream = null;
        CsvWriter writer = null;
        String outputFile = getExportCacheFilePath();
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            writerStream = new OutputStreamWriter(bufferedOutputStream);
            writer = new CsvWriter(writerStream);
            generateExport(writer);
        } catch (IOException ex){
            Crashlytics.log("Error exporting CSV");
            Crashlytics.logException(ex);
        } finally {
            if (writerStream != null) {
                try {
                    writerStream.close();
                } catch (IOException e) {
                    throw new ExporterException(mExportParams, e);
                }
            }
        }

        List<String> exportedFiles = new ArrayList<>();
        exportedFiles.add(outputFile);

        return exportedFiles;
    }

    private void write_split(final Transaction transaction, final Split split, final CsvWriter writer) throws IOException
    {
        String separator = mCsvSeparator + "";
        Account account = mAccountsDbAdapter.getRecord(split.getAccountUID());

        // Date
        Date date = new Date(transaction.getTimeMillis());
        writer.write(dateFormat.format(date) + separator);
        // Account name
        writer.write(account.getName() + separator);
        // TODO:Number is not defined yet?
        writer.write( separator);
        // Description
        writer.write(transaction.getDescription() + separator);
        // Notes of transaction
        writer.write(transaction.getNote() + separator);
        // Memo
        writer.write(
                (split.getMemo()==null?
                "":split.getMemo()) + separator);
        // TODO:Category is not defined yet?
        writer.write(separator);
        // Type
        writer.write(split.getType().name() + separator);
        // TODO:Action is not defined yet?
        writer.write(separator);
        // Reconcile
        writer.write(split.getReconcileState() + separator);

        // Changes
        Money change = split.getFormattedQuantity().withCurrency(transaction.getCommodity());
        Money zero = Money.getZeroInstance().withCurrency(transaction.getCommodity());
        // To currency; From currency; To; From
        if (change.isNegative()) {
            writer.write(zero.toPlainString() + separator);
            writer.write(change.abs().toPlainString() + separator);
            writer.write(Money.getZeroInstance().toPlainString() + separator);
            writer.write(split.getFormattedQuantity().abs().toPlainString() + separator);
        }
        else {
            writer.write(change.abs().toPlainString() + separator);
            writer.write(zero.toPlainString() + separator);
            writer.write(split.getFormattedQuantity().abs().toPlainString() + separator);
            writer.write(Money.getZeroInstance().toPlainString() + separator);
        }

        // TODO: What is price?
        writer.write(separator);
        writer.write(separator);
    }

    public void generateExport(final CsvWriter writer) throws ExporterException {
        try {
            String separator = mCsvSeparator + "";
            List<String> names = new ArrayList<String>();
            names.add("Date");
            names.add("Account name");
            names.add("Number");
            names.add("Description");
            names.add("Notes");
            names.add("Memo");
            names.add("Category");
            names.add("Type");
            names.add("Action");
            names.add("Reconcile");
            names.add("To With Sym");
            names.add("From With Sym");
            names.add("To Num.");
            names.add("From Num.");
            names.add("To Rate/Price");
            names.add("From Rate/Price");

            List<Transaction> transactions = mTransactionsDbAdapter.getAllTransactions();

            for(int i = 0; i < names.size(); i++) {
                writer.write(names.get(i) + separator);
            }
            writer.write("\n");


            Cursor cursor = mTransactionsDbAdapter.fetchAllRecords();
            while (cursor.moveToNext())
            {
                Transaction transaction = mTransactionsDbAdapter.buildModelInstance(cursor);
                List<Split> splits = transaction.getSplits();
                Collections.sort(splits,splitComparator);
                for (int j = 0; j < splits.size()/2; j++) {
                    Split split = splits.get(j);
                    Split pair = null;
                    for (int k = 0; k < splits.size(); k++) {
                        if (split.isPairOf(splits.get(k))) {
                            pair = splits.get(k);
                        }
                    }

                    write_split(transaction, split, writer);
                    writer.write("\n");
                    if (pair != null) {
                        write_split(transaction, pair, writer);
                        writer.write("\n");
                    }
                }
            }

        } catch (Exception e) {
            Crashlytics.logException(e);
            throw new ExporterException(mExportParams, e);
        }
    }
}
