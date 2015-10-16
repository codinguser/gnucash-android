package org.gnucash.android.ui.budget;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
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
import android.widget.ImageView;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BudgetDbAdapter;
import org.gnucash.android.db.DatabaseCursorLoader;
import org.gnucash.android.model.Budget;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.common.FormActivity;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.util.CursorRecyclerAdapter;
import org.gnucash.android.ui.util.Refreshable;
import org.gnucash.android.ui.util.widget.EmptyRecyclerView;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Budget list fragment
 */
public class BudgetListFragment extends Fragment implements Refreshable,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = "BudgetListFragment";
    private static final int REQUEST_EDIT_BUDGET = 0xB;

    private BudgetRecyclerAdapter mBudgetRecyclerAdapter;

    private BudgetDbAdapter mBudgetDbAdapter;

    @Bind(R.id.budget_recycler_view) EmptyRecyclerView mRecyclerView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget_list, container, false);
        ButterKnife.bind(this, view);

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

        mBudgetDbAdapter = BudgetDbAdapter.getInstance();
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
    public void refresh() {
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void refresh(String uid) {
        refresh();
    }

    /**
     * Launches the FormActivity for editing the budget
     * @param budgetId Db record Id of the budget
     */
    private void editBudget(long budgetId){
        Intent addAccountIntent = new Intent(getActivity(), FormActivity.class);
        addAccountIntent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        addAccountIntent.putExtra(UxArgument.FORM_TYPE, FormActivity.FormType.BUDGET.name());
        addAccountIntent.putExtra(UxArgument.BUDGET_UID, mBudgetDbAdapter.getUID(budgetId));
        startActivityForResult(addAccountIntent, REQUEST_EDIT_BUDGET);
    }

    /**
     * Delete the budget from the database
     * @param budgetId Database record ID
     */
    private void deleteBudget(long budgetId){
        BudgetDbAdapter.getInstance().deleteRecord(budgetId);
        refresh();
    }

    /**
     * Returns a color between red and green depending on the value parameter
     * @param value Value between 0 and 1 indicating the red to green ratio
     * @return Color between red and green
     */
    public static int getTrafficlightColor(double value){
        return android.graphics.Color.HSVToColor(new float[]{(float)value*120f,1f,1f});
    }

    class BudgetRecyclerAdapter extends CursorRecyclerAdapter<BudgetRecyclerAdapter.BudgetViewHolder>{

        public BudgetRecyclerAdapter(Cursor cursor) {
            super(cursor);
        }

        @Override
        public void onBindViewHolderCursor(BudgetViewHolder holder, Cursor cursor) {
            Budget budget = mBudgetDbAdapter.buildModelInstance(cursor);
            holder.budgetId = mBudgetDbAdapter.getID(budget.getUID());

            holder.budgetName.setText(budget.getName());

            AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();
            holder.accountName.setText(accountsDbAdapter.getAccountFullName(budget.getAccountUID()));

            Money accountBalance = accountsDbAdapter.getAccountBalance(budget.getAccountUID());
            double redGreenRatio = 1 / (accountBalance.divide(budget.getAmount())).asDouble();

            int bgColor = getTrafficlightColor(redGreenRatio);

            holder.budgetAmount.setBackgroundColor(bgColor);
            holder.budgetAmount.setText(accountBalance.formattedAmount() + " of " + budget.getAmount().formattedAmount());

        }

        @Override
        public BudgetViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cardview_budget, parent, false);

            return new BudgetViewHolder(v);
        }

        class BudgetViewHolder extends RecyclerView.ViewHolder implements PopupMenu.OnMenuItemClickListener{
            @Bind(R.id.primary_text) TextView budgetName;
            @Bind(R.id.secondary_text) TextView accountName;
            @Bind(R.id.budget_indicator) TextView budgetAmount;
            @Bind(R.id.options_menu) ImageView optionsMenu;
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
                        inflater.inflate(R.menu.account_context_menu, popup.getMenu());
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
            mDatabaseAdapter = BudgetDbAdapter.getInstance();
            return mDatabaseAdapter.fetchAllRecords();
        }
    }
}
