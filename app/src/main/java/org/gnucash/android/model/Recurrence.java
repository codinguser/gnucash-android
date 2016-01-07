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
import org.joda.time.LocalDateTime;
import org.joda.time.Months;
import org.joda.time.Weeks;
import org.joda.time.Years;

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

    /**
     * Start time of the recurrence
     */
    private Timestamp mPeriodStart;

    /**
     * End time of this recurrence
     * <p>This value is not persisted to the database</p>
     */
    private Timestamp mPeriodEnd;

    /**
     * Describes which day on which to run the recurrence
     */
    private String mByDay;

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

        StringBuilder repeatBuilder = new StringBuilder(mPeriodType.getFrequencyRepeatString());

        if (mPeriodType == PeriodType.WEEK) {
            repeatBuilder.append(" on ").append(dayOfWeek);
        }

        if (mPeriodEnd != null){
            repeatBuilder.append(" until " + SimpleDateFormat.getDateInstance().format(new Date(mPeriodEnd.getTime())));
        }
        return repeatBuilder.toString();
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
        if (getCount() > 0)
            ruleBuilder.append("COUNT=").append(getCount()).append(separator);
        ruleBuilder.append(mPeriodType.getByParts(mPeriodStart.getTime())).append(separator);

        return ruleBuilder.toString();
    }

    /**
     * Return the number of days left in this period
     * @return Number of days left in period
     */
    public int getDaysLeftInCurrentPeriod(){
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

    /**
     * Returns the number of periods from the start date of this occurence until the end of the
     * interval multiplier specified in the {@link PeriodType}
     * @return Number of periods in this recurrence
     */
    public int getNumberOfPeriods(int numberOfPeriods) {
        LocalDate startDate = new LocalDate(mPeriodStart.getTime());
        LocalDate endDate;
        int interval = mPeriodType.getMultiplier();
        switch (mPeriodType){

            case DAY:
                return 1;
            case WEEK:
                endDate = startDate.dayOfWeek().withMaximumValue().plusWeeks(numberOfPeriods);
                return Weeks.weeksBetween(startDate, endDate).getWeeks() / interval;
            case MONTH:
                endDate = startDate.dayOfMonth().withMaximumValue().plusMonths(numberOfPeriods);
                return Months.monthsBetween(startDate, endDate).getMonths() / interval;
            case YEAR:
                endDate = startDate.dayOfYear().withMaximumValue().plusYears(numberOfPeriods);
                return Years.yearsBetween(startDate, endDate).getYears() / interval;
        }

        return 0;
    }

    /**
     * Return the name of the current period
     * @return String of current period
     */
    public String getTextOfCurrentPeriod(int periodNum){
        LocalDate startDate = new LocalDate(mPeriodStart.getTime());
        switch (mPeriodType){

            case DAY:
                return startDate.dayOfWeek().getAsText();
            case WEEK:
                return startDate.weekOfWeekyear().getAsText();
            case MONTH:
                return startDate.monthOfYear().getAsText();
            case YEAR:
                return startDate.year().getAsText();
        }
        return "Period " + periodNum;
    }

    /**
     * Sets the string which determines on which day the recurrence will be run
     * @param byDay Byday string of recurrence rule (RFC 2445)
     */
    public void setByDay(String byDay){
        this.mByDay = byDay;
    }

    /**
     * Return the byDay string of recurrence rule (RFC 2445)
     * @return String with by day specification
     */
    public String getByDay(){
        return mByDay;
    }

    /**
     * Computes the number of occurrences of this recurrences between start and end date
     * <p>If there is no end date, it returns -1</p>
     * @return Number of occurrences, or -1 if there is no end date
     */
    public int getCount(){
        int count = 0;
        LocalDate startDate = new LocalDate(mPeriodStart.getTime());
        LocalDate endDate = new LocalDate(mPeriodEnd.getTime());
        switch (mPeriodType){
            case DAY:
                count = Days.daysBetween(startDate, endDate).getDays();
                break;
            case WEEK:
                count = Weeks.weeksBetween(startDate, endDate).getWeeks();
                break;
            case MONTH:
                count = Months.monthsBetween(startDate, endDate).getMonths();
                break;
            case YEAR:
                count = Years.yearsBetween(startDate, endDate).getYears();
                break;
        }

        return count/mPeriodType.getMultiplier();
    }

    /**
     * Sets the end time of this recurrence by specifying the number of occurences
     * @param numberOfOccurences Number of occurences from the start time
     */
    public void setPeriodEnd(int numberOfOccurences){
        LocalDateTime localDate = new LocalDateTime(mPeriodStart.getTime());
        LocalDateTime endDate;
        int occurrenceDuration = numberOfOccurences * mPeriodType.getMultiplier();
        switch (mPeriodType){
            case DAY:
                endDate = localDate.dayOfWeek().getLocalDateTime().plusDays(occurrenceDuration);
                break;
            case WEEK:
                endDate = localDate.dayOfWeek().getLocalDateTime().plusWeeks(occurrenceDuration);
                break;
            case MONTH:
                endDate = localDate.dayOfMonth().getLocalDateTime().plusMonths(occurrenceDuration);
                break;
            case YEAR:
                endDate = localDate.monthOfYear().getLocalDateTime().plusYears(occurrenceDuration);
                break;
            default: //default to monthly
                endDate = localDate.dayOfMonth().getLocalDateTime().plusMonths(occurrenceDuration);
                break;
        }
        mPeriodEnd = new Timestamp(endDate.toDate().getTime());
    }

    /**
     * Return the end date of the period in milliseconds
     * @return End date of the recurrence period
     */
    public Timestamp getPeriodEnd(){
        return mPeriodEnd;
    }

    /**
     * Set period end date
     * @param endTimestamp End time in milliseconds
     */
    public void setPeriodEnd(Timestamp endTimestamp){
        mPeriodEnd = endTimestamp;
    }
}
