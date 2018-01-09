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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

import static org.gnucash.android.db.DatabaseSchema.ScheduledActionEntry;
import static org.gnucash.android.db.DatabaseSchema.SplitEntry;
import static org.gnucash.android.db.DatabaseSchema.TransactionEntry;

/**
 * Creates a GnuCash CSV transactions representation of the accounts and transactions
 *
 * @author Semyannikov Gleb <nightdevgame@gmail.com>
 */
public class CsvTransactionsExporter extends Exporter{

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
            //List<Budget> budgets = mBudgetsDbAdapter.getAllRecords();
            //List<Account> accounts = mAccountsDbAdapter.getAllRecords();
            //List<Commodity> commodities = mCommoditiesDbAdapter.getAllRecords();
            //List<Price> prices =  mPricesDbAdapter.getAllRecords();
            //List<ScheduledAction> scheduledActions = mScheduledActionDbAdapter.getAllRecords();
            //List<Split> splits = mSplitsDbAdapter.getAllRecords();


            for(int i = 0; i < names.size(); i++) {
                writer.write(names.get(i) + separator);
            }
            writer.write("\n");
            for(int i = 0; i < transactions.size(); i++) {
                Transaction transaction = transactions.get(i);
                List<Split> splits = transaction.getSplits();
                for (int j = 0; j < splits.size()/2; j++) {
                    Split split = splits.get(j);
                    Split pair = null;
                    for (int k = 0; k < splits.size(); k++) {
                        if (split.isPairOf(splits.get(k))) {
                            pair = splits.get(k);
                        }
                    }

                    Account account = mAccountsDbAdapter.getRecord(split.getAccountUID());
                    Account account_pair = null;
                    if (pair != null) {
                        account_pair = mAccountsDbAdapter.getRecord(pair.getAccountUID());
                    }

                    Date date = new Date(transaction.getTimeMillis());
                    DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
                    writer.write(df.format(date) + separator);

                    writer.write(account.getName() + separator);

                    //Number
                    writer.write(separator);

                    writer.write(transaction.getDescription() + separator);

                    writer.write(transaction.getNote() + separator);

                    writer.write((split.getMemo()==null?"":split.getMemo()) + separator);

                    writer.write((account_pair.getName()==null?"":account_pair.getName()) + separator);

                    writer.write((split.getType().name()) + separator);

                    //Action
                    writer.write(separator);

                    writer.write(split.getReconcileState() + separator);

                    writer.write(split.getFormattedQuantity().toPlainString() + separator);
                    writer.write(separator);
                    writer.write(split.getFormattedQuantity().toPlainString() + separator);
                    writer.write(separator);
                    writer.write(separator);
                    writer.write("\n");
                }
            }

        } catch (Exception e) {
            Crashlytics.logException(e);
            throw new ExporterException(mExportParams, e);
        }
    }
}
