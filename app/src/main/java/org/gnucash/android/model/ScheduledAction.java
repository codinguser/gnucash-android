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

import android.support.annotation.NonNull;

import org.joda.time.LocalDate;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
* Represents a scheduled event which is stored in the database and run at regular mPeriod
*
* @author Ngewi Fet <ngewif@gmail.com>
*/
public class ScheduledAction extends BaseModel{

    private long mStartDate;
    private long mEndDate;
    private String mTag;

    /**
     * Recurrence of this scheduled action
     */
    private Recurrence mRecurrence;

    /**
     * Types of events which can be scheduled
     */
    public enum ActionType {TRANSACTION, BACKUP}

    /**
     * Next scheduled run of Event
     */
    private long mLastRun = 0;

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

    /**
     * Flag for whether the scheduled transaction should be auto-created
     */
    private boolean mAutoCreate = true;
    private boolean mAutoNotify = false;
    private int mAdvanceCreateDays = 0;
    private int mAdvanceNotifyDays = 0;
    private String mTemplateAccountUID;

    public ScheduledAction(ActionType actionType){
        mActionType = actionType;
        mEndDate = 0;
        mIsEnabled = true; //all actions are enabled by default
    }

    /**
     * Returns the type of action to be performed by this scheduled action
     * @return ActionType of the scheduled action
     */
    public ActionType getActionType() {
        return mActionType;
    }

    /**
     * Sets the {@link ActionType}
     * @param actionType Type of action
     */
    public void setActionType(ActionType actionType) {
        this.mActionType = actionType;
    }

    /**
     * Returns the GUID of the action covered by this scheduled action
     * @return GUID of action
     */
    public String getActionUID() {
        return mActionUID;
    }

    /**
     * Sets the GUID of the action being scheduled
     * @param actionUID GUID of the action
     */
    public void setActionUID(String actionUID) {
        this.mActionUID = actionUID;
    }

    /**
     * Returns the timestamp of the last execution of this scheduled action
     * @return Timestamp in milliseconds since Epoch
     */
    public long getLastRunTime() {
        return mLastRun;
    }

    /**
     * Computes the next time that this scheduled action is supposed to be executed, taking the
     * last execution time into account
     * <p>This method does not consider the end time, or number of times it should be run.
     * It only considers when the next execution would theoretically be due</p>
     * @return Next run time in milliseconds
     */
    public long computeNextRunTime(){
        int multiplier = mRecurrence.getPeriodType().getMultiplier();
        long time = mLastRun;
        if (time == 0) {
            time = mStartDate;
        }
        LocalDate localDate = LocalDate.fromDateFields(new Date(mLastRun));
        switch (mRecurrence.getPeriodType()) {
            case DAY:
                localDate.plusDays(multiplier);
                break;
            case WEEK:
                localDate.plusWeeks(multiplier);
                break;
            case MONTH:
                localDate.plusMonths(multiplier);
                break;
            case YEAR:
                localDate.plusYears(multiplier);
                break;
        }
        return localDate.toDate().getTime();
    }

    /**
     * Set time of last execution of the scheduled action
     * @param nextRun Timestamp in milliseconds since Epoch
     */
    public void setLastRun(long nextRun) {
        this.mLastRun = nextRun;
    }

    /**
     * Returns the period of this scheduled action
     * @return Period in milliseconds since Epoch
     */
    public long getPeriod() {
        return mRecurrence.getPeriod();
    }

    /**
     * Returns the time of first execution of the scheduled action
     * @return Start time of scheduled action in milliseconds since Epoch
     */
    public long getStartTime() {
        return mStartDate;
    }

    /**
     * Sets the time of first execution of the scheduled action
     * @param startDate Timestamp in milliseconds since Epoch
     */
    public void setStartTime(long startDate) {
        this.mStartDate = startDate;
        if (mRecurrence != null) {
            mRecurrence.setPeriodStart(new Timestamp(startDate));
        }
    }

