/*
 * Copyright (c) 2012 - 2015 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.ui.account;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.ActionMode.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseCursorLoader;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.transaction.TransactionsActivity;
import org.gnucash.android.ui.util.AccountBalanceTask;
import org.gnucash.android.ui.util.OnAccountClickedListener;
import org.gnucash.android.ui.util.Refreshable;

/**
 * Fragment for displaying the list of accounts in the database
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class AccountsListFragment extends ListFragment implements
        Refreshable,
        LoaderCallbacks<Cursor>, OnItemLongClickListener,
        android.support.v7.widget.SearchView.OnQueryTextListener,
        android.support.v7.widget.SearchView.OnCloseListener {

    /**
     * Describes the kinds of accounts that should be loaded in the accounts list.
     * This enhances reuse of the accounts list fragment
     */
    public enum DisplayMode {
        TOP_LEVEL, RECENT, FAVORITES
    }

    /**
     * Field indicating which kind of accounts to load.
     * Default value is {@link DisplayMode#TOP_LEVEL}
     */
    private DisplayMode mDisplayMode = DisplayMode.TOP_LEVEL;

    /**
     * Logging tag
     */
    protected static final String TAG = "AccountsListFragment";


    /**
     * {@link ListAdapter} for the accounts which will be bound to the list
     */
    AccountsCursorAdapter mAccountsCursorAdapter;
    /**
     * Database adapter for loading Account records from the database
     */
    private AccountsDbAdapter mAccountsDbAdapter;
    /**
     * Listener to be notified when an account is clicked
     */
    private OnAccountClickedListener mAccountSelectedListener;
    /**
     * Flag to indicate if the fragment is in edit mode
     * Edit mode means an account has been selected (through long press) and the
     * context action bar (CAB) is activated
     */
    private boolean mInEditMode = false;
    /**
     * Android action mode
     * Is not null only when an accoun is selected and the Context ActionBar (CAB) is activated
     */
    private ActionMode mActionMode = null;

    /**
     * Stores the database ID of the currently selected account when in action mode.
     * This is necessary because getSelectedItemId() does not work properly (by design)
     * in touch mode (which is the majority of devices today)
     */
    private long mSelectedItemId = -1;

    /**
     * Database record ID of the account whose children will be loaded by the list fragment.
     * If no parent account is specified, then all top-level accounts are loaded.
     */
