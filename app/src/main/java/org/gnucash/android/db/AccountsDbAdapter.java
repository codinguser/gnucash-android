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

package org.gnucash.android.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
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
public class AccountsDbAdapter extends DatabaseAdapter {
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
	 * Transactions database adapter for manipulating transactions associated with accounts
	 */
    private final TransactionsDbAdapter mTransactionsAdapter;

//    private static String mImbalanceAccountPrefix = GnuCashApplication.getAppContext().getString(R.string.imbalance_account_name) + "-";

    /**
     * Overloaded constructor. Creates an adapter for an already open database
     * @param db SQliteDatabase instance
     */
    public AccountsDbAdapter(SQLiteDatabase db, TransactionsDbAdapter transactionsDbAdapter) {
        super(db, AccountEntry.TABLE_NAME);
        mTransactionsAdapter = transactionsDbAdapter;
        LOG_TAG = "AccountsDbAdapter";
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
	 * If an account already exists in the database with the same unique ID, 
	 * then just update that account. 
	 * @param account {@link Account} to be inserted to database
	 * @return Database row ID of the inserted account
	 */
	public long addAccount(Account account){
		ContentValues contentValues = getContentValues(account);
		contentValues.put(AccountEntry.COLUMN_NAME,         account.getName());
		contentValues.put(AccountEntry.COLUMN_TYPE,         account.getAccountType().name());
		contentValues.put(AccountEntry.COLUMN_CURRENCY,     account.getCurrency().getCurrencyCode());
        contentValues.put(AccountEntry.COLUMN_PLACEHOLDER,  account.isPlaceholderAccount() ? 1 : 0);
        contentValues.put(AccountEntry.COLUMN_HIDDEN,       account.isHidden() ? 1 : 0);
        if (account.getColorHexCode() != null) {
            contentValues.put(AccountEntry.COLUMN_COLOR_CODE, account.getColorHexCode());
        } else {
            contentValues.putNull(AccountEntry.COLUMN_COLOR_CODE);
        }
        contentValues.put(AccountEntry.COLUMN_FAVORITE,     account.isFavorite() ? 1 : 0);
        contentValues.put(AccountEntry.COLUMN_FULL_NAME,    account.getFullName());
        String parentAccountUID = account.getParentUID();
        if (parentAccountUID == null && account.getAccountType() != AccountType.ROOT) {
            parentAccountUID = getOrCreateGnuCashRootAccountUID();
        }
        contentValues.put(AccountEntry.COLUMN_PARENT_ACCOUNT_UID, parentAccountUID);

        if (account.getDefaultTransferAccountUID() != null) {
            contentValues.put(AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID, account.getDefaultTransferAccountUID());
        } else {
            contentValues.putNull(AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID);
        }

        Log.d(LOG_TAG, "Replace account to db");
        long rowId =  mDb.replace(AccountEntry.TABLE_NAME, null, contentValues);

		//now add transactions if there are any
		if (rowId > 0 && account.getAccountType() != AccountType.ROOT){
            //update the fully qualified account name
            updateAccount(rowId, AccountEntry.COLUMN_FULL_NAME, getFullyQualifiedAccountName(rowId));
			for (Transaction t : account.getTransactions()) {
		        mTransactionsAdapter.addTransaction(t);
			}
		}
		return rowId;
	}

    /**
     * Adds some accounts to the database.
     * If an account already exists in the database with the same unique ID,
     * then just update that account. This function will NOT try to determine the full name
     * of the accounts inserted, full names should be generated prior to the insert.
     * All or none of the accounts will be inserted;
     * @param accountList {@link Account} to be inserted to database
     * @return number of rows inserted
     */
    public long bulkAddAccounts(List<Account> accountList){
        long nRow = 0;
        try {
            mDb.beginTransaction();
            SQLiteStatement replaceStatement = mDb.compileStatement("REPLACE INTO " + AccountEntry.TABLE_NAME + " ( "
                    + AccountEntry.COLUMN_UID 	            + " , "
                    + AccountEntry.COLUMN_NAME 	            + " , "
                    + AccountEntry.COLUMN_TYPE              + " , "
                    + AccountEntry.COLUMN_CURRENCY          + " , "
                    + AccountEntry.COLUMN_COLOR_CODE        + " , "
                    + AccountEntry.COLUMN_FAVORITE 		    + " , "
                    + AccountEntry.COLUMN_FULL_NAME 	    + " , "
                    + AccountEntry.COLUMN_PLACEHOLDER       + " , "
                    + AccountEntry.COLUMN_CREATED_AT        + " , "
                    + AccountEntry.COLUMN_HIDDEN            + " , "
                    + AccountEntry.COLUMN_PARENT_ACCOUNT_UID    + " , "
                    + AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID   + " ) VALUES ( ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? )");
            for (Account account:accountList) {
                replaceStatement.clearBindings();
                replaceStatement.bindString(1, account.getUID());
                replaceStatement.bindString(2, account.getName());
                replaceStatement.bindString(3, account.getAccountType().name());
                replaceStatement.bindString(4, account.getCurrency().getCurrencyCode());
                if (account.getColorHexCode() != null) {
                    replaceStatement.bindString(5, account.getColorHexCode());
                }
                replaceStatement.bindLong(6,    account.isFavorite() ? 1 : 0);
                replaceStatement.bindString(7,  account.getFullName());
                replaceStatement.bindLong(8,    account.isPlaceholderAccount() ? 1 : 0);
                replaceStatement.bindString(9,  account.getCreatedTimestamp().toString());
                replaceStatement.bindLong(10, account.isHidden() ? 1 : 0);
                if (account.getParentUID() != null) {
                    replaceStatement.bindString(11, account.getParentUID());
                }
                if (account.getDefaultTransferAccountUID() != null) {
                    replaceStatement.bindString(12, account.getDefaultTransferAccountUID());
                }
                //Log.d(LOG_TAG, "Replacing account in db");
                replaceStatement.execute();
                nRow ++;
            }
            mDb.setTransactionSuccessful();
        }
        finally {
            mDb.endTransaction();
        }
        return nRow;
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
                new String[] {accountUID}
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
            if (newParentAccountUID == null || getAccountType(newParentAccountUID) == AccountType.ROOT) {
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
            mDb.delete(
                    AccountEntry.TABLE_NAME,
                    AccountEntry.COLUMN_UID + " IN (" + accountUIDList + ")",
                    null
            );
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
    public Account buildAccountInstance(Cursor c){
        Account account = buildSimpleAccountInstance(c);
        account.setTransactions(mTransactionsAdapter.getAllTransactionsForAccount(account.getUID()));

        return account;
	}

    /**
     * Builds an account instance with the provided cursor and loads its corresponding transactions.
     * <p>The method will not move the cursor position, so the cursor should already be pointing
     * to the account record in the database<br/>
     * <b>Note</b> Unlike {@link  #buildAccountInstance(android.database.Cursor)} this method will not load transactions</p>
     *
     * @param c Cursor pointing to account record in database
     * @return {@link Account} object constructed from database record
     */
    private Account buildSimpleAccountInstance(Cursor c) {
        Account account = new Account(c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_NAME)));
        populateModel(c, account);

        account.setParentUID(c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_PARENT_ACCOUNT_UID)));
        account.setAccountType(AccountType.valueOf(c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_TYPE))));
        Currency currency = Currency.getInstance(c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_CURRENCY)));
        account.setCurrency(currency);
        account.setPlaceHolderFlag(c.getInt(c.getColumnIndexOrThrow(AccountEntry.COLUMN_PLACEHOLDER)) == 1);
        account.setDefaultTransferAccountUID(c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID)));
        account.setColorCode(c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_COLOR_CODE)));
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
    public String getParentAccountUID(String uid){
		Cursor cursor = mDb.query(AccountEntry.TABLE_NAME,
				new String[] {AccountEntry._ID, AccountEntry.COLUMN_PARENT_ACCOUNT_UID},
                AccountEntry.COLUMN_UID + " = ?",
                new String[]{uid},
                null, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                Log.d(LOG_TAG, "Account already exists. Returning existing id");
                return cursor.getString(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_PARENT_ACCOUNT_UID));
            } else {
                return null;
            }
        } finally {
            cursor.close();
        }
	}

    /**
     * Returns the  unique ID of the parent account of the account with database ID <code>id</code>
     * If the account has no parent, null is returned.
     * @param id DB record ID of account . Should not be null
     * @return DB record UID of the parent account, null if the account has no parent
     * @see #getParentAccountUID(String)
     */
    public String getParentAccountUID(long id){
        return getParentAccountUID(getUID(id));
    }

	/**
	 * Retrieves an account object from a database with database ID <code>rowId</code>
	 * @param rowId Identifier of the account record to be retrieved
	 * @return {@link Account} object corresponding to database record
	 */
    public Account getAccount(long rowId){
		Log.v(LOG_TAG, "Fetching account with id " + rowId);
		Cursor c =	fetchRecord(rowId);
		try {
            if (c.moveToFirst()) {
                return buildAccountInstance(c);
            } else {
                throw new IllegalArgumentException(String.format("rowId %d does not exist", rowId));
            }
        } finally {
            c.close();
        }
	}
		
	/**
	 * Returns the {@link Account} object populated with data from the database
	 * for the record with UID <code>uid</code>
	 * @param uid Unique ID of the account to be retrieved
	 * @return {@link Account} object for unique ID <code>uid</code>
	 */
    public Account getAccount(String uid){
		return getAccount(getID(uid));
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
	 * Returns the name of the account with id <code>accountID</code>
	 * @param accountID Database ID of the account record
	 * @return Name of the account 
	 */
    public String getName(long accountID) {
		Cursor c = fetchRecord(accountID);
        try {
            if (c.moveToFirst()) {
                return c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_NAME));
            } else {
                throw new IllegalArgumentException("account " + accountID + " does not exist");
            }
        } finally {
            c.close();
        }
	}
	
	/**
	 * Returns a list of all account objects in the system
	 * @return List of {@link Account}s in the database
	 */
    public List<Account> getAllAccounts(){
		LinkedList<Account> accounts = new LinkedList<Account>();
		Cursor c = fetchAllRecords();
        try {
            while (c.moveToNext()) {
                accounts.add(buildAccountInstance(c));
            }
        } finally {
            c.close();
        }
		return accounts;
	}

    /**
     * Returns a list of all account entries in the system (includes root account)
     * No transactions are loaded, just the accounts
     * @return List of {@link Account}s in the database
     */
    public List<Account> getSimpleAccountList(){
        LinkedList<Account> accounts = new LinkedList<Account>();
        Cursor c = fetchAccounts(null, null, null);

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
        LinkedList<Account> accounts = new LinkedList<Account>();
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
	 * @return List of {@link Account}s with unexported transactions
	 */
    public List<Account> getExportableAccounts(){
        LinkedList<Account> accountsList = new LinkedList<Account>();
        Cursor cursor = mDb.query(
                TransactionEntry.TABLE_NAME + " , " + SplitEntry.TABLE_NAME +
                        " ON " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID + " = " +
                        SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_UID + " , " +
                        AccountEntry.TABLE_NAME + " ON " + AccountEntry.TABLE_NAME + "." +
                        AccountEntry.COLUMN_UID + " = " + SplitEntry.TABLE_NAME + "." +
                        SplitEntry.COLUMN_ACCOUNT_UID,
                new String[]{AccountEntry.TABLE_NAME + ".*"},
                TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_EXPORTED + " == 0",
                null,
                AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_UID,
                null,
                null
        );
        try {
            while (cursor.moveToNext()) {
                accountsList.add(buildAccountInstance(cursor));
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
     * @param currency Currency for the imbalance account
     * @return String unique ID of the account
     */
    public String getOrCreateImbalanceAccountUID(Currency currency){
        String imbalanceAccountName = getImbalanceAccountName(currency);
        String uid = findAccountUidByFullName(imbalanceAccountName);
        if (uid == null){
            Account account = new Account(imbalanceAccountName, currency);
            account.setAccountType(AccountType.BANK);
            account.setParentUID(getOrCreateGnuCashRootAccountUID());
            account.setHidden(!GnuCashApplication.isDoubleEntryEnabled());
            addAccount(account);
            uid = account.getUID();
        }
        return uid;
    }

    /**
     * Returns the GUID of the imbalance account for the currency
     * @param currency Currency for the imbalance account
     * @return GUID of the account or null if the account doesn't exist yet
     * @see #getOrCreateImbalanceAccountUID(java.util.Currency)
     */
    public String getImbalanceAccountUID(Currency currency){
        String imbalanceAccountName = getImbalanceAccountName(currency);
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
        ArrayList<Account> accountsList = new ArrayList<Account>();
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
            bulkAddAccounts(accountsList);
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
    public Cursor fetchAllRecordsOrderedByFullName(){
        Log.v(LOG_TAG, "Fetching all accounts from db");
        String selection =  AccountEntry.COLUMN_HIDDEN + " = 0 AND " + AccountEntry.COLUMN_TYPE + " != ?" ;
        return mDb.query(AccountEntry.TABLE_NAME,
                null,
                selection,
                new String[]{AccountType.ROOT.name()},
                null, null,
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
                null, where, whereArgs, null, null,
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
                null, where, whereArgs, null, null,
                AccountEntry.COLUMN_FULL_NAME + " ASC");
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

    private Money computeBalance(String accountUID, long startTimestamp, long endTimestamp) {
        Log.d(LOG_TAG, "Computing account balance for account ID " + accountUID);
        String currencyCode = mTransactionsAdapter.getAccountCurrencyCode(accountUID);
        boolean hasDebitNormalBalance = getAccountType(accountUID).hasDebitNormalBalance();
        Money balance = Money.createZeroInstance(currencyCode);

        List<String> accountsList = getDescendantAccountUIDs(accountUID,
                AccountEntry.COLUMN_CURRENCY + " = ? ",
                new String[]{currencyCode});

        accountsList.add(0, accountUID);

        Log.d(LOG_TAG, "all account list : " + accountsList.size());
		SplitsDbAdapter splitsDbAdapter = SplitsDbAdapter.getInstance();
        Money splitSum = (startTimestamp == -1 && endTimestamp == -1)
                ? splitsDbAdapter.computeSplitBalance(accountsList, currencyCode, hasDebitNormalBalance)
                : splitsDbAdapter.computeSplitBalance(accountsList, currencyCode, hasDebitNormalBalance, startTimestamp, endTimestamp);
        
        return balance.add(splitSum);
    }

    /**
     * Returns the absolute balance of account list within the specified time range while taking sub-accounts
     * into consideration. The default currency takes as base currency.
     * @param accountUIDList list of account UIDs
     * @param startTimestamp the start timestamp of the time range
     * @param endTimestamp the end timestamp of the time range
     * @return the absolute balance of account list
     */
    public Money getAccountsBalance(List<String> accountUIDList, long startTimestamp, long endTimestamp) {
        String currencyCode = GnuCashApplication.getDefaultCurrency();
        Money balance = Money.createZeroInstance(currencyCode);

        SplitsDbAdapter splitsDbAdapter = SplitsDbAdapter.getInstance();
        Money splitSum = (startTimestamp == -1 && endTimestamp == -1)
                ? splitsDbAdapter.computeSplitBalance(accountUIDList, currencyCode, true)
                : splitsDbAdapter.computeSplitBalance(accountUIDList, currencyCode, true, startTimestamp, endTimestamp);

        return balance.add(splitSum).absolute();
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
        ArrayList<String> accountsList = new ArrayList<String>();
        ArrayList<String> accountsListLevel = new ArrayList<String>();
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
        return fetchAccounts("(" + AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " IS NULL OR "
                        + AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " = ?) AND "
                        + AccountEntry.COLUMN_HIDDEN + " = 0 AND "
                        + AccountEntry.COLUMN_TYPE + " != ?",
                new String[]{getOrCreateGnuCashRootAccountUID(), AccountType.ROOT.name()},
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
        Account rootAccount = new Account("ROOT Account");
        rootAccount.setAccountType(AccountType.ROOT);
        rootAccount.setFullName(ROOT_ACCOUNT_FULL_NAME);
        rootAccount.setHidden(true);
        addAccount(rootAccount);
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
     * Returns the number of accounts in the database
     * @return Number of accounts in the database
     */
    public int getTotalAccountCount() {
        String queryCount = "SELECT COUNT(*) FROM " + AccountEntry.TABLE_NAME;
        Cursor cursor = mDb.rawQuery(queryCount, null);
        try {
            cursor.moveToFirst();
            return cursor.getInt(0);
        } finally {
            cursor.close();
        }
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
        Cursor cursor = mDb.query(AccountEntry.TABLE_NAME,
                new String[]{AccountEntry._ID, AccountEntry.COLUMN_NAME},
                AccountEntry.COLUMN_UID + " = ?",
                new String[]{accountUID}, null, null, null);
        try {
            if (cursor.moveToNext()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_NAME));
            } else {
                throw new IllegalArgumentException("account " + accountUID + " does not exist");
            }
        } finally {
            cursor.close();
        }
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
     * Overloaded convenience method.
     * Simply resolves the account UID and calls {@link #getFullyQualifiedAccountName(String)}
     * @param accountId Database record ID of account
     * @return Fully qualified (with parent hierarchy) account name
     */
    public String getFullyQualifiedAccountName(long accountId){
        return getFullyQualifiedAccountName(getUID(accountId));
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
        List<Transaction> openingTransactions = new ArrayList<Transaction>();
        try {
            SplitsDbAdapter splitsDbAdapter = mTransactionsAdapter.getSplitDbAdapter();
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(AccountEntry._ID));
                String accountUID = getUID(id);
                String currencyCode = getCurrencyCode(accountUID);
                ArrayList<String> accountList = new ArrayList<String>();
                accountList.add(accountUID);
                Money balance = splitsDbAdapter.computeSplitBalance(accountList,
                        currencyCode, getAccountType(accountUID).hasDebitNormalBalance());
                if (balance.asBigDecimal().compareTo(new BigDecimal(0)) == 0)
                    continue;

                Transaction transaction = new Transaction(GnuCashApplication.getAppContext().getString(R.string.account_name_opening_balances));
                transaction.setNote(getName(id));
                transaction.setCurrencyCode(currencyCode);
                TransactionType transactionType = Transaction.getTypeForBalance(getAccountType(accountUID),
                        balance.isNegative());
                Split split = new Split(balance.absolute(), accountUID);
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
     * Returns the imbalance account where to store transactions which are not double entry
     * @param currency Currency of the transaction
     * @return Imbalance account name
     */
    public static String getImbalanceAccountName(Currency currency){
        return getImbalanceAccountPrefix() + currency.getCurrencyCode();
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
     * Returns the list of currencies in the database
     * @return List of currencies in the database
     */
    public List<Currency> getCurrencies(){
        Cursor cursor = mDb.query(true, AccountEntry.TABLE_NAME, new String[]{AccountEntry.COLUMN_CURRENCY},
                null, null, null, null, null, null);
        List<Currency> currencyList = new ArrayList<Currency>();
        try {
            while (cursor.moveToNext()) {
                String currencyCode = cursor.getString(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_CURRENCY));
                currencyList.add(Currency.getInstance(currencyCode));
            }
        } finally {
            cursor.close();
        }
        return currencyList;
    }

    /**
	 * Deletes all accounts, transactions (and their splits) from the database.
     * Basically empties all 3 tables, so use with care ;)
	 */
    @Override
	public int deleteAllRecords(){
		mDb.delete(TransactionEntry.TABLE_NAME, null, null); //this will take the splits along with it
        mDb.delete(DatabaseSchema.ScheduledActionEntry.TABLE_NAME, null, null);
        return mDb.delete(AccountEntry.TABLE_NAME, null, null);
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
