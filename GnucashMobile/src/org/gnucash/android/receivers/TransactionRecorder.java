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

package org.gnucash.android.receivers;

import java.math.BigDecimal;
import java.util.Currency;

import org.gnucash.android.data.Account;
import org.gnucash.android.data.Money;
import org.gnucash.android.data.Transaction;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.ui.widget.WidgetConfigurationActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Broadcast receiver responsible for creating transactions received through {@link Intent}s
 * In order to create a transaction through Intents, broadcast an intent with the arguments needed to 
 * create the transaction. Transactions are strongly bound to {@link Account}s and it is recommended to 
 * create an Account for your transactions. The transactions will be associated to the account using a unique
 * Identifier passed as {@link Transaction#EXTRA_ACCOUNT_UID}
 * <p>Remember to declare the appropriate permissions in order to create transactions with Intents. 
 * The required permission is "org.gnucash.android.permission.RECORD_TRANSACTION"</p>
 * @author Ngewi Fet <ngewif@gmail.com>
 * @see AccountCreator
 */
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
			currencyCode = Money.DEFAULT_CURRENCY_CODE;
		
		String accountUID = args.getString(Transaction.EXTRA_ACCOUNT_UID);
		if (accountUID == null)
			accountUID = "uncategorized";
		
		String doubleAccountUID = args.getString(Transaction.EXTRA_DOUBLE_ACCOUNT_UID);
		
		Money amount = new Money(new BigDecimal(amountDouble), Currency.getInstance(currencyCode));
		Transaction transaction = new Transaction(amount, name);
		transaction.setTime(System.currentTimeMillis());
		transaction.setDescription(note);	
		transaction.setAccountUID(accountUID);
		transaction.setDoubleEntryAccountUID(doubleAccountUID);
		
		TransactionsDbAdapter transacionsDbAdapter = new TransactionsDbAdapter(context);
		transacionsDbAdapter.addTransaction(transaction);
		
		WidgetConfigurationActivity.updateAllWidgets(context);

		transacionsDbAdapter.close();
	}

}
