/*
 * Copyright (c) 2012 - 2015 Ngewi Fet <ngewif@gmail.com>
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
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;

import org.gnucash.android.R;

/**
 * Fragment for displaying transaction preferences
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
@TargetApi(11)
public class TransactionsPreferenceFragment extends PreferenceFragment implements OnPreferenceChangeListener{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.fragment_transaction_preferences);
		ActionBar actionBar = ((SherlockPreferenceActivity) getActivity()).getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.title_transaction_preferences);		
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		
		SharedPreferences manager = PreferenceManager.getDefaultSharedPreferences(getActivity());
		String defaultTransactionType = manager.getString(getString(R.string.key_default_transaction_type), "DEBIT");
		Preference pref = findPreference(getString(R.string.key_default_transaction_type));		
		setLocalizedSummary(pref, defaultTransactionType);
		pref.setOnPreferenceChangeListener(this);

        pref = findPreference(getString(R.string.key_use_double_entry));
        pref.setOnPreferenceChangeListener(this);

        Preference preference = findPreference(getString(R.string.key_delete_all_transactions));
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                deleteAllTransactions();
                return true;
            }
        });
	}


	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.getKey().equals(getString(R.string.key_use_double_entry))){
            ((SettingsActivity)getActivity()).setImbalanceAccountsHidden((Boolean)newValue);
        } else {
            setLocalizedSummary(preference, newValue.toString());
        }
		return true;
	}

    /**
     * Deletes all transactions in the system
     */
    public void deleteAllTransactions(){
        DeleteAllTransacationsConfirmationDialog deleteTransactionsConfirmationDialog =
                DeleteAllTransacationsConfirmationDialog.newInstance();
        deleteTransactionsConfirmationDialog.show(getFragmentManager(), "transaction_settings");
    }

    /**
     * Localizes the label for DEBIT/CREDIT in the settings summary
     * @param preference Preference whose summary is to be localized
     * @param value New value for the preference summary
     */
	private void setLocalizedSummary(Preference preference, String value){
		String localizedLabel = value.equals("DEBIT") ? getString(R.string.label_debit) : getActivity().getString(R.string.label_credit);
		Preference pref = findPreference(getString(R.string.key_default_transaction_type));
		pref.setSummary(localizedLabel);
	}
	
}
