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


import android.graphics.Color;
import android.support.annotation.NonNull;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.export.ofx.OfxHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * An account represents a transaction account in with {@link Transaction}s may be recorded
 * Accounts have different types as specified by {@link AccountType} and also a currency with
 * which transactions may be recorded in the account
 * By default, an account is made an {@link AccountType#CASH} and the default currency is
 * the currency of the Locale of the device on which the software is running. US Dollars is used
 * if the platform locale cannot be determined.
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 * @see AccountType
 */
public class Account extends BaseModel {

    /**
     * The MIME type for accounts in GnucashMobile
     * This is used when sending intents from third-party applications
     */
    public static final String MIME_TYPE = "vnd.android.cursor.item/vnd." + BuildConfig.APPLICATION_ID + ".account";

    /**
     * Default color, if not set explicitly through {@link #setColor(String)}.
     */
    // TODO: get it from a theme value?
    public static final int DEFAULT_COLOR = Color.LTGRAY;

    /**
     * Accounts types which are used by the OFX standard
     */
    public enum OfxAccountType {
        CHECKING, SAVINGS, MONEYMRKT, CREDITLINE
    }

    /**
     * Name of this account
     */
    private String mName;

    /**
     * Fully qualified name of this account including the parent hierarchy.
     * On instantiation of an account, the full name is set to the name by default
     */
    private String mFullName;

    /**
     * Account description
     */
    private String mDescription = "";

    /**
     * Commodity used by this account
     */
    private Commodity mCommodity;


    /**
     * Type of account
     * Defaults to {@link AccountType#CASH}
     */
    private AccountType mAccountType = AccountType.CASH;

    /**
     * List of transactions in this account
     */
    private List<Transaction> mTransactionsList = new ArrayList<>();

    /**
     * Account UID of the parent account. Can be null
     */
    private String mParentAccountUID;

    /**
     * Save UID of a default account for transfers.
     * All transactions in this account will by default be transfers to the other account
     */
    private String mDefaultTransferAccountUID;

    /**
     * Flag for placeholder accounts.
     * These accounts cannot have transactions
     */
    private boolean mIsPlaceholderAccount;

    /**
     * Account color field in hex format #rrggbb
     */
    private int mColor = DEFAULT_COLOR;

    /**
     * Flag which marks this account as a favorite account
     */
    private boolean mIsFavorite;

    /**
     * Flag which indicates if this account is a hidden account or not
     */
    private boolean mIsHidden;

    /**
     * An extra key for passing the currency code (according ISO 4217) in an intent
     */
    public static final String EXTRA_CURRENCY_CODE = "org.gnucash.android.extra.currency_code";

    /**
     * Extra key for passing the unique ID of the parent account when creating a
     * new account using Intents
     */
    public static final String EXTRA_PARENT_UID = "org.gnucash.android.extra.parent_uid";

    /**
     * Constructor
     * Creates a new account with the default currency and a generated unique ID
     * @param name Name of the account
     */
    public Account(String name) {
        setName(name);
        this.mFullName = mName;
        setCommodity(Commodity.DEFAULT_COMMODITY);
    }

    /**
     * Overloaded constructor
     * @param name      Name of the account
     * @param commodity {@link Commodity} to be used by transactions in this account
     */
    public Account(String name, @NonNull Commodity commodity) {
        setName(name);
        this.mFullName = mName;
        setCommodity(commodity);
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
     * Returns the full name of this account.
     * The full name is the full account hierarchy name
     * @return Fully qualified name of the account
     */
    public String getFullName() {
        return mFullName;
    }

    /**
     * Sets the fully qualified name of the account
     * @param fullName Fully qualified account name
     */
    public void setFullName(String fullName) {
        this.mFullName = fullName;
    }

    /**
     * Returns the account description
     * @return String with description
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * Sets the account description
     * @param description Account description
     */
    public void setDescription(@NonNull String description) {
        this.mDescription = description;
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
    public void addTransaction(Transaction transaction) {
        transaction.setCommodity(mCommodity);
        mTransactionsList.add(transaction);
    }

    /**
     * Sets a list of transactions for this account.
     * Overrides any previous transactions with those in the list.
     * The account UID and currency of the transactions will be set to the unique ID
     * and currency of the account respectively
     * @param transactionsList List of {@link Transaction}s to be set.
     */
    public void setTransactions(List<Transaction> transactionsList) {
        this.mTransactionsList = transactionsList;
    }

    /**
     * Returns a list of transactions for this account
     * @return Array list of transactions for the account
     */
    public List<Transaction> getTransactions() {
        return mTransactionsList;
    }

    /**
     * Returns the number of transactions in this account
     * @return Number transactions in account
     */
    public int getTransactionCount() {
        return mTransactionsList.size();
    }

    /**
     * Returns the aggregate of all transactions in this account.
     * It takes into account debit and credit amounts, it does not however consider sub-accounts
     * @return {@link Money} aggregate amount of all transactions in account.
     */
    public Money getBalance() {
        Money balance = Money.createZeroInstance(mCommodity.getCurrencyCode());
        for (Transaction transaction : mTransactionsList) {
            balance.add(transaction.getBalance(getUID()));
        }
        return balance;
    }

    /**
     * Returns the color of the account.
     * @return Color of the account as an int as returned by {@link Color}.
     */
    public int getColor() {
        return mColor;
    }

    /**
     * Sets the color of the account.
     * @param color Color as an int as returned by {@link Color}.
     * @throws java.lang.IllegalArgumentException if the color is transparent,
     *   which is not supported.
     */
    public void setColor(int color) {
        if (Color.alpha(color) < 255)
            throw new IllegalArgumentException("Transparent colors are not supported: " + color);
        mColor = color;
    }

    /**
     * Sets the color of the account.
     * @param colorCode Color code to be set in the format #rrggbb
     * @throws java.lang.IllegalArgumentException if the color code is not properly formatted or
	 *   the color is transparent.
     */
    //TODO: Allow use of #aarrggbb format as well
    public void setColor(@NonNull String colorCode) {
        setColor(Color.parseColor(colorCode));
    }

    /**
     * Tests if this account is a favorite account or not
     * @return <code>true</code> if account is flagged as favorite, <code>false</code> otherwise
     */
    public boolean isFavorite() {
        return mIsFavorite;
    }

    /**
     * Toggles the favorite flag on this account on or off
     * @param isFavorite <code>true</code> if account should be flagged as favorite, <code>false</code> otherwise
     */
    public void setFavorite(boolean isFavorite) {
        this.mIsFavorite = isFavorite;
    }

    /**
     * Return the commodity for this account
     */
    @NonNull
    public Commodity getCommodity() {
        return mCommodity;
    }

    /**
     * Sets the commodity of this account
     * @param commodity Commodity of the account
     */
    public void setCommodity(@NonNull Commodity commodity) {
        this.mCommodity = commodity;
        //todo: should we also change commodity of transactions? Transactions can have splits from different accounts
    }

    /**
     * Sets the Unique Account Identifier of the parent account
     * @param parentUID String Unique ID of parent account
     */
    public void setParentUID(String parentUID) {
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
     * Returns <code>true</code> if this account is a placeholder account, <code>false</code> otherwise.
     * @return <code>true</code> if this account is a placeholder account, <code>false</code> otherwise
     */
    public boolean isPlaceholderAccount() {
        return mIsPlaceholderAccount;
    }

    /**
     * Returns the hidden property of this account.
     * <p>Hidden accounts are not visible in the UI</p>
     * @return <code>true</code> if the account is hidden, <code>false</code> otherwise.
     */
    public boolean isHidden() {
        return mIsHidden;
    }

    /**
     * Toggles the hidden property of the account.
     * <p>Hidden accounts are not visible in the UI</p>
     * @param hidden boolean specifying is hidden or not
     */
    public void setHidden(boolean hidden) {
        this.mIsHidden = hidden;
    }

    /**
     * Sets the placeholder flag for this account.
     * Placeholder accounts cannot have transactions
     * @param isPlaceholder Boolean flag indicating if the account is a placeholder account or not
     */
    public void setPlaceHolderFlag(boolean isPlaceholder) {
        mIsPlaceholderAccount = isPlaceholder;
    }

    /**
     * Return the unique ID of accounts to which to default transfer transactions to
     * @return Unique ID string of default transfer account
     */
    public String getDefaultTransferAccountUID() {
        return mDefaultTransferAccountUID;
    }

    /**
     * Set the unique ID of account which is the default transfer target
     * @param defaultTransferAccountUID Unique ID string of default transfer account
     */
    public void setDefaultTransferAccountUID(String defaultTransferAccountUID) {
        this.mDefaultTransferAccountUID = defaultTransferAccountUID;
    }


    /**
     * Maps the <code>accountType</code> to the corresponding account type.
     * <code>accountType</code> have corresponding values to GnuCash desktop
     * @param accountType {@link AccountType} of an account
     * @return Corresponding {@link OfxAccountType} for the <code>accountType</code>
     * @see AccountType
     * @see OfxAccountType
     */
    public static OfxAccountType convertToOfxAccountType(AccountType accountType) {
        switch (accountType) {
            case CREDIT:
            case LIABILITY:
                return OfxAccountType.CREDITLINE;

            case CASH:
            case INCOME:
            case EXPENSE:
            case PAYABLE:
            case RECEIVABLE:
                return OfxAccountType.CHECKING;

            case BANK:
            case ASSET:
                return OfxAccountType.SAVINGS;

            case MUTUAL:
            case STOCK:
            case EQUITY:
            case CURRENCY:
                return OfxAccountType.MONEYMRKT;

            default:
                return OfxAccountType.CHECKING;
        }
    }

    /**
     * Converts this account's transactions into XML and adds them to the DOM document
     * @param doc             XML DOM document for the OFX data
     * @param parent          Parent node to which to add this account's transactions in XML
     * @param exportStartTime Time from which to export transactions which are created/modified after
     */
    public void toOfx(Document doc, Element parent, Timestamp exportStartTime) {
        Element currency = doc.createElement(OfxHelper.TAG_CURRENCY_DEF);
        currency.appendChild(doc.createTextNode(mCommodity.getCurrencyCode()));

        //================= BEGIN BANK ACCOUNT INFO (BANKACCTFROM) =================================

        Element bankId = doc.createElement(OfxHelper.TAG_BANK_ID);
        bankId.appendChild(doc.createTextNode(OfxHelper.APP_ID));

        Element acctId = doc.createElement(OfxHelper.TAG_ACCOUNT_ID);
        acctId.appendChild(doc.createTextNode(getUID()));

        Element accttype = doc.createElement(OfxHelper.TAG_ACCOUNT_TYPE);
        String ofxAccountType = convertToOfxAccountType(mAccountType).toString();
        accttype.appendChild(doc.createTextNode(ofxAccountType));

        Element bankFrom = doc.createElement(OfxHelper.TAG_BANK_ACCOUNT_FROM);
        bankFrom.appendChild(bankId);
        bankFrom.appendChild(acctId);
        bankFrom.appendChild(accttype);

        //================= END BANK ACCOUNT INFO ============================================


        //================= BEGIN ACCOUNT BALANCE INFO =================================
        String balance = getBalance().toPlainString();
        String formattedCurrentTimeString = OfxHelper.getFormattedCurrentTime();

        Element balanceAmount = doc.createElement(OfxHelper.TAG_BALANCE_AMOUNT);
        balanceAmount.appendChild(doc.createTextNode(balance));
        Element dtasof = doc.createElement(OfxHelper.TAG_DATE_AS_OF);
        dtasof.appendChild(doc.createTextNode(formattedCurrentTimeString));

        Element ledgerBalance = doc.createElement(OfxHelper.TAG_LEDGER_BALANCE);
        ledgerBalance.appendChild(balanceAmount);
        ledgerBalance.appendChild(dtasof);

        //================= END ACCOUNT BALANCE INFO =================================


        //================= BEGIN TIME PERIOD INFO =================================

        Element dtstart = doc.createElement(OfxHelper.TAG_DATE_START);
        dtstart.appendChild(doc.createTextNode(formattedCurrentTimeString));

        Element dtend = doc.createElement(OfxHelper.TAG_DATE_END);
        dtend.appendChild(doc.createTextNode(formattedCurrentTimeString));

        //================= END TIME PERIOD INFO =================================


        //================= BEGIN TRANSACTIONS LIST =================================
        Element bankTransactionsList = doc.createElement(OfxHelper.TAG_BANK_TRANSACTION_LIST);
        bankTransactionsList.appendChild(dtstart);
        bankTransactionsList.appendChild(dtend);

        for (Transaction transaction : mTransactionsList) {
            if (transaction.getModifiedTimestamp().before(exportStartTime))
                continue;
            bankTransactionsList.appendChild(transaction.toOFX(doc, getUID()));
        }
        //================= END TRANSACTIONS LIST =================================

        Element statementTransactions = doc.createElement(OfxHelper.TAG_STATEMENT_TRANSACTIONS);
        statementTransactions.appendChild(currency);
        statementTransactions.appendChild(bankFrom);
        statementTransactions.appendChild(bankTransactionsList);
        statementTransactions.appendChild(ledgerBalance);

        parent.appendChild(statementTransactions);

    }
}
