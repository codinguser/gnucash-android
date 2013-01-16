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

package org.gnucash.android.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.gnucash.android.util.OfxFormatter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An account represents a transaction account in with {@link Transaction}s may be recorded
 * Accounts have different types as specified by {@link AccountType} and also a currency with
 * which transactions may be recorded in the account
 * By default, an account is made an {@link AccountType#CHECKING} and the default currency is
 * the currency of the Locale of the device on which the software is running. US Dollars is used
 * if the platform locale cannot be determined.
 * 
 * @author Ngewi Fet <ngewif@gmail.com>
 * @see AccountType
 */
public class Account {

	/**
	 * The MIME type for accounts in GnucashMobile
	 * This is used when sending intents from third-party applications
	 */
	public static final String MIME_TYPE = "vnd.android.cursor.item/vnd.org.gnucash.android.account";
	
	/**
	 * The type of account
	 * This are the different types specified by the OFX format and 
	 * they are currently not used except for exporting
	 */
	public enum AccountType {CASH, BANK, CREDIT_CARD, ASSET, LIABILITY, INCOME, EXPENSE, 
		EQUITY, CURRENCY, STOCK, MUTUAL_FUND, CHECKING, SAVINGS, MONEYMRKT, CREDITLINE};
	
	public enum OfxAccountType {CHECKING, SAVINGS, MONEYMRKT, CREDITLINE };
		
	/**
	 * Unique Identifier of the account
	 * It is generated when the account is created and can be set a posteriori as well
	 */
	private String mUID;
	
	/**
	 * Name of this account
	 */
	private String mName;
	
	/**
	 * Currency used by transactions in this account
	 */
	private Currency mCurrency; 
	
	/**
	 * Type of account
	 * Defaults to {@link AccountType#CHECKING}
	 */
	private AccountType mAccountType = AccountType.CASH;
	
	/**
	 * List of transactions in this account
	 */
	private List<Transaction> mTransactionsList = new ArrayList<Transaction>();

	/**
	 * Account UID of the parent account
	 */
	private String mParentAccountUID;

	/**
	 * An extra key for passing the currency code (according ISO 4217) in an intent
	 */
	public static final String EXTRA_CURRENCY_CODE 	= "org.gnucash.android.extra.currency_code";
	
	/**
	 * Constructor
	 * Creates a new account with the default currency and a generated unique ID
	 * @param name Name of the account
	 */
	public Account(String name) {
		setName(name);
		this.mUID = generateUID();
		this.mCurrency = Currency.getInstance(Money.DEFAULT_CURRENCY_CODE);
	}
	
	/**
	 * Overloaded constructor
	 * @param name Name of the account
	 * @param currency {@link Currency} to be used by transactions in this account
	 */
	public Account(String name, Currency currency){
		setName(name);
		this.mUID = generateUID();
		this.mCurrency = currency;
	}

	/**
	 * Sets the name of the account
	 * @param name String name of the account
	 */
	public void setName(String name) {
		this.mName = name.trim();
	}

	/**
	 * Returns the name of the account
	 * @return String containing name of the account
	 */
	public String getName() {
		return mName;
	}
	
	/**
	 * Generates a unique ID for the account based on the name and a random string. 
	 * This represents the ACCTID in the exported OFX and should have a maximum of 22 alphanumeric characters
	 * @return Generated Unique ID string
	 */
	protected String generateUID(){
		String uuid = UUID.randomUUID().toString();
		
		if (mName == null || mName.length() == 0){
			//if we do not have a name, return pure random
			return uuid.substring(0, 22);
		}
		
		uuid = uuid.substring(uuid.lastIndexOf("-"));
		String name = mName.toLowerCase(Locale.US).replace(" ", "-");
		if (name.length() > 9)
			name = name.substring(0, 10);
		uuid = name + uuid;		
		return uuid;
	}
	
	/**
	 * Returns the unique ID of this account
	 * @return String containing unique ID for the account
	 */
	public String getUID(){
		return mUID;
	}
	
	/**
	 * Sets the unique identifier of this acocunt
	 * @param uid Unique identifier to be set
	 */
	public void setUID(String uid){
		this.mUID = uid;
	}
	
	/**
	 * Get the type of account
	 * @return {@link AccountType} type of account
	 */
	public AccountType getAccountType() {
		return mAccountType;
	}

	/**
	 * Sets the type of account
	 * @param mAccountType Type of account
	 * @see AccountType
	 */
	public void setAccountType(AccountType mAccountType) {
		this.mAccountType = mAccountType;
	}

	/**
	 * Adds a transaction to this account
	 * The currency of the transaction will be set to the currency of the account
	 * if they are not the same. The currency value conversion is performed, just 
	 * a different currecy is assigned to the same value amount in the transaction.
	 * @param transaction {@link Transaction} to be added to the account
	 */
	public void addTransaction(Transaction transaction){
		transaction.setAccountUID(getUID());
		transaction.setCurrency(mCurrency);
		mTransactionsList.add(transaction);
	}
	
	/**
	 * Sets a list of transactions for this account.
	 * Overrides any previous transactions with those in the list.
	 * The account UID and currency of the transactions will be set to the unique ID 
	 * and currency of the account respectively
	 * @param transactionsList List of {@link Transaction}s to be set.
	 */
	public void setTransactions(List<Transaction> transactionsList){
		for (Transaction transaction : transactionsList) {
			transaction.setAccountUID(getUID());
			transaction.setCurrency(mCurrency);
		}
		this.mTransactionsList = transactionsList;
	}
		
	/**
	 * Removes <code>transaction</code> from this account
	 * @param transaction {@link Transaction} to be removed from account
	 */
	public void removeTransaction(Transaction transaction){
		mTransactionsList.remove(transaction);
	}
	
	/**
	 * Returns a list of transactions for this account
	 * @return Array list of transactions for the account
	 */
	public List<Transaction> getTransactions(){
		return mTransactionsList;
	}
	
	/**
	 * Returns the number of transactions in this account
	 * @return Number transactions in account
	 */
	public int getTransactionCount(){
		return mTransactionsList.size();
	}
	
	/**
	 * Returns true if there is at least one transaction in the account
	 * which has not yet been exported
	 * @return <code>true</code> if there are unexported transactions, <code>false</code> otherwise.
	 */
	public boolean hasUnexportedTransactions(){
		for (Transaction transaction : mTransactionsList) {
			if (transaction.isExported() == false)
				return true;			
		}
		return false;
	}
	
	/**
	 * Returns the aggregate of all transactions in this account.
	 * It takes into account debit and credit amounts
	 * @return {@link Money} aggregate amount of all transactions in account.
	 */
	public Money getBalance(){
		//TODO: Consider double entry transactions
		Money balance = new Money(new BigDecimal(0), this.mCurrency);
		for (Transaction transx : mTransactionsList) {
			balance = balance.add(transx.getAmount());		
		}
		return balance;
	}
	
	/**
	 * @return the mCurrency
	 */
	public Currency getCurrency() {
		return mCurrency;
	}

	/**
	 * Sets the currency to be used by this account
	 * @param mCurrency the mCurrency to set
	 */
	public void setCurrency(Currency mCurrency) {		
		this.mCurrency = mCurrency;
		//TODO: Maybe at some time t, this method should convert all 
		//transaction values to the corresponding value in the new currency
	}

	/**
	 * Sets the Unique Account Identifier of the parent account
	 * @param parentUID String Unique ID of parent account
	 */
	public void setParentUID(String parentUID){
		mParentAccountUID = parentUID;
	}
	
	/**
	 * Returns the Unique Account Identifier of the parent account
	 * @return String Unique ID of parent account
	 */
	public String getParentUID() {
		return mParentAccountUID;
		
	}

	/**
	 * Maps the <code>accountType</code> to the corresponding account type.
	 * <code>accountType</code> have corresponding values to GnuCash desktop
	 * @param accountType {@link AccountType} of an account
	 * @return Corresponding {@link OfxAccountType} for the <code>accountType</code>
	 * @see AccountType
	 * @see OfxAccountType
	 */
	public OfxAccountType ofxAccountTypeMapping(AccountType accountType){
		switch (accountType) {
		case CREDITLINE:
			return OfxAccountType.CREDITLINE;
			
		case CASH:
		case INCOME:
		case EXPENSE:
		case CURRENCY:
			return OfxAccountType.CHECKING;
			
		case BANK:
		case ASSET:
			return OfxAccountType.SAVINGS;
			
		case MUTUAL_FUND:
		case STOCK:
		case EQUITY:
			return OfxAccountType.MONEYMRKT;

		default:
			return OfxAccountType.CHECKING;
		}
	}
	
	/**
	 * Converts this account's transactions into XML and adds them to the DOM document
	 * @param doc XML DOM document for the OFX data
	 * @param parent Parent node to which to add this account's transactions in XML
	 */
	public void toOfx(Document doc, Element parent, boolean allTransactions){
		Element currency = doc.createElement("CURDEF");
		currency.appendChild(doc.createTextNode(mCurrency.getCurrencyCode()));						
		
		//================= BEGIN BANK ACCOUNT INFO (BANKACCTFROM) =================================
		
		Element bankId = doc.createElement("BANKID");
		bankId.appendChild(doc.createTextNode(OfxFormatter.APP_ID));
		
		Element acctId = doc.createElement("ACCTID");
		acctId.appendChild(doc.createTextNode(mUID));
		
		Element accttype = doc.createElement("ACCTTYPE");
		accttype.appendChild(doc.createTextNode(mAccountType.toString()));
		
		Element bankFrom = doc.createElement("BANKACCTFROM");
		bankFrom.appendChild(bankId);
		bankFrom.appendChild(acctId);
		bankFrom.appendChild(accttype);
		
		//================= END BANK ACCOUNT INFO ============================================
		
		
		//================= BEGIN ACCOUNT BALANCE INFO =================================
		String balance = getBalance().toPlainString();
		String formattedCurrentTimeString = OfxFormatter.getFormattedCurrentTime();
		
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
		
		//================= END TIME PERIOD INFO =================================
		
		
		//================= BEGIN TRANSACTIONS LIST =================================
		Element bankTransactionsList = doc.createElement("BANKTRANLIST");
		bankTransactionsList.appendChild(dtstart);
		bankTransactionsList.appendChild(dtend);
		
		for (Transaction transaction : mTransactionsList) {
			if (!allTransactions && transaction.isExported())
				continue;
			
			bankTransactionsList.appendChild(transaction.toOfx(doc, mUID));
		}		
		//================= END TRANSACTIONS LIST =================================
					
		Element statementTransactions = doc.createElement("STMTRS");
		statementTransactions.appendChild(currency);
		statementTransactions.appendChild(bankFrom);
		statementTransactions.appendChild(bankTransactionsList);
		statementTransactions.appendChild(ledgerBalance);
		
		parent.appendChild(statementTransactions);
				
	}

}
