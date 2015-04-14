/*
 * Copyright (c) 2015 Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
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

package org.gnucash.android.ui.chart;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Highlight;
import com.github.mikephil.charting.utils.LargeValueFormatter;

import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.ui.passcode.PassLockActivity;
import org.joda.time.LocalDateTime;
import org.joda.time.Months;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity used for drawing a bar chart
 *
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class BarChartActivity extends PassLockActivity implements OnChartValueSelectedListener {

    private static final String TAG = "BarChartActivity";
    private static final String X_AXIS_PATTERN = "MMM YY";
    private static final String SELECTED_VALUE_PATTERN = "%s - %.2f (%.2f %%)";
    private static final int ANIMATION_DURATION = 3000;
    private static final int NO_DATA_COLOR = Color.LTGRAY;
    private static final int NO_DATA_BAR_COUNTS = 3;
    private static final int[] COLORS = {
            Color.parseColor("#68F1AF"), Color.parseColor("#CC1f09"), Color.parseColor("#EE8600"),
            Color.parseColor("#1469EB"), Color.parseColor("#B304AD"),
    };

    private BarChart mChart;
    private AccountsDbAdapter mAccountsDbAdapter = AccountsDbAdapter.getInstance();
    private Map<AccountType, Long> mEarliestTimestampsMap = new HashMap<AccountType, Long>();
    private Map<AccountType, Long> mLatestTimestampsMap = new HashMap<AccountType, Long>();
    private long mEarliestTransactionTimestamp;
    private long mLatestTransactionTimestamp;
    private boolean mTotalPercentageMode = true;
    private boolean mChartDataPresent = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //it is necessary to set the view first before calling super because of the nav drawer in BaseDrawerActivity
        setContentView(R.layout.activity_line_chart);
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(R.string.title_bar_chart);

        mChart = new BarChart(this);
        ((LinearLayout) findViewById(R.id.chart)).addView(mChart);
        mChart.setOnChartValueSelectedListener(this);
        mChart.setDescription("");
        mChart.setDrawValuesForWholeStack(false);
        mChart.setDrawBarShadow(false);
        mChart.getAxisLeft().setValueFormatter(new LargeValueFormatter());
        mChart.getAxisRight().setEnabled(false);

        // below we can add/remove displayed account's types
        mChart.setData(getData(new ArrayList<AccountType>(Arrays.asList(AccountType.INCOME, AccountType.EXPENSE))));

        Legend legend = mChart.getLegend();
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setPosition(Legend.LegendPosition.RIGHT_OF_CHART_INSIDE);

        if (!mChartDataPresent) {
            mChart.getAxisLeft().setAxisMaxValue(10);
            mChart.getAxisLeft().setDrawLabels(false);
            mChart.getXAxis().setDrawLabels(false);
            mChart.setTouchEnabled(false);
            ((TextView) findViewById(R.id.selected_chart_slice)).setText(getResources().getString(R.string.label_chart_no_data));
        } else {
            mChart.animateY(ANIMATION_DURATION);
        }
        mChart.invalidate();
    }

    /**
     * Returns a data object that represents a user data of the specified account types
     * @param accountTypeList account's types which will be displayed
     * @return a {@code BarData} instance that represents a user data
     */
    private BarData getData(ArrayList<AccountType> accountTypeList) {
        if (!calculateEarliestAndLatestTimestamps(accountTypeList)) {
            mChartDataPresent = false;
            return getEmptyData();
        }

        LocalDateTime startDate = new LocalDateTime(mEarliestTransactionTimestamp).withDayOfMonth(1).withMillisOfDay(0);
        LocalDateTime endDate = new LocalDateTime(mLatestTransactionTimestamp).withDayOfMonth(1).withMillisOfDay(0);
        Log.d(TAG, "X-axis star date: " + startDate.toString("dd MM yyyy"));
        Log.d(TAG, "X-axis end date: " + endDate.toString("dd MM yyyy"));
        int months = Months.monthsBetween(startDate, endDate).getMonths();

        ArrayList<BarDataSet> dataSets = new ArrayList<BarDataSet>();
        ArrayList<BarEntry> values = new ArrayList<BarEntry>();
        ArrayList<String> xValues = new ArrayList<String>();
        for (int i = 0; i <= months; i++) {
            xValues.add(startDate.toString(X_AXIS_PATTERN));

            long start = startDate.dayOfMonth().withMinimumValue().millisOfDay().withMinimumValue().toDate().getTime();
            long end = startDate.dayOfMonth().withMaximumValue().millisOfDay().withMaximumValue().toDate().getTime();
            float stack[] = new float[accountTypeList.size()];
            int j = 0;
            for (Map.Entry<AccountType, List<String>> entry : getAccountMap(accountTypeList).entrySet()) {
                stack[j++] = (float) mAccountsDbAdapter.getAccountsBalance(entry.getValue(), start, end).absolute().asDouble();
                Log.d(TAG, entry.getKey() + startDate.toString(" MMMM yyyy") + ", balance = " + stack[j - 1]);
            }
            values.add(new BarEntry(stack, i));

            startDate = startDate.plusMonths(1);
        }

        BarDataSet set = new BarDataSet(values, "");
        // conversion an enum list to a string array
        set.setStackLabels(accountTypeList.toString().substring(1, accountTypeList.toString().length() - 1).split(", "));
        set.setColors(Arrays.copyOfRange(COLORS, 0, accountTypeList.size()));
        dataSets.add(set);

        return new BarData(xValues, dataSets);
    }

    /**
     * Calculates the earliest and latest transaction's timestamps of the specified account types
     * @param accountTypeList account's types which will be processed
     * @return {@code false} if no data available, {@code true} otherwise
     */
    private boolean calculateEarliestAndLatestTimestamps(List<AccountType> accountTypeList) {
        for (AccountType type : accountTypeList) {
            long earliest = TransactionsDbAdapter.getInstance().getTimestampOfEarliestTransaction(type);
            long latest = TransactionsDbAdapter.getInstance().getTimestampOfLatestTransaction(type);
            if (earliest > 0 && latest > 0) {
                mEarliestTimestampsMap.put(type, earliest);
                mLatestTimestampsMap.put(type, latest);
            } else {
                accountTypeList.remove(type);
            }
        }

        if (mEarliestTimestampsMap.isEmpty() && mLatestTimestampsMap.isEmpty()) {
            return false;
        }

        List<Long> timestamps = new ArrayList<Long>(mEarliestTimestampsMap.values());
        timestamps.addAll(mLatestTimestampsMap.values());
        Collections.sort(timestamps);
        mEarliestTransactionTimestamp = timestamps.get(0);
        mLatestTransactionTimestamp = timestamps.get(timestamps.size() - 1);
        return true;
    }

    /**
     * Returns a map with an account type as key and correspond accounts UIDs as value
     * @param accountTypeList account's types which will be used as keys
     * @return
     */
    private Map<AccountType, List<String>> getAccountMap(List<AccountType> accountTypeList) {
        Map<AccountType, List<String>> accountMap = new HashMap<AccountType, List<String>>();
        for (AccountType accountType : accountTypeList) {
            List<String> accountUIDList = new ArrayList<String>();
            for (Account account : mAccountsDbAdapter.getSimpleAccountList()) {
                if (account.getAccountType() == accountType && !account.isPlaceholderAccount()) {
                    accountUIDList.add(account.getUID());
                }
                accountMap.put(accountType, accountUIDList);
            }
        }
        return accountMap;
    }

    /**
     * Returns a data object that represents situation when no user data available
     * @return a {@code BarData} instance for situation when no user data available
     */
    private BarData getEmptyData() {
        ArrayList<String> xValues = new ArrayList<String>();
        ArrayList<BarEntry> yValues = new ArrayList<BarEntry>();
        for (int i = 0; i < NO_DATA_BAR_COUNTS; i++) {
            xValues.add("");
            yValues.add(new BarEntry(i % 2 == 0 ? 5f : 4.5f, i));
        }
        BarDataSet set = new BarDataSet(yValues, getResources().getString(R.string.label_chart_no_data));
        set.setDrawValues(false);
        set.setColor(NO_DATA_COLOR);

        return new BarData(xValues, new ArrayList<BarDataSet>(Arrays.asList(set)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.chart_actions, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_percentage_mode).setVisible(mChartDataPresent);
        // hide pie/line chart specific menu items
        menu.findItem(R.id.menu_order_by_size).setVisible(false);
        menu.findItem(R.id.menu_toggle_labels).setVisible(false);
        menu.findItem(R.id.menu_toggle_average_lines).setVisible(false);
        menu.findItem(R.id.menu_group_other_slice).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_toggle_legend:
                mChart.getLegend().setEnabled(!mChart.getLegend().isEnabled());
                mChart.invalidate();
                break;

            case R.id.menu_percentage_mode:
                mTotalPercentageMode = !mTotalPercentageMode;
                int msgId = mTotalPercentageMode ? R.string.toast_chart_percentage_mode_total
                        : R.string.toast_chart_percentage_mode_current_bar;
                Toast.makeText(this, msgId, Toast.LENGTH_LONG).show();
                break;

            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
        if (e == null) return;
        BarEntry entry = (BarEntry) e;
        String label = mChart.getData().getXVals().get(entry.getXIndex());
        double value = entry.getVals()[ h.getStackIndex() == -1 ? 0 : h.getStackIndex() ];
        double sum = mTotalPercentageMode ? mChart.getData().getDataSetByIndex(dataSetIndex).getYValueSum() : entry.getVal();
        ((TextView) findViewById(R.id.selected_chart_slice))
                .setText(String.format(SELECTED_VALUE_PATTERN, label, value, value / sum * 100));
    }

    @Override
    public void onNothingSelected() {
        ((TextView) findViewById(R.id.selected_chart_slice)).setText("");
    }
}
