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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.db.adapter.CommoditiesDbAdapter;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.ui.settings.dialog.DeleteAllTransactionsConfirmationDialog;

import java.util.Currency;
import java.util.List;

/**
 * Fragment for displaying transaction preferences
 * @author Ngewi Fet <ngewif@gmail.com>
 *
 */
public class TransactionsPreferenceFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getPreferenceManager().setSharedPreferencesName(BooksDbAdapter.getInstance().getActiveBookUID());

		ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(R.string.title_transaction_preferences);		
	}

	@Override
	public void onCreatePreferences(Bundle bundle, String s) {
		addPreferencesFromResource(R.xml.fragment_transaction_preferences);
	}

	@Override
	public void onResume() {
		super.onResume();
		
		SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
		String defaultTransactionType = sharedPreferences.getString(
				getString(R.string.key_default_transaction_type),
				getString(R.string.label_debit));
		Preference pref = findPreference(getString(R.string.key_default_transaction_type));		
		setLocalizedSummary(pref, defaultTransactionType);
		pref.setOnPreferenceChangeListener(this);

        pref = findPreference(getString(R.string.key_use_double_entry));
        pref.setOnPreferenceChangeListener(this);

		String keyCompactView = getString(R.string.key_use_compact_list);
		SwitchPreferenceCompat switchPref = (SwitchPreferenceCompat) findPreference(keyCompactView);
		switchPref.setChecked(sharedPreferences.getBoolean(keyCompactView, false));

		String keySaveBalance = getString(R.string.key_save_opening_balances);
		switchPref = (SwitchPreferenceCompat) findPreference(keySaveBalance);
		switchPref.setChecked(sharedPreferences.getBoolean(keySaveBalance, false));

		String keyDoubleEntry = getString(R.string.key_use_double_entry);
		switchPref = (SwitchPreferenceCompat) findPreference(keyDoubleEntry);
		switchPref.setChecked(sharedPreferences.getBoolean(keyDoubleEntry, true));

		Preference preference = findPreference(getString(R.string.key_delete_all_transactions));
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showDeleteTransactionsDialog();
                return true;
            }
        });
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.getKey().equals(getString(R.string.key_use_double_entry))){
			boolean useDoubleEntry = (Boolean) newValue;
			setImbalanceAccountsHidden(useDoubleEntry);
        } else {
            setLocalizedSummary(preference, newValue.toString());
        }
		return true;
	}

    /**
     * Deletes all transactions in the system
     */
    public void showDeleteTransactionsDialog(){
        DeleteAllTransactionsConfirmationDialog deleteTransactionsConfirmationDialog =
                DeleteAllTransactionsConfirmationDialog.newInstance();
        deleteTransactionsConfirmationDialog.show(getActivity().getSupportFragmentManager(), "transaction_settings");
    }


	/**
	 * Hide all imbalance accounts when double-entry mode is disabled
	 * @param useDoubleEntry flag if double entry is enabled or not
	 */
	private void setImbalanceAccountsHidden(boolean useDoubleEntry) {
		String isHidden = useDoubleEntry ? "0" : "1";
		AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();
        List<Commodity> commodities = accountsDbAdapter.getCommoditiesInUse();
		for (Commodity commodity : commodities) {
			String uid = accountsDbAdapter.getImbalanceAccountUID(commodity);
			if (uid != null){
				accountsDbAdapter.updateRecord(uid, DatabaseSchema.AccountEntry.COLUMN_HIDDEN, isHidden);
			}
		}
	}
    /**
     * Localizes the label for DEBIT/CREDIT in the settings summary
     * @param preference Preference whose summary is to be localized
     * @param value New value for the preference summary
     */
	private void setLocalizedSummary(Preference preference, String value){
		String localizedLabel = value.equals("DEBIT") ? getString(R.string.label_debit) : getActivity().getString(R.string.label_credit);
		preference.setSummary(localizedLabel);
	}
	
}
