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
import org.gnucash.android.model.Budget;
import org.gnucash.android.model.Money;

import static org.gnucash.android.db.DatabaseSchema.BudgetEntry;

/**
 * Database adapter for accessing {@link org.gnucash.android.model.Budget} records
 */
public class BudgetDbAdapter extends DatabaseAdapter<Budget>{

    private RecurrenceDbAdapter mRecurrenceDbAdapter;

    /**
     * Opens the database adapter with an existing database
     *
     * @param db        SQLiteDatabase object
     */
    public BudgetDbAdapter(SQLiteDatabase db) {
        super(db, BudgetEntry.TABLE_NAME);
        mRecurrenceDbAdapter = new RecurrenceDbAdapter(db);
    }

    /**
     * Returns an instance of the budget database adapter
     * @return BudgetDbAdapter instance
     */
    public static BudgetDbAdapter getInstance(){
        return GnuCashApplication.getBudgetDbAdapter();
    }

    @Override
    public Budget buildModelInstance(@NonNull Cursor cursor) {
        String name = cursor.getString(cursor.getColumnIndexOrThrow(BudgetEntry.COLUMN_NAME));
        String description = cursor.getString(cursor.getColumnIndexOrThrow(BudgetEntry.COLUMN_DESCRIPTION));
        String accountUID = cursor.getString(cursor.getColumnIndexOrThrow(BudgetEntry.COLUMN_ACCOUNT_UID));
        String recurrenceUID = cursor.getString(cursor.getColumnIndexOrThrow(BudgetEntry.COLUMN_RECURRENCE_UID));
        long amountNum = cursor.getLong(cursor.getColumnIndexOrThrow(BudgetEntry.COLUMN_AMOUNT_NUM));
        long amountDenom = cursor.getLong(cursor.getColumnIndexOrThrow(BudgetEntry.COLUMN_AMOUNT_DENOM));
        long numPeriods = cursor.getLong(cursor.getColumnIndexOrThrow(BudgetEntry.COLUMN_NUM_PERIODS));

        String currencyCode = getAccountCurrencyCode(accountUID);
        Budget budget = new Budget(name, new Money(amountNum, amountDenom, currencyCode));
        budget.setDescription(description);
        budget.setRecurrence(mRecurrenceDbAdapter.getRecord(recurrenceUID));
        budget.setNumberOfPeriods(numPeriods);
        populateBaseModelAttributes(cursor, budget);

        return budget;
    }

    @Override
    protected SQLiteStatement compileReplaceStatement(@NonNull Budget budget) {
        if (mReplaceStatement == null){
            mReplaceStatement = mDb.compileStatement("REPLACE INTO " + BudgetEntry.TABLE_NAME + " ( "
                    + BudgetEntry.COLUMN_UID            + " , "
                    + BudgetEntry.COLUMN_NAME           + " , "
                    + BudgetEntry.COLUMN_DESCRIPTION    + " , "
                    + BudgetEntry.COLUMN_ACCOUNT_UID    + " , "
                    + BudgetEntry.COLUMN_AMOUNT_NUM     + " , "
                    + BudgetEntry.COLUMN_AMOUNT_DENOM   + " , "
                    + BudgetEntry.COLUMN_RECURRENCE_UID + " , "
                    + BudgetEntry.COLUMN_NUM_PERIODS    + " ) VALUES ( ? , ? , ? , ? , ? , ? , ? , ? ) ");
        }

        mReplaceStatement.clearBindings();
        mReplaceStatement.bindString(1, budget.getUID());
        mReplaceStatement.bindString(2, budget.getName());
        if (budget.getDescription() != null)
            mReplaceStatement.bindString(3, budget.getDescription());
        mReplaceStatement.bindString(4, budget.getAccountUID());
        mReplaceStatement.bindLong(5, budget.getAmount().getNumerator());
        mReplaceStatement.bindLong(6, budget.getAmount().getDenominator());
        mReplaceStatement.bindString(7, budget.getRecurrence().getUID());
        mReplaceStatement.bindLong(8, budget.getNumberOfPeriods());

        return mReplaceStatement;
    }
}
