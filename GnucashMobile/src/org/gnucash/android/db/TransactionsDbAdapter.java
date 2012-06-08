/*
 * Written By: Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2012 Ngewi Fet
 *
 * This file is part of Gnucash for Android
 * 
 * Gnucash for Android is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, contact:
 *
 * Free Software Foundation           Voice:  +1-617-542-5942
 * 51 Franklin Street, Fifth Floor    Fax:    +1-617-542-2652
 * Boston, MA  02110-1301,  USA       gnu@gnu.org
 */

package org.gnucash.android.db;

import java.util.ArrayList;
import java.util.List;

import org.gnucash.android.data.Transaction;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

/**
 * Manages persistence of {@link Transaction}s in the database
 * Handles adding, modifying and deleting of transaction records.
 * @author Ngewi Fet <ngewif@gmail.com 
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
	 * then just update that transaction.
	 * @param transaction {@link Transaction} to be inserted to database
	 * @return Database row ID of the inserted transaction
	 */
	public long addTransaction(Transaction transaction){
		ContentValues contentValues = new ContentValues();
		contentValues.put(DatabaseHelper.KEY_NAME, transaction.getName());
		contentValues.put(DatabaseHelper.KEY_AMOUNT, transaction.getAmount());
		contentValues.put(DatabaseHelper.KEY_TYPE, transaction.getTransactionType().name());
		contentValues.put(DatabaseHelper.KEY_UID, transaction.getUID());
		contentValues.put(DatabaseHelper.KEY_ACCOUNT_UID, transaction.getAccountUID());
		contentValues.put(DatabaseHelper.KEY_TIMESTAMP, transaction.getTimeMillis());
		contentValues.put(DatabaseHelper.KEY_DESCRIPTION, transaction.getDescription());
		
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
				new String[] {DatabaseHelper.KEY_UID}, 
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
		Transaction transaction = null;
		Log.v(TAG, "Fetching transaction with id " + rowId);
		Cursor c =	fetchRecord(DatabaseHelper.TRANSACTIONS_TABLE_NAME, rowId);
		if (c != null && c.moveToFirst()){
			transaction = buildTransactionInstance(c);			
			c.close();
		}
		return transaction;
	}
	
	/**
	 * Returns a cursor to a set of all transactions for the account with UID <code>accountUID</code>
	 * @param accountUID UID of the account whose transactions are to be retrieved
	 * @return Cursor holding set of transactions for particular account
	 */
	public Cursor fetchAllTransactionsForAccount(String accountUID){
		Cursor cursor = mDb.query(DatabaseHelper.TRANSACTIONS_TABLE_NAME, 
				null, 
				DatabaseHelper.KEY_ACCOUNT_UID + " = '" + accountUID + "'", 
				null, null, null, null);
		
		return cursor;
	}
	
	/**
	 * Returns a cursor to a set of all transactions for the account with ID <code>accountID</code>
	 * @param accountUID ID of the account whose transactions are to be retrieved
	 * @return Cursor holding set of transactions for particular account
	 */
	public Cursor fetchAllTransactionsForAccount(long accountID){
		Cursor c = mDb.query(DatabaseHelper.ACCOUNTS_TABLE_NAME, 
				new String[] {DatabaseHelper.KEY_UID}, 
				DatabaseHelper.KEY_ROW_ID + " = '" + accountID + "'", 
				null, null, null, null);
		String uid = null;
		if (c != null && c.moveToFirst()){
			uid = c.getString(0);
		}
		
		return fetchAllTransactionsForAccount(uid);		
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
			transactionsList.add(buildTransactionInstance(c));
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
		Transaction transaction = new Transaction(c.getDouble(DatabaseAdapter.COLUMN_AMOUNT), 
				c.getString(DatabaseAdapter.COLUMN_NAME));
		transaction.setUID(c.getString(DatabaseAdapter.COLUMN_UID));
		transaction.setAccountUID(c.getString(DatabaseAdapter.COLUMN_ACCOUNT_UID));
		transaction.setTime(c.getLong(DatabaseAdapter.COLUMN_TIMESTAMP));
		
		return transaction;
	}
	
	
}
