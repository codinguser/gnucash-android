package org.gnucash.android.model;

/**
 * Type of transaction, a credit or a debit
 */
public enum TransactionType {
    DEBIT, CREDIT;

    private TransactionType opposite;

    static {
        DEBIT.opposite = CREDIT;
        CREDIT.opposite = DEBIT;
    }

    public TransactionType negate() {
        return opposite;
    }
}
