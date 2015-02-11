/*
 * Copyright (c) 2012 - 2014 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.export.ofx;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import android.preference.PreferenceManager;
import org.gnucash.android.R;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.db.AccountsDbAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Exports the data in the database in OFX format
 * @author Ngewi Fet <ngewi.fet@gmail.com>
 * @author Yongxin Wang <fefe.wyx@gmail.com>
 */
public class OfxExporter extends Exporter{

    /**
	 * List of accounts in the expense report
	 */
	private List<Account> mAccountsList;

    /**
	 * Builds an XML representation of the {@link Account}s and {@link Transaction}s in the database
	 */
	public OfxExporter(ExportParams params) {
        super(params, null);
	}

    /**
	 * Converts all expenses into OFX XML format and adds them to the XML document
	 * @param doc DOM document of the OFX expenses.
	 * @param parent Parent node for all expenses in report
	 */
	private void generateOfx(Document doc, Element parent){
		Element transactionUid = doc.createElement(OfxHelper.TAG_TRANSACTION_UID);
		//unsolicited because the data exported is not as a result of a request
		transactionUid.appendChild(doc.createTextNode(OfxHelper.UNSOLICITED_TRANSACTION_ID));

		Element statementTransactionResponse = doc.createElement(OfxHelper.TAG_STATEMENT_TRANSACTION_RESPONSE);
		statementTransactionResponse.appendChild(transactionUid);
		
		Element bankmsgs = doc.createElement(OfxHelper.TAG_BANK_MESSAGES_V1);
		bankmsgs.appendChild(statementTransactionResponse);
		
		parent.appendChild(bankmsgs);		
		
		AccountsDbAdapter accountsDbAdapter = mAccountsDbAdapter;
		for (Account account : mAccountsList) {		
			if (account.getTransactionCount() == 0)
				continue; 
			
			//add account details (transactions) to the XML document			
			account.toOfx(doc, statementTransactionResponse, mParameters.shouldExportAllTransactions());
			
			//mark as exported
			accountsDbAdapter.markAsExported(account.getUID());
			
		}
	}

    public String generateExport() throws ExporterException {
        mAccountsList = mParameters.shouldExportAllTransactions() ?
                mAccountsDbAdapter.getAllAccounts() : mAccountsDbAdapter.getExportableAccounts();

        DocumentBuilderFactory docFactory = DocumentBuilderFactory
                .newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ExporterException(mParameters, e);
        }

        Document document = docBuilder.newDocument();
        Element root = document.createElement("OFX");

        ProcessingInstruction pi = document.createProcessingInstruction("OFX", OfxHelper.OFX_HEADER);
        document.appendChild(pi);
        document.appendChild(root);

        generateOfx(document, root);

        boolean useXmlHeader = PreferenceManager.getDefaultSharedPreferences(mContext)
                .getBoolean(mContext.getString(R.string.key_xml_ofx_header), false);

        StringWriter stringWriter = new StringWriter();
        //if we want SGML OFX headers, write first to string and then prepend header
        if (useXmlHeader){
            write(document, stringWriter, false);
            return stringWriter.toString();
        } else {
            Node ofxNode = document.getElementsByTagName("OFX").item(0);

            write(ofxNode, stringWriter, true);

            StringBuffer stringBuffer = new StringBuffer(OfxHelper.OFX_SGML_HEADER);
            stringBuffer.append('\n');
            stringBuffer.append(stringWriter.toString());
            return stringBuffer.toString();
        }
    }

    @Override
    public void generateExport(Writer writer) throws ExporterException {
        try {
            writer.write(generateExport());
        }
        catch (IOException e) {
            throw new ExporterException(mParameters, e);
        }
    }

    /**
     * Writes out the document held in <code>node</code> to <code>outputWriter</code>
     * @param node {@link Node} containing the OFX document structure. Usually the parent node
     * @param outputWriter {@link java.io.Writer} to use in writing the file to stream
     * @param omitXmlDeclaration Flag which causes the XML declaration to be omitted
     */
    private void write(Node node, Writer outputWriter, boolean omitXmlDeclaration){
        try {
            TransformerFactory transformerFactory = TransformerFactory
                    .newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(node);
            StreamResult result = new StreamResult(outputWriter);

            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            if (omitXmlDeclaration) {
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            }

            transformer.transform(source, result);
        } catch (TransformerConfigurationException txconfigException) {
            txconfigException.printStackTrace();
        } catch (TransformerException tfException) {
            tfException.printStackTrace();
        }
    }
}
