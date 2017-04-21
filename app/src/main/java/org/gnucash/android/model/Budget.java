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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public Budget(@NonNull String name, @NonNull Recurrence recurrence){
        this.mName = name;
        this.mRecurrence = recurrence;
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
        for (BudgetAmount budgetAmount : mBudgetAmounts) {
            budgetAmount.setBudgetUID(getUID());
        }
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
        for (BudgetAmount budgetAmount : mBudgetAmounts) {
            if (budgetAmount.getAccountUID().equals(accountUID))
                return budgetAmount.getAmount();
        }
        return null;
    }

    /**
     * Returns the budget amount for a specific account and period
     * @param accountUID GUID of the account
     * @param periodNum Budgeting period, zero-based index
     * @return Money amount or zero if no matching {@link BudgetAmount} is found for the period
     */
    public Money getAmount(@NonNull String accountUID, int periodNum){
        for (BudgetAmount budgetAmount : mBudgetAmounts) {
            if (budgetAmount.getAccountUID().equals(accountUID)
                    && (budgetAmount.getPeriodNum() == periodNum || budgetAmount.getPeriodNum() == -1)){
                return budgetAmount.getAmount();
            }
        }
        return Money.getZeroInstance();
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
                    sum = sum.add(budgetAmount.getAmount().abs());
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
        switch (mRecurrence.getPeriodType()){
            //TODO:HOUR
            case DAY:
                localDate = localDate.millisOfDay().withMinimumValue().plusDays(interval);
                break;
            case WEEK:
                localDate = localDate.dayOfWeek().withMinimumValue().minusDays(interval);
                break;
            case MONTH:
                localDate = localDate.dayOfMonth().withMinimumValue().minusMonths(interval);
                break;
            case YEAR:
                localDate = localDate.dayOfYear().withMinimumValue().minusYears(interval);
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
        switch (mRecurrence.getPeriodType()){
            //TODO:HOUR
            case DAY:
                localDate = localDate.millisOfDay().withMaximumValue().plusDays(interval);
                break;
            case WEEK:
                localDate = localDate.dayOfWeek().withMaximumValue().plusWeeks(interval);
                break;
            case MONTH:
                localDate = localDate.dayOfMonth().withMaximumValue().plusMonths(interval);
                break;
            case YEAR:
                localDate = localDate.dayOfYear().withMaximumValue().plusYears(interval);
                break;
        }
        return localDate.toDate().getTime();
    }

    public long getStartOfPeriod(int periodNum){
        LocalDateTime localDate = new LocalDateTime(mRecurrence.getPeriodStart().getTime());
        int interval = mRecurrence.getPeriodType().getMultiplier() * periodNum;
        switch (mRecurrence.getPeriodType()){
            //TODO:HOUR
            case DAY:
                localDate = localDate.millisOfDay().withMinimumValue().plusDays(interval);
                break;
            case WEEK:
                localDate = localDate.dayOfWeek().withMinimumValue().minusDays(interval);
                break;
            case MONTH:
                localDate = localDate.dayOfMonth().withMinimumValue().minusMonths(interval);
                break;
            case YEAR:
                localDate = localDate.dayOfYear().withMinimumValue().minusYears(interval);
                break;
        }
        return localDate.toDate().getTime();
    }

    /**
     * Returns the end timestamp of the period
     * @param periodNum Number of the period
     * @return End timestamp in milliseconds of the period
     */
    public long getEndOfPeriod(int periodNum){
        LocalDateTime localDate = new LocalDateTime();
        int interval = mRecurrence.getPeriodType().getMultiplier() * periodNum;
        switch (mRecurrence.getPeriodType()){
            //TODO:HOUR
            case DAY:
                localDate = localDate.millisOfDay().withMaximumValue().plusDays(interval);
                break;
            case WEEK:
                localDate = localDate.dayOfWeek().withMaximumValue().plusWeeks(interval);
                break;
            case MONTH:
                localDate = localDate.dayOfMonth().withMaximumValue().plusMonths(interval);
                break;
            case YEAR:
                localDate = localDate.dayOfYear().withMaximumValue().plusYears(interval);
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

    /**
     * Returns the number of accounts in this budget
     * @return Number of budgeted accounts
     */
    public int getNumberOfAccounts(){
        Set<String> accountSet = new HashSet<>();
        for (BudgetAmount budgetAmount : mBudgetAmounts) {
            accountSet.add(budgetAmount.getAccountUID());
        }
        return accountSet.size();
    }

    /**
     * Returns the list of budget amounts where only one BudgetAmount is present if the amount of the budget amount
     * is the same for all periods in the budget.
     * BudgetAmounts with different amounts per period are still return separately
     * <p>
     *     This method is used during import because GnuCash desktop saves one BudgetAmount per period for the whole budgeting period.
     *     While this can be easily displayed in a table form on the desktop, it is not feasible in the Android app.
     *     So we display only one BudgetAmount if it covers all periods in the budgeting period
     * </p>
     * @return List of {@link BudgetAmount}s
     */
    public List<BudgetAmount> getCompactedBudgetAmounts(){

        Map<String, List<BigDecimal>> accountAmountMap = new HashMap<>();
        for (BudgetAmount budgetAmount : mBudgetAmounts) {
            String accountUID = budgetAmount.getAccountUID();
            BigDecimal amount = budgetAmount.getAmount().asBigDecimal();
            if (accountAmountMap.containsKey(accountUID)){
                accountAmountMap.get(accountUID).add(amount);
            } else {
                List<BigDecimal> amounts = new ArrayList<>();
                amounts.add(amount);
                accountAmountMap.put(accountUID, amounts);
            }
        }

        List<BudgetAmount> compactBudgetAmounts = new ArrayList<>();
        for (Map.Entry<String, List<BigDecimal>> entry : accountAmountMap.entrySet()) {
            List<BigDecimal> amounts = entry.getValue();
            BigDecimal first = amounts.get(0);
            boolean allSame = true;
            for (BigDecimal bigDecimal : amounts) {
                allSame &= bigDecimal.equals(first);
            }

            if (allSame){
                if (amounts.size() == 1) {
                    for (BudgetAmount bgtAmount : mBudgetAmounts) {
                        if (bgtAmount.getAccountUID().equals(entry.getKey())) {
                            compactBudgetAmounts.add(bgtAmount);
                            break;
                        }
                    }
                } else {
                    BudgetAmount bgtAmount = new BudgetAmount(getUID(), entry.getKey());
                    bgtAmount.setAmount(new Money(first, Commodity.DEFAULT_COMMODITY));
                    bgtAmount.setPeriodNum(-1);
                    compactBudgetAmounts.add(bgtAmount);
                }
            } else {
                //if not all amounts are the same, then just add them as we read them
                for (BudgetAmount bgtAmount : mBudgetAmounts) {
                    if (bgtAmount.getAccountUID().equals(entry.getKey())){
                        compactBudgetAmounts.add(bgtAmount);
                    }
                }
            }
        }

        return compactBudgetAmounts;
    }

    /**
     * Returns a list of budget amounts where each period has it's own budget amount
     * <p>Any budget amounts in the database with a period number of -1 are expanded to individual budget amounts for all periods</p>
     * <p>This method is useful with exporting budget amounts to XML</p>
     * @return List of expande
     */
    public List<BudgetAmount> getExpandedBudgetAmounts(){
        List<BudgetAmount> amountsToAdd = new ArrayList<>();
        List<BudgetAmount> amountsToRemove = new ArrayList<>();
        for (BudgetAmount budgetAmount : mBudgetAmounts) {
            if (budgetAmount.getPeriodNum() == -1){
                amountsToRemove.add(budgetAmount);
                String accountUID = budgetAmount.getAccountUID();
                for (int period = 0; period < mNumberOfPeriods; period++) {
                    BudgetAmount bgtAmount = new BudgetAmount(getUID(), accountUID);
                    bgtAmount.setAmount(budgetAmount.getAmount());
                    bgtAmount.setPeriodNum(period);
                    amountsToAdd.add(bgtAmount);
                }
            }
        }

        List<BudgetAmount> expandedBudgetAmounts = new ArrayList<>(mBudgetAmounts);
        for (BudgetAmount bgtAmount : amountsToRemove) {
            expandedBudgetAmounts.remove(bgtAmount);
        }

        for (BudgetAmount bgtAmount : amountsToAdd) {
            expandedBudgetAmounts.add(bgtAmount);
        }
        return expandedBudgetAmounts;
    }
}
