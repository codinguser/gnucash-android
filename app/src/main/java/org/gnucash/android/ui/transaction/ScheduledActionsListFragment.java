/*
 * Copyright (c) 2013 - 2015 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.ui.transaction;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseCursorLoader;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.ScheduledActionDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.ui.common.FormActivity;
import org.gnucash.android.ui.common.UxArgument;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * Fragment which displays the scheduled actions in the system
 * <p>Currently, it handles the display of scheduled transactions and scheduled exports</p>
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ScheduledActionsListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Logging tag
     */
    protected static final String TAG = "ScheduledActionFragment";

    private TransactionsDbAdapter mTransactionsDbAdapter;
    private SimpleCursorAdapter mCursorAdapter;
    private ActionMode mActionMode = null;

    /**
     * Flag which is set when a transaction is selected
     */
    private boolean mInEditMode = false;

    private ScheduledAction.ActionType mActionType = ScheduledAction.ActionType.TRANSACTION;

    /**
     * Callbacks for the menu items in the Context ActionBar (CAB) in action mode
     */
    private ActionMode.Callback mActionModeCallbacks = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.transactions_context_menu, menu);
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
                case R.id.context_menu_delete:
                    for (long id : getListView().getCheckedItemIds()) {
                        Log.i(TAG, "Cancelling scheduled transaction(s)");
                        String trnUID = mTransactionsDbAdapter.getUID(id);
                        ScheduledActionDbAdapter scheduledActionDbAdapter = GnuCashApplication.getScheduledEventDbAdapter();
                        List<ScheduledAction> actions = scheduledActionDbAdapter.getScheduledActionsWithUID(trnUID);

                        if (mTransactionsDbAdapter.deleteRecord(id)){
                            Toast.makeText(getActivity(), R.string.toast_recurring_transaction_deleted, Toast.LENGTH_SHORT).show();
                            for (ScheduledAction action : actions) {
                                scheduledActionDbAdapter.deleteRecord(action.getUID());
                            }
                        }
                    }
                    mode.finish();
                    setDefaultStatusBarColor();
                    getLoaderManager().destroyLoader(0);
                    refreshList();
                    return true;

                default:
                    setDefaultStatusBarColor();
                    return false;
            }
        }
    };

    private void setDefaultStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().getWindow().setStatusBarColor(getResources().getColor(R.color.theme_primary_dark));
        }
    }

    /**
     * Returns a new instance of the fragment for displayed the scheduled action
     * @param actionType Type of scheduled action to be displayed
     * @return New instance of fragment
     */
    public static Fragment getInstance(ScheduledAction.ActionType actionType){
        ScheduledActionsListFragment fragment = new ScheduledActionsListFragment();
        fragment.mActionType = actionType;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();
        switch (mActionType){
            case TRANSACTION:
                mCursorAdapter = new ScheduledTransactionsCursorAdapter(
                        getActivity().getApplicationContext(),
                        R.layout.list_item_scheduled_trxn, null,
                        new String[] {DatabaseSchema.TransactionEntry.COLUMN_DESCRIPTION},
                        new int[] {R.id.primary_text});
                break;
            case BACKUP:
                mCursorAdapter = new ScheduledExportCursorAdapter(
                        getActivity().getApplicationContext(),
                        R.layout.list_item_scheduled_trxn, null,
                        new String[]{}, new int[]{});
                break;

            default:
                throw new IllegalArgumentException("Unable to display scheduled actions for the specified action type");

        }

        setListAdapter(mCursorAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scheduled_events_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        setHasOptionsMenu(true);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        ((TextView)getListView().getEmptyView()).setTextColor(getResources().getColor(R.color.theme_accent));
        if (mActionType == ScheduledAction.ActionType.TRANSACTION){
            ((TextView)getListView().getEmptyView()).setText(R.string.label_no_recurring_transactions);
        } else if (mActionType == ScheduledAction.ActionType.BACKUP){
            ((TextView)getListView().getEmptyView()).setText(R.string.label_no_scheduled_exports_to_display);
        }
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mActionType == ScheduledAction.ActionType.BACKUP)
            inflater.inflate(R.menu.scheduled_export_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_add_scheduled_export:
                Intent intent = new Intent(getActivity(), FormActivity.class);
                intent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.EXPORT.name());
                startActivityForResult(intent, 0x1);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if (mActionMode != null){
            CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox);
            checkbox.setChecked(!checkbox.isChecked());
            return;
        }

        if (mActionType == ScheduledAction.ActionType.BACKUP) //nothing to do for export actions
            return;

        Transaction transaction = mTransactionsDbAdapter.getRecord(id);

        //this should actually never happen, but has happened once. So perform check for the future
        if (transaction.getSplits().size() == 0){
            Toast.makeText(getActivity(), "The selected transaction has no splits and cannot be opened", Toast.LENGTH_SHORT).show();
            return;
        }

        String accountUID = transaction.getSplits().get(0).getAccountUID();
        openTransactionForEdit(accountUID, mTransactionsDbAdapter.getUID(id),
                v.getTag().toString());
    }

    /**
     * Opens the transaction editor to enable editing of the transaction
     * @param accountUID GUID of account to which transaction belongs
     * @param transactionUID GUID of transaction to be edited
     */
    public void openTransactionForEdit(String accountUID, String transactionUID, String scheduledActionUid){
        Intent createTransactionIntent = new Intent(getActivity(), FormActivity.class);
        createTransactionIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        createTransactionIntent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.TRANSACTION.name());
        createTransactionIntent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, accountUID);
        createTransactionIntent.putExtra(UxArgument.SELECTED_TRANSACTION_UID, transactionUID);
        createTransactionIntent.putExtra(UxArgument.SCHEDULED_ACTION_UID, scheduledActionUid);
        startActivity(createTransactionIntent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        Log.d(TAG, "Creating transactions loader");
        if (mActionType == ScheduledAction.ActionType.TRANSACTION)
            return new ScheduledTransactionsCursorLoader(getActivity());
        else if (mActionType == ScheduledAction.ActionType.BACKUP){
            return new ScheduledExportCursorLoader(getActivity());
        }
        return null;
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
        mActionMode = ((AppCompatActivity) getActivity())
                .startSupportActionMode(mActionModeCallbacks);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            getActivity().getWindow().setStatusBarColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    /**
     * Stops action mode and deselects all selected transactions.
     * This method only has effect if the number of checked items is greater than 0 and {@link #mActionMode} is not null
     */
    private void stopActionMode(){
        int checkedCount = getListView().getCheckedItemIds().length;
        if (checkedCount <= 0 && mActionMode != null) {
            mActionMode.finish();
            setDefaultStatusBarColor();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            refreshList();
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Extends a simple cursor adapter to bind transaction attributes to views
     * @author Ngewi Fet <ngewif@gmail.com>
     */
    protected class ScheduledTransactionsCursorAdapter extends SimpleCursorAdapter {

        public ScheduledTransactionsCursorAdapter(Context context, int layout, Cursor c,
                                                  String[] from, int[] to) {
            super(context, layout, c, from, to, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view = super.getView(position, convertView, parent);
            final int itemPosition = position;
            CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox);
            //TODO: Revisit this if we ever change the application theme
            int id = Resources.getSystem().getIdentifier("btn_check_holo_light", "drawable", "android");
            checkbox.setButtonDrawable(id);

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
                    if (isAdded()){ //may be run when fragment has been unbound from activity
                        float extraPadding = getResources().getDimension(R.dimen.edge_padding);
                        final android.graphics.Rect hitRect = new Rect();
                        checkBoxView.getHitRect(hitRect);
                        hitRect.right   += extraPadding;
                        hitRect.bottom  += 3*extraPadding;
                        hitRect.top     -= extraPadding;
                        hitRect.left    -= 2*extraPadding;
                        parentView.setTouchDelegate(new TouchDelegate(hitRect, checkBoxView));
                    }
                }
            });

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);

            Transaction transaction = mTransactionsDbAdapter.buildModelInstance(cursor);

            TextView amountTextView = (TextView) view.findViewById(R.id.right_text);
            if (transaction.getSplits().size() == 2){
                if (transaction.getSplits().get(0).isPairOf(transaction.getSplits().get(1))){
                    amountTextView.setText(transaction.getSplits().get(0).getValue().formattedString());
                }
            } else {
                amountTextView.setText(transaction.getSplits().size() + " splits");
            }
            TextView descriptionTextView = (TextView) view.findViewById(R.id.secondary_text);

            ScheduledActionDbAdapter scheduledActionDbAdapter = ScheduledActionDbAdapter.getInstance();
            String scheduledActionUID = cursor.getString(cursor.getColumnIndexOrThrow("origin_scheduled_action_uid")); //column created from join when fetching scheduled transactions
            view.setTag(scheduledActionUID);
            ScheduledAction scheduledAction = scheduledActionDbAdapter.getRecord(scheduledActionUID);
            long endTime = scheduledAction.getEndTime();
            if (endTime > 0 && endTime < System.currentTimeMillis()){
                ((TextView)view.findViewById(R.id.primary_text)).setTextColor(getResources().getColor(android.R.color.darker_gray));
                descriptionTextView.setText(getString(R.string.label_scheduled_action_ended,
                        DateFormat.getInstance().format(new Date(scheduledAction.getLastRunTime()))));
            } else {
                descriptionTextView.setText(scheduledAction.getRepeatString());
            }
        }
    }

    /**
     * Extends a simple cursor adapter to bind transaction attributes to views
     * @author Ngewi Fet <ngewif@gmail.com>
     */
    protected class ScheduledExportCursorAdapter extends SimpleCursorAdapter {

        public ScheduledExportCursorAdapter(Context context, int layout, Cursor c,
                                            String[] from, int[] to) {
            super(context, layout, c, from, to, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view = super.getView(position, convertView, parent);
            final int itemPosition = position;
            CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox);
            //TODO: Revisit this if we ever change the application theme
            int id = Resources.getSystem().getIdentifier("btn_check_holo_light", "drawable", "android");
            checkbox.setButtonDrawable(id);

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
                    if (isAdded()){ //may be run when fragment has been unbound from activity
                        float extraPadding = getResources().getDimension(R.dimen.edge_padding);
                        final android.graphics.Rect hitRect = new Rect();
                        checkBoxView.getHitRect(hitRect);
                        hitRect.right   += extraPadding;
                        hitRect.bottom  += 3*extraPadding;
                        hitRect.top     -= extraPadding;
                        hitRect.left    -= 2*extraPadding;
                        parentView.setTouchDelegate(new TouchDelegate(hitRect, checkBoxView));
                    }
                }
            });

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);
            ScheduledActionDbAdapter mScheduledActionDbAdapter = ScheduledActionDbAdapter.getInstance();
            ScheduledAction scheduledAction = mScheduledActionDbAdapter.buildModelInstance(cursor);

            TextView primaryTextView = (TextView) view.findViewById(R.id.primary_text);
            ExportParams params = ExportParams.parseCsv(scheduledAction.getTag());
            primaryTextView.setText(params.getExportFormat().name() + " "
                    + scheduledAction.getActionType().name().toLowerCase() + " to "
                    + params.getExportTarget().name().toLowerCase());

            view.findViewById(R.id.right_text).setVisibility(View.GONE);

            TextView descriptionTextView = (TextView) view.findViewById(R.id.secondary_text);
            descriptionTextView.setText(scheduledAction.getRepeatString());
            long endTime = scheduledAction.getEndTime();
            if (endTime > 0 && endTime < System.currentTimeMillis()){
                ((TextView)view.findViewById(R.id.primary_text)).setTextColor(getResources().getColor(android.R.color.darker_gray));
                descriptionTextView.setText(getString(R.string.label_scheduled_action_ended,
                        DateFormat.getInstance().format(new Date(scheduledAction.getLastRunTime()))));
            } else {
                descriptionTextView.setText(scheduledAction.getRepeatString());
            }
        }
    }


    /**
     * {@link DatabaseCursorLoader} for loading recurring transactions asynchronously from the database
     * @author Ngewi Fet <ngewif@gmail.com>
     */
    protected static class ScheduledTransactionsCursorLoader extends DatabaseCursorLoader {

        public ScheduledTransactionsCursorLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            mDatabaseAdapter = TransactionsDbAdapter.getInstance();

            Cursor c = ((TransactionsDbAdapter) mDatabaseAdapter).fetchAllScheduledTransactions();

            registerContentObserver(c);
            return c;
        }
    }

    /**
     * {@link DatabaseCursorLoader} for loading recurring transactions asynchronously from the database
     * @author Ngewi Fet <ngewif@gmail.com>
     */
    protected static class ScheduledExportCursorLoader extends DatabaseCursorLoader {

        public ScheduledExportCursorLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            mDatabaseAdapter = ScheduledActionDbAdapter.getInstance();

            Cursor c = mDatabaseAdapter.fetchAllRecords(
                    DatabaseSchema.ScheduledActionEntry.COLUMN_TYPE + "=?",
                    new String[]{ScheduledAction.ActionType.BACKUP.name()});

            registerContentObserver(c);
            return c;
        }
    }

}

