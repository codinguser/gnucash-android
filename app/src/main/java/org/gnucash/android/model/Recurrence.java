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

import org.gnucash.android.ui.util.RecurrenceParser;

import java.sql.Timestamp;

/**
 * Model for recurrences in the database
 * <p>Basically a wrapper around {@link PeriodType}</p>
 */
public class Recurrence extends BaseModel {

    private PeriodType mPeriodType;
    private Timestamp mPeriodStart;

    public Recurrence(@NonNull PeriodType periodType){
        setPeriodType(periodType);
        mPeriodStart = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Return the PeriodType for this recurrence
     * @return PeriodType for the recurrence
     */
    public PeriodType getPeriodType() {
        return mPeriodType;
    }

    /**
     * Sets the period type for the recurrence
     * @param periodType PeriodType
     */
    public void setPeriodType(PeriodType periodType) {
        this.mPeriodType = periodType;
    }

    /**
     * Return the start time for this recurrence
     * @return Timestamp of start of recurrence
     */
    public Timestamp getPeriodStart() {
        return mPeriodStart;
    }

    /**
     * Set the start time of this recurrence
     * @param periodStart {@link Timestamp} of recurrence
     */
    public void setPeriodStart(Timestamp periodStart) {
        this.mPeriodStart = periodStart;
    }


    /**
     * Computes the {@link PeriodType} for a given {@code period}
     * @param period Period in milliseconds since Epoch
     * @return PeriodType corresponding to the period
     */
    public static PeriodType getPeriodType(long period){
        PeriodType periodType = PeriodType.DAY;
        int result = (int) (period/ RecurrenceParser.YEAR_MILLIS);
        if (result > 0) {
            periodType = PeriodType.YEAR;
            periodType.setMultiplier(result);
            return periodType;
        }

        result = (int) (period/RecurrenceParser.MONTH_MILLIS);
        if (result > 0) {
            periodType = PeriodType.MONTH;
            periodType.setMultiplier(result);
            return periodType;
        }

        result = (int) (period/RecurrenceParser.WEEK_MILLIS);
        if (result > 0) {
            periodType = PeriodType.WEEK;
            periodType.setMultiplier(result);
            return periodType;
        }

        result = (int) (period/RecurrenceParser.DAY_MILLIS);
        if (result > 0) {
            periodType = PeriodType.DAY;
            periodType.setMultiplier(result);
            return periodType;
        }

        return periodType;
    }

    /**
     * Returns an approximate period for this recurrence
     * <p>The period is approximate because months do not all have the same number of days,
     * but that is assumed</p>
     * @return Milliseconds since Epoch representing the period
     */
    public long getPeriod(){
        long baseMillis = 0;
        switch (mPeriodType){
            case DAY:
                baseMillis = RecurrenceParser.DAY_MILLIS;
                break;
            case WEEK:
                baseMillis = RecurrenceParser.WEEK_MILLIS;
                break;
            case MONTH:
                baseMillis = RecurrenceParser.MONTH_MILLIS;
                break;
            case YEAR:
                baseMillis = RecurrenceParser.YEAR_MILLIS;
                break;
        }
        return mPeriodType.getMultiplier() * baseMillis;
    }
}
