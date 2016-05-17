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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.kobakei.ratethisapp.RateThisApp;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.export.xml.GncXmlExporter;
import org.gnucash.android.importer.ImportAsyncTask;
import org.gnucash.android.ui.common.BaseDrawerActivity;
import org.gnucash.android.ui.common.FormActivity;
import org.gnucash.android.ui.common.Refreshable;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.transaction.TransactionsActivity;
import org.gnucash.android.ui.util.TaskDelegate;
import org.gnucash.android.ui.wizard.FirstRunWizardActivity;

import butterknife.Bind;

/**
 * Manages actions related to accounts, displaying, exporting and creating new accounts
 * The various actions are implemented as Fragments which are then added to this activity
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class AccountsActivity extends BaseDrawerActivity implements OnAccountClickedListener {

    /**
     * Request code for GnuCash account structure file to import
     */
    public static final int REQUEST_PICK_ACCOUNTS_FILE = 0x1;

    /**
     * Request code for opening the account to edit
     */
    public static final int REQUEST_EDIT_ACCOUNT = 0x10;

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
    public static final int REQUEST_PERMISSION_WRITE_SD_CARD = 0xAB;

    /**
     * Map containing fragments for the different tabs
     */
    private SparseArray<Refreshable> mFragmentPageReferenceMap = new SparseArray<>();

    /**
     * ViewPager which manages the different tabs
     */
    @Bind(R.id.pager) ViewPager mViewPager;
    @Bind(R.id.fab_create_account) FloatingActionButton mFloatingActionButton;
    @Bind(R.id.coordinatorLayout) CoordinatorLayout mCoordinatorLayout;

    /**
     * Configuration for rating the app
     */
    public static RateThisApp.Config rateAppConfig = new RateThisApp.Config(14, 100);
    private AccountViewPagerAdapter mPagerAdapter;

    /**
     * Adapter for managing the sub-account and transaction fragment pages in the accounts view
     */
    private class AccountViewPagerAdapter extends FragmentPagerAdapter {

        public AccountViewPagerAdapter(FragmentManager fm){
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            AccountsListFragment currentFragment = (AccountsListFragment) mFragmentPageReferenceMap.get(i);
            if (currentFragment == null) {
                switch (i) {
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
            }
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
        int index = mViewPager.getCurrentItem();
        Fragment fragment = (Fragment) mFragmentPageReferenceMap.get(index);
        if (fragment == null)
            fragment = mPagerAdapter.getItem(index);
        return (AccountsListFragment) fragment;
    }

    @Override
    public int getContentView() {
        return R.layout.activity_accounts;
    }

    @Override
    public int getTitleRes() {
        return R.string.title_accounts;
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        handleOpenFileIntent(intent);

        init();

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.title_recent_accounts));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.title_all_accounts));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.title_favorite_accounts));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        //show the simple accounts list
        mPagerAdapter = new AccountViewPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                //nothing to see here, move along
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                //nothing to see here, move along
            }
        });

        setCurrentTab();

        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addAccountIntent = new Intent(AccountsActivity.this, FormActivity.class);
                addAccountIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
                addAccountIntent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.ACCOUNT.name());
                startActivityForResult(addAccountIntent, AccountsActivity.REQUEST_EDIT_ACCOUNT);
            }
        });
	}

    @Override
    protected void onStart() {
        super.onStart();

        if (BuildConfig.CAN_REQUEST_RATING) {
            RateThisApp.init(rateAppConfig);
            RateThisApp.onStart(this);
            RateThisApp.showRateDialogIfNeeded(this);
        }
    }

    /**
     * Handles the case where another application has selected to open a (.gnucash or .gnca) file with this app
     * @param intent Intent containing the data to be imported
     */
    private void handleOpenFileIntent(Intent intent) {
        //when someone launches the app to view a (.gnucash or .gnca) file
        Uri data = intent.getData();
        if (data != null){
            GncXmlExporter.createBackup();
            intent.setData(null);
            new ImportAsyncTask(this).execute(data);
            removeFirstRunFlag();
        }
    }

    /**
     * Get permission for WRITING SD card
     */
    @TargetApi(23)
    private void getSDWritePermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
//                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    Snackbar.make(mCoordinatorLayout,
                            "GnuCash requires permission to access the SD card for backup and restore",
                            Snackbar.LENGTH_INDEFINITE).setAction("GRANT",
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                            Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_WRITE_SD_CARD);
                                }
                            })
                            .setActionTextColor(getResources().getColor(R.color.theme_accent))
                            .show();
