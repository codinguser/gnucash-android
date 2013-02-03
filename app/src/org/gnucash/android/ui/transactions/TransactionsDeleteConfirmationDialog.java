package org.gnucash.android.ui.transactions;

import org.gnucash.android.R;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.ui.accounts.AccountsListFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockDialogFragment;

/**
 * Displays a delete confirmation dialog for transactions
 * If the transaction ID parameter is 0, then all transactions will be deleted
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class TransactionsDeleteConfirmationDialog extends SherlockDialogFragment {

    public static TransactionsDeleteConfirmationDialog newInstance(int title, long id) {
        TransactionsDeleteConfirmationDialog frag = new TransactionsDeleteConfirmationDialog();
        Bundle args = new Bundle();
        args.putInt("title", title);
        args.putLong(TransactionsListFragment.SELECTED_TRANSACTION_IDS, id);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int title = getArguments().getInt("title");
        final long rowId = getArguments().getLong(TransactionsListFragment.SELECTED_TRANSACTION_IDS);
        int message = rowId == 0 ? R.string.delete_all_transactions_confirmation_message : R.string.delete_transaction_confirmation_message;
        return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_delete)
                .setTitle(title).setMessage(message)
                .setPositiveButton(R.string.alert_dialog_ok_delete,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        	TransactionsDbAdapter adapter = new TransactionsDbAdapter(getSherlockActivity());                            
                            if (rowId == 0){
	                        	adapter.deleteAllTransactions();	                            
                            } else {
                            	adapter.deleteTransaction(rowId);
                            }
                            adapter.close();
                            if (getTargetFragment() instanceof AccountsListFragment){                            	
                            	((AccountsListFragment)getTargetFragment()).refreshList();
                            }                                                        
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
