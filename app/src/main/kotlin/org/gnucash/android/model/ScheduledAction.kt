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
package org.gnucash.android.model

import androidx.annotation.StringRes
import org.gnucash.android.R
import org.gnucash.android.app.GnuCashApplication
import org.joda.time.LocalDateTime
import java.sql.Timestamp
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.gnucash.android.export.xml.GncXmlHelper
import org.joda.time.format.DateTimeFormat

/**
 * Represents a scheduled event which is stored in the database and run at regular mPeriod
 *
 * @author Ngewi Fet <ngewif@gmail.com>
 */
class ScheduledAction    //all actions are enabled by default
    (
    /**
     * Type of event being scheduled
     */
    var actionType: ActionType
) : BaseModel() {

    private var _startDate: Long = 0

    private var _endDate: Long = 0

    /**
     * The tag saves additional information about the scheduled action,
     * e.g. such as export parameters for scheduled backups
     */
    var tag: String? = null

    /**
     * Recurrence of this scheduled action
     */
    var recurrence: Recurrence? = null
        private set

    /**
     * Types of events which can be scheduled
     */
    enum class ActionType(@JvmField @StringRes val labelId: Int) {
        TRANSACTION(R.string.action_transaction),
        // TODO rename `BACKUP` to `EXPORT`
        BACKUP(R.string.action_backup)
    }

    /**
     * Next scheduled run of Event
     */
    var lastRunTime: Long = 0
        private set

    /**
     * Unique ID of the template from which the recurring event will be executed.
     * For example, transaction UID
     */
    var actionUID: String? = null

    /**
     * Flag indicating if this event is enabled or not
     */
    var isEnabled = true

    /**
     * Number of times this event is planned to be executed
     */
    var totalPlannedExecutionCount = 0

    /**
     * How many times this action has already been executed
     */
    var executionCount = 0

    /**
     * Flag for whether the scheduled transaction should be auto-created
     */
    private var _autoCreate = true
    private var _autoNotify = false

    /**
     * Number of days in advance to create the transaction
     *
     * This flag is currently unused in the app. It is only included here for compatibility with GnuCash desktop XML
     */
    var advanceCreateDays = 0

    /**
     * The number of days in advance to notify of scheduled transactions
     *
     * This flag is currently unused in the app. It is only included here for compatibility with GnuCash desktop XML
     */
    var advanceNotifyDays = 0

    /**
     * Returns the time when the last schedule in the sequence of planned executions was executed.
     * This relies on the number of executions of the scheduled action
     *
     * This is different from [.getLastRunTime] which returns the date when the system last
     * run the scheduled action.
     *
     * @return Time of last schedule, or -1 if the scheduled action has never been run
     */
    val timeOfLastSchedule: Long
        get() {
            if (executionCount == 0) return -1
            var startTime = LocalDateTime.fromDateFields(Date(_startDate))
            val multiplier = recurrence!!.multiplier
            val factor = (executionCount - 1) * multiplier
            startTime = when (recurrence!!.periodType) {
                PeriodType.HOUR -> startTime.plusHours(factor)
                PeriodType.DAY -> startTime.plusDays(factor)
                PeriodType.WEEK -> startTime.plusWeeks(factor)
                PeriodType.MONTH -> startTime.plusMonths(factor)
                PeriodType.YEAR -> startTime.plusYears(factor)
            }
            return startTime.toDate().time
        }

    /**
     * Computes the next time that this scheduled action is supposed to be
     * executed based on the execution count.
     *
     * This method does not consider the end time, or number of times it should be run.
     * It only considers when the next execution would theoretically be due.
     *
     * @return Next run time in milliseconds
     */
    fun computeNextCountBasedScheduledExecutionTime(): Long {
        return computeNextScheduledExecutionTimeStartingAt(timeOfLastSchedule)
    }

    /**
     * Computes the next time that this scheduled action is supposed to be
     * executed based on the time of the last run.
     *
     * This method does not consider the end time, or number of times it should be run.
     * It only considers when the next execution would theoretically be due.
     *
     * @return Next run time in milliseconds
     */
    fun computeNextTimeBasedScheduledExecutionTime(): Long {
        return computeNextScheduledExecutionTimeStartingAt(lastRunTime)
    }

    /**
     * Computes the next time that this scheduled action is supposed to be
     * executed starting at startTime.
     *
     * This method does not consider the end time, or number of times it should be run.
     * It only considers when the next execution would theoretically be due.
     *
     * @param startTime time in milliseconds to use as start to compute the next schedule.
     * @return Next run time in milliseconds
     */
    private fun computeNextScheduledExecutionTimeStartingAt(startTime: Long): Long {
        if (startTime <= 0) { // has never been run
            return _startDate
        }
        val multiplier = recurrence!!.multiplier
        var nextScheduledExecution = LocalDateTime.fromDateFields(Date(startTime))
        nextScheduledExecution = when (recurrence!!.periodType) {
            PeriodType.HOUR -> nextScheduledExecution.plusHours(multiplier)
            PeriodType.DAY -> nextScheduledExecution.plusDays(multiplier)
            PeriodType.WEEK -> computeNextWeeklyExecutionStartingAt(nextScheduledExecution)
            PeriodType.MONTH -> nextScheduledExecution.plusMonths(multiplier)
            PeriodType.YEAR -> nextScheduledExecution.plusYears(multiplier)
        }
        return nextScheduledExecution.toDate().time
    }

    /**
     * Computes the next time that this weekly scheduled action is supposed to be
     * executed starting at startTime.
     *
     * If no days of the week have been set (GnuCash desktop allows it), it will return a
     * date in the future to ensure ScheduledActionService doesn't execute it.
     *
     * @param startTime LocalDateTime to use as start to compute the next schedule.
     * @return Next run time as a LocalDateTime. A date in the future, if no days of the week
     * were set in the Recurrence.
     */
    private fun computeNextWeeklyExecutionStartingAt(startTime: LocalDateTime): LocalDateTime {
        if (recurrence!!.byDays.isEmpty()) return LocalDateTime.now()
            .plusDays(1) // Just a date in the future

        // Look into the week of startTime for another scheduled day of the week
        for (dayOfWeek in recurrence!!.byDays) {
            val jodaDayOfWeek = convertCalendarDayOfWeekToJoda(dayOfWeek)
            val candidateNextDueTime = startTime.withDayOfWeek(jodaDayOfWeek)
            if (candidateNextDueTime.isAfter(startTime)) return candidateNextDueTime
        }

        // Return the first scheduled day of the week from the next due week
        val firstScheduledDayOfWeek = convertCalendarDayOfWeekToJoda(recurrence!!.byDays[0])
        return startTime.plusWeeks(recurrence!!.multiplier)
            .withDayOfWeek(firstScheduledDayOfWeek)
    }

    /**
     * Converts a java.util.Calendar day of the week constant to the
     * org.joda.time.DateTimeConstants equivalent.
     *
     * @param calendarDayOfWeek day of the week constant from java.util.Calendar
     * @return day of the week constant equivalent from org.joda.time.DateTimeConstants
     */
    private fun convertCalendarDayOfWeekToJoda(calendarDayOfWeek: Int): Int {
        val cal = Calendar.getInstance()
        cal[Calendar.DAY_OF_WEEK] = calendarDayOfWeek
        return LocalDateTime.fromCalendarFields(cal).dayOfWeek
    }

    /**
     * Set time of last execution of the scheduled action
     *
     * @param nextRun Timestamp in milliseconds since Epoch
     */
    fun setLastRun(nextRun: Long) {
        lastRunTime = nextRun
    }

    /**
     * Returns the period of this scheduled action in milliseconds.
     *
     * @return Period in milliseconds since Epoch
     */
    @get:Deprecated("Uses fixed values for time of months and years (which actually vary depending on number of days in month or leap year)")
    val period: Long
        get() = recurrence!!.period

    /**
     * The time of first execution of the scheduled action, represented as a timestamp in
     * milliseconds since Epoch
     */
    var startTime: Long
        get() = _startDate
        set(startDate) {
            _startDate = startDate
            if (recurrence != null) {
                recurrence!!.periodStart = Timestamp(startDate)
            }
        }

    /**
     * The end time of the scheduled action, represented as a timestamp in milliseconds since Epoch.
     */
    var endTime: Long
        get() = _endDate
        set(endDate) {
            _endDate = endDate
            if (recurrence != null) {
                recurrence!!.setPeriodEnd(Timestamp(_endDate))
            }
        }

    /**
     * Returns flag if transactions should be automatically created or not
     *
     * This flag is currently unused in the app. It is only included here for compatibility with GnuCash desktop XML
     *
     * @return `true` if the transaction should be auto-created, `false` otherwise
     */
    fun shouldAutoCreate(): Boolean {
        return _autoCreate
    }

    /**
     * Set flag for automatically creating transaction based on this scheduled action
     *
     * This flag is currently unused in the app. It is only included here for compatibility with GnuCash desktop XML
     *
     * @param autoCreate Flag for auto creating transactions
     */
    fun setAutoCreate(autoCreate: Boolean) {
        _autoCreate = autoCreate
    }

    /**
     * Check if user will be notified of creation of scheduled transactions
     *
     * This flag is currently unused in the app. It is only included here for compatibility with GnuCash desktop XML
     *
     * @return `true` if user will be notified, `false` otherwise
     */
    fun shouldAutoNotify(): Boolean {
        return _autoNotify
    }

    /**
     * Sets whether to notify the user that scheduled transactions have been created
     *
     * This flag is currently unused in the app. It is only included here for compatibility with GnuCash desktop XML
     *
     * @param autoNotify Boolean flag
     */
    fun setAutoNotify(autoNotify: Boolean) {
        _autoNotify = autoNotify
    }

    /** Backing field for @{link ScheduledAction#templateAccountUID} */
    private var _templateAccountUID: String? = null
    var templateAccountUID: String?
        /**
         * Return the template account GUID for this scheduled action
         *
         * If no GUID was set, a new one is going to be generated and returned.
         *
         * @return String GUID of template account
         */
        get() = if (_templateAccountUID == null) generateUID().also {
            _templateAccountUID = it
        } else _templateAccountUID
        /**
         * Set the template account GUID
         *
         * @param templateAccountUID String GUID of template account
         */
        set(templateAccountUID) {
            _templateAccountUID = templateAccountUID
        }

    /**
     * Returns the event schedule (start, end and recurrence)
     *
     * @return String description of repeat schedule
     */
    val repeatString: String
        get() {
            val ruleBuilder = StringBuilder(recurrence!!.repeatString)
            val context = GnuCashApplication.getAppContext()
            if (_endDate <= 0 && totalPlannedExecutionCount > 0) {
                ruleBuilder.append(", ")
                    .append(context.getString(R.string.repeat_x_times, totalPlannedExecutionCount))
            }
            return ruleBuilder.toString()
        }

    /**
     * Creates an RFC 2445 string which describes this recurring event
     *
     * See [recurrance](http://recurrance.sourceforge.net/)
     *
     * @return String describing event
     */
    val ruleString: String
        get() {
            val separator = ";"
            val ruleBuilder = StringBuilder(recurrence!!.ruleString)
            if (_endDate > 0) {
                val df = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss'Z'").withZoneUTC()
                ruleBuilder.append("UNTIL=").append(df.print(_endDate)).append(separator)
            } else if (totalPlannedExecutionCount > 0) {
                ruleBuilder.append("COUNT=").append(totalPlannedExecutionCount).append(separator)
            }
            return ruleBuilder.toString()
        }

    /**
     * Overloaded method for setting the recurrence of the scheduled action.
     *
     * This method allows you to specify the periodicity and the ordinal of it. For example,
     * a recurrence every fortnight would give parameters: [PeriodType.WEEK], ordinal:2
     *
     * @param periodType Periodicity of the scheduled action
     * @param ordinal    Ordinal of the periodicity. If unsure, specify 1
     * @see .setRecurrence
     */
    fun setRecurrence(periodType: PeriodType?, ordinal: Int) {
        val recurrence = Recurrence(
            periodType!!
        )
        recurrence.multiplier = ordinal
        setRecurrence(recurrence)
    }

    /**
     * Sets the recurrence pattern of this scheduled action
     *
     * This also sets the start period of the recurrence object, if there is one
     *
     * @param recurrence [Recurrence] object
     */
    fun setRecurrence(recurrence: Recurrence) {
        this.recurrence = recurrence
        //if we were parsing XML and parsed the start and end date from the scheduled action first,
        //then use those over the values which might be gotten from the recurrence
        if (_startDate > 0) {
            recurrence.periodStart = Timestamp(_startDate)
        } else {
            _startDate = recurrence.periodStart.time
        }
        if (_endDate > 0) {
            recurrence.setPeriodEnd(Timestamp(_endDate))
        } else {
            val periodEnd = recurrence.periodEnd
            if (periodEnd != null) {
                _endDate = periodEnd.time
            }
        }
    }

    override fun toString(): String {
        return actionType.name + " - " + repeatString
    }

    companion object {
        /**
         * Creates a ScheduledAction from a Transaction and a period
         *
         * @param transaction Transaction to be scheduled
         * @param period      Period in milliseconds since Epoch
         * @return Scheduled Action
         */
        @JvmStatic
        @Deprecated("Used for parsing legacy backup files. Use [Recurrence] instead")
        fun parseScheduledAction(transaction: Transaction, period: Long): ScheduledAction {
            val scheduledAction = ScheduledAction(ActionType.TRANSACTION)
            scheduledAction.actionUID = transaction.uID
            val recurrence = Recurrence.fromLegacyPeriod(period)
            scheduledAction.setRecurrence(recurrence)
            return scheduledAction
        }
    }
}