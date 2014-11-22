package org.gnucash.android.export.log;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.export.qif.QifHelper;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;
import java.util.UUID;

public class LogExporter extends Exporter {

    public LogExporter(ExportParams params) {
        super(params);
    }

    @Override
    public void generateExport(Writer writer) throws ExporterException {
        TransactionsDbAdapter transactionsDbAdapter = new TransactionsDbAdapter(GnuCashApplication.getAppContext());
        try {
            Cursor cursor = transactionsDbAdapter.query (
                    "trans_split_acct, trans_extra_info ON trans_extra_info.trans_acct_t_uid = trans_split_acct." +
                            DatabaseSchema.TransactionEntry.TABLE_NAME + "_" + DatabaseSchema.TransactionEntry.COLUMN_UID,
                    new String[]{
                            "*"
                    },
                    // no recurrence transactions
                    DatabaseSchema.TransactionEntry.TABLE_NAME + "_" + DatabaseSchema.TransactionEntry.COLUMN_RECURRENCE_PERIOD + " == 0" +
                            (
                                    mParameters.shouldExportAllTransactions() ?
                                            "" : " AND " + DatabaseSchema.TransactionEntry.TABLE_NAME + "_" + DatabaseSchema.TransactionEntry.COLUMN_EXPORTED + " == 0"
                            ),
                    null,
                    null,
                    null,
                    DatabaseSchema.TransactionEntry.TABLE_NAME + "_" + DatabaseSchema.TransactionEntry.COLUMN_TIMESTAMP + " ASC, "
                        + DatabaseSchema.TransactionEntry.TABLE_NAME + "_" + DatabaseSchema.TransactionEntry.COLUMN_UID + " ASC"
            );
            try {
                String currentTransactionUID = "";
                String randomUID = "";
                writer.append(LogHelper.header).append(LogHelper.lineEnd);
                while (cursor.moveToNext()) {
                    String transactionUID = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.TABLE_NAME + "_" + DatabaseSchema.TransactionEntry.COLUMN_UID));
                    if (!currentTransactionUID.equals(transactionUID)) {
                        if (!currentTransactionUID.equals("")) {
                            writer.append(LogHelper.end).append(LogHelper.lineEnd);
                        }
                        currentTransactionUID = transactionUID;
                        randomUID = UUID.randomUUID().toString().replaceAll("-", "");
                        writer.append(LogHelper.start).append(LogHelper.lineEnd);
                    }
                    // mod
                    writer.append(LogHelper.LOG_COMMIT).append(LogHelper.separator);
                    // trans_guid
                    writer.append(randomUID).append(LogHelper.separator);
                    // split_guid
                    // String splitUID = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.SplitEntry.TABLE_NAME + "_" + DatabaseSchema.SplitEntry.COLUMN_UID));
                    writer.append(UUID.randomUUID().toString().replaceAll("-", "")).append(LogHelper.separator);
                    // time_now
                    writer.append(LogHelper.getLogFormattedTime((new Date()).getTime())).append(LogHelper.separator);
                    // date_entered
                    String transTime = LogHelper.getLogFormattedTime(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.TABLE_NAME + "_" + DatabaseSchema.TransactionEntry.COLUMN_TIMESTAMP)));
                    writer.append(transTime).append(LogHelper.separator);
                    // date_posted
                    writer.append(transTime).append(LogHelper.separator);
                    // acc_guid
                    String accountUID = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.TABLE_NAME + "_" + DatabaseSchema.AccountEntry.COLUMN_UID));
                    writer.append(accountUID).append(LogHelper.separator);
                    // acc_name
                    String accountName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.TABLE_NAME + "_" + DatabaseSchema.AccountEntry.COLUMN_NAME));
                    writer.append(accountName).append(LogHelper.separator);
                    // num
                    writer.append(LogHelper.separator);
                    // description
                    String description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.TABLE_NAME + "_" + DatabaseSchema.TransactionEntry.COLUMN_DESCRIPTION));
                    writer.append(description).append(LogHelper.separator);
                    // notes
                    String notes = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.TABLE_NAME + "_" + DatabaseSchema.TransactionEntry.COLUMN_NOTES));
                    writer.append(notes==null?"":notes).append(LogHelper.separator);
                    // memo
                    String memo = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.SplitEntry.TABLE_NAME + "_" + DatabaseSchema.SplitEntry.COLUMN_MEMO));
                    writer.append(memo==null?"":memo).append(LogHelper.separator);
                    // action
                    writer.append(LogHelper.separator);
                    // reconciled
                    writer.append("n" + LogHelper.separator);
                    // amount
                    String currency = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.TABLE_NAME + "_" + DatabaseSchema.TransactionEntry.COLUMN_CURRENCY));
                    Currency trxCurrency = Currency.getInstance(currency);
                    int fractionDigits = trxCurrency.getDefaultFractionDigits();
                    int denomInt;
                    denomInt = (int) Math.pow(10, fractionDigits);
                    BigDecimal denom = new BigDecimal(denomInt);
                    String denomString = Integer.toString(denomInt);
                    String trxType = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.SplitEntry.TABLE_NAME + "_" + DatabaseSchema.SplitEntry.COLUMN_TYPE));
                    BigDecimal value = new BigDecimal(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.SplitEntry.TABLE_NAME + "_" + DatabaseSchema.SplitEntry.COLUMN_AMOUNT)));
                    value = value.multiply(denom);
                    String strValue = (trxType.equals("CREDIT") ? "-" : "") + value.stripTrailingZeros().toPlainString() + "/" + denomString;
                    writer.append(strValue).append(LogHelper.separator);
                    // value
                    writer.append(strValue).append(LogHelper.separator);
                    // date_reconciled
                    writer.append(LogHelper.DATE_RECONCILE).append(LogHelper.lineEnd);
                }
                writer.append(LogHelper.end);
            }
            finally {
                cursor.close();
            }
            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseSchema.TransactionEntry.COLUMN_EXPORTED, 1);
            transactionsDbAdapter.updateTransaction(contentValues, null, null);
        }
        catch (IOException e)
        {
            throw new ExporterException(mParameters, e);
        }
        finally {
            transactionsDbAdapter.close();
        }
    }
}
