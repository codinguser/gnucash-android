package org.gnucash.android.test.unit.export;

import org.gnucash.android.export.xml.GncXmlHelper;
import org.gnucash.android.model.Commodity;
import org.junit.Test;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test the helper methods used for generating GnuCash XML
 */
public class GncXmlHelperTest {

    /**
     * Tests the parsing of split amounts
     */
    @Test
    public void testParseSplitAmount() throws ParseException {
        String splitAmount = "12345/100";
        BigDecimal amount = GncXmlHelper.parseSplitAmount(splitAmount);
        assertThat(amount.toPlainString()).isEqualTo("123.45");

        amount = GncXmlHelper.parseSplitAmount("1.234,50/100");
        assertThat(amount.toPlainString()).isEqualTo("1234.50");
    }

    @Test(expected = ParseException.class)
    public void shouldFailToParseWronglyFormattedInput() throws ParseException {
        GncXmlHelper.parseSplitAmount("123.45");
    }

    @Test
    public void testFormatSplitAmount(){
        Commodity usdCommodity = new Commodity("US Dollars", "USD", 100);
        Commodity euroCommodity = new Commodity("Euro", "EUR", 100);

        BigDecimal bigDecimal = new BigDecimal("45.90");
        String amount = GncXmlHelper.formatSplitAmount(bigDecimal, usdCommodity);
        assertThat(amount).isEqualTo("4590/100");


        bigDecimal = new BigDecimal("350");
        amount = GncXmlHelper.formatSplitAmount(bigDecimal, euroCommodity);
        assertThat(amount).isEqualTo("35000/100");
    }
}
