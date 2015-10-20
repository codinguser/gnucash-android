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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BudgetDbAdapter;
import org.gnucash.android.model.Budget;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.common.FormActivity;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.transaction.TransactionsActivity;
import org.gnucash.android.ui.util.Refreshable;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Fragment for displaying budget details
 */
public class BudgetDetailFragment extends Fragment implements Refreshable {
    @Bind(R.id.primary_text)        TextView mBudgetNameTextView;
    @Bind(R.id.secondary_text)      TextView mBudgetAccountTextView;
    @Bind(R.id.budget_recurrence)   TextView mBudgetRecurrence;
    @Bind(R.id.budget_spent)        TextView mBudgetSpent;
    @Bind(R.id.budget_left)         TextView mBudgetLeft;
    @Bind(R.id.budget_indicator)    ProgressBar mBudgetIndicator;
    @Bind(R.id.budget_chart)        BarChart mBudgetChart;


    private String mBudgetUID;
    private BudgetDbAdapter mBudgetDbAdapter;

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
        return view;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mBudgetDbAdapter = BudgetDbAdapter.getInstance();
        mBudgetUID = getArguments().getString(UxArgument.BUDGET_UID);
        bindViews();

        setHasOptionsMenu(true);
    }

    private void bindViews(){
        Budget budget = mBudgetDbAdapter.getRecord(mBudgetUID);
        mBudgetNameTextView.setText(budget.getName());

        AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();
        String accountName = accountsDbAdapter.getAccountFullName(budget.getAccountUID());
        mBudgetAccountTextView.setText(accountName);

        String budgetRecurrence = budget.getAmount().formattedString() + "  " + budget.getRecurrence().getRepeatString();
        mBudgetRecurrence.setText(budgetRecurrence);

        Money budgetAmount = budget.getAmount();
        Money spentAmount = accountsDbAdapter.getAccountBalance(budget.getAccountUID(),
                budget.getStartofCurrentPeriod(), budget.getEndOfCurrentPeriod());

        mBudgetSpent.setText(spentAmount.formattedString());
        mBudgetLeft.setText(budgetAmount.subtract(spentAmount).formattedString());

        double budgetProgress = spentAmount.divide(budgetAmount).asBigDecimal().doubleValue() * 100;
        mBudgetIndicator.setProgress((int) budgetProgress);

        //TODO: display chart for past months/weeks/years depending on budget periodtype
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

            case R.id.menu_goto_account:
                Intent intent = new Intent(getActivity(), TransactionsActivity.class);
                intent.putExtra(UxArgument.SELECTED_ACCOUNT_UID, mBudgetDbAdapter.getAccountUID(mBudgetUID));
                startActivityForResult(intent, 0x10);
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
}
