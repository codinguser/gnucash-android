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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.SeriesSelection;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.joda.time.LocalDateTime;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class PieChartActivity extends SherlockFragmentActivity implements OnItemSelectedListener {

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

    private DefaultRenderer mRenderer = new DefaultRenderer();
    private CategorySeries mSeries = new CategorySeries("");
    private GraphicalView mPieChartView;

    private AccountsDbAdapter mAccountsDbAdapter;

    private double mBalanceSum;

    private ImageButton mPreviousMonthButton;
    private ImageButton mNextMonthButton;

    private LocalDateTime mChartDate = new LocalDateTime();
    private TextView mChartDateTextView;

    private LocalDateTime mEarliestTransaction;
    private LocalDateTime mLatestTransaction;

    private AccountType mAccountType = AccountType.EXPENSE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_reports);

        mPreviousMonthButton = (ImageButton) findViewById(R.id.previous_month_chart_button);
        mNextMonthButton = (ImageButton) findViewById(R.id.next_month_chart_button);
        mChartDateTextView = (TextView) findViewById(R.id.chart_date);

        TransactionsDbAdapter transactionsDbAdapter = new TransactionsDbAdapter(this);
        mEarliestTransaction = new LocalDateTime(transactionsDbAdapter.getTimestampOfEarliestTransaction(mAccountType));
        mLatestTransaction = new LocalDateTime(transactionsDbAdapter.getTimestampOfLatestTransaction(mAccountType));

        mAccountsDbAdapter = new AccountsDbAdapter(this);

        addItemsOnSpinner();
        renderSettings();

        mPieChartView = ChartFactory.getPieChartView(this, mSeries, mRenderer);
        mPieChartView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SeriesSelection selection = mPieChartView.getCurrentSeriesAndPoint();
                if (selection != null) {
                    for (int i = 0; i < mSeries.getItemCount(); i++) {
                        mRenderer.getSeriesRendererAt(i).setHighlighted(i == selection.getPointIndex());
                    }
                    mPieChartView.repaint();

                    double value = selection.getValue();
                    double percent = (value / mBalanceSum) * 100;
                    ((TextView) findViewById(R.id.selected_chart_slice))
                            .setText(mSeries.getCategory(selection.getPointIndex()) + " - " + value
                                    + " (" + String.format("%.2f", percent) + " %)");
                }
            }
        });

        ((LinearLayout) findViewById(R.id.chart)).addView(mPieChartView,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mPreviousMonthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mChartDate = mChartDate.minusMonths(1);
                setDataset(true);
            }
        });

        mNextMonthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mChartDate = mChartDate.plusMonths(1);
                setDataset(true);
            }
        });
    }

    private void setDataset(boolean forCurrentMonth) {
        mChartDateTextView.setText(forCurrentMonth ? mChartDate.toString(datePattern) : "Overall");
        mRenderer.removeAllRenderers();
        mSeries.clear();
        mBalanceSum = 0;

        List<Account> accountList = mAccountsDbAdapter.getSimpleAccountList();
        for (Account account : accountList) {
            if (account.getAccountType() == mAccountType && !account.isPlaceholderAccount()) {
                double balance = 0;
                if (forCurrentMonth) {
                    long start = mChartDate.dayOfMonth().withMinimumValue().millisOfDay().withMinimumValue().toDate().getTime();
                    long end = mChartDate.dayOfMonth().withMaximumValue().millisOfDay().withMaximumValue().toDate().getTime();
                    balance = mAccountsDbAdapter.getAccountBalance(account.getUID(), start, end).asDouble();
                } else {
                    balance = mAccountsDbAdapter.getAccountBalance(account.getUID()).asDouble();
                }
                // ToDo What with negative?
                if (balance > 0) {
                    mBalanceSum += balance;
                    mSeries.add(account.getName(), balance);
                    SimpleSeriesRenderer renderer = new SimpleSeriesRenderer();
                    renderer.setColor(COLORS[(mSeries.getItemCount() - 1) % COLORS.length]);
                    mRenderer.addSeriesRenderer(renderer);
                }
            }
        }

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

        mPieChartView.repaint();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.pie_chart_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_order_by_size) {
            mSeries = bubbleSort(mSeries);
            mPieChartView.repaint();
            return true;
        }
        return false;
    }

    private CategorySeries bubbleSort(CategorySeries series) {
        boolean swapped = true;
        int j = 0;
        double tmp1;
        String tmp2;
        while (swapped) {
            swapped = false;
            j++;
            for (int i = 0; i < series.getItemCount() - j; i++) {
                if (series.getValue(i) > series.getValue(i + 1)) {
                    tmp1 = series.getValue(i);
                    tmp2 = series.getCategory(i);
                    series.set(i, series.getCategory(i + 1), series.getValue(i + 1));
                    series.set(i + 1, tmp2, tmp1);
                    swapped = true;
                }
            }
        }
        return series;
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
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        mAccountType = (AccountType) ((Spinner) findViewById(R.id.chart_data_spinner)).getSelectedItem();
        mRenderer.setChartTitle(mAccountType.toString());
        setDataset(false);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    private void renderSettings() {
        mRenderer.setChartTitle("Expenses");
        mRenderer.setChartTitleTextSize(25);

        mRenderer.setShowLabels(true);
        mRenderer.setLabelsColor(Color.BLACK);
        mRenderer.setLabelsTextSize(15);

        mRenderer.setShowLegend(false);

        mRenderer.setClickEnabled(true);
        mRenderer.setZoomButtonsVisible(true);
        mRenderer.setStartAngle(180);
    }

}
