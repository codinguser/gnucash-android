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

package org.gnucash.android.ui.transaction.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.db.adapter.CommoditiesDbAdapter;
import org.gnucash.android.db.adapter.PricesDbAdapter;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Price;
import org.gnucash.android.ui.transaction.TransactionFormFragment;
import org.gnucash.android.ui.transaction.TransactionsActivity;
import org.gnucash.android.ui.util.AmountInputFormatter;
import org.gnucash.android.ui.transaction.OnTransferFundsListener;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Currency;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Dialog fragment for handling currency conversions when inputting transactions.
 * <p>This is used whenever a multi-currency transaction is being created.</p>
 */
public class TransferFundsDialogFragment extends DialogFragment {

    @Bind(R.id.from_currency)           TextView mFromCurrencyLabel;
    @Bind(R.id.to_currency)             TextView mToCurrencyLabel;
    @Bind(R.id.target_currency)         TextView mConvertedAmountCurrencyLabel;
    @Bind(R.id.amount_to_convert)       TextView mStartAmountLabel;
    @Bind(R.id.input_exchange_rate)     EditText mExchangeRateInput;
    @Bind(R.id.input_converted_amount)  EditText mConvertedAmountInput;
    @Bind(R.id.btn_fetch_exchange_rate) Button mFetchExchangeRateButton;
    @Bind(R.id.radio_exchange_rate)     RadioButton mExchangeRateRadioButton;
    @Bind(R.id.radio_converted_amount)  RadioButton mConvertedAmountRadioButton;
    @Bind(R.id.label_exchange_rate_example)
    TextView mSampleExchangeRate;
    @Bind(R.id.exchange_rate_text_input_layout)
    TextInputLayout mExchangeRateInputLayout;
    @Bind(R.id.converted_amount_text_input_layout)
    TextInputLayout mConvertedAmountInputLayout;

    @Bind(R.id.btn_save) Button mSaveButton;
    @Bind(R.id.btn_cancel) Button mCancelButton;
    Money mOriginAmount;
    Currency mTargetCurrency;

    Money mConvertedAmount;
    OnTransferFundsListener mOnTransferFundsListener;

    public static TransferFundsDialogFragment getInstance(Money transactionAmount, String targetCurrencyCode,
                                                          OnTransferFundsListener transferFundsListener){
        TransferFundsDialogFragment fragment = new TransferFundsDialogFragment();
        fragment.mOriginAmount = transactionAmount;
        fragment.mTargetCurrency = Currency.getInstance(targetCurrencyCode);
        fragment.mOnTransferFundsListener = transferFundsListener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_transfer_funds, container, false);
        ButterKnife.bind(this, view);

        TransactionsActivity.displayBalance(mStartAmountLabel, mOriginAmount);
        Currency fromCurrency = mOriginAmount.getCurrency();
        mFromCurrencyLabel.setText(fromCurrency.getCurrencyCode());
        mToCurrencyLabel.setText(mTargetCurrency.getCurrencyCode());
        mConvertedAmountCurrencyLabel.setText(mTargetCurrency.getCurrencyCode());

        mSampleExchangeRate.setText("e.g. 1 " + fromCurrency.getCurrencyCode() + " = " + " x.xx " + mTargetCurrency.getCurrencyCode());
        final InputWatcher textChangeListener = new InputWatcher();

        CommoditiesDbAdapter commoditiesDbAdapter = CommoditiesDbAdapter.getInstance();
        String commodityUID = commoditiesDbAdapter.getCommodityUID(fromCurrency.getCurrencyCode());
        Commodity currencyCommodity = commoditiesDbAdapter.getCommodity(mTargetCurrency.getCurrencyCode());
        String currencyUID = currencyCommodity.getUID();
        PricesDbAdapter pricesDbAdapter = PricesDbAdapter.getInstance();
        Pair<Long, Long> price = pricesDbAdapter.getPrice(commodityUID, currencyUID);

