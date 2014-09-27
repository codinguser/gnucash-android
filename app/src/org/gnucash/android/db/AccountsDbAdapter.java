/*
 * Copyright (c) 2012 - 2014 Ngewi Fet <ngewif@gmail.com>
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
import android.text.TextUtils;

import android.util.Log;
import android.support.annotation.NonNull;
import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.model.*;

import java.math.BigDecimal;
import java.util.*;

import static org.gnucash.android.db.DatabaseSchema.*;

/**
 * Manages persistence of {@link Account}s in the database
 * Handles adding, modifying and deleting of account records.
 * @author Ngewi Fet <ngewif@gmail.com>
 * @author Yongxin Wang <fefe.wyx@gmail.com>
 */
public class AccountsDbAdapter extends DatabaseAdapter {
    /**
     * Separator used for account name hierarchies between parent and child accounts
     */
    public static final String ACCOUNT_NAME_SEPARATOR = ":";

	/**
	 * Transactions database adapter for manipulating transactions associated with accounts
	 */
	private final TransactionsDbAdapter mTransactionsAdapter;

    /**
     * Overloaded constructor. Creates an adapter for an already open database
     * @param db SQliteDatabase instance
     */
    public AccountsDbAdapter(@NonNull SQLiteDatabase db, @NonNull TransactionsDbAdapter transactionsDbAdapter) {
        super(db);
        mTransactionsAdapter = transactionsDbAdapter;
    }

