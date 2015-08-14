/*
 * Copyright (c) 2012 - 2014 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.model;


import android.support.annotation.NonNull;

import com.crashlytics.android.Crashlytics;

import org.gnucash.android.app.GnuCashApplication;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;

/**
 * Money represents a money amount and a corresponding currency.
 * Money internally uses {@link BigDecimal} to represent the amounts, which enables it 
 * to maintain high precision afforded by BigDecimal. Money objects are immutable and
 * most operations return new Money objects.
 * Money String constructors should not be passed any locale-formatted numbers. Only
 * {@link Locale#US} is supported e.g. "2.45" will be parsed as 2.45 meanwhile 
 * "2,45" will be parsed to 245 although that could be a decimal in {@link Locale#GERMAN}
 * 
 * @author Ngewi Fet<ngewif@gmail.com>
 *
 */
public final class Money implements Comparable<Money>{

	/**
	 * Currency of the account
	 */
	private Currency mCurrency;
	
	/**
	 * Amount value held by this object
	 */
	private BigDecimal mAmount;
	
	/**
	 * Default rounding mode for Money objects
	 * Defaults to {@link RoundingMode#HALF_EVEN}
	 */
	private static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_EVEN;
	
	/**
	 * Number of decimal places to limit the fractions to when performing operations
	 * Defaults to 2 decimal places
	 */
	private static final int DEFAULT_DECIMAL_PLACES = 2;
	
	/**
	 * Rounding mode to be applied when performing operations
	 * Defaults to {@link #DEFAULT_ROUNDING_MODE}
	 */
	protected RoundingMode ROUNDING_MODE = DEFAULT_ROUNDING_MODE;
	
	/**
	 * Number of decimal places to limit fractions to in arithmetic operations
	 * Defaults to {@link #DEFAULT_DECIMAL_PLACES}
	 */
	protected int DECIMAL_PLACES = DEFAULT_DECIMAL_PLACES;

	/**
	 * Default currency code (according ISO 4217) 
	 * This is typically initialized to the currency of the device default locale,
	 * otherwise US dollars are used
	 */
	public static String DEFAULT_CURRENCY_CODE 	= "USD";

    /**
     * A zero instance with the currency of the default locale.
     * This can be used anywhere where a starting amount is required without having to create a new object
     */
    private static Money sDefaultZero;

    /**
     * Returns a Money instance initialized to the local currency and value 0
     * @return Money instance of value 0 in locale currency
     */
    public static Money getZeroInstance(){
		if (sDefaultZero == null) {
			String currencyCode = Currency.getInstance(GnuCashApplication.getDefaultLocale()).getCurrencyCode();
			sDefaultZero = new Money(BigDecimal.ZERO, Currency.getInstance(currencyCode));
		}
		return sDefaultZero;
    }

	/**
	 * Default constructor
	 * Initializes the object with an amount of 0 and currency set to the device default locale
	 */
	public Money() {
		init();
	}
	
	/**
	 * Overloaded constructor
	 * @param amount {@link BigDecimal} value of the money instance
	 * @param currency {@link Currency} associated with the <code>amount</code>
	 */
	public Money(BigDecimal amount, Currency currency){
		this.mAmount = amount;
		this.mCurrency = currency;
	}
	
	/**
	 * Overloaded constructor.
	 * Accepts strings as arguments and parses them to create the Money object
	 * @param amount Numrical value of the Money
	 * @param currencyCode Currency code as specified by ISO 4217
	 */
	public Money(String amount, String currencyCode){
		setAmount(amount);
		setCurrency(Currency.getInstance(currencyCode));
	}
	
	/**
	 * Overloaded constructor
	 * Accepts <code>context</code> options for rounding mode during operations on this money object
	 * @param amount {@link BigDecimal} value of the money instance
	 * @param currency {@link Currency} associated with the <code>amount</code>
	 * @param context {@link MathContext} specifying rounding mode during operations
	 */
	public Money(BigDecimal amount, Currency currency, MathContext context){
		setAmount(amount);
		setCurrency(currency);
		ROUNDING_MODE = context.getRoundingMode();
		DECIMAL_PLACES = context.getPrecision();
	}

