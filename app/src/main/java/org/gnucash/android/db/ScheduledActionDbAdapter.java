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
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.util.Log;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.model.ScheduledAction;

import java.util.ArrayList;
import java.util.List;

import static org.gnucash.android.db.DatabaseSchema.ScheduledActionEntry;

/**
 * Database adapter for fetching/saving/modifying scheduled events
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ScheduledActionDbAdapter extends DatabaseAdapter {

    public ScheduledActionDbAdapter(SQLiteDatabase db){
        super(db, ScheduledActionEntry.TABLE_NAME);
    }

    /**
     * Returns application-wide instance of database adapter
     * @return ScheduledEventDbAdapter instance
     */
    public static ScheduledActionDbAdapter getInstance(){
        return GnuCashApplication.getScheduledEventDbAdapter();
    }

    /**
     * Adds a scheduled event to the database or replaces the existing entry if one with the same GUID exists
     * @param scheduledAction {@link org.gnucash.android.model.ScheduledAction} to be added
     * @return Database row ID of the newly created/replaced instance
     */
    public long addScheduledAction(ScheduledAction scheduledAction){
        ContentValues contentValues = getContentValues(scheduledAction);
        contentValues.put(ScheduledActionEntry.COLUMN_ACTION_UID, scheduledAction.getActionUID());
        contentValues.put(ScheduledActionEntry.COLUMN_PERIOD,    scheduledAction.getPeriod());
        contentValues.put(ScheduledActionEntry.COLUMN_START_TIME, scheduledAction.getStartTime());
        contentValues.put(ScheduledActionEntry.COLUMN_END_TIME,  scheduledAction.getEndTime());
        contentValues.put(ScheduledActionEntry.COLUMN_LAST_RUN,  scheduledAction.getLastRun());
        contentValues.put(ScheduledActionEntry.COLUMN_TYPE,      scheduledAction.getActionType().name());
        contentValues.put(ScheduledActionEntry.COLUMN_TAG,       scheduledAction.getTag());
        contentValues.put(ScheduledActionEntry.COLUMN_ENABLED,   scheduledAction.isEnabled() ? "1":"0");
        contentValues.put(ScheduledActionEntry.COLUMN_NUM_OCCURRENCES, scheduledAction.getNumberOfOccurences());
        contentValues.put(ScheduledActionEntry.COLUMN_EXECUTION_COUNT, scheduledAction.getExecutionCount());

        Log.d(TAG, "Replace scheduled event in the db");
        return mDb.replace(ScheduledActionEntry.TABLE_NAME, null, contentValues);
    }

    /**
     * Adds a multiple scheduled actions to the database in one transaction.
     * @param scheduledActionList List of ScheduledActions
     * @return Returns the number of rows inserted
     */
    public int bulkAddScheduledActions(List<ScheduledAction> scheduledActionList){
        Log.d(TAG, "Bulk adding scheduled actions to the database");
        int nRow = 0;
        try {
            mDb.beginTransaction();
            SQLiteStatement replaceStatement = mDb.compileStatement("REPLACE INTO " + ScheduledActionEntry.TABLE_NAME + " ( "
                    + ScheduledActionEntry.COLUMN_UID 	            + " , "
                    + ScheduledActionEntry.COLUMN_ACTION_UID        + " , "
                    + ScheduledActionEntry.COLUMN_TYPE              + " , "
                    + ScheduledActionEntry.COLUMN_START_TIME        + " , "
                    + ScheduledActionEntry.COLUMN_END_TIME          + " , "
                    + ScheduledActionEntry.COLUMN_LAST_RUN 		    + " , "
                    + ScheduledActionEntry.COLUMN_PERIOD 	        + " , "
                    + ScheduledActionEntry.COLUMN_ENABLED           + " , "
                    + ScheduledActionEntry.COLUMN_CREATED_AT        + " , "
                    + ScheduledActionEntry.COLUMN_TAG               + " , "
                    + ScheduledActionEntry.COLUMN_NUM_OCCURRENCES   + " , "
                    + ScheduledActionEntry.COLUMN_EXECUTION_COUNT   + " ) VALUES ( ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? )");
            for (ScheduledAction schedxAction:scheduledActionList) {
                replaceStatement.clearBindings();
                replaceStatement.bindString(1,  schedxAction.getUID());
                replaceStatement.bindString(2,  schedxAction.getActionUID());
                replaceStatement.bindString(3,  schedxAction.getActionType().name());
                replaceStatement.bindLong(4,    schedxAction.getStartTime());
                replaceStatement.bindLong(5,    schedxAction.getEndTime());
                replaceStatement.bindLong(6,    schedxAction.getLastRun());
                replaceStatement.bindLong(7,    schedxAction.getPeriod());
                replaceStatement.bindLong(8,    schedxAction.isEnabled() ? 1 : 0);
                replaceStatement.bindString(9,  schedxAction.getCreatedTimestamp().toString());
                replaceStatement.bindString(10, schedxAction.getTag());
                replaceStatement.bindString(11, Integer.toString(schedxAction.getNumberOfOccurences()));
                replaceStatement.bindString(12, Integer.toString(schedxAction.getExecutionCount()));

                replaceStatement.execute();
                nRow ++;
            }
            mDb.setTransactionSuccessful();
        }
        finally {
            mDb.endTransaction();
        }
        return nRow;
    }
    /**
     * Builds a {@link org.gnucash.android.model.ScheduledAction} instance from a row to cursor in the database.
     * The cursor should be already pointing to the right entry in the data set. It will not be modified in any way
     * @param cursor Cursor pointing to data set
     * @return ScheduledEvent object instance
     */
    private ScheduledAction buildScheduledEventInstance(final Cursor cursor){
        String actionUid = cursor.getString(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_ACTION_UID));
        long period     = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_PERIOD));
        long startTime  = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_START_TIME));
        long endTime    = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_END_TIME));
        long lastRun    = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_LAST_RUN));
        String typeString = cursor.getString(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_TYPE));
        String tag      = cursor.getString(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_TAG));
        boolean enabled = cursor.getInt(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_ENABLED)) > 0;
        int numOccurrences = cursor.getInt(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_NUM_OCCURRENCES));
        int execCount = cursor.getInt(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_EXECUTION_COUNT));

        ScheduledAction event = new ScheduledAction(ScheduledAction.ActionType.valueOf(typeString));
        populateModel(cursor, event);
        event.setPeriod(period);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setActionUID(actionUid);
        event.setLastRun(lastRun);
        event.setTag(tag);
        event.setEnabled(enabled);
        event.setNumberOfOccurences(numOccurrences);
        event.setExecutionCount(execCount);

        return event;
    }

    /**
     * Returns an instance of {@link org.gnucash.android.model.ScheduledAction} from the database record
     * @param uid GUID of event
     * @return ScheduledEvent object instance
     */
    public ScheduledAction getScheduledEvent(String uid){
        Cursor cursor = fetchRecord(getID(uid));

        ScheduledAction scheduledAction = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                scheduledAction = buildScheduledEventInstance(cursor);
            }
            cursor.close();
        }
        return scheduledAction;
    }

    /**
     * Returns all {@link org.gnucash.android.model.ScheduledAction}s from the database with the specified event UID
     * @param eventUID GUID of the event itself
     * @return List of ScheduledEvents
     */
    public List<ScheduledAction> getScheduledEventsWithUID(@NonNull String eventUID){
        Cursor cursor = mDb.query(ScheduledActionEntry.TABLE_NAME, null,
                ScheduledActionEntry.COLUMN_ACTION_UID + "= ?",
                new String[]{eventUID}, null, null, null);

        List<ScheduledAction> scheduledActions = new ArrayList<ScheduledAction>();
        try {
            while (cursor.moveToNext()) {
                scheduledActions.add(buildScheduledEventInstance(cursor));
            }
        } finally {
            cursor.close();
        }
        return scheduledActions;
    }

    /**
     * Returns all scheduled events in the database
     * @return List with all scheduled events
     */
    public List<ScheduledAction> getAllScheduledEvents(){
        Cursor cursor = fetchAllRecords();
        List<ScheduledAction> scheduledActions = new ArrayList<ScheduledAction>();
        while (cursor.moveToNext()){
            scheduledActions.add(buildScheduledEventInstance(cursor));
        }
        return scheduledActions;
    }

    /**
     * Returns all enabled scheduled actions in the database
     * @return List of enalbed scheduled actions
     */
    public List<ScheduledAction> getAllEnabledScheduledActions(){
        Cursor cursor = mDb.query(mTableName,
                        null, ScheduledActionEntry.COLUMN_ENABLED + "=1", null, null, null, null);
        List<ScheduledAction> scheduledActions = new ArrayList<ScheduledAction>();
        while (cursor.moveToNext()){
            scheduledActions.add(buildScheduledEventInstance(cursor));
        }
        return scheduledActions;
    }

}