//                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode){
            case REQUEST_PERMISSION_WRITE_SD_CARD:{
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    //TODO: permission was granted, yay! do the
                    // calendar task you need to do.

                } else {

                    // TODO: permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            } return;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        setCurrentTab();

        getCurrentAccountListFragment().refresh();

        handleOpenFileIntent(intent);
    }

    /**
     * Sets the current tab in the ViewPager
     */
    public void setCurrentTab(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int lastTabIndex = preferences.getInt(LAST_OPEN_TAB_INDEX, INDEX_TOP_LEVEL_ACCOUNTS_FRAGMENT);
        int index = getIntent().getIntExtra(EXTRA_TAB_INDEX, lastTabIndex);
        mViewPager.setCurrentItem(index);
    }

    /**
     * Loads default setting for currency and performs app first-run initialization.
     * <p>Also handles displaying the What's New dialog</p>
     */
    private void init() {
        PreferenceManager.setDefaultValues(this, R.xml.fragment_transaction_preferences, false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean firstRun = prefs.getBoolean(getString(R.string.key_first_run), true);

        if (firstRun){
            startActivity(new Intent(this, FirstRunWizardActivity.class));

            //default to using double entry and save the preference explicitly
            prefs.edit().putBoolean(getString(R.string.key_use_double_entry), true).apply();
        } else {
            getSDWritePermission();
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
        preferences.edit().putInt(LAST_OPEN_TAB_INDEX, mViewPager.getCurrentItem()).apply();
    }

    /**
	 * Checks if the minor version has been increased and displays the What's New dialog box.
	 * This is the minor version as per semantic versioning.
	 * @return <code>true</code> if the minor version has been increased, <code>false</code> otherwise.
	 */
	private boolean hasNewFeatures(){
        String minorVersion = getResources().getString(R.string.app_minor_version);
        int currentMinor = Integer.parseInt(minorVersion);

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
	public static AlertDialog showWhatsNewDialog(Context context){
        Resources resources = context.getResources();
        StringBuilder releaseTitle = new StringBuilder(resources.getString(R.string.title_whats_new));
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            releaseTitle.append(" - v").append(packageInfo.versionName);
        } catch (NameNotFoundException e) {
            Crashlytics.logException(e);
            Log.e(LOG_TAG, "Error displaying 'Whats new' dialog");
        }

        return new AlertDialog.Builder(context)
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
    public static void openExportFragment(AppCompatActivity activity) {
        Intent intent = new Intent(activity, FormActivity.class);
        intent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.EXPORT.name());
        activity.startActivity(intent);
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.global_actions, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
            case android.R.id.home:
                return super.onOptionsItemSelected(item);

		default:
			return false;
		}
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
                    GnuCashApplication.setDefaultCurrencyCode(currencyCode);
                }
            };
        }

        Uri uri = Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.raw.default_accounts);
        new ImportAsyncTask(activity, delegate).execute(uri);
    }

    /**
     * Starts Intent chooser for selecting a GnuCash accounts file to import.
     * <p>The {@code activity} is responsible for the actual import of the file and can do so by calling {@link #importXmlFileFromIntent(Activity, Intent, TaskDelegate)}<br>
     * The calling class should respond to the request code {@link AccountsActivity#REQUEST_PICK_ACCOUNTS_FILE} in its {@link #onActivityResult(int, int, Intent)} method</p>
     * @param activity Activity starting the request and will also handle the response
     * @see #importXmlFileFromIntent(Activity, Intent, TaskDelegate)
     */
    public static void startXmlFileChooser(Activity activity) {
        Intent pickIntent;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
//            pickIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        } else
            pickIntent = new Intent(Intent.ACTION_GET_CONTENT);

//        ArrayList<String> mimeTypes = new ArrayList<>();
//        mimeTypes.add("application/*");
//        mimeTypes.add("file/*");
//        mimeTypes.add("text/*");
//        mimeTypes.add("application/vnd.google-apps.file");
//        pickIntent.putStringArrayListExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        pickIntent.addCategory(Intent.CATEGORY_OPENABLE);
        pickIntent.setType("*/*");
        Intent chooser = Intent.createChooser(pickIntent, "Select GnuCash account file"); //todo internationalize string

        try {
            activity.startActivityForResult(chooser, REQUEST_PICK_ACCOUNTS_FILE);
        } catch (ActivityNotFoundException ex){
            Crashlytics.log("No file manager for selecting files available");
            Crashlytics.logException(ex);
            Toast.makeText(activity, R.string.toast_install_file_manager, Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Reads and XML file from an intent and imports it into the database
     * <p>This method is usually called in response to {@link AccountsActivity#startXmlFileChooser(Activity)}</p>
     * @param context Activity context
     * @param data Intent data containing the XML uri
     * @param onFinishTask Task to be executed when import is complete
     */
    public static void importXmlFileFromIntent(Activity context, Intent data, TaskDelegate onFinishTask) {
        GncXmlExporter.createBackup();
        new ImportAsyncTask(context, onFinishTask).execute(data.getData());
    }

    /**
     * Starts the AccountsActivity and clears the activity stack
     * @param context Application context
     */
    public static void start(Context context){
        Intent accountsActivityIntent = new Intent(context, AccountsActivity.class);
        accountsActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        accountsActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
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
	public static void removeFirstRunFlag(){
        Context context = GnuCashApplication.getAppContext();
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(context.getString(R.string.key_first_run), false);
		editor.commit();
	}

}