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

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.inputmethodservice.KeyboardView;
import android.support.annotation.XmlRes;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.crashlytics.android.Crashlytics;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.ui.common.FormActivity;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * A custom EditText which supports computations and uses a custom calculator keyboard.
 * <p>Afer the view is inflated, make sure to call {@link #bindListeners(KeyboardView)}
 * with the view from your layout where the calculator keyboard should be displayed:</p>
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class CalculatorEditText extends EditText {
    CalculatorKeyboard mCalculatorKeyboard;
    private Currency mCurrency = Currency.getInstance(GnuCashApplication.getDefaultCurrencyCode());
    private Context mContext;

    /**
     * Flag which is set if the contents of this view have been modified
     */
    private boolean isContentModified = false;

    private int mCalculatorKeysLayout;
    private KeyboardView mCalculatorKeyboardView;

    public CalculatorEditText(Context context) {
        super(context);
        this.mContext = context;
    }

    public CalculatorEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CalculatorEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * Overloaded constructor
     * Reads any attributes which are specified in XML and applies them
     * @param context Activity context
     * @param attrs View attributes
     */
    private void init(Context context, AttributeSet attrs){
        this.mContext = context;
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.CalculatorEditText,
                0, 0);

        try {
            mCalculatorKeysLayout = a.getResourceId(R.styleable.CalculatorEditText_keyboardKeysLayout, R.xml.calculator_keyboard);
        } finally {
            a.recycle();
        }

        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                isContentModified = true;
            }
        });
    }

    public void bindListeners(CalculatorKeyboard calculatorKeyboard){
        mCalculatorKeyboard = calculatorKeyboard;
        mContext = calculatorKeyboard.getContext();
        setOnFocusChangeListener(new OnFocusChangeListener() {
            // NOTE By setting the on focus listener, we can show the custom keyboard when the edit box gets focus, but also hide it when the edit box loses focus
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    setSelection(getText().length());
                    mCalculatorKeyboard.showCustomKeyboard(v);
                }
                else {
                    mCalculatorKeyboard.hideCustomKeyboard();
                    evaluate();
                }
            }
        });

        setOnClickListener(new OnClickListener() {
            // NOTE By setting the on click listener we can show the custom keyboard again,
            // by tapping on an edit box that already had focus (but that had the keyboard hidden).
            @Override
            public void onClick(View v) {
                mCalculatorKeyboard.showCustomKeyboard(v);
            }
        });

        // Disable spell check (hex strings look like words to Android)
        setInputType(getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        // FIXME: for some reason, this prevents the text selection from working
        setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (v != null)
                    ((InputMethodManager) GnuCashApplication.getAppContext()
                            .getSystemService(Activity.INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(v.getWindowToken(), 0);

                return false;
            }
        });

        setDefaultTouchListener();

        ((FormActivity)mContext).setOnBackListener(mCalculatorKeyboard);
    }

    /**
     * Sets the default touch listener which opens the calculator keyboard
     */
    public void setDefaultTouchListener() {
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!mCalculatorKeyboard.isCustomKeyboardVisible())
                    mCalculatorKeyboard.showCustomKeyboard(v);


                // XXX: Use dispatchTouchEvent()?
                onTouchEvent(event);               // Call native handler
                return false;
            }
        });
    }

    /**
     * Initializes listeners on the edittext
     */
    public void bindListeners(KeyboardView keyboardView){
        bindListeners(new CalculatorKeyboard(mContext, keyboardView, mCalculatorKeysLayout));
    }

    /**
     * Returns the calculator keyboard instantiated by this edittext
     * @return CalculatorKeyboard
     */
    public CalculatorKeyboard getCalculatorKeyboard(){
        return mCalculatorKeyboard;
    }

    /**
     * Returns the view Id of the keyboard view
     * @return Keyboard view
     */
    public KeyboardView getCalculatorKeyboardView() {
        return mCalculatorKeyboardView;
    }

    /**
     * Set the keyboard view used for displaying the keyboard
     * @param calculatorKeyboardView Calculator keyboard view
     */
    public void setCalculatorKeyboardView(KeyboardView calculatorKeyboardView) {
        this.mCalculatorKeyboardView = calculatorKeyboardView;
        bindListeners(calculatorKeyboardView);
    }

    /**
     * Returns the XML resource ID describing the calculator keys layout
     * @return XML resource ID
     */
    public int getCalculatorKeysLayout() {
        return mCalculatorKeysLayout;
    }

    /**
     * Sets the XML resource describing the layout of the calculator keys
     * @param mCalculatorKeysLayout XML resource ID
     */
    public void setCalculatorKeysLayout(@XmlRes int mCalculatorKeysLayout) {
        this.mCalculatorKeysLayout = mCalculatorKeysLayout;
        bindListeners(mCalculatorKeyboardView);
    }

    /**
     * Sets the calculator keyboard to use for this EditText
     * @param keyboard Properly intialized calculator keyobard
     */
    public void setCalculatorKeyboard(CalculatorKeyboard keyboard){
        this.mCalculatorKeyboard = keyboard;
    }

    /**
     * Returns the currency used for computations
     * @return ISO 4217 currency
     */
    public Currency getCurrency() {
        return mCurrency;
    }

    /**
     * Sets the currency to use for calculations
     * The currency determines the number of decimal places used
     * @param currency ISO 4217 currency
     */
    public void setCurrency(Currency currency) {
        this.mCurrency = currency;
    }

    /**
     * Evaluates the arithmetic expression in the editText and sets the text property
     * @return Result of arithmetic evaluation which is same as text displayed in edittext
     */
    public String evaluate(){
        String amountText = getText().toString();
        amountText = amountText.replaceAll(",", ".");
        if (amountText.trim().isEmpty())
            return amountText.trim();

        ExpressionBuilder expressionBuilder = new ExpressionBuilder(amountText);
        Expression expression;

        try {
            expression = expressionBuilder.build();
        } catch (RuntimeException e) {
            setError(getContext().getString(R.string.label_error_invalid_expression));
            String msg = "Invalid expression: " + amountText;
            Log.e(this.getClass().getSimpleName(), msg);
            Crashlytics.log(msg);
            return "";
        }

        if (expression != null && expression.validate().isValid()) {
            BigDecimal result = new BigDecimal(expression.evaluate());
            result = result.setScale(mCurrency.getDefaultFractionDigits(), BigDecimal.ROUND_HALF_EVEN);

            DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.getDefault());
            formatter.setMinimumFractionDigits(0);
            formatter.setMaximumFractionDigits(mCurrency.getDefaultFractionDigits());
            formatter.setGroupingUsed(false);
            String resultString = formatter.format(result.doubleValue());

            setText(resultString);
            setSelection(resultString.length());
        } else {
            setError(getContext().getString(R.string.label_error_invalid_expression));
            Log.w(VIEW_LOG_TAG, "Expression is null or invalid: " + expression);
        }
        return getText().toString();
    }

    /**
     * Evaluates the expression in the text and returns true if the result is valid
     * @return @{code true} if the input is valid, {@code false} otherwise
     */
    public boolean isInputValid(){
        evaluate();
        return getText().length() > 0 && getError() == null;
    }

    /**
     * Returns true if the content of this view has been modified
     * @return {@code true} if content has changed, {@code false} otherwise
     */
    public boolean isInputModified(){
        return this.isContentModified;
    }

    /**
     * Returns the value of the amount in the edit text or null if the field is empty.
     * Performs an evaluation of the expression first
     * @return BigDecimal value
     */
    public BigDecimal getValue(){
        evaluate();
        String amountText = getText().toString();
        if (amountText.isEmpty())
            return null;
        return new BigDecimal(amountText.replaceAll(",", ".").trim());
    }
}
