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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	
	private static final String TAG = "DatabaseHelper";
	
	private static final String DATABASE_NAME = "gnucash_db";
	private static final int DATABASE_VERSION = 1;
	
	public static final String ACCOUNTS_TABLE_NAME 		= "accounts";
	public static final String TRANSACTIONS_TABLE_NAME 	= "transactions";
	
	public static final String KEY_ROW_ID 	= "_id";
	public static final String KEY_NAME 	= "name";
	public static final String KEY_UID 		= "uid";
	public static final String KEY_TYPE 	= "type";
	public static final String KEY_CURRENCY_CODE = "currency_code";
	
	public static final String KEY_AMOUNT 		= "amount";
	public static final String KEY_ACCOUNT_UID 	= "account_uid";
	public static final String KEY_DESCRIPTION 	= "description";
	public static final String KEY_TIMESTAMP 	= "timestamp";
	public static final String KEY_EXPORTED		= "is_exported";
	
	//if you modify the order of the columns, 
	//make sure to modify the indices in DatabaseAdapter
	
	private static final String ACCOUNTS_TABLE_CREATE = "create table " + ACCOUNTS_TABLE_NAME + " ("
			+ KEY_ROW_ID + " integer primary key autoincrement, "
			+ KEY_UID 	+ " varchar(255) not null, "
			+ KEY_NAME 	+ " varchar(255) not null, "
			+ KEY_TYPE 	+ " varchar(255), "
			+ KEY_CURRENCY_CODE + " varchar(255));";
	
	private static final String TRANSACTIONS_TABLE_CREATE = "create table " + TRANSACTIONS_TABLE_NAME + " ("
			+ KEY_ROW_ID + " integer primary key autoincrement, "
			+ KEY_UID 	+ " varchar(255) not null, "			
			+ KEY_NAME 	+ " varchar(255), "
			+ KEY_TYPE 	+ " varchar(255) not null, "
			+ KEY_AMOUNT + " varchar(255) not null, "
			+ KEY_DESCRIPTION 	+ " text, "
			+ KEY_TIMESTAMP 	+ " integer not null, "
			+ KEY_ACCOUNT_UID 	+ " varchar(255) not null, "
			+ KEY_EXPORTED 		+ " tinyint default 0, "
			+ "FOREIGN KEY (" + KEY_ACCOUNT_UID + ") REFERENCES " + ACCOUNTS_TABLE_NAME + " (" + KEY_UID + ")"
			+ ");";
	
	public DatabaseHelper(Context context){
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i(TAG, "Creating gnucash database tables");
		db.execSQL(ACCOUNTS_TABLE_CREATE);
		db.execSQL(TRANSACTIONS_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(TAG, "Upgrading database from version " 
				+ oldVersion + " to " + newVersion
				+ " which will destroy all old data");
		
		if (oldVersion < newVersion){
			/*
			Log.i("DatabaseHelper", "Upgrading database to version " + newVersion);
			if (oldVersion == 1 && newVersion == 2){				
				String addColumnSql = "ALTER TABLE " + TRANSACTIONS_TABLE_NAME + 
									" ADD COLUMN " + KEY_EXPORTED + " tinyint default 0";
				db.execSQL(addColumnSql);
			}
			*/
		} else {
			Log.i(TAG, "Cannot downgrade database.");
			/*
			db.execSQL("DROP TABLE IF EXISTS " + TRANSACTIONS_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + ACCOUNTS_TABLE_NAME);
			onCreate(db);
			*/
		}
	}

}
