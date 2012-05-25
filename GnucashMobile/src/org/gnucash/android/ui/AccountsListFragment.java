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

import java.text.NumberFormat;
import java.util.Locale;

import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseHelper;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.MenuItem;

public class AccountsListFragment extends SherlockListFragment {

	SimpleCursorAdapter mCursorAdapter;
	private Cursor mAccountsCursor;
	private AccountsDbAdapter mDbAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_accounts_list, container,
				false);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAccountsCursor = mDbAdapter.fetchAllAccounts();
		getActivity().startManagingCursor(mAccountsCursor);
		mCursorAdapter = new AccountsCursorAdapter(getActivity()
				.getApplicationContext(), R.layout.item_accounts,
				mAccountsCursor, new String[] { DatabaseHelper.KEY_NAME },
				new int[] { R.id.account_name },
				SimpleCursorAdapter.FLAG_AUTO_REQUERY);

		setListAdapter(mCursorAdapter);
		setHasOptionsMenu(true);
	}

	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mDbAdapter = ((AccountsActivity) getActivity()).getAccountsDbAdapter();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_edit_accounts:
			return true;

		default:
			return true;
		}
	}
	
	private class AccountsCursorAdapter extends SimpleCursorAdapter {
		public AccountsCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);
		}
		
		@Override
		public void bindView(View v, Context context, Cursor cursor) {
			// perform the default binding
			super.bindView(v, context, cursor);					
			
			// add a summary of transactions to the account view
			TextView summary = (TextView) v
					.findViewById(R.id.transactions_summary);
			Account acc = mDbAdapter.buildAccountInstance(cursor);
			double balance = acc.getBalance();
			int count = acc.getTransactionCount();
			String statement = "";
			if (count == 0) {
				statement = "No transactions on this account";
			} else {
				String pluralizedText = count != 1 ? " transactions totalling "
						: " transaction totalling ";

				//TODO: Allow the user to set locale, or get it from phone location
				NumberFormat currencyformatter = NumberFormat
						.getCurrencyInstance(Locale.getDefault());

				String formattedAmount = currencyformatter.format(balance);
				statement = count + pluralizedText + formattedAmount;
			}
			summary.setText(statement);
		}
	}
}
