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

/**
 * Budgets model
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class Budget extends BaseModel {

    private String name;
    private String description;
    private Money amount;
    private String accountUID;
    private String recurrenceUID;
    private long numberOfPeriods = 12; //default to 12 periods per year

    /**
     * Default constructor
     */
    public Budget(){
        //nothing to see here
    }

    /**
     * Overloaded constructor.
     * Initializes the name and amount of this budget
     * @param name String name of the budget
     * @param amount Money amount of this budget
     */
    public Budget(@NonNull String name, @NonNull Money amount){
        this.name = name;
        this.amount = amount;
    }

    /**
     * Returns the name of the budget
     * @return name of the budget
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the budget
     * @param name String name of budget
     */
    public void setName(@NonNull String name) {
        this.name = name;
    }

    /**
     * Returns the description of the budget
     * @return String description of budget
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the budget
     * @param description String description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the amount of this budget
     * @return Money amount of budget
     */
    public Money getAmount() {
        return amount;
    }

    /**
     * Sets the amount for this budget
     * @param amount Money amount of the budget
     */
    public void setAmount(@NonNull Money amount) {
        this.amount = amount;
    }

    /**
     * Return the account GUID associated with this budget
     * @return Account GUID string
     */
    public String getAccountUID() {
        return accountUID;
    }

    /**
     * Sets the GUID of the account associated with this budget
     * @param accountUID Account GUID string
     */
    public void setAccountUID(@NonNull String accountUID) {
        this.accountUID = accountUID;
    }

    /**
     * Returns the GUID of the recurrence record for this budget
     * @return GUID of recurrence record
     */
    public String getRecurrenceUID() {
        return recurrenceUID;
    }

    /**
     * Set the GUID of the recurrence record for this budget
     * @param recurrenceUID GUID string of recurrence
     */
    public void setRecurrenceUID(@NonNull String recurrenceUID) {
        this.recurrenceUID = recurrenceUID;
    }

    /**
     * Returns the number of periods covered by this budget
     * @return Number of periods
     */
    public long getNumberOfPeriods() {
        return numberOfPeriods;
    }

    /**
     * Sets the number of periods for the budget
     * @param numberOfPeriods Number of periods as long
     */
    public void setNumberOfPeriods(long numberOfPeriods) {
        this.numberOfPeriods = numberOfPeriods;
    }
}
