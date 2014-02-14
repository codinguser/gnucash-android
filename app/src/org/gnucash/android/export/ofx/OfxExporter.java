/*
 * Copyright (c) 2012 - 2014 Ngewi Fet <ngewif@gmail.com>
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

import java.util.List;

import org.gnucash.android.model.Account;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.content.Context;

/**
 * Exports the data in the database in OFX format
 * @author Ngewi Fet <ngewi.fet@gmail.com>
 */
public class OfxExporter {

    /**
	 * List of accounts in the expense report
	 */
	private List<Account> mAccountsList;
	
	/**
	 * Flag indicating whether to ignore the 'exported' on transactions
	 * If set to true, then all transactions will be exported, regardless of whether they were exported previously
	 */
	private boolean mExportAll = false;
	
	/**
	 * Reference to the application context
	 */
	private Context mContext;

    /**
	 * Builds an XML representation of the {@link Account}s and {@link Transaction}s in the database 
	 * @param context Application context
	 * @param exportAll Whether all transactions should be exported or only new ones since last export
	 */
	public OfxExporter(Context context, boolean exportAll) {
		AccountsDbAdapter dbAdapter = new AccountsDbAdapter(context);
		mAccountsList = exportAll ? dbAdapter.getAllAccounts() : dbAdapter.getExportableAccounts();
		mExportAll = exportAll;
		mContext = context;
	}

    /**
	 * Converts all expenses into OFX XML format and adds them to the XML document
	 * @param doc DOM document of the OFX expenses.
	 * @param parent Parent node for all expenses in report
	 */
	public void toOfx(Document doc, Element parent){
		Element transactionUid = doc.createElement(OfxHelper.TAG_TRANSACTION_UID);
		//unsolicited because the data exported is not as a result of a request
		transactionUid.appendChild(doc.createTextNode(OfxHelper.UNSOLICITED_TRANSACTION_ID));

		Element statementTransactionResponse = doc.createElement(OfxHelper.TAG_STATEMENT_TRANSACTION_RESPONSE);
		statementTransactionResponse.appendChild(transactionUid);
		
		Element bankmsgs = doc.createElement(OfxHelper.TAG_BANK_MESSAGES_V1);
		bankmsgs.appendChild(statementTransactionResponse);
		
		parent.appendChild(bankmsgs);		
		
		TransactionsDbAdapter transactionsDbAdapter = new TransactionsDbAdapter(mContext);
		for (Account account : mAccountsList) {		
			if (account.getTransactionCount() == 0)
				continue; 
			
			//add account details (transactions) to the XML document			
			account.toOfx(doc, statementTransactionResponse, mExportAll);
			
			//mark as exported
			transactionsDbAdapter.markAsExported(account.getUID());
			
		}
		transactionsDbAdapter.close();
	}
}
