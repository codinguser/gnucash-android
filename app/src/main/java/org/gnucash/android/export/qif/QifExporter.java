/*
 * Copyright (c) 2013 - 2014 Ngewi Fet <ngewif@gmail.com>
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
package org.gnucash.android.export.qif;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.util.PreferencesHelper;
import org.gnucash.android.util.TimestampHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static org.gnucash.android.db.DatabaseSchema.AccountEntry;
import static org.gnucash.android.db.DatabaseSchema.SplitEntry;
import static org.gnucash.android.db.DatabaseSchema.TransactionEntry;

/**
 * Exports the accounts and transactions in the database to the QIF format
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 * @author Yongxin Wang <fefe.wyx@gmail.com>
 */
public class QifExporter extends Exporter{
    /**
     * Initialize the exporter
     * @param params Export options
     */
    public QifExporter(ExportParams params){
        super(params, null);
        LOG_TAG = "OfxExporter";
    }

    /**
     * Initialize the exporter
     * @param params Options for export
     * @param db SQLiteDatabase to export
     */
    public QifExporter(ExportParams params, SQLiteDatabase db){
        super(params, db);
        LOG_TAG = "OfxExporter";
    }

    @Override
    public List<String> generateExport() throws ExporterException {
        final String newLine = "\n";
        TransactionsDbAdapter transactionsDbAdapter = mTransactionsDbAdapter;
        try {
            String lastExportTimeStamp = TimestampHelper.getUtcStringFromTimestamp(mExportParams.getExportStartTime());
            Cursor cursor = transactionsDbAdapter.fetchTransactionsWithSplitsWithTransactionAccount(
                    new String[]{
                            TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_UID + " AS trans_uid",
                            TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_TIMESTAMP + " AS trans_time",
                            TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_DESCRIPTION + " AS trans_desc",
                            SplitEntry.TABLE_NAME + "_" + SplitEntry.COLUMN_QUANTITY_NUM + " AS split_quantity_num",
                            SplitEntry.TABLE_NAME + "_" + SplitEntry.COLUMN_QUANTITY_DENOM + " AS split_quantity_denom",
                            SplitEntry.TABLE_NAME + "_" + SplitEntry.COLUMN_TYPE + " AS split_type",
                            SplitEntry.TABLE_NAME + "_" + SplitEntry.COLUMN_MEMO + " AS split_memo",
                            "trans_extra_info.trans_acct_balance AS trans_acct_balance",
                            "trans_extra_info.trans_split_count AS trans_split_count",
                            "account1." + AccountEntry.COLUMN_UID + " AS acct1_uid",
                            "account1." + AccountEntry.COLUMN_FULL_NAME + " AS acct1_full_name",
                            "account1." + AccountEntry.COLUMN_CURRENCY + " AS acct1_currency",
                            "account1." + AccountEntry.COLUMN_TYPE + " AS acct1_type",
                            AccountEntry.TABLE_NAME + "_" + AccountEntry.COLUMN_FULL_NAME + " AS acct2_full_name"
                    },
                    // no recurrence transactions
                    TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_TEMPLATE + " == 0 AND " +
                            // exclude transactions involving multiple currencies
                            "trans_extra_info.trans_currency_count = 1 AND " +
                            // in qif, split from the one account entry is not recorded (will be auto balanced)
                            "( " + AccountEntry.TABLE_NAME + "_" + AccountEntry.COLUMN_UID + " != account1." + AccountEntry.COLUMN_UID + " OR " +
                            // or if the transaction has only one split (the whole transaction would be lost if it is not selected)
                            "trans_split_count == 1 )" +
                            (
                                    " AND " + TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_MODIFIED_AT + " > \"" + lastExportTimeStamp + "\""
                            ),
                    null,
                    // trans_time ASC : put transactions in time order
                    // trans_uid ASC  : put splits from the same transaction together
                   "acct1_currency ASC, trans_time ASC, trans_uid ASC"
                    );

            File file = new File(getExportCacheFilePath());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));

