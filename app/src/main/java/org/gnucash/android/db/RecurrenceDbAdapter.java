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

package org.gnucash.android.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.model.PeriodType;
import org.gnucash.android.model.Recurrence;

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
        super(db, RecurrenceEntry.TABLE_NAME);
    }

    public static RecurrenceDbAdapter getInstance(){
        return GnuCashApplication.getRecurrenceDbAdapter();
    }

    @Override
    protected Recurrence buildModelInstance(@NonNull Cursor cursor) {
        String type = cursor.getString(cursor.getColumnIndexOrThrow(RecurrenceEntry.COLUMN_PERIOD_TYPE));
        long multiplier = cursor.getLong(cursor.getColumnIndexOrThrow(RecurrenceEntry.COLUMN_MULTIPLIER));
        String periodStart = cursor.getString(cursor.getColumnIndexOrThrow(RecurrenceEntry.COLUMN_PERIOD_START));

        PeriodType periodType = PeriodType.valueOf(type);
        periodType.setMultiplier((int) multiplier);

        Recurrence recurrence = new Recurrence(periodType);
        recurrence.setPeriodStart(periodStart);
        populateBaseModelAttributes(cursor, recurrence);

        return recurrence;
    }

    @Override
    protected SQLiteStatement compileReplaceStatement(@NonNull Recurrence recurrence) {
        if (mReplaceStatement == null) {
            mReplaceStatement = mDb.compileStatement("REPLACE INTO " + RecurrenceEntry.TABLE_NAME + " ( "
                    + RecurrenceEntry.COLUMN_UID + " , "
                    + RecurrenceEntry.COLUMN_MULTIPLIER + " , "
                    + RecurrenceEntry.COLUMN_PERIOD_TYPE + " , "
                    + RecurrenceEntry.COLUMN_PERIOD_START + " ) VALUES ( ? , ? , ? , ? ) ");
        }

        mReplaceStatement.clearBindings();
        mReplaceStatement.bindString(1, recurrence.getUID());
        mReplaceStatement.bindLong(2,   recurrence.getPeriodType().getMultiplier());
        mReplaceStatement.bindString(3, recurrence.getPeriodType().name());
        if (recurrence.getPeriodStart() != null)
            mReplaceStatement.bindString(4, recurrence.getPeriodStart());

        return mReplaceStatement;
    }
}
