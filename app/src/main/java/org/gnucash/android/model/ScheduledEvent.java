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

import org.gnucash.android.R;
import org.gnucash.android.ui.util.RecurrenceParser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

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
     * Type of event being scheduled
     */
    private EventType mEventType;

    public ScheduledEvent(EventType eventType){
        mEventType = eventType;
        mStartDate = System.currentTimeMillis();
        mEndDate = 0;
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

    public long getStartTime() {
        return mStartDate;
    }

    public void setStartTime(long startDate) {
        this.mStartDate = startDate;
    }

    public long getEndTime() {
        return mEndDate;
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
