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

import android.database.sqlite.SQLiteDatabase;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.export.Exporter;
import org.gnucash.android.model.Account;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ngewi
 */
public class QifExporter extends Exporter{
    private List<Account> mAccountsList;

    public QifExporter(ExportParams params){
        super(params);
    }

    public QifExporter(ExportParams params,  SQLiteDatabase db){
        super(params, db);
    }

    private String generateQIF(){
        StringBuffer qifBuffer = new StringBuffer();

        List<String> exportedTransactions = new ArrayList<String>();
        for (Account account : mAccountsList) {
            if (account.getTransactionCount() == 0)
                continue;

            qifBuffer.append(account.toQIF(mParameters.shouldExportAllTransactions(), exportedTransactions) + "\n");

            //mark as exported
            mAccountsDbAdapter.markAsExported(account.getUID());
        }
        mAccountsDbAdapter.close();

        return qifBuffer.toString();
    }

    @Override
    public String generateExport() throws ExporterException {
        mAccountsList = mParameters.shouldExportAllTransactions() ?
                mAccountsDbAdapter.getAllAccounts() : mAccountsDbAdapter.getExportableAccounts();

        return generateQIF();
    }
}
