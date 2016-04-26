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

import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.transaction.TransactionsActivity;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.github.mikephil.charting.components.Legend.LegendPosition;

/**
 * Shows a summary of reports
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ReportSummaryFragment extends Fragment {

    public static final int LEGEND_TEXT_SIZE = 14;

    @Bind(R.id.btn_pie_chart) Button mPieChartButton;
    @Bind(R.id.btn_bar_chart) Button mBarChartButton;
    @Bind(R.id.btn_line_chart) Button mLineChartButton;
    @Bind(R.id.btn_balance_sheet) Button mBalanceSheetButton;

    @Bind(R.id.pie_chart) PieChart mChart;
    @Bind(R.id.total_assets) TextView mTotalAssets;
    @Bind(R.id.total_liabilities) TextView mTotalLiabilities;
    @Bind(R.id.net_worth) TextView mNetWorth;

    private AccountsDbAdapter mAccountsDbAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAccountsDbAdapter = AccountsDbAdapter.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report_summary, container, false);
        ButterKnife.bind(this, view);

        mPieChartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadFragment(new PieChartFragment());
            }
        });

        mLineChartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadFragment(new LineChartFragment());
            }
        });

        mBarChartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadFragment(new BarChartFragment());
            }
        });

        mBalanceSheetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadFragment(new BalanceSheetFragment());
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.title_reports);
        ((ReportsActivity)getActivity()).setAppBarColor(R.color.theme_primary);

        getActivity().findViewById(R.id.time_range_layout).setVisibility(View.GONE);
        getActivity().findViewById(R.id.date_range_divider).setVisibility(View.GONE);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        mChart.setCenterTextSize(PieChartFragment.CENTER_TEXT_SIZE);
        mChart.setDescription("");
        mChart.setDrawSliceText(false);
        mChart.getLegend().setEnabled(true);
        mChart.getLegend().setWordWrapEnabled(true);
        mChart.getLegend().setForm(LegendForm.CIRCLE);
        mChart.getLegend().setPosition(LegendPosition.RIGHT_OF_CHART_CENTER);
        mChart.getLegend().setTextSize(LEGEND_TEXT_SIZE);

        ColorStateList csl = new ColorStateList(new int[][]{new int[0]}, new int[]{getResources().getColor(R.color.account_green)});
        setButtonTint(mPieChartButton, csl);
        csl = new ColorStateList(new int[][]{new int[0]}, new int[]{getResources().getColor(R.color.account_red)});
        setButtonTint(mBarChartButton, csl);
        csl = new ColorStateList(new int[][]{new int[0]}, new int[]{getResources().getColor(R.color.account_blue)});
        setButtonTint(mLineChartButton, csl);
        csl = new ColorStateList(new int[][]{new int[0]}, new int[]{getResources().getColor(R.color.account_purple)});
        setButtonTint(mBalanceSheetButton, csl);


        List<AccountType> accountTypes = new ArrayList<>();
        accountTypes.add(AccountType.ASSET);
        accountTypes.add(AccountType.CASH);
        accountTypes.add(AccountType.BANK);
        Money assetsBalance = mAccountsDbAdapter.getAccountBalance(accountTypes, -1, System.currentTimeMillis());

        accountTypes.clear();
        accountTypes.add(AccountType.LIABILITY);
        accountTypes.add(AccountType.CREDIT);
        Money liabilitiesBalance = mAccountsDbAdapter.getAccountBalance(accountTypes, -1, System.currentTimeMillis());

        TransactionsActivity.displayBalance(mTotalAssets, assetsBalance);
        TransactionsActivity.displayBalance(mTotalLiabilities, liabilitiesBalance);
        TransactionsActivity.displayBalance(mNetWorth, assetsBalance.subtract(liabilitiesBalance));

        displayChart();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_group_reports_by).setVisible(false);
    }

    /**
     * Returns {@code PieData} instance with data entries, colors and labels
     * @return {@code PieData} instance
     */
    private PieData getData() {
        String mCurrencyCode = GnuCashApplication.getDefaultCurrencyCode();
        PieDataSet dataSet = new PieDataSet(null, "");
        List<String> labels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (Account account : mAccountsDbAdapter.getSimpleAccountList()) {
            if (account.getAccountType() == AccountType.EXPENSE
                    && !account.isPlaceholderAccount()
                    && account.getCurrency() == Currency.getInstance(mCurrencyCode)) {

                long start = new LocalDate().minusMonths(2).dayOfMonth().withMinimumValue().toDate().getTime();
                long end = new LocalDate().plusDays(1).toDate().getTime();
                double balance = mAccountsDbAdapter.getAccountsBalance(
                        Collections.singletonList(account.getUID()), start, end).asDouble();
                if (balance > 0) {
                    dataSet.addEntry(new Entry((float) balance, dataSet.getEntryCount()));
                    colors.add(account.getColor() != Account.DEFAULT_COLOR
                            ? account.getColor()
                            : ReportsActivity.COLORS[(dataSet.getEntryCount() - 1) % ReportsActivity.COLORS.length]);
                    labels.add(account.getName());
                }
            }
        }
        dataSet.setColors(colors);
        dataSet.setSliceSpace(PieChartFragment.SPACE_BETWEEN_SLICES);
        return new PieData(labels, dataSet);
    }

    /**
     * Manages all actions about displaying the pie chart
     */
    private void displayChart() {
        mChart.highlightValues(null);
        mChart.clear();

        PieData pieData = PieChartFragment.groupSmallerSlices(getData(), getActivity());
        if (pieData != null && pieData.getYValCount() != 0) {
            mChart.setData(pieData);
            float sum = mChart.getData().getYValueSum();
            String total = getResources().getString(R.string.label_chart_total);
            String currencySymbol = Currency.getInstance(GnuCashApplication.getDefaultCurrencyCode()).getSymbol(Locale.getDefault());
            mChart.setCenterText(String.format(PieChartFragment.TOTAL_VALUE_LABEL_PATTERN, total, sum, currencySymbol));
            mChart.animateXY(1800, 1800);
            mChart.setTouchEnabled(true);
        } else {
            mChart.setData(getEmptyData());
            mChart.setCenterText(getResources().getString(R.string.label_chart_no_data));
            mChart.getLegend().setEnabled(false);
            mChart.setTouchEnabled(false);
        }

        mChart.invalidate();
    }

    /**
     * Returns a data object that represents situation when no user data available
     * @return a {@code PieData} instance for situation when no user data available
     */
    private PieData getEmptyData() {
        PieDataSet dataSet = new PieDataSet(null, getResources().getString(R.string.label_chart_no_data));
        dataSet.addEntry(new Entry(1, 0));
        dataSet.setColor(PieChartFragment.NO_DATA_COLOR);
        dataSet.setDrawValues(false);
        return new PieData(Collections.singletonList(""), dataSet);
    }


    public void setButtonTint(Button button, ColorStateList tint) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP && button instanceof AppCompatButton) {
            ((AppCompatButton) button).setSupportBackgroundTintList(tint);
        } else {
            ViewCompat.setBackgroundTintList(button, tint);
        }
        button.setTextColor(getResources().getColor(android.R.color.white));
    }

    private void loadFragment(Fragment fragment){
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}
