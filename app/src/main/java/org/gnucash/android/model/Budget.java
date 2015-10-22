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

import android.support.annotation.NonNull;
import android.util.Log;

import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

/**
 * Budgets model
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class Budget extends BaseModel {

    private String mName;
    private String mDescription;
    private Recurrence mRecurrence;
    private List<BudgetAmount> mBudgetAmounts = new ArrayList<>();
    private long mNumberOfPeriods = 12; //default to 12 periods per year

    /**
     * Default constructor
     */
    public Budget(){
        //nothing to see here, move along
    }

    /**
     * Overloaded constructor.
     * Initializes the name and amount of this budget
     * @param name String name of the budget
     */
    public Budget(@NonNull String name){
        this.mName = name;
    }

    /**
     * Returns the name of the budget
     * @return name of the budget
     */
    public String getName() {
        return mName;
    }

    /**
     * Sets the name of the budget
     * @param name String name of budget
     */
    public void setName(@NonNull String name) {
        this.mName = name;
    }

    /**
     * Returns the description of the budget
     * @return String description of budget
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * Sets the description of the budget
     * @param description String description
     */
    public void setDescription(String description) {
        this.mDescription = description;
    }

    /**
     * Returns the recurrence for this budget
     * @return Recurrence object for this budget
     */
    public Recurrence getRecurrence() {
        return mRecurrence;
    }

    /**
     * Set the recurrence pattern for this budget
     * @param recurrence Recurrence object
     */
    public void setRecurrence(@NonNull Recurrence recurrence) {
        this.mRecurrence = recurrence;
    }

    /**
     * Return list of budget amounts associated with this budget
     * @return List of budget amounts
     */
    public List<BudgetAmount> getBudgetAmounts() {
        return mBudgetAmounts;
    }

    /**
     * Set the list of budget amounts
     * @param budgetAmounts List of budget amounts
     */
    public void setBudgetAmounts(List<BudgetAmount> budgetAmounts) {
        this.mBudgetAmounts = budgetAmounts;
    }

    /**
     * Adds a BudgetAmount to this budget
     * @param budgetAmount Budget amount
     */
    public void addBudgetAmount(BudgetAmount budgetAmount){
        budgetAmount.setBudgetUID(getUID());
        mBudgetAmounts.add(budgetAmount);
    }

    /**
     * Returns the budget amount for a specific account
     * @param accountUID GUID of the account
     * @return Money amount of the budget or null if the budget has no amount for the account
     */
    public Money getAmount(@NonNull String accountUID){
        //TODO: add consideration for the current period
        for (BudgetAmount budgetAmount : mBudgetAmounts) {
            if (budgetAmount.getAccountUID().equals(accountUID))
                return budgetAmount.getAmount();
        }
        return null;
    }

    /**
     * Returns the sum of all budget amounts in this budget
     * <p><b>NOTE:</b> This method ignores budgets of accounts which are in different currencies</p>
     * @return Money sum of all amounts
     */
    public Money getAmountSum(){
        Money sum = null; //we explicitly allow this null instead of a money instance, because this method should never return null for a budget
        for (BudgetAmount budgetAmount : mBudgetAmounts) {
            if (sum == null){
                sum = budgetAmount.getAmount();
            } else {
                try {
                    sum = sum.add(budgetAmount.getAmount());
                } catch (Money.CurrencyMismatchException ex){
                    Log.i(getClass().getSimpleName(), "Skip some budget amounts with different currency");
                }
            }
        }
        return sum;
    }

    /**
     * Returns the number of periods covered by this budget
     * @return Number of periods
     */
    public long getNumberOfPeriods() {
        return mNumberOfPeriods;
    }

    /**
     * Returns the timestamp of the start of current period of the budget
     * @return Start timestamp in milliseconds
     */
    public long getStartofCurrentPeriod(){
        LocalDateTime localDate = new LocalDateTime();
        int interval = mRecurrence.getPeriodType().getMultiplier();
        //// FIXME: 16.10.2015 consider the interval
        switch (mRecurrence.getPeriodType()){
            case DAY:
                localDate = localDate.millisOfDay().withMinimumValue();
                break;
            case WEEK:
                localDate = localDate.dayOfWeek().withMinimumValue().millisOfDay().withMinimumValue();
                break;
            case MONTH:
                localDate = localDate.dayOfMonth().withMinimumValue().millisOfDay().withMinimumValue();
                break;
            case YEAR:
                localDate = localDate.dayOfYear().withMinimumValue().millisOfDay().withMinimumValue();
                break;
        }
        return localDate.toDate().getTime();
    }

    /**
     * Returns the end timestamp of the current period
     * @return End timestamp in milliseconds
     */
    public long getEndOfCurrentPeriod(){
        LocalDateTime localDate = new LocalDateTime();
        int interval = mRecurrence.getPeriodType().getMultiplier();
        //// FIXME: 16.10.2015 Consider the interval
        switch (mRecurrence.getPeriodType()){
            case DAY:
                localDate = localDate.millisOfDay().withMaximumValue();
                break;
            case WEEK:
                localDate = localDate.dayOfWeek().withMaximumValue().millisOfDay().withMaximumValue();
                break;
            case MONTH:
                localDate = localDate.dayOfMonth().withMaximumValue().millisOfDay().withMaximumValue();
                break;
            case YEAR:
                localDate = localDate.dayOfYear().withMaximumValue().millisOfDay().withMaximumValue();
                break;
        }
        return localDate.toDate().getTime();
    }

    /**
     * Sets the number of periods for the budget
     * @param numberOfPeriods Number of periods as long
     */
    public void setNumberOfPeriods(long numberOfPeriods) {
        this.mNumberOfPeriods = numberOfPeriods;
    }
}
