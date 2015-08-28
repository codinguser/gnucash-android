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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.passcode.PassLockActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);
        setUpDrawer();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(R.string.title_reports);
        actionBar.setDisplayHomeAsUpEnabled(true);

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
}
