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

import android.content.Context;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.data.Account.OfxAccountType;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.export.qif.QifHelper;
import org.gnucash.android.util.OfxFormatter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.content.Intent;

/**
 * Represents a financial transaction, either credit or debit.
 * Transactions belong to accounts and each have the unique identifier of the account to which they belong.
 * The default type is a debit, unless otherwise specified.
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class Transaction {
	/**
	 * Type of transaction, a credit or a debit
	 */
	public enum TransactionType {DEBIT, CREDIT};
	
	/**
	 * Mime type for transactions in Gnucash. 
	 * Used for recording transactions through intents
	 */
	public static final String MIME_TYPE 			= "vnd.android.cursor.item/vnd.org.gnucash.android.transaction";
	
	/**
	 * Key for passing the account unique Identifier as an argument through an {@link Intent}
	 */
	public static final String EXTRA_ACCOUNT_UID 	= "org.gnucash.android.extra.account_uid";
	
	/**
	 * Key for specifying the double entry account
	 */
	public static final String EXTRA_DOUBLE_ACCOUNT_UID = "org.gnucash.android.extra.double_account_uid";
	
	/**
	 * Key for identifying the amount of the transaction through an Intent
	 */
	public static final String EXTRA_AMOUNT 		= "org.gnucash.android.extra.amount";
	
	/**
	 * {@link Money} value of this transaction
	 */
	private Money mAmount;
	
	/**
	 * Unique identifier of the transaction. 
	 * This is automatically generated when the transaction is created.
	 */
	private String mTransactionUID;
	
	/**
	 * Name describing the transaction
	 */
	private String mName;
	
	/**
	 * An extra note giving details about the transaction
	 */
	private String mDescription = "";
	
	/**
	 * Unique Identifier of the account to which this transaction belongs
	 */
	private String mAccountUID = null;
	
	/**
	 * Unique Identifier of the account which is used for double entry of this transaction
	 * This value is null by default for transactions not using double entry
	 */
	private String mDoubleEntryAccountUID = null;
	
	/**
	 * Flag indicating if this transaction has been exported before or not
	 * The transactions are typically exported as bank statement in the OFX format
	 */
	private int mIsExported = 0;
	
	/**
	 * Timestamp when this transaction occurred
	 */
	private long mTimestamp;
	
	/**
	 * Type of transaction, either credit or debit
	 * @see TransactionType
	 */
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
	 * @param amount Amount of the transaction
	 */
	public void setAmount(Money amount) {
		this.mAmount = amount;
		mType = amount.isNegative() ? TransactionType.DEBIT : TransactionType.CREDIT; 
	}

	/**
	 * Set the amount of this transaction
	 * @param amount Amount of the transaction
	 */
	public void setAmount(String amount) {
		this.mAmount = new Money(amount);
	}
	
	/**
	 * Sets the amount and currency of the transaction
	 * @param amount String containing number value of transaction amount
	 * @param currencyCode ISO 4217 currency code
	 */
	public void setAmount(String amount, String currencyCode){
		this.mAmount = new Money(new BigDecimal(amount),
								 Currency.getInstance(currencyCode));
	}

	/**
	 * Sets the currency of the transaction
	 * The currency remains in the object model and is not persisted to the database
	 * Transactions always use the currency of their accounts
	 * @param currency {@link Currency} of the transaction value
	 */
	public void setCurrency(Currency currency){		
		mAmount = mAmount.withCurrency(currency);
	}
	
	/**
	 * Sets the amount of the transaction
	 * @param amount Amount value of the transaction
	 * @param currency {@link Currency} of the transaction
	 */
	public void setAmount(double amount, Currency currency){
		this.mAmount = new Money(new BigDecimal(amount), currency);
	}
	
	/**
	 * Returns the amount involved in this transaction
	 * @return {@link Money} amount in the transaction
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
	
	/**
	 * Sets the time when the transaction occurred
	 * @param timeInMillis Time in milliseconds
	 */
	public void setTime(long timeInMillis) {
		this.mTimestamp = timeInMillis;
	}
	
	/**
	 * Returns the time of transaction in milliseconds
	 * @return Time when transaction occurred in milliseconds 
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
	 * Returns the Unique Identifier of account with which this transaction is double entered
	 * @return Unique ID of transfer account or <code>null</code> if it is not a double transaction
	 */
	public String getDoubleEntryAccountUID() {
		return mDoubleEntryAccountUID;
	}

	/**
	 * Sets the account UID with which to double enter this transaction
	 * @param doubleEntryAccountUID Unique Identifier to set
	 */
	public void setDoubleEntryAccountUID(String doubleEntryAccountUID) {
		this.mDoubleEntryAccountUID = doubleEntryAccountUID;
	}
	

	/**
	 * Returns UID of account to which this transaction belongs
	 * @return the UID of the account to which this transaction belongs
	 */
	public String getAccountUID() {
		return mAccountUID;
	}
	
	/**
	 * Sets the exported flag on the transaction
	 * @param isExported <code>true</code> if the transaction has been exported, <code>false</code> otherwise
	 */
	public void setExported(boolean isExported){
		mIsExported = isExported ? 1 : 0;
	}
	
	/**
	 * Returns <code>true</code> if the transaction has been exported, <code>false</code> otherwise
	 * @return <code>true</code> if the transaction has been exported, <code>false</code> otherwise
	 */
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
	 * returns the element node for the transaction.
	 * The Unique ID of the account is needed in order to properly export double entry transactions
	 * @param doc XML document to which transaction should be added
	 * @param accountUID Unique Identifier of the account which called the method.
	 * @return Element in DOM corresponding to transaction
	 */
	public Element toOfx(Document doc, String accountUID){		
		Element transactionNode = doc.createElement("STMTTRN");
		Element type = doc.createElement("TRNTYPE");
		type.appendChild(doc.createTextNode(mType.toString()));
		transactionNode.appendChild(type);

		Element datePosted = doc.createElement("DTPOSTED");
		datePosted.appendChild(doc.createTextNode(OfxFormatter.getOfxFormattedTime(mTimestamp)));
		transactionNode.appendChild(datePosted);
		
		Element dateUser = doc.createElement("DTUSER");
		dateUser.appendChild(doc.createTextNode(
				OfxFormatter.getOfxFormattedTime(mTimestamp)));
		transactionNode.appendChild(dateUser);
		
		Element amount = doc.createElement("TRNAMT");
		amount.appendChild(doc.createTextNode(mAmount.toPlainString()));
		transactionNode.appendChild(amount);
		
		Element transID = doc.createElement("FITID");
		transID.appendChild(doc.createTextNode(mTransactionUID));
		transactionNode.appendChild(transID);
		
		Element name = doc.createElement("NAME");
		name.appendChild(doc.createTextNode(mName));
		transactionNode.appendChild(name);
		
		if (mDescription != null && mDescription.length() > 0){
			Element memo = doc.createElement("MEMO");
			memo.appendChild(doc.createTextNode(mDescription));
			transactionNode.appendChild(memo);
		}
		
		if (mDoubleEntryAccountUID != null && mDoubleEntryAccountUID.length() > 0){
			Element bankId = doc.createElement("BANKID");
			bankId.appendChild(doc.createTextNode(OfxFormatter.APP_ID));
			
			//select the proper account as the double account
			String doubleAccountUID = mDoubleEntryAccountUID.equals(accountUID) ? mAccountUID : mDoubleEntryAccountUID;
			
			Element acctId = doc.createElement("ACCTID");
			acctId.appendChild(doc.createTextNode(doubleAccountUID));
			
			Element accttype = doc.createElement("ACCTTYPE");			
			AccountsDbAdapter acctDbAdapter = new AccountsDbAdapter(GnuCashApplication.getAppContext());
			OfxAccountType ofxAccountType = Account.convertToOfxAccountType(acctDbAdapter.getAccountType(doubleAccountUID));
			accttype.appendChild(doc.createTextNode(ofxAccountType.toString()));
			acctDbAdapter.close();
			
			Element bankAccountTo = doc.createElement("BANKACCTTO");
			bankAccountTo.appendChild(bankId);
			bankAccountTo.appendChild(acctId);
			bankAccountTo.appendChild(accttype);
			
			transactionNode.appendChild(bankAccountTo);
		}
		
		return transactionNode;
	}

    /**
     * Builds a QIF entry representing this transaction
     * @param context Application context
     * @return String QIF representation of this transaction
     */
    public String toQIF(Context context){
        final String newLine = "\n";

        AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(context);

        StringBuffer transactionQifBuffer = new StringBuffer();
        transactionQifBuffer.append(QifHelper.DATE_PREFIX + QifHelper.formatDate(mTimestamp) + newLine);

        if (mDoubleEntryAccountUID != null && !mDoubleEntryAccountUID.isEmpty()){
            String splitAccountFullName = accountsDbAdapter.getFullyQualifiedAccountName(mDoubleEntryAccountUID);
            transactionQifBuffer.append(QifHelper.SPLIT_CATEGORY_PREFIX + splitAccountFullName + newLine);
            if (mDescription != null || mDescription.isEmpty()){
                transactionQifBuffer.append(QifHelper.SPLIT_MEMO_PREFIX + mDescription + newLine);
            }
            transactionQifBuffer.append(QifHelper.SPLIT_AMOUNT_PREFIX + mAmount.negate().asString() + newLine);
        } else {
            transactionQifBuffer.append(QifHelper.AMOUNT_PREFIX + mAmount.asString() + newLine);
            if (mDescription != null && !mDescription.isEmpty()){
                transactionQifBuffer.append(QifHelper.MEMO_PREFIX + mDescription + newLine);
            }
            transactionQifBuffer.append(QifHelper.CATEGORY_PREFIX + QifHelper.getImbalanceAccountName(mAmount.getCurrency()) + newLine);
        }

        transactionQifBuffer.append(QifHelper.ENTRY_TERMINATOR + newLine);

        accountsDbAdapter.close();
        return transactionQifBuffer.toString();
    }
}
