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
package org.gnucash.android.test.unit.model;

import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Transaction;
import org.junit.Test;

import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountTest{

	@Test
	public void testAccountUsesDefaultCurrency(){
		Account account = new Account("Dummy account");
		assertThat(account.getCurrency().getCurrencyCode()).isEqualTo(Money.DEFAULT_CURRENCY_CODE);
	}

	@Test
	public void testAccountAlwaysHasUID(){
		Account account = new Account("Dummy");
		assertThat(account.getUID()).isNotNull();
	}

	@Test
	public void testTransactionsHaveSameCurrencyAsAccount(){
		Account acc1 = new Account("Japanese", Currency.getInstance("JPY"));
		acc1.setUID("simile");
		Transaction trx = new Transaction("Underground");
		Transaction term = new Transaction( "Tube");

		assertThat(trx.getCurrencyCode()).isEqualTo(Money.DEFAULT_CURRENCY_CODE);

		acc1.addTransaction(trx);
		acc1.addTransaction(term);

		assertThat(trx.getCurrencyCode()).isEqualTo("JPY");
		assertThat(term.getCurrencyCode()).isEqualTo("JPY");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetInvalidColorCode(){
		Account account = new Account("Test");
		account.setColorCode("443859");
	}

	@Test
	public void shouldSetFullNameWhenCreated(){
		String fullName = "Full name ";
		Account account = new Account(fullName);
		assertThat(account.getName()).isEqualTo(fullName.trim()); //names are trimmed
		assertThat(account.getFullName()).isEqualTo(fullName.trim()); //names are trimmed
	}

	@Test
	public void settingNameShouldNotChangeFullName(){
		String fullName = "Full name";
		Account account = new Account(fullName);

		account.setName("Name");
		assertThat(account.getName()).isEqualTo("Name");
		assertThat(account.getFullName()).isEqualTo(fullName);
	}
}
