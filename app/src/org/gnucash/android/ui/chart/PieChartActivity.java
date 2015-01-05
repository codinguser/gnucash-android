/*
 * Copyright (c) 2014 Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
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
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.interfaces.OnChartValueSelectedListener;

import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class PieChartActivity extends SherlockFragmentActivity implements OnChartValueSelectedListener, OnItemSelectedListener {

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

    private LocalDateTime mEarliestTransaction;
    private LocalDateTime mLatestTransaction;

    private AccountType mAccountType = AccountType.EXPENSE;

    private double mBalanceSum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_reports);

        mPreviousMonthButton = (ImageButton) findViewById(R.id.previous_month_chart_button);
        mNextMonthButton = (ImageButton) findViewById(R.id.next_month_chart_button);
        mChartDateTextView = (TextView) findViewById(R.id.chart_date);

        mAccountsDbAdapter = new AccountsDbAdapter(this);
        TransactionsDbAdapter transactionsDbAdapter = new TransactionsDbAdapter(this);
        mEarliestTransaction = new LocalDateTime(transactionsDbAdapter.getTimestampOfEarliestTransaction(mAccountType));
        mLatestTransaction = new LocalDateTime(transactionsDbAdapter.getTimestampOfLatestTransaction(mAccountType));

        addItemsOnSpinner();

        mChart = (PieChart) findViewById(R.id.chart);
        mChart.setOnChartValueSelectedListener(this);
        setData(false);

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
    }

    private void setData(boolean forCurrentMonth) {
        mChartDateTextView.setText(forCurrentMonth ? mChartDate.toString(datePattern) : "Overall");
        ((TextView) findViewById(R.id.selected_chart_slice)).setText("");
        mChart.highlightValues(null);
        mChart.clear();
        mBalanceSum = 0;

        long start = mChartDate.dayOfMonth().withMinimumValue().millisOfDay().withMinimumValue().toDate().getTime();
        long end = mChartDate.dayOfMonth().withMaximumValue().millisOfDay().withMaximumValue().toDate().getTime();
        ArrayList<Entry> values = new ArrayList<Entry>();
        ArrayList<String> names = new ArrayList<String>();
        ArrayList<Integer> colors = new ArrayList<Integer>();
        for (Account account : mAccountsDbAdapter.getSimpleAccountList()) {
            if (account.getAccountType() == mAccountType && !account.isPlaceholderAccount()) {
                double balance = 0;
                if (forCurrentMonth) {
                    balance = mAccountsDbAdapter.getAccountBalance(account.getUID(), start, end).asDouble();
                } else {
                    balance = mAccountsDbAdapter.getAccountBalance(account.getUID()).asDouble();
                }
                // ToDo What with negative?
                if (balance > 0) {
                    mBalanceSum += balance;
                    values.add(new Entry((float) balance, values.size()));
                    names.add(account.getName());
                    colors.add(COLORS[(values.size() - 1) % COLORS.length]);
                }
            }
        }

        PieDataSet set = new PieDataSet(values, "");
        set.setColors(colors);
        mChart.setData(new PieData(names, set));

        if (mChartDate.plusMonths(1).dayOfMonth().withMinimumValue().withMillisOfDay(0).isBefore(mLatestTransaction)) {
            setImageButtonEnabled(mNextMonthButton, true);
        } else {
            setImageButtonEnabled(mNextMonthButton, false);
        }
        if (mEarliestTransaction.getYear() != 1970 && mChartDate.minusMonths(1).dayOfMonth()
                .withMaximumValue().withMillisOfDay(86399999).isAfter(mEarliestTransaction)) {
            setImageButtonEnabled(mPreviousMonthButton, true);
        } else {
            setImageButtonEnabled(mPreviousMonthButton, false);
        }

        mChart.setDrawYValues(false);
        mChart.setDescription("");
        mChart.invalidate();
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

    private void bubbleSort() {
        ArrayList<String> names = mChart.getData().getXVals();
        ArrayList<Entry> values = mChart.getData().getDataSet().getYVals();
        ArrayList<Integer> colors = mChart.getData().getDataSet().getColors();
        boolean swapped = true;
        int j = 0;
        float tmp1;
        String tmp2;
        Integer tmp3;
        while (swapped) {
            swapped = false;
            j++;
            for (int i = 0; i < values.size() - j; i++) {
                if (values.get(i).getVal() > values.get(i + 1).getVal()) {
                    tmp1 = values.get(i).getVal();
                    values.get(i).setVal(values.get(i + 1).getVal());
                    values.get(i + 1).setVal(tmp1);

                    tmp2 = names.get(i);
                    names.set(i, names.get(i + 1));
                    names.set(i + 1, tmp2);

                    tmp3 = colors.get(i);
                    colors.set(i, colors.get(i + 1));
                    colors.set(i + 1, tmp3);

                    swapped = true;
                }
            }
        }

        mChart.clear();
        PieDataSet set = new PieDataSet(values, "");
        set.setColors(colors);
        mChart.setData(new PieData(names, set));
        mChart.highlightValues(null);
        mChart.invalidate();
    }

    private void addItemsOnSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.chart_data_spinner);
        ArrayAdapter<AccountType> dataAdapter = new ArrayAdapter<AccountType>(this,
                android.R.layout.simple_spinner_item,
                Arrays.asList(AccountType.EXPENSE, AccountType.INCOME));
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);

        spinner.setOnItemSelectedListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.pie_chart_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_order_by_size) {
            bubbleSort();
            return true;
        }
        return false;
    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex) {
        if (e == null) {
            return;
        }

        ((TextView) findViewById(R.id.selected_chart_slice))
                .setText(mChart.getData().getXVals().get(e.getXIndex()) + " - " + e.getVal()
                        + " (" + String.format("%.2f", (e.getVal() / mBalanceSum) * 100) + " %)");
    }

    @Override
    public void onNothingSelected() {
        ((TextView) findViewById(R.id.selected_chart_slice)).setText("");
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        mAccountType = (AccountType) ((Spinner) findViewById(R.id.chart_data_spinner)).getSelectedItem();
        setData(false);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

}
