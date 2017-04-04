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
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatButton;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;

import org.gnucash.android.R;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.report.barchart.StackedBarChartFragment;
import org.gnucash.android.ui.report.linechart.CashFlowLineChartFragment;
import org.gnucash.android.ui.report.piechart.PieChartFragment;
import org.gnucash.android.ui.report.sheet.BalanceSheetFragment;
import org.gnucash.android.ui.transaction.TransactionsActivity;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

import static com.github.mikephil.charting.components.Legend.LegendPosition;

/**
 * Shows a summary of reports
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ReportsOverviewFragment extends BaseReportFragment {

    public static final int LEGEND_TEXT_SIZE = 14;

    @BindView(R.id.btn_pie_chart) Button mPieChartButton;
    @BindView(R.id.btn_bar_chart) Button mBarChartButton;
    @BindView(R.id.btn_line_chart) Button mLineChartButton;
    @BindView(R.id.btn_balance_sheet) Button mBalanceSheetButton;

    @BindView(R.id.pie_chart) PieChart mChart;
    @BindView(R.id.total_assets) TextView mTotalAssets;
    @BindView(R.id.total_liabilities) TextView mTotalLiabilities;
    @BindView(R.id.net_worth) TextView mNetWorth;

    private AccountsDbAdapter mAccountsDbAdapter;
    private Money mAssetsBalance;
    private Money mLiabilitiesBalance;

    private boolean mChartHasData = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAccountsDbAdapter = AccountsDbAdapter.getInstance();
    }

    @Override
    public int getLayoutResource() {
        return R.layout.fragment_report_summary;
    }

    @Override
    public int getTitle() {
        return R.string.title_reports;
    }

    @Override
    public ReportType getReportType() {
        return ReportType.NONE;
    }

    @Override
    public boolean requiresAccountTypeOptions() {
        return false;
    }

    @Override
    public boolean requiresTimeRangeOptions() {
        return false;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(false);

        mChart.setCenterTextSize(PieChartFragment.CENTER_TEXT_SIZE);
        mChart.setDescription("");
        mChart.setDrawSliceText(false);
        Legend legend = mChart.getLegend();
        legend.setEnabled(true);
        legend.setWordWrapEnabled(true);
        legend.setForm(LegendForm.CIRCLE);
        legend.setPosition(LegendPosition.RIGHT_OF_CHART_CENTER);
        legend.setTextSize(LEGEND_TEXT_SIZE);

        ColorStateList csl = new ColorStateList(new int[][]{new int[0]}, new int[]{getResources().getColor(R.color.account_green)});
        setButtonTint(mPieChartButton, csl);
        csl = new ColorStateList(new int[][]{new int[0]}, new int[]{getResources().getColor(R.color.account_red)});
        setButtonTint(mBarChartButton, csl);
        csl = new ColorStateList(new int[][]{new int[0]}, new int[]{getResources().getColor(R.color.account_blue)});
        setButtonTint(mLineChartButton, csl);
        csl = new ColorStateList(new int[][]{new int[0]}, new int[]{getResources().getColor(R.color.account_purple)});
        setButtonTint(mBalanceSheetButton, csl);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_group_reports_by).setVisible(false);
    }

    @Override
    protected void generateReport() {
        PieData pieData = PieChartFragment.groupSmallerSlices(getData(), getActivity());
        if (pieData != null && pieData.getYValCount() != 0) {
            mChart.setData(pieData);
            float sum = mChart.getData().getYValueSum();
            String total = getResources().getString(R.string.label_chart_total);
            String currencySymbol = mCommodity.getSymbol();
            mChart.setCenterText(String.format(PieChartFragment.TOTAL_VALUE_LABEL_PATTERN, total, sum, currencySymbol));
            mChartHasData = true;
        } else {
            mChart.setData(getEmptyData());
            mChart.setCenterText(getResources().getString(R.string.label_chart_no_data));
            mChart.getLegend().setEnabled(false);
            mChartHasData = false;
        }

        List<AccountType> accountTypes = new ArrayList<>();
        accountTypes.add(AccountType.ASSET);
        accountTypes.add(AccountType.CASH);
        accountTypes.add(AccountType.BANK);
        mAssetsBalance = mAccountsDbAdapter.getAccountBalance(accountTypes, -1, System.currentTimeMillis());

        accountTypes.clear();
        accountTypes.add(AccountType.LIABILITY);
        accountTypes.add(AccountType.CREDIT);
        mLiabilitiesBalance = mAccountsDbAdapter.getAccountBalance(accountTypes, -1, System.currentTimeMillis());
    }

    /**
     * Returns {@code PieData} instance with data entries, colors and labels
     * @return {@code PieData} instance
     */
    private PieData getData() {
        PieDataSet dataSet = new PieDataSet(null, "");
        List<String> labels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (Account account : mAccountsDbAdapter.getSimpleAccountList()) {
            if (account.getAccountType() == AccountType.EXPENSE
                    && !account.isPlaceholderAccount()
                    && account.getCommodity().equals(mCommodity)) {

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

    @Override
    protected void displayReport() {
        if (mChartHasData){
            mChart.animateXY(1800, 1800);
            mChart.setTouchEnabled(true);
        } else {
            mChart.setTouchEnabled(false);
        }
        mChart.highlightValues(null);
        mChart.invalidate();

        TransactionsActivity.displayBalance(mTotalAssets, mAssetsBalance);
        TransactionsActivity.displayBalance(mTotalLiabilities, mLiabilitiesBalance);
        TransactionsActivity.displayBalance(mNetWorth, mAssetsBalance.subtract(mLiabilitiesBalance));
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

    @OnClick({R.id.btn_bar_chart, R.id.btn_pie_chart, R.id.btn_line_chart, R.id.btn_balance_sheet})
    public void onClickChartTypeButton(View view){
        BaseReportFragment fragment;
        switch (view.getId()){
            case R.id.btn_pie_chart:
                fragment = new PieChartFragment();
                break;
            case R.id.btn_bar_chart:
                fragment = new StackedBarChartFragment();
                break;
            case R.id.btn_line_chart:
                fragment = new CashFlowLineChartFragment();
                break;
            case R.id.btn_balance_sheet:
                fragment = new BalanceSheetFragment();
                break;
            default:
                fragment = this;
                break;
        }
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void setButtonTint(Button button, ColorStateList tint) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP && button instanceof AppCompatButton) {
            ((AppCompatButton) button).setSupportBackgroundTintList(tint);
        } else {
            ViewCompat.setBackgroundTintList(button, tint);
        }
        button.setTextColor(getResources().getColor(android.R.color.white));
    }

}
