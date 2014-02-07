package org.gnucash.android.export.qif;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.data.Account;

import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;

/**
 * @author Ngewi
 */
public class QifHelper {
    /*
    Prefixes for the QIF file
     */
    public static final String DATE_PREFIX          = "D";
    public static final String AMOUNT_PREFIX        = "T";
    public static final String MEMO_PREFIX          = "M";
    public static final String CATEGORY_PREFIX      = "L";
    public static final String SPLIT_MEMO_PREFIX    = "E";
    public static final String SPLIT_AMOUNT_PREFIX  = "$";
    public static final String SPLIT_CATEGORY_PREFIX    = "S";
    public static final String SPLIT_PERCENTAGE_PREFIX  = "%";
    public static final String ACCOUNT_HEADER           = "!Account";
    public static final String ACCOUNT_NAME_PREFIX      = "N";


    public static final String ENTRY_TERMINATOR = "^";
    private static final SimpleDateFormat QIF_DATE_FORMATTER = new SimpleDateFormat("yyyy/M/d");

    /**
     * Formats the date for QIF in the form d MMMM YYYY.
     * For example 25 January 2013
     * @param timeMillis Time in milliseconds since epoch
     * @return Formatted date from the time
     */
    public static final String formatDate(long timeMillis){
        Date date = new Date(timeMillis);
        return QIF_DATE_FORMATTER.format(date);
    }

    /**
     * Returns the QIF header for the transaction based on the account type.
     * By default, the QIF cash header is used
     * @param accountType AccountType of account
     * @return QIF header for the transactions
     */
    public static String getQifHeader(Account.AccountType accountType){
        switch (accountType) {
            case CASH:
                return "!Type:Cash";
            case BANK:
                return "!Type:Bank";
            case CREDIT:
                return "!Type:CCard";
            case ASSET:
                return "!Type:Oth A";
            case LIABILITY:
                return "!Type:Oth L";
            default:
                return "!Type:Cash";
        }
    }

    /**
     * Returns the imbalance account where to store transactions which are not double entry
     * @param currency Currency of the transaction
     * @return Imbalance account name
     */
    public static String getImbalanceAccountName(Currency currency){
        //TODO: localize this in the future
        return GnuCashApplication.getAppContext().getString(R.string.imbalance_account_name) + "-" + currency.getCurrencyCode();
    }
}
