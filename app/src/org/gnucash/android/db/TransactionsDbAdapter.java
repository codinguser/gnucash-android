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
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.Transaction.TransactionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

/**
 * Manages persistence of {@link Transaction}s in the database
 * Handles adding, modifying and deleting of transaction records.
 * @author Ngewi Fet <ngewif@gmail.com> 
 * 
 */
public class TransactionsDbAdapter extends DatabaseAdapter {

	/**
	 * Constructor. 
	 * Calls to the base class to open the database
	 * @param context Application context
	 */
	public TransactionsDbAdapter(Context context) {
		super(context);
	}
	
	/**
	 * Adds an transaction to the database. 
	 * If a transaction already exists in the database with the same unique ID, 
	 * then the record will just be updated instead
	 * @param transaction {@link Transaction} to be inserted to database
	 * @return Database row ID of the inserted transaction
	 */
	public long addTransaction(Transaction transaction){
		ContentValues contentValues = new ContentValues();
		contentValues.put(DatabaseHelper.KEY_NAME, transaction.getName());
		contentValues.put(DatabaseHelper.KEY_AMOUNT, transaction.getAmount().toPlainString());
		contentValues.put(DatabaseHelper.KEY_TYPE, transaction.getTransactionType().name());
		contentValues.put(DatabaseHelper.KEY_UID, transaction.getUID());
		contentValues.put(DatabaseHelper.KEY_ACCOUNT_UID, transaction.getAccountUID());
		contentValues.put(DatabaseHelper.KEY_TIMESTAMP, transaction.getTimeMillis());
		contentValues.put(DatabaseHelper.KEY_DESCRIPTION, transaction.getDescription());
		contentValues.put(DatabaseHelper.KEY_EXPORTED, transaction.isExported() ? 1 : 0);
		contentValues.put(DatabaseHelper.KEY_DOUBLE_ENTRY_ACCOUNT_UID, transaction.getDoubleEntryAccountUID());
		contentValues.put(DatabaseHelper.KEY_RECURRENCE_PERIOD, transaction.getRecurrencePeriod());

		long rowId = -1;
		if ((rowId = fetchTransactionWithUID(transaction.getUID())) > 0){
			//if transaction already exists, then just update
			Log.d(TAG, "Updating existing transaction");
			mDb.update(DatabaseHelper.TRANSACTIONS_TABLE_NAME, contentValues, DatabaseHelper.KEY_ROW_ID + " = " + rowId, null);
		} else {
			Log.d(TAG, "Adding new transaction to db");
			rowId = mDb.insert(DatabaseHelper.TRANSACTIONS_TABLE_NAME, null, contentValues);
		}	
		
		return rowId;
	}

    /**
	 * Fetch a transaction from the database which has a unique ID <code>uid</code>
	 * @param uid Unique Identifier of transaction to be retrieved
	 * @return Database row ID of transaction with UID <code>uid</code>
	 */
	public long fetchTransactionWithUID(String uid){
		Cursor cursor = mDb.query(DatabaseHelper.TRANSACTIONS_TABLE_NAME, 
				new String[] {DatabaseHelper.KEY_ROW_ID, DatabaseHelper.KEY_UID}, 
				DatabaseHelper.KEY_UID + " = '" + uid + "'", 
				null, null, null, null);
		long result = -1;
		if (cursor != null && cursor.moveToFirst()){
			Log.d(TAG, "Transaction already exists. Returning existing id");
			result = cursor.getLong(0); //0 because only one row was requested

			cursor.close();
		}
		return result;
	}

	/**
	 * Retrieves a transaction object from a database with database ID <code>rowId</code>
	 * @param rowId Identifier of the transaction record to be retrieved
	 * @return {@link Transaction} object corresponding to database record
	 */
	public Transaction getTransaction(long rowId){
		if (rowId <= 0)
			return null;
		
		Log.v(TAG, "Fetching transaction with id " + rowId);
        Transaction transaction = null;
		Cursor c =	fetchRecord(DatabaseHelper.TRANSACTIONS_TABLE_NAME, rowId);
		if (c != null && c.moveToFirst()){
			transaction = buildTransactionInstance(c);			
			c.close();
		}
		return transaction;
	}
	
