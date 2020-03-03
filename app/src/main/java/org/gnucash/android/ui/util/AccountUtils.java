package org.gnucash.android.ui.util;

import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.model.AccountType;

/**
 * Utilities for Accounts UI
 *
 * @author JeanGarf
 */
public class AccountUtils {

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
