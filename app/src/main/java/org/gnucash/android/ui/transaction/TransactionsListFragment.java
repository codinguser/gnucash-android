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

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.*;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.transaction.dialog.BulkMoveDialogFragment;
import org.gnucash.android.ui.util.AccountBalanceTask;
import org.gnucash.android.ui.util.Refreshable;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.widget.WidgetConfigurationActivity;
import org.gnucash.android.ui.util.OnTransactionClickedListener;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * List Fragment for displaying list of transactions for an account
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class TransactionsListFragment extends SherlockListFragment implements
        Refreshable, LoaderCallbacks<Cursor>, AdapterView.OnItemLongClickListener{

	/**
	 * Logging tag
	 */
	protected static final String LOG_TAG = "TransactionsListFragment";

    private TransactionsDbAdapter mTransactionsDbAdapter;
	private SimpleCursorAdapter mCursorAdapter;
	private ActionMode mActionMode = null;
	private boolean mInEditMode = false;
    private String mAccountUID;

	/**
	 * Callback listener for editing transactions
	 */
	private OnTransactionClickedListener mTransactionEditListener;
	
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
			case R.id.context_menu_move_transactions:
				showBulkMoveDialog();
				mode.finish();
				WidgetConfigurationActivity.updateAllWidgets(getActivity());
				return true;

			case R.id.context_menu_delete:
				for (long id : getListView().getCheckedItemIds()) {
					mTransactionsDbAdapter.deleteRecord(id);
				}
				refresh();
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
		setHasOptionsMenu(true);
		Bundle args = getArguments();
		mAccountUID = args.getString(UxArgument.SELECTED_ACCOUNT_UID);

		mTransactionsDbAdapter = GnuCashApplication.getTransactionDbAdapter();
		mCursorAdapter = new TransactionsCursorAdapter(
				getActivity().getApplicationContext(), 
				R.layout.list_item_transaction, null, 
				new String[] {DatabaseSchema.TransactionEntry.COLUMN_DESCRIPTION},
				new int[] {R.id.primary_text});
		setListAdapter(mCursorAdapter);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_transactions_list, container, false);		
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {		
		super.onActivityCreated(savedInstanceState);
		
		ActionBar aBar = getSherlockActivity().getSupportActionBar();
		aBar.setDisplayShowTitleEnabled(false);
		aBar.setDisplayHomeAsUpEnabled(true);

        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		getListView().setOnItemLongClickListener(this);
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

        /*
	  Text view displaying the sum of the accounts
	 */
        TextView mSumTextView = (TextView) getView().findViewById(R.id.transactions_sum);
        new AccountBalanceTask(mSumTextView, getActivity()).execute(mAccountUID);

	}
			
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			 mTransactionEditListener = (OnTransactionClickedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnAccountSelectedListener");
		}	
	}
	
	@Override
	public void onResume() {
		super.onResume();
		((TransactionsActivity)getSherlockActivity()).updateNavigationSelection();		
		refresh(((TransactionsActivity) getActivity()).getCurrentAccountUID());
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		if (mInEditMode){
			CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox_parent_account);
			checkbox.setChecked(!checkbox.isChecked());
			return;
		}
		mTransactionEditListener.editTransaction(mTransactionsDbAdapter.getUID(id));
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
		getListView().setItemChecked(position, true);
		CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox_parent_account);
		checkbox.setChecked(true);
		startActionMode();
		return true;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {		
		inflater.inflate(R.menu.transactions_list_actions, menu);	
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
            case R.id.menu_add_transaction:
                mTransactionEditListener.createNewTransaction(mAccountUID);
                return true;

            default:
                return false;
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
		mCursorAdapter.swapCursor(cursor);
		mCursorAdapter.notifyDataSetChanged();		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		Log.d(LOG_TAG, "Resetting transactions loader");
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
		if (count > 0 && mActionMode != null){
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
        if (checkedCount <= 0 && mActionMode != null) {
            mActionMode.finish();
        }
    }
		
	/**
	 * Prepares and displays the dialog for bulk moving transactions to another account
	 */
	protected void showBulkMoveDialog(){
		FragmentManager manager = getActivity().getSupportFragmentManager();
		FragmentTransaction ft = manager.beginTransaction();
	    Fragment prev = manager.findFragmentByTag("bulk_move_dialog");
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);

	    // Create and show the dialog.
	    DialogFragment bulkMoveFragment = new BulkMoveDialogFragment();
	    Bundle args = new Bundle();
	    args.putString(UxArgument.ORIGIN_ACCOUNT_UID, mAccountUID);
	    args.putLongArray(UxArgument.SELECTED_TRANSACTION_IDS, getListView().getCheckedItemIds());
	    bulkMoveFragment.setArguments(args);
        bulkMoveFragment.setTargetFragment(this, 0);
	    bulkMoveFragment.show(ft, "bulk_move_dialog");
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

            checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				
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

            //increase the touch target area for the add new transaction button

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

            String transactionUID = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.COLUMN_UID));
			Money amount = mTransactionsDbAdapter.getBalance(transactionUID, mAccountUID);
			TextView amountTextView = (TextView) view.findViewById(R.id.transaction_amount);
            TransactionsActivity.displayBalance(amountTextView, amount);

			TextView trNote = (TextView) view.findViewById(R.id.secondary_text);
			String notes = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.COLUMN_NOTES));
			if (notes == null || notes.length() == 0)
				trNote.setVisibility(View.GONE);
			else {
				trNote.setVisibility(View.VISIBLE);
				trNote.setText(notes);
			}

            setSectionHeaderVisibility(view, cursor);
		}

        /**
         * Toggles the visibilty of the section header based on whether the previous transaction and current were
         * booked on the same day or not. Transactions a generally grouped by day
         * @param view Parent view within which to find the section header
         * @param cursor Cursor containing transaction data set
         * @see #isSameDay(long, long)
         */
        private void setSectionHeaderVisibility(View view, Cursor cursor) {
            long transactionTime = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.COLUMN_TIMESTAMP));
            int position = cursor.getPosition();

            boolean hasSectionHeader;
            if (position == 0){
                hasSectionHeader = true;
            } else {
                cursor.moveToPosition(position - 1);
                long previousTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.TransactionEntry.COLUMN_TIMESTAMP));
                cursor.moveToPosition(position);
                //has header if two consecutive transactions were not on same day
                hasSectionHeader = !isSameDay(previousTimestamp, transactionTime);
            }

            TextView dateHeader = (TextView) view.findViewById(R.id.date_section_header);

            if (hasSectionHeader){
                java.text.DateFormat format = DateFormat.getLongDateFormat(getActivity());
                String dateString = format.format(new Date(transactionTime));
                dateHeader.setText(dateString);
                dateHeader.setVisibility(View.VISIBLE);
            } else {
                dateHeader.setVisibility(View.GONE);
            }
        }

        /**
         * Checks if two timestamps have the same calendar day
         * @param timeMillis1 Timestamp in milliseconds
         * @param timeMillis2 Timestamp in milliseconds
         * @return <code>true</code> if both timestamps are on same day, <code>false</code> otherwise
         */
        private boolean isSameDay(long timeMillis1, long timeMillis2){
			Date date1 = new Date(timeMillis1);
			Date date2 = new Date(timeMillis2);
			
			SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
			return fmt.format(date1).equals(fmt.format(date2));
		}
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
			mDatabaseAdapter = GnuCashApplication.getTransactionDbAdapter();
			Cursor c = ((TransactionsDbAdapter) mDatabaseAdapter).fetchAllTransactionsForAccount(accountUID);
			if (c != null)
				registerContentObserver(c);
			return c;
		}		
	}

}
