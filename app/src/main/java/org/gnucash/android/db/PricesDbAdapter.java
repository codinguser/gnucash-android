package org.gnucash.android.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.util.Log;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.model.Price;

import java.sql.Timestamp;

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
        super(db, PriceEntry.TABLE_NAME);
    }

    public static PricesDbAdapter getInstance(){
        return GnuCashApplication.getPricesDbAdapter();
    }

    @Override
    protected SQLiteStatement compileReplaceStatement(@NonNull final Price price) {
        if (mReplaceStatement == null) {
            mReplaceStatement = mDb.compileStatement("REPLACE INTO " + PriceEntry.TABLE_NAME + " ( "
                    + PriceEntry.COLUMN_UID + " , "
                    + PriceEntry.COLUMN_COMMODITY_UID + " , "
                    + PriceEntry.COLUMN_CURRENCY_UID + " , "
                    + PriceEntry.COLUMN_DATE + " , "
                    + PriceEntry.COLUMN_SOURCE + " , "
                    + PriceEntry.COLUMN_TYPE + " , "
                    + PriceEntry.COLUMN_VALUE_NUM + " , "
                    + PriceEntry.COLUMN_VALUE_DENOM + " ) VALUES ( ? , ? , ? , ? , ? , ? , ? , ? ) ");
        }

        mReplaceStatement.clearBindings();
        mReplaceStatement.bindString(1, price.getUID());
        mReplaceStatement.bindString(2, price.getCommodityUID());
        mReplaceStatement.bindString(3, price.getCurrencyUID());
        mReplaceStatement.bindString(4, price.getDate().toString());
        if (price.getSource() != null) {
            mReplaceStatement.bindString(5, price.getSource());
        }
        if (price.getType() != null) {
            mReplaceStatement.bindString(6, price.getType());
        }
        mReplaceStatement.bindLong(7,   price.getValueNum());
        mReplaceStatement.bindLong(8,   price.getValueDenom());

        return mReplaceStatement;
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
        price.setDate(Timestamp.valueOf(dateString));
        price.setSource(source);
        price.setType(type);
        price.setValueNum(valueNum);
        price.setValueDenom(valueDenom);

        populateBaseModelAttributes(cursor, price);
        return price;
    }
}
