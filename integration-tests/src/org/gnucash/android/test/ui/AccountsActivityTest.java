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

package org.gnucash.android.test.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import com.jayway.android.robotium.solo.Solo;
import org.gnucash.android.R;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.test.util.ActionBarUtils;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.account.AccountsListFragment;
import org.gnucash.android.ui.transaction.TransactionsActivity;

import java.util.Currency;
import java.util.List;

import static org.fest.assertions.api.ANDROID.assertThat;

public class AccountsActivityTest extends ActivityInstrumentationTestCase2<AccountsActivity> {
	private static final String DUMMY_ACCOUNT_CURRENCY_CODE = "USD";
	private static final String DUMMY_ACCOUNT_NAME = "Dummy account";
    public static final String  DUMMY_ACCOUNT_UID   = "dummy-account";
	private Solo mSolo;

	public AccountsActivityTest() {
		super(AccountsActivity.class);
	}

	protected void setUp() throws Exception {
		Context context = getInstrumentation().getTargetContext();
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(context.getString(R.string.key_first_run), false);
		editor.commit();
		
		mSolo = new Solo(getInstrumentation(), getActivity());	
		
		AccountsDbAdapter adapter = new AccountsDbAdapter(getActivity());
		Account account = new Account(DUMMY_ACCOUNT_NAME);
        account.setUID(DUMMY_ACCOUNT_UID);
		account.setCurrency(Currency.getInstance(DUMMY_ACCOUNT_CURRENCY_CODE));
		adapter.addAccount(account);
		adapter.close();

        //the What's new dialog is usually displayed on first run
        String dismissDialog = getActivity().getString(R.string.label_dismiss);
        if (mSolo.waitForText(dismissDialog)){
            mSolo.clickOnText(dismissDialog);
        }
	}

	
	public void testDisplayAccountsList(){
        final int NUMBER_OF_ACCOUNTS = 15;
        AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(getActivity());
        for (int i = 0; i < NUMBER_OF_ACCOUNTS; i++) {
            Account account = new Account("Acct " + i);
            accountsDbAdapter.addAccount(account);
        }
        accountsDbAdapter.close();

        //there should exist a listview of accounts
        refreshAccountsList();
        mSolo.waitForText("Acct");

        ListView accountsListView = mSolo.getCurrentViews(ListView.class).get(0);
		assertNotNull(accountsListView);

        assertEquals(NUMBER_OF_ACCOUNTS + 1, accountsListView.getCount());
	}

    public void testSearchAccounts(){
        String SEARCH_ACCOUNT_NAME = "Search Account";

        Account account = new Account(SEARCH_ACCOUNT_NAME);
        account.setParentUID(DUMMY_ACCOUNT_UID);
        AccountsDbAdapter accountsDbAdapter = new AccountsDbAdapter(getActivity());
        accountsDbAdapter.addAccount(account);
        accountsDbAdapter.close();

        refreshAccountsList();

        //enter search query
        ActionBarUtils.clickSherlockActionBarItem(mSolo, R.id.menu_search);
        mSolo.sleep(200);
        mSolo.enterText(0, "Se");

        boolean accountFound = mSolo.waitForText(SEARCH_ACCOUNT_NAME, 1, 2000);
        assertTrue(accountFound);

        mSolo.clearEditText(0);

        //the child account should be hidden again
        accountFound = mSolo.waitForText(SEARCH_ACCOUNT_NAME, 1, 2000);
        assertFalse(accountFound);
    }

