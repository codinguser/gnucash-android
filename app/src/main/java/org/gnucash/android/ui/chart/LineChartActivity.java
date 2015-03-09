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
import java.util.List;

/**
 *
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class LineChartActivity extends PassLockActivity implements OnChartValueSelectedListener {

    private static final String TAG = "LineChartActivity";

    private LineChart mChart;
    private AccountsDbAdapter mAccountsDbAdapter;

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

        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setPinchZoom(false);

        mChart.getAxisRight().setEnabled(false);

        setData();

        Legend l = mChart.getLegend();
        l.setPosition(Legend.LegendPosition.RIGHT_OF_CHART_INSIDE);
        l.setForm(Legend.LegendForm.CIRCLE);

        mChart.animateX(2500);
        mChart.invalidate();
    }

    private ArrayList<Entry> setData(AccountType accountType) {
        TransactionsDbAdapter transactionsDbAdapter = TransactionsDbAdapter.getInstance();
        LocalDateTime earliest = new LocalDateTime(transactionsDbAdapter.getTimestampOfEarliestTransaction(accountType));
        LocalDateTime latest = new LocalDateTime(transactionsDbAdapter.getTimestampOfLatestTransaction(accountType));
        Log.w(TAG, "START: " + earliest.toString("dd MM yyyy"));
        Log.w(TAG, "END: " + latest.toString("dd MM yyyy"));
        int diff = Months.monthsBetween(earliest.withDayOfMonth(1).withMillisOfDay(0), latest.withDayOfMonth(1).withMillisOfDay(0)).getMonths();
        Log.w(TAG, "DIFF: " + diff);
        // TODO change type to float
        double[] months = new double[diff + 1];

        List<String> skippedUUID = new ArrayList<String>();
        for (Account account : mAccountsDbAdapter.getSimpleAccountList()) {
            if (account.getAccountType() == accountType && !account.isPlaceholderAccount()) {
                // TODO sum of sub accounts?
                if (mAccountsDbAdapter.getSubAccountCount(account.getUID()) > 0) {
                    skippedUUID.addAll(mAccountsDbAdapter.getDescendantAccountUIDs(account.getUID(), null, null));
                }
                if (!skippedUUID.contains(account.getUID())) {
                    LocalDateTime tmpDate = earliest;
                    for (int i = 0; i < months.length; i++) {
                        Log.i(TAG, "ACCOUNT " + account.getName());
                        Log.i(TAG, "MONTHS " + tmpDate.toString("MMMM yyyy"));

                        long start = tmpDate.dayOfMonth().withMinimumValue().millisOfDay().withMinimumValue().toDate().getTime();
                        long end = tmpDate.dayOfMonth().withMaximumValue().millisOfDay().withMaximumValue().toDate().getTime();
                        double balance = mAccountsDbAdapter.getAccountBalance(account.getUID(), start, end).absolute().asDouble();
                        months[i] += balance;

                        Log.i(TAG, "Balance of current month " + balance);
                        Log.i(TAG, "Balance total " + months[i]);

                        tmpDate = tmpDate.plusMonths(1);
                    }
                }
            }
        }

        ArrayList<Entry> values = new ArrayList<Entry>();
        for (int i = 0; i < months.length; i++) {
            Log.w(TAG, accountType + " MONTH " + months[i]);
            values.add(new Entry((float) months[i], i));
        }

        Log.w(TAG, accountType + " ENTRY SIZE " + values.size());

        return values;
    }

    private void setData() {
        //TODO comparing Joda dates with TIME!

        LineDataSet set1 = new LineDataSet(setData(AccountType.INCOME), AccountType.INCOME.toString());
        set1.setDrawCubic(true);
        set1.setDrawFilled(true);
        set1.setDrawCircles(true);
        set1.setLineWidth(2f);
        set1.setCircleSize(5f);
        set1.setColor(Color.rgb(104, 241, 175));
        set1.setFillColor(getResources().getColor(R.color.account_green));

        LineDataSet set2 = new LineDataSet(setData(AccountType.EXPENSE), AccountType.EXPENSE.toString());
        set2.setDrawCubic(true);
        set2.setDrawFilled(true);
        set2.setDrawCircles(true);
        set2.setLineWidth(2f);
        set2.setCircleSize(5f);
        set2.setColor(Color.RED);
        set2.setFillColor(getResources().getColor(R.color.account_red));

        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        dataSets.add(set1);
        dataSets.add(set2);

        LocalDate start = new LocalDate(2014, 1, 1);
        LocalDate end = new LocalDate(2015, 1, 1);

        ArrayList<String> xVals = new ArrayList<String>();
        while (!start.isAfter(end)) {
            xVals.add(start.toString("MMM yy"));
            Log.w(TAG, "xVals " + start.toString("MM yy"));
            start = start.plusMonths(1);
        }

        Log.w(TAG, "X AXIS SIZE " + xVals.size());

        LineData data = new LineData(xVals, dataSets);

        mChart.setData(data);
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
