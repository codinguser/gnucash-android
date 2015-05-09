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

}
