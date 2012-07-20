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

package org.gnucash.android.receivers;

import java.util.Currency;

import org.gnucash.android.data.Account;
import org.gnucash.android.db.AccountsDbAdapter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class AccountCreator extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("Gnucash", "Received account creation intent");
		Bundle args = intent.getExtras();		
		
		String uid = args.getString(Intent.EXTRA_UID);
		
		Account account = new Account(args.getString(Intent.EXTRA_TITLE));
		String currencyCode = args.getString(Account.EXTRA_CURRENCY_CODE);
		
		if (currencyCode != null){
			Currency currency = Currency.getInstance(currencyCode);
			account.setCurrency(currency);
		}
		
		if (uid != null)
			account.setUID(uid);
		
		AccountsDbAdapter accountsAdapter = new AccountsDbAdapter(context);
		accountsAdapter.addAccount(account);
		accountsAdapter.close();
	}

}
