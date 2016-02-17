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

import android.support.annotation.NonNull;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BudgetAmountsDbAdapter;
import org.gnucash.android.db.adapter.BudgetsDbAdapter;
import org.gnucash.android.db.adapter.RecurrenceDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Budget;
import org.gnucash.android.model.BudgetAmount;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.PeriodType;
import org.gnucash.android.model.Recurrence;
import org.gnucash.android.test.unit.testutil.GnucashTestRunner;
import org.gnucash.android.test.unit.testutil.ShadowCrashlytics;
import org.gnucash.android.test.unit.testutil.ShadowUserVoice;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the budgets database adapter
 */
@RunWith(GnucashTestRunner.class) //package is required so that resources can be found in dev mode
@Config(constants = BuildConfig.class, sdk = 21, packageName = "org.gnucash.android", shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class BudgetsDbAdapterTest {

    private BudgetsDbAdapter mBudgetsDbAdapter;
    private RecurrenceDbAdapter mRecurrenceDbAdapter;
    private BudgetAmountsDbAdapter mBudgetAmountsDbAdapter;
    private AccountsDbAdapter mAccountsDbAdapter;

    private Account mAccount;
    private Account mSecondAccount;

    @Before
    public void setUp(){
        mAccountsDbAdapter      = AccountsDbAdapter.getInstance();
        mBudgetsDbAdapter       = BudgetsDbAdapter.getInstance();
        mBudgetAmountsDbAdapter = BudgetAmountsDbAdapter.getInstance();
        mRecurrenceDbAdapter    = RecurrenceDbAdapter.getInstance();

        mAccount = new Account("Budgeted account");
        mSecondAccount = new Account("Another account");
        mAccountsDbAdapter.addRecord(mAccount);
        mAccountsDbAdapter.addRecord(mSecondAccount);
    }

    @After
    public void tearDown(){
        mBudgetsDbAdapter.deleteAllRecords();
        mBudgetAmountsDbAdapter.deleteAllRecords();
        mRecurrenceDbAdapter.deleteAllRecords();
    }

    @Test
    public void testAddingBudget(){
        assertThat(mBudgetsDbAdapter.getRecordsCount()).isZero();
        assertThat(mBudgetAmountsDbAdapter.getRecordsCount()).isZero();
        assertThat(mRecurrenceDbAdapter.getRecordsCount()).isZero();

        Budget budget = new Budget("Test");
        budget.addBudgetAmount(new BudgetAmount(Money.getZeroInstance(), mAccount.getUID()));
        budget.addBudgetAmount(new BudgetAmount(new Money("10", Money.DEFAULT_CURRENCY_CODE), mSecondAccount.getUID()));
        Recurrence recurrence = new Recurrence(PeriodType.MONTH);
        budget.setRecurrence(recurrence);

        mBudgetsDbAdapter.addRecord(budget);
        assertThat(mBudgetsDbAdapter.getRecordsCount()).isEqualTo(1);
        assertThat(mBudgetAmountsDbAdapter.getRecordsCount()).isEqualTo(2);
        assertThat(mRecurrenceDbAdapter.getRecordsCount()).isEqualTo(1);

        budget.getBudgetAmounts().clear();
        BudgetAmount budgetAmount = new BudgetAmount(new Money("5", Money.DEFAULT_CURRENCY_CODE), mAccount.getUID());
        budget.addBudgetAmount(budgetAmount);
        mBudgetsDbAdapter.addRecord(budget);

        assertThat(mBudgetAmountsDbAdapter.getRecordsCount()).isEqualTo(1);
        assertThat(mBudgetAmountsDbAdapter.getAllRecords().get(0).getUID()).isEqualTo(budgetAmount.getUID());
    }

    /**
     * Test that when bulk adding budgets, all the associated budgetAmounts and recurrences are saved
     */
    @Test
    public void testBulkAddBudgets(){
        assertThat(mBudgetsDbAdapter.getRecordsCount()).isZero();
        assertThat(mBudgetAmountsDbAdapter.getRecordsCount()).isZero();
        assertThat(mRecurrenceDbAdapter.getRecordsCount()).isZero();

        List<Budget> budgets = bulkCreateBudgets();

        mBudgetsDbAdapter.bulkAddRecords(budgets);

        assertThat(mBudgetsDbAdapter.getRecordsCount()).isEqualTo(2);
        assertThat(mBudgetAmountsDbAdapter.getRecordsCount()).isEqualTo(3);
        assertThat(mRecurrenceDbAdapter.getRecordsCount()).isEqualTo(2);

    }

    @Test
    public void testGetAccountBudgets(){
        mBudgetsDbAdapter.bulkAddRecords(bulkCreateBudgets());

        List<Budget> budgets = mBudgetsDbAdapter.getAccountBudgets(mAccount.getUID());
        assertThat(budgets).hasSize(2);

        assertThat(mBudgetsDbAdapter.getAccountBudgets(mSecondAccount.getUID())).hasSize(1);
    }

    @NonNull
    private List<Budget> bulkCreateBudgets() {
        List<Budget> budgets = new ArrayList<>();
        Budget budget = new Budget("", new Recurrence(PeriodType.MONTH));
        budget.addBudgetAmount(new BudgetAmount(Money.getZeroInstance(), mAccount.getUID()));
        budgets.add(budget);

        budget = new Budget("Random", new Recurrence(PeriodType.WEEK));
        budget.addBudgetAmount(new BudgetAmount(new Money("10.50", Money.DEFAULT_CURRENCY_CODE), mAccount.getUID()));
        budget.addBudgetAmount(new BudgetAmount(new Money("32.35", Money.DEFAULT_CURRENCY_CODE), mSecondAccount.getUID()));

        budgets.add(budget);
        return budgets;
    }

    @Test(expected = NullPointerException.class)
    public void savingBudget_shouldRequireExistingAccount(){
        Budget budget = new Budget("");
        budget.addBudgetAmount(new BudgetAmount(Money.getZeroInstance(), "unknown-account"));

        mBudgetsDbAdapter.addRecord(budget);
    }

    @Test(expected = NullPointerException.class)
    public void savingBudget_shouldRequireRecurrence(){
        Budget budget = new Budget("");
        budget.addBudgetAmount(new BudgetAmount(Money.getZeroInstance(), mAccount.getUID()));

        mBudgetsDbAdapter.addRecord(budget);
    }

    @Test(expected = IllegalArgumentException.class)
    public void savingBudget_shouldRequireBudgetAmount(){
        Budget budget = new Budget("");
        budget.setRecurrence(new Recurrence(PeriodType.MONTH));

        mBudgetsDbAdapter.addRecord(budget);
    }
}
