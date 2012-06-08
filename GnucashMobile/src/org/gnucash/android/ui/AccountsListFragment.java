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
import org.gnucash.android.util.OnAccountSelectedListener;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class AccountsListFragment extends SherlockListFragment implements
		LoaderCallbacks<Cursor>, View.OnClickListener {

	protected static final String FRAGMENT_NEW_ACCOUNT = "new_account_dialog";

	private static final int DIALOG_ADD_ACCOUNT = 0x10;
	
	protected static final String TAG = "AccountsListFragment";
	
	SimpleCursorAdapter mCursorAdapter;
	NewAccountDialogFragment mAddAccountFragment;
	private AccountsDbAdapter mAccountsDbAdapter;	
	private OnAccountSelectedListener mAccountSelectedListener;	
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_accounts_list, container,
				false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mAccountsDbAdapter = new AccountsDbAdapter(getActivity());
		
		getSherlockActivity().getSupportActionBar().setTitle(R.string.title_accounts);
		
		setHasOptionsMenu(true);
		mCursorAdapter = new AccountsCursorAdapter(
				getActivity(), 
				R.layout.list_item_account, null,
				new String[] { DatabaseHelper.KEY_NAME },
				new int[] { R.id.account_name });

		getLoaderManager().initLoader(0, null, this);
		setListAdapter(mCursorAdapter);	
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mAccountSelectedListener = (OnAccountSelectedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnArticleSelectedListener");
		}
	
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		TextView tv = (TextView) v.findViewById(R.id.account_name);
		String name = tv.getText().toString();
		mAccountSelectedListener.accountSelected(id, name);
	}	
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.acccount_actions, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_edit_accounts:
			return true;

		case R.id.menu_add_account:
			showAddAccountDialog();
			return true;

		default:
			return true;
		}
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroy();
		mAccountsDbAdapter.close();
	}
	
	public void addAccount(String name) {
		mAccountsDbAdapter.addAccount(new Account(name));			
		getLoaderManager().restartLoader(0, null, this);
	}
	
	/**
	 * Show dialog for creating a new {@link Account}
	 */
	public void showAddAccountDialog() {

		FragmentTransaction ft = getSherlockActivity()
				.getSupportFragmentManager().beginTransaction();
		Fragment prev = getSherlockActivity().getSupportFragmentManager()
				.findFragmentByTag(FRAGMENT_NEW_ACCOUNT);
		if (prev != null) {
			ft.remove(prev);
		}

		ft.addToBackStack(null);

		mAddAccountFragment = NewAccountDialogFragment
				.newInstance(this);
		mAddAccountFragment.setTargetFragment(this, DIALOG_ADD_ACCOUNT);
		mAddAccountFragment.show(ft, FRAGMENT_NEW_ACCOUNT);
	}

	/**
	 * Handles creation of new account from the new account dialog
	 */
	@Override
	public void onClick(View v) {		
		addAccount(mAddAccountFragment.getEnteredName());
		mAddAccountFragment.dismiss();
	}
	
	private class AccountsCursorAdapter extends SimpleCursorAdapter {
		public AccountsCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to) {
			super(context, layout, c, from, to, 0);
		}
		
		@Override
		public void bindView(View v, Context context, Cursor cursor) {
			// perform the default binding
			super.bindView(v, context, cursor);

			// add a summary of transactions to the account view
			TextView summary = (TextView) v
					.findViewById(R.id.transactions_summary);
			Account acc = mAccountsDbAdapter.buildAccountInstance(cursor);
			double balance = acc.getBalance();
			int count = acc.getTransactionCount();			
			String statement = "";
			if (count == 0) {
				statement = "No transactions on this account";
			} else {
				String pluralizedText = count != 1 ? " transactions totalling "
						: " transaction totalling ";

				// TODO: Allow the user to set locale, or get it from phone
				// location

				String formattedAmount = Transaction.getFormattedAmount(balance);
				statement = count + pluralizedText + formattedAmount;
			}
			summary.setText(statement);		
			
			ImageView newTrans = (ImageView) v.findViewById(R.id.btn_new_transaction);
			final long accountId = cursor.getLong(DatabaseAdapter.COLUMN_ROW_ID);
			newTrans.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					mAccountSelectedListener.createNewTransaction(accountId);
				}
			});
		}
	}

	private static final class AccountsCursorLoader extends DatabaseCursorLoader {
		//TODO: close this account adapter somewhere
//		AccountsDbAdapter accountsDbAdapter;
		
		public AccountsCursorLoader(Context context) {
			super(context);		
		}

		@Override
		public Cursor loadInBackground() {			
			mDatabaseAdapter = new AccountsDbAdapter(mContext);			
			return ((AccountsDbAdapter) mDatabaseAdapter).fetchAllAccounts();
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.d(TAG, "Creating the accounts loader");
		return new AccountsCursorLoader(this.getActivity());
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
