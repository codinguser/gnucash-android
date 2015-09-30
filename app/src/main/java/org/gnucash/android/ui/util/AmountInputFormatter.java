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

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import org.gnucash.android.ui.transaction.TransactionFormFragment;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Captures input string in the amount input field and parses it into a formatted amount
 * The amount input field allows numbers to be input sequentially and they are parsed
 * into a string with 2 decimal places. This means inputting 245 will result in the amount
 * of 2.45
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 * @deprecated Use {@link org.gnucash.android.ui.util.widget.CalculatorEditText} for getting input amounts from the user
 */
@Deprecated
public class AmountInputFormatter implements TextWatcher {
    private String current = "0";
    private EditText amountEditText;
    /**
     * Flag to note if the user has manually edited the amount of the transaction
     */
    private boolean isModified = false;

    public AmountInputFormatter(EditText amountInput) {
        this.amountEditText = amountInput;
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (s.length() == 0 || s.toString().equals(current))
            return;
//make sure that the sign of the input is in line with the type button state
        BigDecimal amount = TransactionFormFragment.parseInputToDecimal(s.toString());

        DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.getDefault());
        formatter.setMinimumFractionDigits(2);  //TODO: Use fraction of the currency in question
        formatter.setMaximumFractionDigits(2);

        current = formatter.format(amount.doubleValue()); //TODO: Try with Bigdecimal string instead of double
        amountEditText.removeTextChangedListener(this);
        amountEditText.setText(current);
        amountEditText.setSelection(current.length());
        amountEditText.addTextChangedListener(this);

        isModified = true;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
                                  int after) {
        // nothing to see here, move along
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before,
                              int count) {
        // nothing to see here, move along
        isModified = true;
    }

    /**
     * Returns true if input has been entered into the view
     *
     * @return <code>true</code> if the view has been modified, <code>false</code> otherwise.
     */
    public boolean isInputModified() {
        return isModified;
    }
}
