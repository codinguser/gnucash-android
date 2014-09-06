/*
 * Copyright (c) 2013 - 2014 Ngewi Fet <ngewif@gmail.com>
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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.export.xml.GncXmlHelper;
import org.gnucash.android.model.*;
import org.gnucash.android.db.AccountsDbAdapter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.text.ParseException;
import java.util.Currency;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

/**
 * Handler for parsing the GnuCash XML file.
 * The discovered accounts and transactions are automatically added to the database
 *
 * @author Ngewi Fet <ngewif@gmail.com>
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
     * Showing whether we are in bulk import mode
     */
    boolean mBulk = false;

    boolean mInColorSlot        = false;
    boolean mInPlaceHolderSlot  = false;
    boolean mInFavoriteSlot     = false;
    boolean mISO4217Currency    = false;
    boolean mIsDatePosted       = false;
    boolean mIsNote             = false;
    boolean mInDefaultTransferAccount = false;
    boolean mInExported         = false;

    private Context mContext;
    private TransactionsDbAdapter mTransactionsDbAdapter;

    public GncXmlHandler(Context context) {
        init(context, false);
    }

    public GncXmlHandler(Context context, boolean bulk) {
        init(context, bulk);
    }

    private void init(Context context, boolean bulk) {
        mContext = context;
        mAccountsDbAdapter = new AccountsDbAdapter(mContext);
        mTransactionsDbAdapter = new TransactionsDbAdapter(mContext);
        mContent = new StringBuilder();
        mBulk = bulk;
        if (bulk) {
            mAccountList = new ArrayList<Account>();
            mTransactionList = new ArrayList<Transaction>();
        }
    }

    /**
     * Instantiates handler to parse XML into already open db
     * @param db SQLite Database
     */
    public GncXmlHandler(SQLiteDatabase db){
        mContext = GnuCashApplication.getAppContext();
        mAccountsDbAdapter = new AccountsDbAdapter(db);
        mTransactionsDbAdapter = new TransactionsDbAdapter(db);
        mContent = new StringBuilder();
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
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_TEMPLATE_TRANSACTION)) {
            mIgnoreElement = GncXmlHelper.TAG_TEMPLATE_TRANSACTION;
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
            mAccount.setAccountType(AccountType.valueOf(characterString));
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
            if (mBulk) {
                mAccountList.add(mAccount);
            }
            else {
                Log.d(LOG_TAG, "Saving account...");
                mAccountsDbAdapter.addAccount(mAccount);
            }
            mAccount = null;
            //reset ISO 4217 flag for next account
            mISO4217Currency = false;
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
            } catch (ParseException e) {
                e.printStackTrace();
                throw new SAXException("Unable to parse transaction time", e);
            }
        }
        else if (qualifiedName.equalsIgnoreCase(GncXmlHelper.TAG_RECURRENCE_PERIOD)){
            mTransaction.setRecurrencePeriod(Long.parseLong(characterString));
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
            if (mBulk) {
                mTransactionList.add(mTransaction);
            }
            else {
                if (mTransaction.getRecurrencePeriod() > 0) { //TODO: Fix this when scheduled actions are expanded
                    mTransactionsDbAdapter.scheduleTransaction(mTransaction);
                } else {
                    mTransactionsDbAdapter.addTransaction(mTransaction);
                }
            }
            mTransaction = null;
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
        if (mBulk) {
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
                String parentAccountFullName = null;
                while (!stack.isEmpty()) {
                    Account acc = stack.peek();
                    if (acc.getAccountType().name().equals("ROOT")) {
                        mapFullName.put(acc.getUID(), "");
                        stack.pop();
                        parentAccountFullName = "";
                        continue;
                    }
                    if (mapFullName.get(acc.getParentUID()) == null) {
                        stack.push(map.get(acc.getParentUID()));
                        continue;
                    }
                    else {
                        parentAccountFullName = mapFullName.get(acc.getParentUID());
                    }
                    if (parentAccountFullName != null) {
                        parentAccountFullName = parentAccountFullName.length() == 0 ? acc.getName() :
                                (parentAccountFullName + AccountsDbAdapter.ACCOUNT_NAME_SEPARATOR + acc.getName());
                        mapFullName.put(acc.getUID(), parentAccountFullName);
                        stack.pop();
                    }
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
            long endTime = System.nanoTime();
            Log.d("Handler:", String.format(" bulk insert time: %d", endTime - startTime));
        }
        mAccountsDbAdapter.close();
        mTransactionsDbAdapter.close();
    }
}
