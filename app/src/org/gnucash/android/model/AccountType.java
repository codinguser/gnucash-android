package org.gnucash.android.model;

/**
 * The type of account
 * This are the different types specified by the OFX format and
 * they are currently not used except for exporting
 */
public enum AccountType {
    CASH(TransactionType.DEBIT), BANK(TransactionType.DEBIT), CREDIT, ASSET(TransactionType.DEBIT), LIABILITY,
    INCOME, EXPENSE(TransactionType.DEBIT), PAYABLE, RECEIVABLE(TransactionType.DEBIT), EQUITY, CURRENCY,
    STOCK(TransactionType.DEBIT), MUTUAL(TransactionType.DEBIT), ROOT;

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

    public boolean hasDebitNormalBalance(){
        return mNormalBalance == TransactionType.DEBIT;
    }

    /**
     * Returns the type of normal balance this account possesses
     * @return TransactionType balance of the account type
     */
    public TransactionType getNormalBalanceType(){
        return mNormalBalance;
    }
}
