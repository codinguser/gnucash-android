package org.gnucash.android.export.qif;

import android.content.Context;
import org.gnucash.android.data.Account;
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
