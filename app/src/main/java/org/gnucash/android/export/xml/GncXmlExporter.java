/*
 * Copyright (c) 2014 Ngewi Fet <ngewif@gmail.com>
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
import static org.gnucash.android.db.DatabaseSchema.*;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Transaction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

/**
 * Creates a GnuCash XML representation of the accounts and transactions
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 * @author Yongxin Wang <fefe.wyx@gmail.com>
 */
public class GncXmlExporter extends Exporter{

    private TransactionsDbAdapter mTransactionsDbAdapter;

    public GncXmlExporter(ExportParams params){
        super(params);
        mTransactionsDbAdapter = new TransactionsDbAdapter(mContext);
    }

    /**
     * Overloaded constructor.
     * <p>This method is used mainly by the {@link org.gnucash.android.db.DatabaseHelper} for database migrations</p>
     * @param params Export parameters
     * @param db SQLite database from which to export
     */
    public GncXmlExporter(ExportParams params, SQLiteDatabase db){
        super(params, db);
        mTransactionsDbAdapter = new TransactionsDbAdapter(db);
    }

    private void exportSlots(XmlSerializer xmlSerializer,
                             List<String> slotKey,
                             List<String> slotType,
                             List<String> slotValue) throws IOException {
        if (slotKey == null || slotType == null || slotValue == null ||
                slotKey.size() == 0 || slotType.size() != slotKey.size() || slotValue.size() != slotKey.size()) {
            return;
        }
        xmlSerializer.startTag(null, GncXmlHelper.TAG_ACT_SLOTS);
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
        xmlSerializer.endTag(null, GncXmlHelper.TAG_ACT_SLOTS);
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
            exportSlots(xmlSerializer, slotKey, slotType, slotValue);

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

    public void exportTransactions(XmlSerializer xmlSerializer) throws IOException {
        Cursor cursor = mTransactionsDbAdapter.fetchTransactionsWithSplits(
                new String[]{
                        TransactionEntry.TABLE_NAME+"."+ TransactionEntry.COLUMN_UID + " AS trans_uid",
                        TransactionEntry.TABLE_NAME+"."+ TransactionEntry.COLUMN_DESCRIPTION + " AS trans_desc",
                        TransactionEntry.TABLE_NAME+"."+ TransactionEntry.COLUMN_NOTES + " AS trans_notes",
                        TransactionEntry.TABLE_NAME+"."+ TransactionEntry.COLUMN_TIMESTAMP + " AS trans_time",
                        TransactionEntry.TABLE_NAME+"."+ TransactionEntry.COLUMN_EXPORTED + " AS trans_exported",
                        TransactionEntry.TABLE_NAME+"."+ TransactionEntry.COLUMN_CURRENCY + " AS trans_currency",
                        TransactionEntry.TABLE_NAME+"."+ TransactionEntry.COLUMN_RECURRENCE_PERIOD + " AS trans_recur",
                        SplitEntry.TABLE_NAME+"."+ SplitEntry.COLUMN_UID + " AS split_uid",
                        SplitEntry.TABLE_NAME+"."+ SplitEntry.COLUMN_MEMO + " AS split_memo",
                        SplitEntry.TABLE_NAME+"."+ SplitEntry.COLUMN_TYPE + " AS split_type",
                        SplitEntry.TABLE_NAME+"."+ SplitEntry.COLUMN_AMOUNT + " AS split_amount",
                        SplitEntry.TABLE_NAME+"."+ SplitEntry.COLUMN_ACCOUNT_UID + " AS split_acct_uid"
                }, null,
                TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_RECURRENCE_PERIOD + " ASC , " +
                        TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TIMESTAMP + " ASC , " +
                        TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID + " ASC ");

        String lastTrxUID = "";
        Currency trxCurrency;
        int fractionDigits;
        BigDecimal denom = new BigDecimal(100);
        String denomString = "100";
        int recur = 0;
        while (cursor.moveToNext()){
            String curTrxUID = cursor.getString(cursor.getColumnIndexOrThrow("trans_uid"));
            if (!lastTrxUID.equals(curTrxUID)) { // new transaction starts
                if (!lastTrxUID.equals("")) { // there's an old transaction, close it
                    xmlSerializer.endTag(null, GncXmlHelper.TAG_TRN_SPLITS);
                    if (recur > 0) {
                        xmlSerializer.startTag(null, GncXmlHelper.TAG_RECURRENCE_PERIOD);
                        xmlSerializer.text(Integer.toString(recur));
                        xmlSerializer.endTag(null, GncXmlHelper.TAG_RECURRENCE_PERIOD);
                    }
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
                // date posted
                String strDate = GncXmlHelper.formatDate(cursor.getLong(cursor.getColumnIndexOrThrow("trans_time")));
                xmlSerializer.startTag(null, GncXmlHelper.TAG_DATE_POSTED);
                xmlSerializer.startTag(null, GncXmlHelper.TAG_DATE);
                xmlSerializer.text(strDate);
                xmlSerializer.endTag(null, GncXmlHelper.TAG_DATE);
                xmlSerializer.endTag(null, GncXmlHelper.TAG_DATE_POSTED);
                // date entered
                xmlSerializer.startTag(null, GncXmlHelper.TAG_DATE_ENTERED);
                xmlSerializer.startTag(null, GncXmlHelper.TAG_DATE);
                xmlSerializer.text(strDate);
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
                exportSlots(xmlSerializer, slotKey, slotType, slotValue);
                // recurrence period, will be write out when all splits are generated.
                recur = cursor.getInt(cursor.getColumnIndexOrThrow("trans_recur"));
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
            xmlSerializer.text(cursor.getString(cursor.getColumnIndexOrThrow("split_acct_uid")));
            xmlSerializer.endTag(null, GncXmlHelper.TAG_SPLIT_ACCOUNT);
            xmlSerializer.endTag(null, GncXmlHelper.TAG_TRN_SPLIT);
        }
        if (!lastTrxUID.equals("")){ // there's an unfinished transaction, close it
            xmlSerializer.endTag(null,GncXmlHelper.TAG_TRN_SPLITS);
            if (recur > 0) {
                xmlSerializer.startTag(null, GncXmlHelper.TAG_RECURRENCE_PERIOD);
                xmlSerializer.text(Integer.toString(recur));
                xmlSerializer.endTag(null, GncXmlHelper.TAG_RECURRENCE_PERIOD);
            }
            xmlSerializer.endTag(null, GncXmlHelper.TAG_TRANSACTION);
        }
        cursor.close();
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
            exportTransactions(xmlSerializer);

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

    /**
     * Generate GnuCash XML by loading the accounts and transactions from the database and exporting each one.
     * This method consumes a lot of memory and is slow, but exists for database migrations for backwards compatibility.
     * <p>The normal exporter interface should be used to generate GncXML files</p>
     * @return String with the generated XML
     * @throws ParserConfigurationException if there was an error when generating the XML
     * @deprecated Use the {@link #generateExport(java.io.Writer)} to generate XML
     */
    public String generateXML() throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = docFactory.newDocumentBuilder();

        Document document = documentBuilder.newDocument();
        document.setXmlVersion("1.0");
        document.setXmlStandalone(true);

        Element rootElement = document.createElement(GncXmlHelper.TAG_ROOT);
        rootElement.setAttribute("xmlns:gnc",    "http://www.gnucash.org/XML/gnc");
        rootElement.setAttribute("xmlns:act",    "http://www.gnucash.org/XML/act");
        rootElement.setAttribute("xmlns:book",   "http://www.gnucash.org/XML/book");
        rootElement.setAttribute("xmlns:cd",     "http://www.gnucash.org/XML/cd");
        rootElement.setAttribute("xmlns:cmdty",  "http://www.gnucash.org/XML/cmdty");
        rootElement.setAttribute("xmlns:price",  "http://www.gnucash.org/XML/price");
        rootElement.setAttribute("xmlns:slot",   "http://www.gnucash.org/XML/slot");
        rootElement.setAttribute("xmlns:split",  "http://www.gnucash.org/XML/split");
        rootElement.setAttribute("xmlns:trn",    "http://www.gnucash.org/XML/trn");
        rootElement.setAttribute("xmlns:ts",     "http://www.gnucash.org/XML/ts");

        Element bookCountNode = document.createElement(GncXmlHelper.TAG_COUNT_DATA);
        bookCountNode.setAttribute(GncXmlHelper.ATTR_KEY_CD_TYPE, GncXmlHelper.ATTR_VALUE_BOOK);
        bookCountNode.appendChild(document.createTextNode("1"));
        rootElement.appendChild(bookCountNode);

        Element bookNode = document.createElement(GncXmlHelper.TAG_BOOK);
        bookNode.setAttribute(GncXmlHelper.ATTR_KEY_VERSION, GncXmlHelper.BOOK_VERSION);
        rootElement.appendChild(bookNode);

        Element bookIdNode = document.createElement(GncXmlHelper.TAG_BOOK_ID);
        bookIdNode.setAttribute(GncXmlHelper.ATTR_KEY_TYPE, GncXmlHelper.ATTR_VALUE_GUID);
        bookIdNode.appendChild(document.createTextNode(UUID.randomUUID().toString().replaceAll("-", "")));
        bookNode.appendChild(bookIdNode);

        Element cmdtyCountData = document.createElement(GncXmlHelper.TAG_COUNT_DATA);
        cmdtyCountData.setAttribute(GncXmlHelper.ATTR_KEY_CD_TYPE, "commodity");
        cmdtyCountData.appendChild(document.createTextNode(String.valueOf(mAccountsDbAdapter.getCurrencies().size())));
        bookNode.appendChild(cmdtyCountData);

        Element accountCountNode = document.createElement(GncXmlHelper.TAG_COUNT_DATA);
        accountCountNode.setAttribute(GncXmlHelper.ATTR_KEY_CD_TYPE, "account");
        int accountCount = mAccountsDbAdapter.getTotalAccountCount();
        accountCountNode.appendChild(document.createTextNode(String.valueOf(accountCount)));
        bookNode.appendChild(accountCountNode);

        Element transactionCountNode = document.createElement(GncXmlHelper.TAG_COUNT_DATA);
        transactionCountNode.setAttribute(GncXmlHelper.ATTR_KEY_CD_TYPE, "transaction");
        int transactionCount = mTransactionsDbAdapter.getTotalTransactionsCount();
        transactionCountNode.appendChild(document.createTextNode(String.valueOf(transactionCount)));
        bookNode.appendChild(transactionCountNode);

        String rootAccountUID = mAccountsDbAdapter.getGnuCashRootAccountUID();
        Account rootAccount = mAccountsDbAdapter.getAccount(rootAccountUID);
        if (rootAccount != null){
            rootAccount.toGncXml(document, bookNode);
        }
        Cursor accountsCursor = mAccountsDbAdapter.fetchAllRecordsOrderedByFullName();

        //create accounts hierarchically by ordering by full name
        if (accountsCursor != null){
            while (accountsCursor.moveToNext()){
                long id = accountsCursor.getLong(accountsCursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry._ID));
                Account account = mAccountsDbAdapter.getAccount(id);
                account.toGncXml(document, bookNode);
            }
            accountsCursor.close();
        }

        //more memory efficient approach than loading all transactions into memory first
        Cursor transactionsCursor = mTransactionsDbAdapter.fetchAllRecords();
        if (transactionsCursor != null){
            while (transactionsCursor.moveToNext()){
                Transaction transaction = mTransactionsDbAdapter.buildTransactionInstance(transactionsCursor);
                transaction.toGncXml(document, bookNode);
            }
            transactionsCursor.close();
        }

        document.appendChild(rootElement);
        mAccountsDbAdapter.close();
        mTransactionsDbAdapter.close();

        StringWriter stringWriter = new StringWriter();
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();

            Transformer transformer = transformerFactory.newTransformer();

            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(stringWriter);

            transformer.transform(source, result);
            stringWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExporterException(mParameters, e);
        }
        return stringWriter.toString();
    }
}
