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

package org.gnucash.android.util;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.LayoutRes;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import org.gnucash.android.db.adapter.CommoditiesDbAdapter;
import org.gnucash.android.db.DatabaseSchema;

/**
 * Cursor adapter for displaying list of commodities.
 * <p>You should provide the layout and the layout should contain a view with the id {@code android:id/text1},
 * which is where the name of the commodity will be displayed</p>
 * <p>The list is sorted by the currency code (which is also displayed first before the full name)</p>
 */
public class CommoditiesCursorAdapter extends SimpleCursorAdapter {

    public CommoditiesCursorAdapter(Context context, @LayoutRes int itemLayoutResource) {
        super(context, itemLayoutResource,
                CommoditiesDbAdapter.getInstance().fetchAllRecords(DatabaseSchema.CommodityEntry.COLUMN_MNEMONIC + " ASC"),
                new String[]{DatabaseSchema.CommodityEntry.COLUMN_FULLNAME},
                new int[] {android.R.id.text1}, 0);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        textView.setEllipsize(TextUtils.TruncateAt.MIDDLE);

        String currencyName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.CommodityEntry.COLUMN_FULLNAME));
        String currencyCode = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.CommodityEntry.COLUMN_MNEMONIC));

        textView.setText(currencyCode + " - " + currencyName);
    }
}