    /**
     * Returns the time of last execution of the scheduled action
     * @return Timestamp in milliseconds since Epoch
     */
    public long getEndTime() {
        return mEndDate;
    }

    /**
     * Sets the end time of the scheduled action
     * @param endDate Timestamp in milliseconds since Epoch
     */
    public void setEndTime(long endDate) {
        this.mEndDate = endDate;
        if (mRecurrence != null){
            mRecurrence.setPeriodEnd(new Timestamp(mEndDate));
        }
    }

    /**
     * Returns the tag of this scheduled action
     * <p>The tag saves additional information about the scheduled action,
     * e.g. such as export parameters for scheduled backups</p>
     * @return Tag of scheduled action
     */
    public String getTag() {
        return mTag;
    }

    /**
     * Sets the tag of the schedules action.
     * <p>The tag saves additional information about the scheduled action,
     * e.g. such as export parameters for scheduled backups</p>
     * @param tag Tag of scheduled action
     */
    public void setTag(String tag) {
        this.mTag = tag;
    }

    /**
     * Returns {@code true} if the scheduled action is enabled, {@code false} otherwise
     * @return {@code true} if the scheduled action is enabled, {@code false} otherwise
     */
    public boolean isEnabled(){
        return mIsEnabled;
    }

    /**
     * Toggles the enabled state of the scheduled action
     * Disabled scheduled actions will not be executed
     * @param enabled Flag if the scheduled action is enabled or not
     */
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
     * Returns flag if transactions should be automatically created or not
     * <p>This flag is currently unused in the app. It is only included here for compatibility with GnuCash desktop XML</p>
     * @return {@code true} if the transaction should be auto-created, {@code false} otherwise
     */
    public boolean shouldAutoCreate() {
        return mAutoCreate;
    }

    /**
     * Set flag for automatically creating transaction based on this scheduled action
     * <p>This flag is currently unused in the app. It is only included here for compatibility with GnuCash desktop XML</p>
     * @param autoCreate Flag for auto creating transactions
     */
    public void setAutoCreate(boolean autoCreate) {
        this.mAutoCreate = autoCreate;
    }

    /**
     * Check if user will be notified of creation of scheduled transactions
     * <p>This flag is currently unused in the app. It is only included here for compatibility with GnuCash desktop XML</p>
     * @return {@code true} if user will be notified, {@code false} otherwise
     */
    public boolean shouldAutoNotify() {
        return mAutoNotify;
    }

    /**
     * Sets whether to notify the user that scheduled transactions have been created
     * <p>This flag is currently unused in the app. It is only included here for compatibility with GnuCash desktop XML</p>
     * @param autoNotify Boolean flag
     */
    public void setAutoNotify(boolean autoNotify) {
        this.mAutoNotify = autoNotify;
    }

    /**
     * Returns number of days in advance to create the transaction
     * <p>This flag is currently unused in the app. It is only included here for compatibility with GnuCash desktop XML</p>
     * @return Number of days in advance to create transaction
     */
    public int getAdvanceCreateDays() {
        return mAdvanceCreateDays;
    }

    /**
     * Set number of days in advance to create the transaction
     * <p>This flag is currently unused in the app. It is only included here for compatibility with GnuCash desktop XML</p>
     * @param advanceCreateDays Number of days
     */
    public void setAdvanceCreateDays(int advanceCreateDays) {
        this.mAdvanceCreateDays = advanceCreateDays;
    }

    /**
     * Returns the number of days in advance to notify of scheduled transactions
     * <p>This flag is currently unused in the app. It is only included here for compatibility with GnuCash desktop XML</p>
     * @return {@code true} if user will be notified, {@code false} otherwise
     */
    public int getAdvanceNotifyDays() {
        return mAdvanceNotifyDays;
    }

