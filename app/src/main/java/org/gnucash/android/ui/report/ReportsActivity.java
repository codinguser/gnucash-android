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
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.ui.common.BaseDrawerActivity;
import org.gnucash.android.ui.common.Refreshable;
import org.gnucash.android.ui.util.dialog.DateRangePickerDialogFragment;
import org.joda.time.LocalDate;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.Bind;

/**
 * Activity for displaying report fragments (which must implement {@link BaseReportFragment})
 * <p>In order to add new reports, extend the {@link BaseReportFragment} class to provide the view
 * for the report. Then add the report mapping in {@link ReportType} constructor depending on what
 * kind of report it is. The report will be dynamically included at runtime.</p>
 *
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class ReportsActivity extends BaseDrawerActivity implements AdapterView.OnItemSelectedListener,
        DatePickerDialog.OnDateSetListener, DateRangePickerDialogFragment.OnDateRangeSetListener,
        Refreshable{

    public static final int[] COLORS = {
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
    @Bind(R.id.toolbar_spinner) Spinner mReportTypeSpinner;

    private TransactionsDbAdapter mTransactionsDbAdapter;
    private AccountType mAccountType = AccountType.EXPENSE;
    private ReportType mReportType = ReportType.NONE;
    private ReportsOverviewFragment mReportsOverviewFragment;

    public enum GroupInterval {WEEK, MONTH, QUARTER, YEAR, ALL}

    // default time range is the last 3 months
    private long mReportPeriodStart = new LocalDate().minusMonths(2).dayOfMonth().withMinimumValue().toDate().getTime();
    private long mReportPeriodEnd = new LocalDate().plusDays(1).toDate().getTime();

    private GroupInterval mReportGroupInterval = GroupInterval.MONTH;
    private boolean mSkipNextReportTypeSelectedRun = false;

    AdapterView.OnItemSelectedListener mReportTypeSelectedListener = new AdapterView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (mSkipNextReportTypeSelectedRun){
                mSkipNextReportTypeSelectedRun = false;
            } else {
                String reportName = parent.getItemAtPosition(position).toString();
                loadFragment(mReportType.getFragment(reportName));
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            //nothing to see here, move along
        }
    };

    @Override
    public int getContentView() {
        return R.layout.activity_reports;
    }

    @Override
    public int getTitleRes() {
        return R.string.title_reports;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.report_time_range,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTimeRangeSpinner.setAdapter(adapter);
        mTimeRangeSpinner.setOnItemSelectedListener(this);
        mTimeRangeSpinner.setSelection(1);

        ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter.createFromResource(this,
                R.array.report_account_types, android.R.layout.simple_spinner_item);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAccountTypeSpinner.setAdapter(dataAdapter);
        mAccountTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                switch(position) {
                    default:
                    case 0:
                        mAccountType = AccountType.EXPENSE;
                        break;
                    case 1:
                        mAccountType = AccountType.INCOME;
                }
                updateAccountTypeOnFragments();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //nothing to see here, move along
            }
        });

        mReportsOverviewFragment = new ReportsOverviewFragment();

        if (savedInstanceState == null) {
            loadFragment(mReportsOverviewFragment);
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);

        if (fragment instanceof BaseReportFragment) {
            BaseReportFragment reportFragment = (BaseReportFragment)fragment;
            updateReportTypeSpinner(reportFragment.getReportType(), getString(reportFragment.getTitle()));
        }
    }

    /**
     * Load the provided fragment into the view replacing the previous one
     * @param fragment BaseReportFragment instance
     */
    private void loadFragment(BaseReportFragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager
                .beginTransaction();

        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    /**
     * Update the report type spinner
     */
    public void updateReportTypeSpinner(ReportType reportType, String reportName) {
        if (reportType == mReportType)//if it is the same report type, don't change anything
            return;

        mReportType = reportType;
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(actionBar.getThemedContext(),
                android.R.layout.simple_list_item_1,
                mReportType.getReportNames());

        mSkipNextReportTypeSelectedRun = true; //selection event will be fired again
        mReportTypeSpinner.setAdapter(arrayAdapter);
        mReportTypeSpinner.setSelection(arrayAdapter.getPosition(reportName));
        mReportTypeSpinner.setOnItemSelectedListener(mReportTypeSelectedListener);


        toggleToolbarTitleVisibility();
    }

    public void toggleToolbarTitleVisibility() {
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        if (mReportType == ReportType.NONE){
            mReportTypeSpinner.setVisibility(View.GONE);
        } else {
            mReportTypeSpinner.setVisibility(View.VISIBLE);
        }
        actionBar.setDisplayShowTitleEnabled(mReportType == ReportType.NONE);
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

    /**
     * Updates the reporting time range for all listening fragments
     */
    private void updateDateRangeOnFragment(){
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof ReportOptionsListener){
                ((ReportOptionsListener) fragment).onTimeRangeUpdated(mReportPeriodStart, mReportPeriodEnd);
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
        mReportPeriodEnd = new LocalDate().plusDays(1).toDate().getTime();
        switch (position){
            case 0: //current month
                mReportPeriodStart = new LocalDate().dayOfMonth().withMinimumValue().toDate().getTime();
                break;
            case 1: // last 3 months. x-2, x-1, x
                mReportPeriodStart = new LocalDate().minusMonths(2).dayOfMonth().withMinimumValue().toDate().getTime();
                break;
            case 2:
                mReportPeriodStart = new LocalDate().minusMonths(5).dayOfMonth().withMinimumValue().toDate().getTime();
                break;
            case 3:
                mReportPeriodStart = new LocalDate().minusMonths(11).dayOfMonth().withMinimumValue().toDate().getTime();
                break;
            case 4: //ALL TIME
                mReportPeriodStart = -1;
                mReportPeriodEnd = -1;
                break;
            case 5:
                String mCurrencyCode = GnuCashApplication.getDefaultCurrencyCode();
                long earliestTransactionTime = mTransactionsDbAdapter.getTimestampOfEarliestTransaction(mAccountType, mCurrencyCode);
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
        mReportPeriodStart = calendar.getTimeInMillis();
        updateDateRangeOnFragment();
    }

    @Override
    public void onDateRangeSet(Date startDate, Date endDate) {
        mReportPeriodStart = startDate.getTime();
        mReportPeriodEnd = endDate.getTime();
        updateDateRangeOnFragment();

    }

    public AccountType getAccountType(){
        return mAccountType;
    }

    /**
     * Return the end time of the reporting period
     * @return Time in millis
     */
    public long getReportPeriodEnd() {
        return mReportPeriodEnd;
    }

    /**
     * Return the start time of the reporting period
     * @return Time in millis
     */
    public long getReportPeriodStart() {
        return mReportPeriodStart;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK && mReportType != ReportType.NONE){
    		loadFragment(mReportsOverviewFragment);
    		return true;
    	}
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void refresh() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof Refreshable){
                ((Refreshable) fragment).refresh();
            }
        }
    }

    @Override
    /**
     * Just another call to refresh
     */
    public void refresh(String uid) {
        refresh();
    }
}
