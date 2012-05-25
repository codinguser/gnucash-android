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
import org.gnucash.android.db.AccountsDbAdapter;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Displays the list of accounts and summary of transactions
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class AccountsActivity extends SherlockFragmentActivity {
	
	static final int DIALOG_ADD_ACCOUNT = 0x01;
	
	protected static final String TAG = "AccountsActivity";

	private AccountsDbAdapter mAccountsDbAdapter;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.activity_accounts);
        
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();      
        fragmentTransaction.add(R.id.fragment_container, new AccountsListFragment(), "accounts_list");
        mAccountsDbAdapter = new AccountsDbAdapter(this.getApplicationContext());
        
        fragmentTransaction.commit();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	mAccountsDbAdapter.close();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.action_bar, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
		case R.id.menu_add_account:
			showDialog();
			return true;

		default:
			return false; //propagate processing to fragments
		}
    }
    
    public void showDialog(){
    	
    	FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    	Fragment prev = getSupportFragmentManager().findFragmentByTag("add_account_dialog");
    	if (prev != null){
    		ft.remove(prev);
    	}
    	
    	ft.addToBackStack(null);
    	
    	AddAccountDialogFragment addAccountFragment = AddAccountDialogFragment.newInstance();    	
    	addAccountFragment.show(ft, "add_account_dialog");
    }

    public void onNewAccountClick(View v){
    	showDialog();
    }
    
	public void addAccount(String name) {
		mAccountsDbAdapter.addAccount(new Account(name));
	}
	
	public AccountsDbAdapter getAccountsDbAdapter(){
		return mAccountsDbAdapter;
	}
}