/*
 * Copyright (c) 2014 Ngewi Fet <ngewif@gmail.com>
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

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
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
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

/**
 * Creates a GnuCash XML representation of the accounts and transactions
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class GncXmlExporter extends Exporter{

    private Document mDocument;
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

    /**
     * Generate GnuCash XML
     * @throws ParserConfigurationException if there was an error when generating the XML
     */
    private void generateGncXml() throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
//        docFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = docFactory.newDocumentBuilder();

        mDocument = documentBuilder.newDocument();
        mDocument.setXmlVersion("1.0");
        mDocument.setXmlStandalone(true);

        Element rootElement = mDocument.createElement(GncXmlHelper.TAG_ROOT);
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

        Element bookCountNode = mDocument.createElement(GncXmlHelper.TAG_COUNT_DATA);
        bookCountNode.setAttribute(GncXmlHelper.ATTR_KEY_CD_TYPE, GncXmlHelper.ATTR_VALUE_BOOK);
        bookCountNode.appendChild(mDocument.createTextNode("1"));
        rootElement.appendChild(bookCountNode);

        Element bookNode = mDocument.createElement(GncXmlHelper.TAG_BOOK);
        bookNode.setAttribute(GncXmlHelper.ATTR_KEY_VERSION, GncXmlHelper.BOOK_VERSION);
        rootElement.appendChild(bookNode);

        Element bookIdNode = mDocument.createElement(GncXmlHelper.TAG_BOOK_ID);
        bookIdNode.setAttribute(GncXmlHelper.ATTR_KEY_TYPE, GncXmlHelper.ATTR_VALUE_GUID);
        bookIdNode.appendChild(mDocument.createTextNode(UUID.randomUUID().toString().replaceAll("-", "")));
        bookNode.appendChild(bookIdNode);

        Element cmdtyCountData = mDocument.createElement(GncXmlHelper.TAG_COUNT_DATA);
        cmdtyCountData.setAttribute(GncXmlHelper.ATTR_KEY_CD_TYPE, "commodity");
        cmdtyCountData.appendChild(mDocument.createTextNode(String.valueOf(mAccountsDbAdapter.getCurrencies().size())));
        bookNode.appendChild(cmdtyCountData);

        Element accountCountNode = mDocument.createElement(GncXmlHelper.TAG_COUNT_DATA);
        accountCountNode.setAttribute(GncXmlHelper.ATTR_KEY_CD_TYPE, "account");
        int accountCount = mAccountsDbAdapter.getTotalAccountCount();
        accountCountNode.appendChild(mDocument.createTextNode(String.valueOf(accountCount)));
        bookNode.appendChild(accountCountNode);

        Element transactionCountNode = mDocument.createElement(GncXmlHelper.TAG_COUNT_DATA);
        transactionCountNode.setAttribute(GncXmlHelper.ATTR_KEY_CD_TYPE, "transaction");
        int transactionCount = mTransactionsDbAdapter.getTotalTransactionsCount();
        transactionCountNode.appendChild(mDocument.createTextNode(String.valueOf(transactionCount)));
        bookNode.appendChild(transactionCountNode);

        String rootAccountUID = mAccountsDbAdapter.getGnuCashRootAccountUID();
        Account rootAccount = mAccountsDbAdapter.getAccount(rootAccountUID);
        if (rootAccount != null){
            rootAccount.toGncXml(mDocument, bookNode);
        }
        Cursor accountsCursor = mAccountsDbAdapter.fetchAllRecordsOrderedByFullName();

        //create accounts hierarchically by ordering by full name
        if (accountsCursor != null){
            while (accountsCursor.moveToNext()){
                long id = accountsCursor.getLong(accountsCursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry._ID));
                Account account = mAccountsDbAdapter.getAccount(id);
                account.toGncXml(mDocument, bookNode);
            }
            accountsCursor.close();
        }

        //more memory efficient approach than loading all transactions into memory first
        Cursor transactionsCursor = mTransactionsDbAdapter.fetchAllRecords();
        if (transactionsCursor != null){
            while (transactionsCursor.moveToNext()){
                Transaction transaction = mTransactionsDbAdapter.buildTransactionInstance(transactionsCursor);
                transaction.toGncXml(mDocument, bookNode);
            }
            transactionsCursor.close();
        }

        mDocument.appendChild(rootElement);
        mAccountsDbAdapter.close();
        mTransactionsDbAdapter.close();
    }

    @Override
    public String generateExport() throws ExporterException{
        StringWriter stringWriter = new StringWriter();
        try {
            generateGncXml();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();

            Transformer transformer = transformerFactory.newTransformer();

            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(mDocument);
            StreamResult result = new StreamResult(stringWriter);

            transformer.transform(source, result);
            stringWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExporterException(mParameters, e);
        }
        return stringWriter.toString();
    }


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
            mAccountsDbAdapter.exportAccountsToGncXML(xmlSerializer, null, null);

            // transactions.
            mTransactionsDbAdapter.exportTransactionsWithSplitsToGncXML(xmlSerializer, null);

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
            FileWriter fileWriter = new FileWriter(Exporter.createBackupFile());
            //BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            new GncXmlExporter(params).generateExport(fileWriter);
            //bufferedWriter.flush();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("GncXmlExporter", "Error creating backup", e);
        }
    }
}
