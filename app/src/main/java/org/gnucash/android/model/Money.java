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
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.CommoditiesDbAdapter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.InvalidParameterException;
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

	//// FIXME: 03.11.2015 Currency#getDefaultFractionDigits() is unreliable. Switch to Commodity
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

	public static BigDecimal getBigDecimal(long numerator, long denominator) {
		int scale;
		if (numerator == 0 && denominator == 0) {
			denominator = 1;
		}
		switch ((int)denominator) {
			case 1: scale = 0; break;
			case 10: scale = 1; break;
			case 100: scale = 2; break;
			case 1000: scale = 3; break;
			default:
				throw new InvalidParameterException("invalid denominator " + denominator);
		}
		return new BigDecimal(BigInteger.valueOf(numerator), scale);
	}

	/**
	 * Overloaded constructor
	 * @param amount {@link BigDecimal} value of the money instance
	 * @param currency {@link Currency} associated with the <code>amount</code>
	 */
	public Money(BigDecimal amount, Currency currency){
		this.mAmount = amount;
		setCurrency(currency);
	}

	/**
	 * Creates a new money amount
	 * @param amount Value of the amount
	 * @param commodity Commodity of the money
	 */
	public Money(BigDecimal amount, Commodity commodity){
		this.mAmount = amount;
		mCurrency = Currency.getInstance(commodity.getMnemonic());
	}

	/**
	 * Overloaded constructor.
	 * Accepts strings as arguments and parses them to create the Money object
	 * @param amount Numrical value of the Money
	 * @param currencyCode Currency code as specified by ISO 4217
	 */
	public Money(String amount, String currencyCode){
		setCurrency(Currency.getInstance(currencyCode));
		setAmount(amount);
	}

	/**
	 * Constructs a new money amount given the numerator and denominator of the amount.
	 * The rounding mode used for the division is {@link BigDecimal#ROUND_HALF_EVEN}
	 * @param numerator Numerator as integer
	 * @param denominator Denominator as integer
	 * @param currencyCode 3-character currency code string
	 */
	public Money(long numerator, long denominator, String currencyCode){
		mAmount = getBigDecimal(numerator, denominator);
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
		setCurrency(money.getCurrency());
		setAmount(money.asBigDecimal());
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
		this.mCurrency = currency;
	}

	/**
	 * Returns the GnuCash format numerator for this amount.
	 * <p>Example: Given an amount 32.50$, the numerator will be 3250</p>
	 * @return GnuCash numerator for this amount
	 */
	public long getNumerator() {
		try {
			return mAmount.scaleByPowerOfTen(getScale()).longValueExact();
		} catch (ArithmeticException e) {
			Log.e(getClass().getName(), "Currency " + mCurrency.getCurrencyCode() +
					" with scale " + getScale() +
					" has amount " + mAmount.toString());
			throw e;
		}
	}

	/**
	 * Returns the GnuCash amount format denominator for this amount
	 * <p>The denominator is 10 raised to the power of number of fractional digits in the currency</p>
	 * @return GnuCash format denominator
	 */
	public long getDenominator() {
		switch (getScale()) {
			case 0:
				return 1;
			case 1:
				return 10;
			case 2:
				return 100;
			case 3:
				return 1000;
			case 4:
				return 10000;
		}
		throw new RuntimeException("Unsupported number of fraction digits " + getScale());
	}

	/**
	 * Returns the scale (precision) used for the decimal places of this amount.
	 * <p>The scale used depends on the currency</p>
	 * @return Scale of amount as integer
	 */
	private int getScale() {
		Commodity commodity = CommoditiesDbAdapter.getInstance().getCommodity(mCurrency.getCurrencyCode());
		int scale = commodity.getSmallestFractionDigits();
		if (scale < 0) {
			scale = mAmount.scale();
		}
		if (scale < 0) {
			scale = 0;
		}
		return scale;
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
	 * The output precision is limited to the number of fractional digits supported by the currency
	 * @param locale Locale to use when formatting the object
	 * @return String containing formatted Money representation
	 */
    public String formattedString(Locale locale){
		NumberFormat formatter = NumberFormat.getInstance(locale);
		Commodity commodity = CommoditiesDbAdapter.getInstance().getCommodity(mCurrency.getCurrencyCode());
		formatter.setMinimumFractionDigits(commodity.getSmallestFractionDigits());
		formatter.setMaximumFractionDigits(commodity.getSmallestFractionDigits());
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
		Commodity commodity = CommoditiesDbAdapter.getInstance().getCommodity(mCurrency.getCurrencyCode());
		mAmount = amount.setScale(commodity.getSmallestFractionDigits(), ROUNDING_MODE);
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
	 * Returns a new <code>Money</code> object whose value is the product of this object
	 * and the factor <code>multiplier</code>
	 * <p>The currency of the returned object is the same as the current object</p>
	 * @param multiplier Factor to multiply the amount by.
	 * @return Money object whose value is the product of this objects values and <code>multiplier</code>
	 */
    public Money multiply(int multiplier){
		Money moneyFactor = new Money(new BigDecimal(multiplier), mCurrency);
		return multiply(moneyFactor);
	}

	/**
	 * Returns a new <code>Money</code> object whose value is the product of this object
	 * and the factor <code>multiplier</code>
	 * @param multiplier Factor to multiply the amount by.
	 * @return Money object whose value is the product of this objects values and <code>multiplier</code>
	 */
	public Money multiply(BigDecimal multiplier){
		return new Money(mAmount.multiply(multiplier), mCurrency);
	}

	/**
	 * Returns true if the amount held by this Money object is negative
	 * @return <code>true</code> if the amount is negative, <code>false</code> otherwise.
	 */
	public boolean isNegative(){
		return mAmount.compareTo(BigDecimal.ZERO) == -1;
	}
	
	/**
	 * Returns the string representation of the amount (without currency) of the Money object.
	 * <p>This string is not locale-formatted. The decimal operator is a period (.)</p>
	 * @return String representation of the amount (without currency) of the Money object
	 */
	public String toPlainString(){
		Commodity commodity = CommoditiesDbAdapter.getInstance().getCommodity(getCurrency().getCurrencyCode());
		return mAmount.setScale(commodity.getSmallestFractionDigits(), ROUNDING_MODE).toPlainString();
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

	/** //FIXME: equality failing for money objects
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
