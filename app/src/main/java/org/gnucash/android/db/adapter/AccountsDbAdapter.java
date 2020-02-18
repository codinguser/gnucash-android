/*
 * Copyright (c) 2012 - 2015 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.db.adapter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;
import org.gnucash.android.util.TimestampHelper;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static org.gnucash.android.db.DatabaseSchema.AccountEntry;
import static org.gnucash.android.db.DatabaseSchema.SplitEntry;
import static org.gnucash.android.db.DatabaseSchema.TransactionEntry;

/**
 * Manages persistence of {@link Account}s in the database
 * Handles adding, modifying and deleting of account records.
 * @author Ngewi Fet <ngewif@gmail.com>
 * @author Yongxin Wang <fefe.wyx@gmail.com>
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class AccountsDbAdapter extends DatabaseAdapter<Account> {

    /**
     * Separator used for account name hierarchies between parent and child accounts
     */
    public static final String ACCOUNT_NAME_SEPARATOR = ":";

    /**
     * ROOT account full name.
     * should ensure the ROOT account's full name will always sort before any other
     * account's full name.
     */
    public static final String ROOT_ACCOUNT_FULL_NAME = " ";

    /**
     * Where clause to get non hidden nor root account
     */
    public static final String WHERE_NOT_HIDDEN_AND_NOT_ROOT_ACCOUNT =
            AccountEntry.COLUMN_HIDDEN + " = 0 AND " + AccountEntry.COLUMN_TYPE + " != ?";


    /**
	 * Transactions database adapter for manipulating transactions associated with accounts
	 */
    private final TransactionsDbAdapter mTransactionsAdapter;

    /**
     * Commodities database adapter for commodity manipulation
     */
    private final CommoditiesDbAdapter mCommoditiesDbAdapter;

    /**
     * Overloaded constructor. Creates an adapter for an already open database
     * @param db SQliteDatabase instance
     */
    public AccountsDbAdapter(SQLiteDatabase db, TransactionsDbAdapter transactionsDbAdapter) {
        super(db, AccountEntry.TABLE_NAME, new String[]{
                AccountEntry.COLUMN_NAME         ,
                AccountEntry.COLUMN_DESCRIPTION  ,
                AccountEntry.COLUMN_TYPE         ,
                AccountEntry.COLUMN_CURRENCY     ,
                AccountEntry.COLUMN_COLOR_CODE   ,
                AccountEntry.COLUMN_FAVORITE     ,
                AccountEntry.COLUMN_FULL_NAME    ,
                AccountEntry.COLUMN_PLACEHOLDER  ,
                AccountEntry.COLUMN_CREATED_AT   ,
                AccountEntry.COLUMN_HIDDEN       ,
                AccountEntry.COLUMN_COMMODITY_UID,
                AccountEntry.COLUMN_PARENT_ACCOUNT_UID,
                AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID
        });
        mTransactionsAdapter = transactionsDbAdapter;
        mCommoditiesDbAdapter = new CommoditiesDbAdapter(db);
    }

    /**
     * Convenience overloaded constructor. 
     * This is used when an AccountsDbAdapter object is needed quickly. Otherwise, the other 
     * constructor {@link #AccountsDbAdapter(SQLiteDatabase, TransactionsDbAdapter)}
     * should be used whenever possible
     * @param db Database to create an adapter for
     */
    public AccountsDbAdapter(SQLiteDatabase db){
        super(db, AccountEntry.TABLE_NAME, new String[]{
                AccountEntry.COLUMN_NAME         ,
                AccountEntry.COLUMN_DESCRIPTION  ,
                AccountEntry.COLUMN_TYPE         ,
                AccountEntry.COLUMN_CURRENCY     ,
                AccountEntry.COLUMN_COLOR_CODE   ,
                AccountEntry.COLUMN_FAVORITE     ,
                AccountEntry.COLUMN_FULL_NAME    ,
                AccountEntry.COLUMN_PLACEHOLDER  ,
                AccountEntry.COLUMN_CREATED_AT   ,
                AccountEntry.COLUMN_HIDDEN       ,
                AccountEntry.COLUMN_COMMODITY_UID,
                AccountEntry.COLUMN_PARENT_ACCOUNT_UID,
                AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID
        });

        mTransactionsAdapter = new TransactionsDbAdapter(db, new SplitsDbAdapter(db));
        mCommoditiesDbAdapter = new CommoditiesDbAdapter(db);
    }

    /**
     * Returns an application-wide instance of this database adapter
     * @return Instance of Accounts db adapter
     */
    public static AccountsDbAdapter getInstance(){
        return GnuCashApplication.getAccountsDbAdapter();
    }

    /**
	 * Adds an account to the database. 
	 * If an account already exists in the database with the same GUID, it is replaced.
	 * @param account {@link Account} to be inserted to database
	 */
    @Override
	public void addRecord(@NonNull Account account, UpdateMethod updateMethod){
        Log.d(LOG_TAG, "Replace account to db");
        //in-case the account already existed, we want to update the templates based on it as well
        List<Transaction> templateTransactions = mTransactionsAdapter.getScheduledTransactionsForAccount(account.getUID());
        super.addRecord(account, updateMethod);
        String accountUID = account.getUID();
		//now add transactions if there are any
		if (account.getAccountType() != AccountType.ROOT){
            //update the fully qualified account name
            updateRecord(accountUID, AccountEntry.COLUMN_FULL_NAME, getFullyQualifiedAccountName(accountUID));
            for (Transaction t : account.getTransactions()) {
                t.setCommodity(account.getCommodity());
		        mTransactionsAdapter.addRecord(t, updateMethod);
			}
            for (Transaction transaction : templateTransactions) {
                mTransactionsAdapter.addRecord(transaction, UpdateMethod.update);
            }
        }
	}

    /**
     * Adds some accounts and their transactions to the database in bulk.
     * <p>If an account already exists in the database with the same GUID, it is replaced.
     * This function will NOT try to determine the full name
     * of the accounts inserted, full names should be generated prior to the insert.
     * <br>All or none of the accounts will be inserted;</p>
     * @param accountList {@link Account} to be inserted to database
     * @return number of rows inserted
     */
    @Override
    public long bulkAddRecords(@NonNull List<Account> accountList, UpdateMethod updateMethod){
        //scheduled transactions are not fetched from the database when getting account transactions
        //so we retrieve those which affect this account and then re-save them later
        //this is necessary because the database has ON DELETE CASCADE between accounts and splits
        //and all accounts are editing via SQL REPLACE

        //// TODO: 20.04.2016 Investigate if we can safely remove updating the transactions when bulk updating accounts
        List<Transaction> transactionList = new ArrayList<>(accountList.size()*2);
        for (Account account : accountList) {
            transactionList.addAll(account.getTransactions());
            transactionList.addAll(mTransactionsAdapter.getScheduledTransactionsForAccount(account.getUID()));
        }
        long nRow = super.bulkAddRecords(accountList, updateMethod);

        if (nRow > 0 && !transactionList.isEmpty()){
            mTransactionsAdapter.bulkAddRecords(transactionList, updateMethod);
        }
        return nRow;
    }

    @Override
    protected @NonNull SQLiteStatement setBindings(@NonNull SQLiteStatement stmt, @NonNull final Account account) {
        stmt.clearBindings();
        stmt.bindString(1, account.getName());
        if (account.getDescription() != null)
            stmt.bindString(2, account.getDescription());
        stmt.bindString(3, account.getAccountType().name());
        stmt.bindString(4, account.getCommodity().getCurrencyCode());
        if (account.getColor() != Account.DEFAULT_COLOR) {
            stmt.bindString(5, account.getColorHexString());
        }
        stmt.bindLong(6, account.isFavorite() ? 1 : 0);
        stmt.bindString(7, account.getFullName());
        stmt.bindLong(8, account.isPlaceholderAccount() ? 1 : 0);
        stmt.bindString(9, TimestampHelper.getUtcStringFromTimestamp(account.getCreatedTimestamp()));
        stmt.bindLong(10, account.isHidden() ? 1 : 0);
        stmt.bindString(11, account.getCommodity().getUID());

        String parentAccountUID = account.getParentUID();
        if (parentAccountUID == null && account.getAccountType() != AccountType.ROOT) {
            parentAccountUID = getOrCreateGnuCashRootAccountUID();
        }
        if (parentAccountUID != null) {
            stmt.bindString(12, parentAccountUID);
        }
        if (account.getDefaultTransferAccountUID() != null) {
            stmt.bindString(13, account.getDefaultTransferAccountUID());
        }
        stmt.bindString(14, account.getUID());

        return stmt;
    }

    /**
     * Marks all transactions for a given account as exported
     * @param accountUID Unique ID of the record to be marked as exported
     * @return Number of records marked as exported
     */
    public int markAsExported(String accountUID){
        ContentValues contentValues = new ContentValues();
        contentValues.put(TransactionEntry.COLUMN_EXPORTED, 1);
        return mDb.update(
                TransactionEntry.TABLE_NAME,
                contentValues,
                TransactionEntry.COLUMN_UID + " IN ( " +
                        "SELECT DISTINCT " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID +
                        " FROM " + TransactionEntry.TABLE_NAME + " , " + SplitEntry.TABLE_NAME + " ON " +
                        TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID + " = " +
                        SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_UID + " , " +
                        AccountEntry.TABLE_NAME + " ON " + SplitEntry.TABLE_NAME + "." +
                        SplitEntry.COLUMN_ACCOUNT_UID + " = " + AccountEntry.TABLE_NAME + "." +
                        AccountEntry.COLUMN_UID + " WHERE " + AccountEntry.TABLE_NAME + "." +
                        AccountEntry.COLUMN_UID + " = ? "
                        + " ) ",
                new String[]{accountUID}
        );
    }

    /**
     * This feature goes through all the rows in the accounts and changes value for <code>columnKey</code> to <code>newValue</code><br/>
     * The <code>newValue</code> parameter is taken as string since SQLite typically stores everything as text.
     * <p><b>This method affects all rows, exercise caution when using it</b></p>
     * @param columnKey Name of column to be updated
     * @param newValue New value to be assigned to the columnKey
     * @return Number of records affected
     */
    public int updateAllAccounts(String columnKey, String newValue){
        ContentValues contentValues = new ContentValues();
        if (newValue == null) {
            contentValues.putNull(columnKey);
        } else {
            contentValues.put(columnKey, newValue);
        }
        return mDb.update(AccountEntry.TABLE_NAME, contentValues, null, null);
    }

    /**
     * Updates a specific entry of an account
     * @param accountId Database record ID of the account to be updated
     * @param columnKey Name of column to be updated
     * @param newValue  New value to be assigned to the columnKey
     * @return Number of records affected
     */
    public int updateAccount(long accountId, String columnKey, String newValue){
        return updateRecord(AccountEntry.TABLE_NAME, accountId, columnKey, newValue);
    }

    /**
     * This method goes through all the children of {@code accountUID} and updates the parent account
     * to {@code newParentAccountUID}. The fully qualified account names for all descendant accounts will also be updated.
     * @param accountUID GUID of the account
     * @param newParentAccountUID GUID of the new parent account
     */
    public void reassignDescendantAccounts(@NonNull String accountUID, @NonNull String newParentAccountUID) {
        List<String> descendantAccountUIDs = getDescendantAccountUIDs(accountUID, null, null);
        if (descendantAccountUIDs.size() > 0) {
            List<Account> descendantAccounts = getSimpleAccountList(
                    AccountEntry.COLUMN_UID + " IN ('" + TextUtils.join("','", descendantAccountUIDs) + "')",
                    null,
                    null
            );
            HashMap<String, Account> mapAccounts = new HashMap<>();
            for (Account account : descendantAccounts)
                mapAccounts.put(account.getUID(), account);
            String parentAccountFullName;
            if (getAccountType(newParentAccountUID) == AccountType.ROOT) {
                parentAccountFullName = "";
            } else {
                parentAccountFullName = getAccountFullName(newParentAccountUID);
            }
            ContentValues contentValues = new ContentValues();
            for (String acctUID : descendantAccountUIDs) {
                Account acct = mapAccounts.get(acctUID);
                if (accountUID.equals(acct.getParentUID())) {
                    // direct descendant
                    acct.setParentUID(newParentAccountUID);
                    if (parentAccountFullName == null || parentAccountFullName.isEmpty()) {
                        acct.setFullName(acct.getName());
                    } else {
                        acct.setFullName(parentAccountFullName + ACCOUNT_NAME_SEPARATOR + acct.getName());
                    }
                    // update DB
                    contentValues.clear();
                    contentValues.put(AccountEntry.COLUMN_PARENT_ACCOUNT_UID, newParentAccountUID);
                    contentValues.put(AccountEntry.COLUMN_FULL_NAME, acct.getFullName());
                    mDb.update(
                            AccountEntry.TABLE_NAME, contentValues,
                            AccountEntry.COLUMN_UID + " = ?",
                            new String[]{acct.getUID()}
                    );
                } else {
                    // indirect descendant
                    acct.setFullName(
                            mapAccounts.get(acct.getParentUID()).getFullName() +
                                    ACCOUNT_NAME_SEPARATOR + acct.getName()
                    );
                    // update DB
                    contentValues.clear();
                    contentValues.put(AccountEntry.COLUMN_FULL_NAME, acct.getFullName());
                    mDb.update(
                            AccountEntry.TABLE_NAME, contentValues,
                            AccountEntry.COLUMN_UID + " = ?",
                            new String[]{acct.getUID()}
                    );
                }
            }
        }
    }

    /**
     * Deletes an account and its transactions, and all its sub-accounts and their transactions.
     * <p>Not only the splits belonging to the account and its descendants will be deleted, rather,
     * the complete transactions associated with this account and its descendants
     * (i.e. as long as the transaction has at least one split belonging to one of the accounts).
     * This prevents an split imbalance from being caused.</p>
     * <p>If you want to preserve transactions, make sure to first reassign the children accounts (see {@link #reassignDescendantAccounts(String, String)}
     * before calling this method. This method will however not delete a root account. </p>
     * <p><b>This method does a thorough delete, use with caution!!!</b></p>
     * @param accountId Database record ID of account
     * @return <code>true</code> if the account and subaccounts were all successfully deleted, <code>false</code> if
     * even one was not deleted
     * @see #reassignDescendantAccounts(String, String)
     */
    public boolean recursiveDeleteAccount(long accountId){
        String accountUID = getUID(accountId);
        if (getAccountType(accountUID) == AccountType.ROOT) {
            // refuse to delete ROOT
            return false;
        }

        Log.d(LOG_TAG, "Delete account with rowId with its transactions and sub-accounts: " + accountId);

        List<String> descendantAccountUIDs = getDescendantAccountUIDs(accountUID, null, null);
        mDb.beginTransaction();
        try {
            descendantAccountUIDs.add(accountUID); //add account to descendants list just for convenience
            for (String descendantAccountUID : descendantAccountUIDs) {
                mTransactionsAdapter.deleteTransactionsForAccount(descendantAccountUID);
            }

            String accountUIDList = "'" + TextUtils.join("','", descendantAccountUIDs) + "'";

            // delete accounts
            long deletedCount = mDb.delete(
                    AccountEntry.TABLE_NAME,
                    AccountEntry.COLUMN_UID + " IN (" + accountUIDList + ")",
                    null
            );

            //if we delete some accounts, reset the default transfer account to NULL
            //there is also a database trigger from db version > 12
            if (deletedCount > 0){
                ContentValues contentValues = new ContentValues();
                contentValues.putNull(AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID);
                mDb.update(mTableName, contentValues,
                        AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID + " IN (" + accountUIDList + ")",
                        null);
            }

            mDb.setTransactionSuccessful();
            return true;
        }
        finally {
            mDb.endTransaction();
        }
    }

	/**
	 * Builds an account instance with the provided cursor and loads its corresponding transactions.
	 *
	 * @param c Cursor pointing to account record in database
	 * @return {@link Account} object constructed from database record
	 */
    @Override
    public Account buildModelInstance(@NonNull final Cursor c){
        Account account = buildSimpleAccountInstance(c);
        account.setTransactions(mTransactionsAdapter.getAllTransactionsForAccount(account.getUID()));

        return account;
	}

    /**
     * Builds an account instance with the provided cursor and loads its corresponding transactions.
     * <p>The method will not move the cursor position, so the cursor should already be pointing
     * to the account record in the database<br/>
     * <b>Note</b> Unlike {@link  #buildModelInstance(android.database.Cursor)} this method will not load transactions</p>
     *
     * @param c Cursor pointing to account record in database
     * @return {@link Account} object constructed from database record
     */
    private Account buildSimpleAccountInstance(Cursor c) {
        Account account = new Account(c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_NAME)));
        populateBaseModelAttributes(c, account);

        String description = c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_DESCRIPTION));
        account.setDescription(description == null ? "" : description);
        account.setParentUID(c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_PARENT_ACCOUNT_UID)));
        account.setAccountType(AccountType.valueOf(c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_TYPE))));
        String currencyCode = c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_CURRENCY));
        account.setCommodity(mCommoditiesDbAdapter.getCommodity(currencyCode));
        account.setPlaceHolderFlag(c.getInt(c.getColumnIndexOrThrow(AccountEntry.COLUMN_PLACEHOLDER)) == 1);
        account.setDefaultTransferAccountUID(c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID)));
        String color = c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_COLOR_CODE));
        if (color != null)
            account.setColor(color);
        account.setFavorite(c.getInt(c.getColumnIndexOrThrow(AccountEntry.COLUMN_FAVORITE)) == 1);
        account.setFullName(c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_FULL_NAME)));
        account.setHidden(c.getInt(c.getColumnIndexOrThrow(AccountEntry.COLUMN_HIDDEN)) == 1);
        return account;
    }

    /**
	 * Returns the  unique ID of the parent account of the account with unique ID <code>uid</code>
	 * If the account has no parent, null is returned
	 * @param uid Unique Identifier of account whose parent is to be returned. Should not be null
	 * @return DB record UID of the parent account, null if the account has no parent
	 */
    public String getParentAccountUID(@NonNull String uid){
		Cursor cursor = mDb.query(AccountEntry.TABLE_NAME,
                new String[]{AccountEntry.COLUMN_PARENT_ACCOUNT_UID},
                AccountEntry.COLUMN_UID + " = ?",
                new String[]{uid},
                null, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                Log.d(LOG_TAG, "Found parent account UID, returning value");
                return cursor.getString(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_PARENT_ACCOUNT_UID));
            } else {
                return null;
            }
        } finally {
            cursor.close();
        }
	}

    /**
     * Returns the color code for the account in format #rrggbb
     * @param accountId Database row ID of the account
     * @return String color code of account or null if none
     */
    public String getAccountColorCode(long accountId){
        Cursor c = mDb.query(AccountEntry.TABLE_NAME,
                new String[]{AccountEntry._ID, AccountEntry.COLUMN_COLOR_CODE},
                AccountEntry._ID + "=" + accountId,
                null, null, null, null);
        try {
            if (c.moveToFirst()) {
                return c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_COLOR_CODE));
            }
            else {
                return null;
            }
        } finally {
            c.close();
        }
    }

    /**
     * Overloaded method. Resolves the account unique ID from the row ID and makes a call to {@link #getAccountType(String)}
     * @param accountId Database row ID of the account
     * @return {@link AccountType} of the account
     */
    public AccountType getAccountType(long accountId){
        return getAccountType(getUID(accountId));
    }

    /**
     * Returns a list of all account entries in the system (includes root account)
     * No transactions are loaded, just the accounts
     * @return List of {@link Account}s in the database
     */
    public List<Account> getSimpleAccountList(){
        LinkedList<Account> accounts = new LinkedList<>();
        Cursor c = fetchAccounts(null, null, AccountEntry.COLUMN_FULL_NAME + " ASC");

        try {
            while (c.moveToNext()) {
                accounts.add(buildSimpleAccountInstance(c));
            }
        }
        finally {
            c.close();
        }
        return accounts;
    }

    /**
     * Returns a list of all account entries in the system (includes root account)
     * No transactions are loaded, just the accounts
     * @return List of {@link Account}s in the database
     */
    public List<Account> getSimpleAccountList(String where, String[] whereArgs, String orderBy){
        LinkedList<Account> accounts = new LinkedList<>();
        Cursor c = fetchAccounts(where, whereArgs, orderBy);
        try {
            while (c.moveToNext()) {
                accounts.add(buildSimpleAccountInstance(c));
            }
        }
        finally {
            c.close();
        }
        return accounts;
    }
	/**
	 * Returns a list of accounts which have transactions that have not been exported yet
     * @param lastExportTimeStamp Timestamp after which to any transactions created/modified should be exported
	 * @return List of {@link Account}s with unexported transactions
	 */
    public List<Account> getExportableAccounts(Timestamp lastExportTimeStamp){
        LinkedList<Account> accountsList = new LinkedList<>();
        Cursor cursor = mDb.query(
                TransactionEntry.TABLE_NAME + " , " + SplitEntry.TABLE_NAME +
                        " ON " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID + " = " +
                        SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_UID + " , " +
                        AccountEntry.TABLE_NAME + " ON " + AccountEntry.TABLE_NAME + "." +
                        AccountEntry.COLUMN_UID + " = " + SplitEntry.TABLE_NAME + "." +
                        SplitEntry.COLUMN_ACCOUNT_UID,
                new String[]{AccountEntry.TABLE_NAME + ".*"},
                TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_MODIFIED_AT + " > ?",
                new String[]{TimestampHelper.getUtcStringFromTimestamp(lastExportTimeStamp)},
                AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_UID,
                null,
                null
        );
        try {
            while (cursor.moveToNext()) {
                accountsList.add(buildModelInstance(cursor));
            }
        }
        finally {
            cursor.close();
        }
        return accountsList;
	}

    /**
     * Retrieves the unique ID of the imbalance account for a particular currency (creates the imbalance account
     * on demand if necessary)
     * @param commodity Commodity for the imbalance account
     * @return String unique ID of the account
     */
    public String getOrCreateImbalanceAccountUID(Commodity commodity){
        String imbalanceAccountName = getImbalanceAccountName(commodity);
        String uid = findAccountUidByFullName(imbalanceAccountName);
        if (uid == null){
            Account account = new Account(imbalanceAccountName, commodity);
            account.setAccountType(AccountType.BANK);
            account.setParentUID(getOrCreateGnuCashRootAccountUID());
            account.setHidden(!GnuCashApplication.isDoubleEntryEnabled());
            account.setColor("#964B00");
            addRecord(account, UpdateMethod.insert);
            uid = account.getUID();
        }
        return uid;
    }

    /**
     * Returns the GUID of the imbalance account for the commodity
     *
     * <p>This method will not create the imbalance account if it doesn't exist</p>
     *
     * @param commodity Commodity for the imbalance account
     * @return GUID of the account or null if the account doesn't exist yet
     * @see #getOrCreateImbalanceAccountUID(Commodity)
     */
    public String getImbalanceAccountUID(Commodity commodity){
        String imbalanceAccountName = getImbalanceAccountName(commodity);
        return findAccountUidByFullName(imbalanceAccountName);
    }

    /**
     * Creates the account with the specified name and returns its unique identifier.
     * <p>If a full hierarchical account name is provided, then the whole hierarchy is created and the
     * unique ID of the last account (at bottom) of the hierarchy is returned</p>
     * @param fullName Fully qualified name of the account
     * @param accountType Type to assign to all accounts created
     * @return String unique ID of the account at bottom of hierarchy
     */
    public String createAccountHierarchy(String fullName, AccountType accountType) {
        if ("".equals(fullName)) {
            throw new IllegalArgumentException("fullName cannot be empty");
        }
        String[] tokens = fullName.trim().split(ACCOUNT_NAME_SEPARATOR);
        String uid = getOrCreateGnuCashRootAccountUID();
        String parentName = "";
        ArrayList<Account> accountsList = new ArrayList<>();
        for (String token : tokens) {
            parentName += token;
            String parentUID = findAccountUidByFullName(parentName);
            if (parentUID != null) { //the parent account exists, don't recreate
                uid = parentUID;
            } else {
                Account account = new Account(token);
                account.setAccountType(accountType);
                account.setParentUID(uid); //set its parent
                account.setFullName(parentName);
                accountsList.add(account);
                uid = account.getUID();
            }
            parentName += ACCOUNT_NAME_SEPARATOR;
        }
        if (accountsList.size() > 0) {
            bulkAddRecords(accountsList, UpdateMethod.insert);
        }
        // if fullName is not empty, loop will be entered and then uid will never be null
        //noinspection ConstantConditions
        return uid;
    }

    /**
     * Returns the unique ID of the opening balance account or creates one if necessary
     * @return String unique ID of the opening balance account
     */
    public String getOrCreateOpeningBalanceAccountUID() {
        String openingBalanceAccountName = getOpeningBalanceAccountFullName();
        String uid = findAccountUidByFullName(openingBalanceAccountName);
        if (uid == null) {
            uid = createAccountHierarchy(openingBalanceAccountName, AccountType.EQUITY);
        }
        return uid;
    }

    /**
     * Finds an account unique ID by its full name
     * @param fullName Fully qualified name of the account
     * @return String unique ID of the account or null if no match is found
     */
    public String findAccountUidByFullName(String fullName){
        Cursor c = mDb.query(AccountEntry.TABLE_NAME, new String[]{AccountEntry.COLUMN_UID},
                AccountEntry.COLUMN_FULL_NAME + "= ?", new String[]{fullName},
                null, null, null, "1");
        try {
            if (c.moveToNext()) {
                return c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_UID));
            } else {
                return null;
            }
        } finally {
            c.close();
        }
    }

	/**
	 * Returns a cursor to all account records in the database.
     * GnuCash ROOT accounts and hidden accounts will <b>not</b> be included in the result set
	 * @return {@link Cursor} to all account records
	 */
    @Override
	public Cursor fetchAllRecords(){
		Log.v(LOG_TAG, "Fetching all accounts from db");
        String selection =  AccountEntry.COLUMN_HIDDEN + " = 0 AND " + AccountEntry.COLUMN_TYPE + " != ?" ;
        return mDb.query(AccountEntry.TABLE_NAME,
                null,
                selection,
                new String[]{AccountType.ROOT.name()},
                null, null,
                AccountEntry.COLUMN_NAME + " ASC");
	}

    /**
     * Returns a cursor to all account records in the database ordered by full name.
     * GnuCash ROOT accounts and hidden accounts will not be included in the result set.
     * @return {@link Cursor} to all account records
     */
    public Cursor fetchAllRecordsOrderedByFullName() {

        Log.v(LOG_TAG,
              "Fetching all accounts from db");

        String selection = AccountEntry.COLUMN_HIDDEN + " = 0 AND " + AccountEntry.COLUMN_TYPE + " != ?";

        return mDb.query(AccountEntry.TABLE_NAME,
                         null,
                         selection,
                         new String[]{AccountType.ROOT.name()},
                         null,
                         null,
                         AccountEntry.COLUMN_FULL_NAME + " ASC");
    }

    /**
     * Returns a Cursor set of accounts which fulfill <code>where</code>
     * and ordered by <code>orderBy</code>
     * @param where SQL WHERE statement without the 'WHERE' itself
     * @param whereArgs args to where clause
     * @param orderBy orderBy clause
     * @return Cursor set of accounts which fulfill <code>where</code>
     */
    public Cursor fetchAccounts(@Nullable String where, @Nullable String[] whereArgs, @Nullable String orderBy){
        if (orderBy == null){
            orderBy = AccountEntry.COLUMN_NAME + " ASC";
        }
        Log.v(LOG_TAG, "Fetching all accounts from db where " + where + " order by " + orderBy);

        return mDb.query(AccountEntry.TABLE_NAME,
                         null,
                         where,
                         whereArgs,
                         null,
                         null,
                         orderBy);
    }
    
    /**
     * Returns a Cursor set of accounts which fulfill <code>where</code>
     * <p>This method returns the accounts list sorted by the full account name</p>
     * @param where SQL WHERE statement without the 'WHERE' itself
     * @param whereArgs where args
     * @return Cursor set of accounts which fulfill <code>where</code>
     */
    public Cursor fetchAccountsOrderedByFullName(String where, String[] whereArgs) {

        Log.v(LOG_TAG, "Fetching all accounts from db where " + where);

        return mDb.query(AccountEntry.TABLE_NAME,
                         null,
                         where,
                         whereArgs,
                         null,
                         null,
                         AccountEntry.COLUMN_FULL_NAME + " ASC");
    }

    /**
     * Returns a Cursor set of accounts which fulfill <code>where</code>
     * <p>This method returns the favorite accounts first, sorted by name, and then the other accounts,
     * sorted by name.</p>
     * @param where SQL WHERE statement without the 'WHERE' itself
     * @param whereArgs where args
     * @return Cursor set of accounts which fulfill <code>where</code>
     */
    public Cursor fetchAccountsOrderedByFavoriteAndFullName(String where,
                                                            String[] whereArgs) {

        Log.v(LOG_TAG,
              "Fetching all accounts from db where " + where + " order by Favorite then Name");

        return mDb.query(AccountEntry.TABLE_NAME,
                         null,
                         where,
                         whereArgs,
                         null,
                         null,
                         AccountEntry.COLUMN_FAVORITE + " DESC, " + AccountEntry.COLUMN_FULL_NAME + " ASC");
    }

    /**
     * Returns a Cursor set of all Accounts
     *
     * <p>This method returns the favorite accounts first, sorted by name, and then the other accounts,
     * sorted by name.</p>
     *
     * @return Cursor set of all accounts
     */
    public Cursor fetchAccountsOrderedByFavoriteAndFullName() {

        return fetchAccountsOrderedByFavoriteAndFullName(WHERE_NOT_HIDDEN_AND_NOT_ROOT_ACCOUNT,
                                                         new String[]{AccountType.ROOT.name()});
    }



    /**
     * Returns the balance of an account while taking sub-accounts into consideration
     * @return Account Balance of an account including sub-accounts
     */
    public Money getAccountBalance(String accountUID){
        return computeBalance(accountUID, -1, -1);
    }

    /**
     * Returns the balance of an account within the specified time range while taking sub-accounts into consideration
     * @param accountUID the account's UUID
     * @param startTimestamp the start timestamp of the time range
     * @param endTimestamp the end timestamp of the time range
     * @return the balance of an account within the specified range including sub-accounts
     */
    public Money getAccountBalance(String accountUID, long startTimestamp, long endTimestamp) {
        return computeBalance(accountUID, startTimestamp, endTimestamp);
    }

    /**
     * Compute the account balance for all accounts with the specified type within a specific duration
     * @param accountType Account Type for which to compute balance
     * @param startTimestamp Begin time for the duration in milliseconds
     * @param endTimestamp End time for duration in milliseconds
     * @return Account balance
     */
    public Money getAccountBalance(AccountType accountType, long startTimestamp, long endTimestamp){
        Cursor cursor = fetchAccounts(AccountEntry.COLUMN_TYPE + "= ?",
                new String[]{accountType.name()}, null);
        List<String> accountUidList = new ArrayList<>();
        while (cursor.moveToNext()){
            String accountUID = cursor.getString(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_UID));
            accountUidList.add(accountUID);
        }
        cursor.close();

        boolean hasDebitNormalBalance = accountType.hasDebitNormalBalance();
        String currencyCode = GnuCashApplication.getDefaultCurrencyCode();

        Log.d(LOG_TAG, "all account list : " + accountUidList.size());
        SplitsDbAdapter splitsDbAdapter = mTransactionsAdapter.getSplitDbAdapter();

        return (startTimestamp == -1 && endTimestamp == -1)
                ? splitsDbAdapter.computeSplitBalance(accountUidList, currencyCode, hasDebitNormalBalance)
                : splitsDbAdapter.computeSplitBalance(accountUidList, currencyCode, hasDebitNormalBalance, startTimestamp, endTimestamp);
    }

    /**
     * Returns the account balance for all accounts types specified
     * @param accountTypes List of account types
     * @param start Begin timestamp for transactions
     * @param end End timestamp of transactions
     * @return Money balance of the account types
     */
    public Money getAccountBalance(List<AccountType> accountTypes, long start, long end){
        Money balance = Money.createZeroInstance(GnuCashApplication.getDefaultCurrencyCode());
        for (AccountType accountType : accountTypes) {
            balance = balance.add(getAccountBalance(accountType, start, end));
        }
        return balance;
    }

    private Money computeBalance(String accountUID, long startTimestamp, long endTimestamp) {
        Log.d(LOG_TAG, "Computing account balance for account ID " + accountUID);
        String currencyCode = mTransactionsAdapter.getAccountCurrencyCode(accountUID);
        boolean hasDebitNormalBalance = getAccountType(accountUID).hasDebitNormalBalance();

        List<String> accountsList = getDescendantAccountUIDs(accountUID,
                null, null);

        accountsList.add(0, accountUID);

        Log.d(LOG_TAG, "all account list : " + accountsList.size());
        SplitsDbAdapter splitsDbAdapter = mTransactionsAdapter.getSplitDbAdapter();
        return (startTimestamp == -1 && endTimestamp == -1)
                ? splitsDbAdapter.computeSplitBalance(accountsList, currencyCode, hasDebitNormalBalance)
                : splitsDbAdapter.computeSplitBalance(accountsList, currencyCode, hasDebitNormalBalance, startTimestamp, endTimestamp);
        
    }

    /**
     * Returns the balance of account list within the specified time range. The default currency
     * takes as base currency.
     * @param accountUIDList list of account UIDs
     * @param startTimestamp the start timestamp of the time range
     * @param endTimestamp the end timestamp of the time range
     * @return Money balance of account list
     */
    public Money getAccountsBalance(@NonNull  List<String> accountUIDList, long startTimestamp, long endTimestamp) {
        String currencyCode = GnuCashApplication.getDefaultCurrencyCode();
        Money balance = Money.createZeroInstance(currencyCode);

        if (accountUIDList.isEmpty())
            return balance;

        boolean hasDebitNormalBalance = getAccountType(accountUIDList.get(0)).hasDebitNormalBalance();

        SplitsDbAdapter splitsDbAdapter = mTransactionsAdapter.getSplitDbAdapter();
        Money splitSum = (startTimestamp == -1 && endTimestamp == -1)
                ? splitsDbAdapter.computeSplitBalance(accountUIDList, currencyCode, hasDebitNormalBalance)
                : splitsDbAdapter.computeSplitBalance(accountUIDList, currencyCode, hasDebitNormalBalance, startTimestamp, endTimestamp);

        return balance.add(splitSum);
    }

    /**
     * Retrieve all descendant accounts of an account
     * Note, in filtering, once an account is filtered out, all its descendants
     * will also be filtered out, even they don't meet the filter where
     * @param accountUID The account to retrieve descendant accounts
     * @param where      Condition to filter accounts
     * @param whereArgs  Condition args to filter accounts
     * @return The descendant accounts list.
     */
    public List<String> getDescendantAccountUIDs(String accountUID, String where, String[] whereArgs) {
        // accountsList will hold accountUID with all descendant accounts.
        // accountsListLevel will hold descendant accounts of the same level
        ArrayList<String> accountsList = new ArrayList<>();
        ArrayList<String> accountsListLevel = new ArrayList<>();
        accountsListLevel.add(accountUID);
        for (;;) {
            Cursor cursor = mDb.query(AccountEntry.TABLE_NAME,
                    new String[]{AccountEntry.COLUMN_UID},
                    AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " IN ( '" + TextUtils.join("' , '", accountsListLevel) + "' )" +
                            (where == null ? "" : " AND " + where),
                    whereArgs, null, null, null);
            accountsListLevel.clear();
            if (cursor != null) {
                try {
                    int columnIndex = cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_UID);
                    while (cursor.moveToNext()) {
                        accountsListLevel.add(cursor.getString(columnIndex));
                    }
                } finally {
                    cursor.close();
                }
            }
            if (accountsListLevel.size() > 0) {
                accountsList.addAll(accountsListLevel);
            }
            else {
                break;
            }
        }
        return accountsList;
    }

    /**
     * Returns a cursor to the dataset containing sub-accounts of the account with record ID <code>accoundId</code>
     * @param accountUID GUID of the parent account
     * @return {@link Cursor} to the sub accounts data set
     */
    public Cursor fetchSubAccounts(String accountUID) {
        Log.v(LOG_TAG, "Fetching sub accounts for account id " + accountUID);
        String selection = AccountEntry.COLUMN_HIDDEN + " = 0 AND "
                + AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " = ?";
        return mDb.query(AccountEntry.TABLE_NAME,
                null,
                selection,
                new String[]{accountUID}, null, null, AccountEntry.COLUMN_NAME + " ASC");
    }

    /**
     * Returns the top level accounts i.e. accounts with no parent or with the GnuCash ROOT account as parent
     * @return Cursor to the top level accounts
     */
    public Cursor fetchTopLevelAccounts() {
        //condition which selects accounts with no parent, whose UID is not ROOT and whose type is not ROOT
        return fetchAccounts("("
                             + AccountEntry.COLUMN_PARENT_ACCOUNT_UID
                             + " IS NULL OR "
                             + AccountEntry.COLUMN_PARENT_ACCOUNT_UID
                             + " = ?) AND "
                             + AccountEntry.COLUMN_HIDDEN
                             + " = 0 AND "
                             + AccountEntry.COLUMN_TYPE
                             + " != ?",
                             new String[]{getOrCreateGnuCashRootAccountUID(),
                                          AccountType.ROOT.name()},
                             AccountEntry.COLUMN_NAME + " ASC");
    }

    /**
     * Returns a cursor to accounts which have recently had transactions added to them
     * @return Cursor to recently used accounts
     */
    public Cursor fetchRecentAccounts(int numberOfRecent) {
        return mDb.query(TransactionEntry.TABLE_NAME
                        + " LEFT OUTER JOIN " + SplitEntry.TABLE_NAME + " ON "
                        + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID + " = "
                        + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_UID
                        + " , " + AccountEntry.TABLE_NAME + " ON " + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_ACCOUNT_UID
                        + " = " + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_UID,
                new String[]{AccountEntry.TABLE_NAME + ".*"},
                AccountEntry.COLUMN_HIDDEN + " = 0",
                null,
                SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_ACCOUNT_UID, //groupby
                null, //haveing
                "MAX ( " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TIMESTAMP + " ) DESC", // order
                Integer.toString(numberOfRecent) // limit;
        );
    }

    /**
     * Fetches favorite accounts from the database
     * @return Cursor holding set of favorite accounts
     */
    public Cursor fetchFavoriteAccounts(){
        Log.v(LOG_TAG, "Fetching favorite accounts from db");
        String condition = AccountEntry.COLUMN_FAVORITE + " = 1";
        return mDb.query(AccountEntry.TABLE_NAME,
                null, condition, null, null, null,
                AccountEntry.COLUMN_NAME + " ASC");
    }

    /**
     * Returns the GnuCash ROOT account UID if one exists (or creates one if necessary).
     * <p>In GnuCash desktop account structure, there is a root account (which is not visible in the UI) from which
     * other top level accounts derive. GnuCash Android also enforces a ROOT account now</p>
     * @return Unique ID of the GnuCash root account.
     */
    public String getOrCreateGnuCashRootAccountUID() {
        Cursor cursor = fetchAccounts(AccountEntry.COLUMN_TYPE + "= ?",
                new String[]{AccountType.ROOT.name()}, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_UID));
            }
        } finally {
            cursor.close();
        }
        // No ROOT exits, create a new one
        Account rootAccount = new Account("ROOT Account", new CommoditiesDbAdapter(mDb).getCommodity("USD"));
        rootAccount.setAccountType(AccountType.ROOT);
        rootAccount.setFullName(ROOT_ACCOUNT_FULL_NAME);
        rootAccount.setHidden(true);
        rootAccount.setPlaceHolderFlag(true);
        ContentValues contentValues = new ContentValues();
        contentValues.put(AccountEntry.COLUMN_UID, rootAccount.getUID());
        contentValues.put(AccountEntry.COLUMN_NAME, rootAccount.getName());
        contentValues.put(AccountEntry.COLUMN_FULL_NAME, rootAccount.getFullName());
        contentValues.put(AccountEntry.COLUMN_TYPE, rootAccount.getAccountType().name());
        contentValues.put(AccountEntry.COLUMN_HIDDEN, rootAccount.isHidden() ? 1 : 0);
        String defaultCurrencyCode = GnuCashApplication.getDefaultCurrencyCode();
        contentValues.put(AccountEntry.COLUMN_CURRENCY, defaultCurrencyCode);
        contentValues.put(AccountEntry.COLUMN_COMMODITY_UID, getCommodityUID(defaultCurrencyCode));
        Log.i(LOG_TAG, "Creating ROOT account");
        mDb.insert(AccountEntry.TABLE_NAME, null, contentValues);
        return rootAccount.getUID();
    }

    /**
     * Returns the number of accounts for which the account with ID <code>accoundId</code> is a first level parent
     * @param accountUID String Unique ID (GUID) of the account
     * @return Number of sub accounts
     */
    public int getSubAccountCount(String accountUID){
        //TODO: at some point when API level 11 and above only is supported, use DatabaseUtils.queryNumEntries

        String queryCount = "SELECT COUNT(*) FROM " + AccountEntry.TABLE_NAME + " WHERE "
                + AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " = ?";
        Cursor cursor = mDb.rawQuery(queryCount, new String[]{accountUID});
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    /**
	 * Returns currency code of account with database ID <code>id</code>
	 * @param uid GUID of the account
	 * @return Currency code of the account
	 */
	public String getCurrencyCode(String uid){
		return getAccountCurrencyCode(uid);
	}

    /**
     * Returns the simple name of the account with unique ID <code>accountUID</code>.
     * @param accountUID Unique identifier of the account
     * @return Name of the account as String
     * @throws java.lang.IllegalArgumentException if accountUID does not exist
     * @see #getFullyQualifiedAccountName(String)
     */
    public String getAccountName(String accountUID){
        return getAttribute(accountUID, AccountEntry.COLUMN_NAME);
    }

    /**
     * Returns the default transfer account record ID for the account with UID <code>accountUID</code>
     * @param accountID Database ID of the account record
     * @return Record ID of default transfer account
     */
    public long getDefaultTransferAccountID(long accountID){
        Cursor cursor = mDb.query(AccountEntry.TABLE_NAME,
                new String[]{AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID},
                AccountEntry._ID + " = " + accountID,
                null, null, null, null);
       try {
            if (cursor.moveToNext()) {
                String uid = cursor.getString(
                        cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID));
                if (uid == null)
                    return 0;
                else
                    return getID(uid);
            } else {
                return 0;
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Returns the full account name including the account hierarchy (parent accounts)
     * @param accountUID Unique ID of account
     * @return Fully qualified (with parent hierarchy) account name
     */
    public String getFullyQualifiedAccountName(String accountUID){
        String accountName = getAccountName(accountUID);
        String parentAccountUID = getParentAccountUID(accountUID);

        if (parentAccountUID == null || parentAccountUID.equalsIgnoreCase(getOrCreateGnuCashRootAccountUID())){
            return accountName;
        }

        String parentAccountName = getFullyQualifiedAccountName(parentAccountUID);

        return parentAccountName + ACCOUNT_NAME_SEPARATOR + accountName;
    }

    /**
     * get account's full name directly from DB
     * @param accountUID the account to retrieve full name
     * @return full name registered in DB
     */
    public String getAccountFullName(String accountUID) {
        Cursor cursor = mDb.query(AccountEntry.TABLE_NAME, new String[]{AccountEntry.COLUMN_FULL_NAME},
                AccountEntry.COLUMN_UID + " = ?", new String[]{accountUID},
                null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_FULL_NAME));
            }
        }
        finally {
            cursor.close();
        }
        throw new IllegalArgumentException("account UID: " + accountUID + " does not exist");
    }


    /**
     * Returns <code>true</code> if the account with unique ID <code>accountUID</code> is a placeholder account.
     * @param accountUID Unique identifier of the account
     * @return <code>true</code> if the account is a placeholder account, <code>false</code> otherwise
     */
    public boolean isPlaceholderAccount(String accountUID) {
        String isPlaceholder = getAttribute(accountUID, AccountEntry.COLUMN_PLACEHOLDER);
        return Integer.parseInt(isPlaceholder) == 1;
    }

    /**
     * Convenience method, resolves the account unique ID and calls {@link #isPlaceholderAccount(String)}
     * @param accountUID GUID of the account
     * @return <code>true</code> if the account is hidden, <code>false</code> otherwise
     */
    public boolean isHiddenAccount(String accountUID){
        String isHidden = getAttribute(accountUID, AccountEntry.COLUMN_HIDDEN);
        return Integer.parseInt(isHidden) == 1;
    }

    /**
     * Returns true if the account is a favorite account, false otherwise
     * @param accountUID GUID of the account
     * @return <code>true</code> if the account is a favorite account, <code>false</code> otherwise
     */
    public boolean isFavoriteAccount(String accountUID){
        String isFavorite = getAttribute(accountUID, AccountEntry.COLUMN_FAVORITE);
        return Integer.parseInt(isFavorite) == 1;
    }

    /**
     * Updates all opening balances to the current account balances
     */
    public List<Transaction> getAllOpeningBalanceTransactions(){
        Cursor cursor = fetchAccounts(null, null, null);
        List<Transaction> openingTransactions = new ArrayList<>();
        try {
            SplitsDbAdapter splitsDbAdapter = mTransactionsAdapter.getSplitDbAdapter();
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(AccountEntry._ID));
                String accountUID = getUID(id);
                String currencyCode = getCurrencyCode(accountUID);
                ArrayList<String> accountList = new ArrayList<>();
                accountList.add(accountUID);
                Money balance = splitsDbAdapter.computeSplitBalance(accountList,
                        currencyCode, getAccountType(accountUID).hasDebitNormalBalance());
                if (balance.asBigDecimal().compareTo(new BigDecimal(0)) == 0)
                    continue;

                Transaction transaction = new Transaction(GnuCashApplication.getAppContext().getString(R.string.account_name_opening_balances));
                transaction.setNote(getAccountName(accountUID));
                transaction.setCommodity(Commodity.getInstance(currencyCode));
                TransactionType transactionType = Transaction.getTypeForBalance(getAccountType(accountUID),
                        balance.isNegative());
                Split split = new Split(balance, accountUID);
                split.setType(transactionType);
                transaction.addSplit(split);
                transaction.addSplit(split.createPair(getOrCreateOpeningBalanceAccountUID()));
                transaction.setExported(true);
                openingTransactions.add(transaction);
            }
        } finally {
            cursor.close();
        }
        return openingTransactions;
    }

    public static String getImbalanceAccountPrefix() {
         return GnuCashApplication.getAppContext().getString(R.string.imbalance_account_name) + "-";
    }

    /**
     * Returns the imbalance account where to store transactions which are not double entry.
     *
     * @param commodity Commodity of the transaction
     * @return Imbalance account name
     */
    public static String getImbalanceAccountName(Commodity commodity){
        return getImbalanceAccountPrefix() + commodity.getCurrencyCode();
    }

    /**
     * Get the name of the default account for opening balances for the current locale.
     * For the English locale, it will be "Equity:Opening Balances"
     * @return Fully qualified account name of the opening balances account
     */
    public static String getOpeningBalanceAccountFullName(){
        Context context = GnuCashApplication.getAppContext();
        String parentEquity = context.getString(R.string.account_name_equity).trim();
        //German locale has no parent Equity account
        if (parentEquity.length() > 0) {
            return parentEquity + ACCOUNT_NAME_SEPARATOR
                    + context.getString(R.string.account_name_opening_balances);
        } else
            return context.getString(R.string.account_name_opening_balances);
    }

    /**
     * Returns the account color for the active account as an Android resource ID.
     * <p>
     * Basically, if we are in a top level account, use the default title color.
     * but propagate a parent account's title color to children who don't have own color
     * </p>
     * @param accountUID GUID of the account
     * @return Android resource ID representing the color which can be directly set to a view
     */
    public static int getActiveAccountColorResource(@NonNull String accountUID) {

        AccountsDbAdapter accountsDbAdapter = getInstance();

        String colorCode        = null;
        int    iColor           = -1;

        String parentAccountUID = accountUID;
        while (parentAccountUID != null) {

            colorCode = accountsDbAdapter.getAccountColorCode(accountsDbAdapter.getID(parentAccountUID));

            if (colorCode != null) {
                iColor = Color.parseColor(colorCode);
                break;
            }

            // Climb to parent account
            parentAccountUID = accountsDbAdapter.getParentAccountUID(parentAccountUID);
        }

        if (colorCode == null) {
            // No color has been found defined in any ancestor

            // Use black color
            iColor = GnuCashApplication.getAppContext()
                                       .getResources()
                                       .getColor(R.color.bpblack);
        }

        return iColor;
    }

    /**
     * Returns the list of commodities in use in the database.
     *
     * <p>This is not the same as the list of all available commodities.</p>
     *
     * @return List of commodities in use
     */
    public List<Commodity> getCommoditiesInUse() {
        Cursor cursor = mDb.query(true, AccountEntry.TABLE_NAME, new String[]{AccountEntry.COLUMN_CURRENCY},
                null, null, null, null, null, null);
        List<Commodity> commodityList = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                String currencyCode =
                    cursor.getString(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_CURRENCY));
                commodityList.add(mCommoditiesDbAdapter.getCommodity(currencyCode));
            }
        } finally {
            cursor.close();
        }
        return commodityList;
    }
    /**
	 * Deletes all accounts, transactions (and their splits) from the database.
     * Basically empties all 3 tables, so use with care ;)
	 */
    @Override
	public int deleteAllRecords() {
        // Relies "ON DELETE CASCADE" takes too much time
        // It take more than 300s to complete the deletion on my dataset without
        // clearing the split table first, but only needs a little more that 1s
        // if the split table is cleared first.
        mDb.delete(DatabaseSchema.PriceEntry.TABLE_NAME, null, null);
        mDb.delete(SplitEntry.TABLE_NAME, null, null);
        mDb.delete(TransactionEntry.TABLE_NAME, null, null);
        mDb.delete(DatabaseSchema.ScheduledActionEntry.TABLE_NAME, null, null);
        mDb.delete(DatabaseSchema.BudgetAmountEntry.TABLE_NAME, null, null);
        mDb.delete(DatabaseSchema.BudgetEntry.TABLE_NAME, null, null);
        mDb.delete(DatabaseSchema.RecurrenceEntry.TABLE_NAME, null, null);

        return mDb.delete(AccountEntry.TABLE_NAME, null, null);
    }

    @Override
    public boolean deleteRecord(@NonNull String uid) {
        boolean result = super.deleteRecord(uid);
        if (result){
            ContentValues contentValues = new ContentValues();
            contentValues.putNull(AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID);
            mDb.update(mTableName, contentValues,
                    AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID + "=?",
                    new String[]{uid});
        }
        return result;
    }

    public int getTransactionMaxSplitNum(@NonNull String accountUID) {
        Cursor cursor = mDb.query("trans_extra_info",
                new String[]{"MAX(trans_split_count)"},
                "trans_acct_t_uid IN ( SELECT DISTINCT " + TransactionEntry.TABLE_NAME + "_" + TransactionEntry.COLUMN_UID +
                        " FROM trans_split_acct WHERE " + AccountEntry.TABLE_NAME + "_" + AccountEntry.COLUMN_UID +
                        " = ? )",
                new String[]{accountUID},
                null,
                null,
                null
                );
        try {
            if (cursor.moveToFirst()) {
                return (int)cursor.getLong(0);
            } else {
                return 0;
            }
        }
        finally {
            cursor.close();
        }
    }
}
