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
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.codetroopers.betterpickers.recurrencepicker.EventRecurrence;
import com.codetroopers.betterpickers.recurrencepicker.EventRecurrenceFormatter;
import com.codetroopers.betterpickers.recurrencepicker.RecurrencePickerDialogFragment;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BudgetsDbAdapter;
import org.gnucash.android.model.Budget;
import org.gnucash.android.model.BudgetAmount;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Recurrence;
import org.gnucash.android.ui.common.FormActivity;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.transaction.TransactionFormFragment;
import org.gnucash.android.ui.util.RecurrenceParser;
import org.gnucash.android.ui.util.RecurrenceViewClickListener;
import org.gnucash.android.ui.util.widget.CalculatorEditText;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.IllegalFormatCodePointException;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Fragment for creating or editing Budgets
 */
public class BudgetFormFragment extends Fragment implements RecurrencePickerDialogFragment.OnRecurrenceSetListener, CalendarDatePickerDialogFragment.OnDateSetListener {

    public static final int REQUEST_EDIT_BUDGET_AMOUNTS = 0xBA;
    @Bind(R.id.input_budget_name)   EditText mBudgetNameInput;
    @Bind(R.id.input_description)   EditText mDescriptionInput;
    @Bind(R.id.input_recurrence)    TextView mRecurrenceInput;
    @Bind(R.id.name_text_input_layout)  TextInputLayout mNameTextInputLayout;
    @Bind(R.id.calculator_keyboard)     KeyboardView mKeyboardView;
    @Bind(R.id.input_budget_amount)     CalculatorEditText mBudgetAmountInput;
    @Bind(R.id.input_budget_account_spinner) Spinner mBudgetAccountSpinner;
    @Bind(R.id.btn_add_budget_amount)   Button mAddBudgetAmount;
    @Bind(R.id.input_start_date)        TextView mStartDateInput;
    @Bind(R.id.budget_amount_layout)    View mBudgetAmountLayout;

    EventRecurrence mEventRecurrence = new EventRecurrence();
    String mRecurrenceRule;

    private BudgetsDbAdapter mBudgetsDbAdapter;

