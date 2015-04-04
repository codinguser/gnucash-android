package org.gnucash.android.ui.chart;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Highlight;

import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.ui.passcode.PassLockActivity;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Months;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class LineChartActivity extends PassLockActivity implements OnChartValueSelectedListener {

    private static final String TAG = "LineChartActivity";
    private static final String X_AXIS_PATTERN = "MMM YY";

    private static final int[] COLORS = {
            Color.parseColor("#68F1AF"), Color.parseColor("#cc1f09"), Color.parseColor("#EE8600"),
            Color.parseColor("#1469EB"), Color.parseColor("#B304AD"),
    };

    private LineChart mChart;
    private AccountsDbAdapter mAccountsDbAdapter;
    private Map<AccountType, Long> mEarliestTimestampsMap = new HashMap<AccountType, Long>();
    private Map<AccountType, Long> mLatestTimestampsMap = new HashMap<AccountType, Long>();
    private long mEarliestTransactionTimestamp;
    private long mLatestTransactionTimestamp;
    private boolean mChartDataPresent = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //it is necessary to set the view first before calling super because of the nav drawer in BaseDrawerActivity
        setContentView(R.layout.activity_line_chart);
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(R.string.title_line_chart);

        mAccountsDbAdapter = AccountsDbAdapter.getInstance();

        mChart = new LineChart(this);
        ((LinearLayout) findViewById(R.id.chart)).addView(mChart);
        mChart.setOnChartValueSelectedListener(this);
        mChart.setDescription("");
        mChart.getAxisRight().setEnabled(false);
        mChart.setData(getDataSet(new ArrayList<AccountType>(Arrays.asList(AccountType.INCOME, AccountType.EXPENSE))));

        Legend legend = mChart.getLegend();
        legend.setPosition(Legend.LegendPosition.RIGHT_OF_CHART_INSIDE);
        legend.setForm(Legend.LegendForm.CIRCLE);

        if (mChartDataPresent) {
            mChart.animateX(3000);
        }
        mChart.invalidate();
    }

    private LineData getDataSet(List<AccountType> accountTypes) {
        setEarliestAndLatestTimestamps(accountTypes);

        if (mEarliestTransactionTimestamp == 0) {
            if (mLatestTransactionTimestamp == 0) {
                return getEmptyDataSet();
            }
            for (Map.Entry<AccountType, Long> entry : mEarliestTimestampsMap.entrySet()) {
                if (entry.getValue() == 0) {
                    accountTypes.remove(entry.getKey());
                }
            }
            Log.w(TAG, accountTypes.toString());
            setEarliestAndLatestTimestamps(accountTypes);
        }

        LocalDate startDate = new LocalDate(mEarliestTransactionTimestamp).withDayOfMonth(1);
        LocalDate endDate = new LocalDate(mLatestTransactionTimestamp).withDayOfMonth(1);
        ArrayList<String> xValues = new ArrayList<String>();
        while (!startDate.isAfter(endDate)) {
            xValues.add(startDate.toString(X_AXIS_PATTERN));
            Log.d(TAG, "X axis " + startDate.toString("MM yy"));
            startDate = startDate.plusMonths(1);
        }

        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        for (AccountType accountType : accountTypes) {
            LineDataSet set = new LineDataSet(getEntryList(accountType), accountType.toString());
            set.setDrawFilled(true);
            set.setColor(COLORS[dataSets.size()]);
            set.setFillColor(COLORS[dataSets.size()]);

            dataSets.add(set);
        }

        return new LineData(xValues, dataSets);
    }

    private LineData getEmptyDataSet() {
        mChartDataPresent = false;

        ArrayList<String> xValues = new ArrayList<String>();
        ArrayList<Entry> yValues = new ArrayList<Entry>();
        for (int i = 0; i < 5; i++) {
            xValues.add("");
            yValues.add(new Entry(i % 2 == 0 ? 5f : 4.5f, i));
        }
        LineDataSet set = new LineDataSet(yValues, getResources().getString(R.string.label_chart_no_data));
        set.setDrawFilled(true);
        set.setDrawValues(false);
        set.setColor(Color.GRAY);
        set.setFillColor(Color.GRAY);

        mChart.getAxisLeft().setAxisMaxValue(10);
        mChart.getAxisLeft().setDrawLabels(false);
        mChart.getXAxis().setDrawLabels(false);
        mChart.setTouchEnabled(false);
        ((TextView) findViewById(R.id.selected_chart_slice)).setText(getResources().getString(R.string.label_chart_no_data));

        return new LineData(xValues, new ArrayList<LineDataSet>(Arrays.asList(set)));
    }

    private ArrayList<Entry> getEntryList(AccountType accountType) {
        List<String> accountUIDList = new ArrayList<String>();
        for (Account account : mAccountsDbAdapter.getSimpleAccountList()) {
            if (account.getAccountType() == accountType && !account.isPlaceholderAccount()) {
                accountUIDList.add(account.getUID());
            }
        }

        LocalDateTime earliest = new LocalDateTime(mEarliestTimestampsMap.get(accountType));
        LocalDateTime latest = new LocalDateTime(mLatestTimestampsMap.get(accountType));
        Log.d(TAG, "Earliest " + accountType + " date " + earliest.toString("dd MM yyyy"));
        Log.d(TAG, "Latest " + accountType + " date " + latest.toString("dd MM yyyy"));
        int months = Months.monthsBetween(earliest.withDayOfMonth(1).withMillisOfDay(0),
                latest.withDayOfMonth(1).withMillisOfDay(0)).getMonths();

        int offset = getXAxisOffset(accountType);
        ArrayList<Entry> values = new ArrayList<Entry>(months + 1);
        for (int i = 0; i < months + 1; i++) {
            long start = earliest.dayOfMonth().withMinimumValue().millisOfDay().withMinimumValue().toDate().getTime();
            long end = earliest.dayOfMonth().withMaximumValue().millisOfDay().withMaximumValue().toDate().getTime();
            float balance = (float) mAccountsDbAdapter.getAccountsBalance(accountUIDList, start, end).asDouble();
            values.add(new Entry(balance, i + offset));
            Log.d(TAG, accountType + earliest.toString(" MMM yyyy") + ", balance = " + balance);
            earliest = earliest.plusMonths(1);
        }

        return values;
    }

    private void setEarliestAndLatestTimestamps(List<AccountType> accountTypeList) {
        TransactionsDbAdapter transactionsDbAdapter = TransactionsDbAdapter.getInstance();
        mEarliestTimestampsMap.clear();
        mLatestTimestampsMap.clear();
        for (AccountType type : accountTypeList) {
            mEarliestTimestampsMap.put(type, transactionsDbAdapter.getTimestampOfEarliestTransaction(type));
            mLatestTimestampsMap.put(type, transactionsDbAdapter.getTimestampOfLatestTransaction(type));
        }

        List<Long> timestamps = new ArrayList<Long>(mEarliestTimestampsMap.values());
        timestamps.addAll(mLatestTimestampsMap.values());
        Collections.sort(timestamps);
        Log.d(TAG, "X-axis timestamps list: " + timestamps.toString());
        mEarliestTransactionTimestamp = timestamps.get(0);
        mLatestTransactionTimestamp = timestamps.get(timestamps.size() - 1);
    }

    private int getXAxisOffset(AccountType accountType) {
        return Months.monthsBetween(
                new LocalDate(mEarliestTransactionTimestamp).withDayOfMonth(1),
                new LocalDate(mEarliestTimestampsMap.get(accountType)).withDayOfMonth(1)
                ).getMonths();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.chart_actions, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_toggle_average_lines).setVisible(mChartDataPresent);
        // hide pie/bar chart specific menu items
        menu.findItem(R.id.menu_order_by_size).setVisible(false);
        menu.findItem(R.id.menu_toggle_labels).setVisible(false);
        menu.findItem(R.id.menu_percentage_mode).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_toggle_legend: {
                mChart.getLegend().setEnabled(!mChart.getLegend().isEnabled());
                mChart.invalidate();
                break;
            }
            case R.id.menu_toggle_average_lines: {
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
                break;
            }
            case android.R.id.home: {
                finish();
                break;
            }
        }
        return true;
    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
        if (e == null) return;
        ((TextView) findViewById(R.id.selected_chart_slice))
                .setText(mChart.getData().getXVals().get(e.getXIndex()) + " - " + e.getVal()
                        + " (" + String.format("%.2f", (e.getVal() / mChart.getData().getDataSetByIndex(dataSetIndex).getYValueSum()) * 100) + " %)");
    }

    @Override
    public void onNothingSelected() {
        ((TextView) findViewById(R.id.selected_chart_slice)).setText("");
    }
}