            try {
                String currentCurrencyCode = "";
                String currentAccountUID = "";
                String currentTransactionUID = "";
                while (cursor.moveToNext()) {
                    String currencyCode = cursor.getString(cursor.getColumnIndexOrThrow("acct1_currency"));
                    String accountUID = cursor.getString(cursor.getColumnIndexOrThrow("acct1_uid"));
                    String transactionUID = cursor.getString(cursor.getColumnIndexOrThrow("trans_uid"));
                    if (!transactionUID.equals(currentTransactionUID)) {
                        if (!currentTransactionUID.equals("")) {
                            writer.append(QifHelper.ENTRY_TERMINATOR).append(newLine);
                            // end last transaction
                        }
                        if (!accountUID.equals(currentAccountUID)) {
                            // no need to end account
                            //if (!currentAccountUID.equals("")) {
                            //    // end last account
                            //}
                            if (!currencyCode.equals(currentCurrencyCode)) {
                                currentCurrencyCode = currencyCode;
                                writer.append(QifHelper.INTERNAL_CURRENCY_PREFIX)
                                        .append(currencyCode)
                                        .append(newLine);
                            }
                            // start new account
                            currentAccountUID = accountUID;
                            writer.append(QifHelper.ACCOUNT_HEADER).append(newLine);
                            writer.append(QifHelper.ACCOUNT_NAME_PREFIX)
                                    .append(cursor.getString(cursor.getColumnIndexOrThrow("acct1_full_name")))
                                    .append(newLine);
                            writer.append(QifHelper.ENTRY_TERMINATOR).append(newLine);
                            writer.append(QifHelper.getQifHeader(cursor.getString(cursor.getColumnIndexOrThrow("acct1_type"))))
                                    .append(newLine);
                        }
                        // start new transaction
                        currentTransactionUID = transactionUID;
                        writer.append(QifHelper.DATE_PREFIX)
                                .append(QifHelper.formatDate(cursor.getLong(cursor.getColumnIndexOrThrow("trans_time"))))
                                .append(newLine);
                        writer.append(QifHelper.MEMO_PREFIX)
                                .append(cursor.getString(cursor.getColumnIndexOrThrow("trans_desc")))
                                .append(newLine);
                        // deal with imbalance first
                        double imbalance = cursor.getDouble(cursor.getColumnIndexOrThrow("trans_acct_balance"));
                        BigDecimal decimalImbalance = BigDecimal.valueOf(imbalance).setScale(2, BigDecimal.ROUND_HALF_UP);
                        if (decimalImbalance.compareTo(BigDecimal.ZERO) != 0) {
                            writer.append(QifHelper.SPLIT_CATEGORY_PREFIX)
                                    .append(AccountsDbAdapter.getImbalanceAccountName(
                                            Currency.getInstance(cursor.getString(cursor.getColumnIndexOrThrow("acct1_currency")))
                                    ))
                                    .append(newLine);
                            writer.append(QifHelper.SPLIT_AMOUNT_PREFIX)
                                    .append(decimalImbalance.toPlainString())
                                    .append(newLine);
                        }
                    }
                    if (cursor.getInt(cursor.getColumnIndexOrThrow("trans_split_count")) == 1) {
                        // No other splits should be recorded if this is the only split.
                        continue;
                    }
                    // all splits
                    // amount associated with the header account will not be exported.
                    // It can be auto balanced when importing to GnuCash
                    writer.append(QifHelper.SPLIT_CATEGORY_PREFIX)
                            .append(cursor.getString(cursor.getColumnIndexOrThrow("acct2_full_name")))
                            .append(newLine);
                    String splitMemo = cursor.getString(cursor.getColumnIndexOrThrow("split_memo"));
                    if (splitMemo != null && splitMemo.length() > 0) {
                        writer.append(QifHelper.SPLIT_MEMO_PREFIX)
                                .append(splitMemo)
                                .append(newLine);
                    }
                    String splitType = cursor.getString(cursor.getColumnIndexOrThrow("split_type"));
                    Double quantity_num = cursor.getDouble(cursor.getColumnIndexOrThrow("split_quantity_num"));
                    int quantity_denom = cursor.getInt(cursor.getColumnIndexOrThrow("split_quantity_denom"));
                    int precision = 0;
                    switch (quantity_denom) {
                        case 0: // will sometimes happen for zero values
                            break;
                        case 1:
                            precision = 0;
                            break;
                        case 10:
                            precision = 1;
                            break;
                        case 100:
                            precision = 2;
                            break;
                        case 1000:
                            precision = 3;
                            break;
                        default:
                            throw new ExporterException(mExportParams, "split quantity has illegal denominator: "+ quantity_denom);
                    }
                    Double quantity = 0.0;
                    if (quantity_denom != 0) {
                        quantity = quantity_num / quantity_denom;
                    }
                    writer.append(QifHelper.SPLIT_AMOUNT_PREFIX)
                            .append(splitType.equals("DEBIT") ? "-" : "")
                            .append(String.format("%." + precision + "f", quantity))
                            .append(newLine);
                }
                if (!currentTransactionUID.equals("")) {
                    // end last transaction
                    writer.append(QifHelper.ENTRY_TERMINATOR).append(newLine);
                }
                writer.flush();
            } finally {
                cursor.close();
                writer.close();
            }

            ContentValues contentValues = new ContentValues();
            contentValues.put(TransactionEntry.COLUMN_EXPORTED, 1);
            transactionsDbAdapter.updateTransaction(contentValues, null, null);

            /// export successful
            PreferencesHelper.setLastExportTime(TimestampHelper.getTimestampFromNow());
            return splitQIF(file);
        } catch (IOException e) {
            throw new ExporterException(mExportParams, e);
        }
    }

    /**
     * Splits a Qif file into several ones for each currency.
     *
     * @param file File object of the Qif file to split.
     * @return a list of paths of the newly created Qif files.
     * @throws IOException if something went wrong while splitting the file.
     */
    public List<String> splitQIF(File file) throws IOException {
        // split only at the last dot
        String[] pathParts = file.getPath().split("(?=\\.[^\\.]+$)");
        ArrayList<String> splitFiles = new ArrayList<>();
        String line;
        BufferedReader in = new BufferedReader(new FileReader(file));
        BufferedWriter out = null;
        try {
            while ((line = in.readLine()) != null) {
                if (line.startsWith(QifHelper.INTERNAL_CURRENCY_PREFIX)) {
                    String currencyCode = line.substring(1);
                    if (out != null) {
                        out.close();
                    }
                    String newFileName = pathParts[0] + "_" + currencyCode + pathParts[1];
                    splitFiles.add(newFileName);
                    out = new BufferedWriter(new FileWriter(newFileName));
                } else {
                    if (out == null) {
                        throw new IllegalArgumentException(file.getPath() + " format is not correct");
                    }
                    out.append(line).append('\n');
                }
            }
        } finally {
            in.close();
            if (out != null) {
                out.close();
            }
        }
        return splitFiles;
    }

    /**
     * Returns the mime type for this Exporter.
     * @return MIME type as string
     */
    public String getExportMimeType(){
        return "text/plain";
    }
}
