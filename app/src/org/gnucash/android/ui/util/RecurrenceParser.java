package org.gnucash.android.ui.util;

import android.text.format.Time;
import com.doomonafireball.betterpickers.recurrencepicker.EventRecurrence;
import org.gnucash.android.model.ScheduledEvent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Date: 01.08.2014
 *
 * @author Ngewi
 */
public class RecurrenceParser {
    public static final long SECOND_MILLIS  = 1000;
    public static final long MINUTE_MILLIS  = 60*SECOND_MILLIS;
    public static final long DAY_MILLIS     = 24*60*MINUTE_MILLIS;
    public static final long WEEK_MILLIS    = 7*DAY_MILLIS;
    public static final long MONTH_MILLIS   = 4*WEEK_MILLIS;
    public static final long YEAR_MILLIS    = 12*MONTH_MILLIS;


    public static List<ScheduledEvent> parse(EventRecurrence eventRecurrence){
        long period = 0;
        List<ScheduledEvent> scheduledEventList = new ArrayList<ScheduledEvent>();
        switch(eventRecurrence.freq){
            case EventRecurrence.DAILY:
                period = DAY_MILLIS;
                break;

            case EventRecurrence.WEEKLY: {
                period = WEEK_MILLIS * eventRecurrence.interval;
                for (int day : eventRecurrence.byday) {
                    ScheduledEvent scheduledEvent = new ScheduledEvent(ScheduledEvent.EventType.TRANSACTION);
                    scheduledEvent.period = WEEK_MILLIS;

                    scheduledEvent.startDate = nextDayOfWeek(day).getTimeInMillis();
                    if (eventRecurrence.until != null && eventRecurrence.until.length() > 0) {
                        Time endTime = new Time();
                        endTime.parse(eventRecurrence.until);
                        scheduledEvent.endDate = endTime.toMillis(false);
                    } else if (eventRecurrence.count > 0){
                        scheduledEvent.endDate = scheduledEvent.startDate + (scheduledEvent.period*eventRecurrence.count);
                    }
                    scheduledEventList.add(scheduledEvent);
                }
            }
            break;

            case EventRecurrence.MONTHLY: {
                ScheduledEvent event = new ScheduledEvent(ScheduledEvent.EventType.TRANSACTION);
                event.period = MONTH_MILLIS;
                Calendar now = Calendar.getInstance();
                now.add(Calendar.MONTH, 1);
                event.startDate = now.getTimeInMillis();
                if (eventRecurrence.until != null && eventRecurrence.until.length() > 0) {
                    Time endTime = new Time();
                    endTime.parse(eventRecurrence.until);
                    event.endDate = endTime.toMillis(false);
                } else if (eventRecurrence.count > 0){
                    event.endDate = event.startDate + (event.period*eventRecurrence.count);
                }

                scheduledEventList.add(event);
            }
                break;

            case EventRecurrence.YEARLY: {
                ScheduledEvent event = new ScheduledEvent(ScheduledEvent.EventType.TRANSACTION);
                event.period = YEAR_MILLIS;
                Calendar now = Calendar.getInstance();
                now.add(Calendar.YEAR, 1);
                event.startDate = now.getTimeInMillis();
                if (eventRecurrence.until != null && eventRecurrence.until.length() > 0) {
                    Time endTime = new Time();
                    endTime.parse(eventRecurrence.until);
                    event.endDate = endTime.toMillis(false);
                } else if (eventRecurrence.count > 0){
                    event.endDate = event.startDate + (event.period*eventRecurrence.count);
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
