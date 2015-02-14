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

import android.content.Intent;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.export.ofx.OfxHelper;
import org.gnucash.android.export.xml.GncXmlHelper;
import org.gnucash.android.model.Account.OfxAccountType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.*;

/**
 * Represents a financial transaction, either credit or debit.
 * Transactions belong to accounts and each have the unique identifier of the account to which they belong.
 * The default type is a debit, unless otherwise specified.
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class Transaction {

	/**
	 * Mime type for transactions in Gnucash.
	 * Used for recording transactions through intents
	 */
	public static final String MIME_TYPE 			= "vnd.android.cursor.item/vnd.org.gnucash.android.transaction";

	/**
	 * Key for passing the account unique Identifier as an argument through an {@link Intent}
	 */
    @Deprecated
	public static final String EXTRA_ACCOUNT_UID 	= "org.gnucash.android.extra.account_uid";

	/**
	 * Key for specifying the double entry account
	 */
    @Deprecated
	public static final String EXTRA_DOUBLE_ACCOUNT_UID = "org.gnucash.android.extra.double_account_uid";

	/**
	 * Key for identifying the amount of the transaction through an Intent
	 */
    @Deprecated
	public static final String EXTRA_AMOUNT 		= "org.gnucash.android.extra.amount";

    /**
     * Extra key for the transaction type.
     * This value should typically be set by calling {@link TransactionType#name()}
     */
    @Deprecated
    public static final String EXTRA_TRANSACTION_TYPE = "org.gnucash.android.extra.transaction_type";

    /**
     * Argument key for passing splits as comma-separated multi-line list and each line is a split.
     * The line format is: <type>;<amount>;<account_uid>
     * The amount should be formatted in the US Locale
     */
    public static final String EXTRA_SPLITS = "org.gnucash.android.extra.transaction.splits";

    /**
     * Currency used by splits in this transaction
     */
    private String mCurrencyCode = Money.DEFAULT_CURRENCY_CODE;

    /**
     * The splits making up this transaction
     */
    private List<Split> mSplitList = new ArrayList<Split>();

	/**
	 * Unique identifier of the transaction.
	 * This is automatically generated when the transaction is created.
	 */
	private String mUID;

	/**
	 * Name describing the transaction
	 */
	private String mDescription;

	/**
	 * An extra note giving details about the transaction
	 */
	private String mNotes = "";

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
     * Recurrence period of this transaction.
     * <p>If this value is set then it means this transaction is a template which will be used to
     * create a transaction every turn of the recurrence period</p>
     */
    private long mRecurrencePeriod = 0;

	/**
	 * Overloaded constructor. Creates a new transaction instance with the
	 * provided data and initializes the rest to default values.
	 * @param name Name of the transaction
	 */
	public Transaction(String name) {
		initDefaults();
		setDescription(name);
	}

    /**
     * Copy constructor.
     * Creates a new transaction object which is a clone of the parameter.
     * <p><b>Note:</b> The unique ID of the transaction is not cloned if the parameter <code>generateNewUID</code>,
     * is set to false. Otherwise, a new one is generated.</p>
     * @param transaction Transaction to be cloned
     * @param generateNewUID Flag to determine if new UID should be assigned or not
     */
    public Transaction(Transaction transaction, boolean generateNewUID){
        initDefaults();
        setDescription(transaction.getDescription());
        setNote(transaction.getNote());
        setTime(transaction.getTimeMillis());
        mCurrencyCode = transaction.mCurrencyCode;
        mRecurrencePeriod = transaction.mRecurrencePeriod;
        //exported flag is left at default value of false

        for (Split split : transaction.mSplitList) {
            addSplit(new Split(split, true));
        }

        if (!generateNewUID){
            setUID(transaction.getUID());
        }
    }

	/**
	 * Initializes the different fields to their default values.
	 */
	private void initDefaults(){
		this.mTimestamp = System.currentTimeMillis();
		mUID = UUID.randomUUID().toString().replaceAll("-", "");
	}

    /**
     * Returns list of splits for this transaction
     * @return {@link java.util.List} of splits in the transaction
     */
    public List<Split> getSplits(){
        return mSplitList;
    }

    /**
     * Returns the list of splits belonging to a specific account
     * @param accountUID Unique Identifier of the account
     * @return List of {@link org.gnucash.android.model.Split}s
     */
    public List<Split> getSplits(String accountUID){
        List<Split> splits = new ArrayList<Split>();
        for (Split split : mSplitList) {
            if (split.getAccountUID().equals(accountUID)){
                splits.add(split);
            }
        }
        return splits;
    }

    /**
     * Sets the splits for this transaction
     * <p>All the splits in the list will have their transaction UID set to this transaction</p>
     * @param splitList List of splits for this transaction
     */
    public void setSplits(List<Split> splitList){
        mSplitList = splitList;
        for (Split split : splitList) {
            split.setTransactionUID(mUID);
        }
    }

    /**
     * Add a split to the transaction.
     * <p>Sets the split UID and currency to that of this transaction</p>
     * @param split Split for this transaction
     */
    public void addSplit(Split split){
        //sets the currency of the split to the currency of the transaction
        split.setAmount(split.getAmount().withCurrency(Currency.getInstance(mCurrencyCode)));
        split.setTransactionUID(mUID);
        mSplitList.add(split);
    }

    /**
     * Returns the balance of this transaction for only those splits which relate to the account.
     * <p>Uses a call to {@link #getBalance(String)} with the appropriate parameters</p>
     * @param accountUID Unique Identifier of the account
     * @return Money balance of the transaction for the specified account
     * @see #computeBalance(String, java.util.List)
     */
    public Money getBalance(String accountUID){
        return computeBalance(accountUID, mSplitList);
    }

    /**
     * Computes the imbalance amount for the given transaction.
     * In double entry, all transactions should resolve to zero. However a user may not enter all such values which
     * means there is an extra amount which is unresolved.
     * @return Money imbalance of the transaction
     */
    public Money getImbalance(){
        Money imbalance = Money.createZeroInstance(mCurrencyCode);
        for (Split split : mSplitList) {
            Money amount = split.getAmount().absolute();
            if (split.getType() == TransactionType.DEBIT)
                imbalance = imbalance.subtract(amount);
            else
                imbalance = imbalance.add(amount);
        }
        return imbalance;
    }

    /**
     * Computes the balance of the splits belonging to a particular account.
     * Only those splits which belong to the account will be considered.
     * @param accountUID Unique Identifier of the account
     * @param splitList List of splits
     * @return Money list of splits
     */
    public static Money computeBalance(String accountUID, List<Split> splitList){
        AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(GnuCashApplication.getAppContext());
        AccountType accountType = accountsDbAdapter.getAccountType(accountUID);
        String currencyCode = accountsDbAdapter.getCurrencyCode(accountUID);
        accountsDbAdapter.close();

        boolean isDebitAccount = accountType.hasDebitNormalBalance();
        Money balance = Money.createZeroInstance(currencyCode);
        for (Split split : splitList) {
            if (!split.getAccountUID().equals(accountUID))
                continue;
            Money absAmount = split.getAmount().absolute().withCurrency(Currency.getInstance(currencyCode));
            boolean isDebitSplit = split.getType() == TransactionType.DEBIT;
            if (isDebitAccount) {
                if (isDebitSplit) {
                    balance = balance.add(absAmount);
                } else {
                    balance = balance.subtract(absAmount);
                }
            } else {
                if (isDebitSplit) {
                    balance = balance.subtract(absAmount);
                } else {
                    balance = balance.add(absAmount);
                }
            }
        }
        return balance;
    }

    /**
     * Returns the currency code of this transaction.
     * @return ISO 4217 currency code string
     */
    public String getCurrencyCode() {
        return mCurrencyCode;
    }

    /**
     * Sets the ISO 4217 currency code used by this transaction
     * <p>The currency remains in the object model and is not persisted to the database
     * Transactions always use the currency of their accounts. </p>
     * @param currencyCode String with ISO 4217 currency code
     */
    public void setCurrencyCode(String currencyCode) {
        this.mCurrencyCode = currencyCode;
    }

    /**
     * Returns the {@link java.util.Currency} used by this transaction
     * @return Currency of the transaction
     * @see #getCurrencyCode()
     */
    public Currency getCurrency(){
        return Currency.getInstance(this.mCurrencyCode);
    }

	/**
	 * Returns the description of the transaction
	 * @return Transaction description
	 */
	public String getDescription() {
		return mDescription;
	}

	/**
	 * Sets the transaction description
	 * @param description String description
	 */
	public void setDescription(String description) {
		this.mDescription = description.trim();
	}

	/**
	 * Add notes to the transaction
	 * @param notes String containing notes for the transaction
	 */
	public void setNote(String notes) {
		this.mNotes = notes;
	}

	/**
	 * Returns the transaction notes
	 * @return String notes of transaction
	 */
	public String getNote() {
		return mNotes;
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
	 * Set Unique Identifier for this transaction.
     * <p>Remember that the unique ID is auto-generated when transaction is created.
     * So this method is only for cases like building an object instance of a persisted transaction.</p>
	 * @param transactionUID Unique ID string
     * @see #resetUID()
	 */
	public void setUID(String transactionUID) {
		this.mUID = transactionUID;
	}

    /**
     * Resets the UID of this transaction to a newly generated one
     */
    public void resetUID(){
        this.mUID = UUID.randomUUID().toString();
    }
	/**
	 * Returns unique ID string for transaction
	 * @return String with Unique ID of transaction
	 */
	public String getUID() {
		return mUID;
	}

    /**
     * Returns the corresponding {@link TransactionType} given the accounttype and the effect which the transaction
     * type should have on the account balance
     * @param accountType Type of account
     * @param shouldReduceBalance <code>true</code> if type should reduce balance, <code>false</code> otherwise
     * @return TransactionType for the account
     */
    public static TransactionType getTypeForBalance(AccountType accountType, boolean shouldReduceBalance){
        TransactionType type;
        if (accountType.hasDebitNormalBalance()) {
            type = shouldReduceBalance ? TransactionType.CREDIT : TransactionType.DEBIT;
        } else {
            type = shouldReduceBalance ? TransactionType.DEBIT : TransactionType.CREDIT;
        }
        return type;
    }

    /**
     * Returns true if the transaction type represents a decrease for the account balance for the <code>accountType</code>, false otherwise
     * @return true if the amount represents a decrease in the account balance, false otherwise
     * @see #getTypeForBalance(AccountType, boolean)
     */
    public static boolean shouldDecreaseBalance(AccountType accountType, TransactionType transactionType){
        if (accountType.hasDebitNormalBalance()){
            return transactionType == TransactionType.CREDIT;
        } else
            return transactionType == TransactionType.DEBIT;
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
     * Returns the recurrence period for this transaction
     * @return Recurrence period for this transaction in milliseconds
     */
    public long getRecurrencePeriod() {
        return mRecurrencePeriod;
    }

    /**
     * Sets the recurrence period for this transaction
     * @param recurrenceId Recurrence period in milliseconds
     */
    public void setRecurrencePeriod(long recurrenceId) {
        this.mRecurrencePeriod = recurrenceId;
    }

    /**
	 * Converts transaction to XML DOM corresponding to OFX Statement transaction and
	 * returns the element node for the transaction.
	 * The Unique ID of the account is needed in order to properly export double entry transactions
     * @param doc XML document to which transaction should be added
     * @param accountUID Unique Identifier of the account which called the method.  @return Element in DOM corresponding to transaction
     */
	public Element toOFX(Document doc, String accountUID){
        Money balance = getBalance(accountUID);
        TransactionType transactionType = balance.isNegative() ? TransactionType.DEBIT : TransactionType.CREDIT;

        Element transactionNode = doc.createElement(OfxHelper.TAG_STATEMENT_TRANSACTION);
        Element typeNode = doc.createElement(OfxHelper.TAG_TRANSACTION_TYPE);
        typeNode.appendChild(doc.createTextNode(transactionType.toString()));
        transactionNode.appendChild(typeNode);

        Element datePosted = doc.createElement(OfxHelper.TAG_DATE_POSTED);
        datePosted.appendChild(doc.createTextNode(OfxHelper.getOfxFormattedTime(mTimestamp)));
        transactionNode.appendChild(datePosted);

        Element dateUser = doc.createElement(OfxHelper.TAG_DATE_USER);
        dateUser.appendChild(doc.createTextNode(
                OfxHelper.getOfxFormattedTime(mTimestamp)));
        transactionNode.appendChild(dateUser);

        Element amount = doc.createElement(OfxHelper.TAG_TRANSACTION_AMOUNT);
        amount.appendChild(doc.createTextNode(balance.toPlainString()));
        transactionNode.appendChild(amount);

        Element transID = doc.createElement(OfxHelper.TAG_TRANSACTION_FITID);
        transID.appendChild(doc.createTextNode(mUID));
        transactionNode.appendChild(transID);

        Element name = doc.createElement(OfxHelper.TAG_NAME);
        name.appendChild(doc.createTextNode(mDescription));
        transactionNode.appendChild(name);

        if (mNotes != null && mNotes.length() > 0){
            Element memo = doc.createElement(OfxHelper.TAG_MEMO);
            memo.appendChild(doc.createTextNode(mNotes));
            transactionNode.appendChild(memo);
        }

        if (mSplitList.size() == 2){ //if we have exactly one other split, then treat it like a transfer
            String transferAccountUID = accountUID;
            for (Split split : mSplitList) {
                if (!split.getAccountUID().equals(accountUID)){
                    transferAccountUID = split.getAccountUID();
                    break;
                }
            }
            Element bankId = doc.createElement(OfxHelper.TAG_BANK_ID);
            bankId.appendChild(doc.createTextNode(OfxHelper.APP_ID));

            Element acctId = doc.createElement(OfxHelper.TAG_ACCOUNT_ID);
            acctId.appendChild(doc.createTextNode(transferAccountUID));

            Element accttype = doc.createElement(OfxHelper.TAG_ACCOUNT_TYPE);
            AccountsDbAdapter acctDbAdapter = new AccountsDbAdapter(GnuCashApplication.getAppContext());
            OfxAccountType ofxAccountType = Account.convertToOfxAccountType(acctDbAdapter.getAccountType(transferAccountUID));
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

    /**
     * Generate the GncXML for the transaction and append to the DOM document
     * @param doc XML document to which transaction should be added
     * @param rootElement Parent node for the XML
     * @deprecated Use the {@link org.gnucash.android.export.xml.GncXmlExporter} to generate XML
     */
    public void toGncXml(Document doc, Element rootElement) {
        Element idNode = doc.createElement(GncXmlHelper.TAG_TRX_ID);
        idNode.setAttribute(GncXmlHelper.ATTR_KEY_TYPE, GncXmlHelper.ATTR_VALUE_GUID);
        idNode.appendChild(doc.createTextNode(mUID));

        Element currencyNode = doc.createElement(GncXmlHelper.TAG_TRX_CURRENCY);
        Element cmdtySpacenode = doc.createElement(GncXmlHelper.TAG_COMMODITY_SPACE);
        cmdtySpacenode.appendChild(doc.createTextNode("ISO4217"));
        currencyNode.appendChild(cmdtySpacenode);
        Element cmdtyIdNode = doc.createElement(GncXmlHelper.TAG_COMMODITY_ID);
        cmdtyIdNode.appendChild(doc.createTextNode(mCurrencyCode));
        currencyNode.appendChild(cmdtyIdNode);

        Element datePostedNode = doc.createElement(GncXmlHelper.TAG_DATE_POSTED);
        Element datePNode = doc.createElement(GncXmlHelper.TAG_DATE);
        datePNode.appendChild(doc.createTextNode(GncXmlHelper.formatDate(mTimestamp)));
        datePostedNode.appendChild(datePNode);

        Element dateEneteredNode = doc.createElement(GncXmlHelper.TAG_DATE_ENTERED);
        Element dateENode = doc.createElement(GncXmlHelper.TAG_DATE);
        dateENode.appendChild(doc.createTextNode(GncXmlHelper.formatDate(mTimestamp)));
        dateEneteredNode.appendChild(dateENode);

        Element descriptionNode = doc.createElement(GncXmlHelper.TAG_TRN_DESCRIPTION);
        if (mDescription != null) {
            descriptionNode.appendChild(doc.createTextNode(mDescription));
        }

        Element trnSlotsNode = doc.createElement(GncXmlHelper.TAG_TRN_SLOTS);
        if (mNotes != null && mNotes.length() > 0) {
            trnSlotsNode.appendChild(GncXmlHelper.createSlot(doc, GncXmlHelper.KEY_NOTES, mNotes, GncXmlHelper.ATTR_VALUE_STRING));
            //TODO: Consider adding future transactions date as slot here too
        }
        Element trnSplits = doc.createElement(GncXmlHelper.TAG_TRN_SPLITS);
        for (Split split : mSplitList) {
            split.toGncXml(doc, trnSplits);
        }

        Element transactionNode = doc.createElement(GncXmlHelper.TAG_TRANSACTION);
        transactionNode.setAttribute(GncXmlHelper.ATTR_KEY_VERSION, GncXmlHelper.BOOK_VERSION);
        transactionNode.appendChild(idNode);
        transactionNode.appendChild(currencyNode);
        transactionNode.appendChild(datePostedNode);
        transactionNode.appendChild(dateEneteredNode);
        transactionNode.appendChild(descriptionNode);
        if (mNotes != null && mNotes.length() > 0){
            transactionNode.appendChild(trnSlotsNode);
        }
        //TODO: Improve xml compatibilty with desktop for scheduled actions
        if (mRecurrencePeriod != 0) {
            Element recurrenceNode = doc.createElement(GncXmlHelper.TAG_RECURRENCE_PERIOD);
            recurrenceNode.appendChild(doc.createTextNode(String.valueOf(mRecurrencePeriod)));
            transactionNode.appendChild(recurrenceNode);
        }
        transactionNode.appendChild(trnSplits);

        rootElement.appendChild(transactionNode);
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
        intent.putExtra(Intent.EXTRA_TITLE, transaction.getDescription());
        intent.putExtra(Intent.EXTRA_TEXT, transaction.getNote());
        intent.putExtra(Account.EXTRA_CURRENCY_CODE, transaction.getCurrencyCode());
        StringBuilder stringBuilder = new StringBuilder();
        for (Split split : transaction.getSplits()) {
            stringBuilder.append(split.toCsv()).append("\n");
        }
        intent.putExtra(Transaction.EXTRA_SPLITS, stringBuilder.toString());
        return intent;
    }
}
