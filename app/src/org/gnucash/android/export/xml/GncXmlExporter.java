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

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.export.ExportFormat;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Transaction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
        bookCountNode.setAttribute("cd:type", "book");
        bookCountNode.appendChild(mDocument.createTextNode("1"));
        rootElement.appendChild(bookCountNode);

        Element bookNode = mDocument.createElement(GncXmlHelper.TAG_BOOK);
        bookNode.setAttribute("version", GncXmlHelper.BOOK_VERSION);
        rootElement.appendChild(bookNode);

        Element bookIdNode = mDocument.createElement(GncXmlHelper.TAG_BOOK_ID);
        bookIdNode.setAttribute("type", "guid");
        bookIdNode.appendChild(mDocument.createTextNode(UUID.randomUUID().toString().replaceAll("-", "")));
        bookNode.appendChild(bookIdNode);

        Element cmdtyCountData = mDocument.createElement(GncXmlHelper.TAG_COUNT_DATA);
        cmdtyCountData.setAttribute("cd:type", "commodity");
        cmdtyCountData.appendChild(mDocument.createTextNode("1")); //TODO: put actual number of currencies
        bookNode.appendChild(cmdtyCountData);

        List<Account> accountList = mAccountsDbAdapter.getSimpleAccountList();

        Element accountCountNode = mDocument.createElement(GncXmlHelper.TAG_COUNT_DATA);
        accountCountNode.setAttribute("cd:type", "account");
        accountCountNode.appendChild(mDocument.createTextNode(String.valueOf(accountList.size())));
        bookNode.appendChild(accountCountNode);

        List<Transaction> transactionsList = mTransactionsDbAdapter.getAllTransactions();

        Element transactionCountNode = mDocument.createElement(GncXmlHelper.TAG_COUNT_DATA);
        transactionCountNode.setAttribute("cd:type", "transaction");
        transactionCountNode.appendChild(mDocument.createTextNode(String.valueOf(transactionsList.size())));
        bookNode.appendChild(transactionCountNode);

        for (Account account : accountList) {
            account.toGncXml(mDocument, bookNode);
        }

        for (Transaction transaction : transactionsList) {
            transaction.toGncXml(mDocument, bookNode);
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

    /**
     * Creates a backup of current database contents to the default backup location
     */
    public static void createBackup(){
        ExportParams params = new ExportParams(ExportFormat.GNC_XML);
        try {
            FileWriter fileWriter = new FileWriter(Exporter.createBackupFile());
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(new GncXmlExporter(params).generateExport());
            bufferedWriter.flush();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("GncXmlExporter", "Error creating backup", e);
        }
    }
}