    /**
     * Tests that an account can be created successfully and that the account list is sorted alphabetically.
     */
	public void testCreateAccount(){
        mSolo.waitForFragmentByTag(AccountsActivity.FRAGMENT_ACCOUNTS_LIST);
		mSolo.clickOnActionBarItem(R.id.menu_add_account);
		mSolo.waitForText(getActivity().getString(R.string.title_add_account));

        //there already exists one eligible parent account in the system
        assertThat(getActivity().findViewById(R.id.checkbox_parent_account)).isVisible();

        EditText inputAccountName = (EditText) getActivity().findViewById(R.id.edit_text_account_name);
        String NEW_ACCOUNT_NAME = "A New Account";
//        mSolo.enterText(0, NEW_ACCOUNT_NAME);
        mSolo.enterText(inputAccountName, NEW_ACCOUNT_NAME);
        mSolo.clickOnActionBarItem(R.id.menu_save);

        mSolo.waitForText(NEW_ACCOUNT_NAME);

		ListView lv = mSolo.getCurrentViews(ListView.class).get(0);
		assertNotNull(lv);
		TextView v = (TextView) lv.getChildAt(0) //accounts are sorted alphabetically
				.findViewById(R.id.primary_text);

		assertEquals(NEW_ACCOUNT_NAME, v.getText().toString());
		AccountsDbAdapter accAdapter = new AccountsDbAdapter(getActivity());
		
		List<Account> accounts = accAdapter.getAllAccounts();
		Account newestAccount = accounts.get(0);
		
		assertEquals(NEW_ACCOUNT_NAME, newestAccount.getName());
		assertEquals(Money.DEFAULT_CURRENCY_CODE, newestAccount.getCurrency().getCurrencyCode());

		accAdapter.close();		
	}

	public void testEditAccount(){
		Fragment fragment = getActivity()
				.getSupportFragmentManager()
				.findFragmentByTag(AccountsActivity.FRAGMENT_ACCOUNTS_LIST);
		((AccountsListFragment) fragment).refresh();
		
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		
		String editedAccountName = "Edited Account";
				
		mSolo.clickLongOnText(DUMMY_ACCOUNT_NAME);

        clickSherlockActionBarItem(R.id.context_menu_edit_accounts);

        mSolo.waitForView(EditText.class);

		mSolo.clearEditText(0);
		mSolo.enterText(0, editedAccountName);

        clickSherlockActionBarItem(R.id.menu_save);

		mSolo.waitForDialogToClose(2000);
        mSolo.waitForText("Accounts");

		ListView lv = mSolo.getCurrentViews(ListView.class).get(0);
		TextView tv = (TextView) lv.getChildAt(0)
				.findViewById(R.id.primary_text);
		assertEquals(editedAccountName, tv.getText().toString());
		
		AccountsDbAdapter accAdapter = new AccountsDbAdapter(getActivity());
		
		List<Account> accounts = accAdapter.getAllAccounts();
		Account latest = accounts.get(0);  //will be the first due to alphabetical sorting
		
		assertEquals(latest.getName(), "Edited Account");
		assertEquals(DUMMY_ACCOUNT_CURRENCY_CODE, latest.getCurrency().getCurrencyCode());	
		accAdapter.close();
	}
	
	public void testDeleteAccount(){
        final String accountNameToDelete = "TO BE DELETED";
        final String accountUidToDelete = "to-be-deleted";

        Account acc = new Account(accountNameToDelete);
        acc.setUID(accountUidToDelete);

        Transaction transaction = new Transaction("5.99", "hats");
        transaction.setAccountUID(accountUidToDelete);
        acc.addTransaction(transaction);
        AccountsDbAdapter accDbAdapter = new AccountsDbAdapter(getActivity());
        accDbAdapter.addAccount(acc);

        Fragment fragment = getActivity()
                .getSupportFragmentManager()
                .findFragmentByTag(AccountsActivity.FRAGMENT_ACCOUNTS_LIST);
        assertNotNull(fragment);

        ((AccountsListFragment) fragment).refresh();

        mSolo.clickLongOnText(accountNameToDelete);

        clickSherlockActionBarItem(R.id.context_menu_delete);

        String deleteConfirm = getActivity().getString(R.string.alert_dialog_ok_delete);
        mSolo.clickOnText(deleteConfirm);

        mSolo.waitForDialogToClose(1000);
        mSolo.waitForText("Accounts");

        long id = accDbAdapter.getAccountID(accountUidToDelete);
        assertEquals(-1, id);

        TransactionsDbAdapter transDbAdapter = new TransactionsDbAdapter(getActivity());
        List<Transaction> transactions = transDbAdapter.getAllTransactionsForAccount(accountUidToDelete);

        assertEquals(0, transactions.size());

        accDbAdapter.close();
        transDbAdapter.close();
    }

