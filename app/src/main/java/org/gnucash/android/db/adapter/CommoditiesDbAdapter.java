package org.gnucash.android.db.adapter;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
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
        super(db, CommodityEntry.TABLE_NAME, new String[]{
                CommodityEntry.COLUMN_FULLNAME,
                CommodityEntry.COLUMN_NAMESPACE,
                CommodityEntry.COLUMN_MNEMONIC,
                CommodityEntry.COLUMN_LOCAL_SYMBOL,
                CommodityEntry.COLUMN_CUSIP,
                CommodityEntry.COLUMN_SMALLEST_FRACTION,
                CommodityEntry.COLUMN_QUOTE_FLAG
        });
        /**
         * initialize commonly used commodities
         */
        Commodity.USD = getCommodity("USD");
        Commodity.EUR = getCommodity("EUR");
        Commodity.GBP = getCommodity("GBP");
        Commodity.CHF = getCommodity("CHF");
        Commodity.CAD = getCommodity("CAD");
        Commodity.JPY = getCommodity("JPY");
        Commodity.AUD = getCommodity("AUD");

        Commodity.DEFAULT_COMMODITY = getCommodity(GnuCashApplication.getDefaultCurrencyCode());
    }

    public static CommoditiesDbAdapter getInstance(){
        return GnuCashApplication.getCommoditiesDbAdapter();
    }

    @Override
    protected @NonNull SQLiteStatement setBindings(@NonNull SQLiteStatement stmt, @NonNull final Commodity commodity) {
        stmt.clearBindings();
        stmt.bindString(1, commodity.getFullname());
        stmt.bindString(2, commodity.getNamespace().name());
        stmt.bindString(3, commodity.getMnemonic());
        stmt.bindString(4, commodity.getLocalSymbol());
        stmt.bindString(5, commodity.getCusip());
        stmt.bindLong(6, commodity.getSmallestFraction());
        stmt.bindLong(7, commodity.getQuoteFlag());
        stmt.bindString(8, commodity.getUID());

        return stmt;
    }

    @Override
    public Commodity buildModelInstance(@NonNull final Cursor cursor) {
        String fullname = cursor.getString(cursor.getColumnIndexOrThrow(CommodityEntry.COLUMN_FULLNAME));
        String mnemonic = cursor.getString(cursor.getColumnIndexOrThrow(CommodityEntry.COLUMN_MNEMONIC));
        String namespace = cursor.getString(cursor.getColumnIndexOrThrow(CommodityEntry.COLUMN_NAMESPACE));
        String cusip = cursor.getString(cursor.getColumnIndexOrThrow(CommodityEntry.COLUMN_CUSIP));
        String localSymbol = cursor.getString(cursor.getColumnIndexOrThrow(CommodityEntry.COLUMN_LOCAL_SYMBOL));

        int fraction = cursor.getInt(cursor.getColumnIndexOrThrow(CommodityEntry.COLUMN_SMALLEST_FRACTION));
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
     * Fetches all commodities in the database sorted in the specified order
     * @param orderBy SQL statement for orderBy without the ORDER_BY itself
     * @return Cursor holding all commodity records
     */
    public Cursor fetchAllRecords(String orderBy) {
        return mDb.query(mTableName, null, null, null, null, null,
                orderBy);
    }

    /**
     * Returns the commodity associated with the ISO4217 currency code
     * @param currencyCode 3-letter currency code
     * @return Commodity associated with code or null if none is found
     */
    public Commodity getCommodity(String currencyCode){
        Cursor cursor = fetchAllRecords(CommodityEntry.COLUMN_MNEMONIC + "=?", new String[]{currencyCode}, null);
        Commodity commodity = null;
        if (cursor.moveToNext()){
            commodity = buildModelInstance(cursor);
        } else {
            String msg = "Commodity not found in the database: " + currencyCode;
            Log.e(LOG_TAG, msg);
            Crashlytics.log(msg);
        }
        cursor.close();
        return commodity;
    }

    public String getCurrencyCode(@NonNull String guid) {
        Cursor cursor = mDb.query(mTableName, new String[]{CommodityEntry.COLUMN_MNEMONIC},
                DatabaseSchema.CommonColumns.COLUMN_UID + " = ?", new String[]{guid},
                null, null, null);
        try {
            if (cursor.moveToNext()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(CommodityEntry.COLUMN_MNEMONIC));
            } else {
                throw new IllegalArgumentException("guid " + guid + " not exits in commodity db");
            }
        } finally {
            cursor.close();
        }
    }
}
