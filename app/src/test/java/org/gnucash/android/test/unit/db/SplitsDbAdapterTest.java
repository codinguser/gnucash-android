package org.gnucash.android.test.unit.db;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.test.unit.util.GnucashTestRunner;
import org.gnucash.android.test.unit.util.ShadowCrashlytics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Some tests for the splits database adapter
 */
@RunWith(GnucashTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowCrashlytics.class})
public class SplitsDbAdapterTest {

    private AccountsDbAdapter mAccountsDbAdapter;
    private TransactionsDbAdapter mTransactionsDbAdapter;
    private SplitsDbAdapter mSplitsDbAdapter;

    private Account mAccount;

    @Before
    public void setUp() throws Exception {
        mSplitsDbAdapter = SplitsDbAdapter.getInstance();
        mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();
        mAccountsDbAdapter = AccountsDbAdapter.getInstance();
        mAccount = new Account("Test account");
        mAccountsDbAdapter.addAccount(mAccount);
    }

    @Test
    public void shouldHaveAccountInDatabase(){
        Transaction transaction = new Transaction("");
        mTransactionsDbAdapter.addTransaction(transaction);

        Split split = new Split(Money.getZeroInstance(), "non-existent");
        split.setTransactionUID(transaction.getUID());
        mSplitsDbAdapter.addSplit(split);

        List<Split> splits = mSplitsDbAdapter.getSplitsForTransaction(transaction.getUID());
        assertThat(splits).isEmpty();
    }

    /**
     * When a split is added or modified to a transaction, we should set the
     */
    @Test
    public void addingSplitShouldUnsetExportedFlagOfTransaction(){
        Transaction transaction = new Transaction("");
        transaction.setExported(true);
        mTransactionsDbAdapter.addTransaction(transaction);

        assertThat(transaction.isExported()).isTrue();

        Split split = new Split(Money.getZeroInstance(), mAccount.getUID());
        split.setTransactionUID(transaction.getUID());
        mSplitsDbAdapter.addSplit(split);

        String isExported = mTransactionsDbAdapter.getAttribute(transaction.getUID(),
                DatabaseSchema.TransactionEntry.COLUMN_EXPORTED);
        assertThat(Boolean.parseBoolean(isExported)).isFalse();
    }

    @After
    public void tearDown(){
        mAccountsDbAdapter.deleteAllRecords();
    }
}
