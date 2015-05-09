package org.gnucash.android.test.unit.db;

import org.assertj.core.data.Index;
import org.gnucash.android.BuildConfig;
import org.gnucash.android.db.AccountsDbAdapter;
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

import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(GnucashTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowCrashlytics.class})
public class TransactionsDbAdapterTest {
	private static final String ALPHA_ACCOUNT_NAME  = "Alpha";
	private static final String BRAVO_ACCOUNT_NAME  = "Bravo";
	private static final Currency DEFAULT_CURRENCY	= Currency.getInstance(Money.DEFAULT_CURRENCY_CODE);

    private AccountsDbAdapter mAccountsDbAdapter;
    private TransactionsDbAdapter mTransactionsDbAdapter;
    private SplitsDbAdapter mSplitsDbAdapter;
	private Account alphaAccount;
	private Account bravoAccount;

	@Before
	public void setUp() throws Exception {
        mSplitsDbAdapter = SplitsDbAdapter.getInstance();
        mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();
        mAccountsDbAdapter = AccountsDbAdapter.getInstance();

		alphaAccount = new Account(ALPHA_ACCOUNT_NAME);
		bravoAccount = new Account(BRAVO_ACCOUNT_NAME);

		
		mAccountsDbAdapter.addAccount(bravoAccount);
		mAccountsDbAdapter.addAccount(alphaAccount);
	}

	@Test
	public void testTransactionsAreTimeSorted(){
		Transaction t1 = new Transaction("T800");
		t1.setTime(System.currentTimeMillis() - 10000);
		Split split = new Split(Money.getZeroInstance(), alphaAccount.getUID());
		t1.addSplit(split);
		t1.addSplit(split.createPair(bravoAccount.getUID()));

		Transaction t2 = new Transaction( "T1000");
		t2.setTime(System.currentTimeMillis());
		Split split2 = new Split(new Money("23.50"), bravoAccount.getUID());
		t2.addSplit(split2);
		t2.addSplit(split2.createPair(alphaAccount.getUID()));

		mTransactionsDbAdapter.addTransaction(t1);
		mTransactionsDbAdapter.addTransaction(t2);

		List<Transaction> transactionsList = mTransactionsDbAdapter.getAllTransactionsForAccount(alphaAccount.getUID());
		assertThat(transactionsList).contains(t2, Index.atIndex(0));
		assertThat(transactionsList).contains(t1, Index.atIndex(1));
	}

	@Test
	public void deletingTransactionsShouldDeleteSplits(){
		Transaction transaction = new Transaction("");
		Split split = new Split(Money.getZeroInstance(), alphaAccount.getUID());
		transaction.addSplit(split);
		mTransactionsDbAdapter.addTransaction(transaction);

		assertThat(mSplitsDbAdapter.getSplitsForTransaction(transaction.getUID())).hasSize(1);

		mTransactionsDbAdapter.deleteRecord(transaction.getUID());
		assertThat(mSplitsDbAdapter.getSplitsForTransaction(transaction.getUID())).hasSize(0);
	}

	/**
	 * Adding a split to a transaction should set the transaction UID of the split to the GUID of the transaction
	 */
	@Test
	public void addingSplitsShouldSetTransactionUID(){
		Transaction transaction = new Transaction("");
		assertThat(transaction.getCurrencyCode()).isEqualTo(Money.DEFAULT_CURRENCY_CODE);

		Split split = new Split(Money.getZeroInstance(), alphaAccount.getUID());
		assertThat(split.getTransactionUID()).isEmpty();

		transaction.addSplit(split);
		assertThat(split.getTransactionUID()).isEqualTo(transaction.getUID());
	}

/**
 //TODO: move this test to UI code. Autobalancing is done before the database level
	@Test
	public void shouldAutoBalanceTransactions(){
		Transaction t = new Transaction("Autobalance");
		Split split = new Split(new Money(BigDecimal.TEN, DEFAULT_CURRENCY), alphaAccount.getUID());
		t.addSplit(split);

		mTransactionsDbAdapter.addTransaction(t);

		Transaction balanced = mTransactionsDbAdapter.getTransaction(t.getUID());
		assertThat(balanced).isNotNull();
		assertThat(balanced.getSplits()).hasSize(2);

		String imbalanceUID = mAccountsDbAdapter.getImbalanceAccountUID(DEFAULT_CURRENCY);
		assertThat(balanced.getSplits()).extracting("mAccountUID").contains(imbalanceUID);
	}
**/
	@After
	public void tearDown() throws Exception {
		mAccountsDbAdapter.deleteAllRecords();
	}
}
