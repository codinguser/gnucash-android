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
	public static final String CURRENCIES_TABLE_NAME 	= "currencies";
	
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
			+ KEY_CURRENCY_CODE + " varchar(255), "
			+ "UNIQUE (" + KEY_UID + ")"	
			+ ");";
	
	private static final String TRANSACTIONS_TABLE_CREATE = "create table " + TRANSACTIONS_TABLE_NAME + " ("
			+ KEY_ROW_ID 	+ " integer primary key autoincrement, "
			+ KEY_UID 		+ " varchar(255) not null, "			
			+ KEY_NAME 		+ " varchar(255), "
			+ KEY_TYPE 		+ " varchar(255) not null, "
			+ KEY_AMOUNT 	+ " varchar(255) not null, "
			+ KEY_DESCRIPTION 	+ " text, "
			+ KEY_TIMESTAMP 	+ " integer not null, "
			+ KEY_ACCOUNT_UID 	+ " varchar(255) not null, "
			+ KEY_EXPORTED 		+ " tinyint default 0, "
			+ "FOREIGN KEY (" 	+ KEY_ACCOUNT_UID + ") REFERENCES " + ACCOUNTS_TABLE_NAME + " (" + KEY_UID + "), "
			+ "UNIQUE (" 		+ KEY_UID + ") " 
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
