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
import androidx.annotation.ColorRes;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.ui.report.barchart.StackedBarChartFragment;
import org.gnucash.android.ui.report.linechart.CashFlowLineChartFragment;
import org.gnucash.android.ui.report.piechart.PieChartFragment;
import org.gnucash.android.ui.report.sheet.BalanceSheetFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Different types of reports
 * <p>This class also contains mappings for the reports of the different types which are available
 * in the system. When adding a new report, make sure to add a mapping in the constructor</p>
 */
public enum ReportType {
    PIE_CHART(0), BAR_CHART(1), LINE_CHART(2), TEXT(3), NONE(4);

    Map<String, Class> mReportTypeMap = new HashMap<>();
    int mValue = 4;

    ReportType(int index){
        mValue = index;
        Context context = GnuCashApplication.getAppContext();
        switch (index){
            case 0:
                mReportTypeMap.put(context.getString(R.string.title_pie_chart), PieChartFragment.class);
                break;
            case 1:
                mReportTypeMap.put(context.getString(R.string.title_bar_chart), StackedBarChartFragment.class);
                break;
            case 2:
                mReportTypeMap.put(context.getString(R.string.title_cash_flow_report), CashFlowLineChartFragment.class);
                break;
            case 3:
                mReportTypeMap.put(context.getString(R.string.title_balance_sheet_report), BalanceSheetFragment.class);
                break;
            case 4:
                break;
        }
    }

    /**
     * Returns the toolbar color to be used for this report type
     * @return Color resource
     */
    public @ColorRes int getTitleColor(){
        switch (mValue){
            case 0:
                return R.color.account_green;
            case 1:
                return R.color.account_red;
            case 2:
                return R.color.account_blue;
            case 3:
                return R.color.account_purple;
            case 4:
            default:
                return R.color.theme_primary;
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
