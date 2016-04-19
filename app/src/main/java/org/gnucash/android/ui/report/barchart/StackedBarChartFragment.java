/*
 * Copyright (c) 2015 Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
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

package org.gnucash.android.ui.report.barchart;

import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.LargeValueFormatter;

import org.gnucash.android.R;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.ui.report.BaseReportFragment;
import org.gnucash.android.ui.report.ReportType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.Bind;

import static org.gnucash.android.ui.report.ReportsActivity.COLORS;

/**
 * Activity used for drawing a bar chart
 *
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class StackedBarChartFragment extends BaseReportFragment {

    private static final String X_AXIS_MONTH_PATTERN = "MMM YY";
    private static final String X_AXIS_QUARTER_PATTERN = "Q%d %s";
    private static final String X_AXIS_YEAR_PATTERN = "YYYY";

    private static final int ANIMATION_DURATION = 2000;
    private static final int NO_DATA_BAR_COUNTS = 3;

    private AccountsDbAdapter mAccountsDbAdapter = AccountsDbAdapter.getInstance();

    @Bind(R.id.bar_chart) BarChart mChart;

    private boolean mUseAccountColor = true;
    private boolean mTotalPercentageMode = true;
    private boolean mChartDataPresent = true;

    @Override
    public int getTitle() {
        return R.string.title_cash_flow_report;
    }

    @Override
    public int getLayoutResource() {
        return R.layout.fragment_bar_chart;
    }

    @Override
    public ReportType getReportType() {
        return ReportType.BAR_CHART;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mUseAccountColor = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getBoolean(getString(R.string.key_use_account_color), false);

        mChart.setOnChartValueSelectedListener(this);
        mChart.setDescription("");
//        mChart.setDrawValuesForWholeStack(false);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.getAxisRight().setEnabled(false);
        mChart.getAxisLeft().setStartAtZero(false);
        mChart.getAxisLeft().enableGridDashedLine(4.0f, 4.0f, 0);
        mChart.getAxisLeft().setValueFormatter(new LargeValueFormatter(mCurrency.getSymbol(Locale.getDefault())));
        Legend chartLegend = mChart.getLegend();
        chartLegend.setForm(Legend.LegendForm.CIRCLE);
        chartLegend.setPosition(Legend.LegendPosition.BELOW_CHART_CENTER);
        chartLegend.setWordWrapEnabled(true);

    }


    /**
     * Returns a data object that represents a user data of the specified account types
     * @return a {@code BarData} instance that represents a user data
     */
    protected BarData getData() {
        List<BarEntry> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        Map<String, Integer> accountToColorMap = new LinkedHashMap<>();
        List<String> xValues = new ArrayList<>();
        LocalDateTime tmpDate = new LocalDateTime(getStartDate(mAccountType).toDate().getTime());
        int count = getDateDiff(new LocalDateTime(getStartDate(mAccountType).toDate().getTime()),
                new LocalDateTime(getEndDate(mAccountType).toDate().getTime()));
        for (int i = 0; i <= count; i++) {
            long start = 0;
            long end = 0;
            switch (mGroupInterval) {
                case MONTH:
                    start = tmpDate.dayOfMonth().withMinimumValue().millisOfDay().withMinimumValue().toDate().getTime();
                    end = tmpDate.dayOfMonth().withMaximumValue().millisOfDay().withMaximumValue().toDate().getTime();

                    xValues.add(tmpDate.toString(X_AXIS_MONTH_PATTERN));
                    tmpDate = tmpDate.plusMonths(1);
                    break;
                case QUARTER:
                    int quarter = getQuarter(tmpDate);
                    start = tmpDate.withMonthOfYear(quarter * 3 - 2).dayOfMonth().withMinimumValue().millisOfDay().withMinimumValue().toDate().getTime();
                    end = tmpDate.withMonthOfYear(quarter * 3).dayOfMonth().withMaximumValue().millisOfDay().withMaximumValue().toDate().getTime();

                    xValues.add(String.format(X_AXIS_QUARTER_PATTERN, quarter, tmpDate.toString(" YY")));
                    tmpDate = tmpDate.plusMonths(3);
                    break;
                case YEAR:
                    start = tmpDate.dayOfYear().withMinimumValue().millisOfDay().withMinimumValue().toDate().getTime();
                    end = tmpDate.dayOfYear().withMaximumValue().millisOfDay().withMaximumValue().toDate().getTime();

                    xValues.add(tmpDate.toString(X_AXIS_YEAR_PATTERN));
                    tmpDate = tmpDate.plusYears(1);
                    break;
            }
            List<Float> stack = new ArrayList<>();
            for (Account account : mAccountsDbAdapter.getSimpleAccountList()) {
                if (account.getAccountType() == mAccountType
                        && !account.isPlaceholderAccount()
                        && account.getCurrency() == mCurrency) {

                    double balance = mAccountsDbAdapter.getAccountsBalance(
                            Collections.singletonList(account.getUID()), start, end).asDouble();
                    if (balance != 0) {
                        stack.add((float) balance);

                        String accountName = account.getName();
                        while (labels.contains(accountName)) {
                            if (!accountToColorMap.containsKey(account.getUID())) {
                                for (String label : labels) {
                                    if (label.equals(accountName)) {
                                        accountName += " ";
                                    }
                                }
                            } else {
                                break;
                            }
                        }
                        labels.add(accountName);

                        if (!accountToColorMap.containsKey(account.getUID())) {
                            Integer color;
                            if (mUseAccountColor) {
                                color = account.getColor();
                            } else {
                                color = COLORS[accountToColorMap.size() % COLORS.length];
                            }
                            accountToColorMap.put(account.getUID(), color);
                        }
                        colors.add(accountToColorMap.get(account.getUID()));

                        Log.d(TAG, mAccountType + tmpDate.toString(" MMMM yyyy ") + account.getName() + " = " + stack.get(stack.size() - 1));
                    }
                }
            }

            String stackLabels = labels.subList(labels.size() - stack.size(), labels.size()).toString();
            values.add(new BarEntry(floatListToArray(stack), i, stackLabels));
        }

        BarDataSet set = new BarDataSet(values, "");
        set.setDrawValues(false);
        set.setStackLabels(labels.toArray(new String[labels.size()]));
        set.setColors(colors);

        if (set.getYValueSum() == 0) {
            mChartDataPresent = false;
            return getEmptyData();
        }
        mChartDataPresent = true;
        return new BarData(xValues, set);
    }

    /**
     * Returns a data object that represents situation when no user data available
     * @return a {@code BarData} instance for situation when no user data available
     */
    private BarData getEmptyData() {
        List<String> xValues = new ArrayList<>();
        List<BarEntry> yValues = new ArrayList<>();
        for (int i = 0; i < NO_DATA_BAR_COUNTS; i++) {
            xValues.add("");
            yValues.add(new BarEntry(i + 1, i));
        }
        BarDataSet set = new BarDataSet(yValues, getResources().getString(R.string.label_chart_no_data));
        set.setDrawValues(false);
        set.setColor(NO_DATA_COLOR);

        return new BarData(xValues, set);
    }

    /**
     * Returns the start data of x-axis for the specified account type
     * @param accountType account type
     * @return the start data
     */
    private LocalDate getStartDate(AccountType accountType) {
        TransactionsDbAdapter adapter = TransactionsDbAdapter.getInstance();
        String code = mCurrency.getCurrencyCode();
        LocalDate startDate;
        if (mReportPeriodStart == -1) {
            startDate = new LocalDate(adapter.getTimestampOfEarliestTransaction(accountType, code));
        } else {
            startDate = new LocalDate(mReportPeriodStart);
        }
        startDate = startDate.withDayOfMonth(1);
        Log.d(TAG, accountType + " X-axis star date: " + startDate.toString("dd MM yyyy"));
        return startDate;
    }

    /**
     * Returns the end data of x-axis for the specified account type
     * @param accountType account type
     * @return the end data
     */
    private LocalDate getEndDate(AccountType accountType) {
        TransactionsDbAdapter adapter = TransactionsDbAdapter.getInstance();
        String code = mCurrency.getCurrencyCode();
        LocalDate endDate;
        if (mReportPeriodEnd == -1) {
            endDate = new LocalDate(adapter.getTimestampOfLatestTransaction(accountType, code));
        } else {
            endDate = new LocalDate(mReportPeriodEnd);
        }
        endDate = endDate.withDayOfMonth(1);
        Log.d(TAG, accountType + " X-axis end date: " + endDate.toString("dd MM yyyy"));
        return endDate;
    }

    /**
     * Converts the specified list of floats to an array
     * @param list a list of floats
     * @return a float array
     */
    private float[] floatListToArray(List<Float> list) {
        float array[] = new float[list.size()];
        for (int i = 0;  i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }


    @Override
    public void generateReport() {
        mChart.setData(getData());
        setCustomLegend();

        mChart.getAxisLeft().setDrawLabels(mChartDataPresent);
        mChart.getXAxis().setDrawLabels(mChartDataPresent);
        mChart.setTouchEnabled(mChartDataPresent);
    }

    @Override
    protected void displayReport() {
        mChart.notifyDataSetChanged();
        mChart.highlightValues(null);
        if (mChartDataPresent) {
            mChart.animateY(ANIMATION_DURATION);
        } else {
            mChart.clearAnimation();
            mSelectedValueTextView.setText(R.string.label_chart_no_data);
        }

        mChart.invalidate();
    }

    /**
     * Sets custom legend. Disable legend if its items count greater than {@code COLORS} array size.
     */
    private void setCustomLegend() {
        Legend legend = mChart.getLegend();
        BarDataSet dataSet = mChart.getData().getDataSetByIndex(0);

        LinkedHashSet<String> labels = new LinkedHashSet<>(Arrays.asList(dataSet.getStackLabels()));
        LinkedHashSet<Integer> colors = new LinkedHashSet<>(dataSet.getColors());

        if (COLORS.length >= labels.size()) {
            legend.setCustom(new ArrayList<>(colors), new ArrayList<>(labels));
            return;
        }
        legend.setEnabled(false);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_percentage_mode).setVisible(mChartDataPresent);
        // hide pie/line chart specific menu items
        menu.findItem(R.id.menu_order_by_size).setVisible(false);
        menu.findItem(R.id.menu_toggle_labels).setVisible(false);
        menu.findItem(R.id.menu_toggle_average_lines).setVisible(false);
        menu.findItem(R.id.menu_group_other_slice).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.isCheckable())
            item.setChecked(!item.isChecked());
        switch (item.getItemId()) {
            case R.id.menu_toggle_legend:
                Legend legend = mChart.getLegend();
                if (!legend.isLegendCustom()) {
                    Toast.makeText(getActivity(), R.string.toast_legend_too_long, Toast.LENGTH_LONG).show();
                    item.setChecked(false);
                } else {
                    item.setChecked(!mChart.getLegend().isEnabled());
                    legend.setEnabled(!mChart.getLegend().isEnabled());
                    mChart.invalidate();
                }
                return true;

            case R.id.menu_percentage_mode:
                mTotalPercentageMode = !mTotalPercentageMode;
                int msgId = mTotalPercentageMode ? R.string.toast_chart_percentage_mode_total
                        : R.string.toast_chart_percentage_mode_current_bar;
                Toast.makeText(getActivity(), msgId, Toast.LENGTH_LONG).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
        if (e == null || ((BarEntry) e).getVals().length == 0) return;
        BarEntry entry = (BarEntry) e;
        int index = h.getStackIndex() == -1 ? 0 : h.getStackIndex();
        String stackLabels = entry.getData().toString();
        String label = mChart.getData().getXVals().get(entry.getXIndex()) + ", "
                + stackLabels.substring(1, stackLabels.length() - 1).split(",")[index];
        double value = Math.abs(entry.getVals()[index]);
        double sum = 0;
        if (mTotalPercentageMode) {
            for (BarEntry barEntry : mChart.getData().getDataSetByIndex(dataSetIndex).getYVals()) {
                sum += barEntry.getNegativeSum() + barEntry.getPositiveSum();
            }
        } else {
            sum = entry.getNegativeSum() + entry.getPositiveSum();
        }
        mSelectedValueTextView.setText(String.format(SELECTED_VALUE_PATTERN, label.trim(), value, value / sum * 100));
    }

}
