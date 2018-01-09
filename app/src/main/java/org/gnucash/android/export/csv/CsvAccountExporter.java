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

import org.gnucash.android.export.xml.*;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.db.adapter.CommoditiesDbAdapter;
import org.gnucash.android.db.adapter.RecurrenceDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.BaseModel;
import org.gnucash.android.model.Book;
import org.gnucash.android.model.Budget;
import org.gnucash.android.model.BudgetAmount;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.PeriodType;
import org.gnucash.android.model.Price;
import org.gnucash.android.model.Recurrence;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;
import org.gnucash.android.util.BookUtils;
import org.gnucash.android.util.TimestampHelper;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

import static org.gnucash.android.db.DatabaseSchema.ScheduledActionEntry;
import static org.gnucash.android.db.DatabaseSchema.SplitEntry;
import static org.gnucash.android.db.DatabaseSchema.TransactionEntry;

/**
 * Creates a GnuCash CSV account representation of the accounts and transactions
 *
 * @author Semyannikov Gleb <nightdevgame@gmail.com>
 */
public class CsvAccountExporter extends Exporter{

    /**
     * Root account for template accounts
     */
    private Account mRootTemplateAccount;
    private Map<String, Account> mTransactionToTemplateAccountMap = new TreeMap<>();
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

    public void generateExport(final CsvWriter writer) throws ExporterException {
        try {
            String separator = mCsvSeparator + "";
            List<String> names = new ArrayList<String>();
            names.add("type");
            names.add("full_name");
            names.add("name");
            names.add("code");
            names.add("description");
            names.add("color");
            names.add("notes");
            names.add("commoditym");
            names.add("commodityn");
            names.add("hidden");
            names.add("tax");
            names.add("place_holder");


            List<Transaction> transactions = mTransactionsDbAdapter.getAllTransactions();
            List<Budget> budgets = mBudgetsDbAdapter.getAllRecords();
            List<Account> accounts = mAccountsDbAdapter.getAllRecords();
            List<Commodity> commodities = mCommoditiesDbAdapter.getAllRecords();
            List<Price> prices =  mPricesDbAdapter.getAllRecords();
            List<ScheduledAction> scheduledActions = mScheduledActionDbAdapter.getAllRecords();
            List<Split> splits = mSplitsDbAdapter.getAllRecords();


            for(int i = 0; i < names.size(); i++) {
                writer.write(names.get(i) + separator);
            }
            writer.write("\n");
            for(int i = 0; i < accounts.size(); i++) {
                Account account = accounts.get(i);

                writer.write(account.getAccountType().toString() + separator);
                writer.write(account.getFullName() + separator);
                writer.write(account.getName() + separator);

                //Code
                writer.write(separator);

                writer.write(account.getDescription() + separator);
                writer.write(account.getColor() + separator);

                //Notes
                writer.write(separator);

                writer.write(account.getCommodity().getCurrencyCode() + separator);
                writer.write("CURRENCY" + separator);
                writer.write(account.isHidden()?"T":"F" + separator);

                //Not exactly
                writer.write("F" + separator);

                writer.write(account.isPlaceholderAccount()?"T":"F" + separator);


                //writer.write();

                writer.write("\n");
            }
        } catch (Exception e) {
            Crashlytics.logException(e);
            throw new ExporterException(mExportParams, e);
        }
    }
}
