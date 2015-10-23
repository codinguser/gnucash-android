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

package org.gnucash.android.test.unit.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import org.gnucash.android.model.Money;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MoneyTest{

	private static final String CURRENCY_CODE = "EUR";
	private Money mMoneyInEur;
	private int 	mHashcode;
	private String amountString = "15.75";

	@Before
	public void setUp() throws Exception {
		mMoneyInEur = new Money(new BigDecimal(amountString), Currency.getInstance(CURRENCY_CODE));
		mHashcode = mMoneyInEur.hashCode();
	}

	@Test
	public void testCreation(){
		Locale.setDefault(Locale.US);
		String amount = "12.25";		
		
		Money temp = new Money(amount);
		assertThat("12.25").isEqualTo(temp.toPlainString());
		assertThat(Money.DEFAULT_CURRENCY_CODE).isEqualTo(temp.getCurrency().getCurrencyCode());

		Currency currency = Currency.getInstance(CURRENCY_CODE);
		temp = new Money(BigDecimal.TEN, currency);
		
		assertEquals("10", temp.asBigDecimal().toPlainString());
		assertEquals(currency, temp.getCurrency());

		//test only Locale.US parsing even under different locale
		Locale.setDefault(Locale.GERMANY);
		amount = "12,25";
		temp = new Money(amount);
		assertEquals("1225.00", temp.toPlainString());
	}

	@Test
	public void testAddition(){
		Money result = mMoneyInEur.add(new Money("5", CURRENCY_CODE));
		assertEquals("20.75", result.toPlainString());
		assertNotSame(result, mMoneyInEur);
		validateImmutability();				
	}

	@Test(expected = Money.CurrencyMismatchException.class)
	public void testAdditionWithIncompatibleCurrency(){
		Money addend = new Money("4", "USD");
		mMoneyInEur.add(addend);
	}

	@Test
	public void testSubtraction(){
		Money result = mMoneyInEur.subtract(new Money("2", CURRENCY_CODE));
		assertEquals(new BigDecimal("13.75"), result.asBigDecimal());
		assertNotSame(result, mMoneyInEur);
		validateImmutability();		
	}

	@Test(expected = Money.CurrencyMismatchException.class)
	public void testSubtractionWithDifferentCurrency(){
		Money addend = new Money("4", "USD");
		mMoneyInEur.subtract(addend);
	}

	@Test
	public void testMultiplication(){
		Money result = mMoneyInEur.multiply(new Money(BigDecimal.TEN, Currency.getInstance(CURRENCY_CODE)));
		assertThat("157.50").isEqualTo(result.toPlainString());
		assertThat(result).isNotEqualTo(mMoneyInEur);
		validateImmutability();
	}

	@Test(expected = Money.CurrencyMismatchException.class)
	public void testMultiplicationWithDifferentCurrencies(){
		Money addend = new Money("4", "USD");
		mMoneyInEur.multiply(addend);
	}

	@Test
	public void testDivision(){
		Money result = mMoneyInEur.divide(2);
		assertThat(result.toPlainString()).isEqualTo("7.88");
		assertThat(result).isNotEqualTo(mMoneyInEur);
		validateImmutability();
	}

	@Test(expected = Money.CurrencyMismatchException.class)
	public void testDivisionWithDifferentCurrency(){
		Money addend = new Money("4", "USD");
		mMoneyInEur.divide(addend);
	}

	@Test
	public void testNegation(){
		Money result = mMoneyInEur.negate();
		assertThat(result.toPlainString()).startsWith("-");
		validateImmutability();
	}

	@Test
	public void testPrinting(){
		assertEquals(mMoneyInEur.asString(), mMoneyInEur.toPlainString());
		assertEquals(amountString, mMoneyInEur.asString());
		
		// the unicode for Euro symbol is \u20AC
		String symbol = Currency.getInstance("EUR").getSymbol(Locale.GERMAN);
		String symbolUS = Currency.getInstance("EUR").getSymbol(Locale.US);
		assertEquals("15,75 " + symbol, mMoneyInEur.formattedString(Locale.GERMAN));
		assertEquals("15.75 " + symbolUS, mMoneyInEur.formattedString(Locale.US));
		
		//always prints with 2 decimal places only
		Money some = new Money("9.7469");
		assertEquals("9.75", some.asString());
	}

	public void validateImmutability(){
		assertEquals(mHashcode, mMoneyInEur.hashCode());
		assertEquals(amountString, mMoneyInEur.toPlainString());
		assertEquals(CURRENCY_CODE, mMoneyInEur.getCurrency().getCurrencyCode());
	}
	
}
