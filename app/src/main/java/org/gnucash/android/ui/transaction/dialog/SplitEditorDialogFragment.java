/*
 * Copyright (c) 2014 Ngewi Fet <ngewif@gmail.com>
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
package org.gnucash.android.ui.transaction.dialog;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.AccountsDbAdapter;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.SplitsDbAdapter;
import org.gnucash.android.model.*;
import org.gnucash.android.ui.UxArgument;
import org.gnucash.android.ui.transaction.TransactionFormFragment;
import org.gnucash.android.ui.transaction.TransactionsActivity;
import org.gnucash.android.ui.util.AmountInputFormatter;
import org.gnucash.android.ui.util.TransactionTypeToggleButton;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

/**
 * Dialog for editing the splits in a transaction
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class SplitEditorDialogFragment extends DialogFragment {

    private LinearLayout mSplitsLinearLayout;
    private TextView mImbalanceTextView;
    private Button mAddSplit;
    private Button mSaveButton;
    private Button mCancelButton;

    private AccountsDbAdapter mAccountsDbAdapter;
    private SplitsDbAdapter mSplitsDbAdapter;
    private Cursor mCursor;
    private SimpleCursorAdapter mCursorAdapter;
    private List<View> mSplitItemViewList;
    private String mAccountUID;

    private BalanceTextWatcher mBalanceUpdater = new BalanceTextWatcher();
    private BigDecimal mBaseAmount = BigDecimal.ZERO;

    private List<String> mRemovedSplitUIDs = new ArrayList<String>();

    private boolean mMultiCurrency = false;
    /**
     * Create and return a new instance of the fragment with the appropriate paramenters
     * @param baseAmountString String with base amount which is being split
     * @return New instance of SplitEditorDialogFragment
     */
    public static SplitEditorDialogFragment newInstance(String baseAmountString){
        SplitEditorDialogFragment fragment = new SplitEditorDialogFragment();
        Bundle args = new Bundle();
        args.putString(UxArgument.AMOUNT_STRING, baseAmountString);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_split_editor, container, false);
        mSplitsLinearLayout = (LinearLayout) view.findViewById(R.id.split_list_layout);

        mImbalanceTextView = (TextView) view.findViewById(R.id.imbalance_textview);

        mAddSplit   = (Button) view.findViewById(R.id.btn_add_split);
        mSaveButton = (Button) view.findViewById(R.id.btn_save);
        mCancelButton       = (Button) view.findViewById(R.id.btn_cancel);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);

        getDialog().setTitle(R.string.title_transaction_splits);

        mSplitItemViewList = new ArrayList<View>();
        mSplitsDbAdapter = SplitsDbAdapter.getInstance();

        //we are editing splits for a new transaction.
        // But the user may have already created some splits before. Let's check
        List<Split> splitList = ((TransactionFormFragment) getTargetFragment()).getSplitList();
        {
            Currency currency = null;
            for (Split split : splitList) {
                if (currency == null) {
                    currency = split.getAmount().getCurrency();
                } else if (currency != split.getAmount().getCurrency()) {
                    mMultiCurrency = true;
                }
            }
        }

        initArgs();
        if (!splitList.isEmpty()) {
            //aha! there are some splits. Let's load those instead
            loadSplitViews(splitList);
        } else {
            final Currency currency = Currency.getInstance(mAccountsDbAdapter.getAccountCurrencyCode(mAccountUID));
            Split split = new Split(new Money(mBaseAmount, currency), mAccountUID);
            AccountType accountType = mAccountsDbAdapter.getAccountType(mAccountUID);
            TransactionType transactionType = Transaction.getTypeForBalance(accountType, mBaseAmount.signum() < 0);
            split.setType(transactionType);
            View view = addSplitView(split);
            view.findViewById(R.id.input_accounts_spinner).setEnabled(false);
            view.findViewById(R.id.btn_remove_split).setVisibility(View.GONE);
        }

        setListeners();
        updateTotal();
    }

    private void loadSplitViews(List<Split> splitList) {
        for (Split split : splitList) {
            addSplitView(split);
        }
        if (mMultiCurrency) {
            enableAllControls(false);
        }
    }

    private void enableAllControls(boolean b) {
        for (View splitView : mSplitItemViewList) {
            EditText splitMemoEditText = (EditText) splitView.findViewById(R.id.input_split_memo);
            final EditText splitAmountEditText = (EditText) splitView.findViewById(R.id.input_split_amount);
            ImageButton removeSplitButton = (ImageButton) splitView.findViewById(R.id.btn_remove_split);
            Spinner accountsSpinner = (Spinner) splitView.findViewById(R.id.input_accounts_spinner);
            final TextView splitCurrencyTextView = (TextView) splitView.findViewById(R.id.split_currency_symbol);
            final TextView splitUidTextView = (TextView) splitView.findViewById(R.id.split_uid);
            final TransactionTypeToggleButton splitTypeButton = (TransactionTypeToggleButton) splitView.findViewById(R.id.btn_split_type);
            splitMemoEditText.setEnabled(b);
            splitAmountEditText.setEnabled(b);
            removeSplitButton.setEnabled(b);
            accountsSpinner.setEnabled(b);
            splitCurrencyTextView.setEnabled(b);
            splitUidTextView.setEnabled(b);
            splitTypeButton.setEnabled(b);
        }
    }

    /**
     * Add a split view and initialize it with <code>split</code>
     * @param split Split to initialize the contents to
     * @return Returns the split view which was added
     */
    private View addSplitView(Split split){
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View splitView = layoutInflater.inflate(R.layout.item_split_entry, mSplitsLinearLayout, false);
        mSplitsLinearLayout.addView(splitView,0);
        bindSplitView(splitView, split);
        mSplitItemViewList.add(splitView);
        return splitView;
    }

    /**
     * Extracts arguments passed to the view and initializes necessary adapters and cursors
     */
    private void initArgs() {
        mAccountsDbAdapter = AccountsDbAdapter.getInstance();

        Bundle args = getArguments();
        mAccountUID = ((TransactionsActivity) getActivity()).getCurrentAccountUID();
        mBaseAmount = new BigDecimal(args.getString(UxArgument.AMOUNT_STRING));

        String conditions = "(" //+ AccountEntry._ID + " != " + mAccountId + " AND "
                + (mMultiCurrency ? "" : (DatabaseSchema.AccountEntry.COLUMN_CURRENCY + " = ? AND "))
                + DatabaseSchema.AccountEntry.COLUMN_UID + " != '" + mAccountsDbAdapter.getGnuCashRootAccountUID() + "' AND "
                + DatabaseSchema.AccountEntry.COLUMN_PLACEHOLDER + " = 0"
                + ")";
        mCursor = mAccountsDbAdapter.fetchAccountsOrderedByFullName(conditions,
                mMultiCurrency ? new String[]{"" + mAccountsDbAdapter.getGnuCashRootAccountUID()} :
                        new String[]{mAccountsDbAdapter.getCurrencyCode(mAccountUID)}
        );
    }

    /**
     * Binds the different UI elements of an inflated list view to corresponding actions
     * @param splitView Split item view
     * @param split {@link org.gnucash.android.model.Split} to use to populate the view
     */
    private void bindSplitView(final View splitView, Split split){
        EditText splitMemoEditText              = (EditText)    splitView.findViewById(R.id.input_split_memo);
        final EditText splitAmountEditText      = (EditText)    splitView.findViewById(R.id.input_split_amount);
        ImageButton removeSplitButton           = (ImageButton) splitView.findViewById(R.id.btn_remove_split);
        Spinner accountsSpinner                 = (Spinner)     splitView.findViewById(R.id.input_accounts_spinner);
        final TextView splitCurrencyTextView    = (TextView)    splitView.findViewById(R.id.split_currency_symbol);
        final TextView splitUidTextView         = (TextView)    splitView.findViewById(R.id.split_uid);
        final TransactionTypeToggleButton splitTypeButton = (TransactionTypeToggleButton) splitView.findViewById(R.id.btn_split_type);

        splitAmountEditText.addTextChangedListener(new AmountInputFormatter(splitAmountEditText));

        removeSplitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRemovedSplitUIDs.add(splitUidTextView.getText().toString());
                mSplitsLinearLayout.removeView(splitView);
                mSplitItemViewList.remove(splitView);
                updateTotal();
            }
        });

        updateTransferAccountsList(accountsSpinner);
        accountsSpinner.setOnItemSelectedListener(new TypeButtonLabelUpdater(splitTypeButton));

        Currency accountCurrency = Currency.getInstance(mAccountsDbAdapter.getCurrencyCode(
                split == null ? mAccountUID : split.getAccountUID()));
        splitCurrencyTextView.setText(accountCurrency.getSymbol());
        splitTypeButton.setAmountFormattingListener(splitAmountEditText, splitCurrencyTextView);
        splitTypeButton.setChecked(mBaseAmount.signum() > 0);
        splitUidTextView.setText(UUID.randomUUID().toString());

        if (split != null) {
            splitAmountEditText.setText(split.getAmount().toPlainString());
            splitMemoEditText.setText(split.getMemo());
            splitUidTextView.setText(split.getUID());
            String splitAccountUID = split.getAccountUID();
            setSelectedTransferAccount(mAccountsDbAdapter.getID(splitAccountUID), accountsSpinner);
            splitTypeButton.setAccountType(mAccountsDbAdapter.getAccountType(splitAccountUID));
            splitTypeButton.setChecked(split.getType());
        }

        //put these balance update triggers last last so as to avoid computing while still loading
        splitAmountEditText.addTextChangedListener(mBalanceUpdater);
        splitTypeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateTotal();
            }
        });
    }

    /**
     * Updates the spinner to the selected transfer account
     * @param accountId Database ID of the transfer account
     */
    private void setSelectedTransferAccount(long accountId, final Spinner accountsSpinner){
        for (int pos = 0; pos < mCursorAdapter.getCount(); pos++) {
            if (mCursorAdapter.getItemId(pos) == accountId){
                accountsSpinner.setSelection(pos);
                break;
            }
        }
    }
    /**
     * Updates the list of possible transfer accounts.
     * Only accounts with the same currency can be transferred to
     */
    private void updateTransferAccountsList(Spinner transferAccountSpinner){

        mCursorAdapter = new QualifiedAccountNameCursorAdapter(getActivity(),
                android.R.layout.simple_spinner_item, mCursor);
        mCursorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        transferAccountSpinner.setAdapter(mCursorAdapter);
    }

    /**
     * Attaches listeners for the buttons of the dialog
     */
    protected void setListeners(){
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMultiCurrency) {
                    Toast.makeText(getActivity(), R.string.toast_error_edit_multi_currency_transaction, Toast.LENGTH_LONG).show();
                }
                else {
                    List<Split> splitList = extractSplitsFromView();
                    ((TransactionFormFragment) getTargetFragment()).setSplitList(splitList, mRemovedSplitUIDs);
                }
                dismiss();
            }
        });

        mAddSplit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMultiCurrency) {
                    Toast.makeText(getActivity(), R.string.toast_error_edit_multi_currency_transaction, Toast.LENGTH_LONG).show();
                }
                else {
                    addSplitView(null);
                }
            }
        });
    }

    /**
     * Extracts the input from the views and builds {@link org.gnucash.android.model.Split}s to correspond to the input.
     * @return List of {@link org.gnucash.android.model.Split}s represented in the view
     */
    private List<Split> extractSplitsFromView(){
        List<Split> splitList = new ArrayList<Split>();
        for (View splitView : mSplitItemViewList) {
            EditText splitMemoEditText              = (EditText)    splitView.findViewById(R.id.input_split_memo);
            EditText splitAmountEditText            = (EditText)    splitView.findViewById(R.id.input_split_amount);
            Spinner accountsSpinner                 = (Spinner)     splitView.findViewById(R.id.input_accounts_spinner);
            TextView splitUidTextView               = (TextView)    splitView.findViewById(R.id.split_uid);
            TransactionTypeToggleButton splitTypeButton = (TransactionTypeToggleButton) splitView.findViewById(R.id.btn_split_type);

            BigDecimal amountBigDecimal = TransactionFormFragment.parseInputToDecimal(splitAmountEditText.getText().toString());
            String accountUID = mAccountsDbAdapter.getUID(accountsSpinner.getSelectedItemId());
            String currencyCode = mAccountsDbAdapter.getCurrencyCode(accountUID);
            Money amount = new Money(amountBigDecimal, Currency.getInstance(currencyCode));
            Split split = new Split(amount, accountUID);
            split.setMemo(splitMemoEditText.getText().toString());
            split.setType(splitTypeButton.getTransactionType());
            split.setUID(splitUidTextView.getText().toString().trim());
            splitList.add(split);
        }
        return splitList;
    }

    /**
     * Updates the displayed total for the transaction.
     * Computes the total of the splits, the unassigned balance and the split sum
     */
    private void updateTotal(){
        List<Split> splitList   = extractSplitsFromView();
        String currencyCode     = mAccountsDbAdapter.getCurrencyCode(mAccountUID);
        Money splitSum          = Money.createZeroInstance(currencyCode);
        if (!mMultiCurrency) {
            for (Split split : splitList) {
                Money amount = split.getAmount().absolute();
                if (split.getType() == TransactionType.DEBIT)
                    splitSum = splitSum.subtract(amount);
                else
                    splitSum = splitSum.add(amount);
            }
        }
        TransactionsActivity.displayBalance(mImbalanceTextView, splitSum);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Updates the displayed balance of the accounts when the amount of a split is changed
     */
    private class BalanceTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            updateTotal();
        }
    }

    /**
     * Updates the account type for the TransactionTypeButton when the selected account is changed in the spinner
     */
    private class TypeButtonLabelUpdater implements AdapterView.OnItemSelectedListener {
        TransactionTypeToggleButton mTypeToggleButton;

        public TypeButtonLabelUpdater(TransactionTypeToggleButton typeToggleButton){
            this.mTypeToggleButton = typeToggleButton;
        }

        @Override
        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
            AccountType accountType = mAccountsDbAdapter.getAccountType(id);
            mTypeToggleButton.setAccountType(accountType);
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }
}
