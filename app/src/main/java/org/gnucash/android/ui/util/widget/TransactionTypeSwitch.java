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
import android.support.annotation.ColorInt;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.TextView;

import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.TransactionType;
import org.gnucash.android.ui.util.AccountTypeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A special type of {@link android.widget.ToggleButton} which displays the appropriate DEBIT/CREDIT labels for the
 * linked account type and update the color of the amount and currency fields as well
 *
 * checked means CREDIT
 * unchecked means DEBIT
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
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
     * Store views to colorize (green/red) accordingly to TransactionType and AccountType
     * in addition to the current switch button
     *
     * @param amountEditText
     *         EditText displaying the amount value
     *
     * @param currencyTextView
     *         Currency symbol text view
     */
    public void setViewsToColorize(CalculatorEditText amountEditText,
                                   TextView currencyTextView) {

        mAmountEditText = amountEditText;
        mCurrencyTextView = currencyTextView;
    }

    /**
     * Store the accountType
     * and define switch on/off texts accordingly
     *
     * @param accountType
     */
    public void setAccountType(AccountType accountType) {

        mAccountType = accountType;

        //
        // Set switch button text
        //

        setTextOff(AccountTypeUtils.getLabelDebit(mAccountType)); // DEBIT
        setTextOn(AccountTypeUtils.getLabelCredit(mAccountType)); // CREDIT

        //
        // Set switch text and color
        //

        setWidgetTextColor(isChecked());

        invalidate();
    }

    /**
     * Bind a ColorizeOnTransactionTypeChangeListener as a switch checked change listener
     * to update the signum of amount view, colorize amount,
     * currency and switch views accordingly
     */
    public void setColorizeOnCheckedChangeListener() {

        setOnCheckedChangeListener(new ColorizeOnTransactionTypeChangeListener());
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

    /**
     * Set the text color of the 3 views of the widget
     * according to is a Credit amount
     * and the Account type
     *
     * @param isCredit
     *      true if amount is negative
     */
    private void setWidgetTextColor(final boolean isCredit) {

        //
        // Set switch text
        //

        setText(isCredit
                ? getTextOn() // CREDIT
                : getTextOff() // DEBIT
               );

        //
        // Change signum if needed
        //

//        BigDecimal amount = mAmountEditText.getValue();
//
//        if (amount != null) {
//            if ((isCredit && amount.signum() > 0) //we switched to debit but the amount is +ve
//                || (!isCredit && amount.signum() < 0)) { //credit but amount is -ve
//
//                mAmountEditText.setValue(amount.negate());
//            }
//
//        }

        //
        // Set text color of views
        //

        @ColorInt final int color = getAccountType().getAmountColor(isCredit);

        // Set switch color
        TransactionTypeSwitch.this.setTextColor(color);

        // Set Currency color
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
         */
        public ColorizeOnTransactionTypeChangeListener(){

        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton,
                                     boolean isChecked) {

            final boolean isCredit = isChecked;

            //
            // Set switch text and color
            //

            setWidgetTextColor(isCredit);

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
