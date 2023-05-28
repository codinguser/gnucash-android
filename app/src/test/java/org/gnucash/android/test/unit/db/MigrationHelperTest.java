/*
 * Copyright (c) 2016 Alceu Rodrigues Neto <alceurneto@gmail.com>
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
package org.gnucash.android.test.unit.db;

import org.gnucash.android.db.MigrationHelper;
import org.gnucash.android.util.TimestampHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.sql.Timestamp;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 21, packageName = "org.gnucash.android", shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class MigrationHelperTest {
    @Test
    public void shouldSubtractTimeZoneOffset() {
        /**
         * The values used here are well known.
         * See https://en.wikipedia.org/wiki/Unix_time#Notable_events_in_Unix_time
         * for details.
         */

        final long unixBillennium = 1_000_000_000 * 1000L;
        final Timestamp unixBillenniumTimestamp = new Timestamp(unixBillennium);
        final String unixBillenniumUtcString = "2001-09-09 01:46:40.000";
        final String unixBillenniumUtcStringAfterSubtract = "2001-09-09 00:46:40.000";

        TimeZone timeZone = TimeZone.getTimeZone("GMT-1:00");
        Timestamp result = MigrationHelper.subtractTimeZoneOffset(unixBillenniumTimestamp, timeZone);
        assertThat(TimestampHelper.getUtcStringFromTimestamp(result))
                .isEqualTo(unixBillenniumUtcStringAfterSubtract);

        timeZone = TimeZone.getTimeZone("GMT+1:00");
        result = MigrationHelper.subtractTimeZoneOffset(unixBillenniumTimestamp, timeZone);
        assertThat(TimestampHelper.getUtcStringFromTimestamp(result))
                .isEqualTo(unixBillenniumUtcStringAfterSubtract);

        timeZone = TimeZone.getTimeZone("GMT+0:00");
        result = MigrationHelper.subtractTimeZoneOffset(unixBillenniumTimestamp, timeZone);
        assertThat(TimestampHelper.getUtcStringFromTimestamp(result))
                .isEqualTo(unixBillenniumUtcString);
    }
}