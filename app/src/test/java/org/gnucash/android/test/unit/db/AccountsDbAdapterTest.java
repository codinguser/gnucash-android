package org.gnucash.android.test.unit.db;

import org.assertj.core.data.Index;
import org.gnucash.android.BuildConfig;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.ScheduledActionDbAdapter;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;
import org.gnucash.android.test.unit.util.GnucashTestRunner;
import org.gnucash.android.test.unit.util.ShadowCrashlytics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(GnucashTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowCrashlytics.class})
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
	public void shouldBeAlphabeticallySortedByDefault(){
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
    public void shouldAddAccountsToDatabase(){
        Account account1 = new Account("AlphaAccount");
        Account account2 = new Account("BetaAccount");
        Transaction transaction = new Transaction("MyTransaction");
        Split split = new Split(Money.getZeroInstance(), account1.getUID());
        transaction.addSplit(split);
        transaction.addSplit(split.createPair(account2.getUID()));
        account1.addTransaction(transaction);
        account2.addTransaction(transaction);

        long id1 = mAccountsDbAdapter.addAccount(account1);
        long id2 = mAccountsDbAdapter.addAccount(account2);

        assertThat(id1).isGreaterThan(0);
        assertThat(id2).isGreaterThan(0);

        assertThat(mTransactionsDbAdapter.getRecordsCount()).isEqualTo(1);
    }

    /**
     * Tests the foreign key constraint "ON DELETE CASCADE" between accounts and splits
     */
    @Test
    public void shouldDeleteSplitsWhenAccountDeleted(){
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
        assertThat(id).isGreaterThan(0);

        mAccountsDbAdapter.deleteRecord(ALPHA_ACCOUNT_NAME);

        Transaction trxn = mTransactionsDbAdapter.getTransaction(transaction.getUID());
        assertThat(trxn.getSplits().size()).isEqualTo(1);
        assertThat(trxn.getSplits().get(0).getAccountUID()).isEqualTo(BRAVO_ACCOUNT_NAME);
    }

    /**
     * Tests that a ROOT account will always be created in the system
     */
    @Test
    public void shouldCreateDefaultRootAccount(){
        Account account = new Account("Some account");
        mAccountsDbAdapter.addAccount(account);
        assertThat(mAccountsDbAdapter.getRecordsCount()).isEqualTo(2L);

        List<Account> accounts = mAccountsDbAdapter.getSimpleAccountList();
        assertThat(accounts).extracting("mAccountType").contains(AccountType.ROOT);

        String rootAccountUID = mAccountsDbAdapter.getOrCreateGnuCashRootAccountUID();
        assertThat(rootAccountUID).isEqualTo(accounts.get(1).getParentUID());
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

    @Test
    public void shouldAddTransactionsAndSplitsWhenAddingAccounts(){
        Account account = new Account("Test");
        mAccountsDbAdapter.addAccount(account);

        Transaction transaction = new Transaction("Test description");
        Split split = new Split(Money.getZeroInstance(), account.getUID());
        transaction.addSplit(split);
        Account account1 = new Account("Transfer account");
        transaction.addSplit(split.createPair(account1.getUID()));
        account1.addTransaction(transaction);

        mAccountsDbAdapter.addAccount(account1);

        assertThat(mTransactionsDbAdapter.getRecordsCount()).isEqualTo(1);
        assertThat(mSplitsDbAdapter.getRecordsCount()).isEqualTo(2);
        assertThat(mAccountsDbAdapter.getRecordsCount()).isEqualTo(3); //ROOT account automatically added

    }

    @Test
    public void shouldClearAllTablesWhenDeletingAllAccounts(){
        Account account = new Account("Test");
        Transaction transaction = new Transaction("Test description");
        Split split = new Split(Money.getZeroInstance(), account.getUID());
        transaction.addSplit(split);
        Account account2 = new Account("Transfer account");
        transaction.addSplit(split.createPair(account2.getUID()));

        mAccountsDbAdapter.addAccount(account);
        mAccountsDbAdapter.addAccount(account2);

        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.BACKUP);
        scheduledAction.setActionUID("Test-uid");
        ScheduledActionDbAdapter scheduledActionDbAdapter = ScheduledActionDbAdapter.getInstance();

        scheduledActionDbAdapter.addScheduledAction(scheduledAction);

        mAccountsDbAdapter.deleteAllRecords();

        assertThat(mAccountsDbAdapter.getRecordsCount()).isZero();
        assertThat(mTransactionsDbAdapter.getRecordsCount()).isZero();
        assertThat(mSplitsDbAdapter.getRecordsCount()).isZero();
        assertThat(scheduledActionDbAdapter.getRecordsCount()).isZero();
    }

    @Test
    public void simpleAccountListShouldNotContainTransactions(){
        Account account = new Account("Test");
        Transaction transaction = new Transaction("Test description");
        Split split = new Split(Money.getZeroInstance(), account.getUID());
        transaction.addSplit(split);
        Account account1 = new Account("Transfer");
        transaction.addSplit(split.createPair(account1.getUID()));

        mAccountsDbAdapter.addAccount(account);
        mAccountsDbAdapter.addAccount(account1);

        List<Account> accounts = mAccountsDbAdapter.getSimpleAccountList();
        for (Account testAcct : accounts) {
            assertThat(testAcct.getTransactionCount()).isZero();
        }
    }

    @Test
    public void shouldComputeAccountBalanceCorrectly(){
        Account account = new Account("Test", Currency.getInstance("USD"));
        account.setAccountType(AccountType.ASSET); //debit normal account balance
        Account transferAcct = new Account("Transfer");

        mAccountsDbAdapter.addAccount(account);
        mAccountsDbAdapter.addAccount(transferAcct);

        Transaction transaction = new Transaction("Test description");
        mTransactionsDbAdapter.addTransaction(transaction);
        Split split = new Split(new Money(BigDecimal.TEN, Currency.getInstance("USD")), account.getUID());
        split.setTransactionUID(transaction.getUID());
        split.setType(TransactionType.DEBIT);
        mSplitsDbAdapter.addSplit(split);

        split = new Split(new Money("4.99", "USD"), account.getUID());
        split.setTransactionUID(transaction.getUID());
        split.setType(TransactionType.DEBIT);
        mSplitsDbAdapter.addSplit(split);

        split = new Split(new Money("1.19", "USD"), account.getUID());
        split.setTransactionUID(transaction.getUID());
        split.setType(TransactionType.CREDIT);
        mSplitsDbAdapter.addSplit(split);

        split = new Split(new Money("3.49", "EUR"), account.getUID());
        split.setTransactionUID(transaction.getUID());
        split.setType(TransactionType.DEBIT);
        mSplitsDbAdapter.addSplit(split);

        split = new Split(new Money("8.39", "USD"), transferAcct.getUID());
        split.setTransactionUID(transaction.getUID());
        mSplitsDbAdapter.addSplit(split);

        //balance computation ignores the currency of the split
        Money balance = mAccountsDbAdapter.getAccountBalance(account.getUID());
        Money expectedBalance = new Money("17.29", "USD"); //EUR splits should be ignored

        assertThat(balance).isEqualTo(expectedBalance);
    }

    /**
     * Test creating an account hierarchy by specifying fully qualified name
     */
    @Test
    public void shouldCreateAccountHierarchy(){
        String uid = mAccountsDbAdapter.createAccountHierarchy("Assets:Current Assets:Cash in Wallet", AccountType.ASSET);

        List<Account> accounts = mAccountsDbAdapter.getAllAccounts();
        assertThat(accounts).hasSize(3);
        assertThat(accounts).extracting("mUID").contains(uid);
    }

    @Test
    public void shouldRecursivelyDeleteAccount(){
        Account account = new Account("Parent");
        Account account2 = new Account("Child");
        account2.setParentUID(account.getUID());

        Transaction transaction = new Transaction("Random");
        account2.addTransaction(transaction);

        Split split = new Split(Money.getZeroInstance(), account.getUID());
        transaction.addSplit(split);
        transaction.addSplit(split.createPair(account2.getUID()));

        mAccountsDbAdapter.addAccount(account);
        mAccountsDbAdapter.addAccount(account2);

        assertThat(mAccountsDbAdapter.getRecordsCount()).isEqualTo(3);
        assertThat(mTransactionsDbAdapter.getRecordsCount()).isEqualTo(1);
        assertThat(mSplitsDbAdapter.getRecordsCount()).isEqualTo(2);

        boolean result = mAccountsDbAdapter.recursiveDeleteAccount(mAccountsDbAdapter.getID(account.getUID()));
        assertThat(result).isTrue();

        assertThat(mAccountsDbAdapter.getRecordsCount()).isEqualTo(1); //the root account
        assertThat(mTransactionsDbAdapter.getRecordsCount()).isZero();
        assertThat(mSplitsDbAdapter.getRecordsCount()).isZero();

    }

	@After
	public void tearDown() throws Exception {
		mAccountsDbAdapter.deleteAllRecords();
	}
}
