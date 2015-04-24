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

package org.gnucash.android.ui.settings;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;

import org.gnucash.android.R;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.model.Money;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

/**
 * Fragment for charts configuring
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
@TargetApi(11)
public class ChartPreferenceFragment extends PreferenceFragment implements OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.fragment_chart_preferences);

        ActionBar actionBar = ((SherlockPreferenceActivity) getActivity()).getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Chart Prefs");

        List<Currency> currencyList = AccountsDbAdapter.getInstance().getCurrencies();
        int size = currencyList.size();
        String[] currencyCodes = new String[size];
        for (Currency currency : currencyList) {
            currencyCodes[--size] = currency.getCurrencyCode();
        }

        ListPreference pref = (ListPreference) findPreference(getString(R.string.key_chart_currency));
        pref.setEntryValues(currencyCodes);
        pref.setOnPreferenceChangeListener(this);


        List<String> currencyNames = new ArrayList<>();
        String[] allCurrencyNames = getResources().getStringArray(R.array.currency_names);
        List<String> allCurrencyCodes = Arrays.asList(getResources().getStringArray(R.array.key_currency_codes));
        for (String code : currencyCodes) {
            currencyNames.add(allCurrencyNames[allCurrencyCodes.indexOf(code)]);
        }

        pref.setEntries(currencyNames.toArray(new String[currencyNames.size()]));
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String keyChartCurrency = getString(R.string.key_chart_currency);
        Preference pref = findPreference(keyChartCurrency);
        String chartCurrency = sharedPreferences.getString(keyChartCurrency, null);
        if (chartCurrency != null && !chartCurrency.trim().isEmpty()) {
            pref.setSummary(chartCurrency);
        } else {
            pref.setSummary(sharedPreferences.getString(getString(R.string.key_default_currency), Money.DEFAULT_CURRENCY_CODE));
        }

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        preference.setSummary(newValue.toString());
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit()
                .putString(getString(R.string.key_chart_currency), newValue.toString())
                .commit();

        return true;
    }

}
