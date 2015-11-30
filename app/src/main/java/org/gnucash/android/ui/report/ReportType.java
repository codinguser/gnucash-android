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

import android.content.Context;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.ui.report.barchart.BarChartFragment;
import org.gnucash.android.ui.report.barchart.CashFlowBarChartFragment;
import org.gnucash.android.ui.report.linechart.CashFlowLineChartFragment;
import org.gnucash.android.ui.report.piechart.PieChartFragment;
import org.gnucash.android.ui.report.sheet.BalanceSheetFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Different types of reports
 */
public enum ReportType {
    PIE_CHART(0), BAR_CHART(1), LINE_CHART(2), TEXT(3), NONE(4);

    Map<String, Class> mReportTypeMap = new HashMap<>();

    ReportType(int index){
        Context context = GnuCashApplication.getAppContext();
        switch (index){
            case 0:
                mReportTypeMap.put("Pie Chart", PieChartFragment.class);
                break;
            case 1:
                mReportTypeMap.put(context.getString(R.string.title_cash_flow_report), CashFlowBarChartFragment.class);
                mReportTypeMap.put("Bar Chart", BarChartFragment.class);
                break;
            case 2:
                mReportTypeMap.put("Income/Expense Statement", CashFlowLineChartFragment.class);
                break;
            case 3:
                mReportTypeMap.put("Balance Sheet", BalanceSheetFragment.class);
                break;
            case 4:
                break;
        }
    }

    public List<String> getReportNames(){
        return new ArrayList<>(mReportTypeMap.keySet());
    }

    public BaseReportFragment getFragment(String name){
        BaseReportFragment fragment = null;
        try {
            fragment = (BaseReportFragment) mReportTypeMap.get(name).newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return fragment;
    }
}