    private Budget mBudget;
    private Calendar mStartDate;
    private ArrayList<BudgetAmount> mBudgetAmounts;
    private AccountsDbAdapter mAccountsDbAdapter;
    private QualifiedAccountNameCursorAdapter mAccountsCursorAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget_form, container, false);
        ButterKnife.bind(this, view);

        view.findViewById(R.id.btn_remove_item).setVisibility(View.GONE);
        mBudgetAmountInput.bindListeners(mKeyboardView);
        mStartDateInput.setText(TransactionFormFragment.DATE_FORMATTER.format(mStartDate.getTime()));
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBudgetsDbAdapter = BudgetsDbAdapter.getInstance();
        mStartDate = Calendar.getInstance();
        mBudgetAmounts = new ArrayList<>();
        String conditions = "(" + DatabaseSchema.AccountEntry.COLUMN_HIDDEN + " = 0 )";
        mAccountsDbAdapter = AccountsDbAdapter.getInstance();
        Cursor accountCursor = mAccountsDbAdapter.fetchAccountsOrderedByFullName(conditions, null);
        mAccountsCursorAdapter = new QualifiedAccountNameCursorAdapter(getActivity(), accountCursor);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        mBudgetAccountSpinner.setAdapter(mAccountsCursorAdapter);
        String budgetUID = getArguments().getString(UxArgument.BUDGET_UID);
        if (budgetUID != null){ //if we are editing the budget
            initViews(mBudget = mBudgetsDbAdapter.getRecord(budgetUID));
        }
        ActionBar actionbar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        assert actionbar != null;
        if (mBudget ==  null)
            actionbar.setTitle("Create Budget");
        else
            actionbar.setTitle("Edit Budget");

        mRecurrenceInput.setOnClickListener(
                new RecurrenceViewClickListener((AppCompatActivity) getActivity(), mRecurrenceRule, this));
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

        mBudgetAmounts = (ArrayList<BudgetAmount>) budget.getCompactedBudgetAmounts();
        toggleAmountInputVisibility();
    }

    /**
     * Extracts the budget amounts from the form
     * <p>If the budget amount was input using the simple form, then read the values.<br>
     *     Else return the values gotten from the BudgetAmountEditor</p>
     * @return List of budget amounts
     */
    private ArrayList<BudgetAmount> extractBudgetAmounts(){
        BigDecimal value = mBudgetAmountInput.getValue();
        if (value == null)
            return mBudgetAmounts;

        if (mBudgetAmounts.isEmpty()){ //has not been set in budget amounts editor
            ArrayList<BudgetAmount> budgetAmounts = new ArrayList<>();
            Money amount = new Money(value, Commodity.DEFAULT_COMMODITY);
            String accountUID = mAccountsDbAdapter.getUID(mBudgetAccountSpinner.getSelectedItemId());
            BudgetAmount budgetAmount = new BudgetAmount(amount, accountUID);
            budgetAmounts.add(budgetAmount);
            return budgetAmounts;
        } else {
            return mBudgetAmounts;
        }
    }

    /**
     * Checks that this budget can be saved
     * Also sets the appropriate error messages on the relevant views
     * <p>For a budget to be saved, it needs to have a name, an amount and a schedule</p>
     * @return {@code true} if the budget can be saved, {@code false} otherwise
     */
    private boolean canSave(){
        if (mEventRecurrence.until != null && mEventRecurrence.until.length() > 0
                || mEventRecurrence.count <= 0){
            Toast.makeText(getActivity(),
                    "Set a number periods in the recurrence dialog to save the budget",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        mBudgetAmounts = extractBudgetAmounts();
        String budgetName = mBudgetNameInput.getText().toString();
        boolean canSave = mRecurrenceRule != null
                && !budgetName.isEmpty()
                && !mBudgetAmounts.isEmpty();

        if (!canSave){
            if (budgetName.isEmpty()){
                mNameTextInputLayout.setError("A name is required");
                mNameTextInputLayout.setErrorEnabled(true);
            } else {
                mNameTextInputLayout.setErrorEnabled(false);
            }

            if (mBudgetAmounts.isEmpty()){
                mBudgetAmountInput.setError("Enter an amount for the budget");
                Toast.makeText(getActivity(), "Add budget amounts in order to save the budget",
                        Toast.LENGTH_SHORT).show();
            }

            if (mRecurrenceRule == null){
                Toast.makeText(getActivity(), "Set a repeat pattern to create a budget!",
                        Toast.LENGTH_SHORT).show();
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
        extractBudgetAmounts();
        mBudget.setBudgetAmounts(mBudgetAmounts);

        mBudget.setDescription(mDescriptionInput.getText().toString().trim());

        Recurrence recurrence = RecurrenceParser.parse(mEventRecurrence);
        recurrence.setPeriodStart(new Timestamp(mStartDate.getTimeInMillis()));
        mBudget.setRecurrence(recurrence);

        mBudgetsDbAdapter.addRecord(mBudget);
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

    @OnClick(R.id.input_start_date)
    public void onClickBudgetStartDate(View v) {
        long dateMillis = 0;
        try {
            Date date = TransactionFormFragment.DATE_FORMATTER.parse(((TextView) v).getText().toString());
            dateMillis = date.getTime();
        } catch (ParseException e) {
            Log.e(getTag(), "Error converting input time to Date object");
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateMillis);

        int year = calendar.get(Calendar.YEAR);
        int monthOfYear = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        CalendarDatePickerDialogFragment datePickerDialog = CalendarDatePickerDialogFragment.newInstance(
                BudgetFormFragment.this,
                year, monthOfYear, dayOfMonth);
        datePickerDialog.show(getFragmentManager(), "date_picker_fragment");
    }

    @OnClick(R.id.btn_add_budget_amount)
    public void onOpenBudgetAmountEditor(View v){
        Intent intent = new Intent(getActivity(), FormActivity.class);
        intent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.BUDGET_AMOUNT_EDITOR.name());
        mBudgetAmounts = extractBudgetAmounts();
        intent.putParcelableArrayListExtra(UxArgument.BUDGET_AMOUNT_LIST, mBudgetAmounts);
        startActivityForResult(intent, REQUEST_EDIT_BUDGET_AMOUNTS);
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

    @Override
    public void onDateSet(CalendarDatePickerDialogFragment dialog, int year, int monthOfYear, int dayOfMonth) {
        Calendar cal = new GregorianCalendar(year, monthOfYear, dayOfMonth);
        mStartDateInput.setText(TransactionFormFragment.DATE_FORMATTER.format(cal.getTime()));
        mStartDate.set(Calendar.YEAR, year);
        mStartDate.set(Calendar.MONTH, monthOfYear);
        mStartDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EDIT_BUDGET_AMOUNTS){
            if (resultCode == Activity.RESULT_OK){
                ArrayList<BudgetAmount> budgetAmounts = data.getParcelableArrayListExtra(UxArgument.BUDGET_AMOUNT_LIST);
                if (budgetAmounts != null){
                    mBudgetAmounts = budgetAmounts;
                    toggleAmountInputVisibility();
                }
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Toggles the visibility of the amount input based on {@link #mBudgetAmounts}
     */
    private void toggleAmountInputVisibility() {
        if (mBudgetAmounts.size() > 1){
            mBudgetAmountLayout.setVisibility(View.GONE);
            mAddBudgetAmount.setText("Edit Budget Amounts");
        } else {
            mAddBudgetAmount.setText("Add Budget Amounts");
            mBudgetAmountLayout.setVisibility(View.VISIBLE);
            if (!mBudgetAmounts.isEmpty()) {
                BudgetAmount budgetAmount = mBudgetAmounts.get(0);
                mBudgetAmountInput.setValue(budgetAmount.getAmount().asBigDecimal());
                mBudgetAccountSpinner.setSelection(mAccountsCursorAdapter.getPosition(budgetAmount.getAccountUID()));
            }
        }
    }
}
