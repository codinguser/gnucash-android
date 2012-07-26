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

import java.math.BigDecimal;
import java.util.Currency;

import org.gnucash.android.data.Account;
import org.gnucash.android.data.Money;
import org.gnucash.android.data.Transaction;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.ui.accounts.AccountsActivity;
import org.gnucash.android.ui.widget.WidgetConfigurationActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class TransactionRecorder extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("Gnucash", "Received transaction recording intent");
		Bundle args = intent.getExtras();
		String name = args.getString(Intent.EXTRA_TITLE);
		String note = args.getString(Intent.EXTRA_TEXT);
		double amountDouble = args.getDouble(Transaction.EXTRA_AMOUNT, 0);
		String currencyCode = args.getString(Account.EXTRA_CURRENCY_CODE);
		if (currencyCode == null)
			currencyCode = AccountsActivity.DEFAULT_CURRENCY_CODE;
		
		String accountUID = args.getString(Transaction.EXTRA_ACCOUNT_UID);
		if (accountUID == null)
			accountUID = "uncategorized";
		
		Money amount = new Money(new BigDecimal(amountDouble), Currency.getInstance(currencyCode));
		Transaction transaction = new Transaction(amount, name);
		transaction.setTime(System.currentTimeMillis());
		transaction.setDescription(note);	
		transaction.setAccountUID(accountUID);
		
		TransactionsDbAdapter transacionsDbAdapter = new TransactionsDbAdapter(context);
		transacionsDbAdapter.addTransaction(transaction);
		
		WidgetConfigurationActivity.updateAllWidgets(context);

		transacionsDbAdapter.close();
	}

}
