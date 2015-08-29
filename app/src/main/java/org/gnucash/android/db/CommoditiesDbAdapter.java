package org.gnucash.android.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.model.Commodity;

import static org.gnucash.android.db.DatabaseSchema.CommodityEntry;

/**
 * Database adapter for {@link org.gnucash.android.model.Commodity}
 */
public class CommoditiesDbAdapter extends DatabaseAdapter<Commodity> {
    /**
     * Opens the database adapter with an existing database
     *
     * @param db        SQLiteDatabase object
     */
    public CommoditiesDbAdapter(SQLiteDatabase db) {
        super(db, CommodityEntry.TABLE_NAME);
    }

    public static CommoditiesDbAdapter getInstance(){
        return GnuCashApplication.getCommoditiesDbAdapter();
    }

    @Override
    protected SQLiteStatement compileReplaceStatement(@NonNull final Commodity commodity) {
        if (mReplaceStatement == null) {
            mReplaceStatement = mDb.compileStatement("REPLACE INTO " + CommodityEntry.TABLE_NAME + " ( "
                    + CommodityEntry.COLUMN_UID             + " , "
                    + CommodityEntry.COLUMN_FULLNAME        + " , "
                    + CommodityEntry.COLUMN_NAMESPACE       + " , "
                    + CommodityEntry.COLUMN_MNEMONIC        + " , "
                    + CommodityEntry.COLUMN_LOCAL_SYMBOL    + " , "
                    + CommodityEntry.COLUMN_CUSIP           + " , "
                    + CommodityEntry.COLUMN_FRACTION        + " , "
                    + CommodityEntry.COLUMN_QUOTE_FLAG      + " ) VALUES ( ? , ? , ? , ? , ? , ? , ? , ? ) ");
        }

        mReplaceStatement.clearBindings();
        mReplaceStatement.bindString(1, commodity.getUID());
        mReplaceStatement.bindString(2, commodity.getFullname());
        mReplaceStatement.bindString(3, commodity.getNamespace().name());
        mReplaceStatement.bindString(4, commodity.getMnemonic());
        mReplaceStatement.bindString(5, commodity.getLocalSymbol());
        mReplaceStatement.bindString(6, commodity.getCusip());
        mReplaceStatement.bindLong(7, commodity.getFraction());
        mReplaceStatement.bindLong(8,   commodity.getQuoteFlag());

        return mReplaceStatement;
    }

    @Override
    public Commodity buildModelInstance(@NonNull final Cursor cursor) {
        String fullname = cursor.getString(cursor.getColumnIndexOrThrow(CommodityEntry.COLUMN_FULLNAME));
        String mnemonic = cursor.getString(cursor.getColumnIndexOrThrow(CommodityEntry.COLUMN_MNEMONIC));
        String namespace = cursor.getString(cursor.getColumnIndexOrThrow(CommodityEntry.COLUMN_NAMESPACE));
        String cusip = cursor.getString(cursor.getColumnIndexOrThrow(CommodityEntry.COLUMN_CUSIP));
        String localSymbol = cursor.getString(cursor.getColumnIndexOrThrow(CommodityEntry.COLUMN_LOCAL_SYMBOL));

        int fraction = cursor.getInt(cursor.getColumnIndexOrThrow(CommodityEntry.COLUMN_FRACTION));
        int quoteFlag = cursor.getInt(cursor.getColumnIndexOrThrow(CommodityEntry.COLUMN_QUOTE_FLAG));

        Commodity commodity = new Commodity(fullname, mnemonic, fraction);
        commodity.setNamespace(Commodity.Namespace.valueOf(namespace));
        commodity.setCusip(cusip);
        commodity.setQuoteFlag(quoteFlag);
        commodity.setLocalSymbol(localSymbol);
        populateBaseModelAttributes(cursor, commodity);

        return commodity;
    }

    @Override
    public Cursor fetchAllRecords() {
        return mDb.query(mTableName, null, null, null, null, null,
                CommodityEntry.COLUMN_FULLNAME + " ASC");
    }

    /**
     * Returns the commodity associated with the ISO4217 currency code
     * @param currencyCode 3-letter currency code
     * @return Commodity associated with code or null if none is found
     */
    public Commodity getCommodity(String currencyCode){
        Cursor cursor = fetchAllRecords(CommodityEntry.COLUMN_MNEMONIC + "=?", new String[]{currencyCode});
        Commodity commodity = null;
        if (cursor.moveToNext()){
            commodity = buildModelInstance(cursor);
        }
        cursor.close();
        return commodity;
    }

    /**
     * Returns the commodity GUID for the given ISO 4217 currency code
     * @param currencyCode ISO 4217 currency code
     * @return GUID of commodity
     */
    public String getCommodityUID(String currencyCode){
        String where = CommodityEntry.COLUMN_MNEMONIC + "= ?";
        String[] whereArgs = new String[]{currencyCode};

        Cursor cursor = mDb.query(mTableName, new String[]{CommodityEntry.COLUMN_UID},
                where, whereArgs, null, null, null);
        try {
            if (cursor.moveToNext()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(CommodityEntry.COLUMN_UID));
            } else {
                throw new IllegalArgumentException("Currency code not found in commodities");
            }
        } finally {
            cursor.close();
        }
    }
}
