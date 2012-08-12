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

package org.gnucash.android.test.unit;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;

import junit.framework.TestCase;

import org.gnucash.android.data.Money;

public class MoneyTest extends TestCase {

	private static final String CURRENCY_CODE = "EUR";
	private Money 	money; 
	private int 	mHashcode;
	private double 	amount = 15.75;
	
	public MoneyTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		money = new Money(new BigDecimal(amount), Currency.getInstance(CURRENCY_CODE));
		mHashcode = money.hashCode();
	}

	public void testCreation(){
		String amount = "12.25";
		if (Locale.getDefault().equals(Locale.GERMANY)) 
			amount = "12,25";
		
		Money temp = new Money(amount);
		assertEquals(12.25, temp.asDouble());
		
		temp = new Money(9.95);
		assertEquals(9.95, temp.asDouble());
		
		BigDecimal decimal = new BigDecimal(8);
		Currency currency = Currency.getInstance(CURRENCY_CODE);
		temp = new Money(decimal, currency);
		
		assertEquals(decimal, temp.asBigDecimal());
		assertEquals(currency, temp.getCurrency());
		
		amount = "15.50";
		if (Locale.getDefault().equals(Locale.GERMANY)) 
			amount = "15,50";
		temp = new Money(amount,"USD");
		assertEquals(15.50, temp.asDouble());
		assertEquals(temp.getCurrency().getCurrencyCode(), "USD");		
	}
	
	public void testAddition(){		
		Money result = money.add(new Money("5", CURRENCY_CODE));
		assertEquals(amount + 5, result.asDouble());
		assertNotSame(result, money);
		validateImmutability();				
	}
	
	public void testAdditionWithIncompatibleCurrency(){
		Money addend = new Money("4", "USD");
		Exception expectedException = null;
		try{
			money.add(addend);
		} catch (Exception e) {
			expectedException = e;
		}
		assertNotNull(expectedException);
		assertTrue(expectedException instanceof IllegalArgumentException);		
	}
	
	public void testSubtraction(){
		Money result = money.subtract(new Money("2", CURRENCY_CODE));
		assertEquals(amount-2, result.asDouble());
		assertNotSame(result, money);
		validateImmutability();		
	}
	
	public void testSubtractionWithDifferentCurrency(){
		Money addend = new Money("4", "USD");
		Exception expectedException = null;
		try{
			money.subtract(addend);
		} catch (Exception e) {
			expectedException = e;
		}
		assertNotNull(expectedException);
		assertTrue(expectedException instanceof IllegalArgumentException);		
	}
	
	public void testMultiplication(){
		Money result = money.multiply(new Money("3", CURRENCY_CODE));
		assertEquals(amount*3, result.asDouble());
		assertNotSame(result, money);
		validateImmutability();
	}
	
	public void testMultiplicationWithDifferentCurrencies(){
		Money addend = new Money("4", "USD");
		Exception expectedException = null;
		try{
			money.multiply(addend);
		} catch (Exception e) {
			expectedException = e;
		}
		assertNotNull(expectedException);
		assertTrue(expectedException instanceof IllegalArgumentException);		
	}
	
	public void testDivision(){
		Money result = money.divide(2);
		assertEquals(amount/2, result.asDouble());		
		assertNotSame(result, money);
		validateImmutability();
	}
			
	public void testDivisionWithDifferentCurrency(){
		Money addend = new Money("4", "USD");
		Exception expectedException = null;
		try{
			money.divide(addend);
		} catch (Exception e) {
			expectedException = e;
		}
		assertNotNull(expectedException);
		assertTrue(expectedException instanceof IllegalArgumentException);		
	}
		
	public void testNegation(){
		Money result = money.negate();
		assertEquals(amount*-1, result.asDouble());
		
		validateImmutability();
	}
	
	public void testPrinting(){
		assertEquals(money.asString(), money.toPlainString());
		assertEquals("15.75", money.asString());
		
		// the unicode for Euro symbol is \u20AC
		String symbol = Currency.getInstance("EUR").getSymbol(Locale.GERMAN);
		String symbolUS = Currency.getInstance("EUR").getSymbol(Locale.US);
		assertEquals("15,75 " + symbol, money.formattedString(Locale.GERMAN));		
		assertEquals("15.75 " + symbolUS, money.formattedString(Locale.US));
		
		//always prints with 2 decimal places only
		Money some = new Money(9.7469);
		assertEquals("9.75", some.asString());
	}
	
	public void validateImmutability(){
		assertEquals(mHashcode, money.hashCode());
		assertEquals(amount, money.asDouble());
		assertEquals(CURRENCY_CODE, money.getCurrency().getCurrencyCode());
	}
	
}
