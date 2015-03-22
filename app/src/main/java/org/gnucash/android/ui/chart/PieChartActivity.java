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

package org.gnucash.android.ui.chart;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.components.Legend.LegendPosition;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Highlight;

import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.ui.passcode.PassLockActivity;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Activity used for drawing a pie chart
 *
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class PieChartActivity extends PassLockActivity implements OnChartValueSelectedListener, DatePickerDialog.OnDateSetListener {

    private static final int[] COLORS = {
            Color.parseColor("#17ee4e"), Color.parseColor("#cc1f09"), Color.parseColor("#3940f7"),
            Color.parseColor("#f9cd04"), Color.parseColor("#5f33a8"), Color.parseColor("#e005b6"),
            Color.parseColor("#17d6ed"), Color.parseColor("#e4a9a2"), Color.parseColor("#8fe6cd"),
            Color.parseColor("#8b48fb"), Color.parseColor("#343a36"), Color.parseColor("#6decb1"),
            Color.parseColor("#a6dcfd"), Color.parseColor("#5c3378"), Color.parseColor("#a6dcfd"),
            Color.parseColor("#ba037c"), Color.parseColor("#708809"), Color.parseColor("#32072c"),
            Color.parseColor("#fddef8"), Color.parseColor("#fa0e6e"), Color.parseColor("#d9e7b5")
    };

    private static final String datePattern = "MMMM\nYYYY";

    private PieChart mChart;

    private LocalDateTime mChartDate = new LocalDateTime();
    private TextView mChartDateTextView;

    private ImageButton mPreviousMonthButton;
    private ImageButton mNextMonthButton;

    private AccountsDbAdapter mAccountsDbAdapter;
    private TransactionsDbAdapter mTransactionsDbAdapter;

    private LocalDateTime mEarliestTransactionDate;
    private LocalDateTime mLatestTransactionDate;

    private AccountType mAccountType = AccountType.EXPENSE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //it is necessary to set the view first before calling super because of the nav drawer in BaseDrawerActivity
        setContentView(R.layout.activity_pie_chart);
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.title_pie_chart);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        mPreviousMonthButton = (ImageButton) findViewById(R.id.previous_month_chart_button);
        mNextMonthButton = (ImageButton) findViewById(R.id.next_month_chart_button);
        mChartDateTextView = (TextView) findViewById(R.id.chart_date);

        mAccountsDbAdapter = AccountsDbAdapter.getInstance();
        mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();

        mChart = (PieChart) findViewById(R.id.pie_chart);
        mChart.setCenterTextSize(18);
        mChart.setDescription("");
        mChart.setOnChartValueSelectedListener(this);

        setUpSpinner();

        mPreviousMonthButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mChartDate = mChartDate.minusMonths(1);
                setData(true);
            }
        });
        mNextMonthButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mChartDate = mChartDate.plusMonths(1);
                setData(true);
            }
        });

        mChartDateTextView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                DialogFragment newFragment = ChartDatePickerFragment.newInstance(PieChartActivity.this,
                        mChartDate.toDate().getTime(),
                        mEarliestTransactionDate.toDate().getTime(),
                        mLatestTransactionDate.toDate().getTime());
                newFragment.show(getSupportFragmentManager(), "date_dialog");
            }
        });
    }

    /**
     * Sets the chart data
     *
     * @param forCurrentMonth sets data only for current month if {@code true}, otherwise for all time
     */
    private void setData(boolean forCurrentMonth) {
        mChartDateTextView.setText(forCurrentMonth ? mChartDate.toString(datePattern) : getResources().getString(R.string.label_chart_overall));
        ((TextView) findViewById(R.id.selected_chart_slice)).setText("");
        mChart.highlightValues(null);
        mChart.clear();

        PieDataSet dataSet = new PieDataSet(null, "");
        ArrayList<String> names = new ArrayList<String>();
        List<String> skipUUID = new ArrayList<String>();
        for (Account account : mAccountsDbAdapter.getSimpleAccountList()) {
            if (account.getAccountType() == mAccountType && !account.isPlaceholderAccount()) {
                if (mAccountsDbAdapter.getSubAccountCount(account.getUID()) > 0) {
                    skipUUID.addAll(mAccountsDbAdapter.getDescendantAccountUIDs(account.getUID(), null, null));
                }
                if (!skipUUID.contains(account.getUID())) {
                    double balance = 0;
                    if (forCurrentMonth) {
                        long start = mChartDate.dayOfMonth().withMinimumValue().millisOfDay().withMinimumValue().toDate().getTime();
                        long end = mChartDate.dayOfMonth().withMaximumValue().millisOfDay().withMaximumValue().toDate().getTime();
                        balance = mAccountsDbAdapter.getAccountBalance(account.getUID(), start, end).asDouble();
                    } else {
                        balance = mAccountsDbAdapter.getAccountBalance(account.getUID()).asDouble();
                    }
                    if (balance > 0) {
                        dataSet.addEntry(new Entry((float) Math.abs(balance), dataSet.getEntryCount()));
                        dataSet.addColor(COLORS[(dataSet.getEntryCount() - 1) % COLORS.length]);
                        names.add(account.getName());
                    }
                }
            }
        }

        if (dataSet.getEntryCount() == 0) {
            dataSet.addEntry(new Entry(1, 0));
            dataSet.setColor(Color.LTGRAY);
            dataSet.setDrawValues(false);
            names.add("");
            mChart.setCenterText(getResources().getString(R.string.label_chart_no_data));
            mChart.setTouchEnabled(false);
        } else {
            mChart.setCenterText(getResources().getString(R.string.label_chart_total) + dataSet.getYValueSum());
            mChart.setTouchEnabled(true);
        }
        mChart.setData(new PieData(names, dataSet));
        mChart.animateXY(1800, 1800);
        mChart.invalidate();

        setImageButtonEnabled(mNextMonthButton,
                mChartDate.plusMonths(1).dayOfMonth().withMinimumValue().withMillisOfDay(0).isBefore(mLatestTransactionDate));
        setImageButtonEnabled(mPreviousMonthButton, (mEarliestTransactionDate.getYear() != 1970
                && mChartDate.minusMonths(1).dayOfMonth().withMaximumValue().withMillisOfDay(86399999).isAfter(mEarliestTransactionDate)));
    }

    /**
     * Sets the image button to the given state and grays-out the icon
     *
     * @param enabled the button's state
     * @param button the button item to modify
     */
    private void setImageButtonEnabled(ImageButton button, boolean enabled) {
        button.setEnabled(enabled);
        Drawable originalIcon = button.getDrawable();
        if (enabled) {
            originalIcon.clearColorFilter();
        } else {
            originalIcon.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
        }
        button.setImageDrawable(originalIcon);
    }

    /**
     * Sorts the pie's slices in ascending order
     */
    private void bubbleSort() {
        ArrayList<String> labels = mChart.getData().getXVals();
        ArrayList<Entry> values = mChart.getData().getDataSet().getYVals();
        ArrayList<Integer> colors = mChart.getData().getDataSet().getColors();
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

    /**
     * Sets up settings and data for the account type spinner. Currently used only {@code EXPENSE} and {@code INCOME}
     * account types.
     */
    private void setUpSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.chart_data_spinner);
        ArrayAdapter<AccountType> dataAdapter = new ArrayAdapter<AccountType>(this,
                android.R.layout.simple_spinner_item,
                Arrays.asList(AccountType.EXPENSE, AccountType.INCOME));
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mAccountType = (AccountType) ((Spinner) findViewById(R.id.chart_data_spinner)).getSelectedItem();
                mEarliestTransactionDate = new LocalDateTime(mTransactionsDbAdapter.getTimestampOfEarliestTransaction(mAccountType));
                mLatestTransactionDate = new LocalDateTime(mTransactionsDbAdapter.getTimestampOfLatestTransaction(mAccountType));
                mChartDate = mLatestTransactionDate;
                setData(false);
                mChart.getLegend().setEnabled(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.chart_actions, menu);
        // hide bar chart specific menu items
        menu.findItem(R.id.menu_percentage_mode).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_order_by_size: {
                bubbleSort();
                break;
            }
            case R.id.menu_toggle_legend: {
                mChart.getLegend().setEnabled(!mChart.getLegend().isEnabled());
                mChart.getLegend().setForm(LegendForm.CIRCLE);
                mChart.getLegend().setPosition(LegendPosition.RIGHT_OF_CHART_CENTER);
                mChart.notifyDataSetChanged();
                mChart.invalidate();
                break;
            }
            case R.id.menu_toggle_labels: {
                mChart.getData().setDrawValues(!mChart.isDrawSliceTextEnabled());
                mChart.setDrawSliceText(!mChart.isDrawSliceTextEnabled());
                mChart.invalidate();
                break;
            }
            case android.R.id.home: {
                finish();
                break;
            }
        }
        return true;
    }

    /**
     * Since JellyBean, the onDateSet() method of the DatePicker class is called twice i.e. once when
     * OK button is pressed and then when the DatePickerDialog is dismissed. It is a known bug.
     */
    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        if (view.isShown()) {
            mChartDate = new LocalDateTime(year, monthOfYear + 1, dayOfMonth, 0, 0);
            setData(true);
        }
    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
        if (e == null) return;
        ((TextView) findViewById(R.id.selected_chart_slice))
                .setText(mChart.getData().getXVals().get(e.getXIndex()) + " - " + e.getVal()
                        + " (" + String.format("%.2f", (e.getVal() / mChart.getYValueSum()) * 100) + " %)");
    }

    @Override
    public void onNothingSelected() {
        ((TextView) findViewById(R.id.selected_chart_slice)).setText("");
    }
}
