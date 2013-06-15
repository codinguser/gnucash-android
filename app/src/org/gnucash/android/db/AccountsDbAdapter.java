/*
 * Copyright (c) 2012 Ngewi Fet <ngewif@gmail.com>
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

import java.util.*;

import org.gnucash.android.data.Account;
import org.gnucash.android.data.Money;
import org.gnucash.android.data.Account.AccountType;
import org.gnucash.android.data.Transaction;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

/**
 * Manages persistence of {@link Account}s in the database
 * Handles adding, modifying and deleting of account records.
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class AccountsDbAdapter extends DatabaseAdapter {

	/**
	 * Transactions database adapter for manipulating transactions associated with accounts
	 */
	private TransactionsDbAdapter mTransactionsAdapter;
	
	/**
	 * Constructor. Creates a new adapter instance using the application context
	 * @param context Application context
	 */
	public AccountsDbAdapter(Context context) {
		super(context);
		mTransactionsAdapter = new TransactionsDbAdapter(context);
	}

	@Override
	public void close() {
		super.close();
		mTransactionsAdapter.close();
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
		contentValues.put(DatabaseHelper.KEY_NAME, account.getName());
		contentValues.put(DatabaseHelper.KEY_TYPE, account.getAccountType().name());
		contentValues.put(DatabaseHelper.KEY_UID, account.getUID());
		contentValues.put(DatabaseHelper.KEY_CURRENCY_CODE, account.getCurrency().getCurrencyCode());
		contentValues.put(DatabaseHelper.KEY_PARENT_ACCOUNT_UID, account.getParentUID());
		
		long rowId = -1;
		if ((rowId = getAccountID(account.getUID())) > 0){
			//if account already exists, then just update
			Log.d(TAG, "Updating existing account");
			mDb.update(DatabaseHelper.ACCOUNTS_TABLE_NAME, contentValues, 
					DatabaseHelper.KEY_ROW_ID + " = " + rowId, null);
		} else {
			Log.d(TAG, "Adding new account to db");
			rowId = mDb.insert(DatabaseHelper.ACCOUNTS_TABLE_NAME, null, contentValues);
		}
		
		//now add transactions if there are any
		if (rowId > 0){
			for (Transaction t : account.getTransactions()) {
				mTransactionsAdapter.addTransaction(t);
			}
		}
		return rowId;
	}
	
	/**
	 * Deletes an account with database id <code>rowId</code>
	 * All the transactions in the account will also be deleted
	 * @param rowId Database id of the account record to be deleted
	 * @return <code>true</code> if deletion was successful, <code>false</code> otherwise.
	 */
	public boolean destructiveDeleteAccount(long rowId){
		Log.d(TAG, "Delete account with rowId: " + rowId);
		boolean result = true;
		//first remove all transactions for the account
		Cursor c = mTransactionsAdapter.fetchAllTransactionsForAccount(rowId);
		if (c == null)
			return result; 
		
		while (c.moveToNext()){
			long id = c.getLong(DatabaseAdapter.COLUMN_ROW_ID);
			result &= mTransactionsAdapter.deleteRecord(id);
		}
		result &= deleteRecord(DatabaseHelper.ACCOUNTS_TABLE_NAME, rowId);
		return result;
	}
	
	/**
	 * Deletes an account while preserving the linked transactions
	 * Reassigns all transactions belonging to the account with id <code>rowId</code> to 
	 * the account with id <code>accountReassignId</code> before deleting the account.
	 * @param rowIdToDelete
	 * @param accountReassignId
	 * @return <code>true</code> if deletion was successful, <code>false</code> otherwise.
	 */
	public boolean transactionPreservingDelete(long rowIdToDelete, long accountReassignId){
		Cursor transactionsCursor = mDb.query(DatabaseHelper.TRANSACTIONS_TABLE_NAME, 
				new String[]{DatabaseHelper.KEY_ACCOUNT_UID}, 
				DatabaseHelper.KEY_ACCOUNT_UID + " = " + rowIdToDelete, 
				null, null, null, null);
		if (transactionsCursor != null && transactionsCursor.getCount() > 0){
			Log.d(TAG, "Found transactions. Migrating to new account");
			ContentValues contentValues = new ContentValues();
			contentValues.put(DatabaseHelper.KEY_ACCOUNT_UID, accountReassignId);
			mDb.update(DatabaseHelper.TRANSACTIONS_TABLE_NAME, 
					contentValues, 
					DatabaseHelper.KEY_ACCOUNT_UID + "=" + rowIdToDelete, 
					null);
			transactionsCursor.close();
		}
		return destructiveDeleteAccount(rowIdToDelete);
	}
	
	/**
	 * Builds an account instance with the provided cursor.
	 * The cursor should already be pointing to the account record in the database
	 * @param c Cursor pointing to account record in database
	 * @return {@link Account} object constructed from database record
	 */
	public Account buildAccountInstance(Cursor c){
		Account account = new Account(c.getString(DatabaseAdapter.COLUMN_NAME));
		String uid = c.getString(DatabaseAdapter.COLUMN_UID);
		account.setUID(uid);
		account.setParentUID(c.getString(DatabaseAdapter.COLUMN_PARENT_ACCOUNT_UID));
		account.setAccountType(AccountType.valueOf(c.getString(DatabaseAdapter.COLUMN_TYPE)));
		//make sure the account currency is set before setting the transactions
		//else the transactions end up with a different currency from the account
		account.setCurrency(Currency.getInstance(c.getString(DatabaseAdapter.COLUMN_CURRENCY_CODE)));
		account.setTransactions(mTransactionsAdapter.getAllTransactionsForAccount(uid));
		return account;
	}
		
	/**
	 * Fetch an account from the database which has a unique ID <code>uid</code>
	 * @param uid Unique Identifier of account to be retrieved
	 * @return Database row ID of account with UID <code>uid</code>
	 */
	public long getAccountID(String uid){
		Cursor cursor = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME, 
				new String[] {DatabaseHelper.KEY_ROW_ID, DatabaseHelper.KEY_UID}, 
				DatabaseHelper.KEY_UID + " = '" + uid + "'", null, null, null, null);
		long result = -1;
		if (cursor != null && cursor.moveToFirst()){
			Log.d(TAG, "Account already exists. Returning existing id");
			result = cursor.getLong(0); //0 because only one row was requested

			cursor.close();
		}
		return result;
	}
	
	/**
	 * Returns the  unique ID of the parent account of the account with unique ID <code>uid</code>
	 * If the account has no parent, null is returned
	 * @param uid Unique Identifier of account whose parent is to be returned
	 * @return DB record UID of the parent account, null if the account has no parent
	 */
	public String getParentAccountUID(String uid){
		Cursor cursor = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME, 
				new String[] {DatabaseHelper.KEY_ROW_ID, DatabaseHelper.KEY_PARENT_ACCOUNT_UID}, 
				DatabaseHelper.KEY_UID + " = '" + uid + "'", null, null, null, null);
		String result = null;
		if (cursor != null && cursor.moveToFirst()){
			Log.d(TAG, "Account already exists. Returning existing id");
			result = cursor.getString(0); //0 because only one row was requested

			cursor.close();
		}
		return result;
	}	
	
	/**
	 * Retrieves an account object from a database with database ID <code>rowId</code>
	 * @param rowId Identifier of the account record to be retrieved
	 * @return {@link Account} object corresponding to database record
	 */
	public Account getAccount(long rowId){
		Account account = null;
		Log.v(TAG, "Fetching account with id " + rowId);
		Cursor c =	fetchRecord(DatabaseHelper.ACCOUNTS_TABLE_NAME, rowId);
		if (c != null && c.moveToFirst()){
			account = buildAccountInstance(c);	
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
		return getAccount(getId(uid));
	}	
	
	/**
	 * Returns the unique identifier for the account with record ID <code>id</code>
	 * @param id Database record id of account
	 * @return Unique identifier string of the account
	 */
	public String getAccountUID(long id){
		String uid = null;
		Cursor c = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME, 
				new String[]{DatabaseHelper.KEY_ROW_ID, DatabaseHelper.KEY_UID}, 
				DatabaseHelper.KEY_ROW_ID + "=" + id, 
				null, null, null, null);
		if (c != null && c.moveToFirst()){
			uid = c.getString(1);
			c.close();
		}
		return uid;
	}
	
	/**
	 * Returns the {@link AccountType} of the account with unique ID <code>uid</code>
	 * @param uid Unique ID of the account
	 * @return {@link AccountType} of the account
	 */
	public AccountType getAccountType(String uid){
		String type = null;
		Cursor c = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME, 
				new String[]{DatabaseHelper.KEY_TYPE}, 
				DatabaseHelper.KEY_UID + "='" + uid + "'", 
				null, null, null, null);
		if (c != null && c.moveToFirst()){
			type = c.getString(0); //0 because we requested only the type column
			c.close();
		}
		return AccountType.valueOf(type);
	}
	
	/**
	 * Returns the name of the account with id <code>accountID</code>
	 * @param accountID Database ID of the account record
	 * @return Name of the account 
	 */
	public String getName(long accountID) {
		String name = null;
		Cursor c = fetchRecord(DatabaseHelper.ACCOUNTS_TABLE_NAME, accountID);
		if (c != null && c.moveToFirst()){
			name = c.getString(DatabaseAdapter.COLUMN_NAME);
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
	 * Returns a list of accounts which have transactions that have not been exported yet
	 * @return List of {@link Account}s with unexported transactions
	 */
	public List<Account> getExportableAccounts(){
		List<Account> accountsList = getAllAccounts();
		Iterator<Account> it = accountsList.iterator();
		
		while (it.hasNext()){
			Account account = it.next();
			
			if (account.hasUnexportedTransactions() == false)
				it.remove();
		}
		return accountsList;
	}
	
	/**
	 * Returns a cursor to all account records in the database
	 * @return {@link Cursor} to all account records
	 */
    @Override
	public Cursor fetchAllRecords(){
		Log.v(TAG, "Fetching all accounts from db");
        String selection =  DatabaseHelper.KEY_TYPE + " != " + "'ROOT'";
		Cursor cursor = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME,
                null, selection, null, null, null,
                DatabaseHelper.KEY_NAME + " ASC");
		return cursor;
	}


    @Override
    public Cursor fetchRecord(long rowId) {
        return fetchRecord(DatabaseHelper.ACCOUNTS_TABLE_NAME, rowId);
    }

    @Override
    public boolean deleteRecord(long rowId) {
        return deleteRecord(DatabaseHelper.ACCOUNTS_TABLE_NAME, rowId);
    }

    /**
	 * Returns a Cursor set of accounts which fulfill <code>condition</code>
	 * @param condition SQL WHERE statement without the 'WHERE' itself
	 * @return Cursor set of accounts which fulfill <code>condition</code>
	 */
	public Cursor fetchAccounts(String condition){
		Log.v(TAG, "Fetching all accounts from db where " + condition);
		Cursor cursor = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME, 
				null, condition, null, null, null, 
				DatabaseHelper.KEY_NAME + " ASC");
		return cursor;
	}
	
	/**
	 * Returns the balance of all accounts with each transaction counted only once
	 * This does not take into account the currencies and double entry 
	 * transactions are not considered as well.
	 * @return Balance of all accounts in the database
	 * @see AccountsDbAdapter#getDoubleEntryAccountsBalance()
	 */
	public Money getAllAccountsBalance(){
		return mTransactionsAdapter.getAllTransactionsSum();
	}

    /**
     * Returns the balance of an account while taking sub-accounts into consideration
     * @return Account Balance of an account including sub-accounts
     */
    public Money getAccountBalance(long accountId){
        List<Long> subAccounts = getSubAccountIds(accountId);
        Money balance = Money.createInstance(getCurrencyCode(accountId));
        for (long id : subAccounts){
            //recurse because arbitrary nesting depth is allowed
            Money subBalance = getAccountBalance(id);
            if (subBalance.getCurrency().equals(balance.getCurrency())){
                //only add the balances if they are of the same currency
                //ignore sub accounts of different currency just like GnuCash desktop does
                balance = balance.add(getAccountBalance(id));
            }
        }
        return balance.add(mTransactionsAdapter.getTransactionsSum(accountId));
    }

    /**
     * Returns a list of IDs for the sub-accounts for account <code>accountId</code>
     * @param accountId Account ID whose sub-accounts are to be retrieved
     * @return List of IDs for the sub-accounts for account <code>accountId</code>
     */
    public List<Long> getSubAccountIds(long accountId){
        List<Long> subAccounts = new ArrayList<Long>();
        Cursor cursor = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME,
                new String[]{DatabaseHelper.KEY_ROW_ID}, DatabaseHelper.KEY_PARENT_ACCOUNT_UID + " = ?",
                new String[]{getAccountUID(accountId)}, null, null, null);

        if (cursor != null){
            while (cursor.moveToNext()){
                subAccounts.add(cursor.getLong(0));
            }
            cursor.close();
        }

        return subAccounts;
    }

    /**
     * Returns a cursor to the dataset containing sub-accounts of the account with record ID <code>accoundId</code>
     * @param accountId Record ID of the parent account
     * @return {@link Cursor} to the sub accounts data set
     */
    public Cursor fetchSubAccounts(long accountId){
        return mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME,
                null,
                DatabaseHelper.KEY_PARENT_ACCOUNT_UID + " = ?",
                new String[]{getAccountUID(accountId)},
                null, null, null);
    }

    /**
     * Returns the top level accounts i.e. accounts with no parent or with the GnuCash ROOT account as parent
     * @return Cursor to the top level accounts
     */
    public Cursor fetchTopLevelAccounts(){
        StringBuilder condition = new StringBuilder("(");
        condition.append(DatabaseHelper.KEY_PARENT_ACCOUNT_UID + " IS NULL");
        condition.append(" OR ");
        condition.append(DatabaseHelper.KEY_PARENT_ACCOUNT_UID + " = ");
        condition.append("'" + getGnuCashRootAccountUID() + "'");
        condition.append(")");
        condition.append(" AND ");
        condition.append(DatabaseHelper.KEY_TYPE + " != " + "'" + AccountType.ROOT.name() + "'");
        return fetchAccounts(condition.toString());
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
        String condition = DatabaseHelper.KEY_TYPE + "= '" + AccountType.ROOT.name() + "'";
        Cursor cursor =  fetchAccounts(condition);
        String rootUID = null;
        if (cursor != null && cursor.moveToFirst()){
            rootUID = cursor.getString(DatabaseAdapter.COLUMN_UID);
            cursor.close();
        }
        return rootUID;
    }

    /**
     * Returns the number of accounts for which the account with ID <code>accoundId</code> is a first level parent
     * @param accountId Database ID of parent account
     * @return Number of sub accounts
     */
    public int getSubAccountCount(long accountId){
        return getSubAccountIds(accountId).size();
    }

	/**
	 * Returns the balance for all transactions while taking double entry into consideration
	 * This means that double transactions will be counted twice
	 * @return Total balance of the accounts while using double entry
	 */
	public Money getDoubleEntryAccountsBalance(){
        //TODO: take currency into consideration
		Cursor c = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME, 
				new String[]{DatabaseHelper.KEY_ROW_ID},
				null, null, null, null, null);
		Money totalSum = new Money();
		if (c != null){
			while (c.moveToNext()) {
				long id = c.getLong(DatabaseAdapter.COLUMN_ROW_ID);
				Money sum = mTransactionsAdapter.getTransactionsSum(id);
				totalSum = totalSum.add(sum);
			}
			c.close();
		}
		return totalSum;
	}
	
	/**
	 * Return the record ID for the account with UID <code>accountUID</code>
	 * @param accountUID String Unique ID of the account
	 * @return Record ID belonging to account UID
	 */
	public long getId(String accountUID){
		long id = -1;
		Cursor c = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME, 
				new String[]{DatabaseHelper.KEY_ROW_ID, DatabaseHelper.KEY_UID}, 
				DatabaseHelper.KEY_UID + "='" + accountUID + "'", 
				null, null, null, null);
		if (c != null && c.moveToFirst()){
			id = c.getLong(DatabaseAdapter.COLUMN_ROW_ID);
			c.close();
		}
		return id;
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
	 * Returns the currency code of account with database ID
	 * @param accountUID Unique Identifier of the account
	 * @return ISO 4217 currency code of the account
	 * @see #getCurrencyCode(long) 
	 */
	public String getCurrencyCode(String accountUID){
		return getCurrencyCode(getAccountID(accountUID));
	}
	
	/**
	 * Deletes all accounts and their transactions from the database
	 */
    @Override
	public int deleteAllRecords(){
		mDb.delete(DatabaseHelper.TRANSACTIONS_TABLE_NAME, null, null);
        return mDb.delete(DatabaseHelper.ACCOUNTS_TABLE_NAME, null, null);
	}


}
