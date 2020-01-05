/*
 * Copyright (c) 2012 - 2014 Ngewi Fet <ngewif@gmail.com>
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

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
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
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseCursorLoader;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.DatabaseAdapter;
import org.gnucash.android.db.adapter.SplitsDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.ui.common.FormActivity;
import org.gnucash.android.ui.common.Refreshable;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.homescreen.WidgetConfigurationActivity;
import org.gnucash.android.ui.settings.PreferenceActivity;
import org.gnucash.android.ui.transaction.dialog.BulkMoveDialogFragment;
import org.gnucash.android.ui.util.CursorRecyclerAdapter;
import org.gnucash.android.ui.util.widget.EmptyRecyclerView;
import org.gnucash.android.util.BackupManager;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * List Fragment for displaying list of transactions for an account
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class TransactionsListFragment extends Fragment implements
        Refreshable, LoaderCallbacks<Cursor>{

	/**
	 * Logging tag
	 */
	protected static final String LOG_TAG = "TransactionListFragment";

    private TransactionsDbAdapter mTransactionsDbAdapter;
    private String mAccountUID;

	private boolean mUseCompactView = false;

	private TransactionRecyclerAdapter mTransactionRecyclerAdapter;
	@BindView(R.id.transaction_recycler_view) EmptyRecyclerView mRecyclerView;


	@Override
 	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		Bundle args = getArguments();
		mAccountUID = args.getString(UxArgument.SELECTED_ACCOUNT_UID);

		mUseCompactView = PreferenceActivity.getActiveBookSharedPreferences()
				.getBoolean(getActivity().getString(R.string.key_use_compact_list), !GnuCashApplication.isDoubleEntryEnabled());
		//if there was a local override of the global setting, respect it
		if (savedInstanceState != null)
			mUseCompactView = savedInstanceState.getBoolean(getString(R.string.key_use_compact_list), mUseCompactView);

		mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(getString(R.string.key_use_compact_list), mUseCompactView);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_transactions_list, container, false);
		ButterKnife.bind(this, view);

		mRecyclerView.setHasFixedSize(true);
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 2);
			mRecyclerView.setLayoutManager(gridLayoutManager);
		} else {
			LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
			mRecyclerView.setLayoutManager(mLayoutManager);
		}
		mRecyclerView.setEmptyView(view.findViewById(R.id.empty_view));

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		ActionBar aBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
		aBar.setDisplayShowTitleEnabled(false);
		aBar.setDisplayHomeAsUpEnabled(true);

		mTransactionRecyclerAdapter = new TransactionRecyclerAdapter(null);
		mRecyclerView.setAdapter(mTransactionRecyclerAdapter);

		setHasOptionsMenu(true);		
	}

    /**
     * Refresh the list with transactions from account with ID <code>accountId</code>
     * @param accountUID GUID of account to load transactions from
     */
    @Override
	public void refresh(String accountUID){
		mAccountUID = accountUID;
		refresh();
	}

    /**
     * Reload the list of transactions and recompute account balances
     */
    @Override
	public void refresh(){
		getLoaderManager().restartLoader(0, null, this);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		((TransactionsActivity)getActivity()).updateNavigationSelection();
		refresh();
	}

    /**
     * Called when user clicks on a transaction list item
     *
     * @param transactionListItemId
     *         Transaction list item number (starting from 1)
     */
    public void onTransactionListItemClick(long transactionListItemId) {

        Intent intent = new Intent(getActivity(),
                                   TransactionDetailActivity.class);
        intent.putExtra(UxArgument.SELECTED_TRANSACTION_UID,
                        mTransactionsDbAdapter.getUID(transactionListItemId));
		intent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, mAccountUID);
		startActivity(intent);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.transactions_list_actions, menu);	
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem item = menu.findItem(R.id.menu_compact_trn_view);
		item.setChecked(mUseCompactView);
		item.setEnabled(GnuCashApplication.isDoubleEntryEnabled()); //always compact for single-entry
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_compact_trn_view:
				item.setChecked(!item.isChecked());
				mUseCompactView = !mUseCompactView;
				refresh();
				return true;
			default:
                return super.onOptionsItemSelected(item);
        }
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		Log.d(LOG_TAG, "Creating transactions loader");
		return new TransactionsCursorLoader(getActivity(), mAccountUID);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		Log.d(LOG_TAG, "Transactions loader finished. Swapping in cursor");
		mTransactionRecyclerAdapter.swapCursor(cursor);
		mTransactionRecyclerAdapter.notifyDataSetChanged();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.d(LOG_TAG, "Resetting transactions loader");
		mTransactionRecyclerAdapter.swapCursor(null);
	}

	/**
	 * {@link DatabaseCursorLoader} for loading transactions asynchronously from the database
	 * @author Ngewi Fet <ngewif@gmail.com>
	 */
	protected static class TransactionsCursorLoader extends DatabaseCursorLoader {
		private String accountUID;
		
		public TransactionsCursorLoader(Context context, String accountUID) {
			super(context);			
			this.accountUID = accountUID;
		}
		
		@Override
		public Cursor loadInBackground() {
			mDatabaseAdapter = TransactionsDbAdapter.getInstance();
			Cursor c = ((TransactionsDbAdapter) mDatabaseAdapter).fetchAllTransactionsForAccount(accountUID);
			if (c != null)
				registerContentObserver(c);
			return c;
		}		
	}

	public class TransactionRecyclerAdapter extends CursorRecyclerAdapter<TransactionRecyclerAdapter.ViewHolder>{

		public static final int ITEM_TYPE_COMPACT 	= 0x111;
		public static final int ITEM_TYPE_FULL		= 0x100;

		public TransactionRecyclerAdapter(Cursor cursor) {
			super(cursor);
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			int layoutRes = viewType == ITEM_TYPE_COMPACT ? R.layout.cardview_compact_transaction : R.layout.cardview_transaction;
			View v = LayoutInflater.from(parent.getContext())
					.inflate(layoutRes, parent, false);
			return new ViewHolder(v);
		}

		@Override
		public int getItemViewType(int position) {
			return mUseCompactView ? ITEM_TYPE_COMPACT : ITEM_TYPE_FULL;
		}

		@Override
		public void onBindViewHolderCursor(ViewHolder holder, Cursor cursor) {
			holder.transactionId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry._ID));

			String description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.COLUMN_DESCRIPTION));
			holder.primaryText.setText(description);

			final String transactionUID = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.COLUMN_UID));
			Money amount = mTransactionsDbAdapter.getBalance(transactionUID, mAccountUID);
			TransactionsActivity.displayBalance(holder.transactionAmount, amount);

			long dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.COLUMN_TIMESTAMP));
			String dateText = TransactionsActivity.getPrettyDateFormat(getActivity(), dateMillis);

            // Transaction list item number (First item is 1, second is 2, ...)
            final long transactionListItemId = holder.transactionId;

            // Listener when user clicks on a transaction list item
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {

                    // Handle click on a transaction list item
                    onTransactionListItemClick(transactionListItemId);
				}
			});

			if (mUseCompactView) {
				holder.secondaryText.setText(dateText);
			} else {

				List<Split> splits = SplitsDbAdapter.getInstance().getSplitsForTransaction(transactionUID);
				String text = "";

				if (splits.size() == 2 && splits.get(0).isPairOf(splits.get(1))) {
					for (Split split : splits) {
						if (!split.getAccountUID().equals(mAccountUID)) {
							text = AccountsDbAdapter.getInstance().getFullyQualifiedAccountName(split.getAccountUID());
							break;
						}
					}
				}

				if (splits.size() > 2) {
					text = splits.size() + " splits";
				}
				holder.secondaryText.setText(text);
				holder.transactionDate.setText(dateText);

                //
                // Action when clicking on the right pen icon of a cardview_transaction
                // to open transaction editor
                //

				holder.editTransaction.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(getActivity(), FormActivity.class);
						intent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.TRANSACTION.name());
						intent.putExtra(UxArgument.SELECTED_TRANSACTION_UID, transactionUID);
						intent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, mAccountUID);
						startActivity(intent);
					}
				});
			}
		}

		public class ViewHolder extends RecyclerView.ViewHolder implements PopupMenu.OnMenuItemClickListener{
			@BindView(R.id.primary_text) 		public TextView primaryText;
			@BindView(R.id.secondary_text) 		public TextView secondaryText;
			@BindView(R.id.transaction_amount)	public TextView transactionAmount;
			@BindView(R.id.options_menu)		public ImageView optionsMenu;

			//these views are not used in the compact view, hence the nullability
			@Nullable @BindView(R.id.transaction_date)	public TextView transactionDate;
			@Nullable @BindView(R.id.edit_transaction)	public ImageView editTransaction;

			long transactionId;

			public ViewHolder(View itemView) {
				super(itemView);
				ButterKnife.bind(this, itemView);
				primaryText.setTextSize(18);

                //
                // Define action when clicking on the secondary text to jump to this account transaction list
                //

                secondaryText.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        // Prepare Intent to jump to the Transaction List of the Account selected by the user (when clicking on the secondary text)
                        Intent jumpToSelectedAccountTransactionListIntent = new Intent(getActivity(),
                                                                                       TransactionsActivity.class);

                        // Get Transaction UID for the transactionId nth item of the transaction list
                        String transactionUID = mTransactionsDbAdapter.getUID(transactionId);

                        // Get all Splits of Transaction transactionUID
                        final List<Split> splitsForTransaction = mTransactionsDbAdapter.getSplitDbAdapter()
                                                                                       .getSplitsForTransaction(transactionUID);

                        // Get the Account UID to jump to
                        String jumpToAccountUID = "";
                        for (int i = 0; i < splitsForTransaction.size(); i++) {

                            // Get the UID of the i-nth account involved in the Transaction
                            jumpToAccountUID = splitsForTransaction.get(i)
                                                                   .getAccountUID();

                            if (!mAccountUID.equals(jumpToAccountUID)) {
                                // The account transaction list to jump to is not the current one

                                // Stop searching
                                break;

                            } else {
                                // The account transaction list to jump to is the current one

                                // NTD : Continue to look for another Account
                            }
                        } // for

                        jumpToSelectedAccountTransactionListIntent.setAction(Intent.ACTION_VIEW);

                        // Indicate the Account Transaction List to jump to
                        jumpToSelectedAccountTransactionListIntent.putExtra(UxArgument.SELECTED_ACCOUNT_UID,
                                                                            jumpToAccountUID);

                        // Start the Activity to display the Account Transaction List of the other Account
                        startActivity(jumpToSelectedAccountTransactionListIntent);
                    }
                });

                //
                // Define action when clicking on the three dot icon of a cardview_transaction
                // to open a menu
                //

				optionsMenu.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {

                        // Build pop-menu
                        PopupMenu popupMenu = new PopupMenu(getActivity(),
                                                            v);

                        // Current ViewHolder instance will handle the click on a menu item
                        popupMenu.setOnMenuItemClickListener(ViewHolder.this);

                        // Unserialize the menu defined in transactions_context_menu.xml
                        MenuInflater inflater = popupMenu.getMenuInflater();
                        inflater.inflate(R.menu.transactions_context_menu,
                                         popupMenu.getMenu());

                        // Display pop-menu
                        popupMenu.show();
					}
				});
			}

			@Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                //
                // Handle click on pop-up menu item
                //

                switch (menuItem.getItemId()) {
					case R.id.context_menu_delete:
						BackupManager.backupActiveBook();
						mTransactionsDbAdapter.deleteRecord(transactionId);
						WidgetConfigurationActivity.updateAllWidgets(getActivity());
						refresh();
						return true;

					case R.id.context_menu_duplicate_transaction:
						Transaction transaction = mTransactionsDbAdapter.getRecord(transactionId);
						Transaction duplicate = new Transaction(transaction, true);
						duplicate.setTime(System.currentTimeMillis());
						mTransactionsDbAdapter.addRecord(duplicate, DatabaseAdapter.UpdateMethod.insert);
						refresh();
						return true;

					case R.id.context_menu_move_transaction:
						long[] ids = new long[]{transactionId};
						BulkMoveDialogFragment fragment = BulkMoveDialogFragment.newInstance(ids, mAccountUID);
						fragment.show(getActivity().getSupportFragmentManager(), "bulk_move_transactions");
						fragment.setTargetFragment(TransactionsListFragment.this, 0);
						return true;

					default:
						return false;

				}
			}
		}
	}
}
