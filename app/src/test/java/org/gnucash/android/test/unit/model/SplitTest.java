package org.gnucash.android.test.unit.model;

import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for Splits
 *
 * @author Ngewi
 */
public class SplitTest {

    @Test
    public void testAddingSplitToTransaction(){
        Split split = new Split(Money.getZeroInstance(), "Test");
        assertThat(split.getTransactionUID()).isEmpty();

        Transaction transaction = new Transaction("Random");
        transaction.addSplit(split);

        assertThat(transaction.getUID()).isEqualTo(split.getTransactionUID());

    }

    @Test
    public void testCloning(){
        Split split = new Split(new Money(BigDecimal.TEN, Currency.getInstance("EUR")), "random-account");
        split.setTransactionUID("terminator-trx");
        split.setType(TransactionType.CREDIT);

        Split clone1 = new Split(split, false);
        assertThat(clone1).isEqualTo(split);

        Split clone2 = new Split(split, true);
        assertThat(clone2.toCsv()).isEqualTo(split.toCsv());
    }

    /**
     * Tests that a split pair has the inverse transaction type as the origin split.
     * Everything else should be the same
     */
    @Test
    public void shouldCreateInversePair(){
        Split split = new Split(new Money("2"), "dummy");
        split.setType(TransactionType.CREDIT);
        split.setTransactionUID("random-trx");
        Split pair = split.createPair("test");

        assertThat(pair.getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(pair.getAmount()).isEqualTo(split.getAmount());
        assertThat(pair.getMemo()).isEqualTo(split.getMemo());
        assertThat(pair.getTransactionUID()).isEqualTo(split.getTransactionUID());
    }

    @Test
    public void shouldGenerateValidCsv(){
        Split split = new Split(new Money(BigDecimal.TEN, Currency.getInstance("EUR")), "random-account");
        split.setTransactionUID("terminator-trx");
        split.setType(TransactionType.CREDIT);

        assertThat(split.toCsv()).isEqualTo("10.00;EUR;random-account;terminator-trx;CREDIT");
    }

    @Test
    public void shouldParseCsv(){
        String csv = "4.90;USD;test-account;trx-action;DEBIT;Didn't you get the memo?";
        Split split = Split.parseSplit(csv);
        assertThat(split.getAmount()).isEqualTo(new Money("4.90", "USD"));
        assertThat(split.getTransactionUID()).isEqualTo("trx-action");
        assertThat(split.getAccountUID()).isEqualTo("test-account");
        assertThat(split.getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(split.getMemo()).isEqualTo("Didn't you get the memo?");
    }
}
