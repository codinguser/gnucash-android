/*
 * Copyright (c) 2014 - 2015 Ngewi Fet <ngewif@gmail.com>
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

import org.gnucash.android.ui.util.RecurrenceParser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
* Represents a scheduled event which is stored in the database and run at regular mPeriod
*
* @author Ngewi Fet <ngewif@gmail.com>
*/
public class ScheduledEvent extends BaseModel{

    private long mPeriod;
    private long mStartDate;
    private long mEndDate;
    private String mTag;

    /**
     * Types of events which can be scheduled
     */
    public enum EventType {TRANSACTION, EXPORT}

    public enum PeriodType {DAILY, WEEKLY, FORTNIGHTLY, MONTHLY, YEARLY}

    /**
     * Next scheduled run of Event
     */
    private long mLastRun;

    /**
     * Unique ID of the template from which the recurring event will be executed.
     * For example, transaction UID
     */
    private String mEventUID;

    /**
     * Flag indicating if this event is enabled or not
     */
    private boolean mIsEnabled;

    /**
     * Type of event being scheduled
     */
    private EventType mEventType;

    /**
     * Number of occurences of this event
     */
    private int mNumberOfOccurences = 0;

    /**
     * How many times this action has already been executed
     */
    private int mNumberOfExecutions = 0;

    public ScheduledEvent(EventType eventType){
        mEventType = eventType;
        mStartDate = System.currentTimeMillis();
        mEndDate = 0;
        mIsEnabled = true; //all actions are enabled by default
    }

    public EventType getEventType() {
        return mEventType;
    }

    public void setEventType(EventType eventType) {
        this.mEventType = eventType;
    }

    public String getEventUID() {
        return mEventUID;
    }

    public void setEventUID(String eventUID) {
        this.mEventUID = eventUID;
    }

    public long getLastRun() {
        return mLastRun;
    }

    public void setLastRun(long nextRun) {
        this.mLastRun = nextRun;
    }

    public long getPeriod() {
        return mPeriod;
    }

    public void setPeriod(long period) {
        this.mPeriod = period;
    }

    public static PeriodType getPeriodType(long period){
        int result = (int) (period/RecurrenceParser.DAY_MILLIS);
        if (result == 0)
            return PeriodType.DAILY;

        result = (int) (period/RecurrenceParser.WEEK_MILLIS);
        if (result == 0)
            return PeriodType.WEEKLY;

        result = (int) (period/(2*RecurrenceParser.WEEK_MILLIS));
        if (result == 0)
            return PeriodType.FORTNIGHTLY;

        result = (int) (period/RecurrenceParser.MONTH_MILLIS);
        if (result == 0)
            return PeriodType.MONTHLY;

        result = (int) (period/RecurrenceParser.YEAR_MILLIS);
        if (result == 0)
            return PeriodType.YEARLY;

        return PeriodType.DAILY;
    }

    public long getStartTime() {
        return mStartDate;
    }

    public void setStartTime(long startDate) {
        this.mStartDate = startDate;
    }

    public long getEndTime() {
        return mEndDate;
    }

    /**
     * Returns the approximate end time of this scheduled action.
     * <p>This is useful when the number of occurences was set, rather than a specific end time.
     * The end time is then computed from the start time, period and number of occurrences.</p>
     * @return End time in milliseconds for the scheduled action
     */
    public long getApproxEndTime(){
        return mStartDate + (mPeriod * mNumberOfOccurences);
    }
    public void setEndTime(long endDate) {
        this.mEndDate = endDate;
    }

    public String getTag() {
        return mTag;
    }

    public void setTag(String tag) {
        this.mTag = tag;
    }

    public boolean isEnabled(){
        return mIsEnabled;
    }

    public void setEnabled(boolean enabled){
        this.mIsEnabled = enabled;
    }

    /**
     * Returns the total number of occurences of this scheduled action.
     * <p>Typically, this is set explicity, but if not, then it is computed from the values of
     * the start date, end date and period.</p>
     * @return Total number of occurences of this action
     */
    public int getNumberOfOccurences(){
        return mNumberOfOccurences;
    }

    /**
     * Sets the number of occurences of this action
     * @param occurencesCount
     */
    public void setNumberOfOccurences(int occurencesCount){
        this.mNumberOfOccurences = occurencesCount;
    }

    /**
     * Returns how many times this scheduled action has already been executed
     * @return Number of times this action has been executed
     */
    public int getExecutionCount(){
        return mNumberOfExecutions;
    }

    /**
     * Sets the number of times this scheduled action has been executed
     * @param executionCount Number of executions
     */
    public void setExecutionCount(int executionCount){
        mNumberOfExecutions = executionCount;
    }

    /**
     * Returns the event schedule (start, end and recurrence)
     * @return String description of repeat schedule
     */
    public String getRepeatString(){
        //TODO: localize the string
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
        String repeatString = "Repeats every " + mPeriod/RecurrenceParser.DAY_MILLIS + " days starting on "
                + dateFormat.format(new Date(mStartDate));
        if (mEndDate > 0){
            repeatString += " until " + dateFormat.format(mEndDate);
        }
        return repeatString;
    }

    @Override
    public String toString() {

        String eventString = mEventType.name() + " - " + getRepeatString();

        return eventString;
    }
}
