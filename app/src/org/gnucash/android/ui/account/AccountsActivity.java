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

package org.gnucash.android.ui.account;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.viewpagerindicator.TitlePageIndicator;
import org.gnucash.android.R;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.util.Refreshable;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.settings.SettingsActivity;
import org.gnucash.android.ui.transaction.ScheduledTransactionsListFragment;
import org.gnucash.android.ui.transaction.TransactionsActivity;
import org.gnucash.android.util.GnucashAccountXmlHandler;
import org.gnucash.android.ui.util.OnAccountClickedListener;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manages actions related to accounts, displaying, exporting and creating new accounts
 * The various actions are implemented as Fragments which are then added to this activity
 * @author Ngewi Fet <ngewif@gmail.com>
 * 
 */
public class AccountsActivity extends SherlockFragmentActivity implements OnAccountClickedListener {

	/**
	 * Tag used for identifying the account list fragment when it is added to this activity
	 */
	public static final String FRAGMENT_ACCOUNTS_LIST 	= "accounts_list_fragment";

    /**
     * Request code for GnuCash account structure file to import
     */
    public static final int REQUEST_PICK_ACCOUNTS_FILE = 0x1;

    /**
     * Request code for opening the account to edit
     */
    public static final int REQUEST_EDIT_ACCOUNT = 0x10;

    /**
	 * Tag used for identifying the account export fragment
	 */
	protected static final String FRAGMENT_EXPORT_OFX  = "export_ofx";

	/**
	 * Tag for identifying the "New account" fragment
	 */
	protected static final String FRAGMENT_NEW_ACCOUNT = "new_account_dialog";

	/**
	 * Logging tag
	 */
	protected static final String LOG_TAG = "AccountsActivity";

    /**
     * Intent action for viewing recurring transactions
     */
    public static final String ACTION_VIEW_RECURRING = "org.gnucash.android.action.VEIW_RECURRING";

    /**
     * Number of pages to show
     */
    private static final int DEFAULT_NUM_PAGES = 3;

    /**
     * Index for the recent accounts tab
     */
    public static final int INDEX_RECENT_ACCOUNTS_FRAGMENT = 0;

    /**
     * Index of the top level (all) accounts tab
     */
    public static final int INDEX_TOP_LEVEL_ACCOUNTS_FRAGMENT = 1;

    /**
     * Index of the favorite accounts tab
     */
    public static final int INDEX_FAVORITE_ACCOUNTS_FRAGMENT = 2;

    /**
     * Used to save the index of the last open tab and restore the pager to that index
     */
    public static final String LAST_OPEN_TAB_INDEX = "last_open_tab";

    /**
     * Map containing fragments for the different tabs
     */
    private SparseArray<Refreshable> mFragmentPageReferenceMap = new SparseArray<Refreshable>();

    /**
     * ViewPager which manages the different tabs
     */
    private ViewPager mPager;

	/**
	 * Dialog which is shown to the user on first start prompting the user to create some accounts
	 */
	private AlertDialog mDefaultAccountsDialog;


    /**
     * Adapter for managing the sub-account and transaction fragment pages in the accounts view
     */
    private class AccountViewPagerAdapter extends FragmentStatePagerAdapter {

        public AccountViewPagerAdapter(FragmentManager fm){
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            AccountsListFragment currentFragment;
            switch (i){
                case INDEX_RECENT_ACCOUNTS_FRAGMENT:
                    currentFragment = AccountsListFragment.newInstance(AccountsListFragment.DisplayMode.RECENT);
                    break;

                case INDEX_FAVORITE_ACCOUNTS_FRAGMENT:
                    currentFragment = AccountsListFragment.newInstance(AccountsListFragment.DisplayMode.FAVORITES);
                    break;

                case INDEX_TOP_LEVEL_ACCOUNTS_FRAGMENT:
                default:
                    currentFragment = AccountsListFragment.newInstance(AccountsListFragment.DisplayMode.TOP_LEVEL);
                    break;
            }

            mFragmentPageReferenceMap.put(i, currentFragment);
            return currentFragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            mFragmentPageReferenceMap.remove(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position){
                case INDEX_RECENT_ACCOUNTS_FRAGMENT:
                    return getString(R.string.title_recent_accounts);

                case INDEX_FAVORITE_ACCOUNTS_FRAGMENT:
                    return getString(R.string.title_favorite_accounts);

                case INDEX_TOP_LEVEL_ACCOUNTS_FRAGMENT:
                default:
                    return getString(R.string.title_all_accounts);
            }
        }