	/**
	 * Constructs a new money amount given the numerator and denominator of the amount.
	 * The rounding mode used for the division is {@link BigDecimal#ROUND_HALF_EVEN}
	 * @param numerator Numerator as integer
	 * @param denominator Denominator as integer
	 * @param currencyCode 3-character currency code string
	 */
	public Money(long numerator, long denominator, String currencyCode){
		mAmount = new BigDecimal(numerator).divide(new BigDecimal(denominator), BigDecimal.ROUND_HALF_EVEN);
		setCurrency(Currency.getInstance(currencyCode));
	}

	/**
	 * Overloaded constructor. 
	 * Initializes the currency to that specified by {@link Money#DEFAULT_CURRENCY_CODE}
	 * @param amount Value associated with this Money object
	 */
	public Money(String amount){
		init();
		setAmount(parseToDecimal(amount));
	}

    /**
     * Copy constructor.
     * Creates a new Money object which is a clone of <code>money</code>
     * @param money Money instance to be cloned
     */
    public Money(Money money){
        setAmount(money.asBigDecimal());
        setCurrency(money.getCurrency());
    }

    /**
     * Creates a new Money instance with 0 amount and the <code>currencyCode</code>
     * @param currencyCode Currency to use for this money instance
     * @return Money object with value 0 and currency <code>currencyCode</code>
     */
    public static Money createZeroInstance(String currencyCode){
        return new Money(BigDecimal.ZERO, Currency.getInstance(currencyCode));
    }

	/**
	 * Initializes the amount and currency to their default values
	 * @see {@link Money#DEFAULT_CURRENCY_CODE}, {@link #DEFAULT_ROUNDING_MODE}, {@link #DEFAULT_DECIMAL_PLACES}
	 */
	private void init() {
		mCurrency = Currency.getInstance(Money.DEFAULT_CURRENCY_CODE);
		mAmount = BigDecimal.ZERO.setScale(DEFAULT_DECIMAL_PLACES, DEFAULT_ROUNDING_MODE);
	}

	/**
	 * Returns the currency of the money object
	 * @return {@link Currency} of the money value
	 */
	public Currency getCurrency() {
		return mCurrency;
	}

	/**
	 * Returns a new <code>Money</code> object the currency specified by <code>currency</code> 
	 * and the same value as this one. No value exchange between the currencies is performed.
	 * @param currency {@link Currency} to assign to new <code>Money</code> object
	 * @return {@link Money} object with same value as current object, but with new <code>currency</code>
	 */
    public Money withCurrency(Currency currency){
		return new Money(mAmount, currency);
	}
	
	/**
	 * Sets the currency of the money object.
	 * No currency value conversion is performed. The old value for the amount is not changed
	 * This method is only used internally when creating the Money object. 
	 * Money objects are immutable and hence this method should not be called out of a constructor
	 * @param currency {@link Currency} to assign to the Money object  
	 */
	private void setCurrency(Currency currency) {
		//TODO: Consider doing a conversion of the value as well in the future
		this.mCurrency = currency;
	}

	/**
	 * Returns the amount represented by this Money object
	 * @return {@link BigDecimal} valure of amount in object
	 */
	public BigDecimal asBigDecimal() {
		return mAmount;
	}
	
	/**
	 * Returns the amount this object
	 * @return Double value of the amount in the object
	 */
	public double asDouble(){
		return mAmount.doubleValue();
	}

	/**
	 * Returns integer value of this Money amount.
	 * The fractional part is discarded
	 * @return Integer representation of this amount
	 * @see BigDecimal#intValue()
	 */
	public int intValue(){
		return mAmount.intValue();
	}

	/**
	 * An alias for {@link #toPlainString()}
	 * @return Money formatted as a string (excludes the currency)
	 */
	public String asString(){
		return toPlainString();
	}
	
