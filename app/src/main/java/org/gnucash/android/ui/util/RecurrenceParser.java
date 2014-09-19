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
    public static final long MONTH_MILLIS   = 4*WEEK_MILLIS;
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
            case EventRecurrence.DAILY:
                period = DAY_MILLIS;
                break;

            case EventRecurrence.WEEKLY: {
                period = WEEK_MILLIS;
                for (int day : eventRecurrence.byday) {
                    ScheduledEvent scheduledEvent = new ScheduledEvent(eventType);
                    scheduledEvent.setPeriod(period);

                    scheduledEvent.setStartTime(nextDayOfWeek(day).getTimeInMillis());
                    if (eventRecurrence.until != null && eventRecurrence.until.length() > 0) {
                        Time endTime = new Time();
                        endTime.parse(eventRecurrence.until);
                        scheduledEvent.setEndTime(endTime.toMillis(false));
                    } else if (eventRecurrence.count > 0){
                        scheduledEvent.setEndTime(scheduledEvent.getStartTime() + (scheduledEvent.getPeriod() * eventRecurrence.count));
                    }
                    scheduledEventList.add(scheduledEvent);
                }
            }
            break;

            case EventRecurrence.MONTHLY: {
                ScheduledEvent event = new ScheduledEvent(eventType);
                event.setPeriod(MONTH_MILLIS);
                Calendar now = Calendar.getInstance();
                now.add(Calendar.MONTH, 1);
                event.setStartTime(now.getTimeInMillis());
                if (eventRecurrence.until != null && eventRecurrence.until.length() > 0) {
                    Time endTime = new Time();
                    endTime.parse(eventRecurrence.until);
                    event.setEndTime(endTime.toMillis(false));
                } else if (eventRecurrence.count > 0){
                    event.setEndTime(event.getStartTime() + (event.getPeriod()*eventRecurrence.count));
                }

                scheduledEventList.add(event);
            }
                break;

            case EventRecurrence.YEARLY: {
                ScheduledEvent event = new ScheduledEvent(eventType);
                event.setPeriod(YEAR_MILLIS);
                Calendar now = Calendar.getInstance();
                now.add(Calendar.YEAR, 1);
                event.setStartTime(now.getTimeInMillis());
                if (eventRecurrence.until != null && eventRecurrence.until.length() > 0) {
                    Time endTime = new Time();
                    endTime.parse(eventRecurrence.until);
                    event.setEndTime(endTime.toMillis(false));
                } else if (eventRecurrence.count > 0){
                    event.setEndTime(event.getStartTime() + (event.getPeriod()*eventRecurrence.count));
                }
                scheduledEventList.add(event);
            }
                break;
        }
        return scheduledEventList;
    }

    private static Calendar nextDayOfWeek(int dow) {
        Calendar date = Calendar.getInstance();
        int diff = dow - date.get(Calendar.DAY_OF_WEEK);
        if (!(diff > 0)) {
            diff += 7;
        }
        date.add(Calendar.DAY_OF_MONTH, diff);
        return date;
    }
}
