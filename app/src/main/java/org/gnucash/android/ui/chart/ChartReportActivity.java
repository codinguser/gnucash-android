/*
 * Copyright (c) 2015 Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
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

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.passcode.PassLockActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

/**
 * Allows to select chart by type
 *
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class ChartReportActivity extends PassLockActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //it is necessary to set the view first before calling super because of the nav drawer in BaseDrawerActivity
        setContentView(R.layout.activity_chart_report);
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(R.string.title_reports);

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
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencyNames);
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

        findViewById(R.id.pie_chart_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(view.getContext(), PieChartActivity.class));
            }
        });
        findViewById(R.id.line_chart_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(view.getContext(), LineChartActivity.class));
            }
        });
        findViewById(R.id.bar_chart_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(view.getContext(), BarChartActivity.class));
            }
        });

    }

}
