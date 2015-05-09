package org.gnucash.android.test.unit.db;

import org.assertj.core.data.Index;
import org.gnucash.android.BuildConfig;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class AccountsDbAdapterTest{

	private static final String BRAVO_ACCOUNT_NAME = "Bravo";
	private static final String ALPHA_ACCOUNT_NAME = "Alpha";
    private AccountsDbAdapter mAccountsDbAdapter;
    private TransactionsDbAdapter mTransactionsDbAdapter;
    private SplitsDbAdapter mSplitsDbAdapter;

	@Before
	public void setUp() throws Exception {

        mSplitsDbAdapter = SplitsDbAdapter.getInstance();
        mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();
        mAccountsDbAdapter = AccountsDbAdapter.getInstance();
	}

    /**
     * Test that the list of accounts is always returned sorted alphabetically
     */
    @Test
	public void testAlphabeticalSorting(){
        Account first = new Account(ALPHA_ACCOUNT_NAME);
        Account second = new Account(BRAVO_ACCOUNT_NAME);
        //purposefully added the second after the first
        mAccountsDbAdapter.addAccount(second);
        mAccountsDbAdapter.addAccount(first);

		List<Account> accountsList = mAccountsDbAdapter.getAllAccounts();
		assertEquals(2, accountsList.size());
		//bravo was saved first, but alpha should be first alphabetically
        assertThat(accountsList).contains(first, Index.atIndex(0));
        assertThat(accountsList).contains(second, Index.atIndex(1));
	}

    @Test
    public void testAddAccountWithTransaction(){
        Account account1 = new Account("AlphaAccount");
        Account account2 = new Account("BetaAccount");
        Transaction transaction = new Transaction("MyTransaction");
        Split split = new Split(Money.getZeroInstance(), account1.getUID());
        transaction.addSplit(split);
        transaction.addSplit(split.createPair(account2.getUID()));

        long id1 = mAccountsDbAdapter.addAccount(account1);
        long id2 = mAccountsDbAdapter.addAccount(account2);

        assertTrue(id1 > 0);
        assertTrue(id2 > 0);
    }

    /**
     * Tests the foreign key constraint "ON DELETE CASCADE" between accounts and splits
     */
    @Test
    public void testDeletingAccountShouldDeleteSplits(){
        Account first = new Account(ALPHA_ACCOUNT_NAME);
        first.setUID(ALPHA_ACCOUNT_NAME);
        Account second = new Account(BRAVO_ACCOUNT_NAME);
        second.setUID(BRAVO_ACCOUNT_NAME);

        mAccountsDbAdapter.addAccount(second);
        mAccountsDbAdapter.addAccount(first);

        Transaction transaction = new Transaction("TestTrn");
        Split split = new Split(Money.getZeroInstance(), ALPHA_ACCOUNT_NAME);
        transaction.addSplit(split);
        transaction.addSplit(split.createPair(BRAVO_ACCOUNT_NAME));

        long id = mTransactionsDbAdapter.addTransaction(transaction);
        assertTrue(id > 0);

        mAccountsDbAdapter.deleteRecord(ALPHA_ACCOUNT_NAME);

        Transaction trxn = mTransactionsDbAdapter.getTransaction(transaction.getUID());
        assertEquals(1, trxn.getSplits().size());
        assertEquals(BRAVO_ACCOUNT_NAME, trxn.getSplits().get(0).getAccountUID());
    }

    /**
     * Tests that a ROOT account will always be created in the system
     */
    @Test
    public void shouldCreateDefaultRootAccount(){
        Account account = new Account("Some account");
        mAccountsDbAdapter.addAccount(account);
        assertThat(2).isEqualTo(mAccountsDbAdapter.getTotalAccountCount());

        List<Account> accounts = mAccountsDbAdapter.getSimpleAccountList();
        assertThat(accounts).extracting("mAccountType").contains(AccountType.ROOT);

    }

    @Test
    public void shouldUpdateFullNameAfterParentChange(){
        Account parent = new Account("Test");
        Account child = new Account("Child");

        mAccountsDbAdapter.addAccount(parent);
        mAccountsDbAdapter.addAccount(child);

        child.setParentUID(parent.getUID());
        mAccountsDbAdapter.addAccount(child);

        child = mAccountsDbAdapter.getAccount(child.getUID());
        parent = mAccountsDbAdapter.getAccount(parent.getUID());

        assertThat(mAccountsDbAdapter.getSubAccountCount(parent.getUID())).isEqualTo(1);
        assertThat(parent.getUID()).isEqualTo(child.getParentUID());

        assertThat(child.getFullName()).isEqualTo("Test:Child");
    }

	@After
	public void tearDown() throws Exception {
		mAccountsDbAdapter.deleteAllRecords();
	}
}
