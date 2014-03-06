package org.gnucash.android.model;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Currency;

/**
 * In double-entry accounting, each transaction has a double transaction that goes in the transfer account.
 * This class represents a view on the transaction from it's transfer account.
 * @author Jesse Shieh <jesse.shieh.pub@gmail.com>
 */
public class DoubleTransaction implements Transaction {
    private final Transaction original;
    private Account contextAccount;

    public DoubleTransaction(Transaction original, Account contextAccount) {
        this.original = original;
        this.contextAccount = contextAccount;
    }

    // TODO: Implement these methods. This class is currently unused. Once it is implemented, replace instances of
    // OriginalTransaction with DoubleTransaction where the transaction is being viewed from it's transfer account
    // rather than it's original account. Methods like getTransactionType should return the opposite of the
    // transaction's real value. I filled out setTransactionType and getTransactionType below to get started.

    @Override
    public void setAmount(Money negate) {

    }

    @Override
    public void setCurrency(Currency mCurrency) {

    }

    @Override
    public Money getAmount() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setName(String name) {

    }

    @Override
    public void setDescription(String string) {

    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public void setTime(long timeInMillis) {

    }

    @Override
    public long getTimeMillis() {
        return 0;
    }

    /**
     * Sets the transaction type. Because this is the double-account view of the transaction, the stored transaction
     * type is the opposite of the displayed transaction type.
     * @param type The displayed transaction type
     */
    @Override
    public void setTransactionType(TransactionType type) {
        original.setTransactionType(type.negate());
    }

    /**
     * Gets the transaction type. Because this is a double-account view of the transaction, the stored transaction
     * type is the opposed of the displayed transaction type.
     * @return the displayed transaction type
     */
    @Override
    public TransactionType getTransactionType() {
        return original.getTransactionType().negate();
    }

    @Override
    public void setUID(String transactionUID) {

    }

    @Override
    public String getUID() {
        return null;
    }

    @Override
    public String getDoubleEntryAccountUID() {
        return null;
    }

    @Override
    public void setDoubleEntryAccountUID(String doubleEntryAccountUID) {

    }

    @Override
    public String getAccountUID() {
        return null;
    }

    @Override
    public void setExported(boolean isExported) {

    }

    @Override
    public boolean isExported() {
        return false;
    }

    @Override
    public void setAccountUID(String accountUID) {

    }

    @Override
    public long getRecurrencePeriod() {
        return 0;
    }

    @Override
    public void setRecurrencePeriod(long recurrenceId) {

    }

    @Override
    public Element toOfx(Document doc, String accountUID) {
        return null;
    }

    @Override
    public String toQIF() {
        return null;
    }
}
