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
package org.gnucash.android.ui.transaction.dialog;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.export.xml.GncXmlExporter;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.account.AccountsListFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockDialogFragment;
import org.gnucash.android.ui.widget.WidgetConfigurationActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a delete confirmation dialog for transactions
 * If the transaction ID parameter is 0, then all transactions will be deleted
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class TransactionsDeleteConfirmationDialogFragment extends SherlockDialogFragment {

    public static TransactionsDeleteConfirmationDialogFragment newInstance(int title, long id) {
        TransactionsDeleteConfirmationDialogFragment frag = new TransactionsDeleteConfirmationDialogFragment();
        Bundle args = new Bundle();
        args.putInt("title", title);
        args.putLong(UxArgument.SELECTED_TRANSACTION_IDS, id);
        frag.setArguments(args);
        return frag;
    }

    @Override    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int title = getArguments().getInt("title");
        final long rowId = getArguments().getLong(UxArgument.SELECTED_TRANSACTION_IDS);
        int message = rowId == 0 ? R.string.msg_delete_all_transactions_confirmation : R.string.msg_delete_transaction_confirmation;
        return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_delete)
                .setTitle(title).setMessage(message)
                .setPositiveButton(R.string.alert_dialog_ok_delete,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                TransactionsDbAdapter transactionsDbAdapter = GnuCashApplication.getTransactionDbAdapter();
                                if (rowId == 0) {
                                    GncXmlExporter.createBackup(); //create backup before deleting everything
                                    List<Transaction> openingBalances = new ArrayList<Transaction>();
                                    boolean preserveOpeningBalances = GnuCashApplication.shouldSaveOpeningBalances(false);
                                    if (preserveOpeningBalances) {
                                        openingBalances = GnuCashApplication.getAccountsDbAdapter().getAllOpeningBalanceTransactions();
                                    }

                                    transactionsDbAdapter.deleteAllRecords();

                                    if (preserveOpeningBalances) {
                                        transactionsDbAdapter.bulkAddTransactions(openingBalances);
                                    }
                                } else {
                                    transactionsDbAdapter.deleteRecord(rowId);
                                }
                                if (getTargetFragment() instanceof AccountsListFragment) {
                                    ((AccountsListFragment) getTargetFragment()).refresh();
                                }
                                WidgetConfigurationActivity.updateAllWidgets(getActivity());
                            }
                        }
                )
                .setNegativeButton(R.string.alert_dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dismiss();
                            }
                        }
                )
                .create();
    }
}
