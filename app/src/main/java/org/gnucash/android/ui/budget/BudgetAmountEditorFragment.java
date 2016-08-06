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
import android.database.Cursor;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.model.BudgetAmount;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.util.widget.CalculatorEditText;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Fragment for editing budgeting amounts
 */
public class BudgetAmountEditorFragment extends Fragment {

    private Cursor mAccountCursor;
    private QualifiedAccountNameCursorAdapter mAccountCursorAdapter;
    private List<View> mBudgetAmountViews = new ArrayList<>();
    private AccountsDbAdapter mAccountsDbAdapter;

    @Bind(R.id.budget_amount_layout)    LinearLayout mBudgetAmountTableLayout;
    @Bind(R.id.calculator_keyboard)     KeyboardView mKeyboardView;

    public static BudgetAmountEditorFragment newInstance(Bundle args){
        BudgetAmountEditorFragment fragment = new BudgetAmountEditorFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget_amount_editor, container, false);
        ButterKnife.bind(this, view);
        setupAccountSpinnerAdapter();
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAccountsDbAdapter = AccountsDbAdapter.getInstance();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Edit Budget Amounts");
        setHasOptionsMenu(true);

        ArrayList<BudgetAmount> budgetAmounts = getArguments().getParcelableArrayList(UxArgument.BUDGET_AMOUNT_LIST);
        if (budgetAmounts != null) {
            if (budgetAmounts.isEmpty()){
                BudgetAmountViewHolder viewHolder = (BudgetAmountViewHolder) addBudgetAmountView(null).getTag();
                viewHolder.removeItemBtn.setVisibility(View.GONE); //there should always be at least one
            } else {
                loadBudgetAmountViews(budgetAmounts);
            }
        } else {
            BudgetAmountViewHolder viewHolder = (BudgetAmountViewHolder) addBudgetAmountView(null).getTag();
            viewHolder.removeItemBtn.setVisibility(View.GONE); //there should always be at least one
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.budget_amount_editor_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_add_budget_amount:
                addBudgetAmountView(null);
                return true;

            case R.id.menu_save:
                saveBudgetAmounts();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Checks if the budget amounts can be saved
     * @return {@code true} if all amounts a properly entered, {@code false} otherwise
     */
    private boolean canSave(){
        for (View budgetAmountView : mBudgetAmountViews) {
            BudgetAmountViewHolder viewHolder = (BudgetAmountViewHolder) budgetAmountView.getTag();
            viewHolder.amountEditText.evaluate();
            if (viewHolder.amountEditText.getError() != null){
                return false;
            }
            //at least one account should be loaded (don't create budget with empty account tree
            if (viewHolder.budgetAccountSpinner.getCount() == 0){
                Toast.makeText(getActivity(), "You need an account hierarchy to create a budget!",
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void saveBudgetAmounts() {
        if (canSave()){
            ArrayList<BudgetAmount> budgetAmounts = (ArrayList<BudgetAmount>) extractBudgetAmounts();
            Intent data = new Intent();
            data.putParcelableArrayListExtra(UxArgument.BUDGET_AMOUNT_LIST, budgetAmounts);
            getActivity().setResult(Activity.RESULT_OK, data);
            getActivity().finish();
        }
    }

    /**
     * Load views for the budget amounts
     * @param budgetAmounts List of {@link BudgetAmount}s
     */
    private void loadBudgetAmountViews(List<BudgetAmount> budgetAmounts){
        for (BudgetAmount budgetAmount : budgetAmounts) {
            addBudgetAmountView(budgetAmount);
        }
    }

    /**
     * Inflates a new BudgetAmount item view and adds it to the UI.
     * <p>If the {@code budgetAmount} is not null, then it is used to initialize the view</p>
     * @param budgetAmount Budget amount
     */
    private View addBudgetAmountView(BudgetAmount budgetAmount){
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View budgetAmountView = layoutInflater.inflate(R.layout.item_budget_amount,
                mBudgetAmountTableLayout, false);
        BudgetAmountViewHolder viewHolder = new BudgetAmountViewHolder(budgetAmountView);
        if (budgetAmount != null){
            viewHolder.bindViews(budgetAmount);
        }
        mBudgetAmountTableLayout.addView(budgetAmountView, 0);
        mBudgetAmountViews.add(budgetAmountView);
//        mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
        return budgetAmountView;
    }

    /**
     * Loads the accounts in the spinner
     */
    private void setupAccountSpinnerAdapter(){
        String conditions = "(" + DatabaseSchema.AccountEntry.COLUMN_HIDDEN + " = 0 )";

        if (mAccountCursor != null) {
            mAccountCursor.close();
        }
        mAccountCursor = mAccountsDbAdapter.fetchAccountsOrderedByFullName(conditions, null);

        mAccountCursorAdapter = new QualifiedAccountNameCursorAdapter(getActivity(), mAccountCursor);
    }

    /**
     * Extract {@link BudgetAmount}s from the views
     * @return List of budget amounts
     */
    private List<BudgetAmount> extractBudgetAmounts(){
        List<BudgetAmount> budgetAmounts = new ArrayList<>();
        for (View view : mBudgetAmountViews) {
            BudgetAmountViewHolder viewHolder = (BudgetAmountViewHolder) view.getTag();
            BigDecimal amountValue = viewHolder.amountEditText.getValue();
            if (amountValue == null)
                continue;
            Money amount = new Money(amountValue, Commodity.DEFAULT_COMMODITY);
            String accountUID = mAccountsDbAdapter.getUID(viewHolder.budgetAccountSpinner.getSelectedItemId());
            BudgetAmount budgetAmount = new BudgetAmount(amount, accountUID);
            budgetAmounts.add(budgetAmount);
        }
        return budgetAmounts;
    }

    /**
     * View holder for budget amounts
     */
    class BudgetAmountViewHolder{
        @Bind(R.id.currency_symbol)     TextView currencySymbolTextView;
        @Bind(R.id.input_budget_amount) CalculatorEditText amountEditText;
        @Bind(R.id.btn_remove_item)     ImageView removeItemBtn;
        @Bind(R.id.input_budget_account_spinner) Spinner budgetAccountSpinner;
        View itemView;

        public BudgetAmountViewHolder(View view){
            itemView = view;
            ButterKnife.bind(this, view);
            itemView.setTag(this);

            amountEditText.bindListeners(mKeyboardView);
            budgetAccountSpinner.setAdapter(mAccountCursorAdapter);

            budgetAccountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String currencyCode = mAccountsDbAdapter.getCurrencyCode(mAccountsDbAdapter.getUID(id));
                    Currency currency = Currency.getInstance(currencyCode);
                    currencySymbolTextView.setText(currency.getSymbol());
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    //nothing to see here, move along
                }
            });

            removeItemBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mBudgetAmountTableLayout.removeView(itemView);
                    mBudgetAmountViews.remove(itemView);
                }
            });
        }

        public void bindViews(BudgetAmount budgetAmount){
            amountEditText.setValue(budgetAmount.getAmount().asBigDecimal());
            budgetAccountSpinner.setSelection(mAccountCursorAdapter.getPosition(budgetAmount.getAccountUID()));
        }
    }
}
