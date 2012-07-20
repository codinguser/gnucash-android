/*
 * Written By: Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2012 Ngewi Fet
 *
 * This file is part of Gnucash for Android
 * 
 * Gnucash for Android is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, contact:
 *
 * Free Software Foundation           Voice:  +1-617-542-5942
 * 51 Franklin Street, Fifth Floor    Fax:    +1-617-542-2652
 * Boston, MA  02110-1301,  USA       gnu@gnu.org
 */

package org.gnucash.android.ui.transactions;

import java.util.HashMap;
import java.util.Locale;

import org.gnucash.android.R;
import org.gnucash.android.data.Money;
import org.gnucash.android.db.DatabaseAdapter;
import org.gnucash.android.db.DatabaseCursorLoader;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.util.OnItemClickedListener;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class TransactionsListFragment extends SherlockListFragment implements 
	LoaderCallbacks<Cursor> {

	protected static final String TAG = "TransactionsListFragment";

	private static final String SAVED_SELECTED_ITEMS = "selected_items";	
	public static final String SELECTED_ACCOUNT_ID = "selected_account_id";
	public static final String SELECTED_ACCOUNT_NAME = "selected_account_name";
	
	public static final String SELECTED_TRANSACTION_IDS = "selected_transactions";

	public static final String ORIGIN_ACCOUNT_ID = "origin_acccount_id";
	
	private TransactionsDbAdapter mTransactionsDbAdapter;
	private SimpleCursorAdapter mCursorAdapter;
	private ActionMode mActionMode = null;
	private boolean mInEditMode = false;
	private long mAccountID;
	
	private HashMap<Integer, Long> mSelectedIds = new HashMap<Integer, Long>();

	private OnItemClickedListener mTransactionEditListener;
	
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
				return true;

			case R.id.context_menu_delete:
				for (long id : mSelectedIds.values()) {
					mTransactionsDbAdapter.deleteTransaction(id);					
				}				
				refreshList();
				mode.finish();
				return true;
				
			default:
				return false;
			}
		}
	};

	private TextView mSumTextView;
	
	@Override
 	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		Bundle args = getArguments();
		mAccountID = args.getLong(SELECTED_ACCOUNT_ID);	

		mTransactionsDbAdapter = new TransactionsDbAdapter(getActivity().getApplicationContext());
		mCursorAdapter = new TransactionsCursorAdapter(
				getActivity().getApplicationContext(), 
				R.layout.list_item_transaction, null, 
				new String[] {DatabaseHelper.KEY_NAME, DatabaseHelper.KEY_AMOUNT}, 
				new int[] {R.id.transaction_name, R.id.transaction_amount});
		setListAdapter(mCursorAdapter);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_transactions_list, container, false);		
	}
		
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		int[] selectedPositions = new int[mSelectedIds.size()];
		int i = 0;
		for (Integer id : mSelectedIds.keySet()) {
			if (id == null)
				continue;
			selectedPositions[i++] = id;			
		}
		outState.putIntArray(SAVED_SELECTED_ITEMS, selectedPositions);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {		
		super.onActivityCreated(savedInstanceState);
		String title = getArguments().getString(TransactionsListFragment.SELECTED_ACCOUNT_NAME);
		ActionBar aBar = getSherlockActivity().getSupportActionBar();
		aBar.setTitle(title);
		aBar.setDisplayHomeAsUpEnabled(true);

		setHasOptionsMenu(true);		
		refreshList();
		
	}
	
	public void refreshList(){
		getLoaderManager().restartLoader(0, null, this);
		
		Money sum = mTransactionsDbAdapter.getTransactionsSum(mAccountID);		
		mSumTextView = (TextView) getView().findViewById(R.id.transactions_sum);
		mSumTextView.setText(sum.formattedString(Locale.getDefault()));
		if (sum.isNegative())
			mSumTextView.setTextColor(getResources().getColor(R.color.debit_red));
		else
			mSumTextView.setTextColor(getResources().getColor(R.color.credit_green));
			
	}
			
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			 mTransactionEditListener = (OnItemClickedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnAccountSelectedListener");
		}	
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
			CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox);
			checkbox.setChecked(!checkbox.isChecked());
			return;
		}
		mTransactionEditListener.editTransaction(id);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {		
		inflater.inflate(R.menu.transactions_list_actions, menu);	
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_transaction:
			mTransactionEditListener.createNewTransaction(mAccountID);
			return true;

		default:
			return false;
		}
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		Log.d(TAG, "Creating transactions loader");
		return new TransactionsCursorLoader(getActivity(), mAccountID);
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

	public void finishEditMode(){
		mInEditMode = false;
		deselectAllItems();
		mActionMode = null;
		mSelectedIds.clear();
	}
	
	public void setActionModeTitle(){
		int count = mSelectedIds.size();
		if (count > 0){			
			mActionMode.setTitle(count + " " + getResources().getString(R.string.selected));
		}
	}
	
	private void selectItem(int position){		
		ListView lv = getListView();	
		lv.setItemChecked(position, true);
		View v = lv.getChildAt(position);
		
		v.setSelected(true);
        v.setBackgroundColor(getResources().getColor(R.color.abs__holo_blue_light));
        long id = lv.getItemIdAtPosition(position);
        mSelectedIds.put(position, id);
	}
	
	private void deselectAllItems() {
		Integer[] selectedItemPositions = new Integer[mSelectedIds.size()];
		mSelectedIds.keySet().toArray(selectedItemPositions);
		for (int position : selectedItemPositions) {
			deselectItem(position);
		}
	}
	
	private void deselectItem(int position){
		if (position >= 0){
			getListView().setItemChecked(position, false);
			View v = getListView().getChildAt(position);
			if (v == null){
				//if we just deleted a row, then the previous position is invalid
				return;
			}
			v.setBackgroundColor(getResources().getColor(android.R.color.transparent));
			((CheckBox) v.findViewById(R.id.checkbox)).setChecked(false);
			v.setSelected(false);
			mSelectedIds.remove(position);
		}
	}
	
	private void startActionMode(){
		if (mActionMode != null) {
            return;
        }		
		mInEditMode = true;
        // Start the CAB using the ActionMode.Callback defined above
        mActionMode = getSherlockActivity().startActionMode(mActionModeCallbacks);

	}
	
	private void stopActionMode(){
		if (mSelectedIds.size() > 0)
			return;
		else
			mActionMode.finish();
	}
		
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
	    args.putLong(ORIGIN_ACCOUNT_ID, mAccountID);
	    long[] selectedIds = new long[mSelectedIds.size()]; 
	    int i = 0;
	    for (long l : mSelectedIds.values()) {
			selectedIds[i++] = l;			
		}
	    args.putLongArray(SELECTED_TRANSACTION_IDS, selectedIds);
	    bulkMoveFragment.setArguments(args);
	    bulkMoveFragment.show(ft, "bulk_move_dialog");
	}
	
	protected class TransactionsCursorAdapter extends SimpleCursorAdapter {
		
		public TransactionsCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to) {
			super(context, layout, c, from, to, 0);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			final int itemPosition = position;
			CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox);
			checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					startActionMode();
					getListView().setItemChecked(itemPosition, isChecked);
					if (isChecked){
						selectItem(itemPosition);						
					} else {
						deselectItem(itemPosition);
						stopActionMode();
					}
					setActionModeTitle();
				}
			});
			
			return view;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			super.bindView(view, context, cursor);			
			
			Money amount = new Money(
					cursor.getString(DatabaseAdapter.COLUMN_AMOUNT), 
					mTransactionsDbAdapter.getCurrencyCode(mAccountID));
			
			TextView tramount = (TextView) view.findViewById(R.id.transaction_amount);
			tramount.setText(amount.formattedString(Locale.getDefault()));
			
			if (amount.isNegative())
				tramount.setTextColor(getResources().getColor(R.color.debit_red));
			else
				tramount.setTextColor(getResources().getColor(R.color.credit_green));
			
		}
	}
	
	protected static class TransactionsCursorLoader extends DatabaseCursorLoader {
		private long accountID; 
		
		public TransactionsCursorLoader(Context context, long accountID) {
			super(context);			
			this.accountID = accountID;
		}
		
		@Override
		public Cursor loadInBackground() {
			mDatabaseAdapter = new TransactionsDbAdapter(getContext());
			Cursor c = ((TransactionsDbAdapter) mDatabaseAdapter).fetchAllTransactionsForAccount(accountID);
			if (c != null)
				registerContentObserver(c);
			return c;
		}		
	}


}
