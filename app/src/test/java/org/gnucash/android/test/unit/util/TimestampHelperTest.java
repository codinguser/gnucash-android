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
package org.gnucash.android.test.unit.util;

import org.gnucash.android.util.TimestampHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 21, packageName = "org.gnucash.android", shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class TimestampHelperTest {

    @Test
    public void shouldGetUtcStringFromTimestamp() {
        /**
         * The values used here are well known.
         * See https://en.wikipedia.org/wiki/Unix_time#Notable_events_in_Unix_time
         * for details.
         */

        final long unixBillennium = 1_000_000_000 * 1000L;
        final String unixBillenniumUtcString = "2001-09-09 01:46:40.000";
        final Timestamp unixBillenniumTimestamp = new Timestamp(unixBillennium);
        assertThat(TimestampHelper.getUtcStringFromTimestamp(unixBillenniumTimestamp))
                .isEqualTo(unixBillenniumUtcString);

        final long the1234567890thSecond = 1234567890 * 1000L;
        final String the1234567890thSecondUtcString = "2009-02-13 23:31:30.000";
        final Timestamp the1234567890thSecondTimestamp = new Timestamp(the1234567890thSecond);
        assertThat(TimestampHelper.getUtcStringFromTimestamp(the1234567890thSecondTimestamp))
                .isEqualTo(the1234567890thSecondUtcString);
    }

    @Test
    public void shouldGetTimestampFromEpochZero() {
        Timestamp epochZero = TimestampHelper.getTimestampFromEpochZero();
        assertThat(epochZero.getTime()).isZero();
    }

    @Test
    public void shouldGetTimestampFromUtcString() {
        final long unixBillennium = 1_000_000_000 * 1000L;
        final String unixBillenniumUtcString = "2001-09-09 01:46:40";
        final String unixBillenniumWithMillisecondsUtcString = "2001-09-09 01:46:40.000";
        final Timestamp unixBillenniumTimestamp = new Timestamp(unixBillennium);
        assertThat(TimestampHelper.getTimestampFromUtcString(unixBillenniumUtcString))
                .isEqualTo(unixBillenniumTimestamp);
        assertThat(TimestampHelper.getTimestampFromUtcString(unixBillenniumWithMillisecondsUtcString))
                .isEqualTo(unixBillenniumTimestamp);

        final long the1234567890thSecond = 1234567890 * 1000L;
        final String the1234567890thSecondUtcString = "2009-02-13 23:31:30";
        final String the1234567890thSecondWithMillisecondsUtcString = "2009-02-13 23:31:30.000";
        final Timestamp the1234567890thSecondTimestamp = new Timestamp(the1234567890thSecond);
        assertThat(TimestampHelper.getTimestampFromUtcString(the1234567890thSecondUtcString))
                .isEqualTo(the1234567890thSecondTimestamp);
        assertThat(TimestampHelper.getTimestampFromUtcString(the1234567890thSecondWithMillisecondsUtcString))
                .isEqualTo(the1234567890thSecondTimestamp);
    }

    @Test
    public void shouldGetTimestampFromNow() {
        final long before = System.currentTimeMillis();
        final long now = TimestampHelper.getTimestampFromNow().getTime();
        final long after = System.currentTimeMillis();
        assertThat(now).isGreaterThanOrEqualTo(before)
                       .isLessThanOrEqualTo(after);
    }
}