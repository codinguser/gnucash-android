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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseCursorLoader;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.model.Account;
import org.gnucash.android.ui.FormActivity;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.util.AccountBalanceTask;
import org.gnucash.android.ui.util.OnAccountClickedListener;
import org.gnucash.android.ui.util.Refreshable;

/**
 * Fragment for displaying the list of accounts in the database
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class AccountsListFragment extends Fragment implements
        Refreshable,
        LoaderCallbacks<Cursor>,
        android.support.v7.widget.SearchView.OnQueryTextListener,
        android.support.v7.widget.SearchView.OnCloseListener {

    private RecyclerView mRecyclerView;
    private AccountRecyclerAdapter mAccountRecyclerAdapter;

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
     * Database adapter for loading Account records from the database
     */
    private AccountsDbAdapter mAccountsDbAdapter;
    /**
     * Listener to be notified when an account is clicked
     */
    private OnAccountClickedListener mAccountSelectedListener;

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

        mRecyclerView = (RecyclerView) v.findViewById(R.id.account_recycler_view);
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        
        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null)
            mParentAccountUID = args.getString(UxArgument.PARENT_ACCOUNT_UID);

        mAccountsDbAdapter = AccountsDbAdapter.getInstance();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar actionbar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionbar.setTitle(R.string.title_accounts);
        actionbar.setDisplayHomeAsUpEnabled(true);
        setHasOptionsMenu(true);


        // specify an adapter (see also next example)
        mAccountRecyclerAdapter = new AccountRecyclerAdapter(null);
        mRecyclerView.setAdapter(mAccountRecyclerAdapter);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        ActionBar actionbar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionbar.setTitle(R.string.title_accounts);
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

    public void onListItemClick(String accountUID) {
        mAccountSelectedListener.accountSelected(accountUID);
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
        Account acc = mAccountsDbAdapter.getRecord(rowId);
        if (acc.getTransactionCount() > 0 || mAccountsDbAdapter.getSubAccountCount(acc.getUID()) > 0) {
            showConfirmationDialog(rowId);
        } else {
            mAccountsDbAdapter.deleteRecord(rowId);
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
        Intent editAccountIntent = new Intent(AccountsListFragment.this.getActivity(), FormActivity.class);
        editAccountIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        editAccountIntent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, mAccountsDbAdapter.getUID(accountId));
        editAccountIntent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.ACCOUNT_FORM.name());
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
        mAccountRecyclerAdapter.swapCursor(cursor);
        mAccountRecyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        Log.d(TAG, "Resetting the accounts loader");
        mAccountRecyclerAdapter.swapCursor(null);
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


    private class AccountRecyclerAdapter extends org.gnucash.android.db.CursorRecyclerAdapter<AccountRecyclerAdapter.ViewHolder>{

        public AccountRecyclerAdapter(Cursor cursor){
           super(cursor);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cardview_account, parent, false);

            ViewHolder viewHolder = new ViewHolder(v);
            return viewHolder;
        }

        @Override
        public void onBindViewHolderCursor(final ViewHolder holder, final Cursor cursor) {
            final String accountUID = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_UID));
            holder.accoundId = mAccountsDbAdapter.getID(accountUID);

            holder.accountName.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_NAME)));
            int subAccountCount = mAccountsDbAdapter.getSubAccountCount(accountUID);
            if (subAccountCount > 0) {
                holder.description.setVisibility(View.VISIBLE);
                String text = getResources().getQuantityString(R.plurals.label_sub_accounts, subAccountCount, subAccountCount);
                holder.description.setText(text);
            } else
                holder.description.setVisibility(View.GONE);

            // add a summary of transactions to the account view
            new AccountBalanceTask(holder.accountBalance).execute(accountUID);

            String accountColor = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_COLOR_CODE));
            int colorCode = accountColor == null ? Color.TRANSPARENT : Color.parseColor(accountColor);
            holder.colorStripView.setBackgroundColor(colorCode);

            boolean isPlaceholderAccount = mAccountsDbAdapter.isPlaceholderAccount(accountUID);
            if (isPlaceholderAccount){
                holder.createTransaction.setVisibility(View.GONE);
            } else {
                holder.createTransaction.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), FormActivity.class);
                        intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
                        intent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, accountUID);
                        intent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.TRANSACTION_FORM.name());
                        getActivity().startActivity(intent);
                    }
                });
            }

            if (mAccountsDbAdapter.isFavoriteAccount(accountUID)){
                holder.favoriteStatus.setImageResource(R.drawable.ic_star_black_24dp);
            } else {
                holder.favoriteStatus.setImageResource(R.drawable.ic_star_border_black_24dp);
            }

            holder.favoriteStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean isFavoriteAccount = mAccountsDbAdapter.isFavoriteAccount(accountUID);

                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DatabaseSchema.AccountEntry.COLUMN_FAVORITE, !isFavoriteAccount);
                    mAccountsDbAdapter.updateRecord(accountUID, contentValues);

                    int drawableResource = !isFavoriteAccount ?
                            R.drawable.ic_star_black_24dp : R.drawable.ic_star_border_black_24dp;
                    holder.favoriteStatus.setImageResource(drawableResource);
                }
            });

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onListItemClick(accountUID);
                }
            });
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements PopupMenu.OnMenuItemClickListener{
            TextView accountName;
            TextView description;
            TextView accountBalance;
            ImageView createTransaction;
            ImageView favoriteStatus;
            ImageView optionsMenu;
            View colorStripView;
            long accoundId;

            public ViewHolder(View itemView) {
                super(itemView);
                accountName = (TextView) itemView.findViewById(R.id.primary_text);
                description = (TextView) itemView.findViewById(R.id.secondary_text);
                accountBalance = (TextView) itemView.findViewById(R.id.account_balance);
                favoriteStatus = (ImageView) itemView.findViewById(R.id.favorite_status);
                optionsMenu = (ImageView) itemView.findViewById(R.id.options_menu);
                createTransaction = (ImageView) itemView.findViewById(R.id.create_transaction);
                colorStripView = itemView.findViewById(R.id.account_color_strip);

                optionsMenu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu popup = new PopupMenu(getActivity(), v);
                        popup.setOnMenuItemClickListener(ViewHolder.this);
                        MenuInflater inflater = popup.getMenuInflater();
                        inflater.inflate(R.menu.account_context_menu, popup.getMenu());
                        popup.show();
                    }
                });

            }


            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.context_menu_edit_accounts:
                        openCreateOrEditActivity(accoundId);
                        return true;

                    case R.id.context_menu_delete:
                        tryDeleteAccount(accoundId);
                        return true;

                    default:
                        return false;
                }
            }
        }
    }
}