	/**
	 * Returns a string representation of the Money object formatted according to 
	 * the <code>locale</code> and includes the currency symbol. 
	 * The output precision is limited to {@link #DECIMAL_PLACES}.
	 * @param locale Locale to use when formatting the object
	 * @return String containing formatted Money representation
	 */
    public String formattedString(Locale locale){
		NumberFormat formatter = NumberFormat.getInstance(locale);
		formatter.setMinimumFractionDigits(DECIMAL_PLACES);
		formatter.setMaximumFractionDigits(DECIMAL_PLACES);
		return formatter.format(asDouble()) + " " + mCurrency.getSymbol(locale);
	}

    /**
     * Equivalent to calling formattedString(Locale.getDefault())
     * @return String formatted Money representation in default locale
     */
    public String formattedString(){
        return formattedString(Locale.getDefault());
    }

	/**
	 * Returns a new Money object whose amount is the negated value of this object amount.
	 * The original <code>Money</code> object remains unchanged.
	 * @return Negated <code>Money</code> object
	 */
    public Money negate(){
		return new Money(mAmount.negate(), mCurrency);
	}
	
	/**
	 * Sets the amount value of this <code>Money</code> object
	 * @param amount {@link BigDecimal} amount to be set
	 */
	private void setAmount(BigDecimal amount) {
		mAmount = amount.setScale(DECIMAL_PLACES, ROUNDING_MODE);
	}
	
	/**
	 * Sets the amount value of this <code>Money</code> object
	 * The <code>amount</code> is parsed by the {@link BigDecimal} constructor
	 * @param amount {@link String} amount to be set
	 */
	private void setAmount(String amount){
		setAmount(parseToDecimal(amount));
	}	
	
	/**
	 * Returns a new <code>Money</code> object whose value is the sum of the values of 
	 * this object and <code>addend</code>.
	 * 
	 * @param addend Second operand in the addition.
	 * @return Money object whose value is the sum of this object and <code>money</code>
	 * @throws IllegalArgumentException if the <code>Money</code> objects to be added have different Currencies
	 */
    public Money add(Money addend){
		if (!mCurrency.equals(addend.mCurrency))
			throw new IllegalArgumentException("Only Money with same currency can be added");
		
		BigDecimal bigD = mAmount.add(addend.mAmount);
		return new Money(bigD, mCurrency);
	}

	/**
	 * Returns a new <code>Money</code> object whose value is the difference of the values of 
	 * this object and <code>subtrahend</code>.
	 * This object is the minuend and the parameter is the subtrahend
	 * @param subtrahend Second operand in the subtraction.
	 * @return Money object whose value is the difference of this object and <code>subtrahend</code>
	 * @throws IllegalArgumentException if the <code>Money</code> objects to be added have different Currencies
	 */
    public Money subtract(Money subtrahend){
		if (!mCurrency.equals(subtrahend.mCurrency))
			throw new IllegalArgumentException("Operation can only be performed on money with same currency");
		
		BigDecimal bigD = mAmount.subtract(subtrahend.mAmount);		
		return new Money(bigD, mCurrency);
	}
	
	/**
	 * Returns a new <code>Money</code> object whose value is the quotient of the values of 
	 * this object and <code>divisor</code>.
	 * This object is the dividend and <code>divisor</code> is the divisor
	 * @param divisor Second operand in the division.
	 * @return Money object whose value is the quotient of this object and <code>divisor</code>
	 * @throws IllegalArgumentException if the <code>Money</code> objects to be added have different Currencies
	 */
    public Money divide(Money divisor){
		if (!mCurrency.equals(divisor.mCurrency))
			throw new IllegalArgumentException("Operation can only be performed on money with same currency");
		
		BigDecimal bigD = mAmount.divide(divisor.mAmount);		
		return new Money(bigD, mCurrency);
	}
	
	/**
	 * Returns a new <code>Money</code> object whose value is the quotient of the division of this objects 
	 * value by the factor <code>divisor</code>
	 * @param divisor Second operand in the addition.
	 * @return Money object whose value is the quotient of this object and <code>divisor</code>
	 */
    public Money divide(int divisor){
		Money moneyDiv = new Money(new BigDecimal(divisor), mCurrency);
		return divide(moneyDiv);
	}
	
