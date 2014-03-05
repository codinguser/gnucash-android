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

package org.gnucash.android.db;

import java.util.*;

import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Account.AccountType;
import org.gnucash.android.model.Transaction;

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
     * Separator used for account name hierarchies between parent and child accounts
     */
    public static final String ACCOUNT_NAME_SEPARATOR = ":";

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
		contentValues.put(DatabaseHelper.KEY_NAME,          account.getName());
		contentValues.put(DatabaseHelper.KEY_TYPE,          account.getAccountType().name());
		contentValues.put(DatabaseHelper.KEY_UID,           account.getUID());
		contentValues.put(DatabaseHelper.KEY_CURRENCY_CODE, account.getCurrency().getCurrencyCode());
        contentValues.put(DatabaseHelper.KEY_PLACEHOLDER,   account.isPlaceholderAccount() ? 1 : 0);
        contentValues.put(DatabaseHelper.KEY_COLOR_CODE,    account.getColorHexCode());
        contentValues.put(DatabaseHelper.KEY_FAVORITE,      account.isFavorite() ? 1 : 0);
        contentValues.put(DatabaseHelper.KEY_FULL_NAME,     account.getFullName());
        contentValues.put(DatabaseHelper.KEY_PARENT_ACCOUNT_UID,            account.getParentUID());
        contentValues.put(DatabaseHelper.KEY_DEFAULT_TRANSFER_ACCOUNT_UID,  account.getDefaultTransferAccountUID());

		long rowId = -1;
		if ((rowId = getAccountID(account.getUID())) > 0){
			//if account already exists, then just update
			Log.d(TAG, "Updating existing account");
			int rowsAffected = mDb.update(DatabaseHelper.ACCOUNTS_TABLE_NAME, contentValues,
                    DatabaseHelper.KEY_ROW_ID + " = " + rowId, null);
            if (rowsAffected == 1){
                updateAccount(rowId, DatabaseHelper.KEY_FULL_NAME, getFullyQualifiedAccountName(rowId));
            }
		} else {
			Log.d(TAG, "Adding new account to db");
			rowId = mDb.insert(DatabaseHelper.ACCOUNTS_TABLE_NAME, null, contentValues);
		}
		
		//now add transactions if there are any
		if (rowId > 0){
            //update the fully qualified account name
            updateAccount(rowId, DatabaseHelper.KEY_FULL_NAME, getFullyQualifiedAccountName(rowId));
			for (Transaction t : account.getTransactions()) {
				mTransactionsAdapter.addTransaction(t);
			}
		}
		return rowId;
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

        return mDb.update(DatabaseHelper.ACCOUNTS_TABLE_NAME, contentValues, null, null);
    }

    /**
     * Updates a specific entry of an account
     * @param accountId Database record ID of the account to be updated
     * @param columnKey Name of column to be updated
     * @param newValue  New value to be assigned to the columnKey
     * @return Number of records affected
     */
    public int updateAccount(long accountId, String columnKey, String newValue){
        ContentValues contentValues = new ContentValues();
        contentValues.put(columnKey, newValue);

        return mDb.update(DatabaseHelper.ACCOUNTS_TABLE_NAME, contentValues,
                DatabaseHelper.KEY_ROW_ID + "=" + accountId, null);
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
     * Reassigns all accounts with parent UID <code>oldParentUID</code> to <code>newParentUID</code>
     * @param oldParentUID Old parent account Unique ID
     * @param newParentUID Unique ID of new parent account
     * @return Number of records which are modified
     */
    public int reassignParent(String oldParentUID, String newParentUID){
        ContentValues contentValues = new ContentValues();
        if (newParentUID == null)
            contentValues.putNull(DatabaseHelper.KEY_PARENT_ACCOUNT_UID);
        else
            contentValues.put(DatabaseHelper.KEY_PARENT_ACCOUNT_UID, newParentUID);

        return mDb.update(DatabaseHelper.ACCOUNTS_TABLE_NAME,
                contentValues,
                DatabaseHelper.KEY_PARENT_ACCOUNT_UID + "= '" + oldParentUID + "' ",
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
		Cursor transactionsCursor = mDb.query(DatabaseHelper.TRANSACTIONS_TABLE_NAME, 
				new String[]{DatabaseHelper.KEY_ACCOUNT_UID}, 
				DatabaseHelper.KEY_ACCOUNT_UID + " = " + accountId,
				null, null, null, null);
		if (transactionsCursor != null && transactionsCursor.getCount() > 0){
			Log.d(TAG, "Found transactions. Migrating to new account");
			ContentValues contentValues = new ContentValues();
			contentValues.put(DatabaseHelper.KEY_ACCOUNT_UID, accountReassignId);
			mDb.update(DatabaseHelper.TRANSACTIONS_TABLE_NAME, 
					contentValues, 
					DatabaseHelper.KEY_ACCOUNT_UID + "=" + accountId,
					null);
			transactionsCursor.close();
		}
		return destructiveDeleteAccount(accountId);
	}

    /**
     * Deletes an account and all its sub-accounts and transactions with it
     * @param accountId Database record ID of account
     * @return <code>true</code> if the account and subaccounts were all successfully deleted, <code>false</code> if
     * even one was not deleted
     */
    public boolean recursiveDestructiveDelete(long accountId){
        Log.d(TAG, "Delete account with rowId with its transactions and sub-accounts: " + accountId);
        boolean result = true;

        List<Long> subAccountIds = getSubAccountIds(accountId);
        for (long subAccountId : subAccountIds) {
            result |= recursiveDestructiveDelete(subAccountId);
        }
        result |= destructiveDeleteAccount(accountId);

        return result;
    }

	/**
	 * Builds an account instance with the provided cursor.
	 * <p>The method will not move the cursor position, so the cursor should already be pointing
     * to the account record in the database<br/>
     * <b>Note</b> that this method expects the cursor to contain all columns from the database table</p>
     *
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
        account.setPlaceHolderFlag(c.getInt(DatabaseAdapter.COLUMN_PLACEHOLDER) == 1);
        account.setDefaultTransferAccountUID(c.getString(DatabaseAdapter.COLUMN_DEFAULT_TRANSFER_ACCOUNT_UID));
        account.setColorCode(c.getString(DatabaseAdapter.COLUMN_COLOR_CODE));
        account.setFavorite(c.getInt(DatabaseAdapter.COLUMN_FAVORITE) == 1);
        account.setFullName(c.getString(DatabaseAdapter.COLUMN_FULL_NAME));
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
			Log.v(TAG, "Returning account id");
			result = cursor.getLong(DatabaseAdapter.COLUMN_ROW_ID);

			cursor.close();
		}
		return result;
	}
	
	/**
	 * Returns the  unique ID of the parent account of the account with unique ID <code>uid</code>
	 * If the account has no parent, null is returned
	 * @param uid Unique Identifier of account whose parent is to be returned. Should not be null
	 * @return DB record UID of the parent account, null if the account has no parent
	 */
	public String getParentAccountUID(String uid){
		Cursor cursor = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME, 
				new String[] {DatabaseHelper.KEY_ROW_ID, DatabaseHelper.KEY_PARENT_ACCOUNT_UID}, 
				DatabaseHelper.KEY_UID + " = ?",
                new String[]{uid},
                null, null, null, null);
		String result = null;
		if (cursor != null && cursor.moveToFirst()){
			Log.d(TAG, "Account already exists. Returning existing id");
			result = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_PARENT_ACCOUNT_UID));

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
			uid = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.KEY_UID));
			c.close();
		}
		return uid;
	}

    /**
     * Returns the color code for the account in format #rrggbb
     * @param accountId Database row ID of the account
     * @return String color code of account or null if none
     */
    public String getAccountColorCode(long accountId){
        String colorCode = null;
        Cursor c = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME,
                new String[]{DatabaseHelper.KEY_ROW_ID, DatabaseHelper.KEY_COLOR_CODE},
                DatabaseHelper.KEY_ROW_ID + "=" + accountId,
                null, null, null, null);
        if (c != null && c.moveToFirst()){
            colorCode = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.KEY_COLOR_CODE));
            c.close();
        }
        return colorCode;
    }

	/**
	 * Returns the {@link AccountType} of the account with unique ID <code>uid</code>
	 * @param uid Unique ID of the account
	 * @return {@link AccountType} of the account
	 */
	public AccountType getAccountType(String uid){
        return mTransactionsAdapter.getAccountType(uid);
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
        //TODO: Optimize to use SQL DISTINCT and load only necessary accounts from db
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
	 * Returns a cursor to all account records in the database.
     * GnuCash ROOT accounts are ignored
	 * @return {@link Cursor} to all account records
	 */
    @Override
	public Cursor fetchAllRecords(){
		Log.v(TAG, "Fetching all accounts from db");
        String selection =  DatabaseHelper.KEY_TYPE + " != ?" ;
		Cursor cursor = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME,
                null,
                selection,
                new String[]{AccountType.ROOT.name()},
                null, null,
                DatabaseHelper.KEY_NAME + " ASC");
		return cursor;
	}

    /**
     * Returns a cursor to all account records in the database ordered by full name.
     * GnuCash ROOT accounts are ignored
     * @return {@link Cursor} to all account records
     */
    public Cursor fetchAllRecordsOrderedByFullName(){
        Log.v(TAG, "Fetching all accounts from db");
        String selection =  DatabaseHelper.KEY_TYPE + " != ?" ;
        return mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME,
                null,
                selection,
                new String[]{AccountType.ROOT.name()},
                null, null,
                DatabaseHelper.KEY_FULL_NAME + " ASC");
    }

    @Override
    public Cursor fetchRecord(long rowId) {
        return fetchRecord(DatabaseHelper.ACCOUNTS_TABLE_NAME, rowId);
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
        //return deleteRecord(DatabaseHelper.ACCOUNTS_TABLE_NAME, rowId);
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
     * Returns a Cursor set of accounts which fulfill <code>condition</code>
     * <p>This method returns the accounts list sorted by the full account name</p>
     * @param condition SQL WHERE statement without the 'WHERE' itself
     * @return Cursor set of accounts which fulfill <code>condition</code>
     */
    public Cursor fetchAccountsOrderedByFullName(String condition){
        Log.v(TAG, "Fetching all accounts from db where " + condition);
        return mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME,
                null, condition, null, null, null,
                DatabaseHelper.KEY_FULL_NAME + " ASC");
    }
    /**
     * Returns the balance of an account while taking sub-accounts into consideration
     * @return Account Balance of an account including sub-accounts
     */
    public Money getAccountBalance(long accountId){
        String currencyCode = getCurrencyCode(accountId);
        currencyCode = currencyCode == null ? Money.DEFAULT_CURRENCY_CODE : currencyCode;
        Money balance = Money.createInstance(currencyCode);

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
        return balance.add(getAccount(accountId).getBalance());
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
                subAccounts.add(cursor.getLong(DatabaseAdapter.COLUMN_ROW_ID));
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
        Log.v(TAG, "Fetching sub accounts for account id " + accountId);
        return mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME,
                null,
                DatabaseHelper.KEY_PARENT_ACCOUNT_UID + " = ?",
                new String[]{getAccountUID(accountId)},
                null, null, DatabaseHelper.KEY_NAME + " ASC");
    }

    /**
     * Returns the top level accounts i.e. accounts with no parent or with the GnuCash ROOT account as parent
     * @return Cursor to the top level accounts
     */
    public Cursor fetchTopLevelAccounts(){
        //condition which selects accounts with no parent, whose UID is not ROOT and whose name is not ROOT
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
     * Returns a cursor to accounts which have recently had transactions added to them
     * @return Cursor to recently used accounts
     */
    public Cursor fetchRecentAccounts(int numberOfRecents){
        Cursor recentTxCursor = mDb.query(true, DatabaseHelper.TRANSACTIONS_TABLE_NAME,
                new String[]{DatabaseHelper.KEY_ACCOUNT_UID},
                null, null, null, null, DatabaseHelper.KEY_TIMESTAMP + " DESC", Integer.toString(numberOfRecents));
        StringBuilder recentAccountUIDs = new StringBuilder("(");
        while (recentTxCursor.moveToNext()){
            String uid = recentTxCursor.getString(recentTxCursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ACCOUNT_UID));
            recentAccountUIDs.append("'" + uid + "'");
            if (!recentTxCursor.isLast())
                recentAccountUIDs.append(",");
        }
        recentAccountUIDs.append(")");
        recentTxCursor.close();

        return mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME,
                null, DatabaseHelper.KEY_UID + " IN " + recentAccountUIDs.toString(),
                null, null, null, DatabaseHelper.KEY_NAME + " ASC");

    }

    /**
     * Fetches favorite accounts from the database
     * @return Cursor holding set of favorite accounts
     */
    public Cursor fetchFavoriteAccounts(){
        Log.v(TAG, "Fetching favorite accounts from db");
        String condition = DatabaseHelper.KEY_FAVORITE + " = 1";
        Cursor cursor = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME,
                null, condition, null, null, null,
                DatabaseHelper.KEY_NAME + " ASC");
        return cursor;
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
        //TODO: at some point when API level 11 and above only is supported, use DatabaseUtils.queryNumEntries

        String queryCount = "SELECT COUNT(*) FROM " + DatabaseHelper.ACCOUNTS_TABLE_NAME + " WHERE "
                + DatabaseHelper.KEY_PARENT_ACCOUNT_UID + " = ?";
        String accountUID = getAccountUID(accountId);
        if (accountUID == null) //if the account UID is null, then the accountId param was invalid. Just return
            return 0;
        Cursor cursor = mDb.rawQuery(queryCount, new String[]{accountUID});
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
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
     * Returns the simple name of the account with unique ID <code>accountUID</code>.
     * @param accountUID Unique identifier of the account
     * @return Name of the account as String
     * @see #getFullyQualifiedAccountName(String)
     */
    public String getAccountName(String accountUID){
        if (accountUID == null)
            return null;

        Cursor cursor = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME,
                new String[]{DatabaseHelper.KEY_ROW_ID, DatabaseHelper.KEY_NAME},
                DatabaseHelper.KEY_UID + " = ?",
                new String[]{accountUID}, null, null, null);

        if (cursor == null || cursor.getCount() < 1){
            return null;
        } else {  //account UIDs should be unique
            cursor.moveToFirst();
        }

        String accountName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_NAME));
        cursor.close();

        return accountName;
    }

    /**
     * Returns the default transfer account record ID for the account with UID <code>accountUID</code>
     * @param accountID Database ID of the account record
     * @return Record ID of default transfer account
     */
    public long getDefaultTransferAccountID(long accountID){
        Cursor cursor = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME,
                new String[]{DatabaseHelper.KEY_ROW_ID, DatabaseHelper.KEY_DEFAULT_TRANSFER_ACCOUNT_UID},
                DatabaseHelper.KEY_ROW_ID + " = " + accountID,
                null, null, null, null);

        if (cursor == null || cursor.getCount() < 1){
            return 0;
        } else {
            cursor.moveToFirst();
        }

        String defaultTransferAccountUID = cursor.getString(
                cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_DEFAULT_TRANSFER_ACCOUNT_UID));
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

        Cursor cursor = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME,
                new String[]{DatabaseHelper.KEY_ROW_ID, DatabaseHelper.KEY_PLACEHOLDER},
                DatabaseHelper.KEY_UID + " = ?",
                new String[]{accountUID}, null, null, null);

        if (cursor == null || !cursor.moveToFirst()){
            return false;
        }
        boolean isPlaceholder = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_PLACEHOLDER)) == 1;
        cursor.close();

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
        Cursor cursor = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME,
                new String[]{DatabaseHelper.KEY_ROW_ID, DatabaseHelper.KEY_FAVORITE},
                DatabaseHelper.KEY_ROW_ID + " = " + accountId, null,
                null, null, null);

        if (cursor == null || !cursor.moveToFirst()){
            return false;
        }
        boolean isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_FAVORITE)) == 1;
        cursor.close();

        return isFavorite;
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
