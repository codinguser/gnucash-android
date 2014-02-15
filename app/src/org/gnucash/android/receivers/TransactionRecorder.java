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

import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.export.qif.QifHelper;
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
		Log.i("TransactionRecorder", "Received transaction recording intent");
		Bundle args = intent.getExtras();
		String name = args.getString(Intent.EXTRA_TITLE);
		String note = args.getString(Intent.EXTRA_TEXT);
		BigDecimal amountBigDecimal = (BigDecimal) args.getSerializable(Transaction.EXTRA_AMOUNT);
		String currencyCode = args.getString(Account.EXTRA_CURRENCY_CODE);
		if (currencyCode == null)
			currencyCode = Money.DEFAULT_CURRENCY_CODE;
		
		String accountUID = args.getString(Transaction.EXTRA_ACCOUNT_UID);
		if (accountUID == null) //if no account was assigned, throw an exception
			throw new IllegalArgumentException("No account specified for the transaction");
		
		String doubleAccountUID = args.getString(Transaction.EXTRA_DOUBLE_ACCOUNT_UID);
        if (doubleAccountUID == null || doubleAccountUID.length() == 0)
            doubleAccountUID = QifHelper.getImbalanceAccountName(Currency.getInstance(Money.DEFAULT_CURRENCY_CODE));
		Transaction.TransactionType type = Transaction.TransactionType.valueOf(args.getString(Transaction.EXTRA_TRANSACTION_TYPE));

		Money amount = new Money(amountBigDecimal, Currency.getInstance(currencyCode));
		Transaction transaction = new Transaction(amount, name);
		transaction.setTime(System.currentTimeMillis());
		transaction.setDescription(note);	
		transaction.setAccountUID(accountUID);
		transaction.setDoubleEntryAccountUID(doubleAccountUID);
		transaction.setTransactionType(type);

		TransactionsDbAdapter transacionsDbAdapter = new TransactionsDbAdapter(context);
		transacionsDbAdapter.addTransaction(transaction);
		
		WidgetConfigurationActivity.updateAllWidgets(context);

		transacionsDbAdapter.close();
	}

}
