package org.gnucash.android.ui.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;
import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.export.xml.GncXmlExporter;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.ui.widget.WidgetConfigurationActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (c) 2013 - gnucash-android
 *
 * Confirmation dialog for deleting all transactions
 * @author ngewif <ngewif@gmail.com>
 */
public class DeleteAllTransacationsConfirmationDialog extends DialogFragment {

    public static DeleteAllTransacationsConfirmationDialog newInstance() {
        DeleteAllTransacationsConfirmationDialog frag = new DeleteAllTransacationsConfirmationDialog();
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_delete)
                .setTitle(R.string.title_confirm_delete).setMessage(R.string.msg_delete_all_transactions_confirmation)
                .setPositiveButton(R.string.alert_dialog_ok_delete,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                GncXmlExporter.createBackup();

                                Context context = getDialog().getContext();
                                AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(context);
                                List<Transaction> openingBalances = new ArrayList<Transaction>();
                                boolean preserveOpeningBalances = GnuCashApplication.shouldSaveOpeningBalances(false);
                                if (preserveOpeningBalances) {
                                    openingBalances = accountsDbAdapter.getAllOpeningBalanceTransactions();
                                    accountsDbAdapter.close();
                                }
                                TransactionsDbAdapter transactionsDbAdapter = new TransactionsDbAdapter(context);
                                transactionsDbAdapter.deleteAllRecords();

                                if (preserveOpeningBalances) {
                                    transactionsDbAdapter.bulkAddTransactions(openingBalances);
                                }
                                transactionsDbAdapter.close();
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
