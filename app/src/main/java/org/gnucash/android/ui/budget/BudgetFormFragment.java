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

import android.database.Cursor;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.codetroopers.betterpickers.recurrencepicker.EventRecurrence;
import com.codetroopers.betterpickers.recurrencepicker.EventRecurrenceFormatter;
import com.codetroopers.betterpickers.recurrencepicker.RecurrencePickerDialogFragment;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BudgetsDbAdapter;
import org.gnucash.android.model.Budget;
import org.gnucash.android.model.BudgetAmount;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.util.RecurrenceParser;
import org.gnucash.android.ui.util.RecurrenceViewClickListener;
import org.gnucash.android.ui.util.widget.CalculatorEditText;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Fragment for creating or editing Budgets
 */
public class BudgetFormFragment extends Fragment implements RecurrencePickerDialogFragment.OnRecurrenceSetListener {

    @Bind(R.id.input_budget_name)   EditText mBudgetNameInput;
    @Bind(R.id.input_description)   EditText mDescriptionInput;
    @Bind(R.id.input_recurrence)    TextView mRecurrenceInput;
    @Bind(R.id.name_text_input_layout) TextInputLayout mNameTextInputLayout;
    @Bind(R.id.calculator_keyboard) KeyboardView mKeyboardView;
    @Bind(R.id.budget_table_layout) TableLayout mBudgetAmountTableLayout;
    @Bind(R.id.scroll_view)         ScrollView mScrollView;

    EventRecurrence mEventRecurrence = new EventRecurrence();
    String mRecurrenceRule;
    private Cursor mAccountCursor;
    private AccountsDbAdapter mAccountsDbAdapter;
    private BudgetsDbAdapter mBudgetsDbAdapter;

    private Budget mBudget;
    private QualifiedAccountNameCursorAdapter mAccountCursorAdapter;

    private List<View> mBudgetAmountViews = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget_form, container, false);
        ButterKnife.bind(this, view);

        setupAccountSpinnerAdapter();
        mRecurrenceInput.setOnClickListener(
                new RecurrenceViewClickListener((AppCompatActivity) getActivity(), mRecurrenceRule, this));

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAccountsDbAdapter = AccountsDbAdapter.getInstance();
        mBudgetsDbAdapter = BudgetsDbAdapter.getInstance();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ActionBar actionbar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionbar.setTitle("Create Budget");

        setHasOptionsMenu(true);

        String budgetUID = getArguments().getString(UxArgument.BUDGET_UID);
        if (budgetUID != null){ //if we are editing the budget
            initViews(mBudget = mBudgetsDbAdapter.getRecord(budgetUID));
            loadBudgetAmountViews(mBudget.getBudgetAmounts());
        } else {
            BudgetAmountViewHolder viewHolder = (BudgetAmountViewHolder) addBudgetAmountView(null).getTag();
            viewHolder.removeItemBtn.setVisibility(View.GONE); //there should always be at least one
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
     * Extract {@link BudgetAmount}s from the views
     * @return List of budget amounts
     */
    private List<BudgetAmount> extractBudgetAmounts(){
        List<BudgetAmount> budgetAmounts = new ArrayList<>();
        for (View view : mBudgetAmountViews) {
            BudgetAmountViewHolder viewHolder = (BudgetAmountViewHolder) view.getTag();
            Money amount = new Money(viewHolder.amountEditText.getValue(),
                    Currency.getInstance(Money.DEFAULT_CURRENCY_CODE));
            String accountUID = mAccountsDbAdapter.getUID(viewHolder.budgetAccountSpinner.getSelectedItemId());
            BudgetAmount budgetAmount = new BudgetAmount(amount, accountUID);
            budgetAmounts.add(budgetAmount);
        }
        return budgetAmounts;
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
        mBudgetAmountTableLayout.addView(budgetAmountView, mBudgetAmountTableLayout.getChildCount());
        mBudgetAmountViews.add(budgetAmountView);
        mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
        return budgetAmountView;
    }

    /**
     * Initialize views when editing an existing budget
     * @param budget Budget to use to initialize the views
     */
    private void initViews(Budget budget){
        mBudgetNameInput.setText(budget.getName());
        mDescriptionInput.setText(budget.getDescription());

        String recurrenceRuleString = budget.getRecurrence().getRuleString();
        mRecurrenceRule = recurrenceRuleString;
        mEventRecurrence.parse(recurrenceRuleString);
        mRecurrenceInput.setText(budget.getRecurrence().getRepeatString());
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
     * Checks that this budget can be saved
     * Also sets the appropriate error messages on the relevant views
     * <p>For a budget to be saved, it needs to have a name, an amount and a schedule</p>
     * @return {@code true} if the budget can be saved, {@code false} otherwise
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

        String budgetName = mBudgetNameInput.getText().toString();
        boolean canSave = mRecurrenceRule != null
                && !budgetName.isEmpty();
        if (!canSave){

            if (budgetName.isEmpty()){
                mNameTextInputLayout.setError("A name is required");
                mNameTextInputLayout.setErrorEnabled(true);
            } else {
                mNameTextInputLayout.setErrorEnabled(false);
            }

            if (mRecurrenceRule == null){
                Toast.makeText(getActivity(), "Set a repeat pattern to create a budget!", Toast.LENGTH_SHORT).show();
            }
        }

        return canSave;
    }

    /**
     * Extracts the information from the form and saves the budget
     */
    private void saveBudget(){
        if (!canSave())
            return;
        String name = mBudgetNameInput.getText().toString().trim();


        if (mBudget == null){
            mBudget = new Budget(name);
        } else {
            mBudget.setName(name);
        }

        // TODO: 22.10.2015 set the period num of the budget amount
        mBudget.setBudgetAmounts(extractBudgetAmounts());

        mBudget.setDescription(mDescriptionInput.getText().toString().trim());

        List<ScheduledAction> events = RecurrenceParser.parse(mEventRecurrence,
                ScheduledAction.ActionType.TRANSACTION);

        if (!events.isEmpty()){
            mBudget.setRecurrence(events.get(0).getRecurrence());
        }

        mBudgetsDbAdapter.addRecord(mBudget);
        getActivity().finish();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.budget_form_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_save:
                saveBudget();
                return true;

            case R.id.menu_add_budget_amount:
                addBudgetAmountView(null);
                return true;
        }
        return false;
    }

    @Override
    public void onRecurrenceSet(String rrule) {
        mRecurrenceRule = rrule;
        String repeatString = getString(R.string.label_tap_to_create_schedule);
        if (mRecurrenceRule != null){
            mEventRecurrence.parse(mRecurrenceRule);
            repeatString = EventRecurrenceFormatter.getRepeatString(getActivity(), getResources(), mEventRecurrence, true);
        }

        mRecurrenceInput.setText(repeatString);
    }

    /**
     * View holder for budget amounts
     */
    class BudgetAmountViewHolder{
        @Bind(R.id.currency_symbol) TextView currencySymbolTextView;
        @Bind(R.id.input_budget_amount) CalculatorEditText amountEditText;
        @Bind(R.id.input_budget_account_spinner) Spinner budgetAccountSpinner;
        @Bind(R.id.btn_remove_item) ImageView removeItemBtn;
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
