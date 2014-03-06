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

package org.gnucash.android.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.model.Account.OfxAccountType;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.export.ofx.OfxHelper;
import org.gnucash.android.export.qif.QifHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.content.Intent;

/**
 * Represents a financial transaction, either credit or debit.
 * Transactions belong to accounts and each have the unique identifier of the account to which they belong.
 * The default type is a debit, unless otherwise specified.
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class OriginalTransaction implements Transaction {
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
     * Recurrence period of this transaction.
     * <p>If this value is set then it means this transaction is a template which will be used to
     * create a transaction every turn of the recurrence period</p>
     */
    private long mRecurrencePeriod = 0;

	/**
	 * Overloaded constructor. Creates a new transaction instance with the 
	 * provided data and initializes the rest to default values. 
	 * @param amount Amount for the transaction
	 * @param name Name of the transaction
	 */
	public OriginalTransaction(Money amount, String name) {
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
	public OriginalTransaction(String amount, String name) {
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
	public OriginalTransaction(Money amount, String name, TransactionType type){
		initDefaults();
		setAmount(amount);		
		this.mType = type;
		this.mName = name;
	}

    /**
     * Copy constructor.
     * Creates a new transaction object which is a clone of the parameter.
     * <p><b>Note:</b> The unique ID of the transaction is not cloned, but a new one is generated.</p>
     * @param transaction Transaction to be cloned
     */
    public OriginalTransaction(Transaction transaction){
        initDefaults();
        setName(transaction.getName());
        setDescription(transaction.getDescription());
        setAmount(transaction.getAmount());
        setTransactionType(transaction.getTransactionType());
        setAccountUID(transaction.getAccountUID());
        setDoubleEntryAccountUID(transaction.getDoubleEntryAccountUID());
        setExported(transaction.isExported());
        setTime(transaction.getTimeMillis());
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
	
	public void setAmount(Money amount) {
		this.mAmount = new Money(amount);
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
	
	public String getName() {
		return mName;
	}

	public void setName(String name) {
		this.mName = name.trim();
	}

	public void setDescription(String description) {
		this.mDescription = description;
	}

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
	
	public long getTimeMillis(){
		return mTimestamp;
	}
	
	public void setTransactionType(TransactionType type){
		this.mType = type;
	}
		
	public TransactionType getTransactionType(){
		return this.mType;
	}
	
	public void setUID(String transactionUID) {
		this.mTransactionUID = transactionUID;
	}

    /**
     * Resets the UID of this transaction to a newly generated one
     */
    public void resetUID(){
        this.mTransactionUID = UUID.randomUUID().toString();
    }

	public String getUID() {
		return mTransactionUID;
	}

	public String getDoubleEntryAccountUID() {
		return mDoubleEntryAccountUID;
	}

	public void setDoubleEntryAccountUID(String doubleEntryAccountUID) {
		this.mDoubleEntryAccountUID = doubleEntryAccountUID;
	}

	public String getAccountUID() {
		return mAccountUID;
	}
	
	public void setExported(boolean isExported){
		mIsExported = isExported ? 1 : 0;
	}
	
	public boolean isExported(){
		return mIsExported == 1;
	}
	
	public void setAccountUID(String accountUID) {
		this.mAccountUID = accountUID;
	}

    public long getRecurrencePeriod() {
        return mRecurrencePeriod;
    }

    public void setRecurrencePeriod(long recurrenceId) {
        this.mRecurrencePeriod = recurrenceId;
    }

	public Element toOfx(Document doc, String accountUID){
		Element transactionNode = doc.createElement(OfxHelper.TAG_STATEMENT_TRANSACTION);
		Element type = doc.createElement(OfxHelper.TAG_TRANSACTION_TYPE);
		type.appendChild(doc.createTextNode(mType.toString()));
		transactionNode.appendChild(type);

		Element datePosted = doc.createElement(OfxHelper.TAG_DATE_POSTED);
		datePosted.appendChild(doc.createTextNode(OfxHelper.getOfxFormattedTime(mTimestamp)));
		transactionNode.appendChild(datePosted);
		
		Element dateUser = doc.createElement(OfxHelper.TAG_DATE_USER);
		dateUser.appendChild(doc.createTextNode(
				OfxHelper.getOfxFormattedTime(mTimestamp)));
		transactionNode.appendChild(dateUser);
		
		Element amount = doc.createElement(OfxHelper.TAG_TRANSACTION_AMOUNT);
		amount.appendChild(doc.createTextNode(mAmount.toPlainString()));
		transactionNode.appendChild(amount);
		
		Element transID = doc.createElement(OfxHelper.TAG_TRANSACTION_FITID);
		transID.appendChild(doc.createTextNode(mTransactionUID));
		transactionNode.appendChild(transID);
		
		Element name = doc.createElement(OfxHelper.TAG_NAME);
		name.appendChild(doc.createTextNode(mName));
		transactionNode.appendChild(name);
		
		if (mDescription != null && mDescription.length() > 0){
			Element memo = doc.createElement(OfxHelper.TAG_MEMO);
			memo.appendChild(doc.createTextNode(mDescription));
			transactionNode.appendChild(memo);
		}
		
		if (mDoubleEntryAccountUID != null && mDoubleEntryAccountUID.length() > 0){
			Element bankId = doc.createElement(OfxHelper.TAG_BANK_ID);
			bankId.appendChild(doc.createTextNode(OfxHelper.APP_ID));
			
			//select the proper account as the double account
			String doubleAccountUID = mDoubleEntryAccountUID.equals(accountUID) ? mAccountUID : mDoubleEntryAccountUID;
			
			Element acctId = doc.createElement(OfxHelper.TAG_ACCOUNT_ID);
			acctId.appendChild(doc.createTextNode(doubleAccountUID));
			
			Element accttype = doc.createElement(OfxHelper.TAG_ACCOUNT_TYPE);
			AccountsDbAdapter acctDbAdapter = new AccountsDbAdapter(GnuCashApplication.getAppContext());
			OfxAccountType ofxAccountType = Account.convertToOfxAccountType(acctDbAdapter.getAccountType(doubleAccountUID));
			accttype.appendChild(doc.createTextNode(ofxAccountType.toString()));
			acctDbAdapter.close();
			
			Element bankAccountTo = doc.createElement(OfxHelper.TAG_BANK_ACCOUNT_TO);
			bankAccountTo.appendChild(bankId);
			bankAccountTo.appendChild(acctId);
			bankAccountTo.appendChild(accttype);
			
			transactionNode.appendChild(bankAccountTo);
		}
		
		return transactionNode;
	}

    public String toQIF(){
        final String newLine = "\n";

        AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(GnuCashApplication.getAppContext());

        //all transactions are double transactions
        String splitAccountFullName = QifHelper.getImbalanceAccountName(mAmount.getCurrency());
        if (mDoubleEntryAccountUID != null && mDoubleEntryAccountUID.length() > 0){
            splitAccountFullName = accountsDbAdapter.getFullyQualifiedAccountName(mDoubleEntryAccountUID);
        }

        StringBuilder transactionQifBuffer = new StringBuilder();
        transactionQifBuffer.append(QifHelper.DATE_PREFIX).append(QifHelper.formatDate(mTimestamp)).append(newLine);
        transactionQifBuffer.append(QifHelper.MEMO_PREFIX).append(mName).append(newLine);

        transactionQifBuffer.append(QifHelper.SPLIT_CATEGORY_PREFIX).append(splitAccountFullName).append(newLine);
        if (mDescription != null && mDescription.length() > 0){
            transactionQifBuffer.append(QifHelper.SPLIT_MEMO_PREFIX).append(mDescription).append(newLine);
        }
        transactionQifBuffer.append(QifHelper.SPLIT_AMOUNT_PREFIX).append(mAmount.asString()).append(newLine);
        transactionQifBuffer.append(QifHelper.ENTRY_TERMINATOR).append(newLine);

        accountsDbAdapter.close();
        return transactionQifBuffer.toString();
    }

    /**
     * Creates an Intent with arguments from the <code>transaction</code>.
     * This intent can be broadcast to create a new transaction
     * @param transaction Transaction used to create intent
     * @return Intent with transaction details as extras
     */
    public static Intent createIntent(Transaction transaction){
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType(Transaction.MIME_TYPE);
        intent.putExtra(Intent.EXTRA_TITLE, transaction.getName());
        intent.putExtra(Intent.EXTRA_TEXT, transaction.getDescription());
        intent.putExtra(EXTRA_AMOUNT, transaction.getAmount().asBigDecimal());
        intent.putExtra(EXTRA_ACCOUNT_UID, transaction.getAccountUID());
        intent.putExtra(EXTRA_DOUBLE_ACCOUNT_UID, transaction.getDoubleEntryAccountUID());
        intent.putExtra(Account.EXTRA_CURRENCY_CODE, transaction.getAmount().getCurrency().getCurrencyCode());
        intent.putExtra(EXTRA_TRANSACTION_TYPE, transaction.getTransactionType().name());
        return intent;
    }

}