	/**
	 * Returns a cursor to a set of all transactions for the account with UID <code>accountUID</code>
	 * or for which this account is the origin account (double entry) 
	 * i.e <code>accountUID</code> is double entry account UID
	 * @param accountUID UID of the account whose transactions are to be retrieved
	 * @return Cursor holding set of transactions for particular account
	 */
	public Cursor fetchAllTransactionsForAccount(String accountUID){
        //fetch transactions from this account except recurring transactions. Those have their own view
		Cursor cursor = mDb.query(DatabaseHelper.TRANSACTIONS_TABLE_NAME, 
				null, 
				"((" + DatabaseHelper.KEY_ACCOUNT_UID + " = '" + accountUID + "') "
				+ "OR (" + DatabaseHelper.KEY_DOUBLE_ENTRY_ACCOUNT_UID + " = '" + accountUID + "' ))"
                + " AND " + DatabaseHelper.KEY_RECURRENCE_PERIOD + " = 0",
				null, null, null, DatabaseHelper.KEY_TIMESTAMP + " DESC");

		return cursor;
	}

    /**
     * Fetches all recurring transactions from the database.
     * <p>These transactions are not considered "normal" transactions, but only serve to note recurring transactions.
     * They are not considered when computing account balances</p>
     * @return Cursor holding set of all recurring transactions
     */
    public Cursor fetchAllRecurringTransactions(){
        Cursor cursor = mDb.query(DatabaseHelper.TRANSACTIONS_TABLE_NAME,
                null,
                DatabaseHelper.KEY_RECURRENCE_PERIOD + "!= 0",
                null, null, null,
                DatabaseHelper.KEY_ACCOUNT_UID + " ASC, " + DatabaseHelper.KEY_RECURRENCE_PERIOD + " ASC");
//                DatabaseHelper.KEY_RECURRENCE_PERIOD + " ASC, " + DatabaseHelper.KEY_TIMESTAMP + " DESC");
        return cursor;
    }

	/**
	 * Returns a cursor to a set of all transactions for the account with ID <code>accountID</code>
	 * or for which this account is the origin account in a double entry
	 * @param accountID ID of the account whose transactions are to be retrieved
	 * @return Cursor holding set of transactions for particular account
	 */
	public Cursor fetchAllTransactionsForAccount(long accountID){
		return fetchAllTransactionsForAccount(getAccountUID(accountID));	
	}
	
	/**
	 * Returns list of all transactions for account with UID <code>accountUID</code>
	 * @param accountUID UID of account whose transactions are to be retrieved
	 * @return List of {@link Transaction}s for account with UID <code>accountUID</code>
	 */
	public List<Transaction> getAllTransactionsForAccount(String accountUID){
		Cursor c = fetchAllTransactionsForAccount(accountUID);
		ArrayList<Transaction> transactionsList = new ArrayList<Transaction>();
		
		if (c == null || (c.getCount() <= 0))
			return transactionsList;
		
		while (c.moveToNext()) {
			Transaction transaction = buildTransactionInstance(c);
			String doubleEntryAccountUID = transaction.getDoubleEntryAccountUID();
			// Negate double entry transactions for the transfer account
			if (doubleEntryAccountUID != null && doubleEntryAccountUID.equals(accountUID)){
				if (transaction.getType() == TransactionType.DEBIT) {
					transaction.setType(TransactionType.CREDIT);
				} else {
					transaction.setType(TransactionType.DEBIT);
				}
			}
			transactionsList.add(transaction);
		}
		c.close();
		return transactionsList;
	}
	
