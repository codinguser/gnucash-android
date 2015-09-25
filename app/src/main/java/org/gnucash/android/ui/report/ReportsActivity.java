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

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.common.BaseDrawerActivity;
import org.gnucash.android.ui.report.dialog.DateRangePickerDialogFragment;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * base activity for reporting
 *
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ReportsActivity extends BaseDrawerActivity implements AdapterView.OnItemSelectedListener,
        DatePickerDialog.OnDateSetListener, DateRangePickerDialogFragment.OnDateRangeSetListener{

    static final int[] COLORS = {
            Color.parseColor("#17ee4e"), Color.parseColor("#cc1f09"), Color.parseColor("#3940f7"),
            Color.parseColor("#f9cd04"), Color.parseColor("#5f33a8"), Color.parseColor("#e005b6"),
            Color.parseColor("#17d6ed"), Color.parseColor("#e4a9a2"), Color.parseColor("#8fe6cd"),
            Color.parseColor("#8b48fb"), Color.parseColor("#343a36"), Color.parseColor("#6decb1"),
            Color.parseColor("#f0f8ff"), Color.parseColor("#5c3378"), Color.parseColor("#a6dcfd"),
            Color.parseColor("#ba037c"), Color.parseColor("#708809"), Color.parseColor("#32072c"),
            Color.parseColor("#fddef8"), Color.parseColor("#fa0e6e"), Color.parseColor("#d9e7b5")
    };

    @Bind(R.id.time_range_spinner) Spinner mTimeRangeSpinner;
    @Bind(R.id.report_account_type_spinner) Spinner mAccountTypeSpinner;

    TransactionsDbAdapter mTransactionsDbAdapter;
    AccountType mAccountType = AccountType.EXPENSE;

    public enum GroupInterval {WEEK, MONTH, QUARTER, YEAR, ALL}

    private long mReportStartTime = -1;
    private long mReportEndTime = -1;

    GroupInterval mReportGroupInterval = GroupInterval.MONTH;

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


        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.report_time_range,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTimeRangeSpinner.setAdapter(adapter);
        mTimeRangeSpinner.setOnItemSelectedListener(this);

        ArrayAdapter<AccountType> dataAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                Arrays.asList(AccountType.EXPENSE, AccountType.INCOME));
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAccountTypeSpinner.setAdapter(dataAdapter);
        mAccountTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mAccountType = (AccountType) mAccountTypeSpinner.getSelectedItem();
                updateAccountTypeOnFragments();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //nothing to see here, move along
            }
        });

        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager
                    .beginTransaction();

            fragmentTransaction.replace(R.id.fragment_container, new ReportSummaryFragment());
            fragmentTransaction.commit();
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        View timeRangeLayout = findViewById(R.id.time_range_layout);
        View dateRangeDivider = findViewById(R.id.date_range_divider);
        if (timeRangeLayout != null && dateRangeDivider != null) {
            if (fragment instanceof ReportSummaryFragment || fragment instanceof BalanceSheetFragment) {
                timeRangeLayout.setVisibility(View.GONE);
                dateRangeDivider.setVisibility(View.GONE);
            } else {
                timeRangeLayout.setVisibility(View.VISIBLE);
                dateRangeDivider.setVisibility(View.VISIBLE);
            }
        }
        View accountTypeSpinner = findViewById(R.id.report_account_type_spinner);
        if (accountTypeSpinner != null) {
            if (fragment instanceof LineChartFragment) {
                accountTypeSpinner.setVisibility(View.GONE);
            } else {
                accountTypeSpinner.setVisibility(View.VISIBLE);
            }
        }
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

    public AccountType getAccountType(){
        return mAccountType;
    }

    /**
     * Updates the reporting time range for all listening fragments
     */
    private void updateDateRangeOnFragment(){
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof ReportOptionsListener){
                ((ReportOptionsListener) fragment).onTimeRangeUpdated(mReportStartTime, mReportEndTime);
            }
        }
    }

    /**
     * Updates the account type for all attached fragments which are listening
     */
    private void updateAccountTypeOnFragments(){
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof ReportOptionsListener){
                ((ReportOptionsListener) fragment).onAccountTypeUpdated(mAccountType);
            }
        }
    }

    /**
     * Updates the report grouping interval on all attached fragments which are listening
     */
    private void updateGroupingOnFragments(){
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof ReportOptionsListener){
                ((ReportOptionsListener) fragment).onGroupingUpdated(mReportGroupInterval);
            }
        }
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

            case R.id.group_by_month:
                item.setChecked(true);
                mReportGroupInterval = GroupInterval.MONTH;
                updateGroupingOnFragments();
                return true;

            case R.id.group_by_quarter:
                item.setChecked(true);
                mReportGroupInterval = GroupInterval.QUARTER;
                updateGroupingOnFragments();
                return true;

            case R.id.group_by_year:
                item.setChecked(true);
                mReportGroupInterval = GroupInterval.YEAR;
                updateGroupingOnFragments();
                return true;

            case android.R.id.home:
                super.onOptionsItemSelected(item);

            default:
                return false;
        }

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mReportEndTime = System.currentTimeMillis();
        switch (position){
            case 0: //ALL TIME
                mReportStartTime = -1;
                mReportEndTime = -1;
                break;
            case 1: //current month
                mReportStartTime = new DateTime().dayOfMonth().withMinimumValue().toDate().getTime();
                mReportEndTime = new DateTime().dayOfMonth().withMaximumValue().toDate().getTime();
                break;
            case 2: // last 3 months. x-2, x-1, x
                mReportStartTime = new LocalDate().minusMonths(2).toDate().getTime();
                break;
            case 3:
                mReportStartTime = new LocalDate().minusMonths(5).toDate().getTime();
                break;
            case 4:
                mReportStartTime = new LocalDate().minusMonths(11).toDate().getTime();
                break;
            case 5:
                String mCurrencyCode = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.key_report_currency), Money.DEFAULT_CURRENCY_CODE);
                long earliestTransactionTime = mTransactionsDbAdapter.getTimestampOfEarliestTransaction(mAccountType, mCurrencyCode);
                long latestTransactionTime = mTransactionsDbAdapter.getTimestampOfLatestTransaction(mAccountType, mCurrencyCode);
                DialogFragment rangeFragment = DateRangePickerDialogFragment.newInstance(
                        earliestTransactionTime,
                        new LocalDate().plusDays(1).toDate().getTime(),
                        this);
                rangeFragment.show(getSupportFragmentManager(), "range_dialog");
                break;
        }
        if (position != 5){ //the date picker will trigger the update itself
            updateDateRangeOnFragment();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //nothing to see here, move along
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, monthOfYear, dayOfMonth);
        mReportStartTime = calendar.getTimeInMillis();
        updateDateRangeOnFragment();
    }

    @Override
    public void onDateRangeSet(Date startDate, Date endDate) {
        mReportStartTime = startDate.getTime();
        mReportEndTime = endDate.getTime();
        updateDateRangeOnFragment();

    }
}
