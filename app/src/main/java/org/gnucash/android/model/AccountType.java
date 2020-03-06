package org.gnucash.android.model;

import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;

/**
 * The type of account
 * This are the different types specified by the OFX format and
 * they are currently not used except for exporting
 */
public enum AccountType {
    CASH(TransactionType.DEBIT),
    BANK(TransactionType.DEBIT),
    CREDIT,
    ASSET(TransactionType.DEBIT),
    LIABILITY,
    INCOME,
    EXPENSE(TransactionType.DEBIT),
    PAYABLE,
    RECEIVABLE(TransactionType.DEBIT),
    EQUITY,
    CURRENCY,
    STOCK(TransactionType.DEBIT),
    MUTUAL(TransactionType.DEBIT),
    TRADING,
    ROOT;

    /**
     * Indicates that this type of normal balance the account type has
     * <p>To increase the value of an account with normal balance of credit, one would credit the account.
     * To increase the value of an account with normal balance of debit, one would likewise debit the account.</p>
     */
    private TransactionType mNormalBalance = TransactionType.CREDIT;

    AccountType(TransactionType normalBalance){
        this.mNormalBalance = normalBalance;
    }

    AccountType() {
        //nothing to see here, move along
    }

    // TODO TW C 2020-03-06 : Enlever le static

    /**
     * Compute red/green color according to accountType and isCredit
     *
     * @param isCredit
     * @param accountType
     *
     * @return
     */
    @ColorInt
    public static int getAmountColor(final boolean isCredit,
                                     final AccountType accountType) {

        AccountType tmpAccountType = ((accountType != null)
                                      ? accountType
                                      : AccountType.ASSET);

        @ColorRes final int colorRes;

        // TODO TW C 2020-03-06 : Trouver un meilleur nom
        final boolean specialAccountType = tmpAccountType.isEquityAccount() || tmpAccountType.isResultAccount();

        if ((!specialAccountType && isCredit) || (specialAccountType && !isCredit)) {
            // TODO TW C 2020-03-02 : commenter
            // CREDIT

            // RED
            colorRes = R.color.debit_red;

        } else {
            // DEBIT

            // GREEN
            colorRes = R.color.credit_green;
        }

        return GnuCashApplication.getAppContext()
                                 .getResources()
                                 .getColor(colorRes);
    }

    public boolean hasDebitNormalBalance() {

        return mNormalBalance == TransactionType.DEBIT;
    }

    /**
     * Returns the type of normal balance this account possesses
     * @return TransactionType balance of the account type
     */
    public TransactionType getNormalBalanceType() {

        return mNormalBalance;
    }

    public boolean isAssetAccount() {

        return ASSET.equals(this) || BANK.equals(this) || CASH.equals(this);
    }

    public boolean isEquityAccount() {

        return EQUITY.equals(this);
    }

    // TODO TW C 2020-03-03 : A renommer en anglais
    public boolean isResultAccount() {

        return EXPENSE.equals(this) || INCOME.equals(this);
    }

}