	/**
	 * Builds a transaction instance with the provided cursor.
	 * The cursor should already be pointing to the transaction record in the database
	 * @param c Cursor pointing to transaction record in database
	 * @return {@link Transaction} object constructed from database record
	 */
	public Transaction buildTransactionInstance(Cursor c){		
		String accountUID = c.getString(DatabaseAdapter.COLUMN_ACCOUNT_UID);
		String doubleAccountUID = c.getString(DatabaseAdapter.COLUMN_DOUBLE_ENTRY_ACCOUNT_UID);
		Currency currency = Currency.getInstance(getCurrencyCode(accountUID));
		String amount = c.getString(DatabaseAdapter.COLUMN_AMOUNT);
		Money moneyAmount = new Money(new BigDecimal(amount), currency);
		String name   = c.getString(DatabaseAdapter.COLUMN_NAME);
        long recurrencePeriod = c.getLong(DatabaseAdapter.COLUMN_RECURRENCE_PERIOD);
		
		Transaction transaction = new Transaction(moneyAmount, name);
		transaction.setUID(c.getString(DatabaseAdapter.COLUMN_UID));
		transaction.setAccountUID(accountUID);
		transaction.setTime(c.getLong(DatabaseAdapter.COLUMN_TIMESTAMP));
		transaction.setDescription(c.getString(DatabaseAdapter.COLUMN_DESCRIPTION));
		transaction.setExported(c.getInt(DatabaseAdapter.COLUMN_EXPORTED) == 1);
		transaction.setDoubleEntryAccountUID(doubleAccountUID);
        transaction.setRecurrencePeriod(recurrencePeriod);
		transaction.setTransactionType(Transaction.TransactionType.valueOf(c.getString(DatabaseAdapter.COLUMN_TYPE)));

		return transaction;
	}

	/**
	 * Returns the currency code (according to the ISO 4217 standard) of the account 
	 * with unique Identifier <code>accountUID</code>
	 * @param accountUID Unique Identifier of the account
	 * @return Currency code of the account
	 * @see #getCurrencyCode(long)
	 */
	public String getCurrencyCode(String accountUID) {
		Cursor cursor = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME, 
				new String[] {DatabaseHelper.KEY_CURRENCY_CODE}, 
				DatabaseHelper.KEY_UID + "= '" + accountUID + "'", 
				null, null, null, null);
		
		if (cursor == null || cursor.getCount() <= 0)
			return null;
					
