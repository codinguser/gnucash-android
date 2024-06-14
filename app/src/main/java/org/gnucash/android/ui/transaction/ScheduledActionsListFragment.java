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

import static org.gnucash.android.util.ContentExtKt.getDocumentName;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseCursorLoader;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.ScheduledActionDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.export.ExportParams;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.ui.common.FormActivity;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.util.BackupManager;
import org.joda.time.format.DateTimeFormat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Fragment which displays the scheduled actions in the system
 * <p>Currently, it handles the display of scheduled transactions and scheduled exports</p>
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ScheduledActionsListFragment extends ListFragment implements
    LoaderManager.LoaderCallbacks<Cursor> {

    private TransactionsDbAdapter mTransactionsDbAdapter;
    private SimpleCursorAdapter mCursorAdapter;
    private ActionMode mActionMode = null;

    private ScheduledAction.ActionType mActionType = ScheduledAction.ActionType.TRANSACTION;

    /**
     * Callbacks for the menu items in the Context ActionBar (CAB) in action mode
     */
    private ActionMode.Callback mActionModeCallbacks = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.schedxactions_context_menu, menu);
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
                    final Activity activity = requireActivity();
                    BackupManager.backupActiveBookAsync(activity, result -> {
                        afterBackup(activity, mode);
                        return null;
                    });
                    return true;

                default:
                    setDefaultStatusBarColor();
                    return false;
            }
        }
    };

    private void afterBackup(Context context, ActionMode mode) {
        for (long id : getListView().getCheckedItemIds()) {
            if (mActionType == ScheduledAction.ActionType.TRANSACTION) {
                Timber.i("Cancelling scheduled transaction(s)");
                String trnUID = mTransactionsDbAdapter.getUID(id);
                ScheduledActionDbAdapter scheduledActionDbAdapter = GnuCashApplication.getScheduledEventDbAdapter();
                List<ScheduledAction> actions = scheduledActionDbAdapter.getScheduledActionsWithUID(trnUID);

                if (mTransactionsDbAdapter.deleteRecord(id)) {
                    Toast.makeText(context,
                        R.string.toast_recurring_transaction_deleted,
                        Toast.LENGTH_SHORT).show();
                    for (ScheduledAction action : actions) {
                        scheduledActionDbAdapter.deleteRecord(action.getUID());
                    }
                }
            } else if (mActionType == ScheduledAction.ActionType.BACKUP) {
                Timber.i("Removing scheduled exports");
                ScheduledActionDbAdapter.getInstance().deleteRecord(id);
            }
        }
        mode.finish();
        setDefaultStatusBarColor();
        getLoaderManager().destroyLoader(0);
        refreshList();
    }

    private void setDefaultStatusBarColor() {
        Activity activity = getActivity();
        if (activity == null) return;
        activity.getWindow().setStatusBarColor(ContextCompat.getColor(activity, R.color.theme_primary_dark));
    }

    /**
     * Returns a new instance of the fragment for displayed the scheduled action
     *
     * @param actionType Type of scheduled action to be displayed
     * @return New instance of fragment
     */
    public static Fragment getInstance(ScheduledAction.ActionType actionType) {
        ScheduledActionsListFragment fragment = new ScheduledActionsListFragment();
        fragment.mActionType = actionType;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = requireContext();
        mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();
        switch (mActionType) {
            case TRANSACTION:
                mCursorAdapter = new ScheduledTransactionsCursorAdapter(
                    context,
                    R.layout.list_item_scheduled_trxn, null,
                    new String[]{DatabaseSchema.TransactionEntry.COLUMN_DESCRIPTION},
                    new int[]{R.id.primary_text});
                break;
            case BACKUP:
                mCursorAdapter = new ScheduledExportCursorAdapter(
                    context,
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

        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        setHasOptionsMenu(true);

        ListView listView = getListView();
        TextView emptyView = (TextView) listView.getEmptyView();
        if (mActionType == ScheduledAction.ActionType.TRANSACTION) {
            emptyView.setText(R.string.label_no_recurring_transactions);
        } else if (mActionType == ScheduledAction.ActionType.BACKUP) {
            emptyView.setText(R.string.label_no_scheduled_exports_to_display);
        }
    }

    /**
     * Reload the list of transactions and recompute account balances
     */
    public void refreshList() {
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (mActionType == ScheduledAction.ActionType.BACKUP)
            inflater.inflate(R.menu.scheduled_export_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
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
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if (mActionMode != null) {
            CheckBox checkbox = v.findViewById(R.id.checkbox);
            checkbox.setChecked(!checkbox.isChecked());
            return;
        }

        if (mActionType == ScheduledAction.ActionType.BACKUP) {
            ScheduledAction scheduledAction = (ScheduledAction) v.getTag();
            editExport(scheduledAction);
        } else if (mActionType == ScheduledAction.ActionType.TRANSACTION) {
            String scheduledActionUid = v.getTag().toString();
            editTransaction(id, scheduledActionUid);
        }
    }

    private void editTransaction(long id, String scheduledActionUid) {
        Transaction transaction = mTransactionsDbAdapter.getRecord(id);

        //this should actually never happen, but has happened once. So perform check for the future
        if (transaction.getSplits().isEmpty()) {
            Toast.makeText(requireContext(), R.string.toast_transaction_has_no_splits_and_cannot_open, Toast.LENGTH_SHORT).show();
            return;
        }

        String accountUID = transaction.getSplits().get(0).getAccountUID();
        openTransactionForEdit(accountUID, mTransactionsDbAdapter.getUID(id), scheduledActionUid);
    }

    /**
     * Opens the transaction editor to enable editing of the transaction
     *
     * @param accountUID     GUID of account to which transaction belongs
     * @param transactionUID GUID of transaction to be edited
     */
    private void openTransactionForEdit(String accountUID, String transactionUID, String scheduledActionUid) {
        Intent intent = new Intent(requireContext(), FormActivity.class)
            .setAction(Intent.ACTION_INSERT_OR_EDIT)
            .putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.TRANSACTION.name())
            .putExtra(UxArgument.SELECTED_ACCOUNT_UID, accountUID)
            .putExtra(UxArgument.SELECTED_TRANSACTION_UID, transactionUID)
            .putExtra(UxArgument.SCHEDULED_ACTION_UID, scheduledActionUid);
        startActivity(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        Timber.d("Creating transactions loader");
        if (mActionType == ScheduledAction.ActionType.TRANSACTION)
            return new ScheduledTransactionsCursorLoader(getActivity());
        else if (mActionType == ScheduledAction.ActionType.BACKUP) {
            return new ScheduledExportCursorLoader(getActivity());
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Timber.d("Transactions loader finished. Swapping in cursor");
        mCursorAdapter.swapCursor(cursor);
        mCursorAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Timber.d("Resetting transactions loader");
        mCursorAdapter.swapCursor(null);
    }

    /**
     * Finishes the edit mode in the transactions list.
     * Edit mode is started when at least one transaction is selected
     */
    public void finishEditMode() {
        uncheckAllItems();
        mActionMode = null;
    }

    /**
     * Sets the title of the Context ActionBar when in action mode.
     * It sets the number highlighted items
     */
    public void setActionModeTitle() {
        int count = getListView().getCheckedItemIds().length; //mSelectedIds.size();
        if (count > 0) {
            mActionMode.setTitle(getResources().getString(R.string.title_selected, count));
        }
    }

    /**
     * Unchecks all the checked items in the list
     */
    private void uncheckAllItems() {
        ListView listView = getListView();
        SparseBooleanArray checkedPositions = listView.getCheckedItemPositions();
        for (int i = 0; i < checkedPositions.size(); i++) {
            int position = checkedPositions.keyAt(i);
            listView.setItemChecked(position, false);
        }
    }


    /**
     * Starts action mode and activates the Context ActionBar (CAB)
     * Action mode is initiated as soon as at least one transaction is selected (highlighted)
     */
    private void startActionMode() {
        if (mActionMode != null) {
            return;
        }
        final AppCompatActivity activity = (AppCompatActivity) requireActivity();
        // Start the CAB using the ActionMode.Callback defined above
        mActionMode = activity.startSupportActionMode(mActionModeCallbacks);
        activity.getWindow().setStatusBarColor(
            ContextCompat.getColor(activity, android.R.color.darker_gray));
    }

    /**
     * Stops action mode and deselects all selected transactions.
     * This method only has effect if the number of checked items is greater than 0 and {@link #mActionMode} is not null
     */
    private void stopActionMode() {
        int checkedCount = getListView().getCheckedItemCount();
        if (checkedCount <= 0) {
            if (mActionMode != null) {
                mActionMode.finish();
            }
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

    private void editExport(@NonNull ScheduledAction scheduledAction) {
        Intent intent = new Intent(requireContext(), FormActivity.class)
            .setAction(Intent.ACTION_EDIT)
            .putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.EXPORT.name())
            .putExtra(UxArgument.SCHEDULED_UID, scheduledAction.getUID());
        startActivity(intent);
    }

    /**
     * Extends a simple cursor adapter to bind transaction attributes to views
     *
     * @author Ngewi Fet <ngewif@gmail.com>
     */
    protected class ScheduledCursorAdapter extends SimpleCursorAdapter {

        private final Map<Integer, Drawable> backgrounds = new HashMap<>();
        private final int colorChecked;

        public ScheduledCursorAdapter(Context context, int layout, Cursor c,
                                      String[] from, int[] to) {
            super(context, layout, c, from, to, 0);
            colorChecked = ContextCompat.getColor(context, R.color.item_checked);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final View view = super.getView(position, convertView, parent);
            final ListView listView = (ListView) parent;
            final CheckBox checkbox = view.findViewById(R.id.checkbox);

            final Drawable bgDefault;
            if (backgrounds.containsKey(position)) {
                bgDefault = backgrounds.get(position);
            } else {
                Drawable bgOriginal = view.getBackground();
                backgrounds.put(position, bgOriginal);
                bgDefault = bgOriginal;
            }

            checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    listView.setItemChecked(position, isChecked);
                    if (isChecked) {
                        view.setBackgroundColor(colorChecked);
                        startActionMode();
                    } else {
                        view.setBackground(bgDefault);
                        stopActionMode();
                    }
                    setActionModeTitle();
                }
            });

            return view;
        }
    }

    /**
     * Extends a simple cursor adapter to bind transaction attributes to views
     *
     * @author Ngewi Fet <ngewif@gmail.com>
     */
    protected class ScheduledTransactionsCursorAdapter extends ScheduledCursorAdapter {

        public ScheduledTransactionsCursorAdapter(Context context, int layout, Cursor c,
                                                  String[] from, int[] to) {
            super(context, layout, c, from, to);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);

            Transaction transaction = mTransactionsDbAdapter.buildModelInstance(cursor);

            TextView amountTextView = view.findViewById(R.id.right_text);
            TextView descriptionTextView = view.findViewById(R.id.secondary_text);

            String text = "";
            List<Split> splits = transaction.getSplits();
            if (splits.size() == 2) {
                Split first = splits.get(0);
                for (Split split : splits) {
                    if ((first != split) && first.isPairOf(split)) {
                        text = first.getValue().formattedString();
                        break;
                    }
                }
            } else {
                text = getString(R.string.label_split_count, splits.size());
            }
            amountTextView.setText(text);

            ScheduledActionDbAdapter scheduledActionDbAdapter = ScheduledActionDbAdapter.getInstance();
            String scheduledActionUID = cursor.getString(cursor.getColumnIndexOrThrow("origin_scheduled_action_uid")); //column created from join when fetching scheduled transactions
            view.setTag(scheduledActionUID);
            ScheduledAction scheduledAction = scheduledActionDbAdapter.getRecord(scheduledActionUID);
            long endTime = scheduledAction.getEndTime();
            if (endTime > 0 && endTime < System.currentTimeMillis()) {
                ((TextView) view.findViewById(R.id.primary_text))
                    .setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                descriptionTextView.setText(getString(R.string.label_scheduled_action_ended,
                    DateTimeFormat.shortDateTime().print(scheduledAction.getLastRunTime())));
            } else {
                descriptionTextView.setText(scheduledAction.getRepeatString());
            }
        }
    }

    /**
     * Extends a simple cursor adapter to bind transaction attributes to views
     *
     * @author Ngewi Fet <ngewif@gmail.com>
     */
    protected class ScheduledExportCursorAdapter extends ScheduledCursorAdapter {

        public ScheduledExportCursorAdapter(Context context, int layout, Cursor c,
                                            String[] from, int[] to) {
            super(context, layout, c, from, to);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            super.bindView(view, context, cursor);
            ScheduledActionDbAdapter scheduledActionDbAdapter = ScheduledActionDbAdapter.getInstance();
            ScheduledAction scheduledAction = scheduledActionDbAdapter.buildModelInstance(cursor);

            TextView primaryTextView = view.findViewById(R.id.primary_text);
            TextView descriptionTextView = view.findViewById(R.id.secondary_text);
            TextView amountTextView = view.findViewById(R.id.right_text);

            ExportParams params = ExportParams.parseCsv(scheduledAction.getTag());
            view.setTag(scheduledAction);
            String exportDestination = params.getExportTarget().getDescription();
            if (params.getExportTarget() == ExportParams.ExportTarget.URI) {
                exportDestination = exportDestination + " (" + getDocumentName(params.getExportLocation(), context) + ")";
            }
            String description = context.getString(
                R.string.schedule_export_desription,
                params.getExportFormat().name(),
                context.getString(scheduledAction.getActionType().labelId),
                exportDestination
            );
            primaryTextView.setText(description);

            amountTextView.setVisibility(View.GONE);

            descriptionTextView.setText(scheduledAction.getRepeatString());
            long endTime = scheduledAction.getEndTime();
            if (endTime > 0 && endTime < System.currentTimeMillis()) {
                primaryTextView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                descriptionTextView.setText(getString(R.string.label_scheduled_action_ended,
                    DateTimeFormat.shortDateTime().print(scheduledAction.getLastRunTime())));
            } else {
                descriptionTextView.setText(scheduledAction.getRepeatString());
            }
        }
    }

    /**
     * {@link DatabaseCursorLoader} for loading recurring transactions asynchronously from the database
     *
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
     *
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
                new String[]{ScheduledAction.ActionType.BACKUP.name()}, null);

            registerContentObserver(c);
            return c;
        }
    }
}

