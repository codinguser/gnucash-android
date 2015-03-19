package org.gnucash.android.ui.chart;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.LargeValueFormatter;

import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.ui.passcode.PassLockActivity;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class BarChartActivity extends PassLockActivity {

    private static final String TAG = "BarChartActivity";

    private static final int[] COLORS = {
            Color.rgb(104, 241, 175), Color.RED
    };

    private BarChart mChart;
    private List<AccountType> mAccountTypeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_line_chart);
        super.onCreate(savedInstanceState);

        mChart = new com.github.mikephil.charting.charts.BarChart(this);
        ((LinearLayout) findViewById(R.id.chart)).addView(mChart);
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

        LocalDateTime start = new LocalDateTime().minusMonths(5).withDayOfMonth(1).withMillisOfDay(0);
        LocalDateTime end = new LocalDateTime().withDayOfMonth(1).withMillisOfDay(0);
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
            xVals.add(start.toString("MMM yy"));
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

}
