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
package org.gnucash.android.ui.report.barchart;

import org.gnucash.android.R;
import org.gnucash.android.ui.report.BaseReportFragment;
import org.gnucash.android.ui.report.ReportType;

/**
 * Cash flow report fragment
 */
public class CashFlowFragment extends BaseReportFragment {

    @Override
    public int getTitle() {
        return R.string.title_cash_flow_report;
    }

    @Override
    public int getLayoutResource() {
        return R.layout.fragment_bar_chart;
    }

    @Override
    public ReportType getReportType() {
        return ReportType.BAR_CHART;
    }

    @Override
    public boolean requiresAccountTypeOptions() {
        return false;
    }

    @Override
    protected void generateReport() {
        //// TODO: 29.11.2015 Generate cash flow report
    }

    @Override
    protected void displayReport() {
        // TODO: 29.11.2015 Display cash flow report
    }
}