	public void testDisplayTransactionsList(){
        final int TRANSACTION_COUNT = 15;
        //first create a couple of transations
        TransactionsDbAdapter transactionsDbAdapter = new TransactionsDbAdapter(getActivity());
        for (int i = 0; i < TRANSACTION_COUNT; i++) {
            Transaction transaction = new Transaction(Money.getZeroInstance(), "Transaxion " + i);
            transaction.setAccountUID(DUMMY_ACCOUNT_UID);
            transactionsDbAdapter.addTransaction(transaction);
        }
        transactionsDbAdapter.close();

		Fragment fragment = getActivity()
				.getSupportFragmentManager()
				.findFragmentByTag(AccountsActivity.FRAGMENT_ACCOUNTS_LIST);
		((AccountsListFragment) fragment).refresh();
		
		mSolo.waitForText(DUMMY_ACCOUNT_NAME);
		mSolo.clickOnText(DUMMY_ACCOUNT_NAME);
		mSolo.waitForText("Transaxion");

        mSolo.scrollDown();

		String classname = mSolo.getCurrentActivity().getComponentName().getClassName();
		assertEquals(TransactionsActivity.class.getName(), classname);
		
		fragment = ((TransactionsActivity)mSolo.getCurrentActivity())
				.getSupportFragmentManager()
				.findFragmentByTag(TransactionsActivity.FRAGMENT_TRANSACTIONS_LIST);

		assertNotNull(fragment);

        //there are two list views in the transactions activity, one for sub-accounts and another for transactions
        assertEquals(2, mSolo.getCurrentViews(ListView.class).size());
        ListView listView = mSolo.getCurrentViews(ListView.class).get(1);
        assertNotNull(listView);
        assertEquals(TRANSACTION_COUNT, listView.getCount());

	}
		
	public void testIntentAccountCreation(){
		Intent intent = new Intent(Intent.ACTION_INSERT);
		intent.putExtra(Intent.EXTRA_TITLE, "Intent Account");
		intent.putExtra(Intent.EXTRA_UID, "intent-account");
		intent.putExtra(Account.EXTRA_CURRENCY_CODE, "EUR");
		intent.setType(Account.MIME_TYPE);
		getActivity().sendBroadcast(intent);
		
		//give time for the account to be created
		synchronized (mSolo) {
			try {
				mSolo.wait(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
				
		AccountsDbAdapter dbAdapter = new AccountsDbAdapter(getActivity());
		Account account = dbAdapter.getAccount("intent-account");
		dbAdapter.close();
		assertNotNull(account);
		assertEquals("Intent Account", account.getName());
		assertEquals("intent-account", account.getUID());
		assertEquals("EUR", account.getCurrency().getCurrencyCode());
	}
	
	
	protected void tearDown() throws Exception {
		AccountsDbAdapter adapter = new AccountsDbAdapter(getActivity());
		adapter.deleteAllRecords();
		adapter.close();
		
		mSolo.finishOpenedActivities();		
		super.tearDown();
	}

    /**
     * Finds a view in the action bar and clicks it, since the native methods are not supported by ActionBarSherlock
     * @param id
     */
    private void clickSherlockActionBarItem(int id){
        View view = mSolo.getView(id);
        mSolo.clickOnView(view);
    }

    /**
     * Refresh the account list fragment
     */
    private void refreshAccountsList(){
        Fragment fragment = getActivity()
                .getSupportFragmentManager()
                .findFragmentByTag(AccountsActivity.FRAGMENT_ACCOUNTS_LIST);
        ((AccountsListFragment)fragment).refresh();
    }
}
