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

package org.gnucash.android.test.unit.model;

import org.gnucash.android.model.Budget;
import org.gnucash.android.model.BudgetAmount;
import org.gnucash.android.model.Money;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for budgets
 */
public class BudgetTest {
    
    @Test
    public void addingBudgetAmount_shouldSetBudgetUID(){
        Budget budget = new Budget("Test");

        assertThat(budget.getBudgetAmounts()).isNotNull();
        BudgetAmount budgetAmount = new BudgetAmount(Money.getZeroInstance(), "test");
        budget.addBudgetAmount(budgetAmount);

        assertThat(budget.getBudgetAmounts()).hasSize(1);
        assertThat(budgetAmount.getBudgetUID()).isEqualTo(budget.getUID());
    }

    @Test
    public void shouldComputeAbsoluteAmountSum(){
        Budget budget = new Budget("Test");
        Money accountAmount = new Money("-20", "USD");
        BudgetAmount budgetAmount = new BudgetAmount(accountAmount, "account1");
        BudgetAmount budgetAmount1 = new BudgetAmount(new Money("10", "USD"), "account2");

        budget.addBudgetAmount(budgetAmount);
        budget.addBudgetAmount(budgetAmount1);

        assertThat(budget.getAmount("account1")).isEqualTo(accountAmount.abs());
        assertThat(budget.getAmountSum()).isEqualTo(new Money("30", "USD"));
    }
}
