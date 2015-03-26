package org.gnucash.android.ui.chart;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
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
    private static final String SELECTED_VALUE_PATTERN = "%s - %.2f (%.2f %%)";

    private static final int[] COLORS = { Color.rgb(104, 241, 175), Color.RED };

    private BarChart mChart;
    private List<AccountType> mAccountTypeList;
    private Map<AccountType, Long> mEarliestTimestampsMap = new HashMap<AccountType, Long>();
    private Map<AccountType, Long> mLatestTimestampsMap = new HashMap<AccountType, Long>();
    private long mEarliestTransactionTimestamp;
    private long mLatestTransactionTimestamp;
    private boolean mTotalPercentageMode = true;
    private boolean mChartDataPresent = true;

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
        mAccountTypeList = new ArrayList<AccountType>(Arrays.asList(AccountType.INCOME, AccountType.EXPENSE));
        mChart.setData(getDataSet());

        Legend l = mChart.getLegend();
        l.setForm(Legend.LegendForm.CIRCLE);
        l.setPosition(Legend.LegendPosition.RIGHT_OF_CHART_INSIDE);

        mChart.animateX(3000);
        mChart.invalidate();
    }

    private BarData getDataSet() {
        AccountsDbAdapter mAccountsDbAdapter = AccountsDbAdapter.getInstance();

        setEarliestAndLatestTimestamps(mAccountTypeList);

        if (mEarliestTransactionTimestamp == 0) {
            if (mLatestTransactionTimestamp == 0) {
                return getEmptyDataSet();
            }
            for (Map.Entry<AccountType, Long> entry : mEarliestTimestampsMap.entrySet()) {
                if (entry.getValue() == 0) {
                    mAccountTypeList.remove(entry.getKey());
                }
            }
            Log.d(TAG, mAccountTypeList.toString());
            setEarliestAndLatestTimestamps(mAccountTypeList);
        }

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
        // conversion enum list to string array
        set.setStackLabels(mAccountTypeList.toString().substring(1, mAccountTypeList.toString().length() - 1).split(", "));
        set.setColors(Arrays.copyOfRange(COLORS, 0, mAccountTypeList.size()));

        dataSets.add(set);

        return new BarData(xVals, dataSets);
    }

    private BarData getEmptyDataSet() {
        mChartDataPresent = false;

        ArrayList<String> xValues = new ArrayList<String>();
        ArrayList<BarEntry> yValues = new ArrayList<BarEntry>();
        for (int i = 0; i < 3; i++) {
            xValues.add("");
            yValues.add(new BarEntry(i % 2 == 0 ? 5f : 4.5f, i));
        }
        String noDataMsg = getResources().getString(R.string.label_chart_no_data);
        BarDataSet set = new BarDataSet(yValues, noDataMsg);
        set.setDrawValues(false);
        set.setColor(Color.LTGRAY);

        mChart.getAxisLeft().setAxisMaxValue(10);
        mChart.getAxisLeft().setDrawLabels(false);
        mChart.getXAxis().setDrawLabels(false);
        mChart.setTouchEnabled(false);
        ((TextView) findViewById(R.id.selected_chart_slice)).setText(noDataMsg);

        return new BarData(xValues, new ArrayList<BarDataSet>(Arrays.asList(set)));
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
        mEarliestTransactionTimestamp = timestamps.get(0);
        mLatestTransactionTimestamp = timestamps.get(timestamps.size() - 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.chart_actions, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_percentage_mode).setVisible(mChartDataPresent);
        // hide pie and bar chart specific menu items
        menu.findItem(R.id.menu_order_by_size).setVisible(false);
        menu.findItem(R.id.menu_toggle_labels).setVisible(false);
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
            case R.id.menu_percentage_mode: {
                mTotalPercentageMode = !mTotalPercentageMode;
                int msgId = mTotalPercentageMode ? R.string.toast_chart_percentage_mode_total
                        : R.string.toast_chart_percentage_mode_current_bar;
                Toast.makeText(this, msgId, Toast.LENGTH_LONG).show();
                break;
            }
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