	/**
	 * Returns a new <code>Money</code> object whose value is the product of the values of 
	 * this object and <code>money</code>.
	 * 
	 * @param money Second operand in the multiplication.
	 * @return Money object whose value is the product of this object and <code>money</code>
	 * @throws IllegalArgumentException if the <code>Money</code> objects to be added have different Currencies
	 */
    public Money multiply(Money money){
		if (!mCurrency.equals(money.mCurrency))
			throw new IllegalArgumentException("Operation can only be performed on money with same currency");
		
		BigDecimal bigD = mAmount.multiply(money.mAmount);		
		return new Money(bigD, mCurrency);
	}
	
	/**
	 * Returns a new <code>Money</code> object whose value is the product of the division of this objects 
	 * value by the factor <code>multiplier</code>
	 * @param multiplier Factor to multiply the amount by.
	 * @return Money object whose value is the product of this objects values and <code>multiplier</code>
	 */
    public Money multiply(int multiplier){
		Money moneyFactor = new Money(new BigDecimal(multiplier), mCurrency);
		return multiply(moneyFactor);
	}
	
	/**
	 * Returns true if the amount held by this Money object is negative
	 * @return <code>true</code> if the amount is negative, <code>false</code> otherwise.
	 */
	public boolean isNegative(){
		return mAmount.compareTo(BigDecimal.ZERO) == -1;
	}
	
	/**
	 * Returns the string representation of the amount (without currency) of the Money object
	 * @return String representation of the amount (without currency) of the Money object
	 */
	public String toPlainString(){
		return mAmount.setScale(DECIMAL_PLACES, ROUNDING_MODE).toPlainString();
	}
	
	/**
	 * Returns the string representation of the Money object (value + currency) formatted according
	 * to the default locale
	 * @return String representation of the amount formatted with default locale
	 */
    @Override
	public String toString() {
		return formattedString(Locale.getDefault());
	}
		
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (mAmount.hashCode());
		result = prime * result + (mCurrency.hashCode());
		return result;
	}

	/**
	 * Two Money objects are only equal if their amount (value) and currencies are equal
	 * @param obj Object to compare with
	 * @return <code>true</code> if the objects are equal, <code>false</code> otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Money other = (Money) obj;
		if (!mAmount.equals(other.mAmount))
			return false;
		if (!mCurrency.equals(other.mCurrency))
			return false;
		return true;
	}

	@Override
	public int compareTo(@NonNull Money another) {
		if (!mCurrency.equals(another.mCurrency))
			throw new IllegalArgumentException("Cannot compare different currencies yet");
		return mAmount.compareTo(another.mAmount);
	}

	/** TODO: add tests for this
	 * Returns the number of decimal places in this amount
	 * @return Number of decimal places
	 */
	public int getNumberOfDecimalPlaces() {
		String string = mAmount.stripTrailingZeros().toPlainString();
		int index = string.indexOf(".");
		return index < 0 ? 0 : string.length() - index - 1;
	}

	/**
	 * Parses a Locale specific string into a number using format for {@link Locale#US}
	 * @param amountString Formatted String amount
	 * @return String amount formatted in the default locale
	 */
    public static BigDecimal parseToDecimal(String amountString){
		char separator = new DecimalFormatSymbols(Locale.US).getGroupingSeparator();
		amountString = amountString.replace(Character.toString(separator), "");
		NumberFormat formatter = NumberFormat.getInstance(Locale.US);		
		if (formatter instanceof DecimalFormat) {
		     ((DecimalFormat)formatter).setParseBigDecimal(true);		     
		}
		BigDecimal result = new BigDecimal(0);
		try {
			result = (BigDecimal) formatter.parse(amountString);
		} catch (ParseException e) {
			Crashlytics.logException(e);
		}
        return result;
	}

    /**
     * Returns a new instance of {@link Money} object with the absolute value of the current object
     * @return Money object with absolute value of this instance
     */
    public Money absolute() {
        return new Money(mAmount.abs(), mCurrency);
    }

	/**
	 * Checks if the value of this amount is exactly equal to zero.
	 * @return {@code true} if this money amount is zero, {@code false} otherwise
	 */
    public boolean isAmountZero() {
		return mAmount.compareTo(BigDecimal.ZERO) == 0;
	}
}
