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
package org.gnucash.android.db.adapter;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.util.Log;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.model.Recurrence;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.util.TimestampHelper;

import java.util.ArrayList;
import java.util.List;

import static org.gnucash.android.db.DatabaseSchema.ScheduledActionEntry;

/**
 * Database adapter for fetching/saving/modifying scheduled events
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ScheduledActionDbAdapter extends DatabaseAdapter<ScheduledAction> {

    RecurrenceDbAdapter mRecurrenceDbAdapter;

    public ScheduledActionDbAdapter(SQLiteDatabase db, RecurrenceDbAdapter recurrenceDbAdapter){
        super(db, ScheduledActionEntry.TABLE_NAME,  new String[]{
                ScheduledActionEntry.COLUMN_ACTION_UID        ,
                ScheduledActionEntry.COLUMN_TYPE              ,
                ScheduledActionEntry.COLUMN_START_TIME        ,
                ScheduledActionEntry.COLUMN_END_TIME          ,
                ScheduledActionEntry.COLUMN_LAST_RUN 		  ,
                ScheduledActionEntry.COLUMN_ENABLED           ,
                ScheduledActionEntry.COLUMN_CREATED_AT        ,
                ScheduledActionEntry.COLUMN_TAG               ,
                ScheduledActionEntry.COLUMN_TOTAL_FREQUENCY   ,
                ScheduledActionEntry.COLUMN_RECURRENCE_UID    ,
                ScheduledActionEntry.COLUMN_AUTO_CREATE       ,
                ScheduledActionEntry.COLUMN_AUTO_NOTIFY       ,
                ScheduledActionEntry.COLUMN_ADVANCE_CREATION  ,
                ScheduledActionEntry.COLUMN_ADVANCE_NOTIFY    ,
                ScheduledActionEntry.COLUMN_TEMPLATE_ACCT_UID ,
                ScheduledActionEntry.COLUMN_EXECUTION_COUNT
        });
        mRecurrenceDbAdapter = recurrenceDbAdapter;
        LOG_TAG = "ScheduledActionDbAdapter";
    }

    /**
     * Returns application-wide instance of database adapter
     * @return ScheduledEventDbAdapter instance
     */
    public static ScheduledActionDbAdapter getInstance(){
        return GnuCashApplication.getScheduledEventDbAdapter();
    }

    @Override
    public void addRecord(@NonNull ScheduledAction scheduledAction, UpdateMethod updateMethod) {
        mRecurrenceDbAdapter.addRecord(scheduledAction.getRecurrence(), updateMethod);
        super.addRecord(scheduledAction, updateMethod);
    }

    @Override
    public long bulkAddRecords(@NonNull List<ScheduledAction> scheduledActions, UpdateMethod updateMethod) {
        List<Recurrence> recurrenceList = new ArrayList<>(scheduledActions.size());
        for (ScheduledAction scheduledAction : scheduledActions) {
            recurrenceList.add(scheduledAction.getRecurrence());
        }

        //first add the recurrences, they have no dependencies (foreign key constraints)
        long nRecurrences = mRecurrenceDbAdapter.bulkAddRecords(recurrenceList, updateMethod);
        Log.d(LOG_TAG, String.format("Added %d recurrences for scheduled actions", nRecurrences));

        return super.bulkAddRecords(scheduledActions, updateMethod);
    }

    /**
     * Updates only the recurrence attributes of the scheduled action.
     * The recurrence attributes are the period, start time, end time and/or total frequency.
     * All other properties of a scheduled event are only used for interal database tracking and are
     * not central to the recurrence schedule.
     * <p><b>The GUID of the scheduled action should already exist in the database</b></p>
     * @param scheduledAction Scheduled action
     * @return Database record ID of the edited scheduled action
     */
    public long updateRecurrenceAttributes(ScheduledAction scheduledAction){
        //since we are updating, first fetch the existing recurrence UID and set it to the object
        //so that it will be updated and not a new one created
        RecurrenceDbAdapter recurrenceDbAdapter = RecurrenceDbAdapter.getInstance();
        String recurrenceUID = recurrenceDbAdapter.getAttribute(scheduledAction.getUID(), ScheduledActionEntry.COLUMN_RECURRENCE_UID);

        Recurrence recurrence = scheduledAction.getRecurrence();
        recurrence.setUID(recurrenceUID);
        recurrenceDbAdapter.addRecord(recurrence, UpdateMethod.update);

        ContentValues contentValues = new ContentValues();
        extractBaseModelAttributes(contentValues, scheduledAction);
        contentValues.put(ScheduledActionEntry.COLUMN_START_TIME, scheduledAction.getStartTime());
        contentValues.put(ScheduledActionEntry.COLUMN_END_TIME,  scheduledAction.getEndTime());
        contentValues.put(ScheduledActionEntry.COLUMN_TAG,       scheduledAction.getTag());
        contentValues.put(ScheduledActionEntry.COLUMN_TOTAL_FREQUENCY, scheduledAction.getTotalFrequency());

        Log.d(LOG_TAG, "Updating scheduled event recurrence attributes");
        String where = ScheduledActionEntry.COLUMN_UID + "=?";
        String[] whereArgs = new String[]{scheduledAction.getUID()};
        return mDb.update(ScheduledActionEntry.TABLE_NAME, contentValues, where, whereArgs);
    }

    @Override
    protected @NonNull SQLiteStatement setBindings(@NonNull SQLiteStatement stmt, @NonNull final ScheduledAction schedxAction) {
        stmt.clearBindings();
        stmt.bindString(1, schedxAction.getActionUID());
        stmt.bindString(2, schedxAction.getActionType().name());
        stmt.bindLong(3,   schedxAction.getStartTime());
        stmt.bindLong(4, schedxAction.getEndTime());
        stmt.bindLong(5, schedxAction.getLastRunTime());
        stmt.bindLong(6, schedxAction.isEnabled() ? 1 : 0);
        stmt.bindString(7, schedxAction.getCreatedTimestamp().toString());
        if (schedxAction.getTag() == null)
            stmt.bindNull(8);
        else
            stmt.bindString(9, schedxAction.getTag());
        stmt.bindString(9, Integer.toString(schedxAction.getTotalFrequency()));
        stmt.bindString(10, schedxAction.getRecurrence().getUID());
        stmt.bindLong(11,   schedxAction.shouldAutoCreate() ? 1 : 0);
        stmt.bindLong(12,   schedxAction.shouldAutoNotify() ? 1 : 0);
        stmt.bindLong(13,   schedxAction.getAdvanceCreateDays());
        stmt.bindLong(14,   schedxAction.getAdvanceNotifyDays());
        stmt.bindString(15, schedxAction.getTemplateAccountUID());

        stmt.bindString(16, Integer.toString(schedxAction.getExecutionCount()));
        stmt.bindString(17, schedxAction.getUID());
        return stmt;
    }
    /**
     * Builds a {@link org.gnucash.android.model.ScheduledAction} instance from a row to cursor in the database.
     * The cursor should be already pointing to the right entry in the data set. It will not be modified in any way
     * @param cursor Cursor pointing to data set
     * @return ScheduledEvent object instance
     */
    @Override
    public ScheduledAction buildModelInstance(@NonNull final Cursor cursor){
        String actionUid = cursor.getString(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_ACTION_UID));
        long startTime  = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_START_TIME));
        long endTime    = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_END_TIME));
        long lastRun    = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_LAST_RUN));
        String typeString = cursor.getString(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_TYPE));
        String tag      = cursor.getString(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_TAG));
        boolean enabled = cursor.getInt(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_ENABLED)) > 0;
        int numOccurrences = cursor.getInt(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_TOTAL_FREQUENCY));
        int execCount = cursor.getInt(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_EXECUTION_COUNT));
        int autoCreate = cursor.getInt(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_AUTO_CREATE));
        int autoNotify = cursor.getInt(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_AUTO_NOTIFY));
        int advanceCreate = cursor.getInt(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_ADVANCE_CREATION));
        int advanceNotify = cursor.getInt(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_ADVANCE_NOTIFY));
        String recurrenceUID = cursor.getString(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_RECURRENCE_UID));
        String templateActUID = cursor.getString(cursor.getColumnIndexOrThrow(ScheduledActionEntry.COLUMN_TEMPLATE_ACCT_UID));

        ScheduledAction event = new ScheduledAction(ScheduledAction.ActionType.valueOf(typeString));
        populateBaseModelAttributes(cursor, event);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setActionUID(actionUid);
        event.setLastRun(lastRun);
        event.setTag(tag);
        event.setEnabled(enabled);
        event.setTotalFrequency(numOccurrences);
        event.setExecutionCount(execCount);
        event.setAutoCreate(autoCreate == 1);
        event.setAutoNotify(autoNotify == 1);
        event.setAdvanceCreateDays(advanceCreate);
        event.setAdvanceNotifyDays(advanceNotify);
        //TODO: optimize by doing overriding fetchRecord(String) and join the two tables
        event.setRecurrence(mRecurrenceDbAdapter.getRecord(recurrenceUID));
        event.setTemplateAccountUID(templateActUID);

        return event;
    }

    /**
     * Returns all {@link org.gnucash.android.model.ScheduledAction}s from the database with the specified action UID.
     * Note that the parameter is not of the the scheduled action record, but from the action table
     * @param actionUID GUID of the event itself
     * @return List of ScheduledEvents
     */
    public List<ScheduledAction> getScheduledActionsWithUID(@NonNull String actionUID){
        Cursor cursor = mDb.query(ScheduledActionEntry.TABLE_NAME, null,
                ScheduledActionEntry.COLUMN_ACTION_UID + "= ?",
                new String[]{actionUID}, null, null, null);

        List<ScheduledAction> scheduledActions = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                scheduledActions.add(buildModelInstance(cursor));
            }
        } finally {
            cursor.close();
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
        List<ScheduledAction> scheduledActions = new ArrayList<>();
        while (cursor.moveToNext()){
            scheduledActions.add(buildModelInstance(cursor));
        }
        return scheduledActions;
    }

    /**
     * Returns the number of instances of the action which have been created from this scheduled action
     * @param scheduledActionUID GUID of scheduled action
     * @return Number of transactions created from scheduled action
     */
    public long getActionInstanceCount(String scheduledActionUID) {
        String sql = "SELECT COUNT(*) FROM " + DatabaseSchema.TransactionEntry.TABLE_NAME
                + " WHERE " + DatabaseSchema.TransactionEntry.COLUMN_SCHEDX_ACTION_UID + "=?";
        SQLiteStatement statement = mDb.compileStatement(sql);
        statement.bindString(1, scheduledActionUID);
        return statement.simpleQueryForLong();
    }
}
