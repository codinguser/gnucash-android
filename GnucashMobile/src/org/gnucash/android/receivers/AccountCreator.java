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
