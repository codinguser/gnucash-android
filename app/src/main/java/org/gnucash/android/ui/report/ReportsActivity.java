/*
 * Copyright (c) 2015 Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
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

package org.gnucash.android.ui.report;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.passcode.PassLockActivity;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * base activity for reporting
 *
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ReportsActivity extends PassLockActivity {

    static final int[] COLORS = {
            Color.parseColor("#17ee4e"), Color.parseColor("#cc1f09"), Color.parseColor("#3940f7"),
            Color.parseColor("#f9cd04"), Color.parseColor("#5f33a8"), Color.parseColor("#e005b6"),
            Color.parseColor("#17d6ed"), Color.parseColor("#e4a9a2"), Color.parseColor("#8fe6cd"),
            Color.parseColor("#8b48fb"), Color.parseColor("#343a36"), Color.parseColor("#6decb1"),
            Color.parseColor("#a6dcfd"), Color.parseColor("#5c3378"), Color.parseColor("#a6dcfd"),
            Color.parseColor("#ba037c"), Color.parseColor("#708809"), Color.parseColor("#32072c"),
            Color.parseColor("#fddef8"), Color.parseColor("#fa0e6e"), Color.parseColor("#d9e7b5")
    };

    @Bind(R.id.date_range_recyclerview)
    RecyclerView mDateRangeRecyclerView;

    TransactionsDbAdapter mTransactionsDbAdapter;
    AccountType mAccountType = AccountType.EXPENSE;

    List<LocalDate> mDateRange;
    LocalDate mSelectedBeginDate;
    private DateRangeAdapter mDateRangeAdapter;

    public enum RangeInterval {WEEK, MONTH, QUARTER, YEAR, ALL}

    RangeInterval mDateRangeInterval = RangeInterval.MONTH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);
        setUpDrawer();
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(R.string.title_reports);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();
        mDateRange = new ArrayList<>();

        LinearLayoutManager layoutManager
                = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mDateRangeRecyclerView.setLayoutManager(layoutManager);
        mDateRangeAdapter = new DateRangeAdapter();
        mDateRangeRecyclerView.setAdapter(mDateRangeAdapter);

        final List<String> allCurrencyCodes = Arrays.asList(getResources().getStringArray(R.array.key_currency_codes));
        final List<String> allCurrencyNames = Arrays.asList(getResources().getStringArray(R.array.currency_names));

        Currency preferredCurrency = Currency.getInstance(PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext())
                .getString(getString(R.string.key_report_currency), Money.DEFAULT_CURRENCY_CODE));
        List<Currency> currencies = AccountsDbAdapter.getInstance().getCurrencies();
        if (currencies.remove(preferredCurrency)) {
            currencies.add(0, preferredCurrency);
        }
        List<String> currencyNames = new ArrayList<>();
        for (Currency currency : currencies) {
            currencyNames.add(allCurrencyNames.get(allCurrencyCodes.indexOf(currency.getCurrencyCode())));
        }

        Spinner spinner = (Spinner) findViewById(R.id.report_currency_spinner);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(actionBar.getThemedContext(), android.R.layout.simple_spinner_item, currencyNames);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String currencyName = (String) ((Spinner) findViewById(R.id.report_currency_spinner)).getSelectedItem();
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                        .edit()
                        .putString(getString(R.string.key_report_currency), allCurrencyCodes.get(allCurrencyNames.indexOf(currencyName)))
                        .commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager
                    .beginTransaction();

            fragmentTransaction.replace(R.id.fragment_container, new ReportSummaryFragment());
            fragmentTransaction.commit();
        }

        setUpDateRangeGroups();
    }

    /**
     * Sets the color Action Bar and Status bar (where applicable)
     */
    public void setAppBarColor(int color) {
        int resolvedColor = getResources().getColor(color);
        if (getSupportActionBar() != null)
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(resolvedColor));

        if (Build.VERSION.SDK_INT > 20)
            getWindow().setStatusBarColor(GnuCashApplication.darken(resolvedColor));
    }

    private void updateDateRangeOnFragment(){
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        long start = -1, end = -1;
        for (Fragment fragment : fragments) {
            if (fragment instanceof ReportOptionsListener){
                start = mSelectedBeginDate.toDateTimeAtStartOfDay().getMillis();
                LocalDate endDate = new LocalDate();
                switch (mDateRangeInterval) {
                    case WEEK:
                        endDate = mSelectedBeginDate.plusWeeks(1);
                        end = endDate.toDateTimeAtCurrentTime().getMillis();
                        break;
                    case MONTH:
                        endDate = mSelectedBeginDate.plusMonths(1);
                        end = endDate.toDateTimeAtCurrentTime().getMillis();
                        break;
                    case QUARTER:
                        endDate = mSelectedBeginDate.plusMonths(3);
                        end = endDate.toDateTimeAtCurrentTime().getMillis();
                        break;
                    case YEAR:
                        endDate = mSelectedBeginDate.plusYears(1);
                        end = endDate.toDateTimeAtCurrentTime().getMillis();
                        break;
                    case ALL:
                        start = -1;
                        end = -1;
                        break;
                }

                ((ReportOptionsListener) fragment).updateDateRange(start, end, mDateRangeInterval);
            }
        }
    }
    /**
     * Loads the data set which is shown in the recycler list view.
     * The data is the different date ranges for which reports should be loaded
     * @see RangeInterval
     */
    private void setUpDateRangeGroups(){
        String mCurrencyCode = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.key_report_currency), Money.DEFAULT_CURRENCY_CODE);

        LocalDate mEarliestTransactionDate = new LocalDate(mTransactionsDbAdapter.getTimestampOfEarliestTransaction(mAccountType, mCurrencyCode));
        LocalDate mLatestTransactionDate = new LocalDate(mTransactionsDbAdapter.getTimestampOfLatestTransaction(mAccountType, mCurrencyCode));

        mDateRange.clear();
        if (mDateRangeInterval == RangeInterval.ALL){
            mDateRange.add(new LocalDate());
        } else {
            LocalDate iteratorDate = mEarliestTransactionDate;
            while (iteratorDate.isBefore(mLatestTransactionDate)) {
                mDateRange.add(iteratorDate);
                switch (mDateRangeInterval) {
                    case WEEK:
                        iteratorDate = iteratorDate.plusWeeks(1);
                        break;
                    case MONTH:
                        iteratorDate = iteratorDate.plusMonths(1);
                        break;
                    case QUARTER:
                        iteratorDate = iteratorDate.plusMonths(3);
                        break;
                    case YEAR:
                        iteratorDate = iteratorDate.plusYears(1);
                        break;
                }
            }
        }
        mDateRangeAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.report_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_group_reports_by:
                return true;

            case R.id.group_by_week:
                item.setChecked(true);
                item.setChecked(!item.isChecked());
                mDateRangeInterval = RangeInterval.WEEK;
                setUpDateRangeGroups();
                return true;

            case R.id.group_by_month:
                item.setChecked(true);
                mDateRangeInterval = RangeInterval.MONTH;
                setUpDateRangeGroups();
                return true;

            case R.id.group_by_quarter:
                item.setChecked(true);
                mDateRangeInterval = RangeInterval.QUARTER;
                setUpDateRangeGroups();
                return true;

            case R.id.group_by_year:
                item.setChecked(true);
                mDateRangeInterval = RangeInterval.YEAR;
                setUpDateRangeGroups();
                return true;

            case R.id.group_all_time:
                item.setChecked(true);
                mDateRangeInterval = RangeInterval.ALL;
                setUpDateRangeGroups();
                return true;

            case android.R.id.home:
                super.onOptionsItemSelected(item);

            default:
                return false;
        }
    }

    /**
     * RecyclerView adapter which displays the different time ranges for reports
     * The time range is determined by {@link #mDateRangeInterval}
     * @see #setUpDateRangeGroups()
     */
    private class DateRangeAdapter extends RecyclerView.Adapter<DateRangeAdapter.DateViewHolder> {

        int mSelectedPosition = -1;

        @Override
        public DateViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(ReportsActivity.this).inflate(R.layout.card_item_date_range, parent, false);
            return new DateViewHolder(view);
        }

        @Override
        public void onBindViewHolder(DateViewHolder holder, final int position) {
            LocalDate date = mDateRange.get(position);
            String dateString = "";

            switch (mDateRangeInterval){

                case WEEK:
                    dateString = String.format("Week %s of %s", date.toString("w"), date.toString("yyyy"));
                    break;
                case MONTH:
                    dateString = date.toString("MMM yyyy");
                    break;
                case QUARTER:
                    int quarter = (date.getMonthOfYear() / 3) + 1;
                    dateString = String.format("%d quarter of %s", quarter, date.toString("yyyy"));
                    break;
                case YEAR:
                    dateString = date.toString("yyyy");
                    break;
                case ALL:
                    dateString = "All time";
                    break;
            }

            holder.dateText.setText(dateString);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSelectedBeginDate = mDateRange.get(position);
                    updateDateRangeOnFragment();
                    //TODO: fire range to chart fragment
                }
            });
        }

        @Override
        public int getItemCount() {
            return mDateRange.size();
        }

        class DateViewHolder extends RecyclerView.ViewHolder {
            TextView dateText;

            public DateViewHolder(final View itemView) {
                super(itemView);
                dateText = (TextView) itemView.findViewById(R.id.item_date_range);
            }
        }
    }
}
