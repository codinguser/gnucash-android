/*
 * Copyright (c) 2014 - 2015 Ngewi Fet <ngewif@gmail.com>
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
