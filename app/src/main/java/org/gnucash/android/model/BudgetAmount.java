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
package org.gnucash.android.model;

/**
 * Model for budget amounts
 * @see Budget
 */
public class BudgetAmount extends BaseModel {

    private String mBudgetUID;
    private String mAccountUID;
    private long mPeriodNum;
    private Money mAmount;

    public BudgetAmount(String budgetUID, String accountUID){
        this.mBudgetUID = budgetUID;
        this.mAccountUID = accountUID;
    }

    public String getBudgetUID() {
        return mBudgetUID;
    }

    public void setBudgetUID(String budgetUID) {
        this.mBudgetUID = budgetUID;
    }

    public String getAccountUID() {
        return mAccountUID;
    }

    public void setAccountUID(String accountUID) {
        this.mAccountUID = accountUID;
    }

    public long getPeriodNum() {
        return mPeriodNum;
    }

    public void setPeriodNum(long periodNum) {
        this.mPeriodNum = periodNum;
    }

    public Money getAmount() {
        return mAmount;
    }

    public void setAmount(Money amount) {
        this.mAmount = amount;
    }
}
