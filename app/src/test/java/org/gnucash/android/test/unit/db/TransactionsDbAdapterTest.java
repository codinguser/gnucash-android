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

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(GnucashTestRunner.class) //package is required so that resources can be found in dev mode
@Config(constants = BuildConfig.class, packageName = "org.gnucash.android", shadows = {ShadowCrashlytics.class})
public class TransactionsDbAdapterTest {
	private static final String ALPHA_ACCOUNT_NAME  = "Alpha";
	private static final String BRAVO_ACCOUNT_NAME  = "Bravo";
	private static final Currency DEFAULT_CURRENCY	= Currency.getInstance(Money.DEFAULT_CURRENCY_CODE);

    private AccountsDbAdapter mAccountsDbAdapter;
    private TransactionsDbAdapter mTransactionsDbAdapter;
    private SplitsDbAdapter mSplitsDbAdapter;
	private Account alphaAccount;
	private Account bravoAccount;

	private Split mTestSplit;

	@Before
	public void setUp() throws Exception {
        mSplitsDbAdapter = SplitsDbAdapter.getInstance();
        mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();
        mAccountsDbAdapter = AccountsDbAdapter.getInstance();

		alphaAccount = new Account(ALPHA_ACCOUNT_NAME);
		bravoAccount = new Account(BRAVO_ACCOUNT_NAME);

		mAccountsDbAdapter.addRecord(bravoAccount);
		mAccountsDbAdapter.addRecord(alphaAccount);

		mTestSplit = new Split(new Money(BigDecimal.TEN, DEFAULT_CURRENCY), alphaAccount.getUID());
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

		mTransactionsDbAdapter.addRecord(t1);
		mTransactionsDbAdapter.addRecord(t2);

		List<Transaction> transactionsList = mTransactionsDbAdapter.getAllTransactionsForAccount(alphaAccount.getUID());
		assertThat(transactionsList).contains(t2, Index.atIndex(0));
		assertThat(transactionsList).contains(t1, Index.atIndex(1));
	}

	@Test
	public void deletingTransactionsShouldDeleteSplits(){
		Transaction transaction = new Transaction("");
		Split split = new Split(Money.getZeroInstance(), alphaAccount.getUID());
		transaction.addSplit(split);
		mTransactionsDbAdapter.addRecord(transaction);

		assertThat(mSplitsDbAdapter.getSplitsForTransaction(transaction.getUID())).hasSize(1);

		mTransactionsDbAdapter.deleteRecord(transaction.getUID());
		assertThat(mSplitsDbAdapter.getSplitsForTransaction(transaction.getUID())).hasSize(0);
	}

	@Test
	public void shouldBalanceTransactionsOnSave(){
		Transaction transaction = new Transaction("Auto balance");
		Split split = new Split(new Money(BigDecimal.TEN, Currency.getInstance(Money.DEFAULT_CURRENCY_CODE)),
				alphaAccount.getUID());

		transaction.addSplit(split);

		mTransactionsDbAdapter.addRecord(transaction);

		Transaction trn = mTransactionsDbAdapter.getRecord(transaction.getUID());
		assertThat(trn.getSplits()).hasSize(2);

		String imbalanceAccountUID = mAccountsDbAdapter.getImbalanceAccountUID(Currency.getInstance(Money.DEFAULT_CURRENCY_CODE));
		assertThat(trn.getSplits()).extracting("mAccountUID").contains(imbalanceAccountUID);
	}

	@Test
	public void testComputeBalance(){
		Transaction transaction = new Transaction("Compute");
		Money firstSplitAmount = new Money("4.99", DEFAULT_CURRENCY.getCurrencyCode());
		Split split = new Split(firstSplitAmount, alphaAccount.getUID());
		transaction.addSplit(split);
		Money secondSplitAmount = new Money("3.50", DEFAULT_CURRENCY.getCurrencyCode());
		split = new Split(secondSplitAmount, bravoAccount.getUID());
		transaction.addSplit(split);

		mTransactionsDbAdapter.addRecord(transaction);

		//balance is negated because the CASH account has inverse normal balance
		transaction = mTransactionsDbAdapter.getRecord(transaction.getUID());
		Money savedBalance = transaction.getBalance(alphaAccount.getUID());
		assertThat(savedBalance).isEqualTo(firstSplitAmount.negate());

		savedBalance = transaction.getBalance(bravoAccount.getUID());
		assertThat(savedBalance.getNumerator()).isEqualTo(secondSplitAmount.negate().getNumerator());
		assertThat(savedBalance.getCurrency()).isEqualTo(secondSplitAmount.getCurrency());
	}

	@After
	public void tearDown() throws Exception {
		mAccountsDbAdapter.deleteAllRecords();
	}
}
