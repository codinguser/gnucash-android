package org.gnucash.android.model;

import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public final static List<AccountType> ASSET_ACCOUNT_TYPES = new ArrayList<AccountType>(Arrays.asList(AccountType.ASSET,
                                                                                                         AccountType.CASH,
                                                                                                         AccountType.BANK));

    public final static List<AccountType> LIABLITY_ACCOUNT_TYPES = new ArrayList<AccountType>(Arrays.asList(AccountType.LIABILITY,
                                                                                                         AccountType.CREDIT));

    public final static List<AccountType> EQUITY_ACCOUNT_TYPES = new ArrayList<AccountType>(Arrays.asList(AccountType.EQUITY));


    /**
     * Indicates that this type of normal balance the account type has
     * <p>To increase the value of an account with normal balance of credit, one would credit the account.
     * To increase the value of an account with normal balance of debit, one would likewise debit the account.</p>
     */
    private TransactionType mNormalBalance;

    AccountType(TransactionType normalBalance) {

        this.mNormalBalance = normalBalance;
    }

    AccountType() {
        this(TransactionType.CREDIT);
    }

    // TODO TW C 2020-03-06 : Enlever le static
    /**
     * Compute red/green color according to accountType and isCreditAmount
     *
     * @param isCreditAmount
     * @param accountType
     *
     * @return
     */
    @ColorInt
    public static int getAmountColor(final boolean isCreditAmount,
                                     final AccountType accountType) {

        AccountType tmpAccountType = ((accountType != null)
                                      ? accountType
                                      : AccountType.ASSET);

        @ColorRes final int colorRes;

        // Accounts for which
        final boolean debitCreditInvertedColorAccountType = tmpAccountType.isExpenseOrIncomeAccount() || tmpAccountType.isEquityAccount();

        if ((isCreditAmount && !debitCreditInvertedColorAccountType) || (!isCreditAmount && debitCreditInvertedColorAccountType)) {
            // Credit amount and account like Assets, Bank, Cash..., or Debit amount and account like Expense/Income

            // RED
            colorRes = R.color.debit_red;

        } else {
            // Credit amount and account like Expense/Income, or Debit amount and account like Assets, Bank, Cash...)

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

    public boolean isExpenseOrIncomeAccount() {

        return EXPENSE.equals(this) || INCOME.equals(this);
    }

}
