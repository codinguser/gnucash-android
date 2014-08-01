/*
 * Copyright (c) 2014 Ngewi Fet <ngewif@gmail.com>
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

import java.util.UUID;

/**
* Represents a scheduled event which is stored in the database and run at regular period
*
* @author Ngewi Fet <ngewif@gmail.com>
*/
public class ScheduledEvent {
    public long period;
    public long startDate;
    public long endDate;

    /**
     * Types of events which can be scheduled
     */
    public enum EventType {TRANSACTION};

    /**
     * Unique ID of scheduled event
     */
    private String mUID;

    /**
     * Next scheduled run of Event
     */
    private long mNextRun;

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
        mUID = UUID.randomUUID().toString().replaceAll("-", "");
        mEventType = eventType;
    }

    public String getUID(){
        return mUID;
    }

    public void setUID(String uid){
        this.mUID = uid;
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

    public long getNextRun() {
        return mNextRun;
    }

    public void setNextRun(long nextRun) {
        this.mNextRun = nextRun;
    }
}
