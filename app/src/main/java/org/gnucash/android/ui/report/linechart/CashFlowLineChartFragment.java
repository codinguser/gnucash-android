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

package org.gnucash.android.ui.report.linechart;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.LargeValueFormatter;

import org.gnucash.android.R;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.ui.report.BaseReportFragment;
import org.gnucash.android.ui.report.ReportType;
import org.gnucash.android.ui.report.ReportsActivity.GroupInterval;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.Bind;

/**
 * Fragment for line chart reports
 *
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class CashFlowLineChartFragment extends BaseReportFragment {

    private static final String X_AXIS_PATTERN = "MMM YY";
    private static final int ANIMATION_DURATION = 3000;
    private static final int NO_DATA_BAR_COUNTS = 5;
    private static final int[] COLORS = {
            Color.parseColor("#68F1AF"), Color.parseColor("#cc1f09"), Color.parseColor("#EE8600"),
            Color.parseColor("#1469EB"), Color.parseColor("#B304AD"),
    };
    private static final int[] FILL_COLORS = {
            Color.parseColor("#008000"), Color.parseColor("#FF0000"), Color.parseColor("#BE6B00"),
            Color.parseColor("#0065FF"), Color.parseColor("#8F038A"),
    };

    private AccountsDbAdapter mAccountsDbAdapter = AccountsDbAdapter.getInstance();
    private Map<AccountType, Long> mEarliestTimestampsMap = new HashMap<>();
    private Map<AccountType, Long> mLatestTimestampsMap = new HashMap<>();
    private long mEarliestTransactionTimestamp;
    private long mLatestTransactionTimestamp;
    private boolean mChartDataPresent = true;

    @Bind(R.id.line_chart) LineChart mChart;

    @Override
    public int getLayoutResource() {
        return R.layout.fragment_line_chart;
    }

    @Override
    public int getTitle() {
        return R.string.title_cash_flow_report;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mChart.setOnChartValueSelectedListener(this);
        mChart.setDescription("");
        mChart.getXAxis().setDrawGridLines(false);
        mChart.getAxisRight().setEnabled(false);
        mChart.getAxisLeft().enableGridDashedLine(4.0f, 4.0f, 0);
        mChart.getAxisLeft().setValueFormatter(new LargeValueFormatter(mCurrency.getSymbol(Locale.getDefault())));

        Legend legend = mChart.getLegend();
        legend.setPosition(Legend.LegendPosition.BELOW_CHART_CENTER);
        legend.setTextSize(16);
        legend.setForm(Legend.LegendForm.CIRCLE);

    }

    @Override
    public ReportType getReportType() {
        return ReportType.LINE_CHART;
    }

    /**
     * Returns a data object that represents a user data of the specified account types
     * @param accountTypeList account's types which will be displayed
     * @return a {@code LineData} instance that represents a user data
     */
    private LineData getData(List<AccountType> accountTypeList) {
        Log.w(TAG, "getData");
        calculateEarliestAndLatestTimestamps(accountTypeList);
        // LocalDateTime?
        LocalDate startDate;
        LocalDate endDate;
        if (mReportPeriodStart == -1 && mReportPeriodEnd == -1) {
            startDate = new LocalDate(mEarliestTransactionTimestamp).withDayOfMonth(1);
            endDate = new LocalDate(mLatestTransactionTimestamp).withDayOfMonth(1);
        } else {
            startDate = new LocalDate(mReportPeriodStart).withDayOfMonth(1);
            endDate = new LocalDate(mReportPeriodEnd).withDayOfMonth(1);
        }

        int count = getDateDiff(new LocalDateTime(startDate.toDate().getTime()), new LocalDateTime(endDate.toDate().getTime()));
        Log.d(TAG, "X-axis count" + count);
        List<String> xValues = new ArrayList<>();
        for (int i = 0; i <= count; i++) {
            switch (mGroupInterval) {
                case MONTH:
                    xValues.add(startDate.toString(X_AXIS_PATTERN));
                    Log.d(TAG, "X-axis " + startDate.toString("MM yy"));
                    startDate = startDate.plusMonths(1);
                    break;
                case QUARTER:
                    int quarter = getQuarter(new LocalDateTime(startDate.toDate().getTime()));
                    xValues.add("Q" + quarter + startDate.toString(" yy"));
                    Log.d(TAG, "X-axis " + "Q" + quarter + startDate.toString(" MM yy"));
                    startDate = startDate.plusMonths(3);
                    break;
                case YEAR:
                    xValues.add(startDate.toString("yyyy"));
                    Log.d(TAG, "X-axis " + startDate.toString("yyyy"));
                    startDate = startDate.plusYears(1);
                    break;
//                default:
            }
        }

        List<LineDataSet> dataSets = new ArrayList<>();
        for (AccountType accountType : accountTypeList) {
            LineDataSet set = new LineDataSet(getEntryList(accountType), accountType.toString());
            set.setDrawFilled(true);
            set.setLineWidth(2);
            set.setColor(COLORS[dataSets.size()]);
            set.setFillColor(FILL_COLORS[dataSets.size()]);

            dataSets.add(set);
        }

        LineData lineData = new LineData(xValues, dataSets);
        if (lineData.getYValueSum() == 0) {
            mChartDataPresent = false;
            return getEmptyData();
        }
        return lineData;
    }

    /**
     * Returns a data object that represents situation when no user data available
     * @return a {@code LineData} instance for situation when no user data available
     */
    private LineData getEmptyData() {
        List<String> xValues = new ArrayList<>();
        List<Entry> yValues = new ArrayList<>();
        for (int i = 0; i < NO_DATA_BAR_COUNTS; i++) {
            xValues.add("");
            yValues.add(new Entry(i % 2 == 0 ? 5f : 4.5f, i));
        }
        LineDataSet set = new LineDataSet(yValues, getResources().getString(R.string.label_chart_no_data));
        set.setDrawFilled(true);
        set.setDrawValues(false);
        set.setColor(NO_DATA_COLOR);
        set.setFillColor(NO_DATA_COLOR);

        return new LineData(xValues, Collections.singletonList(set));
    }

    /**
     * Returns entries which represent a user data of the specified account type
     * @param accountType account's type which user data will be processed
     * @return entries which represent a user data
     */
    private List<Entry> getEntryList(AccountType accountType) {
        List<String> accountUIDList = new ArrayList<>();
        for (Account account : mAccountsDbAdapter.getSimpleAccountList()) {
            if (account.getAccountType() == accountType
                    && !account.isPlaceholderAccount()
                    && account.getCurrency().equals(mCurrency)) {
                accountUIDList.add(account.getUID());
            }
        }

        LocalDateTime earliest;
        LocalDateTime latest;
        if (mReportPeriodStart == -1 && mReportPeriodEnd == -1) {
            earliest = new LocalDateTime(mEarliestTimestampsMap.get(accountType));
            latest = new LocalDateTime(mLatestTimestampsMap.get(accountType));
        } else {
            earliest = new LocalDateTime(mReportPeriodStart);
            latest = new LocalDateTime(mReportPeriodEnd);
        }
        Log.d(TAG, "Earliest " + accountType + " date " + earliest.toString("dd MM yyyy"));
        Log.d(TAG, "Latest " + accountType + " date " + latest.toString("dd MM yyyy"));

        int xAxisOffset = getDateDiff(new LocalDateTime(mEarliestTransactionTimestamp), earliest);
        int count = getDateDiff(earliest, latest);
        List<Entry> values = new ArrayList<>(count + 1);
        for (int i = 0; i <= count; i++) {
            long start = 0;
            long end = 0;
            switch (mGroupInterval) {
                case QUARTER:
                    int quarter = getQuarter(earliest);
                    start = earliest.withMonthOfYear(quarter * 3 - 2).dayOfMonth().withMinimumValue().millisOfDay().withMinimumValue().toDate().getTime();
                    end = earliest.withMonthOfYear(quarter * 3).dayOfMonth().withMaximumValue().millisOfDay().withMaximumValue().toDate().getTime();

                    earliest = earliest.plusMonths(3);
                    break;
                case MONTH:
                    start = earliest.dayOfMonth().withMinimumValue().millisOfDay().withMinimumValue().toDate().getTime();
                    end = earliest.dayOfMonth().withMaximumValue().millisOfDay().withMaximumValue().toDate().getTime();

                    earliest = earliest.plusMonths(1);
                    break;
                case YEAR:
                    start = earliest.dayOfYear().withMinimumValue().millisOfDay().withMinimumValue().toDate().getTime();
                    end = earliest.dayOfYear().withMaximumValue().millisOfDay().withMaximumValue().toDate().getTime();

                    earliest = earliest.plusYears(1);
                    break;
            }
            float balance = (float) mAccountsDbAdapter.getAccountsBalance(accountUIDList, start, end).asDouble();
            values.add(new Entry(balance, i + xAxisOffset));
            Log.d(TAG, accountType + earliest.toString(" MMM yyyy") + ", balance = " + balance);

        }

        return values;
    }

    /**
     * Calculates the earliest and latest transaction's timestamps of the specified account types
     * @param accountTypeList account's types which will be processed
     */
    private void calculateEarliestAndLatestTimestamps(List<AccountType> accountTypeList) {
        if (mReportPeriodStart != -1 && mReportPeriodEnd != -1) {
            mEarliestTransactionTimestamp = mReportPeriodStart;
            mLatestTransactionTimestamp = mReportPeriodEnd;
            return;
        }

        TransactionsDbAdapter dbAdapter = TransactionsDbAdapter.getInstance();
        for (Iterator<AccountType> iter = accountTypeList.iterator(); iter.hasNext();) {
            AccountType type = iter.next();
            long earliest = dbAdapter.getTimestampOfEarliestTransaction(type, mCurrency.getCurrencyCode());
            long latest = dbAdapter.getTimestampOfLatestTransaction(type, mCurrency.getCurrencyCode());
            if (earliest > 0 && latest > 0) {
                mEarliestTimestampsMap.put(type, earliest);
                mLatestTimestampsMap.put(type, latest);
            } else {
                iter.remove();
            }
        }

        if (mEarliestTimestampsMap.isEmpty() || mLatestTimestampsMap.isEmpty()) {
            return;
        }

        List<Long> timestamps = new ArrayList<>(mEarliestTimestampsMap.values());
        timestamps.addAll(mLatestTimestampsMap.values());
        Collections.sort(timestamps);
        mEarliestTransactionTimestamp = timestamps.get(0);
        mLatestTransactionTimestamp = timestamps.get(timestamps.size() - 1);
    }

    @Override
    public boolean requiresAccountTypeOptions() {
        return false;
    }

    @Override
    protected void generateReport() {
        LineData lineData = getData(new ArrayList<>(Arrays.asList(AccountType.INCOME, AccountType.EXPENSE)));
        if (lineData != null) {
            mChart.setData(lineData);
            mChartDataPresent = true;
        } else {
            mChartDataPresent = false;
        }
    }

    @Override
    protected void displayReport() {
        if (!mChartDataPresent) {
            mChart.getAxisLeft().setAxisMaxValue(10);
            mChart.getAxisLeft().setDrawLabels(false);
            mChart.getXAxis().setDrawLabels(false);
            mChart.setTouchEnabled(false);
            mSelectedValueTextView.setText(getResources().getString(R.string.label_chart_no_data));
        } else {
            mChart.animateX(ANIMATION_DURATION);
        }
        mChart.invalidate();
    }

    @Override
    public void onTimeRangeUpdated(long start, long end) {
        if (mReportPeriodStart != start || mReportPeriodEnd != end) {
            mReportPeriodStart = start;
            mReportPeriodEnd = end;
            mChart.setData(getData(new ArrayList<>(Arrays.asList(AccountType.INCOME, AccountType.EXPENSE))));
            mChart.invalidate();
        }
    }

    @Override
    public void onGroupingUpdated(GroupInterval groupInterval) {
        if (mGroupInterval != groupInterval) {
            mGroupInterval = groupInterval;
            mChart.setData(getData(new ArrayList<>(Arrays.asList(AccountType.INCOME, AccountType.EXPENSE))));
            mChart.invalidate();
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_toggle_average_lines).setVisible(mChartDataPresent);
        // hide pie/bar chart specific menu items
        menu.findItem(R.id.menu_order_by_size).setVisible(false);
        menu.findItem(R.id.menu_toggle_labels).setVisible(false);
        menu.findItem(R.id.menu_percentage_mode).setVisible(false);
        menu.findItem(R.id.menu_group_other_slice).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.isCheckable())
            item.setChecked(!item.isChecked());
        switch (item.getItemId()) {
            case R.id.menu_toggle_legend:
                mChart.getLegend().setEnabled(!mChart.getLegend().isEnabled());
                mChart.invalidate();
                return true;

            case R.id.menu_toggle_average_lines:
                if (mChart.getAxisLeft().getLimitLines().isEmpty()) {
                    for (LineDataSet set : mChart.getData().getDataSets()) {
                        LimitLine line = new LimitLine(set.getYValueSum() / set.getEntryCount(), set.getLabel());
                        line.enableDashedLine(10, 5, 0);
                        line.setLineColor(set.getColor());
                        mChart.getAxisLeft().addLimitLine(line);
                    }
                } else {
                    mChart.getAxisLeft().removeAllLimitLines();
                }
                mChart.invalidate();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
        if (e == null) return;
        String label = mChart.getData().getXVals().get(e.getXIndex());
        double value = e.getVal();
        double sum = mChart.getData().getDataSetByIndex(dataSetIndex).getYValueSum();
        mSelectedValueTextView.setText(String.format(SELECTED_VALUE_PATTERN, label, value, value / sum * 100));
    }

}
