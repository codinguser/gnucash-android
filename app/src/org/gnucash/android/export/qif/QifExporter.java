/*
 * Copyright (c) 2013 - 2014 Ngewi Fet <ngewif@gmail.com>
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

import org.gnucash.android.app.GnuCashApplication;
import static org.gnucash.android.db.DatabaseSchema.*;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Transaction;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ngewi
 */
public class QifExporter extends Exporter{
    private List<Account> mAccountsList;

    public QifExporter(ExportParams params){
        super(params);
    }

    public QifExporter(ExportParams params,  SQLiteDatabase db){
        super(params, db);
    }

    private String generateQIF(){
        StringBuffer qifBuffer = new StringBuffer();

        List<String> exportedTransactions = new ArrayList<String>();
        for (Account account : mAccountsList) {
            if (account.getTransactionCount() == 0)
                continue;

            qifBuffer.append(account.toQIF(mParameters.shouldExportAllTransactions(), exportedTransactions) + "\n");

            //mark as exported
            mAccountsDbAdapter.markAsExported(account.getUID());
        }
        mAccountsDbAdapter.close();

        return qifBuffer.toString();
    }

    @Override
    public String generateExport() throws ExporterException {
        mAccountsList = mParameters.shouldExportAllTransactions() ?
                mAccountsDbAdapter.getAllAccounts() : mAccountsDbAdapter.getExportableAccounts();

        return generateQIF();
    }

    public void generateExport(Writer writer) throws ExporterException , IOException {
        final String newLine = "\n";
        TransactionsDbAdapter transactionsDbAdapter = new TransactionsDbAdapter(GnuCashApplication.getAppContext());
        try {
            Cursor cursor = transactionsDbAdapter.fetchTransactionsWithSplitsWithTransactionAccount(
                    new String[]{
                            TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID + " AS trans_uid",
                            TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TIMESTAMP + " AS trans_time",
                            TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_DESCRIPTION + " AS trans_desc",
                            SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_AMOUNT + " AS split_amount",
                            SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TYPE + " AS split_type",
                            SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_MEMO + " AS split_memo",
                            "trans_acct.trans_acct_balance AS trans_acct_balance",
                            "account1." + AccountEntry.COLUMN_UID + " AS acct1_uid",
                            "account1." + AccountEntry.COLUMN_FULL_NAME + " AS acct1_full_name",
                            "account1." + AccountEntry.COLUMN_CURRENCY + " AS acct1_currency",
                            "account1." + AccountEntry.COLUMN_TYPE + " AS acct1_type",
                            "account2." + AccountEntry.COLUMN_FULL_NAME + " AS acct2_full_name"
                    },
                    // no recurrence transactions
                    TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_RECURRENCE_PERIOD + " == 0 AND " +
                            // in qif, split from the one account entry is not recorded (will be auto balanced)
                            SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_ACCOUNT_UID + " != account1." + AccountEntry.COLUMN_UID +
                            (
                            mParameters.shouldExportAllTransactions() ?
                                    "" : " AND " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_EXPORTED + "== 0"
                            ),
                    "acct1_uid ASC, trans_uid ASC"
                    );
            try {
                String currentAccountUID = "";
                String currentTransactionUID = "";
                while (cursor.moveToNext()) {
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
                                    .append("Imbalance-")
                                    .append(cursor.getString(cursor.getColumnIndexOrThrow("acct1_currency")))
                                    .append(newLine);
                            writer.append(QifHelper.SPLIT_AMOUNT_PREFIX)
                                    .append(decimalImbalance.toPlainString())
                                    .append(newLine);
                        }
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
                    writer.append(QifHelper.SPLIT_AMOUNT_PREFIX)
                            .append(splitType.equals("DEBIT") ? "-" : "")
                            .append(cursor.getString(cursor.getColumnIndexOrThrow("split_amount")))
                            .append(newLine);
                }
                if (!currentTransactionUID.equals("")) {
                    // end last transaction
                    writer.append(QifHelper.ENTRY_TERMINATOR).append(newLine);
                }
            }
            finally {
                cursor.close();
            }
            ContentValues contentValues = new ContentValues();
            contentValues.put(TransactionEntry.COLUMN_EXPORTED, 1);
            transactionsDbAdapter.updateTransaction(contentValues, null, null);
        }
        finally {
            transactionsDbAdapter.close();
        }
    }
}
