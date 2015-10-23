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
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BudgetsDbAdapter;
import org.gnucash.android.model.Budget;
import org.gnucash.android.model.BudgetAmount;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.common.FormActivity;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.transaction.TransactionsActivity;
import org.gnucash.android.ui.util.Refreshable;
import org.gnucash.android.ui.util.widget.EmptyRecyclerView;

import java.math.RoundingMode;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Fragment for displaying budget details
 */
public class BudgetDetailFragment extends Fragment implements Refreshable {
    @Bind(R.id.primary_text)        TextView mBudgetNameTextView;
    @Bind(R.id.secondary_text)      TextView mBudgetDescriptionTextView;
    @Bind(R.id.budget_recurrence)   TextView mBudgetRecurrence;
    @Bind(R.id.budget_amount_recycler) EmptyRecyclerView mRecyclerView;

    private String mBudgetUID;
    private BudgetsDbAdapter mBudgetsDbAdapter;

    public static BudgetDetailFragment newInstance(String budgetUID){
        BudgetDetailFragment fragment = new BudgetDetailFragment();
        Bundle args = new Bundle();
        args.putString(UxArgument.BUDGET_UID, budgetUID);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget_detail, container, false);
        ButterKnife.bind(this, view);
        mBudgetDescriptionTextView.setMaxLines(3);

        mRecyclerView.setHasFixedSize(true);

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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mBudgetsDbAdapter = BudgetsDbAdapter.getInstance();
        mBudgetUID = getArguments().getString(UxArgument.BUDGET_UID);
        bindViews();

        setHasOptionsMenu(true);
    }

    private void bindViews(){
        Budget budget = mBudgetsDbAdapter.getRecord(mBudgetUID);
        mBudgetNameTextView.setText(budget.getName());

        String description = budget.getDescription();
        if (description != null && !description.isEmpty())
            mBudgetDescriptionTextView.setText(description);
        else {
            mBudgetDescriptionTextView.setVisibility(View.GONE);
        }
        mBudgetRecurrence.setText(budget.getRecurrence().getRepeatString());

        mRecyclerView.setAdapter(new BudgetAmountAdapter());
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof BudgetsActivity){
            activity.findViewById(R.id.fab_create_budget).setVisibility(View.GONE);
        }
    }

    @Override
    public void refresh() {
        bindViews();
        String budgetName = mBudgetsDbAdapter.getAttribute(mBudgetUID, DatabaseSchema.BudgetEntry.COLUMN_NAME);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(budgetName);
    }

    @Override
    public void refresh(String budgetUID) {
        mBudgetUID = budgetUID;
        refresh();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.budget_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_edit_budget:
                Intent addAccountIntent = new Intent(getActivity(), FormActivity.class);
                addAccountIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
                addAccountIntent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.BUDGET.name());
                addAccountIntent.putExtra(UxArgument.BUDGET_UID, mBudgetUID);
                startActivityForResult(addAccountIntent, 0x11);
                return true;

            default:
                return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK){
            refresh();
        }
    }


    public class BudgetAmountAdapter extends RecyclerView.Adapter<BudgetAmountAdapter.BudgetAmountViewHolder>{
        private List<BudgetAmount> mBudgetAmounts;
        private Budget mBudget;

        public BudgetAmountAdapter(){
            mBudget = mBudgetsDbAdapter.getRecord(mBudgetUID);
            mBudgetAmounts = mBudget.getBudgetAmounts();
        }

        @Override
        public BudgetAmountViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.cardview_budget_amount, parent, false);
            return new BudgetAmountViewHolder(view);
        }

        @Override
        public void onBindViewHolder(BudgetAmountViewHolder holder, final int position) {
            BudgetAmount budgetAmount = mBudgetAmounts.get(position);
            Money projectedAmount = budgetAmount.getAmount();
            AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();

            holder.budgetAccount.setText(accountsDbAdapter.getAccountFullName(budgetAmount.getAccountUID()));
            holder.budgetAmount.setText(projectedAmount.formattedString());

            Money spentAmount = accountsDbAdapter.getAccountBalance(budgetAmount.getAccountUID(),
                    mBudget.getStartofCurrentPeriod(), mBudget.getEndOfCurrentPeriod());

            holder.budgetSpent.setText(spentAmount.abs().formattedString());
            holder.budgetLeft.setText(projectedAmount.subtract(spentAmount.abs()).formattedString());

            double budgetProgress = spentAmount.asBigDecimal().divide(projectedAmount.asBigDecimal(),
                    spentAmount.getCurrency().getDefaultFractionDigits(), RoundingMode.HALF_EVEN)
                    .doubleValue();
            holder.budgetIndicator.setProgress((int) budgetProgress * 100);
            holder.budgetSpent.setTextColor(BudgetsActivity.getBudgetProgressColor(1 - budgetProgress));

            //TODO: implement chart
            //TODO: display chart for past months/weeks/years depending on budget periodtype

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), TransactionsActivity.class);
                    intent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, mBudgetAmounts.get(position).getAccountUID());
                    startActivityForResult(intent, 0x10);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mBudgetAmounts.size();
        }

        class BudgetAmountViewHolder extends RecyclerView.ViewHolder {
            @Bind(R.id.budget_account)      TextView budgetAccount;
            @Bind(R.id.budget_amount)       TextView budgetAmount;
            @Bind(R.id.budget_spent)        TextView budgetSpent;
            @Bind(R.id.budget_left)         TextView budgetLeft;
            @Bind(R.id.budget_indicator)    ProgressBar budgetIndicator;
            @Bind(R.id.budget_chart)        BarChart budgetChart;

            public BudgetAmountViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }

        }
    }
}
