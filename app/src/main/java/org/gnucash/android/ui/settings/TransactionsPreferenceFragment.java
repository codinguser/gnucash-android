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
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.SwitchPreferenceCompat;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.ui.settings.dialog.DeleteAllTransactionsConfirmationDialog;

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
	public void onCreatePreferences(Bundle bundle,
									String s) {

		addPreferencesFromResource(R.xml.fragment_transaction_preferences);
	}

	@Override
	public void onResume() {

		super.onResume();

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(GnuCashApplication.getAppContext());

		//
		// Default Transaction Type computing mode
		//

		String keyDefaultTransactionType = getString(R.string.key_default_transaction_type);
		String defaultTransactionTypeKey = sharedPreferences.getString(keyDefaultTransactionType,
																	   AccountType.KEY_USE_NORMAL_BALANCE_EXPENSE);

		Preference pref = findPreference(keyDefaultTransactionType);
		setPrefSummary(pref,
					   defaultTransactionTypeKey);

		pref.setOnPreferenceChangeListener(this);

		//
		// Double entry
		//

		pref = findPreference(getString(R.string.key_use_double_entry));
		pref.setOnPreferenceChangeListener(this);

		//
		// Compact list
		//

		String                 keyCompactView = getString(R.string.key_use_compact_list);
		SwitchPreferenceCompat switchPref     = (SwitchPreferenceCompat) findPreference(keyCompactView);
		switchPref.setChecked(sharedPreferences.getBoolean(keyCompactView, false));

		//
		// Display negative signums
		//

		String keyDisplayNegativeSignumInSplits = getString(R.string.key_display_negative_signum_in_splits);
		switchPref = (SwitchPreferenceCompat) findPreference(keyDisplayNegativeSignumInSplits);
		switchPref.setChecked(sharedPreferences.getBoolean(keyDisplayNegativeSignumInSplits,
														   false));
		switchPref.setOnPreferenceChangeListener(this);

		//
		// Save opening balance
		//

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
	public boolean onPreferenceChange(Preference preference,
									  Object newValue) {

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(GnuCashApplication.getAppContext());

		//
		// Preference : key_default_transaction_type
		//

		if (preference.getKey()
					  .equals(getString(R.string.key_default_transaction_type))) {

			// Store the new value of the Preference
			sharedPreferences.edit()
							 .putString(getString(R.string.key_default_transaction_type),
										newValue.toString())
							 .commit();

			setPrefSummary(preference,
						   ((String) newValue));
		}

		//
		// Preference : key_use_double_entry
		//

		if (preference.getKey()
					  .equals(getString(R.string.key_use_double_entry))) {

			boolean useDoubleEntry = (Boolean) newValue;
			setImbalanceAccountsHidden(useDoubleEntry);

		}

		//
		// Preference : key_display_negative_signum_in_splits
		//

		if (preference.getKey()
					  .equals(getString(R.string.key_display_negative_signum_in_splits))) {

			// Store the new value of the Preference
			sharedPreferences.edit()
							 .putBoolean(getString(R.string.key_display_negative_signum_in_splits),
										 Boolean.valueOf(newValue.toString()))
							 .commit();
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
	 * Localizes the label for AUTOMATIC/DEBIT/CREDIT in the settings summary
	 *
     * @param preference Preference whose summary is to be localized
	 * @param defaultTransactionTypeKey New defaultTransactionTypeKey for the preference summary
     */
	private void setPrefSummary(Preference preference,
								String defaultTransactionTypeKey) {

		String localizedLabel = AccountType.KEY_USE_NORMAL_BALANCE_EXPENSE.equals(defaultTransactionTypeKey)
								? getString(R.string.label_use_account_usual_balance_expense_mode)
								: AccountType.KEY_USE_NORMAL_BALANCE_INCOME.equals(defaultTransactionTypeKey)
								  ? getString(R.string.label_use_account_usual_balance_income_mode)
								  : AccountType.KEY_DEBIT.equals(defaultTransactionTypeKey)
									? getString(R.string.label_debit)
									: getString(R.string.label_credit);


		preference.setSummary(localizedLabel);
	}
	
}
