package org.gnucash.android.test.unit.db;

import com.ibm.icu.impl.StringUCharacterIterator;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.db.adapter.CommoditiesDbAdapter;
import org.gnucash.android.db.adapter.PricesDbAdapter;
import org.gnucash.android.model.Price;
import org.gnucash.android.test.unit.util.GnucashTestRunner;
import org.gnucash.android.test.unit.util.ShadowCrashlytics;
import org.gnucash.android.test.unit.util.ShadowUserVoice;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * Test price functions
 */
@RunWith(GnucashTestRunner.class) //package is required so that resources can be found in dev mode
@Config(constants = BuildConfig.class, sdk = 21, packageName = "org.gnucash.android", shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class PriceDbAdapterTest {

    /**
     * The price table should override price for any commodity/currency pair
     * todo: maybe move this to UI testing. Not sure how Robolectric handles this
     */
    @Test
    public void shouldOnlySaveOnePricePerCommodityPair(){
        String commodityUID = CommoditiesDbAdapter.getInstance().getCommodityUID("EUR");
        String currencyUID = CommoditiesDbAdapter.getInstance().getCommodityUID("USD");
        Price price = new Price(commodityUID, currencyUID);
        price.setValueNum(134);
        price.setValueDenom(100);

        PricesDbAdapter pricesDbAdapter = PricesDbAdapter.getInstance();
        pricesDbAdapter.addRecord(price);

        price = pricesDbAdapter.getRecord(price.getUID());
        assertThat(pricesDbAdapter.getRecordsCount()).isEqualTo(1);
        assertThat(price.getValueNum()).isEqualTo(134);

        Price price1 = new Price(commodityUID, currencyUID);
        price1.setValueNum(187);
        price1.setValueDenom(100);
        pricesDbAdapter.addRecord(price);

        assertThat(pricesDbAdapter.getRecordsCount()).isEqualTo(1);
        Price savedPrice = pricesDbAdapter.getAllRecords().get(0);
        assertThat(savedPrice.getUID()).isEqualTo(price1.getUID()); //different records
        assertThat(savedPrice.getValueNum()).isEqualTo(187);
        assertThat(savedPrice.getValueDenom()).isEqualTo(100);


        Price price2 = new Price(currencyUID, commodityUID);
        price2.setValueNum(190);
        price2.setValueDenom(100);
        pricesDbAdapter.addRecord(price2);

        assertThat(pricesDbAdapter.getRecordsCount()).isEqualTo(2);
    }
}
