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
package org.gnucash.android.ui.account;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.db.TransactionsDbAdapter;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.ui.util.Refreshable;
import org.gnucash.android.ui.widget.WidgetConfigurationActivity;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import java.util.Currency;
import java.util.List;

/**
 * Delete confirmation dialog for accounts.
 * It is displayed when deleting an account which has transactions or sub-accounts, and the user
 * has the option to either move the transactions/sub-accounts, or delete them.
 * If an account has no transactions, it is deleted immediately with no confirmation required
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class DeleteAccountDialogFragment extends SherlockDialogFragment {

    /**
     * Spinner for selecting the account to move the transactions to
     */
    Spinner mTransactionsDestinationAccountSpinner;

    Spinner mAccountsDestinationAccountSpinner;

    /**
     * Dialog positive button. Ok to moving the transactions
     */
    Button mOkButton;

    /**
     * Cancel button
     */
    Button mCancelButton;

    /**
     * GUID of account from which to move the transactions
     */
    String mOriginAccountUID = null;

    View mAccountOptionsView;
    View mTransactionOptionsView;
    RadioButton mMoveAccountsRadioButton;
    RadioButton mMoveTransactionsRadioButton;
    RadioButton mDeleteAccountsRadioButton;
    RadioButton mDeleteTransactionsRadioButton;

    private int mTransactionCount;
    private int mSubAccountCount;

    /**
     * Creates new instance of the delete confirmation dialog and provides parameters for it
     * @param accountUID GUID of the account to be deleted
     * @return New instance of the delete confirmation dialog
     */
    public static DeleteAccountDialogFragment newInstance(String accountUID) {
        DeleteAccountDialogFragment fragment = new DeleteAccountDialogFragment();
        fragment.mOriginAccountUID = accountUID;
        fragment.mSubAccountCount = AccountsDbAdapter.getInstance().getSubAccountCount(accountUID);
        fragment.mTransactionCount = TransactionsDbAdapter.getInstance().getTransactionsCount(accountUID);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_account_delete, container, false);
        mTransactionOptionsView = view.findViewById(R.id.transactions_options);
        ((TextView)mTransactionOptionsView.findViewById(R.id.title_content)).setText(R.string.section_header_transactions);
        ((TextView)mTransactionOptionsView.findViewById(R.id.description)).setText(R.string.label_delete_account_transactions_description);
        mDeleteTransactionsRadioButton = (RadioButton) mTransactionOptionsView.findViewById(R.id.radio_delete);
        mDeleteTransactionsRadioButton.setText(R.string.label_delete_transactions);
        mMoveTransactionsRadioButton = ((RadioButton)mTransactionOptionsView.findViewById(R.id.radio_move));
        mTransactionsDestinationAccountSpinner = (Spinner) mTransactionOptionsView.findViewById(R.id.target_accounts_spinner);

        mAccountOptionsView = view.findViewById(R.id.accounts_options);
        ((TextView)mAccountOptionsView.findViewById(R.id.title_content)).setText(R.string.section_header_subaccounts);
        ((TextView)mAccountOptionsView.findViewById(R.id.description)).setText(R.string.label_delete_account_subaccounts_description);
        mDeleteAccountsRadioButton = (RadioButton) mAccountOptionsView.findViewById(R.id.radio_delete);
        mDeleteAccountsRadioButton.setText(R.string.label_delete_sub_accounts);
        mMoveAccountsRadioButton = (RadioButton)mAccountOptionsView.findViewById(R.id.radio_move);
        mAccountsDestinationAccountSpinner = (Spinner) mAccountOptionsView.findViewById(R.id.target_accounts_spinner);

        mTransactionOptionsView.setVisibility(mTransactionCount > 0 ? View.VISIBLE : View.GONE);
        mAccountOptionsView.setVisibility(mSubAccountCount > 0 ? View.VISIBLE : View.GONE);

        mCancelButton = (Button) view.findViewById(R.id.btn_cancel);
        mOkButton = (Button) view.findViewById(R.id.btn_save);
        mOkButton.setText(R.string.alert_dialog_ok_delete);
        mOkButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.content_discard_holo_light,0,0,0);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String accountName = AccountsDbAdapter.getInstance().getAccountName(mOriginAccountUID);
        getDialog().setTitle(getString(R.string.alert_dialog_ok_delete) + ": " + accountName);
        AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();
        List<String> descendantAccountUIDs = accountsDbAdapter.getDescendantAccountUIDs(mOriginAccountUID, null, null);

        String currencyCode = accountsDbAdapter.getCurrencyCode(mOriginAccountUID);
        AccountType accountType = accountsDbAdapter.getAccountType(mOriginAccountUID);

        String transactionDeleteConditions = "(" + DatabaseSchema.AccountEntry.COLUMN_UID + " != ? AND "
                + DatabaseSchema.AccountEntry.COLUMN_CURRENCY               + " = ? AND "
                + DatabaseSchema.AccountEntry.COLUMN_TYPE         + " = ? AND "
                + DatabaseSchema.AccountEntry.COLUMN_PLACEHOLDER + " = 0 AND "
                + DatabaseSchema.AccountEntry.COLUMN_UID + " NOT IN ('" + TextUtils.join("','", descendantAccountUIDs) + "')"
                + ")";
        Cursor cursor = accountsDbAdapter.fetchAccountsOrderedByFullName(transactionDeleteConditions,
                new String[]{mOriginAccountUID, currencyCode, accountType.name()});

        SimpleCursorAdapter mCursorAdapter = new QualifiedAccountNameCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_item, cursor);
        mCursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTransactionsDestinationAccountSpinner.setAdapter(mCursorAdapter);

        //target accounts for transactions and accounts have different conditions
        String accountMoveConditions = "(" + DatabaseSchema.AccountEntry.COLUMN_UID + " != ? AND "
                + DatabaseSchema.AccountEntry.COLUMN_CURRENCY               + " = ? AND "
                + DatabaseSchema.AccountEntry.COLUMN_TYPE         + " = ? AND "
                + DatabaseSchema.AccountEntry.COLUMN_UID + " NOT IN ('" + TextUtils.join("','", descendantAccountUIDs) + "')"
                + ")";
        cursor = accountsDbAdapter.fetchAccountsOrderedByFullName(accountMoveConditions,
                new String[]{mOriginAccountUID, currencyCode, accountType.name()});
        mCursorAdapter = new QualifiedAccountNameCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_item, cursor);
        mCursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAccountsDestinationAccountSpinner.setAdapter(mCursorAdapter);

        setListeners();

        //this comes after the listeners because of some useful bindings done there
        if (cursor.getCount() == 0){
            mMoveAccountsRadioButton.setEnabled(false);
            mMoveAccountsRadioButton.setChecked(false);
            mDeleteAccountsRadioButton.setChecked(true);
            mMoveTransactionsRadioButton.setEnabled(false);
            mMoveTransactionsRadioButton.setChecked(false);
            mDeleteTransactionsRadioButton.setChecked(true);
            mAccountsDestinationAccountSpinner.setVisibility(View.GONE);
            mTransactionsDestinationAccountSpinner.setVisibility(View.GONE);
        }
    }

    /**
     * Binds click listeners for the dialog buttons
     */
    protected void setListeners(){
        mMoveAccountsRadioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAccountsDestinationAccountSpinner.setEnabled(isChecked);
            }
        });

        mMoveTransactionsRadioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mTransactionsDestinationAccountSpinner.setEnabled(isChecked);
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mOkButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();

                if (mMoveTransactionsRadioButton.isChecked()){
                    long targetAccountId = mTransactionsDestinationAccountSpinner.getSelectedItemId();
                    //move all the splits
                    SplitsDbAdapter.getInstance().updateRecords(DatabaseSchema.SplitEntry.COLUMN_ACCOUNT_UID + " = ?",
                            new String[]{mOriginAccountUID}, DatabaseSchema.SplitEntry.COLUMN_ACCOUNT_UID, accountsDbAdapter.getUID(targetAccountId));
                }

                if (mMoveAccountsRadioButton.isChecked()){
                    long targetAccountId = mAccountsDestinationAccountSpinner.getSelectedItemId();
                    AccountsDbAdapter.getInstance().reassignDescendantAccounts(mOriginAccountUID, accountsDbAdapter.getUID(targetAccountId));
                }

                if (GnuCashApplication.isDoubleEntryEnabled()){ //reassign splits to imbalance
                    Currency accountCurrency = Currency.getInstance(accountsDbAdapter.getCurrencyCode(mOriginAccountUID));
                    String imbalanceAccountUID = accountsDbAdapter.getOrCreateImbalanceAccountUID(accountCurrency);
                    SplitsDbAdapter.getInstance().updateRecords(
                            DatabaseSchema.SplitEntry.COLUMN_ACCOUNT_UID + "=?",
                            new String[]{mOriginAccountUID},
                            DatabaseSchema.SplitEntry.COLUMN_ACCOUNT_UID,
                            imbalanceAccountUID);
                }

                //now kill them all!!
                accountsDbAdapter.recursiveDeleteAccount(accountsDbAdapter.getID(mOriginAccountUID));

                WidgetConfigurationActivity.updateAllWidgets(getActivity());
                ((Refreshable)getTargetFragment()).refresh();
                dismiss();
            }
        });
    }

}
