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

package org.gnucash.android.ui.common;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.ui.account.AccountFormFragment;
import org.gnucash.android.ui.budget.BudgetAmountEditorFragment;
import org.gnucash.android.ui.budget.BudgetFormFragment;
import org.gnucash.android.ui.export.ExportFormFragment;
import org.gnucash.android.ui.passcode.PasscodeLockActivity;
import org.gnucash.android.ui.transaction.SplitEditorFragment;
import org.gnucash.android.ui.transaction.TransactionFormFragment;
import org.gnucash.android.ui.util.widget.CalculatorKeyboard;
import org.gnucash.android.util.BookUtils;

/**
 * Activity for displaying forms in the application.
 * The activity provides the standard close button, but it is up to the form fragments to display
 * menu options (e.g. for saving etc)
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class FormActivity extends PasscodeLockActivity {

    private String mAccountUID;

    private CalculatorKeyboard mOnBackListener;

    public enum FormType {
        ACCOUNT,
        TRANSACTION,
        EXPORT,
        SPLIT_EDITOR,
        BUDGET,
        BUDGET_AMOUNT_EDITOR
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        final Intent intent = getIntent();

        //if a parameter was passed to open an account within a specific book, then switch
        String bookUID = intent.getStringExtra(UxArgument.BOOK_UID);
        if (bookUID != null && !bookUID.equals(GnuCashApplication.getActiveBookUID())) {
            BookUtils.activateBook(this, bookUID);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        assert (actionBar != null);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white);

        mAccountUID = intent.getStringExtra(UxArgument.SELECTED_ACCOUNT_UID);
        if (mAccountUID == null) {
            mAccountUID = intent.getStringExtra(UxArgument.PARENT_ACCOUNT_UID);
        }
        if (mAccountUID != null) {
            int colorCode = AccountsDbAdapter.getActiveAccountColorResource(mAccountUID);
            actionBar.setBackgroundDrawable(new ColorDrawable(colorCode));
            getWindow().setStatusBarColor(GnuCashApplication.darken(colorCode));
        }

        Bundle args = intent.getExtras();
        if (args == null) args = new Bundle();
        String formtypeString = args.getString(UxArgument.FORM_TYPE);
        FormType formType = FormType.valueOf(formtypeString);
        switch (formType) {
            case ACCOUNT:
                showAccountFormFragment(args);
                break;

            case TRANSACTION:
                showTransactionFormFragment(args);
                break;

            case EXPORT:
                showExportFormFragment(args);
                break;

            case SPLIT_EDITOR:
                showSplitEditorFragment(args);
                break;

            case BUDGET:
                showBudgetFormFragment(args);
                break;

            case BUDGET_AMOUNT_EDITOR:
                showBudgetAmountEditorFragment(args);
                break;

            default:
                throw new IllegalArgumentException("No form display type specified");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
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
     *
     * @return GUID of account
     */
    public String getCurrentAccountUID() {
        return mAccountUID;
    }

    /**
     * Shows the form for creating/editing accounts
     *
     * @param args Arguments to use for initializing the form.
     *             This could be an account to edit or a preset for the parent account
     */
    private void showAccountFormFragment(Bundle args) {
        AccountFormFragment accountFormFragment = AccountFormFragment.newInstance();
        accountFormFragment.setArguments(args);
        showFormFragment(accountFormFragment);
    }

    /**
     * Loads the transaction insert/edit fragment and passes the arguments
     *
     * @param args Bundle arguments to be passed to the fragment
     */
    private void showTransactionFormFragment(Bundle args) {
        TransactionFormFragment transactionFormFragment = new TransactionFormFragment();
        transactionFormFragment.setArguments(args);
        showFormFragment(transactionFormFragment);
    }

    /**
     * Loads the export form fragment and passes the arguments
     *
     * @param args Bundle arguments
     */
    private void showExportFormFragment(Bundle args) {
        ExportFormFragment exportFragment = new ExportFormFragment();
        exportFragment.setArguments(args);
        showFormFragment(exportFragment);
    }

    /**
     * Load the split editor fragment
     *
     * @param args View arguments
     */
    private void showSplitEditorFragment(Bundle args) {
        SplitEditorFragment splitEditor = SplitEditorFragment.newInstance(args);
        showFormFragment(splitEditor);
    }

    /**
     * Load the budget form
     *
     * @param args View arguments
     */
    private void showBudgetFormFragment(Bundle args) {
        BudgetFormFragment budgetFormFragment = new BudgetFormFragment();
        budgetFormFragment.setArguments(args);
        showFormFragment(budgetFormFragment);
    }

    /**
     * Load the budget amount editor fragment
     *
     * @param args Arguments
     */
    private void showBudgetAmountEditorFragment(Bundle args) {
        BudgetAmountEditorFragment fragment = BudgetAmountEditorFragment.newInstance(args);
        showFormFragment(fragment);
    }

    /**
     * Loads the fragment into the fragment container, replacing whatever was there before
     *
     * @param fragment Fragment to be displayed
     */
    private void showFormFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
    }


    public void setOnBackListener(CalculatorKeyboard keyboard) {
        mOnBackListener = keyboard;
    }

    @Override
    public void onBackPressed() {
        boolean eventProcessed = false;

        if (mOnBackListener != null)
            eventProcessed = mOnBackListener.onBackPressed();

        if (!eventProcessed)
            super.onBackPressed();
    }

}
