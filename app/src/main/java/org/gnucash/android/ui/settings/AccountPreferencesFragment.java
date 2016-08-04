/*
 * Copyright (c) 2013 Ngewi Fet <ngewif@gmail.com>
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.CommoditiesDbAdapter;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.settings.dialog.DeleteAllAccountsConfirmationDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Account settings fragment inside the Settings activity
 *
 * @author Ngewi Fet <ngewi.fet@gmail.com>
 * @author Oleksandr Tyshkovets <olexandr.tyshkovets@gmail.com>
 */
public class AccountPreferencesFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener{

    List<CharSequence> mCurrencyEntries = new ArrayList<>();
    List<CharSequence> mCurrencyEntryValues = new ArrayList<>();

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.fragment_account_preferences);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setTitle(R.string.title_account_preferences);

        Cursor cursor = CommoditiesDbAdapter.getInstance().fetchAllRecords(DatabaseSchema.CommodityEntry.COLUMN_MNEMONIC + " ASC");
        while(cursor.moveToNext()){
            String code = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.CommodityEntry.COLUMN_MNEMONIC));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.CommodityEntry.COLUMN_FULLNAME));
            mCurrencyEntries.add(code + " - " + name);
            mCurrencyEntryValues.add(code);
        }
        cursor.close();
    }

    @Override
    public void onResume() {
        super.onResume();

        String defaultCurrency = GnuCashApplication.getDefaultCurrencyCode();
        Preference pref = findPreference(getString(R.string.key_default_currency));
        String currencyName = CommoditiesDbAdapter.getInstance().getCommodity(defaultCurrency).getFullname();
        pref.setSummary(currencyName);
        pref.setOnPreferenceChangeListener(this);

        CharSequence[] entries = new CharSequence[mCurrencyEntries.size()];
        CharSequence[] entryValues = new CharSequence[mCurrencyEntryValues.size()];
        ((ListPreference) pref).setEntries(mCurrencyEntries.toArray(entries));
        ((ListPreference) pref).setEntryValues(mCurrencyEntryValues.toArray(entryValues));

        Preference preference = findPreference(getString(R.string.key_import_accounts));
        preference.setOnPreferenceClickListener(this);

        preference = findPreference(getString(R.string.key_delete_all_accounts));
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showDeleteAccountsDialog();
                return true;
            }
        });

        preference = findPreference(getString(R.string.key_create_default_accounts));
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.title_create_default_accounts)
                        .setMessage(R.string.msg_confirm_create_default_accounts_setting)
                        .setIcon(R.drawable.ic_warning_black_24dp)
                        .setPositiveButton(R.string.btn_create_accounts, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                AccountsActivity.createDefaultAccounts(Money.DEFAULT_CURRENCY_CODE, getActivity());
                            }
                        })
                        .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create()
                        .show();

                return true;
            }
        });
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        if (key.equals(getString(R.string.key_import_accounts))){
            AccountsActivity.startXmlFileChooser(this);
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals(getString(R.string.key_default_currency))){
            GnuCashApplication.setDefaultCurrencyCode(newValue.toString());
            String fullname = CommoditiesDbAdapter.getInstance().getCommodity(newValue.toString()).getFullname();
            preference.setSummary(fullname);
            return true;
        }
        return false;
    }

    /**
     * Show the dialog for deleting accounts
     */
    public void showDeleteAccountsDialog(){
        DeleteAllAccountsConfirmationDialog deleteConfirmationDialog = DeleteAllAccountsConfirmationDialog.newInstance();
        deleteConfirmationDialog.show(getActivity().getSupportFragmentManager(), "account_settings");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case AccountsActivity.REQUEST_PICK_ACCOUNTS_FILE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    AccountsActivity.importXmlFileFromIntent(getActivity(), data, null);
                }
                break;
        }
    }
}
