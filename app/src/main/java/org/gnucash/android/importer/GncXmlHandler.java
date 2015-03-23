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
import android.util.Log;

import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.ScheduledActionDbAdapter;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.export.xml.GncXmlHelper;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Money;
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
     * Used for parsing old backup files where recurrence was saved inside the transaction
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

    private void init(SQLiteDatabase db) {
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
        mTransactionList = new ArrayList<>();
        mScheduledActionsList = new ArrayList<>();
    }

    @Override
    public void startElement(String uri, String localName,
                             String qualifiedName, Attributes attributes) throws SAXException {
        if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_ACCOUNT)) {
            mAccount = new Account(""); // dummy name, will be replaced when we find name tag
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_TRANSACTION)){
            mTransaction = new Transaction(""); // dummy name will be replaced
            mTransaction.setExported(true);     // default to exported when import transactions
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_TRN_SPLIT)){
            mSplit = new Split(Money.getZeroInstance(),"");
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_DATE_POSTED)){
            mIsDatePosted = true;
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_DATE_ENTERED)){
            mIsDateEntered = true;
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_TEMPLATE_TRANSACTIONS)) {
            mInTemplates = true;
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_SCHEDULED_ACTION)){
            //default to transaction type, will be changed during parsing
            mScheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_SX_START)){
            mIsScheduledStart = true;
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_SX_END)){
            mIsScheduledEnd = true;
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_SX_LAST)){
            mIsLastRun = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qualifiedName) throws SAXException {
        String characterString = mContent.toString().trim();

        if (mIgnoreElement != null) {
            // Ignore everything inside
            if (qualifiedName.equalsIgnoreCase(mIgnoreElement)) {
                mIgnoreElement = null;
            }
            mContent.setLength(0);
            return;
        }

        if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_NAME)) {
            mAccount.setName(characterString);
            mAccount.setFullName(characterString);
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_ACCT_ID)){
            mAccount.setUID(characterString);
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_TYPE)){
            AccountType accountType = AccountType.valueOf(characterString);
            mAccount.setAccountType(accountType);
            mAccount.setHidden(accountType == AccountType.ROOT); //flag root account as hidden
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_COMMODITY_SPACE)){
            if (characterString.equalsIgnoreCase("ISO4217")){
                mISO4217Currency = true;
            }
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_COMMODITY_ID)){
            String currencyCode = mISO4217Currency ? characterString : NO_CURRENCY_CODE;
            if (mAccount != null){
                mAccount.setCurrency(Currency.getInstance(currencyCode));
            }
            if (mTransaction != null){
                mTransaction.setCurrencyCode(currencyCode);
            }
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_PARENT_UID)){
            mAccount.setParentUID(characterString);
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_ACCOUNT)){
            if (!mInTemplates) { //we ignore template accounts, we have no use for them
                mAccountList.add(mAccount);
                mAccount = null;
                //reset ISO 4217 flag for next account
                mISO4217Currency = false;
            }
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_SLOT_KEY)){
            if (characterString.equals(GncXmlHelper.KEY_PLACEHOLDER)){
                mInPlaceHolderSlot = true;
            }
            else if (characterString.equals(GncXmlHelper.KEY_COLOR)){
                mInColorSlot = true;
            }
            else if (characterString.equals(GncXmlHelper.KEY_FAVORITE)){
                mInFavoriteSlot = true;
            }
            else if (characterString.equals(GncXmlHelper.KEY_NOTES)){
                mIsNote = true;
            }
            else if (characterString.equals(GncXmlHelper.KEY_DEFAULT_TRANSFER_ACCOUNT)){
                mInDefaultTransferAccount = true;
            }
            else if (characterString.equals(GncXmlHelper.KEY_EXPORTED)){
                mInExported = true;
            } else if (characterString.equals(GncXmlHelper.KEY_SPLIT_ACCOUNT)){
                mInSplitAccountSlot = true;
            } else if (characterString.equals(GncXmlHelper.KEY_CREDIT_FORMULA)){
                mInCreditFormulaSlot = true;
            } else if (characterString.equals(GncXmlHelper.KEY_DEBIT_FORMULA)){
                mInDebitFormulaSlot = true;
            }
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_SLOT_VALUE)){
            if (mInPlaceHolderSlot){
                Log.v(LOG_TAG, "Setting account placeholder flag");
                mAccount.setPlaceHolderFlag(Boolean.parseBoolean(characterString));
                mInPlaceHolderSlot = false;
            }
            else if (mInColorSlot){
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
            }
            else if (mInFavoriteSlot){
                mAccount.setFavorite(Boolean.parseBoolean(characterString));
                mInFavoriteSlot = false;
            }
            else if (mIsNote){
                if (mTransaction != null){
                    mTransaction.setNote(characterString);
                    mIsNote = false;
                }
            }
            else if (mInDefaultTransferAccount){
                mAccount.setDefaultTransferAccountUID(characterString);
                mInDefaultTransferAccount = false;
            }
            else if (mInExported){
                if (mTransaction != null) {
                    mTransaction.setExported(Boolean.parseBoolean(characterString));
                    mInExported = false;
                }
            }
            else if (mInTemplates && mInSplitAccountSlot){
                mSplit.setAccountUID(characterString);
            }
            else if (mInTemplates && mInCreditFormulaSlot){
                NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.GERMANY);
                try {
                    Number number = numberFormat.parse(characterString);
                    Money amount = new Money(new BigDecimal(number.doubleValue()), mTransaction.getCurrency());
                    mSplit.setAmount(amount.absolute());
                    mSplit.setType(TransactionType.CREDIT);
                } catch (ParseException e) {
                    Log.e(LOG_TAG, "Error parsing template split amount. " + e.getMessage());
                    e.printStackTrace();
                }
            }
            else if (mInTemplates && mInDebitFormulaSlot){
                NumberFormat numberFormat = GncXmlHelper.getNumberFormatForTemplateSplits();
                try {
                    Number number = numberFormat.parse(characterString);
                    Money amount = new Money(new BigDecimal(number.doubleValue()), mTransaction.getCurrency());
                    mSplit.setAmount(amount.absolute());
                    mSplit.setType(TransactionType.DEBIT);
                } catch (ParseException e) {
                    Log.e(LOG_TAG, "Error parsing template split amount. " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }


        //================  PROCESSING OF TRANSACTION TAGS =====================================
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_TRX_ID)){
            mTransaction.setUID(characterString);
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_TRN_DESCRIPTION)){
            mTransaction.setDescription(characterString);
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_DATE)){
            try {
                if (mIsDatePosted && mTransaction != null) {
                    mTransaction.setTime(GncXmlHelper.parseDate(characterString));
                    mIsDatePosted = false;
                }
                if (mIsDateEntered && mTransaction != null){
                    Timestamp timestamp = new Timestamp(GncXmlHelper.parseDate(characterString));
                    mTransaction.setCreatedTimestamp(timestamp);
                    mIsDateEntered = false;
                }
                if (mIsScheduledStart){
                    mScheduledAction.setStartTime(GncXmlHelper.DATE_FORMATTER.parse(characterString).getTime());
                }

                if (mIsScheduledEnd){
                    mScheduledAction.setEndTime(GncXmlHelper.DATE_FORMATTER.parse(characterString).getTime());
                }

                if (mIsLastRun){
                    mScheduledAction.setLastRun(GncXmlHelper.DATE_FORMATTER.parse(characterString).getTime());
                }
            } catch (ParseException e) {
                e.printStackTrace();
                throw new SAXException("Unable to parse transaction time", e);
            }
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_RECURRENCE_PERIOD)){
            mRecurrencePeriod = Long.parseLong(characterString);
            mTransaction.setTemplate(mRecurrencePeriod > 0);
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_SPLIT_ID)){
            mSplit.setUID(characterString);
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_SPLIT_MEMO)){
            mSplit.setMemo(characterString);
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_SPLIT_VALUE)){
            Money amount = new Money(GncXmlHelper.parseMoney(characterString), mTransaction.getCurrency());
            mSplit.setType(amount.isNegative() ? TransactionType.CREDIT : TransactionType.DEBIT);
            mSplit.setAmount(amount.absolute());
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_SPLIT_ACCOUNT)){
            mSplit.setAccountUID(characterString);
        }
        else if (qualifiedName.equals(GncXmlHelper.TAG_TRN_SPLIT)){
            mTransaction.addSplit(mSplit);
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_TRANSACTION)){
            mTransaction.setTemplate(mInTemplates);
            mTransactionList.add(mTransaction);
            if (mRecurrencePeriod > 0) { //if we find an old format recurrence period, parse it
                mTransaction.setTemplate(true);
                ScheduledAction scheduledAction = ScheduledAction.parseScheduledAction(mTransaction, mRecurrencePeriod);
                mScheduledActionsList.add(scheduledAction);
            }
            mRecurrencePeriod = 0;
            mTransaction = null;
        } else if (qualifiedName.equals(GncXmlHelper.TAG_TEMPLATE_TRANSACTIONS)){
            mInTemplates = false;
        }

        // ========================= PROCESSING SCHEDULED ACTIONS ==================================
        else if (qualifiedName.equals(GncXmlHelper.TAG_SX_ID)){
            mScheduledAction.setUID(characterString);
        }
        else if (qualifiedName.equals(GncXmlHelper.TAG_SX_NAME)){
            ScheduledAction.ActionType type = ScheduledAction.ActionType.valueOf(characterString);
            mScheduledAction.setActionType(type);
        }
        else if (qualifiedName.equals(GncXmlHelper.TAG_SX_ENABLED)){
            mScheduledAction.setEnabled(characterString.equalsIgnoreCase("y"));
        }
        else if (qualifiedName.equals(GncXmlHelper.TAG_SX_NUM_OCCUR)){
            mScheduledAction.setNumberOfOccurences(Integer.parseInt(characterString));
        }
        else if (qualifiedName.equals(GncXmlHelper.TAG_RX_MULT)){
            mRecurrenceMultiplier = Integer.parseInt(characterString);
        }
        else if (qualifiedName.equals(GncXmlHelper.TAG_RX_PERIOD_TYPE)){
            ScheduledAction.PeriodType periodType = ScheduledAction.PeriodType.valueOf(characterString.toUpperCase());
            periodType.setMultiplier(mRecurrenceMultiplier);
            mScheduledAction.setPeriod(periodType);
        }
        else if (qualifiedName.equals(GncXmlHelper.TAG_SX_TEMPL_ACTION)){
            mScheduledAction.setActionUID(characterString);
        }
        else if (qualifiedName.equals(GncXmlHelper.TAG_SCHEDULED_ACTION)){
            mScheduledActionsList.add(mScheduledAction);
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
        HashMap<String, Account> map = new HashMap<String, Account>(mAccountList.size());
        HashMap<String, String> mapFullName = new HashMap<String, String>(mAccountList.size());
        for(Account account:mAccountList) {
            map.put(account.getUID(), account);
            mapFullName.put(account.getUID(), null);
        }
        java.util.Stack<Account> stack = new Stack<Account>();
        for (Account account:mAccountList){
            if (mapFullName.get(account.getUID()) != null) {
                continue;
            }
            stack.push(account);
            String parentAccountFullName;
            while (!stack.isEmpty()) {
                Account acc = stack.peek();
                if (acc.getAccountType() == AccountType.ROOT) {
                    // append blank to Root Account, ensure it always sorts first
                    mapFullName.put(acc.getUID(), " " + acc.getName());
                    stack.pop();
                    continue;
                }
                String parentUID = acc.getParentUID();
                Account parentAccount = map.get(parentUID);
                // In accounts tree that are not imported, top level ROOT account
                // does not exist, which will make all top level accounts have a
                // null parent
                if (parentAccount == null || parentAccount.getAccountType() == AccountType.ROOT) {
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
        long nAccounts = mAccountsDbAdapter.bulkAddAccounts(mAccountList);
        Log.d("Handler:", String.format("%d accounts inserted", nAccounts));
        long nTransactions = mTransactionsDbAdapter.bulkAddTransactions(mTransactionList);
        Log.d("Handler:", String.format("%d transactions inserted", nTransactions));
        int nSchedActions = mScheduledActionsDbAdapter.bulkAddScheduledActions(mScheduledActionsList);
        Log.d("Handler:", String.format("%d scheduled actions inserted", nSchedActions));
        long endTime = System.nanoTime();
        Log.d("Handler:", String.format(" bulk insert time: %d", endTime - startTime));

    }
}
