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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.util.Currency;

/**
 * Defines what a transaction must implement.
 * @author Jesse Shieh <jesse.shieh.pub@gmail.com>
 */
public interface Transaction {
    /**
     * Mime type for transactions in Gnucash.
     * Used for recording transactions through intents
     */
    public static final String MIME_TYPE = "vnd.android.cursor.item/vnd.org.gnucash.android.transaction";

    /**
     * Key for passing the account unique Identifier as an argument through an {@link android.content.Intent}
     */
    public static final String EXTRA_ACCOUNT_UID = "org.gnucash.android.extra.account_uid";

    /**
     * Key for specifying the double entry account
     */
    public static final String EXTRA_DOUBLE_ACCOUNT_UID = "org.gnucash.android.extra.double_account_uid";

    /**
     * Key for identifying the amount of the transaction through an Intent
     */
    public static final String EXTRA_AMOUNT = "org.gnucash.android.extra.amount";

    /**
     * Extra key for the transaction type.
     * This value should typically be set by calling {@link TransactionType#name()}
     */
    public static final String EXTRA_TRANSACTION_TYPE = "org.gnucash.android.extra.transaction_type";

    /**
     * Set the amount of this transaction
     * @param amount Amount of the transaction
     */
    void setAmount(Money negate);

    /**
     * Sets the currency of the transaction
     * The currency remains in the object model and is not persisted to the database
     * Transactions always use the currency of their accounts
     * @param currency {@link Currency} of the transaction value
     */
    void setCurrency(Currency mCurrency);

    /**
     * Returns the amount involved in this transaction
     * @return {@link Money} amount in the transaction
     */
    Money getAmount();

    /**
     * Returns the name of the transaction
     * @return Name of the transaction
     */
    String getName();

    /**
     * Sets the name of the transaction
     * @param name String containing name of transaction to set
     */
    void setName(String name);

    /**
     * Set short description of the transaction
     * @param description String containing description of transaction
     */
    void setDescription(String string);

    /**
     * Returns the description of the transaction
     * @return String containing description of transaction
     */
    String getDescription();

    /**
     * Sets the time when the transaction occurred
     * @param timeInMillis Time in milliseconds
     */
    void setTime(long timeInMillis);

    /**
     * Returns the time of transaction in milliseconds
     * @return Time when transaction occurred in milliseconds
     */
    long getTimeMillis();

    /**
     * Sets the type of transaction
     * @param type The transaction type
     * @see TransactionType
     */
    void setTransactionType(TransactionType type);

    /**
     * Returns the type of transaction
     * @return Type of transaction
     */
    TransactionType getTransactionType();

    /**
     * Set Unique Identifier for this transaction
     * @param transactionUID Unique ID string
     */
    void setUID(String transactionUID);

    /**
     * Returns unique ID string for transaction
     * @return String with Unique ID of transaction
     */
    String getUID();

    /**
     * Returns the Unique Identifier of account with which this transaction is double entered
     * @return Unique ID of transfer account or <code>null</code> if it is not a double transaction
     */
    String getDoubleEntryAccountUID();

    /**
     * Sets the account UID with which to double enter this transaction
     * @param doubleEntryAccountUID Unique Identifier to set
     */
    void setDoubleEntryAccountUID(String doubleEntryAccountUID);

    /**
     * Returns UID of account to which this transaction belongs
     * @return the UID of the account to which this transaction belongs
     */
    String getAccountUID();

    /**
     * Sets the exported flag on the transaction
     * @param isExported <code>true</code> if the transaction has been exported, <code>false</code> otherwise
     */
    void setExported(boolean isExported);

    /**
     * Returns <code>true</code> if the transaction has been exported, <code>false</code> otherwise
     * @return <code>true</code> if the transaction has been exported, <code>false</code> otherwise
     */
    boolean isExported();

    /**
     * Set the account UID of the account to which this transaction belongs
     * @param accountUID the UID of the account which owns this transaction
     */
    void setAccountUID(String accountUID);

    /**
     * Returns the recurrence period for this transaction
     * @return Recurrence period for this transaction in milliseconds
     */
    long getRecurrencePeriod();

    /**
     * Sets the recurrence period for this transaction
     * @param recurrenceId Recurrence period in milliseconds
     */
    void setRecurrencePeriod(long recurrenceId);

    /**
     * Converts transaction to XML DOM corresponding to OFX Statement transaction and
     * returns the element node for the transaction.
     * The Unique ID of the account is needed in order to properly export double entry transactions
     * @param doc XML document to which transaction should be added
     * @param accountUID Unique Identifier of the account which called the method.
     * @return Element in DOM corresponding to transaction
     */
    Element toOfx(Document doc, String accountUID);

    /**
     * Builds a QIF entry representing this transaction
     * @return String QIF representation of this transaction
     */
    String toQIF();
}
