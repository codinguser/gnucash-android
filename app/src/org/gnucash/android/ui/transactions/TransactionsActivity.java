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

package org.gnucash.android.ui.transactions;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.*;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.ResourceCursorAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.viewpagerindicator.TitlePageIndicator;
import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseAdapter;
import org.gnucash.android.ui.Refreshable;
import org.gnucash.android.ui.accounts.AccountsActivity;
import org.gnucash.android.ui.accounts.AccountsListFragment;
import org.gnucash.android.util.OnAccountClickedListener;
import org.gnucash.android.util.OnTransactionClickedListener;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity for displaying, creating and editing transactions
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class TransactionsActivity extends SherlockFragmentActivity implements
        Refreshable, OnAccountClickedListener, OnTransactionClickedListener{

	/**
	 * Logging tag
	 */
	protected static final String TAG = "AccountsActivity";
	
	/**
	 * Tag for {@link TransactionsListFragment}
	 * Can be used to check if the fragment is currently loaded
	 */
	public static final String FRAGMENT_TRANSACTIONS_LIST 	= "transactions_list";
	
	/**
	 * Tag for {@link NewTransactionFragment}
	 */
	public static final String FRAGMENT_NEW_TRANSACTION 	= "new_transaction";
    private static final int REQUEST_EDIT_ACCOUNT           = 0x21;

    /**
     * ViewPager index for sub-accounts fragment
     */
    private static final int INDEX_SUB_ACCOUNTS_FRAGMENT     = 0;

    /**
     * ViewPager index for transactions fragment
     */
    private static final int INDEX_TRANSACTIONS_FRAGMENT     = 1;

    /**
     * Number of pages to show
     */
    private static final int DEFAULT_NUM_PAGES = 2;

    /**
     * Pager widget for swiping horizontally between views
     */
    private ViewPager mPager;

    /**
     * Provides the pages to the view pager widget
     */
    private PagerAdapter mPagerAdapter;


    /**
	 * Database ID of {@link Account} whose transactions are displayed 
	 */
	private long mAccountId 	= 0;
		
	/**
	 * Flag which is used to determine if the activity is running or not. 
	 * Basically if onCreate has already been called or not. It is used
	 * to determine if to call addToBackStack() for fragments. When adding 
	 * the very first fragment, it should not be added to the backstack.
	 * @see #showTransactionFormFragment(Bundle)
	 */
	private boolean mActivityRunning = false;

    /**
     * Account database adapter for manipulating the accounts list in navigation
     */
    private AccountsDbAdapter mAccountsDbAdapter;

    /**
     * Spinner adapter for the action bar navigation list of accounts
     */
    private SpinnerAdapter mSpinnerAdapter;

    /**
     * This is the last known color for the title indicator.
     * This is used to remember the color of the top level account if the child account doesn't have one.
     */
    private static int sLastTitleColor = R.color.title_green;

    private TextView mSectionHeaderTransactions;
    private TitlePageIndicator mTitlePageIndicator;

    private Map<Integer, Refreshable> mFragmentPageReferenceMap = new HashMap();

	private OnNavigationListener mTransactionListNavigationListener = new OnNavigationListener() {

		  @Override
		  public boolean onNavigationItemSelected(int position, long itemId) {
			mAccountId = itemId;

            FragmentManager fragmentManager = getSupportFragmentManager();

		    //inform new accounts fragment that account was changed
		    NewTransactionFragment newTransactionsFragment = (NewTransactionFragment) fragmentManager
					.findFragmentByTag(FRAGMENT_NEW_TRANSACTION);
		    if (newTransactionsFragment != null){
		    	newTransactionsFragment.onAccountChanged(itemId);
		    	//if we do not return, the transactions list fragment could also be found (although it's not visible)
		    	return true;
		    }

            refresh();
            return true;
		  }
	};




    private class AccountViewPagerAdapter extends FragmentStatePagerAdapter {

        public AccountViewPagerAdapter(FragmentManager fm){
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            if (isPlaceHolderAccount()){
                Fragment transactionsListFragment = prepareSubAccountsListFragment();
                mFragmentPageReferenceMap.put(i, (Refreshable) transactionsListFragment);
                return transactionsListFragment;
            }

            Fragment currentFragment;
            switch (i){
                case INDEX_SUB_ACCOUNTS_FRAGMENT:
                    currentFragment = prepareSubAccountsListFragment();
                    break;

                case INDEX_TRANSACTIONS_FRAGMENT:
                default:
                    currentFragment = prepareTransactionsListFragment();
                    break;
            }

            mFragmentPageReferenceMap.put(i, (Refreshable)currentFragment);
            return currentFragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            mFragmentPageReferenceMap.remove(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (isPlaceHolderAccount())
                return getString(R.string.section_header_subaccounts);

            switch (position){
                case INDEX_SUB_ACCOUNTS_FRAGMENT:
                    return getString(R.string.section_header_subaccounts);

                case INDEX_TRANSACTIONS_FRAGMENT:
                default:
                    return getString(R.string.section_header_transactions);
            }
        }

        @Override
        public int getCount() {
            if (isPlaceHolderAccount())
                return 1;
            else
                return DEFAULT_NUM_PAGES;
        }

        /**
         * Creates and initializes the fragment for displaying sub-account list
         * @return {@link AccountsListFragment} initialized with the sub-accounts
         */
        private AccountsListFragment prepareSubAccountsListFragment(){
            AccountsListFragment subAccountsListFragment = new AccountsListFragment();
            Bundle args = new Bundle();
            args.putLong(AccountsListFragment.ARG_PARENT_ACCOUNT_ID, mAccountId);
            subAccountsListFragment.setArguments(args);
            return subAccountsListFragment;
        }

        /**
         * Creates and initializes fragment for displaying transactions
         * @return {@link TransactionsListFragment} initialized with the current account transactions
         */
        private TransactionsListFragment prepareTransactionsListFragment(){
            TransactionsListFragment transactionsListFragment = new TransactionsListFragment();
            Bundle args = new Bundle();
            args.putLong(TransactionsListFragment.SELECTED_ACCOUNT_ID,
                    mAccountId);
            transactionsListFragment.setArguments(args);
            Log.i(TAG, "Opening transactions for account id " +  mAccountId);
            return transactionsListFragment;
        }
    }

    /**
     * Returns <code>true</code> is the current account is a placeholder account, <code>false</code> otherwise.
     * @return <code>true</code> is the current account is a placeholder account, <code>false</code> otherwise.
     */
    private boolean isPlaceHolderAccount(){
        return mAccountsDbAdapter.isPlaceholderAccount(mAccountId);
    }

    /**
     * Refreshes the fragments currently in the transactions activity
     */
    @Override
    public void refresh(long accountId) {
        for (Refreshable refreshableFragment : mFragmentPageReferenceMap.values()) {
            refreshableFragment.refresh(accountId);
        }
        mTitlePageIndicator.notifyDataSetChanged();
    }

    @Override
    public void refresh(){
        refresh(mAccountId);
        setTitleIndicatorColor();
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_transactions);

        mPager = (ViewPager) findViewById(R.id.pager);
        mTitlePageIndicator = (TitlePageIndicator) findViewById(R.id.titles);
        mSectionHeaderTransactions = (TextView) findViewById(R.id.section_header_transactions);


		mAccountId = getIntent().getLongExtra(
                TransactionsListFragment.SELECTED_ACCOUNT_ID, -1);

        mAccountsDbAdapter = new AccountsDbAdapter(this);
		setupActionBarNavigation();

        setTitleIndicatorColor();

		if (getIntent().getAction().equals(Intent.ACTION_INSERT_OR_EDIT)) {
            mPager.setVisibility(View.GONE);
            mTitlePageIndicator.setVisibility(View.GONE);

            initializeCreateOrEditTransaction();
        } else {	//load the transactions list
            mSectionHeaderTransactions.setVisibility(View.GONE);

            mPagerAdapter = new AccountViewPagerAdapter(getSupportFragmentManager());
            mPager.setAdapter(mPagerAdapter);
            mTitlePageIndicator.setViewPager(mPager);

            mPager.setCurrentItem(INDEX_TRANSACTIONS_FRAGMENT);
		}

		// done creating, activity now running
		mActivityRunning = true;
	}

   /**
     * Loads the fragment for creating/editing transactions and initializes it to be displayed
     */
    private void initializeCreateOrEditTransaction() {
        long transactionId = getIntent().getLongExtra(NewTransactionFragment.SELECTED_TRANSACTION_ID, -1);
        Bundle args = new Bundle();
        if (transactionId > 0) {
            mSectionHeaderTransactions.setText(R.string.title_edit_transaction);
            args.putLong(NewTransactionFragment.SELECTED_TRANSACTION_ID, transactionId);
            args.putLong(TransactionsListFragment.SELECTED_ACCOUNT_ID, mAccountId);
        } else {
            mSectionHeaderTransactions.setText(R.string.title_add_transaction);
            args.putLong(TransactionsListFragment.SELECTED_ACCOUNT_ID, mAccountId);
        }
        showTransactionFormFragment(args);
    }

    /**
     * Sets the color for the ViewPager title indicator to match the account color
     */
    private void setTitleIndicatorColor() {
        String colorCode = mAccountsDbAdapter.getAccountColorCode(mAccountId);
        if (colorCode != null){
            sLastTitleColor = Color.parseColor(colorCode);
        }

        mTitlePageIndicator.setSelectedColor(sLastTitleColor);
        mTitlePageIndicator.setTextColor(sLastTitleColor);
        mTitlePageIndicator.setFooterColor(sLastTitleColor);
    }

    /**
	 * Set up action bar navigation list and listener callbacks
	 */
	private void setupActionBarNavigation() {
		// set up spinner adapter for navigation list
		Cursor accountsCursor = mAccountsDbAdapter.fetchAllRecords();
		mSpinnerAdapter = new QualifiedAccountNameCursorAdapter(getSupportActionBar().getThemedContext(),
                R.layout.sherlock_spinner_item, accountsCursor);
		((ResourceCursorAdapter) mSpinnerAdapter)
				.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(mSpinnerAdapter,
				mTransactionListNavigationListener);
        actionBar.setDisplayHomeAsUpEnabled(true);
		
		updateNavigationSelection();
	}
	
	/**
	 * Updates the action bar navigation list selection to that of the current account
	 * whose transactions are being displayed/manipulated
	 */
	public void updateNavigationSelection() {
		// set the selected item in the spinner
		int i = 0;
		Cursor accountsCursor = mAccountsDbAdapter.fetchAllRecords();
        while (accountsCursor.moveToNext()) {
            long id = accountsCursor.getLong(DatabaseAdapter.COLUMN_ROW_ID);
            if (mAccountId == id) {
                getSupportActionBar().setSelectedNavigationItem(i);
                break;
            }
            ++i;
        }
        accountsCursor.close();
	}

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);
	        FragmentManager fm = getSupportFragmentManager();
	        if (fm.getBackStackEntryCount() > 0) {
	            fm.popBackStack();
	        } else {
	        	AccountsActivity.start(this);
	        	finish();
	        }
	        return true;

            case R.id.menu_edit_account:
                Intent editAccountIntent = new Intent(this, AccountsActivity.class);
                editAccountIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
                editAccountIntent.putExtra(TransactionsListFragment.SELECTED_ACCOUNT_ID, mAccountId);
                startActivityForResult(editAccountIntent, REQUEST_EDIT_ACCOUNT);
                return true;

        default:
			return false;
		}
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED)
            return;

        refresh();
        setupActionBarNavigation();
    }

    @Override
	protected void onDestroy() {
		super.onDestroy();
		mAccountsDbAdapter.close();
	}
	
	/**
	 * Returns the database row ID of the current account
	 * @return Database row ID of the current account
	 */
	public long getCurrentAccountID(){
		return mAccountId;
	}
	
	/**
	 * Opens a fragment to create a new transaction. 
	 * Is called from the XML views
	 * @param v View which triggered this method
	 */
	public void onNewTransactionClick(View v){
		createNewTransaction(mAccountId);
	}


    /**
     * Opens a dialog fragment to create a new account which is a sub account of the current account
     * @param v View which triggered this callback
     */
    public void onNewAccountClick(View v) {
        Intent addAccountIntent = new Intent(this, AccountsActivity.class);
        addAccountIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        addAccountIntent.putExtra(AccountsListFragment.ARG_PARENT_ACCOUNT_ID, mAccountId);
        startActivityForResult(addAccountIntent, REQUEST_EDIT_ACCOUNT);
    }

	/**
	 * Loads the transaction insert/edit fragment and passes the arguments
	 * @param args Bundle arguments to be passed to the fragment
	 */
	private void showTransactionFormFragment(Bundle args){
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager
				.beginTransaction();
				
		NewTransactionFragment newTransactionFragment = new NewTransactionFragment();	
		newTransactionFragment.setArguments(args);

		fragmentTransaction.add(R.id.fragment_container,
				newTransactionFragment, TransactionsActivity.FRAGMENT_NEW_TRANSACTION);
		
		if (mActivityRunning)
			fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}
	
	@Override
	public void createNewTransaction(long accountRowId) {
        Intent createTransactionIntent = new Intent(this.getApplicationContext(), TransactionsActivity.class);
        createTransactionIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        createTransactionIntent.putExtra(TransactionsListFragment.SELECTED_ACCOUNT_ID, accountRowId);
        startActivity(createTransactionIntent);
	}

	@Override
	public void editTransaction(long transactionId){
        Intent createTransactionIntent = new Intent(this.getApplicationContext(), TransactionsActivity.class);
        createTransactionIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        createTransactionIntent.putExtra(TransactionsListFragment.SELECTED_ACCOUNT_ID, mAccountId);
        createTransactionIntent.putExtra(NewTransactionFragment.SELECTED_TRANSACTION_ID, transactionId);
        startActivity(createTransactionIntent);
	}

    @Override
    public void accountSelected(long accountRowId) {
        Intent restartIntent = new Intent(this.getApplicationContext(), TransactionsActivity.class);
        restartIntent.setAction(Intent.ACTION_VIEW);
        restartIntent.putExtra(TransactionsListFragment.SELECTED_ACCOUNT_ID, accountRowId);
        startActivity(restartIntent);
    }
}
