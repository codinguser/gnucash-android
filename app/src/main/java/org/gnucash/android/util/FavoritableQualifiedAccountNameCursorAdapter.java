/*
 * Copyright (c) 2013 - 2014 Ngewi Fet <ngewif@gmail.com>
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
import android.provider.ContactsContract;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;

/**
 * Cursor adapter which looks up the fully qualified account name and returns that instead of just the simple name.
 * <p>The fully qualified account name includes the parent hierarchy</p>
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class FavoritableQualifiedAccountNameCursorAdapter extends SimpleCursorAdapter {

    /**
     * Initialize the Cursor adapter for account names using default spinner views
     * @param context Application context
     * @param cursor Cursor to accounts
     */
    public FavoritableQualifiedAccountNameCursorAdapter(Context context, Cursor cursor) {
        super(context, R.layout.favorite_transfer_account_spinner_item, cursor,
                new String[]{DatabaseSchema.AccountEntry.COLUMN_FULL_NAME},
                new int[]{R.id.qualified_account_name}, 0);
        setDropDownViewResource(R.layout.favorite_transfer_account_spinner_item);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);

        Integer is_favorite = cursor.getInt(cursor.getColumnIndex(DatabaseSchema.AccountEntry.COLUMN_FAVORITE));
        String colName = cursor.getString(cursor.getColumnIndex(DatabaseSchema.AccountEntry.COLUMN_NAME));

        if(is_favorite == 0) {
            ImageView is_favorite_image = (ImageView) view.findViewById(R.id.favorite_status);
            is_favorite_image.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Returns the position of a given account in the adapter
     * @param accountUID GUID of the account
     * @return Position of the account or -1 if the account is not found
     */
    public int getPosition(@NonNull String accountUID){
        long accountId = AccountsDbAdapter.getInstance().getID(accountUID);
        for (int pos = 0; pos < getCount(); pos++) {
            if (getItemId(pos) == accountId){
                return pos;
            }
        }
        return -1;
    }
}