    /**
	 * Adds an account to the database. 
	 * If an account already exists in the database with the same unique ID, 
	 * then just update that account. 
	 * @param account {@link Account} to be inserted to database
	 * @return Database row ID of the inserted account
	 */
	public long addAccount(Account account){
		ContentValues contentValues = new ContentValues();
		contentValues.put(AccountEntry.COLUMN_NAME,         account.getName());
		contentValues.put(AccountEntry.COLUMN_TYPE,         account.getAccountType().name());
		contentValues.put(AccountEntry.COLUMN_UID,          account.getUID());
		contentValues.put(AccountEntry.COLUMN_CURRENCY,     account.getCurrency().getCurrencyCode());
        contentValues.put(AccountEntry.COLUMN_PLACEHOLDER,  account.isPlaceholderAccount() ? 1 : 0);
        contentValues.put(AccountEntry.COLUMN_COLOR_CODE,   account.getColorHexCode());
        contentValues.put(AccountEntry.COLUMN_FAVORITE,     account.isFavorite() ? 1 : 0);
        contentValues.put(AccountEntry.COLUMN_FULL_NAME,    account.getFullName());
        contentValues.put(AccountEntry.COLUMN_PARENT_ACCOUNT_UID,           account.getParentUID());
        contentValues.put(AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID, account.getDefaultTransferAccountUID());

        Log.d(TAG, "Replace account to db");
        long rowId =  mDb.replace(AccountEntry.TABLE_NAME, null, contentValues);

		//now add transactions if there are any
		if (rowId > 0){
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
                    + AccountEntry.COLUMN_PLACEHOLDER           + " , "
                    + AccountEntry.COLUMN_PARENT_ACCOUNT_UID    + " , "
                    + AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID   + " ) VALUES ( ? , ? , ? , ? , ? , ? , ? , ? , ? , ? )");
            for (Account account:accountList) {
                replaceStatement.clearBindings();
                replaceStatement.bindString(1, account.getUID());
                replaceStatement.bindString(2, account.getName());
                replaceStatement.bindString(3, account.getAccountType().name());
                replaceStatement.bindString(4, account.getCurrency().getCurrencyCode());
                if (account.getColorHexCode() != null) {
                    replaceStatement.bindString(5, account.getColorHexCode());
                }
                replaceStatement.bindLong(6, account.isFavorite() ? 1 : 0);
                replaceStatement.bindString(7, account.getFullName());
                replaceStatement.bindLong(8, account.isPlaceholderAccount() ? 1 : 0);
                if (account.getParentUID() != null) {
                    replaceStatement.bindString(9, account.getParentUID());
                }
                if (account.getDefaultTransferAccountUID() != null) {
                    replaceStatement.bindString(10, account.getDefaultTransferAccountUID());
                }
                //Log.d(TAG, "Replacing account in db");
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
        contentValues.put(columnKey, newValue);

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
	 * Deletes an account with database id <code>rowId</code>
	 * All the transactions in the account will also be deleted
     * All descendant account will be assigned to the account's parent
	 * @param rowId Database id of the account record to be deleted
	 * @return <code>true</code> if deletion was successful, <code>false</code> otherwise.
	 */
	public boolean destructiveDeleteAccount(long rowId){
        String accountUID = getAccountUID(rowId);
        if (getAccountType(accountUID) == AccountType.ROOT) {
            // refuse to delete ROOT
            return false;
        }
		Log.d(TAG, "Delete account with rowId and all its associated splits: " + rowId);
        List<String> descendantAccountUIDs = getDescendantAccountUIDs(accountUID, null, null);

        mDb.beginTransaction();
        try {
            if (descendantAccountUIDs.size() > 0) {
                List<Account> descendantAccounts = getSimpleAccountList(
                        AccountEntry.COLUMN_UID + " IN ('" + TextUtils.join("','", descendantAccountUIDs) + "')",
                        null,
                        null
                );
                HashMap<String, Account> mapAccounts = new HashMap<String, Account>();
                for (Account account : descendantAccounts)
                    mapAccounts.put(account.getUID(), account);
                String parentAccountFullName;
                String parentAccountUID = getParentAccountUID(accountUID);
                if (getAccountType(parentAccountUID) == AccountType.ROOT) {
                    parentAccountFullName = "";
                } else {
                    parentAccountFullName = getAccountFullName(parentAccountUID);
                }
                ContentValues contentValues = new ContentValues();
                for (String acctUID : descendantAccountUIDs) {
                    Account acct = mapAccounts.get(acctUID);
                    if (acct.getParentUID().equals(accountUID)) {
                        // direct descendant
                        acct.setParentUID(parentAccountUID);
                        if (parentAccountFullName.length() == 0) {
                            acct.setFullName(acct.getName());
                        } else {
                            acct.setFullName(parentAccountFullName + ACCOUNT_NAME_SEPARATOR + acct.getName());
                        }
                        // update DB
                        contentValues.clear();
                        contentValues.put(AccountEntry.COLUMN_PARENT_ACCOUNT_UID, parentAccountUID);
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
            //delete splits in this account
            mDb.delete(SplitEntry.TABLE_NAME,
                    SplitEntry.COLUMN_ACCOUNT_UID + "=?",
                    new String[]{getAccountUID(rowId)});
            deleteRecord(AccountEntry.TABLE_NAME, rowId);
            mDb.setTransactionSuccessful();
            return true;
        }
        finally {
            mDb.endTransaction();
        }
	}

    /**
     * Reassigns all accounts with parent UID <code>oldParentUID</code> to <code>newParentUID</code>
     * @param oldParentUID Old parent account Unique ID
     * @param newParentUID Unique ID of new parent account
     * @return Number of records which are modified
     */
    public int reassignParent(String oldParentUID, String newParentUID){
        ContentValues contentValues = new ContentValues();
        if (newParentUID == null)
            contentValues.putNull(AccountEntry.COLUMN_PARENT_ACCOUNT_UID);
        else
            contentValues.put(AccountEntry.COLUMN_PARENT_ACCOUNT_UID, newParentUID);

        return mDb.update(AccountEntry.TABLE_NAME,
                contentValues,
                AccountEntry.COLUMN_PARENT_ACCOUNT_UID + "= '" + oldParentUID + "' ",
                null);
    }

	/**
	 * Deletes an account while preserving the linked transactions
	 * Reassigns all transactions belonging to the account with id <code>rowId</code> to 
	 * the account with id <code>accountReassignId</code> before deleting the account.
	 * @param accountId Database record ID of the account to be deleted
	 * @param accountReassignId Record ID of the account to which to reassign the transactions from the previous
	 * @return <code>true</code> if deletion was successful, <code>false</code> otherwise.
	 */
	public boolean transactionPreservingDelete(long accountId, long accountReassignId){
        Log.d(TAG, "Migrating transaction splits to new account");
        ContentValues contentValues = new ContentValues();
        contentValues.put(SplitEntry.COLUMN_ACCOUNT_UID, accountReassignId);
        mDb.update(SplitEntry.TABLE_NAME,
                contentValues,
                SplitEntry.COLUMN_ACCOUNT_UID + "=?",
                new String[]{getAccountUID(accountId)});
        return destructiveDeleteAccount(accountId);
    }

    /**
     * Deletes an account and all its sub-accounts and splits with it
     * @param accountId Database record ID of account
     * @return <code>true</code> if the account and subaccounts were all successfully deleted, <code>false</code> if
     * even one was not deleted
     */
    public boolean recursiveDestructiveDelete(long accountId){
        Log.d(TAG, "Delete account with rowId with its transactions and sub-accounts: " + accountId);
        String accountUID = getAccountUID(accountId);
        if (accountUID == null) return false;
        List<String> descendantAccountUIDs = getDescendantAccountUIDs(accountUID, null, null);
        mDb.beginTransaction();
        try {
            descendantAccountUIDs.add(accountUID);
            String accountUIDList = "'" + TextUtils.join("','", descendantAccountUIDs) + "'";
            // delete splits
            mDb.delete(
                    SplitEntry.TABLE_NAME,
                    SplitEntry.COLUMN_ACCOUNT_UID + " IN (" + accountUIDList + ")",
                    null
            );
            // delete transactions that do not have any splits associate them any more
            mDb.delete(
                    TransactionEntry.TABLE_NAME,
                    "NOT EXISTS ( SELECT * FROM " + SplitEntry.TABLE_NAME +
                    " WHERE " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID +
                    " = " + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_UID + " ) ",
                    null
            );
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
        String uid = c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_UID));
        account.setUID(uid);
        account.setParentUID(c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_PARENT_ACCOUNT_UID)));
        account.setAccountType(AccountType.valueOf(c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_TYPE))));
        Currency currency = Currency.getInstance(c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_CURRENCY)));
        account.setCurrency(currency);
        account.setPlaceHolderFlag(c.getInt(c.getColumnIndexOrThrow(AccountEntry.COLUMN_PLACEHOLDER)) == 1);
        account.setDefaultTransferAccountUID(c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID)));
        account.setColorCode(c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_COLOR_CODE)));
        account.setFavorite(c.getInt(c.getColumnIndexOrThrow(AccountEntry.COLUMN_FAVORITE)) == 1);
        account.setFullName(c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_FULL_NAME)));
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
		String result = null;
		if (cursor != null) {
            if (cursor.moveToFirst()) {
                Log.d(TAG, "Account already exists. Returning existing id");
                result = cursor.getString(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_PARENT_ACCOUNT_UID));
            }
            cursor.close();
        }
		return result;
	}

    /**
     * Returns the  unique ID of the parent account of the account with database ID <code>id</code>
     * If the account has no parent, null is returned.
     * @param id DB record ID of account . Should not be null
     * @return DB record UID of the parent account, null if the account has no parent
     * @see #getParentAccountUID(String)
     */
    public String getParentAccountUID(long id){
        return getParentAccountUID(getAccountUID(id));
    }

	/**
	 * Retrieves an account object from a database with database ID <code>rowId</code>
	 * @param rowId Identifier of the account record to be retrieved
	 * @return {@link Account} object corresponding to database record
	 */
	public Account getAccount(long rowId){
		Account account = null;
		Log.v(TAG, "Fetching account with id " + rowId);
		Cursor c =	fetchRecord(AccountEntry.TABLE_NAME, rowId);
		if (c != null) {
            if (c.moveToFirst()) {
                account = buildAccountInstance(c);
            }
            c.close();
        }
		return account;
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
        String colorCode = null;
        Cursor c = mDb.query(AccountEntry.TABLE_NAME,
                new String[]{AccountEntry._ID, AccountEntry.COLUMN_COLOR_CODE},
                AccountEntry._ID + "=" + accountId,
                null, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                colorCode = c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_COLOR_CODE));
            }
            c.close();
        }
        return colorCode;
    }

    /**
     * Overloaded method. Resolves the account unique ID from the row ID and makes a call to {@link #getAccountType(String)}
     * @param accountId Database row ID of the account
     * @return {@link AccountType} of the account
     */
    public AccountType getAccountType(long accountId){
        return getAccountType(getAccountUID(accountId));
    }

    /**
	 * Returns the name of the account with id <code>accountID</code>
	 * @param accountID Database ID of the account record
	 * @return Name of the account 
	 */
	public String getName(long accountID) {
		String name = null;
		Cursor c = fetchRecord(AccountEntry.TABLE_NAME, accountID);
		if (c != null) {
            if (c.moveToFirst()) {
                name = c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_NAME));
            }
            c.close();
        }
		return name;
	}
	
	/**
	 * Returns a list of all account objects in the system
	 * @return List of {@link Account}s in the database
	 */
	public List<Account> getAllAccounts(){
		LinkedList<Account> accounts = new LinkedList<Account>();
		Cursor c = fetchAllRecords();
		
		if (c == null)
			return accounts;
		
		while(c.moveToNext()){
			accounts.add(buildAccountInstance(c));
		}
		c.close();
		return accounts;
	}

    /**
     * Returns a list of all account entries in the system (includes root account)
     * No transactions are loaded, just the accounts
     * @return List of {@link Account}s in the database
     */
    public List<Account> getSimpleAccountList(){
        LinkedList<Account> accounts = new LinkedList<Account>();
        Cursor c = fetchAccounts(null);
        if (c == null)
            return accounts;

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
        if (c == null)
            return accounts;
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
            addAccount(account);
            uid = account.getUID();
        }
        return uid;
    }

    /**
     * Creates the account with the specified name and returns its unique identifier.
     * <p>If a full hierarchical account name is provided, then the whole hierarchy is created and the
     * unique ID of the last account (at bottom) of the hierarchy is returned</p>
     * @param fullName Fully qualified name of the account
     * @param accountType Type to assign to all accounts created
     * @return String unique ID of the account at bottom of hierarchy
     */
    @NonNull
    public String createAccountHierarchy(@NonNull String fullName, @NonNull AccountType accountType) {
        String[] tokens = fullName.trim().split(ACCOUNT_NAME_SEPARATOR);
        String uid = getGnuCashRootAccountUID();
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
        return uid;
    }

    /**
     * Returns the unique ID of the opening balance account or creates one if necessary
     * @return String unique ID of the opening balance account
     */
    public String getOrCreateOpeningBalanceAccountUID(){
        String openingBalanceAccountName = getOpeningBalanceAccountFullName();
        String uid = findAccountUidByFullName(openingBalanceAccountName);
        if (uid == null){
            uid = createAccountHierarchy(openingBalanceAccountName, AccountType.EQUITY);
        }
        return uid;
    }

    /**
     * Finds an account unique ID by its full name
     * @param fullName Fully qualified name of the account
     * @return String unique ID of the account
     */
    public String findAccountUidByFullName(String fullName){
        Cursor c = mDb.query(AccountEntry.TABLE_NAME, new String[]{AccountEntry.COLUMN_UID},
                AccountEntry.COLUMN_FULL_NAME + "= ?", new String[]{fullName},
                null, null, null, "1");
        String uid = null;
        if (c != null) {
            if (c.moveToNext()) {
                uid = c.getString(c.getColumnIndexOrThrow(AccountEntry.COLUMN_UID));
            }
            c.close();
        }
        return uid;
    }

	/**
	 * Returns a cursor to all account records in the database.
     * GnuCash ROOT accounts are ignored
	 * @return {@link Cursor} to all account records
	 */
    @Override
	public Cursor fetchAllRecords(){
		Log.v(TAG, "Fetching all accounts from db");
        String selection =  AccountEntry.COLUMN_TYPE + " != ?" ;
        return mDb.query(AccountEntry.TABLE_NAME,
                null,
                selection,
                new String[]{AccountType.ROOT.name()},
                null, null,
                AccountEntry.COLUMN_NAME + " ASC");
	}

    /**
     * Returns a cursor to all account records in the database ordered by full name.
     * GnuCash ROOT accounts are ignored
     * @return {@link Cursor} to all account records
     */
    public Cursor fetchAllRecordsOrderedByFullName(){
        Log.v(TAG, "Fetching all accounts from db");
        String selection =  AccountEntry.COLUMN_TYPE + " != ?" ;
        return mDb.query(AccountEntry.TABLE_NAME,
                null,
                selection,
                new String[]{AccountType.ROOT.name()},
                null, null,
                AccountEntry.COLUMN_FULL_NAME + " ASC");
    }

    @Override
    public Cursor fetchRecord(long rowId) {
        return fetchRecord(AccountEntry.TABLE_NAME, rowId);
    }

    /**
     * Deletes an account and its transactions from the database.
     * This is equivalent to calling {@link #destructiveDeleteAccount(long)}
     * @param rowId ID of record to be deleted
     * @return <code>true</code> if successful, <code>false</code> otherwise
     */
    @Override
    public boolean deleteRecord(long rowId) {
        return destructiveDeleteAccount(rowId);
        //return deleteRecord(DatabaseHelper.TABLE_NAME, rowId);
    }

    /**
	 * Returns a Cursor set of accounts which fulfill <code>condition</code>
	 * @param condition SQL WHERE statement without the 'WHERE' itself
	 * @return Cursor set of accounts which fulfill <code>condition</code>
	 */
	public Cursor fetchAccounts(String condition){
		Log.v(TAG, "Fetching all accounts from db where " + condition);
        return mDb.query(AccountEntry.TABLE_NAME,
                null, condition, null, null, null,
                AccountEntry.COLUMN_NAME + " ASC");
	}

    /**
     * Returns a Cursor set of accounts which fulfill <code>condition</code>
     * and ordered by <code>orderBy</code>
     * @param where SQL WHERE statement without the 'WHERE' itself
     * @param whereArgs args to where clause
     * @param orderBy orderBy clause
     * @return Cursor set of accounts which fulfill <code>condition</code>
     */
    public Cursor fetchAccounts(String where, String[] whereArgs, String orderBy){
        Log.v(TAG, "Fetching all accounts from db where " +
                (where == null ? "NONE" : where) + " order by " +
                (orderBy == null ? "NONE" : orderBy));
        return mDb.query(AccountEntry.TABLE_NAME,
                null, where, whereArgs, null, null,
                orderBy);
    }
    /**
     * Returns a Cursor set of accounts which fulfill <code>condition</code>
     * <p>This method returns the accounts list sorted by the full account name</p>
     * @param condition SQL WHERE statement without the 'WHERE' itself
     * @return Cursor set of accounts which fulfill <code>condition</code>
     */
    public Cursor fetchAccountsOrderedByFullName(String condition){
        Log.v(TAG, "Fetching all accounts from db where " + condition);
        return mDb.query(AccountEntry.TABLE_NAME,
                null, condition, null, null, null,
                AccountEntry.COLUMN_FULL_NAME + " ASC");
    }
    /**
     * Returns the balance of an account while taking sub-accounts into consideration
     * @return Account Balance of an account including sub-accounts
     */
    public Money getAccountBalance(long accountId){
        Log.d(TAG, "Computing account balance for account ID " + accountId);
        String currencyCode = getCurrencyCode(accountId);
        currencyCode = currencyCode == null ? Money.DEFAULT_CURRENCY_CODE : currencyCode;
        Money balance = Money.createZeroInstance(currencyCode);

        List<Long> subAccounts = getSubAccountIds(accountId);
        for (long id : subAccounts){
            //recurse because arbitrary nesting depth is allowed
            Money subBalance = getAccountBalance(id);
            if (subBalance.getCurrency().equals(balance.getCurrency())){
                //only add the balances if they are of the same currency
                //ignore sub accounts of different currency just like GnuCash desktop does
                balance = balance.add(subBalance);
            }
        }

        Money splitSum = mTransactionsAdapter.getSplitDbAdapter().computeSplitBalance(getAccountUID(accountId));
        return balance.add(splitSum);
    }

    /**
     * Returns the balance of an account while taking sub-accounts into consideration
     * @return Account Balance of an account including sub-accounts
     */
    public Money getAccountBalance(String accountUID){
        Log.d(TAG, "Computing account balance for account ID " + accountUID);
        String currencyCode = mTransactionsAdapter.getCurrencyCode(accountUID);
        boolean hasDebitNormalBalance = getAccountType(accountUID).hasDebitNormalBalance();
        currencyCode = currencyCode == null ? Money.DEFAULT_CURRENCY_CODE : currencyCode;
        Money balance = Money.createZeroInstance(currencyCode);

        List<String> accountsList = getDescendantAccountUIDs(accountUID,
                AccountEntry.COLUMN_CURRENCY + " = ? ",
                new String[]{currencyCode});

        accountsList.add(0, accountUID);

        Log.d(TAG, "all account list : " + accountsList.size());
        Money splitSum = mTransactionsAdapter.getSplitDbAdapter().computeSplitBalance(accountsList, currencyCode, hasDebitNormalBalance);
        return balance.add(splitSum);
    }

    /**
     * Retrieve all descendant accounts of an account
     * Note, in filtering, once an account is filtered out, all its descendants
     * will also be filtered out, even they don't meet the filter condition
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
     * Returns a list of IDs for the sub-accounts for account <code>accountId</code>
     * @param accountId Account ID whose sub-accounts are to be retrieved
     * @return List of IDs for the sub-accounts for account <code>accountId</code>
     */
    public List<Long> getSubAccountIds(long accountId){
        List<Long> subAccounts = new ArrayList<Long>();
        String accountUID = getAccountUID(accountId);
        if (accountUID == null)
            return subAccounts;

        Cursor cursor = mDb.query(AccountEntry.TABLE_NAME,
                new String[]{AccountEntry._ID},
                AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " = ?",
                new String[]{accountUID},
                null, null, null);

        if (cursor != null){
            while (cursor.moveToNext()){
                subAccounts.add(cursor.getLong(cursor.getColumnIndexOrThrow(AccountEntry._ID)));
            }
            cursor.close();
        }

        return subAccounts;
    }

    /**
     * Returns a cursor to the dataset containing sub-accounts of the account with record ID <code>accoundId</code>
     * @param accountUID GUID of the parent account
     * @return {@link Cursor} to the sub accounts data set
     */
    public Cursor fetchSubAccounts(String accountUID){
        if (accountUID == null)
            throw new IllegalArgumentException("Account UID cannot be null");

        Log.v(TAG, "Fetching sub accounts for account id " + accountUID);
        return mDb.query(AccountEntry.TABLE_NAME,
                null,
                AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " = '" + accountUID + "'",
                null, null, null, AccountEntry.COLUMN_NAME + " ASC");
    }

    /**
     * Returns the top level accounts i.e. accounts with no parent or with the GnuCash ROOT account as parent
     * @return Cursor to the top level accounts
     */
    public Cursor fetchTopLevelAccounts(){
        //condition which selects accounts with no parent, whose UID is not ROOT and whose name is not ROOT
        return fetchAccounts("(" + AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " IS NULL OR "
                + AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " = '" + getGnuCashRootAccountUID() + "') AND "
                + AccountEntry.COLUMN_TYPE + " != '" + AccountType.ROOT.name() + "'");
    }

    /**
     * Returns a cursor to accounts which have recently had transactions added to them
     * @return Cursor to recently used accounts
     */
    public Cursor fetchRecentAccounts(int numberOfRecents){
        return mDb.query(TransactionEntry.TABLE_NAME
                + " LEFT OUTER JOIN " + SplitEntry.TABLE_NAME + " ON "
                + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID + " = "
                + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_UID
                + " , " + AccountEntry.TABLE_NAME + " ON " + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_ACCOUNT_UID
                + " = " + AccountEntry.TABLE_NAME + "." + AccountEntry.COLUMN_UID,
                new String[]{AccountEntry.TABLE_NAME + ".*"},
                null,
                null,
                SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_ACCOUNT_UID, //groupby
                null, //haveing
                "MAX ( " + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TIMESTAMP + " ) DESC", // order
                Integer.toString(numberOfRecents) // limit;
        );
    }

    /**
     * Fetches favorite accounts from the database
     * @return Cursor holding set of favorite accounts
     */
    public Cursor fetchFavoriteAccounts(){
        Log.v(TAG, "Fetching favorite accounts from db");
        String condition = AccountEntry.COLUMN_FAVORITE + " = 1";
        return mDb.query(AccountEntry.TABLE_NAME,
                null, condition, null, null, null,
                AccountEntry.COLUMN_NAME + " ASC");
    }

    /**
     * Returns the GnuCash ROOT account UID.
     * <p>In GnuCash desktop account structure, there is a root account (which is not visible in the UI) from which
     * other top level accounts derive. GnuCash Android does not have this ROOT account by default unless the account
     * structure was imported from GnuCash for desktop. Hence this method also returns <code>null</code> as an
     * acceptable result.</p>
     * <p><b>Note:</b> NULL is an acceptable response, be sure to check for it</p>
     * @return Unique ID of the GnuCash root account.
     */
    public String getGnuCashRootAccountUID(){
        String condition = AccountEntry.COLUMN_TYPE + "= '" + AccountType.ROOT.name() + "'";
        Cursor cursor =  fetchAccounts(condition);
        String rootUID = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                rootUID = cursor.getString(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_UID));
            }
            cursor.close();
        }
        return rootUID;
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
        if (accountUID == null) //if the account UID is null, then the accountId param was invalid. Just return
            return 0;
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
    public int getTotalAccountCount(){
        String queryCount = "SELECT COUNT(*) FROM " + AccountEntry.TABLE_NAME;
        Cursor cursor = mDb.rawQuery(queryCount, null);
        int count = 0;
        if (cursor != null){
            cursor.moveToFirst();
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

	/**
	 * Return the record ID for the account with UID <code>accountUID</code>
	 * @param accountUID String Unique ID of the account
	 * @return Record ID belonging to account UID
	 */
    @Override
	public long getID(String accountUID){
		long id = -1;
		Cursor c = mDb.query(AccountEntry.TABLE_NAME,
				new String[]{AccountEntry._ID},
				AccountEntry.COLUMN_UID + "='" + accountUID + "'",
				null, null, null, null);
		if (c != null) {
            if (c.moveToFirst()) {
                id = c.getLong(c.getColumnIndexOrThrow(AccountEntry._ID));
            }
            c.close();
        }
		return id;
	}

    @Override
    public String getUID(long id) {
        return getAccountUID(id);
    }

    /**
	 * Returns currency code of account with database ID <code>id</code>
	 * @param id Record ID of the account to be removed
	 * @return Currency code of the account
	 */
	public String getCurrencyCode(long id){
		return mTransactionsAdapter.getCurrencyCode(id);
	}

    /**
     * Returns the simple name of the account with unique ID <code>accountUID</code>.
     * @param accountUID Unique identifier of the account
     * @return Name of the account as String
     * @see #getFullyQualifiedAccountName(String)
     */
    public String getAccountName(String accountUID){
        if (accountUID == null)
            return null;

        Cursor cursor = mDb.query(AccountEntry.TABLE_NAME,
                new String[]{AccountEntry._ID, AccountEntry.COLUMN_NAME},
                AccountEntry.COLUMN_UID + " = ?",
                new String[]{accountUID}, null, null, null);

        if (cursor == null) {
            return null;
        } else if ( cursor.getCount() < 1) {
            cursor.close();
            return null;
        } else {  //account UIDs should be unique
            cursor.moveToFirst();
        }

        String accountName = cursor.getString(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_NAME));
        cursor.close();

        return accountName;
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

        if (cursor == null) {
            return 0;
        } else if (cursor.getCount() < 1) {
            cursor.close();
            return 0;
        } else {
            cursor.moveToFirst();
        }

        String defaultTransferAccountUID = cursor.getString(
                cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID));
        cursor.close();

        return getAccountID(defaultTransferAccountUID);
    }

    /**
     * Returns the full account name including the account hierarchy (parent accounts)
     * @param accountUID Unique ID of account
     * @return Fully qualified (with parent hierarchy) account name
     */
    public String getFullyQualifiedAccountName(String accountUID){
        String accountName = getAccountName(accountUID);
        String parentAccountUID = getParentAccountUID(accountUID);

        if (parentAccountUID == null || parentAccountUID.equalsIgnoreCase(getGnuCashRootAccountUID())){
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
        return null;
    }

    /**
     * Overloaded convenience method.
     * Simply resolves the account UID and calls {@link #getFullyQualifiedAccountName(String)}
     * @param accountId Database record ID of account
     * @return Fully qualified (with parent hierarchy) account name
     */
    public String getFullyQualifiedAccountName(long accountId){
        return getFullyQualifiedAccountName(getAccountUID(accountId));
    }

    /**
     * Returns <code>true</code> if the account with unique ID <code>accountUID</code> is a placeholder account.
     * @param accountUID Unique identifier of the account
     * @return <code>true</code> if the account is a placeholder account, <code>false</code> otherwise
     */
    public boolean isPlaceholderAccount(String accountUID){
        if (accountUID == null)
            return false;

        Cursor cursor = mDb.query(AccountEntry.TABLE_NAME,
                new String[]{AccountEntry.COLUMN_PLACEHOLDER},
                AccountEntry.COLUMN_UID + " = ?",
                new String[]{accountUID}, null, null, null);

        boolean isPlaceholder = false;
        if (cursor != null){
            if (cursor.moveToFirst()){
                isPlaceholder = cursor.getInt(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_PLACEHOLDER)) == 1;
            }
            cursor.close();
        }

        return isPlaceholder;
    }

    /**
     * Convenience method, resolves the account unique ID and calls {@link #isPlaceholderAccount(String)}
     * @param accountId Database row ID of the account
     * @return <code>true</code> if the account is a placeholder account, <code>false</code> otherwise
     */
    public boolean isPlaceholderAccount(long accountId){
        return isPlaceholderAccount(getAccountUID(accountId));
    }

    /**
     * Returns true if the account is a favorite account, false otherwise
     * @param accountId Record ID of the account
     * @return <code>true</code> if the account is a favorite account, <code>false</code> otherwise
     */
    public boolean isFavoriteAccount(long accountId){
        Cursor cursor = mDb.query(AccountEntry.TABLE_NAME,
                new String[]{AccountEntry.COLUMN_FAVORITE},
                AccountEntry._ID + " = " + accountId, null,
                null, null, null);

        boolean isFavorite = false;
        if (cursor != null){
            if (cursor.moveToFirst()){
                isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_FAVORITE)) == 1;
            }
            cursor.close();
        }
        return isFavorite;
    }

    /**
     * Updates all opening balances to the current account balances
     */
    public List<Transaction> getAllOpeningBalanceTransactions(){
        Cursor cursor = fetchAccounts(null);
        List<Transaction> openingTransactions = new ArrayList<Transaction>();
        if (cursor != null){
            SplitsDbAdapter splitsDbAdapter = new SplitsDbAdapter(mDb);
            while(cursor.moveToNext()){
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(AccountEntry._ID));
                String accountUID = getAccountUID(id);
                String currencyCode = getCurrencyCode(id);
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
            cursor.close();
        }
        return openingTransactions;
    }


    /**
     * Returns the imbalance account where to store transactions which are not double entry
     * @param currency Currency of the transaction
     * @return Imbalance account name
     */
    public static String getImbalanceAccountName(Currency currency){
        return GnuCashApplication.getAppContext().getString(R.string.imbalance_account_name) + "-" + currency.getCurrencyCode();
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
        if (cursor != null){
            while (cursor.moveToNext()){
                String currencyCode = cursor.getString(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_CURRENCY));
                currencyList.add(Currency.getInstance(currencyCode));
            }
            cursor.close();
        }
        return currencyList;
    }

    /**
	 * Deletes all accounts and their transactions (and their splits) from the database.
     * Basically empties all 3 tables, so use with care ;)
	 */
    @Override
	public int deleteAllRecords(){
		mDb.delete(TransactionEntry.TABLE_NAME, null, null);
        mDb.delete(SplitEntry.TABLE_NAME, null, null);
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
