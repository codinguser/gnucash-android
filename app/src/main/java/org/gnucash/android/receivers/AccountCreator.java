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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.DatabaseAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Commodity;

/**
 * Broadcast receiver responsible for creating {@link Account}s received through intents.
 * In order to create an <code>Account</code>, you need to broadcast an {@link Intent} with arguments
 * for the name, currency and optionally, a unique identifier for the account (which should be unique to Gnucash)
 * of the Account to be created. Also remember to set the right mime type so that Android can properly route the Intent
 * <b>Note</b> This Broadcast receiver requires the permission "org.gnucash.android.permission.CREATE_ACCOUNT"
 * in order to be able to use Intents to create accounts. So remember to declare it in your manifest
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 * @see {@link Account#EXTRA_CURRENCY_CODE}, {@link Account#MIME_TYPE} {@link Intent#EXTRA_TITLE}, {@link Intent#EXTRA_UID}
 */
public class AccountCreator extends BroadcastReceiver {

    private static final String LOG_TAG = "AccountCreator";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(LOG_TAG, "Received account creation intent");
        Bundle args = intent.getExtras();

        Account account = new Account(args.getString(Intent.EXTRA_TITLE));
        account.setParentUID(args.getString(Account.EXTRA_PARENT_UID));

        String currencyCode = args.getString(Account.EXTRA_CURRENCY_CODE);
        if (currencyCode != null) {
            Commodity commodity = Commodity.getInstance(currencyCode);
            if (commodity != null) {
                account.setCommodity(commodity);
            } else {
                throw new IllegalArgumentException("Commodity with '" + currencyCode
                                        + "' currency code not found in the database");
            }
        }

        String uid = args.getString(Intent.EXTRA_UID);
        if (uid != null)
            account.setUID(uid);

        AccountsDbAdapter.getInstance().addRecord(account, DatabaseAdapter.UpdateMethod.insert);
    }

}
