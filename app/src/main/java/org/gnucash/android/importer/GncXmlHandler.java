/*
 * Copyright (c) 2013 - 2015 Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2014 Yongxin Wang <fefe.wyx@gmail.com>
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
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
     * Accumulate attributes of splits found in this object
     */
    Split mSplit;

    /**
     * (Absolute) quantity of the split
     */
    BigDecimal mQuantity;

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
    boolean mInCreditFormulaSlot = false;
    boolean mInDebitFormulaSlot = false;
    boolean mIsScheduledStart   = false;
    boolean mIsScheduledEnd     = false;
    boolean mIsLastRun          = false;

    /**
     * Multiplier for the recurrence period type. e.g. period type of week and multiplier of 2 means bi-weekly
     */
    int mRecurrenceMultiplier   = 1;

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
                mSplit = new Split(Money.getZeroInstance(),"");
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
                            throw new SAXException("multiple ROOT accounts exist in book");
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
                    case GncXmlHelper.KEY_SPLIT_ACCOUNT:
                        mInSplitAccountSlot = true;
                        break;
                    case GncXmlHelper.KEY_CREDIT_FORMULA:
                        mInCreditFormulaSlot = true;
                        break;
                    case GncXmlHelper.KEY_DEBIT_FORMULA:
                        mInDebitFormulaSlot = true;
                        break;
                }
                break;
            case GncXmlHelper.TAG_SLOT_VALUE:
                if (mInPlaceHolderSlot) {
                    Log.v(LOG_TAG, "Setting account placeholder flag");
                    mAccount.setPlaceHolderFlag(Boolean.parseBoolean(characterString));
                    mInPlaceHolderSlot = false;
                } else if (mInColorSlot) {
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
                            Log.i(LOG_TAG, "Invalid color code '" + color + "' for account " + mAccount.getName());
                            ex.printStackTrace();
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
                } else if (mInTemplates && mInCreditFormulaSlot) {
                    NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.GERMANY);
                    try {
                        Number number = numberFormat.parse(characterString);
                        Money amount = new Money(new BigDecimal(number.doubleValue()), mTransaction.getCurrency());
                        mSplit.setAmount(amount.absolute());
                        mSplit.setType(TransactionType.CREDIT);
                    } catch (ParseException e) {
                        Log.e(LOG_TAG, "Error parsing template split amount. " + e.getMessage());
                        e.printStackTrace();
                    } finally {
                        mInCreditFormulaSlot = false;
                    }
                } else if (mInTemplates && mInDebitFormulaSlot) {
                    try {
                        // TODO: test this. I do not have template transactions to test
                        // Going through double to decimal will lose accuracy.
                        // NEVER use double for money.
                        // from Android SDK Ddoc:
                        //    new BigDecimal(0.1) is equal to 0.1000000000000000055511151231257827021181583404541015625. This happens as 0.1 cannot be represented exactly in binary.
                        //    To generate a big decimal instance which is equivalent to 0.1 use the BigDecimal(String) constructor.
                        Money amount = new Money(new BigDecimal(characterString), mTransaction.getCurrency());
                        mSplit.setAmount(amount.absolute());
                        mSplit.setType(TransactionType.DEBIT);
                    } catch (NumberFormatException e) {
                        Log.e(LOG_TAG, "Error parsing template split amount. " + e.getMessage());
                        e.printStackTrace();
                    } finally {
                        mInDebitFormulaSlot = false;
                    }
                }
                break;
            //================  PROCESSING OF TRANSACTION TAGS =====================================
            case GncXmlHelper.TAG_TRX_ID:
                mTransaction.setUID(characterString);
                break;
            case GncXmlHelper.TAG_TRN_DESCRIPTION:
                mTransaction.setDescription(characterString);
                break;
            case GncXmlHelper.TAG_DATE:
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
                    if (mIsScheduledStart && mScheduledAction != null) {
                        mScheduledAction.setStartTime(GncXmlHelper.DATE_FORMATTER.parse(characterString).getTime());
                        mIsScheduledStart = false;
                    }

                    if (mIsScheduledEnd && mScheduledAction != null) {
                        mScheduledAction.setEndTime(GncXmlHelper.DATE_FORMATTER.parse(characterString).getTime());
                        mIsScheduledEnd = false;
                    }

                    if (mIsLastRun && mScheduledAction != null) {
                        mScheduledAction.setLastRun(GncXmlHelper.DATE_FORMATTER.parse(characterString).getTime());
                        mIsLastRun = false;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                    throw new SAXException("Unable to parse transaction time", e);
                }
                break;
            case GncXmlHelper.TAG_RECURRENCE_PERIOD:
                mRecurrencePeriod = Long.parseLong(characterString);
                mTransaction.setTemplate(mRecurrencePeriod > 0);
                break;
            case GncXmlHelper.TAG_SPLIT_ID:
                mSplit.setUID(characterString);
                break;
            case GncXmlHelper.TAG_SPLIT_MEMO:
                mSplit.setMemo(characterString);
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
                    mQuantity = GncXmlHelper.parseMoney(q);
                } catch (ParseException e) {
                    e.printStackTrace();
                    throw new SAXException("Unable to parse money", e);
                }
                break;
            case GncXmlHelper.TAG_SPLIT_ACCOUNT:
                //the split amount uses the account currency
                Money amount = new Money(mQuantity, getCurrencyForAccount(characterString));
                //this is intentional: GnuCash XML formats split amounts, credits are negative, debits are positive.
                mSplit.setType(mNegativeQuantity ? TransactionType.CREDIT : TransactionType.DEBIT);
                mSplit.setAmount(amount);
                mSplit.setAccountUID(characterString);
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
                mTransactionList.add(mTransaction);

                if (mRecurrencePeriod > 0) { //if we find an old format recurrence period, parse it
                    mTransaction.setTemplate(true);
                    ScheduledAction scheduledAction = ScheduledAction.parseScheduledAction(mTransaction, mRecurrencePeriod);
                    mScheduledActionsList.add(scheduledAction);
                }
                mRecurrencePeriod = 0;
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
                //FIXME: Do not rely on the type, rather lookup the SX_ID from previous tag to find action type
                ScheduledAction.ActionType type = ScheduledAction.ActionType.valueOf(characterString);
                mScheduledAction.setActionType(type);
                break;
            case GncXmlHelper.TAG_SX_ENABLED:
                mScheduledAction.setEnabled(characterString.equals("y"));
                break;
            case GncXmlHelper.TAG_SX_NUM_OCCUR:
                mScheduledAction.setTotalFrequency(Integer.parseInt(characterString));
                break;
            case GncXmlHelper.TAG_RX_MULT:
                mRecurrenceMultiplier = Integer.parseInt(characterString);
                break;
            case GncXmlHelper.TAG_RX_PERIOD_TYPE:
                PeriodType periodType = PeriodType.valueOf(characterString.toUpperCase());
                periodType.setMultiplier(mRecurrenceMultiplier);
                mScheduledAction.setPeriod(periodType);
                break;
            case GncXmlHelper.TAG_SX_TEMPL_ACTION:
                mScheduledAction.setActionUID(characterString);
                break;
            case GncXmlHelper.TAG_SCHEDULED_ACTION:
                mScheduledActionsList.add(mScheduledAction);
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
            long nAccounts = mAccountsDbAdapter.bulkAddAccounts(mAccountList);
            Log.d("Handler:", String.format("%d accounts inserted", nAccounts));
            long nTransactions = mTransactionsDbAdapter.bulkAddTransactions(mTransactionList);
            Log.d("Handler:", String.format("%d transactions inserted", nTransactions));
            int nSchedActions = mScheduledActionsDbAdapter.bulkAddScheduledActions(mScheduledActionsList);
            Log.d("Handler:", String.format("%d scheduled actions inserted", nSchedActions));
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
            e.printStackTrace();
            return Currency.getInstance(Money.DEFAULT_CURRENCY_CODE);
        }
    }
}
