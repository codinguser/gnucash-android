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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.passcode.PassLockActivity;
import org.joda.time.LocalDateTime;
import org.joda.time.Months;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
            Color.parseColor("#17ee4e"), Color.parseColor("#cc1f09"), Color.parseColor("#3940f7"),
            Color.parseColor("#f9cd04"), Color.parseColor("#5f33a8"), Color.parseColor("#e005b6"),
            Color.parseColor("#17d6ed"), Color.parseColor("#e4a9a2"), Color.parseColor("#8fe6cd"),
            Color.parseColor("#8b48fb"), Color.parseColor("#343a36"), Color.parseColor("#6decb1"),
            Color.parseColor("#a6dcfd"), Color.parseColor("#5c3378"), Color.parseColor("#a6dcfd"),
            Color.parseColor("#ba037c"), Color.parseColor("#708809"), Color.parseColor("#32072c"),
            Color.parseColor("#fddef8"), Color.parseColor("#fa0e6e"), Color.parseColor("#d9e7b5")
    };

    private BarChart mChart;
    private AccountsDbAdapter mAccountsDbAdapter = AccountsDbAdapter.getInstance();
    private Map<AccountType, Long> mEarliestTimestampsMap = new HashMap<>();
    private Map<AccountType, Long> mLatestTimestampsMap = new HashMap<>();
    private long mEarliestTransactionTimestamp;
    private long mLatestTransactionTimestamp;
    private boolean mTotalPercentageMode = true;
    private boolean mChartDataPresent = true;
    private Currency mCurrency;

    private Set<String> mLegendLabels;
    private Set<Integer> mLegendColors;

    private AccountType mAccountType = AccountType.EXPENSE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //it is necessary to set the view first before calling super because of the nav drawer in BaseDrawerActivity
        setContentView(R.layout.activity_bar_chart);
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(R.string.title_bar_chart);

        mCurrency = Currency.getInstance(PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.key_report_currency), Money.DEFAULT_CURRENCY_CODE));

        mChart = new BarChart(this);
        ((LinearLayout) findViewById(R.id.bar_chart)).addView(mChart);
        mChart.setOnChartValueSelectedListener(this);
        mChart.setDescription("");
        mChart.getXAxis().setDrawGridLines(false);
        mChart.getAxisRight().setEnabled(false);
        mChart.getAxisLeft().enableGridDashedLine(4.0f, 4.0f, 0);
        mChart.setDrawValuesForWholeStack(false);
        mChart.getAxisLeft().setValueFormatter(new LargeValueFormatter(mCurrency.getSymbol(Locale.getDefault())));
        mChart.getAxisRight().setEnabled(false);
        mChart.getLegend().setEnabled(false);
        mChart.getLegend().setForm(Legend.LegendForm.CIRCLE);
        mChart.getLegend().setPosition(Legend.LegendPosition.RIGHT_OF_CHART_INSIDE);

        setUpSpinner();
    }

    /**
     * Returns a data object that represents a user data of the specified account types
     * @param accountTypeList account's types which will be displayed
     * @return a {@code BarData} instance that represents a user data
     */
    private BarData getData(List<AccountType> accountTypeList) {
        calculateEarliestAndLatestTimestamps(accountTypeList);

        LocalDateTime startDate = new LocalDateTime(mEarliestTransactionTimestamp).withDayOfMonth(1).withMillisOfDay(0);
        LocalDateTime endDate = new LocalDateTime(mLatestTransactionTimestamp).withDayOfMonth(1).withMillisOfDay(0);
        Log.d(TAG, "X-axis star date: " + startDate.toString("dd MM yyyy"));
        Log.d(TAG, "X-axis end date: " + endDate.toString("dd MM yyyy"));
        int months = Months.monthsBetween(startDate, endDate).getMonths();

        List<BarDataSet> dataSets = new ArrayList<>();
        List<BarEntry> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        List<String> xValues = new ArrayList<>();
        for (int i = 0; i <= months; i++) {
            xValues.add(startDate.toString(X_AXIS_PATTERN));

            long start = startDate.dayOfMonth().withMinimumValue().millisOfDay().withMinimumValue().toDate().getTime();
            long end = startDate.dayOfMonth().withMaximumValue().millisOfDay().withMaximumValue().toDate().getTime();
            List<Float> stack = new ArrayList<>();
            for (Account account : mAccountsDbAdapter.getSimpleAccountList()) {
                if (account.getAccountType() == mAccountType
                        && !account.isPlaceholderAccount()
                        && account.getCurrency() == mCurrency) {

                    float balance = (float) mAccountsDbAdapter.getAccountsBalance(
                            Collections.singletonList(account.getUID()), start, end).asDouble();
                    if (balance != 0) {
                        stack.add(balance);
                        labels.add(account.getName());
                        colors.add(COLORS[(colors.size()) % COLORS.length]);
                        Log.i(TAG, mAccountType + startDate.toString(" MMMM yyyy ") + account.getName()
                                + " = " + stack.get(stack.size() - 1)  + ", color = " + colors.get(colors.size() - 1));
                    }
                }
            }

            float array[] = new float[stack.size()];
            for (int k = 0;  k < stack.size(); k++) {
                array[k] = stack.get(k);
            }

            values.add(new BarEntry(array, i));

            startDate = startDate.plusMonths(1);
        }

        mLegendColors = new LinkedHashSet<>(colors);
        mLegendLabels = new LinkedHashSet<>(labels);

        BarDataSet set = new BarDataSet(values, "");
        set.setStackLabels(labels.toArray(new String[labels.size()]));
        set.setColors(colors);
        dataSets.add(set);

        if (set.getYValueSum() == 0) {
            mChartDataPresent = false;
            return getEmptyData();
        }
        return new BarData(xValues, dataSets);
    }

    /**
     * Calculates the earliest and latest transaction's timestamps of the specified account types
     * @param accountTypeList account's types which will be processed
     */
    private void calculateEarliestAndLatestTimestamps(List<AccountType> accountTypeList) {
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

    /**
     * Returns a map with an account type as key and correspond accounts UIDs as value
     * from a specified list of account types
     * @param accountTypeList account's types which will be used as keys
     * @return a map with an account type as key and correspond accounts UIDs as value
     */
    private Map<AccountType, List<String>> getAccountTypeToAccountUidMap(List<AccountType> accountTypeList) {
        Map<AccountType, List<String>> accountMap = new HashMap<>();
        for (AccountType accountType : accountTypeList) {
            List<String> accountUIDList = new ArrayList<>();
            for (Account account : mAccountsDbAdapter.getSimpleAccountList()) {
                if (account.getAccountType() == accountType
                        && !account.isPlaceholderAccount()
                        && account.getCurrency() == mCurrency) {
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
        List<String> xValues = new ArrayList<>();
        List<BarEntry> yValues = new ArrayList<>();
        for (int i = 0; i < NO_DATA_BAR_COUNTS; i++) {
            xValues.add("");
            yValues.add(new BarEntry(i % 2 == 0 ? 5f : 4.5f, i));
        }
        BarDataSet set = new BarDataSet(yValues, getResources().getString(R.string.label_chart_no_data));
        set.setDrawValues(false);
        set.setColor(NO_DATA_COLOR);

        return new BarData(xValues, Collections.singletonList(set));
    }

    /**
     * Sets up settings and data for the account type spinner. Currently used only {@code EXPENSE} and {@code INCOME}
     * account types.
     */
    private void setUpSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.chart_data_spinner);
        ArrayAdapter<AccountType> dataAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                Arrays.asList(AccountType.EXPENSE, AccountType.INCOME));
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mAccountType = (AccountType) ((Spinner) findViewById(R.id.chart_data_spinner)).getSelectedItem();

                // below we can add/remove displayed account's types
                mChart.setData(getData(new ArrayList<>(Arrays.asList(AccountType.INCOME, AccountType.EXPENSE))));

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

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
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
                // workaround for buggy legend
                Legend legend = mChart.getLegend();
                legend.setEnabled(!mChart.getLegend().isEnabled());
                legend.setLabels(mLegendLabels.toArray(new String[mLegendLabels.size()]));
                legend.setColors(Arrays.asList(mLegendColors.toArray(new Integer[mLegendLabels.size()])));
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
