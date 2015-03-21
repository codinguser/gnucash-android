package org.gnucash.android.ui.chart;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

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
public class BarChartActivity extends PassLockActivity implements OnChartValueSelectedListener {

    private static final String TAG = "BarChartActivity";
    private static final String X_AXIS_PATTERN = "MMM YY";
    private static final String SELECTED_VALUE_PATTERN = "%s : %.2f (%.2f %%)";

    private static final int[] COLORS = {
            Color.rgb(104, 241, 175), Color.RED
    };

    private BarChart mChart;
    private List<AccountType> mAccountTypeList;
    private Map<AccountType, Long> mEarliestTimestampsMap = new HashMap<AccountType, Long>();
    private Map<AccountType, Long> mLatestTimestampsMap = new HashMap<AccountType, Long>();
    private long mEarliestTransactionTimestamp;
    private long mLatestTransactionTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_line_chart);
        super.onCreate(savedInstanceState);

        mChart = new com.github.mikephil.charting.charts.BarChart(this);
        ((LinearLayout) findViewById(R.id.chart)).addView(mChart);
        mChart.setOnChartValueSelectedListener(this);
        mChart.setDescription("");
//        mChart.setValueFormatter(new LargeValueFormatter());
        mChart.setDrawValuesForWholeStack(false);
        mChart.setDrawBarShadow(false);
//        XLabels xl  = mChart.getXLabels();
//        xl.setCenterXLabelText(true);
        mChart.getAxisLeft().setValueFormatter(new LargeValueFormatter());
        mChart.getAxisRight().setEnabled(false);

//        mAccountTypeList = Arrays.asList(AccountType.EXPENSE, AccountType.INCOME);
        mAccountTypeList = Arrays.asList(AccountType.INCOME, AccountType.EXPENSE);
        setStackedData();

        Legend l = mChart.getLegend();
        l.setForm(Legend.LegendForm.SQUARE);
        l.setPosition(Legend.LegendPosition.RIGHT_OF_CHART_INSIDE);

        mChart.animateX(3000);
        mChart.invalidate();
    }

    protected void setStackedData() {
        AccountsDbAdapter mAccountsDbAdapter = AccountsDbAdapter.getInstance();

        setEarliestAndLatestTimestamps(mAccountTypeList);

        LocalDateTime start = new LocalDateTime(mEarliestTransactionTimestamp).withDayOfMonth(1).withMillisOfDay(0);
        LocalDateTime end = new LocalDateTime(mLatestTransactionTimestamp).withDayOfMonth(1).withMillisOfDay(0);
        Log.w(TAG, "X AXIS START DATE: " + start.toString("dd MM yyyy"));
        Log.w(TAG, "X AXIS END DATE: " + end.toString("dd MM yyyy"));

        Map<AccountType, List<String>> accountUIDMap = new HashMap<AccountType, List<String>>();
        for (AccountType accountType : mAccountTypeList) {
            List<String> accountUIDList = new ArrayList<String>();

            for (Account account : mAccountsDbAdapter.getSimpleAccountList()) {
                if (account.getAccountType() == accountType && !account.isPlaceholderAccount()) {
                    accountUIDList.add(account.getUID());

                }
                accountUIDMap.put(accountType, accountUIDList);
            }
        }

        ArrayList<BarDataSet> dataSets = new ArrayList<BarDataSet>();
        ArrayList<BarEntry> values = new ArrayList<BarEntry>();
        ArrayList<String> xVals = new ArrayList<String>();
        int z = 0;
        while (!start.isAfter(end)) {
            xVals.add(start.toString(X_AXIS_PATTERN));
            Log.i(TAG, "xVals " + start.toString("MM yy"));

            long startPeriod = start.dayOfMonth().withMinimumValue().millisOfDay().withMinimumValue().toDate().getTime();
            long endPeriod = start.dayOfMonth().withMaximumValue().millisOfDay().withMaximumValue().toDate().getTime();

            float stackedValues[] = new float[mAccountTypeList.size()];
            int i = 0;
            for (Map.Entry<AccountType, List<String>> entry : accountUIDMap.entrySet()) {
                float balance = (float) mAccountsDbAdapter.getAccountsBalance(entry.getValue(), startPeriod, endPeriod).absolute().asDouble();
                stackedValues[i++] = balance;
                Log.w(TAG, entry.getKey() + "" + start.toString(" MMMM yyyy") + ", balance = " + balance);
            }
            values.add(new BarEntry(stackedValues, z));
            z++;

            start = start.plusMonths(1);
        }

        BarDataSet set = new BarDataSet(values, "");
//        set.setValueFormatter();
        set.setStackLabels(new String[] { AccountType.INCOME.toString(), AccountType.EXPENSE.toString() });
        set.setColors(COLORS);

        dataSets.add(set);

        BarData bd = new BarData(xVals, dataSets);
        mChart.setData(bd);
    }

    private void setEarliestAndLatestTimestamps(List<AccountType> accountTypeList) {
        TransactionsDbAdapter transactionsDbAdapter = TransactionsDbAdapter.getInstance();
        for (AccountType type : accountTypeList) {
            mEarliestTimestampsMap.put(type, transactionsDbAdapter.getTimestampOfEarliestTransaction(type));
            mLatestTimestampsMap.put(type, transactionsDbAdapter.getTimestampOfLatestTransaction(type));
        }

        List<Long> timestamps = new ArrayList<Long>(mEarliestTimestampsMap.values());
        timestamps.addAll(mLatestTimestampsMap.values());
        Collections.sort(timestamps);
        mEarliestTransactionTimestamp = timestamps.get(0);
        mLatestTransactionTimestamp = timestamps.get(timestamps.size() - 1);
    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
        if (e == null) return;
        BarEntry entry = (BarEntry) e;
        String label = mChart.getData().getXVals().get(entry.getXIndex());
        double value = entry.getVals()[h.getStackIndex()];
        double percent = value / mChart.getData().getDataSetByIndex(dataSetIndex).getYValueSum() * 100;
        ((TextView) findViewById(R.id.selected_chart_slice)).setText(String.format(SELECTED_VALUE_PATTERN, label, value, percent));
    }

    @Override
    public void onNothingSelected() {
        ((TextView) findViewById(R.id.selected_chart_slice)).setText("");
    }
}
