package org.gnucash.android.test.unit.model;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.test.unit.testutil.GnucashTestRunner;
import org.gnucash.android.test.unit.testutil.ShadowCrashlytics;
import org.gnucash.android.test.unit.testutil.ShadowUserVoice;
import org.gnucash.android.ui.transaction.SplitEditorFragment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(GnucashTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, packageName = "org.gnucash.android", shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class TransactionTest {

	@Test
	public void testCloningTransaction(){
		Transaction transaction = new Transaction("Bobba Fett");
		assertThat(transaction.getUID()).isNotNull();
		assertThat(transaction.getCurrencyCode()).isEqualTo(Commodity.DEFAULT_COMMODITY.getCurrencyCode());

		Transaction clone1 = new Transaction(transaction, false);
		assertThat(transaction.getUID()).isEqualTo(clone1.getUID());
		assertThat(transaction).isEqualTo(clone1);

		Transaction clone2 = new Transaction(transaction, true);
		assertThat(transaction.getUID()).isNotEqualTo(clone2.getUID());
		assertThat(transaction.getCurrencyCode()).isEqualTo(clone2.getCurrencyCode());
		assertThat(transaction.getDescription()).isEqualTo(clone2.getDescription());
		assertThat(transaction.getNote()).isEqualTo(clone2.getNote());
		assertThat(transaction.getTimeMillis()).isEqualTo(clone2.getTimeMillis());
		//TODO: Clone the created_at and modified_at times?
	}

	/**
	 * Adding a split to a transaction should set the transaction UID of the split to the GUID of the transaction
	 */
	@Test
	public void addingSplitsShouldSetTransactionUID(){
		Transaction transaction = new Transaction("");
		assertThat(transaction.getCurrencyCode()).isEqualTo(Commodity.DEFAULT_COMMODITY.getCurrencyCode());

		Split split = new Split(Money.getZeroInstance(), "test-account");
		assertThat(split.getTransactionUID()).isEmpty();

		transaction.addSplit(split);
		assertThat(split.getTransactionUID()).isEqualTo(transaction.getUID());
	}

	@Test
	public void settingUID_shouldSetTransactionUidOfSplits(){
		Transaction t1 = new Transaction("Test");
		Split split1 = new Split(Money.getZeroInstance(), "random");
		split1.setTransactionUID("non-existent");

		Split split2 = new Split(Money.getZeroInstance(), "account-something");
		split2.setTransactionUID("pre-existent");

		List<Split> splits = new ArrayList<>();
		splits.add(split1);
		splits.add(split2);

		t1.setSplits(splits);

		assertThat(t1.getSplits()).extracting("mTransactionUID")
				.contains(t1.getUID())
				.doesNotContain("non-existent")
				.doesNotContain("pre-existent");
	}
}
