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
package org.gnucash.android.ui.report;

import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.transaction.TransactionsActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Fragment report as text
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class BalanceSheetFragment extends Fragment {

    @Bind(R.id.table_assets) TableLayout mAssetsTableLayout;
    @Bind(R.id.table_liabilities) TableLayout mLiabilitiesTableLayout;
    @Bind(R.id.table_equity) TableLayout mEquityTableLayout;

    @Bind(R.id.total_liability_and_equity) TextView mNetWorth;


    AccountsDbAdapter mAccountsDbAdapter = AccountsDbAdapter.getInstance();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_text_report, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.title_balance_sheet_report);
        setHasOptionsMenu(true);

        List<AccountType> accountTypes = new ArrayList<>();
        accountTypes.add(AccountType.ASSET);
        accountTypes.add(AccountType.CASH);
        accountTypes.add(AccountType.BANK);
        loadAccountViews(accountTypes, mAssetsTableLayout);
        Money assetsBalance = mAccountsDbAdapter.getAccountBalance(accountTypes, -1, System.currentTimeMillis());

        accountTypes.clear();
        accountTypes.add(AccountType.LIABILITY);
        accountTypes.add(AccountType.CREDIT);
        loadAccountViews(accountTypes, mLiabilitiesTableLayout);
        Money liabilitiesBalance = mAccountsDbAdapter.getAccountBalance(accountTypes, -1, System.currentTimeMillis());

        accountTypes.clear();
        accountTypes.add(AccountType.EQUITY);
        loadAccountViews(accountTypes, mEquityTableLayout);

        TransactionsActivity.displayBalance(mNetWorth, assetsBalance.subtract(liabilitiesBalance));
    }

    @Override
    public void onResume() {
        super.onResume();
        ((ReportsActivity)getActivity()).setAppBarColor(R.color.account_purple);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_group_reports_by).setVisible(false);
    }

    /**
     * Loads rows for the individual accounts and adds them to the report
     * @param accountTypes Account types for which to load balances
     * @param tableLayout Table layout into which to load the rows
     */
    private void loadAccountViews(List<AccountType> accountTypes, TableLayout tableLayout){
        LayoutInflater inflater = LayoutInflater.from(getActivity());

        Cursor cursor = mAccountsDbAdapter.fetchAccounts(DatabaseSchema.AccountEntry.COLUMN_TYPE
                        + " IN ( '" + TextUtils.join("' , '", accountTypes) + "' ) AND "
                        + DatabaseSchema.AccountEntry.COLUMN_PLACEHOLDER + " = 0",
                null, DatabaseSchema.AccountEntry.COLUMN_FULL_NAME + " ASC");

        while (cursor.moveToNext()){
            String accountUID = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_UID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.AccountEntry.COLUMN_NAME));
            Money balance = mAccountsDbAdapter.getAccountBalance(accountUID);
            View view = inflater.inflate(R.layout.row_balance_sheet, tableLayout, false);
            ((TextView)view.findViewById(R.id.account_name)).setText(name);
            TextView balanceTextView = ((TextView) view.findViewById(R.id.account_balance));
            TransactionsActivity.displayBalance(balanceTextView, balance);
            tableLayout.addView(view);
        }

        View totalView = inflater.inflate(R.layout.row_balance_sheet, tableLayout, false);
        TableLayout.LayoutParams layoutParams = (TableLayout.LayoutParams) totalView.getLayoutParams();
        layoutParams.setMargins(layoutParams.leftMargin, 20, layoutParams.rightMargin, layoutParams.bottomMargin);
        totalView.setLayoutParams(layoutParams);

        TextView accountName = (TextView) totalView.findViewById(R.id.account_name);
        accountName.setTextSize(16);
        accountName.setText(R.string.label_balance_sheet_total);
        TextView accountBalance = (TextView) totalView.findViewById(R.id.account_balance);
        accountBalance.setTextSize(16);
        accountBalance.setTypeface(null, Typeface.BOLD);
        TransactionsActivity.displayBalance(accountBalance, mAccountsDbAdapter.getAccountBalance(accountTypes, -1, System.currentTimeMillis()));

        tableLayout.addView(totalView);
    }

}
