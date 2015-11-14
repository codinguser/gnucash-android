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

import org.gnucash.android.model.PeriodType;
import org.gnucash.android.model.Recurrence;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.Calendar;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test {@link Recurrence}s
 */
public class RecurrenceTest {

    @Test
    public void settingCount_shouldComputeCorrectEndTime(){
        Recurrence recurrence = new Recurrence(PeriodType.MONTH);
        Calendar cal = Calendar.getInstance();
        cal.set(2015, Calendar.OCTOBER, 5);

        recurrence.setPeriodStart(new Timestamp(cal.getTimeInMillis()));
        recurrence.setPeriodEnd(3);

        Calendar endDate = Calendar.getInstance();
        endDate.set(2015, Calendar.DECEMBER, 31);

        assertThat(recurrence.getPeriodEnd().getTime()).isEqualTo(endDate.getTimeInMillis());
    }

    /**
     * When the end date of a recurrence is set, we should be able to correctly get the number of occurrences
     */
    @Test
    public void testRecurrenceCountComputation(){
        //// TODO: 06.11.2015 implement me
        assertThat(false).isTrue();
    }
}
