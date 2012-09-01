/*
 * Copyright (c) 2012 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.ui.accounts;

import java.util.Locale;

import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.data.Money;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseAdapter;
import org.gnucash.android.db.DatabaseCursorLoader;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.ui.settings.SettingsActivity;
import org.gnucash.android.ui.transactions.TransactionsActivity;
import org.gnucash.android.ui.transactions.TransactionsListFragment;
import org.gnucash.android.util.OnAccountClickedListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.ListAdapter;
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

/**
 * Fragment for displaying the list of accounts in the database
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class AccountsListFragment extends SherlockListFragment implements
		LoaderCallbacks<Cursor>, OnItemLongClickListener {

	/**
	 * Request code passed when displaying the "Add Account" dialog. 
	 */
	private static final int DIALOG_ADD_ACCOUNT = 0x10;
	
	/**
	 * Logging tag
	 */
	protected static final String TAG = "AccountsListFragment";
	
	/**
	 * {@link ListAdapter} for the accounts which will be bound to the list
	 */
	AccountsCursorAdapter mAccountsCursorAdapter;
	
	/**
	 * Dialog fragment for adding new accounts
	 */
	NewAccountDialogFragment mAddAccountFragment;
	
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
	 * Position which has been selected in the ListView
	 */
	private int mSelectedViewPosition = -1;
	
	/**
	 * Stores the database ID of the currently selected account when in action mode.
	 * This is necessary because getSelectedItemId() does not work properly (by design) 
	 * in touch mode (which is the majority of devices today)
	 */
	private long mSelectedItemId = -1;
	
	/**
	 * Callbacks for the CAB menu
	 */
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

	/**
	 * Delete confirmation dialog
	 * Is displayed when deleting an account which has transactions. 
	 * If an account has no transactions, it is deleted immediately with no confirmation required
	 * @author Ngewi Fet <ngewif@gmail.com>
	 *
	 */
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
		mAccountsCursorAdapter = new AccountsCursorAdapter(
				getActivity().getApplicationContext(), 
				R.layout.list_item_account, null,
				new String[] { DatabaseHelper.KEY_NAME },
				new int[] { R.id.account_name });
						
		setListAdapter(mAccountsCursorAdapter);
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
	public void onResume() {	
		super.onResume();
		refreshList();
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
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		if (mInEditMode){
			mSelectedItemId = id;
			selectItem(position);
			return;
		}
		mAccountSelectedListener.accountSelected(id);
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

	/**
	 * Delete the account with record ID <code>rowId</code>
	 * It shows the delete confirmation dialog if the account has transactions,
	 * else deletes the account immediately
	 * @param rowId The record ID of the account
	 */
	public void tryDeleteAccount(long rowId){
		Account acc = mAccountsDbAdapter.getAccount(rowId);
		if (acc.getTransactionCount() > 0){
			showConfirmationDialog(rowId);
		} else {
			deleteAccount(rowId);
		}
	}
	
	/**
	 * Deletes an account and show a {@link Toast} notification on success
	 * @param rowId Record ID of the account to be deleted
	 */
	protected void deleteAccount(long rowId){		

		boolean deleted = mAccountsDbAdapter.destructiveDeleteAccount(rowId);
		if (deleted){
			Toast.makeText(getActivity(), R.string.toast_account_deleted, Toast.LENGTH_SHORT).show();
		}
		refreshList();	
	}

	/**
	 * Shows the delete confirmation dialog
	 * @param id Record ID of account to be deleted after confirmation
	 */
	public void showConfirmationDialog(long id){
		MyAlertDialogFragment alertFragment = MyAlertDialogFragment.newInstance(R.string.title_confirm_delete, id);
		alertFragment.setTargetFragment(this, 0);
		alertFragment.show(getSherlockActivity().getSupportFragmentManager(), "dialog");
	}
	
	/**
	 * Finish the edit mode and dismisses the Contextual ActionBar
	 * Any selected (highlighted) accounts are deselected
	 */
	public void finishEditMode(){
		mInEditMode = false;
		deselectPreviousSelectedItem();
		mActionMode = null;
		mSelectedItemId = -1;
	}
	
	/**
	 * Highlights the item at <code>position</code> in the ListView.
	 * Android has facilities for managing list selection but the highlighting 
	 * is not reliable when using the ActionBar on pre-Honeycomb devices-
	 * @param position Position of item to be highlighted
	 */
	private void selectItem(int position){
		deselectPreviousSelectedItem();		
		ListView lv = getListView();	
		lv.setItemChecked(position, true);
		View v = lv.getChildAt(position);
		v.setSelected(true);
        v.setBackgroundColor(getResources().getColor(R.color.abs__holo_blue_light));
        mSelectedViewPosition = position;
	}
	
	/**
	 * De-selects the previously selected item in a ListView.
	 * Only one account entry can be highlighted at a time, so the previously selected
	 * one is deselected. 
	 */
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
			
		case R.id.menu_settings:
			startActivity(new Intent(getActivity(), SettingsActivity.class));
			return true;
			
		default:
			return false;
		}
	}
	
	/**
	 * Refreshes the list by restarting the {@link DatabaseCursorLoader} associated
	 * with the ListView
	 */
	public void refreshList(){
		getLoaderManager().restartLoader(0, null, this);
	}
	
	/**
	 * Closes any open database adapters used by the list
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		mAccountsDbAdapter.close();
		mAccountsCursorAdapter.close();
	}	
	
	/**
	 * Show dialog for creating a new {@link Account}
	 */
	public void showAddAccountDialog(long accountId) {
		FragmentManager manager = getSherlockActivity().getSupportFragmentManager();
		FragmentTransaction ft = manager.beginTransaction();
		Fragment prev = manager.findFragmentByTag(AccountsActivity.FRAGMENT_NEW_ACCOUNT);
		
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
		mAddAccountFragment.show(ft, AccountsActivity.FRAGMENT_NEW_ACCOUNT);
	}

	/**
	 * Displays the dialog for exporting transactions in OFX
	 */
	public void showExportDialog(){
		FragmentManager manager = getSherlockActivity().getSupportFragmentManager();
		FragmentTransaction ft = manager.beginTransaction();
	    Fragment prev = manager.findFragmentByTag(AccountsActivity.FRAGMENT_EXPORT_OFX);
	    if (prev != null) {
	        ft.remove(prev);
	    }
	    ft.addToBackStack(null);

	    // Create and show the dialog.
	    DialogFragment exportFragment = new ExportDialogFragment();
	    exportFragment.show(ft, AccountsActivity.FRAGMENT_EXPORT_OFX);
	}
	
	/**
	 * Overrides the {@link SimpleCursorAdapter} to provide custom binding of the 
	 * information from the database to the views
	 * @author Ngewi Fet <ngewif@gmail.com>
	 */
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
			
			Money balance = transactionsDBAdapter.getTransactionsSum(accountId);
			summary.setText(balance.formattedString(Locale.getDefault()));
			int fontColor = balance.isNegative() ? getResources().getColor(R.color.debit_red) : 
				getResources().getColor(R.color.credit_green);
			summary.setTextColor(fontColor);
			
			ImageView newTrans = (ImageView) v.findViewById(R.id.btn_new_transaction);
			newTrans.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(getActivity(), TransactionsActivity.class);
					intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
					intent.putExtra(TransactionsListFragment.SELECTED_ACCOUNT_ID, accountId);
					getActivity().startActivity(intent);
				}
			});
		}
	}

	/**
	 * Extends {@link DatabaseCursorLoader} for loading of {@link Account} from the 
	 * database asynchronously
	 * @author Ngewi Fet <ngewif@gmail.com>
	 */
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
		mAccountsCursorAdapter.swapCursor(cursor);
		mAccountsCursorAdapter.notifyDataSetChanged();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		Log.d(TAG, "Resetting the accounts loader");
		mAccountsCursorAdapter.swapCursor(null);
	}	

}
