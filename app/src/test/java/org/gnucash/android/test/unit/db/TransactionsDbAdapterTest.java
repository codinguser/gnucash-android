/*
 * Copyright (c) 2015 Ngewi Fet <ngewif@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gnucash.android.test.unit.db;

import org.assertj.core.data.Index;
import org.gnucash.android.BuildConfig;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.SplitsDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.test.unit.testutil.GnucashTestRunner;
import org.gnucash.android.test.unit.testutil.ShadowCrashlytics;
import org.gnucash.android.test.unit.testutil.ShadowUserVoice;
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
@Config(constants = BuildConfig.class, sdk = 21, packageName = "org.gnucash.android", shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class TransactionsDbAdapterTest {
	private static final String ALPHA_ACCOUNT_NAME  = "Alpha";
	private static final String BRAVO_ACCOUNT_NAME  = "Bravo";
	private static final Commodity DEFAULT_CURRENCY	= Commodity.getInstance(Money.DEFAULT_CURRENCY_CODE);

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
		Split split2 = new Split(new Money("23.50", DEFAULT_CURRENCY.getCurrencyCode()), bravoAccount.getUID());
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
		Split split = new Split(new Money(BigDecimal.TEN, DEFAULT_CURRENCY),
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
