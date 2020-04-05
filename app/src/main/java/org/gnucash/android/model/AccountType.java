package org.gnucash.android.model;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.ui.settings.PreferenceActivity;

import java.math.BigDecimal;
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

    //
    // Preference Key (must be the same as in donottranslate.xml)
    //

    public static final String KEY_USE_NORMAL_BALANCE_EXPENSE = "KEY_USE_NORMAL_BALANCE_EXPENSE";
    public static final String KEY_USE_NORMAL_BALANCE_INCOME  = "KEY_USE_NORMAL_BALANCE_INCOME";
    public static final String KEY_DEBIT                      = "KEY_DEBIT";
    public static final String KEY_CREDIT                     = "KEY_CREDIT";


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

    /**
     * @return
     */
    public TransactionType getDefaultTransactionType() {


        String transactionTypePref = PreferenceActivity.getActiveBookSharedPreferences()
                                                       .getString(GnuCashApplication.getAppContext()
                                                                                    .getString(R.string.key_default_transaction_type),
                                                                  KEY_USE_NORMAL_BALANCE_EXPENSE);

        final TransactionType transactionType;

        if (KEY_USE_NORMAL_BALANCE_EXPENSE.equals(transactionTypePref)) {
            // Use Normal Balance (Expense Mode)

            // Use Account Normal Balance as default, except for Asset which are CREDIT by default
            transactionType = isAssetAccount()
                              ? TransactionType.CREDIT
                              : getNormalBalanceType();

        } else if (KEY_USE_NORMAL_BALANCE_INCOME.equals(transactionTypePref)) {
            // Use Normal Balance (Income Mode)

            // Use Account Normal Balance as default
            transactionType = getNormalBalanceType();

        } else {
            // Not Automatic mode

            // Convert String to Enum
            transactionType = KEY_DEBIT.equals(transactionTypePref)
                              ? TransactionType.DEBIT
                              : TransactionType.CREDIT;
        }
        return transactionType;
    }


    /**
     * Display the balance of a transaction in a text view and format the text color to match the sign of the amount
     *
     * @param balanceTextView {@link android.widget.TextView} where balance is to be displayed
     * @param balance {@link org.gnucash.android.model.Money} balance (>0 or <0) to display
     */
    public void displayBalance(final TextView balanceTextView,
                               final Money balance,
                               final boolean shallDisplayAbsValue) {

        //
        // Display amount
        //

        balanceTextView.setText(shallDisplayAbsValue
                                ? balance.abs()
                                         .formattedString()
                                : balance.formattedString());

        //
        // Define amount color
        //

        @ColorInt int fontColor;

        if (balance.asBigDecimal()
                   .compareTo(BigDecimal.ZERO) == 0) {
            // balance is null

            Context context = GnuCashApplication.getAppContext();

            fontColor = context.getResources()
                               .getColor(android.R.color.black);

        } else {
            // balance is not null

            final boolean isCreditBalance = balance.isNegative();

            fontColor = getAmountColor(isCreditBalance);
        }

        balanceTextView.setTextColor(fontColor);
    }

    /**
     * Display the balance of a transaction in a text view and format the text color to match the sign of the amount
     *
     * @param balanceTextView
     *         {@link android.widget.TextView} where balance is to be displayed
     * @param balance
     *         {@link org.gnucash.android.model.Money} balance (>0 or <0) to display
     */
    public void displayBalance(final TextView balanceTextView,
                               final Money balance) {

        displayBalance(balanceTextView,
                       balance,
                       false);
    }
    /**
     * Compute red/green color according to accountType and isCreditAmount
     *
     * @param isCreditAmount
     *
     * @return
     */
    @ColorInt
    public int getAmountColor(final boolean isCreditAmount) {

        @ColorRes final int colorRes;

        // Accounts for which
        final boolean debitCreditInvertedColorAccountType = isExpenseOrIncomeAccount() || isEquityAccount();

        if ((isCreditAmount && !debitCreditInvertedColorAccountType) || (!isCreditAmount && debitCreditInvertedColorAccountType)) {
            // Credit amount and account like Assets, Bank, Cash..., or Debit amount and account like Expense/Income

            if (!isExpenseOrIncomeAccount()) {
                // It is not an Expense/Income account

                // RED
                colorRes = R.color.debit_red;

            } else {
                // It is an Expense/Income account

                // PURPLE
                colorRes = R.color.debit_expense_income;
            }

        } else {
            // Credit amount and account like Expense/Income, or Debit amount and account like Assets, Bank, Cash...)

            if (!isExpenseOrIncomeAccount()) {
                // It is not an Expense/Income account

                // GREEN
                colorRes = R.color.credit_green;

            } else {
                // It is an Expense/Income account

                // BLUE
                colorRes = R.color.credit_expense_income;
            }

        }

        return GnuCashApplication.getAppContext()
                                 .getResources()
                                 .getColor(colorRes);
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

    public boolean hasDebitNormalBalance() {

        return mNormalBalance == TransactionType.DEBIT;
    }

    /**
     * Returns balance with the right signum to be displayed
     *
     * A Debit is always the addition of a positive amount
     * A credit is always the substraction of a positive amount
     * The balance is always Debit - Credit
     * Therefore :
     * Debit > Credit => balance is > 0
     * Debit < Credit => balance is < 0
     *
     * But for display, habit is to reduce the use of negative numbers
     * To achieve this, for accounts which USUALLY have :
     * Debit > Credit => compute balance as usual
     * Debit < Credit => negate balance
     *
     * @return
     *      balance with the right signum to be displayed
     */
    public Money getBalanceWithSignumForDisplay(final Money balance) {

        final Money balanceWithSignumForDisplay;

        if (hasDebitNormalBalance()) {
            // Account usually debitor

            balanceWithSignumForDisplay = balance;

        } else {
            // account usually creditor

            // Negate
            balanceWithSignumForDisplay = balance.negate();
        }

        return balanceWithSignumForDisplay;
    }


    //
    // Getters/Setters
    //

    /**
     * Returns the type of normal balance this account possesses
     * @return TransactionType balance of the account type
     */
    public TransactionType getNormalBalanceType() {

        return mNormalBalance;
    }

}
