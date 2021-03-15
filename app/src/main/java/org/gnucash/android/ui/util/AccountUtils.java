package org.gnucash.android.ui.util;

import android.support.v7.preference.PreferenceManager;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.model.AccountType;

/**
 * Utilities for Accounts UI
 *
 * @author JeanGarf
 */
public class AccountUtils {

    /**
     * Set text color according to account one
     * if preference about using colors in account list is true
     *
     * @param accountTextView
     *          View containing text field to colorize
     *
     * @param accountUID
     *          Account UID
     */
    public static void setAccountTextColor(final TextView accountTextView,
                                           final String accountUID) {

        if (accountTextView != null) {
            // accountTextView is not null

            // Get Preference about using colors in account list
            boolean prefShallUseColorInAccountList = PreferenceManager.getDefaultSharedPreferences(accountTextView.getContext())
                                                                      .getBoolean(accountTextView.getContext()
                                                                                                 .getString(R.string.key_use_color_in_account_list),
                                                                                  true);

            if (prefShallUseColorInAccountList) {
                // Want to use colors for Accounts

                // Get Account color
                int iColor = AccountsDbAdapter.getActiveAccountColorResource(accountUID);

                // Override color
                accountTextView.setTextColor(iColor);

            } else {
                // Do not want to use colors for Accounts

                // NTD
            }

        } else {
            // accountTextView is null

            // RAF
        }
    }

    /**
     * Build the where clause to select Accounts allowed for Transfer
     * for the given accountUID
     *
     * @param accountUID
     *          The account UID for which we want to collect account allowed for transfer
     *          May be null (to allow all non special accounts)
     *
     * @return
     *      the where clause
     *
     * @author JeanGarf
     */
    public static String getTransfertAccountWhereClause(final String accountUID) {

        return "("
               + DatabaseSchema.AccountEntry.COLUMN_UID
               + " != '"
               + ((accountUID != null) ? accountUID : "")
               + "' AND "
               + DatabaseSchema.AccountEntry.COLUMN_TYPE
               + " != '"
               + AccountType.ROOT.name()
               + "' AND "
               + DatabaseSchema.AccountEntry.COLUMN_PLACEHOLDER
               + " = 0"
               + " AND "
               + DatabaseSchema.AccountEntry.COLUMN_HIDDEN
               + " = 0"
               + ")";
    }
}
