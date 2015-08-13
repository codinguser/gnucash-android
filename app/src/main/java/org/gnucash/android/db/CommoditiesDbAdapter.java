package org.gnucash.android.db;

import android.database.sqlite.SQLiteDatabase;
import static org.gnucash.android.db.DatabaseSchema.CommodityEntry;

/**
 * Database adapter for {@link org.gnucash.android.model.Commodity}
 */
public class CommoditiesDbAdapter extends DatabaseAdapter {
    /**
     * Opens the database adapter with an existing database
     *
     * @param db        SQLiteDatabase object
     */
    public CommoditiesDbAdapter(SQLiteDatabase db) {
        super(db, CommodityEntry.TABLE_NAME);
    }




}
