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

package org.gnucash.android.ui.account;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.viewpagerindicator.TitlePageIndicator;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.export.ExportDialogFragment;
import org.gnucash.android.importer.ImportAsyncTask;
import org.gnucash.android.model.Money;
import org.gnucash.android.service.SchedulerService;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.chart.ChartReportActivity;
import org.gnucash.android.ui.passcode.PassLockActivity;
import org.gnucash.android.ui.settings.SettingsActivity;
import org.gnucash.android.ui.transaction.ScheduledActionsActivity;
import org.gnucash.android.ui.transaction.TransactionsActivity;
import org.gnucash.android.ui.util.OnAccountClickedListener;
import org.gnucash.android.ui.util.Refreshable;
import org.gnucash.android.ui.util.TaskDelegate;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

/**
 * Manages actions related to accounts, displaying, exporting and creating new accounts
 * The various actions are implemented as Fragments which are then added to this activity
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class AccountsActivity extends PassLockActivity implements OnAccountClickedListener {

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
	public static final String FRAGMENT_EXPORT_DIALOG = "export_fragment";

	/**
	 * Tag for identifying the "New account" fragment
	 */
	protected static final String FRAGMENT_NEW_ACCOUNT = "new_account_dialog";

	/**
	 * Logging tag
	 */
	protected static final String LOG_TAG = "AccountsActivity";

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
     * Key for putting argument for tab into bundle arguments
     */
    public static final String EXTRA_TAB_INDEX = "org.gnucash.android.extra.TAB_INDEX";

    /**
     * Map containing fragments for the different tabs
     */
    private SparseArray<Refreshable> mFragmentPageReferenceMap = new SparseArray<>();

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

    public AccountsListFragment getCurrentAccountListFragment(){
        int index = mPager.getCurrentItem();
        return (AccountsListFragment)(mFragmentPageReferenceMap.get(index));
    }


	@Override
	public void onCreate(Bundle savedInstanceState) {
        //it is necessary to set the view first before calling super because of the nav drawer in BaseDrawerActivity
        setContentView(R.layout.activity_accounts);
        super.onCreate(savedInstanceState);

        init();

        mPager = (ViewPager) findViewById(R.id.pager);
        TitlePageIndicator titlePageIndicator = (TitlePageIndicator) findViewById(R.id.titles);

        final Intent intent = getIntent();
        String action = intent.getAction();
        if (action != null && action.equals(Intent.ACTION_INSERT_OR_EDIT)) {
            //enter account creation/edit mode if that was specified
            mPager.setVisibility(View.GONE);
            titlePageIndicator.setVisibility(View.GONE);

            String accountUID = intent.getStringExtra(UxArgument.SELECTED_ACCOUNT_UID);
            if (accountUID != null)
                showEditAccountFragment(accountUID);
            else {
                String parentAccountUID = intent.getStringExtra(UxArgument.PARENT_ACCOUNT_UID);
                showAddAccountFragment(parentAccountUID);
            }
        } else {
            //show the simple accounts list
            PagerAdapter mPagerAdapter = new AccountViewPagerAdapter(getSupportFragmentManager());
            mPager.setAdapter(mPagerAdapter);
            titlePageIndicator.setViewPager(mPager);

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            int lastTabIndex = preferences.getInt(LAST_OPEN_TAB_INDEX, INDEX_TOP_LEVEL_ACCOUNTS_FRAGMENT);
            int index = intent.getIntExtra(EXTRA_TAB_INDEX, lastTabIndex);
            mPager.setCurrentItem(index);
        }

	}

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        int index = intent.getIntExtra(EXTRA_TAB_INDEX, INDEX_TOP_LEVEL_ACCOUNTS_FRAGMENT);
        setTab(index);
    }

    /**
     * Sets the current tab in the ViewPager
     * @param index Index of fragment to be loaded
     */
    public void setTab(int index){
        mPager.setCurrentItem(index);
    }

    /**
     * Loads default setting for currency and performs app first-run initialization.
     * <p>Also handles displaying the What's New dialog</p>
     */
    private void init() {
        PreferenceManager.setDefaultValues(this, R.xml.fragment_transaction_preferences, false);

        Money.DEFAULT_CURRENCY_CODE = GnuCashApplication.getDefaultCurrency();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean firstRun = prefs.getBoolean(getString(R.string.key_first_run), true);
        if (firstRun){
            showFirstRunDialog();
            //default to using double entry and save the preference explicitly
            prefs.edit().putBoolean(getString(R.string.key_use_double_entry), true).apply();
        }

        if (hasNewFeatures()){
            showWhatsNewDialog(this);
        }
        GnuCashApplication.startScheduledActionExecutionService(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putInt(LAST_OPEN_TAB_INDEX, mPager.getCurrentItem()).apply();
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
            editor.apply();
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

    /**
     * Displays the dialog for exporting transactions
     */
    public static void showExportDialog(FragmentActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        FragmentTransaction ft = manager.beginTransaction();
        Fragment prev = manager.findFragmentByTag(FRAGMENT_EXPORT_DIALOG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment exportFragment = new ExportDialogFragment();
        exportFragment.show(ft, FRAGMENT_EXPORT_DIALOG);
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
                return super.onOptionsItemSelected(item);

            case R.id.menu_recurring_transactions:
                Intent intent = new Intent(this, ScheduledActionsActivity.class);
                intent.putExtra(ScheduledActionsActivity.EXTRA_DISPLAY_MODE,
                        ScheduledActionsActivity.DisplayMode.TRANSACTION_ACTIONS);
                startActivity(intent);
                return true;

            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.menu_reports:
                startActivity(new Intent(this, ChartReportActivity.class));
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
     * @param parentAccountUID GUID of the parent account present. Can be 0 for top-level account
     */
    private void showAddAccountFragment(String parentAccountUID){
        Bundle args = new Bundle();
        args.putString(UxArgument.PARENT_ACCOUNT_UID, parentAccountUID);
        showAccountFormFragment(args);
    }

    /**
     * Shows the form fragment for editing the account with record ID <code>accountId</code>
     * @param accountUID GUID of the account to be edited
     */
    private void showEditAccountFragment(String accountUID) {
        Bundle args = new Bundle();
        args.putString(UxArgument.SELECTED_ACCOUNT_UID, accountUID);
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

        AccountFormFragment accountFormFragment = AccountFormFragment.newInstance();
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
	private void showFirstRunDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.title_default_accounts);
        builder.setMessage(R.string.msg_confirm_create_default_accounts_first_run);

		builder.setPositiveButton(R.string.btn_create_accounts, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                AlertDialog.Builder adb = new AlertDialog.Builder(AccountsActivity.this);
                adb.setTitle(R.string.title_choose_currency);
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                        AccountsActivity.this,
                        android.R.layout.select_dialog_singlechoice,
                        getResources().getStringArray(R.array.currency_names));

                final List<String> currencyCodes = Arrays.asList(
                        getResources().getStringArray(R.array.key_currency_codes));
                String userCurrencyCode = Currency.getInstance(Locale.getDefault()).getCurrencyCode();
                int currencyIndex = currencyCodes.indexOf(userCurrencyCode.toUpperCase());

                adb.setSingleChoiceItems(arrayAdapter, currencyIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String currency = currencyCodes.get(which);
                        PreferenceManager.getDefaultSharedPreferences(AccountsActivity.this)
                                .edit()
                                .putString(getString(R.string.key_default_currency), currency)
                                .commit();

                        createDefaultAccounts(currency, AccountsActivity.this);
                        removeFirstRunFlag();
                    }
                });
                adb.create().show();
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
        mDefaultAccountsDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mDrawerLayout.openDrawer(mDrawerList);
            }
        });
		mDefaultAccountsDialog.show();
	}

    /**
     * Creates default accounts with the specified currency code.
     * If the currency parameter is null, then locale currency will be used if available
     *
     * @param currencyCode Currency code to assign to the imported accounts
     * @param activity Activity for providing context and displaying dialogs
     */
    public static void createDefaultAccounts(final String currencyCode, final Activity activity) {
        TaskDelegate delegate = null;
        if (currencyCode != null) {
            delegate = new TaskDelegate() {
                @Override
                public void onTaskComplete() {
                    AccountsDbAdapter.getInstance().updateAllAccounts(DatabaseSchema.AccountEntry.COLUMN_CURRENCY, currencyCode);
                }
            };
        }

        InputStream accountFileInputStream = activity.getResources().openRawResource(R.raw.default_accounts);
        new ImportAsyncTask(activity, delegate).execute(accountFileInputStream);
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
	public void accountSelected(String accountUID) {
		Intent intent = new Intent(this, TransactionsActivity.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, accountUID);

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

}