		cursor.moveToFirst();
		String currencyCode = cursor.getString(0);
		cursor.close();
		return currencyCode;
	}
	
	/**
	 * Returns the currency code (ISO 4217) used by the account with id <code>accountId</code>
	 * If you do not have the database record Id, you can call {@link #getAccountID(String)} instead.
	 * @param accountId Database record id of the account 
	 * @return Currency code of the account with Id <code>accountId</code>
	 * @see #getCurrencyCode(String)
	 */
	public String getCurrencyCode(long accountId){
		String accountUID = getAccountUID(accountId);
		return getCurrencyCode(accountUID);
	}
	
	/**
	 * Deletes transaction record with id <code>rowId</code>
	 * @param rowId Long database record id
	 * @return <code>true</code> if deletion was successful, <code>false</code> otherwise
	 */
    @Override
	public boolean deleteRecord(long rowId){
		Log.d(TAG, "Delete transaction with record Id: " + rowId);
		return deleteRecord(DatabaseHelper.TRANSACTIONS_TABLE_NAME, rowId);
	}
	
	/**
	 * Deletes transaction record with unique ID <code>uid</code>
	 * @param uid String unique ID of transaction
	 * @return <code>true</code> if deletion was successful, <code>false</code> otherwise
	 */
	public boolean deleteTransaction(String uid){
		return mDb.delete(DatabaseHelper.TRANSACTIONS_TABLE_NAME, 
				DatabaseHelper.KEY_UID + "='" + uid + "'", null) > 0;
	}
	
	/**
	 * Deletes all transactions in the database
	 * @return Number of affected transaction records
	 */
    @Override
	public int deleteAllRecords(){
		return deleteAllRecords(DatabaseHelper.TRANSACTIONS_TABLE_NAME);
	}
	
	/**
	 * Assigns transaction with id <code>rowId</code> to account with id <code>accountId</code>
	 * @param rowId Record ID of the transaction to be assigned
	 * @param accountId Record Id of the account to which the transaction will be assigned
	 * @return Number of transactions affected
	 */
	public int moveTranscation(long rowId, long accountId){
		Log.i(TAG, "Moving transaction " + rowId + " to account " + accountId);
		String accountUID = getAccountUID(accountId);
		ContentValues contentValue = new ContentValues();
		contentValue.put(DatabaseHelper.KEY_ACCOUNT_UID, accountUID);
		
		return mDb.update(DatabaseHelper.TRANSACTIONS_TABLE_NAME, 
				contentValue, 
				DatabaseHelper.KEY_ROW_ID + "=" + rowId, 
				null);
	}
	
	/**
	 * Returns the number of transactions belonging to account with id <code>accountId</code>
	 * @param accountId Long ID of account
	 * @return Number of transactions assigned to account with id <code>accountId</code>
	 */
	public int getTransactionsCount(long accountId){
		Cursor cursor = fetchAllTransactionsForAccount(accountId);
		int count = 0;
		if (cursor == null)
			return count;
		else {
			count = cursor.getCount();
			cursor.close();
		}
		return count;
	}
	
	/**
	 * Returns the total number of transactions in the database
	 * regardless of what account they belong to
	 * @return Number of transaction in the database
	 */
	public long getAllTransactionsCount(){
		String sql = "SELECT COUNT(*) FROM " + DatabaseHelper.TRANSACTIONS_TABLE_NAME;		
		SQLiteStatement statement = mDb.compileStatement(sql);
        return statement.simpleQueryForLong();
	}
	
	/**
	 * Returns the sum of transactions belonging to the account with id <code>accountId</code>
     * Double entry accounting is taken into account and the balance reflects the transfer transactions.
	 * @param accountId Record ID of the account
	 * @return Sum of transactions belonging to the account
	 */
	public Money getTransactionsSum(long accountId){
        //FIXME: Properly compute the balance while considering normal account balance
        String accountUID = getAccountUID(accountId);

        String querySum = "SELECT TOTAL(" + DatabaseHelper.KEY_AMOUNT
                + ") FROM " + DatabaseHelper.TRANSACTIONS_TABLE_NAME
                + " WHERE " + DatabaseHelper.KEY_ACCOUNT_UID + " = ? AND "
                + DatabaseHelper.KEY_RECURRENCE_PERIOD + "=0";

        Cursor sumCursor = mDb.rawQuery(querySum, new String[]{accountUID});
        double sum = 0d;

        if (sumCursor != null && sumCursor.moveToFirst()){
            sum += sumCursor.getFloat(0);
            sumCursor.close();
        }

        querySum = "SELECT TOTAL(" + DatabaseHelper.KEY_AMOUNT
                + ") FROM " + DatabaseHelper.TRANSACTIONS_TABLE_NAME
                + " WHERE " + DatabaseHelper.KEY_DOUBLE_ENTRY_ACCOUNT_UID + " = ? AND "
                + DatabaseHelper.KEY_RECURRENCE_PERIOD + "=0";

        sumCursor = mDb.rawQuery(querySum, new String[]{accountUID});

        if (sumCursor != null && sumCursor.moveToFirst()){
            sum -= sumCursor.getFloat(0);
            sumCursor.close();
        }

        BigDecimal sumDecimal = new BigDecimal(sum);
        Currency currency = Currency.getInstance(getCurrencyCode(accountUID));
        return new Money(sumDecimal, currency);
	}
	
	/**
	 * Returns true if <code>rowId</code> and <code>accountUID</code> belong to the same account
	 * @param rowId Database record ID
	 * @param accountUID Unique Identifier string of the account
	 * @return <code>true</code> if both are properties of the same account, <code>false</code> otherwise
	 */
	public boolean isSameAccount(long rowId, String accountUID){
		return getAccountID(accountUID) == rowId;
	}

    /**
     * Returns the {@link Account.AccountType} of the account with unique ID <code>uid</code>
     * @param accountUID Unique ID of the account
     * @return {@link Account.AccountType} of the account
     */
    public Account.AccountType getAccountType(String accountUID){
        String type = null;
        Cursor c = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME,
                new String[]{DatabaseHelper.KEY_TYPE},
                DatabaseHelper.KEY_UID + "='" + accountUID + "'",
                null, null, null, null);
        if (c != null && c.moveToFirst()){
            type = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.KEY_TYPE));
            c.close();
        }
        return Account.AccountType.valueOf(type);
    }

	/**
	 * Marks an account record as exported
	 * @param accountUID Unique ID of the record to be marked as exported
	 * @return Number of records marked as exported
	 */
	public int markAsExported(String accountUID){
		ContentValues contentValues = new ContentValues();
		contentValues.put(DatabaseHelper.KEY_EXPORTED, 1);
		
		return mDb.update(DatabaseHelper.TRANSACTIONS_TABLE_NAME, 
				contentValues, 
				DatabaseHelper.KEY_ACCOUNT_UID + "='" + accountUID + "'", 
				null);
	}
	
	/**
	 * Returns list of all accounts which have not been exported yet
	 * @return List of {@link Account}s which have not been exported
	 */
	public List<Transaction> getNonExportedTransactionsForAccount(String accountUID){
		Cursor c = mDb.query(DatabaseHelper.TRANSACTIONS_TABLE_NAME, 
				null, 
				DatabaseHelper.KEY_EXPORTED + "= 0 AND " + 
				DatabaseHelper.KEY_ACCOUNT_UID + "= '" + accountUID + "'", 
				null, null, null, null);
		ArrayList<Transaction> transactionsList = new ArrayList<Transaction>();
		if (c == null)
			return transactionsList;
		
		while (c.moveToNext()){
			transactionsList.add(buildTransactionInstance(c));
		}
		return transactionsList;
	}

	/**
	 * Returns an account UID of the account with record id <code>accountRowID</code>
	 * @param accountRowID Record ID of account as long paramenter
	 * @return String containing UID of account
	 */
	public String getAccountUID(long accountRowID){
		String uid = null;
		Cursor c = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME, 
				new String[]{DatabaseHelper.KEY_UID}, 
				DatabaseHelper.KEY_ROW_ID + "=" + accountRowID, 
				null, null, null, null);
		if (c != null && c.moveToFirst()){
			uid = c.getString(0);
			c.close();
		}
		return uid;
	}

    /**
     * Returns Unique Identifier of account to which <code>transaction</code> belongs
     * @param transactionID Record ID of the transaction
     * @return Unique Identifier string of account to which transaction belongs
     */
    public String getAccountUidFromTransaction(long transactionID){
        Cursor c = mDb.query(DatabaseHelper.TRANSACTIONS_TABLE_NAME,
                new String[]{DatabaseHelper.KEY_ACCOUNT_UID},
                DatabaseHelper.KEY_ROW_ID + "=" + transactionID,
                null, null, null, null);
        String accountUID = null;
        if (c != null && c.moveToFirst()){
            accountUID = c.getString(0);
            c.close();
        }

        return accountUID;
    }

	/**
	 * Returns the database row Id of the account with unique Identifier <code>accountUID</code>
	 * @param accountUID Unique identifier of the account
	 * @return Database row ID of the account
	 */
	public long getAccountID(String accountUID){
		long id = -1;
		Cursor c = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME, 
				new String[]{DatabaseHelper.KEY_ROW_ID}, 
				DatabaseHelper.KEY_UID + "='" + accountUID + "'", 
				null, null, null, null);
		if (c != null && c.moveToFirst()){
			id = c.getLong(0);
			c.close();
		}
		return id;
	}

    /**
     * Returns the database record ID for the specified transaction UID
     * @param transactionUID Unique idendtifier of the transaction
     * @return Database record ID for the transaction
     */
    public long getID(String transactionUID){
        long id = -1;
        Cursor c = mDb.query(DatabaseHelper.TRANSACTIONS_TABLE_NAME,
                new String[]{DatabaseHelper.KEY_ROW_ID},
                DatabaseHelper.KEY_UID + "='" + transactionUID + "'",
                null, null, null, null);
        if (c != null && c.moveToFirst()){
            id = c.getLong(0);
            c.close();
        }
        return id;
    }

    @Override
    public Cursor fetchAllRecords() {
        return fetchAllRecords(DatabaseHelper.TRANSACTIONS_TABLE_NAME);
    }

    @Override
    public Cursor fetchRecord(long rowId) {
        return fetchRecord(DatabaseHelper.TRANSACTIONS_TABLE_NAME, rowId);
    }

    /**
     * Returns a cursor to transactions whose name (UI: description) start with the <code>prefix</code>
     * <p>This method is used for autocomplete suggestions when creating new transactions</p>
     * @param prefix Starting characters of the transaction name
     * @return Cursor to the data set containing all matching transactions
     */
    public Cursor fetchTransactionsStartingWith(String prefix){
        StringBuffer stringBuffer = new StringBuffer(DatabaseHelper.KEY_NAME)
                .append(" LIKE '").append(prefix).append("%'");
        String selection = stringBuffer.toString();

        Cursor c = mDb.query(DatabaseHelper.TRANSACTIONS_TABLE_NAME,
                new String[]{DatabaseHelper.KEY_ROW_ID, DatabaseHelper.KEY_NAME},
                selection,
                null, null, null,
                DatabaseHelper.KEY_NAME);
        return c;
    }
}
