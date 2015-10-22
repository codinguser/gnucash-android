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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.codetroopers.betterpickers.recurrencepicker.EventRecurrence;
import com.codetroopers.betterpickers.recurrencepicker.EventRecurrenceFormatter;
import com.codetroopers.betterpickers.recurrencepicker.RecurrencePickerDialogFragment;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BudgetDbAdapter;
import org.gnucash.android.model.Budget;
import org.gnucash.android.model.BudgetAmount;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.ScheduledAction;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.util.RecurrenceParser;
import org.gnucash.android.ui.util.RecurrenceViewClickListener;
import org.gnucash.android.ui.util.widget.CalculatorEditText;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import java.util.Currency;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Fragment for creating or editing Budgets
 */
public class BudgetFormFragment extends Fragment implements RecurrencePickerDialogFragment.OnRecurrenceSetListener {

    @Bind(R.id.input_budget_name)   EditText mBudgetNameInput;
    @Bind(R.id.currency_symbol)     TextView mCurrencySymbolLabel;
    @Bind(R.id.input_budget_amount) CalculatorEditText mBudgetAmountInput;
    @Bind(R.id.input_description)   EditText mDescriptionInput;
    @Bind(R.id.input_recurrence)    TextView mRecurrenceInput;
    @Bind(R.id.name_text_input_layout) TextInputLayout mNameTextInputLayout;
    @Bind(R.id.input_budget_account_spinner) Spinner mBudgetAccountSpinner;
    @Bind(R.id.calculator_keyboard) KeyboardView mKeyboardView;

    EventRecurrence mEventRecurrence = new EventRecurrence();
    String mRecurrenceRule;
    private Cursor mAccountCursor;
    private AccountsDbAdapter mAccountsDbAdapter;
    private BudgetDbAdapter mBudgetDbAdapter;

    private Budget mBudget;
    private QualifiedAccountNameCursorAdapter mAccountCursorAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget_form, container, false);
        ButterKnife.bind(this, view);

        updateBudgetAccountsList();
        mBudgetAccountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String currencyCode = mAccountsDbAdapter.getAccountCurrencyCode(mAccountsDbAdapter.getUID(id));
                mCurrencySymbolLabel.setText(Currency.getInstance(currencyCode).getSymbol());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //nothing to see here, move along
            }
        });
        mRecurrenceInput.setOnClickListener(new RecurrenceViewClickListener((AppCompatActivity) getActivity(), mRecurrenceRule, this));

        mBudgetAmountInput.bindListeners(mKeyboardView);
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAccountsDbAdapter = AccountsDbAdapter.getInstance();
        mBudgetDbAdapter = BudgetDbAdapter.getInstance();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ActionBar actionbar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionbar.setTitle("Create Budget");

        setHasOptionsMenu(true);

        String budgetUID = getArguments().getString(UxArgument.BUDGET_UID);
        if (budgetUID != null){ //if we are editing the budget
            initViews(mBudget = mBudgetDbAdapter.getRecord(budgetUID));
        }
    }

    /**
     * Initialize views when editing an existing budget
     * @param budget Budget to use to initialize the views
     */
    private void initViews(Budget budget){
        // FIXME: 22.10.2015 allow multiple account views
//        int position = mAccountCursorAdapter.getPosition(budget.getAccountUID());
//        if (position >= 0){
//            mBudgetAccountSpinner.setSelection(position);
//        }
        mRecurrenceRule = budget.getRecurrence().getRuleString();

        mBudgetNameInput.setText(budget.getName());
//        mBudgetAmountInput.setValue(budget.getAmount().asBigDecimal());
        mDescriptionInput.setText(budget.getDescription());
        mRecurrenceInput.setText(budget.getRecurrence().getRuleString());
        mEventRecurrence.parse(budget.getRecurrence().getRuleString());
        mRecurrenceInput.setText(budget.getRecurrence().getRepeatString());
    }

    /**
     * Loads the accounts in the spinner
     */
    private void updateBudgetAccountsList(){
        String conditions = "(" + DatabaseSchema.AccountEntry.COLUMN_HIDDEN + " = 0 )";

        if (mAccountCursor != null) {
            mAccountCursor.close();
        }
        mAccountCursor = mAccountsDbAdapter.fetchAccountsOrderedByFullName(conditions, null);

        mAccountCursorAdapter = new QualifiedAccountNameCursorAdapter(getActivity(), mAccountCursor);
        mBudgetAccountSpinner.setAdapter(mAccountCursorAdapter);

    }

    /**
     * Checks that this budget can be saved
     * Also sets the appropriate error messages on the relevant views
     * <p>For a budget to be saved, it needs to have a name, an amount and a schedule</p>
     * @return {@code true} if the budget can be saved, {@code false} otherwise
     */
    private boolean canSave(){
        String budgetName = mBudgetNameInput.getText().toString();
        boolean canSave = mRecurrenceRule != null
                && mBudgetAmountInput.getValue() != null
                && !budgetName.isEmpty()
                && mBudgetAccountSpinner.getCount() > 0; //there is at least one account in the system
        if (!canSave){
            if (mBudgetAmountInput.getValue() == null)
                mBudgetNameInput.setError("A budget amount is required!");

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
        String accountUID = mAccountsDbAdapter.getUID(mBudgetAccountSpinner.getSelectedItemId());
        String currencyCode = mAccountsDbAdapter.getCurrencyCode(accountUID);
        Money amount = new Money(mBudgetAmountInput.getValue(), Currency.getInstance(currencyCode));

        if (mBudget == null){
            mBudget = new Budget(name);
        } else {
            mBudget.setName(name);
        }

        BudgetAmount budgetAmount = new BudgetAmount(amount, accountUID);
        budgetAmount.setAmount(amount);
        // TODO: 22.10.2015 set the period num of the budget amount
        mBudget.addBudgetAmount(budgetAmount);

        mBudget.setDescription(mDescriptionInput.getText().toString().trim());

        List<ScheduledAction> events = RecurrenceParser.parse(mEventRecurrence,
                ScheduledAction.ActionType.TRANSACTION);

        if (!events.isEmpty()){
            mBudget.setRecurrence(events.get(0).getRecurrence());
        }

        mBudgetDbAdapter.addRecord(mBudget);
        getActivity().finish();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.default_save_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_save:
                saveBudget();
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
}
