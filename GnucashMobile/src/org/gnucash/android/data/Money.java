/*
 * Copyright (c) 2012 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.data;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;


import android.util.Log;

/**
 * Money represents a money amount and a corresponding currency.
 * Money internally uses {@link BigDecimal} to represent the amounts, which enables it 
 * to maintain high precision afforded by BigDecimal. Money objects are immutable and
 * most operations return new Money objects.
 * 
 * @author Ngewi Fet<ngewif@gmail.com>
 *
 */
public class Money implements Comparable<Money>{

	private Currency mCurrency;
	private BigDecimal mAmount;
	
	private static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_EVEN;
	private static final int DEFAULT_DECIMAL_PLACES = 2;
	
	protected RoundingMode ROUNDING_MODE = DEFAULT_ROUNDING_MODE;
	protected int DECIMAL_PLACES = DEFAULT_DECIMAL_PLACES;
	
	public Money() {
		init();
	}
	
	public Money(BigDecimal amount, Currency currency){		
		this.mAmount = amount;
		this.mCurrency = currency;
	}
	
	public Money(String amount, String currencyCode){
		setAmount(amount);
		setCurrency(Currency.getInstance(currencyCode));
	}
	
	public Money(BigDecimal amount, Currency currency, MathContext context){
		setAmount(amount);
		setCurrency(currency);
		ROUNDING_MODE = context.getRoundingMode();
		DECIMAL_PLACES = context.getPrecision();
	}
	
	public Money(String amount){
		init();
		setAmount(parse(amount));
	}
	
	public Money(double amount){
		init();
		setAmount(amount);
	}
	
	private void init(){
		mCurrency = Currency.getInstance(Account.DEFAULT_CURRENCY_CODE);
		mAmount = new BigDecimal(0).setScale(DEFAULT_DECIMAL_PLACES, DEFAULT_ROUNDING_MODE);
	}

	/**
	 * @return the mCurrency
	 */
	public Currency getCurrency() {
		return mCurrency;
	}

	public Money withCurrency(Currency currency){
		return new Money(mAmount, currency);
	}
	/**
	 * @param mCurrency the mCurrency to set
	 */
	private void setCurrency(Currency currency) {
		//TODO: Do a conversion of the value as well in the future
		mCurrency = currency;
	}

	/**
	 * @return the mAmount
	 */
	public BigDecimal asBigDecimal() {
		return mAmount;
	}
	
	public double asDouble(){
		return mAmount.doubleValue();
	}
	
	/**
	 * An alias for {@link #toPlainString()}
	 * @return Money formatted as a string (excludes the currency)
	 */
	public String asString(){
		return toPlainString();
	}
	
	public String formattedString(Locale locale){
		DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(locale);	
		formatter.setMinimumFractionDigits(DECIMAL_PLACES);
		formatter.setMaximumFractionDigits(DECIMAL_PLACES);
		
		return formatter.format(asDouble()) + " " + mCurrency.getSymbol();
	}
	
	public Money negate(){
		return new Money(mAmount.negate(), mCurrency);
	}
	
	/**
	 * @param amount the mAmount to set
	 */
	private void setAmount(BigDecimal amount) {	
		mAmount = amount.setScale(DECIMAL_PLACES, ROUNDING_MODE);
	}
	
	private void setAmount(String amount){
		setAmount(new BigDecimal(amount));
	}
	
	private void setAmount(double amount){
		setAmount(new BigDecimal(amount));
	}
	
	public Money add(Money money){
		if (!mCurrency.equals(money.mCurrency))
			throw new IllegalArgumentException("Only Money with same currency can be added");
		
		BigDecimal bigD = mAmount.add(money.mAmount);
		return new Money(bigD, mCurrency);
	}

	public Money subtract(Money money){
		if (!mCurrency.equals(money.mCurrency))
			throw new IllegalArgumentException("Operation can only be performed on money with same currency");
		
		BigDecimal bigD = mAmount.subtract(money.mAmount);		
		return new Money(bigD, mCurrency);
	}
	
	public Money divide(Money divisor){
		if (!mCurrency.equals(divisor.mCurrency))
			throw new IllegalArgumentException("Operation can only be performed on money with same currency");
		
		BigDecimal bigD = mAmount.divide(divisor.mAmount);		
		return new Money(bigD, mCurrency);
	}
	
	public Money divide(int divisor){
		Money moneyDiv = new Money(new BigDecimal(divisor), mCurrency);
		return divide(moneyDiv);
	}
	
	public Money multiply(Money money){
		if (!mCurrency.equals(money.mCurrency))
			throw new IllegalArgumentException("Operation can only be performed on money with same currency");
		
		BigDecimal bigD = mAmount.multiply(money.mAmount);		
		return new Money(bigD, mCurrency);
	}
	
	public Money multiply(int factor){
		Money moneyFactor = new Money(new BigDecimal(factor), mCurrency);
		return multiply(moneyFactor);
	}
	
	public boolean isNegative(){
		return mAmount.compareTo(new BigDecimal(0)) == -1;
	}
	
	public String toPlainString(){
		return mAmount.setScale(DECIMAL_PLACES, ROUNDING_MODE).toPlainString();
	}
	
	@Override
	public String toString() {
		return mAmount.setScale(DECIMAL_PLACES, ROUNDING_MODE).toPlainString() + " " + mCurrency.getSymbol();
	}
		
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mAmount == null) ? 0 : mAmount.hashCode());
		result = prime * result
				+ ((mCurrency == null) ? 0 : mCurrency.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Money other = (Money) obj;
		if (mAmount == null) {
			if (other.mAmount != null)
				return false;
		} else if (!mAmount.equals(other.mAmount))
			return false;
		if (mCurrency == null) {
			if (other.mCurrency != null)
				return false;
		} else if (!mCurrency.equals(other.mCurrency))
			return false;
		return true;
	}

	@Override
	public int compareTo(Money another) {
		if (!mCurrency.equals(another.mCurrency))
			throw new IllegalArgumentException("Cannot compare different currencies yet");
		return mAmount.compareTo(another.mAmount);
	}

	public static String parse(String formattedAmount){
		DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.getDefault());
		String result = formattedAmount;
		try {
			result = formatter.parse(formattedAmount).toString();
			
		} catch (ParseException e) {
			Log.e("Money", "Could not parse the amount");			
		}
		return result;
	}
}
