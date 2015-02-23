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
package org.gnucash.android.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema.ScheduledEventEntry;
import org.gnucash.android.model.ScheduledEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Database adapter for fetching/saving/modifying scheduled events
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ScheduledEventDbAdapter extends DatabaseAdapter {

    public ScheduledEventDbAdapter(SQLiteDatabase db){
        super(db, ScheduledEventEntry.TABLE_NAME);
    }

    /**
     * Returns application-wide instance of database adapter
     * @return ScheduledEventDbAdapter instance
     */
    public static ScheduledEventDbAdapter getInstance(){
        return GnuCashApplication.getScheduledEventDbAdapter();
    }

    /**
     * Adds a scheduled event to the database or replaces the existing entry if one with the same GUID exists
     * @param scheduledEvent {@link org.gnucash.android.model.ScheduledEvent} to be added
     * @return Database row ID of the newly created/replaced instance
     */
    public long addScheduledEvent(ScheduledEvent scheduledEvent){
        ContentValues contentValues = getContentValues(scheduledEvent);
        contentValues.put(ScheduledEventEntry.COLUMN_EVENT_UID, scheduledEvent.getEventUID());
        contentValues.put(ScheduledEventEntry.COLUMN_PERIOD,    scheduledEvent.getPeriod());
        contentValues.put(ScheduledEventEntry.COLUMN_START_TIME, scheduledEvent.getStartTime());
        contentValues.put(ScheduledEventEntry.COLUMN_END_TIME,  scheduledEvent.getEndTime());
        contentValues.put(ScheduledEventEntry.COLUMN_LAST_RUN,  scheduledEvent.getLastRun());
        contentValues.put(ScheduledEventEntry.COLUMN_TYPE,      scheduledEvent.getEventType().name());
        contentValues.put(ScheduledEventEntry.COLUMN_TAG,       scheduledEvent.getTag());

        Log.d(TAG, "Replace scheduled event in the db");
        return mDb.replace(ScheduledEventEntry.TABLE_NAME, null, contentValues);
    }

    /**
     * Builds a {@link org.gnucash.android.model.ScheduledEvent} instance from a row to cursor in the database.
     * The cursor should be already pointing to the right entry in the data set. It will not be modified in any way
     * @param cursor Cursor pointing to data set
     * @return ScheduledEvent object instance
     */
    private ScheduledEvent buildScheduledEventInstance(final Cursor cursor){
        String eventUid = cursor.getString(cursor.getColumnIndexOrThrow(ScheduledEventEntry.COLUMN_EVENT_UID));
        long period     = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduledEventEntry.COLUMN_PERIOD));
        long startTime  = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduledEventEntry.COLUMN_START_TIME));
        long endTime    = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduledEventEntry.COLUMN_END_TIME));
        long lastRun    = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduledEventEntry.COLUMN_LAST_RUN));
        String typeString = cursor.getString(cursor.getColumnIndexOrThrow(ScheduledEventEntry.COLUMN_TYPE));
        String tag      = cursor.getString(cursor.getColumnIndexOrThrow(ScheduledEventEntry.COLUMN_TAG));

        ScheduledEvent event = new ScheduledEvent(ScheduledEvent.EventType.valueOf(typeString));
        populateModel(cursor, event);
        event.setPeriod(period);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setEventUID(eventUid);
        event.setLastRun(lastRun);
        event.setTag(tag);

        return event;
    }

    /**
     * Returns an instance of {@link org.gnucash.android.model.ScheduledEvent} from the database record
     * @param uid GUID of event
     * @return ScheduledEvent object instance
     */
    public ScheduledEvent getScheduledEvent(String uid){
        Cursor cursor = fetchRecord(getID(uid));

        ScheduledEvent scheduledEvent = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                scheduledEvent = buildScheduledEventInstance(cursor);
            }
            cursor.close();
        }
        return scheduledEvent;
    }

    /**
     * Returns an instance of {@link org.gnucash.android.model.ScheduledEvent} from the database record
     * @param eventUID GUID of the event itself
     * @return ScheduledEvent object instance
     */
    public ScheduledEvent getScheduledEventWithUID(String eventUID){
        Cursor cursor = mDb.query(ScheduledEventEntry.TABLE_NAME, null,
                ScheduledEventEntry.COLUMN_EVENT_UID + "= ?",
                new String[]{eventUID}, null, null, null);

        ScheduledEvent scheduledEvent = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                scheduledEvent = buildScheduledEventInstance(cursor);
            }
            cursor.close();
        }
        return scheduledEvent;
    }

    /**
     * Returns all scheduled events in the database
     * @return List with all scheduled events
     */
    public List<ScheduledEvent> getAllScheduledEvents(){
        Cursor cursor = fetchAllRecords();
        List<ScheduledEvent> scheduledEvents = new ArrayList<ScheduledEvent>();
        while (cursor.moveToNext()){
            scheduledEvents.add(buildScheduledEventInstance(cursor));
        }
        return scheduledEvents;
    }

}
