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

package org.gnucash.android.model;

import android.content.res.Resources;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
* Represents a type of period which can be associated with a recurring event
 * @author Ngewi Fet <ngewif@gmail.com>
 * @see org.gnucash.android.model.ScheduledAction
*/
public enum PeriodType {
    DAY, WEEK, MONTH, YEAR;

    int mMultiplier = 1; //multiplier for the period type

    /**
     * Sets the multiplier for this period type
     * e.g. bi-weekly actions have period type {@link PeriodType#WEEK} and multiplier 2
     * @param multiplier Multiplier for this period type
     */
    public void setMultiplier(int multiplier){
        mMultiplier = multiplier;
    }

    /**
     * Returns the multiplier for this period type. The default multiplier is 1.
     * e.g. bi-weekly actions have period type {@link PeriodType#WEEK} and multiplier 2
     * @return  Multiplier for this period type
     */
    public int getMultiplier(){
        return mMultiplier;
    }

    /**
     * Returns the frequency description of this period type.
     * This is used mostly for generating the recurrence rule.
     * @return Frequency description
     */
    public String getFrequencyDescription() {
        switch (this) {
            case DAY:
                return "DAILY";
            case WEEK:
                return "WEEKLY";
            case MONTH:
                return "MONTHLY";
            case YEAR:
                return "YEARLY";
            default:
                return "";
        }
    }

    /**
     * Returns a localized string describing this period type's frequency.
     * @return String describing period type
     */
    public String getFrequencyRepeatString(){
        Resources res = GnuCashApplication.getAppContext().getResources();

        switch (this) {
            case DAY:
                return res.getQuantityString(R.plurals.label_every_x_days, mMultiplier, mMultiplier);
            case WEEK:
                return res.getQuantityString(R.plurals.label_every_x_weeks, mMultiplier, mMultiplier);
            case MONTH:
                return res.getQuantityString(R.plurals.label_every_x_months, mMultiplier, mMultiplier);
            case YEAR:
                return res.getQuantityString(R.plurals.label_every_x_years, mMultiplier, mMultiplier);
            default:
                return "";
        }
    }

    /**
     * Returns the parts of the recurrence rule which describe the day or month on which to run the
     * scheduled transaction. These parts are the BYxxx
     * @param startTime Start time of transaction used to determine the start day of execution
     * @return String describing the BYxxx parts of the recurrence rule
     */
    public String getByParts(long startTime){
        String partString = "";
        switch (this){
            case DAY:
                break;
            case WEEK:
                String dayOfWeek = new SimpleDateFormat("E", Locale.US).format(new Date(startTime));
                //our parser only supports two-letter day names
                partString = "BYDAY=" + dayOfWeek.substring(0, dayOfWeek.length()-1).toUpperCase();
            case MONTH:
                break;
            case YEAR:
                break;
        }
        return partString;
    }


}
