/*
 * Copyright (c) 2014 Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
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

package org.gnucash.android.ui.chart;

import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;

import java.util.List;

/**
 *
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class PieChartActivity extends SherlockFragmentActivity {

    private static final int[] COLORS = {
            Color.parseColor("#17ee4e"), Color.parseColor("#cc1f09"), Color.parseColor("#3940f7"),
            Color.parseColor("#f9cd04"), Color.parseColor("#5f33a8"), Color.parseColor("#e005b6"),
            Color.parseColor("#17d6ed"), Color.parseColor("#e4a9a2"), Color.parseColor("#8fe6cd"),
            Color.parseColor("#8b48fb"), Color.parseColor("#343a36"), Color.parseColor("#6decb1"),
            Color.parseColor("#a6dcfd"), Color.parseColor("#5c3378"), Color.parseColor("#a6dcfd"),
            Color.parseColor("#ba037c"), Color.parseColor("#708809"), Color.parseColor("#32072c"),
            Color.parseColor("#fddef8"), Color.parseColor("#fa0e6e"), Color.parseColor("#d9e7b5")
    };

    private DefaultRenderer mRenderer = new DefaultRenderer();
    private CategorySeries mSeries = new CategorySeries("");
    private GraphicalView mPieChartView;

    private AccountsDbAdapter mAccountsDbAdapter;

    private double mBalanceSum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_reports);

        renderSettings();

        mPieChartView = ChartFactory.getPieChartView(this, mSeries, mRenderer);

        mAccountsDbAdapter = new AccountsDbAdapter(this);
        setDataset(AccountType.EXPENSE);

        ((LinearLayout) findViewById(R.id.chart)).addView(mPieChartView,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }


    private void setDataset(AccountType type) {
        mRenderer.removeAllRenderers();
        mSeries.clear();
        mBalanceSum = 0;

        List<Account> accountList = mAccountsDbAdapter.getSimpleAccountList();
        for (Account account : accountList) {
            if (account.getAccountType() == type && !account.isPlaceholderAccount()) {
                double balance = mAccountsDbAdapter.getAccountBalance(account.getUID()).asDouble();
                // ToDo What with negative?
                if (balance > 0) {
                    mBalanceSum += balance;
                    mSeries.add(account.getName(), balance);
                    SimpleSeriesRenderer renderer = new SimpleSeriesRenderer();
                    renderer.setColor(COLORS[(mSeries.getItemCount() - 1) % COLORS.length]);
                    mRenderer.addSeriesRenderer(renderer);
                }
            }
        }

        mPieChartView.repaint();
    }

    private void renderSettings() {
        mRenderer.setChartTitle("Expenses");
        mRenderer.setChartTitleTextSize(25);

        mRenderer.setShowLabels(true);
        mRenderer.setLabelsColor(Color.BLACK);
        mRenderer.setLabelsTextSize(15);

        mRenderer.setShowLegend(false);

        mRenderer.setClickEnabled(true);
        mRenderer.setZoomButtonsVisible(true);
        mRenderer.setStartAngle(180);
    }

}
