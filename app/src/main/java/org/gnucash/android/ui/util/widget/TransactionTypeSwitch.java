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
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.TransactionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A special type of {@link android.widget.ToggleButton} which displays the appropriate DEBIT/CREDIT labels for the
 * different account types.
 * @author Ngewi Fet <ngewif@gmail.com>
 */
// TODO TW C 2020-03-03 : A renommer SplitTypeSwitch (AC)
public class TransactionTypeSwitch extends SwitchCompat {

    private AccountType mAccountType = AccountType.EXPENSE;

    // View to update and colorize
    private CalculatorEditText mAmountEditText;
    private TextView mCurrencyTextView;

    // Listeners to call in case of change in Transaction Type switch
    List<OnCheckedChangeListener> mOnCheckedChangeListeners = new ArrayList<>();

    public TransactionTypeSwitch(Context context,
                                 AttributeSet attrs,
                                 int defStyle) {

        super(context,
              attrs,
              defStyle);
    }

    public TransactionTypeSwitch(Context context,
                                 AttributeSet attrs) {

        super(context,
              attrs);
    }

    public TransactionTypeSwitch(Context context) {

        super(context);
    }

    /**
     * Store the accountType
     * and define switch on/off texts accordingly
     *
     * @param accountType
     */
    public void setAccountType(AccountType accountType) {

        this.mAccountType = accountType;

        Context context = getContext().getApplicationContext();

        switch (mAccountType) {
            case CASH:
                setTextOff(context.getString(R.string.label_receive)); // DEBIT
                setTextOn(context.getString(R.string.label_spend)); // CREDIT
                break;
            case BANK:
                setTextOff(context.getString(R.string.label_deposit)); // DEBIT
                setTextOn(context.getString(R.string.label_withdrawal)); // CREDIT
                break;
            case CREDIT:
                // #876 Change according to GnuCash on Windows
                setTextOff(context.getString(R.string.label_payment)); // DEBIT
                setTextOn(context.getString(R.string.label_charge)); // CREDIT
                break;
            case ASSET:
            case EQUITY:
            case LIABILITY:
                // #876 Change according to GnuCash on Windows
                setTextOff(context.getString(R.string.label_decrease)); // DEBIT
                setTextOn(context.getString(R.string.label_increase)); // CREDIT
                break;
            case INCOME:
                // #876 Change according to GnuCash on Windows
                setTextOff(context.getString(R.string.label_charge)); // DEBIT
                setTextOn(context.getString(R.string.label_income)); // CREDIT
                break;
            case EXPENSE:
                setTextOff(context.getString(R.string.label_expense)); // DEBIT
                setTextOn(context.getString(R.string.label_rebate)); // CREDIT
                break;
            case PAYABLE:
                // #876 Change according to GnuCash on Windows
                setTextOff(context.getString(R.string.label_payment)); // DEBIT
                setTextOn(context.getString(R.string.label_bill)); // CREDIT
                break;
            case RECEIVABLE:
                setTextOff(context.getString(R.string.label_invoice)); // DEBIT
                setTextOn(context.getString(R.string.label_payment)); // CREDIT
                break;
            case STOCK:
            case MUTUAL:
                // #876 Change according to GnuCash on Windows
                setTextOff(context.getString(R.string.label_buy)); // DEBIT
                setTextOn(context.getString(R.string.label_sell)); // CREDIT
                break;
            case CURRENCY:
            case ROOT:
            default:
                // #876 Change according to GnuCash on Windows
                setTextOff(context.getString(R.string.label_debit)); // DEBIT
                setTextOn(context.getString(R.string.label_credit)); // CREDIT
                break;
        }

        //
        // Set switch text and color
        //

        setSwitchTextAndColor(isChecked());

        invalidate();
    }

    /**
     * Bind a ColorizeOnTransactionTypeChangeListener as a switch checked change listener
     * to update the signum of amount view, colorize amount,
     * currency and switch views accordingly
     *
     * @param amoutView Amount string {@link android.widget.EditText}
     * @param currencyTextView Currency symbol text view
     */
    public void setColorizeOnCheckedChangeListener(CalculatorEditText amoutView,
                                                   TextView currencyTextView) {

        setOnCheckedChangeListener(new ColorizeOnTransactionTypeChangeListener(amoutView,
                                                                               currencyTextView));
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
        // #876
//        setChecked(Transaction.shouldDecreaseBalance(mAccountType, transactionType));
        setChecked(TransactionType.CREDIT.equals(transactionType));
    }

    /**
     * Returns the account type associated with this button
     * @return Type of account
     */
    public AccountType getAccountType() {

        return mAccountType;
    }

    public TransactionType getTransactionType() {

        // #876
//        if (mAccountType.hasDebitNormalBalance()) {
//
//            return isChecked()
//                   ? TransactionType.CREDIT
//                   : TransactionType.DEBIT;
//
//        } else {
//
//            return isChecked()
//                   ? TransactionType.DEBIT
//                   : TransactionType.CREDIT;
//        }
        return isChecked()
               ? TransactionType.CREDIT
               : TransactionType.DEBIT;
    }

    private void setSwitchTextAndColor(final boolean isCredit) {

        //
        // Set switch text
        //

        setText(isCredit
                ? getTextOn() // CREDIT
                : getTextOff() // DEBIT
               );

        //
        // Set text color
        //

        // TODO TW C 2020-03-02 : A renommer et commenter
        if ((mAccountType.isResultAccount() && !isCredit) || (!mAccountType.isResultAccount() && isCredit)) {
            // CREDIT

            // RED
            int red = ContextCompat.getColor(getContext(),
                                             R.color.debit_red);
            setAllTextsColor(red);

        } else {
            // DEBIT

            // GREEN
            int green = ContextCompat.getColor(getContext(),
                                               R.color.credit_green);
            setAllTextsColor(green);
        }
    }

    private void setAllTextsColor(final int color) {

        TransactionTypeSwitch.this.setTextColor(color);
        mAmountEditText.setTextColor(color);
        mCurrencyTextView.setTextColor(color);
    }

    //
    // Inner Class OnTypeChangedListener
    //

    /**
     * Listener on change on Transaction Type (DEBIT turned to CREDIT or vice-versa)
     * which update displayed amount signum and colorize, accordingly
     */
    private class ColorizeOnTransactionTypeChangeListener
            implements OnCheckedChangeListener{

        /**
         * Constructor with the amount view
         * @param amountEditText EditText displaying the amount value
         * @param currencyTextView Currency symbol text view
         */
        public ColorizeOnTransactionTypeChangeListener(CalculatorEditText amountEditText, TextView currencyTextView){

            mAmountEditText = amountEditText;
            mCurrencyTextView = currencyTextView;
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton,
                                     boolean isChecked) {

            final boolean isCredit = isChecked;

            //
            // Set switch text and color
            //

            setSwitchTextAndColor(isCredit);

            //
            // Change signum if needed
            //

            BigDecimal amount = mAmountEditText.getValue();

            if (amount != null) {
                if ((isCredit && amount.signum() > 0) //we switched to debit but the amount is +ve
                    || (!isCredit && amount.signum() < 0)) { //credit but amount is -ve

                    mAmountEditText.setValue(amount.negate());
                }

            }

            //
            // Call other listeners
            //

            for (OnCheckedChangeListener listener : mOnCheckedChangeListeners) {
                listener.onCheckedChanged(compoundButton,
                                          isChecked);
            } // for
        }

    }
}
