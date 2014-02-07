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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import org.gnucash.android.R;
import org.gnucash.android.data.Account;
import org.gnucash.android.data.Account.AccountType;
import org.gnucash.android.data.Money;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.ui.transactions.TransactionsActivity;
import org.gnucash.android.ui.transactions.TransactionsListFragment;
import org.gnucash.android.util.GnucashAccountXmlHandler;
import org.gnucash.android.util.OnAccountClickedListener;

import java.util.ArrayList;
import java.util.Currency;
import java.util.Locale;

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
	public static final String FRAGMENT_ACCOUNTS_LIST 	= "accounts_list";
		
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
	protected static final String TAG = "AccountsActivity";	
	
	/**
	 * Stores the indices of accounts which have been selected by the user for creation from the dialog.
	 * The account names are stored as string resources and the selected indices are then used to choose which accounts to create
	 * The dialog for creating default accounts is only shown when the app is started for the first time.
	 */
	private ArrayList<Integer> mSelectedDefaultAccounts = new ArrayList<Integer>();
	
	/**
	 * Dialog which is shown to the user on first start prompting the user to create some accounts
	 */
	private AlertDialog mDefaultAccountsDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_accounts);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Locale locale = Locale.getDefault();
		//sometimes the locale en_UK is returned which causes a crash with Currency
		if (locale.getCountry().equals("UK")) {
		    locale = new Locale(locale.getLanguage(), "GB");
		}
		String currencyCode = null;
		try { //there are some strange locales out there
			currencyCode = prefs.getString(getString(R.string.key_default_currency), 
					Currency.getInstance(locale).getCurrencyCode());
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			currencyCode = "USD"; //just use USD and let the user choose
		}
					
		Money.DEFAULT_CURRENCY_CODE = currencyCode;		
		
		boolean firstRun = prefs.getBoolean(getString(R.string.key_first_run), true);
		if (firstRun){
			createDefaultAccounts();
		}

        final Intent intent = getIntent();
        String action = intent.getAction();
        if (action != null && action.equals(Intent.ACTION_INSERT_OR_EDIT)) {
            //enter account creation/edit mode if that was specified
            long accountId = intent.getLongExtra(TransactionsListFragment.SELECTED_ACCOUNT_ID, 0L);
            if (accountId > 0)
                showEditAccountFragment(accountId);
            else {
                long parentAccountId = intent.getLongExtra(AccountsListFragment.ARG_PARENT_ACCOUNT_ID, 0L);
                showAddAccountFragment(parentAccountId);
            }
        } else {
            //show the simple accounts list

            FragmentManager fragmentManager = getSupportFragmentManager();
            AccountsListFragment accountsListFragment = (AccountsListFragment) fragmentManager
                    .findFragmentByTag(FRAGMENT_ACCOUNTS_LIST);

            if (accountsListFragment == null) {
                FragmentTransaction fragmentTransaction = fragmentManager
                        .beginTransaction();
                fragmentTransaction.add(R.id.fragment_container,
                        new AccountsListFragment(), FRAGMENT_ACCOUNTS_LIST);

                fragmentTransaction.commit();
            } else
                accountsListFragment.refreshList();
        }

        if (hasNewFeatures()){
			showWhatsNewDialog();
		}
	}

	/**
	 * Checks if the minor version has been increased and displays the What's New dialog box.
	 * This is the minor version as per semantic versioning.
	 * @return <code>true</code> if the minor version has been increased, <code>false</code> otherwise.
	 */
	private boolean hasNewFeatures(){
		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			String versionName = packageInfo.versionName;			
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
		} catch (NameNotFoundException e) {
			//do not show anything in that case
			e.printStackTrace();			
		}		
		return false;
	}
	
	/**
	 * Show dialog with new features for this version
	 */
	private void showWhatsNewDialog(){
        StringBuilder releaseTitle = new StringBuilder(getResources().getString(R.string.title_whats_new));
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        releaseTitle.append(" - v").append(packageInfo.versionName);

        new AlertDialog.Builder(this)
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

    /**
     * Shows form fragment for creating a new account
     * @param parentAccountId Record ID of the parent account present. Can be 0 for top-level account
     */
    private void showAddAccountFragment(long parentAccountId){
        Bundle args = new Bundle();
        args.putLong(AccountsListFragment.ARG_PARENT_ACCOUNT_ID, parentAccountId);
        showAccountFormFragment(args);
    }

    /**
     * Shows the form fragment for editing the account with record ID <code>accountId</code>
     * @param accountId Record ID of the account to be edited
     */
    private void showEditAccountFragment(long accountId) {
        Bundle args = new Bundle();
        args.putLong(TransactionsListFragment.SELECTED_ACCOUNT_ID, accountId);
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

        AddAccountFragment newAccountFragment = AddAccountFragment.newInstance(null);
        newAccountFragment.setArguments(args);

        fragmentTransaction.replace(R.id.fragment_container,
                newAccountFragment, AccountsActivity.FRAGMENT_NEW_ACCOUNT);

        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

	/**
	 * Opens a dialog fragment to create a new account
	 * @param v View which triggered this callback
	 */
	public void onNewAccountClick(View v) {
		AccountsListFragment accountFragment = (AccountsListFragment) getSupportFragmentManager()
				.findFragmentByTag(FRAGMENT_ACCOUNTS_LIST);
		if (accountFragment != null)
			accountFragment.showAddAccountFragment(0);
        else
            showAddAccountFragment(0);
	}

	/**
	 * Creates the default accounts which have the selected by the user.
	 * The indices of the default accounts is stored in {@link #mSelectedDefaultAccounts}
	 */
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
					Account account = new Account(name);
					
					//these indices are bound to the order in which the accounts occur in strings.xml
					switch (index) {
					case 0:
						account.setAccountType(AccountType.EXPENSE);
						break;
						
					case 1:
						account.setAccountType(AccountType.INCOME);
						break;
						
					case 2:
						account.setAccountType(AccountType.ASSET);
						break;
					case 3:
						account.setAccountType(AccountType.EQUITY);
						break;
					case 4:
						account.setAccountType(AccountType.LIABILITY);
						break;
						
					default:
						account.setAccountType(AccountType.CASH);
						break;
					}
					dbAdapter.addAccount(account);
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

        startActivityForResult(chooser, AccountsListFragment.REQUEST_PICK_ACCOUNTS_FILE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED){
            return;
        }

        switch (requestCode){
            case AccountsListFragment.REQUEST_PICK_ACCOUNTS_FILE:
                new AccountImporterTask(this).execute(data.getData());
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
		intent.putExtra(TransactionsListFragment.SELECTED_ACCOUNT_ID, accountRowId);

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
    public static class AccountImporterTask extends AsyncTask<Uri, Void, Boolean>{
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
        protected Boolean doInBackground(Uri... uris) {
            try {
                GnucashAccountXmlHandler.parse(context, context.getContentResolver().openInputStream(uris[0]));
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