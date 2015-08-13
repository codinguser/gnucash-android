package org.gnucash.android.db;

import android.database.sqlite.SQLiteDatabase;
import static org.gnucash.android.db.DatabaseSchema.PriceEntry;

/**
 * Database adapter for prices
 */
public class PricesDbAdapter extends DatabaseAdapter {
    /**
     * Opens the database adapter with an existing database
     *
     * @param db        SQLiteDatabase object
     */
    public PricesDbAdapter(SQLiteDatabase db) {
        super(db, PriceEntry.TABLE_NAME);
    }
}
