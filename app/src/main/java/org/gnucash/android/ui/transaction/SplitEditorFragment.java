/*
 * Copyright (c) 2014 - 2016 Ngewi Fet <ngewif@gmail.com>
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
package org.gnucash.android.ui.transaction;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.CommoditiesDbAdapter;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.BaseModel;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;
import org.gnucash.android.ui.common.FormActivity;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.transaction.dialog.TransferFundsDialogFragment;
import org.gnucash.android.ui.util.widget.CalculatorEditText;
import org.gnucash.android.ui.util.widget.CalculatorKeyboard;
import org.gnucash.android.ui.util.widget.TransactionTypeSwitch;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Dialog for editing the splits in a transaction
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class SplitEditorFragment extends Fragment {

    //
    // SplitEditorFragment
    //

    @BindView(R.id.split_list_layout)   LinearLayout mSplitsLinearLayout;
    @BindView(R.id.calculator_keyboard) KeyboardView mKeyboardView;
    @BindView(R.id.imbalance_textview)  TextView mImbalanceTextView;

    private AccountsDbAdapter mAccountsDbAdapter;
    private Cursor mCursor;
    private SimpleCursorAdapter mCursorAdapter;
    private List<View> mSplitItemViewList;
    private String mAccountUID;
    private Commodity mCommodity;

    private BigDecimal mBaseAmount = BigDecimal.ZERO;

    CalculatorKeyboard mCalculatorKeyboard;

    BalanceTextWatcher mImbalanceWatcher = new BalanceTextWatcher();

    /**
     * Create and return a new instance of the fragment with the appropriate paramenters
     * @param args Arguments to be set to the fragment. <br>
     *             See {@link UxArgument#AMOUNT_STRING} and {@link UxArgument#SPLIT_LIST}
     * @return New instance of SplitEditorFragment
     */
    public static SplitEditorFragment newInstance(Bundle args){
        SplitEditorFragment fragment = new SplitEditorFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_split_editor, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(R.string.title_split_editor);
        setHasOptionsMenu(true);

        mCalculatorKeyboard = new CalculatorKeyboard(getActivity(), mKeyboardView, R.xml.calculator_keyboard);
        mSplitItemViewList = new ArrayList<>();

        //we are editing splits for a new transaction.
        // But the user may have already created some splits before. Let's check

        List<Split> splitList = getArguments().getParcelableArrayList(UxArgument.SPLIT_LIST);
        assert splitList != null;

        initArgs();
        if (!splitList.isEmpty()) {
            //aha! there are some splits. Let's load those instead
            loadSplitViews(splitList);
            mImbalanceWatcher.afterTextChanged(null);
        } else {
            final String currencyCode = mAccountsDbAdapter.getAccountCurrencyCode(mAccountUID);
            Split split = new Split(new Money(mBaseAmount, Commodity.getInstance(currencyCode)), mAccountUID);
            AccountType accountType = mAccountsDbAdapter.getAccountType(mAccountUID);
            TransactionType transactionType = Transaction.getTypeForBalance(accountType, mBaseAmount.signum() < 0);
            split.setType(transactionType);
            View view = addSplitView(split);
            view.findViewById(R.id.input_accounts_spinner).setEnabled(false);
            view.findViewById(R.id.btn_remove_split).setVisibility(View.GONE);

            // Get Preference about showing signum in Splits
            boolean shallDisplayNegativeSignumInSplits = PreferenceManager.getDefaultSharedPreferences(getActivity())
                                                                          .getBoolean(getString(R.string.key_display_negative_signum_in_splits),
                                                                                      false);
            accountType.displayBalance(mImbalanceTextView,
                                       new Money(mBaseAmount.negate(),
                                                 mCommodity),
                                       shallDisplayNegativeSignumInSplits);
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mCalculatorKeyboard = new CalculatorKeyboard(getActivity(), mKeyboardView, R.xml.calculator_keyboard);
    }

    private void loadSplitViews(List<Split> splitList) {
        for (Split split : splitList) {
            addSplitView(split);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.split_editor_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().setResult(Activity.RESULT_CANCELED);
                getActivity().finish();
                return true;

            case R.id.menu_save:
                saveSplits();
                return true;

            case R.id.menu_add_split:
                addSplitView(null);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Add a split view and initialize it with <code>split</code>
     * @param split Split to initialize the contents to
     * @return Returns the split view which was added
     */
    private View addSplitView(Split split){

        LayoutInflater layoutInflater = getActivity().getLayoutInflater();

        View splitEntryView = layoutInflater.inflate(R.layout.item_split_entry,
                                                     mSplitsLinearLayout,
                                                     false);

        // Respect sort list order
        mSplitsLinearLayout.addView(splitEntryView);

        SplitViewHolder viewHolder = new SplitViewHolder(splitEntryView,
                                                         split);
        splitEntryView.setTag(viewHolder);

        mSplitItemViewList.add(splitEntryView);

        return splitEntryView;
    }

    /**
     * Extracts arguments passed to the view and initializes necessary adapters and cursors
     */
    private void initArgs() {
        mAccountsDbAdapter = AccountsDbAdapter.getInstance();

        Bundle args = getArguments();
        mAccountUID = ((FormActivity) getActivity()).getCurrentAccountUID();
        mBaseAmount = new BigDecimal(args.getString(UxArgument.AMOUNT_STRING));

        // Get account list that are not hidden nor placeholder, and sort them with Favorites first
        String where = "("
                + DatabaseSchema.AccountEntry.COLUMN_HIDDEN + " = 0 AND "
                + DatabaseSchema.AccountEntry.COLUMN_PLACEHOLDER + " = 0"
                + ")";
        mCursor = mAccountsDbAdapter.fetchAccountsOrderedByFavoriteAndFullName(where, null);

        mCommodity = CommoditiesDbAdapter.getInstance().getCommodity(mAccountsDbAdapter.getCurrencyCode(mAccountUID));
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
        mCursorAdapter = new QualifiedAccountNameCursorAdapter(getActivity(), mCursor);
        transferAccountSpinner.setAdapter(mCursorAdapter);
    }

    /**
     * Check if all the split amounts have valid values that can be saved
     * @return {@code true} if splits can be saved, {@code false} otherwise
     */
    private boolean canSave(){
        for (View splitView : mSplitItemViewList) {
            SplitViewHolder viewHolder = (SplitViewHolder) splitView.getTag();
            viewHolder.splitAmountEditText.evaluate();
            if (viewHolder.splitAmountEditText.getError() != null){
                return false;
            }
            //TODO: also check that multicurrency splits have a conversion amount present
        }
        return true;
    }

    /**
     * Save all the splits from the split editor
     */
    private void saveSplits() {
        if (!canSave()){
            Toast.makeText(getActivity(), R.string.toast_error_check_split_amounts,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent data = new Intent();
        data.putParcelableArrayListExtra(UxArgument.SPLIT_LIST, extractSplitsFromView());
        getActivity().setResult(Activity.RESULT_OK, data);

        getActivity().finish();
    }

    /**
     * Extracts the input from the views and builds {@link org.gnucash.android.model.Split}s to correspond to the input.
     * @return List of {@link org.gnucash.android.model.Split}s represented in the view
     */
    private ArrayList<Split> extractSplitsFromView() {

        ArrayList<Split> splitList = new ArrayList<>();

        for (View splitView : mSplitItemViewList) {

            SplitViewHolder viewHolder = (SplitViewHolder) splitView.getTag();

            if (viewHolder.splitAmountEditText.getValue() == null) {
                //

                continue;

            } else {
                //

                BigDecimal amountBigDecimal = viewHolder.splitAmountEditText.getValue();

                String currencyCode = mAccountsDbAdapter.getCurrencyCode(mAccountUID);

                Money  valueAmount  = new Money(amountBigDecimal.abs(),
                                                Commodity.getInstance(currencyCode));

                String accountUID = mAccountsDbAdapter.getUID(viewHolder.accountsSpinner.getSelectedItemId());

                Split  split      = new Split(valueAmount,
                                              accountUID);

                split.setMemo(viewHolder.splitMemoEditText.getText()
                                                          .toString());
                split.setType(viewHolder.splitTypeSwitch.getTransactionType());
                split.setUID(viewHolder.splitUidTextView.getText()
                                                        .toString()
                                                        .trim());
                if (viewHolder.quantity != null) {
                    split.setQuantity(viewHolder.quantity.abs());
                }

                splitList.add(split);
            }

        } // for

        return splitList;
    }

    //
    // SplitViewHolder
    //

    /**
     * Holds a split item view and binds the items in it
     */
    class SplitViewHolder
            implements OnTransferFundsListener {

        @BindView(R.id.split_currency_symbol)
        TextView              splitCurrencyTextView;
        @BindView(R.id.input_split_amount)
        CalculatorEditText    splitAmountEditText;
        @BindView(R.id.btn_split_type)
        TransactionTypeSwitch splitTypeSwitch;
        @BindView(R.id.btn_remove_split)
        ImageView             removeSplitButton;
        @BindView(R.id.input_accounts_spinner)
        Spinner               accountsSpinner;
        @BindView(R.id.input_split_memo)
        EditText              splitMemoEditText;
        @BindView(R.id.split_uid)
        TextView              splitUidTextView;

        private View  splitView;
        private Money quantity;

        public SplitViewHolder(View splitView,
                               Split split) {

            ButterKnife.bind(this,
                             splitView);

            this.splitView = splitView;

            // Set Listeners
            setListeners(split);

            if (split != null && !split.getQuantity()
                                       .equals(split.getValue())) {
                this.quantity = split.getQuantity();
            }

            // Init Views from split
            initViews(split);
        }

        private void setListeners(Split split) {

            //
            // Listeners on splitAmountEditText
            //

            splitAmountEditText.bindListeners(mCalculatorKeyboard);

            splitAmountEditText.addTextChangedListener(mImbalanceWatcher);

            //
            // Listeners on removeSplitButton
            //

            removeSplitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    mSplitsLinearLayout.removeView(splitView);
                    mSplitItemViewList.remove(splitView);
                    mImbalanceWatcher.afterTextChanged(null);
                }
            });

            //
            // Listeners on accountsSpinner
            //

            accountsSpinner.setOnItemSelectedListener(new SplitTransferAccountSelectedListener(splitTypeSwitch,
                                                                                               this));

            //
            // Listeners on splitTypeSwitch
            //

            // Set a ColorizeOnTransactionTypeChange listener
            splitTypeSwitch.setColorizeOnCheckedChangeListener();

            splitTypeSwitch.addOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                                             boolean isChecked) {

                    // Change Transaction Type according to splitTypeSwitch
                    split.setType(splitTypeSwitch.getTransactionType());

                    // Update Split Amount Signum
                    updateSplitAmountEditText(split);

                    // Recompute Split List Balance
                    mImbalanceWatcher.afterTextChanged(null);
                }
            });
        }

        private void initViews(final Split split) {

            //
            // splitTypeSwitch
            //

            splitTypeSwitch.setViewsToColorize(splitAmountEditText,
                                               splitCurrencyTextView);

            // Switch on/off according to amount signum
            splitTypeSwitch.setChecked(mBaseAmount.signum() > 0);

            //
            // Fill spinner
            //

            updateTransferAccountsList(accountsSpinner);

            //
            // Display Currency
            //

            splitCurrencyTextView.setText(mCommodity.getSymbol());

            //
            // uid
            //

            splitUidTextView.setText(BaseModel.generateUID());

            //
            // Fill views from Split data
            //

            if (split != null) {
                // There is a valid Split

                splitAmountEditText.setCommodity(split.getValue()
                                                      .getCommodity());

                // Update Split Amount EditText
                updateSplitAmountEditText(split);

                splitCurrencyTextView.setText(split.getValue()
                                                   .getCommodity()
                                                   .getSymbol());

                splitMemoEditText.setText(split.getMemo());

                splitUidTextView.setText(split.getUID());

                String splitAccountUID = split.getAccountUID();
                setSelectedTransferAccount(mAccountsDbAdapter.getID(splitAccountUID),
                                           accountsSpinner);

                splitTypeSwitch.setAccountType(mAccountsDbAdapter.getAccountType(splitAccountUID));

                splitTypeSwitch.setChecked(split.getType());
            }
        }

        private void updateSplitAmountEditText(final Split split) {

            // Get Preference about showing signum in Splits
            boolean shallDisplayNegativeSignumInSplits = PreferenceManager.getDefaultSharedPreferences(getActivity())
                                                                          .getBoolean(getString(R.string.key_display_negative_signum_in_splits),
                                                                                      false);

            final Money splitValueWithSignum = split.getValueWithSignum();

            AccountType accountType = GnuCashApplication.getAccountsDbAdapter()
                                                        .getAccountType(split.getAccountUID());

            // Display abs value because switch button is visible
            accountType.displayBalanceWithoutCurrency(splitAmountEditText,
                                                      splitValueWithSignum,
                                                      shallDisplayNegativeSignumInSplits);
        }

        /**
         * Returns the value of the amount in the splitAmountEditText field without setting the value to the view
         * <p>If the expression in the view is currently incomplete or invalid, null is returned.
         * This method is used primarily for computing the imbalance</p>
         *
         * @return Value in the split item amount field, or {@link BigDecimal#ZERO} if the expression is empty or invalid
         */
        public BigDecimal getAmountValue() {

            String amountString = splitAmountEditText.getCleanString();
            if (amountString.isEmpty()) {
                return BigDecimal.ZERO;
            }

            ExpressionBuilder expressionBuilder = new ExpressionBuilder(amountString);
            Expression        expression;

            try {
                expression = expressionBuilder.build();
            } catch (RuntimeException e) {
                return BigDecimal.ZERO;
            }

            if (expression != null && expression.validate()
                                                .isValid()) {
                return new BigDecimal(expression.evaluate());
            } else {
                Log.v(SplitEditorFragment.this.getClass()
                                              .getSimpleName(),
                      "Incomplete expression for updating imbalance: " + expression);
                return BigDecimal.ZERO;
            }
        }

        @Override
        public void transferComplete(Money amount) {

            quantity = amount;
        }

    }

    //
    // BalanceTextWatcher
    //

    /**
     * Updates the displayed balance of the list of Splits when the amount of a split is changed
     */
    private class BalanceTextWatcher
            implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence charSequence,
                                      int i,
                                      int i2,
                                      int i3) {
            //nothing to see here, move along
        }

        @Override
        public void onTextChanged(CharSequence charSequence,
                                  int i,
                                  int i2,
                                  int i3) {
            //nothing to see here, move along
        }

        @Override
        public void afterTextChanged(Editable editable) {

            //
            // Compute Split balance
            //

            BigDecimal imbalance = BigDecimal.ZERO;

            for (View splitItem : mSplitItemViewList) {

                SplitViewHolder viewHolder = (SplitViewHolder) splitItem.getTag();

                // Get the absolute value of the amount
                BigDecimal absAmount = viewHolder.getAmountValue()
                                                 .abs();

                long accountId = viewHolder.accountsSpinner.getSelectedItemId();

                // #876 May be usefull for debug
//                String accountFullName = AccountsDbAdapter.getInstance()
//                                                          .getAccountFullName(AccountsDbAdapter.getInstance()
//                                                                                               .getUID(accountId));

                // #876
//                boolean hasDebitNormalBalance = AccountsDbAdapter.getInstance()
//                                                                 .getAccountType(accountId)
//                                                                 .hasDebitNormalBalance();

                if (viewHolder.splitTypeSwitch.isChecked()) {
                    // Switch is CREDIT

                    // #876
//                    if (hasDebitNormalBalance) {
//                        imbalance = imbalance.add(absAmount);
//                    } else {
//                        imbalance = imbalance.subtract(absAmount);
//                    }
                    imbalance = imbalance.add(absAmount);

                } else {
                    // Switch is DEBIT

                    // #876
//                    if (hasDebitNormalBalance) {
//                        imbalance = imbalance.subtract(absAmount);
//                    } else {
//                        imbalance = imbalance.add(absAmount);
//                    }
                    imbalance = imbalance.subtract(absAmount);

                }

            } // for

            // Get Preference about showing signum in Splits
            boolean shallDisplayNegativeSignumInSplits = PreferenceManager.getDefaultSharedPreferences(getActivity())
                                                                          .getBoolean(getString(R.string.key_display_negative_signum_in_splits),
                                                                                      false);

            AccountType.ASSET.displayBalance(mImbalanceTextView,
                                             new Money(imbalance,
                                                       mCommodity),
                                             shallDisplayNegativeSignumInSplits);
        }
    }

    //
    // SplitTransferAccountSelectedListener
    //

    /**
     * Listens to changes in the transfer account and updates the currency symbol, the label of the
     * transaction type and if neccessary
     */
    private class SplitTransferAccountSelectedListener
            implements AdapterView.OnItemSelectedListener {

        private TransactionTypeSwitch mTransactionTypeSwitch;
        private SplitViewHolder       mSplitViewHolder;

        /**
         * Flag to know when account spinner callback is due to user interaction or layout of components
         */
        boolean userInteraction = false;

        public SplitTransferAccountSelectedListener(TransactionTypeSwitch transactionTypeSwitch,
                                                    SplitViewHolder viewHolder) {

            this.mSplitViewHolder = viewHolder;

            this.mTransactionTypeSwitch = transactionTypeSwitch;
            this.mTransactionTypeSwitch.setViewsToColorize(mSplitViewHolder.splitAmountEditText,
                                                           mSplitViewHolder.splitCurrencyTextView);
        }

        /**
         * Called when user has chosen a new Account for the split
         * using the spinner
         *
         * @param parentView
         * @param selectedItemView
         * @param position
         * @param id
         */
        @Override
        public void onItemSelected(AdapterView<?> parentView,
                                   View selectedItemView,
                                   int position,
                                   long id) {

            AccountType accountType = mAccountsDbAdapter.getAccountType(id);

            mTransactionTypeSwitch.setAccountType(accountType);

            //refresh the imbalance amount if we change the account
            mImbalanceWatcher.afterTextChanged(null);

            String fromCurrencyCode   = mAccountsDbAdapter.getCurrencyCode(mAccountUID);
            String targetCurrencyCode = mAccountsDbAdapter.getCurrencyCode(mAccountsDbAdapter.getUID(id));

            if (!userInteraction || fromCurrencyCode.equals(targetCurrencyCode)) {
                //first call is on layout, subsequent calls will be true and transfer will work as usual
                userInteraction = true;
                return;
            }

            BigDecimal amountBigD = mSplitViewHolder.splitAmountEditText.getValue();
            if (amountBigD == null) {
                return;
            }

            Money amount = new Money(amountBigD,
                                     Commodity.getInstance(fromCurrencyCode));

            TransferFundsDialogFragment fragment = TransferFundsDialogFragment.getInstance(amount,
                                                                                           targetCurrencyCode,
                                                                                           mSplitViewHolder);
            fragment.show(getFragmentManager(),
                          "tranfer_funds_editor");
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            //nothing to see here, move along
        }
    }

}
