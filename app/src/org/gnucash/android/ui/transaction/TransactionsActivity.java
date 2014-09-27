/*
 * Copyright (c) 2012 - 2014 Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2014 Yongxin Wang <fefe.wyx@gmail.com>
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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.ResourceCursorAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.viewpagerindicator.TitlePageIndicator;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.account.AccountsListFragment;
import org.gnucash.android.ui.passcode.PassLockActivity;
import org.gnucash.android.ui.util.OnAccountClickedListener;
import org.gnucash.android.ui.util.OnTransactionClickedListener;
import org.gnucash.android.ui.util.Refreshable;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

/**
 * Activity for displaying, creating and editing transactions
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class TransactionsActivity extends PassLockActivity implements
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
	 * Tag for {@link TransactionFormFragment}
	 */
	public static final String FRAGMENT_NEW_TRANSACTION 	= "new_transaction";

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
     * Menu item for marking an account as a favorite
     */
    MenuItem mFavoriteAccountMenu;

    /**
	 * Database ID of {@link Account} whose transactions are displayed 
	 */
//	private long mAccountId 	= 0;

    /**
     * GUID of {@link Account} whose transactions are displayed
     */
    private String mAccountUID = null;

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
     * Hold the accounts cursor that will be used in the Navigation
     */
    private Cursor mAccountsCursor = null;

    private TextView mSectionHeaderTransactions;
    private TitlePageIndicator mTitlePageIndicator;

    private SparseArray<Refreshable> mFragmentPageReferenceMap = new SparseArray<Refreshable>();

	private OnNavigationListener mTransactionListNavigationListener = new OnNavigationListener() {

		  @Override
		  public boolean onNavigationItemSelected(int position, long itemId) {
            mAccountUID = mAccountsDbAdapter.getAccountUID(itemId);
            FragmentManager fragmentManager = getSupportFragmentManager();

		    //inform new accounts fragment that account was changed
		    TransactionFormFragment newTransactionsFragment = (TransactionFormFragment) fragmentManager
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


    /**
     * Adapter for managing the sub-account and transaction fragment pages in the accounts view
     */
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
            args.putString(UxArgument.PARENT_ACCOUNT_UID, mAccountUID);
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
            args.putString(UxArgument.SELECTED_ACCOUNT_UID, mAccountUID);
            transactionsListFragment.setArguments(args);
            Log.i(TAG, "Opening transactions for account:  " +  mAccountUID);
            return transactionsListFragment;
        }
    }

    /**
     * Returns <code>true</code> is the current account is a placeholder account, <code>false</code> otherwise.
     * @return <code>true</code> is the current account is a placeholder account, <code>false</code> otherwise.
     */
    private boolean isPlaceHolderAccount(){
        return mAccountsDbAdapter.isPlaceholderAccount(mAccountUID);
    }

    /**
     * Refreshes the fragments currently in the transactions activity
     */
    @Override
    public void refresh(String accountUID) {
        for (int i = 0; i < mFragmentPageReferenceMap.size(); i++) {
            mFragmentPageReferenceMap.valueAt(i).refresh(accountUID);
        }
        mTitlePageIndicator.notifyDataSetChanged();
    }

    @Override
    public void refresh(){
        refresh(mAccountUID);
        setTitleIndicatorColor();
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_transactions);

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        mTitlePageIndicator = (TitlePageIndicator) findViewById(R.id.titles);
        mSectionHeaderTransactions = (TextView) findViewById(R.id.section_header_transactions);

		mAccountUID = getIntent().getStringExtra(UxArgument.SELECTED_ACCOUNT_UID);

        mAccountsDbAdapter = GnuCashApplication.getAccountsDbAdapter();

        setupActionBarNavigation();

		if (getIntent().getAction().equals(Intent.ACTION_INSERT_OR_EDIT)) {
            pager.setVisibility(View.GONE);
            mTitlePageIndicator.setVisibility(View.GONE);

            initializeCreateOrEditTransaction();
        } else {	//load the transactions list
            mSectionHeaderTransactions.setVisibility(View.GONE);

            PagerAdapter pagerAdapter = new AccountViewPagerAdapter(getSupportFragmentManager());
            pager.setAdapter(pagerAdapter);
            mTitlePageIndicator.setViewPager(pager);

            pager.setCurrentItem(INDEX_TRANSACTIONS_FRAGMENT);
		}

		// done creating, activity now running
		mActivityRunning = true;
	}

    /**
     * Loads the fragment for creating/editing transactions and initializes it to be displayed
     */
    private void initializeCreateOrEditTransaction() {
        String transactionUID = getIntent().getStringExtra(UxArgument.SELECTED_TRANSACTION_UID);
        Bundle args = new Bundle();
        if (transactionUID != null) {
            mSectionHeaderTransactions.setText(R.string.title_edit_transaction);
            args.putString(UxArgument.SELECTED_TRANSACTION_UID, transactionUID);
            args.putString(UxArgument.SELECTED_ACCOUNT_UID, mAccountUID);
        } else {
            mSectionHeaderTransactions.setText(R.string.title_add_transaction);
            args.putString(UxArgument.SELECTED_ACCOUNT_UID, mAccountUID);
        }
        showTransactionFormFragment(args);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTitleIndicatorColor();
    }

    /**
     * Sets the color for the ViewPager title indicator to match the account color
     */
    private void setTitleIndicatorColor() {
        //Basically, if we are in a top level account, use the default title color.
        //but propagate a parent account's title color to children who don't have own color
        String colorCode = mAccountsDbAdapter.getAccountColorCode(mAccountsDbAdapter.getAccountID(mAccountUID));
        int iColor = -1;
        if (colorCode != null){
            iColor = Color.parseColor(colorCode);
        } else {
            String accountUID = mAccountUID;
            while ((accountUID = mAccountsDbAdapter.getParentAccountUID(accountUID)) != null) {
                colorCode = mAccountsDbAdapter.getAccountColorCode(mAccountsDbAdapter.getAccountID(accountUID));
                if (colorCode != null) {
                    iColor = Color.parseColor(colorCode);
                    break;
                }
            }
            if (colorCode == null)
            {
                iColor = getResources().getColor(R.color.title_green);
            }
        }

        mTitlePageIndicator.setSelectedColor(iColor);
        mTitlePageIndicator.setTextColor(iColor);
        mTitlePageIndicator.setFooterColor(iColor);
        mSectionHeaderTransactions.setBackgroundColor(iColor);
    }

    /**
	 * Set up action bar navigation list and listener callbacks
	 */
	private void setupActionBarNavigation() {
		// set up spinner adapter for navigation list
        if (mAccountsCursor != null) {
            mAccountsCursor.close();
        }
		mAccountsCursor = mAccountsDbAdapter.fetchAllRecordsOrderedByFullName();

        SpinnerAdapter mSpinnerAdapter = new QualifiedAccountNameCursorAdapter(
                getSupportActionBar().getThemedContext(),
                R.layout.sherlock_spinner_item, mAccountsCursor);
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
		Cursor accountsCursor = mAccountsDbAdapter.fetchAllRecordsOrderedByFullName();
        while (accountsCursor.moveToNext()) {
            String uid = accountsCursor.getString(accountsCursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_UID));
            if (mAccountUID.equals(uid)) {
                getSupportActionBar().setSelectedNavigationItem(i);
                break;
            }
            ++i;
        }
        accountsCursor.close();
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mFavoriteAccountMenu = menu.findItem(R.id.menu_favorite_account);
        MenuItem favoriteAccountMenuItem = menu.findItem(R.id.menu_favorite_account);

        if (favoriteAccountMenuItem == null) //when the activity is used to edit a transaction
            return super.onPrepareOptionsMenu(menu);

        boolean isFavoriteAccount = GnuCashApplication.getAccountsDbAdapter().isFavoriteAccount(mAccountsDbAdapter.getAccountID(mAccountUID));

        int favoriteIcon = isFavoriteAccount ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off;
        favoriteAccountMenuItem.setIcon(favoriteIcon);
        return super.onPrepareOptionsMenu(menu);

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

            case R.id.menu_favorite_account:
                AccountsDbAdapter accountsDbAdapter = GnuCashApplication.getAccountsDbAdapter();
                long accountId = accountsDbAdapter.getAccountID(mAccountUID);
                boolean isFavorite = accountsDbAdapter.isFavoriteAccount(accountId);
                //toggle favorite preference
                accountsDbAdapter.updateAccount(accountId, DatabaseSchema.AccountEntry.COLUMN_FAVORITE, isFavorite ? "0" : "1");
                supportInvalidateOptionsMenu();
                return true;

            case R.id.menu_edit_account:
                Intent editAccountIntent = new Intent(this, AccountsActivity.class);
                editAccountIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
                editAccountIntent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, mAccountUID);
                startActivityForResult(editAccountIntent, AccountsActivity.REQUEST_EDIT_ACCOUNT);
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
        mAccountsCursor.close();
	}
	
	/**
	 * Returns the global unique ID of the current account
	 * @return GUID of the current account
	 */
	public String getCurrentAccountUID(){
		return mAccountUID;
	}
	
	/**
	 * Opens a fragment to create a new transaction. 
	 * Is called from the XML views
	 * @param v View which triggered this method
	 */
	public void onNewTransactionClick(View v){
		createNewTransaction(mAccountUID);
	}


    /**
     * Opens a dialog fragment to create a new account which is a sub account of the current account
     * @param v View which triggered this callback
     */
    public void onNewAccountClick(View v) {
        Intent addAccountIntent = new Intent(this, AccountsActivity.class);
        addAccountIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        addAccountIntent.putExtra(UxArgument.PARENT_ACCOUNT_UID, mAccountUID);
        startActivityForResult(addAccountIntent, AccountsActivity.REQUEST_EDIT_ACCOUNT);
    }

	/**
	 * Loads the transaction insert/edit fragment and passes the arguments
	 * @param args Bundle arguments to be passed to the fragment
	 */
	private void showTransactionFormFragment(Bundle args){
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager
				.beginTransaction();
				
		TransactionFormFragment transactionFormFragment = new TransactionFormFragment();
		transactionFormFragment.setArguments(args);

		fragmentTransaction.add(R.id.fragment_container,
                transactionFormFragment, TransactionsActivity.FRAGMENT_NEW_TRANSACTION);
		
		if (mActivityRunning)
			fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}

    /**
     * Display the balance of a transaction in a text view and format the text color to match the sign of the amount
     * @param balanceTextView {@link android.widget.TextView} where balance is to be displayed
     * @param balance {@link org.gnucash.android.model.Money} balance to display
     */
    public static void displayBalance(TextView balanceTextView, Money balance){
        balanceTextView.setText(balance.formattedString());
        Context context = GnuCashApplication.getAppContext();
        int fontColor = balance.isNegative() ?
                context.getResources().getColor(R.color.debit_red) :
                context.getResources().getColor(R.color.credit_green);
        balanceTextView.setTextColor(fontColor);
    }

	@Override
	public void createNewTransaction(String accountUID) {
        Intent createTransactionIntent = new Intent(this.getApplicationContext(), TransactionsActivity.class);
        createTransactionIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        createTransactionIntent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, accountUID);
        startActivity(createTransactionIntent);
	}

	@Override
	public void editTransaction(String transactionUID){
        Intent createTransactionIntent = new Intent(this.getApplicationContext(), TransactionsActivity.class);
        createTransactionIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        createTransactionIntent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, mAccountUID);
        createTransactionIntent.putExtra(UxArgument.SELECTED_TRANSACTION_UID, transactionUID);
        startActivity(createTransactionIntent);
	}

    @Override
    public void accountSelected(String accountUID) {
        Intent restartIntent = new Intent(this.getApplicationContext(), TransactionsActivity.class);
        restartIntent.setAction(Intent.ACTION_VIEW);
        restartIntent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, accountUID);
        startActivity(restartIntent);
    }
}
