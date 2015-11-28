/*
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

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.ui.util.Refreshable;
import org.joda.time.LocalDateTime;
import org.joda.time.Months;
import org.joda.time.Years;

import java.util.Currency;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Base class for report fragments.
 * <p>All report fragments should extend this class</p>
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public abstract class BaseReportFragment extends Fragment implements
        OnChartValueSelectedListener, ReportOptionsListener, Refreshable {

    /**
     * Color for chart with no data
     */
    public static final int NO_DATA_COLOR = Color.LTGRAY;

    protected static String TAG = "BaseReportFragment";

    /**
     * Reporting period start time
     */
    protected long mReportStartTime = -1;
    /**
     * Reporting period end time
     */
    protected long mReportEndTime = -1;

    /**
     * Account type for which to display reports
     */
    protected AccountType mAccountType;

    /**
     * Currency for which to display reports
     */
    protected Currency mCurrency;

    /**
     * Intervals in which to group reports
     */
    protected ReportsActivity.GroupInterval mGroupInterval = ReportsActivity.GroupInterval.MONTH;

    /**
     * Pattern to use to display selected chart values
     */
    public static final String SELECTED_VALUE_PATTERN = "%s - %.2f (%.2f %%)";

    @Nullable @Bind(R.id.selected_chart_slice) protected TextView mSelectedValueTextView;

    /**
     * Get the color which should use for decorating the app bar for this report
     * @return Color resource
     */
    public @ColorRes int getTitleColor(){
        return R.color.theme_primary;
    }

    /**
     * Return the title of this report
     * @return Title string identifier
     */
    public abstract @StringRes int getTitle();

    /**
     * Returns the layout resource to use for this report
     * @return Layout resource identifier
     */
    public abstract @LayoutRes int getLayoutResource();

    /**
     * Returns what kind of report this is
     * @return Type of report
     */
    public abstract ReportType getReportType();

    /**
     * Return {@code true} if this report fragment requires account type options.
     * <p>Sub-classes should implement this method. The base implementation returns {@code true}</p>
     * @return {@code true} if the fragment makes use of account type options, {@code false} otherwise
     */
    public boolean requiresAccountTypeOptions(){
        return true;
    }

    /**
     * Return {@code true} if this report fragment requires time range options.
     * <p>Base implementation returns true</p>
     * @return {@code true} if the report fragment requires time range options, {@code false} otherwise
     */
    public boolean requiresTimeRangeOptions(){
        return true;
    }

    /**
     * Generates the data for the report
     * <p>This method should not call any methods which modify the UI as it will be run in a background thread
     * <br>Put any code to update the UI in {@link #displayReport()}
     * </p>
     */
    protected abstract void generateReport();

    /**
     * Update the view after the report chart has been generated <br/>
     * Sub-classes should call to the base method
     */
    protected abstract void displayReport();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TAG = this.getClass().getSimpleName();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutResource(), container, false);
        ButterKnife.bind(this, view);
        return view;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(getTitle());

        setHasOptionsMenu(true);
        mCurrency = Currency.getInstance(GnuCashApplication.getDefaultCurrencyCode());

        ReportsActivity reportsActivity = (ReportsActivity) getActivity();
        mReportStartTime = reportsActivity.getReportStartTime();
        mReportEndTime = reportsActivity.getReportEndTime();
        mAccountType = reportsActivity.getAccountType();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((ReportsActivity)getActivity()).setAppBarColor(getTitleColor());
        ((ReportsActivity) getActivity()).updateReportTypeSpinner(getReportType());
        toggleBaseReportingOptions();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(getActivity() instanceof ReportsActivity))
            throw new RuntimeException("Report fragments can only be used with the ReportsActivity");
    }

    private void toggleBaseReportingOptions() {
        final Activity activity = getActivity();
        View timeRangeLayout = activity.findViewById(R.id.time_range_layout);
        View dateRangeDivider = activity.findViewById(R.id.date_range_divider);
        if (timeRangeLayout != null && dateRangeDivider != null) {
            if (requiresTimeRangeOptions()) {
                timeRangeLayout.setVisibility(View.VISIBLE);
                dateRangeDivider.setVisibility(View.VISIBLE);
            } else {
                timeRangeLayout.setVisibility(View.GONE);
                dateRangeDivider.setVisibility(View.GONE);
            }
        }

        View accountTypeSpinner = activity.findViewById(R.id.report_account_type_spinner);
        if (requiresAccountTypeOptions()) {
            accountTypeSpinner.setVisibility(View.VISIBLE);
        } else {
            accountTypeSpinner.setVisibility(View.GONE);
        }
    }


    /**
     * Calculates difference between two date values accordingly to {@code mGroupInterval}
     * @param start start date
     * @param end end date
     * @return difference between two dates or {@code -1}
     */
    protected int getDateDiff(LocalDateTime start, LocalDateTime end) {
        switch (mGroupInterval) {
            case QUARTER:
                int y = Years.yearsBetween(start.withDayOfYear(1).withMillisOfDay(0), end.withDayOfYear(1).withMillisOfDay(0)).getYears();
                return (getQuarter(end) - getQuarter(start) + y * 4);
            case MONTH:
                return Months.monthsBetween(start.withDayOfMonth(1).withMillisOfDay(0), end.withDayOfMonth(1).withMillisOfDay(0)).getMonths();
            case YEAR:
                return Years.yearsBetween(start.withDayOfYear(1).withMillisOfDay(0), end.withDayOfYear(1).withMillisOfDay(0)).getYears();
            default:
                return -1;
        }
    }


    /**
     * Returns a quarter of the specified date
     * @param date date
     * @return a quarter
     */
    protected int getQuarter(LocalDateTime date) {
        return ((date.getMonthOfYear() - 1) / 3 + 1);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.chart_actions, menu);
    }

    @Override
    public void refresh() {
        new AsyncTask<Void, Void, Void>() {
            ProgressBar progressBar = (ProgressBar) getActivity().findViewById(R.id.progress_indicator);
            @Override
            protected void onPreExecute() {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected Void doInBackground(Void... params) {
                generateReport();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                displayReport();
                progressBar.setVisibility(View.GONE);
            }
        }.execute();
    }

    /**
     * Charts do not support account specific refreshes in general.
     * So we provide a base implementation which just calls {@link #refresh()}
     *
     * @param uid GUID of relevant item to be refreshed
     */
    @Override
    public void refresh(String uid) {
        refresh();
    }

    @Override
    public void onGroupingUpdated(ReportsActivity.GroupInterval groupInterval) {
        if (mGroupInterval != groupInterval) {
            mGroupInterval = groupInterval;
            refresh();
        }
    }

    @Override
    public void onTimeRangeUpdated(long start, long end) {
        if (mReportStartTime != start || mReportEndTime != end) {
            mReportStartTime = start;
            mReportEndTime = end;
            refresh();
        }
    }

    @Override
    public void onAccountTypeUpdated(AccountType accountType) {
        if (mAccountType != accountType) {
            mAccountType = accountType;
            refresh();
        }
    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
        //nothing to see here, move along
    }

    @Override
    public void onNothingSelected() {
        if (mSelectedValueTextView != null)
            mSelectedValueTextView.setText("Select any part of the chart to view details");
    }
}
