package org.gnucash.android.db.adapter;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import androidx.annotation.NonNull;
import android.util.Pair;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.model.Price;
import org.gnucash.android.util.TimestampHelper;

import static org.gnucash.android.db.DatabaseSchema.PriceEntry;

/**
 * Database adapter for prices
 */
public class PricesDbAdapter extends DatabaseAdapter<Price> {
    /**
     * Opens the database adapter with an existing database
     * @param db SQLiteDatabase object
     */
    public PricesDbAdapter(SQLiteDatabase db) {
        super(db, PriceEntry.TABLE_NAME, new String[]{
                PriceEntry.COLUMN_COMMODITY_UID,
                PriceEntry.COLUMN_CURRENCY_UID,
                PriceEntry.COLUMN_DATE,
                PriceEntry.COLUMN_SOURCE,
                PriceEntry.COLUMN_TYPE,
                PriceEntry.COLUMN_VALUE_NUM,
                PriceEntry.COLUMN_VALUE_DENOM
        });
    }

    public static PricesDbAdapter getInstance(){
        return GnuCashApplication.getPricesDbAdapter();
    }

    @Override
    protected @NonNull SQLiteStatement setBindings(@NonNull SQLiteStatement stmt, @NonNull final Price price) {
        stmt.clearBindings();
        stmt.bindString(1, price.getCommodityUID());
        stmt.bindString(2, price.getCurrencyUID());
        stmt.bindString(3, price.getDate().toString());
        if (price.getSource() != null) {
            stmt.bindString(4, price.getSource());
        }
        if (price.getType() != null) {
            stmt.bindString(5, price.getType());
        }
        stmt.bindLong(6, price.getValueNum());
        stmt.bindLong(7, price.getValueDenom());
        stmt.bindString(8, price.getUID());

        return stmt;
    }

    @Override
    public Price buildModelInstance(@NonNull final Cursor cursor) {
        String commodityUID = cursor.getString(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_COMMODITY_UID));
        String currencyUID  = cursor.getString(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_CURRENCY_UID));
        String dateString   = cursor.getString(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_DATE));
        String source       = cursor.getString(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_SOURCE));
        String type         = cursor.getString(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_TYPE));
        long valueNum     = cursor.getLong(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_VALUE_NUM));
        long valueDenom   = cursor.getLong(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_VALUE_DENOM));

        Price price = new Price(commodityUID, currencyUID);
        price.setDate(TimestampHelper.getTimestampFromUtcString(dateString));
        price.setSource(source);
        price.setType(type);
        price.setValueNum(valueNum);
        price.setValueDenom(valueDenom);

        populateBaseModelAttributes(cursor, price);
        return price;
    }

    /**
     * Get the price for commodity / currency pair.
     * The price can be used to convert from one commodity to another. The 'commodity' is the origin and the 'currency' is the target for the conversion.
     *
     * <p>Pair is used instead of Price object because we must sometimes invert the commodity/currency in DB,
     * rendering the Price UID invalid.</p>
     *
     * @param commodityUID GUID of the commodity which is starting point for conversion
     * @param currencyUID GUID of target commodity for the conversion
     *
     * @return The numerator/denominator pair for commodity / currency pair
     */
    public Pair<Long, Long> getPrice(@NonNull String commodityUID, @NonNull String currencyUID) {
        Pair<Long, Long> pairZero = new Pair<>(0L, 0L);
        if (commodityUID.equals(currencyUID))
        {
            return new Pair<Long, Long>(1L, 1L);
        }
        Cursor cursor = mDb.query(PriceEntry.TABLE_NAME, null,
                // the commodity and currency can be swapped
                "( " + PriceEntry.COLUMN_COMMODITY_UID + " = ? AND " + PriceEntry.COLUMN_CURRENCY_UID + " = ? ) OR ( "
                + PriceEntry.COLUMN_COMMODITY_UID + " = ? AND " + PriceEntry.COLUMN_CURRENCY_UID + " = ? )",
                new String[]{commodityUID, currencyUID, currencyUID, commodityUID}, null, null,
                // only get the latest price
                PriceEntry.COLUMN_DATE + " DESC", "1");
        try {
            if (cursor.moveToNext()) {
                String commodityUIDdb = cursor.getString(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_COMMODITY_UID));
                long valueNum     = cursor.getLong(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_VALUE_NUM));
                long valueDenom   = cursor.getLong(cursor.getColumnIndexOrThrow(PriceEntry.COLUMN_VALUE_DENOM));
                if (valueNum < 0 || valueDenom < 0) {
                    // this should not happen
                    return pairZero;
                }
                if (!commodityUIDdb.equals(commodityUID)) {
                    // swap Num and denom
                    long t = valueNum;
                    valueNum = valueDenom;
                    valueDenom = t;
                }
                return new Pair<Long, Long>(valueNum, valueDenom);
            } else {
                return pairZero;
            }
        } finally {
            cursor.close();
        }
    }
}
