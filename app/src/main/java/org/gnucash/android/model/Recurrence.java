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

import android.content.Context;
import android.support.annotation.NonNull;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.ui.util.RecurrenceParser;
import org.joda.time.Hours;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Months;
import org.joda.time.ReadablePeriod;
import org.joda.time.Weeks;
import org.joda.time.Years;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
     * Days of week on which to run the recurrence
     */
    private List<Integer> mByDays = Collections.emptyList();

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
     * @deprecated Do not use in new code. Uses fixed period values for months and years (which have variable units of time)
     */
    public long getPeriod(){
        long baseMillis = 0;
        switch (mPeriodType){
            case HOUR:
                baseMillis = RecurrenceParser.HOUR_MILLIS;
                break;
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
        StringBuilder repeatBuilder = new StringBuilder(mPeriodType.getFrequencyRepeatString());
        Context context = GnuCashApplication.getAppContext();

        if (mPeriodType == PeriodType.WEEK) {
            repeatBuilder.append(" ").
                    append(context.getString(R.string.repeat_on_weekday, getDaysOfWeekString()));
        }

        if (mPeriodEnd != null){
            String endDateString = SimpleDateFormat.getDateInstance().format(new Date(mPeriodEnd.getTime()));
            repeatBuilder.append(", ").append(context.getString(R.string.repeat_until_date, endDateString));
        }
        return repeatBuilder.toString();
    }

    /**
     * Returns a string with the days of the week set in the recurrence separated by commas.
     * @return string with the days of the week set in the recurrence separated by commas.
     */
    private @NonNull String getDaysOfWeekString() {
        // XXX: mByDays should never be empty with PeriodType.WEEK, but we don't enforce it yet
        if (mByDays.isEmpty())
            return "";
        StringBuilder daysOfWeekString = new StringBuilder();
        Calendar calendar = Calendar.getInstance();
        DateFormat dayOfWeekFormatter =
                new SimpleDateFormat("EEEE", GnuCashApplication.getDefaultLocale());
        for (int day : mByDays) {
            calendar.set(Calendar.DAY_OF_WEEK, day);
            daysOfWeekString.append(dayOfWeekFormatter.format(calendar.getTime())).append(", ");
        }
        return daysOfWeekString.substring(0, daysOfWeekString.length()-2);
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
            //TODO:HOUR
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
     * Returns the number of periods from the start date of this recurrence until the end of the
     * interval multiplier specified in the {@link PeriodType}
     * //fixme: Improve the documentation
     * @return Number of periods in this recurrence
     */
    public int getNumberOfPeriods(int numberOfPeriods) {
        LocalDate startDate = new LocalDate(mPeriodStart.getTime());
        LocalDate endDate;
        int interval = mPeriodType.getMultiplier();
        //// TODO: 15.08.2016 Why do we add the number of periods. maybe rename method or param
        switch (mPeriodType){
            // TODO:HOUR
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
        LocalDateTime startDateTime = new LocalDateTime(mPeriodStart.getTime());
        switch (mPeriodType){
            case HOUR:
                return startDateTime.hourOfDay().getAsText();
            case DAY:
                return startDateTime.dayOfWeek().getAsText();
            case WEEK:
                return startDateTime.weekOfWeekyear().getAsText();
            case MONTH:
                return startDateTime.monthOfYear().getAsText();
            case YEAR:
                return startDateTime.year().getAsText();
        }
        return "Period " + periodNum;
    }

    /**
     * Return the days of week on which to run the recurrence.
     *
     * <p>Days are expressed as defined in {@link java.util.Calendar}.
     * For example, Calendar.MONDAY</p>
     *
     * @return list of days of week on which to run the recurrence.
     */
    public @NonNull List<Integer> getByDays(){
        return Collections.unmodifiableList(mByDays);
    }

    /**
     * Sets the days on which to run the recurrence.
     *
     * <p>Days must be expressed as defined in {@link java.util.Calendar}.
     * For example, Calendar.MONDAY</p>
     *
     * @param byDays list of days of week on which to run the recurrence.
     */
    public void setByDays(@NonNull List<Integer> byDays){
        mByDays = new ArrayList<>(byDays);
    }

    /**
     * Computes the number of occurrences of this recurrences between start and end date
     * <p>If there is no end date, it returns -1</p>
     * @return Number of occurrences, or -1 if there is no end date
     */
    public int getCount(){
        if (mPeriodEnd == null)
            return -1;

        int multiple = mPeriodType.getMultiplier();
        ReadablePeriod jodaPeriod;
        switch (mPeriodType){
            case HOUR:
                jodaPeriod = Hours.hours(multiple);
                break;
            case DAY:
                jodaPeriod = Days.days(multiple);
                break;
            case WEEK:
                jodaPeriod = Weeks.weeks(multiple);
                break;
            case MONTH:
                jodaPeriod = Months.months(multiple);
                break;
            case YEAR:
                jodaPeriod = Years.years(multiple);
                break;
            default:
                jodaPeriod = Months.months(multiple);
        }
        int count = 0;
        LocalDateTime startTime = new LocalDateTime(mPeriodStart.getTime());
        while (startTime.toDateTime().getMillis() < mPeriodEnd.getTime()){
            ++count;
            startTime = startTime.plus(jodaPeriod);
        }
        return count;

/*
        //this solution does not use looping, but is not very accurate

        int multiplier = mPeriodType.getMultiplier();
        LocalDateTime startDateTime = new LocalDateTime(mPeriodStart.getTime());
        LocalDateTime endDateTime = new LocalDateTime(mPeriodEnd.getTime());
        switch (mPeriodType){
            case HOUR:
                return Hours.hoursBetween(startDateTime, endDateTime).dividedBy(multiplier).getHours();
            case DAY:
                return Days.daysBetween(startDateTime, endDateTime).dividedBy(multiplier).getDays();
            case WEEK:
                return Weeks.weeksBetween(startDateTime, endDateTime).dividedBy(multiplier).getWeeks();
            case MONTH:
                return Months.monthsBetween(startDateTime, endDateTime).dividedBy(multiplier).getMonths();
            case YEAR:
                return Years.yearsBetween(startDateTime, endDateTime).dividedBy(multiplier).getYears();
            default:
                return -1;
        }
*/
    }

    /**
     * Sets the end time of this recurrence by specifying the number of occurences
     * @param numberOfOccurences Number of occurences from the start time
     */
    public void setPeriodEnd(int numberOfOccurences){
        LocalDateTime localDateTime = new LocalDateTime(mPeriodStart.getTime());
        LocalDateTime endDateTime;
        int occurrenceDuration = numberOfOccurences * mPeriodType.getMultiplier();
        switch (mPeriodType){
            case HOUR:
                endDateTime = localDateTime.plusHours(occurrenceDuration);
                break;
            case DAY:
                endDateTime = localDateTime.plusDays(occurrenceDuration);
                break;
            case WEEK:
                endDateTime = localDateTime.plusWeeks(occurrenceDuration);
                break;
            default:
            case MONTH:
                endDateTime = localDateTime.plusMonths(occurrenceDuration);
                break;
            case YEAR:
                endDateTime = localDateTime.plusYears(occurrenceDuration);
                break;
        }
        mPeriodEnd = new Timestamp(endDateTime.toDateTime().getMillis());
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