    /**
     * Set number of days in advance to notify of scheduled transactions
     * <p>This flag is currently unused in the app. It is only included here for compatibility with GnuCash desktop XML</p>
     * @param advanceNotifyDays Number of days
     */
    public void setAdvanceNotifyDays(int advanceNotifyDays) {
        this.mAdvanceNotifyDays = advanceNotifyDays;
    }

    /**
     * Return the template account GUID for this scheduled action
     * <p>This method generates one if none was set</p>
     * @return String GUID of template account
     */
    public String getTemplateAccountUID() {
        if (mTemplateAccountUID == null)
            return mTemplateAccountUID = generateUID();
        else
            return mTemplateAccountUID;
    }

    /**
     * Set the template account GUID
     * @param templateAccountUID String GUID of template account
     */
    public void setTemplateAccountUID(String templateAccountUID) {
        this.mTemplateAccountUID = templateAccountUID;
    }

    /**
     * Returns the event schedule (start, end and recurrence)
     * @return String description of repeat schedule
     */
    public String getRepeatString(){
        StringBuilder ruleBuilder = new StringBuilder(mRecurrence.getRepeatString());

        if (mEndDate > 0){
            ruleBuilder.append(", ");
            ruleBuilder.append(" until ")
                    .append(SimpleDateFormat.getDateInstance(DateFormat.SHORT).format(new Date(mEndDate)));
        } else if (mTotalFrequency > 0){
            ruleBuilder.append(", ");
            ruleBuilder.append(" for ").append(mTotalFrequency).append(" times");
        }
        return ruleBuilder.toString();
    }

    /**
     * Creates an RFC 2445 string which describes this recurring event
     * <p>See http://recurrance.sourceforge.net/</p>
     * @return String describing event
     */
    public String getRuleString(){
        String separator = ";";

        StringBuilder ruleBuilder = new StringBuilder(mRecurrence.getRuleString());

        if (mEndDate > 0){
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            ruleBuilder.append("UNTIL=").append(df.format(new Date(mEndDate))).append(separator);
        } else if (mTotalFrequency > 0){
            ruleBuilder.append("COUNT=").append(mTotalFrequency).append(separator);
        }

        return ruleBuilder.toString();
    }

    /**
     * Return GUID of recurrence pattern for this scheduled action
     * @return {@link Recurrence} object
     */
    public Recurrence getRecurrence() {
        return mRecurrence;
    }

    /**
     * Sets the recurrence pattern of this scheduled action
     * <p>This also sets the start period of the recurrence object, if there is one</p>
     * @param recurrence {@link Recurrence} object
     */
    public void setRecurrence(@NonNull Recurrence recurrence) {
        this.mRecurrence = recurrence;
        //if we were parsing XML and parsed the start and end date from the scheduled action first,
        //then use those over the values which might be gotten from the recurrence
        if (mStartDate > 0){
            mRecurrence.setPeriodStart(new Timestamp(mStartDate));
        } else {
            mStartDate = mRecurrence.getPeriodStart().getTime();
        }

        if (mEndDate > 0){
            mRecurrence.setPeriodEnd(new Timestamp(mEndDate));
        } else if (mRecurrence.getPeriodEnd() != null){
            mEndDate = mRecurrence.getPeriodEnd().getTime();
        }
    }

    /**
     * Creates a ScheduledAction from a Transaction and a period
     * @param transaction Transaction to be scheduled
     * @param period Period in milliseconds since Epoch
     * @return Scheduled Action
     * @deprecated Used for parsing legacy backup files. Use {@link Recurrence} instead
     */
    @Deprecated
    public static ScheduledAction parseScheduledAction(Transaction transaction, long period){
        ScheduledAction scheduledAction = new ScheduledAction(ActionType.TRANSACTION);
        scheduledAction.mActionUID = transaction.getUID();
        Recurrence recurrence = new Recurrence(PeriodType.parse(period));
        scheduledAction.setRecurrence(recurrence);
        return scheduledAction;
    }

    @Override
    public String toString() {
        return mActionType.name() + " - " + getRepeatString();
    }
}
