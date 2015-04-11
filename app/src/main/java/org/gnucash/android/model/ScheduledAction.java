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
public class ScheduledAction extends BaseModel{

    private long mPeriod;
    private long mStartDate;
    private long mEndDate;
    private String mTag;

    /**
     * Types of events which can be scheduled
     */
    public enum ActionType {TRANSACTION, EXPORT}

    /**
     * Next scheduled run of Event
     */
    private long mLastRun;

    /**
     * Unique ID of the template from which the recurring event will be executed.
     * For example, transaction UID
     */
    private String mActionUID;

    /**
     * Flag indicating if this event is enabled or not
     */
    private boolean mIsEnabled;

    /**
     * Type of event being scheduled
     */
    private ActionType mActionType;

    /**
     * Number of times this event is to be executed
     */
    private int mTotalFrequency = 0;

    /**
     * How many times this action has already been executed
     */
    private int mExecutionCount = 0;

    public ScheduledAction(ActionType actionType){
        mActionType = actionType;
        mStartDate = System.currentTimeMillis();
        mEndDate = 0;
        mIsEnabled = true; //all actions are enabled by default
    }

    public ActionType getActionType() {
        return mActionType;
    }

    public void setActionType(ActionType actionType) {
        this.mActionType = actionType;
    }

    public String getActionUID() {
        return mActionUID;
    }

    public void setActionUID(String actionUID) {
        this.mActionUID = actionUID;
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

    /**
     * Sets the period given the period type.
     * The {@link PeriodType} should have the multiplier set,
     * e.g. bi-weekly actions have period type {@link PeriodType#WEEK} and multiplier 2
     * @param periodType Type of period
     */
    public void setPeriod(PeriodType periodType){
        int multiplier = periodType.getMultiplier();
        switch (periodType){
            case DAY:
                mPeriod = RecurrenceParser.DAY_MILLIS * multiplier;
                break;
            case WEEK:
                mPeriod = RecurrenceParser.WEEK_MILLIS * multiplier;
                break;
            case MONTH:
                mPeriod = RecurrenceParser.MONTH_MILLIS * multiplier;
                break;
            case YEAR:
                mPeriod = RecurrenceParser.YEAR_MILLIS * multiplier;
                break;
        }
    }

    /**
     * Returns the period type for this scheduled action
     * @return Period type of the action
     */
    public PeriodType getPeriodType(){
        return getPeriodType(mPeriod);
    }

    /**
     * Computes the {@link PeriodType} for a given {@code period}
     * @param period Period in milliseconds since Epoch
     * @return PeriodType corresponding to the period
     */
    public static PeriodType getPeriodType(long period){
        PeriodType periodType = PeriodType.DAY;
        int result = (int) (period/RecurrenceParser.YEAR_MILLIS);
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
        return mStartDate + (mPeriod * mTotalFrequency);
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
     * @return Total number of occurences of this action
     */
    public int getTotalFrequency(){
        return mTotalFrequency;
    }

    /**
     * Sets the number of occurences of this action
     * @param occurencesCount Number of occurences
     */
    public void setTotalFrequency(int occurencesCount){
        this.mTotalFrequency = occurencesCount;
    }

    /**
     * Returns how many times this scheduled action has already been executed
     * @return Number of times this action has been executed
     */
    public int getExecutionCount(){
        return mExecutionCount;
    }

    /**
     * Sets the number of times this scheduled action has been executed
     * @param executionCount Number of executions
     */
    public void setExecutionCount(int executionCount){
        mExecutionCount = executionCount;
    }

    /**
     * Returns the event schedule (start, end and recurrence)
     * @return String description of repeat schedule
     */
    public String getRepeatString(){
        String dayOfWeek = new SimpleDateFormat("E", Locale.US).format(new Date(mStartDate));
        PeriodType periodType = getPeriodType();
        StringBuilder ruleBuilder = new StringBuilder(periodType.getLocalizedFrequencyDescription());
        ruleBuilder.append(" on " + dayOfWeek);
        ruleBuilder.append(";");
        if (mEndDate > 0){
            ruleBuilder.append(" until " + new SimpleDateFormat("M/d", Locale.US).format(new Date(mEndDate)) + ";");
        } else if (mTotalFrequency > 0){
            ruleBuilder.append(" for " + mTotalFrequency + " times;");
        }
        return ruleBuilder.toString();
    }

    /**
     * Creates an RFC 2445 string which describes this recurring event
     * @return String describing event
     */
    public String getRuleString(){
        String dayOfWeek = new SimpleDateFormat("EE", Locale.US).format(new Date(mStartDate));
        PeriodType periodType = getPeriodType();
        StringBuilder ruleBuilder = new StringBuilder(periodType.getFrequencyDescription());
        ruleBuilder.append(" on " + dayOfWeek);
        ruleBuilder.append(";");
        if (mEndDate > 0){
            ruleBuilder.append(" until " + new SimpleDateFormat("M/d/yyyy", Locale.US).format(new Date(mEndDate)) + ";");
        } else if (mTotalFrequency > 0){
            ruleBuilder.append(" for " + mTotalFrequency + " times;");
        }
        return ruleBuilder.toString();
    }

    /**
     * Creates a ScheduledAction from a Transaction and a period
     * @param transaction
     * @param period
     * @return
     */
    public static ScheduledAction parseScheduledAction(Transaction transaction, long period){
        ScheduledAction scheduledAction = new ScheduledAction(ActionType.TRANSACTION);
        scheduledAction.mActionUID = transaction.getUID();
        scheduledAction.mPeriod = period;
        return scheduledAction;
    }

    @Override
    public String toString() {

        String eventString = mActionType.name() + " - " + getRepeatString();

        return eventString;
    }
}
