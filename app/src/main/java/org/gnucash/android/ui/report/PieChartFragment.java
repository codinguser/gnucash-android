/*
 * Copyright (c) 2014-2015 Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
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

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.components.Legend.LegendPosition;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Activity used for drawing a pie chart
 *
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class PieChartFragment extends Fragment implements OnChartValueSelectedListener,
        ReportOptionsListener {

    public static final String SELECTED_VALUE_PATTERN = "%s - %.2f (%.2f %%)";
    public static final String TOTAL_VALUE_LABEL_PATTERN = "%s\n%.2f %s";
    private static final int ANIMATION_DURATION = 1800;
    public static final int NO_DATA_COLOR = Color.LTGRAY;
    public static final int CENTER_TEXT_SIZE = 18;
    /**
     * The space in degrees between the chart slices
     */
    public static final float SPACE_BETWEEN_SLICES = 2f;
    /**
     * All pie slices less than this threshold will be group in "other" slice. Using percents not absolute values.
     */
    private static final double GROUPING_SMALLER_SLICES_THRESHOLD = 5;

    @Bind(R.id.pie_chart) PieChart mChart;
    @Bind(R.id.selected_chart_slice) TextView mSelectedValueTextView;

    private AccountsDbAdapter mAccountsDbAdapter;
    private TransactionsDbAdapter mTransactionsDbAdapter;

    private AccountType mAccountType;

    private boolean mChartDataPresent = true;

    private boolean mUseAccountColor = true;

    private boolean mGroupSmallerSlices = true;

    private String mCurrencyCode;

    /**
     * Start time for reporting period in millis
     */
    private long mReportStartTime = -1;

    /**
     * End time for reporting period in millis
     */
    private long mReportEndTime = -1;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pie_chart, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.title_pie_chart);
        setHasOptionsMenu(true);

        mUseAccountColor = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getBoolean(getString(R.string.key_use_account_color), false);

        mAccountsDbAdapter = AccountsDbAdapter.getInstance();
        mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();

        mCurrencyCode = GnuCashApplication.getDefaultCurrencyCode();

        mChart.setCenterTextSize(CENTER_TEXT_SIZE);
        mChart.setDescription("");
        mChart.getLegend().setWordWrapEnabled(true);
        mChart.setOnChartValueSelectedListener(this);

        ReportsActivity reportsActivity = (ReportsActivity) getActivity();
        mReportStartTime = reportsActivity.getReportStartTime();
        mReportEndTime = reportsActivity.getReportEndTime();
        mAccountType = reportsActivity.getAccountType();

        displayChart();
    }

    /**
     * Sets the app bar color
     */
    @Override
    public void onResume() {
        super.onResume();
        ((ReportsActivity)getActivity()).setAppBarColor(R.color.account_green);
    }

    /**
     * Manages all actions about displaying the pie chart
     */
    private void displayChart() {
        mSelectedValueTextView.setText(R.string.label_select_pie_slice_to_see_details);
        mChart.highlightValues(null);
        mChart.clear();

        PieData pieData = getData();
        if (pieData != null && pieData.getYValCount() != 0) {
            mChartDataPresent = true;
            mChart.setData(mGroupSmallerSlices ? groupSmallerSlices(pieData, getActivity()) : pieData);
            float sum = mChart.getData().getYValueSum();
            String total = getResources().getString(R.string.label_chart_total);
            String currencySymbol = Currency.getInstance(mCurrencyCode).getSymbol(Locale.getDefault());
            mChart.setCenterText(String.format(TOTAL_VALUE_LABEL_PATTERN, total, sum, currencySymbol));
            mChart.animateXY(ANIMATION_DURATION, ANIMATION_DURATION);
        } else {
            mChartDataPresent = false;
            mChart.setCenterText(getResources().getString(R.string.label_chart_no_data));
            mChart.setData(getEmptyData());
        }

        mChart.setTouchEnabled(mChartDataPresent);
        mChart.invalidate();
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
            if (account.getAccountType() == mAccountType
                    && !account.isPlaceholderAccount()
                    && account.getCurrency() == Currency.getInstance(mCurrencyCode)) {

                double balance = mAccountsDbAdapter.getAccountsBalance(Collections.singletonList(account.getUID()),
                        mReportStartTime, mReportEndTime).asDouble();
                if (balance > 0) {
                    dataSet.addEntry(new Entry((float) balance, dataSet.getEntryCount()));
                    colors.add(mUseAccountColor && account.getColorHexCode() != null
                            ? Color.parseColor(account.getColorHexCode())
                            : ReportsActivity.COLORS[(dataSet.getEntryCount() - 1) % ReportsActivity.COLORS.length]);
                    labels.add(account.getName());
                }
            }
        }
        dataSet.setColors(colors);
        dataSet.setSliceSpace(SPACE_BETWEEN_SLICES);
        return new PieData(labels, dataSet);
    }

    @Override
    public void onTimeRangeUpdated(long start, long end) {
        if (mReportStartTime != start || mReportEndTime != end) {
            mReportStartTime = start;
            mReportEndTime = end;
            displayChart();
        }
    }

    @Override
    public void onGroupingUpdated(ReportsActivity.GroupInterval groupInterval) {
        //nothing to see here, this doesn't make sense for a pie chart
    }

    @Override
    public void onAccountTypeUpdated(AccountType accountType) {
        if (mAccountType != accountType) {
            mAccountType = accountType;
            displayChart();
        }
    }

    /**
     * Returns a data object that represents situation when no user data available
     * @return a {@code PieData} instance for situation when no user data available
     */
    private PieData getEmptyData() {
        PieDataSet dataSet = new PieDataSet(null, getResources().getString(R.string.label_chart_no_data));
        dataSet.addEntry(new Entry(1, 0));
        dataSet.setColor(NO_DATA_COLOR);
        dataSet.setDrawValues(false);
        return new PieData(Collections.singletonList(""), dataSet);
    }

    /**
     * Sorts the pie's slices in ascending order
     */
    private void bubbleSort() {
        List<String> labels = mChart.getData().getXVals();
        List<Entry> values = mChart.getData().getDataSet().getYVals();
        List<Integer> colors = mChart.getData().getDataSet().getColors();
        float tmp1;
        String tmp2;
        Integer tmp3;
        for(int i = 0; i < values.size() - 1; i++) {
            for(int j = 1; j < values.size() - i; j++) {
                if (values.get(j-1).getVal() > values.get(j).getVal()) {
                    tmp1 = values.get(j - 1).getVal();
                    values.get(j - 1).setVal(values.get(j).getVal());
                    values.get(j).setVal(tmp1);

                    tmp2 = labels.get(j - 1);
                    labels.set(j - 1, labels.get(j));
                    labels.set(j, tmp2);

                    tmp3 = colors.get(j - 1);
                    colors.set(j - 1, colors.get(j));
                    colors.set(j, tmp3);
                }
            }
        }

        mChart.notifyDataSetChanged();
        mChart.highlightValues(null);
        mChart.invalidate();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.chart_actions, menu);
        menu.findItem(R.id.menu_toggle_legend).setChecked(false);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_order_by_size).setVisible(mChartDataPresent);
        menu.findItem(R.id.menu_toggle_labels).setVisible(mChartDataPresent);
        menu.findItem(R.id.menu_group_other_slice).setVisible(mChartDataPresent);
        // hide line/bar chart specific menu items
        menu.findItem(R.id.menu_percentage_mode).setVisible(false);
        menu.findItem(R.id.menu_toggle_average_lines).setVisible(false);
        menu.findItem(R.id.menu_group_reports_by).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.isCheckable())
            item.setChecked(!item.isChecked());
        switch (item.getItemId()) {
            case R.id.menu_order_by_size: {
                bubbleSort();
                return true;
            }
            case R.id.menu_toggle_legend: {
                mChart.getLegend().setEnabled(!mChart.getLegend().isEnabled());
                mChart.getLegend().setForm(LegendForm.CIRCLE);
                mChart.getLegend().setPosition(LegendPosition.RIGHT_OF_CHART_CENTER);
                mChart.notifyDataSetChanged();
                mChart.invalidate();
                return true;
            }
            case R.id.menu_toggle_labels: {
                mChart.getData().setDrawValues(!mChart.isDrawSliceTextEnabled());
                mChart.setDrawSliceText(!mChart.isDrawSliceTextEnabled());
                mChart.invalidate();
                return true;
            }
            case R.id.menu_group_other_slice: {
                mGroupSmallerSlices = !mGroupSmallerSlices;
                displayChart();
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Groups smaller slices. All smaller slices will be combined and displayed as a single "Other".
     * @param data the pie data which smaller slices will be grouped
     * @param context Context for retrieving resources
     * @return a {@code PieData} instance with combined smaller slices
     */
    public static PieData groupSmallerSlices(PieData data, Context context) {
        float otherSlice = 0f;
        List<Entry> newEntries = new ArrayList<>();
        List<String> newLabels = new ArrayList<>();
        List<Integer> newColors = new ArrayList<>();
        List<Entry> entries = data.getDataSet().getYVals();
        for (int i = 0; i < entries.size(); i++) {
            float val = entries.get(i).getVal();
            if (val / data.getYValueSum() * 100 > GROUPING_SMALLER_SLICES_THRESHOLD) {
                newEntries.add(new Entry(val, newEntries.size()));
                newLabels.add(data.getXVals().get(i));
                newColors.add(data.getDataSet().getColors().get(i));
            } else {
                otherSlice += val;
            }
        }

        if (otherSlice > 0) {
            newEntries.add(new Entry(otherSlice, newEntries.size()));
            newLabels.add(context.getResources().getString(R.string.label_other_slice));
            newColors.add(Color.LTGRAY);
        }

        PieDataSet dataSet = new PieDataSet(newEntries, "");
        dataSet.setSliceSpace(SPACE_BETWEEN_SLICES);
        dataSet.setColors(newColors);
        return new PieData(newLabels, dataSet);
    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
        if (e == null) return;
        String label = mChart.getData().getXVals().get(e.getXIndex());
        float value = e.getVal();
        float percent = value / mChart.getYValueSum() * 100;
        mSelectedValueTextView.setText(String.format(SELECTED_VALUE_PATTERN, label, value, percent));
    }

    @Override
    public void onNothingSelected() {
        mSelectedValueTextView.setText("");
    }
}
