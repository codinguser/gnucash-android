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

        cal.set(2016, Calendar.JANUARY, 5);
        assertThat(recurrence.getPeriodEnd().getTime()).isEqualTo(cal.getTimeInMillis());
    }

    /**
     * When the end date of a recurrence is set, we should be able to correctly get the number of occurrences
     */
    @Test
    public void testRecurrenceCountComputation(){
        Recurrence recurrence = new Recurrence(PeriodType.MONTH);
        Calendar cal = Calendar.getInstance();
        cal.set(2015, Calendar.OCTOBER, 5);

        recurrence.setPeriodStart(new Timestamp(cal.getTimeInMillis()));
        cal.set(2016, Calendar.AUGUST, 5);
        recurrence.setPeriodEnd(new Timestamp(cal.getTimeInMillis()));

        assertThat(recurrence.getCount()).isEqualTo(10);
    }

    /**
     * When no end period is set, getCount() should return the special value -1.
     *
     * <p>Tests for bug https://github.com/codinguser/gnucash-android/issues/526</p>
     */
    @Test
    public void notSettingEndDate_shouldReturnSpecialCountValue() {
        Recurrence recurrence = new Recurrence(PeriodType.MONTH);
        Calendar cal = Calendar.getInstance();
        cal.set(2015, Calendar.OCTOBER, 5);
        recurrence.setPeriodStart(new Timestamp(cal.getTimeInMillis()));

        assertThat(recurrence.getCount()).isEqualTo(-1);
    }
}
