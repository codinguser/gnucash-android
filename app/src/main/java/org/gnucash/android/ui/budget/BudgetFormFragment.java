package org.gnucash.android.ui.budget;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.gnucash.android.R;

import butterknife.ButterKnife;

/**
 * Fragment for creating or editing Budgets
 */
public class BudgetFormFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget_form, container, false);
        ButterKnife.bind(this, view);

        return view;
    }
}
