/*
 * Written By: Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2012 Ngewi Fet
 *
 * This file is part of Gnucash for Android
 * 
 * Gnucash for Android is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, contact:
 *
 * Free Software Foundation           Voice:  +1-617-542-5942
 * 51 Franklin Street, Fifth Floor    Fax:    +1-617-542-2652
 * Boston, MA  02110-1301,  USA       gnu@gnu.org
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

	public final static SimpleDateFormat ofxDateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
	
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
	
	private boolean mExportAll = false;
	private Context mContext;
	
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
	
	public static String getFormattedCurrentTime(){
		return getFormattedCurrentTime(System.currentTimeMillis());
	}
	
	public static String getFormattedCurrentTime(long milliseconds){
		Date date = new Date(milliseconds);
		String dateString = ofxDateFormatter.format(date);
		TimeZone tz = Calendar.getInstance().getTimeZone();
		int offset = tz.getRawOffset();
		int hours   = (int) (( offset / (1000*60*60)) % 24);
		String sign = offset > 0 ?  "+" : "";
		return dateString + "[" + sign + hours + ":" + tz.getDisplayName(false, TimeZone.SHORT, Locale.getDefault()) + "]";
	}
	
	/**
	 * Converts all expenses into OFX XML format and adds them to the XML document
	 * @param doc DOM document of the OFX expenses
	 * @param parent Parent node for all expenses in report
	 */
	public void toXml(Document doc, Element parent){
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
			
			Element currency = doc.createElement("CURDEF");
			currency.appendChild(doc.createTextNode(account.getCurrency().getCurrencyCode()));						
			
			//================= BEGIN BANK ACCOUNT INFO (BANKACCTFROM) =================================
			
			Element bankId = doc.createElement("BANKID");
			bankId.appendChild(doc.createTextNode(APP_ID));
			
			Element acctId = doc.createElement("ACCTID");
			acctId.appendChild(doc.createTextNode(account.getUID()));
			
			Element accttype = doc.createElement("ACCTTYPE");
			accttype.appendChild(doc.createTextNode(account.getAccountType().toString()));
			
			Element bankFrom = doc.createElement("BANKACCTFROM");
			bankFrom.appendChild(bankId);
			bankFrom.appendChild(acctId);
			bankFrom.appendChild(accttype);
			
			//================= END BANK ACCOUNT INFO ============================================
			
			
			//================= BEGIN ACCOUNT BALANCE INFO =================================
			String balance = account.getBalance().toPlainString();
			String formattedCurrentTimeString = getFormattedCurrentTime();
			
			Element balanceAmount = doc.createElement("BALAMT");
			balanceAmount.appendChild(doc.createTextNode(balance));			
			Element dtasof = doc.createElement("DTASOF");
			dtasof.appendChild(doc.createTextNode(formattedCurrentTimeString));
			
			Element ledgerBalance = doc.createElement("LEDGERBAL");
			ledgerBalance.appendChild(balanceAmount);
			ledgerBalance.appendChild(dtasof);
			
			//================= END ACCOUNT BALANCE INFO =================================
			
			
			//================= BEGIN TIME PERIOD INFO =================================
			
			Element dtstart = doc.createElement("DTSTART");			
			dtstart.appendChild(doc.createTextNode(formattedCurrentTimeString));
			
			Element dtend = doc.createElement("DTEND");
			dtend.appendChild(doc.createTextNode(formattedCurrentTimeString));
			
			Element bankTransactionsList = doc.createElement("BANKTRANLIST");
			bankTransactionsList.appendChild(dtstart);
			bankTransactionsList.appendChild(dtend);
			
			//================= END TIME PERIOD INFO =================================
			
						
			Element statementTransactions = doc.createElement("STMTRS");
			statementTransactions.appendChild(currency);
			statementTransactions.appendChild(bankFrom);
			statementTransactions.appendChild(bankTransactionsList);
			statementTransactions.appendChild(ledgerBalance);
			
			statementTransactionResponse.appendChild(statementTransactions);
			
			//add account details (transactions) to the XML document			
			account.toXml(doc, bankTransactionsList, mExportAll);
			
			//mark as exported
			transactionsDbAdapter.markAsExported(account.getUID());
			
		}
		transactionsDbAdapter.close();
	}
}
