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
import org.gnucash.android.data.Transaction;
import org.gnucash.android.db.DatabaseAdapter;
import org.gnucash.android.db.DatabaseCursorLoader;
import org.gnucash.android.db.DatabaseHelper;
import org.gnucash.android.db.TransactionsDbAdapter;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

public class TransactionsListFragment extends SherlockListFragment implements LoaderCallbacks<Cursor>{

	protected static final String TAG = "TransactionsListFragment";
	
	public static final String SELECTED_ACCOUNT_ID = "selected_account_id";
	public static final String SELECTED_ACCOUNT_NAME = "selected_account_name";
	private TransactionsDbAdapter mDbAdapter;
	private SimpleCursorAdapter mCursorAdapter;
	private long mAccountID;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		Bundle args = getArguments();
		mAccountID = args.getLong(SELECTED_ACCOUNT_ID);		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_transactions_list, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {		
		super.onActivityCreated(savedInstanceState);
		String title = getArguments().getString(TransactionsListFragment.SELECTED_ACCOUNT_NAME);
		getSherlockActivity().getSupportActionBar().setTitle(title);
		
		mDbAdapter = new TransactionsDbAdapter(getActivity().getApplicationContext());
		mCursorAdapter = new TransactionsCursorAdapter(
				getActivity().getApplicationContext(), 
				R.layout.list_item_transaction, null, 
				new String[] {DatabaseHelper.KEY_NAME, DatabaseHelper.KEY_AMOUNT}, 
				new int[] {R.id.transaction_name, R.id.transaction_amount});
		setListAdapter(mCursorAdapter);
		
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mDbAdapter.close();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {		
		super.onCreateOptionsMenu(menu, inflater);		
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
	
	
	protected class TransactionsCursorAdapter extends SimpleCursorAdapter {
		
		public TransactionsCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to) {
			super(context, layout, c, from, to, 0);
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			super.bindView(view, context, cursor);
			
			double amount = cursor.getDouble(DatabaseAdapter.COLUMN_AMOUNT);
			
			TextView tramount = (TextView) view.findViewById(R.id.transaction_amount);
			tramount.setText(Transaction.getFormattedAmount(amount));
			
			if (amount < 0)
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
			mDatabaseAdapter = new TransactionsDbAdapter(mContext);
			return ((TransactionsDbAdapter) mDatabaseAdapter).fetchAllTransactionsForAccount(accountID);
		}		
	}
}
