/*
 * Written By: Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2012 Ngewi Fet
 *
 * This file is part of Gnucash for Android
 * 
 * Gnucash for Android is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, contact:
 *
 * Free Software Foundation           Voice:  +1-617-542-5942
 * 51 Franklin Street, Fifth Floor    Fax:    +1-617-542-2652
 * Boston, MA  02110-1301,  USA       gnu@gnu.org
 */

package org.gnucash.android.data;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

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
	
	public Money(BigDecimal amount, Currency currency, MathContext context){
		setAmount(amount);
		setCurrency(currency);
		ROUNDING_MODE = context.getRoundingMode();
		DECIMAL_PLACES = context.getPrecision();
	}
	
	public Money(String amount){
		init();
		setAmount(amount);
	}
	
	public Money(double amount){
		init();
		setAmount(amount);
	}
	
	private void init(){
		mCurrency = Currency.getInstance(Locale.getDefault());
		mAmount = new BigDecimal(0).setScale(DEFAULT_DECIMAL_PLACES, DEFAULT_ROUNDING_MODE);
	}

	/**
	 * @return the mCurrency
	 */
	public Currency getCurrency() {
		return mCurrency;
	}

	/**
	 * @param mCurrency the mCurrency to set
	 */
	public Money setCurrency(Currency currency) {
		//TODO: Do a conversion of the value as well in the future
		return new Money(mAmount, currency);
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
	
	public String asString(){
		return mAmount.toPlainString();
	}
	
	public String formattedString(Locale locale){
		DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(locale);			
		return formatter.format(asDouble());
	}
	
	public Money negate(){
		return new Money(mAmount.negate(), mCurrency);
	}
	
	/**
	 * @param amount the mAmount to set
	 */
	public Money setAmount(BigDecimal amount) {		
		return new Money(amount.setScale(DECIMAL_PLACES, ROUNDING_MODE), mCurrency);
	}
	
	public Money setAmount(String amount){
		return setAmount(new BigDecimal(amount));
	}
	
	public Money setAmount(double amount){
		return setAmount(new BigDecimal(amount));
	}
	
	public Money add(Money money){
		if (!mCurrency.equals(money.mCurrency))
			throw new IllegalArgumentException("Only Money with same currency can be added");
		
		Money result = new Money();
		BigDecimal bigD = mAmount.add(money.mAmount);
		result.setAmount(bigD);
		return result;
	}

	public Money subtract(Money money){
		if (!mCurrency.equals(money.mCurrency))
			throw new IllegalArgumentException("Operation can only be performed on money with same currency");
		Money result = new Money();
		BigDecimal bigD = mAmount.subtract(money.mAmount);
		result.setAmount(bigD);
		return result;
	}
	
	public Money divide(Money divisor){
		if (!mCurrency.equals(divisor.mCurrency))
			throw new IllegalArgumentException("Operation can only be performed on money with same currency");
		Money result = new Money();
		BigDecimal bigD = mAmount.divide(divisor.mAmount);
		result.setAmount(bigD);
		return result;
	}
	
	public Money divide(int divisor){
		Money moneyDiv = new Money(new BigDecimal(divisor), mCurrency);
		return divide(moneyDiv);
	}
	
	public Money multiply(Money money){
		if (!mCurrency.equals(money.mCurrency))
			throw new IllegalArgumentException("Operation can only be performed on money with same currency");
		Money result = new Money();
		BigDecimal bigD = mAmount.multiply(money.mAmount);
		result.setAmount(bigD);
		return result;
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
		return mAmount.setScale(DECIMAL_PLACES, ROUNDING_MODE).toPlainString() + " " + mCurrency.getCurrencyCode();
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
}