        if (price.first > 0 && price.second > 0) {
            // a valid price exists
            BigDecimal num = new BigDecimal(price.first);
            BigDecimal denom = new BigDecimal(price.second);
            mExchangeRateInput.setText(num.divide(denom, MathContext.DECIMAL32).toString());
            mConvertedAmountInput.setText(mOriginAmount.asBigDecimal().multiply(num).divide(denom, currencyCommodity.getSmallestFractionDigits(), BigDecimal.ROUND_HALF_EVEN).toString());
        }

        mExchangeRateInput.addTextChangedListener(textChangeListener);
        mExchangeRateInput.addTextChangedListener(new AmountInputFormatter(mExchangeRateInput));
        mConvertedAmountInput.addTextChangedListener(textChangeListener);
        mConvertedAmountInput.addTextChangedListener(new AmountInputFormatter(mConvertedAmountInput));

        mConvertedAmountRadioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mConvertedAmountInput.setEnabled(isChecked);
                mConvertedAmountInputLayout.setErrorEnabled(isChecked);
                mExchangeRateRadioButton.setChecked(!isChecked);
                if (isChecked) {
                    mConvertedAmountInput.requestFocus();
                }
            }
        });

        mExchangeRateRadioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mExchangeRateInput.setEnabled(isChecked);
                mExchangeRateInputLayout.setErrorEnabled(isChecked);
                mFetchExchangeRateButton.setEnabled(isChecked);
                mConvertedAmountRadioButton.setChecked(!isChecked);
                if (isChecked) {
                    mExchangeRateInput.requestFocus();
                }
            }
        });

        mFetchExchangeRateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: Pull the exchange rate for the currency here
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                transferFunds();
            }
        });
        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.title_transfer_funds);
        return dialog;
    }

    /**
     * Converts the currency amount with the given exchange rate and saves the price to the db
     */
    private void transferFunds(){
        if (mExchangeRateRadioButton.isChecked()){
            String exchangeRateString = mExchangeRateInput.getText().toString();
            if (exchangeRateString.isEmpty()){
                mExchangeRateInputLayout.setError(getString(R.string.error_exchange_rate_required));
                return;
            }

            BigDecimal rate = TransactionFormFragment.parseInputToDecimal(exchangeRateString);
            mConvertedAmount = mOriginAmount.multiply(rate);
        }

        if (mConvertedAmountRadioButton.isChecked()){
            String convertedAmount = mConvertedAmountInput.getText().toString();
            if (convertedAmount.isEmpty()){
                mConvertedAmountInputLayout.setError(getString(R.string.error_converted_amount_required));
                return;
            }

            BigDecimal amount = TransactionFormFragment.parseInputToDecimal(convertedAmount);
            mConvertedAmount = new Money(amount, Commodity.getInstance(mTargetCurrency.getCurrencyCode()));
        }

        if (mOnTransferFundsListener != null) {
            PricesDbAdapter pricesDbAdapter = PricesDbAdapter.getInstance();
            CommoditiesDbAdapter commoditiesDbAdapter = CommoditiesDbAdapter.getInstance();
            Price price = new Price(commoditiesDbAdapter.getCommodityUID(mOriginAmount.getCurrency().getCurrencyCode()),
                    commoditiesDbAdapter.getCommodityUID(mTargetCurrency.getCurrencyCode()));
            price.setSource(Price.SOURCE_USER);
            // fractions cannot be exacted represented by BigDecimal.
            price.setValueNum(mConvertedAmount.getNumerator() * mOriginAmount.getDenominator());
            price.setValueDenom(mOriginAmount.getNumerator() * mConvertedAmount.getDenominator());
            price.reduce();
            pricesDbAdapter.addRecord(price);

            mOnTransferFundsListener.transferComplete(mConvertedAmount);
        }
        dismiss();
    }

    private class InputWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            mConvertedAmountInputLayout.setErrorEnabled(false);
            mExchangeRateInputLayout.setErrorEnabled(false);
        }
    }
}
