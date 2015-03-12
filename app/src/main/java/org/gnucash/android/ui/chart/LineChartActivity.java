package org.gnucash.android.ui.chart;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
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

    private LineChart mChart;
    private AccountsDbAdapter mAccountsDbAdapter;
    private Map<AccountType, Long> mEarliestTimestampsMap = new HashMap<AccountType, Long>();
    private Map<AccountType, Long> mLatestTimestampsMap = new HashMap<AccountType, Long>();
    private long mEarliestTransactionTimestamp;
    private long mLatestTransactionTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //it is necessary to set the view first before calling super because of the nav drawer in BaseDrawerActivity
        setContentView(R.layout.activity_line_chart);
        super.onCreate(savedInstanceState);

        mAccountsDbAdapter = AccountsDbAdapter.getInstance();

        mChart = new LineChart(this);
        ((LinearLayout) findViewById(R.id.chart)).addView(mChart);

        mChart.setOnChartValueSelectedListener(this);
        mChart.setDescription("");
        // TEST THIS!!!
        mChart.setNoDataTextDescription("You need to provide data for the chart.");

        mChart.getAxisRight().setEnabled(false);

        setData();

        Legend l = mChart.getLegend();
        l.setPosition(Legend.LegendPosition.RIGHT_OF_CHART_INSIDE);
        l.setForm(Legend.LegendForm.CIRCLE);

        mChart.animateX(2500);
        mChart.invalidate();
    }

    private ArrayList<Entry> setData(AccountType accountType) {
        List<String> accountUIDList = new ArrayList<String>();
        for (Account account : mAccountsDbAdapter.getSimpleAccountList()) {
            if (account.getAccountType() == accountType && !account.isPlaceholderAccount()) {
                accountUIDList.add(account.getUID());
            }
        }

        LocalDateTime earliest = new LocalDateTime(mEarliestTimestampsMap.get(accountType));
        LocalDateTime latest = new LocalDateTime(mLatestTimestampsMap.get(accountType));
        Log.w(TAG, "START: " + earliest.toString("dd MM yyyy"));
        Log.w(TAG, "END: " + latest.toString("dd MM yyyy"));
        int months = Months.monthsBetween(earliest.withDayOfMonth(1).withMillisOfDay(0),
                latest.withDayOfMonth(1).withMillisOfDay(0)).getMonths();

        int offset = getXAxisOffset(accountType);
        Log.w(TAG, "OFFSET OF " + accountType + " IS " + offset);
        ArrayList<Entry> values = new ArrayList<Entry>(months + 1);
        for (int i = 0; i < months + 1; i++) {
            long start = earliest.dayOfMonth().withMinimumValue().millisOfDay().withMinimumValue().toDate().getTime();
            long end = earliest.dayOfMonth().withMaximumValue().millisOfDay().withMaximumValue().toDate().getTime();

            float balance = (float) mAccountsDbAdapter.getAccountsBalance(accountUIDList, start, end).asDouble();
            values.add(new Entry(balance, i + offset));

            Log.w(TAG, accountType + earliest.toString(" MMMM yyyy") + ", balance = " + balance);

            earliest = earliest.plusMonths(1);
        }

        return values;
    }

    private void setData() {
        //TODO comparing Joda dates with TIME!

        setEarliestAndLatestTimestamps(Arrays.asList(AccountType.INCOME, AccountType.EXPENSE));

        LocalDate startDate = new LocalDate(mEarliestTransactionTimestamp);
        LocalDate endDate = new LocalDate(mLatestTransactionTimestamp);
        ArrayList<String> xValues = new ArrayList<String>();
        while (!startDate.isAfter(endDate)) {
            xValues.add(startDate.toString(X_AXIS_PATTERN));
            Log.w(TAG, "xValues " + startDate.toString("MM yy"));
            startDate = startDate.plusMonths(1);
        }

        LineDataSet set1 = new LineDataSet(setData(AccountType.INCOME), AccountType.INCOME.toString());
        set1.setDrawFilled(true);
        set1.setDrawCircles(true);
        set1.setLineWidth(2f);
        set1.setCircleSize(5f);
        set1.setColor(Color.rgb(104, 241, 175));
        set1.setFillColor(getResources().getColor(R.color.account_green));

        LineDataSet set2 = new LineDataSet(setData(AccountType.EXPENSE), AccountType.EXPENSE.toString());
        set2.setDrawFilled(true);
        set2.setDrawCircles(true);
        set2.setLineWidth(2f);
        set2.setCircleSize(5f);
        set2.setColor(Color.RED);
        set2.setFillColor(getResources().getColor(R.color.account_red));

        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        dataSets.add(set1);
        dataSets.add(set2);

        LineData data = new LineData(xValues, dataSets);

        mChart.setData(data);
    }

    private void setEarliestAndLatestTimestamps(List<AccountType> accountTypeList) {
        TransactionsDbAdapter transactionsDbAdapter = TransactionsDbAdapter.getInstance();
        for (AccountType type : accountTypeList) {
            mEarliestTimestampsMap.put(type, transactionsDbAdapter.getTimestampOfEarliestTransaction(type));
            mLatestTimestampsMap.put(type, transactionsDbAdapter.getTimestampOfLatestTransaction(type));
        }

        //TODO what if account has no transaction and list contain zero items
        List<Long> timestamps = new ArrayList<Long>(mEarliestTimestampsMap.values());
        timestamps.addAll(mLatestTimestampsMap.values());
        Collections.sort(timestamps);
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
