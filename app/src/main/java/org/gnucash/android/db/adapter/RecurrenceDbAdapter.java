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

package org.gnucash.android.db.adapter;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.model.PeriodType;
import org.gnucash.android.model.Recurrence;

import java.sql.Timestamp;

import static org.gnucash.android.db.DatabaseSchema.RecurrenceEntry;

/**
 * Database adapter for {@link Recurrence} entries
 */
public class RecurrenceDbAdapter extends DatabaseAdapter<Recurrence> {
    /**
     * Opens the database adapter with an existing database
     *
     * @param db        SQLiteDatabase object
     */
    public RecurrenceDbAdapter(SQLiteDatabase db) {
        super(db, RecurrenceEntry.TABLE_NAME, new String[]{
                RecurrenceEntry.COLUMN_MULTIPLIER,
                RecurrenceEntry.COLUMN_PERIOD_TYPE,
                RecurrenceEntry.COLUMN_BYDAY,
                RecurrenceEntry.COLUMN_PERIOD_START,
                RecurrenceEntry.COLUMN_PERIOD_END
        });
    }

    public static RecurrenceDbAdapter getInstance(){
        return GnuCashApplication.getRecurrenceDbAdapter();
    }

    @Override
    public Recurrence buildModelInstance(@NonNull Cursor cursor) {
        String type = cursor.getString(cursor.getColumnIndexOrThrow(RecurrenceEntry.COLUMN_PERIOD_TYPE));
        long multiplier = cursor.getLong(cursor.getColumnIndexOrThrow(RecurrenceEntry.COLUMN_MULTIPLIER));
        String periodStart = cursor.getString(cursor.getColumnIndexOrThrow(RecurrenceEntry.COLUMN_PERIOD_START));
        String periodEnd = cursor.getString(cursor.getColumnIndexOrThrow(RecurrenceEntry.COLUMN_PERIOD_END));
        String byDay = cursor.getString(cursor.getColumnIndexOrThrow(RecurrenceEntry.COLUMN_BYDAY));

        PeriodType periodType = PeriodType.valueOf(type);
        periodType.setMultiplier((int) multiplier);

        Recurrence recurrence = new Recurrence(periodType);
        recurrence.setPeriodStart(Timestamp.valueOf(periodStart));
        if (periodEnd != null)
            recurrence.setPeriodEnd(Timestamp.valueOf(periodEnd));
        recurrence.setByDay(byDay);

        populateBaseModelAttributes(cursor, recurrence);

        return recurrence;
    }

    @Override
    protected @NonNull SQLiteStatement setBindings(@NonNull SQLiteStatement stmt, @NonNull final Recurrence recurrence) {
        stmt.clearBindings();
        stmt.bindLong(1, recurrence.getPeriodType().getMultiplier());
        stmt.bindString(2, recurrence.getPeriodType().name());
        if (recurrence.getByDay() != null)
            stmt.bindString(3, recurrence.getByDay());
        //recurrence should always have a start date
        stmt.bindString(4, recurrence.getPeriodStart().toString());

        if (recurrence.getPeriodEnd() != null)
            stmt.bindString(5, recurrence.getPeriodEnd().toString());
        stmt.bindString(6, recurrence.getUID());

        return stmt;
    }
}
