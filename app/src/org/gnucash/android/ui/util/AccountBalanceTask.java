/*
 * Copyright (c) 2014 Ngewi Fet <ngewif@gmail.com>
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

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.model.Money;

import java.lang.ref.WeakReference;

/**
 * An asynchronous task for computing the account balance of an account.
 * This is done asynchronously because in cases of deeply nested accounts,
 * it can take some time and would block the UI thread otherwise.
 */
public class AccountBalanceTask extends AsyncTask<Long, Void, Money> {
    public static final String LOG_TAG = AccountBalanceTask.class.getName();

    private final WeakReference<TextView> accountBalanceTextViewReference;
    private final AccountsDbAdapter accountsDbAdapter;

    public AccountBalanceTask(TextView balanceTextView, Context context){
        accountBalanceTextViewReference = new WeakReference<TextView>(balanceTextView);
        accountsDbAdapter = new AccountsDbAdapter(context);
    }

    @Override
    protected Money doInBackground(Long... params) {
        //if the view for which we are doing this job is dead, kill the job as well
        if (accountBalanceTextViewReference.get() == null){
            cancel(true);
            return Money.getZeroInstance();
        }

        Money balance = Money.getZeroInstance();
        try {
            balance = accountsDbAdapter.getAccountBalance(accountsDbAdapter.getAccountUID(params[0]));
        } catch (IllegalArgumentException ex){
            //sometimes a load computation has been started and the data set changes.
            //the account ID may no longer exist. So we catch that exception here and do nothing
            Log.e(LOG_TAG, "Error computing account balance: " + ex);
        } catch (Exception ex) {
            Log.e(LOG_TAG, "Error computing account balance: " + ex);
            ex.printStackTrace();
        }
        return balance;
    }

    @Override
    protected void onPostExecute(Money balance) {
        if (accountBalanceTextViewReference.get() != null && balance != null){
            final Context context = accountsDbAdapter.getContext();
            final TextView balanceTextView = accountBalanceTextViewReference.get();
            if (balanceTextView != null){
                balanceTextView.setText(balance.formattedString());
                int fontColor = balance.isNegative() ? context.getResources().getColor(R.color.debit_red) :
                        context.getResources().getColor(R.color.credit_green);
                balanceTextView.setTextColor(fontColor);
            }
        }
        accountsDbAdapter.close();
    }
}
