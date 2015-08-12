/*
 * Copyright (c) 2015 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.ui;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.ui.account.AccountFormFragment;
import org.gnucash.android.ui.export.ExportFormFragment;
import org.gnucash.android.ui.transaction.TransactionFormFragment;

/**
 * Activity for displaying forms
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class FormActivity extends AppCompatActivity {

    private String mAccountUID;

    public enum FormType {ACCOUNT_FORM, TRANSACTION_FORM, EXPORT_FORM}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_transaction_info);
        setSupportActionBar(toolbar);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        assert(actionBar != null);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);

        final Intent intent = getIntent();
        String formtypeString = intent.getStringExtra(UxArgument.FORM_TYPE);
        FormType formType = FormType.valueOf(formtypeString);

        mAccountUID = intent.getStringExtra(UxArgument.SELECTED_ACCOUNT_UID);
        if (mAccountUID == null){
            mAccountUID = intent.getStringExtra(UxArgument.PARENT_ACCOUNT_UID);
        }
        if (mAccountUID != null) {
            int colorCode = AccountsDbAdapter.getActiveAccountColorResource(mAccountUID);
            actionBar.setBackgroundDrawable(new ColorDrawable(colorCode));
            if (Build.VERSION.SDK_INT > 20)
                getWindow().setStatusBarColor(GnuCashApplication.darken(colorCode));
        }
        switch (formType){
            case ACCOUNT_FORM:
                showAccountFormFragment(intent.getExtras());
                break;

            case TRANSACTION_FORM:
                showTransactionFormFragment(intent.getExtras());
                break;

            case EXPORT_FORM:
                showExportFormFragment(null);
                break;

            default:
                throw new IllegalArgumentException("No form display type specified");
        }


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Return the GUID of the account for which the form is displayed.
     * If the form is a transaction form, the transaction is created within that account. If it is
     * an account form, then the GUID is the parent account
     * @return GUID of account
     */
    public String getCurrentAccountUID() {
        return mAccountUID;
    }

    /**
     * Shows the form for creating/editing accounts
     * @param args Arguments to use for initializing the form.
     *             This could be an account to edit or a preset for the parent account
     */
    private void showAccountFormFragment(Bundle args){
        AccountFormFragment accountFormFragment = AccountFormFragment.newInstance();
        accountFormFragment.setArguments(args);
        showFormFragment(accountFormFragment);
    }

    /**
     * Loads the transaction insert/edit fragment and passes the arguments
     * @param args Bundle arguments to be passed to the fragment
     */
    private void showTransactionFormFragment(Bundle args){
        TransactionFormFragment transactionFormFragment = new TransactionFormFragment();
        transactionFormFragment.setArguments(args);
        showFormFragment(transactionFormFragment);
    }

    /**
     * Loads the export form fragment and passes the arguments
     * @param args Bundle arguments
     */
    private void showExportFormFragment(Bundle args){
        ExportFormFragment exportFragment = new ExportFormFragment();
        showFormFragment(exportFragment);
    }

    /**
     * Loads the fragment into the fragment container, replacing whatever was there before
     * @param fragment Fragment to be displayed
     */
    private void showFormFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager
                .beginTransaction();

        fragmentTransaction.add(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }
}
