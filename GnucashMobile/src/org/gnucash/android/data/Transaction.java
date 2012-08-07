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
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Data model representation of a transaction
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class Transaction {
	/**
	 * Type of transaction, a credit or a debit
	 * 
	 */
	public enum TransactionType {DEBIT, CREDIT};
	
	public static final String MIME_TYPE 			= "vnd.android.cursor.item/vnd.org.gnucash.android.transaction";
	public static final String EXTRA_ACCOUNT_UID 	= "org.gnucash.android.extra.account_uid";
	public static final String EXTRA_AMOUNT 		= "org.gnucash.android.extra.amount";
	
	private Money mAmount;
	private String mTransactionUID;
	private String mName;
	private String mDescription = "";
	private String mAccountUID = null;
	private int mIsExported = 0;
	private long mTimestamp;
	private TransactionType mType = TransactionType.DEBIT;
	
	/**
	 * Overloaded constructor. Creates a new transaction instance with the 
	 * provided data and initializes the rest to default values. 
	 * @param amount Amount for the transaction
	 * @param name Name of the transaction
	 */
	public Transaction(Money amount, String name) {
		initDefaults();		
		setName(name);
		setAmount(amount); //takes care of setting the type for us
	}
	
	public Transaction(double amount, String name){
		initDefaults();
		setName(name);
		setAmount(amount);
	}
	
	/**
	 * Overloaded constructor. Creates a new transaction instance with the 
	 * provided data and initializes the rest to default values. 
	 * @param amount Amount for the transaction
	 * @param name Name of the transaction
	 */
	public Transaction(String amount, String name) {
		initDefaults();		
		setName(name);
		setAmount(amount); //takes care of setting the type for us
	}
	
	/**
	 * Overloaded constructor. Creates a new transaction instance with the 
	 * provided data and initializes the rest to default values. 
	 * @param amount Amount for the transaction
	 * @param name Name of the transaction
	 * @param type Type of transaction
	 */
	public Transaction(Money amount, String name, TransactionType type){
		initDefaults();
		setAmount(amount);		
		this.mType = type;
		this.mName = name;
	}
	
	/**
	 * Initializes the different fields to their default values.
	 */
	private void initDefaults(){
		setAmount(new Money());
		this.mTimestamp = System.currentTimeMillis();
		this.mType = TransactionType.DEBIT;
		mTransactionUID = UUID.randomUUID().toString();
		
	}
	
	/**
	 * Set the amount of this transaction
	 * @param mAmount Amount of the transaction
	 */
	public void setAmount(Money amount) {
		this.mAmount = amount;
		mType = amount.isNegative() ? TransactionType.DEBIT : TransactionType.CREDIT; 
	}

	/**
	 * Set the amount of this transaction
	 * @param mAmount Amount of the transaction
	 */
	public void setAmount(String amount) {
		this.mAmount = new Money(amount);
	}
	
	public void setAmount(String amount, String currencyCode){
		this.mAmount = new Money(new BigDecimal(amount),
								 Currency.getInstance(currencyCode));
	}
	
	public void setAmount(double amount){
		setAmount(new Money(amount));
	}
	
	public void setCurrency(Currency currency){		
		mAmount = mAmount.withCurrency(currency);
	}
	
	public void setAmount(double amount, Currency currency){
		this.mAmount = new Money(new BigDecimal(amount), currency);
	}
	
	/**
	 * Returns the amount involved in this transaction
	 * @return Amount in the transaction
	 */
	public Money getAmount() {
		return mAmount;
	}
	
	/**
	 * Returns the transaction properly formatted for display
	 * @return Properly formatted string amount
	 */
	public String getFormattedAmount(){		
		return mAmount.formattedString(Locale.getDefault());		
	}
	
	/**
	 * Returns the name of the transaction
	 * @return Name of the transaction
	 */
	public String getName() {
		return mName;
	}

	/**
	 * Sets the name of the transaction
	 * @param name String containing name of transaction to set
	 */
	public void setName(String name) {
		this.mName = name.trim();
	}

	/**
	 * Set short description of the transaction
	 * @param description String containing description of transaction
	 */
	public void setDescription(String description) {
		this.mDescription = description;
	}

	/**
	 * Returns the description of the transaction
	 * @return String containing description of transaction
	 */
	public String getDescription() {
		return mDescription;
	}

	/**
	 * Set the time of the transaction
	 * @param timestamp Time when transaction occurred as {@link Date}
	 */
	public void setTime(Date timestamp){
		this.mTimestamp = timestamp.getTime();
	}
	
	public void setTime(long timeInMillis) {
		this.mTimestamp = timeInMillis;
	}
	
	/**
	 * Returns the time of transaction in milliseconds
	 * @return Time when transaction occured in milliseconds 
	 */
	public long getTimeMillis(){
		return mTimestamp;
	}
	
	/**
	 * Sets the type of transaction
	 * @param type The transaction type 
	 * @see TransactionType 
	 */
	public void setTransactionType(TransactionType type){
		this.mType = type;
	}
		
	/**
	 * Returns the type of transaction
	 * @return Type of transaction
	 */
	public TransactionType getTransactionType(){
		return this.mType;
	}
	
	/**
	 * Set Unique Identifier for this transaction
	 * @param transactionUID Unique ID string
	 */
	public void setUID(String transactionUID) {
		this.mTransactionUID = transactionUID;
	}

	/**
	 * Returns unique ID string for transaction
	 * @return String with Unique ID of transaction
	 */
	public String getUID() {
		return mTransactionUID;
	}

	/**
	 * Returns UID of account to which this transaction belongs
	 * @return the UID of the account to which this transaction belongs
	 */
	public String getAccountUID() {
		return mAccountUID;
	}
	
	public void setExported(boolean isExported){
		mIsExported = isExported ? 1 : 0;
	}
	
	public boolean isExported(){
		return mIsExported == 1;
	}
	
	/**
	 * Set the account UID of the account to which this transaction belongs
	 * @param accountUID the UID of the account which owns this transaction
	 */
	public void setAccountUID(String accountUID) {
		this.mAccountUID = accountUID;
	}
	
	/**
	 * Converts transaction to XML DOM corresponding to OFX Statement transaction and 
	 * returns the element node for the transaction
	 * @param doc XML document to which transaction should be added
	 * @return Element in DOM corresponding to transaction
	 */
	public Element toXml(Document doc){		
		Element transactionNode = doc.createElement("STMTTRN");
		Element type = doc.createElement("TRNTYPE");
		type.appendChild(doc.createTextNode(mType.toString()));
		transactionNode.appendChild(type);

/* TODO Remove references to expenses
		Element datePosted = doc.createElement("DTPOSTED");
		datePosted.appendChild(doc.createTextNode(Expenses.getFormattedCurrentTime(mTimestamp.getTime())));
		transaction.appendChild(datePosted);
		
		Element dateUser = doc.createElement("DTUSER");
		dateUser.appendChild(doc.createTextNode(
				Expenses.getFormattedCurrentTime(mTimestamp.getTime())));
		transaction.appendChild(dateUser);
*/		
		Element amount = doc.createElement("TRNAMT");
		amount.appendChild(doc.createTextNode(mAmount.toPlainString()));
		transactionNode.appendChild(amount);
		
		Element transID = doc.createElement("FITID");
		transID.appendChild(doc.createTextNode(mTransactionUID));
		transactionNode.appendChild(transID);
		
		Element name = doc.createElement("NAME");
		name.appendChild(doc.createTextNode(mName));
		transactionNode.appendChild(name);
		
		Element memo = doc.createElement("MEMO");
		memo.appendChild(doc.createTextNode(mDescription));
		transactionNode.appendChild(memo);
		
		return transactionNode;
	}

}