//    private long mParentAccountId = -1;

    /**
     * GUID of the account whose children will be loaded in the list fragment.
     * If no parent account is specified, then all top-level accounts are loaded.
     */
    private String mParentAccountUID = null;

    /**
     * Filter for which accounts should be displayed. Used by search interface
     */
    private String mCurrentFilter;

    /**
     * Search view for searching accounts
     */
    private android.support.v7.widget.SearchView mSearchView;

    /**
     * Callbacks for the CAB menu
     */
    private ActionMode.Callback mActionModeCallbacks = new Callback() {

        String mSelectedAccountUID;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.account_context_menu, menu);
            mode.setTitle(getString(R.string.title_selected, 1));
            mSelectedAccountUID = mAccountsDbAdapter.getUID(mSelectedItemId);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // nothing to see here, move along
            MenuItem favoriteAccountMenuItem = menu.findItem(R.id.menu_favorite_account);
            boolean isFavoriteAccount = AccountsDbAdapter.getInstance().isFavoriteAccount(mSelectedAccountUID);

            int favoriteIcon = isFavoriteAccount ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off;
            favoriteAccountMenuItem.setIcon(favoriteIcon);

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_favorite_account:
                    boolean isFavorite = mAccountsDbAdapter.isFavoriteAccount(mSelectedAccountUID);
                    //toggle favorite preference
                    mAccountsDbAdapter.updateAccount(mSelectedItemId,
                            DatabaseSchema.AccountEntry.COLUMN_FAVORITE, isFavorite ? "0" : "1");
                    mode.invalidate();
                    return true;

                case R.id.context_menu_edit_accounts:
                    openCreateOrEditActivity(mSelectedItemId);
                    mode.finish();
                    mActionMode = null;
                    return true;

                case R.id.context_menu_delete:
                    tryDeleteAccount(mSelectedItemId);
                    mode.finish();
                    mActionMode = null;
                    return true;

                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            finishEditMode();
        }
    };

    public static AccountsListFragment newInstance(DisplayMode displayMode){
        AccountsListFragment fragment = new AccountsListFragment();
        fragment.mDisplayMode = displayMode;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_accounts_list, container,
                false);
        TextView sumlabelTextView = (TextView) v.findViewById(R.id.label_sum);
        sumlabelTextView.setText(R.string.account_balance);
        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null)
            mParentAccountUID = args.getString(UxArgument.PARENT_ACCOUNT_UID);

        mAccountsDbAdapter = AccountsDbAdapter.getInstance();
        mAccountsCursorAdapter = new AccountsCursorAdapter(
                getActivity().getApplicationContext(),
                R.layout.list_item_account, null,
                new String[]{DatabaseSchema.AccountEntry.COLUMN_NAME},
                new int[]{R.id.primary_text});

        setListAdapter(mAccountsCursorAdapter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar actionbar = getActivity().getSupportActionBar();
        actionbar.setTitle(R.string.title_accounts);
        actionbar.setDisplayHomeAsUpEnabled(true);

        setHasOptionsMenu(true);

        ListView lv = getListView();
        lv.setOnItemLongClickListener(this);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mAccountSelectedListener = (OnAccountClickedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnAccountSelectedListener");
        }
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        if (mInEditMode) {
            mSelectedItemId = id;
            listView.setItemChecked(position, true);
            return;
        }
        mAccountSelectedListener.accountSelected(mAccountsDbAdapter.getUID(id));
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                                   long id) {
        if (mActionMode != null) {
            return false;
        }
        mInEditMode = true;
        mSelectedItemId = id;
        // Start the CAB using the ActionMode.Callback defined above
        mActionMode = getActivity().startActionMode(
                mActionModeCallbacks);

        getListView().setItemChecked(position, true);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED)
            return;

        refresh();
    }

    /**
     * Delete the account with record ID <code>rowId</code>
     * It shows the delete confirmation dialog if the account has transactions,
     * else deletes the account immediately
     *
     * @param rowId The record ID of the account
     */
    public void tryDeleteAccount(long rowId) {
        Account acc = mAccountsDbAdapter.getAccount(rowId);
        if (acc.getTransactionCount() > 0 || mAccountsDbAdapter.getSubAccountCount(acc.getUID()) > 0) {
            showConfirmationDialog(rowId);
        } else {
            mAccountsDbAdapter.deleteRecord(rowId);
            mAccountsCursorAdapter.swapCursor(null);
            refresh();
        }
    }

    /**
     * Shows the delete confirmation dialog
     *
     * @param id Record ID of account to be deleted after confirmation
     */
    public void showConfirmationDialog(long id) {
        DeleteAccountDialogFragment alertFragment =
                DeleteAccountDialogFragment.newInstance(mAccountsDbAdapter.getUID(id));
        alertFragment.setTargetFragment(this, 0);
        alertFragment.show(getActivity().getSupportFragmentManager(), "delete_confirmation_dialog");
    }

    /**
     * Finish the edit mode and dismisses the Contextual ActionBar
     * Any selected (highlighted) accounts are deselected
     */
    public void finishEditMode() {
        mInEditMode = false;
        getListView().setItemChecked(getListView().getCheckedItemPosition(), false);
        mActionMode = null;
        mSelectedItemId = -1;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mParentAccountUID != null)
            inflater.inflate(R.menu.sub_account_actions, menu);
        else {
            inflater.inflate(R.menu.account_actions, menu);
            // Associate searchable configuration with the SearchView
            SearchManager searchManager =
                    (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
            mSearchView = (android.support.v7.widget.SearchView)
                MenuItemCompat.getActionView(menu.findItem(R.id.menu_search));
            if (mSearchView == null)
                return;

            mSearchView.setSearchableInfo(
                    searchManager.getSearchableInfo(getActivity().getComponentName()));
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setOnCloseListener(this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_add_account:
                Intent addAccountIntent = new Intent(getActivity(), AccountsActivity.class);
                addAccountIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
                addAccountIntent.putExtra(UxArgument.PARENT_ACCOUNT_UID, mParentAccountUID);
                startActivityForResult(addAccountIntent, AccountsActivity.REQUEST_EDIT_ACCOUNT);
                return true;

            case R.id.menu_export:
                AccountsActivity.showExportDialog(getActivity());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void refresh(String parentAccountUID) {
        getArguments().putString(UxArgument.PARENT_ACCOUNT_UID, parentAccountUID);
        refresh();
    }

    /**
     * Refreshes the list by restarting the {@link DatabaseCursorLoader} associated
     * with the ListView
     */
    @Override
    public void refresh() {
        getLoaderManager().restartLoader(0, null, this);
    }

    /**
     * Closes any open database adapters used by the list
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Opens a new activity for creating or editing an account.
     * If the <code>accountId</code> &lt; 1, then create else edit the account.
     * @param accountId Long record ID of account to be edited. Pass 0 to create a new account.
     */
    public void openCreateOrEditActivity(long accountId){
        Intent editAccountIntent = new Intent(AccountsListFragment.this.getActivity(), AccountsActivity.class);
        editAccountIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        editAccountIntent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, mAccountsDbAdapter.getUID(accountId));
        startActivityForResult(editAccountIntent, AccountsActivity.REQUEST_EDIT_ACCOUNT);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "Creating the accounts loader");
        Bundle arguments = getArguments();
        String accountUID = arguments == null ? null : arguments.getString(UxArgument.PARENT_ACCOUNT_UID);

        if (mCurrentFilter != null){
            return new AccountsCursorLoader(getActivity(), mCurrentFilter);
        } else {
            return new AccountsCursorLoader(this.getActivity(), accountUID, mDisplayMode);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loaderCursor, Cursor cursor) {
        Log.d(TAG, "Accounts loader finished. Swapping in cursor");
        mAccountsCursorAdapter.swapCursor(cursor);
        mAccountsCursorAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        Log.d(TAG, "Resetting the accounts loader");
        mAccountsCursorAdapter.swapCursor(null);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        //nothing to see here, move along
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        String newFilter = !TextUtils.isEmpty(newText) ? newText : null;

        if (mCurrentFilter == null && newFilter == null) {
            return true;
        }
        if (mCurrentFilter != null && mCurrentFilter.equals(newFilter)) {
            return true;
        }
        mCurrentFilter = newFilter;
        getLoaderManager().restartLoader(0, null, this);
        return true;
    }

    @Override
    public boolean onClose() {
        if (!TextUtils.isEmpty(mSearchView.getQuery())) {
            mSearchView.setQuery(null, true);
        }
        return true;
    }

    /**
     * Extends {@link DatabaseCursorLoader} for loading of {@link Account} from the
     * database asynchronously.
     * <p>By default it loads only top-level accounts (accounts which have no parent or have GnuCash ROOT account as parent.
     * By submitting a parent account ID in the constructor parameter, it will load child accounts of that parent.</p>
     * <p>Class must be static because the Android loader framework requires it to be so</p>
     * @author Ngewi Fet <ngewif@gmail.com>
     */
    private static final class AccountsCursorLoader extends DatabaseCursorLoader {
        private String mParentAccountUID = null;
        private String mFilter;
        private DisplayMode mDisplayMode = DisplayMode.TOP_LEVEL;

        /**
         * Initializes the loader to load accounts from the database.
         * If the <code>parentAccountId <= 0</code> then only top-level accounts are loaded.
         * Else only the child accounts of the <code>parentAccountId</code> will be loaded
         * @param context Application context
         * @param parentAccountUID GUID of the parent account
         */
        public AccountsCursorLoader(Context context, String parentAccountUID, DisplayMode displayMode) {
            super(context);
            this.mParentAccountUID = parentAccountUID;
            this.mDisplayMode = displayMode;
        }

        /**
         * Initializes the loader with a filter for account names.
         * Only accounts whose name match the filter will be loaded.
         * @param context Application context
         * @param filter Account name filter string
         */
        public AccountsCursorLoader(Context context, String filter){
            super(context);
            mFilter = filter;
        }

        @Override
        public Cursor loadInBackground() {
            mDatabaseAdapter = AccountsDbAdapter.getInstance();
            Cursor cursor;

            if (mFilter != null){
                cursor = ((AccountsDbAdapter)mDatabaseAdapter)
                        .fetchAccounts(DatabaseSchema.AccountEntry.COLUMN_HIDDEN + "= 0 AND "
                                + DatabaseSchema.AccountEntry.COLUMN_NAME + " LIKE '%" + mFilter + "%'",
                                null, null);
            } else {
                if (mParentAccountUID != null && mParentAccountUID.length() > 0)
                    cursor = ((AccountsDbAdapter) mDatabaseAdapter).fetchSubAccounts(mParentAccountUID);
                else {
                    switch (this.mDisplayMode){
                        case RECENT:
                            cursor = ((AccountsDbAdapter) mDatabaseAdapter).fetchRecentAccounts(10);
                            break;
                        case FAVORITES:
                            cursor = ((AccountsDbAdapter) mDatabaseAdapter).fetchFavoriteAccounts();
                            break;
                        case TOP_LEVEL:
                        default:
                            cursor = ((AccountsDbAdapter) mDatabaseAdapter).fetchTopLevelAccounts();
                            break;
                    }
                }

            }

            if (cursor != null)
                registerContentObserver(cursor);
            return cursor;
        }
    }

    /**
     * Overrides the {@link SimpleCursorAdapter} to provide custom binding of the
     * information from the database to the views
     *
     * @author Ngewi Fet <ngewif@gmail.com>
     */
    private class AccountsCursorAdapter extends SimpleCursorAdapter {
        TransactionsDbAdapter transactionsDBAdapter;

        public AccountsCursorAdapter(Context context, int layout, Cursor c,
                                     String[] from, int[] to) {
            super(context, layout, c, from, to, 0);
            transactionsDBAdapter = TransactionsDbAdapter.getInstance();
        }

        @Override
        public void bindView(View v, Context context, Cursor cursor) {
            // perform the default binding
            super.bindView(v, context, cursor);

            final String accountUID = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_UID));

            TextView subAccountTextView = (TextView) v.findViewById(R.id.secondary_text);
            int subAccountCount = mAccountsDbAdapter.getSubAccountCount(accountUID);
            if (subAccountCount > 0) {
                subAccountTextView.setVisibility(View.VISIBLE);
                String text = getResources().getQuantityString(R.plurals.label_sub_accounts, subAccountCount, subAccountCount);
                subAccountTextView.setText(text);
            } else
                subAccountTextView.setVisibility(View.GONE);

            // add a summary of transactions to the account view
            TextView accountBalanceTextView = (TextView) v
                    .findViewById(R.id.transactions_summary);
            new AccountBalanceTask(accountBalanceTextView).execute(accountUID);

            View colorStripView = v.findViewById(R.id.account_color_strip);
            String accountColor = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_COLOR_CODE));
            if (accountColor != null){
                int color = Color.parseColor(accountColor);
                colorStripView.setBackgroundColor(color);
            } else {
                colorStripView.setBackgroundColor(Color.TRANSPARENT);
            }

            boolean isPlaceholderAccount = mAccountsDbAdapter.isPlaceholderAccount(accountUID);
            ImageButton newTransactionButton = (ImageButton) v.findViewById(R.id.btn_new_transaction);
            if (isPlaceholderAccount){
                newTransactionButton.setVisibility(View.GONE);
                v.findViewById(R.id.vertical_line).setVisibility(View.GONE);
            } else {
                newTransactionButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), TransactionsActivity.class);
                        intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
                        intent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, accountUID);
                        getActivity().startActivity(intent);
                    }
                });
            }
            newTransactionButton.setFocusable(false);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = super.getView(position, convertView, parent);
            TextView secondaryText = (TextView) itemView.findViewById(R.id.secondary_text);

            ListView listView = (ListView) parent;
            if (mInEditMode && listView.isItemChecked(position)){
                itemView.setBackgroundColor(getResources().getColor(R.color.abs__holo_blue_light));
                secondaryText.setTextColor(getResources().getColor(android.R.color.white));
            } else {
                itemView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                secondaryText.setTextColor(getResources().getColor(android.R.color.secondary_text_light_nodisable));
            }


            //increase the touch target area for the add new transaction button

            final View addTransactionButton = itemView.findViewById(R.id.btn_new_transaction);
            final View parentView = itemView;
            parentView.post(new Runnable() {
                @Override
                public void run() {
                    if (isAdded()){ //may be run when fragment has been unbound from activity
                        final android.graphics.Rect hitRect = new Rect();
                        float extraPadding = getResources().getDimension(R.dimen.edge_padding);
                        addTransactionButton.getHitRect(hitRect);
                        hitRect.right   += extraPadding;
                        hitRect.bottom  += extraPadding;
                        hitRect.top     -= extraPadding;
                        hitRect.left    -= extraPadding;
                        parentView.setTouchDelegate(new TouchDelegate(hitRect, addTransactionButton));
                    }
                }
            });

            return itemView;
        }
    }

}
