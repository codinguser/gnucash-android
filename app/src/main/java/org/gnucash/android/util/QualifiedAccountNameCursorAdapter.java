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
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
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
public class QualifiedAccountNameCursorAdapter
        extends SimpleCursorAdapter {

    //
    private int _spinnerSelectedItemLayout;
    private int _spinnerDropDownItemLayout;

    /**
     * Overloaded constructor. Specifies the view to use for displaying selected spinner text
     *
     * @param context
     *         Application context
     * @param cursor
     *         Cursor to account data
     * @param spinnerSelectedItemLayout
     *         Layout resource for selected item text
     */
    public QualifiedAccountNameCursorAdapter(Context context,
                                             Cursor cursor,
                                             @LayoutRes int spinnerSelectedItemLayout,
                                             @LayoutRes int spinnerDropDownItemLayout
                                            ) {

        super(context,
              spinnerSelectedItemLayout,  // Layout of the closed spinner item
              cursor,
              new String[]{DatabaseSchema.AccountEntry.COLUMN_FULL_NAME},
              new int[]{android.R.id.text1},
              0);

        // Store layout of each item in the open drop down of the spinner
        setSpinnerSelectedItemLayout(spinnerSelectedItemLayout);

        // Store layout of each item in the open drop down of the spinner
        setSpinnerDropDownItemLayout(spinnerDropDownItemLayout);
    }

    /**
     * Overloaded constructor. Specifies the view to use for displaying selected spinner text
     *
     * @param context
     *         Application context
     * @param cursor
     *         Cursor to account data
     * @param selectedSpinnerItemLayout
     *         Layout resource for selected item text
     */
    public QualifiedAccountNameCursorAdapter(Context context,
                                             Cursor cursor,
                                             @LayoutRes int selectedSpinnerItemLayout) {

        this(context,
             cursor,
             selectedSpinnerItemLayout,  // Layout of the closed spinner item
             R.layout.account_spinner_dropdown_item
            );
    }

    /**
     * Initialize the Cursor adapter for account names using default spinner views
     *
     * @param context
     *         Application context
     * @param cursor
     *         Cursor to accounts
     */
    public QualifiedAccountNameCursorAdapter(Context context,
                                             Cursor cursor) {

        this(context,
             cursor,
             android.R.layout.simple_spinner_item  // Layout of the closed spinner item
            );
    }

    @Override
    public void bindView(View view,
                         Context context,
                         Cursor cursor) {

        super.bindView(view,
                       context,
                       cursor);

        // item text
        TextView text1 = (TextView) view.findViewById(android.R.id.text1);

        Integer isFavorite = cursor.getInt(cursor.getColumnIndex(DatabaseSchema.AccountEntry.COLUMN_FAVORITE));

        if (isFavorite == 0) {
            text1.setCompoundDrawablesWithIntrinsicBounds(0,
                                                             0,
                                                             0,
                                                             0);
        } else {
            text1.setCompoundDrawablesWithIntrinsicBounds(0,
                                                             0,
                                                             R.drawable.ic_star_black_18dp,
                                                             0);
        }
    }

    /**
     * Returns the position of a given account in the adapter
     *
     * @param accountUID
     *         GUID of the account
     *
     * @return Position of the account or -1 if the account is not found
     */
    public int getPosition(@NonNull String accountUID) {

        long accountId = AccountsDbAdapter.getInstance()
                                          .getID(accountUID);

        for (int pos = 0; pos < getCount(); pos++) {

            if (getItemId(pos) == accountId) {
                return pos;
            }
        }
        
        return -1;
    }

    public int getSpinnerSelectedItemLayout() {

        return _spinnerSelectedItemLayout;
    }

    public void setSpinnerSelectedItemLayout(int spinnerSelectedItemLayout) {

        _spinnerSelectedItemLayout = spinnerSelectedItemLayout;
    }

    public int getSpinnerDropDownItemLayout() {

        return _spinnerDropDownItemLayout;
    }

    public void setSpinnerDropDownItemLayout(int spinnerDropDownItemLayout) {

        _spinnerDropDownItemLayout = spinnerDropDownItemLayout;

        setDropDownViewResource(getSpinnerDropDownItemLayout());
    }

}
