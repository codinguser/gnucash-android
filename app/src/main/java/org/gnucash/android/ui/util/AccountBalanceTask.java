/*
 * Copyright (c) 2014 Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2014 Yongxin Wang <fefe.wyx@gmail.com>
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

package org.gnucash.android.ui.util;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.transaction.TransactionsActivity;

import java.lang.ref.WeakReference;

/**
 * An asynchronous task for computing the account balance of an account.
 * This is done asynchronously because in cases of deeply nested accounts,
 * it can take some time and would block the UI thread otherwise.
 */
public class AccountBalanceTask extends AsyncTask<String, Void, Money> {
    public static final String LOG_TAG = AccountBalanceTask.class.getName();

    private final WeakReference<TextView> accountBalanceTextViewReference;
    private final AccountsDbAdapter       accountsDbAdapter;
    private       String                  mAccountUID;

    public AccountBalanceTask(TextView balanceTextView){
        accountBalanceTextViewReference = new WeakReference<>(balanceTextView);
        accountsDbAdapter = AccountsDbAdapter.getInstance();
    }

    @Override
    protected Money doInBackground(String... params) {
        //if the view for which we are doing this job is dead, kill the job as well
        if (accountBalanceTextViewReference.get() == null){
            cancel(true);
            return Money.getZeroInstance();
        }

        Money balance = Money.getZeroInstance();
        try {
            mAccountUID = params[0];
            balance = accountsDbAdapter.getAccountBalance(mAccountUID,
                                                          -1,
                                                          -1);
        } catch (Exception ex) {
            Log.e(LOG_TAG, "Error computing account balance ", ex);
            Crashlytics.logException(ex);
        }
        return balance;
    }

    @Override
    protected void onPostExecute(Money balance) {

        if (accountBalanceTextViewReference.get() != null && balance != null) {

            final TextView balanceTextView = accountBalanceTextViewReference.get();

            if (balanceTextView != null) {

                TransactionsActivity.displayBalance(balanceTextView,
                                                    balance,
                                                    accountsDbAdapter.getAccountType(mAccountUID));
            }
        }
    }
}
