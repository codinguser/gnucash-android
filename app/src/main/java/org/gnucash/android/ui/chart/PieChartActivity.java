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
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

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
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.passcode.PassLockActivity;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.gnucash.android.db.DatabaseSchema.AccountEntry;

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

    private static final String DATE_PATTERN = "MMMM\nYYYY";
    private static final String TOTAL_VALUE_LABEL_PATTERN = "%s\n%.2f %s";
    private static final int ANIMATION_DURATION = 1800;

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

    private boolean mChartDataPresent = true;

    private boolean mUseAccountColor = true;

    private double mSlicePercentThreshold = 6;

    private String mCurrencyCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pie_chart);
        setUpDrawer();
        getSupportActionBar().setTitle(R.string.title_pie_chart);

        mUseAccountColor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getBoolean(getString(R.string.key_use_account_color), false);

        mPreviousMonthButton = (ImageButton) findViewById(R.id.previous_month_chart_button);
        mNextMonthButton = (ImageButton) findViewById(R.id.next_month_chart_button);
        mChartDateTextView = (TextView) findViewById(R.id.chart_date);

        mAccountsDbAdapter = AccountsDbAdapter.getInstance();
        mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();

        mCurrencyCode = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.key_report_currency), Money.DEFAULT_CURRENCY_CODE);

        mChart = (PieChart) findViewById(R.id.pie_chart);
        mChart.setCenterTextSize(18);
        mChart.setDescription("");
        mChart.getLegend().setEnabled(false);
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
     * @param forCurrentMonth sets data only for current month if {@code true}, otherwise for all time
     */
    private void setData(boolean forCurrentMonth) {
        mChartDateTextView.setText(forCurrentMonth ? mChartDate.toString(DATE_PATTERN) : getResources().getString(R.string.label_chart_overall));
        ((TextView) findViewById(R.id.selected_chart_slice)).setText("");
        mChart.highlightValues(null);
        mChart.clear();

        mChart.setData(getData(forCurrentMonth));
        if (mChartDataPresent) {
            mChart.animateXY(ANIMATION_DURATION, ANIMATION_DURATION);
        }
        mChart.invalidate();

        mChartDateTextView.setEnabled(mChartDataPresent);
        setImageButtonEnabled(mNextMonthButton,
                mChartDate.plusMonths(1).dayOfMonth().withMinimumValue().withMillisOfDay(0).isBefore(mLatestTransactionDate));
        setImageButtonEnabled(mPreviousMonthButton, (mEarliestTransactionDate.getYear() != 1970
                && mChartDate.minusMonths(1).dayOfMonth().withMaximumValue().withMillisOfDay(86399999).isAfter(mEarliestTransactionDate)));
    }

    /**
     * Returns {@code PieData} instance with data entries and labels
     * @param forCurrentMonth sets data only for current month if {@code true}, otherwise for all time
     * @return {@code PieData} instance
     */
    private PieData getData(boolean forCurrentMonth) {
        List<Account> accountList = mAccountsDbAdapter.getSimpleAccountList(
                AccountEntry.COLUMN_TYPE + " = ? AND " + AccountEntry.COLUMN_PLACEHOLDER + " = ?",
                new String[]{ mAccountType.name(), "0" }, null);
        List<String> uidList = new ArrayList<>();
        for (Account account : accountList) {
            uidList.add(account.getUID());
        }
        double sum;
        if (forCurrentMonth) {
            long start = mChartDate.dayOfMonth().withMinimumValue().millisOfDay().withMinimumValue().toDate().getTime();
            long end = mChartDate.dayOfMonth().withMaximumValue().millisOfDay().withMaximumValue().toDate().getTime();
            sum = mAccountsDbAdapter.getAccountsBalance(uidList, start, end).absolute().asDouble();
        } else {
            sum = mAccountsDbAdapter.getAccountsBalance(uidList, -1, -1).absolute().asDouble();
        }

        double otherSlice = 0;
        PieDataSet dataSet = new PieDataSet(null, "");
        List<String> names = new ArrayList<>();
        List<String> skipUUID = new ArrayList<>();
        for (Account account : getCurrencyCodeToAccountMap(accountList).get(mCurrencyCode)) {
            if (mAccountsDbAdapter.getSubAccountCount(account.getUID()) > 0) {
                skipUUID.addAll(mAccountsDbAdapter.getDescendantAccountUIDs(account.getUID(), null, null));
            }
            if (!skipUUID.contains(account.getUID())) {
                double balance;
                if (forCurrentMonth) {
                    long start = mChartDate.dayOfMonth().withMinimumValue().millisOfDay().withMinimumValue().toDate().getTime();
                    long end = mChartDate.dayOfMonth().withMaximumValue().millisOfDay().withMaximumValue().toDate().getTime();
                    balance = mAccountsDbAdapter.getAccountBalance(account.getUID(), start, end).absolute().asDouble();
                } else {
                    balance = mAccountsDbAdapter.getAccountBalance(account.getUID()).absolute().asDouble();
                }

                if (balance / sum * 100 > mSlicePercentThreshold) {
                    dataSet.addEntry(new Entry((float) balance, dataSet.getEntryCount()));
                    if (mUseAccountColor) {
                        dataSet.getColors().set(dataSet.getColors().size() - 1, (account.getColorHexCode() != null)
                                ? Color.parseColor(account.getColorHexCode())
                                : COLORS[(dataSet.getEntryCount() - 1) % COLORS.length]);
                    }
                    dataSet.addColor(COLORS[(dataSet.getEntryCount() - 1) % COLORS.length]);
                    names.add(account.getName());
                } else {
                    otherSlice += balance;
                }
            }
        }
        if (otherSlice > 0) {
            dataSet.addEntry(new Entry((float) otherSlice, dataSet.getEntryCount()));
            dataSet.getColors().set(dataSet.getColors().size() - 1, Color.LTGRAY);
            names.add(getResources().getString(R.string.label_other_slice));
        }

        if (dataSet.getEntryCount() == 0) {
            mChartDataPresent = false;
            dataSet.addEntry(new Entry(1, 0));
            dataSet.setColor(Color.LTGRAY);
            dataSet.setDrawValues(false);
            names.add("");
            mChart.setCenterText(getResources().getString(R.string.label_chart_no_data));
            mChart.setTouchEnabled(false);
        } else {
            mChartDataPresent = true;
            dataSet.setSliceSpace(2);
            mChart.setCenterText(String.format(TOTAL_VALUE_LABEL_PATTERN,
                            getResources().getString(R.string.label_chart_total),
                            dataSet.getYValueSum(),
                            Currency.getInstance(mCurrencyCode).getSymbol(Locale.getDefault()))
            );
            mChart.setTouchEnabled(true);
        }

        return new PieData(names, dataSet);
    }

    /**
     * Returns a map with a currency code as key and corresponding accounts list
     * as value from a specified list of accounts
     * @param accountList a list of accounts
     * @return a map with a currency code as key and corresponding accounts list as value
     */
    private Map<String, List<Account>> getCurrencyCodeToAccountMap(List<Account> accountList) {
        Map<String, List<Account>> currencyAccountMap = new HashMap<>();
        for (Currency currency : mAccountsDbAdapter.getCurrencies()) {
            currencyAccountMap.put(currency.getCurrencyCode(), new ArrayList<Account>());
        }

        for (Account account : accountList) {
            currencyAccountMap.get(account.getCurrency().getCurrencyCode()).add(account);
        }
        return currencyAccountMap;
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
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mAccountType = (AccountType) ((Spinner) findViewById(R.id.chart_data_spinner)).getSelectedItem();
                mEarliestTransactionDate = new LocalDateTime(mTransactionsDbAdapter.getTimestampOfEarliestTransaction(mAccountType, mCurrencyCode));
                mLatestTransactionDate = new LocalDateTime(mTransactionsDbAdapter.getTimestampOfLatestTransaction(mAccountType, mCurrencyCode));
                mChartDate = mLatestTransactionDate;
                setData(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chart_actions, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_order_by_size).setVisible(mChartDataPresent);
        menu.findItem(R.id.menu_toggle_labels).setVisible(mChartDataPresent);
        menu.findItem(R.id.menu_group_other_slice).setVisible(mChartDataPresent);
        // hide line/bar chart specific menu items
        menu.findItem(R.id.menu_percentage_mode).setVisible(false);
        menu.findItem(R.id.menu_toggle_average_lines).setVisible(false);
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
            case R.id.menu_group_other_slice: {
                mSlicePercentThreshold = Math.abs(mSlicePercentThreshold - 6);
                setData(false);
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
