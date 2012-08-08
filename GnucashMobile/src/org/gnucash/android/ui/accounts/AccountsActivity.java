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

import java.util.ArrayList;
import java.util.Currency;
import java.util.Locale;

import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.data.Money;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.ui.transactions.TransactionsActivity;
import org.gnucash.android.ui.transactions.TransactionsListFragment;
import org.gnucash.android.util.OnAccountClickedListener;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Displays the list of accounts and summary of transactions
 * 
 * @author Ngewi Fet <ngewif@gmail.com>
 * 
 */
public class AccountsActivity extends SherlockFragmentActivity implements OnAccountClickedListener {

	public static final String FRAGMENT_ACCOUNTS_LIST 	= "accounts_list";
		
	protected static final String TAG = "AccountsActivity";	
	
	private ArrayList<Integer> mSelectedDefaultAccounts = new ArrayList<Integer>();
	private AlertDialog mDefaultAccountsDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_accounts);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String currencyCode = prefs.getString(getString(R.string.pref_default_currency), Currency.getInstance(Locale.getDefault()).getCurrencyCode());
		Money.DEFAULT_CURRENCY_CODE = currencyCode;		
		
		boolean firstRun = prefs.getBoolean(getString(R.string.key_first_run), true);
		if (firstRun){
			createDefaultAccounts();
		}
		
		FragmentManager fragmentManager = getSupportFragmentManager();

		AccountsListFragment accountsListFragment = (AccountsListFragment) fragmentManager
				.findFragmentByTag(FRAGMENT_ACCOUNTS_LIST);

		if (accountsListFragment == null) {
			FragmentTransaction fragmentTransaction = fragmentManager
					.beginTransaction();
			fragmentTransaction.add(R.id.fragment_container,
					new AccountsListFragment(), FRAGMENT_ACCOUNTS_LIST);

			fragmentTransaction.commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.global_actions, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
	        FragmentManager fm = getSupportFragmentManager();
	        if (fm.getBackStackEntryCount() > 0) {
	            fm.popBackStack();
	        }
	        return true;

		default:
			return false;
		}
	}

	@Override
	protected void onActivityResult(int arg0, int arg1, Intent arg2) {
		// TODO Auto-generated method stub
		super.onActivityResult(arg0, arg1, arg2);
	}
	
	/**
	 * Opens a dialog fragment to create a new account
	 * @param v View which triggered this callback
	 */
	public void onNewAccountClick(View v) {
		AccountsListFragment accountFragment = (AccountsListFragment) getSupportFragmentManager()
				.findFragmentByTag(FRAGMENT_ACCOUNTS_LIST);
		if (accountFragment != null)
			accountFragment.showAddAccountDialog(0);
	}

	private void createDefaultAccounts(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		boolean[] checkedDefaults = new boolean[]{true, true, false, false, false};
		//add the checked defaults, the rest will be added by user action
		mSelectedDefaultAccounts.add(0);
		mSelectedDefaultAccounts.add(1);
		builder.setTitle(R.string.title_default_accounts);		
		builder.setMultiChoiceItems(R.array.default_accounts, checkedDefaults, new DialogInterface.OnMultiChoiceClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				if (isChecked){
					mSelectedDefaultAccounts.add(which);
				} else {
					mSelectedDefaultAccounts.remove(Integer.valueOf(which));
				}
			}
		});
		builder.setPositiveButton(R.string.btn_create_accounts, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				AccountsDbAdapter dbAdapter = new AccountsDbAdapter(getApplicationContext());
				String[] defaultAccounts = getResources().getStringArray(R.array.default_accounts);
				for (int index : mSelectedDefaultAccounts) {
					String name = defaultAccounts[index];
					dbAdapter.addAccount(new Account(name));
				}
				
				dbAdapter.close();
				removeFirstRunFlag();
				Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_ACCOUNTS_LIST);
				if (fragment != null){
					try{
						((AccountsListFragment) fragment).refreshList();
					} catch (ClassCastException e) {
						Log.e(TAG, e.getMessage());
					}
				}
			}
		});
		
		builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mDefaultAccountsDialog.dismiss();
				removeFirstRunFlag();
			}
		});
		mDefaultAccountsDialog = builder.create();
		mDefaultAccountsDialog.show();		
	}
		
	@Override
	public void accountSelected(long accountRowId) {
		Intent intent = new Intent(this, TransactionsActivity.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.putExtra(TransactionsListFragment.SELECTED_ACCOUNT_ID, accountRowId);
		
		startActivity(intent);
	}
	
	private void removeFirstRunFlag(){
		Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.putBoolean(getString(R.string.key_first_run), false);
		editor.commit();
	}

}