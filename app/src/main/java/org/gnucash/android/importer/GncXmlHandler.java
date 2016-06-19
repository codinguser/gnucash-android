/*
 * Copyright (c) 2013 - 2015 Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2014 - 2015 Yongxin Wang <fefe.wyx@gmail.com>
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

package org.gnucash.android.importer;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.db.adapter.BudgetAmountsDbAdapter;
import org.gnucash.android.db.adapter.BudgetsDbAdapter;
import org.gnucash.android.db.adapter.CommoditiesDbAdapter;
import org.gnucash.android.db.adapter.DatabaseAdapter;
import org.gnucash.android.db.adapter.PricesDbAdapter;
import org.gnucash.android.db.adapter.RecurrenceDbAdapter;
import org.gnucash.android.db.adapter.ScheduledActionDbAdapter;
import org.gnucash.android.db.adapter.SplitsDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.export.xml.GncXmlHelper;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.BaseModel;
import org.gnucash.android.model.Book;
import org.gnucash.android.model.Budget;
import org.gnucash.android.model.BudgetAmount;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.PeriodType;
import org.gnucash.android.model.Price;
import org.gnucash.android.model.Recurrence;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;

/**
 * Handler for parsing the GnuCash XML file.
 * The discovered accounts and transactions are automatically added to the database
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 * @author Yongxin Wang <fefe.wyx@gmail.com>
 */
public class GncXmlHandler extends DefaultHandler {

    /**
     * ISO 4217 currency code for "No Currency"
     */
    private static final String NO_CURRENCY_CODE    = "XXX";

    /**
     * Tag for logging
     */
    private static final String LOG_TAG = "GnuCashAccountImporter";

    /*
        ^             anchor for start of string
        #             the literal #
        (             start of group
        ?:            indicate a non-capturing group that doesn't generate back-references
        [0-9a-fA-F]   hexadecimal digit
        {3}           three times
        )             end of group
        {2}           repeat twice
        $             anchor for end of string
     */
    /**
     * Regular expression for validating color code strings.
     * Accepts #rgb and #rrggbb
     */
    //TODO: Allow use of #aarrggbb format as well
    public static final String ACCOUNT_COLOR_HEX_REGEX = "^#(?:[0-9a-fA-F]{3}){2}$";

    /**
     * Adapter for saving the imported accounts
     */
    AccountsDbAdapter mAccountsDbAdapter;

    /**
     * StringBuilder for accumulating characters between XML tags
     */
    StringBuilder mContent;

    /**
     * Reference to account which is built when each account tag is parsed in the XML file
     */
    Account mAccount;

    /**
     * All the accounts found in a file to be imported, used for bulk import mode
     */
    List<Account> mAccountList;

    /**
     * List of all the template accounts found
     */
    List<Account> mTemplatAccountList;

    /**
     * Map of the tempate accounts to the template transactions UIDs
     */
    Map<String, String> mTemplateAccountToTransactionMap;

    /**
     * Account map for quick referencing from UID
     */
    HashMap<String, Account> mAccountMap;

    /**
     * ROOT account of the imported book
     */
    Account mRootAccount;

    /**
     * Transaction instance which will be built for each transaction found
     */
    Transaction mTransaction;

    /**
     * All the transaction instances found in a file to be inserted, used in bulk mode
     */
    List<Transaction> mTransactionList;

    /**
     * All the template transactions found during parsing of the XML
     */
    List<Transaction> mTemplateTransactions;

    /**
     * Accumulate attributes of splits found in this object
     */
    Split mSplit;

    /**
     * (Absolute) quantity of the split, which uses split account currency
     */
    BigDecimal mQuantity;

    /**
     * (Absolute) value of the split, which uses transaction currency
     */
    BigDecimal mValue;

    /**
     * price table entry
     */
    Price mPrice;

    boolean mPriceCommodity;
    boolean mPriceCurrency;

    List<Price> mPriceList;

    /**
     * Whether the quantity is negative
     */
    boolean mNegativeQuantity;

    /**
     * The list for all added split for autobalancing
     */
    List<Split> mAutoBalanceSplits;

    /**
     * Ignore certain elements in GnuCash XML file, such as "<gnc:template-transactions>"
     */
    String mIgnoreElement = null;

    /**
     * {@link ScheduledAction} instance for each scheduled action parsed
     */
    ScheduledAction mScheduledAction;

    /**
     * List of scheduled actions to be bulk inserted
     */
    List<ScheduledAction> mScheduledActionsList;

    /**
     * List of budgets which have been parsed from XML
     */
    List<Budget> mBudgetList;
    Budget mBudget;
    Recurrence mRecurrence;
    BudgetAmount mBudgetAmount;

    boolean mInColorSlot        = false;
    boolean mInPlaceHolderSlot  = false;
    boolean mInFavoriteSlot     = false;
    boolean mISO4217Currency    = false;
    boolean mIsDatePosted       = false;
    boolean mIsDateEntered      = false;
    boolean mIsNote             = false;
    boolean mInDefaultTransferAccount = false;
    boolean mInExported         = false;
    boolean mInTemplates        = false;
    boolean mInSplitAccountSlot = false;
    boolean mInCreditNumericSlot = false;
    boolean mInDebitNumericSlot = false;
    boolean mIsScheduledStart   = false;
    boolean mIsScheduledEnd     = false;
    boolean mIsLastRun          = false;
    boolean mIsRecurrenceStart  = false;
    boolean mInBudgetSlot       = false;

