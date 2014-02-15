/*
 * Copyright (c) 2013 - 2014 Ngewi Fet <ngewif@gmail.com>
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
package org.gnucash.android.export.qif;

import android.content.Context;
import org.gnucash.android.model.Account;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;

import java.util.List;

/**
 * @author Ngewi
 */
public class QifExporter {
    boolean mExportAll;
    Context mContext;
    private List<Account> mAccountsList;

    public QifExporter(Context context, boolean exportAll){
        AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(context);
        mAccountsList = exportAll ? accountsDbAdapter.getAllAccounts() : accountsDbAdapter.getExportableAccounts();
        accountsDbAdapter.close();

        this.mExportAll = exportAll;
        this.mContext = context;
    }

    public String generateQIF(){
        StringBuffer qifBuffer = new StringBuffer();

        TransactionsDbAdapter transactionsDbAdapter = new TransactionsDbAdapter(mContext);
        for (Account account : mAccountsList) {
            if (account.getTransactionCount() == 0)
                continue;

            qifBuffer.append(account.toQIF(mExportAll) + "\n");

            //mark as exported
            transactionsDbAdapter.markAsExported(account.getUID());
        }
        transactionsDbAdapter.close();

        return qifBuffer.toString();
    }

}
