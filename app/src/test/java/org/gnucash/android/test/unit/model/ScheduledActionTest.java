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
import org.gnucash.android.model.ScheduledAction;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.Calendar;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * Test scheduled actions
 */
public class ScheduledActionTest {

    @Test
    public void settingStartTime_shouldSetRecurrenceStart(){
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2014, 8, 26);
        long startTime = calendar.getTimeInMillis();
        scheduledAction.setStartTime(startTime);
        assertThat(scheduledAction.getRecurrence()).isNull();

        Recurrence recurrence = new Recurrence(PeriodType.MONTH);
        assertThat(recurrence.getPeriodStart().getTime()).isNotEqualTo(startTime);
        scheduledAction.setRecurrence(recurrence);
        assertThat(recurrence.getPeriodStart().getTime()).isEqualTo(startTime);

        calendar.clear();
        calendar.set(2015, 6, 6);
        long newStartTime = calendar.getTimeInMillis();
        scheduledAction.setStartTime(newStartTime);
        assertThat(recurrence.getPeriodStart().getTime()).isEqualTo(newStartTime);
    }

    @Test
    public void settingRecurrence_shouldSetScheduledActionStartTime(){
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.BACKUP);
        assertThat(scheduledAction.getStartTime()).isEqualTo(0);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2014, 8, 26);
        Recurrence recurrence = new Recurrence(PeriodType.WEEK);
        recurrence.setPeriodStart(new Timestamp(calendar.getTimeInMillis()));
        scheduledAction.setRecurrence(recurrence);
        assertThat(scheduledAction.getStartTime()).isEqualTo(calendar.getTimeInMillis());
    }

    @Test
    public void settingRecurrence_shouldSetEndTime(){
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.BACKUP);
        assertThat(scheduledAction.getStartTime()).isEqualTo(0);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2017, 8, 26);
        Recurrence recurrence = new Recurrence(PeriodType.WEEK);
        recurrence.setPeriodEnd(new Timestamp(calendar.getTimeInMillis()));
        scheduledAction.setRecurrence(recurrence);

        assertThat(scheduledAction.getEndTime()).isEqualTo(calendar.getTimeInMillis());
    }

    //todo add test for computing the scheduledaction endtime from the recurrence count
}