    /**
     * Saves the attribute of the slot tag
     * Used for determining where we are in the budget amounts
     */
    String mSlotTagAttribute = null;

    String mBudgetAmountAccountUID = null;

    /**
     * Multiplier for the recurrence period type. e.g. period type of week and multiplier of 2 means bi-weekly
     */
    int mRecurrenceMultiplier   = 1;

    /**
     * Flag which says to ignore template transactions until we successfully parse a split amount
     * Is updated for each transaction template split parsed
     */
    boolean mIgnoreTemplateTransaction = true;

    /**
     * Flag which notifies the handler to ignore a scheduled action because some error occurred during parsing
     */
    boolean mIgnoreScheduledAction = false;

    /**
     * Used for parsing old backup files where recurrence was saved inside the transaction.
     * Newer backup files will not require this
     * @deprecated Use the new scheduled action elements instead
     */
    @Deprecated
    private long mRecurrencePeriod = 0;

    private TransactionsDbAdapter mTransactionsDbAdapter;

    private ScheduledActionDbAdapter mScheduledActionsDbAdapter;

    private CommoditiesDbAdapter mCommoditiesDbAdapter;

    private PricesDbAdapter mPricesDbAdapter;

    private Map<String, Integer> mCurrencyCount;

    private BudgetsDbAdapter mBudgetsDbAdapter;
    private Book mBook;
    private SQLiteDatabase mainDb;

    /**
     * Creates a handler for handling XML stream events when parsing the XML backup file
     */
    public GncXmlHandler() {
        init();
    }

    /**
     * Initialize the GnuCash XML handler
     */
    private void init() {
        mBook = new Book();

        DatabaseHelper databaseHelper = new DatabaseHelper(GnuCashApplication.getAppContext(), mBook.getUID());
        mainDb = databaseHelper.getWritableDatabase();
        mTransactionsDbAdapter = new TransactionsDbAdapter(mainDb, new SplitsDbAdapter(mainDb));
        mAccountsDbAdapter = new AccountsDbAdapter(mainDb, mTransactionsDbAdapter);
        RecurrenceDbAdapter recurrenceDbAdapter = new RecurrenceDbAdapter(mainDb);
        mScheduledActionsDbAdapter = new ScheduledActionDbAdapter(mainDb, recurrenceDbAdapter);
        mCommoditiesDbAdapter = new CommoditiesDbAdapter(mainDb);
        mPricesDbAdapter = new PricesDbAdapter(mainDb);
        mBudgetsDbAdapter = new BudgetsDbAdapter(mainDb, new BudgetAmountsDbAdapter(mainDb), recurrenceDbAdapter);


        mContent = new StringBuilder();

        mAccountList = new ArrayList<>();
        mAccountMap = new HashMap<>();
        mTransactionList = new ArrayList<>();
        mScheduledActionsList = new ArrayList<>();
        mBudgetList = new ArrayList<>();

        mTemplatAccountList = new ArrayList<>();
        mTemplateTransactions = new ArrayList<>();
        mTemplateAccountToTransactionMap = new HashMap<>();

        mAutoBalanceSplits = new ArrayList<>();

        mPriceList = new ArrayList<>();
        mCurrencyCount = new HashMap<>();
    }

