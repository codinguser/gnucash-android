package org.gnucash.android.ui.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;
import org.gnucash.android.R;
import org.gnucash.android.db.TransactionsDbAdapter;

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
                .setTitle(R.string.title_confirm_delete).setMessage(R.string.delete_all_transactions_confirmation_message)
                .setPositiveButton(R.string.alert_dialog_ok_delete,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Context context = getDialog().getContext();
                                TransactionsDbAdapter transactionsDbAdapter = new TransactionsDbAdapter(context);
                                transactionsDbAdapter.deleteAllRecords();
                                transactionsDbAdapter.close();
                                Toast.makeText(context, R.string.toast_all_transactions_deleted, Toast.LENGTH_SHORT).show();

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
