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

import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigDecimal;

/**
 * Budget amounts for the different accounts.
 * The {@link Money} amounts are absolute values
 * @see Budget
 */
public class BudgetAmount extends BaseModel implements Parcelable {

    private String mBudgetUID;
    private String mAccountUID;
    /**
     * Period number for this budget amount
     * A value of -1 indicates that this budget amount applies to all periods
     */
    private long mPeriodNum;
    private Money mAmount;

    /**
     * Create a new budget amount
     * @param budgetUID GUID of the budget
     * @param accountUID GUID of the account
     */
    public BudgetAmount(String budgetUID, String accountUID){
        this.mBudgetUID = budgetUID;
        this.mAccountUID = accountUID;
    }

    /**
     * Creates a new budget amount with the absolute value of {@code amount}
     * @param amount Money amount of the budget
     * @param accountUID GUID of the account
     */
    public BudgetAmount(Money amount, String accountUID){
        this.mAmount = amount.abs();
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

    /**
     * Returns the period number of this budget amount
     * <p>The period is zero-based index, and a value of -1 indicates that this budget amount is applicable to all budgeting periods</p>
     * @return Period number
     */
    public long getPeriodNum() {
        return mPeriodNum;
    }

    /**
     * Set the period number for this budget amount
     * <p>A value of -1 indicates that this BudgetAmount is for all periods</p>
     * @param periodNum Zero-based period number of the budget amount
     */
    public void setPeriodNum(long periodNum) {
        this.mPeriodNum = periodNum;
    }

    /**
     * Returns the Money amount of this budget amount
     * @return Money amount
     */
    public Money getAmount() {
        return mAmount;
    }

    /**
     * Sets the amount for the budget
     * <p>The absolute value of the amount is used</p>
     * @param amount Money amount
     */
    public void setAmount(Money amount) {
        this.mAmount = amount.abs();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getUID());
        dest.writeString(mBudgetUID);
        dest.writeString(mAccountUID);
        dest.writeString(mAmount.toPlainString());
        dest.writeLong(mPeriodNum);
    }

    public static final Parcelable.Creator<BudgetAmount> CREATOR = new Parcelable.Creator<BudgetAmount>(){

        @Override
        public BudgetAmount createFromParcel(Parcel source) {
            return new BudgetAmount(source);
        }

        @Override
        public BudgetAmount[] newArray(int size) {
            return new BudgetAmount[size];
        }
    };

    /**
     * Private constructor for creating new BudgetAmounts from a Parcel
     * @param source Parcel
     */
    private BudgetAmount(Parcel source){
        setUID(source.readString());
        mBudgetUID = source.readString();
        mAccountUID = source.readString();
        mAmount = new Money(new BigDecimal(source.readString()), Commodity.DEFAULT_COMMODITY);
        mPeriodNum = source.readLong();
    }


}
