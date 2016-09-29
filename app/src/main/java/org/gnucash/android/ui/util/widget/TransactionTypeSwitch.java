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

package org.gnucash.android.ui.util.widget;

import android.content.Context;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A special type of {@link android.widget.ToggleButton} which displays the appropriate CREDIT/DEBIT labels for the
 * different account types.
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class TransactionTypeSwitch extends SwitchCompat {
    private AccountType mAccountType = AccountType.EXPENSE;

    List<OnCheckedChangeListener> mOnCheckedChangeListeners = new ArrayList<>();

    public TransactionTypeSwitch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public TransactionTypeSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TransactionTypeSwitch(Context context) {
        super(context);
    }

    public void setAccountType(AccountType accountType){
        this.mAccountType = accountType;
        Context context = getContext().getApplicationContext();
        switch (mAccountType) {
            case CASH:
                setTextOn(context.getString(R.string.label_spend));
                setTextOff(context.getString(R.string.label_receive));
                break;
            case BANK:
                setTextOn(context.getString(R.string.label_withdrawal));
                setTextOff(context.getString(R.string.label_deposit));
                break;
            case CREDIT:
                setTextOn(context.getString(R.string.label_payment));
                setTextOff(context.getString(R.string.label_charge));
                break;
            case ASSET:
            case EQUITY:
            case LIABILITY:
                setTextOn(context.getString(R.string.label_decrease));
                setTextOff(context.getString(R.string.label_increase));
                break;
            case INCOME:
                setTextOn(context.getString(R.string.label_charge));
                setTextOff(context.getString(R.string.label_income));
                break;
            case EXPENSE:
                setTextOn(context.getString(R.string.label_rebate));
                setTextOff(context.getString(R.string.label_expense));
                break;
            case PAYABLE:
                setTextOn(context.getString(R.string.label_payment));
                setTextOff(context.getString(R.string.label_bill));
                break;
            case RECEIVABLE:
                setTextOn(context.getString(R.string.label_payment));
                setTextOff(context.getString(R.string.label_invoice));
                break;
            case STOCK:
            case MUTUAL:
                setTextOn(context.getString(R.string.label_buy));
                setTextOff(context.getString(R.string.label_sell));
                break;
            case CURRENCY:
            case ROOT:
            default:
                setTextOn(context.getString(R.string.label_debit));
                setTextOff(context.getString(R.string.label_credit));
                break;
        }
        setText(isChecked() ? getTextOn() : getTextOff());
        invalidate();
    }

    /**
     * Set a checked change listener to monitor the amount view and currency views and update the display (color & balance accordingly)
     * @param amoutView Amount string {@link android.widget.EditText}
     * @param currencyTextView Currency symbol text view
     */
    public void setAmountFormattingListener(CalculatorEditText amoutView, TextView currencyTextView){
        setOnCheckedChangeListener(new OnTypeChangedListener(amoutView, currencyTextView));
    }

    /**
     * Add listeners to be notified when the checked status changes
     * @param checkedChangeListener Checked change listener
     */
    public void addOnCheckedChangeListener(OnCheckedChangeListener checkedChangeListener){
        mOnCheckedChangeListeners.add(checkedChangeListener);
    }

    /**
     * Toggles the button checked based on the movement caused by the transaction type for the specified account
     * @param transactionType {@link org.gnucash.android.model.TransactionType} of the split
     */
    public void setChecked(TransactionType transactionType){
        setChecked(Transaction.shouldDecreaseBalance(mAccountType, transactionType));
    }

    /**
     * Returns the account type associated with this button
     * @return Type of account
     */
    public AccountType getAccountType(){
        return mAccountType;
    }

    public TransactionType getTransactionType(){
        if (mAccountType.hasDebitNormalBalance()){
            return isChecked() ? TransactionType.CREDIT : TransactionType.DEBIT;
        } else {
            return isChecked() ? TransactionType.DEBIT : TransactionType.CREDIT;
        }
    }

    private class OnTypeChangedListener implements OnCheckedChangeListener{
        private CalculatorEditText mAmountEditText;
        private TextView mCurrencyTextView;
        /**
         * Constructor with the amount view
         * @param amountEditText EditText displaying the amount value
         * @param currencyTextView Currency symbol text view
         */
        public OnTypeChangedListener(CalculatorEditText amountEditText, TextView currencyTextView){
            this.mAmountEditText = amountEditText;
            this.mCurrencyTextView = currencyTextView;
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            setText(isChecked ? getTextOn() : getTextOff());
            if (isChecked){
                int red = getResources().getColor(R.color.debit_red);
                TransactionTypeSwitch.this.setTextColor(red);
                mAmountEditText.setTextColor(red);
                mCurrencyTextView.setTextColor(red);
            }
            else {
                int green = getResources().getColor(R.color.credit_green);
                TransactionTypeSwitch.this.setTextColor(green);
                mAmountEditText.setTextColor(green);
                mCurrencyTextView.setTextColor(green);
            }
            BigDecimal amount = mAmountEditText.getValue();
            if (amount != null && ((isChecked && amount.signum() > 0) //we switched to debit but the amount is +ve
                        || (!isChecked && amount.signum() < 0))){ //credit but amount is -ve
                mAmountEditText.setValue(amount.negate());
            }
            for (OnCheckedChangeListener listener : mOnCheckedChangeListeners) {
                listener.onCheckedChanged(compoundButton, isChecked);
            }
        }
    }
}