    @Override
    public void startElement(String uri, String localName,
                             String qualifiedName, Attributes attributes) throws SAXException {
        switch (qualifiedName){
            case GncXmlHelper.TAG_ACCOUNT:
                mAccount = new Account(""); // dummy name, will be replaced when we find name tag
                mISO4217Currency = false;
                break;
            case GncXmlHelper.TAG_TRANSACTION:
                mTransaction = new Transaction(""); // dummy name will be replaced
                mTransaction.setExported(true);     // default to exported when import transactions
                mISO4217Currency = false;
                break;
            case GncXmlHelper.TAG_TRN_SPLIT:
                mSplit = new Split(Money.getZeroInstance(), "");
                break;
            case GncXmlHelper.TAG_DATE_POSTED:
                mIsDatePosted = true;
                break;
            case GncXmlHelper.TAG_DATE_ENTERED:
                mIsDateEntered = true;
                break;
            case GncXmlHelper.TAG_TEMPLATE_TRANSACTIONS:
                mInTemplates = true;
                break;
            case GncXmlHelper.TAG_SCHEDULED_ACTION:
                //default to transaction type, will be changed during parsing
                mScheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
                break;
            case GncXmlHelper.TAG_SX_START:
                mIsScheduledStart = true;
                break;
            case GncXmlHelper.TAG_SX_END:
                mIsScheduledEnd = true;
                break;
            case GncXmlHelper.TAG_SX_LAST:
                mIsLastRun = true;
                break;
            case GncXmlHelper.TAG_RX_START:
                mIsRecurrenceStart = true;
                break;
            case GncXmlHelper.TAG_PRICE:
                mPrice = new Price();
                break;
            case GncXmlHelper.TAG_PRICE_CURRENCY:
                mPriceCurrency = true;
                mPriceCommodity = false;
                mISO4217Currency = false;
                break;
            case GncXmlHelper.TAG_PRICE_COMMODITY:
                mPriceCurrency = false;
                mPriceCommodity = true;
                mISO4217Currency = false;
                break;

            case GncXmlHelper.TAG_BUDGET:
                mBudget = new Budget();
                break;

            case GncXmlHelper.TAG_GNC_RECURRENCE:
            case GncXmlHelper.TAG_BUDGET_RECURRENCE:
                mRecurrenceMultiplier = 1;
                mRecurrence = new Recurrence(PeriodType.MONTH);
                break;
            case GncXmlHelper.TAG_BUDGET_SLOTS:
                mInBudgetSlot = true;
                break;
            case GncXmlHelper.TAG_SLOT:
                if (mInBudgetSlot){
                    mBudgetAmount = new BudgetAmount(mBudget.getUID(), mBudgetAmountAccountUID);
                }
                break;
            case GncXmlHelper.TAG_SLOT_VALUE:
                mSlotTagAttribute = attributes.getValue(GncXmlHelper.ATTR_KEY_TYPE);
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qualifiedName) throws SAXException {
        // FIXME: 22.10.2015 First parse the number of accounts/transactions and use the numer to init the array lists
        String characterString = mContent.toString().trim();

        if (mIgnoreElement != null) {
            // Ignore everything inside
            if (qualifiedName.equals(mIgnoreElement)) {
                mIgnoreElement = null;
            }
            mContent.setLength(0);
            return;
        }

        switch (qualifiedName) {
            case GncXmlHelper.TAG_ACCT_NAME:
                mAccount.setName(characterString);
                mAccount.setFullName(characterString);
                break;
            case GncXmlHelper.TAG_ACCT_ID:
                mAccount.setUID(characterString);
                break;
            case GncXmlHelper.TAG_ACCT_TYPE:
                AccountType accountType = AccountType.valueOf(characterString);
                mAccount.setAccountType(accountType);
                mAccount.setHidden(accountType == AccountType.ROOT); //flag root account as hidden
                break;
            case GncXmlHelper.TAG_COMMODITY_SPACE:
                if (characterString.equals("ISO4217")) {
                    mISO4217Currency = true;
                } else {
                    // price of non-ISO4217 commodities cannot be handled
                    mPrice = null;
                }
                break;
            case GncXmlHelper.TAG_COMMODITY_ID:
                String currencyCode = mISO4217Currency ? characterString : NO_CURRENCY_CODE;
                if (mAccount != null) {
                    Commodity commodity = mCommoditiesDbAdapter.getCommodity(currencyCode);
                    if (commodity != null) {
                        mAccount.setCommodity(commodity);
                    } else {
                        throw new SAXException("Commodity with '" + currencyCode
                                + "' currency code not found in the database");
                    }
                    if (mCurrencyCount.containsKey(currencyCode)) {
                        mCurrencyCount.put(currencyCode, mCurrencyCount.get(currencyCode) + 1);
                    } else {
                        mCurrencyCount.put(currencyCode, 1);
                    }
                }
                if (mTransaction != null) {
                    mTransaction.setCurrencyCode(currencyCode);
                }
                if (mPrice != null) {
                    if (mPriceCommodity) {
                        mPrice.setCommodityUID(mCommoditiesDbAdapter.getCommodityUID(currencyCode));
                        mPriceCommodity = false;
                    }
                    if (mPriceCurrency) {
                        mPrice.setCurrencyUID(mCommoditiesDbAdapter.getCommodityUID(currencyCode));
                        mPriceCurrency = false;
                    }
                }
                break;
            case GncXmlHelper.TAG_ACCT_DESCRIPTION:
                mAccount.setDescription(characterString);
                break;
            case GncXmlHelper.TAG_PARENT_UID:
                mAccount.setParentUID(characterString);
                break;
            case GncXmlHelper.TAG_ACCOUNT:
                if (!mInTemplates) { //we ignore template accounts, we have no use for them. FIXME someday and import the templates too
                    mAccountList.add(mAccount);
                    mAccountMap.put(mAccount.getUID(), mAccount);
                    // check ROOT account
                    if (mAccount.getAccountType() == AccountType.ROOT) {
                        if (mRootAccount == null) {
                            mRootAccount = mAccount;
                        } else {
                            throw new SAXException("Multiple ROOT accounts exist in book");
                        }
                    }
                    // prepare for next input
                    mAccount = null;
                    //reset ISO 4217 flag for next account
                    mISO4217Currency = false;
                }
                break;
            case GncXmlHelper.TAG_SLOT:
                break;
            case GncXmlHelper.TAG_SLOT_KEY:
                switch (characterString) {
                    case GncXmlHelper.KEY_PLACEHOLDER:
                        mInPlaceHolderSlot = true;
                        break;
                    case GncXmlHelper.KEY_COLOR:
                        mInColorSlot = true;
                        break;
                    case GncXmlHelper.KEY_FAVORITE:
                        mInFavoriteSlot = true;
                        break;
                    case GncXmlHelper.KEY_NOTES:
                        mIsNote = true;
                        break;
                    case GncXmlHelper.KEY_DEFAULT_TRANSFER_ACCOUNT:
                        mInDefaultTransferAccount = true;
                        break;
                    case GncXmlHelper.KEY_EXPORTED:
                        mInExported = true;
                        break;
                    case GncXmlHelper.KEY_SPLIT_ACCOUNT_SLOT:
                        mInSplitAccountSlot = true;
                        break;
                    case GncXmlHelper.KEY_CREDIT_NUMERIC:
                        mInCreditNumericSlot = true;
                        break;
                    case GncXmlHelper.KEY_DEBIT_NUMERIC:
                        mInDebitNumericSlot = true;
                        break;
                }
                if (mInBudgetSlot && mBudgetAmountAccountUID == null){
                    mBudgetAmountAccountUID = characterString;
                    mBudgetAmount.setAccountUID(characterString);
                } else if (mInBudgetSlot){
                    mBudgetAmount.setPeriodNum(Long.parseLong(characterString));
                }
                break;
            case GncXmlHelper.TAG_SLOT_VALUE:
                if (mInPlaceHolderSlot) {
                    //Log.v(LOG_TAG, "Setting account placeholder flag");
                    mAccount.setPlaceHolderFlag(Boolean.parseBoolean(characterString));
                    mInPlaceHolderSlot = false;
                } else if (mInColorSlot) {
                    //Log.d(LOG_TAG, "Parsing color code: " + characterString);
                    String color = characterString.trim();
                    //Gnucash exports the account color in format #rrrgggbbb, but we need only #rrggbb.
                    //so we trim the last digit in each block, doesn't affect the color much
                    if (!color.equals("Not Set")) {
                        // avoid known exception, printStackTrace is very time consuming
                        if (!Pattern.matches(ACCOUNT_COLOR_HEX_REGEX, color))
                            color = "#" + color.replaceAll(".(.)?", "$1").replace("null", "");
                        try {
                            if (mAccount != null)
                                mAccount.setColor(color);
                        } catch (IllegalArgumentException ex) {
                            //sometimes the color entry in the account file is "Not set" instead of just blank. So catch!
                            Log.e(LOG_TAG, "Invalid color code '" + color + "' for account " + mAccount.getName());
                            Crashlytics.logException(ex);
                        }
                    }
                    mInColorSlot = false;
                } else if (mInFavoriteSlot) {
                    mAccount.setFavorite(Boolean.parseBoolean(characterString));
                    mInFavoriteSlot = false;
                } else if (mIsNote) {
                    if (mTransaction != null) {
                        mTransaction.setNote(characterString);
                        mIsNote = false;
                    }
                } else if (mInDefaultTransferAccount) {
                    mAccount.setDefaultTransferAccountUID(characterString);
                    mInDefaultTransferAccount = false;
                } else if (mInExported) {
                    if (mTransaction != null) {
                        mTransaction.setExported(Boolean.parseBoolean(characterString));
                        mInExported = false;
                    }
                } else if (mInTemplates && mInSplitAccountSlot) {
                    mSplit.setAccountUID(characterString);
                    mInSplitAccountSlot = false;
                } else if (mInTemplates && mInCreditNumericSlot) {
                    handleEndOfTemplateNumericSlot(characterString, TransactionType.CREDIT);
                } else if (mInTemplates && mInDebitNumericSlot) {
                    handleEndOfTemplateNumericSlot(characterString, TransactionType.DEBIT);
                } else if (mInBudgetSlot){
                    if (mSlotTagAttribute.equals(GncXmlHelper.ATTR_VALUE_NUMERIC)) {
                        try {
                            BigDecimal bigDecimal = GncXmlHelper.parseSplitAmount(characterString);
                            //currency doesn't matter since we don't persist it in the budgets table
                            mBudgetAmount.setAmount(new Money(bigDecimal, Commodity.DEFAULT_COMMODITY));
                        } catch (ParseException e) {
                            mBudgetAmount.setAmount(Money.getZeroInstance()); //just put zero, in case it was a formula we couldnt parse
                            e.printStackTrace();
                        } finally {
                            mBudget.addBudgetAmount(mBudgetAmount);
                        }
                        mSlotTagAttribute = GncXmlHelper.ATTR_VALUE_FRAME;
                    } else {
                        mBudgetAmountAccountUID = null;
                    }
                }
                break;

            case GncXmlHelper.TAG_BUDGET_SLOTS:
                mInBudgetSlot = false;
                break;

            //================  PROCESSING OF TRANSACTION TAGS =====================================
            case GncXmlHelper.TAG_TRX_ID:
                mTransaction.setUID(characterString);
                break;
            case GncXmlHelper.TAG_TRN_DESCRIPTION:
                mTransaction.setDescription(characterString);
                break;
            case GncXmlHelper.TAG_TS_DATE:
                try {
                    if (mIsDatePosted && mTransaction != null) {
                        mTransaction.setTime(GncXmlHelper.parseDate(characterString));
                        mIsDatePosted = false;
                    }
                    if (mIsDateEntered && mTransaction != null) {
                        Timestamp timestamp = new Timestamp(GncXmlHelper.parseDate(characterString));
                        mTransaction.setCreatedTimestamp(timestamp);
                        mIsDateEntered = false;
                    }
                    if (mPrice != null) {
                        mPrice.setDate(new Timestamp(GncXmlHelper.parseDate(characterString)));
                    }
                } catch (ParseException e) {
                    Crashlytics.logException(e);
                    String message = "Unable to parse transaction time - " + characterString;
                    Log.e(LOG_TAG, message + "\n" + e.getMessage());
                    Crashlytics.log(message);
                    throw new SAXException(message, e);
                }
                break;
            case GncXmlHelper.TAG_RECURRENCE_PERIOD: //for parsing of old backup files
                mRecurrencePeriod = Long.parseLong(characterString);
                mTransaction.setTemplate(mRecurrencePeriod > 0);
                break;
            case GncXmlHelper.TAG_SPLIT_ID:
                mSplit.setUID(characterString);
                break;
            case GncXmlHelper.TAG_SPLIT_MEMO:
                mSplit.setMemo(characterString);
                break;
            case GncXmlHelper.TAG_SPLIT_VALUE:
                try {
                    // The value and quantity can have different sign for custom currency(stock).
                    // Use the sign of value for split, as it would not be custom currency
                    String q = characterString;
                    if (q.charAt(0) == '-') {
                        mNegativeQuantity = true;
                        q = q.substring(1);
                    } else {
                        mNegativeQuantity = false;
                    }
                    mValue = GncXmlHelper.parseSplitAmount(characterString).abs(); // use sign from quantity
                } catch (ParseException e) {
                    String msg = "Error parsing split quantity - " + characterString;
                    Crashlytics.log(msg);
                    Crashlytics.logException(e);
                    throw new SAXException(msg, e);
                }
                break;
            case GncXmlHelper.TAG_SPLIT_QUANTITY:
                // delay the assignment of currency when the split account is seen
                try {
                    mQuantity = GncXmlHelper.parseSplitAmount(characterString).abs();
                } catch (ParseException e) {
                    String msg = "Error parsing split quantity - " + characterString;
                    Crashlytics.log(msg);
                    Crashlytics.logException(e);
                    throw new SAXException(msg, e);
                }
                break;
            case GncXmlHelper.TAG_SPLIT_ACCOUNT:
                if (!mInTemplates) {
                    //this is intentional: GnuCash XML formats split amounts, credits are negative, debits are positive.
                    mSplit.setType(mNegativeQuantity ? TransactionType.CREDIT : TransactionType.DEBIT);
                    //the split amount uses the account currency
                    mSplit.setQuantity(new Money(mQuantity, getCommodityForAccount(characterString)));
                    //the split value uses the transaction currency
                    mSplit.setValue(new Money(mValue, mCommoditiesDbAdapter.getCommodity(mTransaction.getCurrency().getCurrencyCode())));
                    mSplit.setAccountUID(characterString);
                } else {
                    if (!mIgnoreTemplateTransaction)
                        mTemplateAccountToTransactionMap.put(characterString, mTransaction.getUID());
                }
                break;
            //todo: import split reconciled state and date
            case GncXmlHelper.TAG_TRN_SPLIT:
                mTransaction.addSplit(mSplit);
                break;
            case GncXmlHelper.TAG_TRANSACTION:
                mTransaction.setTemplate(mInTemplates);
                Split imbSplit = mTransaction.getAutoBalanceSplit();
                if (imbSplit != null) {
                    mAutoBalanceSplits.add(imbSplit);
                }
                if (mInTemplates){
                    if (!mIgnoreTemplateTransaction)
                        mTemplateTransactions.add(mTransaction);
                } else {
                    mTransactionList.add(mTransaction);
                }
                if (mRecurrencePeriod > 0) { //if we find an old format recurrence period, parse it
                    mTransaction.setTemplate(true);
                    ScheduledAction scheduledAction = ScheduledAction.parseScheduledAction(mTransaction, mRecurrencePeriod);
                    mScheduledActionsList.add(scheduledAction);
                }
                mRecurrencePeriod = 0;
                mIgnoreTemplateTransaction = true;
                mTransaction = null;
                break;
            case GncXmlHelper.TAG_TEMPLATE_TRANSACTIONS:
                mInTemplates = false;
                break;

            // ========================= PROCESSING SCHEDULED ACTIONS ==================================
            case GncXmlHelper.TAG_SX_ID:
                mScheduledAction.setUID(characterString);
                break;
            case GncXmlHelper.TAG_SX_NAME:
                if (characterString.equals(ScheduledAction.ActionType.BACKUP.name()))
                    mScheduledAction.setActionType(ScheduledAction.ActionType.BACKUP);
                else
                    mScheduledAction.setActionType(ScheduledAction.ActionType.TRANSACTION);
                break;
            case GncXmlHelper.TAG_SX_ENABLED:
                mScheduledAction.setEnabled(characterString.equals("y"));
                break;
            case GncXmlHelper.TAG_SX_AUTO_CREATE:
                mScheduledAction.setAutoCreate(characterString.equals("y"));
                break;
            //todo: export auto_notify, advance_create, advance_notify
            case GncXmlHelper.TAG_SX_NUM_OCCUR:
                mScheduledAction.setTotalFrequency(Integer.parseInt(characterString));
                break;
            case GncXmlHelper.TAG_RX_MULT:
                mRecurrenceMultiplier = Integer.parseInt(characterString);
                break;
            case GncXmlHelper.TAG_RX_PERIOD_TYPE:
                try {
                    PeriodType periodType = PeriodType.valueOf(characterString.toUpperCase());
                    periodType.setMultiplier(mRecurrenceMultiplier);
                    mRecurrence.setPeriodType(periodType);
                } catch (IllegalArgumentException ex){ //the period type constant is not supported
                    String msg = "Unsupported period constant: " + characterString;
                    Log.e(LOG_TAG, msg);
                    Crashlytics.logException(ex);
                    mIgnoreScheduledAction = true;
                }
                break;
            case GncXmlHelper.TAG_GDATE:
                try {
                    long date = GncXmlHelper.DATE_FORMATTER.parse(characterString).getTime();
                    if (mIsScheduledStart && mScheduledAction != null) {
                        mScheduledAction.setCreatedTimestamp(new Timestamp(date));
                        mIsScheduledStart = false;
                    }

                    if (mIsScheduledEnd && mScheduledAction != null) {
                        mScheduledAction.setEndTime(date);
                        mIsScheduledEnd = false;
                    }

                    if (mIsLastRun && mScheduledAction != null) {
                        mScheduledAction.setLastRun(date);
                        mIsLastRun = false;
                    }

                    if (mIsRecurrenceStart && mScheduledAction != null){
                        mRecurrence.setPeriodStart(new Timestamp(date));
                        mIsRecurrenceStart = false;
                    }
                } catch (ParseException e) {
                    String msg = "Error parsing scheduled action date " + characterString;
                    Log.e(LOG_TAG, msg + e.getMessage());
                    Crashlytics.log(msg);
                    Crashlytics.logException(e);
                    throw new SAXException(msg, e);
                }
                break;
            case GncXmlHelper.TAG_SX_TEMPL_ACCOUNT:
                if (mScheduledAction.getActionType() == ScheduledAction.ActionType.TRANSACTION) {
                    mScheduledAction.setActionUID(mTemplateAccountToTransactionMap.get(characterString));
                } else {
                    mScheduledAction.setActionUID(BaseModel.generateUID());
                }
                break;
            case GncXmlHelper.TAG_GNC_RECURRENCE:
                if (mScheduledAction != null){
                    mScheduledAction.setRecurrence(mRecurrence);
                }
                break;

            case GncXmlHelper.TAG_SCHEDULED_ACTION:
                if (mScheduledAction.getActionUID() != null && !mIgnoreScheduledAction) {
                    mScheduledActionsList.add(mScheduledAction);
                    int count = generateMissedScheduledTransactions(mScheduledAction);
                    Log.i(LOG_TAG, String.format("Generated %d transactions from scheduled action", count));
                }
                mIgnoreScheduledAction = false;
                break;
            // price table
            case GncXmlHelper.TAG_PRICE_ID:
                mPrice.setUID(characterString);
                break;
            case GncXmlHelper.TAG_PRICE_SOURCE:
                if (mPrice != null) {
                    mPrice.setSource(characterString);
                }
                break;
            case GncXmlHelper.TAG_PRICE_VALUE:
                if (mPrice != null) {
                    String[] parts = characterString.split("/");
                    if (parts.length != 2) {
                        String message = "Illegal price - " + characterString;
                        Log.e(LOG_TAG, message);
                        Crashlytics.log(message);
                        throw new SAXException(message);
                    } else {
                        mPrice.setValueNum(Long.valueOf(parts[0]));
                        mPrice.setValueDenom(Long.valueOf(parts[1]));
                        Log.d(getClass().getName(), "price " + characterString +
                        " .. " + mPrice.getValueNum() + "/" + mPrice.getValueDenom());
                    }
                }
                break;
            case GncXmlHelper.TAG_PRICE_TYPE:
                if (mPrice != null) {
                    mPrice.setType(characterString);
                }
                break;
            case GncXmlHelper.TAG_PRICE:
                if (mPrice != null) {
                    mPriceList.add(mPrice);
                    mPrice = null;
                }
                break;

            case GncXmlHelper.TAG_BUDGET:
                if (mBudget.getBudgetAmounts().size() > 0) //ignore if no budget amounts exist for the budget
                    mBudgetList.add(mBudget);
                break;

            case GncXmlHelper.TAG_BUDGET_NAME:
                mBudget.setName(characterString);
                break;

            case GncXmlHelper.TAG_BUDGET_DESCRIPTION:
                mBudget.setDescription(characterString);
                break;

            case GncXmlHelper.TAG_BUDGET_NUM_PERIODS:
                mBudget.setNumberOfPeriods(Long.parseLong(characterString));
                break;

            case GncXmlHelper.TAG_BUDGET_RECURRENCE:
                mBudget.setRecurrence(mRecurrence);
                break;

        }

        //reset the accumulated characters
        mContent.setLength(0);
    }

    @Override
    public void characters(char[] chars, int start, int length) throws SAXException {
        mContent.append(chars, start, length);
    }

    @Override
    public void endDocument() throws SAXException {
        super.endDocument();
        HashMap<String, String> mapFullName = new HashMap<>(mAccountList.size());
        HashMap<String, Account> mapImbalanceAccount = new HashMap<>();

        // The XML has no ROOT, create one
        if (mRootAccount == null) {
            mRootAccount = new Account("ROOT");
            mRootAccount.setAccountType(AccountType.ROOT);
            mAccountList.add(mRootAccount);
            mAccountMap.put(mRootAccount.getUID(), mRootAccount);
        }

        String imbalancePrefix = AccountsDbAdapter.getImbalanceAccountPrefix();

        // Add all account without a parent to ROOT, and collect top level imbalance accounts
        for(Account account:mAccountList) {
            mapFullName.put(account.getUID(), null);
            boolean topLevel = false;
            if (account.getParentUID() == null && account.getAccountType() != AccountType.ROOT) {
                account.setParentUID(mRootAccount.getUID());
                topLevel = true;
            }
            if (topLevel || (mRootAccount.getUID().equals(account.getParentUID()))) {
                if (account.getName().startsWith(imbalancePrefix)) {
                    mapImbalanceAccount.put(account.getName().substring(imbalancePrefix.length()), account);
                }
            }
        }

        // Set the account for created balancing splits to correct imbalance accounts
        for (Split split: mAutoBalanceSplits) {
            // XXX: yes, getAccountUID() returns a currency code in this case (see Transaction.getAutoBalanceSplit())
            String currencyCode = split.getAccountUID();
            Account imbAccount = mapImbalanceAccount.get(currencyCode);
            if (imbAccount == null) {
                imbAccount = new Account(imbalancePrefix + currencyCode, mCommoditiesDbAdapter.getCommodity(currencyCode));
                imbAccount.setParentUID(mRootAccount.getUID());
                imbAccount.setAccountType(AccountType.BANK);
                mapImbalanceAccount.put(currencyCode, imbAccount);
                mAccountList.add(imbAccount);
            }
            split.setAccountUID(imbAccount.getUID());
        }

        java.util.Stack<Account> stack = new Stack<>();
        for (Account account:mAccountList){
            if (mapFullName.get(account.getUID()) != null) {
                continue;
            }
            stack.push(account);
            String parentAccountFullName;
            while (!stack.isEmpty()) {
                Account acc = stack.peek();
                if (acc.getAccountType() == AccountType.ROOT) {
                    // ROOT_ACCOUNT_FULL_NAME should ensure ROOT always sorts first
                    mapFullName.put(acc.getUID(), AccountsDbAdapter.ROOT_ACCOUNT_FULL_NAME);
                    stack.pop();
                    continue;
                }
                String parentUID = acc.getParentUID();
                Account parentAccount = mAccountMap.get(parentUID);
                // ROOT account will be added if not exist, so now anly ROOT
                // has an empty parent
                if (parentAccount.getAccountType() == AccountType.ROOT) {
                    // top level account, full name is the same as its name
                    mapFullName.put(acc.getUID(), acc.getName());
                    stack.pop();
                    continue;
                }
                parentAccountFullName = mapFullName.get(parentUID);
                if (parentAccountFullName == null) {
                    // non-top-level account, parent full name still unknown
                    stack.push(parentAccount);
                    continue;
                }
                mapFullName.put(acc.getUID(), parentAccountFullName +
                        AccountsDbAdapter.ACCOUNT_NAME_SEPARATOR + acc.getName());
                stack.pop();
            }
        }
        for (Account account:mAccountList){
            account.setFullName(mapFullName.get(account.getUID()));
        }

        String mostAppearedCurrency = "";
        int mostCurrencyAppearance = 0;
        for (Map.Entry<String, Integer> entry : mCurrencyCount.entrySet()) {
            if (entry.getValue() > mostCurrencyAppearance) {
                mostCurrencyAppearance = entry.getValue();
                mostAppearedCurrency = entry.getKey();
            }
        }
        if (mostCurrencyAppearance > 0) {
            GnuCashApplication.setDefaultCurrencyCode(mostAppearedCurrency);
        }

        saveToDatabase();
    }

    /**
     * Saves the imported data to the database
     * @return GUID of the newly created book, or null if not successful
     */
    private void saveToDatabase() {
        BooksDbAdapter booksDbAdapter = BooksDbAdapter.getInstance();
        mBook.setRootAccountUID(mRootAccount.getUID());
        mBook.setDisplayName(booksDbAdapter.generateDefaultBookName());
        //we on purpose do not set the book active. Only import. Caller should handle activation
        
        long startTime = System.nanoTime();
        mAccountsDbAdapter.beginTransaction();
        Log.d(getClass().getSimpleName(), "bulk insert starts");
        try {
            // disable foreign key. The database structure should be ensured by the data inserted.
            // it will make insertion much faster.
            mAccountsDbAdapter.enableForeignKey(false);
            Log.d(getClass().getSimpleName(), "before clean up db");
            mAccountsDbAdapter.deleteAllRecords();
            Log.d(getClass().getSimpleName(), String.format("deb clean up done %d ns", System.nanoTime()-startTime));
            long nAccounts = mAccountsDbAdapter.bulkAddRecords(mAccountList, DatabaseAdapter.UpdateMethod.insert);
            Log.d("Handler:", String.format("%d accounts inserted", nAccounts));
            //We need to add scheduled actions first because there is a foreign key constraint on transactions
            //which are generated from scheduled actions (we do auto-create some transactions during import)
            long nSchedActions = mScheduledActionsDbAdapter.bulkAddRecords(mScheduledActionsList, DatabaseAdapter.UpdateMethod.insert);
            Log.d("Handler:", String.format("%d scheduled actions inserted", nSchedActions));

            long nTempTransactions = mTransactionsDbAdapter.bulkAddRecords(mTemplateTransactions, DatabaseAdapter.UpdateMethod.insert);
            Log.d("Handler:", String.format("%d template transactions inserted", nTempTransactions));

            long nTransactions = mTransactionsDbAdapter.bulkAddRecords(mTransactionList, DatabaseAdapter.UpdateMethod.insert);
            Log.d("Handler:", String.format("%d transactions inserted", nTransactions));

            long nPrices = mPricesDbAdapter.bulkAddRecords(mPriceList, DatabaseAdapter.UpdateMethod.insert);
            Log.d(getClass().getSimpleName(), String.format("%d prices inserted", nPrices));

            //// TODO: 01.06.2016 Re-enable import of Budget stuff when the UI is complete
//            long nBudgets = mBudgetsDbAdapter.bulkAddRecords(mBudgetList, DatabaseAdapter.UpdateMethod.insert);
//            Log.d(getClass().getSimpleName(), String.format("%d budgets inserted", nBudgets));

            long endTime = System.nanoTime();
            Log.d(getClass().getSimpleName(), String.format("bulk insert time: %d", endTime - startTime));

            //if all of the import went smoothly, then add the book to the book db
            booksDbAdapter.addRecord(mBook, DatabaseAdapter.UpdateMethod.insert);
            mAccountsDbAdapter.setTransactionSuccessful();
        } finally {
            mAccountsDbAdapter.enableForeignKey(true);
            mAccountsDbAdapter.endTransaction();
            mainDb.close(); //close it after import
        }
    }

    /**
     * Returns the unique identifier of the just-imported book
     * @return GUID of the newly imported book
     */
    public @NonNull String getBookUID(){
        return mBook.getUID();
    }

    /**
     * Returns the currency for an account which has been parsed (but not yet saved to the db)
     * <p>This is used when parsing splits to assign the right currencies to the splits</p>
     * @param accountUID GUID of the account
     * @return Commodity of the account
     */
    private Commodity getCommodityForAccount(String accountUID){
        try {
            return mAccountMap.get(accountUID).getCommodity();
        } catch (Exception e) {
            Crashlytics.logException(e);
            return Commodity.DEFAULT_COMMODITY;
        }
    }


    /**
     * Handles the case when we reach the end of the template numeric slot
     * @param characterString Parsed characters containing split amount
     */
    private void handleEndOfTemplateNumericSlot(String characterString, TransactionType splitType) {
        try {
            BigDecimal amountBigD = GncXmlHelper.parseSplitAmount(characterString);
            Money amount = new Money(amountBigD, getCommodityForAccount(mSplit.getAccountUID()));
            mSplit.setValue(amount.abs());
            mSplit.setType(splitType);
            mIgnoreTemplateTransaction = false; //we have successfully parsed an amount
        } catch (NumberFormatException | ParseException e) {
            String msg = "Error parsing template credit split amount " + characterString;
            Log.e(LOG_TAG, msg + "\n" + e.getMessage());
            Crashlytics.log(msg);
            Crashlytics.logException(e);
        } finally {
            if (splitType == TransactionType.CREDIT)
                mInCreditNumericSlot = false;
            else
                mInDebitNumericSlot = false;
        }
    }

    /**
     * Generates the runs of the scheduled action which have been missed since the file was last opened.
     * @param scheduledAction Scheduled action for transaction
     * @return Number of transaction instances generated
     */
    private int generateMissedScheduledTransactions(ScheduledAction scheduledAction){
        //if this scheduled action should not be run for any reason, return immediately
        if (scheduledAction.getActionType() != ScheduledAction.ActionType.TRANSACTION
                || !scheduledAction.isEnabled() || !scheduledAction.shouldAutoCreate()
                || (scheduledAction.getEndTime() > 0 && scheduledAction.getEndTime() > System.currentTimeMillis())
                || (scheduledAction.getTotalFrequency() > 0 && scheduledAction.getExecutionCount() >= scheduledAction.getTotalFrequency())){
            return 0;
        }

        long lastRuntime = scheduledAction.getStartTime();
        if (scheduledAction.getLastRunTime() > 0){
            lastRuntime = scheduledAction.getLastRunTime();
        }

        int generatedTransactionCount = 0;
        long period = scheduledAction.getPeriod();
        final String actionUID = scheduledAction.getActionUID();
        while ((lastRuntime = lastRuntime + period) <= System.currentTimeMillis()){
            for (Transaction templateTransaction : mTemplateTransactions) {
                if (templateTransaction.getUID().equals(actionUID)){
                    Transaction transaction = new Transaction(templateTransaction, true);
                    transaction.setTime(lastRuntime);
                    transaction.setScheduledActionUID(scheduledAction.getUID());
                    mTransactionList.add(transaction);
                    scheduledAction.setExecutionCount(scheduledAction.getExecutionCount() + 1);
                    ++generatedTransactionCount;
                    break;
                }
            }
        }
        scheduledAction.setLastRun(lastRuntime);
        return generatedTransactionCount;
    }
}
