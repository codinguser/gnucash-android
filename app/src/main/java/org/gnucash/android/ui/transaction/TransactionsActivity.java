/*
 * Copyright (c) 2012 - 2015 Ngewi Fet <ngewif@gmail.com>
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
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.common.FormActivity;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.account.AccountsListFragment;
import org.gnucash.android.ui.passcode.PassLockActivity;
import org.gnucash.android.ui.util.AccountBalanceTask;
import org.gnucash.android.ui.util.OnAccountClickedListener;
import org.gnucash.android.ui.util.OnTransactionClickedListener;
import org.gnucash.android.ui.util.Refreshable;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import java.math.BigDecimal;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Activity for displaying, creating and editing transactions
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class TransactionsActivity extends PassLockActivity implements
        Refreshable, OnAccountClickedListener, OnTransactionClickedListener{

	/**
	 * Logging tag
	 */
	protected static final String TAG = "TransactionsActivity";

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
     * GUID of {@link Account} whose transactions are displayed
     */
    private String mAccountUID = null;

    /**
     * Account database adapter for manipulating the accounts list in navigation
     */
    private AccountsDbAdapter mAccountsDbAdapter;

    /**
     * Hold the accounts cursor that will be used in the Navigation
     */
    private Cursor mAccountsCursor = null;

    @Bind(R.id.pager) ViewPager mViewPager;
    @Bind(R.id.spinner_toolbar) Spinner mToolbarSpinner;
    @Bind(R.id.tab_layout) TabLayout mTabLayout;
    @Bind(R.id.transactions_sum) TextView mSumTextView;
    @Bind(R.id.fab_create_transaction) FloatingActionButton mCreateFloatingButton;

    private SparseArray<Refreshable> mFragmentPageReferenceMap = new SparseArray<>();

    /**
     * Flag for determining is the currently displayed account is a placeholder account or not.
     * This will determine if the transactions tab is displayed or not
     */
    private boolean mIsPlaceholderAccount;

	private AdapterView.OnItemSelectedListener mTransactionListNavigationListener = new AdapterView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mAccountUID = mAccountsDbAdapter.getUID(id);
            mIsPlaceholderAccount = mAccountsDbAdapter.isPlaceholderAccount(mAccountUID);
            if (mIsPlaceholderAccount){
                if (mTabLayout.getTabCount() > 1) {
                    mTabLayout.removeTabAt(1);
                    mPagerAdapter.notifyDataSetChanged();
                }
            } else {
                if (mTabLayout.getTabCount() < 2) {
                    mTabLayout.addTab(mTabLayout.newTab().setText(R.string.section_header_transactions));
                    mPagerAdapter.notifyDataSetChanged();
                }
            }
            //refresh any fragments in the tab with the new account UID
            refresh();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            //nothing to see here, move along
        }
	};

    private PagerAdapter mPagerAdapter;



    /**
     * Adapter for managing the sub-account and transaction fragment pages in the accounts view
     */
    private class AccountViewPagerAdapter extends FragmentStatePagerAdapter {

        public AccountViewPagerAdapter(FragmentManager fm){
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            if (mIsPlaceholderAccount){
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
            if (mIsPlaceholderAccount)
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
            if (mIsPlaceholderAccount)
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
     * Refreshes the fragments currently in the transactions activity
     */
    @Override
    public void refresh(String accountUID) {
        for (int i = 0; i < mFragmentPageReferenceMap.size(); i++) {
            mFragmentPageReferenceMap.valueAt(i).refresh(accountUID);
        }

        if (mPagerAdapter != null)
            mPagerAdapter.notifyDataSetChanged();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // make sure the account balance task is truely multi-thread
            new AccountBalanceTask(mSumTextView).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mAccountUID);
        } else {
            new AccountBalanceTask(mSumTextView).execute(mAccountUID);
        }
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
        setUpDrawer();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        ButterKnife.bind(this);

		mAccountUID = getIntent().getStringExtra(UxArgument.SELECTED_ACCOUNT_UID);
        mAccountsDbAdapter = AccountsDbAdapter.getInstance();

        mIsPlaceholderAccount = mAccountsDbAdapter.isPlaceholderAccount(mAccountUID);

        mTabLayout.addTab(mTabLayout.newTab().setText(R.string.section_header_subaccounts));
        if (!mIsPlaceholderAccount) {
            mTabLayout.addTab(mTabLayout.newTab().setText(R.string.section_header_transactions), true);
        }

        setupActionBarNavigation();

        mPagerAdapter = new AccountViewPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);

        mViewPager.setCurrentItem(INDEX_TRANSACTIONS_FRAGMENT);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));
        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        mCreateFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mViewPager.getCurrentItem()) {
                    case INDEX_SUB_ACCOUNTS_FRAGMENT:
                        Intent addAccountIntent = new Intent(TransactionsActivity.this, FormActivity.class);
                        addAccountIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
                        addAccountIntent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.ACCOUNT.name());
                        addAccountIntent.putExtra(UxArgument.PARENT_ACCOUNT_UID, mAccountUID);
                        startActivityForResult(addAccountIntent, AccountsActivity.REQUEST_EDIT_ACCOUNT);
                        ;
                        break;

                    case INDEX_TRANSACTIONS_FRAGMENT:
                        createNewTransaction(mAccountUID);
                        break;

                }
            }
        });
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
        int iColor = AccountsDbAdapter.getActiveAccountColorResource(mAccountUID);

        mTabLayout.setBackgroundColor(iColor);

        if (getSupportActionBar() != null)
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(iColor));

        if (Build.VERSION.SDK_INT > 20)
            getWindow().setStatusBarColor(GnuCashApplication.darken(iColor));
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
                getSupportActionBar().getThemedContext(), mAccountsCursor);

        mToolbarSpinner.setAdapter(mSpinnerAdapter);
        mToolbarSpinner.setOnItemSelectedListener(mTransactionListNavigationListener);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
                mToolbarSpinner.setSelection(i);
                break;
            }
            ++i;
        }
        accountsCursor.close();
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem favoriteAccountMenuItem = menu.findItem(R.id.menu_favorite_account);

        if (favoriteAccountMenuItem == null) //when the activity is used to edit a transaction
            return super.onPrepareOptionsMenu(menu);

        boolean isFavoriteAccount = AccountsDbAdapter.getInstance().isFavoriteAccount(mAccountUID);

        int favoriteIcon = isFavoriteAccount ? R.drawable.ic_star_white_24dp : R.drawable.ic_star_border_white_24dp;
        favoriteAccountMenuItem.setIcon(favoriteIcon);
        return super.onPrepareOptionsMenu(menu);

    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
            case android.R.id.home:
                return super.onOptionsItemSelected(item);

            case R.id.menu_favorite_account:
                AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();
                long accountId = accountsDbAdapter.getID(mAccountUID);
                boolean isFavorite = accountsDbAdapter.isFavoriteAccount(mAccountUID);
                //toggle favorite preference
                accountsDbAdapter.updateAccount(accountId, DatabaseSchema.AccountEntry.COLUMN_FAVORITE, isFavorite ? "0" : "1");
                supportInvalidateOptionsMenu();
                return true;

            case R.id.menu_edit_account:
                Intent editAccountIntent = new Intent(this, FormActivity.class);
                editAccountIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
                editAccountIntent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, mAccountUID);
                editAccountIntent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.ACCOUNT.name());
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
        super.onActivityResult(requestCode, resultCode, data);
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
        if (balance.asBigDecimal().compareTo(BigDecimal.ZERO) == 0)
            fontColor = context.getResources().getColor(android.R.color.black);
        balanceTextView.setTextColor(fontColor);
    }

	@Override
	public void createNewTransaction(String accountUID) {
        Intent createTransactionIntent = new Intent(this.getApplicationContext(), FormActivity.class);
        createTransactionIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        createTransactionIntent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, accountUID);
        createTransactionIntent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.TRANSACTION.name());
        startActivity(createTransactionIntent);
	}

	@Override
	public void editTransaction(String transactionUID){
        Intent createTransactionIntent = new Intent(this.getApplicationContext(), FormActivity.class);
        createTransactionIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        createTransactionIntent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, mAccountUID);
        createTransactionIntent.putExtra(UxArgument.SELECTED_TRANSACTION_UID, transactionUID);
        createTransactionIntent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.TRANSACTION.name());
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
