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

import java.util.Currency;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class CurrencyDbAdapter extends DatabaseAdapter {

	public CurrencyDbAdapter(Context context) {
		super(context);
	}

	public Currency getCurrency(long id){
		Log.v(TAG, "Fetching currency with id " + id);
		Currency cur = Currency.getInstance(Locale.getDefault()); 
		Cursor c =	fetchRecord(DatabaseHelper.CURRENCIES_TABLE_NAME, id);
		if (c != null && c.moveToFirst()){
			cur = Currency.getInstance(c.getString(DatabaseAdapter.COLUMN_CURRENCY_CODE));	
			c.close();
		}
		return cur;
	}
		
	public long getCurrencyId(String currencyCode){
		Cursor c =	mDb.query(DatabaseHelper.CURRENCIES_TABLE_NAME, null, 
				DatabaseHelper.KEY_CURRENCY_CODE + "='" + currencyCode + "'", 
				null, null, null, null);
		long id = -1;
		if (c != null && c.moveToFirst()){
			id = c.getLong(DatabaseAdapter.COLUMN_ROW_ID);
			c.close();
		}
		return id;
	}
}
