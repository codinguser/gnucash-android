package org.gnucash.android.test.unit.model;

import junit.framework.TestCase;

import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for Splits
 *
 * @author Ngewi
 */
public class SplitTest extends TestCase {

    public void testAddingSplitToTransaction(){
        Split split = new Split(Money.getZeroInstance(), "Test");
        assertThat(split.getTransactionUID()).isEmpty();

        Transaction transaction = new Transaction("Random");
        transaction.addSplit(split);

        assertThat(transaction.getUID()).isEqualTo(split.getTransactionUID());

    }

    public void testCsvGeneration(){

    }

    public void testParsingCsv(){

    }
}
