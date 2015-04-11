/*
 * Copyright (c) 2014 - 2015 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.export.xml;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.model.TransactionType;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import static org.gnucash.android.db.DatabaseSchema.ScheduledActionEntry;
import static org.gnucash.android.db.DatabaseSchema.SplitEntry;
import static org.gnucash.android.db.DatabaseSchema.TransactionEntry;

/**
 * Creates a GnuCash XML representation of the accounts and transactions
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 * @author Yongxin Wang <fefe.wyx@gmail.com>
 */
public class GncXmlExporter extends Exporter{

    /**
     * Construct a new exporter with export parameters
     * @param params Parameters for the export
     */
    public GncXmlExporter(ExportParams params) {
        super(params, null);
    }

    /**
     * Overloaded constructor.
     * Creates an exporter with an already open database instance.
     * @param params Parameters for the export
     * @param db SQLite database
     */
    public GncXmlExporter(ExportParams params, SQLiteDatabase db) {
        super(params, db);
    }

    private void exportSlots(XmlSerializer xmlSerializer,
                             List<String> slotKey,
                             List<String> slotType,
                             List<String> slotValue) throws IOException {
        if (slotKey == null || slotType == null || slotValue == null ||
                slotKey.size() == 0 || slotType.size() != slotKey.size() || slotValue.size() != slotKey.size()) {
            return;
        }

        for (int i = 0; i < slotKey.size(); i++) {
            xmlSerializer.startTag(null, GncXmlHelper.TAG_SLOT);
            xmlSerializer.startTag(null, GncXmlHelper.TAG_SLOT_KEY);
            xmlSerializer.text(slotKey.get(i));
            xmlSerializer.endTag(null, GncXmlHelper.TAG_SLOT_KEY);
            xmlSerializer.startTag(null, GncXmlHelper.TAG_SLOT_VALUE);
            xmlSerializer.attribute(null, GncXmlHelper.ATTR_KEY_TYPE, slotType.get(i));
            xmlSerializer.text(slotValue.get(i));
            xmlSerializer.endTag(null, GncXmlHelper.TAG_SLOT_VALUE);
            xmlSerializer.endTag(null, GncXmlHelper.TAG_SLOT);
        }
    }

