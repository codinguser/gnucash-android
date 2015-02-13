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
import com.doomonafireball.betterpickers.recurrencepicker.EventRecurrence;
import org.gnucash.android.model.ScheduledEvent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Parses {@link com.doomonafireball.betterpickers.recurrencepicker.EventRecurrence}s to generate
 * {@link org.gnucash.android.model.ScheduledEvent}s
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
     * Parses an event recurrence to produce {@link org.gnucash.android.model.ScheduledEvent}s for each recurrence.
     * <p>Each {@link org.gnucash.android.model.ScheduledEvent} represents just one simple repeating schedule, e.g. every Monday.
     * If there are multiple schedules in the recurrence e.g. every Monday and Tuesday, then two ScheduledEvents will be generated</p>
     * @param eventRecurrence Event recurrence pattern obtained from dialog
     * @param eventType Type of event recurrence
     * @return List of ScheduledEvents
     */
    public static List<ScheduledEvent> parse(EventRecurrence eventRecurrence, ScheduledEvent.EventType eventType){
        long period = 0;
        List<ScheduledEvent> scheduledEventList = new ArrayList<ScheduledEvent>();
        switch(eventRecurrence.freq){
            case EventRecurrence.DAILY: {
                if (eventRecurrence.interval == 0) //I assume this is a bug from the picker library
                    period = DAY_MILLIS;
                else
                    period = eventRecurrence.interval * DAY_MILLIS;

                ScheduledEvent scheduledEvent = new ScheduledEvent(eventType);
                scheduledEvent.setPeriod(period);
                parseEndTime(eventRecurrence, scheduledEvent);
                scheduledEventList.add(scheduledEvent);
            }
                break;

            case EventRecurrence.WEEKLY: {
                if (eventRecurrence.interval == 0)
                    period = WEEK_MILLIS;
                else
                    period = eventRecurrence.interval * WEEK_MILLIS;
                for (int day : eventRecurrence.byday) {
                    ScheduledEvent scheduledEvent = new ScheduledEvent(eventType);
                    scheduledEvent.setPeriod(period);

                    scheduledEvent.setStartTime(nextDayOfWeek(day2CalendarDay(day)).getTimeInMillis());
                    parseEndTime(eventRecurrence, scheduledEvent);
                    scheduledEventList.add(scheduledEvent);
                }
            }
            break;

            case EventRecurrence.MONTHLY: {
                if (eventRecurrence.interval == 0)
                    period = MONTH_MILLIS;
                else
                    period = eventRecurrence.interval * MONTH_MILLIS;
                ScheduledEvent event = new ScheduledEvent(eventType);
                event.setPeriod(period);
                Calendar now = Calendar.getInstance();
                now.add(Calendar.MONTH, 1);
                event.setStartTime(now.getTimeInMillis());
                parseEndTime(eventRecurrence, event);

                scheduledEventList.add(event);
            }
                break;

            case EventRecurrence.YEARLY: {
                if (eventRecurrence.interval == 0)
                    period = YEAR_MILLIS;
                else
                    period = eventRecurrence.interval * YEAR_MILLIS;
                ScheduledEvent event = new ScheduledEvent(eventType);
                event.setPeriod(period);
                Calendar now = Calendar.getInstance();
                now.add(Calendar.YEAR, 1);
                event.setStartTime(now.getTimeInMillis());
                parseEndTime(eventRecurrence, event);
                scheduledEventList.add(event);
            }
                break;
        }
        return scheduledEventList;
    }

    /**
     * Parses the end time from an EventRecurrence object and sets it to the <code>scheduledEvent</code>.
     * The end time is specified in the dialog either by number of occurences or a date.
     * @param eventRecurrence Event recurrence pattern obtained from dialog
     * @param scheduledEvent ScheduledEvent to be to updated
     */
    private static void parseEndTime(EventRecurrence eventRecurrence, ScheduledEvent scheduledEvent) {
        if (eventRecurrence.until != null && eventRecurrence.until.length() > 0) {
            Time endTime = new Time();
            endTime.parse(eventRecurrence.until);
            scheduledEvent.setEndTime(endTime.toMillis(false));
        } else if (eventRecurrence.count > 0){
            scheduledEvent.setEndTime(scheduledEvent.getStartTime() + (scheduledEvent.getPeriod() * eventRecurrence.count));
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
