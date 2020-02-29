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
import android.widget.FilterQueryProvider;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.ui.util.AccountUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Cursor adapter which looks up the fully qualified account name and returns that instead of just the simple name.
 * <p>The fully qualified account name includes the parent hierarchy</p>
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class QualifiedAccountNameCursorAdapter
        extends SimpleCursorAdapter {

    /**
     * Removes the icon from view to avoid visual clutter
     *
     * @param spinnerView
     */
    public static void hideFavoriteAccountStarIcon(View spinnerView) {

        TextView textViewWithStarIcon = (TextView) spinnerView.findViewById(R.id.text2);

        if (textViewWithStarIcon != null) {

            textViewWithStarIcon.setCompoundDrawablesWithIntrinsicBounds(0,
                                                                         0,
                                                                         0,
                                                                         0);
        }
    }

    // Layout of the selected spinner item (the one with down arrow)
    private int mSpinnerSelectedItemLayout;

    // Layout of items in the drop down list
    private int mSpinnerDropDownItemLayout;

    // Clause WHERE du Cursor (in order to be replayed by the item filter)
    private String   mCursorWhere;
    private String[] mCursorWhereArgs;

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
                                             String cursorWhere,
                                             String[] cursorWhereArgs,
                                             @LayoutRes int spinnerSelectedItemLayout,
                                             @LayoutRes int spinnerDropDownItemLayout
                                            ) {

        super(context,
              spinnerSelectedItemLayout,// Layout of the selected spinner item
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

        // Store the WHERE clause associated with the Cursor
        setCursorWhere(cursorWhere);
        setCursorWhereArgs(cursorWhereArgs);

        // Define filter
        setFilterQueryProvider(new FilterQueryProvider() {

            public Cursor runQuery(CharSequence constraint) {

                //
                // Add %constraint% at the end of the whereArgs
                //

                // Convert WhereArgs into List
                final String[] cursorWhereArgs = getCursorWhereArgs();
                final List<String> whereArgsAsList = (cursorWhereArgs != null)
                                                     ? new ArrayList<String>(Arrays.asList(cursorWhereArgs))
                                                     : new ArrayList<String>();

                // Add the %constraint% for the LIKE added in the where clause
                whereArgsAsList.add("%" + ((constraint != null)
                                           ? constraint.toString()
                                           : "") + "%");

                // Convert List into WhereArgs
                final String[] whereArgs = whereArgsAsList.toArray(new String[whereArgsAsList.size()]);


                //
                // Run the original query but constrained with full account name containing constraint
                //

                final AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();

                final String where = ((getCursorWhere() != null)
                                      ? getCursorWhere() + " AND "
                                      : "")
                                     + DatabaseSchema.AccountEntry.COLUMN_FULL_NAME
                                     + " LIKE ?";

                final Cursor accountsCursor = accountsDbAdapter.fetchAccountsOrderedByFavoriteAndFullName(where,
                                                                                                          whereArgs);

                return accountsCursor;
            }
        });
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
                                             String cursorWhere,
                                             String[] cursorWhereArgs,
                                             @LayoutRes int selectedSpinnerItemLayout) {

        this(context,
             cursor,
             cursorWhere,
             cursorWhereArgs,
             selectedSpinnerItemLayout,  // Layout of the closed spinner item
             R.layout.account_spinner_dropdown_item_2lines
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
             null,
             null,
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

        setTextColorAccordingToAccountUID(view,
                                          accountUID);


        //
        // Put Parent Account Full Name in text3
        //

        TextView parentAccountFullNameTextView = (TextView) view.findViewById(R.id.text3);

        if (parentAccountFullNameTextView != null) {
            // The field exists

            String accountFullName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_FULL_NAME));

            // Get Parent account Full Name
            String parentAccountFullName = getParentAccountFullName(accountFullName);

            // Display Parent Account Full Name
            parentAccountFullNameTextView.setText(parentAccountFullName);

        } else {
            // The field does not exist

            // NTD
        }

        //
        // Add or not Favorite Star Icon
        //

        Integer isFavorite = cursor.getInt(cursor.getColumnIndex(DatabaseSchema.AccountEntry.COLUMN_FAVORITE));

        displayFavoriteAccountStarIcon(view,
                                       isFavorite);

    }

    /**
     * Extract parent account full name
     *
     * @param accountFullName
     *         Account full name
     *
     * @return parent account full name
     */
    String getParentAccountFullName(final String accountFullName) {

        String parentAccountFullName;

        // Look for last separator
        int index = accountFullName.lastIndexOf(AccountsDbAdapter.ACCOUNT_NAME_SEPARATOR);

        if (index > 0) {
            // An account separator has been found

            // parent full name is before the account separator
            parentAccountFullName = accountFullName.substring(0,
                                                              index);

        } else {
            // No account separator has been found

            // Do not display anything for parent
            parentAccountFullName = "";
        }
        return parentAccountFullName;
    }

    /**
     * Set text color according to account one
     *
     * @param view
     *         View containing text field to colorize
     * @param accountUID
     *         Account UID
     */
    void setTextColorAccordingToAccountUID(final View view,
                                           final String accountUID) {

        TextView simpleAccountNameTextView = (TextView) view.findViewById(R.id.text2);

        AccountUtils.setAccountTextColor(simpleAccountNameTextView,
                                         accountUID);
    }

    /**
     * Display or hide star icon according to favorite account status
     *
     * @param spinnerSelectedItemView
     * @param isFavoriteAccount
     */
    void displayFavoriteAccountStarIcon(View spinnerSelectedItemView,
                                        Integer isFavoriteAccount) {

        TextView simpleAccountNameTextView = (TextView) spinnerSelectedItemView.findViewById(R.id.text2);

        if (simpleAccountNameTextView != null) {
            // The field exists

            if (isFavoriteAccount == 0) {
                // It is not a Favorite account

                // Hide Favorite Account Star
                hideFavoriteAccountStarIcon(spinnerSelectedItemView);

            } else {
                // It is a Favorite account

                // Display Favorite Account Star
                simpleAccountNameTextView.setCompoundDrawablesWithIntrinsicBounds(0,
                                                                                  0,
                                                                                  R.drawable.ic_star_black_18dp,
                                                                                  0);
            }

        } else {
            // The field does not exists

            // NTD
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

    //
    // Getters/Setters
    //

    String getCursorWhere() {

        return mCursorWhere;
    }

    protected void setCursorWhere(final String cursorWhere) {

        mCursorWhere = cursorWhere;
    }

    String[] getCursorWhereArgs() {

        return mCursorWhereArgs;
    }

    protected void setCursorWhereArgs(final String[] cursorWhereArgs) {

        mCursorWhereArgs = cursorWhereArgs;
    }

    public int getSpinnerSelectedItemLayout() {

        return mSpinnerSelectedItemLayout;
    }

    public void setSpinnerSelectedItemLayout(int spinnerSelectedItemLayout) {

        mSpinnerSelectedItemLayout = spinnerSelectedItemLayout;
    }

    public int getSpinnerDropDownItemLayout() {

        return mSpinnerDropDownItemLayout;
    }

    public void setSpinnerDropDownItemLayout(int spinnerDropDownItemLayout) {

        mSpinnerDropDownItemLayout = spinnerDropDownItemLayout;

        setDropDownViewResource(getSpinnerDropDownItemLayout());
    }

}
