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

import org.gnucash.android.BuildConfig;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.test.unit.testutil.GnucashTestRunner;
import org.gnucash.android.test.unit.testutil.ShadowCrashlytics;
import org.gnucash.android.test.unit.testutil.ShadowUserVoice;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

@RunWith(GnucashTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, packageName = "org.gnucash.android", shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class MoneyTest{

	private static final String CURRENCY_CODE = "EUR";
	private Money mMoneyInEur;
	private int 	mHashcode;
	private String amountString = "15.75";

	@Before
	public void setUp() throws Exception {
		mMoneyInEur = new Money(new BigDecimal(amountString), Commodity.getInstance(CURRENCY_CODE));
		mHashcode = mMoneyInEur.hashCode();
	}

	@Test
	public void testCreation(){
		Locale.setDefault(Locale.US);
		String amount = "12.25";		
		
		Money temp = new Money(amount, CURRENCY_CODE);
		assertThat("12.25").isEqualTo(temp.toPlainString());
		assertThat(temp.getNumerator()).isEqualTo(1225L);
		assertThat(temp.getDenominator()).isEqualTo(100L);

		Commodity commodity = Commodity.getInstance(CURRENCY_CODE);
		temp = new Money(BigDecimal.TEN, commodity);
		
		assertEquals("10.00", temp.asBigDecimal().toPlainString()); //decimal places for EUR currency
		assertEquals(commodity, temp.getCommodity());
		assertThat("10").isNotEqualTo(temp.asBigDecimal().toPlainString());
	}

	@Test
	public void testAddition(){
		Money result = mMoneyInEur.add(new Money("5", CURRENCY_CODE));
		assertEquals("20.75", result.toPlainString());
		assertNotSame(result, mMoneyInEur);
		validateImmutability();				
	}

	@Test(expected = IllegalArgumentException.class)
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

	@Test(expected = IllegalArgumentException.class)
	public void testSubtractionWithDifferentCurrency(){
		Money addend = new Money("4", "USD");
		mMoneyInEur.subtract(addend);
	}

	@Test
	public void testMultiplication(){
		Money result = mMoneyInEur.multiply(new Money(BigDecimal.TEN, Commodity.getInstance(CURRENCY_CODE)));
		assertThat("157.50").isEqualTo(result.toPlainString());
		assertThat(result).isNotEqualTo(mMoneyInEur);
		validateImmutability();
	}

	@Test(expected = IllegalArgumentException.class)
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

	@Test(expected = IllegalArgumentException.class)
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
	public void testFractionParts(){
		Money money = new Money("14.15", "USD");
		assertThat(money.getNumerator()).isEqualTo(1415L);
		assertThat(money.getDenominator()).isEqualTo(100L);

		money = new Money("125", "JPY");
		assertThat(money.getNumerator()).isEqualTo(125L);
		assertThat(money.getDenominator()).isEqualTo(1L);
	}

	@Test
	public void nonMatchingCommodityFraction_shouldThrowException(){
		Money money = new Money("12.345", "JPY");
		assertThat(money.getNumerator()).isEqualTo(12L);
		assertThat(money.getDenominator()).isEqualTo(1);
	}

	@Test
	public void testPrinting(){
		assertEquals(mMoneyInEur.asString(), mMoneyInEur.toPlainString());
		assertEquals(amountString, mMoneyInEur.asString());

		// the unicode for Euro symbol is \u20AC

		String symbol = Currency.getInstance("EUR").getSymbol(Locale.GERMANY);
		String actualOuputDE = mMoneyInEur.formattedString(Locale.GERMANY);
		assertThat(actualOuputDE).isEqualTo("15,75 " + symbol);

		symbol = Currency.getInstance("EUR").getSymbol(Locale.GERMANY);
		String actualOuputUS = mMoneyInEur.formattedString(Locale.US);
		assertThat(actualOuputUS).isEqualTo(symbol + "15.75");
		
		//always prints with 2 decimal places only
		Money some = new Money("9.7469", CURRENCY_CODE);
		assertEquals("9.75", some.asString());
	}

	public void validateImmutability(){
		assertEquals(mHashcode, mMoneyInEur.hashCode());
		assertEquals(amountString, mMoneyInEur.toPlainString());
		assertEquals(CURRENCY_CODE, mMoneyInEur.getCurrency().getCurrencyCode());
	}
	
}