    private void exportAccounts(XmlSerializer xmlSerializer) throws IOException {
        Cursor cursor = mAccountsDbAdapter.fetchAccounts(null, null, null);
        while (cursor.moveToNext()) {
            // write account
            xmlSerializer.startTag(null, GncXmlHelper.TAG_ACCOUNT);
            xmlSerializer.attribute(null, GncXmlHelper.ATTR_KEY_VERSION, GncXmlHelper.BOOK_VERSION);
            // account name
            xmlSerializer.startTag(null, GncXmlHelper.TAG_NAME);
            xmlSerializer.text(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_NAME)));
            xmlSerializer.endTag(null, GncXmlHelper.TAG_NAME);
            // account guid
            xmlSerializer.startTag(null, GncXmlHelper.TAG_ACCT_ID);
            xmlSerializer.attribute(null, GncXmlHelper.ATTR_KEY_TYPE, GncXmlHelper.ATTR_VALUE_GUID);
            xmlSerializer.text(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_UID)));
            xmlSerializer.endTag(null, GncXmlHelper.TAG_ACCT_ID);
            // account type
            xmlSerializer.startTag(null, GncXmlHelper.TAG_TYPE);
            String acct_type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_TYPE));
            xmlSerializer.text(acct_type);
            xmlSerializer.endTag(null, GncXmlHelper.TAG_TYPE);
            // commodity
            xmlSerializer.startTag(null, GncXmlHelper.TAG_COMMODITY);
            xmlSerializer.startTag(null, GncXmlHelper.TAG_COMMODITY_SPACE);
            xmlSerializer.text("ISO4217");
            xmlSerializer.endTag(null, GncXmlHelper.TAG_COMMODITY_SPACE);
            xmlSerializer.startTag(null, GncXmlHelper.TAG_COMMODITY_ID);
            String acctCurrencyCode = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_CURRENCY));
            xmlSerializer.text(acctCurrencyCode);
            xmlSerializer.endTag(null, GncXmlHelper.TAG_COMMODITY_ID);
            xmlSerializer.endTag(null, GncXmlHelper.TAG_COMMODITY);
            // commodity scu
            xmlSerializer.startTag(null, GncXmlHelper.TAG_COMMODITY_SCU);
            xmlSerializer.text(Integer.toString((int) Math.pow(10, Currency.getInstance(acctCurrencyCode).getDefaultFractionDigits())));
            xmlSerializer.endTag(null, GncXmlHelper.TAG_COMMODITY_SCU);
            // account description
            // this is optional in Gnc XML, and currently not in the db, so description node
            // is omitted
            //
            // account slots, color, placeholder, default transfer account, favorite
            ArrayList<String> slotKey = new ArrayList<String>();
            ArrayList<String> slotType = new ArrayList<String>();
            ArrayList<String> slotValue = new ArrayList<String>();
            slotKey.add(GncXmlHelper.KEY_PLACEHOLDER);
            slotType.add(GncXmlHelper.ATTR_VALUE_STRING);
            slotValue.add(Boolean.toString(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_PLACEHOLDER)) != 0));

            String color = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_COLOR_CODE));
            if (color != null && color.length() > 0) {
                slotKey.add(GncXmlHelper.KEY_COLOR);
                slotType.add(GncXmlHelper.ATTR_VALUE_STRING);
                slotValue.add(color);
            }

            String defaultTransferAcctUID = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID));
            if (defaultTransferAcctUID != null && defaultTransferAcctUID.length() > 0) {
                slotKey.add(GncXmlHelper.KEY_DEFAULT_TRANSFER_ACCOUNT);
                slotType.add(GncXmlHelper.ATTR_VALUE_STRING);
                slotValue.add(defaultTransferAcctUID);
            }

            slotKey.add(GncXmlHelper.KEY_FAVORITE);
            slotType.add(GncXmlHelper.ATTR_VALUE_STRING);
            slotValue.add(Boolean.toString(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_FAVORITE)) != 0));

            xmlSerializer.startTag(null, GncXmlHelper.TAG_ACT_SLOTS);
            exportSlots(xmlSerializer, slotKey, slotType, slotValue);
            xmlSerializer.endTag(null, GncXmlHelper.TAG_ACT_SLOTS);

            // parent uid
            String parentUID = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_PARENT_ACCOUNT_UID));
            if (!acct_type.equals("ROOT") && parentUID != null && parentUID.length() > 0) {
                xmlSerializer.startTag(null, GncXmlHelper.TAG_PARENT_UID);
                xmlSerializer.attribute(null, GncXmlHelper.ATTR_KEY_TYPE, GncXmlHelper.ATTR_VALUE_STRING);
                xmlSerializer.text(parentUID);
                xmlSerializer.endTag(null, GncXmlHelper.TAG_PARENT_UID);
            } else {
                Log.d("export", "root account : " + cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_UID)));
            }
            xmlSerializer.endTag(null, GncXmlHelper.TAG_ACCOUNT);
        }
        cursor.close();
    }

    /**
     * Serializes transactions from the database to XML
     * @param xmlSerializer XML serializer
     * @param exportTemplates Flag whether to export templates or normal transactions
     * @throws IOException if the XML serializer cannot be written to
     */
    private void exportTransactions(XmlSerializer xmlSerializer, boolean exportTemplates) throws IOException {
        String where = null;
        if (exportTemplates){
            where = TransactionEntry.TABLE_NAME+"."+TransactionEntry.COLUMN_TEMPLATE + "=0";
        }
        Cursor cursor = mTransactionsDbAdapter.fetchTransactionsWithSplits(
                new String[]{
                        TransactionEntry.TABLE_NAME+"."+ TransactionEntry.COLUMN_UID + " AS trans_uid",
                        TransactionEntry.TABLE_NAME+"."+ TransactionEntry.COLUMN_DESCRIPTION + " AS trans_desc",
                        TransactionEntry.TABLE_NAME+"."+ TransactionEntry.COLUMN_NOTES + " AS trans_notes",
                        TransactionEntry.TABLE_NAME+"."+ TransactionEntry.COLUMN_TIMESTAMP + " AS trans_time",
                        TransactionEntry.TABLE_NAME+"."+ TransactionEntry.COLUMN_EXPORTED + " AS trans_exported",
                        TransactionEntry.TABLE_NAME+"."+ TransactionEntry.COLUMN_CURRENCY + " AS trans_currency",
                        TransactionEntry.TABLE_NAME+"."+ TransactionEntry.COLUMN_CREATED_AT + " AS trans_date_posted",
                        TransactionEntry.TABLE_NAME+"."+ TransactionEntry.COLUMN_SCHEDX_ACTION_UID + " AS trans_from_sched_action",
                        SplitEntry.TABLE_NAME+"."+ SplitEntry.COLUMN_UID + " AS split_uid",
                        SplitEntry.TABLE_NAME+"."+ SplitEntry.COLUMN_MEMO + " AS split_memo",
                        SplitEntry.TABLE_NAME+"."+ SplitEntry.COLUMN_TYPE + " AS split_type",
                        SplitEntry.TABLE_NAME+"."+ SplitEntry.COLUMN_AMOUNT + " AS split_amount",
                        SplitEntry.TABLE_NAME+"."+ SplitEntry.COLUMN_ACCOUNT_UID + " AS split_acct_uid"},
                        where, null,
                        TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TIMESTAMP + " ASC , " +
                        TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID + " ASC ");

        String lastTrxUID = "";
        Currency trxCurrency;
        int fractionDigits;
        BigDecimal denom = new BigDecimal(100);
        String denomString = "100";
        while (cursor.moveToNext()){
            String curTrxUID = cursor.getString(cursor.getColumnIndexOrThrow("trans_uid"));
            if (!lastTrxUID.equals(curTrxUID)) { // new transaction starts
                if (!lastTrxUID.equals("")) { // there's an old transaction, close it
                    xmlSerializer.endTag(null, GncXmlHelper.TAG_TRN_SPLITS);
                    xmlSerializer.endTag(null, GncXmlHelper.TAG_TRANSACTION);
                }
                // new transaction
                xmlSerializer.startTag(null, GncXmlHelper.TAG_TRANSACTION);
                xmlSerializer.attribute(null, GncXmlHelper.ATTR_KEY_VERSION, GncXmlHelper.BOOK_VERSION);
                // transaction id
                xmlSerializer.startTag(null, GncXmlHelper.TAG_TRX_ID);
                xmlSerializer.attribute(null, GncXmlHelper.ATTR_KEY_TYPE, GncXmlHelper.ATTR_VALUE_GUID);
                xmlSerializer.text(curTrxUID);
                xmlSerializer.endTag(null, GncXmlHelper.TAG_TRX_ID);
                // currency
                String currency = cursor.getString(cursor.getColumnIndexOrThrow("trans_currency"));
                trxCurrency = Currency.getInstance(currency);
                fractionDigits = trxCurrency.getDefaultFractionDigits();
                int denomInt;
                denomInt = (int) Math.pow(10, fractionDigits);
                denom = new BigDecimal(denomInt);
                denomString = Integer.toString(denomInt);
                xmlSerializer.startTag(null, GncXmlHelper.TAG_TRX_CURRENCY);
                xmlSerializer.startTag(null, GncXmlHelper.TAG_COMMODITY_SPACE);
                xmlSerializer.text("ISO4217");
                xmlSerializer.endTag(null, GncXmlHelper.TAG_COMMODITY_SPACE);
                xmlSerializer.startTag(null, GncXmlHelper.TAG_COMMODITY_ID);
                xmlSerializer.text(currency);
                xmlSerializer.endTag(null, GncXmlHelper.TAG_COMMODITY_ID);
                xmlSerializer.endTag(null, GncXmlHelper.TAG_TRX_CURRENCY);
                // date posted, time which user put on the transaction
                String strDate = GncXmlHelper.formatDate(cursor.getLong(cursor.getColumnIndexOrThrow("trans_time")));
                xmlSerializer.startTag(null, GncXmlHelper.TAG_DATE_POSTED);
                xmlSerializer.startTag(null, GncXmlHelper.TAG_DATE);
                xmlSerializer.text(strDate);
                xmlSerializer.endTag(null, GncXmlHelper.TAG_DATE);
                xmlSerializer.endTag(null, GncXmlHelper.TAG_DATE_POSTED);

                // date entered, time when the transaction was actually created
                Timestamp timeEntered = Timestamp.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("trans_date_posted")));
                String dateEntered = GncXmlHelper.formatDate(timeEntered.getTime());
                xmlSerializer.startTag(null, GncXmlHelper.TAG_DATE_ENTERED);
                xmlSerializer.startTag(null, GncXmlHelper.TAG_DATE);
                xmlSerializer.text(dateEntered);
                xmlSerializer.endTag(null, GncXmlHelper.TAG_DATE);
                xmlSerializer.endTag(null, GncXmlHelper.TAG_DATE_ENTERED);

                // description
                xmlSerializer.startTag(null, GncXmlHelper.TAG_TRN_DESCRIPTION);
                xmlSerializer.text(cursor.getString(cursor.getColumnIndexOrThrow("trans_desc")));
                xmlSerializer.endTag(null, GncXmlHelper.TAG_TRN_DESCRIPTION);
                lastTrxUID = curTrxUID;
                // slots
                ArrayList<String> slotKey = new ArrayList<String>();
                ArrayList<String> slotType = new ArrayList<String>();
                ArrayList<String> slotValue = new ArrayList<String>();

                String notes = cursor.getString(cursor.getColumnIndexOrThrow("trans_notes"));
                boolean exported = cursor.getInt(cursor.getColumnIndexOrThrow("trans_exported")) == 1;
                if (notes != null && notes.length() > 0) {
                    slotKey.add(GncXmlHelper.KEY_NOTES);
                    slotType.add(GncXmlHelper.ATTR_VALUE_STRING);
                    slotValue.add(notes);
                }
                if (!exported) {
                    slotKey.add(GncXmlHelper.KEY_EXPORTED);
                    slotType.add(GncXmlHelper.ATTR_VALUE_STRING);
                    slotValue.add("false");
                }

                String scheduledActionUID = cursor.getString(cursor.getColumnIndexOrThrow("trans_from_sched_action"));
                if (scheduledActionUID != null && !scheduledActionUID.isEmpty()){
                    slotKey.add(GncXmlHelper.KEY_FROM_SCHED_ACTION);
                    slotType.add(GncXmlHelper.ATTR_VALUE_GUID);
                    slotValue.add(scheduledActionUID);
                }
                xmlSerializer.startTag(null, GncXmlHelper.TAG_TRN_SLOTS);
                exportSlots(xmlSerializer, slotKey, slotType, slotValue);
                xmlSerializer.endTag(null, GncXmlHelper.TAG_TRN_SLOTS);

                // splits start
                xmlSerializer.startTag(null, GncXmlHelper.TAG_TRN_SPLITS);
            }
            xmlSerializer.startTag(null, GncXmlHelper.TAG_TRN_SPLIT);
            // split id
            xmlSerializer.startTag(null, GncXmlHelper.TAG_SPLIT_ID);
            xmlSerializer.attribute(null, GncXmlHelper.ATTR_KEY_TYPE, GncXmlHelper.ATTR_VALUE_GUID);
            xmlSerializer.text(cursor.getString(cursor.getColumnIndexOrThrow("split_uid")));
            xmlSerializer.endTag(null, GncXmlHelper.TAG_SPLIT_ID);
            // memo
            String memo = cursor.getString(cursor.getColumnIndexOrThrow("split_memo"));
            if (memo != null && memo.length() > 0){
                xmlSerializer.startTag(null, GncXmlHelper.TAG_SPLIT_MEMO);
                xmlSerializer.text(memo);
                xmlSerializer.endTag(null, GncXmlHelper.TAG_SPLIT_MEMO);
            }
            // reconciled
            xmlSerializer.startTag(null, GncXmlHelper.TAG_RECONCILED_STATE);
            xmlSerializer.text("n");
            xmlSerializer.endTag(null, GncXmlHelper.TAG_RECONCILED_STATE);
            // value, in the transaction's currency
            String trxType = cursor.getString(cursor.getColumnIndexOrThrow("split_type"));
            BigDecimal value = new BigDecimal(cursor.getString(cursor.getColumnIndexOrThrow("split_amount")));
            value = value.multiply(denom);
            String strValue = (trxType.equals("CREDIT") ? "-" : "") + value.stripTrailingZeros().toPlainString() + "/" + denomString;
            xmlSerializer.startTag(null, GncXmlHelper.TAG_SPLIT_VALUE);
            xmlSerializer.text(strValue);
            xmlSerializer.endTag(null, GncXmlHelper.TAG_SPLIT_VALUE);
            // quantity, in the split account's currency
            // TODO: multi currency support.
            xmlSerializer.startTag(null, GncXmlHelper.TAG_SPLIT_QUANTITY);
            xmlSerializer.text(strValue);
            xmlSerializer.endTag(null, GncXmlHelper.TAG_SPLIT_QUANTITY);
            // account guid
            xmlSerializer.startTag(null, GncXmlHelper.TAG_SPLIT_ACCOUNT);
            xmlSerializer.attribute(null, GncXmlHelper.ATTR_KEY_TYPE, GncXmlHelper.ATTR_VALUE_GUID);
            String splitAccountUID = cursor.getString(cursor.getColumnIndexOrThrow("split_acct_uid"));
            xmlSerializer.text(splitAccountUID);
            xmlSerializer.endTag(null, GncXmlHelper.TAG_SPLIT_ACCOUNT);

            //if we are exporting a template transaction, then we need to add some extra slots
            if (exportTemplates){
                xmlSerializer.startTag(null, GncXmlHelper.TAG_SPLIT_SLOTS);
                xmlSerializer.startTag(null, GncXmlHelper.TAG_SLOT);
                xmlSerializer.startTag(null, GncXmlHelper.TAG_SLOT_KEY);
                xmlSerializer.text(GncXmlHelper.KEY_SCHEDX_ACTION); //FIXME: not all templates may be scheduled actions
                xmlSerializer.endTag(null, GncXmlHelper.TAG_SLOT_KEY);
                xmlSerializer.startTag(null, GncXmlHelper.TAG_SLOT_VALUE);
                xmlSerializer.attribute(null, GncXmlHelper.ATTR_KEY_TYPE, "frame");

                List<String> slotKeys = new ArrayList<>();
                List<String> slotTypes = new ArrayList<>();
                List<String> slotValues = new ArrayList<>();
                slotKeys.add(GncXmlHelper.KEY_SPLIT_ACCOUNT);
                slotTypes.add(GncXmlHelper.ATTR_VALUE_GUID);
                slotValues.add(splitAccountUID);
                TransactionType type = TransactionType.valueOf(trxType);
                slotKeys.add(type == TransactionType.CREDIT ? GncXmlHelper.KEY_CREDIT_FORMULA : GncXmlHelper.KEY_DEBIT_FORMULA);
                slotTypes.add(GncXmlHelper.ATTR_VALUE_STRING);
                slotValues.add(GncXmlHelper.getNumberFormatForTemplateSplits().format(value.doubleValue()));

                exportSlots(xmlSerializer, slotKeys, slotTypes, slotValues);

                xmlSerializer.endTag(null, GncXmlHelper.TAG_SLOT_VALUE);
                xmlSerializer.endTag(null, GncXmlHelper.TAG_SLOT);
                xmlSerializer.endTag(null, GncXmlHelper.TAG_SPLIT_SLOTS);
            }

            xmlSerializer.endTag(null, GncXmlHelper.TAG_TRN_SPLIT);
        }
        if (!lastTrxUID.equals("")){ // there's an unfinished transaction, close it
            xmlSerializer.endTag(null,GncXmlHelper.TAG_TRN_SPLITS);
            xmlSerializer.endTag(null, GncXmlHelper.TAG_TRANSACTION);
        }
        cursor.close();
    }

    /**
     * Serializes {@link ScheduledAction}s from the database to XML
     * @param xmlSerializer XML serializer
     * @throws IOException
     */
    private void exportScheduledActions(XmlSerializer xmlSerializer) throws IOException{
        Cursor cursor = mScheduledActionDbAdapter.fetchAllRecords();
        while (cursor.moveToNext()) {
            xmlSerializer.startTag(null, GncXmlHelper.TAG_SCHEDULED_ACTION);
            xmlSerializer.attribute(null, GncXmlHelper.ATTR_KEY_VERSION, GncXmlHelper.BOOK_VERSION);
            xmlSerializer.startTag(null, GncXmlHelper.TAG_SX_ID);
            String scheduledActionUID = cursor.getString(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_UID));
            xmlSerializer.attribute(null, GncXmlHelper.ATTR_VALUE_GUID, scheduledActionUID);
            xmlSerializer.endTag(null, GncXmlHelper.TAG_SX_ID);
            xmlSerializer.startTag(null, GncXmlHelper.TAG_SX_NAME);
            xmlSerializer.text(cursor.getString(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_TYPE)));
            xmlSerializer.endTag(null, GncXmlHelper.TAG_SX_NAME);
            xmlSerializer.startTag(null, GncXmlHelper.TAG_SX_ENABLED);
            boolean enabled = cursor.getShort(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_ENABLED)) > 0;
            xmlSerializer.text(enabled ? "y" : "n");
            xmlSerializer.endTag(null, GncXmlHelper.TAG_SX_ENABLED);
            xmlSerializer.startTag(null, GncXmlHelper.TAG_SX_AUTO_CREATE);
            xmlSerializer.text("y");
            xmlSerializer.endTag(null, GncXmlHelper.TAG_SX_AUTO_CREATE);
            xmlSerializer.startTag(null, GncXmlHelper.TAG_SX_AUTO_CREATE_NOTIFY);
            xmlSerializer.text("n"); //TODO: if we ever support notifying before creating a scheduled transaction, then update this
            xmlSerializer.endTag(null, GncXmlHelper.TAG_SX_AUTO_CREATE_NOTIFY);
            xmlSerializer.startTag(null, GncXmlHelper.TAG_SX_ADVANCE_CREATE_DAYS);
            xmlSerializer.text("0");
            xmlSerializer.endTag(null, GncXmlHelper.TAG_SX_ADVANCE_CREATE_DAYS);
            xmlSerializer.startTag(null, GncXmlHelper.TAG_SX_ADVANCE_REMIND_DAYS);
            xmlSerializer.text("0");
            xmlSerializer.endTag(null, GncXmlHelper.TAG_SX_ADVANCE_REMIND_DAYS);
            xmlSerializer.startTag(null, GncXmlHelper.TAG_SX_INSTANCE_COUNT);
            xmlSerializer.text("1");
            xmlSerializer.endTag(null, GncXmlHelper.TAG_SX_INSTANCE_COUNT);

            //start date
            long startTime = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_START_TIME));
            serializeDate(xmlSerializer, GncXmlHelper.TAG_SX_START, startTime);

            long lastRunTime = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_LAST_RUN));
            if (lastRunTime > 0){
                serializeDate(xmlSerializer, GncXmlHelper.TAG_SX_LAST, lastRunTime);
            }

            long endTime = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_END_TIME));
            if (endTime > 0) {
                //end date
                serializeDate(xmlSerializer, GncXmlHelper.TAG_SX_END, endTime);
            } else { //add number of occurrences
                int numOccurrences = cursor.getInt(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_TOTAL_FREQUENCY));
                xmlSerializer.startTag(null, GncXmlHelper.TAG_SX_NUM_OCCUR);
                xmlSerializer.text(Integer.toString(numOccurrences));
                xmlSerializer.endTag(null, GncXmlHelper.TAG_SX_NUM_OCCUR);

                //remaining occurrences
                int executionCount = cursor.getInt(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_EXECUTION_COUNT));
                xmlSerializer.startTag(null, GncXmlHelper.TAG_SX_REM_OCCUR);
                xmlSerializer.text(Integer.toString(numOccurrences - executionCount));
                xmlSerializer.endTag(null, GncXmlHelper.TAG_SX_REM_OCCUR);
            }

            String tag = cursor.getString(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_TAG));
            if (tag != null && !tag.isEmpty()){
                xmlSerializer.startTag(null, GncXmlHelper.TAG_SX_TAG);
                xmlSerializer.text(tag);
                xmlSerializer.endTag(null, GncXmlHelper.TAG_SX_TAG);
            }

            //TODO: possibly generate temporary template accounts to keep gnucash desktop happy
            //Ignoring GnuCash XML template account: <sx:templ-acct type="guid">2da76df09056540bb3a37e4a04547d82</sx:templ-acct>

            String actionUID = cursor.getString(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_ACTION_UID));
            xmlSerializer.startTag(null, GncXmlHelper.TAG_SX_TEMPL_ACTION);
            xmlSerializer.attribute(null, GncXmlHelper.ATTR_KEY_TYPE, GncXmlHelper.ATTR_VALUE_GUID);
            xmlSerializer.text(actionUID);
            xmlSerializer.endTag(null, GncXmlHelper.TAG_SX_TEMPL_ACTION);

            xmlSerializer.startTag(null, GncXmlHelper.TAG_SX_SCHEDULE);
            xmlSerializer.startTag(null, GncXmlHelper.TAG_RECURRENCE);
            xmlSerializer.attribute(null, GncXmlHelper.ATTR_KEY_VERSION, GncXmlHelper.RECURRENCE_VERSION);
            xmlSerializer.startTag(null, GncXmlHelper.TAG_RX_MULT);
            xmlSerializer.text("1");
            xmlSerializer.endTag(null, GncXmlHelper.TAG_RX_MULT);
            xmlSerializer.startTag(null, GncXmlHelper.TAG_RX_PERIOD_TYPE);
            long period = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_PERIOD));
            xmlSerializer.text(ScheduledAction.getPeriodType(period).name());
            xmlSerializer.endTag(null, GncXmlHelper.TAG_RX_PERIOD_TYPE);

            serializeDate(xmlSerializer, GncXmlHelper.TAG_RX_START, startTime);

            xmlSerializer.endTag(null, GncXmlHelper.TAG_RECURRENCE);
            xmlSerializer.endTag(null, GncXmlHelper.TAG_SX_SCHEDULE);

            xmlSerializer.endTag(null, GncXmlHelper.TAG_SCHEDULED_ACTION);
        }
    }

    /**
     * Serializes a date as a {@code tag} which has a nested {@link GncXmlHelper#TAG_GDATE} which
     * has the date as a text element formatted using {@link GncXmlHelper#DATE_FORMATTER}
     * @param xmlSerializer XML serializer
     * @param tag Enclosing tag
     * @param timeMillis Date to be formatted and output
     * @throws IOException
     */
    private void serializeDate(XmlSerializer xmlSerializer, String tag, long timeMillis) throws IOException {
        xmlSerializer.startTag(null, tag);
        xmlSerializer.startTag(null, GncXmlHelper.TAG_GDATE);
        xmlSerializer.text(GncXmlHelper.DATE_FORMATTER.format(timeMillis));
        xmlSerializer.endTag(null, GncXmlHelper.TAG_GDATE);
        xmlSerializer.endTag(null, tag);
    }

    @Override
    public void generateExport(Writer writer) throws ExporterException{
        try {
            String[] namespaces = new String[] {"gnc", "act", "book", "cd", "cmdty", "price", "slot", "split", "trn", "ts"};
            XmlSerializer xmlSerializer = XmlPullParserFactory.newInstance().newSerializer();
            xmlSerializer.setOutput(writer);
            xmlSerializer.startDocument("utf-8", true);
            // root tag
            xmlSerializer.startTag(null, GncXmlHelper.TAG_ROOT);
            for(String ns : namespaces) {
                xmlSerializer.attribute(null, "xmlns:" + ns, "http://www.gnucash.org/XML/" + ns);
            }
            // book count
            xmlSerializer.startTag(null, GncXmlHelper.TAG_COUNT_DATA);
            xmlSerializer.attribute(null, GncXmlHelper.ATTR_KEY_CD_TYPE, "book");
            xmlSerializer.text("1");
            xmlSerializer.endTag(null, GncXmlHelper.TAG_COUNT_DATA);
            // book
            xmlSerializer.startTag(null, GncXmlHelper.TAG_BOOK);
            xmlSerializer.attribute(null, GncXmlHelper.ATTR_KEY_VERSION, GncXmlHelper.BOOK_VERSION);
            // book_id
            xmlSerializer.startTag(null, GncXmlHelper.TAG_BOOK_ID);
            xmlSerializer.attribute(null, GncXmlHelper.ATTR_KEY_TYPE, GncXmlHelper.ATTR_VALUE_GUID);
            xmlSerializer.text(UUID.randomUUID().toString().replaceAll("-", ""));
            xmlSerializer.endTag(null, GncXmlHelper.TAG_BOOK_ID);
            //commodity count
            xmlSerializer.startTag(null, GncXmlHelper.TAG_COUNT_DATA);
            xmlSerializer.attribute(null, GncXmlHelper.ATTR_KEY_CD_TYPE, "commodity");
            xmlSerializer.text(mAccountsDbAdapter.getCurrencies().size() + "");
            xmlSerializer.endTag(null, GncXmlHelper.TAG_COUNT_DATA);
            //account count
            xmlSerializer.startTag(null, GncXmlHelper.TAG_COUNT_DATA);
            xmlSerializer.attribute(null, GncXmlHelper.ATTR_KEY_CD_TYPE, "account");
            xmlSerializer.text(mAccountsDbAdapter.getTotalAccountCount() + "");
            xmlSerializer.endTag(null, GncXmlHelper.TAG_COUNT_DATA);
            //transaction count
            xmlSerializer.startTag(null, GncXmlHelper.TAG_COUNT_DATA);
            xmlSerializer.attribute(null, GncXmlHelper.ATTR_KEY_CD_TYPE, "transaction");
            xmlSerializer.text(mTransactionsDbAdapter.getTotalTransactionsCount() + "");
            xmlSerializer.endTag(null, GncXmlHelper.TAG_COUNT_DATA);
            // accounts. bulk import does not rely on account order
            // the cursor gather account in arbitrary order
            exportAccounts(xmlSerializer);

            // transactions.
            exportTransactions(xmlSerializer, false);

            //transaction templates
            xmlSerializer.startTag(null, GncXmlHelper.TAG_TEMPLATE_TRANSACTIONS);
            exportTransactions(xmlSerializer, true);
            xmlSerializer.endTag(null, GncXmlHelper.TAG_TEMPLATE_TRANSACTIONS);

            //scheduled actions
            exportScheduledActions(xmlSerializer);

            xmlSerializer.endTag(null, GncXmlHelper.TAG_BOOK);
            xmlSerializer.endTag(null, GncXmlHelper.TAG_ROOT);
            xmlSerializer.endDocument();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExporterException(mParameters, e);
        }
    }
    /**
     * Creates a backup of current database contents to the default backup location
     */
    public static void createBackup(){
        ExportParams params = new ExportParams(ExportFormat.GNC_XML);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(Exporter.createBackupFile());
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bufferedOutputStream);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(gzipOutputStream);
            new GncXmlExporter(params).generateExport(outputStreamWriter);
            outputStreamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("GncXmlExporter", "Error creating backup", e);
        }
    }
}
