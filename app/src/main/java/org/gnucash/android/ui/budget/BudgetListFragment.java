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

package org.gnucash.android.ui.budget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseCursorLoader;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BudgetsDbAdapter;
import org.gnucash.android.model.Budget;
import org.gnucash.android.model.BudgetAmount;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.common.FormActivity;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.util.CursorRecyclerAdapter;
import org.gnucash.android.ui.common.Refreshable;
import org.gnucash.android.ui.util.widget.EmptyRecyclerView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Budget list fragment
 */
public class BudgetListFragment extends Fragment implements Refreshable,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = "BudgetListFragment";
    private static final int REQUEST_EDIT_BUDGET = 0xB;
    private static final int REQUEST_OPEN_ACCOUNT = 0xC;

    private BudgetRecyclerAdapter mBudgetRecyclerAdapter;

    private BudgetsDbAdapter mBudgetsDbAdapter;

    @Bind(R.id.budget_recycler_view) EmptyRecyclerView mRecyclerView;
    @Bind(R.id.empty_view) Button mProposeBudgets;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget_list, container, false);
        ButterKnife.bind(this, view);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setEmptyView(mProposeBudgets);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 2);
            mRecyclerView.setLayoutManager(gridLayoutManager);
        } else {
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            mRecyclerView.setLayoutManager(mLayoutManager);
        }
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mBudgetsDbAdapter = BudgetsDbAdapter.getInstance();
        mBudgetRecyclerAdapter = new BudgetRecyclerAdapter(null);

        mRecyclerView.setAdapter(mBudgetRecyclerAdapter);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(LOG_TAG, "Creating the accounts loader");
        return new BudgetsCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loaderCursor, Cursor cursor) {
        Log.d(LOG_TAG, "Budget loader finished. Swapping in cursor");
        mBudgetRecyclerAdapter.swapCursor(cursor);
        mBudgetRecyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        Log.d(LOG_TAG, "Resetting the accounts loader");
        mBudgetRecyclerAdapter.swapCursor(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
        getActivity().findViewById(R.id.fab_create_budget).setVisibility(View.VISIBLE);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Budgets");
    }

    @Override
    public void refresh() {
        getLoaderManager().restartLoader(0, null, this);
    }

    /**
     * This method does nothing with the GUID.
     * Is equivalent to calling {@link #refresh()}
     * @param uid GUID of relevant item to be refreshed
     */
    @Override
    public void refresh(String uid) {
        refresh();
    }

    /**
     * Opens the budget detail fragment
     * @param budgetUID GUID of budget
     */
    public void onClickBudget(String budgetUID){
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager
                .beginTransaction();

        fragmentTransaction.replace(R.id.fragment_container, BudgetDetailFragment.newInstance(budgetUID));
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    /**
     * Launches the FormActivity for editing the budget
     * @param budgetId Db record Id of the budget
     */
    private void editBudget(long budgetId){
        Intent addAccountIntent = new Intent(getActivity(), FormActivity.class);
        addAccountIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        addAccountIntent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.BUDGET.name());
        addAccountIntent.putExtra(UxArgument.BUDGET_UID, mBudgetsDbAdapter.getUID(budgetId));
        startActivityForResult(addAccountIntent, REQUEST_EDIT_BUDGET);
    }

    /**
     * Delete the budget from the database
     * @param budgetId Database record ID
     */
    private void deleteBudget(long budgetId){
        BudgetsDbAdapter.getInstance().deleteRecord(budgetId);
        refresh();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK){
            refresh();
        }
    }

    class BudgetRecyclerAdapter extends CursorRecyclerAdapter<BudgetRecyclerAdapter.BudgetViewHolder>{

        public BudgetRecyclerAdapter(Cursor cursor) {
            super(cursor);
        }

        @Override
        public void onBindViewHolderCursor(BudgetViewHolder holder, Cursor cursor) {
            final Budget budget = mBudgetsDbAdapter.buildModelInstance(cursor);
            holder.budgetId = mBudgetsDbAdapter.getID(budget.getUID());

            holder.budgetName.setText(budget.getName());

            AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();
            String accountString;
            int numberOfAccounts = budget.getNumberOfAccounts();
            if (numberOfAccounts == 1){
                accountString = accountsDbAdapter.getAccountFullName(budget.getBudgetAmounts().get(0).getAccountUID());
            } else {
                accountString = numberOfAccounts + " budgeted accounts";
            }
            holder.accountName.setText(accountString);

            holder.budgetRecurrence.setText(budget.getRecurrence().getRepeatString() + " - "
                    + budget.getRecurrence().getDaysLeftInCurrentPeriod() + " days left");

            BigDecimal spentAmountValue = BigDecimal.ZERO;
            for (BudgetAmount budgetAmount : budget.getCompactedBudgetAmounts()) {
                Money balance = accountsDbAdapter.getAccountBalance(budgetAmount.getAccountUID(),
                        budget.getStartofCurrentPeriod(), budget.getEndOfCurrentPeriod());
                spentAmountValue = spentAmountValue.add(balance.asBigDecimal());
            }

            Money budgetTotal = budget.getAmountSum();
            Currency currency = budgetTotal.getCurrency();
            String usedAmount = currency.getSymbol() + spentAmountValue+ " of "
                    + budgetTotal.formattedString();
            holder.budgetAmount.setText(usedAmount);

            double budgetProgress = spentAmountValue.divide(budgetTotal.asBigDecimal(),
                    currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN)
                    .doubleValue();
            holder.budgetIndicator.setProgress((int) (budgetProgress * 100));

            holder.budgetAmount.setTextColor(BudgetsActivity.getBudgetProgressColor(1 - budgetProgress));

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickBudget(budget.getUID());
                }
            });
        }

        @Override
        public BudgetViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cardview_budget, parent, false);

            return new BudgetViewHolder(v);
        }

        class BudgetViewHolder extends RecyclerView.ViewHolder implements PopupMenu.OnMenuItemClickListener{
            @Bind(R.id.primary_text)        TextView budgetName;
            @Bind(R.id.secondary_text)      TextView accountName;
            @Bind(R.id.budget_amount)       TextView budgetAmount;
            @Bind(R.id.options_menu)        ImageView optionsMenu;
            @Bind(R.id.budget_indicator)    ProgressBar budgetIndicator;
            @Bind(R.id.budget_recurrence)   TextView budgetRecurrence;
            long budgetId;

            public BudgetViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);

                optionsMenu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        android.support.v7.widget.PopupMenu popup = new android.support.v7.widget.PopupMenu(getActivity(), v);
                        popup.setOnMenuItemClickListener(BudgetViewHolder.this);
                        MenuInflater inflater = popup.getMenuInflater();
                        inflater.inflate(R.menu.budget_context_menu, popup.getMenu());
                        popup.show();
                    }
                });

            }

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.context_menu_edit_budget:
                        editBudget(budgetId);
                        return true;

                    case R.id.context_menu_delete:
                        deleteBudget(budgetId);
                        return true;

                    default:
                        return false;
                }
            }
        }
    }

    /**
     * Loads Budgets asynchronously from the database
     */
    private static class BudgetsCursorLoader extends DatabaseCursorLoader {

        /**
         * Constructor
         * Initializes the content observer
         *
         * @param context Application context
         */
        public BudgetsCursorLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            mDatabaseAdapter = BudgetsDbAdapter.getInstance();
            return mDatabaseAdapter.fetchAllRecords(null, null, DatabaseSchema.BudgetEntry.COLUMN_NAME + " ASC");
        }
    }
}
