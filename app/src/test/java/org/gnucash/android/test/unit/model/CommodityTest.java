/*
 * Copyright (c) 2015 Ngewi Fet <ngewif@gmail.com>
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

import org.gnucash.android.model.Commodity;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test commodities
 */
public class CommodityTest {

    @Test
    public void setSmallestFraction_shouldNotUseDigits(){
        Commodity commodity = new Commodity("Test", "USD", 100);
        assertThat(commodity.getSmallestFraction()).isEqualTo(100);

        commodity.setSmallestFraction(1000);
        assertThat(commodity.getSmallestFraction()).isEqualTo(1000);
    }

    @Test
    public void testSmallestFractionDigits(){
        Commodity commodity = new Commodity("Test", "USD", 100);
        assertThat(commodity.getSmallestFractionDigits()).isEqualTo(2);

        commodity.setSmallestFraction(10);
        assertThat(commodity.getSmallestFractionDigits()).isEqualTo(1);

        commodity.setSmallestFraction(1);
        assertThat(commodity.getSmallestFractionDigits()).isEqualTo(0);

    }
}
