package org.gnucash.android.test.unit.model;

import junit.framework.TestCase;

import org.gnucash.android.model.Account;
import org.gnucash.android.model.Money;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountTest extends TestCase {


	public AccountTest(String name) {
		super(name);
	}

	public void testAccountUsesDefaultCurrency(){
		Account account = new Account("Dummy account");
		assertThat(account.getCurrency().getCurrencyCode()).isEqualTo(Money.DEFAULT_CURRENCY_CODE);
	}

	public void testAccountAlwaysHasUID(){
		Account account = new Account("Dummy");
		assertThat(account.getUID()).isNotNull();
	}
}
