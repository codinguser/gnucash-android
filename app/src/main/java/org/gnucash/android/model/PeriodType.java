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
        if (mMultiplier > 1){
            switch (this) {
                case DAY:
                    return "Every " + mMultiplier + " days";
                case WEEK:
                    return "Every " + mMultiplier + " weeks";
                case MONTH:
                    return "Every " + mMultiplier + " months";
                case YEAR:
                    return "Every " + mMultiplier + " years";
                default:
                    return "Every " + mMultiplier + " days";
            }
        } else {
            switch (this) {
                case DAY:
                    return "Daily";
                case WEEK:
                    return "Weekly";
                case MONTH:
                    return "Monthly";
                case YEAR:
                    return "Yearly";
                default:
                    return "Daily";
            }
        }
    }

    /**
     * Returns a localized string describing this period type's frequency.
     * @return String describing period type
     */
    public String getLocalizedFrequencyDescription(){
        Resources res = GnuCashApplication.getAppContext().getResources();

        switch (this) {
            case DAY:
                return res.getQuantityString(R.plurals.label_every_x_days, mMultiplier);
            case WEEK:
                return res.getQuantityString(R.plurals.label_every_x_weeks, mMultiplier);
            case MONTH:
                return res.getQuantityString(R.plurals.label_every_x_months, mMultiplier);
            case YEAR:
                return res.getQuantityString(R.plurals.label_every_x_years, mMultiplier);
            default:
                return res.getQuantityString(R.plurals.label_every_x_days, mMultiplier);
        }
    }

}
