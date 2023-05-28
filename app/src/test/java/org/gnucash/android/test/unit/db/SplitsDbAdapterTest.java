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

import android.database.sqlite.SQLiteException;

import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.SplitsDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Some tests for the splits database adapter
 */
@RunWith(RobolectricTestRunner.class) //package is required so that resources can be found in dev mode
@Config(sdk = 21, packageName = "org.gnucash.android", shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
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
        mAccountsDbAdapter.addRecord(mAccount);
    }

    /**
     * Adding a split where the account does not exist in the database should generate an exception
     */
    @Test(expected = SQLiteException.class)
    public void shouldHaveAccountInDatabase(){
        Transaction transaction = new Transaction("");
        mTransactionsDbAdapter.addRecord(transaction);

        Split split = new Split(Money.getZeroInstance(), "non-existent");
        split.setTransactionUID(transaction.getUID());
        mSplitsDbAdapter.addRecord(split);
    }

    /**
     * Adding a split where the account does not exist in the database should generate an exception
     */
    @Test(expected = SQLiteException.class)
    public void shouldHaveTransactionInDatabase(){
        Transaction transaction = new Transaction(""); //not added to the db

        Split split = new Split(Money.getZeroInstance(), mAccount.getUID());
        split.setTransactionUID(transaction.getUID());
        mSplitsDbAdapter.addRecord(split);
    }

    @Test
    public void testAddSplit(){
        Transaction transaction = new Transaction("");
        mTransactionsDbAdapter.addRecord(transaction);

        Split split = new Split(Money.getZeroInstance(), mAccount.getUID());
        split.setTransactionUID(transaction.getUID());
        mSplitsDbAdapter.addRecord(split);

        List<Split> splits = mSplitsDbAdapter.getSplitsForTransaction(transaction.getUID());
        assertThat(splits).isNotEmpty();
        assertThat(splits.get(0).getUID()).isEqualTo(split.getUID());
    }

    /**
     * When a split is added or modified to a transaction, we should set the
     */
    @Test
    public void addingSplitShouldUnsetExportedFlagOfTransaction(){
        Transaction transaction = new Transaction("");
        transaction.setExported(true);
        mTransactionsDbAdapter.addRecord(transaction);

        assertThat(transaction.isExported()).isTrue();

        Split split = new Split(Money.getZeroInstance(), mAccount.getUID());
        split.setTransactionUID(transaction.getUID());
        mSplitsDbAdapter.addRecord(split);

        String isExported = mTransactionsDbAdapter.getAttribute(transaction.getUID(),
                DatabaseSchema.TransactionEntry.COLUMN_EXPORTED);
        assertThat(Boolean.parseBoolean(isExported)).isFalse();
    }

    @After
    public void tearDown(){
        mAccountsDbAdapter.deleteAllRecords();
    }
}
