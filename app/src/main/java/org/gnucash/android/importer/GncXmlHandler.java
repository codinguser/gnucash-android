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
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.ScheduledActionDbAdapter;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.export.xml.GncXmlHelper;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.PeriodType;
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
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;
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

    /**
     * Creates a handler for handling XML stream events when parsing the XML backup file
     */
    public GncXmlHandler() {
        init(null);
    }

    /**
     * Overloaded constructor.
     * Useful when reading XML into an already open database connection e.g. during migration
     * @param db SQLite database object
     */
    public GncXmlHandler(SQLiteDatabase db) {
        init(db);
    }

    private void init(@Nullable SQLiteDatabase db) {
        if (db == null) {
            mAccountsDbAdapter = AccountsDbAdapter.getInstance();
            mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();
            mScheduledActionsDbAdapter = ScheduledActionDbAdapter.getInstance();
        } else {
            mTransactionsDbAdapter = new TransactionsDbAdapter(db, new SplitsDbAdapter(db));
            mAccountsDbAdapter = new AccountsDbAdapter(db, mTransactionsDbAdapter);
            mScheduledActionsDbAdapter = new ScheduledActionDbAdapter(db);
        }

        mContent = new StringBuilder();

        mAccountList = new ArrayList<>();
        mAccountMap = new HashMap<>();
        mTransactionList = new ArrayList<>();
        mScheduledActionsList = new ArrayList<>();

        mTemplatAccountList = new ArrayList<>();
        mTemplateTransactions = new ArrayList<>();
        mTemplateAccountToTransactionMap = new HashMap<>();

        mAutoBalanceSplits = new ArrayList<>();
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
        }
    }

    @Override
    public void endElement(String uri, String localName, String qualifiedName) throws SAXException {
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
            case GncXmlHelper.TAG_NAME:
                mAccount.setName(characterString);
                mAccount.setFullName(characterString);
                break;
            case GncXmlHelper.TAG_ACCT_ID:
                mAccount.setUID(characterString);
                break;
            case GncXmlHelper.TAG_TYPE:
                AccountType accountType = AccountType.valueOf(characterString);
                mAccount.setAccountType(accountType);
                mAccount.setHidden(accountType == AccountType.ROOT); //flag root account as hidden
                break;
            case GncXmlHelper.TAG_COMMODITY_SPACE:
                if (characterString.equals("ISO4217")) {
                    mISO4217Currency = true;
                }
                break;
            case GncXmlHelper.TAG_COMMODITY_ID:
                String currencyCode = mISO4217Currency ? characterString : NO_CURRENCY_CODE;
                if (mAccount != null) {
                    mAccount.setCurrency(Currency.getInstance(currencyCode));
                }
                if (mTransaction != null) {
                    mTransaction.setCurrencyCode(currencyCode);
                }
                break;
            case GncXmlHelper.TAG_PARENT_UID:
                mAccount.setParentUID(characterString);
                break;
            case GncXmlHelper.TAG_ACCOUNT:
                if (!mInTemplates) { //we ignore template accounts, we have no use for them
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
                break;
            case GncXmlHelper.TAG_SLOT_VALUE:
                if (mInPlaceHolderSlot) {
                    Log.v(LOG_TAG, "Setting account placeholder flag");
                    mAccount.setPlaceHolderFlag(Boolean.parseBoolean(characterString));
                    mInPlaceHolderSlot = false;
                } else if (mInColorSlot) {
                    Log.d(LOG_TAG, "Parsing color code: " + characterString);
                    String color = characterString.trim();
                    //Gnucash exports the account color in format #rrrgggbbb, but we need only #rrggbb.
                    //so we trim the last digit in each block, doesn't affect the color much
                    if (!color.equals("Not Set")) {
                        // avoid known exception, printStackTrace is very time consuming
                        if (!Pattern.matches(Account.COLOR_HEX_REGEX, color))
                            color = "#" + color.replaceAll(".(.)?", "$1").replace("null", "");
                        try {
                            if (mAccount != null)
                                mAccount.setColorCode(color);
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
                }
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
                    String q = characterString;
                    if (q.charAt(0) == '-') {
                        mNegativeQuantity = true;
                        q = q.substring(1);
                    } else {
                        mNegativeQuantity = false;
                    }
                    mQuantity = GncXmlHelper.parseSplitAmount(q);
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
                    mSplit.setQuantity(new Money(mQuantity, getCurrencyForAccount(characterString)));
                    //the split value uses the transaction currency
                    mSplit.setValue(new Money(mQuantity, mTransaction.getCurrency()));
                    mSplit.setAccountUID(characterString);
                } else {
                    if (!mIgnoreTemplateTransaction)
                        mTemplateAccountToTransactionMap.put(characterString, mTransaction.getUID());
                }
                break;
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
                    if (mScheduledAction != null) //there might be recurrence tags for bugdets and other stuff
                        mScheduledAction.setPeriod(periodType);
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
                        mScheduledAction.setStartTime(date);
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
                    mScheduledAction.setActionUID(UUID.randomUUID().toString().replaceAll("-",""));
                }
                break;
            case GncXmlHelper.TAG_SCHEDULED_ACTION:
                if (mScheduledAction.getActionUID() != null && !mIgnoreScheduledAction) {
                    mScheduledActionsList.add(mScheduledAction);
                    int count = generateMissedScheduledTransactions(mScheduledAction);
                    Log.i(LOG_TAG, String.format("Generated %d transactions from scheduled action", count));
                }
                mRecurrenceMultiplier = 1; //reset it, even though it will be parsed from XML each time
                mIgnoreScheduledAction = false;
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
            String currencyCode = split.getAccountUID();
            Account imbAccount = mapImbalanceAccount.get(currencyCode);
            if (imbAccount == null) {
                imbAccount = new Account(imbalancePrefix + currencyCode, Currency.getInstance(currencyCode));
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
        long startTime = System.nanoTime();
        mAccountsDbAdapter.beginTransaction();
        try {
            mAccountsDbAdapter.deleteAllRecords();

            long nAccounts = mAccountsDbAdapter.bulkAddRecords(mAccountList);
            Log.d("Handler:", String.format("%d accounts inserted", nAccounts));
            //We need to add scheduled actions first because there is a foreign key constraint on transactions
            //which are generated from scheduled actions (we do auto-create some transactions during import)
            long nSchedActions = mScheduledActionsDbAdapter.bulkAddRecords(mScheduledActionsList);
            Log.d("Handler:", String.format("%d scheduled actions inserted", nSchedActions));

            long nTempTransactions = mTransactionsDbAdapter.bulkAddRecords(mTemplateTransactions);
            Log.d("Handler:", String.format("%d template transactions inserted", nTempTransactions));

            long nTransactions = mTransactionsDbAdapter.bulkAddRecords(mTransactionList);
            Log.d("Handler:", String.format("%d transactions inserted", nTransactions));

            long endTime = System.nanoTime();
            Log.d("Handler:", String.format(" bulk insert time: %d", endTime - startTime));

            mAccountsDbAdapter.setTransactionSuccessful();
        } finally {
            mAccountsDbAdapter.endTransaction();
        }
    }

    /**
     * Returns the currency for an account which has been parsed (but not yet saved to the db)
     * <p>This is used when parsing splits to assign the right currencies to the splits</p>
     * @param accountUID GUID of the account
     * @return Currency of the account
     */
    private Currency getCurrencyForAccount(String accountUID){
        try {
            return mAccountMap.get(accountUID).getCurrency();
        } catch (Exception e) {
            Crashlytics.logException(e);
            return Currency.getInstance(Money.DEFAULT_CURRENCY_CODE);
        }
    }


    /**
     * Handles the case when we reach the end of the template numeric slot
     * @param characterString Parsed characters containing split amount
     */
    private void handleEndOfTemplateNumericSlot(String characterString, TransactionType splitType) {
        try {
            BigDecimal amountBigD = GncXmlHelper.parseSplitAmount(characterString);
            Money amount = new Money(amountBigD, getCurrencyForAccount(mSplit.getAccountUID()));
            mSplit.setValue(amount.absolute());
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
        if (scheduledAction.getLastRun() > 0){
            lastRuntime = scheduledAction.getLastRun();
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
