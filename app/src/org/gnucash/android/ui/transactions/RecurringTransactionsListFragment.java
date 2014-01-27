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

package org.gnucash.android.ui.transactions;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import org.gnucash.android.R;
import org.gnucash.android.data.Money;
import org.gnucash.android.data.Transaction;
import org.gnucash.android.db.*;
import org.gnucash.android.ui.widget.WidgetConfigurationActivity;

import java.util.Locale;

/**
 * Fragment which displays the recurring transactions in the system.
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class RecurringTransactionsListFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Logging tag
     */
    protected static final String TAG = "TransactionsListFragment";

    private TransactionsDbAdapter mTransactionsDbAdapter;
    private SimpleCursorAdapter mCursorAdapter;
    private ActionMode mActionMode = null;

    /**
     * Flag which is set when a transaction is selected
     */
    private boolean mInEditMode = false;


    /**
     * Callbacks for the menu items in the Context ActionBar (CAB) in action mode
     */
    private ActionMode.Callback mActionModeCallbacks = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.transactions_context_menu, menu);
            menu.removeItem(R.id.context_menu_move_transactions);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            //nothing to see here, move along
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            finishEditMode();
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.context_menu_move_transactions:
                    mode.finish();
                    WidgetConfigurationActivity.updateAllWidgets(getActivity());
                    return true;

                case R.id.context_menu_delete:
                    for (long id : getListView().getCheckedItemIds()) {
                        Log.i(TAG, "Cancelling recurring transaction(s)");

                        PendingIntent recurringPendingIntent = PendingIntent.getBroadcast(getActivity().getApplicationContext(),
                                (int)id, Transaction.createIntent(mTransactionsDbAdapter.getTransaction(id)), PendingIntent.FLAG_UPDATE_CURRENT);
                        recurringPendingIntent.cancel();
                        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
                        alarmManager.cancel(recurringPendingIntent);
                        if (mTransactionsDbAdapter.deleteRecord(id)){
                            Toast.makeText(getActivity(), R.string.toast_recurring_transaction_deleted, Toast.LENGTH_SHORT).show();
                        };
                    }
                    refreshList();
                    mode.finish();
                    WidgetConfigurationActivity.updateAllWidgets(getActivity());
                    return true;

                default:
                    return false;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTransactionsDbAdapter = new TransactionsDbAdapter(getActivity());
        mCursorAdapter = new TransactionsCursorAdapter(
                getActivity().getApplicationContext(),
                R.layout.list_item_transaction, null,
                new String[] {DatabaseHelper.KEY_NAME, DatabaseHelper.KEY_AMOUNT},
                new int[] {R.id.primary_text, R.id.transaction_amount});
        setListAdapter(mCursorAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recurring_transactions_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar aBar = getSherlockActivity().getSupportActionBar();
        aBar.setDisplayShowTitleEnabled(true);
        aBar.setDisplayHomeAsUpEnabled(true);
        aBar.setTitle("Recurring Transactions");

        setHasOptionsMenu(true);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }

    /**
     * Reload the list of transactions and recompute account balances
     */
    public void refreshList(){
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTransactionsDbAdapter.close();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if (mInEditMode){
            CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox_parent_account);
            checkbox.setChecked(!checkbox.isChecked());
            return;
        }
        //else
        String accountUID = mTransactionsDbAdapter.getAccountUidFromTransaction(id);
        long accountID = mTransactionsDbAdapter.getAccountID(accountUID);

        openTransactionForEdit(accountID, id);
    }

    /**
     * Opens the transaction editor to enable editing of the transaction
     * @param accountId Account ID of the transaction
     * @param transactionId Transaction to be edited
     */
    public void openTransactionForEdit(long accountId, long transactionId){
        Intent createTransactionIntent = new Intent(getActivity(), TransactionsActivity.class);
        createTransactionIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        createTransactionIntent.putExtra(TransactionsListFragment.SELECTED_ACCOUNT_ID, accountId);
        createTransactionIntent.putExtra(NewTransactionFragment.SELECTED_TRANSACTION_ID, transactionId);
        startActivity(createTransactionIntent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        inflater.inflate(R.menu.transactions_list_actions, menu);
        //remove menu items from the AccountsActivity
        menu.removeItem(R.id.menu_search);
        menu.removeItem(R.id.menu_settings);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        Log.d(TAG, "Creating transactions loader");
        return new RecurringTransactionsCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.d(TAG, "Transactions loader finished. Swapping in cursor");
        mCursorAdapter.swapCursor(cursor);
        mCursorAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, "Resetting transactions loader");
        mCursorAdapter.swapCursor(null);
    }

    /**
     * Finishes the edit mode in the transactions list.
     * Edit mode is started when at least one transaction is selected
     */
    public void finishEditMode(){
        mInEditMode = false;
        uncheckAllItems();
        mActionMode = null;
    }

    /**
     * Sets the title of the Context ActionBar when in action mode.
     * It sets the number highlighted items
     */
    public void setActionModeTitle(){
        int count = getListView().getCheckedItemIds().length; //mSelectedIds.size();
        if (count > 0){
            mActionMode.setTitle(getResources().getString(R.string.title_selected, count));
        }
    }

    /**
     * Unchecks all the checked items in the list
     */
    private void uncheckAllItems() {
        SparseBooleanArray checkedPositions = getListView().getCheckedItemPositions();
        ListView listView = getListView();
        for (int i = 0; i < checkedPositions.size(); i++) {
            int position = checkedPositions.keyAt(i);
            listView.setItemChecked(position, false);
        }
    }


    /**
     * Starts action mode and activates the Context ActionBar (CAB)
     * Action mode is initiated as soon as at least one transaction is selected (highlighted)
     */
    private void startActionMode(){
        if (mActionMode != null) {
            return;
        }
        mInEditMode = true;
        // Start the CAB using the ActionMode.Callback defined above
        mActionMode = getSherlockActivity().startActionMode(mActionModeCallbacks);
    }

    /**
     * Stops action mode and deselects all selected transactions.
     * This method only has effect if the number of checked items is greater than 0 and {@link #mActionMode} is not null
     */
    private void stopActionMode(){
        int checkedCount = getListView().getCheckedItemIds().length;
        if (checkedCount > 0 || mActionMode == null)
            return;
        else
            mActionMode.finish();
    }


    /**
     * Extends a simple cursor adapter to bind transaction attributes to views
     * @author Ngewi Fet <ngewif@gmail.com>
     */
    protected class TransactionsCursorAdapter extends SimpleCursorAdapter {

        public TransactionsCursorAdapter(Context context, int layout, Cursor c,
                                         String[] from, int[] to) {
            super(context, layout, c, from, to, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view = super.getView(position, convertView, parent);
            final int itemPosition = position;
            CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox_parent_account);
            final TextView secondaryText = (TextView) view.findViewById(R.id.secondary_text);

            checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    getListView().setItemChecked(itemPosition, isChecked);
                    if (isChecked) {
                        startActionMode();
                    } else {
                        stopActionMode();
                    }
                    setActionModeTitle();
                }
            });


            ListView listView = (ListView) parent;
            if (mInEditMode && listView.isItemChecked(position)){
                view.setBackgroundColor(getResources().getColor(R.color.abs__holo_blue_light));
                secondaryText.setTextColor(getResources().getColor(android.R.color.white));
            } else {
                view.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                secondaryText.setTextColor(getResources().getColor(android.R.color.secondary_text_light_nodisable));
                checkbox.setChecked(false);
            }

            final View checkBoxView = checkbox;
            final View parentView = view;
            parentView.post(new Runnable() {
                @Override
                public void run() {
                    float extraPadding = getResources().getDimension(R.dimen.edge_padding);
                    final android.graphics.Rect hitRect = new Rect();
                    checkBoxView.getHitRect(hitRect);
                    hitRect.right   += extraPadding;
                    hitRect.bottom  += 3*extraPadding;
                    hitRect.top     -= extraPadding;
                    hitRect.left    -= 2*extraPadding;
                    parentView.setTouchDelegate(new TouchDelegate(hitRect, checkBoxView));
                }
            });

            return view;
        }

        /**
         * Returns the string representation of the recurrence period of the transaction
         * @param periodMillis Recurrence period in milliseconds
         * @return String formatted representation of recurrence period
         */
        public String getRecurrenceAsString(long periodMillis){
            String[] recurrencePeriods = getResources().getStringArray(R.array.recurrence_options);
            String[] recurrenceStrings = getResources().getStringArray(R.array.recurrence_entries);

            int index = 0;
            for (String recurrencePeriod : recurrencePeriods) {
                long period = Long.parseLong(recurrencePeriod);
                if (period == periodMillis){
                    break;
                }
                index++;
            }
            return recurrenceStrings[index];
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);
            AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(getActivity());
            long accountID = accountsDbAdapter.getAccountID(cursor.getString(DatabaseAdapter.COLUMN_ACCOUNT_UID));

            Money amount = new Money(
                    cursor.getString(DatabaseAdapter.COLUMN_AMOUNT),
                    mTransactionsDbAdapter.getCurrencyCode(accountID));

            TextView tramount = (TextView) view.findViewById(R.id.transaction_amount);
            tramount.setText(amount.formattedString(Locale.getDefault()));

            if (amount.isNegative())
                tramount.setTextColor(getResources().getColor(R.color.debit_red));
            else
                tramount.setTextColor(getResources().getColor(R.color.credit_green));

            TextView trNote = (TextView) view.findViewById(R.id.secondary_text);
            trNote.setText("Repeats  " + getRecurrenceAsString(cursor.getLong(DatabaseAdapter.COLUMN_RECURRENCE_PERIOD))) ;

            String currentAccountUid = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ACCOUNT_UID));
            int position = cursor.getPosition();
            boolean hasSectionHeader;

            if (position == 0){
                hasSectionHeader = true;
            } else {
                cursor.moveToPosition(position - 1);
                String previousAccountUid = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ACCOUNT_UID));
                cursor.moveToPosition(position);

                hasSectionHeader = !previousAccountUid.equals(currentAccountUid);
            }

            TextView dateHeader = (TextView) view.findViewById(R.id.date_section_header);

            if (hasSectionHeader){
                dateHeader.setText(accountsDbAdapter.getFullyQualifiedAccountName(currentAccountUid));
                dateHeader.setVisibility(View.VISIBLE);
            } else {
                dateHeader.setVisibility(View.GONE);
            }

            accountsDbAdapter.close();
        }

    }

    /**
     * {@link DatabaseCursorLoader} for loading recurring transactions asynchronously from the database
     * @author Ngewi Fet <ngewif@gmail.com>
     */
    protected static class RecurringTransactionsCursorLoader extends DatabaseCursorLoader {

        public RecurringTransactionsCursorLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            mDatabaseAdapter = new TransactionsDbAdapter(getContext());
            Cursor c = ((TransactionsDbAdapter) mDatabaseAdapter).fetchAllRecurringTransactions();

            if (c != null)
                registerContentObserver(c);
            return c;
        }
    }

}

