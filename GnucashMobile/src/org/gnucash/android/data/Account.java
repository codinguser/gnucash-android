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

package org.gnucash.android.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.gnucash.android.ui.MainActivity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An account within which many {@link Transaction}s occur
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class Account {

	/**
	 * The type of account
	 *
	 */
	public enum AccountType {CHECKING, SAVINGS, MONEYMRKT, CREDITLINE};
	
	/**
	 * Account ID
	 */
	private String mUID;
	
	/**
	 * Name of this account
	 */
	private String mName;
	
	private Currency mCurrency; 
	
	private AccountType mAccountType = AccountType.CHECKING;
	
	/**
	 * List of transactions in this account
	 */
	private List<Transaction> mTransactionsList = new ArrayList<Transaction>();
	
	/**
	 * Constructor
	 * @param name Name of the account
	 */
	public Account(String name) {
		setName(name);
		this.mUID = generateUID();
		this.mCurrency = Currency.getInstance(MainActivity.DEFAULT_CURRENCY_CODE);
	}

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
	 * Generates a unique ID for the account that includes the 
	 * name and a random string. This represents the ACCTID in the exported OFX
	 * and should have a maximum of 22 alphanumeric characters
	 * @return Generated Unique ID string
	 */
	protected String generateUID(){
		String uuid = UUID.randomUUID().toString();
		
		if (mName == null || mName.length() == 0){
			//if we do not have a name, return pure random
			return uuid.substring(0, 22);
		}
		
		uuid = uuid.substring(uuid.lastIndexOf("-"));
		String name = mName.toLowerCase().replace(" ", "-");
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
	 * @param transaction {@link Transaction} to be added to the account
	 */
	public void addTransaction(Transaction transaction){
		transaction.setAccountUID(getUID());
		transaction.setCurrency(mCurrency);
		mTransactionsList.add(transaction);
	}
	
	/**
	 * Sets a list of transactions for this acccount.
	 * Overrides any previous transactions with those in the list
	 * @param transactionsList List of transactions to be set.
	 */
	public void setTransactions(List<Transaction> transactionsList){
		for (Transaction transaction : transactionsList) {
			transaction.setAccountUID(getUID());
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
	 * Returns true if there is atleast one transaction in the account
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
	 * @return Aggregate amount of all transactions in account.
	 */
	public BigDecimal getBalance(){
		BigDecimal balance = new BigDecimal(0);
		for (Transaction transx : mTransactionsList) {
			balance.add(transx.getAmount().asBigDecimal());		}
		return balance;
	}
	
	/**
	 * @return the mCurrency
	 */
	public Currency getCurrency() {
		return mCurrency;
	}

	/**
	 * @param mCurrency the mCurrency to set
	 */
	public void setCurrency(Currency mCurrency) {		
		this.mCurrency = mCurrency;
		//TODO: Maybe at some time t, this method should convert all 
		//transaction values to the corresponding value in the new currency
	}

	/**
	 * Converts this account's transactions into XML and adds them to the DOM document
	 * @param doc XML DOM document for the OFX data
	 * @param parent Node to which to add this account's transactions
	 */
	public void toXml(Document doc, Element parent, boolean allTransactions){
		for (Transaction transaction : mTransactionsList) {
			if (!allTransactions && transaction.isExported())
				continue;
			parent.appendChild(transaction.toXml(doc));
		}
	}
}
