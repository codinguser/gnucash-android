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
import org.joda.time.DateTime;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Test scheduled actions
 */
public class ScheduledActionTest {

    @Test
    public void settingStartTime_shouldSetRecurrenceStart(){
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
        long startTime = getTimeInMillis(2014, 8, 26);
        scheduledAction.setStartTime(startTime);
        assertThat(scheduledAction.getRecurrence()).isNull();

        Recurrence recurrence = new Recurrence(PeriodType.MONTH);
        assertThat(recurrence.getPeriodStart().getTime()).isNotEqualTo(startTime);
        scheduledAction.setRecurrence(recurrence);
        assertThat(recurrence.getPeriodStart().getTime()).isEqualTo(startTime);

        long newStartTime = getTimeInMillis(2015, 6, 6);
        scheduledAction.setStartTime(newStartTime);
        assertThat(recurrence.getPeriodStart().getTime()).isEqualTo(newStartTime);
    }

    @Test
    public void settingEndTime_shouldSetRecurrenceEnd(){
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
        long endTime = getTimeInMillis(2014, 8, 26);
        scheduledAction.setEndTime(endTime);
        assertThat(scheduledAction.getRecurrence()).isNull();

        Recurrence recurrence = new Recurrence(PeriodType.MONTH);
        assertThat(recurrence.getPeriodEnd()).isNull();
        scheduledAction.setRecurrence(recurrence);
        assertThat(recurrence.getPeriodEnd().getTime()).isEqualTo(endTime);

        long newEndTime = getTimeInMillis(2015, 6, 6);
        scheduledAction.setEndTime(newEndTime);
        assertThat(recurrence.getPeriodEnd().getTime()).isEqualTo(newEndTime);
    }

    @Test
    public void settingRecurrence_shouldSetScheduledActionStartTime(){
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.BACKUP);
        assertThat(scheduledAction.getStartTime()).isEqualTo(0);

        long startTime = getTimeInMillis(2014, 8, 26);
        Recurrence recurrence = new Recurrence(PeriodType.WEEK);
        recurrence.setPeriodStart(new Timestamp(startTime));
        scheduledAction.setRecurrence(recurrence);
        assertThat(scheduledAction.getStartTime()).isEqualTo(startTime);
    }

    @Test
    public void settingRecurrence_shouldSetEndTime(){
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.BACKUP);
        assertThat(scheduledAction.getStartTime()).isEqualTo(0);

        long endTime = getTimeInMillis(2017, 8, 26);
        Recurrence recurrence = new Recurrence(PeriodType.WEEK);
        recurrence.setPeriodEnd(new Timestamp(endTime));
        scheduledAction.setRecurrence(recurrence);

        assertThat(scheduledAction.getEndTime()).isEqualTo(endTime);
    }

    /**
     * Checks that scheduled actions accurately compute the next run time based on the start date
     * and the last time the action was run
     */
    @Test
    public void testComputingNextScheduledExecution(){
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
        PeriodType periodType = PeriodType.MONTH;

        Recurrence recurrence = new Recurrence(periodType);
        recurrence.setMultiplier(2);
        DateTime startDate = new DateTime(2015, 8, 15, 12, 0);
        recurrence.setPeriodStart(new Timestamp(startDate.getMillis()));
        scheduledAction.setRecurrence(recurrence);

        assertThat(scheduledAction.computeNextCountBasedScheduledExecutionTime()).isEqualTo(startDate.getMillis());

        scheduledAction.setExecutionCount(3);
        DateTime expectedTime = new DateTime(2016, 2, 15, 12, 0);
        assertThat(scheduledAction.computeNextCountBasedScheduledExecutionTime()).isEqualTo(expectedTime.getMillis());
    }

    @Test
    public void testComputingTimeOfLastSchedule(){
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.TRANSACTION);
        PeriodType periodType = PeriodType.WEEK;
        Recurrence recurrence = new Recurrence(periodType);
        recurrence.setMultiplier(2);
        scheduledAction.setRecurrence(recurrence);
        DateTime startDate = new DateTime(2016, 6, 6, 9, 0);
        scheduledAction.setStartTime(startDate.getMillis());

        assertThat(scheduledAction.getTimeOfLastSchedule()).isEqualTo(-1L);

        scheduledAction.setExecutionCount(3);
        DateTime expectedDate = new DateTime(2016, 7, 4, 9, 0);
        assertThat(scheduledAction.getTimeOfLastSchedule()).isEqualTo(expectedDate.getMillis());

    }

    /**
     * Weekly actions scheduled to run on multiple weekdays should be due
     * in each of them in the same week.
     *
     * For an action scheduled on Mondays and Thursdays, we test that, if
     * the last run was on Monday, the next should be due on the Thursday
     * of the same week instead of the following week.
     */
    @Test
    public void multiWeekdayWeeklyActions_shouldBeDueOnEachWeekdaySet() {
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.BACKUP);
        Recurrence recurrence = new Recurrence(PeriodType.WEEK);
        recurrence.setByDays(Arrays.asList(Calendar.MONDAY, Calendar.THURSDAY));
        scheduledAction.setRecurrence(recurrence);
        scheduledAction.setStartTime(new DateTime(2016, 6, 6, 9, 0).getMillis());
        scheduledAction.setLastRun(new DateTime(2017, 4, 17, 9, 0).getMillis()); // Monday

        long expectedNextDueDate = new DateTime(2017, 4, 20, 9, 0).getMillis(); // Thursday
        assertThat(scheduledAction.computeNextTimeBasedScheduledExecutionTime())
                .isEqualTo(expectedNextDueDate);
    }

    /**
     * Weekly actions scheduled with multiplier should skip intermediate
     * weeks and be due in the specified weekday.
     */
    @Test
    public void weeklyActionsWithMultiplier_shouldBeDueOnTheWeekdaySet() {
        ScheduledAction scheduledAction = new ScheduledAction(ScheduledAction.ActionType.BACKUP);
        Recurrence recurrence = new Recurrence(PeriodType.WEEK);
        recurrence.setMultiplier(2);
        recurrence.setByDays(Collections.singletonList(Calendar.WEDNESDAY));
        scheduledAction.setRecurrence(recurrence);
        scheduledAction.setStartTime(new DateTime(2016, 6, 6, 9, 0).getMillis());
        scheduledAction.setLastRun(new DateTime(2017, 4, 12, 9, 0).getMillis()); // Wednesday

        // Wednesday, 2 weeks after the last run
        long expectedNextDueDate = new DateTime(2017, 4, 26, 9, 0).getMillis();
        assertThat(scheduledAction.computeNextTimeBasedScheduledExecutionTime())
                .isEqualTo(expectedNextDueDate);
    }

    private long getTimeInMillis(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);
        return calendar.getTimeInMillis();
    }

    //todo add test for computing the scheduledaction endtime from the recurrence count
}
