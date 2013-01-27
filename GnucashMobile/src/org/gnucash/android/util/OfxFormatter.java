/*
 * Copyright (c) 2012 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.gnucash.android.data.Account;
import org.gnucash.android.data.Transaction;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.content.Context;

/**
 * Exports the data in the database in OFX format
 * @author Ngewi Fet <ngewi.fet@gmail.com>
 */
public class OfxFormatter {

	/**
	 * A date formatter used when creating file names for the exported data
	 */
	public final static SimpleDateFormat OFX_DATE_FORMATTER = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
	
	/**
	 * ID which will be used as the bank ID for OFX from this app
	 */
	public static String APP_ID = "org.gnucash.android";
	
	/**
	 * The Transaction ID is usually the client ID sent in a request.
	 * Since the data exported is not as a result of a request, we use 0
	 */
	public static final String UNSOLICITED_TRANSACTION_ID = "0";
	
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
	 * Header for OFX documents
	 */
	public static final String OFX_HEADER = "OFXHEADER=\"200\" VERSION=\"211\" SECURITY=\"NONE\" OLDFILEUID=\"NONE\" NEWFILEUID=\"NONE\"";
	
	/**
	 * SGML header for OFX. Used for compatibility with desktop GnuCash
	 */
	public static final String OFX_SGML_HEADER = "ENCODING:UTF-8\nOFXHEADER:100\nDATA:OFXSGML\nVERSION:211\nSECURITY:NONE\nCHARSET:UTF-8\nCOMPRESSION:NONE\nOLDFILEUID:NONE\nNEWFILEUID:NONE";
	
	/**
	 * Builds an XML representation of the {@link Account}s and {@link Transaction}s in the database 
	 * @param context Application context
	 * @param exportAll Whether all transactions should be exported or only new ones since last export
	 */
	public OfxFormatter(Context context, boolean exportAll) {
		AccountsDbAdapter dbAdapter = new AccountsDbAdapter(context);
		mAccountsList = exportAll ? dbAdapter.getAllAccounts() : dbAdapter.getExportableAccounts();
		dbAdapter.close();
		mExportAll = exportAll;
		mContext = context;
	}
	
	/**
	 * Returns the current time formatted using the pattern in {@link #OFX_DATE_FORMATTER}
	 * @return Current time as a formatted string
	 * @see #getOfxFormattedTime(long)
	 */
	public static String getFormattedCurrentTime(){
		return getOfxFormattedTime(System.currentTimeMillis());
	}
	
	/**
	 * Returns a formatted string representation of time in <code>milliseconds</code>
	 * @param milliseconds Long value representing the time to be formatted
	 * @return Formatted string representation of time in <code>milliseconds</code>
	 */
	public static String getOfxFormattedTime(long milliseconds){
		Date date = new Date(milliseconds);
		String dateString = OFX_DATE_FORMATTER.format(date);
		TimeZone tz = Calendar.getInstance().getTimeZone();
		int offset = tz.getRawOffset();
		int hours   = (int) (( offset / (1000*60*60)) % 24);
		String sign = offset > 0 ?  "+" : "";
		return dateString + "[" + sign + hours + ":" + tz.getDisplayName(false, TimeZone.SHORT, Locale.getDefault()) + "]";
	}
	
	/**
	 * Converts all expenses into OFX XML format and adds them to the XML document
	 * @param doc DOM document of the OFX expenses.
	 * @param parent Parent node for all expenses in report
	 */
	public void toOfx(Document doc, Element parent){
		Element transactionUid = doc.createElement("TRNUID");		
		//unsolicited because the data exported is not as a result of a request
		transactionUid.appendChild(doc.createTextNode(UNSOLICITED_TRANSACTION_ID));

		Element statementTransactionResponse = doc.createElement("STMTTRNRS");
		statementTransactionResponse.appendChild(transactionUid);
		
		Element bankmsgs = doc.createElement("BANKMSGSRSV1");
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
