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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;
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

    /**
     * Overloaded constructor. Creates an adapter for an already open database
     * @param db SQliteDatabase instance
     */
    public AccountsDbAdapter(SQLiteDatabase db) {
        super(db);
        mTransactionsAdapter = new TransactionsDbAdapter(db);
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

        long rowId = -1;
		if ((rowId = getAccountID(account.getUID())) > 0){
			//if account already exists, then just update
			Log.d(TAG, "Updating existing account");
			mDb.update(AccountEntry.TABLE_NAME, contentValues,
                    AccountEntry._ID + " = " + rowId, null);
		} else {
			Log.d(TAG, "Adding new account to db");
			rowId = mDb.insert(AccountEntry.TABLE_NAME, null, contentValues);
		}
		
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
     * Marks all transactions for a given account as exported
     * @param accountUID Unique ID of the record to be marked as exported
     * @return Number of records marked as exported
     */
    public int markAsExported(String accountUID){
        ContentValues contentValues = new ContentValues();
        contentValues.put(TransactionEntry.COLUMN_EXPORTED, 1);
        Cursor cursor = mTransactionsAdapter.fetchAllTransactionsForAccount(accountUID);
        List<Long> transactionIdList = new ArrayList<Long>();
        if (cursor != null){
            while(cursor.moveToNext()){
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(TransactionEntry._ID));
                transactionIdList.add(id);
            }
            cursor.close();
        }
        int recordsTouched = 0;
        for (long id : transactionIdList) {
            recordsTouched += mDb.update(TransactionEntry.TABLE_NAME,
                    contentValues,
                    TransactionEntry._ID + "=" + id, null);
        }
        return recordsTouched;
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
	 * @param rowId Database id of the account record to be deleted
	 * @return <code>true</code> if deletion was successful, <code>false</code> otherwise.
	 */
	public boolean destructiveDeleteAccount(long rowId){
		Log.d(TAG, "Delete account with rowId and all its associated splits: " + rowId);

        //delete splits in this account
        mDb.delete(SplitEntry.TABLE_NAME,
               SplitEntry.COLUMN_ACCOUNT_UID + "=?",
                new String[]{getAccountUID(rowId)});

		return deleteRecord(AccountEntry.TABLE_NAME, rowId);
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
     * Deletes an account and all its sub-accounts and transactions with it
     * @param accountId Database record ID of account
     * @return <code>true</code> if the account and subaccounts were all successfully deleted, <code>false</code> if
     * even one was not deleted
     */
    public boolean recursiveDestructiveDelete(long accountId){
        Log.d(TAG, "Delete account with rowId with its transactions and sub-accounts: " + accountId);
        boolean result = false;

        List<Long> subAccountIds = getSubAccountIds(accountId);
        for (long subAccountId : subAccountIds) {
            result |= recursiveDestructiveDelete(subAccountId);
        }
        result |= destructiveDeleteAccount(accountId);

        return result;
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
		return getAccount(getId(uid));
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

        while(c.moveToNext()){
            accounts.add(buildSimpleAccountInstance(c));
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
			
			if (!account.hasUnexportedTransactions())
				it.remove();
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
    public String createAccountHierarchy(String fullName, AccountType accountType){
        if (fullName == null)
            throw new IllegalArgumentException("The account name cannot be null");

        String[] tokens = fullName.trim().split(ACCOUNT_NAME_SEPARATOR);
        String uid = null;
        String parentName = "";
        for (String token : tokens) {
            parentName += token;
            String parentUID = findAccountUidByFullName(parentName);
            parentName += ACCOUNT_NAME_SEPARATOR;
            if (parentUID != null){ //the parent account exists, don't recreate
                uid = parentUID;
                continue;
            }
            Account account = new Account(token);
            account.setAccountType(accountType);
            account.setParentUID(uid); //set its parent
            uid = account.getUID();
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

        SplitsDbAdapter splitsDbAdapter = new SplitsDbAdapter(getContext());
        Money splitSum = splitsDbAdapter.computeSplitBalance(getAccountUID(accountId));
        splitsDbAdapter.close();
        return balance.add(splitSum);
    }

    /**
     * Returns a list of IDs for the sub-accounts for account <code>accountId</code>
     * @param accountId Account ID whose sub-accounts are to be retrieved
     * @return List of IDs for the sub-accounts for account <code>accountId</code>
     */
    public List<Long> getSubAccountIds(long accountId){
        List<Long> subAccounts = new ArrayList<Long>();
        Cursor cursor = mDb.query(AccountEntry.TABLE_NAME,
                new String[]{AccountEntry._ID},
                AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " = ?",
                new String[]{getAccountUID(accountId)},
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
     * @param accountId Record ID of the parent account
     * @return {@link Cursor} to the sub accounts data set
     */
    public Cursor fetchSubAccounts(long accountId){
        Log.v(TAG, "Fetching sub accounts for account id " + accountId);
        return mDb.query(AccountEntry.TABLE_NAME,
                null,
                AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " = ?",
                new String[]{getAccountUID(accountId)},
                null, null, AccountEntry.COLUMN_NAME + " ASC");
    }

    /**
     * Returns the top level accounts i.e. accounts with no parent or with the GnuCash ROOT account as parent
     * @return Cursor to the top level accounts
     */
    public Cursor fetchTopLevelAccounts(){
        //condition which selects accounts with no parent, whose UID is not ROOT and whose name is not ROOT
        StringBuilder condition = new StringBuilder("(");
        condition.append(AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " IS NULL");
        condition.append(" OR ");
        condition.append(AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " = ");
        condition.append("'").append(getGnuCashRootAccountUID()).append("'");
        condition.append(")");
        condition.append(" AND ");
        condition.append(AccountEntry.COLUMN_TYPE + " != " + "'").append(AccountType.ROOT.name()).append("'");
        return fetchAccounts(condition.toString());
    }

    /**
     * Returns a cursor to accounts which have recently had transactions added to them
     * @return Cursor to recently used accounts
     */
    public Cursor fetchRecentAccounts(int numberOfRecents){
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TransactionEntry.TABLE_NAME
                + " LEFT OUTER JOIN " + SplitEntry.TABLE_NAME + " ON "
                + TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_UID + " = "
                + SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_TRANSACTION_UID);
        queryBuilder.setDistinct(true);
        String sortOrder = TransactionEntry.TABLE_NAME + "." + TransactionEntry.COLUMN_TIMESTAMP + " DESC";
        Map<String, String> projectionMap = new HashMap<String, String>();
        projectionMap.put(SplitEntry.COLUMN_ACCOUNT_UID, SplitEntry.TABLE_NAME + "." + SplitEntry.COLUMN_ACCOUNT_UID);
        queryBuilder.setProjectionMap(projectionMap);
        Cursor recentTxCursor =  queryBuilder.query(mDb,
                new String[]{SplitEntry.COLUMN_ACCOUNT_UID},
                null, null, null, null, sortOrder, Integer.toString(numberOfRecents));


        StringBuilder recentAccountUIDs = new StringBuilder("(");
        while (recentTxCursor.moveToNext()){
            String uid = recentTxCursor.getString(recentTxCursor.getColumnIndexOrThrow(SplitEntry.COLUMN_ACCOUNT_UID));
            recentAccountUIDs.append("'" + uid + "'");
            if (!recentTxCursor.isLast())
                recentAccountUIDs.append(",");
        }
        recentAccountUIDs.append(")");
        recentTxCursor.close();

        return mDb.query(AccountEntry.TABLE_NAME,
                null, AccountEntry.COLUMN_UID + " IN " + recentAccountUIDs.toString(),
                null, null, null, AccountEntry.COLUMN_NAME + " ASC");

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
        if (cursor != null && cursor.moveToFirst()){
            rootUID = cursor.getString(cursor.getColumnIndexOrThrow(AccountEntry.COLUMN_UID));
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

        String queryCount = "SELECT COUNT(*) FROM " + AccountEntry.TABLE_NAME + " WHERE "
                + AccountEntry.COLUMN_PARENT_ACCOUNT_UID + " = ?";
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
	public long getId(String accountUID){
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
                Money balance = splitsDbAdapter.computeSplitBalance(accountUID);
                if (balance.asBigDecimal().compareTo(new BigDecimal(0)) == 0)
                    continue;

                Transaction transaction = new Transaction(mContext.getString(R.string.account_name_opening_balances));
                transaction.setDescription(getName(id));
                transaction.setCurrencyCode(currencyCode);
                TransactionType transactionType = Transaction.getTypeForBalance(getAccountType(accountUID),
                        balance.isNegative());
                Split split = new Split(balance.absolute(), accountUID);
                split.setType(transactionType);
                transaction.addSplit(split);
                transaction.addSplit(split.createPair(getOrCreateOpeningBalanceAccountUID()));
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

    @Override
    public void close() {
        super.close();
        mTransactionsAdapter.close();
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

}
