/*
 * Copyright (c) 2014 Ngewi Fet <ngewif@gmail.com>
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

package org.gnucash.android.ui.util;

import android.text.format.Time;

import com.codetroopers.betterpickers.recurrencepicker.EventRecurrence;

import org.gnucash.android.model.PeriodType;
import org.gnucash.android.model.Recurrence;
import org.gnucash.android.model.ScheduledAction;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Parses {@link EventRecurrence}s to generate
 * {@link org.gnucash.android.model.ScheduledAction}s
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
public class RecurrenceParser {
    public static final long SECOND_MILLIS  = 1000;
    public static final long MINUTE_MILLIS  = 60*SECOND_MILLIS;
    public static final long DAY_MILLIS     = 24*60*MINUTE_MILLIS;
    public static final long WEEK_MILLIS    = 7*DAY_MILLIS;
    public static final long MONTH_MILLIS   = 30*DAY_MILLIS;
    public static final long YEAR_MILLIS    = 12*MONTH_MILLIS;

    /**
     * Parse an {@link EventRecurrence} into a {@link Recurrence} object
     * @param eventRecurrence EventRecurrence object
     * @return Recurrence object
     */
    public static Recurrence parse(EventRecurrence eventRecurrence){
        if (eventRecurrence == null)
            return null;

        PeriodType periodType;
        switch(eventRecurrence.freq){
            case EventRecurrence.DAILY:
                periodType = PeriodType.DAY;
                break;

            case EventRecurrence.WEEKLY:
                periodType = PeriodType.WEEK;
                break;

            case EventRecurrence.MONTHLY:
                periodType = PeriodType.MONTH;
                break;

            case EventRecurrence.YEARLY:
                periodType = PeriodType.YEAR;
                break;

            default:
                periodType = PeriodType.MONTH;
                break;
        }

        int interval = eventRecurrence.interval == 0 ? 1 : eventRecurrence.interval; //bug from betterpickers library sometimes returns 0 as the interval
        periodType.setMultiplier(interval);
        Recurrence recurrence = new Recurrence(periodType);
        parseEndTime(eventRecurrence, recurrence);
        recurrence.setByDay(parseByDay(eventRecurrence.byday));
        recurrence.setPeriodStart(new Timestamp(eventRecurrence.startDate.toMillis(false)));

        return recurrence;
    }

    /**
     * Parses the end time from an EventRecurrence object and sets it to the <code>scheduledEvent</code>.
     * The end time is specified in the dialog either by number of occurences or a date.
     * @param eventRecurrence Event recurrence pattern obtained from dialog
     * @param recurrence Recurrence event to set the end period to
     */
    private static void parseEndTime(EventRecurrence eventRecurrence, Recurrence recurrence) {
        if (eventRecurrence.until != null && eventRecurrence.until.length() > 0) {
            Time endTime = new Time();
            endTime.parse(eventRecurrence.until);
            recurrence.setPeriodEnd(new Timestamp(endTime.toMillis(false)));
        } else if (eventRecurrence.count > 0){
            recurrence.setPeriodEnd(eventRecurrence.count);
        }
    }

    /**
     * Returns the date for the next day of the week
     * @param dow Day of the week (Calendar constants)
     * @return Calendar instance with the next day of the week
     */
    private static Calendar nextDayOfWeek(int dow) {
        Calendar date = Calendar.getInstance();
        int diff = dow - date.get(Calendar.DAY_OF_WEEK);
        if (!(diff > 0)) {
            diff += 7;
        }
        date.add(Calendar.DAY_OF_MONTH, diff);
        return date;
    }

    /**
     * Parses an array of byday values to return the string concatenation of days of the week.
     * <p>Currently only supports byDay values for weeks</p>
     * @param byday Array of byday values
     * @return String concat of days of the week or null if {@code byday} was empty
     */
    private static String parseByDay(int[] byday){
        if (byday == null || byday.length == 0){
            return null;
        }
        //todo: parse for month and year as well, when our dialog supports those
        StringBuilder builder = new StringBuilder();
        for (int day : byday) {
            switch (day)
            {
                case EventRecurrence.SU:
                    builder.append("SU");
                    break;
                case EventRecurrence.MO:
                    builder.append("MO");
                    break;
                case EventRecurrence.TU:
                    builder.append("TU");
                    break;
                case EventRecurrence.WE:
                    builder.append("WE");
                    break;
                case EventRecurrence.TH:
                    builder.append("TH");
                    break;
                case EventRecurrence.FR:
                    builder.append("FR");
                    break;
                case EventRecurrence.SA:
                    builder.append("SA");
                    break;
                default:
                    throw new RuntimeException("bad day of week: " + day);
            }
            builder.append(",");
        }
        builder.deleteCharAt(builder.length());
        return builder.toString();
    }

    /**
     * Converts one of the SU, MO, etc. constants to the Calendar.SUNDAY
     * constants.  btw, I think we should switch to those here too, to
     * get rid of this function, if possible.
     */
    public static int day2CalendarDay(int day)
    {
        switch (day)
        {
            case EventRecurrence.SU:
                return Calendar.SUNDAY;
            case EventRecurrence.MO:
                return Calendar.MONDAY;
            case EventRecurrence.TU:
                return Calendar.TUESDAY;
            case EventRecurrence.WE:
                return Calendar.WEDNESDAY;
            case EventRecurrence.TH:
                return Calendar.THURSDAY;
            case EventRecurrence.FR:
                return Calendar.FRIDAY;
            case EventRecurrence.SA:
                return Calendar.SATURDAY;
            default:
                throw new RuntimeException("bad day of week: " + day);
        }
    }
}
