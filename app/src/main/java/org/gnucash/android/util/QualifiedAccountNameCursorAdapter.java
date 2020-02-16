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
import android.widget.Spinner;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.ui.transaction.TransactionFormFragment;

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
              spinnerSelectedItemLayout,// Layout of the closed spinner item
              cursor,
              new String[]{DatabaseSchema.AccountEntry.COLUMN_FULL_NAME,
                           DatabaseSchema.AccountEntry.COLUMN_NAME},
              new int[]{android.R.id.text1,
                        R.id.text2},
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
//             R.layout.account_spinner_dropdown_item
             R.layout.account_spinner_dropdown_item_2lines
//             R.layout.list_item_2_lines
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

        //
        // Set Account Color
        //

        String accountUID = cursor.getString(cursor.getColumnIndex(DatabaseSchema.AccountEntry.COLUMN_UID));

        TextView simpleAccountNameTextView = (TextView) view.findViewById(R.id.text2);

        TransactionFormFragment.setAccountTextColor(simpleAccountNameTextView,
                                                    accountUID);

        //
        // Put Parent Account Full Name in text3
        //

        TextView parentAccountFullNameTextView = (TextView) view.findViewById(R.id.text3);

        if (parentAccountFullNameTextView != null) {
            //

            // TODO TW C 2020-02-15 : A factoriser et commenter

            String accountFullName = cursor.getString(cursor.getColumnIndex(DatabaseSchema.AccountEntry.COLUMN_FULL_NAME));

            String parentAccountFullName;
            int    index = accountFullName.lastIndexOf(AccountsDbAdapter.ACCOUNT_NAME_SEPARATOR);

            if (index > 0) {
                //

                //
                parentAccountFullName = accountFullName.substring(0,
                                                                  index);

            } else {
                //  n' pas

                parentAccountFullName="";
            }

            // Display Parent Account Full Name
            parentAccountFullNameTextView.setText(parentAccountFullName);

        } else {
            //  n' pas

            // RAF
        }

        //
        // Add or not Favorite Star Icon
        //

        Integer isFavorite = cursor.getInt(cursor.getColumnIndex(DatabaseSchema.AccountEntry.COLUMN_FAVORITE));

        displayFavoriteAccountStarIcon(view,
                                       isFavorite);

    }

    /**
     * @param spinnerSelectedItemView
     * @param isFavorite
     */
    public static void displayFavoriteAccountStarIcon(View spinnerSelectedItemView,
                                                      Integer isFavorite) {

        TextView simpleAccountNameTextView = (TextView) spinnerSelectedItemView.findViewById(R.id.text2);

        if (simpleAccountNameTextView != null) {
            //

            //
            if (isFavorite == 0) {

                // Hide Favorite Account Star
                hideFavoriteAccountStarIcon(spinnerSelectedItemView);

            } else {

                // Display Favorite Account Star
                simpleAccountNameTextView.setCompoundDrawablesWithIntrinsicBounds(0,
                                                                                  0,
                                                                                  R.drawable.ic_star_black_18dp,
                                                                                  0);
            }

        } else {
            //  n' pas

            // RAF
        }
    }

    /**
     * Removes the icon from view to avoid visual clutter
     *
     * @param spinnerView
     */
    public static void hideFavoriteAccountStarIcon(View spinnerView) {

        TextView textViewWithStarIcon = (TextView) spinnerView.findViewById(R.id.text2);

        if (textViewWithStarIcon != null) {

            textViewWithStarIcon.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
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
