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
import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    /**
     * Returns the event schedule (start, end and recurrence)
     * @return String description of repeat schedule
     */
    public String getRepeatString(){
        String dayOfWeek = new SimpleDateFormat("EEEE", Locale.US).format(new Date(mPeriodStart.getTime()));

        StringBuilder ruleBuilder = new StringBuilder(mPeriodType.getFrequencyRepeatString());

        if (mPeriodType == PeriodType.WEEK) {
            ruleBuilder.append(" on ").append(dayOfWeek);
        }

        return ruleBuilder.toString();
    }

        /**
         * Creates an RFC 2445 string which describes this recurring event.
         * <p>See http://recurrance.sourceforge.net/</p>
         * <p>The output of this method is not meant for human consumption</p>
         * @return String describing event
         */
    public String getRuleString(){
        String separator = ";";

        StringBuilder ruleBuilder = new StringBuilder();

//        =======================================================================
        //This section complies with the formal rules, but the betterpickers library doesn't like/need it

//        SimpleDateFormat startDateFormat = new SimpleDateFormat("'TZID'=zzzz':'yyyyMMdd'T'HHmmss", Locale.US);
//        ruleBuilder.append("DTSTART;");
//        ruleBuilder.append(startDateFormat.format(new Date(mStartDate)));
//            ruleBuilder.append("\n");
//        ruleBuilder.append("RRULE:");
//        ========================================================================


        ruleBuilder.append("FREQ=").append(mPeriodType.getFrequencyDescription()).append(separator);
        ruleBuilder.append("INTERVAL=").append(mPeriodType.getMultiplier()).append(separator);
        ruleBuilder.append(mPeriodType.getByParts(mPeriodStart.getTime())).append(separator);

        return ruleBuilder.toString();
    }

    /**
     * Return the number of days left in this period
     * @return Number of days left in period
     */
    public int getDaysLeft(){
        LocalDate startDate = new LocalDate(System.currentTimeMillis());
        int interval = mPeriodType.getMultiplier() - 1;
        LocalDate endDate = null;
        switch (mPeriodType){
            case DAY:
                endDate = new LocalDate(System.currentTimeMillis()).plusDays(interval);
                break;
            case WEEK:
                endDate = startDate.dayOfWeek().withMaximumValue().plusWeeks(interval);
                break;
            case MONTH:
                endDate = startDate.dayOfMonth().withMaximumValue().plusMonths(interval);
                break;
            case YEAR:
                endDate = startDate.dayOfYear().withMaximumValue().plusYears(interval);
                break;
        }

        return Days.daysBetween(startDate, endDate).getDays();
    }
}
