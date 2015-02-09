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

package org.gnucash.android.ui.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import org.gnucash.android.R;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.model.TransactionType;
import org.gnucash.android.ui.transaction.TransactionFormFragment;

/**
 * A special type of {@link android.widget.ToggleButton} which displays the appropriate CREDIT/DEBIT labels for the
 * different account types.
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class TransactionTypeToggleButton extends ToggleButton {
    private AccountType mAccountType = AccountType.EXPENSE;

    public TransactionTypeToggleButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public TransactionTypeToggleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TransactionTypeToggleButton(Context context) {
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
    public void setAmountFormattingListener(EditText amoutView, TextView currencyTextView){
        setOnCheckedChangeListener(new OnTypeChangedListener(amoutView, currencyTextView));
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
     * @return
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
        private EditText mAmountEditText;
        private TextView mCurrencyTextView;
        /**
         * Constructor with the amount view
         * @param amountEditText EditText displaying the amount value
         * @param currencyTextView Currency symbol text view
         */
        public OnTypeChangedListener(EditText amountEditText, TextView currencyTextView){
            this.mAmountEditText = amountEditText;
            this.mCurrencyTextView = currencyTextView;
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            if (isChecked){
                int red = getResources().getColor(R.color.debit_red);
                TransactionTypeToggleButton.this.setTextColor(red);
                mAmountEditText.setTextColor(red);
                mCurrencyTextView.setTextColor(red);
            }
            else {
                int green = getResources().getColor(R.color.credit_green);
                TransactionTypeToggleButton.this.setTextColor(green);
                mAmountEditText.setTextColor(green);
                mCurrencyTextView.setTextColor(green);
            }
            String amountText = mAmountEditText.getText().toString();
            if (amountText.length() > 0){
                String changedSignText = TransactionFormFragment.parseInputToDecimal(amountText).negate().toPlainString();
                mAmountEditText.setText(changedSignText); //trigger an edit to update the number sign
            }
        }
    }
}
