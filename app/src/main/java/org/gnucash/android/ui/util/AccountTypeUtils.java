package org.gnucash.android.ui.util;

import android.content.Context;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.model.AccountType;

/**
 * Utilities for AccountType UI
 *
 * @author JeanGarf
 */
public class AccountTypeUtils {

    /**
     * Get the debit label customized for the account type
     *
     * @param accountType
     *          Account Type
     *
     * @return
     *          The debit label customized for the account type
     *
     * @author JeanGarf
     */
    public static String getLabelDebit(final AccountType accountType) {

        final String label;
        
        Context context = GnuCashApplication.getAppContext().getApplicationContext();

        switch (accountType) {
            
            case CASH:
                label = context.getString(R.string.label_receive); // DEBIT
                break;

            case BANK:
                label = context.getString(R.string.label_deposit); // DEBIT
                break;

            case CREDIT:
                // #876 Change according to GnuCash on Windows
                label = context.getString(R.string.label_payment); // DEBIT
                break;

            case ASSET:
            case EQUITY:
            case LIABILITY:
                // #876 Change according to GnuCash on Windows
                label = context.getString(R.string.label_decrease); // DEBIT
                break;

            case INCOME:
                // #876 Change according to GnuCash on Windows
                label = context.getString(R.string.label_charge); // DEBIT
                break;

            case EXPENSE:
                label = context.getString(R.string.label_expense); // DEBIT
                break;

            case PAYABLE:
                // #876 Change according to GnuCash on Windows
                label = context.getString(R.string.label_payment); // DEBIT
                break;

            case RECEIVABLE:
                label = context.getString(R.string.label_invoice); // DEBIT
                break;

            case STOCK:
            case MUTUAL:
                // #876 Change according to GnuCash on Windows
                label = context.getString(R.string.label_buy); // DEBIT
                break;

            case CURRENCY:
            case ROOT:
            default:
                // #876 Change according to GnuCash on Windows
                label = context.getString(R.string.label_debit); // DEBIT
                break;
        }

        return label;
    }

    /**
     * Get the credit label customized for the account type
     *
     * @param accountType
     *          Account Type
     *
     * @return
     *          The credit label customized for the account type
     *
     * @author JeanGarf
     */
    public static String getLabelCredit(final AccountType accountType) {

        final String label;

        Context context = GnuCashApplication.getAppContext().getApplicationContext();

        switch (accountType) {

            case CASH:
                label = context.getString(R.string.label_spend); // CREDIT
                break;

            case BANK:
                label = context.getString(R.string.label_withdrawal); // CREDIT
                break;

            case CREDIT:
                // #876 Change according to GnuCash on Windows
                label = context.getString(R.string.label_charge); // CREDIT
                break;

            case ASSET:
            case EQUITY:
            case LIABILITY:
                // #876 Change according to GnuCash on Windows
                label = context.getString(R.string.label_increase); // CREDIT
                break;

            case INCOME:
                // #876 Change according to GnuCash on Windows
                label = context.getString(R.string.label_income); // CREDIT
                break;

            case EXPENSE:
                label = context.getString(R.string.label_rebate); // CREDIT
                break;

            case PAYABLE:
                // #876 Change according to GnuCash on Windows
                label = context.getString(R.string.label_bill); // CREDIT
                break;

            case RECEIVABLE:
                label = context.getString(R.string.label_payment); // CREDIT
                break;

            case STOCK:
            case MUTUAL:
                // #876 Change according to GnuCash on Windows
                label = context.getString(R.string.label_sell); // CREDIT
                break;

            case CURRENCY:
            case ROOT:
            default:
                // #876 Change according to GnuCash on Windows
                label = context.getString(R.string.label_credit); // CREDIT
                break;
        }

        return label;
    }
}
