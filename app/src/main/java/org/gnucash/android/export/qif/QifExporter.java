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

import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Currency;

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
    public QifExporter(ExportParams params){
        super(params, null);
    }

    @Override
    public void generateExport(Writer writer) throws ExporterException {
        final String newLine = "\n";
        TransactionsDbAdapter transactionsDbAdapter = mTransactionsDbAdapter;
        try {
            Cursor cursor = transactionsDbAdapter.fetchTransactionsWithSplitsWithTransactionAccount(
                    new String[]{
                            TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_UID + " AS trans_uid",
                            TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_TIMESTAMP + " AS trans_time",
                            TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_DESCRIPTION + " AS trans_desc",
                            SplitEntry.TABLE_NAME + "_" + SplitEntry.COLUMN_AMOUNT + " AS split_amount",
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
                            mParameters.shouldExportAllTransactions() ?
                                    "" : " AND " + TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_EXPORTED + "== 0"
                            ),
                    null,
                    // trans_time ASC : put transactions in time order
                    // trans_uid ASC  : put splits from the same transaction together
                   "acct1_currency ASC, trans_time ASC, trans_uid ASC"
                    );
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
        catch (IOException e)
        {
            throw new ExporterException(mParameters, e);
        }
    }
}
