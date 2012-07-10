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

package org.gnucash.android.ui;

import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.data.Transaction;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseAdapter;
import org.gnucash.android.db.DatabaseCursorLoader;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.util.OnItemClickedListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.ActionMode.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class AccountsListFragment extends SherlockListFragment implements
		LoaderCallbacks<Cursor>, OnItemLongClickListener {

	protected static final String FRAGMENT_NEW_ACCOUNT = "new_account_dialog";
	protected static final String FRAGMENT_EXPORT_OFX  = "export_ofx";
	private static final int DIALOG_ADD_ACCOUNT = 0x10;
	
	protected static final String TAG = "AccountsListFragment";
	
	AccountsCursorAdapter mCursorAdapter;
	NewAccountDialogFragment mAddAccountFragment;
	private AccountsDbAdapter mAccountsDbAdapter;	
	private OnItemClickedListener mAccountSelectedListener;	
	private boolean mInEditMode = false;
	private ActionMode mActionMode = null;
	private int mSelectedViewPosition = -1;
	
	/**
	 * Stores the database ID of the currently selected account when in action mode.
	 * This is necessary because getSelectedItemId() does not work properly (by design) 
	 * in touch mode (which is the majority of devices today)
	 */
	private long mSelectedItemId = -1;
	
	private ActionMode.Callback mActionModeCallbacks = new Callback() {
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
	        inflater.inflate(R.menu.account_context_menu, menu);
	        mode.setTitle("1 selected");
	        return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			// nothing to see here, move along
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.context_menu_edit_accounts:
				showAddAccountDialog(mSelectedItemId);
				return true;

			case R.id.context_menu_delete:
				tryDeleteAccount(mSelectedItemId);
				mode.finish();
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

	public static class MyAlertDialogFragment extends SherlockDialogFragment {

        public static MyAlertDialogFragment newInstance(int title, long id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("title", title);
            args.putLong(TransactionsListFragment.SELECTED_ACCOUNT_ID, id);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int title = getArguments().getInt("title");
            final long rowId = getArguments().getLong(TransactionsListFragment.SELECTED_ACCOUNT_ID);
            
            return new AlertDialog.Builder(getActivity())
                    .setIcon(android.R.drawable.ic_delete)
                    .setTitle(title).setMessage(R.string.delete_confirmation_message)
                    .setPositiveButton(R.string.alert_dialog_ok_delete,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ((AccountsListFragment) getTargetFragment()).deleteAccount(rowId);
                            }
                        }
                    )
                    .setNegativeButton(R.string.alert_dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            	dismiss();
                            }
                        }
                    )
                    .create();
        }
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_accounts_list, container,
				false);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAccountsDbAdapter = new AccountsDbAdapter(getActivity());
		mCursorAdapter = new AccountsCursorAdapter(
				getActivity().getApplicationContext(), 
				R.layout.list_item_account, null,
				new String[] { DatabaseHelper.KEY_NAME },
				new int[] { R.id.account_name });
						
		setListAdapter(mCursorAdapter);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		ActionBar actionbar = getSherlockActivity().getSupportActionBar();
		actionbar.setTitle(R.string.title_accounts);
		actionbar.setDisplayHomeAsUpEnabled(false);
		
		setHasOptionsMenu(true);
		
		ListView lv = getListView();
		lv.setOnItemLongClickListener(this);	
		getLoaderManager().initLoader(0, null, this);		
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mAccountSelectedListener = (OnItemClickedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnAccountSelectedListener");
		}	
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		if (mInEditMode){
			mSelectedItemId = id;
			selectItem(position);
			return;
		}
		TextView tv = (TextView) v.findViewById(R.id.account_name);
		String name = tv.getText().toString();
		mAccountSelectedListener.accountSelected(id, name);
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
	        mActionMode = getSherlockActivity().startActionMode(mActionModeCallbacks);
	             
	        selectItem(position);
	        return true;
		}

	public void tryDeleteAccount(long rowId){
		Account acc = mAccountsDbAdapter.getAccount(rowId);
		if (acc.getTransactionCount() > 0){
			showConfirmationDialog(rowId);
		} else {
			deleteAccount(rowId);
		}
	}
	
	protected void deleteAccount(long rowId){		

		boolean deleted = mAccountsDbAdapter.destructiveDeleteAccount(rowId);
		if (deleted){
			Toast.makeText(getActivity(), R.string.notify_account_deleted, Toast.LENGTH_SHORT).show();
		}
		refreshList();	
	}

	public void showConfirmationDialog(long id){
		MyAlertDialogFragment alertFragment = MyAlertDialogFragment.newInstance(R.string.title_confirm_delete, id);
		alertFragment.setTargetFragment(this, 0);
		alertFragment.show(getSherlockActivity().getSupportFragmentManager(), "dialog");
	}
	
	public void finishEditMode(){
		mInEditMode = false;
		deselectPreviousSelectedItem();
		mActionMode = null;
		mSelectedItemId = -1;
	}
	
	private void selectItem(int position){
		deselectPreviousSelectedItem();		
		ListView lv = getListView();	
		lv.setItemChecked(position, true);
		View v = lv.getChildAt(position);
		v.setSelected(true);
        v.setBackgroundColor(getResources().getColor(R.color.abs__holo_blue_light));
        mSelectedViewPosition = position;
	}
	
	private void deselectPreviousSelectedItem(){
		if (mSelectedViewPosition >= 0){
			getListView().setItemChecked(mSelectedViewPosition, false);
			View v = getListView().getChildAt(mSelectedViewPosition);
			if (v == null){
				//if we just deleted a row, then the previous position is invalid
				return;
			}
			v.setBackgroundColor(getResources().getColor(android.R.color.transparent));
			v.setSelected(false);
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.account_actions, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_add_account:
			showAddAccountDialog(0);
			return true;

		case R.id.menu_export:
			showExportDialog();
			return true;
			
		default:
			return false;
		}
	}
	
	public void refreshList(){
		getLoaderManager().restartLoader(0, null, this);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mAccountsDbAdapter.close();
		mCursorAdapter.close();
	}	
	
	/**
	 * Show dialog for creating a new {@link Account}
	 */
	public void showAddAccountDialog(long accountId) {
		FragmentManager manager = getSherlockActivity().getSupportFragmentManager();
		FragmentTransaction ft = manager.beginTransaction();
		Fragment prev = manager.findFragmentByTag(FRAGMENT_NEW_ACCOUNT);
		
		if (prev != null) {
			ft.remove(prev);
		}

		ft.addToBackStack(null);

		mAddAccountFragment = NewAccountDialogFragment.newInstance(mAccountsDbAdapter);
		Bundle args = new Bundle();
		args.putLong(TransactionsListFragment.SELECTED_ACCOUNT_ID, accountId);
		mAddAccountFragment.setArguments(args);
		
		mAddAccountFragment.setTargetFragment(this, DIALOG_ADD_ACCOUNT);
		if (mActionMode != null){
			//if we were editing, stop before going somewhere else
			mActionMode.finish(); 
		}
		mAddAccountFragment.show(ft, FRAGMENT_NEW_ACCOUNT);
	}

	public void showExportDialog(){
		FragmentManager manager = getSherlockActivity().getSupportFragmentManager();
		FragmentTransaction ft = manager.beginTransaction();
	    Fragment prev = manager.findFragmentByTag("dialog");
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);

	    // Create and show the dialog.
	    DialogFragment exportFragment = new ExportDialogFragment();
	    exportFragment.show(ft, FRAGMENT_EXPORT_OFX);
	}
	
	private class AccountsCursorAdapter extends SimpleCursorAdapter {
		TransactionsDbAdapter transactionsDBAdapter;
		
		public AccountsCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to) {
			super(context, layout, c, from, to, 0);
			transactionsDBAdapter = new TransactionsDbAdapter(context);
		}

		public void close(){
			transactionsDBAdapter.close();
		}
		
		@Override
		public void bindView(View v, Context context, Cursor cursor) {
			// perform the default binding
			super.bindView(v, context, cursor);

			// add a summary of transactions to the account view
			TextView summary = (TextView) v
					.findViewById(R.id.transactions_summary);
			final long accountId = cursor.getLong(DatabaseAdapter.COLUMN_ROW_ID);
			
			double balance = transactionsDBAdapter.getTransactionsSum(accountId);
			summary.setText(Transaction.getFormattedAmount(balance));
			int fontColor = balance < 0 ? getResources().getColor(R.color.debit_red) : 
				getResources().getColor(R.color.credit_green);
			summary.setTextColor(fontColor);
			
			ImageView newTrans = (ImageView) v.findViewById(R.id.btn_new_transaction);
			newTrans.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					mAccountSelectedListener.createNewTransaction(accountId);
				}
			});
		}
	}

	private static final class AccountsCursorLoader extends DatabaseCursorLoader {
		
		public AccountsCursorLoader(Context context) {
			super(context);		
		}

		@Override
		public Cursor loadInBackground() {			
			mDatabaseAdapter = new AccountsDbAdapter(getContext());	
			Cursor cursor = ((AccountsDbAdapter) mDatabaseAdapter).fetchAllAccounts();		
			if (cursor != null)
				registerContentObserver(cursor);
			return cursor;
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.d(TAG, "Creating the accounts loader");
		return new AccountsCursorLoader(this.getActivity().getApplicationContext());		
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loaderCursor, Cursor cursor) {
		Log.d(TAG, "Accounts loader finished. Swapping in cursor");
		mCursorAdapter.swapCursor(cursor);
		mCursorAdapter.notifyDataSetChanged();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		Log.d(TAG, "Resetting the accounts loader");
		mCursorAdapter.swapCursor(null);
	}	

}
