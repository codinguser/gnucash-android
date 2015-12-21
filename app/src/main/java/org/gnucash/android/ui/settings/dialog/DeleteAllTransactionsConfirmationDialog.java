/*
 * Copyright (c) 2013 - 2014 Ngewi Fet <ngewif@gmail.com>
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
package org.gnucash.android.ui.settings.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.widget.Toast;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.export.xml.GncXmlExporter;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.ui.homescreen.WidgetConfigurationActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Confirmation dialog for deleting all transactions
 *
 * @author ngewif <ngewif@gmail.com>
 * @author Yongxin Wang <fefe.wyx@gmail.com>
 */
public class DeleteAllTransactionsConfirmationDialog extends DialogFragment {

    public static DeleteAllTransactionsConfirmationDialog newInstance() {
        DeleteAllTransactionsConfirmationDialog frag = new DeleteAllTransactionsConfirmationDialog();
        return frag;
    }

    @Override
    @NonNull public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_delete)
                .setTitle(R.string.title_confirm_delete).setMessage(R.string.msg_delete_all_transactions_confirmation)
                .setPositiveButton(R.string.alert_dialog_ok_delete,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                GncXmlExporter.createBackup();

                                Context context = getActivity();
                                AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();
                                List<Transaction> openingBalances = new ArrayList<>();
                                boolean preserveOpeningBalances = GnuCashApplication.shouldSaveOpeningBalances(false);
                                if (preserveOpeningBalances) {
                                    openingBalances = accountsDbAdapter.getAllOpeningBalanceTransactions();
                                }
                                TransactionsDbAdapter transactionsDbAdapter = TransactionsDbAdapter.getInstance();
                                int count = transactionsDbAdapter.deleteAllNonTemplateTransactions();
                                Log.i("DeleteDialog", String.format("Deleted %d transactions successfully", count));

                                if (preserveOpeningBalances) {
                                    transactionsDbAdapter.bulkAddRecords(openingBalances);
                                }
                                Toast.makeText(context, R.string.toast_all_transactions_deleted, Toast.LENGTH_SHORT).show();
                                WidgetConfigurationActivity.updateAllWidgets(getActivity());
                            }
                        }

                )
                .

                        setNegativeButton(R.string.alert_dialog_cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dismiss();
                                    }
                                }

                        )
                .

                        create();
    }
}
