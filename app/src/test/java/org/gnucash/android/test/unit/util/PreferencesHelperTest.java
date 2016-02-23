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

import org.gnucash.android.BuildConfig;
import org.gnucash.android.test.unit.testutil.GnucashTestRunner;
import org.gnucash.android.test.unit.testutil.ShadowCrashlytics;
import org.gnucash.android.test.unit.testutil.ShadowUserVoice;
import org.gnucash.android.util.PreferencesHelper;
import org.gnucash.android.util.TimestampHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(GnucashTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, packageName = "org.gnucash.android", shadows = {ShadowCrashlytics.class, ShadowUserVoice.class})
public class PreferencesHelperTest {

    @Test
    public void shouldGetLastExportTimeDefaultValue() {
        final Timestamp lastExportTime = PreferencesHelper.getLastExportTime();
        assertThat(lastExportTime).isEqualTo(TimestampHelper.getTimestampFromEpochZero());
    }

    @Test
    public void shouldGetLastExportTimeCurrentValue() {
        final long goldenBoyBirthday = 1190136000L * 1000;
        final Timestamp goldenBoyBirthdayTimestamp = new Timestamp(goldenBoyBirthday);
        PreferencesHelper.setLastExportTime(goldenBoyBirthdayTimestamp);
        assertThat(PreferencesHelper.getLastExportTime())
                .isEqualTo(goldenBoyBirthdayTimestamp);
    }
}