package org.gnucash.android.ui.budget;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.codetroopers.betterpickers.recurrencepicker.EventRecurrence;

import org.gnucash.android.R;
import org.gnucash.android.ui.util.widget.CalculatorEditText;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Fragment for creating or editing Budgets
 */
public class BudgetFormFragment extends Fragment {

    @Bind(R.id.input_budget_name)   EditText mBudgetNameInput;
    @Bind(R.id.currency_symbol)     TextView mCurrencySymbolLabel;
    @Bind(R.id.input_budget_amount) CalculatorEditText mBudgetAmountInput;
    @Bind(R.id.input_description)   EditText mDescriptionInput;
    @Bind(R.id.input_recurrence)    TextView mRecurrenceInput;
    @Bind(R.id.name_text_input_layout) TextInputLayout mNameTextInputLayout;

    EventRecurrence mEventRecurrence;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget_form, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    /**
     * Checks that this budget can be saved
     * Also sets the appropriate error messages on the relevant views
     * <p>For a budget to be saved, it needs to have a name, an amount and a schedule</p>
     * @return {@code true} if the budget can be saved, {@code false} otherwise
     */
    private boolean canSave(){
        String budgetName = mBudgetNameInput.getText().toString();
        boolean canSave = mEventRecurrence != null
                && mBudgetAmountInput.getValue() != null
                && !budgetName.isEmpty();
        if (!canSave){
            if (mBudgetAmountInput.getValue() == null)
                mBudgetNameInput.setError("A budget amount is required!");

            if (budgetName.isEmpty()){
                mNameTextInputLayout.setError("A name is required");
                mNameTextInputLayout.setErrorEnabled(true);
            } else {
                mNameTextInputLayout.setErrorEnabled(false);
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


    }
}