        @Override
        public int getCount() {
            return DEFAULT_NUM_PAGES;
        }
    }


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_accounts);

        init();

        mPager = (ViewPager) findViewById(R.id.pager);
        TitlePageIndicator titlePageIndicator = (TitlePageIndicator) findViewById(R.id.titles);

        final Intent intent = getIntent();
        String action = intent.getAction();
        if (action != null && action.equals(Intent.ACTION_INSERT_OR_EDIT)) {
            //enter account creation/edit mode if that was specified
            mPager.setVisibility(View.GONE);
            titlePageIndicator.setVisibility(View.GONE);

            long accountId = intent.getLongExtra(UxArgument.SELECTED_ACCOUNT_ID, 0L);
            if (accountId > 0)
                showEditAccountFragment(accountId);
            else {
                long parentAccountId = intent.getLongExtra(UxArgument.PARENT_ACCOUNT_ID, 0L);
                showAddAccountFragment(parentAccountId);
            }
        } else if (action != null && action.equals(ACTION_VIEW_RECURRING)) {
            mPager.setVisibility(View.GONE);
            titlePageIndicator.setVisibility(View.GONE);
            showRecurringTransactionsFragment();
        } else {
            //show the simple accounts list
            PagerAdapter mPagerAdapter = new AccountViewPagerAdapter(getSupportFragmentManager());
            mPager.setAdapter(mPagerAdapter);
            titlePageIndicator.setViewPager(mPager);

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            int lastTabIndex = preferences.getInt(LAST_OPEN_TAB_INDEX, INDEX_TOP_LEVEL_ACCOUNTS_FRAGMENT);
            mPager.setCurrentItem(lastTabIndex);
        }

	}

    /**
     * Loads default setting for currency and performs app first-run initialization
     */
    private void init() {
        PreferenceManager.setDefaultValues(this, R.xml.fragment_transaction_preferences, false);

        Locale locale = Locale.getDefault();
        //sometimes the locale en_UK is returned which causes a crash with Currency
        if (locale.getCountry().equals("UK")) {
            locale = new Locale(locale.getLanguage(), "GB");
        }

        String currencyCode;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        try { //there are some strange locales out there
            currencyCode = prefs.getString(getString(R.string.key_default_currency),
                    Currency.getInstance(locale).getCurrencyCode());
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            currencyCode = "USD";
        }

        Money.DEFAULT_CURRENCY_CODE = currencyCode;

        boolean firstRun = prefs.getBoolean(getString(R.string.key_first_run), true);
        if (firstRun){
            createDefaultAccounts();
        }

        if (hasNewFeatures()){
            showWhatsNewDialog(this);
        }

    }

     @Override
    protected void onResume() {
        super.onResume();
        TransactionsActivity.sLastTitleColor = -1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putInt(LAST_OPEN_TAB_INDEX, mPager.getCurrentItem()).commit();
    }

    /**
	 * Checks if the minor version has been increased and displays the What's New dialog box.
	 * This is the minor version as per semantic versioning.
	 * @return <code>true</code> if the minor version has been increased, <code>false</code> otherwise.
	 */
	private boolean hasNewFeatures(){
        String versionName = getResources().getString(R.string.app_version_name);
        int end = versionName.indexOf('.');
        int currentMinor = Integer.parseInt(versionName.substring(0, end));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int previousMinor = prefs.getInt(getString(R.string.key_previous_minor_version), 0);
        if (currentMinor > previousMinor){
            Editor editor = prefs.edit();
            editor.putInt(getString(R.string.key_previous_minor_version), currentMinor);
            editor.commit();
            return true;
        }
        return false;
	}
	
	/**
	 * Show dialog with new features for this version
	 */
	public static void showWhatsNewDialog(Context context){
        Resources resources = context.getResources();
        StringBuilder releaseTitle = new StringBuilder(resources.getString(R.string.title_whats_new));
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            releaseTitle.append(" - v").append(packageInfo.versionName);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        new AlertDialog.Builder(context)
		.setTitle(releaseTitle.toString())
		.setMessage(R.string.whats_new)
		.setPositiveButton(R.string.label_dismiss, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		}).show();
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
            case R.id.menu_recurring_transactions:
                Intent intent = new Intent(this, AccountsActivity.class);
                intent.setAction(ACTION_VIEW_RECURRING);
                startActivity(intent);
                return true;

            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

		default:
			return false;
		}
	}

    /**
     * Creates an intent which can be used start activity for creating new account
     * @return Intent which can be used to start activity for creating new account
     */
    private Intent createNewAccountIntent(){
        Intent addAccountIntent = new Intent(this, AccountsActivity.class);
        addAccountIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        return addAccountIntent;
    }

    /**
     * Shows form fragment for creating a new account
     * @param parentAccountId Record ID of the parent account present. Can be 0 for top-level account
     */
    private void showAddAccountFragment(long parentAccountId){
        Bundle args = new Bundle();
        args.putLong(UxArgument.PARENT_ACCOUNT_ID, parentAccountId);
        showAccountFormFragment(args);
    }

    /**
     * Launches the fragment which lists the recurring transactions in the database
     */
    private void showRecurringTransactionsFragment(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager
                .beginTransaction();

        ScheduledTransactionsListFragment recurringTransactionsFragment = new ScheduledTransactionsListFragment();

        fragmentTransaction.replace(R.id.fragment_container,
                recurringTransactionsFragment, "fragment_recurring_transactions");

        fragmentTransaction.commit();
    }
    /**
     * Shows the form fragment for editing the account with record ID <code>accountId</code>
     * @param accountId Record ID of the account to be edited
     */
    private void showEditAccountFragment(long accountId) {
        Bundle args = new Bundle();
        args.putLong(UxArgument.SELECTED_ACCOUNT_ID, accountId);
        showAccountFormFragment(args);
    }

    /**
     * Shows the form for creating/editing accounts
     * @param args Arguments to use for initializing the form.
     *             This could be an account to edit or a preset for the parent account
     */
    private void showAccountFormFragment(Bundle args){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager
                .beginTransaction();

        AccountFormFragment accountFormFragment = AccountFormFragment.newInstance(null);
        accountFormFragment.setArguments(args);

        fragmentTransaction.replace(R.id.fragment_container,
                accountFormFragment, AccountsActivity.FRAGMENT_NEW_ACCOUNT);

        fragmentTransaction.commit();
    }

	/**
	 * Opens a dialog fragment to create a new account
	 * @param v View which triggered this callback
	 */
	public void onNewAccountClick(View v) {
        startActivity(createNewAccountIntent());
	}

	/**
	 * Shows the user dialog to create default account structure or import existing account structure
	 */
	private void createDefaultAccounts(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.title_default_accounts);
        builder.setMessage(R.string.msg_confirm_create_default_accounts_first_run);

		builder.setPositiveButton(R.string.btn_create_accounts, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
                InputStream accountFileInputStream = getResources().openRawResource(R.raw.default_accounts);
                new AccountsActivity.AccountImporterTask(AccountsActivity.this).execute(accountFileInputStream);
                removeFirstRunFlag();
			}
		});
		
		builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mDefaultAccountsDialog.dismiss();
				removeFirstRunFlag();
			}
		});

        builder.setNeutralButton(R.string.btn_import_accounts, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                importAccounts();
                removeFirstRunFlag();
            }
        });

		mDefaultAccountsDialog = builder.create();
		mDefaultAccountsDialog.show();
	}

    /**
     * Starts Intent chooser for selecting a GnuCash accounts file to import.
     * The accounts are actually imported in onActivityResult
     */
    public void importAccounts() {
        Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT);
        pickIntent.setType("application/octet-stream");
        Intent chooser = Intent.createChooser(pickIntent, "Select GnuCash account file");

        startActivityForResult(chooser, REQUEST_PICK_ACCOUNTS_FILE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED){
            return;
        }

        switch (requestCode){
            case REQUEST_PICK_ACCOUNTS_FILE:
                try {
                    InputStream accountInputStream = getContentResolver().openInputStream(data.getData());
                    new AccountImporterTask(this).execute(accountInputStream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    /**
     * Starts the AccountsActivity and clears the activity stack
     * @param context Application context
     */
    public static void start(Context context){
        Intent accountsActivityIntent = new Intent(context, AccountsActivity.class);
        accountsActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        accountsActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(accountsActivityIntent);
    }

    @Override
	public void accountSelected(long accountRowId) {
		Intent intent = new Intent(this, TransactionsActivity.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.putExtra(UxArgument.SELECTED_ACCOUNT_ID, accountRowId);

		startActivity(intent);
	}
	
	/**
	 * Removes the flag indicating that the app is being run for the first time. 
	 * This is called every time the app is started because the next time won't be the first time
	 */
	private void removeFirstRunFlag(){
		Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.putBoolean(getString(R.string.key_first_run), false);
		editor.commit();
	}

    /**
     * Imports a GnuCash (desktop) account file and displays a progress dialog.
     * The AccountsActivity is opened when importing is done.
     */
    public static class AccountImporterTask extends AsyncTask<InputStream, Void, Boolean>{
        private final Context context;
        private ProgressDialog progressDialog;

        public AccountImporterTask(Context context){
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setTitle(R.string.title_progress_importing_accounts);
            progressDialog.setIndeterminate(true);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(InputStream... inputStreams) {
            try {
                GnucashAccountXmlHandler.parse(context, inputStreams[0]);
            } catch (Exception exception){
                exception.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean importSuccess) {
            progressDialog.dismiss();

            int message = importSuccess ? R.string.toast_success_importing_accounts : R.string.toast_error_importing_accounts;
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();

            AccountsActivity.start(context);
        }
    }
}